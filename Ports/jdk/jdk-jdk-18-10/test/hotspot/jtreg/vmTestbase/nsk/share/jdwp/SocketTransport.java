/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdwp;

import nsk.share.*;

import java.io.*;
import java.net.*;

/**
 * This class represents a socket transport for JDWP.
 */
public class SocketTransport extends Transport {

    /**
     * Socket for established JDWP connection.
     */
    private Socket socket = null;

    /**
     * ServerSocket for listening JDWP connection.
     */
    private ServerSocket serverSocket = null;

    /**
     * Input stream of socket.
     */
    private InputStream in = null;

    /**
     * Output stream of socket.
     */
    private OutputStream out = null;

    /**
     * Port to listen to.
     */
     int listenPort = 0;

    /**
     * Make <code>SocketTransport</cose> object with providing specified Log.
     */
    public SocketTransport(Log log) {
        super(log);
    }

    /**
     * Bind for connection from target VM to specified port number.
     * If given port number is 0 then bind to any free port number.
     *
     * @return port number which this transport is listening for
     */
    public int bind(int port)
            throws IOException {
        serverSocket = new ServerSocket();
        if (port == 0) {
            // Only need SO_REUSEADDR if we're using a fixed port. If we
            // start seeing EADDRINUSE due to collisions in free ports
            // then we should retry the bind() a few times.
            display("port == 0, disabling SO_REUSEADDR");
            serverSocket.setReuseAddress(false);
        }
        serverSocket.bind(new InetSocketAddress(port));
        listenPort = serverSocket.getLocalPort();
        return listenPort;
    }

    /**
     * Attach to the target VM for specified host name and port number.
     */
    public void attach(String ServerName, int PortNumber)
            throws UnknownHostException, IOException {
        for (int i = 0; i < Binder.CONNECT_TRIES; i++ ) {
            try {
                socket = new Socket(ServerName, PortNumber);
                display("JDWP socket connection established");
//                socket.setTcpNoDelay(true);
                in = socket.getInputStream();
                out = socket.getOutputStream();
                return;
            } catch (IOException e) {
                display("Attempt #" + i + " to establish JDWP socket connection failed:\n\t" + e);
                try {
                    Thread.currentThread().sleep(Binder.CONNECT_TRY_DELAY);
                } catch (InterruptedException ie) {
                    ie.printStackTrace(log.getOutStream());
                    throw new Failure("Thread interrupted while establishing JDWP connection: \n\t"
                                    + ie);
                }
            }
        }
        throw new IOException("Timeout exceeded while establishing JDWP socket connection to "
                              + ServerName + ":" + PortNumber);
    }

    /**
     * Accept connection from target VM for already boud port.
     */
    public void accept()
            throws IOException {

        if (serverSocket == null)
            throw new Failure("Attempt to accept JDWP socket connection from unbound port");

        socket = serverSocket.accept();
        serverSocket.close();
        serverSocket = null;

        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    /**
     * Close socket and streams.
     */
    public void close() throws IOException {

        if (socket == null)
            return;

        if (out != null) {
            flush();
            out.close();
        }

        if (in != null) {
            in.close();
        }

        if (socket != null) {
            socket.close();
            socket = null;
        }

        if (serverSocket != null) {
            serverSocket.close();
            serverSocket = null;
        }
    }

    /**
     * Return number of bytes that can be read.
     */
    public int available() throws IOException {
        return in.available();
    };

    /**
     * Flush bytes being buffered for writing if any.
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Set timeout for reading data in milliseconds.
     */
    public void setReadTimeout(long millisecs) throws IOException {
        socket.setSoTimeout((int)millisecs);
    }

    /**
     * Read the next byte of data from the socket.
     * The value byte is returned as an int in the range 0 to 255.
     * If no byte is available, the value -1 is returned.
     */
    public byte read() throws IOException {
        int b = in.read();
        if (b < 0) {
            throw new IOException("JDWP socket connection closed by remote host");
        }
        return (byte) b;
    };

    /**
     * Write the specified byte to the socket.
     */
    public void write(int b) throws IOException {
        out.write(b);
    };
}
