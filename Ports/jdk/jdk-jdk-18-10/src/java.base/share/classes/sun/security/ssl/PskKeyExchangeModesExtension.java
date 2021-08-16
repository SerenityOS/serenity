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
import java.util.*;
import javax.net.ssl.SSLProtocolException;
import sun.security.ssl.SSLExtension.ExtensionConsumer;

import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the "psk_key_exchange_modes" extensions.
 */
final class PskKeyExchangeModesExtension {
    static final HandshakeProducer chNetworkProducer =
            new PskKeyExchangeModesProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new PskKeyExchangeModesConsumer();
    static final HandshakeAbsence chOnLoadAbsence =
            new PskKeyExchangeModesOnLoadAbsence();
    static final HandshakeAbsence chOnTradeAbsence =
            new PskKeyExchangeModesOnTradeAbsence();

    static final SSLStringizer pkemStringizer =
            new PskKeyExchangeModesStringizer();

    enum PskKeyExchangeMode {
        PSK_KE          ((byte)0, "psk_ke"),
        PSK_DHE_KE      ((byte)1, "psk_dhe_ke");

        final byte id;
        final String name;

        PskKeyExchangeMode(byte id, String name) {
            this.id = id;
            this.name = name;
        }

        static PskKeyExchangeMode valueOf(byte id) {
            for(PskKeyExchangeMode pkem : values()) {
                if (pkem.id == id) {
                    return pkem;
                }
            }

            return null;
        }

        static String nameOf(byte id) {
            for (PskKeyExchangeMode pkem : PskKeyExchangeMode.values()) {
                if (pkem.id == id) {
                    return pkem.name;
                }
            }

            return "<UNKNOWN PskKeyExchangeMode TYPE: " + (id & 0x0FF) + ">";
        }
    }

    static final
            class PskKeyExchangeModesSpec implements SSLExtensionSpec {
        private static final PskKeyExchangeModesSpec DEFAULT =
                new PskKeyExchangeModesSpec(new byte[] {
                        PskKeyExchangeMode.PSK_DHE_KE.id});

        final byte[] modes;

        PskKeyExchangeModesSpec(byte[] modes) {
            this.modes = modes;
        }

        PskKeyExchangeModesSpec(HandshakeContext hc,
                ByteBuffer m) throws IOException {
            if (m.remaining() < 2) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid psk_key_exchange_modes extension: " +
                    "insufficient data"));
            }

            this.modes = Record.getBytes8(m);
        }

        boolean contains(PskKeyExchangeMode mode) {
            if (modes != null) {
                for (byte m : modes) {
                    if (mode.id == m) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"ke_modes\": '['{0}']'", Locale.ENGLISH);
            if (modes == null || modes.length ==  0) {
                Object[] messageFields = {
                        "<no PSK key exchange modes specified>"
                    };
                return messageFormat.format(messageFields);
            } else {
                StringBuilder builder = new StringBuilder(64);
                boolean isFirst = true;
                for (byte mode : modes) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        builder.append(", ");
                    }

                    builder.append(PskKeyExchangeMode.nameOf(mode));
                }

                Object[] messageFields = {
                        builder.toString()
                    };

                return messageFormat.format(messageFields);
            }
        }
    }

    private static final
            class PskKeyExchangeModesStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new PskKeyExchangeModesSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data consumer of a "psk_key_exchange_modes" extension in
     * the ClientHello handshake message.
     */
    private static final
            class PskKeyExchangeModesConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private PskKeyExchangeModesConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {

            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!shc.sslConfig.isAvailable(
                    SSLExtension.PSK_KEY_EXCHANGE_MODES)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable psk_key_exchange_modes extension");
                }

                // No session resumption is allowed.
                if (shc.isResumption && shc.resumingSession != null) {
                    shc.isResumption = false;
                    shc.resumingSession = null;
                }

                return;     // ignore the extension
            }

            // Parse the extension.
            PskKeyExchangeModesSpec spec =
                    new PskKeyExchangeModesSpec(shc, buffer);

            // Update the context.
            shc.handshakeExtensions.put(
                    SSLExtension.PSK_KEY_EXCHANGE_MODES, spec);

            // Impact on session resumption.
            //
            // Do the requested modes support session resumption?
            if (shc.isResumption) {     // resumingSession may not be set
                // Note: psk_dhe_ke is the only supported mode now.  If the
                // psk_ke mode is supported in the future, may need an update
                // here.
                if (!spec.contains(PskKeyExchangeMode.PSK_DHE_KE)) {
                    shc.isResumption = false;
                    shc.resumingSession = null;
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "abort session resumption, " +
                            "no supported psk_dhe_ke PSK key exchange mode");
                    }
                }
            }
        }
    }

    /**
     * Network data producer of a "psk_key_exchange_modes" extension in the
     * ClientHello handshake message.
     */
    private static final
            class PskKeyExchangeModesProducer implements HandshakeProducer {

        // Prevent instantiation of this class.
        private PskKeyExchangeModesProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(
                    SSLExtension.PSK_KEY_EXCHANGE_MODES)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                        "Ignore unavailable psk_key_exchange_modes extension");
                }

                return null;
            }

            byte[] extData = new byte[] {0x01, 0x01};   // psk_dhe_ke

            // Update the context.
            chc.handshakeExtensions.put(
                    SSLExtension.PSK_KEY_EXCHANGE_MODES,
                    PskKeyExchangeModesSpec.DEFAULT);

            return extData;
        }
    }
    /**
     * The absence processing if a "psk_key_exchange_modes" extension is
     * not present in the ClientHello handshake message.
     */
    private static final
        class PskKeyExchangeModesOnLoadAbsence implements HandshakeAbsence {

        // Prevent instantiation of this class.
        private PskKeyExchangeModesOnLoadAbsence() {
            // blank
        }

        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // No session resumptio is allowed.
            if (shc.isResumption) {     // resumingSession may not be set
                shc.isResumption = false;
                shc.resumingSession = null;
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "abort session resumption, " +
                            "no supported psk_dhe_ke PSK key exchange mode");
                }
            }
        }
    }

    /**
     * The absence processing if a "signature_algorithms" extension is
     * not present in the ClientHello handshake message.
     */
    private static final
        class PskKeyExchangeModesOnTradeAbsence implements HandshakeAbsence {

        // Prevent instantiation of this class.
        private PskKeyExchangeModesOnTradeAbsence() {
            // blank
        }

        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // A client MUST provide a "psk_key_exchange_modes" extension if
            // it offers a "pre_shared_key" extension.  If clients offer
            // "pre_shared_key" without a "psk_key_exchange_modes" extension,
            // servers MUST abort the handshake.
            SSLExtensionSpec spec =
                shc.handshakeExtensions.get(SSLExtension.CH_PRE_SHARED_KEY);
            if (spec != null) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "pre_shared_key key extension is offered " +
                        "without a psk_key_exchange_modes extension");
            }
        }
    }
}
