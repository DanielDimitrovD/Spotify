package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.enums.SpotifyCommands;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyClientRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyPlaylistRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyStreamer;

import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpotifyCommandExecutor {

    private final static String NO_PERMISSION_MESSAGE = "Please login in the system to use this command!";
    private final SpotifyClientRepository spotifyClientRepository;
    private final SpotifyPlaylistRepository spotifyPlaylistRepository;


    public SpotifyCommandExecutor(Path credentialsFile, Path playlistFile) {
        this.spotifyClientRepository = new SpotifyClientRepository(credentialsFile);
        this.spotifyPlaylistRepository = new SpotifyPlaylistRepository(playlistFile);

        System.out.println("Spotify Command Interpreter constructor : " + playlistFile);
    }

    public byte[] interpretCommand(String userMessage, SocketChannel userSocketChannel) {

        if (userMessage == null) {
            throw new IllegalArgumentException("userMessage argument in interpretCommand method is null");
        }

        byte[] reply;

        String[] tokens = userMessage.split("\\s+");

        String clientCommand = tokens[0];

        Optional<SpotifyCommands> command = Arrays.stream(SpotifyCommands.values())
                .filter(c -> clientCommand.equals(c.getCommand()))
                .findFirst();

        if (!command.isPresent()) {
            return String.format("Unknown command%n").getBytes(StandardCharsets.UTF_8);
        }

        String email = spotifyClientRepository.getEmail(userSocketChannel);
        //TODO fix registration

        if (spotifyClientRepository.isLoggedIn(userSocketChannel)) {
            switch (command.get()) {
                case SEARCH -> reply = search(tokens);
                //            case TOP -> reply = top();
                case CREATE_PLAYLIST -> reply = createPlaylist(email, tokens);
                case ADD_SONG_TO -> reply = addSongToPlaylist(email, tokens);
                case SHOW_PLAYLIST -> reply = showPlaylist(email, tokens);
                //           case PLAY_SONG -> reply = playSong(userSocketChannel);
//                case STOP -> reply = stop();
                case DISCONNECT -> reply = disconnect(userSocketChannel);
                default -> reply = String.format("Unknown command.%n").getBytes(StandardCharsets.UTF_8);
            }
        } else {
            switch (command.get()) {
                case REGISTER -> reply = register(tokens);
                case LOGIN -> reply = login(tokens, userSocketChannel);
                case DISCONNECT -> reply = disconnect(userSocketChannel);
                default -> reply = String.format("Unknown command%n").getBytes(StandardCharsets.UTF_8);
            }
        }

        return reply;
    }

    private byte[] search(String[] tokens) {

        String[] t = new String[tokens.length - 1];
        int j = 0;
        for (int i = 1; i < tokens.length; i++) {
            t[j++] = tokens[i];
        }

        List<String> searchSongs = SpotifyStreamer.searchSongs((t));

        System.out.printf("Matched songs: %s", searchSongs.toString());

        if (searchSongs.isEmpty()) {
            return String.format("No songs containing %s found.%n", tokens.toString()).getBytes(StandardCharsets.UTF_8);
        }

        return searchSongs.stream()
                .collect(Collectors.joining(System.lineSeparator()))
                .getBytes(StandardCharsets.UTF_8);
    }

    private byte[] addSongToPlaylist(String email, String[] tokens) {
        return spotifyPlaylistRepository.addSongToPlaylist(email, tokens);
    }

    private byte[] showPlaylist(String email, String[] tokens) {
        return spotifyPlaylistRepository.showPlaylist(email, tokens);
    }

    private byte[] disconnect(SocketChannel userSocketChannel) {
        spotifyClientRepository.disconnect(userSocketChannel);
        return String.format("Account successfully disconnected%n").getBytes(StandardCharsets.UTF_8);
    }

    private byte[] login(String[] tokens, SocketChannel userSocketChannel) {
        return spotifyClientRepository.login(tokens, userSocketChannel);
    }


    private byte[] register(String[] tokens) {
        return spotifyClientRepository.register(tokens);
    }

//
//    private byte[] stop() {
//        return ;
//    }


    private boolean authenticateUser(SocketChannel userChannel) {
        return spotifyClientRepository.isLoggedIn(userChannel);
    }

    private byte[] createPlaylist(String email, String[] tokens) {
        return spotifyPlaylistRepository.createPlaylist(email, tokens);
    }

    private String top() {
        return null;
    }

}
