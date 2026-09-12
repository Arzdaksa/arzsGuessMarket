package engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// per-event limit order book: same-option price-time matching plus cross-option auto-minting
public class OrderBook implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Event event;
    private final OrderBookConfig config;
    private final Map<String, List<Order>> bids = new HashMap<>();
    private final Map<String, List<Order>> asks = new HashMap<>();
    private final List<OrderBookTrade> fills = new ArrayList<>();
    private final Map<String, Map<String, Integer>> holdings = new HashMap<>();
    private int nextOrderId = 1;
    private long sequenceCounter = 0;

    OrderBook(Event event, OrderBookConfig config) {
        this.event = event;
        this.config = config;
        for (String optionName : event.getOptionNames()) {
            bids.put(optionName, new ArrayList<>());
            asks.put(optionName, new ArrayList<>());
        }
    }

    public OrderBookConfig getConfig() {return config;}

    public List<Order> getBids(String optionName) {
        return Collections.unmodifiableList(bids.getOrDefault(optionName, List.of()));
    }

    public List<Order> getAsks(String optionName) {
        return Collections.unmodifiableList(asks.getOrDefault(optionName, List.of()));
    }

    public List<OrderBookTrade> getFills() {
        return Collections.unmodifiableList(fills);
    }

    public Double getBestBid(String optionName) {
        List<Order> list = bids.get(optionName);
        return (list == null || list.isEmpty()) ? null : list.get(0).getPrice();
    }

    public Double getBestAsk(String optionName) {
        List<Order> list = asks.get(optionName);
        return (list == null || list.isEmpty()) ? null : list.get(0).getPrice();
    }

    public Double getLast(String optionName) {
        for (int i = fills.size() - 1; i >= 0; i--) {
            if (fills.get(i).optionName().equals(optionName)) return fills.get(i).price();
        }
        return null;
    }

    public Double getMid(String optionName) {
        Double bid = getBestBid(optionName), ask = getBestAsk(optionName);
        return (bid == null || ask == null) ? null : (bid + ask) / 2.0;
    }

    public Double getSpread(String optionName) {
        Double bid = getBestBid(optionName), ask = getBestAsk(optionName);
        return (bid == null || ask == null) ? null : ask - bid;
    }

    public Map<String, Integer> getHoldings(String userName) {
        return Collections.unmodifiableMap(holdings.getOrDefault(userName, Map.of()));
    }

    public Set<String> getParticipantNames() {
        return Collections.unmodifiableSet(holdings.keySet());
    }

    // called once by Event.open() to seed the MM's initial inventory
    void creditInitialHoldings(String userName, String optionName, int quantity) {
        addHolding(userName, optionName, quantity);
    }

    public OrderSubmissionResult submitOrder(User user, String optionName, OrderSide side, int quantity, double price) {
        if (event.getState() != EventState.ACTIVE) {
            throw new IllegalStateException("Event is not active; cannot submit orders.");
        }
        if (user.isLocked()) {
            throw new IllegalStateException("User '" + user.getName() + "' is locked and cannot submit orders.");
        }
        if (!event.getOptionNames().contains(optionName)) {
            throw new IllegalArgumentException("Unknown option: " + optionName);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        double maxPrice = config.getD() - 0.01;
        if (price <= 0 || price > maxPrice) {
            throw new IllegalArgumentException("Price must be between 0 and " + maxPrice + ".");
        }

        if (side == OrderSide.ASK) {
            int held = holdings.getOrDefault(user.getName(), Map.of()).getOrDefault(optionName, 0);
            if (held < quantity) {
                throw new IllegalStateException("User '" + user.getName() + "' does not hold enough shares of '"
                        + optionName + "' to sell (" + held + " held, " + quantity + " requested).");
            }
            addHolding(user.getName(), optionName, -quantity); // reserve immediately
        }

        Order incoming = new Order(nextOrderId++, user, side, optionName, quantity, price, sequenceCounter++);
        List<OrderBookTrade> newFills = new ArrayList<>();

        // 1. normal same-option matching, price-time priority
        List<Order> opposite = (side == OrderSide.BID) ? asks.get(optionName) : bids.get(optionName);
        while (incoming.getQuantity() > 0 && !opposite.isEmpty()) {
            Order best = opposite.get(0);
            boolean crosses = (side == OrderSide.BID) ? price >= best.getPrice() : best.getPrice() >= price;
            if (!crosses) break;

            int matchQty = Math.min(incoming.getQuantity(), best.getQuantity());
            double fillPrice = best.getPrice();
            User buyer = (side == OrderSide.BID) ? user : best.getOwner();
            User seller = (side == OrderSide.BID) ? best.getOwner() : user;

            newFills.add(settleMatch(optionName, buyer, seller, matchQty, fillPrice, user));

            incoming.reduceQuantity(matchQty);
            best.reduceQuantity(matchQty);
            if (best.getQuantity() == 0) opposite.remove(0);
        }

        // 2. auto-mint (BID orders only, cross-option)
        if (side == OrderSide.BID && incoming.getQuantity() > 0 && config.isAllowMint()) {
            String otherOptionName = otherOption(optionName);
            List<Order> otherBids = bids.get(otherOptionName);
            while (incoming.getQuantity() > 0 && !otherBids.isEmpty()) {
                Order otherBest = otherBids.get(0);
                double combined = price + otherBest.getPrice();
                if (combined < config.getD()) break;

                int mintQty = Math.min(incoming.getQuantity(), otherBest.getQuantity());
                double thisFillPrice = (combined > config.getD()) ? config.getD() - otherBest.getPrice() : price;
                double otherFillPrice = otherBest.getPrice();

                OrderBookTrade[] mintTrades = settleMint(optionName, user, thisFillPrice,
                        otherOptionName, otherBest.getOwner(), otherFillPrice, mintQty);
                newFills.add(mintTrades[0]);
                newFills.add(mintTrades[1]);

                incoming.reduceQuantity(mintQty);
                otherBest.reduceQuantity(mintQty);
                if (otherBest.getQuantity() == 0) otherBids.remove(0);
            }
        }

        // 3. rest remainder
        int restingQty = incoming.getQuantity();
        if (restingQty > 0) {
            Order resting = new Order(incoming.getId(), user, side, optionName, restingQty, price, incoming.getSequence());
            List<Order> book = (side == OrderSide.BID) ? bids.get(optionName) : asks.get(optionName);
            insertSorted(book, resting, side == OrderSide.BID);
        }

        user.markParticipated(event.getID());
        fills.addAll(newFills);

        return new OrderSubmissionResult(quantity - restingQty, restingQty, newFills);
    }

    private OrderBookTrade settleMatch(String optionName, User buyer, User seller, int quantity, double fillPrice, User taker) {
        double notional = quantity * fillPrice;
        double commission = event.onPurchaseCommission(notional);

        buyer.debit(notional + (taker == buyer ? commission : 0));
        seller.credit(notional - (taker == seller ? commission : 0));
        if (commission > 0) {
            event.getMarketMaker().credit(commission);
            event.addCommissionCollected(commission);
        }

        addHolding(buyer.getName(), optionName, quantity);
        // seller's holding was already reserved (decremented) at ask-submission time

        return new OrderBookTrade(optionName, buyer.getName(), seller.getName(), quantity, fillPrice, commission, false);
    }

    private OrderBookTrade[] settleMint(String optionA, User userA, double priceA,
                                         String optionB, User userB, double priceB, int quantity) {
        double notionalA = quantity * priceA;
        double notionalB = quantity * priceB;
        double commissionA = event.onPurchaseCommission(notionalA);
        double commissionB = event.onPurchaseCommission(notionalB);

        userA.debit(notionalA + commissionA);
        userB.debit(notionalB + commissionB);

        double totalCommission = commissionA + commissionB;
        if (totalCommission > 0) {
            event.getMarketMaker().credit(totalCommission);
            event.addCommissionCollected(totalCommission);
        }
        event.creditAccountBalance(notionalA + notionalB);

        addHolding(userA.getName(), optionA, quantity);
        addHolding(userB.getName(), optionB, quantity);

        return new OrderBookTrade[]{
                new OrderBookTrade(optionA, userA.getName(), "MINT", quantity, priceA, commissionA, true),
                new OrderBookTrade(optionB, userB.getName(), "MINT", quantity, priceB, commissionB, true)
        };
    }

    private void addHolding(String userName, String optionName, int delta) {
        Map<String, Integer> userHoldings = holdings.computeIfAbsent(userName, k -> new HashMap<>());
        userHoldings.merge(optionName, delta, Integer::sum);
    }

    private String otherOption(String optionName) {
        for (String name : event.getOptionNames()) {
            if (!name.equals(optionName)) return name;
        }
        throw new IllegalStateException("Order book event does not have exactly two options.");
    }

    private void insertSorted(List<Order> book, Order order, boolean descending) {
        int index = 0;
        while (index < book.size()) {
            double existingPrice = book.get(index).getPrice();
            boolean staysAhead = descending ? existingPrice >= order.getPrice() : existingPrice <= order.getPrice();
            if (!staysAhead) break;
            index++;
        }
        book.add(index, order);
    }
}
