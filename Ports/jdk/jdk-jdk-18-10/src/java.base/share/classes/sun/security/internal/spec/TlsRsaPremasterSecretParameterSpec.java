/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.internal.spec;

import sun.security.action.GetBooleanAction;

import java.security.spec.AlgorithmParameterSpec;

/**
 * Parameters for SSL/TLS RSA premaster secret.
 *
 * <p>Instances of this class are immutable.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @deprecated Sun JDK internal use only --- WILL BE REMOVED in a future
 * release.
 */
@Deprecated
public class TlsRsaPremasterSecretParameterSpec
        implements AlgorithmParameterSpec {

    private final byte[] encodedSecret;

    /*
     * The TLS spec says that the version in the RSA premaster secret must
     * be the maximum version supported by the client (i.e. the version it
     * requested in its client hello version). However, we (and other
     * implementations) used to send the active negotiated version. The
     * system property below allows to toggle the behavior.
     * Default is "false" (old behavior) for compatibility reasons in
     * SSLv3/TLSv1.  Later protocols (TLSv1.1+) do not use this property.
     */
    private static final boolean rsaPreMasterSecretFix = GetBooleanAction
            .privilegedGetProperty("com.sun.net.ssl.rsaPreMasterSecretFix");

    private final int clientVersion;
    private final int serverVersion;

    /**
     * Constructs a new TlsRsaPremasterSecretParameterSpec.
     *
     * @param clientVersion the version of the TLS protocol by which the
     *        client wishes to communicate during this session
     * @param serverVersion the negotiated version of the TLS protocol which
     *        contains the lower of that suggested by the client in the client
     *        hello and the highest supported by the server.
     *
     * @throws IllegalArgumentException if clientVersion or serverVersion are
     *   negative or larger than (2^16 - 1)
     */
    public TlsRsaPremasterSecretParameterSpec(
            int clientVersion, int serverVersion) {

        this.clientVersion = checkVersion(clientVersion);
        this.serverVersion = checkVersion(serverVersion);
        this.encodedSecret = null;
    }

    /**
     * Constructs a new TlsRsaPremasterSecretParameterSpec.
     *
     * @param clientVersion the version of the TLS protocol by which the
     *        client wishes to communicate during this session
     * @param serverVersion the negotiated version of the TLS protocol which
     *        contains the lower of that suggested by the client in the client
     *        hello and the highest supported by the server.
     * @param encodedSecret the encoded secret key
     *
     * @throws IllegalArgumentException if clientVersion or serverVersion are
     *   negative or larger than (2^16 - 1) or if encodedSecret is not
     *   exactly 48 bytes
     */
    public TlsRsaPremasterSecretParameterSpec(
            int clientVersion, int serverVersion, byte[] encodedSecret) {

        this.clientVersion = checkVersion(clientVersion);
        this.serverVersion = checkVersion(serverVersion);
        if (encodedSecret == null || encodedSecret.length != 48) {
            throw new IllegalArgumentException(
                        "Encoded secret is not exactly 48 bytes");
        }
        this.encodedSecret = encodedSecret.clone();
    }

    /**
     * Returns the version of the TLS protocol by which the client wishes to
     * communicate during this session.
     *
     * @return the version of the TLS protocol in ClientHello message
     */
    public int getClientVersion() {
        return clientVersion;
    }

    /**
     * Returns the negotiated version of the TLS protocol which contains the
     * lower of that suggested by the client in the client hello and the
     * highest supported by the server.
     *
     * @return the negotiated version of the TLS protocol in ServerHello message
     */
    public int getServerVersion() {
        return serverVersion;
    }

    /**
     * Returns the major version used in RSA premaster secret.
     *
     * @return the major version used in RSA premaster secret.
     */
    public int getMajorVersion() {
        if (rsaPreMasterSecretFix || clientVersion >= 0x0302) {
                                                        // 0x0302: TLSv1.1
            return (clientVersion >>> 8) & 0xFF;
        }

        return (serverVersion >>> 8) & 0xFF;
    }

    /**
     * Returns the minor version used in RSA premaster secret.
     *
     * @return the minor version used in RSA premaster secret.
     */
    public int getMinorVersion() {
        if (rsaPreMasterSecretFix || clientVersion >= 0x0302) {
                                                        // 0x0302: TLSv1.1
            return clientVersion & 0xFF;
        }

        return serverVersion & 0xFF;
    }

    private int checkVersion(int version) {
        if ((version < 0) || (version > 0xFFFF)) {
            throw new IllegalArgumentException(
                        "Version must be between 0 and 65,535");
        }
        return version;
    }

    /**
     * Returns the encoded secret.
     *
     * @return the encoded secret, may be null if no encoded secret.
     */
    public byte[] getEncodedSecret() {
        return encodedSecret == null ? null : encodedSecret.clone();
    }
}
