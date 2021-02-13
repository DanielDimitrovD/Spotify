package bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents;

import bg.sofia.uni.fmi.mjt.spotify.Server.dto.Playlist;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SpotifyPlaylistRepository {

    private final Path playlistFile;
    private final Type token = new TypeToken<Map<String, Map<String, Playlist>>>() {
    }.getType();
    private final Gson gson = new Gson();
    private Map<String, Map<String, Playlist>> userPlaylistMap;

    public SpotifyPlaylistRepository(Path playlistFile) {
        this.playlistFile = playlistFile;
        initializeUserPlaylist();
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

        } catch (IOException e) {
            //TODO add concrete exception
            try {
                userPlaylistMap = new HashMap<>();
                Files.createFile(playlistFile);

            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public byte[] createPlaylist(String email, String[] tokens) {

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


    public byte[] showPlaylist(String email, String[] tokens) {
        final int SHOW_PLAYLIST_PLAYLIST_NAME_INDEX = 1;

        String playlistName = tokens[SHOW_PLAYLIST_PLAYLIST_NAME_INDEX];

        userPlaylistMap.putIfAbsent(email, new HashMap<>());

        if (!userPlaylistMap.get(email).containsKey(playlistName)) {
            return String.format("Playlist %s does not exist. Please create it first%n", playlistName)
                    .getBytes(StandardCharsets.UTF_8);
        }

        return userPlaylistMap.get(email).get(playlistName).toString().getBytes(StandardCharsets.UTF_8);
    }
}
