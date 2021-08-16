/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 *
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import sun.net.www.MessageHeader;

public class TunnelProxy {

    ServerSocketChannel schan;
    int threads;
    int cperthread;
    Server[] servers;

    /**
     * Create a <code>TunnelProxy<code> instance with the specified callback object
     * for handling requests. One thread is created to handle requests,
     * and up to ten TCP connections will be handled simultaneously.
     *  incoming request
     */

    public TunnelProxy () throws IOException {
        this (1, 10, 0);
    }

    /**
     * Create a <code>TunnelProxy<code> instance with the specified number of
     * threads and maximum number of connections per thread. This functions
     * the same as the 4 arg constructor, where the port argument is set to zero.
     * @param threads the number of threads to create to handle requests
     *     in parallel
     * @param cperthread the number of simultaneous TCP connections to
     *     handle per thread
     */

    public TunnelProxy (int threads, int cperthread)
        throws IOException {
        this (threads, cperthread, 0);
    }

    /**
     * Create a <code>TunnelProxy<code> instance with the specified number
     * of threads and maximum number of connections per thread and running on
     * the specified port. The specified number of threads are created to
     * handle incoming requests, and each thread is allowed
     * to handle a number of simultaneous TCP connections.
     * @param threads the number of threads to create to handle
     *  requests in parallel
     * @param cperthread the number of simultaneous TCP connections
     *  to handle per thread
     * @param port the port number to bind the server to. <code>Zero</code>
     *  means choose any free port.
     */

    public TunnelProxy (int threads, int cperthread, int port)
        throws IOException {
        this(threads, cperthread, null, 0);
    }

    /**
     * Create a <code>TunnelProxy<code> instance with the specified number
     * of threads and maximum number of connections per thread and running on
     * the specified port. The specified number of threads are created to
     * handle incoming requests, and each thread is allowed
     * to handle a number of simultaneous TCP connections.
     * @param threads the number of threads to create to handle
     *  requests in parallel
     * @param cperthread the number of simultaneous TCP connections
     *  to handle per thread
     * @param address the address to bind to. null means all addresses.
     * @param port the port number to bind the server to. <code>Zero</code>
     *  means choose any free port.
     */
    public TunnelProxy (int threads, int cperthread, InetAddress address, int port)
        throws IOException {
        schan = ServerSocketChannel.open ();
        InetSocketAddress addr = new InetSocketAddress (address, port);
        schan.socket().bind (addr);
        this.threads = threads;
        this.cperthread = cperthread;
        servers = new Server [threads];
        for (int i=0; i<threads; i++) {
            servers[i] = new Server (schan, cperthread);
            servers[i].start();
        }
    }

    /** Tell all threads in the server to exit within 5 seconds.
     *  This is an abortive termination. Just prior to the thread exiting
     *  all channels in that thread waiting to be closed are forceably closed.
     */

    public void terminate () {
        for (int i=0; i<threads; i++) {
            servers[i].terminate ();
        }
    }

    /**
     * return the local port number to which the server is bound.
     * @return the local port number
     */

    public int getLocalPort () {
        return schan.socket().getLocalPort ();
    }

    static class Server extends Thread {

        ServerSocketChannel schan;
        Selector selector;
        SelectionKey listenerKey;
        SelectionKey key; /* the current key being processed */
        ByteBuffer consumeBuffer;
        int maxconn;
        int nconn;
        ClosedChannelList clist;
        boolean shutdown;
        Pipeline pipe1 = null;
        Pipeline pipe2 = null;

        Server (ServerSocketChannel schan, int maxconn) {
            this.schan = schan;
            this.maxconn = maxconn;
            nconn = 0;
            consumeBuffer = ByteBuffer.allocate (512);
            clist = new ClosedChannelList ();
            try {
                selector = Selector.open ();
                schan.configureBlocking (false);
                listenerKey = schan.register (selector, SelectionKey.OP_ACCEPT);
            } catch (IOException e) {
                System.err.println ("Server could not start: " + e);
            }
        }

        /* Stop the thread as soon as possible */
        public synchronized void terminate () {
            shutdown = true;
            if (pipe1 != null) pipe1.terminate();
            if (pipe2 != null) pipe2.terminate();
        }

