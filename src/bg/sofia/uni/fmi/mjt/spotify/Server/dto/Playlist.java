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
        return String.format("Playlist name: %s %nSongs: %s %n", playlistName, playlistSongs);
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public List<String> getPlaylistSongs() {
        return playlistSongs;
    }

    public void addSong(String song) {
        playlistSongs.add(song);
    }

}
