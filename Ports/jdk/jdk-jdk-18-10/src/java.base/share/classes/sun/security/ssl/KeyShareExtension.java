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
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.SSLProtocolException;
import sun.security.ssl.NamedGroup.NamedGroupSpec;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.SupportedGroupsExtension.SupportedGroups;
import sun.security.util.HexDumpEncoder;

/**
 * Pack of the "key_share" extensions.
 */
final class KeyShareExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHKeyShareProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHKeyShareConsumer();
    static final HandshakeAbsence chOnTradAbsence =
            new CHKeyShareOnTradeAbsence();
    static final SSLStringizer chStringizer =
            new CHKeyShareStringizer();

    static final HandshakeProducer shNetworkProducer =
            new SHKeyShareProducer();
    static final ExtensionConsumer shOnLoadConsumer =
            new SHKeyShareConsumer();
    static final HandshakeAbsence shOnLoadAbsence =
            new SHKeyShareAbsence();
    static final SSLStringizer shStringizer =
            new SHKeyShareStringizer();

    static final HandshakeProducer hrrNetworkProducer =
            new HRRKeyShareProducer();
    static final ExtensionConsumer hrrOnLoadConsumer =
            new HRRKeyShareConsumer();
    static final HandshakeProducer hrrNetworkReproducer =
            new HRRKeyShareReproducer();
    static final SSLStringizer hrrStringizer =
            new HRRKeyShareStringizer();

    /**
     * The key share entry used in "key_share" extensions.
     */
    private static final class KeyShareEntry {
        final int namedGroupId;
        final byte[] keyExchange;

        private KeyShareEntry(int namedGroupId, byte[] keyExchange) {
            this.namedGroupId = namedGroupId;
            this.keyExchange = keyExchange;
        }

        private byte[] getEncoded() {
            byte[] buffer = new byte[keyExchange.length + 4];
                                            //  2: named group id
                                            // +2: key exchange length
            ByteBuffer m = ByteBuffer.wrap(buffer);
            try {
                Record.putInt16(m, namedGroupId);
                Record.putBytes16(m, keyExchange);
            } catch (IOException ioe) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                        "Unlikely IOException", ioe);
                }
            }

            return buffer;
        }

        private int getEncodedSize() {
            return keyExchange.length + 4;  //  2: named group id
                                            // +2: key exchange length
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\n'{'\n" +
                "  \"named group\": {0}\n" +
                "  \"key_exchange\": '{'\n" +
                "{1}\n" +
                "  '}'\n" +
                "'}',", Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                NamedGroup.nameOf(namedGroupId),
                Utilities.indent(hexEncoder.encode(keyExchange), "    ")
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "key_share" extension in a ClientHello handshake message.
     */
    static final class CHKeyShareSpec implements SSLExtensionSpec {
        final List<KeyShareEntry> clientShares;

        private CHKeyShareSpec(List<KeyShareEntry> clientShares) {
            this.clientShares = clientShares;
        }

        private CHKeyShareSpec(HandshakeContext handshakeContext,
                ByteBuffer buffer) throws IOException {
            // struct {
            //      KeyShareEntry client_shares<0..2^16-1>;
            // } KeyShareClientHello;
            if (buffer.remaining() < 2) {
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: " +
                    "insufficient data (length=" + buffer.remaining() + ")"));
            }

            int listLen = Record.getInt16(buffer);
            if (listLen != buffer.remaining()) {
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: " +
                    "incorrect list length (length=" + listLen + ")"));
            }

            List<KeyShareEntry> keyShares = new LinkedList<>();
            while (buffer.hasRemaining()) {
                int namedGroupId = Record.getInt16(buffer);
                byte[] keyExchange = Record.getBytes16(buffer);
                if (keyExchange.length == 0) {
                    throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                            new SSLProtocolException(
                        "Invalid key_share extension: empty key_exchange"));
                }

                keyShares.add(new KeyShareEntry(namedGroupId, keyExchange));
            }

            this.clientShares = Collections.unmodifiableList(keyShares);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"client_shares\": '['{0}\n']'", Locale.ENGLISH);

            StringBuilder builder = new StringBuilder(512);
            for (KeyShareEntry entry : clientShares) {
                builder.append(entry.toString());
            }

            Object[] messageFields = {
                Utilities.indent(builder.toString())
            };

            return messageFormat.format(messageFields);
        }
    }

    private static final class CHKeyShareStringizer implements SSLStringizer {
        @Override
        public String toString(
                HandshakeContext handshakeContext, ByteBuffer buffer) {
            try {
                return (new CHKeyShareSpec(handshakeContext, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of the extension in a ClientHello
     * handshake message.
     */
    private static final
            class CHKeyShareProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private CHKeyShareProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(SSLExtension.CH_KEY_SHARE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable key_share extension");
                }
                return null;
            }

            List<NamedGroup> namedGroups;
            if (chc.serverSelectedNamedGroup != null) {
                // Response to HelloRetryRequest
                namedGroups = List.of(chc.serverSelectedNamedGroup);
            } else {
                namedGroups = chc.clientRequestedNamedGroups;
                if (namedGroups == null || namedGroups.isEmpty()) {
                    // No supported groups.
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                            "Ignore key_share extension, no supported groups");
                    }
                    return null;
                }
            }

            // Go through the named groups and take the most-preferred
            // group from two categories (i.e. XDH and ECDHE).  Once we have
            // the most preferred group from two types we can exit the loop.
            List<KeyShareEntry> keyShares = new LinkedList<>();
            EnumSet<NamedGroupSpec> ngTypes =
                    EnumSet.noneOf(NamedGroupSpec.class);
            byte[] keyExchangeData;
            for (NamedGroup ng : namedGroups) {
                if (!ngTypes.contains(ng.spec)) {
                    if ((keyExchangeData = getShare(chc, ng)) != null) {
                        keyShares.add(new KeyShareEntry(ng.id,
                                keyExchangeData));
                        ngTypes.add(ng.spec);
                        if (ngTypes.size() == 2) {
                            break;
                        }
                    }
                }
            }

            int listLen = 0;
            for (KeyShareEntry entry : keyShares) {
                listLen += entry.getEncodedSize();
            }
            byte[] extData = new byte[listLen + 2];     //  2: list length
            ByteBuffer m = ByteBuffer.wrap(extData);
            Record.putInt16(m, listLen);
            for (KeyShareEntry entry : keyShares) {
                m.put(entry.getEncoded());
            }

            // update the context
            chc.handshakeExtensions.put(SSLExtension.CH_KEY_SHARE,
                    new CHKeyShareSpec(keyShares));

            return extData;
        }

        private static byte[] getShare(ClientHandshakeContext chc,
                NamedGroup ng) {
            SSLKeyExchange ke = SSLKeyExchange.valueOf(ng);
            if (ke == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                        "No key exchange for named group " + ng.name);
                }
            } else {
                SSLPossession[] poses = ke.createPossessions(chc);
                for (SSLPossession pos : poses) {
                    // update the context
                    chc.handshakePossessions.add(pos);
                    // May need more possesion types in the future.
                    if (pos instanceof NamedGroupPossession) {
                        return pos.encode();
                    }
                }
            }
            return null;
        }
    }

    /**
     * Network data consumer of the extension in a ClientHello
     * handshake message.
     */
    private static final class CHKeyShareConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private CHKeyShareConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (shc.handshakeExtensions.containsKey(SSLExtension.CH_KEY_SHARE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "The key_share extension has been loaded");
                }
                return;
            }

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(SSLExtension.CH_KEY_SHARE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable key_share extension");
                }
                return;     // ignore the extension
            }

            // Parse the extension
            CHKeyShareSpec spec = new CHKeyShareSpec(shc, buffer);
            List<SSLCredentials> credentials = new LinkedList<>();
            for (KeyShareEntry entry : spec.clientShares) {
                NamedGroup ng = NamedGroup.valueOf(entry.namedGroupId);
                if (ng == null || !SupportedGroups.isActivatable(
                        shc.algorithmConstraints, ng)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "Ignore unsupported named group: " +
                                NamedGroup.nameOf(entry.namedGroupId));
                    }
                    continue;
                }

                try {
                    SSLCredentials kaCred =
                        ng.decodeCredentials(entry.keyExchange,
                        shc.algorithmConstraints,
                        s -> SSLLogger.warning(s));
                    if (kaCred != null) {
                        credentials.add(kaCred);
                    }
                } catch (GeneralSecurityException ex) {
                    SSLLogger.warning(
                        "Cannot decode named group: " +
                        NamedGroup.nameOf(entry.namedGroupId));
                }
            }

            if (!credentials.isEmpty()) {
                shc.handshakeCredentials.addAll(credentials);
            } else {
                // New handshake credentials are required from the client side.
                shc.handshakeProducers.put(
                        SSLHandshake.HELLO_RETRY_REQUEST.id,
                        SSLHandshake.HELLO_RETRY_REQUEST);
            }

            // update the context
            shc.handshakeExtensions.put(SSLExtension.CH_KEY_SHARE, spec);
        }
    }

    /**
     * The absence processing if the extension is not present in
     * a ClientHello handshake message.
     */
    private static final class CHKeyShareOnTradeAbsence
            implements HandshakeAbsence {
        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // A client is considered to be attempting to negotiate using this
            // specification if the ClientHello contains a "supported_versions"
            // extension with 0x0304 contained in its body.  Such a ClientHello
            // message MUST meet the following requirements:
            //    -  If containing a "supported_groups" extension, it MUST also
            //       contain a "key_share" extension, and vice versa.  An empty
            //       KeyShare.client_shares vector is permitted.
            if (shc.negotiatedProtocol.useTLS13PlusSpec() &&
                    shc.handshakeExtensions.containsKey(
                            SSLExtension.CH_SUPPORTED_GROUPS)) {
                throw shc.conContext.fatal(Alert.MISSING_EXTENSION,
                        "No key_share extension to work with " +
                        "the supported_groups extension");
            }
        }
    }


    /**
     * The key share entry used in ServerHello "key_share" extensions.
     */
    static final class SHKeyShareSpec implements SSLExtensionSpec {
        final KeyShareEntry serverShare;

        SHKeyShareSpec(KeyShareEntry serverShare) {
            this.serverShare = serverShare;
        }

        private SHKeyShareSpec(HandshakeContext handshakeContext,
                ByteBuffer buffer) throws IOException {
            // struct {
            //      KeyShareEntry server_share;
            // } KeyShareServerHello;
            if (buffer.remaining() < 5) {       // 5: minimal server_share
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: " +
                    "insufficient data (length=" + buffer.remaining() + ")"));
            }

            int namedGroupId = Record.getInt16(buffer);
            byte[] keyExchange = Record.getBytes16(buffer);

            if (buffer.hasRemaining()) {
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: unknown extra data"));
            }

            this.serverShare = new KeyShareEntry(namedGroupId, keyExchange);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"server_share\": '{'\n" +
                "  \"named group\": {0}\n" +
                "  \"key_exchange\": '{'\n" +
                "{1}\n" +
                "  '}'\n" +
                "'}',", Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                NamedGroup.nameOf(serverShare.namedGroupId),
                Utilities.indent(
                        hexEncoder.encode(serverShare.keyExchange), "    ")
            };

            return messageFormat.format(messageFields);
        }
    }

    private static final class SHKeyShareStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext handshakeContext,
                ByteBuffer buffer) {
            try {
                return (new SHKeyShareSpec(handshakeContext, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of the extension in a ServerHello
     * handshake message.
     */
    private static final class SHKeyShareProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private SHKeyShareProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // In response to key_share request only
            CHKeyShareSpec kss =
                    (CHKeyShareSpec)shc.handshakeExtensions.get(
                            SSLExtension.CH_KEY_SHARE);
            if (kss == null) {
                // Unlikely, no key_share extension requested.
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "Ignore, no client key_share extension");
                }
                return null;
            }

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(SSLExtension.SH_KEY_SHARE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "Ignore, no available server key_share extension");
                }
                return null;
            }

            // use requested key share entries
            if ((shc.handshakeCredentials == null) ||
                    shc.handshakeCredentials.isEmpty()) {
                // Unlikely, HelloRetryRequest should be used ealier.
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "No available client key share entries");
                }
                return null;
            }

            KeyShareEntry keyShare = null;
            for (SSLCredentials cd : shc.handshakeCredentials) {
                NamedGroup ng = null;
                if (cd instanceof NamedGroupCredentials) {
                    NamedGroupCredentials creds = (NamedGroupCredentials)cd;
                    ng = creds.getNamedGroup();
                }

                if (ng == null) {
                    continue;
                }

                SSLKeyExchange ke = SSLKeyExchange.valueOf(ng);
                if (ke == null) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                            "No key exchange for named group " + ng.name);
                    }
                    continue;
                }

                SSLPossession[] poses = ke.createPossessions(shc);
                for (SSLPossession pos : poses) {
                    if (!(pos instanceof NamedGroupPossession)) {
                        // May need more possesion types in the future.
                        continue;
                    }

                    // update the context
                    shc.handshakeKeyExchange = ke;
                    shc.handshakePossessions.add(pos);
                    keyShare = new KeyShareEntry(ng.id, pos.encode());
                    break;
                }

                if (keyShare != null) {
                    for (Map.Entry<Byte, HandshakeProducer> me :
                            ke.getHandshakeProducers(shc)) {
                        shc.handshakeProducers.put(
                                me.getKey(), me.getValue());
                    }

                    // We have got one! Don't forget to break.
                    break;
                }
            }

            if (keyShare == null) {
                // Unlikely, HelloRetryRequest should be used instead ealier.
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "No available server key_share extension");
                }
                return null;
            }

            byte[] extData = keyShare.getEncoded();

            // update the context
            SHKeyShareSpec spec = new SHKeyShareSpec(keyShare);
            shc.handshakeExtensions.put(SSLExtension.SH_KEY_SHARE, spec);

            return extData;
        }
    }

    /**
     * Network data consumer of the extension in a ServerHello
     * handshake message.
     */
    private static final class SHKeyShareConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private SHKeyShareConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // Happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            if (chc.clientRequestedNamedGroups == null ||
                    chc.clientRequestedNamedGroups.isEmpty()) {
                // No supported groups.
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected key_share extension in ServerHello");
            }

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(SSLExtension.SH_KEY_SHARE)) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported key_share extension in ServerHello");
            }

            // Parse the extension
            SHKeyShareSpec spec = new SHKeyShareSpec(chc, buffer);
            KeyShareEntry keyShare = spec.serverShare;
            NamedGroup ng = NamedGroup.valueOf(keyShare.namedGroupId);
            if (ng == null || !SupportedGroups.isActivatable(
                    chc.algorithmConstraints, ng)) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported named group: " +
                        NamedGroup.nameOf(keyShare.namedGroupId));
            }

            SSLKeyExchange ke = SSLKeyExchange.valueOf(ng);
            if (ke == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "No key exchange for named group " + ng.name);
            }

            SSLCredentials credentials = null;
            try {
                SSLCredentials kaCred = ng.decodeCredentials(
                    keyShare.keyExchange, chc.algorithmConstraints,
                    s -> chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE, s));
                if (kaCred != null) {
                    credentials = kaCred;
                }
            } catch (GeneralSecurityException ex) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Cannot decode named group: " +
                        NamedGroup.nameOf(keyShare.namedGroupId));
            }

            if (credentials == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported named group: " + ng.name);
            }

            // update the context
            chc.handshakeKeyExchange = ke;
            chc.handshakeCredentials.add(credentials);
            chc.handshakeExtensions.put(SSLExtension.SH_KEY_SHARE, spec);
        }
    }

    /**
     * The absence processing if the extension is not present in
     * the ServerHello handshake message.
     */
    private static final class SHKeyShareAbsence implements HandshakeAbsence {
        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Cannot use the previous requested key shares any more.
            if (SSLLogger.isOn && SSLLogger.isOn("handshake")) {
                SSLLogger.fine(
                        "No key_share extension in ServerHello, " +
                        "cleanup the key shares if necessary");
            }
            chc.handshakePossessions.clear();
        }
    }

    /**
     * The key share entry used in HelloRetryRequest "key_share" extensions.
     */
    static final class HRRKeyShareSpec implements SSLExtensionSpec {
        final int selectedGroup;

        HRRKeyShareSpec(NamedGroup serverGroup) {
            this.selectedGroup = serverGroup.id;
        }

        private HRRKeyShareSpec(HandshakeContext handshakeContext,
                ByteBuffer buffer) throws IOException {
            // struct {
            //     NamedGroup selected_group;
            // } KeyShareHelloRetryRequest;
            if (buffer.remaining() != 2) {
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: " +
                    "improper data (length=" + buffer.remaining() + ")"));
            }

            this.selectedGroup = Record.getInt16(buffer);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"selected group\": '['{0}']'", Locale.ENGLISH);

            Object[] messageFields = {
                    NamedGroup.nameOf(selectedGroup)
                };
            return messageFormat.format(messageFields);
        }
    }

    private static final class HRRKeyShareStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext handshakeContext,
                ByteBuffer buffer) {
            try {
                return (new HRRKeyShareSpec(handshakeContext, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of the extension in a HelloRetryRequest
     * handshake message.
     */
    private static final
            class HRRKeyShareProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private HRRKeyShareProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext) context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(SSLExtension.HRR_KEY_SHARE)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported key_share extension in HelloRetryRequest");
            }

            if (shc.clientRequestedNamedGroups == null ||
                    shc.clientRequestedNamedGroups.isEmpty()) {
                // No supported groups.
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected key_share extension in HelloRetryRequest");
            }

            NamedGroup selectedGroup = null;
            for (NamedGroup ng : shc.clientRequestedNamedGroups) {
                if (SupportedGroups.isActivatable(
                        shc.algorithmConstraints, ng)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "HelloRetryRequest selected named group: " +
                                ng.name);
                    }

                    selectedGroup = ng;
                    break;
                }
            }

            if (selectedGroup == null) {
                throw shc.conContext.fatal(
                        Alert.UNEXPECTED_MESSAGE, "No common named group");
            }

            byte[] extdata = new byte[] {
                    (byte)((selectedGroup.id >> 8) & 0xFF),
                    (byte)(selectedGroup.id & 0xFF)
                };

            // update the context
            shc.serverSelectedNamedGroup = selectedGroup;
            shc.handshakeExtensions.put(SSLExtension.HRR_KEY_SHARE,
                    new HRRKeyShareSpec(selectedGroup));

            return extdata;
        }
    }

    /**
     * Network data producer of the extension for stateless
     * HelloRetryRequest reconstruction.
     */
    private static final
            class HRRKeyShareReproducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private HRRKeyShareReproducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext) context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(SSLExtension.HRR_KEY_SHARE)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported key_share extension in HelloRetryRequest");
            }

            CHKeyShareSpec spec = (CHKeyShareSpec)shc.handshakeExtensions.get(
                    SSLExtension.CH_KEY_SHARE);
            if (spec != null && spec.clientShares != null &&
                    spec.clientShares.size() == 1) {
                int namedGroupId = spec.clientShares.get(0).namedGroupId;

                return new byte[] {
                        (byte)((namedGroupId >> 8) & 0xFF),
                        (byte)(namedGroupId & 0xFF)
                    };
            }

            return null;
        }
    }

    /**
     * Network data consumer of the extension in a HelloRetryRequest
     * handshake message.
     */
    private static final
            class HRRKeyShareConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private HRRKeyShareConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(SSLExtension.HRR_KEY_SHARE)) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported key_share extension in HelloRetryRequest");
            }

            if (chc.clientRequestedNamedGroups == null ||
                    chc.clientRequestedNamedGroups.isEmpty()) {
                // No supported groups.
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected key_share extension in HelloRetryRequest");
            }

            // Parse the extension
            HRRKeyShareSpec spec = new HRRKeyShareSpec(chc, buffer);
            NamedGroup serverGroup = NamedGroup.valueOf(spec.selectedGroup);
            if (serverGroup == null) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Unsupported HelloRetryRequest selected group: " +
                                NamedGroup.nameOf(spec.selectedGroup));
            }

            // The server-selected named group from a HelloRetryRequest must
            // meet the following criteria:
            // 1. It must be one of the named groups in the supported_groups
            //    extension in the client hello.
            // 2. It cannot be one of the groups in the key_share extension
            //    from the client hello.
            if (!chc.clientRequestedNamedGroups.contains(serverGroup)) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Unexpected HelloRetryRequest selected group: " +
                                serverGroup.name);
            }
            CHKeyShareSpec chKsSpec = (CHKeyShareSpec)
                    chc.handshakeExtensions.get(SSLExtension.CH_KEY_SHARE);
            if (chKsSpec != null) {
                for (KeyShareEntry kse : chKsSpec.clientShares) {
                    if (serverGroup.id == kse.namedGroupId) {
                        throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                                "Illegal HelloRetryRequest selected group: " +
                                        serverGroup.name);
                    }
                }
            } else {
                // Something has gone very wrong if we're here.
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unable to retrieve ClientHello key_share extension " +
                                "during HRR processing");
            }

            // update the context

            // When sending the new ClientHello, the client MUST replace the
            // original "key_share" extension with one containing only a new
            // KeyShareEntry for the group indicated in the selected_group
            // field of the triggering HelloRetryRequest.
            //
            chc.serverSelectedNamedGroup = serverGroup;
            chc.handshakeExtensions.put(SSLExtension.HRR_KEY_SHARE, spec);
        }
    }
}
