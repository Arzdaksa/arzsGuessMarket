package ui.net;

// a request the server rejected (status is the HTTP code) or that never reached it (status 0)
public class ServerException extends RuntimeException {

    private final int status;

    public ServerException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {return status;}

    public boolean isUnauthorized() {
        return status == 401;
    }

    public boolean isUnreachable() {
        return status == 0;
    }
}
