package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class LMSR_Data {

    @XmlElement(name = "b")
    private int b;

    public int getB() {return b;}
    public void setB(int b) {this.b = b;}

    public LMSR_Data() {}
}
