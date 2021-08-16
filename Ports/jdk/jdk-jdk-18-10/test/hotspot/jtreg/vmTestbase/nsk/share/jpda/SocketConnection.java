/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jpda;

import java.io.*;
import java.net.*;

import nsk.share.*;

/**
 * This class implements basic connection channel via TCP/IP sockets.
 */
class BasicSocketConnection {

    protected static int TRACE_LEVEL_PACKETS = 10;

    protected static int TRACE_LEVEL_THREADS = 20;

    protected static int TRACE_LEVEL_ACTIONS = 30;

    protected static int TRACE_LEVEL_SOCKETS = 40;

    protected static int TRACE_LEVEL_IO = 50;

    protected String name = null;

    protected ServerSocket serverSocket = null;

    protected Socket socket = null;

    protected InputStream sin = null;

    protected OutputStream sout = null;

    protected volatile boolean connected = false;

    protected volatile boolean closed = false;

    protected volatile boolean connectionClosed = false;

    protected volatile boolean shouldStop = false;

    protected Log.Logger logger = null;

    /**
     * Make an empty connection with specified name.
     *
     * @param logger
     *                Logger object for printing log messages
     * @param name
     *                connection name
     */
    public BasicSocketConnection(Log.Logger logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    /**
     * Try to bind connection to the local port.
     *
     * @param port
     *                port number to bind to
     *
     * @throws IOException
     *                 if error occured while binding
     */
    protected void tryBind(int port) throws IOException {
        logger.trace(TRACE_LEVEL_IO, "Binding for " + name + " connection to port: " + port);
        serverSocket = new ServerSocket(port, 1);
        logger.trace(TRACE_LEVEL_IO, "Bound for " + name + " connection to port: " + port);
    }

    /**
     * Bind connection to the local port for specified timeout.
     *
     * @param port
     *                port number to bind to
     * @param timeout
     *                binding timeout in milliseconds
     *
     * @throws Failure
     *                 if error ocured while binding
     */
    protected void bind(int port, long timeout) {
        BindException bindException = null;
        long timeToFinish = System.currentTimeMillis() + timeout;
        for (long i = 0; !shouldStop && (timeout == 0 || System.currentTimeMillis() < timeToFinish); i++) {
            try {
                tryBind(port);
                return;
            } catch (BindException e) {
                bindException = e;
                logger.display("Attempt #" + i + " to bind to port " + port + " failed:\n\t" + e);
                try {
                    Thread.sleep(DebugeeBinder.CONNECT_TRY_DELAY);
                } catch (InterruptedException ie) {
                    ie.printStackTrace(logger.getOutStream());
                    throw new Failure("Thread interrupted while binding for " + name + " connection to port " + port + ":\n\t" + ie);
                }
            } catch (IOException e) {
                e.printStackTrace(logger.getOutStream());
                throw new Failure("Caught IOException while binding for " + name + " connection to port " + port + ":\n\t" + e);
            }
        }
        throw new Failure("Unable to bind for " + name + " connection to port " + port + " for " + timeout + "ms timeout:\n\t" + bindException);
    }

    /**
     * Accept connection at the bound port for specified timeout.
     *
     * @param timeout
     *                accepting timeout in milliseconds
     *
     * @throws Failure
     *                 if error occured while accepting connection
     */
    public void accept(long timeout) {
        int port = serverSocket.getLocalPort();
        logger.trace(TRACE_LEVEL_IO, "Listening for " + name + " connection at port: " + port);
        socket = null;
        try {
            if (timeout > Integer.MAX_VALUE) {
                throw new TestBug("Too large timeout long value: " + timeout + " (can't cast it to int)");
            }

            serverSocket.setSoTimeout((int)timeout);

            long waitStartTime = System.currentTimeMillis();

            /*
             * We found that sometimes (very rarely) on Solaris ServerSocket.accept() throws InterruptedIOException
             * even if connection timeout (specified through ServerSocket.setSoTimeout) didn't expire.
             * Following code tries to catch such case and call ServerSocket.accept() while timeout didn't expire.
             */
            do {
                try {
                    socket = serverSocket.accept();
                    logger.trace(TRACE_LEVEL_IO, "Accepted " + name + " connection at port: " + port);
                } catch (InterruptedIOException e) {
                    long interruptTime = System.currentTimeMillis();
                    long waitTime = interruptTime - waitStartTime;

                    logger.display("Caught InterruptedIOException. Wait start time: " + waitStartTime + ", exception was thrown at: "
                            + interruptTime + ", wait time: " + (interruptTime - waitStartTime) + ", actual timeout: " + timeout);

                    // if waitTime was too small call ServerSocket.accept() one more time
                    if (!shouldStop && (waitTime < (timeout / 2))) {
                        logger.display("InterruptedIOException was thrown too early, trying to call ServerSocket.accept() one more time");
                        continue;
                    } else {
                        if (!shouldStop) {
                            logger.complain("Caught InterruptedIOException while listening for " + name + " connection at port " + port + ":\n\t" + e);
                            throw new Failure("Connection for " + name +
                                    " at port " + port +
                                    " wasn't accepted in " + timeout + "ms");
                        } else {
                            logger.display("Listening was interrupted (caught InterruptedIOException while listening for " + name + " connection at port " + port + ":\n\t" + e + ")");
                            break;
                        }
                    }
                }
            } while (socket == null);

        } catch (IOException e) {
            if (!shouldStop) {
                e.printStackTrace(logger.getOutStream());
                throw new Failure("Caught IOException while listening for " + name + " connection at port " + port + ":\n\t" + e);
            } else {
                logger.display("Listening was interrupted (caught InterruptedIOException while listening for " + name + " connection at port " + port + ":\n\t" + e + ")");
            }
        } finally {
            closeServerConnection();
        }

        if (!shouldStop) {
            if (socket == null) {
                throw new Failure("No " + name + " connection accepted at port " + port + " for " + timeout + "ms timeout");
            }

            onConnected();
        }
    }

    /**
     * Attach connection to the remote host and port.
     *
     * @param host
     *                name of remote host to attach to
     * @param port
     *                port number to attach to
     *
     * @throws Failure
     *                 if error occured while attaching
     */
    public void attach(String host, int port) {
        try {
            logger.trace(TRACE_LEVEL_IO, "Attaching for " + name + " connection to host: " + host + ":" + port);
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            logger.trace(TRACE_LEVEL_IO, "Attached for " + name + " connection to host: " + host + ":" + port);
        } catch (IOException e) {
            e.printStackTrace(logger.getOutStream());
            throw new Failure("Caught IOException while attaching for " + name + " connection to " + host + ":" + port + ":\n\t" + e);
        }
        if (!shouldStop) {
            onConnected();
        }
    }

    /**
     * Continuously attach to the remote host for the specified timeout.
     *
     * @param host
     *                name of remote host to attach to
     * @param port
     *                port number to attach to
     * @param timeout
     *                attaching timeout in milliseconds
     *
     * @throws Failure
     *                 if error occured while attaching
     */
    public void continueAttach(String host, int port, long timeout) {
        socket = null;
        long timeToFinish = System.currentTimeMillis() + timeout;
        ConnectException lastException = null;
        logger.trace(TRACE_LEVEL_IO, "Attaching for " + name + " connection to host: " + host + ":" + port);
        try {
            for (long i = 0; !shouldStop && (timeout == 0 || System.currentTimeMillis() < timeToFinish); i++) {
                try {
                    socket = new Socket(host, port);
                    logger.trace(TRACE_LEVEL_IO, "Attached for " + name + " connection to host: " + host + ":" + port);
                    break;
                } catch (ConnectException e) {
                    logger.display("Attempt #" + i + " to attach for " + name + " connection failed:\n\t" + e);
                    lastException = e;
                    // sleep between attempts
                    try {
                        Thread.sleep(DebugeeBinder.CONNECT_TRY_DELAY);
                    } catch (InterruptedException ie) {
                        throw new Failure("Thread interrupted while attaching for " + name + " connection to " + host + ":" + port + ":\n\t" + ie);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace(logger.getOutStream());
            throw new Failure("Caught IOException while attaching for " + name + " connection to " + host + ":" + port + ":\n\t" + e);
        }

        if (!shouldStop) {
            if (socket == null) {
                throw new Failure("Unable to attach for " + name + " connection to " + host + ":" + port + " for " + timeout + "ms timeout:\n\t"
                        + lastException);
            }

            onConnected();
        }
    }

    /**
     * Set already bound serverSocket for further connection.
     */
    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Set already connected socket for connection.
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
        if (!shouldStop) {
            onConnected();
        }
    }

    /**
     * Get socket of already established connection.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Check if connection is established.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Close socket and associated streams.
     */
    public void close() {
        if (!closed) {
            shouldStop = true;
            closeConnection();
            closed = true;
        }
    }

    /**
     * Send the specified byte throw the connection.
     */
    public void writeByte(byte b) throws IOException {
        logger.trace(TRACE_LEVEL_IO, "Writing byte: " + b);
        sout.write(b);
        sout.flush();
        logger.trace(TRACE_LEVEL_IO, "Wrote byte: " + b);
    }

    /**
     * Read a byte and return it or -1.
     */
    public int readByte() throws IOException {
        logger.trace(TRACE_LEVEL_IO, "Reading byte");
        int b = sin.read();
        logger.trace(TRACE_LEVEL_IO, "Received byte: " + b);
        return b;
    }

    /**
     * Perform some actions after connection established.
     */
    protected void onConnected() {
        if (!shouldStop) {
            setSocketOptions();
            makeSocketStreams();
            connected = true;
        }
    }

    /**
     * Set socket options after connection established.
     */
    protected void setSocketOptions() {
    }

    /**
     * Close server socket.
     */
    protected void closeServerConnection() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                logger.trace(TRACE_LEVEL_IO, "ServerSocket closed: " + serverSocket);
            } catch (IOException e) {
                logger.display("# WARNING: " + "Caught IOException while closing ServerSocket of " + name + " connection:\n\t" + e);
            }
        }
    }

