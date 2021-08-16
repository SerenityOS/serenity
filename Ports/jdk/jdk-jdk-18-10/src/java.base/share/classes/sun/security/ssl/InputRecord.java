/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.BadPaddingException;
import sun.security.ssl.SSLCipher.SSLReadCipher;

/**
 * {@code InputRecord} takes care of the management of SSL/TLS/DTLS input
 * records, including buffering, decryption, handshake messages marshal, etc.
 *
 * @author David Brownell
 */
abstract class InputRecord implements Record, Closeable {
    SSLReadCipher       readCipher;
    // Needed for KeyUpdate, used after Handshake.Finished
    TransportContext    tc;

    final HandshakeHash handshakeHash;
    volatile boolean    isClosed;

    // The ClientHello version to accept. If set to ProtocolVersion.SSL20Hello
    // and the first message we read is a ClientHello in V2 format, we convert
    // it to V3. Otherwise we throw an exception when encountering a V2 hello.
    ProtocolVersion     helloVersion;

    // fragment size
    int                 fragmentSize;

    final ReentrantLock recordLock = new ReentrantLock();

    InputRecord(HandshakeHash handshakeHash, SSLReadCipher readCipher) {
        this.readCipher = readCipher;
        this.helloVersion = ProtocolVersion.TLS10;
        this.handshakeHash = handshakeHash;
        this.isClosed = false;
        this.fragmentSize = Record.maxDataSize;
    }

    void setHelloVersion(ProtocolVersion helloVersion) {
        this.helloVersion = helloVersion;
    }

    boolean seqNumIsHuge() {
        return (readCipher.authenticator != null) &&
                        readCipher.authenticator.seqNumIsHuge();
    }

    boolean isEmpty() {
        return false;
    }

    // apply to DTLS SSLEngine
    void expectingFinishFlight() {
        // blank
    }

    // apply to DTLS SSLEngine
    void finishHandshake() {
        // blank
    }

    /**
     * Prevent any more data from being read into this record,
     * and flag the record as holding no data.
     */
    @Override
    public void close() throws IOException {
        recordLock.lock();
        try {
            if (!isClosed) {
                isClosed = true;
                readCipher.dispose();
            }
        } finally {
            recordLock.unlock();
        }
    }

    boolean isClosed() {
        return isClosed;
    }

    // apply to SSLSocket and SSLEngine
    void changeReadCiphers(SSLReadCipher readCipher) {

        /*
         * Dispose of any intermediate state in the underlying cipher.
         * For PKCS11 ciphers, this will release any attached sessions,
         * and thus make finalization faster.
         *
         * Since MAC's doFinal() is called for every SSL/TLS packet, it's
         * not necessary to do the same with MAC's.
         */
        readCipher.dispose();

        this.readCipher = readCipher;
    }

