package engine;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class GuessMarket {

    @XmlElementWrapper(name = "GM-events")
    @XmlElement(name = "GM-event")
    private List<Event> events;

    public List<Event> getEvents() {
        return events == null ? List.of() : events;
    }

    public GuessMarket() {}
}
