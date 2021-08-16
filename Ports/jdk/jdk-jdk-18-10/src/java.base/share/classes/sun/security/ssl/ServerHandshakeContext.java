/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.security.AlgorithmConstraints;
import java.security.AccessController;
import sun.security.util.LegacyAlgorithmConstraints;
import sun.security.action.GetLongAction;

class ServerHandshakeContext extends HandshakeContext {
    // To prevent the TLS renegotiation issues, by setting system property
    // "jdk.tls.rejectClientInitiatedRenegotiation" to true, applications in
    // server side can disable all client initiated SSL renegotiation
    // regardless of the support of TLS protocols.
    //
    // By default, allow client initiated renegotiation.
    static final boolean rejectClientInitiatedRenego =
            Utilities.getBooleanProperty(
                "jdk.tls.rejectClientInitiatedRenegotiation", false);

    // legacy algorithm constraints
    static final AlgorithmConstraints legacyAlgorithmConstraints =
            new LegacyAlgorithmConstraints(
                    LegacyAlgorithmConstraints.PROPERTY_TLS_LEGACY_ALGS,
                    new SSLAlgorithmDecomposer());

    // temporary authentication information
    SSLPossession interimAuthn;

    StatusResponseManager.StaplingParameters stapleParams;
    CertificateMessage.CertificateEntry currentCertEntry;
    private static final long DEFAULT_STATUS_RESP_DELAY = 5000L;
    final long statusRespTimeout;


    ServerHandshakeContext(SSLContextImpl sslContext,
            TransportContext conContext) throws IOException {
        super(sslContext, conContext);
        @SuppressWarnings("removal")
        long respTimeOut = AccessController.doPrivileged(
                    new GetLongAction("jdk.tls.stapling.responseTimeout",
                        DEFAULT_STATUS_RESP_DELAY));
        statusRespTimeout = respTimeOut >= 0 ? respTimeOut :
                DEFAULT_STATUS_RESP_DELAY;
        handshakeConsumers.put(
                SSLHandshake.CLIENT_HELLO.id, SSLHandshake.CLIENT_HELLO);
    }

    @Override
    void kickstart() throws IOException {
        if (!conContext.isNegotiated || kickstartMessageDelivered) {
            return;
        }

        SSLHandshake.kickstart(this);
        kickstartMessageDelivered = true;
    }
}

