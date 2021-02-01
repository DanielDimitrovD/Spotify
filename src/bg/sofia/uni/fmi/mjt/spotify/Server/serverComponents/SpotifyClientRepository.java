package bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SpotifyClientRepository {

    private final Map<String, String> clientCredentials = new HashMap<>();
    private final Map<SocketChannel, String> loggedUsers = new HashMap<>();
    private final Gson gson = new Gson();

    private final Path credentialsFile;


    private final Type mapType = new TypeToken<Map<String, String>>() {
    }.getType();

    public SpotifyClientRepository(Path credentialsFile) {
        this.credentialsFile = credentialsFile;
        setUpClientCredentials();
    }

    public static void main(String[] args) {
        SpotifyClientRepository spotifyClientRepository = new SpotifyClientRepository(Path.of("credentials.json"));
    }

    private void setUpClientCredentials() {
        try {
            String jsonString = Files.readString(credentialsFile);

            Map<String, String> users = gson.fromJson(jsonString, mapType);

            if (users != null) {
                clientCredentials.putAll(users);
            }

            System.out.println(users.toString());
        } catch (IOException e) {
            //TODO add exception
            throw new UnsupportedOperationException();
        }
    }

    private boolean validateArguments(String... arguments) {
        return Arrays.stream(arguments).noneMatch(e -> e == null);
    }

    public boolean login(String email, String password, SocketChannel userChannel) {

        if (!validateArguments(email, password)) {
            throw new IllegalArgumentException("parameter in method login is null");
        }

        if (!clientCredentials.containsKey(email)) {
            return false;
        }

        if (clientCredentials.get(email).equals(password)) {
            loggedUsers.put(userChannel, email);
            return true;
        }

        return false;
    }

    public void disconnect(SocketChannel userChannel) {
        loggedUsers.remove(userChannel);
    }

    private void writeCredentialsToJson() {
        String toJson = gson.toJson(clientCredentials, mapType);

        try {
            Files.writeString(credentialsFile, toJson, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UnsupportedOperationException();
        }
    }

    public boolean register(String email, String password) {

        if (!validateArguments(email, password)) {
            throw new IllegalArgumentException("parameter in method register is null");
        }

        if (clientCredentials.containsKey(email)) {
            return false;
        }

        clientCredentials.put(email, password);

        writeCredentialsToJson();

        return true;
    }

    public String getEmail(SocketChannel userChannel) {
        return loggedUsers.get(userChannel);
    }

}
