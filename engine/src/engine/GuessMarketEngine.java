package engine;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

public class GuessMarketEngine {

    private static final String SCHEMA_FILE_NAME = "GM-EX1-Schema.xsd";

    private GuessMarket xmlEvents;

    public void loadDataFromXML(String filePath) {

        try {
            File file = new File(filePath);
            File schemaFile = new File(file.getParentFile(), SCHEMA_FILE_NAME);

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaFile);

            JAXBContext context = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setSchema(schema);

            xmlEvents = (GuessMarket) unmarshaller.unmarshal(file);

            System.out.println("File read successfully!");
            System.out.println(xmlEvents.getEventAt(0).toString());

        } catch (JAXBException | SAXException e) {
            e.printStackTrace();
        }
    }

    public GuessMarketEngine() {}
}
