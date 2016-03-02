package httpserver;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * The main server class.
 *
 */
public class Server {

    ServerSocketChannel ssc;
    FileContentManager frm;

    static private int PORT = 8000;
    static private int BACKLOG = 1024;
    static private boolean CACHING = true;
    static private String PROPERTIES_FILENAME = "src/main/resources/config.properties";

    // TODO: 28.02.2016: fix root dir
    private static String ROOT = "root";

    Server(int port, int backlog, boolean caching) throws Exception {

        ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        ssc.socket().bind(new InetSocketAddress(port), backlog);

        frm = new FileContentManager(Paths.get(ROOT), caching);
    }

    void runServer() throws Exception {
        Dispatcher d = new Dispatcher();
        Acceptor a = new Acceptor(ssc, d, frm);
        new Thread(a).start();
        d.run();
    }

    static private void usage() {
        System.out.println("Usage:  httpserver.jar [path_to_config]\n");
        System.exit(1);
    }

    /*
     * Parse the arguments
     */
    static private Server createServer(String args[]) throws Exception {

        int port = PORT;
        int backlog = BACKLOG;
        boolean caching = CACHING;

        String fisName = (args.length > 0) ? args[0] : PROPERTIES_FILENAME;
        FileInputStream fis;
        Properties property = new Properties();

        try {
            fis = new FileInputStream(fisName);
            property.load(fis);

            port = Integer.parseInt(property.getProperty("port"));
            backlog = Integer.parseInt(property.getProperty("backlog"));
            caching = Boolean.parseBoolean(property.getProperty("caching"));

        } catch (IOException e) {
            usage();
        }

        Server server = new Server(port, backlog, caching);
        return server;
    }

    static public void main(String args[]) throws Exception {
        Server server = createServer(args);

        if (server == null) {
            usage();
        }

        System.out.println("Server started.");
        server.runServer();
    }
}
