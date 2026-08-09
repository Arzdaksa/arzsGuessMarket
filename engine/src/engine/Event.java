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

    public boolean validateCommission(){
        return this.commission.getCommission() >= 0 && this.commission.getCommission() <= 90;
    }

    @Override
    public String toString() {
        return String.format(
                """
                _______ Event - (%d) __________
                | Name: %s
                | Description: %s
                | Commission: %d, type: %s
                | Options: %s
                | Active status: %s
                _______________________________
                """,
                this.ID,
                this.name,
                this.description,
                this.commission.getCommission(),
                this.commission.getType(),
                this.GM_options,
                "closed" //ask teacher about this *******
        );
    }
}
