/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.security.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * A minimal proxy server that supports CONNECT tunneling. It does not do
 * any header transformations. In future this could be added.
 * Two threads are created per client connection. So, it's not
 * intended for large numbers of parallel connections.
 */
public class ProxyServer extends Thread implements Closeable {

    // could use the test library here - Platform.isWindows(),
    // but it would force all tests that use ProxyServer to
    // build it. Let's keep it simple.
    static final boolean IS_WINDOWS;
    static {
        PrivilegedAction<String> action =
                () -> System.getProperty("os.name", "unknown");
        String osName = AccessController.doPrivileged(action);
        IS_WINDOWS = osName.toLowerCase(Locale.ROOT).startsWith("win");
    }

    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    ServerSocketChannel listener;
    int port;
    volatile boolean debug;
    private final Credentials credentials;  // may be null

    private static class Credentials {
        private final String name;
        private final String password;
        private Credentials(String name, String password) {
            this.name = name;
            this.password = password;
        }
        public String name() { return name; }
        public String password() { return password; }
    }

    /**
     * Create proxy on port (zero means don't care). Call getPort()
     * to get the assigned port.
     */
    public ProxyServer(Integer port) throws IOException {
        this(port, false);
    }

    public ProxyServer(Integer port,
                       Boolean debug,
                       String username,
                       String password)
        throws IOException
    {
        this(port, debug, new Credentials(username, password));
    }

    public ProxyServer(Integer port,
                       Boolean debug)
        throws IOException
    {
        this(port, debug, null);
    }

    public ProxyServer(Integer port,
                       Boolean debug,
                       Credentials credentials)
        throws IOException
    {
        this.debug = debug;
        listener = ServerSocketChannel.open();
        listener.setOption(StandardSocketOptions.SO_REUSEADDR, false);
        listener.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        this.port = ((InetSocketAddress)listener.getLocalAddress()).getPort();
        this.credentials = credentials;
        setName("ProxyListener");
        setDaemon(true);
        connections = new CopyOnWriteArrayList<Connection>();
        start();
    }

    public ProxyServer(String s) {
        credentials = null;
        connections = new CopyOnWriteArrayList<Connection>();
    }

    /**
     * Returns the port number this proxy is listening on
     */
    public int getPort() {
        return port;
    }

    public InetSocketAddress getProxyAddress() throws IOException  {
        return (InetSocketAddress)listener.getLocalAddress();
    }

    /**
     * Shuts down the proxy, probably aborting any connections
     * currently open
     */
    public void close() throws IOException {
        if (debug) System.out.println("Proxy: closing server");
        done = true;
        listener.close();
        for (Connection c : connections) {
            c.close();
            c.awaitCompletion();
        }
    }

    final CopyOnWriteArrayList<Connection> connections;

    volatile boolean done;

