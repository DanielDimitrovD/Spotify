package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories;

import bg.sofia.uni.fmi.mjt.spotify.Server.dto.Playlist;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class SpotifyPlaylistRepositoryTest {

    private static final Type token = new TypeToken<Map<String, Map<String, Playlist>>>() {
    }.getType();
    private static Path playlistFile;
    private static Gson gson = new Gson();

    private static SpotifyPlaylistRepository spotifyPlaylistRepository;

    private SpotifySongRepository spotifySongRepository = new
            SpotifySongRepository("D:\\4-course\\songs\\");

    @BeforeClass
    public static void writeCredentialsToJson() {

        Map<String, Playlist> testPlaylist = Map.of("bulgaria", new Playlist("bulgaria",
                new ArrayList<>()));

        Playlist daniPaly = new Playlist("sofia", new ArrayList<>());
        daniPaly.addSong("Papi Hans - Hubavo mi Stava");

        Map<String, Playlist> daniPlaylist = Map.of("sofia", daniPaly);

        Map<String, Map> playlistMap = Map.of("test", testPlaylist, "dani", daniPlaylist);

        System.out.println(playlistMap.toString());

        try {

            Files.deleteIfExists(Path.of("test_playlist.json"));
            Files.createFile(Path.of("test_playlist.json"));
            playlistFile = Path.of("test_playlist.json");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String toJson = gson.toJson(playlistMap, token);
        try {
            Files.writeString(playlistFile, toJson, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UnsupportedOperationException();
        }

        spotifyPlaylistRepository = new SpotifyPlaylistRepository(playlistFile);
    }

    @AfterClass
    public static void tearDown() {
        try {
            Files.delete(playlistFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSongDoesNotExistReply() throws UnsupportedEncodingException {

        String command = "add-song-to sofia asdf";
        String email = "dani";

        String song = Arrays.stream(command.split("\\s+")).skip(2).collect(Collectors.joining(" "));

        byte[] response = spotifyPlaylistRepository.addSongToPlaylist(email, command.split("\\s+"));
        String responseReply = new String(response, "UTF-8");

        String expected = String.format("Song %s does not exist.%n", song);

        Assert.assertEquals("Expected song does not exist reply", expected, responseReply);
    }

    @Test
    public void testIllegalNumberOfParametersForAddSongToPlaylistCommand() throws UnsupportedEncodingException {
        String command = "add-song-to a";
        String email = "dani";

        String song = Arrays.stream(command.split("\\s+")).skip(2).collect(Collectors.joining(" "));

        byte[] response = spotifyPlaylistRepository.addSongToPlaylist(email, command.split("\\s+"));
        String responseReply = new String(response, "UTF-8");

        String expected = String.format("command format : add-song-to <name_of_the_playlist> <song>");

        Assert.assertEquals("Expected illegal command format reply", expected, responseReply);
    }

    @Test
    public void testPlaylistDoesNotExistReply() throws UnsupportedEncodingException {
        String command = "add-song-to asdf Justin Bieber";
        String email = "dani";
        String playlist = "asdf";

        String song = Arrays.stream(command.split("\\s+")).skip(2).collect(Collectors.joining(" "));

        byte[] response = spotifyPlaylistRepository.addSongToPlaylist(email, command.split("\\s+"));
        String responseReply = new String(response, "UTF-8");

        String expected = String.format("playlist %s does not exists. Please create %s first then add songs.%n",
                playlist, playlist);

        Assert.assertEquals("Expected playlist does not exist reply", expected, responseReply);
    }

    @Test
    public void testPlaylistAlreadyHasSongResponse() throws UnsupportedEncodingException {
        String command = "add-song-to sofia Papi Hans - Hubavo mi Stava";
        String email = "dani";
        String playlist = "sofia";

        String song = Arrays.stream(command.split("\\s+")).skip(2).collect(Collectors.joining(" "));

        byte[] response = spotifyPlaylistRepository.addSongToPlaylist(email, command.split("\\s+"));
        String responseReply = new String(response, "UTF-8");

        String expected = String.format("Playlist %s already has song %s.%n", playlist, song);

        Assert.assertEquals("Expected playlist already has this song reply", expected, responseReply);
    }

    @Test
    public void testSongAddedSuccessfullyToPlaylist() throws UnsupportedEncodingException {
        String command = "add-song-to bulgaria Papi Hans - Hubavo mi Stava";
        String email = "test";
        String playlist = "bulgaria";

        String song = Arrays.stream(command.split("\\s+")).skip(2).collect(Collectors.joining(" "));

        byte[] response = spotifyPlaylistRepository.addSongToPlaylist(email, command.split("\\s+"));
        String responseReply = new String(response, "UTF-8");

        String expected = String.format("Song %s added successfully to playlist %s%n", song, playlist);

        Assert.assertEquals("Expected song to be added successfully to the playlist",
                expected, responseReply);
    }


    @Test
    public void testPlaylistAlreadyExistsResponse() throws UnsupportedEncodingException {
        String command = "create-playlist bulgaria";
        String email = "test";
        String playlist = "bulgaria";

        String song = Arrays.stream(command.split("\\s+")).skip(2).collect(Collectors.joining(" "));

        byte[] response = spotifyPlaylistRepository.createPlaylist(email, command.split("\\s+"));
        String responseReply = new String(response, "UTF-8");

        String expected = String.format("Playlist already exists%n");

        Assert.assertEquals("Expected playlist already exists response", expected, responseReply);
    }

    @Test
    public void testPlaylistSuccessfullyCreatedResponse() throws UnsupportedEncodingException {
        String command = "create-playlist tempo";
        String email = "test";
        String playlist = "tempo";

        String song = Arrays.stream(command.split("\\s+")).skip(2).collect(Collectors.joining(" "));

        byte[] response = spotifyPlaylistRepository.createPlaylist(email, command.split("\\s+"));
        String responseReply = new String(response, "UTF-8");

        String expected = String.format("Playlist successfully created%n");

        Assert.assertEquals("Expected playlist successfully created response", expected, responseReply);
    }

    @Test
    public void testShowPlaylistInfoPlaylistDoesNotExistResponse() throws UnsupportedEncodingException {
        String command = "show-playlist rnb";
        String email = "test";
        String playlist = "rnb";

        String song = Arrays.stream(command.split("\\s+")).skip(2).collect(Collectors.joining(" "));

        byte[] response = spotifyPlaylistRepository.showPlaylist(email, command.split("\\s+"));
        String responseReply = new String(response, "UTF-8");

        String expected = String.format("Playlist %s does not exist. Please create it first%n", playlist);

        Assert.assertEquals("Expected playlist does not exist response when trying to show info",
                expected, responseReply);
    }

    @Test
    public void testShowPlaylistInfoCommand() throws UnsupportedEncodingException {
        String command = "show-playlist sofia";
        String email = "dani";
        String playlist = "sofia";

        String song = Arrays.stream(command.split("\\s+")).skip(2).collect(Collectors.joining(" "));

        byte[] response = spotifyPlaylistRepository.showPlaylist(email, command.split("\\s+"));
        String responseReply = new String(response, "UTF-8");

        String expected = String.format("Playlist name: sofia %n" +
                                        "Songs: [Papi Hans - Hubavo mi Stava] %n");

        Assert.assertEquals("Expected playlist info is not correct",
                expected, responseReply);
    }

}