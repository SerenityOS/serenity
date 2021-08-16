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
import java.security.AlgorithmConstraints;
import java.security.CryptoPrimitive;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.EnumSet;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLHandshakeException;
import sun.security.ssl.NamedGroup.NamedGroupSpec;
import sun.security.ssl.SupportedGroupsExtension.SupportedGroups;
import sun.security.ssl.X509Authentication.X509Credentials;
import sun.security.ssl.X509Authentication.X509Possession;
import sun.security.ssl.XDHKeyExchange.XDHECredentials;
import sun.security.ssl.XDHKeyExchange.XDHEPossession;
import sun.security.util.ECUtil;

final class ECDHKeyExchange {
    static final SSLPossessionGenerator poGenerator =
            new ECDHEPossessionGenerator();
    static final SSLKeyAgreementGenerator ecdhKAGenerator =
            new ECDHKAGenerator();

    // TLSv1.3
    static final SSLKeyAgreementGenerator ecdheKAGenerator =
            new ECDHEKAGenerator();

    // TLSv1-1.2, the KA gets more difficult with EC/XEC keys
    static final SSLKeyAgreementGenerator ecdheXdhKAGenerator =
            new ECDHEXDHKAGenerator();

    static final class ECDHECredentials implements NamedGroupCredentials {
        final ECPublicKey popPublicKey;
        final NamedGroup namedGroup;

        ECDHECredentials(ECPublicKey popPublicKey, NamedGroup namedGroup) {
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

        static ECDHECredentials valueOf(NamedGroup namedGroup,
            byte[] encodedPoint) throws IOException, GeneralSecurityException {

            if (namedGroup.spec != NamedGroupSpec.NAMED_GROUP_ECDHE) {
                throw new RuntimeException(
                    "Credentials decoding:  Not ECDHE named group");
            }

            if (encodedPoint == null || encodedPoint.length == 0) {
                return null;
            }

            ECParameterSpec parameters =
                    (ECParameterSpec)namedGroup.keAlgParamSpec;
            ECPoint point = ECUtil.decodePoint(
                    encodedPoint, parameters.getCurve());
            KeyFactory factory = KeyFactory.getInstance("EC");
            ECPublicKey publicKey = (ECPublicKey)factory.generatePublic(
                    new ECPublicKeySpec(point, parameters));
            return new ECDHECredentials(publicKey, namedGroup);
        }
    }

    static final class ECDHEPossession implements NamedGroupPossession {
        final PrivateKey privateKey;
        final ECPublicKey publicKey;
        final NamedGroup namedGroup;

        ECDHEPossession(NamedGroup namedGroup, SecureRandom random) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(namedGroup.keAlgParamSpec, random);
                KeyPair kp = kpg.generateKeyPair();
                privateKey = kp.getPrivate();
                publicKey = (ECPublicKey)kp.getPublic();
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(
                    "Could not generate ECDH keypair", e);
            }

            this.namedGroup = namedGroup;
        }

        ECDHEPossession(ECDHECredentials credentials, SecureRandom random) {
            ECParameterSpec params = credentials.popPublicKey.getParams();
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(params, random);
                KeyPair kp = kpg.generateKeyPair();
                privateKey = kp.getPrivate();
                publicKey = (ECPublicKey)kp.getPublic();
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(
                    "Could not generate ECDH keypair", e);
            }

