/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.Locale;
import javax.crypto.SecretKey;
import sun.security.ssl.RSAKeyExchange.EphemeralRSACredentials;
import sun.security.ssl.RSAKeyExchange.EphemeralRSAPossession;
import sun.security.ssl.RSAKeyExchange.RSAPremasterSecret;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.X509Authentication.X509Credentials;
import sun.security.ssl.X509Authentication.X509Possession;
import sun.security.util.HexDumpEncoder;

/**
 * Pack of the "ClientKeyExchange" handshake message.
 */
final class RSAClientKeyExchange {
    static final SSLConsumer rsaHandshakeConsumer =
        new RSAClientKeyExchangeConsumer();
    static final HandshakeProducer rsaHandshakeProducer =
        new RSAClientKeyExchangeProducer();

    /**
     * The RSA ClientKeyExchange handshake message.
     */
    private static final
            class RSAClientKeyExchangeMessage extends HandshakeMessage {
        final int protocolVersion;
        final boolean useTLS10PlusSpec;
        final byte[] encrypted;

        RSAClientKeyExchangeMessage(HandshakeContext context,
                RSAPremasterSecret premaster,
                PublicKey publicKey) throws GeneralSecurityException {
            super(context);
            this.protocolVersion = context.clientHelloVersion;
            this.encrypted = premaster.getEncoded(
                    publicKey, context.sslContext.getSecureRandom());
            this.useTLS10PlusSpec = ProtocolVersion.useTLS10PlusSpec(
                    protocolVersion, context.sslContext.isDTLS());
        }

        RSAClientKeyExchangeMessage(HandshakeContext context,
                ByteBuffer m) throws IOException {
            super(context);

            if (m.remaining() < 2) {
                throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "Invalid RSA ClientKeyExchange message: insufficient data");
            }

            this.protocolVersion = context.clientHelloVersion;
            this.useTLS10PlusSpec = ProtocolVersion.useTLS10PlusSpec(
                    protocolVersion, context.sslContext.isDTLS());
            if (useTLS10PlusSpec) {
                this.encrypted = Record.getBytes16(m);
            } else {    //  SSL 3.0
                this.encrypted = new byte[m.remaining()];
                m.get(encrypted);
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CLIENT_KEY_EXCHANGE;
        }

        @Override
        public int messageLength() {
            if (useTLS10PlusSpec) {
                return encrypted.length + 2;
            } else {
                return encrypted.length;
            }
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            if (useTLS10PlusSpec) {
                hos.putBytes16(encrypted);
            } else {
                hos.write(encrypted);
            }
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"RSA ClientKeyExchange\": '{'\n" +
                "  \"client_version\":  {0}\n" +
                "  \"encncrypted\": '{'\n" +
                "{1}\n" +
                "  '}'\n" +
                "'}'",
                Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                ProtocolVersion.nameOf(protocolVersion),
                Utilities.indent(
                        hexEncoder.encodeBuffer(encrypted), "    "),
            };
            return messageFormat.format(messageFields);
        }
    }

    /**
     * The RSA "ClientKeyExchange" handshake message producer.
     */
    private static final
            class RSAClientKeyExchangeProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private RSAClientKeyExchangeProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // This happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            EphemeralRSACredentials rsaCredentials = null;
            X509Credentials x509Credentials = null;
            for (SSLCredentials credential : chc.handshakeCredentials) {
                if (credential instanceof EphemeralRSACredentials) {
                    rsaCredentials = (EphemeralRSACredentials)credential;
                    if (x509Credentials != null) {
                        break;
                    }
                } else if (credential instanceof X509Credentials) {
                    x509Credentials = (X509Credentials)credential;
                    if (rsaCredentials != null) {
                        break;
                    }
                }
            }

            if (rsaCredentials == null && x509Credentials == null) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "No RSA credentials negotiated for client key exchange");
            }

            PublicKey publicKey = (rsaCredentials != null) ?
                    rsaCredentials.popPublicKey : x509Credentials.popPublicKey;
            if (!publicKey.getAlgorithm().equals("RSA")) {      // unlikely
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Not RSA public key for client key exchange");
            }

            RSAPremasterSecret premaster;
            RSAClientKeyExchangeMessage ckem;
            try {
                premaster = RSAPremasterSecret.createPremasterSecret(chc);
                chc.handshakePossessions.add(premaster);
                ckem = new RSAClientKeyExchangeMessage(
                        chc, premaster, publicKey);
            } catch (GeneralSecurityException gse) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Cannot generate RSA premaster secret", gse);
            }
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced RSA ClientKeyExchange handshake message", ckem);
            }

            // Output the handshake message.
            ckem.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            // update the states
            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    chc.negotiatedCipherSuite.keyExchange,
                    chc.negotiatedProtocol);
            if (ke == null) {   // unlikely
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key exchange type");
            } else {
                SSLKeyDerivation masterKD = ke.createKeyDerivation(chc);
                SecretKey masterSecret =
                        masterKD.deriveKey("MasterSecret", null);

                // update the states
                chc.handshakeSession.setMasterSecret(masterSecret);
                SSLTrafficKeyDerivation kd =
                        SSLTrafficKeyDerivation.valueOf(chc.negotiatedProtocol);
                if (kd == null) {   // unlikely
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
     * The RSA "ClientKeyExchange" handshake message consumer.
     */
    private static final
            class RSAClientKeyExchangeConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private RSAClientKeyExchangeConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            EphemeralRSAPossession rsaPossession = null;
            X509Possession x509Possession = null;
            for (SSLPossession possession : shc.handshakePossessions) {
                if (possession instanceof EphemeralRSAPossession) {
                    rsaPossession = (EphemeralRSAPossession)possession;
                    break;
                } else if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    if (rsaPossession != null) {
                        break;
                    }
                }
            }

            if (rsaPossession == null && x509Possession == null) {  // unlikely
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "No RSA possessions negotiated for client key exchange");
            }

            PrivateKey privateKey = (rsaPossession != null) ?
                    rsaPossession.popPrivateKey : x509Possession.popPrivateKey;
            if (!privateKey.getAlgorithm().equals("RSA")) {     // unlikely
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Not RSA private key for client key exchange");
            }

            RSAClientKeyExchangeMessage ckem =
                    new RSAClientKeyExchangeMessage(shc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Consuming RSA ClientKeyExchange handshake message", ckem);
            }

            // create the credentials
            RSAPremasterSecret premaster;
            try {
                premaster =
                    RSAPremasterSecret.decode(shc, privateKey, ckem.encrypted);
                shc.handshakeCredentials.add(premaster);
            } catch (GeneralSecurityException gse) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Cannot decode RSA premaster secret", gse);
            }

            // update the states
            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    shc.negotiatedCipherSuite.keyExchange,
                    shc.negotiatedProtocol);
            if (ke == null) {   // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key exchange type");
            } else {
                SSLKeyDerivation masterKD = ke.createKeyDerivation(shc);
                SecretKey masterSecret =
                        masterKD.deriveKey("MasterSecret", null);

                // update the states
                shc.handshakeSession.setMasterSecret(masterSecret);
                SSLTrafficKeyDerivation kd =
                        SSLTrafficKeyDerivation.valueOf(shc.negotiatedProtocol);
                if (kd == null) {       // unlikely
                    throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                            "Not supported key derivation: " +
                            shc.negotiatedProtocol);
                } else {
                    shc.handshakeKeyDerivation =
                        kd.createKeyDerivation(shc, masterSecret);
                }
            }
        }
    }
}
