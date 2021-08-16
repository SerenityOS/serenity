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

import com.sun.security.ntlm.Client;
import com.sun.security.ntlm.NTLMException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Random;
import javax.security.auth.callback.Callback;


import javax.security.sasl.*;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * Required callbacks:
 * - RealmCallback
 *    handle can provide domain info for authentication, optional
 * - NameCallback
 *    handler must enter username to use for authentication
 * - PasswordCallback
 *    handler must enter password for username to use for authentication
 *
 * Environment properties that affect behavior of implementation:
 *
 * javax.security.sasl.qop
 *    String, quality of protection; only "auth" is accepted, default "auth"
 *
 * com.sun.security.sasl.ntlm.version
 *    String, name a specific version to use; can be:
 *      LM/NTLM: Original NTLM v1
 *      LM: Original NTLM v1, LM only
 *      NTLM: Original NTLM v1, NTLM only
 *      NTLM2: NTLM v1 with Client Challenge
 *      LMv2/NTLMv2: NTLM v2
 *      LMv2: NTLM v2, LM only
 *      NTLMv2: NTLM v2, NTLM only
 *    If not specified, use system property "ntlm.version". If
 *    still not specified, use default value "LMv2/NTLMv2".
 *
 * com.sun.security.sasl.ntlm.random
 *    java.util.Random, the nonce source to be used in NTLM v2 or NTLM v1 with
 *    Client Challenge. Default null, an internal java.util.Random object
 *    will be used
 *
 * Negotiated Properties:
 *
 * javax.security.sasl.qop
 *    Always "auth"
 *
 * com.sun.security.sasl.html.domain
 *    The domain for the user, provided by the server
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2222.txt">RFC 2222</a>
 * - Simple Authentication and Security Layer (SASL)
 *
 */
final class NTLMClient implements SaslClient {

    private static final String NTLM_VERSION =
            "com.sun.security.sasl.ntlm.version";
    private static final String NTLM_RANDOM =
            "com.sun.security.sasl.ntlm.random";
    private static final String NTLM_DOMAIN =
            "com.sun.security.sasl.ntlm.domain";
    private static final String NTLM_HOSTNAME =
            "com.sun.security.sasl.ntlm.hostname";

    private final Client client;
    private final String mech;
    private final Random random;

    private int step = 0;   // 0-start,1-nego,2-auth,3-done

    /**
     * @param mech non-null
     * @param authorizationId can be null or empty and ignored
     * @param protocol non-null for Sasl, useless for NTLM
     * @param serverName non-null for Sasl, but can be null for NTLM
     * @param props can be null
     * @param cbh can be null for Sasl, already null-checked in factory
     * @throws SaslException
     */
    NTLMClient(String mech, String authzid, String protocol, String serverName,
            Map<String, ?> props, CallbackHandler cbh) throws SaslException {

        this.mech = mech;
        String version = null;
        Random rtmp = null;
        String hostname = null;

        if (props != null) {
            String qop = (String)props.get(Sasl.QOP);
            if (qop != null && !qop.equals("auth")) {
                throw new SaslException("NTLM only support auth");
            }
            version = (String)props.get(NTLM_VERSION);
            rtmp = (Random)props.get(NTLM_RANDOM);
            hostname = (String)props.get(NTLM_HOSTNAME);
        }
        this.random = rtmp != null ? rtmp : new Random();

        if (version == null) {
            version = System.getProperty("ntlm.version");
        }

        RealmCallback dcb = (serverName != null && !serverName.isEmpty())?
            new RealmCallback("Realm: ", serverName) :
            new RealmCallback("Realm: ");
        NameCallback ncb = (authzid != null && !authzid.isEmpty()) ?
            new NameCallback("User name: ", authzid) :
            new NameCallback("User name: ");
        PasswordCallback pcb =
            new PasswordCallback("Password: ", false);

        try {
            cbh.handle(new Callback[] {dcb, ncb, pcb});
        } catch (UnsupportedCallbackException e) {
            throw new SaslException("NTLM: Cannot perform callback to " +
                "acquire realm, username or password", e);
        } catch (IOException e) {
            throw new SaslException(
                "NTLM: Error acquiring realm, username or password", e);
        }

        if (hostname == null) {
            try {
                hostname = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                hostname = "localhost";
            }
        }
        try {
            String name = ncb.getName();
            if (name == null) {
                name = authzid;
            }
            String domain = dcb.getText();
            if (domain == null) {
                domain = serverName;
            }
            client = new Client(version, hostname,
                    name,
                    domain,
                    pcb.getPassword());
        } catch (NTLMException ne) {
            throw new SaslException(
                    "NTLM: client creation failure", ne);
        }
    }

    @Override
    public String getMechanismName() {
        return mech;
    }

    @Override
    public boolean isComplete() {
        return step >= 2;
    }

    @Override
    public byte[] unwrap(byte[] incoming, int offset, int len)
            throws SaslException {
        throw new IllegalStateException("Not supported.");
    }

    @Override
    public byte[] wrap(byte[] outgoing, int offset, int len)
            throws SaslException {
        throw new IllegalStateException("Not supported.");
    }

    @Override
    public Object getNegotiatedProperty(String propName) {
        if (!isComplete()) {
            throw new IllegalStateException("authentication not complete");
        }
        switch (propName) {
            case Sasl.QOP:
                return "auth";
            case NTLM_DOMAIN:
                return client.getDomain();
            default:
                return null;
        }
    }

    @Override
    public void dispose() throws SaslException {
        client.dispose();
    }

    @Override
    public boolean hasInitialResponse() {
        return true;
    }

    @Override
    public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
        step++;
        if (step == 1) {
            return client.type1();
        } else {
            try {
                byte[] nonce = new byte[8];
                random.nextBytes(nonce);
                return client.type3(challenge, nonce);
            } catch (NTLMException ex) {
                throw new SaslException("Type3 creation failed", ex);
            }
        }
    }
}
