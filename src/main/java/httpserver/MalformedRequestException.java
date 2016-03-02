package httpserver;

/**
 * Exception class used when a request can't be properly parsed.
 *
 */
class MalformedRequestException extends Exception {

    MalformedRequestException() { }

    MalformedRequestException(String msg) {
        super(msg);
    }

    MalformedRequestException(Exception x) {
        super(x);
    }
}