    // change fragment size
    void changeFragmentSize(int fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    /*
     * Check if there is enough inbound data in the ByteBuffer to make
     * a inbound packet.
     *
     * @return -1 if there are not enough bytes to tell (small header),
     */
    // apply to SSLEngine only
    int bytesInCompletePacket(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength) throws IOException {

        throw new UnsupportedOperationException("Not supported yet.");
    }

    // apply to SSLSocket only
    int bytesInCompletePacket() throws IOException {
        throw new UnsupportedOperationException();
    }

    // apply to SSLSocket only
    void setReceiverStream(InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    // apply to DTLS SSLEngine only
    Plaintext acquirePlaintext()
            throws IOException, BadPaddingException {
        throw new UnsupportedOperationException();
    }

    // read, decrypt and decompress the network record.
    //
    abstract Plaintext[] decode(ByteBuffer[] srcs, int srcsOffset,
            int srcsLength) throws IOException, BadPaddingException;

    // apply to SSLSocket only
    void setDeliverStream(OutputStream outputStream) {
        throw new UnsupportedOperationException();
    }

    // calculate plaintext fragment size
    //
    // apply to SSLEngine only
    int estimateFragmentSize(int packetSize) {
        throw new UnsupportedOperationException();
    }

    //
    // shared helpers
    //

    // Not apply to DTLS
    static ByteBuffer convertToClientHello(ByteBuffer packet) {
        int srcPos = packet.position();

        byte firstByte = packet.get();
        byte secondByte = packet.get();
        int recordLen = (((firstByte & 0x7F) << 8) | (secondByte & 0xFF)) + 2;

        packet.position(srcPos + 3);        // the V2ClientHello record header

        byte majorVersion = packet.get();
        byte minorVersion = packet.get();

        int cipherSpecLen = ((packet.get() & 0xFF) << 8) +
                             (packet.get() & 0xFF);
        int sessionIdLen  = ((packet.get() & 0xFF) << 8) +
                             (packet.get() & 0xFF);
        int nonceLen      = ((packet.get() & 0xFF) << 8) +
                             (packet.get() & 0xFF);

        // Required space for the target SSLv3 ClientHello message.
        //  5: record header size
        //  4: handshake header size
        //  2: ClientHello.client_version
        // 32: ClientHello.random
        //  1: length byte of ClientHello.session_id
        //  2: length bytes of ClientHello.cipher_suites
        //  2: empty ClientHello.compression_methods
        int requiredSize = 48 + sessionIdLen + ((cipherSpecLen * 2 ) / 3);
        byte[] converted = new byte[requiredSize];

        /*
         * Build the first part of the V3 record header from the V2 one
         * that's now buffered up.  (Lengths are fixed up later).
         */
        // Note: need not to set the header actually.
        converted[0] = ContentType.HANDSHAKE.id;
        converted[1] = majorVersion;
        converted[2] = minorVersion;
        // header [3..4] for handshake message length
        // required size is 5;

        /*
         * Store the generic V3 handshake header:  4 bytes
         */
        converted[5] = 1;    // HandshakeMessage.ht_client_hello
        // buf [6..8] for length of ClientHello (int24)
        // required size += 4;

        /*
         * ClientHello header starts with SSL version
         */
        converted[9] = majorVersion;
        converted[10] = minorVersion;
        // required size += 2;
        int pointer = 11;

        /*
         * Copy Random value/nonce ... if less than the 32 bytes of
         * a V3 "Random", right justify and zero pad to the left.  Else
         * just take the last 32 bytes.
         */
        int offset = srcPos + 11 + cipherSpecLen + sessionIdLen;

        if (nonceLen < 32) {
            for (int i = 0; i < (32 - nonceLen); i++) {
                converted[pointer++] = 0;
            }
            packet.position(offset);
            packet.get(converted, pointer, nonceLen);

            pointer += nonceLen;
        } else {
            packet.position(offset + nonceLen - 32);
            packet.get(converted, pointer, 32);

            pointer += 32;
        }

        /*
         * Copy session ID (only one byte length!)
         */
        offset -= sessionIdLen;
        converted[pointer++] = (byte)(sessionIdLen & 0xFF);
        packet.position(offset);
        packet.get(converted, pointer, sessionIdLen);

        /*
         * Copy and translate cipher suites ... V2 specs with first byte zero
         * are really V3 specs (in the last 2 bytes), just copy those and drop
         * the other ones.  Preference order remains unchanged.
         *
         * Example:  Netscape Navigator 3.0 (exportable) says:
         *
         * 0/3,     SSL_RSA_EXPORT_WITH_RC4_40_MD5
         * 0/6,     SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5
         *
         * Microsoft Internet Explorer 3.0 (exportable) supports only
         *
         * 0/3,     SSL_RSA_EXPORT_WITH_RC4_40_MD5
         */
        int j;

        offset -= cipherSpecLen;
        packet.position(offset);

        j = pointer + 2;
        for (int i = 0; i < cipherSpecLen; i += 3) {
            if (packet.get() != 0) {
                // Ignore version 2.0 specific cipher suite.  Clients
                // should also include the version 3.0 equivalent in
                // the V2ClientHello message.
                packet.get();           // ignore the 2nd byte
                packet.get();           // ignore the 3rd byte
                continue;
            }

            converted[j++] = packet.get();
            converted[j++] = packet.get();
        }

        j -= pointer + 2;
        converted[pointer++] = (byte)((j >>> 8) & 0xFF);
        converted[pointer++] = (byte)(j & 0xFF);
        pointer += j;

        /*
         * Append compression methods (default/null only)
         */
        converted[pointer++] = 1;
        converted[pointer++] = 0;      // Session.compression_null

        /*
         * Fill in lengths of the messages we synthesized (nested:
         * V3 handshake message within V3 record).
         */
        // Note: need not to set the header actually.
        int fragLen = pointer - 5;                      // TLSPlaintext.length
        converted[3] = (byte)((fragLen >>> 8) & 0xFF);
        converted[4] = (byte)(fragLen & 0xFF);

        /*
         * Handshake.length, length of ClientHello message
         */
        fragLen = pointer - 9;                          // Handshake.length
        converted[6] = (byte)((fragLen >>> 16) & 0xFF);
        converted[7] = (byte)((fragLen >>> 8) & 0xFF);
        converted[8] = (byte)(fragLen & 0xFF);

        // consume the full record
        packet.position(srcPos + recordLen);

        // Need no header bytes.
        return ByteBuffer.wrap(converted, 5, pointer - 5);  // 5: header size
    }

    // Extract an SSL/(D)TLS record from the specified source buffers.
    static ByteBuffer extract(
            ByteBuffer[] buffers, int offset, int length, int headerSize) {

        boolean hasFullHeader = false;
        int contentLen = -1;
        for (int i = offset, j = 0;
                i < (offset + length) && j < headerSize; i++) {
            int remains = buffers[i].remaining();
            int pos = buffers[i].position();
            for (int k = 0; k < remains && j < headerSize; j++, k++) {
                byte b = buffers[i].get(pos + k);
                if (j == (headerSize - 2)) {
                    contentLen = ((b & 0xFF) << 8);
                } else if (j == (headerSize -1)) {
                    contentLen |= (b & 0xFF);
                    hasFullHeader = true;
                    break;
                }
            }
        }

        if (!hasFullHeader) {
            throw new BufferUnderflowException();
        }

        int packetLen = headerSize + contentLen;
        int remains = 0;
        for (int i = offset; i < offset + length; i++) {
            remains += buffers[i].remaining();
            if (remains >= packetLen) {
                break;
            }
        }

        if (remains < packetLen) {
            throw new BufferUnderflowException();
        }

        byte[] packet = new byte[packetLen];
        int packetOffset = 0;
        int packetSpaces = packetLen;
        for (int i = offset; i < offset + length; i++) {
            if (buffers[i].hasRemaining()) {
                int len = Math.min(packetSpaces, buffers[i].remaining());
                buffers[i].get(packet, packetOffset, len);
                packetOffset += len;
                packetSpaces -= len;
            }

            if (packetSpaces <= 0) {
                break;
            }
        }

        return ByteBuffer.wrap(packet);
    }
}
