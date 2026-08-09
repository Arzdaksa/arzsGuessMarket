package engine;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuessMarketEngine {

    private static final String SCHEMA_FILE_NAME = "GM-EX1-Schema.xsd";

    private GuessMarket loadedEvents;
    public boolean isLoaded = false;

    public void loadDataFromXML(String filePath) {

        try {
            File file = new File(filePath);
            File schemaFile = new File(file.getParentFile(), SCHEMA_FILE_NAME);

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaFile);

            JAXBContext context = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setSchema(schema);

            GuessMarket xmlEvents = (GuessMarket) unmarshaller.unmarshal(file);
            this.validateEvents(xmlEvents);
            loadedEvents = xmlEvents;
            this.isLoaded = true;

            System.out.println("----- Message -----\n* File loaded successfully!");

        } catch (Exception e) {
            this.printException(e);
        }
    }

    private void printException(Exception e) {
        System.out.println("----- Message -----");
        System.out.println("* " + e.getMessage());
        System.out.println("* File not loaded!");
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

    public void printAllEvents()
    {
        loadedEvents.getEvents().forEach(event -> System.out.println(event.toString()));
    }

    public void printEventWithID(String eventID)
    {
        try {
            Event target = this.loadedEvents.getEventWithID(Integer.parseInt(eventID));
            if (target == null) {
                throw new Exception("Event with ID: " + eventID + " not found!");
            }


        }
        catch(Exception e) {
            this.printException(e);
        }
    }

    public GuessMarketEngine() {}
}