        public void run ()  {
            try {
                while (true) {
                    selector.select (1000);
                    Set selected = selector.selectedKeys();
                    Iterator iter = selected.iterator();
                    while (iter.hasNext()) {
                        key = (SelectionKey)iter.next();
                        if (key.equals (listenerKey)) {
                            SocketChannel sock = schan.accept ();
                            if (sock == null) {
                                /* false notification */
                                iter.remove();
                                continue;
                            }
                            sock.configureBlocking (false);
                            sock.register (selector, SelectionKey.OP_READ);
                            nconn ++;
                            if (nconn == maxconn) {
                                /* deregister */
                                listenerKey.cancel ();
                                listenerKey = null;
                            }
                        } else {
                            if (key.isReadable()) {
                                boolean closed;
                                SocketChannel chan = (SocketChannel) key.channel();
                                if (key.attachment() != null) {
                                    closed = consume (chan);
                                } else {
                                    closed = read (chan, key);
                                }
                                if (closed) {
                                    chan.close ();
                                    key.cancel ();
                                    if (nconn == maxconn) {
                                        listenerKey = schan.register (selector, SelectionKey.OP_ACCEPT);
                                    }
                                    nconn --;
                                }
                            }
                        }
                        iter.remove();
                    }
                    clist.check();
                    if (shutdown) {
                        clist.terminate ();
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println ("Server exception: " + e);
                // TODO finish
            }
        }

        /* read all the data off the channel without looking at it
             * return true if connection closed
             */
        boolean consume (SocketChannel chan) {
            try {
                consumeBuffer.clear ();
                int c = chan.read (consumeBuffer);
                if (c == -1)
                    return true;
            } catch (IOException e) {
                return true;
            }
            return false;
        }

        /* return true if the connection is closed, false otherwise */

        private boolean read (SocketChannel chan, SelectionKey key) {
            boolean res;
            try {
                InputStream is = new BufferedInputStream (new NioInputStream (chan));
                String requestline = readLine (is);
                MessageHeader mhead = new MessageHeader (is);
                String[] req = requestline.split (" ");
                if (req.length < 2) {
                    /* invalid request line */
                    return false;
                }
                String cmd = req[0];
                URI uri = null;
                if (!("CONNECT".equalsIgnoreCase(cmd))) {
                    // we expect CONNECT command
                    return false;
                }
                try {
                    uri = new URI("http://" + req[1]);
                } catch (URISyntaxException e) {
                    System.err.println ("Invalid URI: " + e);
                    res = true;
                }

                // CONNECT ack
                OutputStream os = new BufferedOutputStream(new NioOutputStream(chan));
                byte[] ack = "HTTP/1.1 200 Connection established\r\n\r\n".getBytes();
                os.write(ack, 0, ack.length);
                os.flush();

                // tunnel anything else
                tunnel(is, os, uri);

                res = false;
            } catch (IOException e) {
                res = true;
            }
            return res;
        }

        private void tunnel(InputStream fromClient, OutputStream toClient, URI serverURI) throws IOException {
            Socket sockToServer = new Socket(serverURI.getHost(), serverURI.getPort());
            OutputStream toServer = sockToServer.getOutputStream();
            InputStream fromServer = sockToServer.getInputStream();

            pipe1 = new Pipeline(fromClient, toServer);
            pipe2 = new Pipeline(fromServer, toClient);
            // start pump
            pipe1.start();
            pipe2.start();
            // wait them to end
            try {
                pipe1.join();
            } catch (InterruptedException e) {
                // No-op
            } finally {
                sockToServer.close();
            }
        }

        private String readLine (InputStream is) throws IOException {
            boolean done=false, readCR=false;
            byte[] b = new byte [512];
            int c, l = 0;

            while (!done) {
                c = is.read ();
                if (c == '\n' && readCR) {
                    done = true;
                } else {
                    if (c == '\r' && !readCR) {
                        readCR = true;
                    } else {
                        b[l++] = (byte)c;
                    }
                }
            }
            return new String (b);
        }

        /** close the channel associated with the current key by:
         * 1. shutdownOutput (send a FIN)
         * 2. mark the key so that incoming data is to be consumed and discarded
         * 3. After a period, close the socket
         */

        synchronized void orderlyCloseChannel (SelectionKey key) throws IOException {
            SocketChannel ch = (SocketChannel)key.channel ();
            ch.socket().shutdownOutput();
            key.attach (this);
            clist.add (key);
        }

        synchronized void abortiveCloseChannel (SelectionKey key) throws IOException {
            SocketChannel ch = (SocketChannel)key.channel ();
            Socket s = ch.socket ();
            s.setSoLinger (true, 0);
            ch.close();
        }
    }


    /**
     * Implements blocking reading semantics on top of a non-blocking channel
     */

    static class NioInputStream extends InputStream {
        SocketChannel channel;
        Selector selector;
        ByteBuffer chanbuf;
        SelectionKey key;
        int available;
        byte[] one;
        boolean closed;
        ByteBuffer markBuf; /* reads may be satisifed from this buffer */
        boolean marked;
        boolean reset;
        int readlimit;

        public NioInputStream (SocketChannel chan) throws IOException {
            this.channel = chan;
            selector = Selector.open();
            chanbuf = ByteBuffer.allocate (1024);
            key = chan.register (selector, SelectionKey.OP_READ);
            available = 0;
            one = new byte[1];
            closed = marked = reset = false;
        }

        public synchronized int read (byte[] b) throws IOException {
            return read (b, 0, b.length);
        }

        public synchronized int read () throws IOException {
            return read (one, 0, 1);
        }

        public synchronized int read (byte[] b, int off, int srclen) throws IOException {

            int canreturn, willreturn;

            if (closed)
                return -1;

            if (reset) { /* satisfy from markBuf */
                canreturn = markBuf.remaining ();
                willreturn = canreturn>srclen ? srclen : canreturn;
                markBuf.get(b, off, willreturn);
                if (canreturn == willreturn) {
                    reset = false;
                }
            } else { /* satisfy from channel */
                canreturn = available();
                if (canreturn == 0) {
                    block ();
                    canreturn = available();
                }
                willreturn = canreturn>srclen ? srclen : canreturn;
                chanbuf.get(b, off, willreturn);
                available -= willreturn;

                if (marked) { /* copy into markBuf */
                    try {
                        markBuf.put (b, off, willreturn);
                    } catch (BufferOverflowException e) {
                        marked = false;
                    }
                }
            }
            return willreturn;
        }

        public synchronized int available () throws IOException {
            if (closed)
                throw new IOException ("Stream is closed");

            if (reset)
                return markBuf.remaining();

            if (available > 0)
                return available;

            chanbuf.clear ();
            available = channel.read (chanbuf);
            if (available > 0)
                chanbuf.flip();
            else if (available == -1)
                throw new IOException ("Stream is closed");
            return available;
        }

        /**
         * block() only called when available==0 and buf is empty
         */
        private synchronized void block () throws IOException {
            //assert available == 0;
            int n = selector.select ();
            //assert n == 1;
            selector.selectedKeys().clear();
            available ();
        }

        public void close () throws IOException {
            if (closed)
                return;
            channel.close ();
            closed = true;
        }

        public synchronized void mark (int readlimit) {
            if (closed)
                return;
            this.readlimit = readlimit;
            markBuf = ByteBuffer.allocate (readlimit);
            marked = true;
            reset = false;
        }

        public synchronized void reset () throws IOException {
            if (closed )
                return;
            if (!marked)
                throw new IOException ("Stream not marked");
            marked = false;
            reset = true;
            markBuf.flip ();
        }
    }

    static class NioOutputStream extends OutputStream {
        SocketChannel channel;
        ByteBuffer buf;
        SelectionKey key;
        Selector selector;
        boolean closed;
        byte[] one;

        public NioOutputStream (SocketChannel channel) throws IOException {
            this.channel = channel;
            selector = Selector.open ();
            key = channel.register (selector, SelectionKey.OP_WRITE);
            closed = false;
            one = new byte [1];
        }

        public synchronized void write (int b) throws IOException {
            one[0] = (byte)b;
            write (one, 0, 1);
        }

        public synchronized void write (byte[] b) throws IOException {
            write (b, 0, b.length);
        }

        public synchronized void write (byte[] b, int off, int len) throws IOException {
            if (closed)
                throw new IOException ("stream is closed");

            buf = ByteBuffer.allocate (len);
            buf.put (b, off, len);
            buf.flip ();
            int n;
            while ((n = channel.write (buf)) < len) {
                len -= n;
                if (len == 0)
                    return;
                selector.select ();
                selector.selectedKeys().clear ();
            }
        }

        public void close () throws IOException {
            if (closed)
                return;
            channel.close ();
            closed = true;
        }
    }

    /*
     * Pipeline object :-
     * 1) Will pump every byte from its input stream to output stream
     * 2) Is an 'active object'
     */
    static class Pipeline implements Runnable {
        InputStream in;
        OutputStream out;
        Thread t;

        public Pipeline(InputStream is, OutputStream os) {
            in = is;
            out = os;
        }

        public void start() {
            t = new Thread(this);
            t.start();
        }

        public void join() throws InterruptedException {
            t.join();
        }

        public void terminate() {
            t.interrupt();
        }

        public void run() {
            byte[] buffer = new byte[10000];
            try {
                while (!Thread.interrupted()) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                        out.flush();
                    }
                }
            } catch(IOException e) {
                // No-op
            } finally {
            }
        }
    }

