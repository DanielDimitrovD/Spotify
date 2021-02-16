package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions;

public class PlaylistInitializationException extends RuntimeException{
    public PlaylistInitializationException(String s, Throwable e) {
        super(s, e);
    }
}
