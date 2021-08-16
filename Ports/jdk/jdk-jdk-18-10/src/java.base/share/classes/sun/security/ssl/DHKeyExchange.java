/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import sun.security.action.GetPropertyAction;
import sun.security.ssl.NamedGroup.NamedGroupSpec;
import sun.security.ssl.SupportedGroupsExtension.SupportedGroups;
import sun.security.ssl.X509Authentication.X509Possession;
import sun.security.util.KeyUtil;

final class DHKeyExchange {
    static final SSLPossessionGenerator poGenerator =
            new DHEPossessionGenerator(false);
    static final SSLPossessionGenerator poExportableGenerator =
            new DHEPossessionGenerator(true);
    static final SSLKeyAgreementGenerator kaGenerator =
            new DHEKAGenerator();

    static final class DHECredentials implements NamedGroupCredentials {
        final DHPublicKey popPublicKey;
        final NamedGroup namedGroup;

        DHECredentials(DHPublicKey popPublicKey, NamedGroup namedGroup) {
            this.popPublicKey = popPublicKey;
            this.namedGroup = namedGroup;
        }

        @Override
        public PublicKey getPublicKey() {
            return popPublicKey;
        }

        @Override
        public NamedGroup getNamedGroup() {
            return namedGroup;
        }

        static DHECredentials valueOf(NamedGroup ng,
            byte[] encodedPublic) throws IOException, GeneralSecurityException {

            if (ng.spec != NamedGroupSpec.NAMED_GROUP_FFDHE) {
                throw new RuntimeException(
                        "Credentials decoding:  Not FFDHE named group");
            }

            if (encodedPublic == null || encodedPublic.length == 0) {
                return null;
            }

            DHParameterSpec params = (DHParameterSpec)ng.keAlgParamSpec;
            KeyFactory kf = KeyFactory.getInstance("DiffieHellman");
            DHPublicKeySpec spec = new DHPublicKeySpec(
                    new BigInteger(1, encodedPublic),
                    params.getP(), params.getG());
            DHPublicKey publicKey =
                    (DHPublicKey)kf.generatePublic(spec);

            return new DHECredentials(publicKey, ng);
        }
    }

    static final class DHEPossession implements NamedGroupPossession {
        final PrivateKey privateKey;
        final DHPublicKey publicKey;
        final NamedGroup namedGroup;

        DHEPossession(NamedGroup namedGroup, SecureRandom random) {
            try {
                KeyPairGenerator kpg =
                        KeyPairGenerator.getInstance("DiffieHellman");
                kpg.initialize(namedGroup.keAlgParamSpec, random);
                KeyPair kp = generateDHKeyPair(kpg);
                if (kp == null) {
                    throw new RuntimeException("Could not generate DH keypair");
                }
                privateKey = kp.getPrivate();
                publicKey = (DHPublicKey)kp.getPublic();
            } catch (GeneralSecurityException gse) {
                throw new RuntimeException(
                        "Could not generate DH keypair", gse);
            }

            this.namedGroup = namedGroup;
        }

        DHEPossession(int keyLength, SecureRandom random) {
            DHParameterSpec params =
                    PredefinedDHParameterSpecs.definedParams.get(keyLength);
            try {
                KeyPairGenerator kpg =
                    KeyPairGenerator.getInstance("DiffieHellman");
                if (params != null) {
                    kpg.initialize(params, random);
                } else {
                    kpg.initialize(keyLength, random);
                }

                KeyPair kp = generateDHKeyPair(kpg);
                if (kp == null) {
                    throw new RuntimeException(
                            "Could not generate DH keypair of " +
                            keyLength + " bits");
                }
                privateKey = kp.getPrivate();
                publicKey = (DHPublicKey)kp.getPublic();
            } catch (GeneralSecurityException gse) {
                throw new RuntimeException(
                        "Could not generate DH keypair", gse);
            }

            this.namedGroup = NamedGroup.valueOf(publicKey.getParams());
        }

        DHEPossession(DHECredentials credentials, SecureRandom random) {
            try {
                KeyPairGenerator kpg =
                        KeyPairGenerator.getInstance("DiffieHellman");
                kpg.initialize(credentials.popPublicKey.getParams(), random);
                KeyPair kp = generateDHKeyPair(kpg);
                if (kp == null) {
                    throw new RuntimeException("Could not generate DH keypair");
                }
                privateKey = kp.getPrivate();
                publicKey = (DHPublicKey)kp.getPublic();
            } catch (GeneralSecurityException gse) {
                throw new RuntimeException(
                        "Could not generate DH keypair", gse);
            }

            this.namedGroup = credentials.namedGroup;
        }

