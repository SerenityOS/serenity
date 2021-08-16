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
import java.text.MessageFormat;
import java.util.Locale;
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the EncryptedExtensions handshake message.
 */
final class EncryptedExtensions {
    static final HandshakeProducer handshakeProducer =
        new EncryptedExtensionsProducer();
    static final SSLConsumer handshakeConsumer =
        new EncryptedExtensionsConsumer();

    /**
     * The EncryptedExtensions handshake message.
     */
    static final class EncryptedExtensionsMessage extends HandshakeMessage {
        private final SSLExtensions extensions;

        EncryptedExtensionsMessage(
                HandshakeContext handshakeContext) throws IOException {
            super(handshakeContext);
            this.extensions = new SSLExtensions(this);
        }

        EncryptedExtensionsMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);

            // struct {
            //     Extension extensions<0..2^16-1>;
            // } EncryptedExtensions;
            if (m.remaining() < 2) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Invalid EncryptedExtensions handshake message: " +
                        "no sufficient data");
            }

            SSLExtension[] encryptedExtensions =
                    handshakeContext.sslConfig.getEnabledExtensions(
                            SSLHandshake.ENCRYPTED_EXTENSIONS);
            this.extensions = new SSLExtensions(this, m, encryptedExtensions);
        }

        @Override
        SSLHandshake handshakeType() {
            return SSLHandshake.ENCRYPTED_EXTENSIONS;
        }

        @Override
        int messageLength() {
            int extLen = extensions.length();
            if (extLen == 0) {
                extLen = 2;     // empty extensions
            }
            return extLen;
        }

        @Override
        void send(HandshakeOutStream hos) throws IOException {
            // Is it an empty extensions?
            if (extensions.length() == 0) {
                hos.putInt16(0);
            } else {
                extensions.send(hos);
            }
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    "\"EncryptedExtensions\": [\n" +
                    "{0}\n" +
                    "]",
                    Locale.ENGLISH);
            Object[] messageFields = {
                Utilities.indent(extensions.toString())
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The EncryptedExtensions handshake message consumer.
     */
    private static final class EncryptedExtensionsProducer
            implements HandshakeProducer {
        // Prevent instantiation of this class.
        private EncryptedExtensionsProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            EncryptedExtensionsMessage eem =
                    new EncryptedExtensionsMessage(shc);
            SSLExtension[] extTypes =
                    shc.sslConfig.getEnabledExtensions(
                            SSLHandshake.ENCRYPTED_EXTENSIONS,
                            shc.negotiatedProtocol);
            eem.extensions.produce(shc, extTypes);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced EncryptedExtensions message", eem);
            }

            // Output the handshake message.
            eem.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The EncryptedExtensions handshake message consumer.
     */
    private static final class EncryptedExtensionsConsumer
            implements SSLConsumer {
        // Prevent instantiation of this class.
        private EncryptedExtensionsConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // clean up this consumer
            chc.handshakeConsumers.remove(SSLHandshake.ENCRYPTED_EXTENSIONS.id);

            EncryptedExtensionsMessage eem =
                    new EncryptedExtensionsMessage(chc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming EncryptedExtensions handshake message", eem);
            }

            //
            // validate
            //
            SSLExtension[] extTypes = chc.sslConfig.getEnabledExtensions(
                    SSLHandshake.ENCRYPTED_EXTENSIONS);
            eem.extensions.consumeOnLoad(chc, extTypes);

            //
            // update
            //
            eem.extensions.consumeOnTrade(chc, extTypes);

            //
            // produce
            //
            // Need no new handshake message producers here.
        }
    }
}
