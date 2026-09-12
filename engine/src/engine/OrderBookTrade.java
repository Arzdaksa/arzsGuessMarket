package engine;

import java.io.Serializable;

// one executed leg (a normal match, or one side of a mint — "MINT" as sellerName marks the latter)
public record OrderBookTrade(String optionName, String buyerName, String sellerName,
                              int quantity, double price, double commission, boolean minted)
        implements Serializable {

    public double notional() {
        return quantity * price;
    }
}
