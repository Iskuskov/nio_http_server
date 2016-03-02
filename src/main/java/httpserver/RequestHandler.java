package httpserver;

import org.apache.commons.io.FilenameUtils;
import java.io.*;
import java.net.URI;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;

/**
 * Primary driver class used by non-blocking Servers to receive,
 * prepare, send, and shutdown requests.
 *
 */
class RequestHandler implements Handler {

    private ChannelIO cio;
    FileContentManager frm;
    private ByteBuffer rbb = null;

    private boolean requestReceived = false;
    private Request request = null;
    private Reply reply = null;

    private static int created = 0;

    RequestHandler(ChannelIO cio, FileContentManager frm) {
        this.cio = cio;
        this.frm = frm;

        // Simple heartbeat to let user know we're alive.
        synchronized (RequestHandler.class) {
            created++;
            if ((created % 50) == 0) {
                System.out.println(".");
                created = 0;
            } else {
                System.out.print(".");
            }
        }
    }

    // Returns true when request is complete
    // May expand rbb if more room required
    //
    private boolean receive(SelectionKey sk) throws IOException {
        ByteBuffer tmp = null;

        if (requestReceived) {
            return true;
        }

        if (!cio.doHandshake(sk)) {
            return false;
        }

        if ((cio.read() < 0) || Request.isComplete(cio.getReadBuf())) {
            rbb = cio.getReadBuf();
            return (requestReceived = true);
        }
        return false;
    }

    // When parse is successfull, saves request and returns true
    //
    private boolean parse() throws IOException {
        try {
            request = Request.parse(rbb);
            return true;
        } catch (MalformedRequestException x) {
            reply = new Reply(Reply.Code.BAD_REQUEST,
                              new StringContent(x));
        }
        return false;
    }

    // Ensures that reply field is non-null
    //
    private void build() throws IOException {

        Request.Action action = request.action();
        if ((action != Request.Action.GET)) {
            reply = new Reply(Reply.Code.METHOD_NOT_ALLOWED,
                              new StringContent(request.toString()));
            return;
        }

        URI requestUri = request.uri();
        String acceptCharsetName = request.acceptCharset() != null
                ? request.acceptCharset() : "UTF-8";;
        String charsetName = acceptCharsetName.equalsIgnoreCase("US-ASCII")
                ? acceptCharsetName : "UTF-8";
        Charset charset = Charset.forName(charsetName.toUpperCase());

        String contentPath = requestUri.getPath().replace('/', File.separatorChar);
        MappedByteBuffer mbb;
        try {
            mbb = frm.getFileContent(contentPath);
        } catch (IOException x) {
            reply = new Reply(Reply.Code.NOT_FOUND,
                    new StringContent(x));
            return;
        }
        String extension = FilenameUtils.getExtension(contentPath);
        reply = new Reply(Reply.Code.OK,
                new FileContent(mbb, extension, charsetName),
                charset, action);

        // Etag handle
        if (request.ifNoneMatch()) {
            String actualETag = reply.etag();
            String requestETag = request.eTag();
            if (actualETag.equals(requestETag)) {
                reply = new Reply(Reply.Code.NOT_MODIFIED,
                        new StringContent(request.toString()));
            }
        }
    }

    public void handle(SelectionKey sk) throws IOException {
        try {

            if (request == null) {
                if (!receive(sk))
                    return;
                rbb.flip();
                if (parse()) {
                    build();
                }
                try {
                    reply.prepare();
                } catch (IOException x) {
                    reply.release();
                    reply = new Reply(Reply.Code.NOT_FOUND,
                                      new StringContent(x));
                    reply.prepare();
                }
                if (send()) {
                    // More bytes remain to be written
                    sk.interestOps(SelectionKey.OP_WRITE);
                } else {
                    // Reply completely written; we're done
                    if (cio.shutdown()) {
                        cio.close();
                        reply.release();
                    }
                }
            } else {
                if (!send()) {  // Should be rp.send()
                    if (cio.shutdown()) {
                        cio.close();
                        reply.release();
                    }
                }
            }
        } catch (IOException x) {
            String m = x.getMessage();
            if (!m.equals("Broken pipe") &&
                    !m.equals("Connection reset by peer")) {
                System.err.println("RequestHandler: " + x.toString());
            }

            try {
                /*
                 * We had a failure here, so we'll try to be nice
                 * before closing down and send off a close_notify,
                 * but if we can't get the message off with one try,
                 * we'll just shutdown.
                 */
                cio.shutdown();
            } catch (IOException e) {
                // ignore
            }

            cio.close();
            if (reply !=  null) {
                reply.release();
            }
        }

    }

    private boolean send() throws IOException {
        try {
            return reply.send(cio);
        } catch (IOException x) {
            if (x.getMessage().startsWith("Resource temporarily")) {
                System.err.println("## RTA");
                return true;
            }
            throw x;
        }
    }
}
