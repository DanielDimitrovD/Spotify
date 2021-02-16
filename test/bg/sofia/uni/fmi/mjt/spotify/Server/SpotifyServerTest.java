package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyStreamer;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SpotifyServerTest {

    private static final int PORT_NUMBER = 7777;
    private static final String musicFolderURL = "D:\\4-course\\songs\\";

    private static final Path credentialsURL = Path.of("credentials.json");
    private static final Path playlistURL = Path.of("playlists.json");

    private static SpotifyStreamer spotifyStreamer = new SpotifyStreamer(musicFolderURL);
    private static SpotifyCommandExecutor spotifyCommandExecutor =
            new SpotifyCommandExecutor(credentialsURL, playlistURL);
    private static Thread serverStarterThread;
    private static SpotifyServer server;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        serverStarterThread = new Thread(() -> {
            try (SpotifyServer spotifyServer = new SpotifyServer(PORT_NUMBER, spotifyStreamer, spotifyCommandExecutor)) {
                server = spotifyServer;
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        serverStarterThread.start();
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
        serverStarterThread.interrupt();
    }

    @Test
    public void testInvalidLogin() throws IOException {
        List<String> command = List.of("login test 1235asdf");
        String email = "test";
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Wrong password for %s%n", email);
        Assert.assertEquals("Response must be wrong password", expected, reply.get(0));
    }

    @Test
    public void testWrongNumberOfParametersForLogin() throws IOException {
        List<String> command = List.of("login test 1235asdf a s d f");
        String email = "test";
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Wrong number of arguments for login command%n");
        Assert.assertEquals("Response must be wrong format of login command", expected, reply.get(0));
    }


    private List<String> getListOfReplies(List<String> commands) {

        List<String> replies = new ArrayList<>();

        try (Socket socket = new Socket(SpotifyServer.SERVER_HOST, SpotifyServer.SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            System.out.println("Client " + socket + " connected to server");

            for (String command : commands) {

                out.println(command);
                String reply = String.format("%s%n", in.readLine());

                replies.add(reply);
            }

            return replies;

        } catch (IOException e) {
            System.out.println("An error has occurred " + e.getMessage());
            e.printStackTrace();
        }


        return null;
    }

}