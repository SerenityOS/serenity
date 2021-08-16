/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

/*
 * Plaintext
 */
final class Plaintext {
    static final Plaintext PLAINTEXT_NULL = new Plaintext();

    final byte       contentType;
    final byte       majorVersion;
    final byte       minorVersion;
    final int        recordEpoch;     // increments on every cipher state change
    final long       recordSN;        // epoch | sequence number
    final ByteBuffer fragment;        // null if need to be reassembled

    HandshakeStatus  handshakeStatus; // null if not used or not handshaking

    private Plaintext() {
        this.contentType = 0;
        this.majorVersion = 0;
        this.minorVersion = 0;
        this.recordEpoch = -1;
        this.recordSN = -1;
        this.fragment = null;
        this.handshakeStatus = null;
    }

    Plaintext(byte contentType,
            byte majorVersion, byte minorVersion,
            int recordEpoch, long recordSN, ByteBuffer fragment) {

        this.contentType = contentType;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.recordEpoch = recordEpoch;
        this.recordSN = recordSN;
        this.fragment = fragment;

        this.handshakeStatus = null;
    }

    @Override
    public String toString() {
        return "contentType: " + contentType + "/" +
               "majorVersion: " + majorVersion + "/" +
               "minorVersion: " + minorVersion + "/" +
               "recordEpoch: " + recordEpoch + "/" +
               "recordSN: 0x" + Long.toHexString(recordSN) + "/" +
               "fragment: " + fragment;
    }
}
