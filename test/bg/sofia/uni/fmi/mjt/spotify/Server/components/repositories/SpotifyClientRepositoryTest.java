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
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;


public class SpotifyClientRepositoryTest {

    private static final Type mapType = new TypeToken<Map<String, String>>() {
    }.getType();
    private static Path credentialsFile;
    private static Gson gson = new Gson();

    private static SpotifyClientRepository clientRepository;


    @BeforeClass
    public static void writeCredentialsToJson() {

        Map<String, String> entries = Map.of("test", "1234", "dani", "1234", "pesho", "asdf");

        try {
            Files.createFile(Path.of("test_credentials.json"));
            credentialsFile = Path.of("test_credentials.json");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String toJson = gson.toJson(entries, mapType);
        try {
            Files.writeString(credentialsFile, toJson, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UnsupportedOperationException();
        }

        clientRepository = new SpotifyClientRepository(credentialsFile);
        System.out.println(clientRepository);
    }

    @AfterClass
    public static void tearDown() {
        try {
            Files.delete(credentialsFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUserNotRegisteredInSystem() throws UnsupportedEncodingException {
        String email = "gosho";
        String password = "12345";

        String[] tokens = "login gosho 12345".split("\\s+");

        SocketChannel test = null;

        byte[] response = clientRepository.login(tokens, test);
        String repoRespone = new String(response, "UTF-8");

        String expected = String.format("Username %s does not exist.%n", email);
        Assert.assertEquals("Expected username does not exist response", expected, repoRespone);
    }

    @Test
    public void testUserAlreadyRegisteredInSystem() throws UnsupportedEncodingException {
        String email = "test";
        String password = "1234";

        String[] tokens = "register test 1234".split("\\s+");
        SocketChannel test = null;

        byte[] response = clientRepository.register(tokens);
        String repoRespone = new String(response, "UTF-8");

        String expected = String.format("Account with email %s already registered. Please try another email.%n",
                email);
        Assert.assertEquals("Expected username already registered response", expected, repoRespone);

    }

    @Test
    public void testUserSuccessfullyRegusteredInSystem() throws UnsupportedEncodingException {
        String email = "tedi";
        String password = "1234";

        String[] tokens = "register tedi 1234".split("\\s+");
        SocketChannel test = null;

        byte[] response = clientRepository.register(tokens);
        String repoRespone = new String(response, "UTF-8");

        String expected = String.format("Account with email %s successfully registered.%n", email);
        Assert.assertEquals("Expected username successfully registered response", expected, repoRespone);
    }

    @Test
    public void testWrongNumberOfArgumentsWhenLoggingIntoSystem() throws UnsupportedEncodingException {
        String email = "tedi";
        String password = "1234";

        String[] tokens = "register tedi 1234 asdf asdas asdasdsa".split("\\s+");
        SocketChannel test = null;

        byte[] response = clientRepository.login(tokens, test);
        String repoRespone = new String(response, "UTF-8");

        String expected = String.format("Wrong number of arguments for login command%n" +
                                        "login <email> <password>%n");
        Assert.assertEquals("Expected wrong number of arguments when logging response", expected, repoRespone);
    }


    @Test
    public void testWrongPasswordResponseWhenLoggingIntoSystem() throws UnsupportedEncodingException {
        String email = "test";
        String password = "asdf1234";

        String[] tokens = "login test asdf1234".split("\\s+");
        SocketChannel test = null;

        byte[] response = clientRepository.login(tokens, test);
        String repoRespone = new String(response, "UTF-8");

        String expected = String.format("Wrong password for %s%n", email);
        Assert.assertEquals("Expected wrong password for user response", expected, repoRespone);
    }


    @Test
    public void testLoginSuccessfullyResponse() throws UnsupportedEncodingException {
        String email = "test";
        String password = "1234";

        String[] tokens = "login test 1234".split("\\s+");
        SocketChannel test = null;

        byte[] response = clientRepository.login(tokens, test);
        String repoRespone = new String(response, "UTF-8");

        String expected = String.format("%s logged in successfully%n", email);
        Assert.assertEquals("Expected login successfully for user response", expected, repoRespone);
    }


}