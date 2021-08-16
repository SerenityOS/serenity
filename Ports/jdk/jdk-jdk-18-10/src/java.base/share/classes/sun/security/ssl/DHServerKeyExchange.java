/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.security.CryptoPrimitive;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import sun.security.ssl.DHKeyExchange.DHECredentials;
import sun.security.ssl.DHKeyExchange.DHEPossession;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.X509Authentication.X509Credentials;
import sun.security.ssl.X509Authentication.X509Possession;
import sun.security.util.HexDumpEncoder;
import sun.security.util.KeyUtil;

/**
 * Pack of the ServerKeyExchange handshake message.
 */
final class DHServerKeyExchange {
    static final SSLConsumer dhHandshakeConsumer =
            new DHServerKeyExchangeConsumer();
    static final HandshakeProducer dhHandshakeProducer =
            new DHServerKeyExchangeProducer();

    /**
     * The DiffieHellman ServerKeyExchange handshake message.
     */
    private static final
            class DHServerKeyExchangeMessage extends HandshakeMessage {
        // public key encapsulated in this message
        private final byte[] p;        // 1 to 2^16 - 1 bytes
        private final byte[] g;        // 1 to 2^16 - 1 bytes
        private final byte[] y;        // 1 to 2^16 - 1 bytes

        // the signature algorithm used by this ServerKeyExchange message
        private final boolean useExplicitSigAlgorithm;
        private final SignatureScheme signatureScheme;

        // signature bytes, or null if anonymous
        private final byte[] paramsSignature;

        DHServerKeyExchangeMessage(
                HandshakeContext handshakeContext) throws IOException {
            super(handshakeContext);

            // This happens in server side only.
            ServerHandshakeContext shc =
                    (ServerHandshakeContext)handshakeContext;

            DHEPossession dhePossession = null;
            X509Possession x509Possession = null;
            for (SSLPossession possession : shc.handshakePossessions) {
                if (possession instanceof DHEPossession) {
                    dhePossession = (DHEPossession)possession;
                    if (x509Possession != null) {
                        break;
                    }
                } else if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    if (dhePossession != null) {
                        break;
                    }
                }
            }

            if (dhePossession == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "No DHE credentials negotiated for server key exchange");
            }
            DHPublicKey publicKey = dhePossession.publicKey;
            DHParameterSpec params = publicKey.getParams();
            this.p = Utilities.toByteArray(params.getP());
            this.g = Utilities.toByteArray(params.getG());
            this.y = Utilities.toByteArray(publicKey.getY());

