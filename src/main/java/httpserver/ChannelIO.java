package httpserver;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * A helper class for properly sizing inbound byte buffers and
 * redirecting I/O calls to the proper SocketChannel call.
 *
 */
class ChannelIO {

    protected SocketChannel sc;

    /*
     * All of the inbound request data lives here until we determine
     * that we've read everything, then we pass that data back to the
     * caller.
     */
    protected ByteBuffer requestBB;
    static private int requestBBSize = 4096;

    protected ChannelIO(SocketChannel sc, boolean blocking)
            throws IOException {
        this.sc = sc;
        sc.configureBlocking(blocking);
    }

    static ChannelIO getInstance(SocketChannel sc, boolean blocking)
            throws IOException {
        ChannelIO cio = new ChannelIO(sc, blocking);
        cio.requestBB = ByteBuffer.allocate(requestBBSize);

        return cio;
    }

    SocketChannel getSocketChannel() {
        return sc;
    }

    /*
     * Return a ByteBuffer with "remaining" space to work.  If you have to
     * reallocate the ByteBuffer, copy the existing info into the new buffer.
     */
    protected void resizeRequestBB(int remaining) {
        if (requestBB.remaining() < remaining) {
            // Expand buffer for large request
            ByteBuffer bb = ByteBuffer.allocate(requestBB.capacity() * 2);
            requestBB.flip();
            bb.put(requestBB);
            requestBB = bb;
        }
    }

    /*
     * Perform any handshaking processing.
     * <P>
     * This variant is for Servers without SelectionKeys (e.g.
     * blocking).
     * <P>
     * return true when we're done with handshaking.
     */
    boolean doHandshake() throws IOException {
        return true;
    }

    /*
     * Perform any handshaking processing.
     * <P>
     * This variant is for Servers with SelectionKeys, so that
     * we can register for selectable operations (e.g. selectable
     * non-blocking).
     * <P>
     * return true when we're done with handshaking.
     */
    boolean doHandshake(SelectionKey sk) throws IOException {
        return true;
    }

    /*
     * Resize (if necessary) the inbound data buffer, and then read more
     * data into the read buffer.
     */
    int read() throws IOException {
        /*
         * Allocate more space if less than 5% remains
         */
        resizeRequestBB(requestBBSize/20);
        return sc.read(requestBB);
    }

    /*
     * All data has been read, pass back the request in one buffer.
     */
    ByteBuffer getReadBuf() {
        return requestBB;
    }

    /*
     * Write the src buffer into the socket channel.
     */
    int write(ByteBuffer src) throws IOException {
        return sc.write(src);
    }

    /*
     * Perform a FileChannel.TransferTo on the socket channel.
     */
    long transferTo(FileChannel fc, long pos, long len) throws IOException {
        return fc.transferTo(pos, len, sc);
    }

    /*
     * Flush any outstanding data to the network if possible.
     * <P>
     * This isn't really necessary for the insecure variant, but needed
     * for the secure one where intermediate buffering must take place.
     * <P>
     * Return true if successful.
     */
    boolean dataFlush() throws IOException {
        return true;
    }

    /*
     * Start any connection shutdown processing.
     * <P>
     * This isn't really necessary for the insecure variant, but needed
     * for the secure one where intermediate buffering must take place.
     * <P>
     * Return true if successful, and the data has been flushed.
     */
    boolean shutdown() throws IOException {
        return true;
    }

    /*
     * Close the underlying connection.
     */
    void close() throws IOException {
        sc.close();
    }

}
