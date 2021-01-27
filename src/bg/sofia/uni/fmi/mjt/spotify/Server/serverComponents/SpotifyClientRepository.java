package bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SpotifyClientRepository {

    private final Map<String, String> clientCredentials = new HashMap<>();

    public SpotifyClientRepository(Path credentialsFile) {
        setUpClientCredentials(credentialsFile);
    }


    // TODO read credentials from a file (add persistence)
    private void setUpClientCredentials(Path credentialsFile) {

    }

    private boolean validateArguments(String... arguments) {
        return Arrays.stream(arguments)
                       .filter(e -> e == null)
                       .count() == 0 ? true : false;
    }

    public boolean login(String email, String password) {

        if (!validateArguments(email, password)) {
            throw new IllegalArgumentException("parameter in method login is null");
        }

        if (!clientCredentials.containsKey(email)) {
            return false;
        }

        if (clientCredentials.get(email).equals(password)) {
            return true;
        }

        return false;
    }

    public boolean register(String email, String password) {

        if (!validateArguments(email, password)) {
            throw new IllegalArgumentException("parameter in method register is null");
        }

        if (clientCredentials.containsKey(email)) {
            return false;
        }

        clientCredentials.put(email, password);

        return true;
    }
}
