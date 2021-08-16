/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the HelloRequest handshake message.
 */
final class HelloRequest {
    static final SSLProducer kickstartProducer =
        new HelloRequestKickstartProducer();

    static final SSLConsumer handshakeConsumer =
        new HelloRequestConsumer();
    static final HandshakeProducer handshakeProducer =
        new HelloRequestProducer();

    /**
     * The HelloRequest handshake message.
     *
     * [RFC 5246] The HelloRequest message MAY be sent by the server at any
     * time.  HelloRequest is a simple notification that the client should
     * begin the negotiation process anew.
     *
     *      struct { } HelloRequest;
     */
    static final class HelloRequestMessage extends HandshakeMessage {
        HelloRequestMessage(HandshakeContext handshakeContext) {
            super(handshakeContext);
        }

        HelloRequestMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);
            if (m.hasRemaining()) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Error parsing HelloRequest message: not empty");
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.HELLO_REQUEST;
        }

        @Override
        public int messageLength() {
            return 0;
        }

        @Override
        public void send(HandshakeOutStream s) throws IOException {
            // empty, nothing to send
        }

        @Override
        public String toString() {
            return "<empty>";
        }
    }

    /**
     * The "HelloRequest" handshake message kick start producer.
     */
    private static final
            class HelloRequestKickstartProducer implements SSLProducer {
        // Prevent instantiation of this class.
        private HelloRequestKickstartProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            HelloRequestMessage hrm = new HelloRequestMessage(shc);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced HelloRequest handshake message", hrm);
            }

            // Output the handshake message.
            hrm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            // update the context

            // What's the expected response?
            shc.handshakeConsumers.put(
                    SSLHandshake.CLIENT_HELLO.id, SSLHandshake.CLIENT_HELLO);

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The "HelloRequest" handshake message producer.
     */
    private static final class HelloRequestProducer
            implements HandshakeProducer {
        // Prevent instantiation of this class.
        private HelloRequestProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            HelloRequestMessage hrm = new HelloRequestMessage(shc);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced HelloRequest handshake message", hrm);
            }

            // Output the handshake message.
            hrm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            // update the context

            // What's the expected response?
            shc.handshakeConsumers.put(
                    SSLHandshake.CLIENT_HELLO.id, SSLHandshake.CLIENT_HELLO);

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The "HelloRequest" handshake message consumer.
     */
    private static final class HelloRequestConsumer
            implements SSLConsumer {

        // Prevent instantiation of this class.
        private HelloRequestConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // For TLS 1.2 and prior versions, the HelloRequest message MAY
            // be sent by the server at any time.  Please don't clean up this
            // handshake consumer.
            HelloRequestMessage hrm = new HelloRequestMessage(chc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming HelloRequest handshake message", hrm);
            }

            if (!chc.kickstartMessageDelivered) {
                if (!chc.conContext.secureRenegotiation &&
                        !HandshakeContext.allowUnsafeRenegotiation) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Unsafe renegotiation is not allowed");
                }

                if (!chc.conContext.secureRenegotiation) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                                "Continue with insecure renegotiation");
                    }
                }

                // update the responders
                chc.handshakeProducers.put(
                        SSLHandshake.CLIENT_HELLO.id,
                        SSLHandshake.CLIENT_HELLO);

                //
                // produce response handshake message
                //
                SSLHandshake.CLIENT_HELLO.produce(context, hrm);
            } else {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ingore HelloRequest, handshaking is in progress");
                }
            }
        }
    }
}

