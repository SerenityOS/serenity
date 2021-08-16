/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;
import sun.security.ssl.SSLCipher.SSLReadCipher;

/**
 * {@code InputRecord} implementation for {@code SSLEngine}.
 */
final class SSLEngineInputRecord extends InputRecord implements SSLRecord {
    private boolean formatVerified = false;     // SSLv2 ruled out?

    // Cache for incomplete handshake messages.
    private ByteBuffer handshakeBuffer = null;

    SSLEngineInputRecord(HandshakeHash handshakeHash) {
        super(handshakeHash, SSLReadCipher.nullTlsReadCipher());
    }

    @Override
    int estimateFragmentSize(int packetSize) {
        if (packetSize > 0) {
            return readCipher.estimateFragmentSize(packetSize, headerSize);
        } else {
            return Record.maxDataSize;
        }
    }

    @Override
    int bytesInCompletePacket(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength) throws IOException {

        return bytesInCompletePacket(srcs[srcsOffset]);
    }

    private int bytesInCompletePacket(ByteBuffer packet) throws SSLException {
        /*
         * SSLv2 length field is in bytes 0/1
         * SSLv3/TLS length field is in bytes 3/4
         */
        if (packet.remaining() < 5) {
            return -1;
        }

        int pos = packet.position();
        byte byteZero = packet.get(pos);
        int len;

        /*
         * If we have already verified previous packets, we can
         * ignore the verifications steps, and jump right to the
         * determination.  Otherwise, try one last heuristic to
         * see if it's SSL/TLS.
         */
        if (formatVerified ||
                (byteZero == ContentType.HANDSHAKE.id) ||
                (byteZero == ContentType.ALERT.id)) {
            /*
             * Last sanity check that it's not a wild record
             */
            byte majorVersion = packet.get(pos + 1);
            byte minorVersion = packet.get(pos + 2);
            if (!ProtocolVersion.isNegotiable(
                    majorVersion, minorVersion, false, false)) {
                throw new SSLException("Unrecognized record version " +
                        ProtocolVersion.nameOf(majorVersion, minorVersion) +
                        " , plaintext connection?");
            }

            /*
             * Reasonably sure this is a V3, disable further checks.
             * We can't do the same in the v2 check below, because
             * read still needs to parse/handle the v2 clientHello.
             */
            formatVerified = true;

            /*
             * One of the SSLv3/TLS message types.
             */
            len = ((packet.get(pos + 3) & 0xFF) << 8) +
                   (packet.get(pos + 4) & 0xFF) + headerSize;

        } else {
            /*
             * Must be SSLv2 or something unknown.
             * Check if it's short (2 bytes) or
             * long (3) header.
             *
             * Internals can warn about unsupported SSLv2
             */
            boolean isShort = ((byteZero & 0x80) != 0);

            if (isShort &&
                    ((packet.get(pos + 2) == 1) || packet.get(pos + 2) == 4)) {

                byte majorVersion = packet.get(pos + 3);
                byte minorVersion = packet.get(pos + 4);
                if (!ProtocolVersion.isNegotiable(
                        majorVersion, minorVersion, false, false)) {
                    throw new SSLException("Unrecognized record version " +
                            ProtocolVersion.nameOf(majorVersion, minorVersion) +
                            " , plaintext connection?");
                }

                /*
                 * Client or Server Hello
                 */
                int mask = (isShort ? 0x7F : 0x3F);
                len = ((byteZero & mask) << 8) +
                        (packet.get(pos + 1) & 0xFF) + (isShort ? 2 : 3);

            } else {
                // Gobblygook!
                throw new SSLException(
                        "Unrecognized SSL message, plaintext connection?");
            }
        }

        return len;
    }

    @Override
    Plaintext[] decode(ByteBuffer[] srcs, int srcsOffset,
            int srcsLength) throws IOException, BadPaddingException {
        if (srcs == null || srcs.length == 0 || srcsLength == 0) {
            return new Plaintext[0];
        } else if (srcsLength == 1) {
            return decode(srcs[srcsOffset]);
        } else {
            ByteBuffer packet = extract(srcs,
                    srcsOffset, srcsLength, SSLRecord.headerSize);

            return decode(packet);
        }
    }

