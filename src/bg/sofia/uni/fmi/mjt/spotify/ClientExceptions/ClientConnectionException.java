package bg.sofia.uni.fmi.mjt.spotify.ClientExceptions;

public class ClientConnectionException extends RuntimeException {

    private String message;
    private Throwable cause;

    public ClientConnectionException(String s, Throwable cause) {
        this.message = s;
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }
}
