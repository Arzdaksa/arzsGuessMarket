package dto;

public record TradeDto(String buyerName, String optionName, int quantity, double sharesCost, double commissionPaid) {

    public double totalPaid() {
        return sharesCost + commissionPaid;
    }
}
