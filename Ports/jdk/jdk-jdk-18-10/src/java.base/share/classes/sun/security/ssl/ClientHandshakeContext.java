/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.security.cert.X509Certificate;

import sun.security.ssl.ClientHello.ClientHelloMessage;

class ClientHandshakeContext extends HandshakeContext {
    /*
     * Allow unsafe server certificate change?
     *
     * Server certificate change during SSL/TLS renegotiation may be considered
     * unsafe, as described in the Triple Handshake attacks:
     *
     *     https://secure-resumption.com/tlsauth.pdf
     *
     * Endpoint identification (See
     * SSLParameters.getEndpointIdentificationAlgorithm()) is a pretty nice
     * guarantee that the server certificate change in renegotiation is legal.
     * However, endpoint identification is only enabled for HTTPS and LDAP
     * over SSL/TLS by default.  It is not enough to protect SSL/TLS
     * connections other than HTTPS and LDAP.
     *
     * The renegotiation indication extension (See RFC 5746) is a pretty
     * strong guarantee that the endpoints on both client and server sides
     * are identical on the same connection.  However, the Triple Handshake
     * attacks can bypass this guarantee if there is a session-resumption
     * handshake between the initial full handshake and the renegotiation
     * full handshake.
     *
     * Server certificate change may be unsafe and should be restricted if
     * endpoint identification is not enabled and the previous handshake is
     * a session-resumption abbreviated initial handshake, unless the
     * identities represented by both certificates can be regraded as the
     * same (See isIdentityEquivalent()).
     *
     * Considering the compatibility impact and the actual requirements to
     * support server certificate change in practice, the system property,
     * jdk.tls.allowUnsafeServerCertChange, is used to define whether unsafe
     * server certificate change in renegotiation is allowed or not.  The
     * default value of the system property is "false".  To mitigate the
     * compatibility impact, applications may want to set the system
     * property to "true" at their own risk.
     *
     * If the value of the system property is "false", server certificate
     * change in renegotiation after a session-resumption abbreviated initial
     * handshake is restricted (See isIdentityEquivalent()).
     *
     * If the system property is set to "true" explicitly, the restriction on
     * server certificate change in renegotiation is disabled.
     */
    static final boolean allowUnsafeServerCertChange =
            Utilities.getBooleanProperty(
                    "jdk.tls.allowUnsafeServerCertChange", false);

    /*
     * the reserved server certificate chain in previous handshaking
     *
     * The server certificate chain is only reserved if the previous
     * handshake is a session-resumption abbreviated initial handshake.
     */
    X509Certificate[] reservedServerCerts = null;

    X509Certificate[] deferredCerts;

    ClientHelloMessage initialClientHelloMsg = null;

    // PSK identity is selected in first Hello and used again after HRR
    byte[] pskIdentity;

    ClientHandshakeContext(SSLContextImpl sslContext,
            TransportContext conContext) throws IOException {
        super(sslContext, conContext);
    }

    @Override
    void kickstart() throws IOException {
        if (kickstartMessageDelivered) {
            return;
        }

        SSLHandshake.kickstart(this);
        kickstartMessageDelivered = true;
    }
}
