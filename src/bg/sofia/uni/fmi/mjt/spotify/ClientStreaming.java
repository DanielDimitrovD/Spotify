package bg.sofia.uni.fmi.mjt.spotify;

import bg.sofia.uni.fmi.mjt.spotify.Server.dto.AudioFormatDTO;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ClientStreaming implements Runnable {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static ByteBuffer buffer = ByteBuffer.allocateDirect(16_384);

    private final String command;

    public ClientStreaming(String command) {
        this.command = command;
    }


    @Override
    public void run() {

        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

            buffer.clear();
            buffer.put(command.getBytes(StandardCharsets.UTF_8));

            buffer.flip();
            socketChannel.write(buffer);

            buffer.clear();
            socketChannel.read(buffer);
            buffer.flip();

            byte[] byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);

            AudioFormatDTO dto = bytesToObject(byteArray);

            AudioFormat format = new AudioFormat(new AudioFormat.Encoding(dto.getEncoding()), dto.getSampleRate(),
                    dto.getSampleSizeInBits(), dto.getChannels(), dto.getFrameSize(),
                    dto.getFrameRate(), dto.isBigEndian());

//            System.out.println(dto.toString());

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine dataLine = (SourceDataLine) AudioSystem.getLine(info);

            dataLine.open();

            dataLine.start();

            long receivedBytes = 0;

            int r;

            while (true) {

                buffer.clear();

                r = socketChannel.read(buffer);
                buffer.flip();

                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                receivedBytes += bytes.length;

//                System.out.println("received bytes:" + receivedBytes);


                if (r == 1) {
                    dataLine.close();
                    break;
                }

                dataLine.write(bytes, 0, bytes.length);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private AudioFormatDTO bytesToObject(byte[] byteArray) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
             ObjectInputStream in = new ObjectInputStream(bis)) {

            return (AudioFormatDTO) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


}
