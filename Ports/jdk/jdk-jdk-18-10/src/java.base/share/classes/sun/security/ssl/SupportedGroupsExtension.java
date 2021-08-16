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
import java.security.AlgorithmConstraints;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLProtocolException;
import sun.security.action.GetPropertyAction;
import sun.security.ssl.NamedGroup.NamedGroupSpec;
import static sun.security.ssl.SSLExtension.CH_SUPPORTED_GROUPS;
import static sun.security.ssl.SSLExtension.EE_SUPPORTED_GROUPS;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;


/**
 * Pack of the "supported_groups" extensions [RFC 4492/7919].
 */
final class SupportedGroupsExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHSupportedGroupsProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHSupportedGroupsConsumer();
    static final HandshakeAbsence chOnTradAbsence =
            new CHSupportedGroupsOnTradeAbsence();
    static final SSLStringizer sgsStringizer =
            new SupportedGroupsStringizer();

    static final HandshakeProducer eeNetworkProducer =
            new EESupportedGroupsProducer();
    static final ExtensionConsumer eeOnLoadConsumer =
            new EESupportedGroupsConsumer();

    /**
     * The "supported_groups" extension.
     */
    static final class SupportedGroupsSpec implements SSLExtensionSpec {
        final int[] namedGroupsIds;

        private SupportedGroupsSpec(int[] namedGroupsIds) {
            this.namedGroupsIds = namedGroupsIds;
        }

        private SupportedGroupsSpec(List<NamedGroup> namedGroups) {
            this.namedGroupsIds = new int[namedGroups.size()];
            int i = 0;
            for (NamedGroup ng : namedGroups) {
                namedGroupsIds[i++] = ng.id;
            }
        }

        private SupportedGroupsSpec(HandshakeContext hc,
                ByteBuffer m) throws IOException  {
            if (m.remaining() < 2) {      // 2: the length of the list
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid supported_groups extension: insufficient data"));
            }

            byte[] ngs = Record.getBytes16(m);
            if (m.hasRemaining()) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid supported_groups extension: unknown extra data"));
            }

            if ((ngs == null) || (ngs.length == 0) || (ngs.length % 2 != 0)) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid supported_groups extension: incomplete data"));
            }

            int[] ids = new int[ngs.length / 2];
            for (int i = 0, j = 0; i < ngs.length;) {
                ids[j++] = ((ngs[i++] & 0xFF) << 8) | (ngs[i++] & 0xFF);
            }

            this.namedGroupsIds = ids;
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"versions\": '['{0}']'", Locale.ENGLISH);

            if (namedGroupsIds == null || namedGroupsIds.length == 0) {
                Object[] messageFields = {
                        "<no supported named group specified>"
                    };
                return messageFormat.format(messageFields);
            } else {
                StringBuilder builder = new StringBuilder(512);
                boolean isFirst = true;
                for (int ngid : namedGroupsIds) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        builder.append(", ");
                    }

                    builder.append(NamedGroup.nameOf(ngid));
                }

                Object[] messageFields = {
                        builder.toString()
                    };

                return messageFormat.format(messageFields);
            }
        }
    }

    private static final
            class SupportedGroupsStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new SupportedGroupsSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    static class SupportedGroups {
        // To switch off the supported_groups extension for DHE cipher suite.
        static final boolean enableFFDHE =
                Utilities.getBooleanProperty("jsse.enableFFDHE", true);

        // the supported named groups
        static final NamedGroup[] supportedNamedGroups;

        static {
            // The value of the System Property defines a list of enabled named
            // groups in preference order, separated with comma.  For example:
            //
            //      jdk.tls.namedGroups="secp521r1, secp256r1, ffdhe2048"
            //
            // If the System Property is not defined or the value is empty, the
            // default groups and preferences will be used.
            String property = GetPropertyAction
                    .privilegedGetProperty("jdk.tls.namedGroups");
            if (property != null && !property.isEmpty()) {
                // remove double quote marks from beginning/end of the property
                if (property.length() > 1 && property.charAt(0) == '"' &&
                        property.charAt(property.length() - 1) == '"') {
                    property = property.substring(1, property.length() - 1);
                }
            }

            ArrayList<NamedGroup> groupList;
            if (property != null && !property.isEmpty()) {
                String[] groups = property.split(",");
                groupList = new ArrayList<>(groups.length);
                for (String group : groups) {
                    group = group.trim();
                    if (!group.isEmpty()) {
                        NamedGroup namedGroup = NamedGroup.nameOf(group);
                        if (namedGroup != null) {
                            if (namedGroup.isAvailable) {
                                groupList.add(namedGroup);
                            }
                        }   // ignore unknown groups
                    }
                }

                if (groupList.isEmpty()) {
                    throw new IllegalArgumentException(
                            "System property jdk.tls.namedGroups(" +
                            property + ") contains no supported named groups");
                }
            } else {        // default groups
                NamedGroup[] groups = new NamedGroup[] {

                        // Primary XDH (RFC 7748) curves
                        NamedGroup.X25519,

                        // Primary NIST Suite B curves
                        NamedGroup.SECP256_R1,
                        NamedGroup.SECP384_R1,
                        NamedGroup.SECP521_R1,

                        // Secondary XDH curves
                        NamedGroup.X448,

                        // FFDHE (RFC 7919)
                        NamedGroup.FFDHE_2048,
                        NamedGroup.FFDHE_3072,
                        NamedGroup.FFDHE_4096,
                        NamedGroup.FFDHE_6144,
                        NamedGroup.FFDHE_8192,
                    };

                groupList = new ArrayList<>(groups.length);
                for (NamedGroup group : groups) {
                    if (group.isAvailable) {
                        groupList.add(group);
                    }
                }

                if (groupList.isEmpty() &&
                        SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning("No default named groups");
                }
            }

            supportedNamedGroups = new NamedGroup[groupList.size()];
            int i = 0;
            for (NamedGroup namedGroup : groupList) {
                supportedNamedGroups[i++] = namedGroup;
            }
        }

        // Is there any supported group permitted by the constraints?
        static boolean isActivatable(
                AlgorithmConstraints constraints, NamedGroupSpec type) {

            boolean hasFFDHEGroups = false;
            for (NamedGroup namedGroup : supportedNamedGroups) {
                if (namedGroup.isAvailable && namedGroup.spec == type) {
                    if (namedGroup.isPermitted(constraints)) {
                        return true;
                    }

                    if (!hasFFDHEGroups &&
                            (type == NamedGroupSpec.NAMED_GROUP_FFDHE)) {
                        hasFFDHEGroups = true;
                    }
                }
            }

            // For compatibility, if no FFDHE groups are defined, the non-FFDHE
            // compatible mode (using DHE cipher suite without FFDHE extension)
            // is allowed.
            //
            // Note that the constraints checking on DHE parameters will be
            // performed during key exchanging in a handshake.
            return !hasFFDHEGroups && type == NamedGroupSpec.NAMED_GROUP_FFDHE;
        }

        // Is the named group permitted by the constraints?
        static boolean isActivatable(
                AlgorithmConstraints constraints, NamedGroup namedGroup) {
            if (!namedGroup.isAvailable || !isSupported(namedGroup)) {
                return false;
            }

            return namedGroup.isPermitted(constraints);
        }

        // Is the named group supported?
        static boolean isSupported(NamedGroup namedGroup) {
            for (NamedGroup group : supportedNamedGroups) {
                if (namedGroup.id == group.id) {
                    return true;
                }
            }

            return false;
        }

        static NamedGroup getPreferredGroup(
                ProtocolVersion negotiatedProtocol,
                AlgorithmConstraints constraints, NamedGroupSpec[] types,
                List<NamedGroup> requestedNamedGroups) {
            for (NamedGroup namedGroup : requestedNamedGroups) {
                if ((NamedGroupSpec.arrayContains(types, namedGroup.spec)) &&
                        namedGroup.isAvailable(negotiatedProtocol) &&
                        isSupported(namedGroup) &&
                        namedGroup.isPermitted(constraints)) {
                    return namedGroup;
                }
            }

            return null;
        }

        static NamedGroup getPreferredGroup(
                ProtocolVersion negotiatedProtocol,
                AlgorithmConstraints constraints, NamedGroupSpec[] types) {
            for (NamedGroup namedGroup : supportedNamedGroups) {
                if ((NamedGroupSpec.arrayContains(types, namedGroup.spec)) &&
                        namedGroup.isAvailable(negotiatedProtocol) &&
                        namedGroup.isPermitted(constraints)) {
                    return namedGroup;
                }
            }

            return null;
        }
    }

    /**
     * Network data producer of a "supported_groups" extension in
     * the ClientHello handshake message.
     */
    private static final class CHSupportedGroupsProducer
            extends SupportedGroups implements HandshakeProducer {
        // Prevent instantiation of this class.
        private CHSupportedGroupsProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(CH_SUPPORTED_GROUPS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable supported_groups extension");
                }
                return null;
            }

            // Produce the extension.
            ArrayList<NamedGroup> namedGroups =
                new ArrayList<>(SupportedGroups.supportedNamedGroups.length);
            for (NamedGroup ng : SupportedGroups.supportedNamedGroups) {
                if ((!SupportedGroups.enableFFDHE) &&
                    (ng.spec == NamedGroupSpec.NAMED_GROUP_FFDHE)) {
                    continue;
                }

                if (ng.isAvailable(chc.activeProtocols) &&
                        ng.isSupported(chc.activeCipherSuites) &&
                        ng.isPermitted(chc.algorithmConstraints)) {
                    namedGroups.add(ng);
                } else if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore inactive or disabled named group: " + ng.name);
                }
            }

            if (namedGroups.isEmpty()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning("no available named group");
                }

                return null;
            }

            int vectorLen = namedGroups.size() << 1;
            byte[] extData = new byte[vectorLen + 2];
            ByteBuffer m = ByteBuffer.wrap(extData);
            Record.putInt16(m, vectorLen);
            for (NamedGroup namedGroup : namedGroups) {
                    Record.putInt16(m, namedGroup.id);
            }

            // Update the context.
            chc.clientRequestedNamedGroups =
                    Collections.<NamedGroup>unmodifiableList(namedGroups);
            chc.handshakeExtensions.put(CH_SUPPORTED_GROUPS,
                    new SupportedGroupsSpec(namedGroups));

            return extData;
        }
    }

    /**
     * Network data producer of a "supported_groups" extension in
     * the ClientHello handshake message.
     */
    private static final
            class CHSupportedGroupsConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private CHSupportedGroupsConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(CH_SUPPORTED_GROUPS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable supported_groups extension");
                }
                return;     // ignore the extension
            }

            // Parse the extension.
            SupportedGroupsSpec spec = new SupportedGroupsSpec(shc, buffer);

            // Update the context.
            List<NamedGroup> knownNamedGroups = new LinkedList<>();
            for (int id : spec.namedGroupsIds) {
                NamedGroup ng = NamedGroup.valueOf(id);
                if (ng != null) {
                    knownNamedGroups.add(ng);
                }
            }

            shc.clientRequestedNamedGroups = knownNamedGroups;
            shc.handshakeExtensions.put(CH_SUPPORTED_GROUPS, spec);

            // No impact on session resumption.
        }
    }

    /**
     * The absence processing if the extension is not present in
     * a ClientHello handshake message.
     */
    private static final class CHSupportedGroupsOnTradeAbsence
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
                            SSLExtension.CH_KEY_SHARE)) {
                throw shc.conContext.fatal(Alert.MISSING_EXTENSION,
                        "No supported_groups extension to work with " +
                        "the key_share extension");
            }
        }
    }

    /**
     * Network data producer of a "supported_groups" extension in
     * the EncryptedExtensions handshake message.
     */
    private static final class EESupportedGroupsProducer
            extends SupportedGroups implements HandshakeProducer {

        // Prevent instantiation of this class.
        private EESupportedGroupsProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(EE_SUPPORTED_GROUPS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable supported_groups extension");
                }
                return null;
            }

            // Produce the extension.
            //
            // Contains all groups the server supports, regardless of whether
            // they are currently supported by the client.
            ArrayList<NamedGroup> namedGroups = new ArrayList<>(
                    SupportedGroups.supportedNamedGroups.length);
            for (NamedGroup ng : SupportedGroups.supportedNamedGroups) {
                if ((!SupportedGroups.enableFFDHE) &&
                    (ng.spec == NamedGroupSpec.NAMED_GROUP_FFDHE)) {
                    continue;
                }

                if (ng.isAvailable(shc.activeProtocols) &&
                        ng.isSupported(shc.activeCipherSuites) &&
                        ng.isPermitted(shc.algorithmConstraints)) {
                    namedGroups.add(ng);
                } else if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore inactive or disabled named group: " + ng.name);
                }
            }

            if (namedGroups.isEmpty()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning("no available named group");
                }

                return null;
            }

            int vectorLen = namedGroups.size() << 1;
            byte[] extData = new byte[vectorLen + 2];
            ByteBuffer m = ByteBuffer.wrap(extData);
            Record.putInt16(m, vectorLen);
            for (NamedGroup namedGroup : namedGroups) {
                    Record.putInt16(m, namedGroup.id);
            }

            // Update the context.
            shc.conContext.serverRequestedNamedGroups =
                    Collections.<NamedGroup>unmodifiableList(namedGroups);
            SupportedGroupsSpec spec = new SupportedGroupsSpec(namedGroups);
            shc.handshakeExtensions.put(EE_SUPPORTED_GROUPS, spec);

            return extData;
        }
    }

    private static final
            class EESupportedGroupsConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private EESupportedGroupsConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(EE_SUPPORTED_GROUPS)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable supported_groups extension");
                }
                return;     // ignore the extension
            }

            // Parse the extension.
            SupportedGroupsSpec spec = new SupportedGroupsSpec(chc, buffer);

            // Update the context.
            List<NamedGroup> knownNamedGroups =
                    new ArrayList<>(spec.namedGroupsIds.length);
            for (int id : spec.namedGroupsIds) {
                NamedGroup ng = NamedGroup.valueOf(id);
                if (ng != null) {
                    knownNamedGroups.add(ng);
                }
            }

            chc.conContext.serverRequestedNamedGroups = knownNamedGroups;
            chc.handshakeExtensions.put(EE_SUPPORTED_GROUPS, spec);

            // No impact on session resumption.
        }
    }
}
