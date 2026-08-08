package engine;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Event {
    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "id")
    private int ID;

    @XmlElement(name = "description")
    private String description;

    @XmlElementWrapper(name = "GM-options")
    @XmlElement(name = "GM-option")
    private List<String> GM_options;

    @XmlElement(name = "comision")
    private Commission commission;

    @XmlElement(name = "GM-method")
    private GMMethod method;

    //getters
    public String getName() {return name;}
    public int getID() {return ID;}
    public String getDescription() {return description;}
    public List<String> getGM_options() {return GM_options;}
    public Commission getCommission() {return commission;}
    public LMSR_Data getLmsrData() {return method.getLmsr();}

    public Event() {}

    @Override
    public String toString() {
        return "Event{" +
                "name='" + name + '\'' +
                ", ID=" + ID +
                ", description='" + description + '\'' +
                ", options=" + GM_options +
                ", commission=" + commission.getCommission() + " (" + commission.getType() + ")" +
                ", lmsrB=" + method.getLmsr().getB() +
                '}';
    }
}
