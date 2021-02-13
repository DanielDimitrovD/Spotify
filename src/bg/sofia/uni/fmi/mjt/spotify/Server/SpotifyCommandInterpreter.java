package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.dto.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.Server.enums.SpotifyCommands;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyClientRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class SpotifyCommandInterpreter {

    private final static String NO_PERMISSION_MESSAGE = "Please login in the system to use this command!";
    private final SpotifyClientRepository spotifyClientRepository;


    private final Path playlistFile;
    private final Type token = new TypeToken<Map<String, Map<String, Playlist>>>() {
    }.getType();
    private final Gson gson = new Gson();
    private Map<String, Map<String, Playlist>> userPlaylistMap;

    public SpotifyCommandInterpreter(Path credentialsFile, Path playlistFile) {
        this.spotifyClientRepository = new SpotifyClientRepository(credentialsFile);
        this.playlistFile = playlistFile;

        initializeUserPlaylist();

        System.out.println("Spotify Command Interpreter constructor : " + playlistFile);
    }

    private void initializeUserPlaylist() {

        System.out.println("Initialize User Playlist method:" + playlistFile);

        try {
            String json = Files.readString(playlistFile);

            if (json.isEmpty()) {
                userPlaylistMap = new HashMap<>();
            } else {
                userPlaylistMap = gson.fromJson(json, token);
            }

//            System.out.println("User Playlist Map: " + userPlaylistMap.toString());

        } catch (IOException e) {
//            //TODO add concrete exception
//            e.printStackTrace();

            try {
                userPlaylistMap = new HashMap<>();
                Files.createFile(playlistFile);

            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public byte[] interpretCommand(String userMessage, SocketChannel userSocketChannel) {

        if (userMessage == null) {
            throw new IllegalArgumentException("userMessage argument in interpretCommand method is null");
        }

        byte[] reply = null;

        String[] tokens = userMessage.split("\\s+");

        String clientCommand = tokens[0];

        Optional<SpotifyCommands> command = Arrays.stream(SpotifyCommands.values())
                .filter(c -> clientCommand.equals(c.getCommand()))
                .findFirst();

        if (!command.isPresent()) {
            return String.format("Unknown command%n").getBytes(StandardCharsets.UTF_8);
        }

        switch (command.get()) {
            case REGISTER -> reply = register(tokens);
            case LOGIN -> reply = login(tokens, userSocketChannel);
            case DISCONNECT -> reply = disconnect(userSocketChannel);
            case SEARCH -> reply = search(tokens, userSocketChannel);
//            case TOP -> reply = top();
            case CREATE_PLAYLIST -> reply = createPlaylist(tokens, userSocketChannel);
            case ADD_SONG_TO -> reply = addSongToPlaylist(tokens, userSocketChannel);
            case SHOW_PLAYLIST -> reply = showPlaylist(tokens, userSocketChannel);
            //           case PLAY_SONG -> reply = playSong(userSocketChannel);
//            case STOP -> reply = stop();
//            default -> reply = String.format("Unknown command%n");
        }

        return reply;
    }

    private byte[] addSongToPlaylist(String[] tokens, SocketChannel userSocketChannel) {

        if (!authenticateUser(userSocketChannel)) {
            return NO_PERMISSION_MESSAGE.getBytes(StandardCharsets.UTF_8);
        }

        final int NAME_OF_PLAYLIST_INDEX = 1;
        final int NAME_OF_SONG_INDEX = 2;

        final int ADD_SONG_TO_PLAYLIST_PARAMETERS = 3;

        if (tokens.length != ADD_SONG_TO_PLAYLIST_PARAMETERS) {
            return "command format : add-song-to <name_of_the_playlist> <song>".getBytes(StandardCharsets.UTF_8);
        }

        String email = spotifyClientRepository.getEmail(userSocketChannel);
        String playlist = tokens[NAME_OF_PLAYLIST_INDEX];
        String song = tokens[NAME_OF_SONG_INDEX];

        return null;
    }

    private byte[] showPlaylist(String[] tokens, SocketChannel userSocketChannel) {

        if (!authenticateUser(userSocketChannel)) {
            return NO_PERMISSION_MESSAGE.getBytes(StandardCharsets.UTF_8);
        }

        final int SHOW_PLAYLIST_PLAYLIST_NAME_INDEX = 1;

        String email = spotifyClientRepository.getEmail(userSocketChannel);

        String playlistName = tokens[SHOW_PLAYLIST_PLAYLIST_NAME_INDEX];

        userPlaylistMap.putIfAbsent(email, new HashMap<>());

        if (!userPlaylistMap.get(email).containsKey(playlistName)) {
            return String.format("Playlist %s does not exist. Please create it first%n", playlistName)
                    .getBytes(StandardCharsets.UTF_8);
        }

        return userPlaylistMap.get(email).get(playlistName).toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] disconnect(SocketChannel userSocketChannel) {
        spotifyClientRepository.disconnect(userSocketChannel);
        return String.format("Account successfully disconnected%n").getBytes(StandardCharsets.UTF_8);
    }

    private byte[] login(String[] tokens, SocketChannel userSocketChannel) {

        final int LOGIN_COMMAND_USERNAME_INDEX = 1;
        final int LOGIN_COMMAND_PASSWORD_INDEX = 2;
        final int LOGIN_COMMAND_PARAMETERS = 3;

        if (tokens.length != LOGIN_COMMAND_PARAMETERS) {
            return String.format("Wrong number of arguments for login command%n").getBytes(StandardCharsets.UTF_8);
        }

        String email = tokens[LOGIN_COMMAND_USERNAME_INDEX];
        String password = tokens[LOGIN_COMMAND_PASSWORD_INDEX];

        boolean success = spotifyClientRepository.login(email, password, userSocketChannel);

        if (success) {
            return String.format("Account with email %s logged in successfully%n", email).getBytes(StandardCharsets.UTF_8);
        } else {
            return String.format("Invalid login attempt from account %s%n", email).getBytes(StandardCharsets.UTF_8);
        }
    }


    private byte[] register(String[] tokens) {

        final int REGISTER_COMMAND_USERNAME_INDEX = 1;
        final int REGISTER_COMMAND_PASSWORD_INDEX = 2;
        final int REGISTER_COMMAND_PARAMETERS = 3;

        if (tokens.length != REGISTER_COMMAND_PARAMETERS) {
            return String.format("Wrong number of arguments for register command%n").getBytes(StandardCharsets.UTF_8);
        }

        String email = tokens[REGISTER_COMMAND_USERNAME_INDEX];
        String password = tokens[REGISTER_COMMAND_PASSWORD_INDEX];

        boolean success = spotifyClientRepository.register(email, password);

        if (success) {
            return String.format("Account with email %s successfully registered%n", email).getBytes(StandardCharsets.UTF_8);
        } else {
            return String.format("Account with email %s already registered. Please try another email%n", email).getBytes(StandardCharsets.UTF_8);
        }
    }


    private String stop() {
        return null;
    }


    private boolean authenticateUser(SocketChannel userChannel) {
        return spotifyClientRepository.isLoggedIn(userChannel);
    }

    private byte[] createPlaylist(String[] tokens, SocketChannel userSocketChannel) {

        if (!authenticateUser(userSocketChannel)) {
            return NO_PERMISSION_MESSAGE.getBytes(StandardCharsets.UTF_8);
        }

        String email = spotifyClientRepository.getEmail(userSocketChannel);
        final int PLAYLIST_COMMAND_NAME_INDEX = 1;
        String playlistName = tokens[PLAYLIST_COMMAND_NAME_INDEX];

        userPlaylistMap.putIfAbsent(email, new HashMap<>());

        if (userPlaylistMap.get(email).containsKey(playlistName)) {
            return "Playlist already exists".getBytes(StandardCharsets.UTF_8);
        } else {

            Playlist newPlaylist = new Playlist(playlistName, new ArrayList<>());

            Map<String, Playlist> userPlaylists = userPlaylistMap.get(email);
            userPlaylists.put(playlistName, newPlaylist);

            userPlaylistMap.put(email, userPlaylists);
        }

        String toJson = gson.toJson(userPlaylistMap, token);

        try {
            Files.writeString(playlistFile, toJson, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return "Playlist successfully created".getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "Playlist not created error".getBytes(StandardCharsets.UTF_8);
        }
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
