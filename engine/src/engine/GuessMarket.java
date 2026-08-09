package engine;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class GuessMarket {

    @XmlElementWrapper(name = "GM-events")
    @XmlElement(name = "GM-event")
    private List<Event> events;

    public Event getEventWithID(int searchID) {
        for  (Event event : events) {
            if (event.getID() == searchID) {
                return event;
            }
        }
        return null;
    }

    public List<Event> getEvents() {
        return events;
    }

    public GuessMarket() {}
}
