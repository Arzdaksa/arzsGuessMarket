package engine;

public record Trade(String optionName, int quantity, double sharesCost, double commissionPaid) {

    // total amount the buyer paid for this trade
    public double totalPaid() {
        return sharesCost + commissionPaid;
    }
}
