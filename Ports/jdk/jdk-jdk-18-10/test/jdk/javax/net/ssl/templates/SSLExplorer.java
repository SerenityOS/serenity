/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.io.IOException;
import javax.net.ssl.*;
import java.util.*;

/**
 * Instances of this class acts as an explorer of the network data of an
 * SSL/TLS connection.
 */
public final class SSLExplorer {

    // Private constructor prevents construction outside this class.
    private SSLExplorer() {
    }

    /**
     * The header size of TLS/SSL records.
     * <P>
     * The value of this constant is {@value}.
     */
    public final static int RECORD_HEADER_SIZE = 0x05;

    /**
     * Returns the required number of bytes in the {@code source}
     * {@link ByteBuffer} necessary to explore SSL/TLS connection.
     * <P>
     * This method tries to parse as few bytes as possible from
     * {@code source} byte buffer to get the length of an
     * SSL/TLS record.
     * <P>
     * This method accesses the {@code source} parameter in read-only
     * mode, and does not update the buffer's properties such as capacity,
     * limit, position, and mark values.
     *
     * @param  source
     *         a {@link ByteBuffer} containing
     *         inbound or outbound network data for an SSL/TLS connection.
     * @throws BufferUnderflowException if less than {@code RECORD_HEADER_SIZE}
     *         bytes remaining in {@code source}
     * @return the required size in byte to explore an SSL/TLS connection
     */
    public final static int getRequiredSize(ByteBuffer source) {

        ByteBuffer input = source.duplicate();

        // Do we have a complete header?
        if (input.remaining() < RECORD_HEADER_SIZE) {
            throw new BufferUnderflowException();
        }

        // Is it a handshake message?
        byte firstByte = input.get();
        byte secondByte = input.get();
        byte thirdByte = input.get();
        if ((firstByte & 0x80) != 0 && thirdByte == 0x01) {
            // looks like a V2ClientHello
            // return (((firstByte & 0x7F) << 8) | (secondByte & 0xFF)) + 2;
            return RECORD_HEADER_SIZE;   // Only need the header fields
        } else {
            return (((input.get() & 0xFF) << 8) | (input.get() & 0xFF)) + 5;
        }
    }

    /**
     * Returns the required number of bytes in the {@code source} byte array
     * necessary to explore SSL/TLS connection.
     * <P>
     * This method tries to parse as few bytes as possible from
     * {@code source} byte array to get the length of an
     * SSL/TLS record.
     *
     * @param  source
     *         a byte array containing inbound or outbound network data for
     *         an SSL/TLS connection.
     * @param  offset
     *         the start offset in array {@code source} at which the
     *         network data is read from.
     * @param  length
     *         the maximum number of bytes to read.
     *
     * @throws BufferUnderflowException if less than {@code RECORD_HEADER_SIZE}
     *         bytes remaining in {@code source}
     * @return the required size in byte to explore an SSL/TLS connection
     */
    public final static int getRequiredSize(byte[] source,
            int offset, int length) throws IOException {

        ByteBuffer byteBuffer =
            ByteBuffer.wrap(source, offset, length).asReadOnlyBuffer();
        return getRequiredSize(byteBuffer);
    }

    /**
     * Launch and explore the security capabilities from byte buffer.
     * <P>
     * This method tries to parse as few records as possible from
     * {@code source} byte buffer to get the {@link SSLCapabilities}
     * of an SSL/TLS connection.
     * <P>
     * Please NOTE that this method must be called before any handshaking
     * occurs.  The behavior of this method is not defined in this release
     * if the handshake has begun, or has completed.
     * <P>
     * This method accesses the {@code source} parameter in read-only
     * mode, and does not update the buffer's properties such as capacity,
     * limit, position, and mark values.
     *
     * @param  source
     *         a {@link ByteBuffer} containing
     *         inbound or outbound network data for an SSL/TLS connection.
     *
     * @throws IOException on network data error
     * @throws BufferUnderflowException if not enough source bytes available
     *         to make a complete exploration.
     *
     * @return the explored {@link SSLCapabilities} of the SSL/TLS
     *         connection
     */
    public final static SSLCapabilities explore(ByteBuffer source)
            throws IOException {

        ByteBuffer input = source.duplicate();

        // Do we have a complete header?
        if (input.remaining() < RECORD_HEADER_SIZE) {
            throw new BufferUnderflowException();
        }

        // Is it a handshake message?
        byte firstByte = input.get();
        byte secondByte = input.get();
        byte thirdByte = input.get();
        if ((firstByte & 0x80) != 0 && thirdByte == 0x01) {
            // looks like a V2ClientHello
            return exploreV2HelloRecord(input,
                                    firstByte, secondByte, thirdByte);
        } else if (firstByte == 22) {   // 22: handshake record
            return exploreTLSRecord(input,
                                    firstByte, secondByte, thirdByte);
        } else {
            throw new SSLException("Not handshake record");
        }
    }

