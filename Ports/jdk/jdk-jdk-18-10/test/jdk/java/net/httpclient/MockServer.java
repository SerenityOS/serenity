/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * A cut-down Http/1 Server for testing various error situations
 *
 * use interrupt() to halt
 */
public class MockServer extends Thread implements Closeable {

    final ServerSocket ss;
    private final List<Connection> sockets;
    private final List<Connection> removals;
    private final List<Connection> additions;
    AtomicInteger counter = new AtomicInteger(0);
    // if specified (not null), only requests which
    // contain this value in their status line
    // will be taken into account and returned by activity().
    // Other requests will get summarily closed.
    // When specified, this can prevent answering to rogue
    // (external) clients that might be lurking
    // on the test machine instead of answering
    // to the test client.
   final String root;

    // waits up to 2000 seconds for something to happen
    // dont use this unless certain activity coming.
    public Connection activity() {
        for (int i = 0; i < 80 * 100; i++) {
            doRemovalsAndAdditions();
            for (Connection c : sockets) {
                if (c.poll()) {
                    if (root != null) {
                        // if a root was specified in MockServer
                        // constructor, rejects (by closing) all
                        // requests whose statusLine does not contain
                        // root.
                        if (!c.statusLine.contains(root)) {
                            System.out.println("Bad statusLine: "
                                    + c.statusLine
                                    + " closing connection");
                            c.close();
                            continue;
                        }
                    }
                    return c;
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void doRemovalsAndAdditions() {
        synchronized (removals) {
            Iterator<Connection> i = removals.iterator();
            while (i.hasNext()) {
                Connection c = i.next();
                System.out.println("socket removed: " + c);
                sockets.remove(c);
            }
            removals.clear();
        }

        synchronized (additions) {
            Iterator<Connection> i = additions.iterator();
            while (i.hasNext()) {
                Connection c = i.next();
                System.out.println("socket added: " + c);
                sockets.add(c);
            }
            additions.clear();
        }
    }

    // clears all current connections on Server.
    public void reset() {
        for (Connection c : sockets) {
            c.close();
        }
    }

    /**
     * Reads data into an ArrayBlockingQueue<String> where each String
     * is a line of input, that was terminated by CRLF (not included)
     */
    class Connection extends Thread {
        Connection(Socket s) throws IOException {
            this.socket = s;
            id = counter.incrementAndGet();
            is = s.getInputStream();
            os = s.getOutputStream();
            incoming = new ArrayBlockingQueue<>(100);
            setName("Server-Connection");
            setDaemon(true);
        }
        final Socket socket;
        final int id;
        final InputStream is;
        final OutputStream os;
        final ArrayBlockingQueue<String> incoming;
        volatile String statusLine;

        final static String CRLF = "\r\n";

        // sentinel indicating connection closed
        final static String CLOSED = "C.L.O.S.E.D";
        volatile boolean closed = false;
        volatile boolean released = false;

        @Override
        public void run() {
            byte[] buf = new byte[256];
            String s = "";
            try {
                while (true) {
                    int n = is.read(buf);
                    if (n == -1) {
                        cleanup();
                        return;
                    }
                    String s0 = new String(buf, 0, n, ISO_8859_1);
                    s = s + s0;
                    int i;
                    while ((i=s.indexOf(CRLF)) != -1) {
                        String s1 = s.substring(0, i+2);
                        System.out.println("Server got: " + s1.substring(0,i));
                        if (statusLine == null) statusLine = s1.substring(0,i);
                        incoming.put(s1);
                        if (i+2 == s.length()) {
                            s = "";
                            break;
                        }
                        s = s.substring(i+2);
                    }
                }
            } catch (IOException |InterruptedException e1) {
                cleanup();
            } catch (Throwable t) {
                System.out.println("Exception: " + t);
                t.printStackTrace();
                cleanup();
            }
        }

        @Override
        public String toString() {
            return "Server.Connection: " + socket.toString();
        }

        public void sendHttpResponse(int code, String body, String... headers)
            throws IOException
        {
            String r1 = "HTTP/1.1 " + Integer.toString(code) + " status" + CRLF;
            for (int i=0; i<headers.length; i+=2) {
                r1 += headers[i] + ": " + headers[i+1] + CRLF;
            }
            int clen = body == null ? 0 : body.getBytes(ISO_8859_1).length;
            r1 += "Content-Length: " + Integer.toString(clen) + CRLF;
            r1 += CRLF;
            if (body != null) {
                r1 += body;
            }
            send(r1);
        }

        // content-length is 10 bytes too many
        public void sendIncompleteHttpResponseBody(int code) throws IOException {
            String body = "Hello World Helloworld Goodbye World";
            String r1 = "HTTP/1.1 " + Integer.toString(code) + " status" + CRLF;
            int clen = body.getBytes(ISO_8859_1).length + 10;
            r1 += "Content-Length: " + Integer.toString(clen) + CRLF;
            r1 += CRLF;
            if (body != null) {
                r1 += body;
            }
            send(r1);
        }

        public void sendIncompleteHttpResponseHeaders(int code)
            throws IOException
        {
            String r1 = "HTTP/1.1 " + Integer.toString(code) + " status" + CRLF;
            send(r1);
        }

        public void send(String r) throws IOException {
            try {
                os.write(r.getBytes(ISO_8859_1));
            } catch (IOException x) {
                IOException suppressed =
                        new IOException("MockServer["
                            + ss.getLocalPort()
                            +"] Failed while writing bytes: "
                            +  x.getMessage());
                x.addSuppressed(suppressed);
                System.err.println("WARNING: " + suppressed);
                throw x;
            }
        }

        public synchronized void close() {
            cleanup();
            closed = true;
            incoming.clear();
        }

        public String nextInput(long timeout, TimeUnit unit) {
            String result = "";
            while (poll()) {
                try {
                    String s = incoming.poll(timeout, unit);
                    if (s == null && closed) {
                        return CLOSED;
                    } else {
                        result += s;
                    }
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return result;
        }

        public String nextInput() {
            return nextInput(0, TimeUnit.SECONDS);
        }

        public boolean poll() {
            return incoming.peek() != null;
        }

        private void cleanup() {
            if (released) return;
            synchronized(this) {
                if (released) return;
                released = true;
            }
            try {
                socket.close();
            } catch (Throwable e) {}
            synchronized (removals) {
                removals.add(this);
            }
        }
    }

    MockServer(int port, ServerSocketFactory factory, String root) throws IOException {
        ss = factory.createServerSocket();
        ss.setReuseAddress(false);
        ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        this.root = root; // if specified, any request which don't have this value
                          // in their statusLine will be rejected.
        sockets = Collections.synchronizedList(new LinkedList<>());
        removals = new LinkedList<>();
        additions = new LinkedList<>();
        setName("Test-Server");
        setDaemon(true);
    }

    MockServer(int port, ServerSocketFactory factory) throws IOException {
        this(port, factory, "/foo/");
    }

    MockServer(int port) throws IOException {
        this(port, ServerSocketFactory.getDefault());
    }

    MockServer() throws IOException {
        this(0);
    }

    int port() {
        return ss.getLocalPort();
    }

    String serverAuthority() {
        return InetAddress.getLoopbackAddress().getHostName() + ":" + port();
    }

    public String getURL() {
        if (ss instanceof SSLServerSocket) {
            return "https://" + serverAuthority() + "/foo/";
        } else {
            return "http://" + serverAuthority() + "/foo/";
        }
    }

    private volatile boolean closed;

    @Override
    public void close() {
        closed = true;
        try {
            ss.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        for (Connection c : sockets) {
            c.close();
        }
    }

    @Override
    public void run() {
        try {
            while (!closed) {
                try {
                    System.out.println("Server waiting for connection");
                    Socket s = ss.accept();
                    Connection c = new Connection(s);
                    c.start();
                    System.out.println("Server got new connection: " + c);
                    synchronized (additions) {
                        additions.add(c);
                    }
                } catch (IOException e) {
                    if (closed)
                        return;
                    e.printStackTrace(System.out);
                }
            }
        } catch (Throwable t) {
            System.out.println("Unexpected exception in accept loop: " + t);
            t.printStackTrace(System.out);
        } finally {
            if (closed) {
                System.out.println("Server closed: exiting accept loop");
            } else {
                System.out.println("Server not closed: exiting accept loop and closing");
                close();
            }
        }
    }

}
