package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions;

public class SongRepositoryAccessException extends RuntimeException {
    public SongRepositoryAccessException(String s, Throwable e) {
        super(s, e);
    }
}
