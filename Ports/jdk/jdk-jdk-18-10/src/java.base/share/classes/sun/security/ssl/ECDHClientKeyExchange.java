/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.text.MessageFormat;
import java.util.Locale;
import javax.crypto.SecretKey;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.X509Authentication.X509Credentials;
import sun.security.ssl.X509Authentication.X509Possession;
import sun.security.util.HexDumpEncoder;

/**
 * Pack of the "ClientKeyExchange" handshake message.
 *
 * This file is used by both the ECDH/ECDHE/XDH code since much of the
 * code is the same between the EC named groups (i.e.
 * x25519/x448/secp*r1), even though the APIs are very different (i.e.
 * ECPublicKey/XECPublicKey, KeyExchange.getInstance("EC"/"XDH"), etc.).
 */
final class ECDHClientKeyExchange {
    static final SSLConsumer ecdhHandshakeConsumer =
            new ECDHClientKeyExchangeConsumer();
    static final HandshakeProducer ecdhHandshakeProducer =
            new ECDHClientKeyExchangeProducer();

    static final SSLConsumer ecdheHandshakeConsumer =
            new ECDHEClientKeyExchangeConsumer();
    static final HandshakeProducer ecdheHandshakeProducer =
            new ECDHEClientKeyExchangeProducer();

    /**
     * The ECDH/ECDHE/XDH ClientKeyExchange handshake message.
     */
    private static final
            class ECDHClientKeyExchangeMessage extends HandshakeMessage {
        private final byte[] encodedPoint;

        ECDHClientKeyExchangeMessage(HandshakeContext handshakeContext,
                byte[] encodedPublicKey) {
            super(handshakeContext);

            this.encodedPoint = encodedPublicKey;
        }

        ECDHClientKeyExchangeMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);
            if (m.remaining() != 0) {       // explicit PublicValueEncoding
                this.encodedPoint = Record.getBytes8(m);
            } else {
                this.encodedPoint = new byte[0];
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CLIENT_KEY_EXCHANGE;
        }

