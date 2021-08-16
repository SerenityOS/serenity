/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AlgorithmConstraints;
import java.security.AlgorithmParameters;
import java.security.CryptoPrimitive;
import java.security.Key;
import java.util.Set;
import javax.net.ssl.*;
import sun.security.util.DisabledAlgorithmConstraints;
import static sun.security.util.DisabledAlgorithmConstraints.*;

/**
 * Algorithm constraints for disabled algorithms property
 *
 * See the "jdk.certpath.disabledAlgorithms" specification in java.security
 * for the syntax of the disabled algorithm string.
 */
final class SSLAlgorithmConstraints implements AlgorithmConstraints {

    private static final AlgorithmConstraints tlsDisabledAlgConstraints =
            new DisabledAlgorithmConstraints(PROPERTY_TLS_DISABLED_ALGS,
                    new SSLAlgorithmDecomposer());

    private static final AlgorithmConstraints x509DisabledAlgConstraints =
            new DisabledAlgorithmConstraints(PROPERTY_CERTPATH_DISABLED_ALGS,
                    new SSLAlgorithmDecomposer(true));

    private final AlgorithmConstraints userSpecifiedConstraints;
    private final AlgorithmConstraints peerSpecifiedConstraints;

    private final boolean enabledX509DisabledAlgConstraints;

    // the default algorithm constraints
    static final AlgorithmConstraints DEFAULT =
                        new SSLAlgorithmConstraints(null);

    // the default SSL only algorithm constraints
    static final AlgorithmConstraints DEFAULT_SSL_ONLY =
                        new SSLAlgorithmConstraints((SSLSocket)null, false);

    SSLAlgorithmConstraints(AlgorithmConstraints userSpecifiedConstraints) {
        this.userSpecifiedConstraints = userSpecifiedConstraints;
        this.peerSpecifiedConstraints = null;
        this.enabledX509DisabledAlgConstraints = true;
    }

    SSLAlgorithmConstraints(SSLSocket socket,
            boolean withDefaultCertPathConstraints) {
        this.userSpecifiedConstraints = getUserSpecifiedConstraints(socket);
        this.peerSpecifiedConstraints = null;
        this.enabledX509DisabledAlgConstraints = withDefaultCertPathConstraints;
    }

    SSLAlgorithmConstraints(SSLEngine engine,
            boolean withDefaultCertPathConstraints) {
        this.userSpecifiedConstraints = getUserSpecifiedConstraints(engine);
        this.peerSpecifiedConstraints = null;
        this.enabledX509DisabledAlgConstraints = withDefaultCertPathConstraints;
    }

    SSLAlgorithmConstraints(SSLSocket socket, String[] supportedAlgorithms,
            boolean withDefaultCertPathConstraints) {
        this.userSpecifiedConstraints = getUserSpecifiedConstraints(socket);
        this.peerSpecifiedConstraints =
                new SupportedSignatureAlgorithmConstraints(supportedAlgorithms);
        this.enabledX509DisabledAlgConstraints = withDefaultCertPathConstraints;
    }

    SSLAlgorithmConstraints(SSLEngine engine, String[] supportedAlgorithms,
            boolean withDefaultCertPathConstraints) {
        this.userSpecifiedConstraints = getUserSpecifiedConstraints(engine);
        this.peerSpecifiedConstraints =
                new SupportedSignatureAlgorithmConstraints(supportedAlgorithms);
        this.enabledX509DisabledAlgConstraints = withDefaultCertPathConstraints;
    }

    private static AlgorithmConstraints getUserSpecifiedConstraints(
            SSLEngine engine) {
        if (engine != null) {
            // Note that the KeyManager or TrustManager implementation may be
            // not implemented in the same provider as SSLSocket/SSLEngine.
            // Please check the instance before casting to use SSLEngineImpl.
            if (engine instanceof SSLEngineImpl) {
                HandshakeContext hc =
                        ((SSLEngineImpl)engine).conContext.handshakeContext;
                if (hc != null) {
                    return hc.sslConfig.userSpecifiedAlgorithmConstraints;
                }
            }

            return engine.getSSLParameters().getAlgorithmConstraints();
        }

        return null;
    }

