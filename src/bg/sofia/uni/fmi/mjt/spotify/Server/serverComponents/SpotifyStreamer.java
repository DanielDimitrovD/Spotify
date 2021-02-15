package bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents;

import bg.sofia.uni.fmi.mjt.spotify.Server.dto.AudioFormatDTO;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.repositories.SpotifySongRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.repositories.SpotifyStatistics;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
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
import java.util.*;

public class SpotifyStreamer {

    private static final int BUFFER_SIZE = 1_024;

//    private static String musicFolderURL;


    private Map<SocketChannel, Long> songCurrentBytesMap = new HashMap<>();

    private Set<SocketChannel> streamingUsers = new HashSet<>();

    private Map<SocketChannel, String> userSongMap = new HashMap<>();

//    private Map<String, String> songMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


    private Map<String, Long> songSize = new HashMap<>();

    private SpotifyStatistics statistics = new SpotifyStatistics();

    private SpotifySongRepository songRepository;

    public SpotifyStreamer(String musicFolderURL) {
//        this.musicFolderURL = musicFolderURL;
        songRepository = new SpotifySongRepository(musicFolderURL);
//        initializeSongMap();
    }

//    public static boolean containsSong(String song) {
//        try {
//            return Files.walk(Path.of(musicFolderURL))
//                    .map(s -> s.getFileName().toString().split(".wav")[0])
//                    .anyMatch(p -> p.equalsIgnoreCase(song));
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    public static List<String> searchSongs(String[] words) {
//        List<String> tokens = Arrays.stream(words).collect(Collectors.toList());
//
//        Function<Path, String> pathToString = s -> s.getFileName().toString().split(".wav")[0];
//        Predicate<String> matchWordsToSong = p -> {
//            Set<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
//            s.addAll(Arrays.asList(p.split("\\s+")));
//            return s.containsAll(tokens);
//        };
//
//        try {
//            return Files.walk(Path.of(musicFolderURL))
//                    .map(pathToString)
//                    .filter(matchWordsToSong)
//                    .collect(Collectors.toList());
//        } catch (Exception e) {
//            //TODO add exception
//            e.printStackTrace();
//            return null;
//        }
//    }


//    private void initializeSongMap() {
//        try {
//            List<String> songsNames = getSongs();
//
//            for (String song : songsNames) {
//                String songPath = String.format("%s%s.wav", musicFolderURL, song);
//
//                //TODO experimental
//                songMap.put(song, songPath);
//
//                long songSizeInBytes = Files.size(Path.of(songPath));
//
//                songSize.put(songPath, songSizeInBytes);
//            }
//
//            System.out.println("song size map :" + songSize.toString());
//
//        } catch (Exception e) {
//            // TODO add exception
//            throw new UnsupportedOperationException();
//        }
//    }

//    private List<String> getSongs() {
//        try {
//            Stream<Path> songs = Files.walk(Path.of(musicFolderURL)).filter(p -> !Files.isDirectory(p));
//
//            List<String> songsNames = songs.map(s -> s.getFileName().toString().split(".wav")[0])
//                    .collect(Collectors.toList());
//
//            return songsNames;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            //TODO add exception
//            return null;
//        }
//    }

    public void setSongForUser(SocketChannel userChannel, String[] song) {

        System.out.printf("Song parameters: %s", Arrays.deepToString(song));

        List<String> matchedSongs = songRepository.searchSongs(song);

        if (matchedSongs.isEmpty()) {
            System.out.println("Could not find match");
            return;
        }

        System.out.println("Set song for user");
        System.out.println("Matched songs: " + matchedSongs.toString());

        System.out.printf("Matched song:%s", matchedSongs.get(0));

        String matchedSong = matchedSongs.get(0);

        userSongMap.put(userChannel, matchedSong);
        statistics.updateSong(matchedSong);
    }


    public byte[] getAudioFormatHeaders(SocketChannel userSocketChannel) {

//        String songAbsolutePath = songMap.get(userSongMap.get(userSocketChannel));

        String userSongChoice = userSongMap.get(userSocketChannel);

        String songAbsolutePath = songRepository.getSongAbsolutePath(userSongChoice);

        if (songAbsolutePath == null) {
            return "No such song is Spotify!".getBytes(StandardCharsets.UTF_8);
        }

        System.out.println("Song path:" + songAbsolutePath);

        try {
            AudioFormat format = AudioSystem.getAudioInputStream(new File(songAbsolutePath)).getFormat();

            long songSizeInBytes = Files.size(Path.of(songAbsolutePath));

            AudioFormatDTO dto = new AudioFormatDTO(format.getEncoding(), format.getSampleRate(),
                    format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(),
                    format.isBigEndian(), songSizeInBytes);

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

        String userSong = userSongMap.get(socketChannel);

//        System.out.println("user song: " + userSong);

        // TODO experiment
//        String songChoice = songMap.get(userSong);
        String songChoice = songRepository.getSongAbsolutePath(userSong);

//        System.out.println("user song name:" + songName);

        String songAbsolutePath = songChoice;

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

                System.out.println("clear song");

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
        streamingUsers.remove(socketChannel);
        userSongMap.remove(socketChannel);
    }

}