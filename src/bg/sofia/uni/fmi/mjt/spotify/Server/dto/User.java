package bg.sofia.uni.fmi.mjt.spotify.Server.dto;

import java.util.List;

public class User {

    private List<Playlist> playlists;

    public User(List<Playlist> playlists) {
        this.playlists = playlists;
    }
}
