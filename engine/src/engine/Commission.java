package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class Commission implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlValue
    private int commission;

    @XmlAttribute(name = "type")
    private String type;

    public int getCommission() {return commission;}
    public String getType() {return type;}

    public void setCommission(int commission) {this.commission = commission;}
    public void setType(String type) {this.type = type;}

    public Commission() {}
}
