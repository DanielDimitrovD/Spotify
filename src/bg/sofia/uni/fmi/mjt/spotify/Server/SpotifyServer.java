package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.dto.AudioFormatDTO;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.ServerSpotifyStreamer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SpotifyServer implements AutoCloseable {

    public static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 16_384;
    private final int port;
    private boolean runServer = true;

    private SpotifyCommandInterpreter commandInterpreter;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer buffer;

    private Path credentialsFile;

    private ServerSpotifyStreamer spotifyStreamer = new ServerSpotifyStreamer();

    public SpotifyServer(int port, Path credentialsFile) {
        this.port = port;
        this.credentialsFile = credentialsFile;

        this.commandInterpreter = new SpotifyCommandInterpreter(this.credentialsFile);

        initialServerConfiguration();

        // allocate byte buffer
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    public static void main(String[] args) {
        try (var wishListServer = new SpotifyServer(1234, Path.of("credentials.json"))) {

            wishListServer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialServerConfiguration() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            serverSocketChannel.configureBlocking(false);

            selector = Selector.open();
            // register selector to channel
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        // start server listening
        while (runServer) {

            try {

                int readyChannels = selector.select();

                // no ready channels for I/O
                if (readyChannels == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {

                    SelectionKey key = keyIterator.next();

                    // check if channel reads input
                    if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    } else if (key.isAcceptable()) {
                        acceptConnection(key);
                    }

                    keyIterator.remove();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void write(SelectionKey key) throws IOException, UnsupportedAudioFileException {

        System.out.println("key is writable");

        SocketChannel socketChannel = (SocketChannel) key.channel();

        buffer.clear();

        System.out.println();

        byte[] bytes = spotifyStreamer.readMusicChunk(socketChannel);

            // reset song
            if (bytes.length == 1) {
                clearStreamingSocketChannel(socketChannel);
                key.interestOps(SelectionKey.OP_READ);
                return;
            }

            streamMusicChunk(socketChannel, bytes);
        }

    private void streamMusicChunk(SocketChannel socketChannel, byte[] bytes) throws IOException {
        buffer.put(bytes);
        buffer.flip();

        try {
            socketChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            socketChannel.close();
        }
        buffer.clear();
    }

    private void clearStreamingSocketChannel(SocketChannel socketChannel) {
        buffer.put(new byte[]{-1});
        buffer.flip();

        try {

            socketChannel.write(buffer);

        } catch (Exception e) {
            e.printStackTrace();
        }

        buffer.clear();
    }

    private void read(SelectionKey key) throws IOException {
        System.out.println("key is readable");

        SocketChannel socketChannel = (SocketChannel) key.channel();

        buffer.clear();

        int r = 0;

        try {

            r = socketChannel.read(buffer);

        } catch (SocketException e) {
            socketChannel.close();
        }

        if (r <= 0) {
            socketChannel.close();
            return;
        }

        buffer.flip();

        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);

        String userMessage = new String(byteArray, "UTF-8");

        buffer.clear();

        if (userMessage.contains("play")) {

            int songIndex = Integer.parseInt(userMessage.split("\\s+")[1]);

            spotifyStreamer.setSongForUser(socketChannel, songIndex);

            System.out.println("want to stream music. Sending music info to client");

            byte[] bytes = spotifyStreamer.getAudioFormatHeaders(socketChannel);

            buffer.put(bytes);
            buffer.flip();

            socketChannel.write(buffer);

            buffer.clear();

            // preregister key
            key.interestOps(SelectionKey.OP_WRITE);

        } else {

            byte[] serverReply = commandInterpreter.interpretCommand(userMessage, socketChannel);

            buffer.put(serverReply);
            buffer.flip();

            socketChannel.write(buffer);

            System.out.println("Server replied to client command" + userMessage);

            buffer.clear();

        }

    }

    private void acceptConnection(SelectionKey key) {

        ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();

        try {
            SocketChannel accept = socketChannel.accept();
            accept.configureBlocking(false);
            accept.register(selector, SelectionKey.OP_READ);

            System.out.println("Client connected to server");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        runServer = false;
    }

    @Override
    public void close() throws Exception {
        serverSocketChannel.close();
        selector.close();
    }
}
