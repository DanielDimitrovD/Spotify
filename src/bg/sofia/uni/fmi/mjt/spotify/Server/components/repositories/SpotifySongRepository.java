package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories;

import bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions.SongRepositoryInitializationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpotifySongRepository {

    private static String musicFolderURL;
    private final Map<String, String> songMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Long> songSizeInBytesMap = new HashMap<>();

    public SpotifySongRepository(String musicFolderURL) {
        SpotifySongRepository.musicFolderURL = musicFolderURL;
        initializeSongMap();
    }

    public static boolean containsSong(String song) {
        try {
            return Files.walk(Path.of(musicFolderURL))
                    .map(s -> s.getFileName().toString().split(".wav")[0])
                    .anyMatch(p -> p.equalsIgnoreCase(song));
        } catch (Exception e) {
            throw new RuntimeException("I/O error trying to search for a song matching user parameter");
        }
    }

    public static List<String> searchSongs(String[] words) {
        List<String> tokens = Arrays.stream(words).collect(Collectors.toList());

        Function<Path, String> pathToString = s -> s.getFileName().toString().split(".wav")[0];
        Predicate<String> matchWordsToSong = p -> {
            Set<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            s.addAll(Arrays.asList(p.split("\\s+")));
            return s.containsAll(tokens);
        };

        try {
            return Files.walk(Path.of(musicFolderURL))
                    .map(pathToString)
                    .filter(matchWordsToSong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Could not search songs in Song Repository", e);
        }
    }

    public String getSongAbsolutePath(String song) {
        return songMap.get(song);
    }

    private void initializeSongMap() {
        List<String> songs = getSongs();

        for (String song : songs) {

            String songPath = String.format("%s%s.wav", musicFolderURL, song);

            songMap.put(song, songPath);

            try {
                long songSizeInBytes = Files.size(Path.of(songPath));
                songSizeInBytesMap.put(songPath, songSizeInBytes);
            } catch (Exception e) {
                throw new RuntimeException("I/O error when trying to read the size of a song");
            }
        }

        System.out.println("song size map :" + songSizeInBytesMap.toString());

    }

    private List<String> getSongs() {
        try {
            Stream<Path> songs = Files.walk(Path.of(musicFolderURL))
                    .filter(p -> !Files.isDirectory(p));

            Function<Path, String> songFileName = s -> s.getFileName().toString().split(".wav")[0];

            return songs.map(songFileName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new SongRepositoryInitializationException("Could not initialize song repository");
        }
    }


}
