package bg.sofia.uni.fmi.mjt.spotify.client;

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
import java.util.Arrays;
import java.util.stream.Collectors;

public class ClientStreaming implements Runnable {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";

    private static int BUFFER_SIZE = 1_024;

    private static ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    private final String command;
    private final String email;


    public ClientStreaming(String command, String email) {

        String[] tokens = command.split("\\s+");

        this.email = email;
        this.command = String.format("%s %s %s", tokens[0], email, Arrays.stream(tokens)
                .skip(1)
                .collect(Collectors.joining(" ")));
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

            int r = 0;

            while (true) {

                buffer.clear();

                try {
                    r = socketChannel.read(buffer);
                } catch (Exception e) {
                    System.out.println("Hard reset");
                }

                if (r == 1) {
                    dataLine.stop();
                    dataLine.close();
                    socketChannel.close();

                    System.out.println("Die streaming");
                    return;
                }


                buffer.flip();

                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                receivedBytes += bytes.length;

//                System.out.println("received bytes:" + receivedBytes);


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
