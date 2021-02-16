package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions;

public class SongAccessException extends RuntimeException {
    public SongAccessException(String s, Throwable e) {
        super(s, e);
    }
}
