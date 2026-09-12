package engine;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@XmlAccessorType(XmlAccessType.FIELD)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlAttribute
    private String name;

    @XmlElement(name = "initial-cash")
    private int initialCash;

    @XmlElement(name = "GM-market-maker")
    private MarketMakerAssignment marketMakerAssignment;

    @XmlTransient private double balance;
    @XmlTransient private boolean locked;
    @XmlTransient private final Set<Integer> marketMakerEventIds = new HashSet<>();
    @XmlTransient private final Set<Integer> participatedEventIds = new HashSet<>();

    public String getName() {return name;}
    public int getInitialCash() {return initialCash;}
    public double getBalance() {return balance;}
    public boolean isLocked() {return locked;}

    public List<EventRef> getMarketMakerEventRefs() {
        return marketMakerAssignment == null ? List.of() : marketMakerAssignment.getEvents();
    }

    public Set<Integer> getParticipatedEventIds() {
        return Collections.unmodifiableSet(participatedEventIds);
    }

    // called once by the engine after successful load-validation
    void initializeBalance() {
        this.balance = initialCash;
        this.locked = false;
        marketMakerEventIds.clear();
        for (EventRef ref : getMarketMakerEventRefs()) {
            marketMakerEventIds.add(ref.getId());
        }
    }

    public boolean isMarketMakerOf(int eventId) {
        return marketMakerEventIds.contains(eventId);
    }

    void markParticipated(int eventId) {
        participatedEventIds.add(eventId);
    }

    // blocks and permanently locks the user if the debit would drive balance negative
    public void debit(double amount) {
        if (locked) {
            throw new IllegalStateException("User '" + name + "' is locked and cannot perform further actions.");
        }
        if (balance - amount < 0) {
            locked = true;
            throw new InsufficientFundsException(
                    "Action would leave user '" + name + "' with a negative balance; blocked and account locked.");
        }
        balance -= amount;
    }

    public void credit(double amount) {
        balance += amount;
    }

    public User() {}
}
