package engine;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
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
    private List<Option> options;

    @XmlElement(name = "comision")
    private Commission commission;

    @XmlElement(name = "GM-method")
    private GMMethod method;

    private final List<Trade> tradeHistory = new ArrayList<>();
    private double accountBalance = 0;
    private double totalCommissionCollected = 0;
    private boolean closed = false;
    private String winningOption = null;

    //getters
    public String getName() {return name;}
    public int getID() {return ID;}
    public String getDescription() {return description;}
    public Commission getCommission() {return commission;}
    public LMSR_Data getLmsrData() {return method.getLmsr();}
    public List<Option> getOptions() {return Collections.unmodifiableList(options);}
    public List<Trade> getTradeHistory() {return Collections.unmodifiableList(tradeHistory);}
    public double getAccountBalance() {return accountBalance;}
    public double getTotalCommissionCollected() {return totalCommissionCollected;}
    public boolean isClosed() {return closed;}
    public String getWinningOption() {return winningOption;}

    public List<String> getOptionNames() {
        List<String> result = new ArrayList<>();
        this.options.forEach((option) -> {result.add(option.getOptionName());});
        return result;
    }

    // current LMSR cost of the market, computed live from option quantities
    public double getCurrentCost() {
        return this.method.getLmsr().cost_function(this.options);
    }

    public Map<String, Double> getOptionProbabilities() {
        LMSR_Data lmsr = this.method.getLmsr();
        double sumEx = lmsr.sumExponentials(this.options);

        Map<String, Double> result = new HashMap<>();
        for (Option option : this.options) {
            result.put(option.getOptionName(), lmsr.calc_probability(option, sumEx));
        }

        return result;
    }

    public Event() {
    }

    public boolean validateCommission(){
        return this.commission.getCommission() >= 0 && this.commission.getCommission() <= 90;
    }

    public void setGM_options(List<Option> options) {
        this.options = options;
    }

    private void updateOption(String target, int quantity){
        for (Option option : this.options) {
            if (option.getOptionName().equals(target)) {
                option.addShares(quantity);
            }
        }
    }

    // prices and records a share purchase against this event; the only path that mutates trade state
    public Trade executeTrade(String optionName, int quantity) {
        if (closed) throw new IllegalStateException("Cannot trade in a closed event.");

        double costBefore = getCurrentCost();
        updateOption(optionName, quantity);
        double costAfter = getCurrentCost();
        double sharesCost = costAfter - costBefore;

        double commissionPaid = 0;
        if ("on-purchase".equals(commission.getType())) {
            commissionPaid = sharesCost * commission.getCommission() / 100.0;
        }

        accountBalance += sharesCost + commissionPaid;
        totalCommissionCollected += commissionPaid;

        Trade trade = new Trade(optionName, quantity, sharesCost, commissionPaid);
        tradeHistory.add(trade);
        return trade;
    }

    // closes the event and settles the LMSR payout: $1 per winning share, minus on-close commission
    public void closeEvent(String winningOptionName) {
        if (closed) throw new IllegalStateException("Event is already closed.");

        int winningShares = getOptions().stream()
                .filter(option -> option.getOptionName().equals(winningOptionName))
                .mapToInt(Option::getOptionQuantity)
                .findFirst().orElse(0);

        double grossPayout = winningShares;
        accountBalance -= grossPayout;

        if ("on-close".equals(commission.getType())) {
            double commissionOnClose = grossPayout * commission.getCommission() / 100.0;
            accountBalance += commissionOnClose;
            totalCommissionCollected += commissionOnClose;
        }

        this.closed = true;
        this.winningOption = winningOptionName;
    }
}
