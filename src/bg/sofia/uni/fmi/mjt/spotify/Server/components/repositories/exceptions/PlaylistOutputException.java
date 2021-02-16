package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions;

public class PlaylistOutputException extends RuntimeException {
    public PlaylistOutputException(String s, Throwable e) {
        super(s, e);
    }
}
