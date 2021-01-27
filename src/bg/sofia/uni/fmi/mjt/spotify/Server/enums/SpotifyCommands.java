package bg.sofia.uni.fmi.mjt.spotify.Server.enums;

public enum SpotifyCommands {
    REGISTER("register"), LOGIN("login"), DISCONNECT("disconnect"), SEARCH("search"),
    TOP("top"), CREATE_PLAYLIST("create-playlist"), ADD_SONG_TO("add-song-to"), SHOW_PLAYLIST("show-playlist"),
    PLAY_SONG("play"), STOP("stop");

    private String command;

    SpotifyCommands(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
    }
