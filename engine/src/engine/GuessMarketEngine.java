package engine;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GuessMarketEngine {

    private static final String SCHEMA_FILE_NAME = "GM-EX1-Schema.xsd";

    private static final JAXBContext JAXB_CONTEXT;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(GuessMarket.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private GuessMarket loadedEvents;
    public boolean isLoaded = false;

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
            loadedEvents = xmlEvents;
            this.isLoaded = true;

            return new LoadResult(true, "File loaded successfully!");

        } catch (Exception e) {
            return new LoadResult(false, e.getMessage());
        }
    }

    public List<Event> getEvents() {
        return loadedEvents == null ? List.of() : loadedEvents.getEvents();
    }

    // events still open for trading
    public List<Event> getActiveEvents() {
        return getEvents().stream().filter(event -> !event.isClosed()).collect(Collectors.toList());
    }

    private void validateEvents(GuessMarket eventsToValidate) throws Exception {
        List<Event> events = eventsToValidate.getEvents();
        Set<Integer> seenIds = new HashSet<>();

        for (Event event : events) {
            //check if all commissions valid
            if (!event.validateCommission()) {
                throw new Exception("Invalid Commission on Event ID: " + event.getID());
            }

            //check if IDs unique
            if (!seenIds.add(event.getID())) {
                throw new Exception("Duplicate Event ID found: " + event.getID());
            }
        }
    }

    public GuessMarketEngine() {}
}
