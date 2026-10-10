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

    @XmlElement(name = "GM-order-book")
    private OrderBookConfig orderBookConfig;

    public LMSR_Data getLmsr() {return lmsr;}
    public OrderBookConfig getOrderBookConfig() {return orderBookConfig;}

    public MethodType getType() {
        return lmsr != null ? MethodType.LMSR : MethodType.ORDER_BOOK;
    }

    public GMMethod() {}

    GMMethod(LMSR_Data lmsr) {
        this.lmsr = lmsr;
    }

    GMMethod(OrderBookConfig orderBookConfig) {
        this.orderBookConfig = orderBookConfig;
    }
}
