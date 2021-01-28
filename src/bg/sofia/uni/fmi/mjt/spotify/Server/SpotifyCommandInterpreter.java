package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.dto.AudioFormatDTO;
import bg.sofia.uni.fmi.mjt.spotify.Server.enums.SpotifyCommands;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyClientRepository;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class SpotifyCommandInterpreter {

    private final SpotifyClientRepository spotifyClientRepository;

    public SpotifyCommandInterpreter(Path credentialsFile) {
        this.spotifyClientRepository = new SpotifyClientRepository(credentialsFile);
    }

    public byte[] interpretCommand(String userMessage, SocketChannel userSocketChannel) {

        if (userMessage == null) {
            throw new IllegalArgumentException("userMessage argument in interpretCommand method is null");
        }

        byte[] reply = null;

        String[] tokens = userMessage.split("\\s+");

        String clientCommand = tokens[0];

        Optional<SpotifyCommands> command = Arrays.stream(SpotifyCommands.values())
                .filter(c -> clientCommand.equals(c.getCommand()))
                .findFirst();

        if (!command.isPresent()) {
            return String.format("Unknown command%n").getBytes(StandardCharsets.UTF_8);
        }

        switch (command.get()) {
            case REGISTER -> reply = register(tokens);
            case LOGIN -> reply = login(tokens, userSocketChannel);
            case DISCONNECT -> reply = disconnect(userSocketChannel);
//            case SEARCH -> reply = search();
//            case TOP -> reply = top();
//            case CREATE_PLAYLIST -> reply = createPlaylist();
//            case ADD_SONG_TO -> reply = addSongTo();
//            case SHOW_PLAYLIST -> reply = showPlaylist();
            case PLAY_SONG -> reply = playSong(userSocketChannel);
//            case STOP -> reply = stop();
//            default -> reply = String.format("Unknown command%n");
        }

        return reply;
    }

    private byte[] objectToByteArray(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);

            System.out.println(object.toString());

            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] playSong(SocketChannel userSocketChannel) {

        try {
            AudioFormat format = AudioSystem.getAudioInputStream(new File("../songs/Little Mix - DNA.wav"))
                    .getFormat();

            long fileSizeInBytes = Files.size(Path.of("../songs/Little Mix - DNA.wav"));

            AudioFormatDTO dto = new AudioFormatDTO(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(),
                    format.getChannels(), format.getFrameSize(), format.getFrameRate(), format.isBigEndian(), fileSizeInBytes);

            System.out.println(dto.toString());

            return objectToByteArray(dto);
        } catch (IOException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] disconnect(SocketChannel userSocketChannel) {
        spotifyClientRepository.disconnect(userSocketChannel);
        return String.format("Account successfully disconnected%n").getBytes(StandardCharsets.UTF_8);
    }

    private byte[] login(String[] tokens, SocketChannel userSocketChannel) {

        final int LOGIN_COMMAND_PARAMETERS = 3;

        final int LOGIN_COMMAND_USERNAME_INDEX = 1;
        final int LOGIN_COMMAND_PASSWORD_INDEX = 2;

        if (tokens.length != LOGIN_COMMAND_PARAMETERS) {
            return String.format("Wrong number of arguments for login command%n").getBytes(StandardCharsets.UTF_8);
        }

        String email = tokens[LOGIN_COMMAND_USERNAME_INDEX];
        String password = tokens[LOGIN_COMMAND_PASSWORD_INDEX];

        boolean success = spotifyClientRepository.login(email, password, userSocketChannel);

        if (success) {
            return String.format("Account with email %s logged in successfully%n", email).getBytes(StandardCharsets.UTF_8);
        } else {
            return String.format("Invalid login attempt from account %s%n", email).getBytes(StandardCharsets.UTF_8);
        }
    }


    private byte[] register(String[] tokens) {

        final int REGISTER_COMMAND_PARAMETERS = 3;

        final int REGISTER_COMMAND_USERNAME_INDEX = 1;
        final int REGISTER_COMMAND_PASSWORD_INDEX = 2;

        if (tokens.length != REGISTER_COMMAND_PARAMETERS) {
            return String.format("Wrong number of arguments for register command%n").getBytes(StandardCharsets.UTF_8);
        }

        String email = tokens[REGISTER_COMMAND_USERNAME_INDEX];
        String password = tokens[REGISTER_COMMAND_PASSWORD_INDEX];

        boolean success = spotifyClientRepository.register(email, password);

        if (success) {
            return String.format("Account with email %s successfully registered%n", email).getBytes(StandardCharsets.UTF_8);
        } else {
            return String.format("Account with email %s already registered. Please try another email%n", email).getBytes(StandardCharsets.UTF_8);
        }
    }


    private String stop() {
        return null;
    }


    private String showPlaylist() {
        return null;
    }

    private String addSongTo() {
        return null;
    }

    private String createPlaylist() {
        return null;
    }

    private String top() {
        return null;
    }

    private String search() {
        return null;
    }


}
