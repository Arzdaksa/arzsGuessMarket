package engine;

public class InsufficientFundsException extends RuntimeException {
    // thrown when an action would drive a user's balance negative; the user is locked as a side effect
    public InsufficientFundsException(String message) {
        super(message);
    }
}
