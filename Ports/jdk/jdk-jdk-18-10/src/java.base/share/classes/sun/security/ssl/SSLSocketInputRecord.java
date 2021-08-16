/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Azul Systems, Inc. All rights reserved.
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

import java.io.EOFException;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;

import sun.security.ssl.SSLCipher.SSLReadCipher;

/**
 * {@code InputRecord} implementation for {@code SSLSocket}.
 *
 * @author David Brownell
 */
final class SSLSocketInputRecord extends InputRecord implements SSLRecord {
    private InputStream is = null;
    private OutputStream os = null;
    private final byte[] header = new byte[headerSize];
    private int headerOff = 0;
    // Cache for incomplete record body.
    private ByteBuffer recordBody = ByteBuffer.allocate(1024);

    private boolean formatVerified = false;     // SSLv2 ruled out?

    // Cache for incomplete handshake messages.
    private ByteBuffer handshakeBuffer = null;

    SSLSocketInputRecord(HandshakeHash handshakeHash) {
        super(handshakeHash, SSLReadCipher.nullTlsReadCipher());
    }

    @Override
    int bytesInCompletePacket() throws IOException {
        // read header
        try {
            readHeader();
        } catch (EOFException eofe) {
            // The caller will handle EOF.
            return -1;
        }

        byte byteZero = header[0];
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
            if (!ProtocolVersion.isNegotiable(
                    header[1], header[2], false, false)) {
                throw new SSLException("Unrecognized record version " +
                        ProtocolVersion.nameOf(header[1], header[2]) +
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
            len = ((header[3] & 0xFF) << 8) +
                    (header[4] & 0xFF) + headerSize;
        } else {
            /*
             * Must be SSLv2 or something unknown.
             * Check if it's short (2 bytes) or
             * long (3) header.
             *
             * Internals can warn about unsupported SSLv2
             */
            boolean isShort = ((byteZero & 0x80) != 0);

            if (isShort && ((header[2] == 1) || (header[2] == 4))) {
                if (!ProtocolVersion.isNegotiable(
                        header[3], header[4], false, false)) {
                    throw new SSLException("Unrecognized record version " +
                            ProtocolVersion.nameOf(header[3], header[4]) +
                            " , plaintext connection?");
                }

                /*
                 * Client or Server Hello
                 */
                //
                // Short header is using here.  We reverse the code here
                // in case it is used in the future.
                //
                // int mask = (isShort ? 0x7F : 0x3F);
                // len = ((byteZero & mask) << 8) +
                //        (header[1] & 0xFF) + (isShort ? 2 : 3);
                //
                len = ((byteZero & 0x7F) << 8) + (header[1] & 0xFF) + 2;
            } else {
                // Gobblygook!
                throw new SSLException(
                        "Unrecognized SSL message, plaintext connection?");
            }
        }

        return len;
    }

    // Note that the input arguments are not used actually.
    @Override
    Plaintext[] decode(ByteBuffer[] srcs, int srcsOffset,
            int srcsLength) throws IOException, BadPaddingException {

        if (isClosed) {
            return null;
        }

        // read header
        readHeader();

        Plaintext[] plaintext = null;
        boolean cleanInBuffer = true;
        try {
            if (!formatVerified) {
                formatVerified = true;

                /*
                 * The first record must either be a handshake record or an
                 * alert message. If it's not, it is either invalid or an
                 * SSLv2 message.
                 */
                if ((header[0] != ContentType.HANDSHAKE.id) &&
                        (header[0] != ContentType.ALERT.id)) {
                    plaintext = handleUnknownRecord();
                }
            }

            // The record header should has consumed.
            if (plaintext == null) {
                plaintext = decodeInputRecord();
            }
        } catch(InterruptedIOException e) {
            // do not clean header and recordBody in case of Socket Timeout
            cleanInBuffer = false;
            throw e;
        } finally {
            if (cleanInBuffer) {
                headerOff = 0;
                recordBody.clear();
            }
        }
        return plaintext;
    }

    @Override
    void setReceiverStream(InputStream inputStream) {
        this.is = inputStream;
    }

    @Override
    void setDeliverStream(OutputStream outputStream) {
        this.os = outputStream;
    }

    private Plaintext[] decodeInputRecord() throws IOException, BadPaddingException {
        byte contentType = header[0];                   // pos: 0
        byte majorVersion = header[1];                  // pos: 1
        byte minorVersion = header[2];                  // pos: 2
        int contentLen = ((header[3] & 0xFF) << 8) +
                           (header[4] & 0xFF);          // pos: 3, 4

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
        // Read a complete record and store in the recordBody
        // recordBody is used to cache incoming record and restore in case of
        // read operation timedout
        //
        if (recordBody.position() == 0) {
            if (recordBody.capacity() < contentLen) {
                recordBody = ByteBuffer.allocate(contentLen);
            }
            recordBody.limit(contentLen);
        } else {
            contentLen = recordBody.remaining();
        }
        readFully(contentLen);
        recordBody.flip();

        if (SSLLogger.isOn && SSLLogger.isOn("record")) {
            SSLLogger.fine(
                    "READ: " +
                    ProtocolVersion.nameOf(majorVersion, minorVersion) +
                    " " + ContentType.nameOf(contentType) + ", length = " +
                    recordBody.remaining());
        }

        //
        // Decrypt the fragment
        //
        ByteBuffer fragment;
        try {
            Plaintext plaintext =
                    readCipher.decrypt(contentType, recordBody, null);
            fragment = plaintext.fragment;
            contentType = plaintext.contentType;
        } catch (BadPaddingException bpe) {
            throw bpe;
        } catch (GeneralSecurityException gse) {
            throw (SSLProtocolException)(new SSLProtocolException(
                    "Unexpected exception")).initCause(gse);
        }

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
                }

                if (remaining == handshakeMessageLen) {
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

    private Plaintext[] handleUnknownRecord() throws IOException, BadPaddingException {
        byte firstByte = header[0];
        byte thirdByte = header[2];

        // Does it look like a Version 2 client hello (V2ClientHello)?
        if (((firstByte & 0x80) != 0) && (thirdByte == 1)) {
            /*
             * If SSLv2Hello is not enabled, throw an exception.
             */
            if (helloVersion != ProtocolVersion.SSL20Hello) {
                throw new SSLHandshakeException("SSLv2Hello is not enabled");
            }

            byte majorVersion = header[3];
            byte minorVersion = header[4];

            if ((majorVersion == ProtocolVersion.SSL20Hello.major) &&
                (minorVersion == ProtocolVersion.SSL20Hello.minor)) {

                /*
                 * Looks like a V2 client hello, but not one saying
                 * "let's talk SSLv3".  So we need to send an SSLv2
                 * error message, one that's treated as fatal by
                 * clients (Otherwise we'll hang.)
                 */
                os.write(SSLRecord.v2NoCipher);      // SSLv2Hello

                if (SSLLogger.isOn) {
                    if (SSLLogger.isOn("record")) {
                         SSLLogger.fine(
                                "Requested to negotiate unsupported SSLv2!");
                    }

                    if (SSLLogger.isOn("packet")) {
                        SSLLogger.fine("Raw write", SSLRecord.v2NoCipher);
                    }
                }

                throw new SSLException("Unsupported SSL v2.0 ClientHello");
            }

            int msgLen = ((header[0] & 0x7F) << 8) | (header[1] & 0xFF);
            if (recordBody.position() == 0) {
                if (recordBody.capacity() < (headerSize + msgLen)) {
                    recordBody = ByteBuffer.allocate(headerSize + msgLen);
                }
                recordBody.limit(headerSize + msgLen);
                recordBody.put(header, 0, headerSize);
            } else {
                msgLen = recordBody.remaining();
            }
            msgLen -= 3;            // had read 3 bytes of content as header
            readFully(msgLen);
            recordBody.flip();

            /*
             * If we can map this into a V3 ClientHello, read and
             * hash the rest of the V2 handshake, turn it into a
             * V3 ClientHello message, and pass it up.
             */
            recordBody.position(2);     // exclude the header
            handshakeHash.receive(recordBody);
            recordBody.position(0);

            ByteBuffer converted = convertToClientHello(recordBody);

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

    // Read the exact bytes of data, otherwise, throw IOException.
    private int readFully(int len) throws IOException {
        int end = len + recordBody.position();
        int off = recordBody.position();
        try {
            while (off < end) {
                off += read(is, recordBody.array(), off, end - off);
            }
        } finally {
            recordBody.position(off);
        }
        return len;
    }

    // Read SSE record header, otherwise, throw IOException.
    private int readHeader() throws IOException {
        while (headerOff < headerSize) {
            headerOff += read(is, header, headerOff, headerSize - headerOff);
        }
        return headerSize;
    }

    private static int read(InputStream is, byte[] buf, int off, int len)  throws IOException {
        int readLen = is.read(buf, off, len);
        if (readLen < 0) {
            if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
                SSLLogger.fine("Raw read: EOF");
            }
            throw new EOFException("SSL peer shut down incorrectly");
        }

        if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
            ByteBuffer bb = ByteBuffer.wrap(buf, off, readLen);
            SSLLogger.fine("Raw read", bb);
        }
        return readLen;
    }

    // Try to use up the input stream without impact the performance too much.
    void deplete(boolean tryToRead) throws IOException {
        int remaining = is.available();
        if (tryToRead && (remaining == 0)) {
            // try to wait and read one byte if no buffered input
            is.read();
        }

        while ((remaining = is.available()) != 0) {
            is.skip(remaining);
        }
    }
}