    /**
     * Launch and explore the security capabilities from byte array.
     * <P>
     * Please NOTE that this method must be called before any handshaking
     * occurs.  The behavior of this method is not defined in this release
     * if the handshake has begun, or has completed.  Once handshake has
     * begun, or has completed, the security capabilities can not and
     * should not be launched with this method.
     *
     * @param  source
     *         a byte array containing inbound or outbound network data for
     *         an SSL/TLS connection.
     * @param  offset
     *         the start offset in array {@code source} at which the
     *         network data is read from.
     * @param  length
     *         the maximum number of bytes to read.
     *
     * @throws IOException on network data error
     * @throws BufferUnderflowException if not enough source bytes available
     *         to make a complete exploration.
     * @return the explored {@link SSLCapabilities} of the SSL/TLS
     *         connection
     *
     * @see #explore(ByteBuffer)
     */
    public final static SSLCapabilities explore(byte[] source,
            int offset, int length) throws IOException {
        ByteBuffer byteBuffer =
            ByteBuffer.wrap(source, offset, length).asReadOnlyBuffer();
        return explore(byteBuffer);
    }

    /*
     * uint8 V2CipherSpec[3];
     * struct {
     *     uint16 msg_length;         // The highest bit MUST be 1;
     *                                // the remaining bits contain the length
     *                                // of the following data in bytes.
     *     uint8 msg_type;            // MUST be 1
     *     Version version;
     *     uint16 cipher_spec_length; // It cannot be zero and MUST be a
     *                                // multiple of the V2CipherSpec length.
     *     uint16 session_id_length;  // This field MUST be empty.
     *     uint16 challenge_length;   // SHOULD use a 32-byte challenge
     *     V2CipherSpec cipher_specs[V2ClientHello.cipher_spec_length];
     *     opaque session_id[V2ClientHello.session_id_length];
     *     opaque challenge[V2ClientHello.challenge_length;
     * } V2ClientHello;
     */
    private static SSLCapabilities exploreV2HelloRecord(
            ByteBuffer input, byte firstByte, byte secondByte,
            byte thirdByte) throws IOException {

        // We only need the header. We have already had enough source bytes.
        // int recordLength = (firstByte & 0x7F) << 8) | (secondByte & 0xFF);
        try {
            // Is it a V2ClientHello?
            if (thirdByte != 0x01) {
                throw new SSLException(
                        "Unsupported or Unrecognized SSL record");
            }

            // What's the hello version?
            byte helloVersionMajor = input.get();
            byte helloVersionMinor = input.get();

            // 0x00: major version of SSLv20
            // 0x02: minor version of SSLv20
            //
            // SNIServerName is an extension, SSLv20 doesn't support extension.
            return new SSLCapabilitiesImpl((byte)0x00, (byte)0x02,
                        helloVersionMajor, helloVersionMinor,
                        Collections.<SNIServerName>emptyList());
        } catch (BufferUnderflowException bufe) {
            throw new SSLProtocolException(
                        "Invalid handshake record");
        }
    }

    /*
     * struct {
     *     uint8 major;
     *     uint8 minor;
     * } ProtocolVersion;
     *
     * enum {
     *     change_cipher_spec(20), alert(21), handshake(22),
     *     application_data(23), (255)
     * } ContentType;
     *
     * struct {
     *     ContentType type;
     *     ProtocolVersion version;
     *     uint16 length;
     *     opaque fragment[TLSPlaintext.length];
     * } TLSPlaintext;
     */
    private static SSLCapabilities exploreTLSRecord(
            ByteBuffer input, byte firstByte, byte secondByte,
            byte thirdByte) throws IOException {

        // Is it a handshake message?
        if (firstByte != 22) {        // 22: handshake record
            throw new SSLException("Not handshake record");
        }

        // We need the record version to construct SSLCapabilities.
        byte recordMajorVersion = secondByte;
        byte recordMinorVersion = thirdByte;

        // Is there enough data for a full record?
        int recordLength = getInt16(input);
        if (recordLength > input.remaining()) {
            throw new BufferUnderflowException();
        }

        // We have already had enough source bytes.
        try {
            return exploreHandshake(input,
                recordMajorVersion, recordMinorVersion, recordLength);
        } catch (BufferUnderflowException bufe) {
            throw new SSLProtocolException(
                        "Invalid handshake record");
        }
    }

