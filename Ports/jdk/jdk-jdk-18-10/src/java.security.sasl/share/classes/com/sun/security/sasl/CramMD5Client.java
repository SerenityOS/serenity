/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.security.NoSuchAlgorithmException;

import java.util.logging.Logger;
import java.util.logging.Level;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implements the CRAM-MD5 SASL client-side mechanism.
 * (<A HREF="http://www.ietf.org/rfc/rfc2195.txt">RFC 2195</A>).
 * CRAM-MD5 has no initial response. It receives bytes from
 * the server as a challenge, which it hashes by using MD5 and the password.
 * It concatenates the authentication ID with this result and returns it
 * as the response to the challenge. At that point, the exchange is complete.
 *
 * @author Vincent Ryan
 * @author Rosanna Lee
 */
final class CramMD5Client extends CramMD5Base implements SaslClient {
    private String username;

    /**
     * Creates a SASL mechanism with client credentials that it needs
     * to participate in CRAM-MD5 authentication exchange with the server.
     *
     * @param authID A  non-null string representing the principal
     * being authenticated.
     *
     * @param pw A non-null String or byte[]
     * containing the password. If it is an array, it is first cloned.
     */
    CramMD5Client(String authID, byte[] pw) throws SaslException {
        if (authID == null || pw == null) {
            throw new SaslException(
                "CRAM-MD5: authentication ID and password must be specified");
        }

        username = authID;
        this.pw = pw;  // caller should have already cloned
    }

    /**
     * CRAM-MD5 has no initial response.
     */
    public boolean hasInitialResponse() {
        return false;
    }

    /**
     * Processes the challenge data.
     *
     * The server sends a challenge data using which the client must
     * compute an MD5-digest with its password as the key.
     *
     * @param challengeData A non-null byte array containing the challenge
     *        data from the server.
     * @return A non-null byte array containing the response to be sent to
     *        the server.
     * @throws SaslException if platform does not have MD5 support
     * @throws IllegalStateException if this method is invoked more than once.
     */
    public byte[] evaluateChallenge(byte[] challengeData)
        throws SaslException {

        // See if we've been here before
        if (completed) {
            throw new IllegalStateException(
                "CRAM-MD5 authentication already completed");
        }

        if (aborted) {
            throw new IllegalStateException(
                "CRAM-MD5 authentication previously aborted due to error");
        }

        // generate a keyed-MD5 digest from the user's password and challenge.
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "CRAMCLNT01:Received challenge: {0}",
                    new String(challengeData, UTF_8));
            }

            String digest = HMAC_MD5(pw, challengeData);

            // clear it when we no longer need it
            clearPassword();

            // response is username + " " + digest
            String resp = username + " " + digest;

            logger.log(Level.FINE, "CRAMCLNT02:Sending response: {0}", resp);

            completed = true;

            return resp.getBytes(UTF_8);
        } catch (java.security.NoSuchAlgorithmException e) {
            aborted = true;
            throw new SaslException("MD5 algorithm not available on platform", e);
        }
    }
}
