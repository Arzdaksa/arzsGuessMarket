package engine;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;


public class Commission {

    @XmlElement(name = "comision")
    private int commission;

    @XmlAttribute(name = "type")
    private String type;

    public int getCommission() {return commission;}
    public String getType() {return type;}

    public void setCommission(int commission) {this.commission = commission;}
    public void setType(String type) {this.type = type;}

    public Commission() {}
}
