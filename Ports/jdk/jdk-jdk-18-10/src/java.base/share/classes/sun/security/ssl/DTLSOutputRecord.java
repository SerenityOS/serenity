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

import java.io.*;
import java.nio.*;
import java.util.*;
import javax.net.ssl.*;
import sun.security.ssl.SSLCipher.SSLWriteCipher;

/**
 * DTLS {@code OutputRecord} implementation for {@code SSLEngine}.
 */
final class DTLSOutputRecord extends OutputRecord implements DTLSRecord {

    private DTLSFragmenter fragmenter;

    int                 writeEpoch;

    int                 prevWriteEpoch;
    Authenticator       prevWriteAuthenticator;
    SSLWriteCipher      prevWriteCipher;

    private volatile boolean isCloseWaiting;

    DTLSOutputRecord(HandshakeHash handshakeHash) {
        super(handshakeHash, SSLWriteCipher.nullDTlsWriteCipher());

        this.writeEpoch = 0;
        this.prevWriteEpoch = 0;
        this.prevWriteCipher = SSLWriteCipher.nullDTlsWriteCipher();

        this.packetSize = DTLSRecord.maxRecordSize;
        this.protocolVersion = ProtocolVersion.NONE;
    }

    @Override
    public void close() throws IOException {
        recordLock.lock();
        try {
            if (!isClosed) {
                if (fragmenter != null && fragmenter.hasAlert()) {
                    isCloseWaiting = true;
                } else {
                    super.close();
                }
            }
        } finally {
            recordLock.unlock();
        }
    }

    boolean isClosed() {
        return isClosed || isCloseWaiting;
    }

    @Override
    void initHandshaker() {
        // clean up
        fragmenter = null;
    }

    @Override
    void finishHandshake() {
        // Nothing to do here currently.
    }

