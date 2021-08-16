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

package com.sun.security.sasl.gsskerb;

import javax.security.sasl.*;
import com.sun.security.sasl.util.PolicyUtils;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;

/**
 * Client/server factory for GSSAPI (Kerberos V5) SASL client/server mechs.
 * See GssKrb5Client/GssKrb5Server for input requirements.
 *
 * @author Rosanna Lee
 */
public final class FactoryImpl implements SaslClientFactory, SaslServerFactory {
    private static final String[] myMechs = {
        "GSSAPI"};

    private static final int[] mechPolicies = {
        PolicyUtils.NOPLAINTEXT|PolicyUtils.NOANONYMOUS|PolicyUtils.NOACTIVE
    };

    private static final int GSS_KERB_V5 = 0;

    public FactoryImpl() {
    }

    public SaslClient createSaslClient(String[] mechs,
        String authorizationId,
        String protocol,
        String serverName,
        Map<String,?> props,
        CallbackHandler cbh) throws SaslException {

            for (int i = 0; i < mechs.length; i++) {
                if (mechs[i].equals(myMechs[GSS_KERB_V5])
                    && PolicyUtils.checkPolicy(mechPolicies[GSS_KERB_V5], props)) {
                    return new GssKrb5Client(
                        authorizationId,
                        protocol,
                        serverName,
                        props,
                        cbh);
                }
            }
            return null;
    };

    public SaslServer createSaslServer(String mech,
        String protocol,
        String serverName,
        Map<String,?> props,
        CallbackHandler cbh) throws SaslException {
            if (mech.equals(myMechs[GSS_KERB_V5])
                && PolicyUtils.checkPolicy(mechPolicies[GSS_KERB_V5], props)) {
                if (cbh == null) {
                    throw new SaslException(
                "Callback handler with support for AuthorizeCallback required");
                }
                return new GssKrb5Server(
                    protocol,
                    serverName,
                    props,
                    cbh);
            }
            return null;
    };

    public String[] getMechanismNames(Map<String,?> props) {
        return PolicyUtils.filterMechs(myMechs, mechPolicies, props);
    }
}
