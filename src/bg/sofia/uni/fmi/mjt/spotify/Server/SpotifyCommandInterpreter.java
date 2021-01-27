package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.enums.SpotifyCommands;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyClientRepository;

import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class SpotifyCommandInterpreter {

    private final SpotifyClientRepository spotifyClientRepository;

    public SpotifyCommandInterpreter(Path credentialsFile) {
        this.spotifyClientRepository = new SpotifyClientRepository(credentialsFile);
    }

    public String interpretCommand(String userMessage, SocketChannel userSocketChannel) {

        if (userMessage == null) {
            throw new IllegalArgumentException("userMessage argument in interpretCommand method is null");
        }

        String reply;

        String[] tokens = userMessage.split("\\s+");

        String clientCommand = tokens[0];

        Optional<SpotifyCommands> command = Arrays.stream(SpotifyCommands.values())
                .filter(c -> clientCommand.equals(c.getCommand()))
                .findFirst();

        if (!command.isPresent()) {
            return String.format("Unknown command%n");
        }

        switch (command.get()) {
            case REGISTER -> reply = register(tokens);
            case LOGIN -> reply = login(tokens, userSocketChannel);
            case DISCONNECT -> reply = disconnect();
            case SEARCH -> reply = search();
            case TOP -> reply = top();
            case CREATE_PLAYLIST -> reply = createPlaylist();
            case ADD_SONG_TO -> reply = addSongTo();
            case SHOW_PLAYLIST -> reply = showPlaylist();
            case PLAY_SONG -> reply = playSong();
            case STOP -> reply = stop();
            default -> reply = String.format("Unknown command%n");
        }

        return reply;
    }

    private String login(String[] tokens, SocketChannel userSocketChannel) {

        final int LOGIN_COMMAND_PARAMETERS = 3;

        final int LOGIN_COMMAND_USERNAME_INDEX = 1;
        final int LOGIN_COMMAND_PASSWORD_INDEX = 2;

        if (tokens.length != LOGIN_COMMAND_PARAMETERS) {
            return String.format("Wrong number of arguments for login command%n");
        }

        String email = tokens[LOGIN_COMMAND_USERNAME_INDEX];
        String password = tokens[LOGIN_COMMAND_PASSWORD_INDEX];

        boolean success = spotifyClientRepository.login(email, password, userSocketChannel);

        if (success) {
            return String.format("Account with email %s logged in successfully%n", email);
        } else {
            return String.format("Invalid login attempt from account %s%n", email);
        }
    }


    private String register(String[] tokens) {

        final int REGISTER_COMMAND_PARAMETERS = 3;

        final int REGISTER_COMMAND_USERNAME_INDEX = 1;
        final int REGISTER_COMMAND_PASSWORD_INDEX = 2;

        if (tokens.length != REGISTER_COMMAND_PARAMETERS) {
            return String.format("Wrong number of arguments for register command%n");
        }

        String email = tokens[REGISTER_COMMAND_USERNAME_INDEX];
        String password = tokens[REGISTER_COMMAND_PASSWORD_INDEX];

        boolean success = spotifyClientRepository.register(email, password);

        if (success) {
            return String.format("Account with email %s successfully registered%n", email);
        } else {
            return String.format("Account with email %s already registered. Please try another email%n", email);
        }
    }


    private String stop() {
        return null;
    }

    private String playSong() {
        return null;
    }

    private String showPlaylist() {
        return null;
    }

    private String addSongTo() {
        return null;
    }

    private String createPlaylist() {
        return null;
    }

    private String top() {
        return null;
    }

    private String search() {
        return null;
    }

    private String disconnect() {
        return String.format("User successfully disconnected%n");
    }


}
