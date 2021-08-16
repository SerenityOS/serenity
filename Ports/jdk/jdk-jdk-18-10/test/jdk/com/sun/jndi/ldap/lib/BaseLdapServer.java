/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.Logger.Level.INFO;

/*
 * A bare-bones (testing aid) server for LDAP scenarios.
 *
 * Override the following methods to provide customized behavior
 *
 *     * beforeAcceptingConnections
 *     * beforeConnectionHandled
 *     * handleRequest (or handleRequestEx)
 *
 * Instances of this class are safe for use by multiple threads.
 */
public class BaseLdapServer implements Closeable {

    private static final System.Logger logger = System.getLogger("BaseLdapServer");

    private final Thread acceptingThread = new Thread(this::acceptConnections);
    private final ServerSocket serverSocket;
    private final List<Socket> socketList = new ArrayList<>();
    private final ExecutorService connectionsPool;

    private final Object lock = new Object();
    /*
     * 3-valued state to detect restarts and other programming errors.
     */
    private State state = State.NEW;

    private enum State {
        NEW,
        STARTED,
        STOPPED
    }

    public BaseLdapServer() throws IOException {
        this(new ServerSocket(0, 0, InetAddress.getLoopbackAddress()));
    }

    public BaseLdapServer(ServerSocket serverSocket) {
        this.serverSocket = Objects.requireNonNull(serverSocket);
        this.connectionsPool = Executors.newCachedThreadPool();
    }

    private void acceptConnections() {
        logger().log(INFO, "Server is accepting connections at port {0}",
                     getPort());
        try {
            beforeAcceptingConnections();
            while (isRunning()) {
                Socket socket = serverSocket.accept();
                logger().log(INFO, "Accepted new connection at {0}", socket);
                synchronized (lock) {
                    // Recheck if the server is still running
                    // as someone has to close the `socket`
                    if (isRunning()) {
                        socketList.add(socket);
                    } else {
                        closeSilently(socket);
                    }
                }
                connectionsPool.submit(() -> handleConnection(socket));
            }
        } catch (Throwable t) {
            if (isRunning()) {
                throw new RuntimeException(
                        "Unexpected exception while accepting connections", t);
            }
        } finally {
            logger().log(INFO, "Server stopped accepting connections at port {0}",
                                getPort());
        }
    }

    /*
     * Called once immediately preceding the server accepting connections.
     *
     * Override to customize the behavior.
     */
    protected void beforeAcceptingConnections() { }

    /*
     * A "Template Method" describing how a connection (represented by a socket)
     * is handled.
     *
     * The socket is closed immediately before the method returns (normally or
     * abruptly).
     */
    private void handleConnection(Socket socket) {
        // No need to close socket's streams separately, they will be closed
        // automatically when `socket.close()` is called
        beforeConnectionHandled(socket);
        ConnWrapper connWrapper = new ConnWrapper(socket);
        try (socket) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            byte[] inBuffer = new byte[1024];
            int count;
            byte[] request;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int msgLen = -1;

            // As inBuffer.length > 0, at least 1 byte is read
            while ((count = in.read(inBuffer)) > 0) {
                buffer.write(inBuffer, 0, count);
                if (msgLen <= 0) {
                    msgLen = LdapMessage.getMessageLength(buffer.toByteArray());
                }

                if (msgLen > 0 && buffer.size() >= msgLen) {
                    if (buffer.size() > msgLen) {
                        byte[] tmpBuffer = buffer.toByteArray();
                        request = Arrays.copyOf(tmpBuffer, msgLen);
                        buffer.reset();
                        buffer.write(tmpBuffer, msgLen, tmpBuffer.length - msgLen);
                    } else {
                        request = buffer.toByteArray();
                        buffer.reset();
                    }
                    msgLen = -1;
                } else {
                    logger.log(INFO, "Request message incomplete, " +
                            "bytes received {0}, expected {1}", buffer.size(), msgLen);
                    continue;
                }
                handleRequestEx(socket, new LdapMessage(request), out, connWrapper);
                if (connWrapper.updateRequired()) {
                    var wrapper = connWrapper.getWrapper();
                    in = wrapper.getInputStream();
                    out = wrapper.getOutputStream();
                    connWrapper.clearFlag();
                }
            }
        } catch (Throwable t) {
            if (!isRunning()) {
                logger.log(INFO, "Connection Handler exit {0}", t.getMessage());
            } else {
                t.printStackTrace();
            }
        }

