package httpserver;

import java.io.*;
import java.nio.channels.*;

/**
 * Base class for the Handlers.
 *
 */
interface Handler {

    void handle(SelectionKey sk) throws IOException;

}