    public void run() {
        if (System.getSecurityManager() == null) {
            execute();
        } else {
            // so calling domain does not need to have socket permission
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    execute();
                    return null;
                }
            });
        }
    }

    public void execute() {
        int id = 0;
        try {
            while (!done) {
                SocketChannel s = listener.accept();
                id++;
                Connection c = new Connection(s, id);
                if (debug)
                    System.out.println("Proxy: accepted new connection: " + c);
                connections.add(c);
                c.init();
            }
        } catch(Throwable e) {
            if (debug && !done) {
                System.out.println("Proxy: Fatal error, listener got " + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * Transparently forward everything, once we know what the destination is
     */
    class Connection {

        private final int id;
        SocketChannel clientSocket, serverSocket;
        Thread out, in;
        volatile InputStream clientIn, serverIn;
        volatile OutputStream clientOut, serverOut;

        final static int CR = 13;
        final static int LF = 10;

        Connection(SocketChannel s, int id) throws IOException {
            this.id = id;
            this.clientSocket= s;
            this.clientIn = new BufferedInputStream(s.socket().getInputStream());
            this.clientOut = s.socket().getOutputStream();
        }

        byte[] readHeaders(InputStream is) throws IOException {
            byte[] outbuffer = new byte[8000];
            int crlfcount = 0;
            int bytecount = 0;
            int c;
            while ((c=is.read()) != -1 && bytecount < outbuffer.length) {
                outbuffer[bytecount++] = (byte)c;
                if (debug) System.out.write(c);
                // were looking for CRLFCRLF sequence
                if (c == CR || c == LF) {
                    switch(crlfcount) {
                        case 0:
                            if (c == CR) crlfcount ++;
                            break;
                        case 1:
                            if (c == LF) crlfcount ++;
                            break;
                        case 2:
                            if (c == CR) crlfcount ++;
                            break;
                        case 3:
                            if (c == LF) crlfcount ++;
                            break;
                    }
                } else {
                    crlfcount = 0;
                }
                if (crlfcount == 4) {
                    break;
                }
            }
            byte[] ret = new byte[bytecount];
            System.arraycopy(outbuffer, 0, ret, 0, bytecount);
            return ret;
        }

        boolean running() {
            return out.isAlive() || in.isAlive();
        }

        private volatile boolean closing;
        public synchronized void close() throws IOException {
            closing = true;
            if (debug)
                System.out.println("Proxy: closing connection {" + this + "}");
            if (serverSocket != null)
                serverSocket.close();
            if (clientSocket != null)
                clientSocket.close();
        }

        public void awaitCompletion() {
            try {
                if (in != null)
                    in.join();
                if (out!= null)
                    out.join();
            } catch (InterruptedException e) { }
        }

        int findCRLF(byte[] b) {
            for (int i=0; i<b.length-1; i++) {
                if (b[i] == CR && b[i+1] == LF) {
                    return i;
                }
            }
            return -1;
        }

        // Checks credentials in the request against those allowable by the proxy.
        private boolean authorized(Credentials credentials,
                                   List<String> requestHeaders) {
            List<String> authorization = requestHeaders.stream()
                    .filter(n -> n.toLowerCase(Locale.US).startsWith("proxy-authorization"))
                    .collect(toList());

            if (authorization.isEmpty())
                return false;

            if (authorization.size() != 1) {
                throw new IllegalStateException("Authorization unexpected count:" + authorization);
            }
            String value = authorization.get(0).substring("proxy-authorization".length()).trim();
            if (!value.startsWith(":"))
                throw new IllegalStateException("Authorization malformed: " + value);
            value = value.substring(1).trim();

            if (!value.startsWith("Basic "))
                throw new IllegalStateException("Authorization not Basic: " + value);

            value = value.substring("Basic ".length());
            String values = new String(Base64.getDecoder().decode(value), UTF_8);
            int sep = values.indexOf(':');
            if (sep < 1) {
                throw new IllegalStateException("Authorization no colon: " +  values);
            }
            String name = values.substring(0, sep);
            String password = values.substring(sep + 1);

            if (name.equals(credentials.name()) && password.equals(credentials.password()))
                return true;

            return false;
        }

        public void init() {
            try {
                byte[] buf;
                String host;
                List<String> headers;
                boolean authorized = false;
                while (true) {
                    buf = readHeaders(clientIn);
                    if (findCRLF(buf) == -1) {
                        if (debug)
                            System.out.println("Proxy: no CRLF closing, buf contains:["
                                    + new String(buf, ISO_8859_1) + "]" );
                        close();
                        return;
                    }

                    headers = asList(new String(buf, ISO_8859_1).split("\r\n"));
                    host = findFirst(headers, "host");
                    // check authorization credentials, if required by the server
                    if (credentials != null) {
                        if (!authorized(credentials, headers)) {
                            String resp = "HTTP/1.1 407 Proxy Authentication Required\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Proxy-Authenticate: Basic realm=\"proxy realm\"\r\n\r\n";
                            clientSocket.setOption(StandardSocketOptions.TCP_NODELAY, true);
                            clientSocket.setOption(StandardSocketOptions.SO_LINGER, 2);
                            var buffer = ByteBuffer.wrap(resp.getBytes(ISO_8859_1));
                            clientSocket.write(buffer);
                            if (debug) {
                                var linger = clientSocket.getOption(StandardSocketOptions.SO_LINGER);
                                var nodelay = clientSocket.getOption(StandardSocketOptions.TCP_NODELAY);
                                System.out.printf("Proxy: unauthorized; 407 sent (%s/%s), linger: %s, nodelay: %s%n",
                                        buffer.position(), buffer.position() + buffer.remaining(), linger, nodelay);
                            }
                            if (shouldCloseAfter407(headers)) {
                                closeConnection();
                                return;
                            }
                            continue;
                        }
                        authorized = true;
                        break;
                    } else {
                        break;
                    }
                }

                int p = findCRLF(buf);
                String cmd = new String(buf, 0, p, ISO_8859_1);
                String[] params = cmd.split(" ");

                if (params[0].equals("CONNECT")) {
                    doTunnel(params[1]);
                } else {
                    // TODO:
                    // this does not really work as it should: it also establishes
                    // a tunnel (proxyCommon). So it works as long as the client only
                    // sends a single plain request through the proxy - as all
                    // other requests would otherwise be tunneled to the
                    // server identified in the first request.
                    // It seems enough for our purpose for now, though.
                    // Fixing this would imply dealing with Content-Length, Transfer-Encoding,
                    // etc, both when writing to and reading from the server.
                    doProxy(params[1], cmd, headers, host, authorized);
                }
            } catch (Throwable e) {
                if (debug) {
                    System.out.println("Proxy: " + e);
                    e.printStackTrace();
                }
                try {close(); } catch (IOException e1) {}
            }
        }

        String findFirst(List<String> headers, String key) {
            var h = key.toLowerCase(Locale.ROOT) + ": ";
            return headers.stream()
                    .filter((s) -> s.toLowerCase(Locale.ROOT).startsWith(h))
                    .findFirst()
                    .map((s) -> s.substring(h.length()))
                    .map(String::trim)
                    .orElse(null);
        }

        private long drain(SocketChannel socket) throws IOException {
            boolean isBlocking = socket.isBlocking();
            if (isBlocking) {
                socket.configureBlocking(false);
            }
            try {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int read;
                long drained = 0;
                while ((read = socket.read(buffer)) > 0) {
                    drained += read;
                    buffer.position(0);
                    buffer.limit(buffer.capacity());
                }
                return drained;
            } finally {
                if (isBlocking) {
                    socket.configureBlocking(true);
                }
            }
        }

        void closeConnection() throws IOException {
            if (debug) {
                var linger = clientSocket.getOption(StandardSocketOptions.SO_LINGER);
                var nodelay = clientSocket.getOption(StandardSocketOptions.TCP_NODELAY);
                System.out.printf("Proxy: closing connection id=%s, linger: %s, nodelay: %s%n",
                        id, linger, nodelay);
            }
            long drained = drain(clientSocket);
            if (debug) {
                System.out.printf("Proxy: drained: %s%n", drained);
            }
            clientSocket.shutdownOutput();
            try {
                // On windows, we additionally need to delay before
                // closing the socket. Otherwise we get a reset on the
                // client side (The connection was aborted by a software
                // on the host machine).
                // Delay 500ms before actually closing the socket
                if (isWindows()) Thread.sleep(500);
            } catch (InterruptedException x) {
                // OK
            }
            clientSocket.shutdownInput();
            close();
        }

        // If the client sends a request body we will need to close the connection
        // otherwise, we can keep it open.
        private boolean shouldCloseAfter407(List<String> headers) throws IOException {
            var te = findFirst(headers, "transfer-encoding");
            if (te != null) {
                // processing transfer encoding not implemented
                if (debug) {
                    System.out.println("Proxy: transfer-encoding with 407, closing connection");
                }
                return true; // should close
            }
            var cl = findFirst(headers, "content-length");
            int n = -1;
            try {
                n = Integer.parseInt(cl);
                if (debug) {
                    System.out.printf("Proxy: content-length: %d%n", cl);
                }
            } catch (IllegalFormatException x) {
                if (debug) {
                    System.out.println("Proxy: bad content-length, closing connection");
                }
                return true;  // should close
            }
            if (n > 0 || n < -1) {
                if (debug) {
                    System.out.println("Proxy: request body with 407, closing connection");
                }
                return true;  // should close
            }
            var cmdline = headers.get(0);
            int m = cmdline.indexOf(' ');
            var method = (m > 0) ? cmdline.substring(0, m) : null;
            var nobody = List.of("GET", "HEAD");
            if (n == 0 || nobody.contains(m)) {
                var available = clientIn.available();
                var drained = drain(clientSocket);
                if (drained > 0 || available > 0) {
                    if (debug) {
                        System.out.printf("Proxy: unexpected bytes (%d) with 407, closing connection%n",
                                drained + available);
                    }
                    return true;  // should close
                }
                // can keep open: CL=0 or no CL and GET or HEAD
                return false;
            } else {
                if (debug) {
                    System.out.println("Proxy: possible body with 407, closing connection");
                }
                return true; // should close
            }
        }

        void doProxy(String dest, String cmdLine, List<String> headers, String host, boolean authorized)
            throws IOException
        {
            try {
                URI uri = new URI(dest);
                if (!uri.isAbsolute()) {
                    if (host == null) {
                        throw new IOException("request URI not absolute");
                    } else {
                        uri = new URI("http://" + host + dest);
                    }
                }
                if (debug) System.out.printf("Proxy: uri=%s%n", uri);
                dest = uri.getAuthority();
                // now extract the path from the URI and recreate the cmd line
                int sp = cmdLine.indexOf(' ');
                String method = cmdLine.substring(0, sp);
                cmdLine = method + " " + uri.getPath() + " HTTP/1.1";

                commonInit(dest, 80);
                OutputStream sout;
                synchronized (this) {
                    if (closing) return;
                    sout = serverOut;
                }
                // might fail if we're closing but we don't care.
                byte[] CRLF = new byte[] { (byte) '\r', (byte) '\n'};
                sout.write(cmdLine.getBytes(ISO_8859_1));
                sout.write(CRLF);
                if (debug) System.out.printf("Proxy Forwarding: %s%n", cmdLine);
                for (int l=1; l<headers.size(); l++) {
                    var s = headers.get(l);
                    if (!authorized || !s.toLowerCase(Locale.ROOT).startsWith("proxy-authorization")) {
                        sout.write(s.getBytes(ISO_8859_1));
                        sout.write(CRLF);
                        if (debug) System.out.printf("Proxy Forwarding: %s%n", s);
                    } else {
                        if (debug) System.out.printf("Proxy Skipping: %s%n", s);
                    }
                }
                sout.write(CRLF);
                if (debug) System.out.printf("Proxy Forwarding: %n");

                // This will now establish a tunnel :-(
                proxyCommon(debug);

            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }

        synchronized void commonInit(String dest, int defaultPort) throws IOException {
            if (closing) return;
            int port;
            String[] hostport = dest.split(":");
            if (hostport.length == 1) {
                port = defaultPort;
            } else {
                port = Integer.parseInt(hostport[1]);
            }
            if (debug)
                System.out.printf("Proxy: connecting to (%s/%d)\n", hostport[0], port);
            serverSocket = SocketChannel.open();
            serverSocket.connect(new InetSocketAddress(hostport[0], port));
            serverOut = serverSocket.socket().getOutputStream();

            serverIn = new BufferedInputStream(serverSocket.socket().getInputStream());
        }

        synchronized void proxyCommon(boolean log) throws IOException {
            if (closing) return;
            out = new Thread(() -> {
                try {
                    byte[] bb = new byte[8000];
                    int n;
                    int body = 0;
                    while ((n = clientIn.read(bb)) != -1) {
                        serverOut.write(bb, 0, n);
                        body += n;
                        if (log)
                            System.out.printf("Proxy Forwarding [request body]: total %d%n", body);
                    }
                    closing = true;
                    serverSocket.close();
                    clientSocket.close();
                } catch (IOException e) {
                    if (!closing && debug) {
                        System.out.println("Proxy: " + e);
                        e.printStackTrace();
                    }
                }
            });
            in = new Thread(() -> {
                try {
                    byte[] bb = new byte[8000];
                    int n;
                    int resp = 0;
                    while ((n = serverIn.read(bb)) != -1) {
                        clientOut.write(bb, 0, n);
                        resp += n;
                        if (log) System.out.printf("Proxy Forwarding [response]: %s%n", new String(bb, 0, n, UTF_8));
                        if (log) System.out.printf("Proxy Forwarding [response]: total %d%n", resp);
                    }
                    closing = true;
                    serverSocket.close();
                    clientSocket.close();
                } catch (IOException e) {
                    if (!closing && debug) {
                        System.out.println("Proxy: " + e);
                        e.printStackTrace();
                    }
                }
            });
            out.setName("Proxy-outbound");
            out.setDaemon(true);
            in.setDaemon(true);
            in.setName("Proxy-inbound");
            out.start();
            in.start();
        }

        void doTunnel(String dest) throws IOException {
            if (closing) return; // no need to go further.
            commonInit(dest, 443);
            // might fail if we're closing, but we don't care.
            clientOut.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
            proxyCommon(false);
        }

        @Override
        public String toString() {
            return "Proxy connection " + id + ", client sock:" + clientSocket;
        }
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        boolean debug = args.length > 1 && args[1].equals("-debug");
        System.out.println("Debugging : " + debug);
        ProxyServer ps = new ProxyServer(port, debug);
        System.out.println("Proxy server listening on port " + ps.getPort());
        while (true) {
            Thread.sleep(5000);
        }
    }
}
