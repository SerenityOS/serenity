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
import com.sun.security.sasl.util.PolicyUtils;

import java.util.Map;
import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Client factory for EXTERNAL, CRAM-MD5, PLAIN.
 *
 * Requires the following callbacks to be satisfied by callback handler
 * when using CRAM-MD5 or PLAIN.
 * - NameCallback (to get username)
 * - PasswordCallback (to get password)
 *
 * @author Rosanna Lee
 */
public final class ClientFactoryImpl implements SaslClientFactory {
    private static final String[] myMechs = {
        "EXTERNAL",
        "CRAM-MD5",
        "PLAIN",
    };

    private static final int[] mechPolicies = {
        // %%% RL: Policies should actually depend on the external channel
        PolicyUtils.NOPLAINTEXT|PolicyUtils.NOACTIVE|PolicyUtils.NODICTIONARY,
        PolicyUtils.NOPLAINTEXT|PolicyUtils.NOANONYMOUS,    // CRAM-MD5
        PolicyUtils.NOANONYMOUS,                            // PLAIN
    };

    private static final int EXTERNAL = 0;
    private static final int CRAMMD5 = 1;
    private static final int PLAIN = 2;

    public ClientFactoryImpl() {
    }

    public SaslClient createSaslClient(String[] mechs,
        String authorizationId,
        String protocol,
        String serverName,
        Map<String,?> props,
        CallbackHandler cbh) throws SaslException {

            for (int i = 0; i < mechs.length; i++) {
                if (mechs[i].equals(myMechs[EXTERNAL])
                    && PolicyUtils.checkPolicy(mechPolicies[EXTERNAL], props)) {
                    return new ExternalClient(authorizationId);

                } else if (mechs[i].equals(myMechs[CRAMMD5])
                    && PolicyUtils.checkPolicy(mechPolicies[CRAMMD5], props)) {

                    Object[] uinfo = getUserInfo("CRAM-MD5", authorizationId, cbh);

                    // Callee responsible for clearing bytepw
                    return new CramMD5Client((String) uinfo[0],
                        (byte []) uinfo[1]);

                } else if (mechs[i].equals(myMechs[PLAIN])
                    && PolicyUtils.checkPolicy(mechPolicies[PLAIN], props)) {

                    Object[] uinfo = getUserInfo("PLAIN", authorizationId, cbh);

                    // Callee responsible for clearing bytepw
                    return new PlainClient(authorizationId,
                        (String) uinfo[0], (byte []) uinfo[1]);
                }
            }
            return null;
    };

    public String[] getMechanismNames(Map<String,?> props) {
        return PolicyUtils.filterMechs(myMechs, mechPolicies, props);
    }

    /**
     * Gets the authentication id and password. The
     * password is converted to bytes using UTF-8 and stored in bytepw.
     * The authentication id is stored in authId.
     *
     * @param prefix The non-null prefix to use for the prompt (e.g., mechanism
     *  name)
     * @param authorizationId The possibly null authorization id. This is used
     * as a default for the NameCallback. If null, it is not used in prompt.
     * @param cbh The non-null callback handler to use.
     * @return an {authid, passwd} pair
     */
    private Object[] getUserInfo(String prefix, String authorizationId,
        CallbackHandler cbh) throws SaslException {
        if (cbh == null) {
            throw new SaslException(
                "Callback handler to get username/password required");
        }
        try {
            String userPrompt = prefix + " authentication id: ";
            String passwdPrompt = prefix + " password: ";

            NameCallback ncb = authorizationId == null?
                new NameCallback(userPrompt) :
                new NameCallback(userPrompt, authorizationId);

            PasswordCallback pcb = new PasswordCallback(passwdPrompt, false);

            cbh.handle(new Callback[]{ncb,pcb});

            char[] pw = pcb.getPassword();

            byte[] bytepw;
            String authId;

            if (pw != null) {
                bytepw = new String(pw).getBytes(UTF_8);
                pcb.clearPassword();
            } else {
                bytepw = null;
            }

            authId = ncb.getName();

            return new Object[]{authId, bytepw};

        } catch (IOException e) {
            throw new SaslException("Cannot get password", e);
        } catch (UnsupportedCallbackException e) {
            throw new SaslException("Cannot get userid/password", e);
        }
    }
}
