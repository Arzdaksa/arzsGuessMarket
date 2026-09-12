package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;

// one <event id="X"/> entry inside a GM-market-maker block
@XmlAccessorType(XmlAccessType.FIELD)
public class EventRef implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlAttribute
    private int id;

    public int getId() {return id;}

    public EventRef() {}
}