            this.namedGroup = credentials.namedGroup;
        }

        @Override
        public byte[] encode() {
            return ECUtil.encodePoint(
                    publicKey.getW(), publicKey.getParams().getCurve());
        }

        // called by ClientHandshaker with either the server's static or
        // ephemeral public key
        SecretKey getAgreedSecret(
                PublicKey peerPublicKey) throws SSLHandshakeException {

            try {
                KeyAgreement ka = KeyAgreement.getInstance("ECDH");
                ka.init(privateKey);
                ka.doPhase(peerPublicKey, true);
                return ka.generateSecret("TlsPremasterSecret");
            } catch (GeneralSecurityException e) {
                throw (SSLHandshakeException) new SSLHandshakeException(
                    "Could not generate secret").initCause(e);
            }
        }

        // called by ServerHandshaker
        SecretKey getAgreedSecret(
                byte[] encodedPoint) throws SSLHandshakeException {
            try {
                ECParameterSpec params = publicKey.getParams();
                ECPoint point =
                        ECUtil.decodePoint(encodedPoint, params.getCurve());
                KeyFactory kf = KeyFactory.getInstance("EC");
                ECPublicKeySpec spec = new ECPublicKeySpec(point, params);
                PublicKey peerPublicKey = kf.generatePublic(spec);
                return getAgreedSecret(peerPublicKey);
            } catch (GeneralSecurityException | java.io.IOException e) {
                throw (SSLHandshakeException) new SSLHandshakeException(
                    "Could not generate secret").initCause(e);
            }
        }

        // Check constraints of the specified EC public key.
        void checkConstraints(AlgorithmConstraints constraints,
                byte[] encodedPoint) throws SSLHandshakeException {
            try {

                ECParameterSpec params = publicKey.getParams();
                ECPoint point =
                        ECUtil.decodePoint(encodedPoint, params.getCurve());
                ECPublicKeySpec spec = new ECPublicKeySpec(point, params);

                KeyFactory kf = KeyFactory.getInstance("EC");
                ECPublicKey pubKey = (ECPublicKey)kf.generatePublic(spec);

                // check constraints of ECPublicKey
                if (!constraints.permits(
                        EnumSet.of(CryptoPrimitive.KEY_AGREEMENT), pubKey)) {
                    throw new SSLHandshakeException(
                        "ECPublicKey does not comply to algorithm constraints");
                }
            } catch (GeneralSecurityException | java.io.IOException e) {
                throw (SSLHandshakeException) new SSLHandshakeException(
                        "Could not generate ECPublicKey").initCause(e);
            }
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

    private static final
            class ECDHEPossessionGenerator implements SSLPossessionGenerator {
        // Prevent instantiation of this class.
        private ECDHEPossessionGenerator() {
            // blank
        }

        @Override
        public SSLPossession createPossession(HandshakeContext context) {

            NamedGroup preferableNamedGroup;

            // Find most preferred EC or XEC groups
            if ((context.clientRequestedNamedGroups != null) &&
                    (!context.clientRequestedNamedGroups.isEmpty())) {
                preferableNamedGroup = SupportedGroups.getPreferredGroup(
                        context.negotiatedProtocol,
                        context.algorithmConstraints,
                        new NamedGroupSpec[] {
                            NamedGroupSpec.NAMED_GROUP_ECDHE,
                            NamedGroupSpec.NAMED_GROUP_XDH },
                        context.clientRequestedNamedGroups);
            } else {
                preferableNamedGroup = SupportedGroups.getPreferredGroup(
                        context.negotiatedProtocol,
                        context.algorithmConstraints,
                        new NamedGroupSpec[] {
                            NamedGroupSpec.NAMED_GROUP_ECDHE,
                            NamedGroupSpec.NAMED_GROUP_XDH });
            }

            if (preferableNamedGroup != null) {
                return preferableNamedGroup.createPossession(
                    context.sslContext.getSecureRandom());
            }

            // no match found, cannot use this cipher suite.
            //
            return null;
        }
    }

    private static final
            class ECDHKAGenerator implements SSLKeyAgreementGenerator {
        // Prevent instantiation of this class.
        private ECDHKAGenerator() {
            // blank
        }

        @Override
        public SSLKeyDerivation createKeyDerivation(
                HandshakeContext context) throws IOException {
            if (context instanceof ServerHandshakeContext) {
                return createServerKeyDerivation(
                        (ServerHandshakeContext)context);
            } else {
                return createClientKeyDerivation(
                        (ClientHandshakeContext)context);
            }
        }

        private SSLKeyDerivation createServerKeyDerivation(
                ServerHandshakeContext shc) throws IOException {
            X509Possession x509Possession = null;
            ECDHECredentials ecdheCredentials = null;
            for (SSLPossession poss : shc.handshakePossessions) {
                if (!(poss instanceof X509Possession)) {
                    continue;
                }

                ECParameterSpec params =
                        ((X509Possession)poss).getECParameterSpec();
                if (params == null) {
                    continue;
                }

                NamedGroup ng = NamedGroup.valueOf(params);
                if (ng == null) {
                    // unlikely, have been checked during cipher suite
                    // negotiation.
                    throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Unsupported EC server cert for ECDH key exchange");
                }

                for (SSLCredentials cred : shc.handshakeCredentials) {
                    if (!(cred instanceof ECDHECredentials)) {
                        continue;
                    }
                    if (ng.equals(((ECDHECredentials)cred).namedGroup)) {
                        ecdheCredentials = (ECDHECredentials)cred;
                        break;
                    }
                }

                if (ecdheCredentials != null) {
                    x509Possession = (X509Possession)poss;
                    break;
                }
            }

            if (x509Possession == null || ecdheCredentials == null) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No sufficient ECDHE key agreement parameters negotiated");
            }

            return new KAKeyDerivation("ECDH", shc,
                x509Possession.popPrivateKey, ecdheCredentials.popPublicKey);
        }

        private SSLKeyDerivation createClientKeyDerivation(
                ClientHandshakeContext chc) throws IOException {
            ECDHEPossession ecdhePossession = null;
            X509Credentials x509Credentials = null;
            for (SSLPossession poss : chc.handshakePossessions) {
                if (!(poss instanceof ECDHEPossession)) {
                    continue;
                }

                NamedGroup ng = ((ECDHEPossession)poss).namedGroup;
                for (SSLCredentials cred : chc.handshakeCredentials) {
                    if (!(cred instanceof X509Credentials)) {
                        continue;
                    }

                    PublicKey publicKey = ((X509Credentials)cred).popPublicKey;
                    if (!publicKey.getAlgorithm().equals("EC")) {
                        continue;
                    }
                    ECParameterSpec params =
                            ((ECPublicKey)publicKey).getParams();
                    NamedGroup namedGroup = NamedGroup.valueOf(params);
                    if (namedGroup == null) {
                        // unlikely, should have been checked previously
                        throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                            "Unsupported EC server cert for ECDH key exchange");
                    }

                    if (ng.equals(namedGroup)) {
                        x509Credentials = (X509Credentials)cred;
                        break;
                    }
                }

                if (x509Credentials != null) {
                    ecdhePossession = (ECDHEPossession)poss;
                    break;
                }
            }

            if (ecdhePossession == null || x509Credentials == null) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No sufficient ECDH key agreement parameters negotiated");
            }

            return new KAKeyDerivation("ECDH", chc,
                ecdhePossession.privateKey, x509Credentials.popPublicKey);
        }
    }

    private static final
            class ECDHEKAGenerator implements SSLKeyAgreementGenerator {
        // Prevent instantiation of this class.
        private ECDHEKAGenerator() {
            // blank
        }

        @Override
        public SSLKeyDerivation createKeyDerivation(
                HandshakeContext context) throws IOException {
            ECDHEPossession ecdhePossession = null;
            ECDHECredentials ecdheCredentials = null;
            for (SSLPossession poss : context.handshakePossessions) {
                if (!(poss instanceof ECDHEPossession)) {
                    continue;
                }

                NamedGroup ng = ((ECDHEPossession)poss).namedGroup;
                for (SSLCredentials cred : context.handshakeCredentials) {
                    if (!(cred instanceof ECDHECredentials)) {
                        continue;
                    }
                    if (ng.equals(((ECDHECredentials)cred).namedGroup)) {
                        ecdheCredentials = (ECDHECredentials)cred;
                        break;
                    }
                }

                if (ecdheCredentials != null) {
                    ecdhePossession = (ECDHEPossession)poss;
                    break;
                }
            }

            if (ecdhePossession == null || ecdheCredentials == null) {
                throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No sufficient ECDHE key agreement parameters negotiated");
            }

            return new KAKeyDerivation("ECDH", context,
                ecdhePossession.privateKey, ecdheCredentials.popPublicKey);
        }
    }

    /*
     * A Generator for TLSv1-1.2 to create a ECDHE or a XDH KeyDerivation
     * object depending on the negotiated group.
     */
    private static final
            class ECDHEXDHKAGenerator implements SSLKeyAgreementGenerator {
        // Prevent instantiation of this class.
        private ECDHEXDHKAGenerator() {
            // blank
        }

        @Override
        public SSLKeyDerivation createKeyDerivation(
                HandshakeContext context) throws IOException {

            NamedGroupPossession namedGroupPossession = null;
            NamedGroupCredentials namedGroupCredentials = null;
            NamedGroup namedGroup = null;

            // Find a possession/credential combo using the same named group
            search:
            for (SSLPossession poss : context.handshakePossessions) {
                for (SSLCredentials cred : context.handshakeCredentials) {
                    if (((poss instanceof ECDHEPossession) &&
                            (cred instanceof ECDHECredentials)) ||
                            (((poss instanceof XDHEPossession) &&
                            (cred instanceof XDHECredentials)))) {
                        NamedGroupPossession p = (NamedGroupPossession)poss;
                        NamedGroupCredentials c = (NamedGroupCredentials)cred;
                        if (p.getNamedGroup() != c.getNamedGroup()) {
                            continue;
                        } else {
                            namedGroup = p.getNamedGroup();
                        }
                        namedGroupPossession = p;
                        namedGroupCredentials = c;
                        break search;
                    }
                }
            }

            if (namedGroupPossession == null || namedGroupCredentials == null) {
                throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No sufficient ECDHE/XDH key agreement " +
                            "parameters negotiated");
            }

            String alg;
            switch (namedGroup.spec) {
                case NAMED_GROUP_ECDHE:
                    alg = "ECDH";
                    break;
                case NAMED_GROUP_XDH:
                    alg = "XDH";
                    break;
                default:
                    throw new RuntimeException("Unexpected named group type");
            }

            return new KAKeyDerivation(alg, context,
                    namedGroupPossession.getPrivateKey(),
                    namedGroupCredentials.getPublicKey());
        }
    }
}
