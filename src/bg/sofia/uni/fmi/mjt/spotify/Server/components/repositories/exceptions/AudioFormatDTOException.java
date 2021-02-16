package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions;

public class AudioFormatDTOException extends RuntimeException {
    public AudioFormatDTOException(String msg, Exception e) {
        super(msg);
    }
}
