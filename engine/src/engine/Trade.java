package engine;

import java.io.Serializable;

public record Trade(String optionName, int quantity, double sharesCost, double commissionPaid) implements Serializable {

    // total amount the buyer paid for this trade
    public double totalPaid() {
        return sharesCost + commissionPaid;
    }
}