    /**
     * Utilities for synchronization. A condition is
     * identified by a string name, and is initialized
     * upon first use (ie. setCondition() or waitForCondition()). Threads
     * are blocked until some thread calls (or has called) setCondition() for the same
     * condition.
     * <P>
     * A rendezvous built on a condition is also provided for synchronizing
     * N threads.
     */

    private static HashMap conditions = new HashMap();

    /*
     * Modifiable boolean object
     */
    private static class BValue {
        boolean v;
    }

    /*
     * Modifiable int object
     */
    private static class IValue {
        int v;
        IValue (int i) {
            v =i;
        }
    }


    private static BValue getCond (String condition) {
        synchronized (conditions) {
            BValue cond = (BValue) conditions.get (condition);
            if (cond == null) {
                cond = new BValue();
                conditions.put (condition, cond);
            }
            return cond;
        }
    }

    /**
     * Set the condition to true. Any threads that are currently blocked
     * waiting on the condition, will be unblocked and allowed to continue.
     * Threads that subsequently call waitForCondition() will not block.
     * If the named condition did not exist prior to the call, then it is created
     * first.
     */

    public static void setCondition (String condition) {
        BValue cond = getCond (condition);
        synchronized (cond) {
            if (cond.v) {
                return;
            }
            cond.v = true;
            cond.notifyAll();
        }
    }

