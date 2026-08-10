package engine;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class Event {
    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "id")
    private int ID;

    @XmlElement(name = "description")
    private String description;

    @XmlElementWrapper(name = "GM-options")
    @XmlElement(name = "GM-option")
    private List<Option> GM_options;

    @XmlElement(name = "comision")
    private Commission commission;

    @XmlElement(name = "GM-method")
    private GMMethod method;

    //getters
    public String getName() {return name;}
    public int getID() {return ID;}
    public String getDescription() {return description;}
    public Commission getCommission() {return commission;}
    public LMSR_Data getLmsrData() {return method.getLmsr();}
    public List<String> getGM_options_names() {
        List<String> result = new ArrayList<>();
        this.GM_options.forEach((option) -> {result.add(option.getOptionName());});
        return result;
    }

    public Map<String, Double> get_options_probabilities() {
        LMSR_Data lmsr = this.method.getLmsr();
        double sumEx = lmsr.sumExponentials(this.GM_options);

        Map<String, Double> result = new HashMap<>();
        for (Option option : this.GM_options) {
            result.put(option.getOptionName(), lmsr.calc_probability(option, sumEx));
        }

        return result;
    }


    public Event() {}

    public boolean validateCommission(){
        return this.commission.getCommission() >= 0 && this.commission.getCommission() <= 90;
    }

    public void setGM_options(List<Option> GM_options) {
        this.GM_options = GM_options;
    }
}
