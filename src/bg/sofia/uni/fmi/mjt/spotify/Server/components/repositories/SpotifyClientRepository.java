package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories;

import bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions.ClientRepositoryInitializationException;
import bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions.ClientRepositoryOutputException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SpotifyClientRepository {

    private static final Map<SocketChannel, String> loggedUsers = new HashMap<>();
    private final Map<String, String> clientCredentials = new HashMap<>();
    private final Gson gson = new Gson();

    private final Path credentialsFile;
    private final Type mapType = new TypeToken<Map<String, String>>() {
    }.getType();

    public SpotifyClientRepository(Path credentialsFile) {
        this.credentialsFile = credentialsFile;
        setUpClientCredentials();
    }

    public static String getEmail(SocketChannel userChannel) {
        return loggedUsers.get(userChannel);
    }

    private void setUpClientCredentials() {
        try {
            String jsonString = Files.readString(credentialsFile);

            Map<String, String> users = gson.fromJson(jsonString, mapType);

            if (users != null) {
                clientCredentials.putAll(users);
            }

        } catch (IOException e) {
            throw new ClientRepositoryInitializationException("I/O error when trying to initialize client repository", e);
        }
    }

    private boolean validateArguments(String... arguments) {
        return Arrays.stream(arguments).noneMatch(e -> e == null);
    }

    public byte[] login(String[] tokens, SocketChannel userChannel) {

        final int LOGIN_COMMAND_USERNAME_INDEX = 1;
        final int LOGIN_COMMAND_PASSWORD_INDEX = 2;
        final int LOGIN_COMMAND_PARAMETERS = 3;

        if (tokens.length != LOGIN_COMMAND_PARAMETERS) {
            return String.format("Wrong number of arguments for login command%n" +
                                 "login <email> <password>%n").getBytes(StandardCharsets.UTF_8);
        }

        if (!validateArguments(tokens)) {
            return String.format("Parameter/s in method login is/are null%n").getBytes(StandardCharsets.UTF_8);
        }

        String email = tokens[LOGIN_COMMAND_USERNAME_INDEX];
        String password = tokens[LOGIN_COMMAND_PASSWORD_INDEX];


        if (!clientCredentials.containsKey(email)) {
            return String.format("Username %s does not exist.%n", email).getBytes(StandardCharsets.UTF_8);
        }

        if (!clientCredentials.get(email).equals(password)) {
            return String.format("Wrong password for %s%n", email).getBytes(StandardCharsets.UTF_8);
        }

        loggedUsers.put(userChannel, email);

        return String.format("%s logged in successfully%n", email).getBytes(StandardCharsets.UTF_8);
    }

    public void disconnect(SocketChannel userChannel) {
        loggedUsers.remove(userChannel);
    }

    private void writeCredentialsToJson() {
        String toJson = gson.toJson(clientCredentials, mapType);

        try {
            Files.writeString(credentialsFile, toJson, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new ClientRepositoryOutputException("Error with writing to client credentials file", e);
        }
    }

    public byte[] register(String[] tokens) {

        final int REGISTER_COMMAND_USERNAME_INDEX = 1;
        final int REGISTER_COMMAND_PASSWORD_INDEX = 2;
        final int REGISTER_COMMAND_PARAMETERS = 3;

        if (!validateArguments(tokens)) {
            throw new IllegalArgumentException("parameter in method register is null");
        }

        if (tokens.length != REGISTER_COMMAND_PARAMETERS) {
            return String.format("Wrong number of arguments for register command%nregister <email> <password>%n")
                    .getBytes(StandardCharsets.UTF_8);
        }

        String email = tokens[REGISTER_COMMAND_USERNAME_INDEX];
        String password = tokens[REGISTER_COMMAND_PASSWORD_INDEX];

        if (clientCredentials.containsKey(email)) {
            return String.format("Account with email %s already registered. Please try another email.%n", email)
                    .getBytes(StandardCharsets.UTF_8);
        }

        clientCredentials.put(email, password);
        writeCredentialsToJson();

        return String.format("Account with email %s successfully registered.%n", email)
                .getBytes(StandardCharsets.UTF_8);
    }

    public boolean isLoggedIn(SocketChannel userChannel) {
        return loggedUsers.containsKey(userChannel);
    }
}
