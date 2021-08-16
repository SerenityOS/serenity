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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLHandshakeException;

/**
 * {@code OutputRecord} implementation for {@code SSLSocket}.
 */
final class SSLSocketOutputRecord extends OutputRecord implements SSLRecord {
    private OutputStream deliverStream = null;

    SSLSocketOutputRecord(HandshakeHash handshakeHash) {
        this(handshakeHash, null);
    }

    SSLSocketOutputRecord(HandshakeHash handshakeHash,
            TransportContext tc) {
        super(handshakeHash, SSLCipher.SSLWriteCipher.nullTlsWriteCipher());
        this.tc = tc;
        this.packetSize = SSLRecord.maxRecordSize;
        this.protocolVersion = ProtocolVersion.NONE;
    }

    @Override
    void encodeAlert(byte level, byte description) throws IOException {
        recordLock.lock();
        try {
            if (isClosed()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning("outbound has closed, ignore outbound " +
                        "alert message: " + Alert.nameOf(description));
                }
                return;
            }

            // use the buf of ByteArrayOutputStream
            count = headerSize + writeCipher.getExplicitNonceSize();

            write(level);
            write(description);
            if (SSLLogger.isOn && SSLLogger.isOn("record")) {
                SSLLogger.fine("WRITE: " + protocolVersion.name +
                        " " + ContentType.ALERT.name +
                        "(" + Alert.nameOf(description) + ")" +
                        ", length = " + (count - headerSize));
            }

            // Encrypt the fragment and wrap up a record.
            encrypt(writeCipher, ContentType.ALERT.id, headerSize);

            // deliver this message
            deliverStream.write(buf, 0, count);    // may throw IOException
            deliverStream.flush();                 // may throw IOException

            if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
                SSLLogger.fine("Raw write",
                        (new ByteArrayInputStream(buf, 0, count)));
            }

