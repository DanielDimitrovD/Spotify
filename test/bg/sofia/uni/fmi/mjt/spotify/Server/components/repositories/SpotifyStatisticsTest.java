package bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories;

import junit.framework.Assert;
import org.junit.Test;

import java.util.List;

public class SpotifyStatisticsTest {

    private SpotifyStatistics statistics = new SpotifyStatistics();

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIndexOutOfBoundsException() {
        SpotifyStatistics.getNMostPopularSongs(-10);
    }

    @Test
    public void testUpdateSong() {
        String song = "Papi Hans - Hubavo mi Stava";
        statistics.updateSong(song);

        List<String> mostPopular = SpotifyStatistics.getNMostPopularSongs(1);

        Assert.assertEquals("updating song is not correct", song, mostPopular.get(0));
    }


}