    /**
     * If the named condition does not exist, then it is created and initialized
     * to false. If the condition exists or has just been created and its value
     * is false, then the thread blocks until another thread sets the condition.
     * If the condition exists and is already set to true, then this call returns
     * immediately without blocking.
     */

    public static void waitForCondition (String condition) {
        BValue cond = getCond (condition);
        synchronized (cond) {
            if (!cond.v) {
                try {
                    cond.wait();
                } catch (InterruptedException e) {}
            }
        }
    }

    /* conditions must be locked when accessing this */
    static HashMap rv = new HashMap();

    /**
     * Force N threads to rendezvous (ie. wait for each other) before proceeding.
     * The first thread(s) to call are blocked until the last
     * thread makes the call. Then all threads continue.
     * <p>
     * All threads that call with the same condition name, must use the same value
     * for N (or the results may be not be as expected).
     * <P>
     * Obviously, if fewer than N threads make the rendezvous then the result
     * will be a hang.
     */

    public static void rendezvous (String condition, int N) {
        BValue cond;
        IValue iv;
        String name = "RV_"+condition;

        /* get the condition */

        synchronized (conditions) {
            cond = (BValue)conditions.get (name);
            if (cond == null) {
                /* we are first caller */
                if (N < 2) {
                    throw new RuntimeException ("rendezvous must be called with N >= 2");
                }
                cond = new BValue ();
                conditions.put (name, cond);
                iv = new IValue (N-1);
                rv.put (name, iv);
            } else {
                /* already initialised, just decrement the counter */
                iv = (IValue) rv.get (name);
                iv.v --;
            }
        }

        if (iv.v > 0) {
            waitForCondition (name);
        } else {
            setCondition (name);
            synchronized (conditions) {
                clearCondition (name);
                rv.remove (name);
            }
        }
    }

    /**
     * If the named condition exists and is set then remove it, so it can
     * be re-initialized and used again. If the condition does not exist, or
     * exists but is not set, then the call returns without doing anything.
     * Note, some higher level synchronization
     * may be needed between clear and the other operations.
     */

    public static void clearCondition(String condition) {
        BValue cond;
        synchronized (conditions) {
            cond = (BValue) conditions.get (condition);
            if (cond == null) {
                return;
            }
            synchronized (cond) {
                if (cond.v) {
                    conditions.remove (condition);
                }
            }
        }
    }
}
