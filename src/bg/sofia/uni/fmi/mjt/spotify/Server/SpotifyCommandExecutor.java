package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.enums.SpotifyCommands;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.repositories.SpotifyClientRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.repositories.SpotifyPlaylistRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.repositories.SpotifySongRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.repositories.SpotifyStatistics;

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

        if (spotifyClientRepository.isLoggedIn(userSocketChannel)) {
            switch (command.get()) {
                case SEARCH -> reply = search(tokens);
                case TOP -> reply = top(tokens);
                case CREATE_PLAYLIST -> reply = createPlaylist(email, tokens);
                case ADD_SONG_TO -> reply = addSongToPlaylist(email, tokens);
                case SHOW_PLAYLIST -> reply = showPlaylist(email, tokens);
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

        List<String> searchSongs = SpotifySongRepository.searchSongs((t));

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

    private byte[] createPlaylist(String email, String[] tokens) {
        return spotifyPlaylistRepository.createPlaylist(email, tokens);
    }

    private byte[] top(String[] tokens) {

        final int TOP_N_COMMAND_PARAMETER_INDEX = 1;
        final int TOP_N_COMMAND_NUMBER_OF_PARAMETERS = 2;

        if (tokens.length != TOP_N_COMMAND_NUMBER_OF_PARAMETERS) {
            return String.format("Wrong number of parameters. Must be top <n*> where " +
                                  "n is a non-negative number%n")
                    .getBytes(StandardCharsets.UTF_8);
        }

        int number;

        try {
            number = Integer.parseInt(tokens[TOP_N_COMMAND_PARAMETER_INDEX]);
        } catch (NumberFormatException e) {
            return String.format("Wrong command format. Must be top <n*> where n is a non-negative number%n")
                    .getBytes(StandardCharsets.UTF_8);
        }

        List<String> topSongs = SpotifyStatistics.getNMostPopularSongs(number)
                .stream()
                .collect(Collectors.toList());

        if (topSongs.isEmpty()) {
            return String.format("No songs played in the system%n").getBytes(StandardCharsets.UTF_8);
        }

        return topSongs.stream()
                .collect(Collectors.joining(System.lineSeparator()))
                .getBytes(StandardCharsets.UTF_8);
    }

}
