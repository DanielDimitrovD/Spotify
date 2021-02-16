package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions;

import java.io.IOException;

public class ClientRepositoryInitializationException extends RuntimeException {
    public ClientRepositoryInitializationException(String s, IOException e) {
        super(s, e);
    }
}
