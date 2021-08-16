/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.sasl;

import javax.security.sasl.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implements the PLAIN SASL client mechanism.
 * (<A
 * HREF="http://ftp.isi.edu/in-notes/rfc2595.txt">RFC 2595</A>)
 *
 * @author Rosanna Lee
 */
final class PlainClient implements SaslClient {
    private boolean completed = false;
    private byte[] pw;
    private String authorizationID;
    private String authenticationID;
    private static byte SEP = 0; // US-ASCII <NUL>

    /**
     * Creates a SASL mechanism with client credentials that it needs
     * to participate in Plain authentication exchange with the server.
     *
     * @param authorizationID A possibly null string representing the principal
     *  for which authorization is being granted; if null, same as
     *  authenticationID
     * @param authenticationID A non-null string representing the principal
     * being authenticated. pw is associated with this principal.
     * @param pw A non-null byte[] containing the password.
     */
    PlainClient(String authorizationID, String authenticationID, byte[] pw)
    throws SaslException {
        if (authenticationID == null || pw == null) {
            throw new SaslException(
                "PLAIN: authorization ID and password must be specified");
        }

        this.authorizationID = authorizationID;
        this.authenticationID = authenticationID;
        this.pw = pw;  // caller should have already cloned
    }

    /**
     * Retrieves this mechanism's name for to initiate the PLAIN protocol
     * exchange.
     *
     * @return  The string "PLAIN".
     */
    public String getMechanismName() {
        return "PLAIN";
    }

    public boolean hasInitialResponse() {
        return true;
    }

    public void dispose() throws SaslException {
        clearPassword();
    }

    /**
     * Retrieves the initial response for the SASL command, which for
     * PLAIN is the concatenation of authorization ID, authentication ID
     * and password, with each component separated by the US-ASCII <NUL> byte.
     *
     * @param challengeData Ignored
     * @return A non-null byte array containing the response to be sent to the server.
     * @throws IllegalStateException if authentication already completed
     */
    public byte[] evaluateChallenge(byte[] challengeData) {
        if (completed) {
            throw new IllegalStateException(
                "PLAIN authentication already completed");
        }
        completed = true;
        byte[] authz = (authorizationID != null)
            ? authorizationID.getBytes(UTF_8)
            : null;
        byte[] auth = authenticationID.getBytes(UTF_8);

        byte[] answer = new byte[pw.length + auth.length + 2 +
                (authz == null ? 0 : authz.length)];

        int pos = 0;
        if (authz != null) {
            System.arraycopy(authz, 0, answer, 0, authz.length);
            pos = authz.length;
        }
        answer[pos++] = SEP;
        System.arraycopy(auth, 0, answer, pos, auth.length);

        pos += auth.length;
        answer[pos++] = SEP;

        System.arraycopy(pw, 0, answer, pos, pw.length);

        clearPassword();
        return answer;
    }

    /**
     * Determines whether this mechanism has completed.
     * Plain completes after returning one response.
     *
     * @return true if has completed; false otherwise;
     */
    public boolean isComplete() {
        return completed;
    }

    /**
     * Unwraps the incoming buffer.
     *
     * @throws SaslException Not applicable to this mechanism.
     */
    public byte[] unwrap(byte[] incoming, int offset, int len)
        throws SaslException {
        if (completed) {
            throw new SaslException(
                "PLAIN supports neither integrity nor privacy");
        } else {
            throw new IllegalStateException("PLAIN authentication not completed");
        }
    }

    /**
     * Wraps the outgoing buffer.
     *
     * @throws SaslException Not applicable to this mechanism.
     */
    public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
        if (completed) {
            throw new SaslException(
                "PLAIN supports neither integrity nor privacy");
        } else {
            throw new IllegalStateException("PLAIN authentication not completed");
        }
    }

    /**
     * Retrieves the negotiated property.
     * This method can be called only after the authentication exchange has
     * completed (i.e., when {@code isComplete()} returns true); otherwise, a
     * {@code SaslException} is thrown.
     *
     * @return value of property; only QOP is applicable to PLAIN.
     * @exception IllegalStateException if this authentication exchange
     *     has not completed
     */
    public Object getNegotiatedProperty(String propName) {
        if (completed) {
            if (propName.equals(Sasl.QOP)) {
                return "auth";
            } else {
                return null;
            }
        } else {
            throw new IllegalStateException("PLAIN authentication not completed");
        }
    }

    private void clearPassword() {
        if (pw != null) {
            // zero out password
            for (int i = 0; i < pw.length; i++) {
                pw[i] = (byte)0;
            }
            pw = null;
        }
    }

    @SuppressWarnings("deprecation")
    protected void finalize() {
        clearPassword();
    }
}
