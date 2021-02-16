package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyStreamer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;

@RunWith(MockitoJUnitRunner.class)
public class SpotifyServerTest {

    private static final int PORT_NUMBER = 7777;
    @Mock
    private static SpotifyStreamer spotifyStreamer;
    @Mock
    private static SpotifyCommandExecutor spotifyCommandExecutor;
    @Mock
    private static Selector selector;

    private static SpotifyServer spotifyServer;

    @BeforeClass
    public static void setUp() throws IOException {

        selector = Selector.open();

        spotifyServer = new SpotifyServer(PORT_NUMBER, selector, spotifyStreamer, spotifyCommandExecutor);
        spotifyServer.start();
    }

    @AfterClass
    public static void tearDown() {
        spotifyServer.stop();
    }

    @Test
    public void testInvalidLogin() throws IOException {

        Mockito.when(spotifyCommandExecutor.interpretCommand("login test asdf", ArgumentMatchers.any()))
                .thenReturn("asdf".getBytes(StandardCharsets.UTF_8));

        System.out.println("Losho mi e");


    }
}