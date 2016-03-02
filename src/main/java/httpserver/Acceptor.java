package httpserver;

import java.io.*;
import java.nio.channels.*;
import javax.net.ssl.*;

/**
 * A Runnable class which sits in a loop accepting SocketChannels,
 * then registers the Channels with the read/write Selector.
 *
 */
class Acceptor implements Runnable {

    private ServerSocketChannel ssc;
    private Dispatcher d;
    private FileContentManager frm;

    Acceptor(ServerSocketChannel ssc, Dispatcher d, FileContentManager frm) {
        this.ssc = ssc;
        this.d = d;
        this.frm = frm;
    }

    public void run() {
        for (;;) {
            try {
                SocketChannel sc = ssc.accept();
                ChannelIO cio = ChannelIO.getInstance(sc, false /* non-blocking */);
                RequestHandler rh = new RequestHandler(cio, frm);
                d.register(cio.getSocketChannel(), SelectionKey.OP_READ, rh);

            } catch (IOException x) {
                x.printStackTrace();
                break;
            }
        }
    }
}
