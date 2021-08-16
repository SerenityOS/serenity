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
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Locale;

import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.SSLCipher.SSLReadCipher;
import sun.security.ssl.SSLCipher.SSLWriteCipher;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Pack of the KeyUpdate handshake message.
 */
final class KeyUpdate {
    static final SSLProducer kickstartProducer =
        new KeyUpdateKickstartProducer();

    static final SSLConsumer handshakeConsumer =
        new KeyUpdateConsumer();
    static final HandshakeProducer handshakeProducer =
        new KeyUpdateProducer();

    /**
     * The KeyUpdate handshake message.
     *
     * The KeyUpdate handshake message is used to indicate that the sender is
     * updating its sending cryptographic keys.
     *
     *       enum {
     *           update_not_requested(0), update_requested(1), (255)
     *       } KeyUpdateRequest;
     *
     *       struct {
     *           KeyUpdateRequest request_update;
     *       } KeyUpdate;
     */
    static final class KeyUpdateMessage extends HandshakeMessage {
        private final KeyUpdateRequest status;

        KeyUpdateMessage(PostHandshakeContext context,
                KeyUpdateRequest status) {
            super(context);
            this.status = status;
        }

        KeyUpdateMessage(PostHandshakeContext context,
                ByteBuffer m) throws IOException {
            super(context);

            if (m.remaining() != 1) {
                throw context.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "KeyUpdate has an unexpected length of "+
                        m.remaining());
            }

            byte request = m.get();
            this.status = KeyUpdateRequest.valueOf(request);
            if (status == null) {
                throw context.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Invalid KeyUpdate message value: " +
                        KeyUpdateRequest.nameOf(request));
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.KEY_UPDATE;
        }

        @Override
        public int messageLength() {
            // one byte enum
            return 1;
        }

        @Override
        public void send(HandshakeOutStream s) throws IOException {
            s.putInt8(status.id);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    "\"KeyUpdate\": '{'\n" +
                    "  \"request_update\": {0}\n" +
                    "'}'",
                    Locale.ENGLISH);

            Object[] messageFields = {
                status.name
            };