        // Generate and validate DHPublicKeySpec
        private KeyPair generateDHKeyPair(
                KeyPairGenerator kpg) throws GeneralSecurityException {
            boolean doExtraValidation =
                    (!KeyUtil.isOracleJCEProvider(kpg.getProvider().getName()));
            boolean isRecovering = false;
            for (int i = 0; i <= 2; i++) {      // Try to recover from failure.
                KeyPair kp = kpg.generateKeyPair();
                // validate the Diffie-Hellman public key
                if (doExtraValidation) {
                    DHPublicKeySpec spec = getDHPublicKeySpec(kp.getPublic());
                    try {
                        KeyUtil.validate(spec);
                    } catch (InvalidKeyException ivke) {
                        if (isRecovering) {
                            throw ivke;
                        }
                        // otherwise, ignore the exception and try again
                        isRecovering = true;
                        continue;
                    }
                }

                return kp;
            }

            return null;
        }

        private static DHPublicKeySpec getDHPublicKeySpec(PublicKey key) {
            if (key instanceof DHPublicKey) {
                DHPublicKey dhKey = (DHPublicKey)key;
                DHParameterSpec params = dhKey.getParams();
                return new DHPublicKeySpec(dhKey.getY(),
                                        params.getP(), params.getG());
            }
            try {
                KeyFactory factory = KeyFactory.getInstance("DiffieHellman");
                return factory.getKeySpec(key, DHPublicKeySpec.class);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                // unlikely
                throw new RuntimeException("Unable to get DHPublicKeySpec", e);
            }
        }

        @Override
        public byte[] encode() {
            // Note: the DH public value is encoded as a big-endian integer
            // and padded to the left with zeros to the size of p in bytes.
            byte[] encoded = Utilities.toByteArray(publicKey.getY());
            int pSize = (KeyUtil.getKeySize(publicKey) + 7) >>> 3;
            if (pSize > 0 && encoded.length < pSize) {
                byte[] buffer = new byte[pSize];
                System.arraycopy(encoded, 0,
                        buffer, pSize - encoded.length, encoded.length);
                encoded = buffer;
            }

            return encoded;
        }

        @Override
        public PublicKey getPublicKey() {
            return publicKey;
        }

        @Override
        public NamedGroup getNamedGroup() {
            return namedGroup;
        }

        @Override
        public PrivateKey getPrivateKey() {
            return privateKey;
        }
    }

