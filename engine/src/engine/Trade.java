package engine;

import java.io.Serializable;

public record Trade(String buyerName, String optionName, int quantity, double sharesCost, double commissionPaid)
        implements Serializable {

    public double totalPaid() {
        return sharesCost + commissionPaid;
    }
}
