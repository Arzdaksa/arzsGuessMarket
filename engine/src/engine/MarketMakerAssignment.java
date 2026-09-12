package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class MarketMakerAssignment implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(name = "event")
    private List<EventRef> events;

    public List<EventRef> getEvents() {
        return events == null ? List.of() : events;
    }

    public MarketMakerAssignment() {}
}
