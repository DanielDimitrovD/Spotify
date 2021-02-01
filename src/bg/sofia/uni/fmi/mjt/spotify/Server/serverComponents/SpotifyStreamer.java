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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SpotifyStreamer {

    private String musicFolderURL = "D:\\4-course\\songs\\";

    private Map<SocketChannel, Long> songCurrentBytesMap = new HashMap<>();

    private Map<SocketChannel, Integer> userToSongMap = new HashMap<>();

    private Map<Integer, String> songsMap = new HashMap<>();

    private static final int BUFFER_SIZE = 16_384;

    public SpotifyStreamer() {
        songsMap.putAll(Map.of(
                1, "Ice Cream - Захир (HD)",
                2, "Iggy Azalea - Black Widow ft. Rita Ora",
                3, "Iggy Azalea - Fancy ft. Charli XCX",
                4, "INNA - Take Me Higher (by Play&amp;Win) [Online Video]",
                5, "Inna feat. Marian Hill - Diggy Down",
                6, "Jason Derulo - &quot;Talk Dirty&quot; feat. 2Chainz (Official HD Music Video)",
                7, "Jason Derulo - Wiggle feat. Snoop Dogg (Official HD Music Video)",
                8, "Jay Z ft. Kanye West - Niggas in Paris (Official music video)",
                9, "Eminem - Till I Collapse",
                10, "Papi Hans - Hubavo mi stava Х2 (ft. Sando & Mando)"));
    }

    public void setSongForUser(SocketChannel userChannel, int songIndex) {
        userToSongMap.put(userChannel, songIndex);
    }

    public byte[] getAudioFormatHeaders(SocketChannel userSocketChannel) {

        try {
            System.out.println("../songs/" + songsMap.get(userToSongMap.get(userSocketChannel))
                               + ".wav");

            AudioFormat format = AudioSystem.getAudioInputStream(new File("../songs/" +
                                                                          songsMap.get(userToSongMap.get(userSocketChannel))
                                                                          + ".wav")).getFormat();


            long songSizeInBytes = Files.size(Path.of("../songs/" + songsMap.get(userToSongMap.get(userSocketChannel))
                                                      + ".wav"));


            AudioFormatDTO dto = new AudioFormatDTO(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(),
                    format.getChannels(), format.getFrameSize(), format.getFrameRate(), format.isBigEndian(), songSizeInBytes);

            System.out.println(dto.toString());

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
        songCurrentBytesMap.putIfAbsent(socketChannel, 35_000_000L);

        System.out.println();

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(
                new File(musicFolderURL + songsMap.get(userToSongMap.get(socketChannel))
                         + ".wav"))) {

            long currentPositionInBytes = songCurrentBytesMap.get(socketChannel);

            System.out.println("current position in bytes: " + currentPositionInBytes);

            byte[] bytes = new byte[BUFFER_SIZE];

            long skipped = stream.skip(currentPositionInBytes);

            System.out.println("skipped: " + skipped);

            int r = stream.read(bytes);

            int availableBytes = r < BUFFER_SIZE ? r : BUFFER_SIZE;

            // reset song
            if (r == -1) {
                clearStreamingSocketChannel(socketChannel);
                return new byte[]{-1};
            }

            System.out.println("Stream available bytes: " + availableBytes);

            songCurrentBytesMap.put(socketChannel, currentPositionInBytes + availableBytes);

            return bytes;
        }
    }

        private void clearStreamingSocketChannel(SocketChannel socketChannel) {
            songCurrentBytesMap.put(socketChannel, 0L);
            userToSongMap.remove(socketChannel);
        }

}