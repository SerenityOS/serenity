/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package sun.rmi.transport.tcp;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.rmi.server.LogStream;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.RemoteCall;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UID;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import sun.rmi.runtime.Log;
import sun.rmi.runtime.NewThreadAction;
import sun.rmi.transport.Channel;
import sun.rmi.transport.Connection;
import sun.rmi.transport.DGCAckHandler;
import sun.rmi.transport.Endpoint;
import sun.rmi.transport.StreamRemoteCall;
import sun.rmi.transport.Target;
import sun.rmi.transport.Transport;
import sun.rmi.transport.TransportConstants;

/**
 * TCPTransport is the socket-based implementation of the RMI Transport
 * abstraction.
 *
 * @author Ann Wollrath
 * @author Peter Jones
 */
@SuppressWarnings("deprecation")
public class TCPTransport extends Transport {

    /* tcp package log */
    @SuppressWarnings("removal")
    static final Log tcpLog = Log.getLog("sun.rmi.transport.tcp", "tcp",
        LogStream.parseLevel(AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("sun.rmi.transport.tcp.logLevel"))));

    /** maximum number of connection handler threads */
    @SuppressWarnings("removal")
    private static final int maxConnectionThreads =     // default no limit
        AccessController.doPrivileged((PrivilegedAction<Integer>) () ->
            Integer.getInteger("sun.rmi.transport.tcp.maxConnectionThreads",
                               Integer.MAX_VALUE));

    /** keep alive time for idle connection handler threads */
    @SuppressWarnings("removal")
    private static final long threadKeepAliveTime =     // default 1 minute
        AccessController.doPrivileged((PrivilegedAction<Long>) () ->
            Long.getLong("sun.rmi.transport.tcp.threadKeepAliveTime", 60000));

    /** thread pool for connection handlers */
    private static final ExecutorService connectionThreadPool =
        new ThreadPoolExecutor(0, maxConnectionThreads,
            threadKeepAliveTime, TimeUnit.MILLISECONDS,
            new SynchronousQueue<Runnable>(),
            new ThreadFactory() {
                @SuppressWarnings("removal")
                public Thread newThread(Runnable runnable) {
                    return AccessController.doPrivileged(new NewThreadAction(
                        runnable, "TCP Connection(idle)", true, true));
                }
            });

    /** total connections handled */
    private static final AtomicInteger connectionCount = new AtomicInteger();

    /** client host for the current thread's connection */
    private static final ThreadLocal<ConnectionHandler>
        threadConnectionHandler = new ThreadLocal<>();

    /** an AccessControlContext with no permissions */
    @SuppressWarnings("removal")
    private static final AccessControlContext NOPERMS_ACC = createNopermsAcc();

    @SuppressWarnings("removal")
    private static AccessControlContext createNopermsAcc() {
        Permissions perms = new Permissions();
        ProtectionDomain[] pd = { new ProtectionDomain(null, perms) };
        return new AccessControlContext(pd);
    }

    /** endpoints for this transport */
    private final LinkedList<TCPEndpoint> epList;
    /** number of objects exported on this transport */
    private int exportCount = 0;
    /** server socket for this transport */
    private ServerSocket server = null;
    /** table mapping endpoints to channels */
    private final Map<TCPEndpoint,Reference<TCPChannel>> channelTable =
        new WeakHashMap<>();

    static final RMISocketFactory defaultSocketFactory =
        RMISocketFactory.getDefaultSocketFactory();

    /** number of milliseconds in accepted-connection timeout.
     * Warning: this should be greater than 15 seconds (the client-side
     * timeout), and defaults to 2 hours.
     * The maximum representable value is slightly more than 24 days
     * and 20 hours.
     */
    @SuppressWarnings("removal")
    private static final int connectionReadTimeout =    // default 2 hours
        AccessController.doPrivileged((PrivilegedAction<Integer>) () ->
            Integer.getInteger("sun.rmi.transport.tcp.readTimeout", 2 * 3600 * 1000));

    /**
     * Constructs a TCPTransport.
     */
    TCPTransport(LinkedList<TCPEndpoint> epList)  {
        // assert ((epList.size() != null) && (epList.size() >= 1))
        this.epList = epList;
        if (tcpLog.isLoggable(Log.BRIEF)) {
            tcpLog.log(Log.BRIEF, "Version = " +
                TransportConstants.Version + ", ep = " + getEndpoint());
        }
    }

    /**
     * Closes all cached connections in every channel subordinated to this
     * transport.  Currently, this only closes outgoing connections.
     */
    public void shedConnectionCaches() {
        List<TCPChannel> channels;
        synchronized (channelTable) {
            channels = new ArrayList<TCPChannel>(channelTable.values().size());
            for (Reference<TCPChannel> ref : channelTable.values()) {
                TCPChannel ch = ref.get();
                if (ch != null) {
                    channels.add(ch);
                }
            }
        }
        for (TCPChannel channel : channels) {
            channel.shedCache();
        }
    }

    /**
     * Returns a <I>Channel</I> that generates connections to the
     * endpoint <I>ep</I>. A Channel is an object that creates and
     * manages connections of a particular type to some particular
     * address space.
     * @param ep the endpoint to which connections will be generated.
     * @return the channel or null if the transport cannot
     * generate connections to this endpoint
     */
    public TCPChannel getChannel(Endpoint ep) {
        TCPChannel ch = null;
        if (ep instanceof TCPEndpoint) {
            synchronized (channelTable) {
                Reference<TCPChannel> ref = channelTable.get(ep);
                if (ref != null) {
                    ch = ref.get();
                }
                if (ch == null) {
                    TCPEndpoint tcpEndpoint = (TCPEndpoint) ep;
                    ch = new TCPChannel(this, tcpEndpoint);
                    channelTable.put(tcpEndpoint,
                                     new WeakReference<TCPChannel>(ch));
                }
            }
        }
        return ch;
    }

    /**
     * Removes the <I>Channel</I> that generates connections to the
     * endpoint <I>ep</I>.
     */
    public void free(Endpoint ep) {
        if (ep instanceof TCPEndpoint) {
            synchronized (channelTable) {
                Reference<TCPChannel> ref = channelTable.remove(ep);
                if (ref != null) {
                    TCPChannel channel = ref.get();
                    if (channel != null) {
                        channel.shedCache();
                    }
                }
            }
        }
    }

    /**
     * Export the object so that it can accept incoming calls.
     */
    public void exportObject(Target target) throws RemoteException {
        /*
         * Ensure that a server socket is listening, and count this
         * export while synchronized to prevent the server socket from
         * being closed due to concurrent unexports.
         */
        synchronized (this) {
            listen();
            exportCount++;
        }

        /*
         * Try to add the Target to the exported object table; keep
         * counting this export (to keep server socket open) only if
         * that succeeds.
         */
        boolean ok = false;
        try {
            super.exportObject(target);
            ok = true;
        } finally {
            if (!ok) {
                synchronized (this) {
                    decrementExportCount();
                }
            }
        }
    }

    protected synchronized void targetUnexported() {
        decrementExportCount();
    }

    /**
     * Decrements the count of exported objects, closing the current
     * server socket if the count reaches zero.
     **/
    private void decrementExportCount() {
        assert Thread.holdsLock(this);
        exportCount--;
        if (tcpLog.isLoggable(Log.VERBOSE)) {
            tcpLog.log(Log.VERBOSE,
                    "server socket: " + server + ", exportCount: " + exportCount);
        }
        if (exportCount == 0 && getEndpoint().getListenPort() != 0) {
            ServerSocket ss = server;
            server = null;
            try {
                if (tcpLog.isLoggable(Log.BRIEF)) {
                    tcpLog.log(Log.BRIEF, "server socket close: " + ss);
                }
                ss.close();
            } catch (IOException e) {
                if (tcpLog.isLoggable(Log.BRIEF)) {
                    tcpLog.log(Log.BRIEF,
                            "server socket close throws: " + e);
                }
            }
        }
    }

    /**
     * Verify that the current access control context has permission to
     * accept the connection being dispatched by the current thread.
     */
    protected void checkAcceptPermission(@SuppressWarnings("removal") AccessControlContext acc) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return;
        }
        ConnectionHandler h = threadConnectionHandler.get();
        if (h == null) {
            throw new Error(
                "checkAcceptPermission not in ConnectionHandler thread");
        }
        h.checkAcceptPermission(sm, acc);
    }

    private TCPEndpoint getEndpoint() {
        synchronized (epList) {
            return epList.getLast();
        }
    }

    /**
     * Listen on transport's endpoint.
     */
    private void listen() throws RemoteException {
        assert Thread.holdsLock(this);
        TCPEndpoint ep = getEndpoint();
        int port = ep.getPort();

        if (server == null) {
            if (tcpLog.isLoggable(Log.BRIEF)) {
                tcpLog.log(Log.BRIEF,
                    "(port " + port + ") create server socket");
            }

            try {
                server = ep.newServerSocket();
                /*
                 * Don't retry ServerSocket if creation fails since
                 * "port in use" will cause export to hang if an
                 * RMIFailureHandler is not installed.
                 */
                @SuppressWarnings("removal")
                Thread t = AccessController.doPrivileged(
                    new NewThreadAction(new AcceptLoop(server),
                                        "TCP Accept-" + port, true));
                t.start();
            } catch (java.net.BindException e) {
                throw new ExportException("Port already in use: " + port, e);
            } catch (IOException e) {
                throw new ExportException("Listen failed on port: " + port, e);
            }

        } else {
            // otherwise verify security access to existing server socket
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkListen(port);
            }
        }
    }

    /**
     * Worker for accepting connections from a server socket.
     **/
    private class AcceptLoop implements Runnable {

        private final ServerSocket serverSocket;

        // state for throttling loop on exceptions (local to accept thread)
        private long lastExceptionTime = 0L;
        private int recentExceptionCount;

        AcceptLoop(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run() {
            try {
                executeAcceptLoop();
            } finally {
                try {
                    if (tcpLog.isLoggable(Log.BRIEF)) {
                        tcpLog.log(Log.BRIEF,
                                "server socket close: " + serverSocket);
                    }
                    /*
                     * Only one accept loop is started per server
                     * socket, so after no more connections will be
                     * accepted, ensure that the server socket is no
                     * longer listening.
                     */
                    serverSocket.close();
                } catch (IOException e) {
                    if (tcpLog.isLoggable(Log.BRIEF)) {
                        tcpLog.log(Log.BRIEF,
                                "server socket close throws: " + e);
                    }
                }
            }
        }

        /**
         * Accepts connections from the server socket and executes
         * handlers for them in the thread pool.
         **/
        private void executeAcceptLoop() {
            if (tcpLog.isLoggable(Log.BRIEF)) {
                tcpLog.log(Log.BRIEF, "listening on port " +
                           getEndpoint().getPort());
            }

            while (true) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();

                    /*
                     * Find client host name (or "0.0.0.0" if unknown)
                     */
                    InetAddress clientAddr = socket.getInetAddress();
                    String clientHost = (clientAddr != null
                                         ? clientAddr.getHostAddress()
                                         : "0.0.0.0");

                    /*
                     * Execute connection handler in the thread pool,
                     * which uses non-system threads.
                     */
                    try {
                        connectionThreadPool.execute(
                            new ConnectionHandler(socket, clientHost));
                    } catch (RejectedExecutionException e) {
                        closeSocket(socket);
                        tcpLog.log(Log.BRIEF,
                                   "rejected connection from " + clientHost);
                    }

                } catch (Throwable t) {
                    try {
                        /*
                         * If the server socket has been closed, such
                         * as because there are no more exported
                         * objects, then we expect accept to throw an
                         * exception, so just terminate normally.
                         */
                        if (serverSocket.isClosed()) {
                            break;
                        }

                        try {
                            if (tcpLog.isLoggable(Level.WARNING)) {
                                tcpLog.log(Level.WARNING,
                                           "accept loop for " + serverSocket +
                                           " throws", t);
                            }
                        } catch (Throwable tt) {
                        }
                    } finally {
                        /*
                         * Always close the accepted socket (if any)
                         * if an exception occurs, but only after
                         * logging an unexpected exception.
                         */
                        if (socket != null) {
                            closeSocket(socket);
                        }
                    }

                    /*
                     * In case we're running out of file descriptors,
                     * release resources held in caches.
                     */
                    if (!(t instanceof SecurityException)) {
                        try {
                            TCPEndpoint.shedConnectionCaches();
                        } catch (Throwable tt) {
                        }
                    }

                    /*
                     * A NoClassDefFoundError can occur if no file
                     * descriptors are available, in which case this
                     * loop should not terminate.
                     */
                    if (t instanceof Exception ||
                        t instanceof OutOfMemoryError ||
                        t instanceof NoClassDefFoundError)
                    {
                        if (!continueAfterAcceptFailure(t)) {
                            return;
                        }
                        // continue loop
                    } else if (t instanceof Error) {
                        throw (Error) t;
                    } else {
                        throw new UndeclaredThrowableException(t);
                    }
                }
            }
        }

        /**
         * Returns true if the accept loop should continue after the
         * specified exception has been caught, or false if the accept
         * loop should terminate (closing the server socket).  If
         * there is an RMIFailureHandler, this method returns the
         * result of passing the specified exception to it; otherwise,
         * this method always returns true, after sleeping to throttle
         * the accept loop if necessary.
         **/
        private boolean continueAfterAcceptFailure(Throwable t) {
            RMIFailureHandler fh = RMISocketFactory.getFailureHandler();
            if (fh != null) {
                return fh.failure(t instanceof Exception ? (Exception) t :
                                  new InvocationTargetException(t));
            } else {
                throttleLoopOnException();
                return true;
            }
        }

        /**
         * Throttles the accept loop after an exception has been
         * caught: if a burst of 10 exceptions in 5 seconds occurs,
         * then wait for 10 seconds to curb busy CPU usage.
         **/
        private void throttleLoopOnException() {
            long now = System.currentTimeMillis();
            if (lastExceptionTime == 0L || (now - lastExceptionTime) > 5000) {
                // last exception was long ago (or this is the first)
                lastExceptionTime = now;
                recentExceptionCount = 0;
            } else {
                // exception burst window was started recently
                if (++recentExceptionCount >= 10) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
    }

    /** close socket and eat exception */
    private static void closeSocket(Socket sock) {
        try {
            if (tcpLog.isLoggable(Log.BRIEF)) {
                tcpLog.log(Log.BRIEF, "socket close: " + sock);
            }
            sock.close();
        } catch (IOException ex) {
            // eat exception
            if (tcpLog.isLoggable(Log.BRIEF)) {
                tcpLog.log(Log.BRIEF, "socket close throws: " + ex);
            }
        }
    }

    /**
     * handleMessages decodes transport operations and handles messages
     * appropriately.  If an exception occurs during message handling,
     * the socket is closed.
     */
    void handleMessages(Connection conn, boolean persistent) {
        int port = getEndpoint().getPort();

        try {
            DataInputStream in = new DataInputStream(conn.getInputStream());
            do {
                int op = in.read();     // transport op
                if (op == -1) {
                    if (tcpLog.isLoggable(Log.BRIEF)) {
                        tcpLog.log(Log.BRIEF, "(port " +
                            port + ") connection closed");
                    }
                    break;
                }

                if (tcpLog.isLoggable(Log.BRIEF)) {
                    tcpLog.log(Log.BRIEF, "(port " + port +
                        ") op = " + op);
                }

                switch (op) {
                case TransportConstants.Call:
                    // service incoming RMI call
                    RemoteCall call = new StreamRemoteCall(conn);
                    if (serviceCall(call) == false)
                        return;
                    break;

                case TransportConstants.Ping:
                    // send ack for ping
                    DataOutputStream out =
                        new DataOutputStream(conn.getOutputStream());
                    out.writeByte(TransportConstants.PingAck);
                    conn.releaseOutputStream();
                    break;

                case TransportConstants.DGCAck:
                    DGCAckHandler.received(UID.read(in));
                    break;

                default:
                    throw new IOException("unknown transport op " + op);
                }
            } while (persistent);

        } catch (IOException e) {
            // exception during processing causes connection to close (below)
            if (tcpLog.isLoggable(Log.BRIEF)) {
                tcpLog.log(Log.BRIEF, "(port " + port +
                    ") exception: ", e);
            }
        } finally {
            try {
                conn.close();
            } catch (IOException ex) {
                // eat exception
                if (tcpLog.isLoggable(Log.BRIEF)) {
                    tcpLog.log(Log.BRIEF, "Connection close throws " + ex);
                }
            }
        }
    }

    /**
     * Returns the client host for the current thread's connection.  Throws
     * ServerNotActiveException if no connection is active for this thread.
     */
    public static String getClientHost() throws ServerNotActiveException {
        ConnectionHandler h = threadConnectionHandler.get();
        if (h != null) {
            return h.getClientHost();
        } else {
            throw new ServerNotActiveException("not in a remote call");
        }
    }

    /**
     * Services messages on accepted connection
     */
    private class ConnectionHandler implements Runnable {

        /** int value of "POST" in ASCII (Java's specified data formats
         *  make this once-reviled tactic again socially acceptable) */
        private static final int POST = 0x504f5354;

        /** most recently accept-authorized AccessControlContext */
        @SuppressWarnings("removal")
        private AccessControlContext okContext;
        /** cache of accept-authorized AccessControlContexts */
        @SuppressWarnings("removal")
        private Map<AccessControlContext,
                    Reference<AccessControlContext>> authCache;
        /** security manager which authorized contexts in authCache */
        @SuppressWarnings("removal")
        private SecurityManager cacheSecurityManager = null;

        private Socket socket;
        private String remoteHost;

        ConnectionHandler(Socket socket, String remoteHost) {
            this.socket = socket;
            this.remoteHost = remoteHost;
        }

        String getClientHost() {
            return remoteHost;
        }

        /**
         * Verify that the given AccessControlContext has permission to
         * accept this connection.
         */
        @SuppressWarnings("removal")
        void checkAcceptPermission(SecurityManager sm,
                                   AccessControlContext acc)
        {
            /*
             * Note: no need to synchronize on cache-related fields, since this
             * method only gets called from the ConnectionHandler's thread.
             */
            if (sm != cacheSecurityManager) {
                okContext = null;
                authCache = new WeakHashMap<AccessControlContext,
                                            Reference<AccessControlContext>>();
                cacheSecurityManager = sm;
            }
            if (acc.equals(okContext) || authCache.containsKey(acc)) {
                return;
            }
            InetAddress addr = socket.getInetAddress();
            String host = (addr != null) ? addr.getHostAddress() : "*";

            sm.checkAccept(host, socket.getPort());

            authCache.put(acc, new SoftReference<AccessControlContext>(acc));
            okContext = acc;
        }

        @SuppressWarnings("removal")
        public void run() {
            Thread t = Thread.currentThread();
            String name = t.getName();
            try {
                t.setName("RMI TCP Connection(" +
                          connectionCount.incrementAndGet() +
                          ")-" + remoteHost);
                AccessController.doPrivileged((PrivilegedAction<Void>)() -> {
                    run0();
                    return null;
                }, NOPERMS_ACC);
            } finally {
                t.setName(name);
            }
        }

        @SuppressWarnings("fallthrough")
        private void run0() {
            TCPEndpoint endpoint = getEndpoint();
            int port = endpoint.getPort();

            threadConnectionHandler.set(this);

            // set socket to disable Nagle's algorithm (always send
            // immediately)
            // TBD: should this be left up to socket factory instead?
            try {
                socket.setTcpNoDelay(true);
            } catch (Exception e) {
                // if we fail to set this, ignore and proceed anyway
            }
            // set socket to timeout after excessive idle time
            try {
                if (connectionReadTimeout > 0)
                    socket.setSoTimeout(connectionReadTimeout);
            } catch (Exception e) {
                // too bad, continue anyway
            }

            try {
                InputStream sockIn = socket.getInputStream();
                InputStream bufIn = sockIn.markSupported()
                        ? sockIn
                        : new BufferedInputStream(sockIn);

                // Read magic
                DataInputStream in = new DataInputStream(bufIn);
                int magic = in.readInt();

                // read and verify transport header
                short version = in.readShort();
                if (magic != TransportConstants.Magic ||
                    version != TransportConstants.Version) {
                    // protocol mismatch detected...
                    // just close socket: this would recurse if we marshal an
                    // exception to the client and the protocol at other end
                    // doesn't match.
                    if (tcpLog.isLoggable(Log.BRIEF)) {
                        tcpLog.log(Log.BRIEF, "magic or version not match: "
                                                  + magic + ", " + version);
                    }
                    closeSocket(socket);
                    return;
                }

                OutputStream sockOut = socket.getOutputStream();
                BufferedOutputStream bufOut =
                    new BufferedOutputStream(sockOut);
                DataOutputStream out = new DataOutputStream(bufOut);

                int remotePort = socket.getPort();

                if (tcpLog.isLoggable(Log.BRIEF)) {
                    tcpLog.log(Log.BRIEF, "accepted socket from [" +
                                     remoteHost + ":" + remotePort + "]");
                }

                TCPEndpoint ep;
                TCPChannel ch;
                TCPConnection conn;

                // send ack (or nack) for protocol
                byte protocol = in.readByte();
                switch (protocol) {
                case TransportConstants.SingleOpProtocol:
                    // no ack for protocol

                    // create dummy channel for receiving messages
                    ep = new TCPEndpoint(remoteHost, socket.getLocalPort(),
                                         endpoint.getClientSocketFactory(),
                                         endpoint.getServerSocketFactory());
                    ch = new TCPChannel(TCPTransport.this, ep);
                    conn = new TCPConnection(ch, socket, bufIn, bufOut);

                    // read input messages
                    handleMessages(conn, false);
                    break;

                case TransportConstants.StreamProtocol:
                    // send ack
                    out.writeByte(TransportConstants.ProtocolAck);

                    // suggest endpoint (in case client doesn't know host name)
                    if (tcpLog.isLoggable(Log.VERBOSE)) {
                        tcpLog.log(Log.VERBOSE, "(port " + port +
                            ") " + "suggesting " + remoteHost + ":" +
                            remotePort);
                    }

                    out.writeUTF(remoteHost);
                    out.writeInt(remotePort);
                    out.flush();

                    // read and discard (possibly bogus) endpoint
                    // REMIND: would be faster to read 2 bytes then skip N+4
                    String clientHost = in.readUTF();
                    int    clientPort = in.readInt();
                    if (tcpLog.isLoggable(Log.VERBOSE)) {
                        tcpLog.log(Log.VERBOSE, "(port " + port +
                            ") client using " + clientHost + ":" + clientPort);
                    }

                    // create dummy channel for receiving messages
                    // (why not use clientHost and clientPort?)
                    ep = new TCPEndpoint(remoteHost, socket.getLocalPort(),
                                         endpoint.getClientSocketFactory(),
                                         endpoint.getServerSocketFactory());
                    ch = new TCPChannel(TCPTransport.this, ep);
                    conn = new TCPConnection(ch, socket, bufIn, bufOut);

                    // read input messages
                    handleMessages(conn, true);
                    break;

                case TransportConstants.MultiplexProtocol:
                    if (tcpLog.isLoggable(Log.VERBOSE)) {
                        tcpLog.log(Log.VERBOSE, "(port " + port +
                                ") rejecting multiplex protocol");
                    }
                    // Fall-through to reject use of MultiplexProtocol
                default:
                    // protocol not understood, send nack and close socket
                    out.writeByte(TransportConstants.ProtocolNack);
                    out.flush();
                    break;
                }

            } catch (IOException e) {
                // socket in unknown state: destroy socket
                tcpLog.log(Log.BRIEF, "terminated with exception:", e);
            } finally {
                closeSocket(socket);
            }
        }
    }
}
