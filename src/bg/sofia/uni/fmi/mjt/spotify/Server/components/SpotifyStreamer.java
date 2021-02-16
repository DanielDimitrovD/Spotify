package bg.sofia.uni.fmi.mjt.spotify.Server.components;

import bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.SpotifySongRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.SpotifyStatistics;
import bg.sofia.uni.fmi.mjt.spotify.Server.components.repositories.exceptions.AudioFormatDTOException;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SpotifyStreamer {

    private static final int BUFFER_SIZE = 1_024;

    private Map<SocketChannel, Long> channelToSongCurrentBytes = new HashMap<>();
    private Set<SocketChannel> streamingChannels = new HashSet<>();
    private Map<SocketChannel, String> channelToSong = new HashMap<>();
    private SpotifyStatistics statistics = new SpotifyStatistics();

    private SpotifySongRepository songRepository;

    public SpotifyStreamer(String musicFolderURL) {
        songRepository = new SpotifySongRepository(musicFolderURL);
    }

    public void setSongForChannel(SocketChannel userChannel, String[] song) {

        List<String> matchedSongs = songRepository.searchSongs(song);

        if (matchedSongs.isEmpty()) {
            return;
        }
        String matchedSong = matchedSongs.get(0);

        channelToSong.put(userChannel, matchedSong);
        statistics.updateSong(matchedSong);
    }

    private String channelToSongAbsolutePath(SocketChannel userSocketChannel) {

        String userSongChoice = channelToSong.get(userSocketChannel);
        String songAbsolutePath = songRepository.getSongAbsolutePath(userSongChoice);

        return songAbsolutePath;
    }

    public byte[] getAudioFormatHeaders(SocketChannel userSocketChannel) {

        String songAbsolutePath = channelToSongAbsolutePath(userSocketChannel);

        if (songAbsolutePath == null) {
            return "No such song is Spotify!".getBytes(StandardCharsets.UTF_8);
        }

        try {
            AudioFormat format = AudioSystem.getAudioInputStream(new File(songAbsolutePath)).getFormat();

            long songSizeInBytes = Files.size(Path.of(songAbsolutePath));

            AudioFormatDTO dto = new AudioFormatDTO(format.getEncoding(), format.getSampleRate(),
                    format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(),
                    format.isBigEndian(), songSizeInBytes);

            return objectToByteArray(dto);
        } catch (IOException | UnsupportedAudioFileException e) {
            throw new AudioFormatDTOException("Could not marshall AudioFormatDTO", e);
        }
    }

    private byte[] objectToByteArray(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);

            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not convert object to byte array");
        }

    }

    public byte[] readMusicChunk(SocketChannel socketChannel) throws IOException, UnsupportedAudioFileException {
        channelToSongCurrentBytes.putIfAbsent(socketChannel, 0L);

        String songAbsolutePath = channelToSongAbsolutePath(socketChannel);

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new File(songAbsolutePath))) {

            long currentPositionInBytes = channelToSongCurrentBytes.get(socketChannel);

            byte[] bytes = new byte[BUFFER_SIZE];

            int r = stream.read(bytes);

            int availableBytes = r < BUFFER_SIZE ? r : BUFFER_SIZE;

            // reset song
            if (r == -1) {

                clearStreamingSocketChannel(socketChannel);
                streamingChannels.remove(socketChannel);
                return new byte[]{-1};
            }

            channelToSongCurrentBytes.put(socketChannel, currentPositionInBytes + availableBytes);

            return bytes;
        }
    }

    private void clearStreamingSocketChannel(SocketChannel socketChannel) {
        channelToSongCurrentBytes.put(socketChannel, 0L);
        streamingChannels.remove(socketChannel);
        channelToSong.remove(socketChannel);
    }

}