package bg.sofia.uni.fmi.mjt.spotify;

import bg.sofia.uni.fmi.mjt.spotify.ClientExceptions.ClientConnectionException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class SpotifyClient {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static ByteBuffer buffer = ByteBuffer.allocateDirect(512);

    public static void main(String[] args) {
        SpotifyClient spotifyClient = new SpotifyClient();

        spotifyClient.startClient();
    }

    public void startClient() {

        try (SocketChannel socketChannel = SocketChannel.open()) {

            Scanner scanner = new Scanner(System.in);

            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

            System.out.println("connected to server");

            while (true) {

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

                String reply = new String(byteArray, "UTF-8");

                System.out.print(reply);

                if (reply.contains("Disconnected")) {
                    return;
                }

            }
        } catch (IOException e) {
            throw new ClientConnectionException("There is a problem with the network communication", e.getCause());
        }

    }

}
