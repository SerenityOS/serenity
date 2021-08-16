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
import java.util.Map;
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the "ClientKeyExchange" handshake message.
 */
final class ClientKeyExchange {
    static final SSLConsumer handshakeConsumer =
            new ClientKeyExchangeConsumer();
    static final HandshakeProducer handshakeProducer =
            new ClientKeyExchangeProducer();


    /**
     * The "ClientKeyExchange" handshake message producer.
     */
    private static final
            class ClientKeyExchangeProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private ClientKeyExchangeProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                        chc.negotiatedCipherSuite.keyExchange,
                        chc.negotiatedProtocol);
            if (ke != null) {
                for (Map.Entry<Byte, HandshakeProducer> hp :
                        ke.getHandshakeProducers(chc)) {
                    if (hp.getKey() == SSLHandshake.CLIENT_KEY_EXCHANGE.id) {
                        return hp.getValue().produce(context, message);
                    }
                }
            }

            // not consumer defined.
            throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected ClientKeyExchange handshake message.");
        }
    }

    /**
     * The "ClientKeyExchange" handshake message consumer.
     */
    private static final
            class ClientKeyExchangeConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private ClientKeyExchangeConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            // clean up this consumer
            shc.handshakeConsumers.remove(SSLHandshake.CLIENT_KEY_EXCHANGE.id);

            // Check for an unprocessed client Certificate message.  If that
            // handshake consumer is still present then that expected message
            // was not sent.
            if (shc.handshakeConsumers.containsKey(
                    SSLHandshake.CERTIFICATE.id)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected ClientKeyExchange handshake message.");
            }

            SSLKeyExchange ke = SSLKeyExchange.valueOf(
                    shc.negotiatedCipherSuite.keyExchange,
                    shc.negotiatedProtocol);
            if (ke != null) {
                for (Map.Entry<Byte, SSLConsumer> hc :
                        ke.getHandshakeConsumers(shc)) {
                    if (hc.getKey() == SSLHandshake.CLIENT_KEY_EXCHANGE.id) {
                        hc.getValue().consume(context, message);
                        return;
                    }
                }
            }

            // not consumer defined.
            throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected ClientKeyExchange handshake message.");
        }
    }
}

