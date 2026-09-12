package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrderBookConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlAttribute(name = "allow-mint")
    private boolean allowMint;

    @XmlAttribute
    private int initial;

    @XmlAttribute
    private int d;

    public boolean isAllowMint() {return allowMint;}
    public int getInitial() {return initial;}
    public int getD() {return d;}

    public OrderBookConfig() {}
}
