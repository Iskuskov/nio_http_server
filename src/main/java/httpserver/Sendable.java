package httpserver;

import java.io.*;

/**
 * Method definitions used for preparing, sending, and release
 * content.
 *
 */
interface Sendable {

    void prepare() throws IOException;

    // Sends (some) content to the given channel.
    // Returns true if more bytes remain to be written.
    // Throws IllegalStateException if not prepared.
    //
    boolean send(ChannelIO cio) throws IOException;

    void release() throws IOException;
}
