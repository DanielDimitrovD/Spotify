package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyClientRepository;

import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class SpotifyCommandInterpreter {

    private final SpotifyClientRepository spotifyClientRepository;

    public SpotifyCommandInterpreter(Path credentialsFile) {
        this.spotifyClientRepository = new SpotifyClientRepository(credentialsFile);
    }

    public String interpretCommand(String userMessage, SocketChannel userSocketChannel) {
        return null;
    }
}