        @Override
        public int messageLength() {
            if (encodedPoint == null || encodedPoint.length == 0) {
                return 0;
            } else {
                return 1 + encodedPoint.length;
            }
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            if (encodedPoint != null && encodedPoint.length != 0) {
                hos.putBytes8(encodedPoint);
            }
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"ECDH ClientKeyExchange\": '{'\n" +
                "  \"ecdh public\": '{'\n" +
                "{0}\n" +
                "  '}',\n" +
                "'}'",
                Locale.ENGLISH);
            if (encodedPoint == null || encodedPoint.length == 0) {
                Object[] messageFields = {
                    "    <implicit>"
                };
                return messageFormat.format(messageFields);
            } else {
                HexDumpEncoder hexEncoder = new HexDumpEncoder();
                Object[] messageFields = {
                    Utilities.indent(
                            hexEncoder.encodeBuffer(encodedPoint), "    "),
                };
                return messageFormat.format(messageFields);
            }
        }
    }

    /**
     * The ECDH "ClientKeyExchange" handshake message producer.
     */
    private static final
            class ECDHClientKeyExchangeProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private ECDHClientKeyExchangeProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            X509Credentials x509Credentials = null;
            for (SSLCredentials credential : chc.handshakeCredentials) {
                if (credential instanceof X509Credentials) {
                    x509Credentials = (X509Credentials)credential;
                    break;
                }
            }

            if (x509Credentials == null) {
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "No server certificate for ECDH client key exchange");
            }

            PublicKey publicKey = x509Credentials.popPublicKey;

            NamedGroup namedGroup = null;
            String algorithm = publicKey.getAlgorithm();

            // Determine which NamedGroup we'll be using, then use
            // the creator functions.
            if (algorithm.equals("EC")) {
                ECParameterSpec params = ((ECPublicKey)publicKey).getParams();
                namedGroup = NamedGroup.valueOf(params);
            } else if (algorithm.equals("XDH")) {
                AlgorithmParameterSpec params =
                        ((XECPublicKey)publicKey).getParams();
                if (params instanceof NamedParameterSpec) {
                    String name = ((NamedParameterSpec)params).getName();
                    namedGroup = NamedGroup.nameOf(name);
                }
            } else {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Not EC/XDH server certificate for " +
                            "ECDH client key exchange");
            }

            if (namedGroup == null) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Unsupported EC/XDH server cert for " +
                        "ECDH client key exchange");
            }

            SSLPossession sslPossession = namedGroup.createPossession(
                    chc.sslContext.getSecureRandom());

            chc.handshakePossessions.add(sslPossession);
            ECDHClientKeyExchangeMessage cke =
                    new ECDHClientKeyExchangeMessage(
                            chc, sslPossession.encode());
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced ECDH ClientKeyExchange handshake message", cke);
            }

            // Output the handshake message.
            cke.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            // update the states
            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    chc.negotiatedCipherSuite.keyExchange,
                    chc.negotiatedProtocol);
            if (ke == null) {
                // unlikely
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key exchange type");
            } else {
                SSLKeyDerivation masterKD = ke.createKeyDerivation(chc);
                SecretKey masterSecret =
                        masterKD.deriveKey("MasterSecret", null);
                chc.handshakeSession.setMasterSecret(masterSecret);

                SSLTrafficKeyDerivation kd =
                        SSLTrafficKeyDerivation.valueOf(chc.negotiatedProtocol);
                if (kd == null) {
                    // unlikely
                    throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                            "Not supported key derivation: " +
                            chc.negotiatedProtocol);
                } else {
                    chc.handshakeKeyDerivation =
                        kd.createKeyDerivation(chc, masterSecret);
                }
            }

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The ECDH "ClientKeyExchange" handshake message consumer.
     */
    private static final
            class ECDHClientKeyExchangeConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private ECDHClientKeyExchangeConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            X509Possession x509Possession = null;
            for (SSLPossession possession : shc.handshakePossessions) {
                if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    break;
                }
            }

            if (x509Possession == null) {
                // unlikely, have been checked during cipher suite negotiation.
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "No expected EC server cert for ECDH client key exchange");
            }

            // Determine which NamedGroup we'll be using, then use
            // the creator functions.
            NamedGroup namedGroup = null;

            // Iteratively determine the X509Possession type's ParameterSpec.
            ECParameterSpec ecParams = x509Possession.getECParameterSpec();
            NamedParameterSpec namedParams = null;
            if (ecParams != null) {
                namedGroup = NamedGroup.valueOf(ecParams);
            }

            // Wasn't EC, try XEC.
            if (ecParams == null) {
                namedParams = x509Possession.getXECParameterSpec();
                namedGroup = NamedGroup.nameOf(namedParams.getName());
            }

            // Can't figure this out, bail.
            if ((ecParams == null) && (namedParams == null)) {
                // unlikely, have been checked during cipher suite negotiation.
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Not EC/XDH server cert for ECDH client key exchange");
            }

            // unlikely, have been checked during cipher suite negotiation.
            if (namedGroup == null) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Unknown named group in server cert for " +
                        "ECDH client key exchange");
            }

            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    shc.negotiatedCipherSuite.keyExchange,
                    shc.negotiatedProtocol);
            if (ke == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key exchange type");
            }

            // parse either handshake message containing either EC/XEC.
            ECDHClientKeyExchangeMessage cke =
                    new ECDHClientKeyExchangeMessage(shc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Consuming ECDH ClientKeyExchange handshake message", cke);
            }

            // create the credentials
            try {
                NamedGroup ng = namedGroup;  // "effectively final" the lambda
                // AlgorithmConstraints are checked internally.
                SSLCredentials sslCredentials = namedGroup.decodeCredentials(
                        cke.encodedPoint, shc.algorithmConstraints,
                        s -> shc.conContext.fatal(Alert.INSUFFICIENT_SECURITY,
                        "ClientKeyExchange " + ng + ": " + s));

                shc.handshakeCredentials.add(sslCredentials);
            } catch (GeneralSecurityException e) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Cannot decode ECDH PublicKey: " + namedGroup);
            }

            // update the states
            SSLKeyDerivation masterKD = ke.createKeyDerivation(shc);
            SecretKey masterSecret =
                    masterKD.deriveKey("MasterSecret", null);
            shc.handshakeSession.setMasterSecret(masterSecret);

            SSLTrafficKeyDerivation kd =
                    SSLTrafficKeyDerivation.valueOf(shc.negotiatedProtocol);
            if (kd == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "Not supported key derivation: " + shc.negotiatedProtocol);
            } else {
                shc.handshakeKeyDerivation =
                    kd.createKeyDerivation(shc, masterSecret);
            }
        }
    }

    /**
     * The ECDHE "ClientKeyExchange" handshake message producer.
     */
    private static final
            class ECDHEClientKeyExchangeProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private ECDHEClientKeyExchangeProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            SSLCredentials sslCredentials = null;
            NamedGroup ng = null;

            // Find a good EC/XEC credential to use, determine the
            // NamedGroup to use for creating Possessions/Credentials/Keys.
            for (SSLCredentials cd : chc.handshakeCredentials) {
                if (cd instanceof NamedGroupCredentials) {
                    NamedGroupCredentials creds = (NamedGroupCredentials)cd;
                    ng = creds.getNamedGroup();
                    sslCredentials = cd;
                    break;
                }
            }

            if (sslCredentials == null) {
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "No ECDHE credentials negotiated for client key exchange");
            }

            SSLPossession sslPossession = ng.createPossession(
                    chc.sslContext.getSecureRandom());

            chc.handshakePossessions.add(sslPossession);

            // Write the EC/XEC message.
            ECDHClientKeyExchangeMessage cke =
                    new ECDHClientKeyExchangeMessage(
                            chc, sslPossession.encode());

            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced ECDHE ClientKeyExchange handshake message", cke);
            }

            // Output the handshake message.
            cke.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            // update the states
            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    chc.negotiatedCipherSuite.keyExchange,
                    chc.negotiatedProtocol);
            if (ke == null) {
                // unlikely
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key exchange type");
            } else {
                SSLKeyDerivation masterKD = ke.createKeyDerivation(chc);
                SecretKey masterSecret =
                        masterKD.deriveKey("MasterSecret", null);
                chc.handshakeSession.setMasterSecret(masterSecret);

                SSLTrafficKeyDerivation kd =
                        SSLTrafficKeyDerivation.valueOf(chc.negotiatedProtocol);
                if (kd == null) {
                    // unlikely
                    throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                            "Not supported key derivation: " +
                            chc.negotiatedProtocol);
                } else {
                    chc.handshakeKeyDerivation =
                        kd.createKeyDerivation(chc, masterSecret);
                }
            }

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The ECDHE "ClientKeyExchange" handshake message consumer.
     */
    private static final
            class ECDHEClientKeyExchangeConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private ECDHEClientKeyExchangeConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            SSLPossession sslPossession = null;
            NamedGroup namedGroup = null;

           // Find a good EC/XEC credential to use, determine the
           // NamedGroup to use for creating Possessions/Credentials/Keys.
            for (SSLPossession possession : shc.handshakePossessions) {
                if (possession instanceof NamedGroupPossession) {
                    NamedGroupPossession poss =
                            (NamedGroupPossession)possession;
                    namedGroup = poss.getNamedGroup();
                    sslPossession = poss;
                    break;
                }
            }

            if (sslPossession == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "No expected ECDHE possessions for client key exchange");
            }

            if (namedGroup == null) {
                // unlikely, have been checked during cipher suite negotiation
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Unsupported EC server cert for ECDHE client key exchange");
            }

            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    shc.negotiatedCipherSuite.keyExchange,
                    shc.negotiatedProtocol);
            if (ke == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key exchange type");
            }

            // parse the EC/XEC handshake message
            ECDHClientKeyExchangeMessage cke =
                    new ECDHClientKeyExchangeMessage(shc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Consuming ECDHE ClientKeyExchange handshake message", cke);
            }

            // create the credentials
            try {
                NamedGroup ng = namedGroup; // "effectively final" the lambda
                // AlgorithmConstraints are checked internally.
                SSLCredentials sslCredentials = namedGroup.decodeCredentials(
                        cke.encodedPoint, shc.algorithmConstraints,
                        s -> shc.conContext.fatal(Alert.INSUFFICIENT_SECURITY,
                        "ClientKeyExchange " + ng + ": " + s));

                shc.handshakeCredentials.add(sslCredentials);
            } catch (GeneralSecurityException e) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Cannot decode named group: " + namedGroup);
            }

            // update the states
            SSLKeyDerivation masterKD = ke.createKeyDerivation(shc);
            SecretKey masterSecret =
                    masterKD.deriveKey("MasterSecret", null);
            shc.handshakeSession.setMasterSecret(masterSecret);

            SSLTrafficKeyDerivation kd =
                    SSLTrafficKeyDerivation.valueOf(shc.negotiatedProtocol);
            if (kd == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "Not supported key derivation: " + shc.negotiatedProtocol);
            } else {
                shc.handshakeKeyDerivation =
                    kd.createKeyDerivation(shc, masterSecret);
            }
        }
    }
}
