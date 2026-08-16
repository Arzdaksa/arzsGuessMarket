package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class Option implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlValue
    private String optionName;

    @XmlTransient
    private int optionQuantity = 0;

    public void addShares(int quantity) {
        optionQuantity += quantity;
    }

    public String getOptionName() {return optionName;}
    public int getOptionQuantity() {return optionQuantity;}

    public Option() {
    }

}
