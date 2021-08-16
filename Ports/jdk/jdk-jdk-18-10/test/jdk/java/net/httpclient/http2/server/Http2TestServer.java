/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SNIServerName;
import jdk.internal.net.http.frame.ErrorFrame;

/**
 * Waits for incoming TCP connections from a client and establishes
 * a HTTP2 connection. Two threads are created per connection. One for reading
 * and one for writing. Incoming requests are dispatched to the supplied
 * Http2Handler on additional threads. All threads
 * obtained from the supplied ExecutorService.
 */
public class Http2TestServer implements AutoCloseable {
    final ServerSocket server;
    final boolean supportsHTTP11;
    volatile boolean secure;
    final ExecutorService exec;
    volatile boolean stopping = false;
    final Map<String,Http2Handler> handlers;
    final SSLContext sslContext;
    final String serverName;
    final HashMap<InetSocketAddress,Http2TestServerConnection> connections;
    final Properties properties;

    private static ThreadFactory defaultThreadFac =
        (Runnable r) -> {
            Thread t = new Thread(r);
            t.setName("Test-server-pool");
            return t;
        };


    private static ExecutorService getDefaultExecutor() {
        return Executors.newCachedThreadPool(defaultThreadFac);
    }

    public Http2TestServer(String serverName, boolean secure, int port) throws Exception {
        this(serverName, secure, port, getDefaultExecutor(), 50, null, null);
    }

    public Http2TestServer(boolean secure, int port) throws Exception {
        this(null, secure, port, getDefaultExecutor(), 50, null, null);
    }

    public InetSocketAddress getAddress() {
        return (InetSocketAddress)server.getLocalSocketAddress();
    }

    public String serverAuthority() {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + getAddress().getPort();
    }

    public Http2TestServer(boolean secure,
                           SSLContext context) throws Exception {
        this(null, secure, 0, null, 50, null, context);
    }

    public Http2TestServer(String serverName, boolean secure,
                           SSLContext context) throws Exception {
        this(serverName, secure, 0, null, 50, null, context);
    }

    public Http2TestServer(boolean secure,
                           int port,
                           ExecutorService exec,
                           SSLContext context) throws Exception {
        this(null, secure, port, exec, 50, null, context);
    }

    public Http2TestServer(String serverName,
                           boolean secure,
                           int port,
                           ExecutorService exec,
                           SSLContext context)
        throws Exception
    {
        this(serverName, secure, port, exec, 50, null, context);
    }

    public Http2TestServer(String serverName,
                           boolean secure,
                           int port,
                           ExecutorService exec,
                           int backlog,
                           Properties properties,
                           SSLContext context)
        throws Exception
    {
        this(serverName, secure, port, exec, backlog, properties, context, false);
    }

    /**
     * Create a Http2Server listening on the given port. Currently needs
     * to know in advance whether incoming connections are plain TCP "h2c"
     * or TLS "h2".
     *
     * The HTTP/1.1 support, when supportsHTTP11 is true, is currently limited
     * to a canned 0-length response that contains the following headers:
     *       "X-Magic", "HTTP/1.1 request received by HTTP/2 server",
     *       "X-Received-Body", <the request body>);
     *
     * @param serverName SNI servername
     * @param secure https or http
     * @param port listen port
     * @param exec executor service (cached thread pool is used if null)
     * @param backlog the server socket backlog
     * @param properties additional configuration properties
     * @param context the SSLContext used when secure is true
     * @param supportsHTTP11 if true, the server may issue an HTTP/1.1 response
     *        to either 1) a non-Upgrade HTTP/1.1 request, or 2) a secure
     *        connection without the h2 ALPN. Otherwise, false to operate in
     *        HTTP/2 mode exclusively.
     */
    public Http2TestServer(String serverName,
                           boolean secure,
                           int port,
                           ExecutorService exec,
                           int backlog,
                           Properties properties,
                           SSLContext context,
                           boolean supportsHTTP11)
        throws Exception
    {
        this.serverName = serverName;
        this.supportsHTTP11 = supportsHTTP11;
        if (secure) {
           if (context != null)
               this.sslContext = context;
           else
               this.sslContext = SSLContext.getDefault();
            server = initSecure(port, backlog);
        } else {
            this.sslContext = context;
            server = initPlaintext(port, backlog);
        }
        this.secure = secure;
        this.exec = exec == null ? getDefaultExecutor() : exec;
        this.handlers = Collections.synchronizedMap(new HashMap<>());
        this.properties = properties;
        this.connections = new HashMap<>();
    }

