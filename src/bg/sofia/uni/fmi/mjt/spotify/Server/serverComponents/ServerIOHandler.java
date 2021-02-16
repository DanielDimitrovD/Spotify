package bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents;

import java.nio.channels.Selector;

public class ServerIOHandler {

    private Selector selector;

    public ServerIOHandler(Selector selector) {
        this.selector = selector;
    }



}
