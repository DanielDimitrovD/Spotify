package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

    public byte[] addSongToPlaylist(String email, String[] tokens) {

        final int NAME_OF_PLAYLIST_INDEX = 1;
        final int NAME_OF_SONG_INDEX_START = 2;

        final int ADD_SONG_TO_PLAYLIST_PARAMETERS = 3;

        if (tokens.length < ADD_SONG_TO_PLAYLIST_PARAMETERS) {
            return "command format : add-song-to <name_of_the_playlist> <song>".getBytes(StandardCharsets.UTF_8);
        }

        if (!validateArguments(tokens)) {
            throw new IllegalArgumentException("parameter in method login is null");
        }

        String playlistName = tokens[NAME_OF_PLAYLIST_INDEX];
        String song = Arrays.stream(tokens).skip(NAME_OF_SONG_INDEX_START).collect(Collectors.joining(" "));

        userPlaylistMap.putIfAbsent(email, new HashMap<>());

        if (!userPlaylistMap.get(email).containsKey(playlistName)) {
            return String.format("playlist %s does not exists. Please create %s first then add songs.%n",
                    playlistName, playlistName).getBytes(StandardCharsets.UTF_8);
        }

        if (userPlaylistMap.get(email).get(playlistName).getPlaylistSongs()
                .stream().anyMatch(p -> p.equals(song))) {
            return String.format("Playlist %s already has song %s.%n", playlistName, song)
                    .getBytes(StandardCharsets.UTF_8);
        }

        if (!SpotifySongRepository.containsSong(song)) {
            return String.format("Song %s does not exist.%n", song)
                    .getBytes(StandardCharsets.UTF_8);
        }


        Map<String, Playlist> userPlaylists = userPlaylistMap.get(email);

        Playlist targetPlaylist = userPlaylists.get(playlistName);
        targetPlaylist.addSong(song);

        userPlaylists.put(playlistName, targetPlaylist);
        userPlaylistMap.put(email, userPlaylists);

        writeToPlaylist();

        return String.format("Song %s added successfully to playlist %s%n", song, playlistName)
                .getBytes(StandardCharsets.UTF_8);
    }

    private void writeToPlaylist() {
        String toJson = gson.toJson(userPlaylistMap, token);
        try {
            Files.writeString(playlistFile, toJson, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public byte[] createPlaylist(String email, String[] tokens) {

        final int PLAYLIST_COMMAND_NAME_INDEX = 1;
        String playlistName = tokens[PLAYLIST_COMMAND_NAME_INDEX];

        userPlaylistMap.putIfAbsent(email, new HashMap<>());

        if (userPlaylistMap.get(email).containsKey(playlistName)) {
            return String.format("Playlist already exists%n").getBytes(StandardCharsets.UTF_8);
        } else {

            Playlist newPlaylist = new Playlist(playlistName, new ArrayList<>());

            Map<String, Playlist> userPlaylists = userPlaylistMap.get(email);
            userPlaylists.put(playlistName, newPlaylist);

            userPlaylistMap.put(email, userPlaylists);
        }

        String toJson = gson.toJson(userPlaylistMap, token);

        try {
            Files.writeString(playlistFile, toJson, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return String.format("Playlist successfully created%n").getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return String.format("Playlist not created error%n").getBytes(StandardCharsets.UTF_8);
        }
    }


    public byte[] showPlaylist(String email, String[] tokens) {
        final int SHOW_PLAYLIST_PLAYLIST_NAME_INDEX = 1;
        final int SHOW_PLAYLIST_PARAMETERS = 2;

        if (tokens.length != SHOW_PLAYLIST_PARAMETERS) {
            final String errorMessage = String.format(" Show-playlist command accepts one parameter:" +
                                                      "show-playlist <name>%n");
            return errorMessage.getBytes(StandardCharsets.UTF_8);
        }

        String playlistName = tokens[SHOW_PLAYLIST_PLAYLIST_NAME_INDEX];

        userPlaylistMap.putIfAbsent(email, new HashMap<>());

        if (!userPlaylistMap.get(email).containsKey(playlistName)) {
            return String.format("Playlist %s does not exist. Please create it first%n", playlistName)
                    .getBytes(StandardCharsets.UTF_8);
        }

        return userPlaylistMap.get(email).get(playlistName).toString().getBytes(StandardCharsets.UTF_8);
    }

    private boolean validateArguments(String... arguments) {
        return Arrays.stream(arguments).noneMatch(e -> e == null);
    }
}
