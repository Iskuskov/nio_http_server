package httpserver;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.*;

/**
 * An encapsulation of the request received.
 * <P>
 * The static method parse() is responsible for creating this
 * object.
 *
 */
class Request {

    /**
     * A helper class for parsing HTTP command actions.
     */
    static class Action {

        private String name;
        private Action(String name) { this.name = name; }
        public String toString() { return name; }

        static Action GET = new Action("GET");
        static Action PUT = new Action("PUT");
        static Action POST = new Action("POST");
        static Action HEAD = new Action("HEAD");

        static Action parse(String s) {
            if (s.equals("GET"))
                return GET;
            if (s.equals("PUT"))
                return PUT;
            if (s.equals("POST"))
                return POST;
            if (s.equals("HEAD"))
                return HEAD;
            throw new IllegalArgumentException(s);
        }
    }

    private Action action;
    private String version;
    private URI uri;
    private Hashtable headers;

    private String acceptCharset;
    private boolean ifNoneMatch;
    private String eTag;

    Action action() { return action; }
    String version() { return version; }
    URI uri() { return uri; }
    String acceptCharset() { return acceptCharset; }
    boolean ifNoneMatch() { return ifNoneMatch; }
    String eTag() { return eTag; }

    private Request(Action a, String v, URI u, String ac, String et) {
        action = a;
        version = v;
        uri = u;
        acceptCharset = ac;
        eTag = et;
        ifNoneMatch = (!eTag.isEmpty());
    }

    public String toString() {
        return (action + " " + version + " " + uri);
    }

    static boolean isComplete(ByteBuffer bb) {
        int p = bb.position() - 4;
        if (p < 0)
            return false;
        return (((bb.get(p + 0) == '\r') &&
                 (bb.get(p + 1) == '\n') &&
                 (bb.get(p + 2) == '\r') &&
                 (bb.get(p + 3) == '\n')));
    }

    private static Charset ascii = Charset.forName("US-ASCII");

    /*
     * The expected message format is first compiled into a pattern,
     * and is then compared against the inbound character buffer to
     * determine if there is a match.  This convienently tokenizes
     * our request into usable pieces.
     *
     * This uses Matcher "expression capture groups" to tokenize
     * requests like:
     *
     *     GET /dir/file HTTP/1.1
     *     Host: hostname
     *
     * into:
     *
     *     group[1] = "GET"
     *     group[2] = "/dir/file"
     *     group[3] = "1.1"
     *     group[4] = <headers lines>
     *     group[5] = "hostname"
     *
     * The text in between the parens are used to captured the regexp text.
     */
    private static Pattern requestPattern
        = Pattern.compile("\\A([A-Z]+) +([^ ]+) +HTTP/([0-9\\.]+)$"
                          + "(.*^Host: ([^ ]+)$.*)\r\n\r\n\\z",
                          Pattern.MULTILINE | Pattern.DOTALL);

    static Request parse(ByteBuffer bb) throws MalformedRequestException {

        CharBuffer cb = ascii.decode(bb);
        Matcher m = requestPattern.matcher(cb);
        if (!m.matches())
            throw new MalformedRequestException();

        Action a;
        try {
            a = Action.parse(m.group(1));
        } catch (IllegalArgumentException x) {
            throw new MalformedRequestException();
        }

        URI u;
        try {
            u = new URI("http://"
                        + m.group(5)
                        + m.group(2));
        } catch (URISyntaxException x) {
            throw new MalformedRequestException();
        }

        // Parse headers
        String headers = m.group(4);
        String[] headersLines = headers.split(System.getProperty("line.separator"));
        Hashtable<String, String> headerTable = new Hashtable();
        for (String header: headersLines) {
            int idx = header.indexOf(':');
            if (idx > 0) {
                headerTable.put(header.substring(0, idx).toLowerCase(),
                        header.substring(idx + 1).trim());
            }
        }

        // headerTable keys are in lowercase
        String ac = headerTable.get("accept-charset");
        String et = "";
        if (headerTable.containsKey("if-none-match")) {
            //ETag should no contain " symbol
            et = headerTable.get("if-none-match").replace("\"","");
        }

        return new Request(a, m.group(3), u, ac, et);
    }
}
