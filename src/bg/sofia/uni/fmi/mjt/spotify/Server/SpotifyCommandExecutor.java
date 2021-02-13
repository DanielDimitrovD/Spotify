package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.enums.SpotifyCommands;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyClientRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyPlaylistRepository;

import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

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
                case SEARCH -> reply = search(tokens, userSocketChannel);
                //            case TOP -> reply = top();
                case CREATE_PLAYLIST -> reply = createPlaylist(email, tokens);
                case ADD_SONG_TO -> reply = addSongToPlaylist(tokens, userSocketChannel);
                case SHOW_PLAYLIST -> reply = showPlaylist(email, tokens);
                //           case PLAY_SONG -> reply = playSong(userSocketChannel);
                //            case STOP -> reply = stop();
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

    private byte[] addSongToPlaylist(String[] tokens, SocketChannel userSocketChannel) {

        return null;
//        if (!authenticateUser(userSocketChannel)) {
//            return NO_PERMISSION_MESSAGE.getBytes(StandardCharsets.UTF_8);
//        }
//
//
//        final int NAME_OF_PLAYLIST_INDEX = 1;
//        final int NAME_OF_SONG_INDEX = 2;
//
//        final int ADD_SONG_TO_PLAYLIST_PARAMETERS = 3;
//
//        if (tokens.length != ADD_SONG_TO_PLAYLIST_PARAMETERS) {
//            return "command format : add-song-to <name_of_the_playlist> <song>".getBytes(StandardCharsets.UTF_8);
//        }
//
//        String email = spotifyClientRepository.getEmail(userSocketChannel);
//        String playlistName = tokens[NAME_OF_PLAYLIST_INDEX];
//        String song = tokens[NAME_OF_SONG_INDEX];
//
//        userPlaylistMap.putIfAbsent(email, new HashMap<>());
//
//        if (!userPlaylistMap.get(email).containsKey(playlistName)) {
//            return String.format("playlist %s does not exists. Please create %s first then add songs to it.",
//                    playlistName).getBytes(StandardCharsets.UTF_8);
//        }
//
//        Map<String, Playlist> userPlaylists = userPlaylistMap.get(email);
//
//        Playlist targetPlaylist = userPlaylists.get(playlistName);
//        targetPlaylist.addSong(song);
//
//        userPlaylists.put(playlistName, targetPlaylist);
//        userPlaylistMap.put(email, userPlaylists);
//
//        //TODO need to add validation for songs in Spotify
//
//        return String.format("Song %s added successfully to playlist %s%n", song, playlistName)
//                .getBytes(StandardCharsets.UTF_8);
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


    private String stop() {
        return null;
    }


    private boolean authenticateUser(SocketChannel userChannel) {
        return spotifyClientRepository.isLoggedIn(userChannel);
    }

    private byte[] createPlaylist(String email, String[] tokens) {
        return spotifyPlaylistRepository.createPlaylist(email, tokens);
    }

    private String top() {
        return null;
    }

    private byte[] search(String[] tokens, SocketChannel userSocketChannel) {

        if (!authenticateUser(userSocketChannel)) {
            return NO_PERMISSION_MESSAGE.getBytes(StandardCharsets.UTF_8);
        }


        return null;
    }


}
