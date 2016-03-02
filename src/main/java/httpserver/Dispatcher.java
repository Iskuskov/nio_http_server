package httpserver;

import java.io.*;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * A Multi-threaded dispatcher.
 * <P>
 * Servers use these to obtain ready status, and then to dispatch jobs.
 * One thread does accepts, and the second does read/writes.
 *
 */

class Dispatcher {

    private Selector sel;

    Dispatcher() throws IOException {
        sel = Selector.open();
    }

    public void run() {
        for (;;) {
            try {
                dispatch();
            } catch (IOException x) {
                x.printStackTrace();
            }
        }
    }

    private Object gate = new Object();

    private void dispatch() throws IOException {
        sel.select();
        for (Iterator i = sel.selectedKeys().iterator(); i.hasNext(); ) {
            SelectionKey sk = (SelectionKey)i.next();
            i.remove();
            Handler h = (Handler)sk.attachment();
            h.handle(sk);
        }
        synchronized (gate) { }
    }

    public void register(SelectableChannel ch, int ops, Handler h)
            throws IOException {
        synchronized (gate) {
            sel.wakeup();
            ch.register(sel, ops, h);
        }
    }

}