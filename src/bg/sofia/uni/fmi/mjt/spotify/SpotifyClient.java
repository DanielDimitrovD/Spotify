package bg.sofia.uni.fmi.mjt.spotify;

import bg.sofia.uni.fmi.mjt.spotify.ClientExceptions.ClientConnectionException;
import bg.sofia.uni.fmi.mjt.spotify.Server.dto.AudioFormatDTO;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Scanner;

public class SpotifyClient {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static ByteBuffer buffer = ByteBuffer.allocateDirect(16_384);


    public static void main(String[] args) {
        SpotifyClient spotifyClient = new SpotifyClient();

        spotifyClient.startClient();
    }

    private void printMenu() {
        System.out.println(String.join(System.lineSeparator(),
                List.of(
                        "1. Ice Cream - Захир (HD)%n",
                        "2. Iggy Azalea - Black Widow ft. Rita Ora",
                        "3. Iggy Azalea - Fancy ft. Charli XCX",
                        "4. INNA - Take Me Higher (by Play&amp;Win) [Online Video]",
                        "5. Inna feat. Marian Hill - Diggy Down",
                        "6. Jason Derulo - &quot;Talk Dirty&quot; feat. 2Chainz (Official HD Music Video)",
                        "7. Jason Derulo - Wiggle feat. Snoop Dogg (Official HD Music Video)",
                        "8. Jay Z ft. Kanye West - Niggas in Paris (Official music video)",
                        "9. Eminem - Till I Collapse",
                        "10. Papi Hans - Hubavo mi stava Х2 (ft. Sando & Mando)")));
    }

    public void startClient() {

        try (SocketChannel socketChannel = SocketChannel.open()) {

            Scanner scanner = new Scanner(System.in);

            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

            System.out.println("connected to server");

            while (true) {

                System.out.println("=>");

                printMenu();

                //    System.out.println("Enter message");
                String message = String.format("%s%n", scanner.nextLine());
                //    System.out.println("Sending message <" + message + "> to the server...");

                buffer.clear();
                buffer.put(message.getBytes());
                buffer.flip();
                socketChannel.write(buffer);

                buffer.clear();
                socketChannel.read(buffer);
                buffer.flip();

                byte[] byteArray = new byte[buffer.remaining()];
                buffer.get(byteArray);


                if (message.contains("play")) {

                    AudioFormatDTO dto = bytesToObject(byteArray);

//                    System.out.printf("%s %f %d %d %d %f %b", dto.getEncoding(), dto.getSampleRate(),
//                            dto.getSampleSizeInBits(), dto.getChannels(), dto.getFrameSize(),
//                            dto.getFrameRate(), dto.isBigEndian());

                    long songSizeInBytes = dto.getSongSizeInBytes();

                    AudioFormat format = new AudioFormat(new AudioFormat.Encoding(dto.getEncoding()), dto.getSampleRate(),
                            dto.getSampleSizeInBits(), dto.getChannels(), dto.getFrameSize(),
                            dto.getFrameRate(), dto.isBigEndian());

                    System.out.println(dto.toString());

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

                        System.out.println("received bytes:" + receivedBytes);


                        if (r == 1) {
                            dataLine.close();
                            break;
                        }

                        //     System.out.println("writing package" + packages++);
                        dataLine.write(bytes, 0, bytes.length);
                    }
                } else {


                    String reply = new String(byteArray, "UTF-8");

                    System.out.print(reply);

                    if (reply.contains("disconnected")) {
                        return;
                    }
                }


            }
        } catch (IOException | LineUnavailableException e) {
            throw new ClientConnectionException("There is a problem with the network communication", e.getCause());
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