            if (x509Possession == null) {
                // anonymous, no authentication, no signature
                paramsSignature = null;
                signatureScheme = null;
                useExplicitSigAlgorithm = false;
            } else {
                useExplicitSigAlgorithm =
                        shc.negotiatedProtocol.useTLS12PlusSpec();
                Signature signer;
                if (useExplicitSigAlgorithm) {
                    Map.Entry<SignatureScheme, Signature> schemeAndSigner =
                            SignatureScheme.getSignerOfPreferableAlgorithm(
                                    shc.algorithmConstraints,
                                    shc.peerRequestedSignatureSchemes,
                                    x509Possession,
                                    shc.negotiatedProtocol);
                    if (schemeAndSigner == null) {
                        // Unlikely, the credentials generator should have
                        // selected the preferable signature algorithm properly.
                        throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                                "No supported signature algorithm for " +
                                x509Possession.popPrivateKey.getAlgorithm() +
                                "  key");
                    } else {
                        signatureScheme = schemeAndSigner.getKey();
                        signer = schemeAndSigner.getValue();
                    }
                } else {
                    signatureScheme = null;
                    try {
                        signer = getSignature(
                                x509Possession.popPrivateKey.getAlgorithm(),
                                x509Possession.popPrivateKey);
                    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                        throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                            "Unsupported signature algorithm: " +
                            x509Possession.popPrivateKey.getAlgorithm(), e);
                    }
                }

                byte[] signature;
                try {
                    updateSignature(signer, shc.clientHelloRandom.randomBytes,
                            shc.serverHelloRandom.randomBytes);
                    signature = signer.sign();
                } catch (SignatureException ex) {
                    throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Failed to sign dhe parameters: " +
                        x509Possession.popPrivateKey.getAlgorithm(), ex);
                }
                paramsSignature = signature;
            }
        }

        DHServerKeyExchangeMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);

            // This happens in client side only.
            ClientHandshakeContext chc =
                    (ClientHandshakeContext)handshakeContext;

            this.p = Record.getBytes16(m);
            this.g = Record.getBytes16(m);
            this.y = Record.getBytes16(m);

            try {
                KeyUtil.validate(new DHPublicKeySpec(
                        new BigInteger(1, y),
                        new BigInteger(1, p),
                        new BigInteger(1, p)));
            } catch (InvalidKeyException ike) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "Invalid DH ServerKeyExchange: invalid parameters", ike);
            }

            X509Credentials x509Credentials = null;
            for (SSLCredentials cd : chc.handshakeCredentials) {
                if (cd instanceof X509Credentials) {
                    x509Credentials = (X509Credentials)cd;
                    break;
                }
            }

            if (x509Credentials == null) {
                // anonymous, no authentication, no signature
                if (m.hasRemaining()) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid DH ServerKeyExchange: unknown extra data");
                }

                this.signatureScheme = null;
                this.paramsSignature = null;
                this.useExplicitSigAlgorithm = false;

                return;
            }

            this.useExplicitSigAlgorithm =
                    chc.negotiatedProtocol.useTLS12PlusSpec();
            if (useExplicitSigAlgorithm) {
                int ssid = Record.getInt16(m);
                signatureScheme = SignatureScheme.valueOf(ssid);
                if (signatureScheme == null) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Invalid signature algorithm (" + ssid +
                            ") used in DH ServerKeyExchange handshake message");
                }

                if (!chc.localSupportedSignAlgs.contains(signatureScheme)) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Unsupported signature algorithm (" +
                            signatureScheme.name +
                            ") used in DH ServerKeyExchange handshake message");
                }
            } else {
                this.signatureScheme = null;
            }

            // read and verify the signature
            this.paramsSignature = Record.getBytes16(m);
            Signature signer;
            if (useExplicitSigAlgorithm) {
                try {
                    signer = signatureScheme.getVerifier(
                            x509Credentials.popPublicKey);
                } catch (NoSuchAlgorithmException | InvalidKeyException |
                        InvalidAlgorithmParameterException nsae) {
                    throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                            "Unsupported signature algorithm: " +
                            signatureScheme.name, nsae);
                }
            } else {
                try {
                    signer = getSignature(
                            x509Credentials.popPublicKey.getAlgorithm(),
                            x509Credentials.popPublicKey);
                } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                    throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                            "Unsupported signature algorithm: " +
                            x509Credentials.popPublicKey.getAlgorithm(), e);
                }
            }

            try {
                updateSignature(signer,
                        chc.clientHelloRandom.randomBytes,
                        chc.serverHelloRandom.randomBytes);

                if (!signer.verify(paramsSignature)) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid signature on DH ServerKeyExchange message");
                }
            } catch (SignatureException ex) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Cannot verify DH ServerKeyExchange signature", ex);
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.SERVER_KEY_EXCHANGE;
        }

        @Override
        public int messageLength() {
            int sigLen = 0;
            if (paramsSignature != null) {
                sigLen = 2 + paramsSignature.length;
                if (useExplicitSigAlgorithm) {
                    sigLen += SignatureScheme.sizeInRecord();
                }
            }

            return 6 + p.length + g.length + y.length + sigLen;
                    // 6: overhead for p, g, y values
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putBytes16(p);
            hos.putBytes16(g);
            hos.putBytes16(y);

            if (paramsSignature != null) {
                if (useExplicitSigAlgorithm) {
                    hos.putInt16(signatureScheme.id);
                }

                hos.putBytes16(paramsSignature);
            }
        }

        @Override
        public String toString() {
            if (paramsSignature == null) {    // anonymous
                MessageFormat messageFormat = new MessageFormat(
                    "\"DH ServerKeyExchange\": '{'\n" +
                    "  \"parameters\": '{'\n" +
                    "    \"dh_p\": '{'\n" +
                    "{0}\n" +
                    "    '}',\n" +
                    "    \"dh_g\": '{'\n" +
                    "{1}\n" +
                    "    '}',\n" +
                    "    \"dh_Ys\": '{'\n" +
                    "{2}\n" +
                    "    '}',\n" +
                    "  '}'\n" +
                    "'}'",
                    Locale.ENGLISH);

                HexDumpEncoder hexEncoder = new HexDumpEncoder();
                Object[] messageFields = {
                    Utilities.indent(
                            hexEncoder.encodeBuffer(p), "      "),
                    Utilities.indent(
                            hexEncoder.encodeBuffer(g), "      "),
                    Utilities.indent(
                            hexEncoder.encodeBuffer(y), "      "),
                };

                return messageFormat.format(messageFields);
            }

            if (useExplicitSigAlgorithm) {
                MessageFormat messageFormat = new MessageFormat(
                    "\"DH ServerKeyExchange\": '{'\n" +
                    "  \"parameters\": '{'\n" +
                    "    \"dh_p\": '{'\n" +
                    "{0}\n" +
                    "    '}',\n" +
                    "    \"dh_g\": '{'\n" +
                    "{1}\n" +
                    "    '}',\n" +
                    "    \"dh_Ys\": '{'\n" +
                    "{2}\n" +
                    "    '}',\n" +
                    "  '}',\n" +
                    "  \"digital signature\":  '{'\n" +
                    "    \"signature algorithm\": \"{3}\"\n" +
                    "    \"signature\": '{'\n" +
                    "{4}\n" +
                    "    '}',\n" +
                    "  '}'\n" +
                    "'}'",
                    Locale.ENGLISH);

                HexDumpEncoder hexEncoder = new HexDumpEncoder();
                Object[] messageFields = {
                    Utilities.indent(
                            hexEncoder.encodeBuffer(p), "      "),
                    Utilities.indent(
                            hexEncoder.encodeBuffer(g), "      "),
                    Utilities.indent(
                            hexEncoder.encodeBuffer(y), "      "),
                    signatureScheme.name,
                    Utilities.indent(
                            hexEncoder.encodeBuffer(paramsSignature), "      ")
                };

                return messageFormat.format(messageFields);
            } else {
                MessageFormat messageFormat = new MessageFormat(
                    "\"DH ServerKeyExchange\": '{'\n" +
                    "  \"parameters\": '{'\n" +
                    "    \"dh_p\": '{'\n" +
                    "{0}\n" +
                    "    '}',\n" +
                    "    \"dh_g\": '{'\n" +
                    "{1}\n" +
                    "    '}',\n" +
                    "    \"dh_Ys\": '{'\n" +
                    "{2}\n" +
                    "    '}',\n" +
                    "  '}',\n" +
                    "  \"signature\": '{'\n" +
                    "{3}\n" +
                    "  '}'\n" +
                    "'}'",
                    Locale.ENGLISH);

                HexDumpEncoder hexEncoder = new HexDumpEncoder();
                Object[] messageFields = {
                    Utilities.indent(
                            hexEncoder.encodeBuffer(p), "      "),
                    Utilities.indent(
                            hexEncoder.encodeBuffer(g), "      "),
                    Utilities.indent(
                            hexEncoder.encodeBuffer(y), "      "),
                    Utilities.indent(
                            hexEncoder.encodeBuffer(paramsSignature), "    ")
                };

                return messageFormat.format(messageFields);
            }
        }

        private static Signature getSignature(String keyAlgorithm,
                Key key) throws NoSuchAlgorithmException, InvalidKeyException {
            Signature signer;
            switch (keyAlgorithm) {
                case "DSA":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_DSA);
                    break;
                case "RSA":
                    signer = RSASignature.getInstance();
                    break;
                default:
                    throw new NoSuchAlgorithmException(
                        "neither an RSA or a DSA key : " + keyAlgorithm);
            }

            if (signer != null) {
                if (key instanceof PublicKey) {
                    signer.initVerify((PublicKey)(key));
                } else {
                    signer.initSign((PrivateKey)key);
                }
            }

            return signer;
        }

        /*
         * Update sig with nonces and Diffie-Hellman public key.
         */
        private void updateSignature(Signature sig, byte[] clntNonce,
                byte[] svrNonce) throws SignatureException {
            int tmp;

            sig.update(clntNonce);
            sig.update(svrNonce);

            sig.update((byte)(p.length >> 8));
            sig.update((byte)(p.length & 0x0ff));
            sig.update(p);

            sig.update((byte)(g.length >> 8));
            sig.update((byte)(g.length & 0x0ff));
            sig.update(g);

            sig.update((byte)(y.length >> 8));
            sig.update((byte)(y.length & 0x0ff));
            sig.update(y);
        }
    }

    /**
     * The DiffieHellman "ServerKeyExchange" handshake message producer.
     */
    static final class DHServerKeyExchangeProducer
            implements HandshakeProducer {
        // Prevent instantiation of this class.
        private DHServerKeyExchangeProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            DHServerKeyExchangeMessage skem =
                    new DHServerKeyExchangeMessage(shc);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced DH ServerKeyExchange handshake message", skem);
            }

            // Output the handshake message.
            skem.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The DiffieHellman "ServerKeyExchange" handshake message consumer.
     */
    static final class DHServerKeyExchangeConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private DHServerKeyExchangeConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            DHServerKeyExchangeMessage skem =
                    new DHServerKeyExchangeMessage(chc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Consuming DH ServerKeyExchange handshake message", skem);
            }

            //
            // validate
            //
            // check constraints of EC PublicKey
            DHPublicKey publicKey;
            try {
                KeyFactory kf = KeyFactory.getInstance("DiffieHellman");
                DHPublicKeySpec spec = new DHPublicKeySpec(
                        new BigInteger(1, skem.y),
                        new BigInteger(1, skem.p),
                        new BigInteger(1, skem.g));
                publicKey = (DHPublicKey)kf.generatePublic(spec);
            } catch (GeneralSecurityException gse) {
                throw chc.conContext.fatal(Alert.INSUFFICIENT_SECURITY,
                    "Could not generate DHPublicKey", gse);
            }

            if (!chc.algorithmConstraints.permits(
                    EnumSet.of(CryptoPrimitive.KEY_AGREEMENT), publicKey)) {
                throw chc.conContext.fatal(Alert.INSUFFICIENT_SECURITY,
                        "DH ServerKeyExchange does not comply to " +
                        "algorithm constraints");
            }

            //
            // update
            //
            NamedGroup namedGroup = NamedGroup.valueOf(publicKey.getParams());
            chc.handshakeCredentials.add(
                    new DHECredentials(publicKey, namedGroup));

            //
            // produce
            //
            // Need no new handshake message producers here.
        }
    }
}

