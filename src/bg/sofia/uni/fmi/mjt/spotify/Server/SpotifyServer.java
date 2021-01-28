package bg.sofia.uni.fmi.mjt.spotify.Server;

import bg.sofia.uni.fmi.mjt.spotify.Server.dto.AudioFormatDTO;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
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
import java.util.*;

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

    private Map<SocketChannel, Long> songCurrentBytesMap = new HashMap<>();

    private Set<SocketChannel> streamingClients = new HashSet<>();


    private Map<SocketChannel, Integer> userToSongMap = new HashMap<>();

    private Map<Integer, String> songsMap = new HashMap<>();

    public SpotifyServer(int port, Path credentialsFile) {

        songsMap.putAll(Map.of(
                1, "Ice Cream - Захир (HD)",
                2, "Iggy Azalea - Black Widow ft. Rita Ora",
                3, "Iggy Azalea - Fancy ft. Charli XCX",
                4, "INNA - Take Me Higher (by Play&amp;Win) [Online Video]",
                5, "Inna feat. Marian Hill - Diggy Down",
                6, "Jason Derulo - &quot;Talk Dirty&quot; feat. 2Chainz (Official HD Music Video)",
                7, "Jason Derulo - Wiggle feat. Snoop Dogg (Official HD Music Video)",
                8, "Jay Z ft. Kanye West - Niggas in Paris (Official music video)"));

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

        if (!streamingClients.contains(socketChannel)) {
            return;
        }

        buffer.clear();

        songCurrentBytesMap.putIfAbsent(socketChannel, 0L);

        System.out.println();

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(
                new File("../songs/" + songsMap.get(userToSongMap.get(socketChannel))
                         + ".wav"))) {

            long currentPositionInBytes = songCurrentBytesMap.get(socketChannel);

            System.out.println("current position in bytes: " + currentPositionInBytes);


            byte[] bytes = new byte[BUFFER_SIZE];

            long skipped = stream.skip(currentPositionInBytes);

            System.out.println("skipped: " + skipped);

            int r = stream.read(bytes);

            int availableBytes = r < BUFFER_SIZE ? r : BUFFER_SIZE;

            System.out.println("Stream available bytes: " + availableBytes);

            songCurrentBytesMap.put(socketChannel, currentPositionInBytes + availableBytes);

            buffer.put(bytes);
            buffer.flip();
            socketChannel.write(buffer);

            buffer.clear();

            // reset song
            if (r == -1) {
                songCurrentBytesMap.put(socketChannel, 0L);
                streamingClients.remove(socketChannel);
                userToSongMap.remove(socketChannel);
                key.interestOps(SelectionKey.OP_READ);
            }
        }

    }

    private void read(SelectionKey key) throws IOException {
        System.out.println("key is readable");

        SocketChannel socketChannel = (SocketChannel) key.channel();

        buffer.clear();

        int r = 0;

        try {

            r = socketChannel.read(buffer);

        } catch (SocketException e) {
            //System.out.println("User hard reset");
            socketChannel.close();
            //     commandInterpreter.clearUserHardReset(socketChannel);
        }

        if (r <= 0) {
            socketChannel.close();
            return;
        }

        buffer.flip();

        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);

        String userMessage = new String(byteArray, "UTF-8");

        buffer.clear(); // change to writing mode

        //TODO fix desing stream music
        if (userMessage.contains("play")) {

            int songIndex = Integer.parseInt(userMessage.split("\\s+")[1]);

            userToSongMap.put(socketChannel, songIndex);

            System.out.println("want to stream music. Sending music info to client");

            byte[] bytes = playSong(socketChannel);

            buffer.put(bytes);
            buffer.flip();

            socketChannel.write(buffer);

            buffer.clear();

            streamingClients.add(socketChannel);

            userToSongMap.put(socketChannel, Integer.parseInt(userMessage.split("\\s+")[1]));

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

    private byte[] objectToByteArray(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);

            System.out.println(object.toString());

            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] playSong(SocketChannel userSocketChannel) {


        try {


            System.out.println("../songs/" + songsMap.get(userToSongMap.get(userSocketChannel))
                               + ".wav");

            AudioFormat format = AudioSystem.getAudioInputStream(new File("../songs/" +
                                                                          songsMap.get(userToSongMap.get(userSocketChannel))
                                                                          + ".wav")).getFormat();


            long songSizeInBytes = Files.size(Path.of("../songs/" + songsMap.get(userToSongMap.get(userSocketChannel))
                                                      + ".wav"));


            AudioFormatDTO dto = new AudioFormatDTO(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(),
                    format.getChannels(), format.getFrameSize(), format.getFrameRate(), format.isBigEndian(), songSizeInBytes);

            System.out.println(dto.toString());

            return objectToByteArray(dto);
        } catch (IOException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }

        return null;
    }


    private void acceptConnection(SelectionKey key) {

        ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();

        try {
            SocketChannel accept = socketChannel.accept();
            accept.configureBlocking(false);
            accept.register(selector, SelectionKey.OP_READ);

            //     channelToUsernameMap.put(accept, null);

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
