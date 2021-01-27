package bg.sofia.uni.fmi.mjt.spotify.Server;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

public class SpotifyServer implements AutoCloseable {

    public static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 1024;
    private final int port;
    private boolean runServer = true;

    private SpotifyCommandInterpreter commandInterpreter;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer buffer;

    private Path credentialsFile;

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

                        SocketChannel socketChannel = (SocketChannel) key.channel();

                        buffer.clear();

                        int r = 0;

                        try {

                            r = socketChannel.read(buffer);

                        } catch (SocketException e) {
                            //System.out.println("User hard reset");
                            socketChannel.close();
                            keyIterator.remove();
                            //     commandInterpreter.clearUserHardReset(socketChannel);
                        }

                        if (r <= 0) {
                            socketChannel.close();
                            break;
                        }

                        buffer.flip();

                        byte[] byteArray = new byte[buffer.remaining()];
                        buffer.get(byteArray);

                        String userMessage = new String(byteArray, "UTF-8");
                        buffer.clear(); // change to writing mode

                        String serverReply = commandInterpreter.interpretCommand(userMessage, socketChannel);

                        buffer.put(serverReply.getBytes());
                        buffer.flip();

                        socketChannel.write(buffer);

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

    private void acceptConnection(SelectionKey key) {

        ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();

        try {
            SocketChannel accept = socketChannel.accept();
            accept.configureBlocking(false);
            accept.register(selector, SelectionKey.OP_READ);

            //     channelToUsernameMap.put(accept, null);

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
