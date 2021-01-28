package bg.sofia.uni.fmi.mjt.spotify.Server.dto;

import javax.sound.sampled.AudioFormat;
import java.io.Serializable;

public class AudioFormatDTO implements Serializable {

    private final String encoding;
    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final int frameSize;
    private final float frameRate;
    private final boolean bigEndian;

    private final long songSizeInBytes;

    public AudioFormatDTO(AudioFormat.Encoding encoding, float sampleRate, int sampleSizeInBits,
                          int channels, int frameSize, float frameRate, boolean bigEndian, long songSizeInBytes) {
        this.encoding = encoding.toString();
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.frameSize = frameSize;
        this.frameRate = frameRate;
        this.bigEndian = bigEndian;
        this.songSizeInBytes = songSizeInBytes;
    }

    public long getSongSizeInBytes() {
        return songSizeInBytes;
    }

    public String getEncoding() {
        return encoding;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public int getChannels() {
        return channels;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    @Override
    public String toString() {
        return "AudioFormatDTO{" +
               "encoding=" + encoding +
               ", sampleRate=" + sampleRate +
               ", sampleSizeInBits=" + sampleSizeInBits +
               ", channels=" + channels +
               ", frameSize=" + frameSize +
               ", frameRate=" + frameRate +
               ", bigEndian=" + bigEndian +
               '}';
    }
}
