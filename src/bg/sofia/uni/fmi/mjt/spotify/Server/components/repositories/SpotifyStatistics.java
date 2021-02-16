package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SpotifyStatistics {
    private static final Map<String, Long> songStreamingCountMap = new TreeMap<>();

    public static List<String> getNMostPopularSongs(int n) {

        int listenedSongsSize = songStreamingCountMap.keySet().size();

        if (n < 0) {
            throw new IndexOutOfBoundsException("n must be a non-negative number");
        }

        if (listenedSongsSize < n) {
            return new ArrayList<>(songStreamingCountMap.keySet());
        }

        return new ArrayList<>(songStreamingCountMap.keySet()).subList(0, n);
    }

    public void updateSong(String songName) {
        songStreamingCountMap.putIfAbsent(songName, 0L);
        songStreamingCountMap.put(songName, songStreamingCountMap.get(songName) + 1);
    }

}