        if (connWrapper.getWrapper() != null) {
            closeSilently(connWrapper.getWrapper());
        }
    }

    /*
     * Called first thing in `handleConnection()`.
     *
     * Override to customize the behavior.
     */
    protected void beforeConnectionHandled(Socket socket) { /* empty */ }

    /*
     * Called after an LDAP request has been read in `handleConnection()`.
     *
     * Override to customize the behavior.
     */
    protected void handleRequest(Socket socket,
                                 LdapMessage request,
                                 OutputStream out)
            throws IOException
    {
        logger().log(INFO, "Discarding message {0} from {1}. "
                             + "Override {2}.handleRequest to change this behavior.",
                     request, socket, getClass().getName());
    }

    /*
     * Called after an LDAP request has been read in `handleConnection()`.
     *
     * Override to customize the behavior if you want to handle starttls
     * extended op, otherwise override handleRequest method instead.
     *
     * This is extended handleRequest method which provide possibility to
     * wrap current socket connection, that's necessary to handle starttls
     * extended request, here is sample code about how to wrap current socket
     *
     * switch (request.getOperation()) {
     *     ......
     *     case EXTENDED_REQUEST:
     *         if (new String(request.getMessage()).endsWith(STARTTLS_REQ_OID)) {
     *             out.write(STARTTLS_RESPONSE);
     *             SSLSocket sslSocket = (SSLSocket) sslSocketFactory
     *                     .createSocket(socket, null, socket.getLocalPort(),
     *                             false);
     *             sslSocket.setUseClientMode(false);
     *             connWrapper.setWrapper(sslSocket);
     *         }
     *         break;
     *     ......
     * }
     */
    protected void handleRequestEx(Socket socket,
            LdapMessage request,
            OutputStream out,
            ConnWrapper connWrapper)
            throws IOException {
        // by default, just call handleRequest to keep compatibility
        handleRequest(socket, request, out);
    }

    /*
     * To be used by subclasses.
     */
    protected final System.Logger logger() {
        return logger;
    }

    /*
     * Starts this server. May be called only once.
     */
    public BaseLdapServer start() {
        synchronized (lock) {
            if (state != State.NEW) {
                throw new IllegalStateException(state.toString());
            }
            state = State.STARTED;
            logger().log(INFO, "Starting server at port {0}", getPort());
            acceptingThread.start();
            return this;
        }
    }

    /*
     * Stops this server.
     *
     * May be called at any time, even before a call to `start()`. In the latter
     * case the subsequent call to `start()` will throw an exception. Repeated
     * calls to this method have no effect.
     *
     * Stops accepting new connections, interrupts the threads serving already
     * accepted connections and closes all the sockets.
     */
    @Override
    public void close() {
        synchronized (lock) {
            if (state == State.STOPPED) {
                return;
            }
            state = State.STOPPED;
            logger().log(INFO, "Stopping server at port {0}", getPort());
            acceptingThread.interrupt();
            closeSilently(serverSocket);
            // It's important to signal an interruption so that overridden
            // methods have a chance to return if they use
            // interruption-sensitive blocking operations. However, blocked I/O
            // operations on the socket will NOT react on that, hence the socket
            // also has to be closed to propagate shutting down.
            connectionsPool.shutdownNow();
            socketList.forEach(BaseLdapServer.this::closeSilently);
        }
    }

    /**
     * Returns the local port this server is listening at.
     *
     * This method can be called at any time.
     *
     * @return the port this server is listening at
     */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * Returns the address this server is listening at.
     *
     * This method can be called at any time.
     *
     * @return the address
     */
    public InetAddress getInetAddress() {
        return serverSocket.getInetAddress();
    }

    /*
     * Returns a flag to indicate whether this server is running or not.
     *
     * @return {@code true} if this server is running, {@code false} otherwise.
     */
    public boolean isRunning() {
        synchronized (lock) {
            return state == State.STARTED;
        }
    }

    /*
     * To be used by subclasses.
     */
    protected final void closeSilently(Closeable resource) {
        try {
            resource.close();
        } catch (IOException ignored) { }
    }

    /*
     * To be used for handling starttls extended request
     */
    protected class ConnWrapper {
        private Socket original;
        private Socket wrapper;
        private boolean flag = false;

        public ConnWrapper(Socket socket) {
            original = socket;
        }

        public Socket getWrapper() {
            return wrapper;
        }

        public void setWrapper(Socket wrapper) {
            if (wrapper != null && wrapper != original) {
                this.wrapper = wrapper;
                flag = true;
            }
        }

        public boolean updateRequired() {
            return flag;
        }

        public void clearFlag() {
            flag = false;
        }
    }
}
