/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.sasl.ntlm;

import com.sun.security.ntlm.NTLMException;
import com.sun.security.ntlm.Server;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Random;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.*;

/**
 * Required callbacks:
 * - RealmCallback
 *      used as key by handler to fetch password, optional
 * - NameCallback
 *      used as key by handler to fetch password
 * - PasswordCallback
 *      handler must enter password for username/realm supplied
 *
 * Environment properties that affect the implementation:
 *
 * javax.security.sasl.qop
 *    String, quality of protection; only "auth" is accepted, default "auth"
 *
 * com.sun.security.sasl.ntlm.version
 *    String, name a specific version to accept:
 *      LM/NTLM: Original NTLM v1
 *      LM: Original NTLM v1, LM only
 *      NTLM: Original NTLM v1, NTLM only
 *      NTLM2: NTLM v1 with Client Challenge
 *      LMv2/NTLMv2: NTLM v2
 *      LMv2: NTLM v2, LM only
 *      NTLMv2: NTLM v2, NTLM only
 *    If not specified, use system property "ntlm.version". If also
 *    not specified, all versions are accepted.
 *
 * com.sun.security.sasl.ntlm.domain
 *    String, the domain of the server, default is server name (fqdn parameter)
 *
 * com.sun.security.sasl.ntlm.random
 *    java.util.Random, the nonce source. Default null, an internal
 *    java.util.Random object will be used
 *
 * Negotiated Properties:
 *
 * javax.security.sasl.qop
 *    Always "auth"
 *
 * com.sun.security.sasl.ntlm.hostname
 *    The hostname for the user, provided by the client
 *
 */

final class NTLMServer implements SaslServer {

    private static final String NTLM_VERSION =
            "com.sun.security.sasl.ntlm.version";
    private static final String NTLM_DOMAIN =
            "com.sun.security.sasl.ntlm.domain";
    private static final String NTLM_HOSTNAME =
            "com.sun.security.sasl.ntlm.hostname";
    private static final String NTLM_RANDOM =
            "com.sun.security.sasl.ntlm.random";

    private final Random random;
    private final Server server;
    private byte[] nonce;
    private int step = 0;
    private String authzId;
    private final String mech;
    private String hostname;
    private String target;

    /**
     * @param mech not null
     * @param protocol not null for Sasl, ignored in NTLM
     * @param serverName not null for Sasl, can be null in NTLM. If non-null,
     * might be used as domain if not provided in props
     * @param props can be null
     * @param cbh can be null for Sasl, already null-checked in factory
     * @throws SaslException
     */
    NTLMServer(String mech, String protocol, String serverName,
            Map<String, ?> props, final CallbackHandler cbh)
            throws SaslException {

        this.mech = mech;
        String version = null;
        String domain = null;
        Random rtmp = null;

        if (props != null) {
            domain = (String) props.get(NTLM_DOMAIN);
            version = (String)props.get(NTLM_VERSION);
            rtmp = (Random)props.get(NTLM_RANDOM);
        }
        random = rtmp != null ? rtmp : new Random();

        if (version == null) {
            version = System.getProperty("ntlm.version");
        }
        if (domain == null) {
            domain = serverName;
        }
        if (domain == null) {
            throw new SaslException("Domain must be provided as"
                    + " the serverName argument or in props");
        }

        try {
            server = new Server(version, domain) {
                public char[] getPassword(String ntdomain, String username) {
                    try {
                        RealmCallback rcb =
                                (ntdomain == null || ntdomain.isEmpty())
                                    ? new RealmCallback("Domain: ")
                                    : new RealmCallback("Domain: ", ntdomain);
                        NameCallback ncb = new NameCallback(
                                "Name: ", username);
                        PasswordCallback pcb = new PasswordCallback(
                                "Password: ", false);
                        cbh.handle(new Callback[] { rcb, ncb, pcb });
                        char[] passwd = pcb.getPassword();
                        pcb.clearPassword();
                        return passwd;
                    } catch (IOException ioe) {
                        return null;
                    } catch (UnsupportedCallbackException uce) {
                        return null;
                    }
                }
            };
        } catch (NTLMException ne) {
            throw new SaslException(
                    "NTLM: server creation failure", ne);
        }
        nonce = new byte[8];
    }

    @Override
    public String getMechanismName() {
        return mech;
    }

    @Override
    public byte[] evaluateResponse(byte[] response) throws SaslException {
        try {
            step++;
            if (step == 1) {
                random.nextBytes(nonce);
                return server.type2(response, nonce);
            } else {
                String[] out = server.verify(response, nonce);
                authzId = out[0];
                hostname = out[1];
                target = out[2];
                return null;
            }
        } catch (NTLMException ex) {
            throw new SaslException("NTLM: generate response failure", ex);
        }
    }

    @Override
    public boolean isComplete() {
        return step >= 2;
    }

    @Override
    public String getAuthorizationID() {
        if (!isComplete()) {
            throw new IllegalStateException("authentication not complete");
        }
        return authzId;
    }

    @Override
    public byte[] unwrap(byte[] incoming, int offset, int len)
            throws SaslException {
        throw new IllegalStateException("Not supported yet.");
    }

    @Override
    public byte[] wrap(byte[] outgoing, int offset, int len)
            throws SaslException {
        throw new IllegalStateException("Not supported yet.");
    }

    @Override
    public Object getNegotiatedProperty(String propName) {
        if (!isComplete()) {
            throw new IllegalStateException("authentication not complete");
        }
        switch (propName) {
            case Sasl.QOP:
                return "auth";
            case Sasl.BOUND_SERVER_NAME:
                return target;
            case NTLM_HOSTNAME:
                return hostname;
            default:
                return null;
        }
    }

    @Override
    public void dispose() throws SaslException {
        return;
    }
}
