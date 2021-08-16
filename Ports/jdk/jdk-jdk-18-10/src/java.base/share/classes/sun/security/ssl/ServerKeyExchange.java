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
import java.nio.ByteBuffer;
import java.util.Map;
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the ServerKeyExchange handshake message.
 */
final class ServerKeyExchange {
    static final SSLConsumer handshakeConsumer =
        new ServerKeyExchangeConsumer();
    static final HandshakeProducer handshakeProducer =
        new ServerKeyExchangeProducer();

    /**
     * The "ServerKeyExchange" handshake message producer.
     */
    private static final
            class ServerKeyExchangeProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private ServerKeyExchangeProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    shc.negotiatedCipherSuite.keyExchange,
                    shc.negotiatedProtocol);
            if (ke != null) {
                for (Map.Entry<Byte, HandshakeProducer> hc :
                        ke.getHandshakeProducers(shc)) {
                    if (hc.getKey() == SSLHandshake.SERVER_KEY_EXCHANGE.id) {
                        return hc.getValue().produce(context, message);
                    }
                }
            }

            // not producer defined.
            throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No ServerKeyExchange handshake message can be produced.");
        }
    }

    /**
     * The "ServerKeyExchange" handshake message consumer.
     */
    private static final
            class ServerKeyExchangeConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private ServerKeyExchangeConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // clean up this consumer
            chc.handshakeConsumers.remove(SSLHandshake.SERVER_KEY_EXCHANGE.id);

            SSLConsumer certStatCons = chc.handshakeConsumers.remove(
                    SSLHandshake.CERTIFICATE_STATUS.id);
            if (certStatCons != null) {
                // Stapling was active but no certificate status message
                // was sent.  We need to run the absence handler which will
                // check the certificate chain.
                CertificateStatus.handshakeAbsence.absent(context, null);
            }

            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    chc.negotiatedCipherSuite.keyExchange,
                    chc.negotiatedProtocol);
            if (ke != null) {
                for (Map.Entry<Byte, SSLConsumer> hc :
                        ke.getHandshakeConsumers(chc)) {
                    if (hc.getKey() == SSLHandshake.SERVER_KEY_EXCHANGE.id) {
                        hc.getValue().consume(context, message);
                        return;
                    }
                }
            }

            // no consumer defined.
            throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected ServerKeyExchange handshake message.");
        }
    }
}