    private static AlgorithmConstraints getUserSpecifiedConstraints(
            SSLSocket socket) {
        if (socket != null) {
            // Note that the KeyManager or TrustManager implementation may be
            // not implemented in the same provider as SSLSocket/SSLEngine.
            // Please check the instance before casting to use SSLSocketImpl.
            if (socket instanceof SSLSocketImpl) {
                HandshakeContext hc =
                        ((SSLSocketImpl)socket).conContext.handshakeContext;
                if (hc != null) {
                    return hc.sslConfig.userSpecifiedAlgorithmConstraints;
                }
            }

            return socket.getSSLParameters().getAlgorithmConstraints();
        }

        return null;
    }

    @Override
    public boolean permits(Set<CryptoPrimitive> primitives,
            String algorithm, AlgorithmParameters parameters) {

        boolean permitted = true;

        if (peerSpecifiedConstraints != null) {
            permitted = peerSpecifiedConstraints.permits(
                                    primitives, algorithm, parameters);
        }

        if (permitted && userSpecifiedConstraints != null) {
            permitted = userSpecifiedConstraints.permits(
                                    primitives, algorithm, parameters);
        }

        if (permitted) {
            permitted = tlsDisabledAlgConstraints.permits(
                                    primitives, algorithm, parameters);
        }

        if (permitted && enabledX509DisabledAlgConstraints) {
            permitted = x509DisabledAlgConstraints.permits(
                                    primitives, algorithm, parameters);
        }

        return permitted;
    }

    @Override
    public boolean permits(Set<CryptoPrimitive> primitives, Key key) {

        boolean permitted = true;

        if (peerSpecifiedConstraints != null) {
            permitted = peerSpecifiedConstraints.permits(primitives, key);
        }

        if (permitted && userSpecifiedConstraints != null) {
            permitted = userSpecifiedConstraints.permits(primitives, key);
        }

        if (permitted) {
            permitted = tlsDisabledAlgConstraints.permits(primitives, key);
        }

        if (permitted && enabledX509DisabledAlgConstraints) {
            permitted = x509DisabledAlgConstraints.permits(primitives, key);
        }

        return permitted;
    }

    @Override
    public boolean permits(Set<CryptoPrimitive> primitives,
            String algorithm, Key key, AlgorithmParameters parameters) {

        boolean permitted = true;

        if (peerSpecifiedConstraints != null) {
            permitted = peerSpecifiedConstraints.permits(
                                    primitives, algorithm, key, parameters);
        }

        if (permitted && userSpecifiedConstraints != null) {
            permitted = userSpecifiedConstraints.permits(
                                    primitives, algorithm, key, parameters);
        }

        if (permitted) {
            permitted = tlsDisabledAlgConstraints.permits(
                                    primitives, algorithm, key, parameters);
        }

        if (permitted && enabledX509DisabledAlgConstraints) {
            permitted = x509DisabledAlgConstraints.permits(
                                    primitives, algorithm, key, parameters);
        }

        return permitted;
    }


    private static class SupportedSignatureAlgorithmConstraints
                                    implements AlgorithmConstraints {
        // supported signature algorithms
        private final String[] supportedAlgorithms;

        SupportedSignatureAlgorithmConstraints(String[] supportedAlgorithms) {
            if (supportedAlgorithms != null) {
                this.supportedAlgorithms = supportedAlgorithms.clone();
            } else {
                this.supportedAlgorithms = null;
            }
        }

        @Override
        public boolean permits(Set<CryptoPrimitive> primitives,
                String algorithm, AlgorithmParameters parameters) {

            if (algorithm == null || algorithm.isEmpty()) {
                throw new IllegalArgumentException(
                        "No algorithm name specified");
            }

            if (primitives == null || primitives.isEmpty()) {
                throw new IllegalArgumentException(
                        "No cryptographic primitive specified");
            }

            if (supportedAlgorithms == null ||
                        supportedAlgorithms.length == 0) {
                return false;
            }

            // trim the MGF part: <digest>with<encryption>and<mgf>
            int position = algorithm.indexOf("and");
            if (position > 0) {
                algorithm = algorithm.substring(0, position);
            }

            for (String supportedAlgorithm : supportedAlgorithms) {
                if (algorithm.equalsIgnoreCase(supportedAlgorithm)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public final boolean permits(Set<CryptoPrimitive> primitives, Key key) {
            return true;
        }

        @Override
        public final boolean permits(Set<CryptoPrimitive> primitives,
                String algorithm, Key key, AlgorithmParameters parameters) {

            if (algorithm == null || algorithm.isEmpty()) {
                throw new IllegalArgumentException(
                        "No algorithm name specified");
            }

            return permits(primitives, algorithm, parameters);
        }
    }
}