    /*
     * enum {
     *     hello_request(0), client_hello(1), server_hello(2),
     *     certificate(11), server_key_exchange (12),
     *     certificate_request(13), server_hello_done(14),
     *     certificate_verify(15), client_key_exchange(16),
     *     finished(20)
     *     (255)
     * } HandshakeType;
     *
     * struct {
     *     HandshakeType msg_type;
     *     uint24 length;
     *     select (HandshakeType) {
     *         case hello_request:       HelloRequest;
     *         case client_hello:        ClientHello;
     *         case server_hello:        ServerHello;
     *         case certificate:         Certificate;
     *         case server_key_exchange: ServerKeyExchange;
     *         case certificate_request: CertificateRequest;
     *         case server_hello_done:   ServerHelloDone;
     *         case certificate_verify:  CertificateVerify;
     *         case client_key_exchange: ClientKeyExchange;
     *         case finished:            Finished;
     *     } body;
     * } Handshake;
     */
    private static SSLCapabilities exploreHandshake(
            ByteBuffer input, byte recordMajorVersion,
            byte recordMinorVersion, int recordLength) throws IOException {

        // What is the handshake type?
        byte handshakeType = input.get();
        if (handshakeType != 0x01) {   // 0x01: client_hello message
            throw new IllegalStateException("Not initial handshaking");
        }

        // What is the handshake body length?
        int handshakeLength = getInt24(input);

        // Theoretically, a single handshake message might span multiple
        // records, but in practice this does not occur.
        if (handshakeLength > (recordLength - 4)) { // 4: handshake header size
            throw new SSLException("Handshake message spans multiple records");
        }

        input = input.duplicate();
        input.limit(handshakeLength + input.position());
        return exploreClientHello(input,
                                    recordMajorVersion, recordMinorVersion);
    }

    /*
     * struct {
     *     uint32 gmt_unix_time;
     *     opaque random_bytes[28];
     * } Random;
     *
     * opaque SessionID<0..32>;
     *
     * uint8 CipherSuite[2];
     *
     * enum { null(0), (255) } CompressionMethod;
     *
     * struct {
     *     ProtocolVersion client_version;
     *     Random random;
     *     SessionID session_id;
     *     CipherSuite cipher_suites<2..2^16-2>;
     *     CompressionMethod compression_methods<1..2^8-1>;
     *     select (extensions_present) {
     *         case false:
     *             struct {};
     *         case true:
     *             Extension extensions<0..2^16-1>;
     *     };
     * } ClientHello;
     */
    private static SSLCapabilities exploreClientHello(
            ByteBuffer input,
            byte recordMajorVersion,
            byte recordMinorVersion) throws IOException {

        List<SNIServerName> snList = Collections.<SNIServerName>emptyList();

        // client version
        byte helloMajorVersion = input.get();
        byte helloMinorVersion = input.get();

        // ignore random
        int position = input.position();
        input.position(position + 32);  // 32: the length of Random

        // ignore session id
        ignoreByteVector8(input);

        // ignore cipher_suites
        ignoreByteVector16(input);

        // ignore compression methods
        ignoreByteVector8(input);

        if (input.remaining() > 0) {
            snList = exploreExtensions(input);
        }

        return new SSLCapabilitiesImpl(
                recordMajorVersion, recordMinorVersion,
                helloMajorVersion, helloMinorVersion, snList);
    }

    /*
     * struct {
     *     ExtensionType extension_type;
     *     opaque extension_data<0..2^16-1>;
     * } Extension;
     *
     * enum {
     *     server_name(0), max_fragment_length(1),
     *     client_certificate_url(2), trusted_ca_keys(3),
     *     truncated_hmac(4), status_request(5), (65535)
     * } ExtensionType;
     */
    private static List<SNIServerName> exploreExtensions(ByteBuffer input)
            throws IOException {

        int length = getInt16(input);           // length of extensions
        while (length > 0) {
            int extType = getInt16(input);      // extenson type
            int extLen = getInt16(input);       // length of extension data

            if (extType == 0x00) {      // 0x00: type of server name indication
                return exploreSNIExt(input, extLen);
            } else {                    // ignore other extensions
                ignoreByteVector(input, extLen);
            }

            length -= extLen + 4;
        }

        return Collections.<SNIServerName>emptyList();
    }

