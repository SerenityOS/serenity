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
import javax.net.ssl.SSLProtocolException;
import static sun.security.ssl.SSLExtension.CH_MAX_FRAGMENT_LENGTH;
import static sun.security.ssl.SSLExtension.EE_MAX_FRAGMENT_LENGTH;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import static sun.security.ssl.SSLExtension.SH_MAX_FRAGMENT_LENGTH;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the "max_fragment_length" extensions [RFC6066].
 */
final class MaxFragExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHMaxFragmentLengthProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHMaxFragmentLengthConsumer();

    static final HandshakeProducer shNetworkProducer =
            new SHMaxFragmentLengthProducer();
    static final ExtensionConsumer shOnLoadConsumer =
            new SHMaxFragmentLengthConsumer();
    static final HandshakeConsumer shOnTradeConsumer =
            new SHMaxFragmentLengthUpdate();

    static final HandshakeProducer eeNetworkProducer =
            new EEMaxFragmentLengthProducer();
    static final ExtensionConsumer eeOnLoadConsumer =
            new EEMaxFragmentLengthConsumer();
    static final HandshakeConsumer eeOnTradeConsumer =
            new EEMaxFragmentLengthUpdate();

    static final SSLStringizer maxFragLenStringizer =
            new MaxFragLenStringizer();

    /**
     * The "max_fragment_length" extension [RFC 6066].
     */
    static final class MaxFragLenSpec implements SSLExtensionSpec {
        byte id;

        private MaxFragLenSpec(byte id) {
            this.id = id;
        }

        private MaxFragLenSpec(HandshakeContext hc,
                ByteBuffer buffer) throws IOException {
            if (buffer.remaining() != 1) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid max_fragment_length extension data"));
            }

            this.id = buffer.get();
        }

        @Override
        public String toString() {
            return MaxFragLenEnum.nameOf(id);
        }
    }

    private static final class MaxFragLenStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new MaxFragLenSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                // For debug logging only, so please swallow exceptions.
                return ioe.getMessage();
            }
        }
    }

    static enum MaxFragLenEnum {
        MFL_512     ((byte)0x01,  512,  "2^9"),
        MFL_1024    ((byte)0x02, 1024,  "2^10"),
        MFL_2048    ((byte)0x03, 2048,  "2^11"),
        MFL_4096    ((byte)0x04, 4096,  "2^12");

        final byte id;
        final int fragmentSize;
        final String description;

        private MaxFragLenEnum(byte id, int fragmentSize, String description) {
            this.id = id;
            this.fragmentSize = fragmentSize;
            this.description = description;
        }

        private static MaxFragLenEnum valueOf(byte id) {
            for (MaxFragLenEnum mfl : MaxFragLenEnum.values()) {
                if (mfl.id == id) {
                    return mfl;
                }
            }

            return null;
        }

        private static String nameOf(byte id) {
            for (MaxFragLenEnum mfl : MaxFragLenEnum.values()) {
                if (mfl.id == id) {
                    return mfl.description;
                }
            }

            return "UNDEFINED-MAX-FRAGMENT-LENGTH(" + id + ")";
        }

        /**
         * Returns the best match enum constant of the specified
         * fragment size.
         */
        static MaxFragLenEnum valueOf(int fragmentSize) {
            if (fragmentSize <= 0) {
                return null;
            } else if (fragmentSize < 1024) {
                return MFL_512;
            } else if (fragmentSize < 2048) {
                return MFL_1024;
            } else if (fragmentSize < 4096) {
                return MFL_2048;
            } else if (fragmentSize == 4096) {
                return MFL_4096;
            }

            return null;
        }
    }

    /**
     * Network data producer of a "max_fragment_length" extension in
     * the ClientHello handshake message.
     */
    private static final
            class CHMaxFragmentLengthProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private CHMaxFragmentLengthProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // Is it a supported and enabled extension?
            if (!chc.sslConfig.isAvailable(CH_MAX_FRAGMENT_LENGTH)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable max_fragment_length extension");
                }
                return null;
            }

            // Produce the extension and update the context.
            int requestedMFLength;
            if (chc.isResumption && (chc.resumingSession != null)) {
                // The same extension should be sent for resumption.
                requestedMFLength =
                    chc.resumingSession.getNegotiatedMaxFragSize();
            } else if (chc.sslConfig.maximumPacketSize != 0) {
                // Maybe we can calculate the fragment size more accurate
                // by condering the enabled cipher suites in the future.
                requestedMFLength = chc.sslConfig.maximumPacketSize;
                if (chc.sslContext.isDTLS()) {
                    requestedMFLength -= DTLSRecord.maxPlaintextPlusSize;
                } else {
                    requestedMFLength -= SSLRecord.maxPlaintextPlusSize;
                }
            } else {
                // Need no max_fragment_length extension.
                requestedMFLength = -1;
            }

            MaxFragLenEnum mfl = MaxFragLenEnum.valueOf(requestedMFLength);
            if (mfl != null) {
                // update the context.
                chc.handshakeExtensions.put(
                        CH_MAX_FRAGMENT_LENGTH, new MaxFragLenSpec(mfl.id));

                return new byte[] { mfl.id };
            } else {
                // log and ignore, no MFL extension.
                chc.maxFragmentLength = -1;
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "No available max_fragment_length extension can " +
                        "be used for fragment size of " +
                        requestedMFLength + "bytes");
                }
            }

            return null;
        }
    }

    /**
     * Network data consumer of a "max_fragment_length" extension in
     * the ClientHello handshake message.
     */
    private static final
            class CHMaxFragmentLengthConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private CHMaxFragmentLengthConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (!shc.sslConfig.isAvailable(CH_MAX_FRAGMENT_LENGTH)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable max_fragment_length extension");
                }
                return;     // ignore the extension
            }

            // Parse the extension.
            MaxFragLenSpec spec = new MaxFragLenSpec(shc, buffer);
            MaxFragLenEnum mfle = MaxFragLenEnum.valueOf(spec.id);
            if (mfle == null) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "the requested maximum fragment length is other " +
                    "than the allowed values");
            }

            // Update the context.
            shc.maxFragmentLength = mfle.fragmentSize;
            shc.handshakeExtensions.put(CH_MAX_FRAGMENT_LENGTH, spec);

            // No impact on session resumption.
        }
    }

    /**
     * Network data producer of a "max_fragment_length" extension in
     * the ServerHello handshake message.
     */
    private static final
            class SHMaxFragmentLengthProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private SHMaxFragmentLengthProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // In response to "max_fragment_length" extension request only
            MaxFragLenSpec spec = (MaxFragLenSpec)
                    shc.handshakeExtensions.get(CH_MAX_FRAGMENT_LENGTH);
            if (spec == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "Ignore unavailable max_fragment_length extension");
                }
                return null;        // ignore the extension
            }

            if ((shc.maxFragmentLength > 0) &&
                    (shc.sslConfig.maximumPacketSize != 0)) {
                int estimatedMaxFragSize =
                        shc.negotiatedCipherSuite.calculatePacketSize(
                                shc.maxFragmentLength, shc.negotiatedProtocol,
                                shc.sslContext.isDTLS());
                if (estimatedMaxFragSize > shc.sslConfig.maximumPacketSize) {
                    // For better interoperability, abort the maximum
                    // fragment length negotiation, rather than terminate
                    // the connection with a fatal alert.
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "Abort the maximum fragment length negotiation, " +
                            "may overflow the maximum packet size limit.");
                    }
                    shc.maxFragmentLength = -1;
                }
            }

            // update the context
            if (shc.maxFragmentLength > 0) {
                shc.handshakeSession.setNegotiatedMaxFragSize(
                        shc.maxFragmentLength);
                shc.conContext.inputRecord.changeFragmentSize(
                        shc.maxFragmentLength);
                shc.conContext.outputRecord.changeFragmentSize(
                        shc.maxFragmentLength);

                // The response extension data is the same as the requested one.
                shc.handshakeExtensions.put(SH_MAX_FRAGMENT_LENGTH, spec);
                return new byte[] { spec.id };
            }

            return null;
        }
    }

    /**
     * Network data consumer of a "max_fragment_length" extension in
     * the ServerHello handshake message.
     */
    private static final
            class SHMaxFragmentLengthConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private SHMaxFragmentLengthConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {

            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // In response to "max_fragment_length" extension request only
            MaxFragLenSpec requestedSpec = (MaxFragLenSpec)
                    chc.handshakeExtensions.get(CH_MAX_FRAGMENT_LENGTH);
            if (requestedSpec == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                    "Unexpected max_fragment_length extension in ServerHello");
            }

            // Parse the extension.
            MaxFragLenSpec spec = new MaxFragLenSpec(chc, buffer);
            if (spec.id != requestedSpec.id) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "The maximum fragment length response is not requested");
            }

            MaxFragLenEnum mfle = MaxFragLenEnum.valueOf(spec.id);
            if (mfle == null) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "the requested maximum fragment length is other " +
                    "than the allowed values");
            }

            // update the context
            chc.maxFragmentLength = mfle.fragmentSize;
            chc.handshakeExtensions.put(SH_MAX_FRAGMENT_LENGTH, spec);
        }
    }

    /**
     * After session creation consuming of a "max_fragment_length"
     * extension in the ClientHello handshake message.
     */
    private static final class SHMaxFragmentLengthUpdate
            implements HandshakeConsumer {

        // Prevent instantiation of this class.
        private SHMaxFragmentLengthUpdate() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            MaxFragLenSpec spec = (MaxFragLenSpec)
                    chc.handshakeExtensions.get(SH_MAX_FRAGMENT_LENGTH);
            if (spec == null) {
                // Ignore, no "max_fragment_length" extension response.
                return;
            }

            if ((chc.maxFragmentLength > 0) &&
                    (chc.sslConfig.maximumPacketSize != 0)) {
                int estimatedMaxFragSize =
                        chc.negotiatedCipherSuite.calculatePacketSize(
                                chc.maxFragmentLength, chc.negotiatedProtocol,
                                chc.sslContext.isDTLS());
                if (estimatedMaxFragSize > chc.sslConfig.maximumPacketSize) {
                    // For better interoperability, abort the maximum
                    // fragment length negotiation, rather than terminate
                    // the connection with a fatal alert.
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "Abort the maximum fragment length negotiation, " +
                            "may overflow the maximum packet size limit.");
                    }
                    chc.maxFragmentLength = -1;
                }
            }

            // update the context
            if (chc.maxFragmentLength > 0) {
                chc.handshakeSession.setNegotiatedMaxFragSize(
                        chc.maxFragmentLength);
                chc.conContext.inputRecord.changeFragmentSize(
                        chc.maxFragmentLength);
                chc.conContext.outputRecord.changeFragmentSize(
                        chc.maxFragmentLength);
            }
        }
    }

    /**
     * Network data producer of a "max_fragment_length" extension in
     * the EncryptedExtensions handshake message.
     */
    private static final
            class EEMaxFragmentLengthProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private EEMaxFragmentLengthProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            // In response to "max_fragment_length" extension request only
            MaxFragLenSpec spec = (MaxFragLenSpec)
                    shc.handshakeExtensions.get(CH_MAX_FRAGMENT_LENGTH);
            if (spec == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "Ignore unavailable max_fragment_length extension");
                }
                return null;        // ignore the extension
            }

            if ((shc.maxFragmentLength > 0) &&
                    (shc.sslConfig.maximumPacketSize != 0)) {
                int estimatedMaxFragSize =
                        shc.negotiatedCipherSuite.calculatePacketSize(
                                shc.maxFragmentLength, shc.negotiatedProtocol,
                                shc.sslContext.isDTLS());
                if (estimatedMaxFragSize > shc.sslConfig.maximumPacketSize) {
                    // For better interoperability, abort the maximum
                    // fragment length negotiation, rather than terminate
                    // the connection with a fatal alert.
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "Abort the maximum fragment length negotiation, " +
                            "may overflow the maximum packet size limit.");
                    }
                    shc.maxFragmentLength = -1;
                }
            }

            // update the context
            if (shc.maxFragmentLength > 0) {
                shc.handshakeSession.setNegotiatedMaxFragSize(
                        shc.maxFragmentLength);
                shc.conContext.inputRecord.changeFragmentSize(
                        shc.maxFragmentLength);
                shc.conContext.outputRecord.changeFragmentSize(
                        shc.maxFragmentLength);

                // The response extension data is the same as the requested one.
                shc.handshakeExtensions.put(EE_MAX_FRAGMENT_LENGTH, spec);
                return new byte[] { spec.id };
            }

            return null;
        }
    }

    /**
     * Network data consumer of a "max_fragment_length" extension in the
     * EncryptedExtensions handshake message.
     */
    private static final
            class EEMaxFragmentLengthConsumer implements ExtensionConsumer {
        // Prevent instantiation of this class.
        private EEMaxFragmentLengthConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // In response to "max_fragment_length" extension request only
            MaxFragLenSpec requestedSpec = (MaxFragLenSpec)
                    chc.handshakeExtensions.get(CH_MAX_FRAGMENT_LENGTH);
            if (requestedSpec == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                    "Unexpected max_fragment_length extension in ServerHello");
            }

            // Parse the extension.
            MaxFragLenSpec spec = new MaxFragLenSpec(chc, buffer);
            if (spec.id != requestedSpec.id) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "The maximum fragment length response is not requested");
            }

            MaxFragLenEnum mfle = MaxFragLenEnum.valueOf(spec.id);
            if (mfle == null) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "the requested maximum fragment length is other " +
                    "than the allowed values");
            }

            // update the context
            chc.maxFragmentLength = mfle.fragmentSize;
            chc.handshakeExtensions.put(EE_MAX_FRAGMENT_LENGTH, spec);
        }
    }

    /**
     * After session creation consuming of a "max_fragment_length"
     * extension in the EncryptedExtensions handshake message.
     */
    private static final
            class EEMaxFragmentLengthUpdate implements HandshakeConsumer {
        // Prevent instantiation of this class.
        private EEMaxFragmentLengthUpdate() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            MaxFragLenSpec spec = (MaxFragLenSpec)
                    chc.handshakeExtensions.get(EE_MAX_FRAGMENT_LENGTH);
            if (spec == null) {
                // Ignore, no "max_fragment_length" extension response.
                return;
            }

            if ((chc.maxFragmentLength > 0) &&
                    (chc.sslConfig.maximumPacketSize != 0)) {
                int estimatedMaxFragSize =
                        chc.negotiatedCipherSuite.calculatePacketSize(
                                chc.maxFragmentLength, chc.negotiatedProtocol,
                                chc.sslContext.isDTLS());
                if (estimatedMaxFragSize > chc.sslConfig.maximumPacketSize) {
                    // For better interoperability, abort the maximum
                    // fragment length negotiation, rather than terminate
                    // the connection with a fatal alert.
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "Abort the maximum fragment length negotiation, " +
                            "may overflow the maximum packet size limit.");
                    }
                    chc.maxFragmentLength = -1;
                }
            }

            // update the context
            if (chc.maxFragmentLength > 0) {
                chc.handshakeSession.setNegotiatedMaxFragSize(
                        chc.maxFragmentLength);
                chc.conContext.inputRecord.changeFragmentSize(
                        chc.maxFragmentLength);
                chc.conContext.outputRecord.changeFragmentSize(
                        chc.maxFragmentLength);
            }
        }
    }
}
