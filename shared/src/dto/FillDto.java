package dto;

public record FillDto(String optionName, String buyerName, String sellerName,
                      int quantity, double price, double commission, boolean minted) {

    public double notional() {
        return quantity * price;
    }
}