            // reset the internal buffer
            count = 0;
        } finally {
            recordLock.unlock();
        }
    }

    @Override
    void encodeHandshake(byte[] source,
            int offset, int length) throws IOException {
        recordLock.lock();
        try {
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

                if ((helloVersion == ProtocolVersion.SSL20Hello) &&
                    (source[offset] == SSLHandshake.CLIENT_HELLO.id) &&
                                            //  5: recode header size
                    (source[offset + 4 + 2 + 32] == 0)) {
                                            // V3 session ID is empty
                                            //  4: handshake header size
                                            //  2: client_version in ClientHello
                                            // 32: random in ClientHello

                    ByteBuffer v2ClientHello = encodeV2ClientHello(
                            source, (offset + 4), (length - 4));

                    // array offset is zero
                    byte[] record = v2ClientHello.array();
                    int limit = v2ClientHello.limit();
                    handshakeHash.deliver(record, 2, (limit - 2));

                    if (SSLLogger.isOn && SSLLogger.isOn("record")) {
                        SSLLogger.fine(
                                "WRITE: SSLv2 ClientHello message" +
                                ", length = " + limit);
                    }

                    // deliver this message
                    //
                    // Version 2 ClientHello message should be plaintext.
                    //
                    // No max fragment length negotiation.
                    deliverStream.write(record, 0, limit);
                    deliverStream.flush();

                    if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
                        SSLLogger.fine("Raw write",
                                (new ByteArrayInputStream(record, 0, limit)));
                    }

                    return;
                }
            }

            byte handshakeType = source[0];
            if (handshakeHash.isHashable(handshakeType)) {
                handshakeHash.deliver(source, offset, length);
            }

            int fragLimit = getFragLimit();
            int position = headerSize + writeCipher.getExplicitNonceSize();
            if (count == 0) {
                count = position;
            }

            if ((count - position) < (fragLimit - length)) {
                write(source, offset, length);
                return;
            }

            for (int limit = (offset + length); offset < limit;) {

                int remains = (limit - offset) + (count - position);
                int fragLen = Math.min(fragLimit, remains);

                // use the buf of ByteArrayOutputStream
                write(source, offset, fragLen);
                if (remains < fragLimit) {
                    return;
                }

                if (SSLLogger.isOn && SSLLogger.isOn("record")) {
                    SSLLogger.fine(
                            "WRITE: " + protocolVersion.name +
                            " " + ContentType.HANDSHAKE.name +
                            ", length = " + (count - headerSize));
                }

                // Encrypt the fragment and wrap up a record.
                encrypt(writeCipher, ContentType.HANDSHAKE.id, headerSize);

                // deliver this message
                deliverStream.write(buf, 0, count);    // may throw IOException
                deliverStream.flush();                 // may throw IOException

                if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
                    SSLLogger.fine("Raw write",
                            (new ByteArrayInputStream(buf, 0, count)));
                }

                // reset the offset
                offset += fragLen;

                // reset the internal buffer
                count = position;
            }
        } finally {
            recordLock.unlock();
        }
    }

    @Override
    void encodeChangeCipherSpec() throws IOException {
        recordLock.lock();
        try {
            if (isClosed()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning("outbound has closed, ignore outbound " +
                        "change_cipher_spec message");
                }
                return;
            }

            // use the buf of ByteArrayOutputStream
            count = headerSize + writeCipher.getExplicitNonceSize();

            write((byte)1);         // byte 1: change_cipher_spec(

            // Encrypt the fragment and wrap up a record.
            encrypt(writeCipher, ContentType.CHANGE_CIPHER_SPEC.id, headerSize);

            // deliver this message
            deliverStream.write(buf, 0, count);        // may throw IOException
            // deliverStream.flush();                  // flush in Finished

            if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
                SSLLogger.fine("Raw write",
                        (new ByteArrayInputStream(buf, 0, count)));
            }

            // reset the internal buffer
            count = 0;
        } finally {
            recordLock.unlock();
        }
    }

    @Override
    public void flush() throws IOException {
        recordLock.lock();
        try {
            int position = headerSize + writeCipher.getExplicitNonceSize();
            if (count <= position) {
                return;
            }

            if (SSLLogger.isOn && SSLLogger.isOn("record")) {
                SSLLogger.fine(
                        "WRITE: " + protocolVersion.name +
                        " " + ContentType.HANDSHAKE.name +
                        ", length = " + (count - headerSize));
            }

            // Encrypt the fragment and wrap up a record.
            encrypt(writeCipher, ContentType.HANDSHAKE.id, headerSize);

            // deliver this message
            deliverStream.write(buf, 0, count);    // may throw IOException
            deliverStream.flush();                 // may throw IOException

            if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
                SSLLogger.fine("Raw write",
                        (new ByteArrayInputStream(buf, 0, count)));
            }

            // reset the internal buffer
            count = 0;      // DON'T use position
        } finally {
            recordLock.unlock();
        }
    }

    @Override
    void deliver(byte[] source, int offset, int length) throws IOException {
        recordLock.lock();
        try {
            if (isClosed()) {
                throw new SocketException(
                        "Connection or outbound has been closed");
            }

            if (writeCipher.authenticator.seqNumOverflow()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine(
                        "sequence number extremely close to overflow " +
                        "(2^64-1 packets). Closing connection.");
                }

                throw new SSLHandshakeException("sequence number overflow");
            }

            boolean isFirstRecordOfThePayload = true;
            for (int limit = (offset + length); offset < limit;) {
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

                if (isFirstRecordOfThePayload && needToSplitPayload()) {
                    fragLen = 1;
                    isFirstRecordOfThePayload = false;
                } else {
                    fragLen = Math.min(fragLen, (limit - offset));
                }

                // use the buf of ByteArrayOutputStream
                int position = headerSize + writeCipher.getExplicitNonceSize();
                count = position;
                write(source, offset, fragLen);

                if (SSLLogger.isOn && SSLLogger.isOn("record")) {
                    SSLLogger.fine(
                            "WRITE: " + protocolVersion.name +
                            " " + ContentType.APPLICATION_DATA.name +
                            ", length = " + (count - position));
                }

                // Encrypt the fragment and wrap up a record.
                encrypt(writeCipher,
                        ContentType.APPLICATION_DATA.id, headerSize);

                // deliver this message
                deliverStream.write(buf, 0, count);    // may throw IOException
                deliverStream.flush();                 // may throw IOException

                if (SSLLogger.isOn && SSLLogger.isOn("packet")) {
                    SSLLogger.fine("Raw write",
                            (new ByteArrayInputStream(buf, 0, count)));
                }

                // reset the internal buffer
                count = 0;

                if (isFirstAppOutputRecord) {
                    isFirstAppOutputRecord = false;
                }

                offset += fragLen;
            }
        } finally {
            recordLock.unlock();
        }
    }

    @Override
    void setDeliverStream(OutputStream outputStream) {
        recordLock.lock();
        try {
            this.deliverStream = outputStream;
        } finally {
            recordLock.unlock();
        }
    }

    /*
     * Need to split the payload except the following cases:
     *
     * 1. protocol version is TLS 1.1 or later;
     * 2. bulk cipher does not use CBC mode, including null bulk cipher suites.
     * 3. the payload is the first application record of a freshly
     *    negotiated TLS session.
     * 4. the CBC protection is disabled;
     *
     * By default, we counter chosen plaintext issues on CBC mode
     * ciphersuites in SSLv3/TLS1.0 by sending one byte of application
     * data in the first record of every payload, and the rest in
     * subsequent record(s). Note that the issues have been solved in
     * TLS 1.1 or later.
     *
     * It is not necessary to split the very first application record of
     * a freshly negotiated TLS session, as there is no previous
     * application data to guess.  To improve compatibility, we will not
     * split such records.
     *
     * This avoids issues in the outbound direction.  For a full fix,
     * the peer must have similar protections.
     */
    private boolean needToSplitPayload() {
        return (!protocolVersion.useTLS11PlusSpec()) &&
                writeCipher.isCBCMode() && !isFirstAppOutputRecord &&
                Record.enableCBCProtection;
    }

    private int getFragLimit() {
        int fragLimit;
        if (packetSize > 0) {
            fragLimit = Math.min(maxRecordSize, packetSize);
            fragLimit =
                    writeCipher.calculateFragmentSize(fragLimit, headerSize);

            fragLimit = Math.min(fragLimit, Record.maxDataSize);
        } else {
            fragLimit = Record.maxDataSize;
        }

        // Calculate more impact, for example TLS 1.3 padding.
        fragLimit = calculateFragmentSize(fragLimit);

        return fragLimit;
    }
}