            return messageFormat.format(messageFields);
        }
    }

    enum KeyUpdateRequest {
        NOTREQUESTED        ((byte)0, "update_not_requested"),
        REQUESTED           ((byte)1, "update_requested");

        final byte id;
        final String name;

        private KeyUpdateRequest(byte id, String name) {
            this.id = id;
            this.name = name;
        }

        static KeyUpdateRequest valueOf(byte id) {
            for (KeyUpdateRequest kur : KeyUpdateRequest.values()) {
                if (kur.id == id) {
                    return kur;
                }
            }

            return null;
        }

        static String nameOf(byte id) {
            for (KeyUpdateRequest kur : KeyUpdateRequest.values()) {
                if (kur.id == id) {
                    return kur.name;
                }
            }

            return "<UNKNOWN KeyUpdateRequest TYPE: " + (id & 0x0FF) + ">";
        }
    }

    private static final
            class KeyUpdateKickstartProducer implements SSLProducer {
        // Prevent instantiation of this class.
        private KeyUpdateKickstartProducer() {
            // blank
        }

        // Produce kickstart handshake message.
        @Override
        public byte[] produce(ConnectionContext context) throws IOException {
            PostHandshakeContext hc = (PostHandshakeContext)context;
            return handshakeProducer.produce(context,
                    new KeyUpdateMessage(hc, KeyUpdateRequest.REQUESTED));
        }
    }

    /**
     * The "KeyUpdate" handshake message consumer.
     */
    private static final class KeyUpdateConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private KeyUpdateConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            PostHandshakeContext hc = (PostHandshakeContext)context;
            KeyUpdateMessage km = new KeyUpdateMessage(hc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming KeyUpdate post-handshake message", km);
            }

            // Update read key and IV.
            SSLTrafficKeyDerivation kdg =
                SSLTrafficKeyDerivation.valueOf(hc.conContext.protocolVersion);
            if (kdg == null) {
                // unlikely
                throw hc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key derivation: " +
                                hc.conContext.protocolVersion);
            }

            SSLKeyDerivation skd = kdg.createKeyDerivation(hc,
                    hc.conContext.inputRecord.readCipher.baseSecret);
            if (skd == null) {
                // unlikely
                throw hc.conContext.fatal(
                        Alert.INTERNAL_ERROR, "no key derivation");
            }

            SecretKey nplus1 = skd.deriveKey("TlsUpdateNplus1", null);
            SSLKeyDerivation kd = kdg.createKeyDerivation(hc, nplus1);
            SecretKey key = kd.deriveKey("TlsKey", null);
            IvParameterSpec ivSpec = new IvParameterSpec(
                    kd.deriveKey("TlsIv", null).getEncoded());
            try {
                SSLReadCipher rc =
                    hc.negotiatedCipherSuite.bulkCipher.createReadCipher(
                        Authenticator.valueOf(hc.conContext.protocolVersion),
                        hc.conContext.protocolVersion, key, ivSpec,
                        hc.sslContext.getSecureRandom());

                if (rc == null) {
                    throw hc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Illegal cipher suite (" + hc.negotiatedCipherSuite +
                        ") and protocol version (" + hc.negotiatedProtocol +
                        ")");
                }

                rc.baseSecret = nplus1;
                hc.conContext.inputRecord.changeReadCiphers(rc);
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("KeyUpdate: read key updated");
                }
            } catch (GeneralSecurityException gse) {
                throw hc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Failure to derive read secrets", gse);
            }

            if (km.status == KeyUpdateRequest.REQUESTED) {
                // Update the write key and IV.
                handshakeProducer.produce(hc,
                    new KeyUpdateMessage(hc, KeyUpdateRequest.NOTREQUESTED));
                return;
            }

            // clean handshake context
            hc.conContext.finishPostHandshake();
        }
    }

    /**
     * The "KeyUpdate" handshake message producer.
     */
    private static final class KeyUpdateProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private KeyUpdateProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            PostHandshakeContext hc = (PostHandshakeContext)context;
            KeyUpdateMessage km = (KeyUpdateMessage)message;
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Produced KeyUpdate post-handshake message", km);
            }

            // Update the write key and IV.
            SSLTrafficKeyDerivation kdg =
                SSLTrafficKeyDerivation.valueOf(hc.conContext.protocolVersion);
            if (kdg == null) {
                // unlikely
                throw hc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key derivation: " +
                                hc.conContext.protocolVersion);
            }

            SSLKeyDerivation skd = kdg.createKeyDerivation(hc,
                    hc.conContext.outputRecord.writeCipher.baseSecret);
            if (skd == null) {
                // unlikely
                throw hc.conContext.fatal(
                        Alert.INTERNAL_ERROR, "no key derivation");
            }

            SecretKey nplus1 = skd.deriveKey("TlsUpdateNplus1", null);
            SSLKeyDerivation kd = kdg.createKeyDerivation(hc, nplus1);
            SecretKey key = kd.deriveKey("TlsKey", null);
            IvParameterSpec ivSpec = new IvParameterSpec(
                    kd.deriveKey("TlsIv", null).getEncoded());

            SSLWriteCipher wc;
            try {
                wc = hc.negotiatedCipherSuite.bulkCipher.createWriteCipher(
                        Authenticator.valueOf(hc.conContext.protocolVersion),
                        hc.conContext.protocolVersion, key, ivSpec,
                        hc.sslContext.getSecureRandom());
            } catch (GeneralSecurityException gse) {
                throw hc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Failure to derive write secrets", gse);
            }

            if (wc == null) {
                throw hc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Illegal cipher suite (" + hc.negotiatedCipherSuite +
                    ") and protocol version (" + hc.negotiatedProtocol + ")");
            }

            // Output the handshake message and change the write cipher.
            //
            // The KeyUpdate handshake message SHALL be delivered in the
            // changeWriteCiphers() implementation.
            wc.baseSecret = nplus1;
            hc.conContext.outputRecord.changeWriteCiphers(wc, km.status.id);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.fine("KeyUpdate: write key updated");
            }

            // clean handshake context
            hc.conContext.finishPostHandshake();

            // The handshake message has been delivered.
            return null;
        }
    }
}
