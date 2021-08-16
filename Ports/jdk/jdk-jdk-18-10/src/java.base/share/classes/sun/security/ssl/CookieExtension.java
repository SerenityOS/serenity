/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import javax.net.ssl.SSLProtocolException;

import sun.security.ssl.ClientHello.ClientHelloMessage;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.ServerHello.ServerHelloMessage;
import sun.security.util.HexDumpEncoder;

public class CookieExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHCookieProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHCookieConsumer();
    static final HandshakeConsumer chOnTradeConsumer =
            new CHCookieUpdate();

    static final HandshakeProducer hrrNetworkProducer =
            new HRRCookieProducer();
    static final ExtensionConsumer hrrOnLoadConsumer =
            new HRRCookieConsumer();

    static final HandshakeProducer hrrNetworkReproducer =
            new HRRCookieReproducer();

    static final CookieStringizer cookieStringizer =
            new CookieStringizer();

    /**
     * The "cookie" extension.
     */
    static class CookieSpec implements SSLExtensionSpec {
        final byte[] cookie;

        private CookieSpec(HandshakeContext hc,
                ByteBuffer m) throws IOException {
            // opaque cookie<1..2^16-1>;
            if (m.remaining() < 3) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid cookie extension: insufficient data"));
            }

            this.cookie = Record.getBytes16(m);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    "\"cookie\": '{'\n" +
                    "{0}\n" +
                    "'}',", Locale.ENGLISH);
            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                Utilities.indent(hexEncoder.encode(cookie))
            };

            return messageFormat.format(messageFields);
        }
    }

    private static final class CookieStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new CookieSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    private static final
            class CHCookieProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private CHCookieProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext) context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(SSLExtension.CH_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return null;
            }

            // response to an HelloRetryRequest cookie
            CookieSpec spec = (CookieSpec)chc.handshakeExtensions.get(
                    SSLExtension.HRR_COOKIE);

            if (spec != null &&
                    spec.cookie != null && spec.cookie.length != 0) {
                byte[] extData = new byte[spec.cookie.length + 2];
                ByteBuffer m = ByteBuffer.wrap(extData);
                Record.putBytes16(m, spec.cookie);
                return extData;
            }

            return null;
        }
    }

    private static final
            class CHCookieConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private CHCookieConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(SSLExtension.CH_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return;     // ignore the extension
            }

            CookieSpec spec = new CookieSpec(shc, buffer);
            shc.handshakeExtensions.put(SSLExtension.CH_COOKIE, spec);

            // No impact on session resumption.
            //
            // Note that the protocol version negotiation happens before the
            // session resumption negotiation.  And the session resumption
            // negotiation depends on the negotiated protocol version.
        }
    }

    private static final
            class CHCookieUpdate implements HandshakeConsumer {
        // Prevent instantiation of this class.
        private CHCookieUpdate() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ClientHelloMessage clientHello = (ClientHelloMessage)message;

            CookieSpec spec = (CookieSpec)
                    shc.handshakeExtensions.get(SSLExtension.CH_COOKIE);
            if (spec == null) {
                // Ignore, no "cookie" extension requested.
                return;
            }

            HelloCookieManager hcm =
                shc.sslContext.getHelloCookieManager(shc.negotiatedProtocol);
            if (!hcm.isCookieValid(shc, clientHello, spec.cookie)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "unrecognized cookie");
            }
        }
    }

    private static final
            class HRRCookieProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private HRRCookieProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ServerHelloMessage hrrm = (ServerHelloMessage)message;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(SSLExtension.HRR_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return null;
            }

            HelloCookieManager hcm =
                shc.sslContext.getHelloCookieManager(shc.negotiatedProtocol);

            byte[] cookie = hcm.createCookie(shc, hrrm.clientHello);

            byte[] extData = new byte[cookie.length + 2];
            ByteBuffer m = ByteBuffer.wrap(extData);
            Record.putBytes16(m, cookie);

            return extData;
        }
    }

    private static final
            class HRRCookieConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private HRRCookieConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(SSLExtension.HRR_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return;     // ignore the extension
            }

            CookieSpec spec = new CookieSpec(chc, buffer);
            chc.handshakeExtensions.put(SSLExtension.HRR_COOKIE, spec);
        }
    }

    private static final
            class HRRCookieReproducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private HRRCookieReproducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext) context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(SSLExtension.HRR_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return null;
            }

            // copy of the ClientHello cookie
            CookieSpec spec = (CookieSpec)shc.handshakeExtensions.get(
                    SSLExtension.CH_COOKIE);

            if (spec != null &&
                    spec.cookie != null && spec.cookie.length != 0) {
                byte[] extData = new byte[spec.cookie.length + 2];
                ByteBuffer m = ByteBuffer.wrap(extData);
                Record.putBytes16(m, spec.cookie);
                return extData;
            }

            return null;
        }
    }
}
