/*
 * Copyright (c) 2001, 2003, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap.sasl;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;

/**
 * This class is used by clients of Java SASL that need to create an input stream
 * that uses SaslClient's unwrap() method to decode the SASL buffers
 * sent by the SASL server.
 *
 * Extend from InputStream instead of FilterInputStream because
 * we need to override less methods in InputStream. That is, the
 * behavior of the default implementations in InputStream matches
 * more closely with the behavior we want in SaslInputStream.
 *
 * @author Rosanna Lee
 */
public class SaslInputStream extends InputStream {
    private static final boolean debug = false;

    private byte[] saslBuffer;  // buffer for storing raw bytes
    private byte[] lenBuf = new byte[4];  // buffer for storing length

    private byte[] buf = new byte[0];   // buffer for storing processed bytes
                                        // Initialized to empty buffer
    private int bufPos = 0;             // read position in buf
    private InputStream in;             // underlying input stream
    private SaslClient sc;
    private int recvMaxBufSize = 65536;

    SaslInputStream(SaslClient sc, InputStream in) throws SaslException {
        super();
        this.in = in;
        this.sc = sc;

        String str = (String) sc.getNegotiatedProperty(Sasl.MAX_BUFFER);
        if (str != null) {
            try {
                recvMaxBufSize = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                throw new SaslException(Sasl.MAX_BUFFER +
                    " property must be numeric string: " + str);
            }
        }
        saslBuffer = new byte[recvMaxBufSize];
    }

    public int read() throws IOException {
        byte[] inBuf = new byte[1];
        int count = read(inBuf, 0, 1);
        if (count > 0) {
            return inBuf[0];
        } else {
            return -1;
        }
    }

    public int read(byte[] inBuf, int start, int count) throws IOException {

        if (bufPos >= buf.length) {
            int actual = fill();   // read and unwrap next SASL buffer
            while (actual == 0) {  // ignore zero length content
                actual = fill();
            }
            if (actual == -1) {
                return -1;    // EOF
            }
        }

        int avail = buf.length - bufPos;
        if (count > avail) {
            // Requesting more that we have stored
            // Return all that we have; next invocation of read() will
            // trigger fill()
            System.arraycopy(buf, bufPos, inBuf, start, avail);
            bufPos = buf.length;
            return avail;
        } else {
            // Requesting less than we have stored
            // Return all that was requested
            System.arraycopy(buf, bufPos, inBuf, start, count);
            bufPos += count;
            return count;
        }
    }

    /**
     * Fills the buf with more data by reading a SASL buffer, unwrapping it,
     * and leaving the bytes in buf for read() to return.
     * @return The number of unwrapped bytes available
     */
    private int fill() throws IOException {
        // Read in length of buffer
        int actual = readFully(lenBuf, 4);
        if (actual != 4) {
            return -1;
        }
        int len = networkByteOrderToInt(lenBuf, 0, 4);

        if (len > recvMaxBufSize) {
            throw new IOException(
                len + "exceeds the negotiated receive buffer size limit:" +
                recvMaxBufSize);
        }

        if (debug) {
            System.err.println("reading " + len + " bytes from network");
        }

        // Read SASL buffer
        actual = readFully(saslBuffer, len);
        if (actual != len) {
            throw new EOFException("Expecting to read " + len +
                " bytes but got " + actual + " bytes before EOF");
        }

        // Unwrap
        buf = sc.unwrap(saslBuffer, 0, len);

        bufPos = 0;

        return buf.length;
    }

    /**
     * Read requested number of bytes before returning.
     * @return The number of bytes actually read; -1 if none read
     */
    private int readFully(byte[] inBuf, int total) throws IOException {
        int count, pos = 0;

        if (debug) {
            System.err.println("readFully " + total + " from " + in);
        }

        while (total > 0) {
            count = in.read(inBuf, pos, total);

            if (debug) {
                System.err.println("readFully read " + count);
            }

            if (count == -1 ) {
                return (pos == 0? -1 : pos);
            }
            pos += count;
            total -= count;
        }
        return pos;
    }

    public int available() throws IOException {
        return buf.length - bufPos;
    }

    public void close() throws IOException {
        SaslException save = null;
        try {
            sc.dispose(); // Dispose of SaslClient's state
        } catch (SaslException e) {
            // Save exception for throwing after closing 'in'
            save = e;
        }

        in.close();  // Close underlying input stream

        if (save != null) {
            throw save;
        }
    }

    /**
     * Returns the integer represented by  4 bytes in network byte order.
     */
    // Copied from com.sun.security.sasl.util.SaslImpl.
    private static int networkByteOrderToInt(byte[] buf, int start, int count) {
        if (count > 4) {
            throw new IllegalArgumentException("Cannot handle more than 4 bytes");
        }

        int answer = 0;

        for (int i = 0; i < count; i++) {
            answer <<= 8;
            answer |= ((int)buf[start+i] & 0xff);
        }
        return answer;
    }
}
