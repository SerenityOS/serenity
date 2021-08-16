/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLException;

/**
 * SSL/(D)TLS record.
 *
 * This is the base interface, which defines common information and interfaces
 * used by both Input and Output records.
 *
 * @author David Brownell
 */
interface Record {
    static final int    maxMacSize = 48;        // the max supported MAC or
                                                // AEAD tag size
    static final int    maxDataSize = 16384;    // 2^14 bytes of data
    static final int    maxPadding = 256;       // block cipher padding
    static final int    maxIVLength = 16;       // the max supported IV length

    static final int    maxFragmentSize = 18432;    // the max fragment size
                                                    // 2^14 + 2048

    /*
     * System property to enable/disable CBC protection in SSL3/TLS1.
     */
    static final boolean enableCBCProtection =
            Utilities.getBooleanProperty("jsse.enableCBCProtection", true);

    /*
     * The overflow values of integers of 8, 16 and 24 bits.
     */
    static final int OVERFLOW_OF_INT08 = (0x01 << 8);
    static final int OVERFLOW_OF_INT16 = (0x01 << 16);
    static final int OVERFLOW_OF_INT24 = (0x01 << 24);

    /*
     * Read 8, 16, 24, and 32 bit integer data types, encoded
     * in standard big-endian form.
     */
    static int getInt8(ByteBuffer m) throws IOException {
        verifyLength(m, 1);
        return (m.get() & 0xFF);
    }

    static int getInt16(ByteBuffer m) throws IOException {
        verifyLength(m, 2);
        return ((m.get() & 0xFF) << 8) |
                (m.get() & 0xFF);
    }

    static int getInt24(ByteBuffer m) throws IOException {
        verifyLength(m, 3);
        return ((m.get() & 0xFF) << 16) |
               ((m.get() & 0xFF) <<  8) |
                (m.get() & 0xFF);
    }

    static int getInt32(ByteBuffer m) throws IOException {
        verifyLength(m, 4);
        return ((m.get() & 0xFF) << 24) |
               ((m.get() & 0xFF) << 16) |
               ((m.get() & 0xFF) <<  8) |
                (m.get() & 0xFF);
    }

    /*
     * Read byte vectors with 8, 16, and 24 bit length encodings.
     */
    static byte[] getBytes8(ByteBuffer m) throws IOException {
        int len = Record.getInt8(m);
        verifyLength(m, len);
        byte[] b = new byte[len];

        m.get(b);
        return b;
    }

    static byte[] getBytes16(ByteBuffer m) throws IOException {
        int len = Record.getInt16(m);
        verifyLength(m, len);
        byte[] b = new byte[len];

        m.get(b);
        return b;
    }

    static byte[] getBytes24(ByteBuffer m) throws IOException {
        int len = Record.getInt24(m);
        verifyLength(m, len);
        byte[] b = new byte[len];

        m.get(b);
        return b;
    }

    /*
     * Write 8, 16, 24, and 32 bit integer data types, encoded
     * in standard big-endian form.
     */
    static void putInt8(ByteBuffer m, int i) throws IOException {
        verifyLength(m, 1);
        m.put((byte)(i & 0xFF));
    }

    static void putInt16(ByteBuffer m, int i) throws IOException {
        verifyLength(m, 2);
        m.put((byte)((i >> 8) & 0xFF));
        m.put((byte)(i & 0xFF));
    }

    static void putInt24(ByteBuffer m, int i) throws IOException {
        verifyLength(m, 3);
        m.put((byte)((i >> 16) & 0xFF));
        m.put((byte)((i >> 8) & 0xFF));
        m.put((byte)(i & 0xFF));
    }

    static void putInt32(ByteBuffer m, int i) throws IOException {
        m.put((byte)((i >> 24) & 0xFF));
        m.put((byte)((i >> 16) & 0xFF));
        m.put((byte)((i >> 8) & 0xFF));
        m.put((byte)(i & 0xFF));
    }

    /*
     * Write byte vectors with 8, 16, and 24 bit length encodings.
     */
    static void putBytes8(ByteBuffer m, byte[] s) throws IOException {
        if (s == null || s.length == 0) {
            verifyLength(m, 1);
            putInt8(m, 0);
        } else {
            verifyLength(m, 1 + s.length);
            putInt8(m, s.length);
            m.put(s);
        }
    }

    static void putBytes16(ByteBuffer m, byte[] s) throws IOException {
        if (s == null || s.length == 0) {
            verifyLength(m, 2);
            putInt16(m, 0);
        } else {
            verifyLength(m, 2 + s.length);
            putInt16(m, s.length);
            m.put(s);
        }
    }

    static void putBytes24(ByteBuffer m, byte[] s) throws IOException {
        if (s == null || s.length == 0) {
            verifyLength(m, 3);
            putInt24(m, 0);
        } else {
            verifyLength(m, 3 + s.length);
            putInt24(m, s.length);
            m.put(s);
        }
    }

    // Verify that the buffer has sufficient remaining.
    static void verifyLength(
            ByteBuffer m, int len) throws SSLException {
        if (len > m.remaining()) {
            throw new SSLException("Insufficient space in the buffer, " +
                    "may be cause by an unexpected end of handshake data.");
        }
    }
}
