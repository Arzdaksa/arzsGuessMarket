package engine;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// holds the whole market in memory; not thread-safe, callers serialize access
public class GuessMarketEngine {

    private static final String SCHEMA_RESOURCE = "GM-EX3-Schema.xsd";

    private static final JAXBContext JAXB_CONTEXT;
    private static final Schema SCHEMA;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(GuessMarket.class);
            SCHEMA = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(GuessMarketEngine.class.getResource(SCHEMA_RESOURCE));
        } catch (JAXBException | org.xml.sax.SAXException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final List<Event> events = new ArrayList<>();
    // one live directory shared with every event, so users who join later can still be paid out
    private final Map<String, User> usersByName = new LinkedHashMap<>();
    private int nextEventId = 1;

    public GuessMarketEngine() {}

    public User registerUser(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("User name is required.");
        }
        if (usersByName.containsKey(trimmed)) {
            throw new IllegalStateException("User name '" + trimmed + "' is already taken.");
        }
        User user = new User(trimmed);
        usersByName.put(trimmed, user);
        return user;
    }

    public void deposit(User user, double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0.");
        }
        user.credit(amount);
    }

    // parses straight from the stream; either every event in the file is added or none is
    public List<Event> addEventsFromXml(InputStream xml, User uploader) throws Exception {
        if (uploader == null) {
            throw new IllegalArgumentException("An uploading user is required.");
        }
        List<Event> parsed;
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            XMLStreamReader reader = factory.createXMLStreamReader(xml);

            Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            unmarshaller.setSchema(SCHEMA);
            parsed = ((GuessMarket) unmarshaller.unmarshal(reader)).getEvents();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid XML file: " + describeError(e));
        }

        validateEvents(parsed);
        for (Event event : parsed) {
            addEvent(event, uploader);
        }
        return parsed;
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

    private void validateEvents(List<Event> candidates) {
        Set<String> seenNames = new HashSet<>();
        for (Event event : candidates) {
            String name = event.getName() == null ? "" : event.getName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Every event must have a name.");
            }
            if (!seenNames.add(nameKey(name))) {
                throw new IllegalArgumentException("Duplicate event name in file: '" + name + "'.");
            }
            if (isEventNameTaken(name)) {
                throw new IllegalArgumentException("An event named '" + name + "' already exists.");
            }
            if (!event.validateCommission()) {
                throw new IllegalArgumentException("Invalid commission on event '" + name + "' (must be 0-90).");
            }
            if (new HashSet<>(event.getOptionNames()).size() != event.getOptionNames().size()) {
                throw new IllegalArgumentException("Event '" + name + "' has duplicate option names.");
            }
        }
    }

    private void addEvent(Event event, User marketMaker) {
        event.setId(nextEventId++);
        event.setMarketMaker(marketMaker);
        event.setUserDirectory(usersByName);
        events.add(event);
    }

    private boolean isEventNameTaken(String name) {
        String key = nameKey(name);
        return events.stream().anyMatch(e -> nameKey(e.getName()).equals(key));
    }

    private static String nameKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<Event> getActiveEvents() {
        return events.stream().filter(Event::isActive).collect(Collectors.toList());
    }

    public List<Event> getInactiveEvents() {
        return events.stream().filter(Event::isInactive).collect(Collectors.toList());
    }

    public List<Event> getClosedEvents() {
        return events.stream().filter(Event::isClosed).collect(Collectors.toList());
    }

    public Event getEventById(int id) {
        for (Event event : events) {
            if (event.getID() == id) return event;
        }
        return null;
    }

    public List<User> getUsers() {
        return new ArrayList<>(usersByName.values());
    }

    public User getUserByName(String name) {
        return usersByName.get(name);
    }

    // bonus feature: builds and wires a new event outside of XML loading, with the given user as market maker
    public Event createEvent(User marketMaker, String name, String description, int commissionPercent,
                              String commissionType, List<String> optionNames, MethodType type,
                              Integer lmsrB, Integer orderBookD, Integer orderBookInitial, boolean allowMint) {
        if (marketMaker == null) {
            throw new IllegalArgumentException("A market maker must be selected.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name is required.");
        }
        if (isEventNameTaken(name)) {
            throw new IllegalArgumentException("An event named '" + name.trim() + "' already exists.");
        }
        if (commissionPercent < 0 || commissionPercent > 90) {
            throw new IllegalArgumentException("Commission must be between 0 and 90.");
        }
        if (!"on-purchase".equals(commissionType) && !"on-close".equals(commissionType)) {
            throw new IllegalArgumentException("Commission type must be 'on-purchase' or 'on-close'.");
        }
        if (optionNames == null || type == null) {
            throw new IllegalArgumentException("Options and market type are required.");
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

        Event event = new Event(0, name.trim(), description == null ? "" : description.trim(), options, commission, method);
        addEvent(event, marketMaker);
        return event;
    }
}
