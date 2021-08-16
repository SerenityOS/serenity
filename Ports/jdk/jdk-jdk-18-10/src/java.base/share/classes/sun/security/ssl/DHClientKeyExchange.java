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
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.CryptoPrimitive;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.Locale;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.net.ssl.SSLHandshakeException;
import sun.security.ssl.DHKeyExchange.DHECredentials;
import sun.security.ssl.DHKeyExchange.DHEPossession;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.util.HexDumpEncoder;

/**
 * Pack of the "ClientKeyExchange" handshake message.
 */
final class DHClientKeyExchange {
    static final DHClientKeyExchangeConsumer dhHandshakeConsumer =
            new DHClientKeyExchangeConsumer();
    static final DHClientKeyExchangeProducer dhHandshakeProducer =
            new DHClientKeyExchangeProducer();

    /**
     * The DiffieHellman ClientKeyExchange handshake message.
     *
     * If the client has sent a certificate which contains a suitable
     * DiffieHellman key (for fixed_dh client authentication), then the
     * client public value is implicit and does not need to be sent again.
     * In this case, the client key exchange message will be sent, but it
     * MUST be empty.
     *
     * Currently, we don't support cipher suite that requires implicit public
     * key of client.
     */
    private static final
            class DHClientKeyExchangeMessage extends HandshakeMessage {
        private final byte[] y;        // 1 to 2^16 - 1 bytes

        DHClientKeyExchangeMessage(
                HandshakeContext handshakeContext) throws IOException {
            super(handshakeContext);
            // This happens in client side only.
            ClientHandshakeContext chc =
                    (ClientHandshakeContext)handshakeContext;

            DHEPossession dhePossession = null;
            for (SSLPossession possession : chc.handshakePossessions) {
                if (possession instanceof DHEPossession) {
                    dhePossession = (DHEPossession)possession;
                    break;
                }
            }

            if (dhePossession == null) {
                // unlikely
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No DHE credentials negotiated for client key exchange");
            }

            DHPublicKey publicKey = dhePossession.publicKey;
            DHParameterSpec params = publicKey.getParams();
            this.y = Utilities.toByteArray(publicKey.getY());
        }

        DHClientKeyExchangeMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);
            // This happens in server side only.
            ServerHandshakeContext shc =
                    (ServerHandshakeContext)handshakeContext;

            if (m.remaining() < 3) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "Invalid DH ClientKeyExchange message: insufficient data");
            }

            this.y = Record.getBytes16(m);

            if (m.hasRemaining()) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "Invalid DH ClientKeyExchange message: unknown extra data");
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CLIENT_KEY_EXCHANGE;
        }

        @Override
        public int messageLength() {
            return y.length + 2;    // 2: length filed
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putBytes16(y);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"DH ClientKeyExchange\": '{'\n" +
                "  \"parameters\": '{'\n" +
                "    \"dh_Yc\": '{'\n" +
                "{0}\n" +
                "    '}',\n" +
                "  '}'\n" +
                "'}'",
                Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                Utilities.indent(
                        hexEncoder.encodeBuffer(y), "      "),
            };
            return messageFormat.format(messageFields);
        }
    }

    /**
     * The DiffieHellman "ClientKeyExchange" handshake message producer.
     */
    private static final
            class DHClientKeyExchangeProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private DHClientKeyExchangeProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            DHECredentials dheCredentials = null;
            for (SSLCredentials cd : chc.handshakeCredentials) {
                if (cd instanceof DHECredentials) {
                    dheCredentials = (DHECredentials)cd;
                    break;
                }
            }

            if (dheCredentials == null) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No DHE credentials negotiated for client key exchange");
            }


            DHEPossession dhePossession = new DHEPossession(
                    dheCredentials, chc.sslContext.getSecureRandom());
            chc.handshakePossessions.add(dhePossession);
            DHClientKeyExchangeMessage ckem =
                    new DHClientKeyExchangeMessage(chc);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced DH ClientKeyExchange handshake message", ckem);
            }

            // Output the handshake message.
            ckem.write(chc.handshakeOutput);
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
     * The DiffieHellman "ClientKeyExchange" handshake message consumer.
     */
    private static final
            class DHClientKeyExchangeConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private DHClientKeyExchangeConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            DHEPossession dhePossession = null;
            for (SSLPossession possession : shc.handshakePossessions) {
                if (possession instanceof DHEPossession) {
                    dhePossession = (DHEPossession)possession;
                    break;
                }
            }

            if (dhePossession == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No expected DHE possessions for client key exchange");
            }

            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    shc.negotiatedCipherSuite.keyExchange,
                    shc.negotiatedProtocol);
            if (ke == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key exchange type");
            }

            DHClientKeyExchangeMessage ckem =
                    new DHClientKeyExchangeMessage(shc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Consuming DH ClientKeyExchange handshake message", ckem);
            }

            // create the credentials
            try {
                DHParameterSpec params = dhePossession.publicKey.getParams();
                DHPublicKeySpec spec = new DHPublicKeySpec(
                        new BigInteger(1, ckem.y),
                        params.getP(), params.getG());
                KeyFactory kf = KeyFactory.getInstance("DiffieHellman");
                DHPublicKey peerPublicKey =
                        (DHPublicKey)kf.generatePublic(spec);

                // check constraints of peer DHPublicKey
                if (!shc.algorithmConstraints.permits(
                        EnumSet.of(CryptoPrimitive.KEY_AGREEMENT),
                        peerPublicKey)) {
                    throw new SSLHandshakeException(
                        "DHPublicKey does not comply to algorithm constraints");
                }

                NamedGroup namedGroup = NamedGroup.valueOf(params);
                shc.handshakeCredentials.add(
                        new DHECredentials(peerPublicKey, namedGroup));
            } catch (GeneralSecurityException | java.io.IOException e) {
                throw (SSLHandshakeException)(new SSLHandshakeException(
                        "Could not generate DHPublicKey").initCause(e));
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