    /**
     * Close socket of connection to remote host.
     */
    protected void closeHostConnection() {
        if (socket != null) {
            try {
                socket.close();
                logger.trace(TRACE_LEVEL_IO, "Socket closed: " + socket);
            } catch (IOException e) {
                logger.display("# WARNING: " + "Caught IOException while closing socket of " + name + " connection:\n\t" + e);
            }
        }
    }

    /**
     * Close socket streams.
     */
    protected void closeSocketStreams() {
        if (sout != null) {
            try {
                logger.trace(TRACE_LEVEL_IO, "Closing socket output stream: " + sout);
                sout.close();
                logger.trace(TRACE_LEVEL_IO, "Output stream closed: " + sout);
            } catch (IOException e) {
                logger.display("# WARNING: " + "Caught IOException while closing OutputStream of " + name + " connection:\n\t" + e);
            }
        }
        if (sin != null) {
            try {
                logger.trace(TRACE_LEVEL_IO, "Closing socket input stream: " + sin);
                sin.close();
                logger.trace(TRACE_LEVEL_IO, "Input stream closed: " + sin);
            } catch (IOException e) {
                logger.display("# WARNING: " + "Caught IOException while closing InputStream of" + name + " connection:\n\t" + e);
            }
        }
    }

    /**
     * Close sockets and associated streams.
     */
    protected void closeConnection() {
        if (connectionClosed)
            return;

        logger.trace(TRACE_LEVEL_IO, "Closing " + name + " connection");
        closeSocketStreams();
        closeHostConnection();
        closeServerConnection();
        connectionClosed = true;
    }

