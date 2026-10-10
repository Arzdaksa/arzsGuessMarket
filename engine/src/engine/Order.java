package engine;

import java.io.Serializable;

public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final User owner;
    private final OrderSide side;
    private final String optionName;
    private int quantity;
    private final double price;
    private final long sequence;

    Order(int id, User owner, OrderSide side, String optionName, int quantity, double price, long sequence) {
        this.id = id;
        this.owner = owner;
        this.side = side;
        this.optionName = optionName;
        this.quantity = quantity;
        this.price = price;
        this.sequence = sequence;
    }

    public int getId() {return id;}
    public User getOwner() {return owner;}
    public String getOwnerName() {return owner.getName();}
    public OrderSide getSide() {return side;}
    public String getOptionName() {return optionName;}
    public int getQuantity() {return quantity;}
    public double getPrice() {return price;}
    public long getSequence() {return sequence;}

    void reduceQuantity(int amount) {
        quantity -= amount;
    }
}