    private static final class
            DHEPossessionGenerator implements SSLPossessionGenerator {
        // Flag to use smart ephemeral DH key which size matches the
        // corresponding authentication key
        private static final boolean useSmartEphemeralDHKeys;

        // Flag to use legacy ephemeral DH key which size is 512 bits for
        // exportable cipher suites, and 768 bits for others
        private static final boolean useLegacyEphemeralDHKeys;

        // The customized ephemeral DH key size for non-exportable
        // cipher suites.
        private static final int customizedDHKeySize;

        // Is it for exportable cipher suite?
        private final boolean exportable;

        static {
            String property = GetPropertyAction.privilegedGetProperty(
                    "jdk.tls.ephemeralDHKeySize");
            if (property == null || property.isEmpty()) {
                useLegacyEphemeralDHKeys = false;
                useSmartEphemeralDHKeys = false;
                customizedDHKeySize = -1;
            } else if ("matched".equals(property)) {
                useLegacyEphemeralDHKeys = false;
                useSmartEphemeralDHKeys = true;
                customizedDHKeySize = -1;
            } else if ("legacy".equals(property)) {
                useLegacyEphemeralDHKeys = true;
                useSmartEphemeralDHKeys = false;
                customizedDHKeySize = -1;
            } else {
                useLegacyEphemeralDHKeys = false;
                useSmartEphemeralDHKeys = false;

                try {
                    // DH parameter generation can be extremely slow, best to
                    // use one of the supported pre-computed DH parameters
                    // (see DHCrypt class).
                    customizedDHKeySize = Integer.parseUnsignedInt(property);
                    if (customizedDHKeySize < 1024 ||
                            customizedDHKeySize > 8192 ||
                            (customizedDHKeySize & 0x3f) != 0) {
                        throw new IllegalArgumentException(
                            "Unsupported customized DH key size: " +
                            customizedDHKeySize + ". " +
                            "The key size must be multiple of 64, " +
                            "and range from 1024 to 8192 (inclusive)");
                    }
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(
                        "Invalid system property jdk.tls.ephemeralDHKeySize");
                }
            }
        }

        // Prevent instantiation of this class.
        private DHEPossessionGenerator(boolean exportable) {
            this.exportable = exportable;
        }

        // Used for ServerKeyExchange, TLS 1.2 and prior versions.
        @Override
        public SSLPossession createPossession(HandshakeContext context) {
            NamedGroup preferableNamedGroup;
            if (!useLegacyEphemeralDHKeys &&
                    (context.clientRequestedNamedGroups != null) &&
                    (!context.clientRequestedNamedGroups.isEmpty())) {
                preferableNamedGroup =
                        SupportedGroups.getPreferredGroup(context.negotiatedProtocol,
                                context.algorithmConstraints,
                                new NamedGroupSpec [] {
                                    NamedGroupSpec.NAMED_GROUP_FFDHE },
                                context.clientRequestedNamedGroups);
                if (preferableNamedGroup != null) {
                    return new DHEPossession(preferableNamedGroup,
                                context.sslContext.getSecureRandom());
                }
            }

            /*
             * 768 bits ephemeral DH private keys were used to be used in
             * ServerKeyExchange except that exportable ciphers max out at 512
             * bits modulus values. We still adhere to this behavior in legacy
             * mode (system property "jdk.tls.ephemeralDHKeySize" is defined
             * as "legacy").
             *
             * Old JDK (JDK 7 and previous) releases don't support DH keys
             * bigger than 1024 bits. We have to consider the compatibility
             * requirement. 1024 bits DH key is always used for non-exportable
             * cipher suites in default mode (system property
             * "jdk.tls.ephemeralDHKeySize" is not defined).
             *
             * However, if applications want more stronger strength, setting
             * system property "jdk.tls.ephemeralDHKeySize" to "matched"
             * is a workaround to use ephemeral DH key which size matches the
             * corresponding authentication key. For example, if the public key
             * size of an authentication certificate is 2048 bits, then the
             * ephemeral DH key size should be 2048 bits accordingly unless
             * the cipher suite is exportable.  This key sizing scheme keeps
             * the cryptographic strength consistent between authentication
             * keys and key-exchange keys.
             *
             * Applications may also want to customize the ephemeral DH key
             * size to a fixed length for non-exportable cipher suites. This
             * can be approached by setting system property
             * "jdk.tls.ephemeralDHKeySize" to a valid positive integer between
             * 1024 and 8192 bits, inclusive.
             *
             * Note that the minimum acceptable key size is 1024 bits except
             * exportable cipher suites or legacy mode.
             *
             * Note that per RFC 2246, the key size limit of DH is 512 bits for
             * exportable cipher suites.  Because of the weakness, exportable
             * cipher suites are deprecated since TLS v1.1 and they are not
             * enabled by default in Oracle provider. The legacy behavior is
             * reserved and 512 bits DH key is always used for exportable
             * cipher suites.
             */
            int keySize = exportable ? 512 : 1024;           // default mode
            if (!exportable) {
                if (useLegacyEphemeralDHKeys) {          // legacy mode
                    keySize = 768;
                } else if (useSmartEphemeralDHKeys) {    // matched mode
                    PrivateKey key = null;
                    ServerHandshakeContext shc =
                            (ServerHandshakeContext)context;
                    if (shc.interimAuthn instanceof X509Possession) {
                        key = ((X509Possession)shc.interimAuthn).popPrivateKey;
                    }

                    if (key != null) {
                        int ks = KeyUtil.getKeySize(key);

                        // DH parameter generation can be extremely slow, make
                        // sure to use one of the supported pre-computed DH
                        // parameters.
                        //
                        // Old deployed applications may not be ready to
                        // support DH key sizes bigger than 2048 bits.  Please
                        // DON'T use value other than 1024 and 2048 at present.
                        // May improve the underlying providers and key size
                        // limit in the future when the compatibility and
                        // interoperability impact is limited.
                        keySize = ks <= 1024 ? 1024 : 2048;
                    } // Otherwise, anonymous cipher suites, 1024-bit is used.
                } else if (customizedDHKeySize > 0) {    // customized mode
                    keySize = customizedDHKeySize;
                }
            }

            return new DHEPossession(
                    keySize, context.sslContext.getSecureRandom());
        }
    }

    private static final
            class DHEKAGenerator implements SSLKeyAgreementGenerator {
        private static final DHEKAGenerator instance = new DHEKAGenerator();

        // Prevent instantiation of this class.
        private DHEKAGenerator() {
            // blank
        }

        @Override
        public SSLKeyDerivation createKeyDerivation(
                HandshakeContext context) throws IOException {
            DHEPossession dhePossession = null;
            DHECredentials dheCredentials = null;
            for (SSLPossession poss : context.handshakePossessions) {
                if (!(poss instanceof DHEPossession)) {
                    continue;
                }

                DHEPossession dhep = (DHEPossession)poss;
                for (SSLCredentials cred : context.handshakeCredentials) {
                    if (!(cred instanceof DHECredentials)) {
                        continue;
                    }
                    DHECredentials dhec = (DHECredentials)cred;
                    if (dhep.namedGroup != null && dhec.namedGroup != null) {
                        if (dhep.namedGroup.equals(dhec.namedGroup)) {
                            dheCredentials = (DHECredentials)cred;
                            break;
                        }
                    } else {
                        DHParameterSpec pps = dhep.publicKey.getParams();
                        DHParameterSpec cps = dhec.popPublicKey.getParams();
                        if (pps.getP().equals(cps.getP()) &&
                                pps.getG().equals(cps.getG())) {
                            dheCredentials = (DHECredentials)cred;
                            break;
                        }
                    }
                }

                if (dheCredentials != null) {
                    dhePossession = (DHEPossession)poss;
                    break;
                }
            }

            if (dhePossession == null || dheCredentials == null) {
                throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No sufficient DHE key agreement parameters negotiated");
            }

            return new KAKeyDerivation("DiffieHellman", context,
                    dhePossession.privateKey, dheCredentials.popPublicKey);
        }
    }
}