    /*
     * struct {
     *     NameType name_type;
     *     select (name_type) {
     *         case host_name: HostName;
     *     } name;
     * } ServerName;
     *
     * enum {
     *     host_name(0), (255)
     * } NameType;
     *
     * opaque HostName<1..2^16-1>;
     *
     * struct {
     *     ServerName server_name_list<1..2^16-1>
     * } ServerNameList;
     */
    private static List<SNIServerName> exploreSNIExt(ByteBuffer input,
            int extLen) throws IOException {

        Map<Integer, SNIServerName> sniMap = new LinkedHashMap<>();

        int remains = extLen;
        if (extLen >= 2) {     // "server_name" extension in ClientHello
            int listLen = getInt16(input);     // length of server_name_list
            if (listLen == 0 || listLen + 2 != extLen) {
                throw new SSLProtocolException(
                    "Invalid server name indication extension");
            }

            remains -= 2;     // 0x02: the length field of server_name_list
            while (remains > 0) {
                int code = getInt8(input);      // name_type
                int snLen = getInt16(input);    // length field of server name
                if (snLen > remains) {
                    throw new SSLProtocolException(
                        "Not enough data to fill declared vector size");
                }
                byte[] encoded = new byte[snLen];
                input.get(encoded);

                SNIServerName serverName;
                switch (code) {
                    case StandardConstants.SNI_HOST_NAME:
                        if (encoded.length == 0) {
                            throw new SSLProtocolException(
                                "Empty HostName in server name indication");
                        }
                        serverName = new SNIHostName(encoded);
                        break;
                    default:
                        serverName = new UnknownServerName(code, encoded);
                }
                // check for duplicated server name type
                if (sniMap.put(serverName.getType(), serverName) != null) {
                    throw new SSLProtocolException(
                            "Duplicated server name of type " +
                            serverName.getType());
                }

                remains -= encoded.length + 3;  // NameType: 1 byte
                                                // HostName length: 2 bytes
            }
        } else if (extLen == 0) {     // "server_name" extension in ServerHello
            throw new SSLProtocolException(
                        "Not server name indication extension in client");
        }

        if (remains != 0) {
            throw new SSLProtocolException(
                        "Invalid server name indication extension");
        }

        return Collections.<SNIServerName>unmodifiableList(
                                            new ArrayList<>(sniMap.values()));
    }

    private static int getInt8(ByteBuffer input) {
        return input.get();
    }

    private static int getInt16(ByteBuffer input) {
        return ((input.get() & 0xFF) << 8) | (input.get() & 0xFF);
    }

    private static int getInt24(ByteBuffer input) {
        return ((input.get() & 0xFF) << 16) | ((input.get() & 0xFF) << 8) |
                (input.get() & 0xFF);
    }

    private static void ignoreByteVector8(ByteBuffer input) {
        ignoreByteVector(input, getInt8(input));
    }

    private static void ignoreByteVector16(ByteBuffer input) {
        ignoreByteVector(input, getInt16(input));
    }

    private static void ignoreByteVector24(ByteBuffer input) {
        ignoreByteVector(input, getInt24(input));
    }

    private static void ignoreByteVector(ByteBuffer input, int length) {
        if (length != 0) {
            int position = input.position();
            input.position(position + length);
        }
    }

    private static class UnknownServerName extends SNIServerName {
        UnknownServerName(int code, byte[] encoded) {
            super(code, encoded);
        }
    }

    private static final class SSLCapabilitiesImpl extends SSLCapabilities {
        private final static Map<Integer, String> versionMap = new HashMap<>(5);

        private final String recordVersion;
        private final String helloVersion;
        List<SNIServerName> sniNames;

        static {
            versionMap.put(0x0002, "SSLv2Hello");
            versionMap.put(0x0300, "SSLv3");
            versionMap.put(0x0301, "TLSv1");
            versionMap.put(0x0302, "TLSv1.1");
            versionMap.put(0x0303, "TLSv1.2");
        }

        SSLCapabilitiesImpl(byte recordMajorVersion, byte recordMinorVersion,
                byte helloMajorVersion, byte helloMinorVersion,
                List<SNIServerName> sniNames) {

            int version = (recordMajorVersion << 8) | recordMinorVersion;
            this.recordVersion = versionMap.get(version) != null ?
                        versionMap.get(version) :
                        unknownVersion(recordMajorVersion, recordMinorVersion);

            version = (helloMajorVersion << 8) | helloMinorVersion;
            this.helloVersion = versionMap.get(version) != null ?
                        versionMap.get(version) :
                        unknownVersion(helloMajorVersion, helloMinorVersion);

            this.sniNames = sniNames;
        }

        @Override
        public String getRecordVersion() {
            return recordVersion;
        }

        @Override
        public String getHelloVersion() {
            return helloVersion;
        }

        @Override
        public List<SNIServerName> getServerNames() {
            if (!sniNames.isEmpty()) {
                return Collections.<SNIServerName>unmodifiableList(sniNames);
            }

            return sniNames;
        }

        private static String unknownVersion(byte major, byte minor) {
            return "Unknown-" + ((int)major) + "." + ((int)minor);
        }
    }
}

