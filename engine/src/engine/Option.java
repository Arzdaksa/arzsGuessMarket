package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class Option {

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
