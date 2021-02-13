package bg.sofia.uni.fmi.mjt.spotify.Server.dto;

import java.util.List;

public class Playlist {

    private String playlistName;

    private List<String> playlistSongs;

    public Playlist(String name, List<String> playlistSongs) {
        this.playlistName = name;
        this.playlistSongs = playlistSongs;
    }

    @Override
    public String toString() {
        return "Playlist{" +
               "playlistName='" + playlistName + '\'' +
               ", playlistSongs=" + playlistSongs +
               '}';
    }
}
