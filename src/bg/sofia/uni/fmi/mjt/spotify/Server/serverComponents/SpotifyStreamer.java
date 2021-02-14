package bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents;

import bg.sofia.uni.fmi.mjt.spotify.Server.dto.AudioFormatDTO;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpotifyStreamer {

    private static final int BUFFER_SIZE = 16_384;
    private static String musicFolderURL;
    private Map<SocketChannel, Long> songCurrentBytesMap = new HashMap<>();
    private Map<SocketChannel, Integer> userToSongMap = new HashMap<>();
    private Map<Integer, String> songsMap = new HashMap<>();

    private Set<SocketChannel> streamingUsers = new HashSet<>();


    public SpotifyStreamer(String musicFolderURL) {
        this.musicFolderURL = musicFolderURL;
        initializeSongMap();
    }

    public static boolean containsSong(String song) {
        try {
            return Files.walk(Path.of(musicFolderURL))
                    .map(s -> s.getFileName().toString().split(".wav")[0])
                    .anyMatch(p -> p.equalsIgnoreCase(song));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> searchSongs(String[] words) {

        List<String> tokens = Arrays.stream(words)
                .skip(1)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        Function<Path, String> pathToString = s -> s.getFileName().toString().split(".wav")[0].toLowerCase();
        Predicate<String> matchWordsToSong = p -> Arrays.asList(p.split("\\s+")).containsAll(tokens);

        try {
            return Files.walk(Path.of(musicFolderURL))
                    .map(pathToString)
                    .filter(matchWordsToSong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            //TODO add exception
            e.printStackTrace();
            return null;
        }
    }


    private void initializeSongMap() {
        try {
            int i = 1;

            List<String> songsNames = getSongs();

            for (String song : songsNames) {
                songsMap.put(i++, song + ".wav");
            }
        } catch (Exception e) {
            // TODO add exception
            throw new UnsupportedOperationException();
        }
    }

    private List<String> getSongs() {
        try {
            Stream<Path> songs = Files.walk(Path.of(musicFolderURL));

            List<String> songsNames = songs.map(s -> s.getFileName().toString().split(".wav")[0])
                    .collect(Collectors.toList());

            return songsNames;

        } catch (Exception e) {
            e.printStackTrace();
            //TODO add exception
            return null;
        }
    }

    public void setSongForUser(SocketChannel userChannel, int songIndex) {
        userToSongMap.put(userChannel, songIndex);
        streamingUsers.add(userChannel);
    }

    public byte[] getAudioFormatHeaders(SocketChannel userSocketChannel) {

        String songAbsolutePath = musicFolderURL + songsMap.get(userToSongMap.get(userSocketChannel));

        try {
//            System.out.println(songAbsolutePath);

            AudioFormat format = AudioSystem.getAudioInputStream(new File(songAbsolutePath)).getFormat();

            long songSizeInBytes = Files.size(Path.of(songAbsolutePath));

            AudioFormatDTO dto = new AudioFormatDTO(format.getEncoding(), format.getSampleRate(),
                    format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(),
                    format.isBigEndian(), songSizeInBytes);

//            System.out.println(dto.toString());

            return objectToByteArray(dto);
        } catch (IOException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }

        return null;
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

    public byte[] readMusicChunk(SocketChannel socketChannel) throws IOException, UnsupportedAudioFileException {
        songCurrentBytesMap.putIfAbsent(socketChannel, 0L);

//        System.out.println();

        String songAbsolutePath = musicFolderURL + songsMap.get(userToSongMap.get(socketChannel));

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new File(songAbsolutePath))) {

            long currentPositionInBytes = songCurrentBytesMap.get(socketChannel);

//            System.out.println("current position in bytes: " + currentPositionInBytes);

            byte[] bytes = new byte[BUFFER_SIZE];

            long skipped = stream.skip(currentPositionInBytes);

//            System.out.println("skipped: " + skipped);

            int r = stream.read(bytes);

            int availableBytes = r < BUFFER_SIZE ? r : BUFFER_SIZE;

            // reset song
            if (r == -1) {
                clearStreamingSocketChannel(socketChannel);
                streamingUsers.remove(socketChannel);
                return new byte[]{-1};
            }

//            System.out.println("Stream available bytes: " + availableBytes);

            songCurrentBytesMap.put(socketChannel, currentPositionInBytes + availableBytes);

            return bytes;
        }
    }

    private void clearStreamingSocketChannel(SocketChannel socketChannel) {
        songCurrentBytesMap.put(socketChannel, 0L);
        userToSongMap.remove(socketChannel);
        streamingUsers.remove(socketChannel);
    }

    public boolean isUserStreaming(SocketChannel userChannel) {
        return streamingUsers.contains(userChannel);
    }

}