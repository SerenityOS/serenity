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

import java.io.IOException;
//import java.util.Vector;

/**
 * This class represents an abstract transport for JDWP.
 */
public abstract class Transport extends Log.Logger {

    public static final String LOG_PREFIX = "transport> ";

    /**
     * Make base <code>Transport</code> object providing with specified Log.
     */
    public Transport(Log log) {
        super(log, LOG_PREFIX);
    }

    /**
     * Return number of bytes that can be received.
     */
    public abstract int available() throws IOException;

    /**
     * Flushe bytes being buffered for writing if any.
     */
    public abstract void flush() throws IOException;

    /**
     * Receive the next byte of data.
     *
     * The value byte is returned as an int in the range 0 to 255.
     * If no byte is available, the value -1 is returned.
     */
    public abstract byte read() throws IOException;

    /**
     * Send the specified byte.
     */
    public abstract void write(int b) throws IOException;

    /**
     * Send the specified bytes.
     */
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++)
            write(b[off + i]);
    }

    /**
     * Receive bytes of JDWP packet for default timeout.
     */
    public void read(Packet packet) throws IOException {
        packet.readFrom(this);
    }

    /**
     * Send and flushe bytes of JDWP packet.
     */
    public void write(Packet packet) throws IOException {
        packet.writeTo(this);
        flush();
    }

    /**
     * Perform JDWP "handshake" procedure.
     */
    public void handshake() throws IOException {

        String hs = "JDWP-Handshake";
        byte[] hsb = hs.getBytes();

        write(hsb, 0, hsb.length);
        flush();

        try {
            Thread.currentThread().sleep(500);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        int received = 0;
        for (int i = 0; i < Binder.CONNECT_TRIES; i++) {
            received = available();
            if (received < hsb.length) {
//                System.err.println("Failed to hadshake try #" + i + ", bytes: " + received);
                try {
                    Thread.currentThread().sleep(Binder.CONNECT_TRY_DELAY);
                } catch (InterruptedException e) {
                    throw new Failure("Thread interrupted while sleeping between connection attempts:\n\t"
                                    + e);
                }
            } else {
//                System.err.println("Successed to hadshake try #" + i + ", bytes: " + received);
                for (int j = 0; j < hsb.length; j++) {
                    byte b = (byte) (read() & 0xFF);
                    if (b != hsb[j])
                        throw new IOException("Target VM failed to handshake: unexpected byte #" + j
                                            + ": " + b + " (expected:" + hsb[j] + ")");
                }
                return;
            }
        }

        throw new IOException("Target VM failed to handshake: too few bytes in reply: "
                            + received + "(expected: " + hsb.length + ")");
    }

    /**
     * Set timeout for reading data in milliseconds.
     * Timeout of 0 means wait infinitly.
     */
    public abstract void setReadTimeout(long millisecs) throws IOException;

    /**
     * Close JDWP connection.
     */
    public abstract void close() throws IOException;

    /**
     * Perform finalization of object by closing JDWP connection.
     */
    protected void finalize() throws Throwable {
        close();
    }

}
