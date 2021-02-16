package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions;

import java.io.IOException;

public class ClientRepositoryOutputException extends RuntimeException {
    public ClientRepositoryOutputException(String s, IOException e) {
        super(s, e);
    }
}
