package httpserver;

import java.io.*;
import java.nio.MappedByteBuffer;

/**
 * A Content type that provides for transferring files.
 *
 */
class FileContent implements Content {

    private MappedByteBuffer mbb = null;
    private String charset;
    private String extension;
    private String type = null;

    FileContent(MappedByteBuffer mbb,
                String extension, String acceptCharset) {

        this.mbb = mbb;
        this.extension = extension;
        this.charset = acceptCharset.equalsIgnoreCase("US-ASCII")
                ? acceptCharset : "UTF-8";
    }

    public String type() {
        if (type != null)
            return type;

        switch (extension) {
            case "txt":
            case "html":
                type = "text/html; charset=" + charset;
                break;
            case "js":
                type = "application/javascript; charset=" + charset;
                break;
            case "jpg":
            case "jpeg":
                type = "image/jpg";
                break;
            default:
                type = "application/octet-stream";
        }
        return type;
    }

    @Override
    public String etag() {
        return String.valueOf(mbb.hashCode());
    }

    public long length() {
        return mbb.remaining();
    }

    public void prepare() throws IOException {
        if (mbb == null)
            throw new IOException();
        mbb.rewind();
    }

    public boolean send(ChannelIO cio) throws IOException {
        if (mbb == null)
            throw new IllegalStateException();
        cio.write(mbb);
        return mbb.hasRemaining();
    }

    public void release() throws IOException {
    }
}
