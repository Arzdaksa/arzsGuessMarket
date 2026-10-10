package engine;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlAttribute(name = "name")
    private String name;

    // assigned by the engine when the event is added; the upload schema carries no id
    @XmlTransient private int ID;

    @XmlElement(name = "description")
    private String description;

    @XmlElementWrapper(name = "GM-options")
    @XmlElement(name = "GM-option")
    private List<Option> options;

    @XmlElement(name = "commission")
    private Commission commission;

    @XmlElement(name = "GM-method")
    private GMMethod method;

    private final List<Trade> tradeHistory = new ArrayList<>();
    private double accountBalance = 0;
    private double totalCommissionCollected = 0;
    private String winningOption = null;

    @XmlTransient private EventState state = EventState.INACTIVE;
    @XmlTransient private User marketMaker;
    @XmlTransient private OrderBook orderBook;
    @XmlTransient private Map<String, User> userDirectory = Map.of();

    public String getName() {return name;}
    public int getID() {return ID;}
    public String getDescription() {return description;}
    public Commission getCommission() {return commission;}
    public LMSR_Data getLmsrData() {return method.getLmsr();}
    public List<Option> getOptions() {return Collections.unmodifiableList(options);}
    public List<Trade> getTradeHistory() {return Collections.unmodifiableList(tradeHistory);}
    public double getAccountBalance() {return accountBalance;}
    public double getTotalCommissionCollected() {return totalCommissionCollected;}
    public String getWinningOption() {return winningOption;}

    public EventState getState() {return state;}
    public boolean isActive() {return state == EventState.ACTIVE;}
    public boolean isInactive() {return state == EventState.INACTIVE;}
    public boolean isClosed() {return state == EventState.CLOSED;}
    public User getMarketMaker() {return marketMaker;}
    public MethodType getEventType() {return method.getType();}
    public OrderBookConfig getOrderBookConfig() {return method.getOrderBookConfig();}
    public OrderBook getOrderBook() {return orderBook;}

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

    // used by GuessMarketEngine.createEvent() to build events outside of XML loading
    Event(int id, String name, String description, List<Option> options, Commission commission, GMMethod method) {
        this.ID = id;
        this.name = name;
        this.description = description;
        this.options = options;
        this.commission = commission;
        this.method = method;
    }

    public boolean validateCommission(){
        return this.commission.getCommission() >= 0 && this.commission.getCommission() <= 90;
    }

    public void setGM_options(List<Option> options) {
        this.options = options;
    }

    void setId(int id) {
        this.ID = id;
    }

    // wired once by GuessMarketEngine when the event is added
    void setMarketMaker(User marketMaker) {
        this.marketMaker = marketMaker;
    }

    void setUserDirectory(Map<String, User> userDirectory) {
        this.userDirectory = userDirectory;
    }

    private void updateOption(String target, int quantity){
        for (Option option : this.options) {
            if (option.getOptionName().equals(target)) {
                option.addShares(quantity);
            }
        }
    }

    // on-purchase commission for a given notional amount; 0 for on-close events. Shared by LMSR trades and OB fills.
    double onPurchaseCommission(double notional) {
        return "on-purchase".equals(commission.getType()) ? notional * commission.getCommission() / 100.0 : 0;
    }

    void addCommissionCollected(double amount) {
        totalCommissionCollected += amount;
    }

    void creditAccountBalance(double amount) {
        accountBalance += amount;
    }

    public List<Trade> getTradeHistoryForUser(String userName) {
        List<Trade> result = new ArrayList<>();
        for (Trade trade : tradeHistory) {
            if (trade.buyerName().equals(userName)) result.add(trade);
        }
        return result;
    }

    public boolean canOpen(User requester) {
        return state == EventState.INACTIVE && requester != null && requester == marketMaker;
    }

    // LMSR: the zero-share subsidy; Order Book: initial pairs * d
    public double previewOpenCost() {
        if (getEventType() == MethodType.LMSR) {
            return getCurrentCost();
        }
        OrderBookConfig config = getOrderBookConfig();
        return config.getInitial() * (double) config.getD();
    }

    public void open(User requester) {
        if (state != EventState.INACTIVE) {
            throw new IllegalStateException("Event is not inactive.");
        }
        if (marketMaker == null || requester != marketMaker) {
            throw new SecurityException("Only the assigned market maker can open this event.");
        }

        double cost = previewOpenCost();
        requester.debit(cost);

        accountBalance += cost;
        if (getEventType() == MethodType.ORDER_BOOK) {
            OrderBookConfig config = getOrderBookConfig();
            orderBook = new OrderBook(this, config);
            for (String optionName : getOptionNames()) {
                orderBook.creditInitialHoldings(requester.getName(), optionName, config.getInitial());
            }
        }
        state = EventState.ACTIVE;
    }

    // user-aware LMSR purchase; blocks and locks the buyer (via User.debit) if it would go negative
    public Trade executeTrade(User buyer, String optionName, int quantity) {
        if (state != EventState.ACTIVE) {
            throw new IllegalStateException("Event is not active.");
        }
        if (buyer.isLocked()) {
            throw new IllegalStateException("User '" + buyer.getName() + "' is locked and cannot trade.");
        }

        double costBefore = getCurrentCost();
        updateOption(optionName, quantity);
        double costAfter = getCurrentCost();
        double sharesCost = costAfter - costBefore;
        double commissionPaid = onPurchaseCommission(sharesCost);

        try {
            buyer.debit(sharesCost + commissionPaid);
        } catch (RuntimeException e) {
            updateOption(optionName, -quantity);
            throw e;
        }

        accountBalance += sharesCost;
        if (commissionPaid > 0 && marketMaker != null) {
            marketMaker.credit(commissionPaid);
        }
        totalCommissionCollected += commissionPaid;

        Trade trade = new Trade(buyer.getName(), optionName, quantity, sharesCost, commissionPaid);
        tradeHistory.add(trade);
        buyer.markParticipated(ID);
        return trade;
    }

    // user-aware close: MM-gated, pays real per-user payouts, sweeps remaining escrow to the MM
    public void closeEvent(User requester, String winningOptionName) {
        if (marketMaker == null || requester != marketMaker) {
            throw new SecurityException("Only the assigned market maker can close this event.");
        }
        if (state != EventState.ACTIVE) {
            throw new IllegalStateException("Event must be active to close.");
        }
        if (!getOptionNames().contains(winningOptionName)) {
            throw new IllegalArgumentException("Unknown option: " + winningOptionName);
        }

        if (getEventType() == MethodType.LMSR) {
            payoutLmsrWinners(winningOptionName);
        } else {
            payoutOrderBookWinners(winningOptionName);
        }

        marketMaker.credit(accountBalance);
        accountBalance = 0;
        state = EventState.CLOSED;
        winningOption = winningOptionName;
    }

    private void payoutLmsrWinners(String winningOptionName) {
        boolean onClose = "on-close".equals(commission.getType());
        Map<String, Integer> holderShares = new LinkedHashMap<>();
        for (Trade trade : tradeHistory) {
            if (trade.optionName().equals(winningOptionName)) {
                holderShares.merge(trade.buyerName(), trade.quantity(), Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> entry : holderShares.entrySet()) {
            double gross = entry.getValue();
            double commissionAmt = onClose ? gross * commission.getCommission() / 100.0 : 0;
            double net = gross - commissionAmt;
            User holder = userDirectory.get(entry.getKey());
            if (holder != null) holder.credit(net);
            accountBalance -= net;
            totalCommissionCollected += commissionAmt;
        }
    }

    private void payoutOrderBookWinners(String winningOptionName) {
        boolean onClose = "on-close".equals(commission.getType());
        int d = getOrderBookConfig().getD();
        for (String userName : orderBook.getParticipantNames()) {
            int shares = orderBook.getHoldings(userName).getOrDefault(winningOptionName, 0);
            if (shares <= 0) continue;
            double gross = shares * (double) d;
            double commissionAmt = onClose ? gross * commission.getCommission() / 100.0 : 0;
            double net = gross - commissionAmt;
            User holder = userDirectory.get(userName);
            if (holder != null) holder.credit(net);
            accountBalance -= net;
            totalCommissionCollected += commissionAmt;
        }
    }
}
