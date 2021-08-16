/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * The NTLM client. Not multi-thread enabled.<p>
 * Example:
 * <pre>
 * Client client = new Client(null, "host", "dummy",
 *       "REALM", "t0pSeCr3t".toCharArray());
 * byte[] type1 = client.type1();
 * // Send type1 to server and receive response as type2
 * byte[] type3 = client.type3(type2, nonce);
 * // Send type3 to server
 * </pre>
 */
public final class Client extends NTLM {
    private final String hostname;
    private final String username;

    private String domain;
    private byte[] pw1, pw2;

    /**
     * Creates an NTLM Client instance.
     * @param version the NTLM version to use, which can be:
     * <ul>
     * <li>LM/NTLM: Original NTLM v1
     * <li>LM: Original NTLM v1, LM only
     * <li>NTLM: Original NTLM v1, NTLM only
     * <li>NTLM2: NTLM v1 with Client Challenge
     * <li>LMv2/NTLMv2: NTLM v2
     * <li>LMv2: NTLM v2, LM only
     * <li>NTLMv2: NTLM v2, NTLM only
     * </ul>
     * If null, "LMv2/NTLMv2" will be used.
     * @param hostname hostname of the client, can be null
     * @param username username to be authenticated, must not be null
     * @param domain domain of {@code username}, can be null
     * @param password password for {@code username}, must not be not null.
     * This method does not make any modification to this parameter, it neither
     * needs to access the content of this parameter after this method call,
     * so you are free to modify or nullify this parameter after this call.
     * @throws NTLMException if {@code username} or {@code password} is null,
     * or {@code version} is illegal.
     *
     */
    public Client(String version, String hostname, String username,
            String domain, char[] password) throws NTLMException {
        super(version);
        if ((username == null || password == null)) {
            throw new NTLMException(NTLMException.PROTOCOL,
                    "username/password cannot be null");
        }
        this.hostname = hostname;
        this.username = username;
        this.domain = domain == null ? "" : domain;
        this.pw1 = getP1(password);
        this.pw2 = getP2(password);
        debug("NTLM Client: (h,u,t,version(v)) = (%s,%s,%s,%s(%s))\n",
                    hostname, username, domain, version, v.toString());
    }

    /**
     * Generates the Type 1 message
     * @return the message generated
     */
    public byte[] type1() {
        Writer p = new Writer(1, 32);
        // Negotiate always sign, Negotiate NTLM,
        // Request Target, Negotiate OEM, Negotiate unicode
        int flags = 0x8207;
        if (v != Version.NTLM) {
            flags |= 0x80000;
        }
        p.writeInt(12, flags);
        debug("NTLM Client: Type 1 created\n");
        debug(p.getBytes());
        return p.getBytes();
    }

    /**
     * Generates the Type 3 message
     * @param type2 the responding Type 2 message from server, must not be null
     * @param nonce random 8-byte array to be used in message generation,
     * must not be null except for original NTLM v1
     * @return the message generated
     * @throws NTLMException if the incoming message is invalid, or
     * {@code nonce} is null for NTLM v1.
     */
    public byte[] type3(byte[] type2, byte[] nonce) throws NTLMException {
        if (type2 == null || (v != Version.NTLM && nonce == null)) {
            throw new NTLMException(NTLMException.PROTOCOL,
                    "type2 and nonce cannot be null");
        }
        debug("NTLM Client: Type 2 received\n");
        debug(type2);
        Reader r = new Reader(type2);
        byte[] challenge = r.readBytes(24, 8);
        int inputFlags = r.readInt(20);
        boolean unicode = (inputFlags & 1) == 1;

        // IE uses domainFromServer to generate an alist if server has not
        // provided one. Firefox/WebKit do not. Neither do we.
        //String domainFromServer = r.readSecurityBuffer(12, unicode);

        int flags = 0x88200 | (inputFlags & 3);
        Writer p = new Writer(3, 64);
        byte[] lm = null, ntlm = null;

        p.writeSecurityBuffer(28, domain, unicode);
        p.writeSecurityBuffer(36, username, unicode);
        p.writeSecurityBuffer(44, hostname, unicode);

        if (v == Version.NTLM) {
            byte[] lmhash = calcLMHash(pw1);
            byte[] nthash = calcNTHash(pw2);
            if (writeLM) lm = calcResponse (lmhash, challenge);
            if (writeNTLM) ntlm = calcResponse (nthash, challenge);
        } else if (v == Version.NTLM2) {
            byte[] nthash = calcNTHash(pw2);
            lm = ntlm2LM(nonce);
            ntlm = ntlm2NTLM(nthash, nonce, challenge);
        } else {
            byte[] nthash = calcNTHash(pw2);
            if (writeLM) lm = calcV2(nthash,
                    username.toUpperCase(Locale.US)+domain, nonce, challenge);
            if (writeNTLM) {
                // Some client create a alist even if server does not send
                // one: (i16)2 (i16)len target_in_unicode (i16)0 (i16) 0
                byte[] alist = ((inputFlags & 0x800000) != 0) ?
                    r.readSecurityBuffer(40) : new byte[0];
                byte[] blob = new byte[32+alist.length];
                System.arraycopy(new byte[]{1,1,0,0,0,0,0,0}, 0, blob, 0, 8);
                // TS
                byte[] time = BigInteger.valueOf(new Date().getTime())
                        .add(new BigInteger("11644473600000"))
                        .multiply(BigInteger.valueOf(10000))
                        .toByteArray();
                for (int i=0; i<time.length; i++) {
                    blob[8+time.length-i-1] = time[i];
                }
                System.arraycopy(nonce, 0, blob, 16, 8);
                System.arraycopy(new byte[]{0,0,0,0}, 0, blob, 24, 4);
                System.arraycopy(alist, 0, blob, 28, alist.length);
                System.arraycopy(new byte[]{0,0,0,0}, 0,
                        blob, 28+alist.length, 4);
                ntlm = calcV2(nthash, username.toUpperCase(Locale.US)+domain,
                        blob, challenge);
            }
        }
        p.writeSecurityBuffer(12, lm);
        p.writeSecurityBuffer(20, ntlm);
        p.writeSecurityBuffer(52, new byte[0]);

        p.writeInt(60, flags);
        debug("NTLM Client: Type 3 created\n");
        debug(p.getBytes());
        return p.getBytes();
    }

    /**
     * Returns the domain value provided by server after the authentication
     * is complete, or the domain value provided by the client before it.
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Disposes any password-derived information.
     */
    public void dispose() {
        Arrays.fill(pw1, (byte)0);
        Arrays.fill(pw2, (byte)0);
    }
}
