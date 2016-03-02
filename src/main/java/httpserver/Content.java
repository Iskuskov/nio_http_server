package httpserver;

/**
 * An Sendable interface extension that adds additional
 * methods for additional information, such as Files
 * or Strings.
 *
 */
interface Content extends Sendable {

    String type();
    String etag();

    // Returns -1 until prepare() invoked
    long length();

}