    /**
     * Adds the given handler for the given path
     */
    public void addHandler(Http2Handler handler, String path) {
        handlers.put(path, handler);
    }

    volatile Http2TestExchangeSupplier exchangeSupplier = Http2TestExchangeSupplier.ofDefault();

    /**
     * Sets an explicit exchange handler to be used for all future connections.
     * Useful for testing scenarios where non-standard or specific server
     * behaviour is required, either direct control over the frames sent, "bad"
     * behaviour, or something else.
     */
    public void setExchangeSupplier(Http2TestExchangeSupplier exchangeSupplier) {
        this.exchangeSupplier = exchangeSupplier;
    }

    Http2Handler getHandlerFor(String path) {
        if (path == null || path.equals(""))
            path = "/";

        final String fpath = path;
        AtomicReference<String> bestMatch = new AtomicReference<>("");
        AtomicReference<Http2Handler> href = new AtomicReference<>();

        handlers.forEach((key, value) -> {
            if (fpath.startsWith(key) && key.length() > bestMatch.get().length()) {
                bestMatch.set(key);
                href.set(value);
            }
        });
        Http2Handler handler = href.get();
        if (handler == null)
            throw new RuntimeException("No handler found for path " + path);
        System.err.println("Using handler for: " + bestMatch.get());
        return handler;
    }

    final ServerSocket initPlaintext(int port, int backlog) throws Exception {
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(false);
        ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), backlog);
        return ss;
    }

    public synchronized void stop() {
        // TODO: clean shutdown GoAway
        stopping = true;
        System.err.printf("Server stopping %d connections\n", connections.size());
        for (Http2TestServerConnection connection : connections.values()) {
            connection.close(ErrorFrame.NO_ERROR);
        }
        try {
            server.close();
        } catch (IOException e) {}
        exec.shutdownNow();
    }


    final ServerSocket initSecure(int port, int backlog) throws Exception {
        ServerSocketFactory fac;
        SSLParameters sslp = null;
        fac = sslContext.getServerSocketFactory();
        sslp = sslContext.getSupportedSSLParameters();
        SSLServerSocket se = (SSLServerSocket) fac.createServerSocket();
        se.setReuseAddress(false);
        se.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), backlog);
        if (supportsHTTP11) {
            sslp.setApplicationProtocols(new String[]{"h2", "http/1.1"});
        } else {
            sslp.setApplicationProtocols(new String[]{"h2"});
        }
        sslp.setEndpointIdentificationAlgorithm("HTTPS");
        se.setSSLParameters(sslp);
        se.setEnabledCipherSuites(se.getSupportedCipherSuites());
        se.setEnabledProtocols(se.getSupportedProtocols());
        // other initialisation here
        return se;
    }

    public String serverName() {
        return serverName;
    }

    private synchronized void putConnection(InetSocketAddress addr, Http2TestServerConnection c) {
        if (!stopping)
            connections.put(addr, c);
    }

    private synchronized void removeConnection(InetSocketAddress addr, Http2TestServerConnection c) {
        connections.remove(addr, c);
    }

    /**
     * Starts a thread which waits for incoming connections.
     */
    public void start() {
        exec.submit(() -> {
            try {
                while (!stopping) {
                    Socket socket = server.accept();
                    Http2TestServerConnection c = null;
                    InetSocketAddress addr = null;
                    try {
                        addr = (InetSocketAddress) socket.getRemoteSocketAddress();
                        c = createConnection(this, socket, exchangeSupplier);
                        putConnection(addr, c);
                        c.run();
                    } catch (Throwable e) {
                        // we should not reach here, but if we do
                        // the connection might not have been closed
                        // and if so then the client might wait
                        // forever.
                        if (c != null) {
                            removeConnection(addr, c);
                            c.close(ErrorFrame.PROTOCOL_ERROR);
                        } else {
                            socket.close();
                        }
                        System.err.println("TestServer: start exception: " + e);
                    }
                }
            } catch (SecurityException se) {
                System.err.println("TestServer: terminating, caught " + se);
                se.printStackTrace();
                stopping = true;
                try { server.close(); } catch (IOException ioe) { /* ignore */}
            } catch (Throwable e) {
                if (!stopping) {
                    System.err.println("TestServer: terminating, caught " + e);
                    e.printStackTrace();
                }
            }
        });
    }

    protected Http2TestServerConnection createConnection(Http2TestServer http2TestServer,
                                                         Socket socket,
                                                         Http2TestExchangeSupplier exchangeSupplier)
            throws IOException {
        return new Http2TestServerConnection(http2TestServer, socket, exchangeSupplier, properties);
    }

    @Override
    public void close() throws Exception {
        stop();
    }
}
