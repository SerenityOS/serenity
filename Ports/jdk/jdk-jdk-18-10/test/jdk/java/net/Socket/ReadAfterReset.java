/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @requires (os.family == "linux" | os.family == "mac")
 * @bug 8203937
 * @summary Test reading bytes from a socket after the connection has been
 *          reset by the peer
 */

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This test exercises platform specific and unspecified behavior. It exists
 * only to ensure that the behavior doesn't change between JDK releases.
 */

public class ReadAfterReset {
    private static final PrintStream out = System.out;

    // number of bytes to write before the connection reset
    private static final int NUM_BYTES_TO_WRITE = 1000;

    public static void main(String[] args) throws IOException {
        try (ServerSocket ss = new ServerSocket()) {
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

            /**
             * Connect to the server which will write some bytes and reset the
             * connection. The client then attempts to read the bytes sent by
             * the server before it closed the connection.
             */
            out.println("Test connection ...");
            try (Socket s = new Socket()) {
                s.connect(ss.getLocalSocketAddress());
                int nwrote = acceptAndResetConnection(ss);
                int nread = readUntilIOException(s);
                if (nread != nwrote) {
                    throw new RuntimeException("Client read " + nread + ", expected " + nwrote);
                }
            }

            /**
             * Connect to the server which will write some bytes and reset the
             * connection. The client then writes to its end of the connection,
             * failing as the connection is reset. It then attempts to read the
             * bytes sent by the server before it closed the connection.
             */
            out.println();
            out.println("Test connection ...");
            try (Socket s = new Socket()) {
                s.connect(ss.getLocalSocketAddress());
                int nwrote = acceptAndResetConnection(ss);
                writeUntilIOException(s);
                int nread = readUntilIOException(s);
                if (nread != nwrote) {
                    throw new RuntimeException("Client read " + nread + ", expected " + nwrote);
                }
            }
        }
    }

    /**
     * Accept a connection, write bytes, and then reset the connection
     */
    static int acceptAndResetConnection(ServerSocket ss) throws IOException {
        int count = NUM_BYTES_TO_WRITE;
        try (Socket peer = ss.accept()) {
            peer.getOutputStream().write(new byte[count]);
            peer.setSoLinger(true, 0);
            out.format("Server wrote %d bytes and reset connection%n", count);
        }
        return count;
    }

    /**
     * Write bytes to a socket until I/O exception is thrown
     */
    static void writeUntilIOException(Socket s) {
        try {
            byte[] bytes = new byte[100];
            while (true) {
                s.getOutputStream().write(bytes);
                out.format("Client wrote %d bytes%n", bytes.length);
            }
        } catch (IOException ioe) {
            out.format("Client write failed: %s (expected)%n", ioe);
        }
    }

    /**
     * Read bytes from a socket until I/O exception is thrown.
     *
     * @return the number of bytes read before the I/O exception was thrown
     */
    static int readUntilIOException(Socket s) {
        int nread = 0;
        try {
            byte[] bytes = new byte[100];
            while (true) {
                int n = s.getInputStream().read(bytes);
                if (n < 0) {
                    out.println("Client read EOF");
                    break;
                } else {
                    out.format("Client read %s bytes%n", n);
                    nread += n;
                }
            }
        } catch (IOException ioe) {
            out.format("Client read failed: %s (expected)%n", ioe);
        }
        return nread;
    }
}
