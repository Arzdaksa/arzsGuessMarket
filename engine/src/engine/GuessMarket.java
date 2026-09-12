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

    @XmlElementWrapper(name = "GM-users")
    @XmlElement(name = "GM-user")
    private List<User> users;

    public List<Event> getEvents() {
        return events;
    }

    public List<User> getUsers() {
        return users == null ? List.of() : users;
    }

    public GuessMarket() {}
}
