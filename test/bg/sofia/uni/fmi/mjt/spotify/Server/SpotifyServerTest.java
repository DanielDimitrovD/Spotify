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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static bg.sofia.uni.fmi.mjt.spotify.Server.SpotifyServer.SERVER_HOST;
import static bg.sofia.uni.fmi.mjt.spotify.Server.SpotifyServer.SERVER_PORT;

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

        Files.deleteIfExists(Path.of("test_credentials.json"));
        Files.deleteIfExists(Path.of("test_playlist.json"));

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
    public void testLoginIntoSystem() throws IOException {
        List<String> command = List.of("register borko 1235", "login borko 1235");
        String email = "borko";
        List<String> reply = getListOfReplies(command);
        String expected = String.format("borko logged in successfully%n");
        Assert.assertEquals("Response must be borko logged in into Spotify", expected, reply.get(1));
    }

    @Test
    public void testDisconnectFromSystem() throws IOException {
        List<String> command = List.of("disconnect");
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Account successfully disconnected%n");
        Assert.assertEquals("Response must be successfull disconnection from server",
                expected, reply.get(0));
    }

    @Test
    public void testsSarchSongInSpotify() throws IOException {
        List<String> command = List.of("register ceco 1234", "login ceco 1234", "search Papi");
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Lariss - Dale Papi%n");

        Assert.assertEquals("Searched songs are not correct",
                expected, reply.get(2));
    }

    @Test
    public void testGetTopNSongsWrongCommandFormat() throws IOException {
        List<String> command = List.of("register vesko 1234", "login vesko 1234", "top asdf");
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Wrong command format. Must be top <n*> where n is a non-negative number" +
                                        "%n");

        Assert.assertEquals("Top command help section did not show up",
                expected, reply.get(2));
    }

    @Test
    public void testGetTopNSongsNoSongsInSystem() throws IOException {
        List<String> command = List.of("register desko 1234", "login desko 1234", "top 5");
        List<String> reply = getListOfReplies(command);
        String expected = String.format("No songs played in the system%n");

        Assert.assertEquals("There should be no songs in the system", expected, reply.get(2));
    }

    @Test
    public void testCreatePlaylist() throws IOException {
        List<String> command = List.of("register desi 1234", "login desi 1234", "create-playlist FMI");
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Playlist successfully created%n");

        Assert.assertEquals("Playlist should be successfully created",
                expected, reply.get(2));
    }

    @Test
    public void testCreatePlaylistAlreadyExists() throws IOException {
        List<String> command = List.of("register pesho 1234", "login pesho 1234", "create-playlist FMI",
                "create-playlist FMI");
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Playlist already exists%n");

        Assert.assertEquals("There must be an existing playlist already",
                expected, reply.get(3));
    }

    @Test
    public void testAddSongToPlaylist() throws IOException {
        List<String> command = List.of("register gesho 1234", "login gesho 1234", "create-playlist FMI",
                "add-song-to FMI Papi Hans - Hubavo mi Stava");
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Song Papi Hans - Hubavo mi Stava added successfully to playlist FMI%n");

        Assert.assertEquals("There must be an existing playlist already",
                expected, reply.get(3));
    }

    @Test
    public void testShowPlaylist() throws IOException {
        List<String> command = List.of("register mesho 1234", "login mesho 1234", "create-playlist FMI",
                "add-song-to FMI Papi Hans - Hubavo mi Stava", "show-playlist FMI");
        List<String> reply = getListOfReplies(command);
        String expected = String.format("Playlist name: FMI %n");

        Assert.assertEquals("There must be an existing playlist already",
                expected, reply.get(4));
    }

    @Test
    public void testPlaySongReturnsAudioFormatHeader() {

        List<String> commands = List.of("register tesho 1234", "login tesho 1234",
                "play tesho Papi Hans - Hubavo mi stava");

        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            ByteBuffer buffer = ByteBuffer.allocateDirect(1_024);

            for (int i = 0; i < commands.size(); i++) {

                buffer.clear();
                buffer.put(commands.get(i).getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                socketChannel.write(buffer);

                buffer.clear();
                socketChannel.read(buffer);

                buffer.flip();

                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                if (i == commands.size() - 1) {
                    String failCondition = String.format("No such song in Spotify%n");
                    Assert.assertFalse(commands.get(commands.size() - 1).equals(failCondition));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private List<String> getListOfReplies(List<String> commands) throws IOException {

        List<String> replies = new ArrayList<>();

        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));


        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
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