/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.ntlm;

import java.util.Arrays;
import java.util.Locale;

/**
 * The NTLM server, not multi-thread enabled.<p>
 * Example:
 * <pre>
 * Server server = new Server(null, "REALM") {
 *     public char[] getPassword(String ntdomain, String username) {
 *         switch (username) {
 *             case "dummy": return "t0pSeCr3t".toCharArray();
 *             case "guest": return "".toCharArray();
 *             default: return null;
 *         }
 *     }
 * };
 * // Receive client request as type1
 * byte[] type2 = server.type2(type1, nonce);
 * // Send type2 to client and receive type3
 * verify(type3, nonce);
 * </pre>
 */
public abstract class Server extends NTLM {
    private final String domain;
    private final boolean allVersion;
    /**
     * Creates a Server instance.
     * @param version the NTLM version to use, which can be:
     * <ul>
     * <li>NTLM: Original NTLM v1
     * <li>NTLM2: NTLM v1 with Client Challenge
     * <li>NTLMv2: NTLM v2
     * </ul>
     * If null, all versions will be supported. Please note that unless NTLM2
     * is selected, authentication succeeds if one of LM (or LMv2) or
     * NTLM (or NTLMv2) is verified.
     * @param domain the domain, must not be null
     * @throws NTLMException if {@code domain} is null.
     */
    public Server(String version, String domain) throws NTLMException {
        super(version);
        if (domain == null) {
            throw new NTLMException(NTLMException.PROTOCOL,
                    "domain cannot be null");
        }
        this.allVersion = (version == null);
        this.domain = domain;
        debug("NTLM Server: (t,version) = (%s,%s)\n", domain, version);
    }

    /**
     * Generates the Type 2 message
     * @param type1 the Type1 message received, must not be null
     * @param nonce the random 8-byte array to be used in message generation,
     * must not be null
     * @return the message generated
     * @throws NTLMException if the incoming message is invalid, or
     * {@code nonce} is null.
     */
    public byte[] type2(byte[] type1, byte[] nonce) throws NTLMException {
        if (nonce == null) {
            throw new NTLMException(NTLMException.PROTOCOL,
                    "nonce cannot be null");
        }
        debug("NTLM Server: Type 1 received\n");
        if (type1 != null) debug(type1);
        Writer p = new Writer(2, 32);
        // Negotiate NTLM2 Key, Target Type Domain,
        // Negotiate NTLM, Request Target, Negotiate unicode
        int flags = 0x90205;
        p.writeSecurityBuffer(12, domain, true);
        p.writeInt(20, flags);
        p.writeBytes(24, nonce);
        debug("NTLM Server: Type 2 created\n");
        debug(p.getBytes());
        return p.getBytes();
    }

    /**
     * Verifies the Type3 message received from client and returns
     * various negotiated information.
     * @param type3 the incoming Type3 message from client, must not be null
     * @param nonce the same nonce provided in {@link #type2}, must not be null
     * @return client username, client hostname, and the request target
     * @throws NTLMException if the incoming message is invalid, or
     * {@code nonce} is null.
     */
    public String[] verify(byte[] type3, byte[] nonce)
            throws NTLMException {
        if (type3 == null || nonce == null) {
            throw new NTLMException(NTLMException.PROTOCOL,
                    "type1 or nonce cannot be null");
        }
        debug("NTLM Server: Type 3 received\n");
        if (type3 != null) debug(type3);
        Reader r = new Reader(type3);
        String username = r.readSecurityBuffer(36, true);
        String hostname = r.readSecurityBuffer(44, true);
        String incomingDomain = r.readSecurityBuffer(28, true);
        /*if (incomingDomain != null && !incomingDomain.equals(domain)) {
            throw new NTLMException(NTLMException.DOMAIN_UNMATCH,
                    "Wrong domain: " + incomingDomain +
                    " vs " + domain); // Needed?
        }*/

        boolean verified = false;
        char[] password = getPassword(incomingDomain, username);
        if (password == null) {
            throw new NTLMException(NTLMException.USER_UNKNOWN,
                    "Unknown user");
        }
        byte[] incomingLM = r.readSecurityBuffer(12);
        byte[] incomingNTLM = r.readSecurityBuffer(20);

        if (!verified && (allVersion || v == Version.NTLM)) {
            if (incomingLM.length > 0) {
                byte[] pw1 = getP1(password);
                byte[] lmhash = calcLMHash(pw1);
                byte[] lmresponse = calcResponse (lmhash, nonce);
                if (Arrays.equals(lmresponse, incomingLM)) {
                    verified = true;
                }
            }
            if (incomingNTLM.length > 0) {
                byte[] pw2 = getP2(password);
                byte[] nthash = calcNTHash(pw2);
                byte[] ntresponse = calcResponse (nthash, nonce);
                if (Arrays.equals(ntresponse, incomingNTLM)) {
                    verified = true;
                }
            }
            debug("NTLM Server: verify using NTLM: " + verified  + "\n");
        }
        if (!verified && (allVersion || v == Version.NTLM2)) {
            byte[] pw2 = getP2(password);
            byte[] nthash = calcNTHash(pw2);
            byte[] clientNonce = Arrays.copyOf(incomingLM, 8);
            byte[] ntlmresponse = ntlm2NTLM(nthash, clientNonce, nonce);
            if (Arrays.equals(incomingNTLM, ntlmresponse)) {
                verified = true;
            }
            debug("NTLM Server: verify using NTLM2: " + verified + "\n");
        }
        if (!verified && (allVersion || v == Version.NTLMv2)) {
            byte[] pw2 = getP2(password);
            byte[] nthash = calcNTHash(pw2);
            if (incomingLM.length > 0) {
                byte[] clientNonce = Arrays.copyOfRange(
                        incomingLM, 16, incomingLM.length);
                byte[] lmresponse = calcV2(nthash,
                        username.toUpperCase(Locale.US)+incomingDomain,
                        clientNonce, nonce);
                if (Arrays.equals(lmresponse, incomingLM)) {
                    verified = true;
                }
            }
            if (incomingNTLM.length > 0) {
                // We didn't sent alist in type2(), so there
                // is nothing to check here.
                byte[] clientBlob = Arrays.copyOfRange(
                        incomingNTLM, 16, incomingNTLM.length);
                byte[] ntlmresponse = calcV2(nthash,
                        username.toUpperCase(Locale.US)+incomingDomain,
                        clientBlob, nonce);
                if (Arrays.equals(ntlmresponse, incomingNTLM)) {
                    verified = true;
                }
            }
            debug("NTLM Server: verify using NTLMv2: " + verified + "\n");
        }
        if (!verified) {
            throw new NTLMException(NTLMException.AUTH_FAILED,
                    "None of LM and NTLM verified");
        }
        return new String[] {username, hostname, incomingDomain};
    }

    /**
     * Retrieves the password for a given user. This method should be
     * overridden in a concrete class.
     * @param domain can be null
     * @param username must not be null
     * @return the password for the user, or null if unknown
     */
    public abstract char[] getPassword(String domain, String username);
}
