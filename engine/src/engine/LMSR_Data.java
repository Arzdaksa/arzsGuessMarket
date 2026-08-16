package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class LMSR_Data implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(name = "b")
    private int b;

    public int getB() {return b;}
    public void setB(int b) {this.b = b;}

    public double sumExponentials(List<Option> options) {
        double sum = 0;
        for (Option option : options) {
            sum += Math.exp((double) option.getOptionQuantity() / b);
        }
        return sum;
    }

    public double calc_probability(Option target, double sumExponentials) {
        return Math.exp((double) target.getOptionQuantity() / b) / sumExponentials;
    }

    public double cost_function(List<Option> options){
        return this.b * Math.log((double)this.sumExponentials(options));
    }

    public LMSR_Data() {}
}