    /**
     * Make up socket streams after connection established.
     */
    protected void makeSocketStreams() {
        try {
            logger.trace(TRACE_LEVEL_IO, "Getting input/output socket streams for " + name + " connection");
            sout = socket.getOutputStream();
            logger.trace(TRACE_LEVEL_IO, "Got socket output stream: " + sout);
            sin = socket.getInputStream();
            logger.trace(TRACE_LEVEL_IO, "Got socket input stream: " + sin);
        } catch (IOException e) {
            e.printStackTrace(logger.getOutStream());
            throw new Failure("Caught exception while making streams for " + name + " connection:\n\t" + e);
        }
    }

} // BasicSocketConnection

/**
 * This class implements connection channel via TCP/IP sockets. After connection
 * established special inner threads are started, which periodically test the
 * connection by pinging each other. If ping timeout occurs connection is closed
 * and any thread waiting for read from this connection gets exception.
 *
 * @see #setPingTimeout(long)
 */
public class SocketConnection extends BasicSocketConnection {

    private static final long PING_INTERVAL = 1 * 1000; // milliseconds

    private static byte DATA_BYTE = (byte) 0x03;

    private static byte DISCONNECT_BYTE = (byte) 0x04;

    private final Object inLock = new Object();
    private ObjectInputStream in = null;

    private final Object outLock = new Object();
    private ObjectOutputStream out = null;

    private volatile long pingTimeout = 0; // don't use ping

    /**
     * Make an empty connection with specified name.
     *
     * @param log
     *                Log object for printing log messages
     * @param name
     *                connection name
     */
    public SocketConnection(Log log, String name) {
        this(new Log.Logger(log, name + " connection> "), name);
    }

