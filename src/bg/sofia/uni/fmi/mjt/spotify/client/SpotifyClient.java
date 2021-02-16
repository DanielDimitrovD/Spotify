package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.ClientConnectionException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpotifyClient {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 1_024;

    private final static String NOT_LOGGED_IN_MESSAGE = String.format("Please login into Spotify!");
    private final static String WAIT_FOR_STREAMING_NO_END_MESSAGE = String.format("Please wait for streaming to end");
    private final static String LOGGED_IN_MESSAGE = String.format("logged in successfully");
    private final static String STOP_STREAMING_MESSAGE = String.format("Stopped streaming");
    private final static String DISCONNECT_MESSAGE = String.format("disconnected");
    public static boolean isStreaming = false;
    private static ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String email;
    private boolean isLogged = false;

    public static void main(String[] args) {
        SpotifyClient spotifyClient = new SpotifyClient();
        spotifyClient.startClient();
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
                        System.out.println(NOT_LOGGED_IN_MESSAGE);
                        continue;
                    }

                    if (isStreaming) {
                        System.out.println(WAIT_FOR_STREAMING_NO_END_MESSAGE);
                        continue;
                    }

                    executorService.execute(new SpotifyClientStreamingRunnable(message, email));
                } else {
                    writeToChannel(message.getBytes(StandardCharsets.UTF_8), socketChannel);
                    byte[] byteArray = readMessage(socketChannel);

                    String reply = new String(byteArray, "UTF-8");

                    System.out.println(reply);

                    if (reply.contains(LOGGED_IN_MESSAGE)) {
                        this.email = reply.split("\\s+")[0];
                        this.isLogged = true;
                    } else if (reply.contains(STOP_STREAMING_MESSAGE)) {
                        this.isStreaming = false;
                    } else if (reply.contains(DISCONNECT_MESSAGE)) {
                        executorService.shutdown();
                    }
                }
            }
        } catch (IOException e) {
            throw new ClientConnectionException("There is a problem with the network communication", e.getCause());
        }
    }
}
