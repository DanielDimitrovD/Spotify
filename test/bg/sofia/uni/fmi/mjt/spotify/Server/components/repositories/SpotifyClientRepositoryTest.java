package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories;

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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SpotifyClientRepositoryTest {

    private static final Type mapType = new TypeToken<Map<String, String>>() {
    }.getType();
    private static Path credentialsFile;
    private static Gson gson = new Gson();

    private static SpotifyClientRepository clientRepository;
    private static Logger logger = Logger.getLogger("Spotify Client Repository Test Logger");

    @BeforeClass
    public static void writeCredentialsToJson() {

        Map<String, String> entries = Map.of("test", "1234", "dani", "1234", "pesho", "asdf");

        try {
            Files.createFile(Path.of("test_credentials.json"));
            credentialsFile = Path.of("test_credentials.json");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to create a file for test_credentials.json", e);
        }

        String toJson = gson.toJson(entries, mapType);
        try {
            Files.writeString(credentialsFile, toJson, StandardOpenOption.WRITE);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error trying to initialize users credentials in test_credentials.json", e);
        }

        clientRepository = new SpotifyClientRepository(credentialsFile);
    }

    @AfterClass
    public static void tearDown() {
        try {
            Files.delete(credentialsFile);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to delete tst_credentials.json in tearDown method", e);
        }
    }

    @Test
    public void testUserNotRegisteredInSystem() throws UnsupportedEncodingException {
        String email = "gosho";

        String[] tokens = "login gosho 12345".split("\\s+");

        byte[] response = clientRepository.login(tokens, null);
        String repoResponse = new String(response, "UTF-8");

        String expected = String.format("Username %s does not exist.%n", email);
        Assert.assertEquals("Expected username does not exist response", expected, repoResponse);
    }

    @Test
    public void testUserAlreadyRegisteredInSystem() throws UnsupportedEncodingException {
        String email = "test";

        String[] tokens = "register test 1234".split("\\s+");

        byte[] response = clientRepository.register(tokens);
        String repoResponse = new String(response, "UTF-8");

        String expected = String.format("Account with email %s already registered. Please try another email.%n",
                email);
        Assert.assertEquals("Expected username already registered response", expected, repoResponse);

    }

    @Test
    public void testUserSuccessfullyRegusteredInSystem() throws UnsupportedEncodingException {
        String email = "tedi";

        String[] tokens = "register tedi 1234".split("\\s+");

        byte[] response = clientRepository.register(tokens);
        String repoResponse = new String(response, "UTF-8");

        String expected = String.format("Account with email %s successfully registered.%n", email);
        Assert.assertEquals("Expected username successfully registered response", expected, repoResponse);
    }

    @Test
    public void testWrongNumberOfArgumentsWhenLoggingIntoSystem() throws UnsupportedEncodingException {
        String[] tokens = "register tedi 1234 asdf asdas asdasdsa".split("\\s+");

        byte[] response = clientRepository.login(tokens, null);
        String repoResponse = new String(response, "UTF-8");

        String expected = String.format("Wrong number of arguments for login command%n" +
                                        "login <email> <password>%n");
        Assert.assertEquals("Expected wrong number of arguments when logging response", expected, repoResponse);
    }


    @Test
    public void testWrongPasswordResponseWhenLoggingIntoSystem() throws UnsupportedEncodingException {
        String email = "test";

        String[] tokens = "login test asdf1234".split("\\s+");

        byte[] response = clientRepository.login(tokens, null);
        String repoResponse = new String(response, "UTF-8");

        String expected = String.format("Wrong password for %s%n", email);
        Assert.assertEquals("Expected wrong password for user response", expected, repoResponse);
    }


    @Test
    public void testLoginSuccessfullyResponse() throws UnsupportedEncodingException {
        String email = "test";

        String[] tokens = "login test 1234".split("\\s+");

        byte[] response = clientRepository.login(tokens, null);
        String repoResponse = new String(response, "UTF-8");

        String expected = String.format("%s logged in successfully%n", email);
        Assert.assertEquals("Expected login successfully for user response", expected, repoResponse);
    }


}