    /**
     * Make an empty connection with specified name.
     *
     * @param logger
     *                Logger object for printing log messages
     * @param name
     *                connection name
     */
    public SocketConnection(Log.Logger logger, String name) {
        super(logger, name);
    }

    /**
     * Set ping timeout in milliseconds (0 means don't use ping at all).
     */
    public void setPingTimeout(long timeout) {
        logger.display("# WARNING: Setting ping timeout for " + name + " connection ingnored: " + timeout + " ms");
        pingTimeout = timeout;
    }

    /**
     * Returns value of current ping timeout in milliseconds (0 means ping is
     * not used).
     */
    public long getPingTimeout() {
        return pingTimeout;
    }

    /**
     * Receive an object from remote host.
     */
    public Object readObject() {
        if (!isConnected()) {
            throw new Failure("Unable to read object from not established " + name + " connection");
        }

        try {
            return doReadObject();
        } catch (EOFException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace(logger.getOutStream());
            throw new Failure("Caught Exception while reading an object from " + name + " connection:\n\t" + e);
        }
    }

    /**
     * Send an object to remote host.
     */
    public void writeObject(Object object) {
        if (!isConnected()) {
            throw new Failure("Unable to send object throw not established " + name + " connection:\n\t" + object);
        }

        try {
            doWriteObject(object);
        } catch (IOException e) {
            e.printStackTrace(logger.getOutStream());
            throw new Failure("Caught IOException while writing an object to " + name + " connection:\n\t" + e);
        }
    }

    /**
     * Close socket and associated streams and finish all internal threads.
     */
    public void close() {
        if (!closed) {
            // disconnect();
            shouldStop = true;
            super.close();
            closed = true;
        }
    }

    /**
     * Perform some actions after connection has established.
     */
    protected void onConnected() {
        super.onConnected();
    }

    /**
     * Do write an object to the connection channel.
     */
    private void doWriteObject(Object object) throws IOException {
        logger.trace(TRACE_LEVEL_IO, "writing object: " + object);
        synchronized(outLock) {
            out.writeObject(object);
            out.flush();
        }
        logger.trace(TRACE_LEVEL_PACKETS, "* sent: " + object);
    }

    /**
     * Do read an object from the connection channel.
     */
    private Object doReadObject() throws IOException, ClassNotFoundException {
        logger.trace(TRACE_LEVEL_IO, "Reading object");
        Object object = null;
        synchronized(inLock) {
            object = in.readObject();
        }
        logger.trace(TRACE_LEVEL_PACKETS, "* recv: " + object);
        return object;
    }

    /**
     * Close socket streams.
     */
    protected void closeSocketStreams() {
        synchronized(outLock) {
            if (out != null) {
                try {
                    logger.trace(TRACE_LEVEL_IO, "Closing socket output stream: " + out);
                    out.close();
                    logger.trace(TRACE_LEVEL_IO, "Output stream closed: " + out);
                } catch (IOException e) {
                    logger.display("# WARNING: " + "Caught IOException while closing ObjectOutputStream of " + name + " connection:\n\t" + e);
                }
            }
        }
        synchronized(inLock) {
            if (in != null) {
                try {
                    logger.trace(TRACE_LEVEL_IO, "Closing socket input stream: " + in);
                    in.close();
                    logger.trace(TRACE_LEVEL_IO, "Input stream closed: " + in);
                } catch (IOException e) {
                    logger.display("# WARNING: " + "Caught IOException while closing ObjectInputStream of" + name + " connection:\n\t" + e);
                }
            }
        }
        super.closeSocketStreams();
    }

    /**
     * Close sockets and associated streams.
     */
    protected void closeConnection() {
        if (connectionClosed)
            return;
        connected = false;
        shouldStop = true;
        super.closeConnection();
    }

    /**
     * Make up object streams for socket.
     */
    protected void makeSocketStreams() {
        try {
            logger.trace(TRACE_LEVEL_IO, "Making input/output object streams for " + name + " connection");
            synchronized(outLock) {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
            }
            logger.trace(TRACE_LEVEL_IO, "Output stream created: " + out);
            synchronized(inLock) {
                in = new ObjectInputStream(socket.getInputStream());
            }
            logger.trace(TRACE_LEVEL_IO, "Input stream created: " + in);
        } catch (IOException e) {
            e.printStackTrace(logger.getOutStream());
            throw new Failure("Caught exception while making streams for " + name + " connection:\n\t" + e);
        }
    }

} // SocketConnection
