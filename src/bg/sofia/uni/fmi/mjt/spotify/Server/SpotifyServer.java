package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.SpotifyStreamer;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.repositories.SpotifyClientRepository;
import bg.sofia.uni.fmi.mjt.spotify.Server.serverComponents.repositories.SpotifySongRepository;
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
import java.util.stream.Collectors;

public class SpotifyServer implements AutoCloseable {

    public static final int SERVER_PORT = 7777;
    public static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 1_024;
    private static final int STOP_SIGNAL_BYTE_SIZE = 1;

    private final int port;
    private boolean runServer = true;

    private SpotifyCommandExecutor commandInterpreter;

    private SpotifyStreamer spotifyStreamer;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer buffer;

    private Map<String, SocketChannel> userToChannel = new HashMap<>();
    private Set<SocketChannel> candidatesToStopStreaming = new HashSet<>();

    private String lastUserCommand;

//    public SpotifyServer(int port, Path credentialsFile, Path playlistFile, String musicFolderURL) {
//
//        System.out.println("Spotify server constructor :" + playlistFile);
//
//        this.port = port;
//        this.spotifyStreamer = new SpotifyStreamer(musicFolderURL);
//        this.commandInterpreter = new SpotifyCommandExecutor(credentialsFile, playlistFile);
//        initialServerConfiguration();
//
//        // allocate byte buffer
//        buffer = ByteBuffer.allocate(BUFFER_SIZE);
//    }

    public SpotifyServer(int port, SpotifyStreamer spotifyStreamer, SpotifyCommandExecutor commandInterpreter) {

//        System.out.println("Spotify server constructor :" + playlistFile);

        this.port = port;

        this.spotifyStreamer = spotifyStreamer;
        this.commandInterpreter = commandInterpreter;
        initialServerConfiguration();

        // allocate byte buffer
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }


    public static void main(String[] args) throws IOException {

        final String musicFolderURL = "D:\\4-course\\songs\\";
        final Path credentials = Path.of("credentials.json");
        final Path playlists = Path.of("playlists.json");

        final SpotifyStreamer spotifyStreamer = new SpotifyStreamer(musicFolderURL);
        final SpotifyCommandExecutor spotifyCommandExecutor = new SpotifyCommandExecutor(credentials, playlists);

        final Selector selector = Selector.open();

        try (var wishListServer = new SpotifyServer(1234, spotifyStreamer, spotifyCommandExecutor)) {
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

    private void stopStreamingToChannelCleanup(SocketChannel socketChannel) throws IOException {
        candidatesToStopStreaming.remove(socketChannel);
        writeToChannel(new byte[]{-1}, socketChannel);

        System.out.println("Stopping streaming to " + socketChannel);
        socketChannel.close();
    }

    private void write(SelectionKey key) throws IOException, UnsupportedAudioFileException {

//        System.out.println("key is writable");

        SocketChannel socketChannel = (SocketChannel) key.channel();

        if (candidatesToStopStreaming.contains(socketChannel)) {
            stopStreamingToChannelCleanup(socketChannel);
            return;
        }

        byte[] bytes = spotifyStreamer.readMusicChunk(socketChannel);

        if (bytes.length == STOP_SIGNAL_BYTE_SIZE) {
            streamingSongEndDurationCleanup(socketChannel);
            return;
        }

        writeToChannel(bytes, socketChannel);
    }

    private void streamingSongEndDurationCleanup(SocketChannel socketChannel) {
        writeToChannel(new byte[]{-1}, socketChannel);
        try {
            socketChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] readFromBuffer() {
        buffer.flip();
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        buffer.clear();
        return byteArray;
    }

    private String[] getUserSongName(String[] tokens) {
        final int PLAY_COMMAND_SONG_PARAMETERS_START = 2;

        String[] songName = new String[tokens.length - PLAY_COMMAND_SONG_PARAMETERS_START];
        int j = 0;
        for (int i = PLAY_COMMAND_SONG_PARAMETERS_START; i < tokens.length; i++) {
            songName[j++] = tokens[i];
        }
        return songName;
    }

    private boolean songExists(String[] songTokens) {
        return SpotifySongRepository.containsSong(Arrays.stream(songTokens)
                .collect(Collectors.joining(" ")));
    }


    private void prepareChannelForStreaming(String userMessage, SocketChannel socketChannel, SelectionKey key) {
        String[] tokens = userMessage.split("\\s+");

        final int PLAY_COMMAND_USER_EMAIL_INDEX = 1;
        String email = tokens[PLAY_COMMAND_USER_EMAIL_INDEX];

        String[] songName = getUserSongName(tokens);

        if (!songExists(songName)) {
            writeToChannel("No such song in Spotify".getBytes(StandardCharsets.UTF_8), socketChannel);
            return;
        }

        spotifyStreamer.setSongForChannel(socketChannel, songName);
        System.out.println("want to stream music. Sending music info to client");
        byte[] bytes = spotifyStreamer.getAudioFormatHeaders(socketChannel);

        writeToChannel(bytes, socketChannel);

        // preregister key
        key.interestOps(SelectionKey.OP_WRITE);

        userToChannel.put(email, socketChannel);

        System.out.println("Streaming socket channel : " + socketChannel);
    }


    private void prepareChannelToStopStreaming(SocketChannel socketChannel) {
        String email = SpotifyClientRepository.getEmail(socketChannel);

        System.out.println("User wants to stop streaming :" + email);

        SocketChannel streamingChannel = userToChannel.get(email);

        candidatesToStopStreaming.add(streamingChannel);

        writeToChannel("Stopping streaming in a moment ...".getBytes(StandardCharsets.UTF_8), socketChannel);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        System.out.println("Reading socket channel: " + socketChannel);
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

        byte[] responseBytes = readFromBuffer();

        String userMessage = new String(responseBytes, "UTF-8");
        lastUserCommand = userMessage;

        if (userMessage.startsWith("play")) {
            prepareChannelForStreaming(userMessage, socketChannel, key);
        } else if (userMessage.startsWith("stop")) {
            prepareChannelToStopStreaming(socketChannel);
        } else {
            byte[] serverReply = commandInterpreter.interpretCommand(userMessage, socketChannel);
            writeToChannel(serverReply, socketChannel);
            System.out.println("Server replied to client command" + userMessage);
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
