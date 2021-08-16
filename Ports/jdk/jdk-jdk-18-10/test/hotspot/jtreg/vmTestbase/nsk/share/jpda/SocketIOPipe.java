/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import nsk.share.*;

/*
 * This class represents communication channel based on TCP/IP sockets.
 * Usage of this class implies creation of objects of 2 types: server SocketIOPipe object
 * (this object creates server socket and waits for incoming connection) and client
 * SocketIOPipe (this object attaches to server).
 *
 * Server and client objects should be created using special static methods provided by this class,
 * for example 'createServerIOPipe(Log log, int port, long timeout)' for server SocketIOPipe
 * and 'createClientIOPipe(Log log, String host, int port, long timeout)' for client SocketIOPipe.
 *
 * When SocketIOPipe is created it can be used to send and receive strings using methods 'readln()' and 'println(String s)'.
 * TCP/IP connection is established at the first attempt to read or write data.
 *
 * For example, if client process should send string 'OK' to the server process which is run
 * at the host 'SERVER_HOST' following code can be written:
 *
 * Server side:
 *
 *  // SocketIOPipe creates ServerSocket listening given port
 *  SocketIOPipe pipe = SocketIOPipe.createServerIOPipe(log, port, timeoutValue);
 *
 *  // SocketIOPipe waits connection from client and reads data sent by the client
 *  String command = pipe.readln();
 *
 * Client side:
 *
 *  // initialize SocketIOPipe with given values of server host name and port
 *  SocketIOPipe pipe = SocketIOPipe.createClientIOPipe(log, 'SERVER_HOST', port, timeoutValue);
 *
 *  String command = "OK";
 *  // SocketIOPipe tries to create socket and send command to the server
 *  pipe.println(command);
 *
 */
public class SocketIOPipe extends Log.Logger implements Finalizable {

    public static final int DEFAULT_TIMEOUT_VALUE = 1 * 60 * 1000;

    public static final String DEFAULT_PIPE_LOG_PREFIX = "SocketIOPipe> ";

    protected boolean listening;

    protected String host;

    protected int port;

    protected long timeout;

    protected SocketConnection connection;

    protected volatile boolean shouldStop;

    protected ServerSocket serverSocket;

    protected String name;

    /**
     * Make general <code>IOPipe</code> object with specified parameters.
     */
    protected SocketIOPipe(String name, Log log, String logPrefix, String host, int port, long timeout, boolean listening) {
        super(log, logPrefix);
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.listening = listening;
        this.name = name;
    }

    /**
     * Make general <code>IOPipe</code> object with specified parameters.
     */
    protected SocketIOPipe(Log log, String logPrefix, String host, int port, long timeout, boolean listening) {
        super(log, logPrefix);
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.listening = listening;
    }

    /**
     *  Create listening SocketIOPipe using given port
     */
    public static SocketIOPipe createServerIOPipe(Log log, int port, long timeout) {
        SocketIOPipe pipe = new SocketIOPipe(log, DEFAULT_PIPE_LOG_PREFIX, null, 0, timeout, true);

        try {
            ServerSocket ss = new ServerSocket();
            if (port == 0) {
              // Only need SO_REUSEADDR if we're using a fixed port. If we
              // start seeing EADDRINUSE due to collisions in free ports
              // then we should retry the bind() a few times.
              ss.setReuseAddress(false);
            }
            ss.bind(new InetSocketAddress(port));
            pipe.setServerSocket(ss);
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Caught IOException while binding for IOPipe connection: \n\t" + e);
        }

        return pipe;
    }

    /**
     *  Create listening SocketIOPipe using any free port
     */
    public static SocketIOPipe createServerIOPipe(Log log, long timeout) {
        return createServerIOPipe(log, 0, timeout);
    }

    /**
     *  Create attaching SocketIOPipe using given port and timeout
     */
    public static SocketIOPipe createClientIOPipe(Log log, String host, int port, long timeout) {
        return new SocketIOPipe(log, DEFAULT_PIPE_LOG_PREFIX, host, port, timeout, false);
    }