    @Override
    void changeWriteCiphers(SSLWriteCipher writeCipher,
            boolean useChangeCipherSpec) throws IOException {
        if (isClosed()) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("outbound has closed, ignore outbound " +
                    "change_cipher_spec message");
            }
            return;
        }

        if (useChangeCipherSpec) {
            encodeChangeCipherSpec();
        }

        prevWriteCipher.dispose();

        this.prevWriteCipher = this.writeCipher;
        this.prevWriteEpoch = this.writeEpoch;

        this.writeCipher = writeCipher;
        this.writeEpoch++;

        this.isFirstAppOutputRecord = true;

        // set the epoch number
        this.writeCipher.authenticator.setEpochNumber(this.writeEpoch);
    }

    @Override
    void encodeAlert(byte level, byte description) throws IOException {
        if (isClosed()) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("outbound has closed, ignore outbound " +
                    "alert message: " + Alert.nameOf(description));
            }
            return;
        }

        if (fragmenter == null) {
           fragmenter = new DTLSFragmenter();
        }

        fragmenter.queueUpAlert(level, description);
    }

    @Override
    void encodeChangeCipherSpec() throws IOException {
        if (isClosed()) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("outbound has closed, ignore outbound " +
                    "change_cipher_spec message");
            }
            return;
        }

        if (fragmenter == null) {
           fragmenter = new DTLSFragmenter();
        }
        fragmenter.queueUpChangeCipherSpec();
    }

    @Override
    void encodeHandshake(byte[] source,
            int offset, int length) throws IOException {
        if (isClosed()) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("outbound has closed, ignore outbound " +
                        "handshake message",
                        ByteBuffer.wrap(source, offset, length));
            }
            return;
        }

        if (firstMessage) {
            firstMessage = false;
        }

        if (fragmenter == null) {
           fragmenter = new DTLSFragmenter();
        }

        fragmenter.queueUpHandshake(source, offset, length);
    }

    @Override
    Ciphertext encode(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength,
        ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws IOException {

        if (isClosed) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("outbound has closed, ignore outbound " +
                    "application data or cached messages");
            }

            return null;
        } else if (isCloseWaiting) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("outbound has closed, ignore outbound " +
                    "application data");
            }

            srcs = null;    // use no application data.
        }

        return encode(srcs, srcsOffset, srcsLength, dsts[0]);
    }

    private Ciphertext encode(ByteBuffer[] sources, int offset, int length,
            ByteBuffer destination) throws IOException {

        if (writeCipher.authenticator.seqNumOverflow()) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.fine(
                    "sequence number extremely close to overflow " +
                    "(2^64-1 packets). Closing connection.");
            }

            throw new SSLHandshakeException("sequence number overflow");
        }

        // Don't process the incoming record until all of the buffered records
        // get handled.  May need retransmission if no sources specified.
        if (!isEmpty() || sources == null || sources.length == 0) {
            Ciphertext ct = acquireCiphertext(destination);
            if (ct != null) {
                return ct;
            }
        }

        if (sources == null || sources.length == 0) {
            return null;
        }

        int srcsRemains = 0;
        for (int i = offset; i < offset + length; i++) {
            srcsRemains += sources[i].remaining();
        }

        if (srcsRemains == 0) {
            return null;
        }

        // not apply to handshake message
        int fragLen;
        if (packetSize > 0) {
            fragLen = Math.min(maxRecordSize, packetSize);
            fragLen = writeCipher.calculateFragmentSize(
                    fragLen, headerSize);

            fragLen = Math.min(fragLen, Record.maxDataSize);
        } else {
            fragLen = Record.maxDataSize;
        }

        // Calculate more impact, for example TLS 1.3 padding.
        fragLen = calculateFragmentSize(fragLen);

        int dstPos = destination.position();
        int dstLim = destination.limit();
        int dstContent = dstPos + headerSize +
                                writeCipher.getExplicitNonceSize();
        destination.position(dstContent);

        int remains = Math.min(fragLen, destination.remaining());
        fragLen = 0;
        int srcsLen = offset + length;
        for (int i = offset; (i < srcsLen) && (remains > 0); i++) {
            int amount = Math.min(sources[i].remaining(), remains);
            int srcLimit = sources[i].limit();
            sources[i].limit(sources[i].position() + amount);
            destination.put(sources[i]);
            sources[i].limit(srcLimit);         // restore the limit
            remains -= amount;
            fragLen += amount;
        }

        destination.limit(destination.position());
        destination.position(dstContent);

        if (SSLLogger.isOn && SSLLogger.isOn("record")) {
            SSLLogger.fine(
                    "WRITE: " + protocolVersion.name + " " +
                    ContentType.APPLICATION_DATA.name +
                    ", length = " + destination.remaining());
        }

        // Encrypt the fragment and wrap up a record.
        long recordSN = encrypt(writeCipher,
                ContentType.APPLICATION_DATA.id, destination,
                dstPos, dstLim, headerSize,
                protocolVersion);

        if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
            ByteBuffer temporary = destination.duplicate();
            temporary.limit(temporary.position());
            temporary.position(dstPos);
            SSLLogger.fine("Raw write", temporary);
        }

        // remain the limit unchanged
        destination.limit(dstLim);

        return new Ciphertext(ContentType.APPLICATION_DATA.id,
                SSLHandshake.NOT_APPLICABLE.id, recordSN);
    }

    private Ciphertext acquireCiphertext(
            ByteBuffer destination) throws IOException {
        if (fragmenter != null) {
            return fragmenter.acquireCiphertext(destination);
        }

        return null;
    }

    @Override
    boolean isEmpty() {
        return (fragmenter == null) || fragmenter.isEmpty();
    }

    @Override
    void launchRetransmission() {
        // Note: Please don't retransmit if there are handshake messages
        // or alerts waiting in the queue.
        if ((fragmenter != null) && fragmenter.isRetransmittable()) {
            fragmenter.setRetransmission();
        }
    }

    // buffered record fragment
    private static class RecordMemo {
        byte            contentType;
        byte            majorVersion;
        byte            minorVersion;
        int             encodeEpoch;
        SSLWriteCipher  encodeCipher;

        byte[]          fragment;
    }

    private static class HandshakeMemo extends RecordMemo {
        byte            handshakeType;
        int             messageSequence;
        int             acquireOffset;
    }

    private final class DTLSFragmenter {
        private final LinkedList<RecordMemo> handshakeMemos =
                new LinkedList<>();
        private int acquireIndex = 0;
        private int messageSequence = 0;
        private boolean flightIsReady = false;

        // Per section 4.1.1, RFC 6347:
        //
        // If repeated retransmissions do not result in a response, and the
        // PMTU is unknown, subsequent retransmissions SHOULD back off to a
        // smaller record size, fragmenting the handshake message as
        // appropriate.
        //
        // In this implementation, two times of retransmits would be attempted
        // before backing off.  The back off is supported only if the packet
        // size is bigger than 256 bytes.
        private int retransmits = 2;            // attemps of retransmits

        void queueUpHandshake(byte[] buf,
                int offset, int length) throws IOException {

            // Cleanup if a new flight starts.
            if (flightIsReady) {
                handshakeMemos.clear();
                acquireIndex = 0;
                flightIsReady = false;
            }

            HandshakeMemo memo = new HandshakeMemo();

            memo.contentType = ContentType.HANDSHAKE.id;
            memo.majorVersion = protocolVersion.major;
            memo.minorVersion = protocolVersion.minor;
            memo.encodeEpoch = writeEpoch;
            memo.encodeCipher = writeCipher;

            memo.handshakeType = buf[offset];
            memo.messageSequence = messageSequence++;
            memo.acquireOffset = 0;
            memo.fragment = new byte[length - 4];       // 4: header size
                                                        //    1: HandshakeType
                                                        //    3: message length
            System.arraycopy(buf, offset + 4, memo.fragment, 0, length - 4);

            handshakeHashing(memo, memo.fragment);
            handshakeMemos.add(memo);

            if ((memo.handshakeType == SSLHandshake.CLIENT_HELLO.id) ||
                (memo.handshakeType == SSLHandshake.HELLO_REQUEST.id) ||
                (memo.handshakeType ==
                        SSLHandshake.HELLO_VERIFY_REQUEST.id) ||
                (memo.handshakeType == SSLHandshake.SERVER_HELLO_DONE.id) ||
                (memo.handshakeType == SSLHandshake.FINISHED.id)) {

                flightIsReady = true;
            }
        }

        void queueUpChangeCipherSpec() {

            // Cleanup if a new flight starts.
            if (flightIsReady) {
                handshakeMemos.clear();
                acquireIndex = 0;
                flightIsReady = false;
            }

            RecordMemo memo = new RecordMemo();

            memo.contentType = ContentType.CHANGE_CIPHER_SPEC.id;
            memo.majorVersion = protocolVersion.major;
            memo.minorVersion = protocolVersion.minor;
            memo.encodeEpoch = writeEpoch;
            memo.encodeCipher = writeCipher;

            memo.fragment = new byte[1];
            memo.fragment[0] = 1;

            handshakeMemos.add(memo);
        }

        void queueUpAlert(byte level, byte description) throws IOException {
            RecordMemo memo = new RecordMemo();

            memo.contentType = ContentType.ALERT.id;
            memo.majorVersion = protocolVersion.major;
            memo.minorVersion = protocolVersion.minor;
            memo.encodeEpoch = writeEpoch;
            memo.encodeCipher = writeCipher;

            memo.fragment = new byte[2];
            memo.fragment[0] = level;
            memo.fragment[1] = description;

            handshakeMemos.add(memo);
        }

        Ciphertext acquireCiphertext(ByteBuffer dstBuf) throws IOException {
            if (isEmpty()) {
                if (isRetransmittable()) {
                    setRetransmission();    // configure for retransmission
                } else {
                    return null;
                }
            }

            RecordMemo memo = handshakeMemos.get(acquireIndex);
            HandshakeMemo hsMemo = null;
            if (memo.contentType == ContentType.HANDSHAKE.id) {
                hsMemo = (HandshakeMemo)memo;
            }

            // ChangeCipherSpec message is pretty small.  Don't worry about
            // the fragmentation of ChangeCipherSpec record.
            int fragLen;
            if (packetSize > 0) {
                fragLen = Math.min(maxRecordSize, packetSize);
                fragLen = memo.encodeCipher.calculateFragmentSize(
                        fragLen, 25);   // 25: header size
                                                //   13: DTLS record
                                                //   12: DTLS handshake message
                fragLen = Math.min(fragLen, Record.maxDataSize);
            } else {
                fragLen = Record.maxDataSize;
            }

            // Calculate more impact, for example TLS 1.3 padding.
            fragLen = calculateFragmentSize(fragLen);

            int dstPos = dstBuf.position();
            int dstLim = dstBuf.limit();
            int dstContent = dstPos + headerSize +
                                    memo.encodeCipher.getExplicitNonceSize();
            dstBuf.position(dstContent);

            if (hsMemo != null) {
                fragLen = Math.min(fragLen,
                        (hsMemo.fragment.length - hsMemo.acquireOffset));

                dstBuf.put(hsMemo.handshakeType);
                dstBuf.put((byte)((hsMemo.fragment.length >> 16) & 0xFF));
                dstBuf.put((byte)((hsMemo.fragment.length >> 8) & 0xFF));
                dstBuf.put((byte)(hsMemo.fragment.length & 0xFF));
                dstBuf.put((byte)((hsMemo.messageSequence >> 8) & 0xFF));
                dstBuf.put((byte)(hsMemo.messageSequence & 0xFF));
                dstBuf.put((byte)((hsMemo.acquireOffset >> 16) & 0xFF));
                dstBuf.put((byte)((hsMemo.acquireOffset >> 8) & 0xFF));
                dstBuf.put((byte)(hsMemo.acquireOffset & 0xFF));
                dstBuf.put((byte)((fragLen >> 16) & 0xFF));
                dstBuf.put((byte)((fragLen >> 8) & 0xFF));
                dstBuf.put((byte)(fragLen & 0xFF));
                dstBuf.put(hsMemo.fragment, hsMemo.acquireOffset, fragLen);
            } else {
                fragLen = Math.min(fragLen, memo.fragment.length);
                dstBuf.put(memo.fragment, 0, fragLen);
            }

            dstBuf.limit(dstBuf.position());
            dstBuf.position(dstContent);

            if (SSLLogger.isOn && SSLLogger.isOn("record")) {
                SSLLogger.fine(
                        "WRITE: " + protocolVersion.name + " " +
                        ContentType.nameOf(memo.contentType) +
                        ", length = " + dstBuf.remaining());
            }

            // Encrypt the fragment and wrap up a record.
            long recordSN = encrypt(memo.encodeCipher,
                    memo.contentType, dstBuf,
                    dstPos, dstLim, headerSize,
                    ProtocolVersion.valueOf(memo.majorVersion,
                            memo.minorVersion));

            if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
                ByteBuffer temporary = dstBuf.duplicate();
                temporary.limit(temporary.position());
                temporary.position(dstPos);
                SSLLogger.fine(
                        "Raw write (" + temporary.remaining() + ")", temporary);
            }

            // remain the limit unchanged
            dstBuf.limit(dstLim);

            // Reset the fragmentation offset.
            if (hsMemo != null) {
                hsMemo.acquireOffset += fragLen;
                if (hsMemo.acquireOffset == hsMemo.fragment.length) {
                    acquireIndex++;
                }

                return new Ciphertext(hsMemo.contentType,
                        hsMemo.handshakeType, recordSN);
            } else {
                if (isCloseWaiting &&
                        memo.contentType == ContentType.ALERT.id) {
                    close();
                }

                acquireIndex++;
                return new Ciphertext(memo.contentType,
                        SSLHandshake.NOT_APPLICABLE.id, recordSN);
            }
        }

        private void handshakeHashing(HandshakeMemo hsFrag, byte[] hsBody) {

            byte hsType = hsFrag.handshakeType;
            if (!handshakeHash.isHashable(hsType)) {
                // omitted from handshake hash computation
                return;
            }

            // calculate the DTLS header
            byte[] temporary = new byte[12];    // 12: handshake header size

            // Handshake.msg_type
            temporary[0] = hsFrag.handshakeType;

            // Handshake.length
            temporary[1] = (byte)((hsBody.length >> 16) & 0xFF);
            temporary[2] = (byte)((hsBody.length >> 8) & 0xFF);
            temporary[3] = (byte)(hsBody.length & 0xFF);

            // Handshake.message_seq
            temporary[4] = (byte)((hsFrag.messageSequence >> 8) & 0xFF);
            temporary[5] = (byte)(hsFrag.messageSequence & 0xFF);

            // Handshake.fragment_offset
            temporary[6] = 0;
            temporary[7] = 0;
            temporary[8] = 0;

            // Handshake.fragment_length
            temporary[9] = temporary[1];
            temporary[10] = temporary[2];
            temporary[11] = temporary[3];

            handshakeHash.deliver(temporary, 0, 12);
            handshakeHash.deliver(hsBody, 0, hsBody.length);
        }

        boolean isEmpty() {
            if (!flightIsReady || handshakeMemos.isEmpty() ||
                    acquireIndex >= handshakeMemos.size()) {
                return true;
            }

            return false;
        }

        boolean hasAlert() {
            for (RecordMemo memo : handshakeMemos) {
                if (memo.contentType == ContentType.ALERT.id) {
                    return true;
                }
            }

            return false;
        }

        boolean isRetransmittable() {
            return (flightIsReady && !handshakeMemos.isEmpty() &&
                                (acquireIndex >= handshakeMemos.size()));
        }

        private void setRetransmission() {
            acquireIndex = 0;
            for (RecordMemo memo : handshakeMemos) {
                if (memo instanceof HandshakeMemo) {
                    HandshakeMemo hmemo = (HandshakeMemo)memo;
                    hmemo.acquireOffset = 0;
                }
            }

            // Shrink packet size if:
            // 1. maximum fragment size is allowed, in which case the packet
            //    size is configured bigger than maxRecordSize;
            // 2. maximum packet is bigger than 256 bytes;
            // 3. two times of retransmits have been attempted.
            if ((packetSize <= maxRecordSize) &&
                    (packetSize > 256) && ((retransmits--) <= 0)) {

                // shrink packet size
                shrinkPacketSize();
                retransmits = 2;        // attemps of retransmits
            }
        }

        private void shrinkPacketSize() {
            packetSize = Math.max(256, packetSize / 2);
        }
    }
}
