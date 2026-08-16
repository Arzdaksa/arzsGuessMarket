package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMMethod implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(name = "GM-LMSR")
    private LMSR_Data lmsr;

    public LMSR_Data getLmsr() {return lmsr;}

    public GMMethod() {}
}
