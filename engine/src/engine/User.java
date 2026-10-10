package engine;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private double balance;
    private boolean locked;
    private final Set<Integer> participatedEventIds = new HashSet<>();

    // users are created by the engine on first login and start with an empty balance
    User(String name) {
        this.name = name;
    }

    public String getName() {return name;}
    public double getBalance() {return balance;}
    public boolean isLocked() {return locked;}

    public Set<Integer> getParticipatedEventIds() {
        return Collections.unmodifiableSet(participatedEventIds);
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
}
