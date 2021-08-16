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

/**
 * SSL/TLS record
 *
 * @author David Brownell
 */
interface SSLRecord extends Record {

    static final int    headerSize = 5;             // SSLv3 record header
    static final int    handshakeHeaderSize = 4;    // SSLv3 handshake header

    /*
     * The size of the header plus the max IV length
     */
    static final int    headerPlusMaxIVSize =
                                      headerSize        // header
                                    + maxIVLength;      // iv

    /*
     * The maximum size that may be increased when translating plaintext to
     * ciphertext fragment.
     */
    static final int    maxPlaintextPlusSize =
                                      headerSize        // header
                                    + maxIVLength       // iv
                                    + maxMacSize        // MAC or AEAD tag
                                    + maxPadding;       // block cipher padding

    /*
     * SSL has a maximum record size.  It's header, (compressed) data,
     * padding, and a trailer for the message authentication information (MAC
     * for block and stream ciphers, and message authentication tag for AEAD
     * ciphers).
     *
     * Some compression algorithms have rare cases where they expand the data.
     * As we don't support compression at this time, leave that out.
     */
    static final int    maxRecordSize =
                                      headerPlusMaxIVSize   // header + iv
                                    + maxDataSize           // data
                                    + maxPadding            // padding
                                    + maxMacSize;           // MAC or AEAD tag

    /*
     * The maximum large record size.
     *
     * Some SSL/TLS implementations support large fragment upto 2^15 bytes,
     * such as Microsoft. We support large incoming fragments.
     *
     * The maximum large record size is defined as maxRecordSize plus 2^14,
     * this is the amount OpenSSL is using.
     */
    static final int    maxLargeRecordSize =
                maxRecordSize   // Max size with a conforming implementation
              + maxDataSize;    // extra 2^14 bytes for large data packets.

    /*
     * We may need to send this SSL v2 "No Cipher" message back, if we
     * are faced with an SSLv2 "hello" that's not saying "I talk v3".
     * It's the only one documented in the V2 spec as a fatal error.
     */
    static final byte[] v2NoCipher = {
        (byte)0x80, (byte)0x03, // unpadded 3 byte record
        (byte)0x00,             // ... error message
        (byte)0x00, (byte)0x01  // ... NO_CIPHER error
    };
}
