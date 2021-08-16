/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.security.SecureRandom;
import java.util.Arrays;
import javax.net.ssl.SSLProtocolException;

/**
 * Encapsulates an SSL session ID.
 *
 * @author Satish Dharmaraj
 * @author David Brownell
 */
final class SessionId {
    static final int MAX_LENGTH = 32;
    private final byte[] sessionId;          // max 32 bytes

    // Constructs a new session ID ... perhaps for a rejoinable session
    SessionId(boolean isRejoinable, SecureRandom generator) {
        if (isRejoinable && (generator != null)) {
            sessionId = new RandomCookie(generator).randomBytes;
        } else {
            sessionId = new byte[0];
        }
    }

    // Constructs a session ID from a byte array (max size 32 bytes)
    SessionId(byte[] sessionId) {
        this.sessionId = sessionId.clone();
    }

    // Returns the length of the ID, in bytes
    int length() {
        return sessionId.length;
    }

    // Returns the bytes in the ID.  May be an empty array.
    byte[] getId() {
        return sessionId.clone();
    }

    // Returns the ID as a string
    @Override
    public String toString() {
        if (sessionId.length == 0) {
            return "";
        }

        return Utilities.toHexString(sessionId);
    }


    // Returns a value which is the same for session IDs which are equal
    @Override
    public int hashCode() {
        return Arrays.hashCode(sessionId);
    }

    // Returns true if the parameter is the same session ID
    @Override
    public boolean equals (Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof SessionId) {
            SessionId that = (SessionId)obj;
            return Arrays.equals(this.sessionId, that.sessionId);
        }

        return false;
    }

    /**
     * Checks the length of the session ID to make sure it sits within
     * the range called out in the specification
     */
    void checkLength(int protocolVersion) throws SSLProtocolException {
        // As of today all versions of TLS have a 32-byte maximum length.
        // In the future we can do more here to support protocol versions
        // that may have longer max lengths.
        if (sessionId.length > MAX_LENGTH) {
            throw new SSLProtocolException("Invalid session ID length (" +
                    sessionId.length + " bytes)");
        }
    }
}
