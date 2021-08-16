/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.StandardConstants;
import static sun.security.ssl.SSLExtension.CH_SERVER_NAME;
import static sun.security.ssl.SSLExtension.EE_SERVER_NAME;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import static sun.security.ssl.SSLExtension.SH_SERVER_NAME;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the "server_name" extensions [RFC 4366/6066].
 */
final class ServerNameExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHServerNameProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHServerNameConsumer();
    static final SSLStringizer chStringizer =
            new CHServerNamesStringizer();

    static final HandshakeProducer shNetworkProducer =
            new SHServerNameProducer();
    static final ExtensionConsumer shOnLoadConsumer =
            new SHServerNameConsumer();
    static final SSLStringizer shStringizer =
            new SHServerNamesStringizer();

    static final HandshakeProducer eeNetworkProducer =
            new EEServerNameProducer();
    static final ExtensionConsumer eeOnLoadConsumer =
            new EEServerNameConsumer();

    /**
     * The "server_name" extension.
     *
     * See RFC 4366/6066 for the specification of the extension.
     */
    static final class CHServerNamesSpec implements SSLExtensionSpec {
        // For backward compatibility, all future data structures associated
        // with new NameTypes MUST begin with a 16-bit length field.
        static final int NAME_HEADER_LENGTH = 3;    //  1: NameType
                                                    // +2: Name length
        final List<SNIServerName> serverNames;

        /*
         * Note: For the unmodifiable collection we are creating new
         * collections as inputs to avoid potential deep nesting of
         * unmodifiable collections that can cause StackOverflowErrors
         * (see JDK-6323374).
         */
        private CHServerNamesSpec(List<SNIServerName> serverNames) {
            this.serverNames = List.copyOf(serverNames);
        }

        private CHServerNamesSpec(HandshakeContext hc,
                ByteBuffer buffer) throws IOException {
            if (buffer.remaining() < 2) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid server_name extension: insufficient data"));
            }

            int sniLen = Record.getInt16(buffer);
            if ((sniLen == 0) || sniLen != buffer.remaining()) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid server_name extension: incomplete data"));
            }

            Map<Integer, SNIServerName> sniMap = new LinkedHashMap<>();
            while (buffer.hasRemaining()) {
                int nameType = Record.getInt8(buffer);
                SNIServerName serverName;

                // HostName (length read in getBytes16);
                //
                // [RFC 6066] The data structure associated with the host_name
                // NameType is a variable-length vector that begins with a
                // 16-bit length.  For backward compatibility, all future data
                // structures associated with new NameTypes MUST begin with a
                // 16-bit length field.  TLS MAY treat provided server names as
                // opaque data and pass the names and types to the application.
                byte[] encoded = Record.getBytes16(buffer);
                if (nameType == StandardConstants.SNI_HOST_NAME) {
                    if (encoded.length == 0) {
                        throw hc.conContext.fatal(Alert.DECODE_ERROR,
                                new SSLProtocolException(
                            "Empty HostName in server_name extension"));
                    }

                    try {
                        serverName = new SNIHostName(encoded);
                    } catch (IllegalArgumentException iae) {
                        SSLProtocolException spe = new SSLProtocolException(
                            "Illegal server name, type=host_name(" +
                            nameType + "), name=" +
                            (new String(encoded, StandardCharsets.UTF_8)) +
                            ", value={" +
                            Utilities.toHexString(encoded) + "}");
                        throw hc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                                (SSLProtocolException)spe.initCause(iae));
                    }
                } else {
                    try {
                        serverName = new UnknownServerName(nameType, encoded);
                    } catch (IllegalArgumentException iae) {
                        SSLProtocolException spe = new SSLProtocolException(
                            "Illegal server name, type=(" + nameType +
                            "), value={" +
                            Utilities.toHexString(encoded) + "}");
                        throw hc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                                (SSLProtocolException)spe.initCause(iae));
                    }
                }

                // check for duplicated server name type
                if (sniMap.put(serverName.getType(), serverName) != null) {
                        throw hc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                                new SSLProtocolException(
                            "Duplicated server name of type " +
                            serverName.getType()));
                }
            }

            this.serverNames = new ArrayList<>(sniMap.values());
        }

        @Override
        public String toString() {
            if (serverNames == null || serverNames.isEmpty()) {
                return "<no server name indicator specified>";
            } else {
                StringBuilder builder = new StringBuilder(512);
                for (SNIServerName sn : serverNames) {
                    builder.append(sn.toString());
                    builder.append("\n");
                }

                return builder.toString();
            }
        }

        private static class UnknownServerName extends SNIServerName {
            UnknownServerName(int code, byte[] encoded) {
                super(code, encoded);
            }
        }
    }

    private static final class CHServerNamesStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new CHServerNamesSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of a "server_name" extension in the
     * ClientHello handshake message.
     */
    private static final
            class CHServerNameProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private CHServerNameProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(CH_SERVER_NAME)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                        "Ignore unavailable server_name extension");
                }
                return null;
            }

            // Produce the extension.
            List<SNIServerName> serverNames;
            if (chc.isResumption && (chc.resumingSession != null)) {
                serverNames =
                        chc.resumingSession.getRequestedServerNames();
            } else {
                serverNames = chc.sslConfig.serverNames;
            }   // Shall we use host too?

            // Empty server name list is not allowed in client mode.
            if ((serverNames != null) && !serverNames.isEmpty()) {
                int sniLen = 0;
                for (SNIServerName sniName : serverNames) {
                    // For backward compatibility, all future data structures
                    // associated with new NameTypes MUST begin with a 16-bit
                    // length field.  The header length of server name is 3
                    // bytes, including 1 byte NameType, and 2 bytes length
                    // of the name.
                    sniLen += CHServerNamesSpec.NAME_HEADER_LENGTH;
                    sniLen += sniName.getEncoded().length;
                }

                byte[] extData = new byte[sniLen + 2];
                ByteBuffer m = ByteBuffer.wrap(extData);
                Record.putInt16(m, sniLen);
                for (SNIServerName sniName : serverNames) {
                    Record.putInt8(m, sniName.getType());
                    Record.putBytes16(m, sniName.getEncoded());
                }

                // Update the context.
                chc.requestedServerNames = serverNames;
                chc.handshakeExtensions.put(CH_SERVER_NAME,
                        new CHServerNamesSpec(serverNames));

                return extData;
            }

            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.warning("Unable to indicate server name");
            }
            return null;
        }
    }

    /**
     * Network data consumer of a "server_name" extension in the
     * ClientHello handshake message.
     */
    private static final
            class CHServerNameConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private CHServerNameConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(CH_SERVER_NAME)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " + CH_SERVER_NAME.name);
                }
                return;     // ignore the extension
            }

            // Parse the extension.
            CHServerNamesSpec spec = new CHServerNamesSpec(shc, buffer);

            // Update the context.
            shc.handshakeExtensions.put(CH_SERVER_NAME, spec);

            // Does the server match the server name request?
            SNIServerName sni = null;
            if (!shc.sslConfig.sniMatchers.isEmpty()) {
                sni = chooseSni(shc.sslConfig.sniMatchers, spec.serverNames);
                if (sni != null) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "server name indication (" +
                                sni + ") is accepted");
                    }
                } else {
                    // We do not reject client without SNI extension currently.
                    throw shc.conContext.fatal(Alert.UNRECOGNIZED_NAME,
                            "Unrecognized server name indication");
                }
            } else {
                // Note: Servers MAY require clients to send a valid
                // "server_name" extension and respond to a ClientHello
                // lacking a "server_name" extension by terminating the
                // connection with a "missing_extension" alert.
                //
                // We do not reject client without SNI extension currently.
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "no server name matchers, " +
                            "ignore server name indication");
                }
            }

            // Impact on session resumption.
            //
            // Does the resuming session have the same principal?
            if (shc.isResumption && shc.resumingSession != null) {
                // A server that implements this extension MUST NOT accept
                // the request to resume the session if the server_name
                // extension contains a different name.
                //
                // May only need to check that the session SNI is one of
                // the requested server names.
                if (!Objects.equals(
                        sni, shc.resumingSession.serverNameIndication)) {
                    shc.isResumption = false;
                    shc.resumingSession = null;
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "abort session resumption, " +
                                "different server name indication used");
                    }
                }
            }

            shc.requestedServerNames = spec.serverNames;
            shc.negotiatedServerName = sni;
        }

        private static SNIServerName chooseSni(Collection<SNIMatcher> matchers,
                List<SNIServerName> sniNames) {
            if (sniNames != null && !sniNames.isEmpty()) {
                for (SNIMatcher matcher : matchers) {
                    int matcherType = matcher.getType();
                    for (SNIServerName sniName : sniNames) {
                        if (sniName.getType() == matcherType) {
                            if (matcher.matches(sniName)) {
                                return sniName;
                            }

                            // no duplicated entry in the server names list.
                            break;
                        }
                    }
                }
            }

            return null;
        }
    }

    /**
     * The "server_name" extension in the ServerHello handshake message.
     *
     * The "extension_data" field of this extension shall be empty.
     */
    static final class SHServerNamesSpec implements SSLExtensionSpec {
        static final SHServerNamesSpec DEFAULT = new SHServerNamesSpec();

        private SHServerNamesSpec() {
            // blank
        }

        private SHServerNamesSpec(HandshakeContext hc,
                ByteBuffer buffer) throws IOException {
            if (buffer.remaining() != 0) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid ServerHello server_name extension: not empty"));
            }
        }

        @Override
        public String toString() {
            return "<empty extension_data field>";
        }
    }

    private static final class SHServerNamesStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new SHServerNamesSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of a "server_name" extension in the
     * ServerHello handshake message.
     */
    private static final
            class SHServerNameProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private SHServerNameProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // In response to "server_name" extension request only
            CHServerNamesSpec spec = (CHServerNamesSpec)
                    shc.handshakeExtensions.get(CH_SERVER_NAME);
            if (spec == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "Ignore unavailable extension: " + SH_SERVER_NAME.name);
                }
                return null;        // ignore the extension
            }

            // When resuming a session, the server MUST NOT include a
            // server_name extension in the server hello.
            if (shc.isResumption || shc.negotiatedServerName == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "No expected server name indication response");
                }
                return null;        // ignore the extension
            }

            // Produce the extension and update the context.
            shc.handshakeExtensions.put(
                    SH_SERVER_NAME, SHServerNamesSpec.DEFAULT);

            return (new byte[0]);   // the empty extension_data
        }
    }

    /**
     * Network data consumer of a "server_name" extension in the
     * ServerHello handshake message.
     */
    private static final
            class SHServerNameConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private SHServerNameConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // In response to "server_name" extension request only
            CHServerNamesSpec spec = (CHServerNamesSpec)
                    chc.handshakeExtensions.get(CH_SERVER_NAME);
            if (spec == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                    "Unexpected ServerHello server_name extension");
            }

            // Parse the extension.
            if (buffer.remaining() != 0) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                    "Invalid ServerHello server_name extension");
            }

            // Update the context.
            chc.handshakeExtensions.put(
                    SH_SERVER_NAME, SHServerNamesSpec.DEFAULT);
            // The negotiated server name is unknown in client side. Just
            // use the first request name as the value is not actually used
            // in the current implementation.
            chc.negotiatedServerName = spec.serverNames.get(0);
        }
    }

    /**
     * Network data producer of a "server_name" extension in the
     * EncryptedExtensions handshake message.
     */
    private static final
            class EEServerNameProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private EEServerNameProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // In response to "server_name" extension request only
            CHServerNamesSpec spec = (CHServerNamesSpec)
                    shc.handshakeExtensions.get(CH_SERVER_NAME);
            if (spec == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "Ignore unavailable extension: " + EE_SERVER_NAME.name);
                }
                return null;        // ignore the extension
            }

            // When resuming a session, the server MUST NOT include a
            // server_name extension in the server hello.
            if (shc.isResumption || shc.negotiatedServerName == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "No expected server name indication response");
                }
                return null;        // ignore the extension
            }

            // Produce the extension and update the context.
            shc.handshakeExtensions.put(
                    EE_SERVER_NAME, SHServerNamesSpec.DEFAULT);

            return (new byte[0]);   // the empty extension_data
        }
    }

    /**
     * Network data consumer of a "server_name" extension in the
     * EncryptedExtensions handshake message.
     */
    private static final
            class EEServerNameConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private EEServerNameConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // In response to "server_name" extension request only
            CHServerNamesSpec spec = (CHServerNamesSpec)
                    chc.handshakeExtensions.get(CH_SERVER_NAME);
            if (spec == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                    "Unexpected EncryptedExtensions server_name extension");
            }

            // Parse the extension.
            if (buffer.remaining() != 0) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                    "Invalid EncryptedExtensions server_name extension");
            }

            // Update the context.
            chc.handshakeExtensions.put(
                    EE_SERVER_NAME, SHServerNamesSpec.DEFAULT);
            // The negotiated server name is unknown in client side. Just
            // use the first request name as the value is not actually used
            // in the current implementation.
            chc.negotiatedServerName = spec.serverNames.get(0);
        }
    }
}
