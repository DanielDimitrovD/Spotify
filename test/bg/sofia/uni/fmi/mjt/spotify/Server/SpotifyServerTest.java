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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SpotifyServerTest {

    private static final int PORT_NUMBER = 7777;
    private static final String musicFolderURL = "D:\\4-course\\songs\\";

    private static Path credentialsURL;
    private static Path playlistURL;

    private static SpotifyStreamer spotifyStreamer = new SpotifyStreamer(musicFolderURL);
    private static SpotifyCommandExecutor spotifyCommandExecutor;
    private static Thread serverStarterThread;
    private static SpotifyServer server;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {

        Files.createFile(Path.of("test_credentials.json"));
        Files.createFile(Path.of("test_playlist.json"));

        credentialsURL = Path.of("test_credentials.json");
        playlistURL = Path.of("test_playlist.json");

        spotifyCommandExecutor = new SpotifyCommandExecutor(credentialsURL, playlistURL);

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
    public static void tearDown() throws IOException {
        server.stop();
        serverStarterThread.interrupt();

        Files.delete(Path.of("test_credentials.json"));
        Files.delete(Path.of("test_playlist.json"));
    }

    @Test
    public void testInvalidLogin() throws IOException {
        List<String> command = List.of("login test 1235asdf");
        String email = "test";
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Username test does not exist.%n", email);
        Assert.assertEquals("Response must be username does not exist", expected, reply.get(0));
    }

    @Test
    public void testWrongNumberOfParametersForLogin() throws IOException {
        List<String> command = List.of("login test 1235asdf a s d f");
        String email = "test";
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Wrong number of arguments for login command%n");
        Assert.assertEquals("Response must be wrong format of login command", expected, reply.get(0));
    }

    @Test
    public void testRegisterSuccessfullyIntoSystem() throws IOException {
        List<String> command = List.of("register asi 1235asdf");
        String email = "test";
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Account with email asi successfully registered.%n");
        Assert.assertEquals("Response must be account successfully registered", expected, reply.get(0));
    }

    @Test
    public void testLoginIntoSystem() {
        List<String> command = List.of("register borko 1235", "login borko 1235");
        String email = "borko";
        List<String> reply = getListOfReplies(command);
        String expected = String.format("borko logged in successfully%n");
        Assert.assertEquals("Response must be borko logged in into Spotify", expected, reply.get(1));
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