/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
import static sun.security.ssl.SSLExtension.CH_SUPPORTED_VERSIONS;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import static sun.security.ssl.SSLExtension.HRR_SUPPORTED_VERSIONS;
import static sun.security.ssl.SSLExtension.SH_SUPPORTED_VERSIONS;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the "supported_versions" extensions.
 */
final class SupportedVersionsExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHSupportedVersionsProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHSupportedVersionsConsumer();
    static final SSLStringizer chStringizer =
            new CHSupportedVersionsStringizer();

    static final HandshakeProducer shNetworkProducer =
            new SHSupportedVersionsProducer();
    static final ExtensionConsumer shOnLoadConsumer =
            new SHSupportedVersionsConsumer();
    static final SSLStringizer shStringizer =
            new SHSupportedVersionsStringizer();

    static final HandshakeProducer hrrNetworkProducer =
            new HRRSupportedVersionsProducer();
    static final ExtensionConsumer hrrOnLoadConsumer =
            new HRRSupportedVersionsConsumer();
    static final HandshakeProducer hrrReproducer =
            new HRRSupportedVersionsReproducer();
    static final SSLStringizer hrrStringizer =
            new SHSupportedVersionsStringizer();
    /**
     * The "supported_versions" extension in ClientHello.
     */
    static final class CHSupportedVersionsSpec implements SSLExtensionSpec {
        final int[] requestedProtocols;

        private CHSupportedVersionsSpec(int[] requestedProtocols) {
            this.requestedProtocols = requestedProtocols;
        }

        private CHSupportedVersionsSpec(HandshakeContext hc,
                ByteBuffer m) throws IOException  {
            if (m.remaining() < 3) {        //  1: the length of the list
                                            // +2: one version at least
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid supported_versions extension: insufficient data"));
            }

            byte[] vbs = Record.getBytes8(m);   // Get the version bytes.
            if (m.hasRemaining()) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid supported_versions extension: unknown extra data"));
            }

            if (vbs == null || vbs.length == 0 || (vbs.length & 0x01) != 0) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid supported_versions extension: incomplete data"));
            }

            int[] protocols = new int[vbs.length >> 1];
            for (int i = 0, j = 0; i < vbs.length;) {
                byte major = vbs[i++];
                byte minor = vbs[i++];
                protocols[j++] = ((major & 0xFF) << 8) | (minor & 0xFF);
            }

            this.requestedProtocols = protocols;
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"versions\": '['{0}']'", Locale.ENGLISH);

            if (requestedProtocols == null || requestedProtocols.length == 0) {
                Object[] messageFields = {
                        "<no supported version specified>"
                    };
                return messageFormat.format(messageFields);
            } else {
                StringBuilder builder = new StringBuilder(512);
                boolean isFirst = true;
                for (int pv : requestedProtocols) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        builder.append(", ");
                    }

                    builder.append(ProtocolVersion.nameOf(pv));
                }

                Object[] messageFields = {
                        builder.toString()
                    };

                return messageFormat.format(messageFields);
            }
        }
    }

    private static final
            class CHSupportedVersionsStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new CHSupportedVersionsSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of a "supported_versions" extension in ClientHello.
     */
    private static final
            class CHSupportedVersionsProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private CHSupportedVersionsProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(CH_SUPPORTED_VERSIONS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " +
                        CH_SUPPORTED_VERSIONS.name);
                }
                return null;
            }

            // Produce the extension.
            //
            // The activated protocols are used as the supported versions.
            int[] protocols = new int[chc.activeProtocols.size()];
            int verLen = protocols.length * 2;
            byte[] extData = new byte[verLen + 1];      // 1: versions length
            extData[0] = (byte)(verLen & 0xFF);
            int i = 0, j = 1;
            for (ProtocolVersion pv : chc.activeProtocols) {
                protocols[i++] = pv.id;
                extData[j++] = pv.major;
                extData[j++] = pv.minor;
            }

            // Update the context.
            chc.handshakeExtensions.put(CH_SUPPORTED_VERSIONS,
                    new CHSupportedVersionsSpec(protocols));

            return extData;
        }
    }

    /**
     * Network data consumer of a "supported_versions" extension in ClientHello.
     */
    private static final
            class CHSupportedVersionsConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private CHSupportedVersionsConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(CH_SUPPORTED_VERSIONS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " +
                        CH_SUPPORTED_VERSIONS.name);
                }
                return;     // ignore the extension
            }

            // Parse the extension.
            CHSupportedVersionsSpec spec =
                    new CHSupportedVersionsSpec(shc, buffer);

            // Update the context.
            shc.handshakeExtensions.put(CH_SUPPORTED_VERSIONS, spec);

            // No impact on session resumption.
            //
            // Note that the protocol version negotiation happens before the
            // session resumption negotiation.  And the session resumption
            // negotiation depends on the negotiated protocol version.
        }
    }

    /**
     * The "supported_versions" extension in ServerHello and HelloRetryRequest.
     */
    static final class SHSupportedVersionsSpec implements SSLExtensionSpec {
        final int selectedVersion;

        private SHSupportedVersionsSpec(ProtocolVersion selectedVersion) {
            this.selectedVersion = selectedVersion.id;
        }

        private SHSupportedVersionsSpec(HandshakeContext hc,
                ByteBuffer m) throws IOException  {
            if (m.remaining() != 2) {       // 2: the selected version
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid supported_versions: insufficient data"));
            }

            byte major = m.get();
            byte minor = m.get();
            this.selectedVersion = ((major & 0xFF) << 8) | (minor & 0xFF);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"selected version\": '['{0}']'", Locale.ENGLISH);

            Object[] messageFields = {
                    ProtocolVersion.nameOf(selectedVersion)
                };
            return messageFormat.format(messageFields);
        }
    }

    private static final
            class SHSupportedVersionsStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new SHSupportedVersionsSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of a "supported_versions" extension in ServerHello.
     */
    private static final
            class SHSupportedVersionsProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private SHSupportedVersionsProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // In response to supported_versions request only
            CHSupportedVersionsSpec svs = (CHSupportedVersionsSpec)
                    shc.handshakeExtensions.get(CH_SUPPORTED_VERSIONS);
            if (svs == null) {
                // Unlikely, no key_share extension requested.
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "Ignore unavailable supported_versions extension");
                }
                return null;
            }

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(SH_SUPPORTED_VERSIONS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " +
                        SH_SUPPORTED_VERSIONS.name);
                }
                return null;
            }

            // Produce the extension.
            byte[] extData = new byte[2];
            extData[0] = shc.negotiatedProtocol.major;
            extData[1] = shc.negotiatedProtocol.minor;

            // Update the context.
            shc.handshakeExtensions.put(SH_SUPPORTED_VERSIONS,
                    new SHSupportedVersionsSpec(shc.negotiatedProtocol));

            return extData;
        }
    }

    /**
     * Network data consumer of a "supported_versions" extension in ServerHello.
     */
    private static final
            class SHSupportedVersionsConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private SHSupportedVersionsConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(SH_SUPPORTED_VERSIONS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " +
                        SH_SUPPORTED_VERSIONS.name);
                }
                return;     // ignore the extension
            }

            // Parse the extension.
            SHSupportedVersionsSpec spec =
                    new SHSupportedVersionsSpec(chc, buffer);

            // Update the context.
            chc.handshakeExtensions.put(SH_SUPPORTED_VERSIONS, spec);

            // No impact on session resumption.
            //
            // Note that the protocol version negotiation happens before the
            // session resumption negotiation.  And the session resumption
            // negotiation depends on the negotiated protocol version.
        }
    }

    /**
     * Network data producer of a "supported_versions" extension in
     * HelloRetryRequest.
     */
    private static final
            class HRRSupportedVersionsProducer implements HandshakeProducer {

        // Prevent instantiation of this class.
        private HRRSupportedVersionsProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(HRR_SUPPORTED_VERSIONS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " +
                        HRR_SUPPORTED_VERSIONS.name);
                }
                return null;
            }

            // Produce the extension.
            byte[] extData = new byte[2];
            extData[0] = shc.negotiatedProtocol.major;
            extData[1] = shc.negotiatedProtocol.minor;

            // Update the context.
            shc.handshakeExtensions.put(HRR_SUPPORTED_VERSIONS,
                    new SHSupportedVersionsSpec(shc.negotiatedProtocol));

            return extData;
        }
    }

    /**
     * Network data consumer of a "supported_versions" extension in
     * HelloRetryRequest.
     */
    private static final
            class HRRSupportedVersionsConsumer implements ExtensionConsumer {

        // Prevent instantiation of this class.
        private HRRSupportedVersionsConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {

            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(HRR_SUPPORTED_VERSIONS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " +
                        HRR_SUPPORTED_VERSIONS.name);
                }
                return;     // ignore the extension
            }

            // Parse the extension.
            SHSupportedVersionsSpec spec =
                    new SHSupportedVersionsSpec(chc, buffer);

            // Update the context.
            chc.handshakeExtensions.put(HRR_SUPPORTED_VERSIONS, spec);

            // No impact on session resumption.
            //
            // Note that the protocol version negotiation happens before the
            // session resumption negotiation.  And the session resumption
            // negotiation depends on the negotiated protocol version.
        }
    }

    /**
     * Network data producer of a "supported_versions" extension for stateless
     * HelloRetryRequest reconstruction.
     */
    private static final
            class HRRSupportedVersionsReproducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private HRRSupportedVersionsReproducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(HRR_SUPPORTED_VERSIONS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "[Reproduce] Ignore unavailable extension: " +
                        HRR_SUPPORTED_VERSIONS.name);
                }
                return null;
            }

            // Produce the extension.
            byte[] extData = new byte[2];
            extData[0] = shc.negotiatedProtocol.major;
            extData[1] = shc.negotiatedProtocol.minor;

            return extData;
        }
    }
}
