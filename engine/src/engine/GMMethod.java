package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMMethod {

    @XmlElement(name = "GM-LMSR")
    private LMSR_Data lmsr;

    public LMSR_Data getLmsr() {return lmsr;}

    public GMMethod() {}
}
