package engine;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.Serializable;

public class GuessMarketEngine implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SCHEMA_FILE_NAME = "GM-EX2-Schema.xsd";

    private static final JAXBContext JAXB_CONTEXT;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(GuessMarket.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private GuessMarket loadedEvents;
    private List<User> users = new ArrayList<>();
    public boolean isLoaded = false;
    public boolean dynamicEventCreationEnabled = false;

    public LoadResult loadDataFromXML(String filePath) {

        try {
            File file = new File(filePath);
            File schemaFile = new File(file.getParentFile(), SCHEMA_FILE_NAME);

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaFile);

            Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            unmarshaller.setSchema(schema);

            GuessMarket xmlEvents = (GuessMarket) unmarshaller.unmarshal(file);
            this.validateEvents(xmlEvents);
            this.validateUsersAndMarketMakers(xmlEvents);
            this.wireUsersAndMarketMakers(xmlEvents);

            loadedEvents = xmlEvents;
            this.users = new ArrayList<>(xmlEvents.getUsers());
            this.isLoaded = true;

            return new LoadResult(true, "File loaded successfully!");

        } catch (Exception e) {
            return new LoadResult(false, describeError(e));
        }
    }

    // walks the cause chain since JAXB validation failures often carry a null top-level message
    private String describeError(Throwable t) {
        Throwable current = t;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return t.toString();
    }

    public List<Event> getEvents() {
        return loadedEvents == null ? List.of() : loadedEvents.getEvents();
    }

    public List<Event> getActiveEvents() {
        return getEvents().stream().filter(Event::isActive).collect(Collectors.toList());
    }

    public List<Event> getInactiveEvents() {
        return getEvents().stream().filter(Event::isInactive).collect(Collectors.toList());
    }

    public List<Event> getClosedEvents() {
        return getEvents().stream().filter(Event::isClosed).collect(Collectors.toList());
    }

    public List<User> getUsers() {
        return users;
    }

    public User getUserByName(String name) {
        for (User user : users) {
            if (user.getName().equals(name)) return user;
        }
        return null;
    }

    private void validateEvents(GuessMarket eventsToValidate) throws Exception {
        List<Event> events = eventsToValidate.getEvents();
        Set<Integer> seenIds = new HashSet<>();

        for (Event event : events) {
            if (!event.validateCommission()) {
                throw new Exception("Invalid Commission on Event ID: " + event.getID());
            }

            if (!seenIds.add(event.getID())) {
                throw new Exception("Duplicate Event ID found: " + event.getID());
            }

            if (event.getEventType() == MethodType.LMSR && event.getLmsrData().getB() <= 0) {
                throw new Exception("Event ID " + event.getID() + " has an invalid LMSR 'b' value (must be greater than 0).");
            }
        }
    }

    // unique user names, positive initial cash, valid MM references, exactly one MM per event
    private void validateUsersAndMarketMakers(GuessMarket data) throws Exception {
        List<User> dataUsers = data.getUsers();
        List<Event> events = data.getEvents();

        Set<String> seenNames = new HashSet<>();
        for (User user : dataUsers) {
            if (!seenNames.add(user.getName())) {
                throw new Exception("Duplicate user name found: " + user.getName());
            }
            if (user.getInitialCash() <= 0) {
                throw new Exception("User '" + user.getName() + "' must have an initial cash balance greater than 0.");
            }
        }

        Set<Integer> eventIds = new HashSet<>();
        for (Event event : events) eventIds.add(event.getID());

        Map<Integer, Integer> mmCountByEventId = new HashMap<>();
        for (User user : dataUsers) {
            for (EventRef ref : user.getMarketMakerEventRefs()) {
                if (!eventIds.contains(ref.getId())) {
                    throw new Exception("User '" + user.getName() + "' is assigned as market maker for unknown event id "
                            + ref.getId() + ".");
                }
                mmCountByEventId.merge(ref.getId(), 1, Integer::sum);
            }
        }

        for (Event event : events) {
            int count = mmCountByEventId.getOrDefault(event.getID(), 0);
            if (count == 0) {
                throw new Exception("Event '" + event.getName() + "' (id " + event.getID() + ") has no assigned market maker.");
            }
            if (count > 1) {
                throw new Exception("Event '" + event.getName() + "' (id " + event.getID() + ") has more than one assigned market maker.");
            }
        }
    }

    private void wireUsersAndMarketMakers(GuessMarket data) {
        List<User> dataUsers = data.getUsers();
        for (User user : dataUsers) {
            user.initializeBalance();
        }

        Map<String, User> userDirectory = new HashMap<>();
        for (User user : dataUsers) userDirectory.put(user.getName(), user);

        Map<Integer, User> mmByEventId = new HashMap<>();
        for (User user : dataUsers) {
            for (EventRef ref : user.getMarketMakerEventRefs()) {
                mmByEventId.put(ref.getId(), user);
            }
        }

        for (Event event : data.getEvents()) {
            event.setMarketMaker(mmByEventId.get(event.getID()));
            event.setUserDirectory(userDirectory);
        }
    }

    // bonus feature: builds and wires a new event outside of XML loading, with the given user as market maker
    public Event createEvent(User marketMaker, String name, String description, int commissionPercent,
                              String commissionType, List<String> optionNames, MethodType type,
                              Integer lmsrB, Integer orderBookD, Integer orderBookInitial, boolean allowMint) {
        if (!dynamicEventCreationEnabled) {
            throw new IllegalStateException("Dynamic event creation is not enabled.");
        }
        if (!isLoaded || loadedEvents == null) {
            throw new IllegalStateException("Load market data before creating an event.");
        }
        if (marketMaker == null) {
            throw new IllegalArgumentException("A market maker must be selected.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name is required.");
        }
        if (commissionPercent < 0 || commissionPercent > 90) {
            throw new IllegalArgumentException("Commission must be between 0 and 90.");
        }
        if (!"on-purchase".equals(commissionType) && !"on-close".equals(commissionType)) {
            throw new IllegalArgumentException("Commission type must be 'on-purchase' or 'on-close'.");
        }

        List<String> distinctNames = optionNames.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        if (distinctNames.size() < 2) {
            throw new IllegalArgumentException("At least two distinct options are required.");
        }
        if (type == MethodType.ORDER_BOOK && distinctNames.size() != 2) {
            throw new IllegalArgumentException("Order Book events support exactly two options.");
        }

        List<Option> options = new ArrayList<>();
        for (String optionName : distinctNames) {
            options.add(new Option(optionName));
        }

        Commission commission = new Commission();
        commission.setCommission(commissionPercent);
        commission.setType(commissionType);

        GMMethod method;
        if (type == MethodType.LMSR) {
            if (lmsrB == null || lmsrB <= 0) {
                throw new IllegalArgumentException("LMSR liquidity parameter 'b' must be greater than 0.");
            }
            LMSR_Data lmsr = new LMSR_Data();
            lmsr.setB(lmsrB);
            method = new GMMethod(lmsr);
        } else {
            if (orderBookD == null || orderBookD <= 0) {
                throw new IllegalArgumentException("Order Book base value 'd' must be greater than 0.");
            }
            if (orderBookInitial == null || orderBookInitial < 0) {
                throw new IllegalArgumentException("Order Book initial investment must be zero or greater.");
            }
            method = new GMMethod(new OrderBookConfig(orderBookInitial, orderBookD, allowMint));
        }

        int newId = getEvents().stream().mapToInt(Event::getID).max().orElse(0) + 1;
        Event event = new Event(newId, name.trim(), description == null ? "" : description.trim(), options, commission, method);
        event.setMarketMaker(marketMaker);
        event.setUserDirectory(buildUserDirectory());

        loadedEvents.getEvents().add(event);
        return event;
    }

    private Map<String, User> buildUserDirectory() {
        Map<String, User> directory = new HashMap<>();
        for (User user : users) directory.put(user.getName(), user);
        return directory;
    }

    public boolean saveState(String path) {
        try (FileOutputStream fileOut = new FileOutputStream(path);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(this);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean loadState(String path) {
        try (FileInputStream fileIn = new FileInputStream(path);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            GuessMarketEngine loaded = (GuessMarketEngine) in.readObject();
            this.loadedEvents = loaded.loadedEvents;
            this.isLoaded = loaded.isLoaded;
            this.users = loaded.users;
            return true;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public GuessMarketEngine() {}
}
