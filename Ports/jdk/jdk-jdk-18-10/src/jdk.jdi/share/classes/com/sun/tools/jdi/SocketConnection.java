/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.sun.jdi.connect.spi.ClosedConnectionException;
import com.sun.jdi.connect.spi.Connection;

/*
 * The Connection returned by attach and accept is one of these
 */
class SocketConnection extends Connection {
    private Socket socket;
    private boolean closed = false;
    private OutputStream socketOutput;
    private InputStream socketInput;
    private Object receiveLock = new Object();
    private Object sendLock = new Object();
    private Object closeLock = new Object();

    SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        socket.setTcpNoDelay(true);
        socketInput = socket.getInputStream();
        socketOutput = socket.getOutputStream();
    }

    public void close() throws IOException {
        synchronized (closeLock) {
           if (closed) {
                return;
           }
           socketOutput.close();
           socketInput.close();
           socket.close();
           closed = true;
        }
    }

    public boolean isOpen() {
        synchronized (closeLock) {
            return !closed;
        }
    }

    public byte[] readPacket() throws IOException {
        if (!isOpen()) {
            throw new ClosedConnectionException("connection is closed");
        }
        synchronized (receiveLock) {
            int b1,b2,b3,b4;

            // length
            try {
                b1 = socketInput.read();
                b2 = socketInput.read();
                b3 = socketInput.read();
                b4 = socketInput.read();
            } catch (IOException ioe) {
                if (!isOpen()) {
                    throw new ClosedConnectionException("connection is closed");
                } else {
                    throw ioe;
                }
            }

            // EOF
            if (b1<0) {
               return new byte[0];
            }

            if (b2<0 || b3<0 || b4<0) {
                throw new IOException("protocol error - premature EOF");
            }

            int len = ((b1 << 24) | (b2 << 16) | (b3 << 8) | (b4 << 0));

            if (len < 0) {
                throw new IOException("protocol error - invalid length");
            }

            byte b[] = new byte[len];
            b[0] = (byte)b1;
            b[1] = (byte)b2;
            b[2] = (byte)b3;
            b[3] = (byte)b4;

            int off = 4;
            len -= off;

            while (len > 0) {
                int count;
                try {
                    count = socketInput.read(b, off, len);
                } catch (IOException ioe) {
                    if (!isOpen()) {
                        throw new ClosedConnectionException("connection is closed");
                    } else {
                        throw ioe;
                    }
                }
                if (count < 0) {
                    throw new IOException("protocol error - premature EOF");
                }
                len -= count;
                off += count;
            }

            return b;
        }
    }

    public void writePacket(byte b[]) throws IOException {
        if (!isOpen()) {
            throw new ClosedConnectionException("connection is closed");
        }

        /*
         * Check the packet size
         */
        if (b.length < 11) {
            throw new IllegalArgumentException("packet is insufficient size");
        }
        int b0 = b[0] & 0xff;
        int b1 = b[1] & 0xff;
        int b2 = b[2] & 0xff;
        int b3 = b[3] & 0xff;
        int len = ((b0 << 24) | (b1 << 16) | (b2 << 8) | (b3 << 0));
        if (len < 11) {
            throw new IllegalArgumentException("packet is insufficient size");
        }

        /*
         * Check that the byte array contains the complete packet
         */
        if (len > b.length) {
            throw new IllegalArgumentException("length mis-match");
        }

        synchronized (sendLock) {
            try {
                /*
                 * Send the packet (ignoring any bytes that follow
                 * the packet in the byte array).
                 */
                socketOutput.write(b, 0, len);
            } catch (IOException ioe) {
                if (!isOpen()) {
                    throw new ClosedConnectionException("connection is closed");
                } else {
                    throw ioe;
                }
            }
        }
    }
}
