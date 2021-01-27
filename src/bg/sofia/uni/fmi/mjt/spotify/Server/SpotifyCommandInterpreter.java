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

        switch (command.get()) {
            case REGISTER -> reply = register();
            case LOGIN -> reply = login();
            case DISCONNECT -> reply = disconnect();
            case SEARCH -> reply = search();
            case TOP -> reply = top();
            case CREATE_PLAYLIST -> reply = createPlaylist();
            case ADD_SONG_TO -> reply = addSongTo();
            case SHOW_PLAYLIST -> reply = showPlaylist();
            case PLAY_SONG -> reply = playSong();
            case STOP -> reply = stop();
            default -> throw new IllegalStateException("Unexpected value: " + command.get());
        }

        return reply;
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
        return null;
    }

    private String login() {
        return null;
    }

    private String register() {
        return null;
    }
}