    private Plaintext[] decode(ByteBuffer packet)
            throws IOException, BadPaddingException {

        if (isClosed) {
            return null;
        }

        if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
            SSLLogger.fine("Raw read", packet);
        }

        // The caller should have validated the record.
        if (!formatVerified) {
            formatVerified = true;

            /*
             * The first record must either be a handshake record or an
             * alert message. If it's not, it is either invalid or an
             * SSLv2 message.
             */
            int pos = packet.position();
            byte byteZero = packet.get(pos);
            if (byteZero != ContentType.HANDSHAKE.id &&
                    byteZero != ContentType.ALERT.id) {
                return handleUnknownRecord(packet);
            }
        }

        return decodeInputRecord(packet);
    }

    private Plaintext[] decodeInputRecord(ByteBuffer packet)
            throws IOException, BadPaddingException {
        //
        // The packet should be a complete record, or more.
        //
        int srcPos = packet.position();
        int srcLim = packet.limit();

        byte contentType = packet.get();                   // pos: 0
        byte majorVersion = packet.get();                  // pos: 1
        byte minorVersion = packet.get();                  // pos: 2
        int contentLen = Record.getInt16(packet);          // pos: 3, 4

        if (SSLLogger.isOn && SSLLogger.isOn("record")) {
            SSLLogger.fine(
                    "READ: " +
                    ProtocolVersion.nameOf(majorVersion, minorVersion) +
                    " " + ContentType.nameOf(contentType) + ", length = " +
                    contentLen);
        }

        //
        // Check for upper bound.
        //
        // Note: May check packetSize limit in the future.
        if (contentLen < 0 || contentLen > maxLargeRecordSize - headerSize) {
            throw new SSLProtocolException(
                "Bad input record size, TLSCiphertext.length = " + contentLen);
        }

        //
        // Decrypt the fragment
        //
        int recLim = srcPos + SSLRecord.headerSize + contentLen;
        packet.limit(recLim);
        packet.position(srcPos + SSLRecord.headerSize);

        ByteBuffer fragment;
        try {
            Plaintext plaintext =
                    readCipher.decrypt(contentType, packet, null);
            fragment = plaintext.fragment;
            contentType = plaintext.contentType;
        } catch (BadPaddingException bpe) {
            throw bpe;
        } catch (GeneralSecurityException gse) {
            throw (SSLProtocolException)(new SSLProtocolException(
                    "Unexpected exception")).initCause(gse);
        } finally {
            // consume a complete record
            packet.limit(srcLim);
            packet.position(recLim);
        }

        //
        // check for handshake fragment
        //
        if (contentType != ContentType.HANDSHAKE.id &&
                handshakeBuffer != null && handshakeBuffer.hasRemaining()) {
            throw new SSLProtocolException(
                    "Expecting a handshake fragment, but received " +
                    ContentType.nameOf(contentType));
        }

        //
        // parse handshake messages
        //
        if (contentType == ContentType.HANDSHAKE.id) {
            ByteBuffer handshakeFrag = fragment;
            if ((handshakeBuffer != null) &&
                    (handshakeBuffer.remaining() != 0)) {
                ByteBuffer bb = ByteBuffer.wrap(new byte[
                        handshakeBuffer.remaining() + fragment.remaining()]);
                bb.put(handshakeBuffer);
                bb.put(fragment);
                handshakeFrag = bb.rewind();
                handshakeBuffer = null;
            }

            ArrayList<Plaintext> plaintexts = new ArrayList<>(5);
            while (handshakeFrag.hasRemaining()) {
                int remaining = handshakeFrag.remaining();
                if (remaining < handshakeHeaderSize) {
                    handshakeBuffer = ByteBuffer.wrap(new byte[remaining]);
                    handshakeBuffer.put(handshakeFrag);
                    handshakeBuffer.rewind();
                    break;
                }

                handshakeFrag.mark();

                // Fail fast for unknown handshake message.
                byte handshakeType = handshakeFrag.get();
                if (!SSLHandshake.isKnown(handshakeType)) {
                    throw new SSLProtocolException(
                        "Unknown handshake type size, Handshake.msg_type = " +
                        (handshakeType & 0xFF));
                }

                int handshakeBodyLen = Record.getInt24(handshakeFrag);
                if (handshakeBodyLen > SSLConfiguration.maxHandshakeMessageSize) {
                    throw new SSLProtocolException(
                            "The size of the handshake message ("
                            + handshakeBodyLen
                            + ") exceeds the maximum allowed size ("
                            + SSLConfiguration.maxHandshakeMessageSize
                            + ")");
                }

                handshakeFrag.reset();
                int handshakeMessageLen =
                        handshakeHeaderSize + handshakeBodyLen;
                if (remaining < handshakeMessageLen) {
                    handshakeBuffer = ByteBuffer.wrap(new byte[remaining]);
                    handshakeBuffer.put(handshakeFrag);
                    handshakeBuffer.rewind();
                    break;
                } else if (remaining == handshakeMessageLen) {
                    if (handshakeHash.isHashable(handshakeType)) {
                        handshakeHash.receive(handshakeFrag);
                    }

                    plaintexts.add(
                        new Plaintext(contentType,
                            majorVersion, minorVersion, -1, -1L, handshakeFrag)
                    );
                    break;
                } else {
                    int fragPos = handshakeFrag.position();
                    int fragLim = handshakeFrag.limit();
                    int nextPos = fragPos + handshakeMessageLen;
                    handshakeFrag.limit(nextPos);

                    if (handshakeHash.isHashable(handshakeType)) {
                        handshakeHash.receive(handshakeFrag);
                    }

                    plaintexts.add(
                        new Plaintext(contentType, majorVersion, minorVersion,
                            -1, -1L, handshakeFrag.slice())
                    );

                    handshakeFrag.position(nextPos);
                    handshakeFrag.limit(fragLim);
                }
            }

            return plaintexts.toArray(new Plaintext[0]);
        }

        return new Plaintext[] {
            new Plaintext(contentType,
                majorVersion, minorVersion, -1, -1L, fragment)
        };
    }

    private Plaintext[] handleUnknownRecord(ByteBuffer packet)
            throws IOException, BadPaddingException {
        //
        // The packet should be a complete record.
        //
        int srcPos = packet.position();
        int srcLim = packet.limit();

        byte firstByte = packet.get(srcPos);
        byte thirdByte = packet.get(srcPos + 2);

        // Does it look like a Version 2 client hello (V2ClientHello)?
        if (((firstByte & 0x80) != 0) && (thirdByte == 1)) {
            /*
             * If SSLv2Hello is not enabled, throw an exception.
             */
            if (helloVersion != ProtocolVersion.SSL20Hello) {
                throw new SSLHandshakeException("SSLv2Hello is not enabled");
            }

            byte majorVersion = packet.get(srcPos + 3);
            byte minorVersion = packet.get(srcPos + 4);

            if ((majorVersion == ProtocolVersion.SSL20Hello.major) &&
                (minorVersion == ProtocolVersion.SSL20Hello.minor)) {

                /*
                 * Looks like a V2 client hello, but not one saying
                 * "let's talk SSLv3".  So we need to send an SSLv2
                 * error message, one that's treated as fatal by
                 * clients (Otherwise we'll hang.)
                 */
                if (SSLLogger.isOn && SSLLogger.isOn("record")) {
                   SSLLogger.fine(
                            "Requested to negotiate unsupported SSLv2!");
                }

                // Note that the exception is caught in SSLEngineImpl
                // so that SSLv2 error message can be delivered properly.
                throw new UnsupportedOperationException(        // SSLv2Hello
                        "Unsupported SSL v2.0 ClientHello");
            }

            /*
             * If we can map this into a V3 ClientHello, read and
             * hash the rest of the V2 handshake, turn it into a
             * V3 ClientHello message, and pass it up.
             */
            packet.position(srcPos + 2);        // exclude the header
            handshakeHash.receive(packet);
            packet.position(srcPos);

            ByteBuffer converted = convertToClientHello(packet);

            if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
                SSLLogger.fine(
                        "[Converted] ClientHello", converted);
            }

            return new Plaintext[] {
                    new Plaintext(ContentType.HANDSHAKE.id,
                    majorVersion, minorVersion, -1, -1L, converted)
                };
        } else {
            if (((firstByte & 0x80) != 0) && (thirdByte == 4)) {
                throw new SSLException("SSL V2.0 servers are not supported.");
            }

            throw new SSLException("Unsupported or unrecognized SSL message");
        }
    }
}
