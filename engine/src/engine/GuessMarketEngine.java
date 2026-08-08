package engine;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

public class GuessMarketEngine {

    private GuessMarket xmlEvents;

    //extract data from xml
    public void loadDataFromXML(String filePath) {

        try {
            JAXBContext context = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            File file = new File(filePath);

            xmlEvents = (GuessMarket) unmarshaller.unmarshal(file);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
        System.out.println("sucsess");
        System.out.println(xmlEvents.getEventAt(0).toString());
    }

    public GuessMarketEngine() {}
}
