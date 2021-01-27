package bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;

public class ServerSpotifyStreamer {

    public static void main(String[] args) {

        try {

            AudioInputStream stream = AudioSystem.getAudioInputStream(new File("../songs/Little Mix - DNA.wav"));
            SourceDataLine dataLine = AudioSystem.getSourceDataLine(stream.getFormat());
            dataLine.open();

            dataLine.start();

            int r = 0;

            byte[] bytes = new byte[1024];

            stream.available();

            while ((r = stream.read(bytes)) != -1) {
                dataLine.write(bytes, 0, r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