    /**
     * Return true if <code>IOPipe</code> connection established.
     */
    public boolean isConnected() {
        return (connection != null && connection.isConnected());
    }

    /**
     * Returns port number used by SocketIOPipe
     */
    public int getPort() {
        return port;
    }

    protected void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        if (serverSocket != null)
            port = serverSocket.getLocalPort();
    }

    /**
     * Write (and flush) given <code>line</code> to this
     * <code>IOPipe</code> cnannel.
     *
     * @throws Failure if error occured while sending data
     */
    public void println(String line) {
        if (connection == null) {
            connect();
        }
        connection.writeObject(line);
    }

    /**
     * Read a text line from this <code>IOPipe</code> channel,
     * or return <i>null</i> if EOF reached.
     *
     * @throws Failure if error occured while reading data
     */
    public String readln() {
        if (connection == null) {
            connect();
        }
        String line = (String) connection.readObject();
        return line;
    }

    /**
     * Close this <code>IOPipe</code> connection.
     */
    public void close() {
        shouldStop = true;
        if (connection != null) {
            connection.close();
        }
    }

    protected class ListenerThread extends Thread {
        private SocketConnection connection;
        private RuntimeException error;

        ListenerThread() {
            super("PipeIO Listener Thread");
            setDaemon(true);

            connection = new SocketConnection(SocketIOPipe.this, getName());

            if (serverSocket == null) {
                connection.bind(port, timeout);
            } else {
                connection.setServerSocket(serverSocket);
            }
        }

        @Override
        public void run() {
            synchronized (this) {
                try {
                    connection.accept(timeout);
                } catch (Throwable th) {
                    error = th instanceof RuntimeException
                            ? (RuntimeException)th
                            : new RuntimeException(th);
                }
                notifyAll();
            }
        }

        public SocketConnection getConnection() {
            synchronized (this) {
                while (!connection.isConnected() && error == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                if (error != null) {
                    throw error;
                }
                return connection;
            }
        }
    }

    private ListenerThread listenerThread;

    protected void startListening() {
        if (listenerThread != null) {
            throw new TestBug("already listening");
        }
        listenerThread = new ListenerThread();
        listenerThread.start();
    }

    /**
     * Establish <code>IOPipe</code> connection by attaching or accepting
     * connection appropriately.
     */
    protected void connect() {
        if (connection != null) {
            throw new TestBug("IOPipe connection is already established");
        }

        if (shouldStop)
            return;

        if (listening) {
            // listenerThread == null means the test is not updated yet
            // to start IOPipe listening before launching debuggee.
            if (listenerThread == null) {
                // start listening and accept connection on the current thread
                listenerThread = new ListenerThread();
                listenerThread.run();
            }
            connection = listenerThread.getConnection();
        } else {
            connection = new SocketConnection(this, getName());
            // attach from the debuggee's side
            connection.continueAttach(host, port, timeout);
        }
    }

    /**
     * Set ping timeout in milliseconds (0 means don't use ping at all).
     */
    public void setPingTimeout(long timeout) {
        if (connection == null) {
            throw new TestBug("Attempt to set ping timeout for not established connection");
        }
        connection.setPingTimeout(timeout);
    }

    /**
     * Returns value of current ping timeout in milliseconds (0 means ping is not used).
     */
    public long getPingTimeout() {
        if (connection == null) {
            throw new TestBug("Attempt to get ping timeout for not established connection");
        }
        return connection.getPingTimeout();
    }

    /**
     * Perform finalization of the object by invoking close().
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Perform finalization of the object at exit by invoking finalize().
     */
    public void finalizeAtExit() throws Throwable {
        finalize();
    }

    /**
     * Field 'pipeCounter' and method 'getNextPipeNumber' are used to construct unique names for SocketIOPipes
     */
    private static int pipeCounter;

    private synchronized int getNextPipeNumber() {
        return pipeCounter++;
    }

    /**
     * Construct name for SocketIOPipe if it wasn't specified
     */
    private String getName() {
        if (name == null) {
            name = "SocketIOPipe-" + getNextPipeNumber();
        }

        return name;
    }
}
