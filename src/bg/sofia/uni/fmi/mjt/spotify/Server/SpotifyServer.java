package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.repositories.SpotifyClientRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyStreamer;
import bg.sofia.uni.fmi.mjt.spotify.serverException.ServerStartupException;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class SpotifyServer implements AutoCloseable {

    public static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 1_024;
    private final int port;
    private boolean runServer = true;

    private SpotifyCommandExecutor commandInterpreter;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer buffer;

    private SpotifyStreamer spotifyStreamer;

    private Map<String, SocketChannel> streamingUsersMap = new HashMap<>();

    private Set<SocketChannel> stopStreaming = new HashSet<>();

    public SpotifyServer(int port, Path credentialsFile, Path playlistFile, String musicFolderURL) {

        System.out.println("Spotify server constructor :" + playlistFile);

        this.port = port;

        this.spotifyStreamer = new SpotifyStreamer(musicFolderURL);

        this.commandInterpreter = new SpotifyCommandExecutor(credentialsFile, playlistFile);

        initialServerConfiguration();

        // allocate byte buffer
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    public static void main(String[] args) {

        final String musicFolderURL = "D:\\4-course\\songs\\";
        final Path credentials = Path.of("credentials.json");
        final Path playlists = Path.of("playlists.json");

        try (var wishListServer = new SpotifyServer(1234, credentials, playlists, musicFolderURL)) {
            wishListServer.start();

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServerStartupException("Could not initialize server with these arguments. Please validate arguments.");
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


    private void writeToChannel(byte[] bytes, SocketChannel channel) {
        buffer.clear();
        buffer.put(bytes);
        buffer.flip();

        try {
            channel.write(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        buffer.clear();
    }

    private void write(SelectionKey key) throws IOException, UnsupportedAudioFileException {

//        System.out.println("key is writable");

        SocketChannel socketChannel = (SocketChannel) key.channel();

        if (stopStreaming.contains(socketChannel)) {

            stopStreaming.remove(socketChannel);
            writeToChannel(new byte[]{-1}, socketChannel);

            System.out.println("Stopping streaming to " + socketChannel);

            socketChannel.close();
            return;
        }

        buffer.clear();
        byte[] bytes = spotifyStreamer.readMusicChunk(socketChannel);

        // reset song
        if (bytes.length == 1) {
            clearStreamingSocketChannel(socketChannel);
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
            socketChannel.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        buffer.clear();
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        System.out.println("Reading socket channel: " + socketChannel);

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

        if (userMessage.startsWith("play")) {

            String[] tokens = userMessage.split("\\s+");

            String email = tokens[1];

            String[] songName = new String[tokens.length - 2];
            int j = 0;
            for (int i = 2; i < tokens.length; i++) {
                songName[j++] = tokens[i];
            }

            spotifyStreamer.setSongForUser(socketChannel, songName);

            System.out.println("want to stream music. Sending music info to client");

            byte[] bytes = spotifyStreamer.getAudioFormatHeaders(socketChannel);

            buffer.put(bytes);
            buffer.flip();

            socketChannel.write(buffer);

            buffer.clear();

            // preregister key
            key.interestOps(SelectionKey.OP_WRITE);
            streamingUsersMap.put(email, socketChannel);

            System.out.println("Streaming socket channel : " + socketChannel);


        } else if (userMessage.startsWith("stop")) {

            String email = SpotifyClientRepository.getEmail(socketChannel);

            System.out.println("User wants to stop streaming :" + email);

            SocketChannel streamingChannel = streamingUsersMap.get(email);

            stopStreaming.add(streamingChannel);

            writeToChannel("Stopping streaming in a moment ...".getBytes(StandardCharsets.UTF_8), socketChannel);


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
