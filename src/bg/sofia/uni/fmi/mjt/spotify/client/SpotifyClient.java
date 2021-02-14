package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.ClientExceptions.ClientConnectionException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpotifyClient {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static ByteBuffer buffer = ByteBuffer.allocateDirect(16_384);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        SpotifyClient spotifyClient = new SpotifyClient();
        spotifyClient.startClient();
    }

    private void printSongs() {
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

                System.out.println(" =>");
                String message = String.format("%s%n", scanner.nextLine());

                if (message.startsWith("play")) {
                    executorService.execute(new ClientStreaming(message));
                } else {

                    buffer.clear();
                    buffer.put(message.getBytes());
                    buffer.flip();
                    socketChannel.write(buffer);

                    buffer.clear();
                    socketChannel.read(buffer);
                    buffer.flip();

                    byte[] byteArray = new byte[buffer.remaining()];
                    buffer.get(byteArray);

                    String reply = new String(byteArray, "UTF-8");

                    System.out.print(reply);

                    if (reply.contains("disconnected")) {
                        return;
                    }
                }
            }
        } catch (IOException e) {
            throw new ClientConnectionException("There is a problem with the network communication", e.getCause());
        }

    }


}
