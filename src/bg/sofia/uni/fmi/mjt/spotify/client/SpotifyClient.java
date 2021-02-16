package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.ClientExceptions.ClientConnectionException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpotifyClient {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 1_024;


    private static ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);


    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private String email;
    private boolean isLogged = false;
    private boolean isStreaming = false;


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

    private byte[] readMessage(SocketChannel socketChannel) throws IOException {
        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();

        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);

        return byteArray;
    }

    private void writeToChannel(byte[] bytes, SocketChannel socketChannel) throws IOException {
        buffer.clear();
        buffer.put(bytes);
        buffer.flip();
        socketChannel.write(buffer);
    }

    public void startClient() {

        try (SocketChannel socketChannel = SocketChannel.open()) {

            Scanner scanner = new Scanner(System.in);

            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

            System.out.println("connected to server");

            while (true) {

                System.out.print(" =>  ");
                String message = String.format("%s%n", scanner.nextLine());

                if (message.startsWith("play")) {

                    if (!isLogged) {
                        System.out.println("Please login into Spotify!");
                        continue;
                    }

                    executorService.execute(new SpotifyClientStreamingRunnable(message, email));
                    isStreaming = true;
                } else {
                    writeToChannel(message.getBytes(StandardCharsets.UTF_8), socketChannel);
                    byte[] byteArray = readMessage(socketChannel);

                    String reply = new String(byteArray, "UTF-8");

                    System.out.print(reply);

                    if (reply.contains("logged in successfully")) {
                        this.email = reply.split("\\s+")[0];
                        this.isLogged = true;
                    }

                    if (reply.contains("Die streaming")) {
                        this.isStreaming = false;
                    }

                    if (reply.contains("disconnected")) {
                        executorService.shutdown();
                        return;
                    }
                }
            }
        } catch (IOException e) {
            throw new ClientConnectionException("There is a problem with the network communication", e.getCause());
        }

    }


}
