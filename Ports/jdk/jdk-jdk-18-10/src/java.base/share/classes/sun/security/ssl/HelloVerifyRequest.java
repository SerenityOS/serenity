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
import java.text.MessageFormat;
import java.util.Locale;
import sun.security.ssl.ClientHello.ClientHelloMessage;
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the HelloVerifyRequest handshake message.
 */
final class HelloVerifyRequest {
    static final SSLConsumer handshakeConsumer =
            new HelloVerifyRequestConsumer();
    static final HandshakeProducer handshakeProducer =
            new HelloVerifyRequestProducer();

    /**
     * The HelloVerifyRequest handshake message [RFC 6347].
     */
    static final class HelloVerifyRequestMessage extends HandshakeMessage {
        final int                   serverVersion;
        final byte[]                cookie;

        HelloVerifyRequestMessage(HandshakeContext context,
                HandshakeMessage message) throws IOException {
            super(context);
            // This happens in server side only.
            ServerHandshakeContext shc =
                    (ServerHandshakeContext)context;
            ClientHelloMessage clientHello = (ClientHelloMessage)message;

            HelloCookieManager hcMgr =
                shc.sslContext.getHelloCookieManager(ProtocolVersion.DTLS10);
            this.serverVersion = shc.clientHelloVersion;
            this.cookie = hcMgr.createCookie(shc, clientHello);
        }

        HelloVerifyRequestMessage(HandshakeContext context,
                ByteBuffer m) throws IOException {
            super(context);
            // This happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            //  struct {
            //      ProtocolVersion server_version;
            //      opaque cookie<0..2^8-1>;
            //  } HelloVerifyRequest;
            if (m.remaining() < 3) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid HelloVerifyRequest: no sufficient data");
            }

            byte major = m.get();
            byte minor = m.get();
            this.serverVersion = ((major & 0xFF) << 8) | (minor & 0xFF);
            this.cookie = Record.getBytes8(m);
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.HELLO_VERIFY_REQUEST;
        }

        @Override
        public int messageLength() {
            return 3 + cookie.length;   //  2: the length of protocol version
                                        // +1: the cookie length
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putInt8((byte)((serverVersion >>> 8) & 0xFF));
            hos.putInt8((byte)(serverVersion & 0xFF));
            hos.putBytes8(cookie);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"HelloVerifyRequest\": '{'\n" +
                "  \"server version\"      : \"{0}\",\n" +
                "  \"cookie\"              : \"{1}\",\n" +
                "'}'",
                Locale.ENGLISH);
            Object[] messageFields = {
                ProtocolVersion.nameOf(serverVersion),
                Utilities.toHexString(cookie),
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "HelloVerifyRequest" handshake message producer.
     */
    private static final
            class HelloVerifyRequestProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private HelloVerifyRequestProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // clean up this producer
            shc.handshakeProducers.remove(SSLHandshake.HELLO_VERIFY_REQUEST.id);

            HelloVerifyRequestMessage hvrm =
                    new HelloVerifyRequestMessage(shc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Produced HelloVerifyRequest handshake message", hvrm);
            }

            // Output the handshake message.
            hvrm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            // update the context

            // Stateless, clean up the handshake context as well?
            shc.handshakeHash.finish();     // forgot about the handshake hash
            shc.handshakeExtensions.clear();

            // What's the expected response?
            shc.handshakeConsumers.put(
                    SSLHandshake.CLIENT_HELLO.id, SSLHandshake.CLIENT_HELLO);

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The "HelloVerifyRequest" handshake message consumer.
     */
    private static final class HelloVerifyRequestConsumer
            implements SSLConsumer {

        // Prevent instantiation of this class.
        private HelloVerifyRequestConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // clean up this consumer
            chc.handshakeConsumers.remove(SSLHandshake.HELLO_VERIFY_REQUEST.id);
            if (!chc.handshakeConsumers.isEmpty()) {
                chc.handshakeConsumers.remove(SSLHandshake.SERVER_HELLO.id);
            }
            if (!chc.handshakeConsumers.isEmpty()) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "No more message expected before " +
                        "HelloVerifyRequest is processed");
            }

            // Refresh handshake hash.
            chc.handshakeHash.finish();     // forgot about the handshake hash

            HelloVerifyRequestMessage hvrm =
                    new HelloVerifyRequestMessage(chc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming HelloVerifyRequest handshake message", hvrm);
            }

            // Note that HelloVerifyRequest.server_version is used solely to
            // indicate packet formatting, and not as part of version
            // negotiation.  Need not to check version values match for
            // HelloVerifyRequest message.
            chc.initialClientHelloMsg.setHelloCookie(hvrm.cookie);

            //
            // produce response handshake message
            //
            SSLHandshake.CLIENT_HELLO.produce(context, hvrm);
        }
    }
}

