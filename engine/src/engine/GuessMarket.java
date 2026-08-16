package engine;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.io.Serializable;

@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class GuessMarket implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElementWrapper(name = "GM-events")
    @XmlElement(name = "GM-event")
    private List<Event> events;

    public List<Event> getEvents() {
        return events;
    }

    public GuessMarket() {}
}
