/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import sun.security.ssl.CipherSuite.MacAlg;

/**
 * This class represents an SSL/TLS/DTLS message authentication token,
 * which encapsulates a sequence number and ensures that attempts to
 * delete or reorder messages can be detected.
 */
abstract class Authenticator {
    // byte array containing the additional authentication information for
    // each record
    protected final byte[] block;   // at least 8 bytes for sequence number

    private Authenticator(byte[] block) {
        this.block = block;
    }

    /**
     * Constructs the message authentication token for the specified
     * SSL/TLS protocol.
     */
    static Authenticator valueOf(ProtocolVersion protocolVersion) {
        if (protocolVersion.isDTLS) {
            if (protocolVersion.useTLS13PlusSpec()) {
                return new DTLS13Authenticator(protocolVersion);
            } else {
                return new DTLS10Authenticator(protocolVersion);
            }
        } else {
            if (protocolVersion.useTLS13PlusSpec()) {
                return new TLS13Authenticator(protocolVersion);
            } else if (protocolVersion.useTLS10PlusSpec()) {
                return new TLS10Authenticator(protocolVersion);
            } else {
                return new SSL30Authenticator();
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    static <T extends Authenticator & MAC> T
         valueOf(ProtocolVersion protocolVersion, MacAlg macAlg,
                 SecretKey key) throws NoSuchAlgorithmException,
                        InvalidKeyException {
        if (protocolVersion.isDTLS) {
            if (protocolVersion.useTLS13PlusSpec()) {
                throw new RuntimeException("No MacAlg used in DTLS 1.3");
            } else {
                return (T)(new DTLS10Mac(protocolVersion, macAlg, key));
            }
        } else {
            if (protocolVersion.useTLS13PlusSpec()) {
                throw new RuntimeException("No MacAlg used in TLS 1.3");
            } else if (protocolVersion.useTLS10PlusSpec()) {
                return (T)(new TLS10Mac(protocolVersion, macAlg, key));
            } else {
                return (T)(new SSL30Mac(protocolVersion, macAlg, key));
            }
        }
    }

    static Authenticator nullTlsMac() {
        return new SSLNullMac();
    }

    static Authenticator nullDtlsMac() {
        return new DTLSNullMac();
    }

    /**
     * Checks whether the sequence number is close to wrap.
     *
     * Sequence numbers are of type uint64 and may not exceed 2^64-1.
     * Sequence numbers do not wrap. When the sequence number is near
     * to wrap, we need to close the connection immediately.
     *
     * @return true if the sequence number is close to wrap
     */
    abstract boolean seqNumOverflow();

    /**
     * Checks whether the sequence number close to renew.
     *
     * Sequence numbers are of type uint64 and may not exceed 2^64-1.
     * Sequence numbers do not wrap.  If a TLS
     * implementation would need to wrap a sequence number, it must
     * renegotiate instead.
     *
     * @return true if the sequence number is huge enough to renew
     */
    abstract boolean seqNumIsHuge();

    /**
     * Gets the current sequence number, including the epoch number for
     * DTLS protocols.
     *
     * @return the byte array of the current sequence number
     */
    final byte[] sequenceNumber() {
        return Arrays.copyOf(block, 8);
    }

    /**
     * Sets the epoch number (only apply to DTLS protocols).
     */
    void setEpochNumber(int epoch) {
        throw new UnsupportedOperationException(
                "Epoch numbers apply to DTLS protocols only");
    }

    /**
     * Increase the sequence number.
     */
    final void increaseSequenceNumber() {
        /*
         * The sequence number in the block array is a 64-bit
         * number stored in big-endian format.
         */
        int k = 7;
        while ((k >= 0) && (++block[k] == 0)) {
            k--;
        }
    }

    /**
     * Acquires the current message authentication information with the
     * specified record type and fragment length, and then increases the
     * sequence number if using implicit sequence number.
     *
     * @param  type the record type
     * @param  length the fragment of the record
     * @param  sequence the explicit sequence number of the record
     *
     * @return the byte array of the current message authentication information
     */
    byte[] acquireAuthenticationBytes(
            byte type, int length, byte[] sequence) {
        throw new UnsupportedOperationException("Used by AEAD algorithms only");
    }

    private static class SSLAuthenticator extends Authenticator {
        private SSLAuthenticator(byte[] block) {
            super(block);
        }

        @Override
        boolean seqNumOverflow() {
            /*
             * Conservatively, we don't allow more records to be generated
             * when there are only 2^8 sequence numbers left.
             */
            return (block.length != 0 &&
                block[0] == (byte)0xFF && block[1] == (byte)0xFF &&
                block[2] == (byte)0xFF && block[3] == (byte)0xFF &&
                block[4] == (byte)0xFF && block[5] == (byte)0xFF &&
                block[6] == (byte)0xFF);
        }

        @Override
        boolean seqNumIsHuge() {
            return (block.length != 0 &&
                block[0] == (byte)0xFF && block[1] == (byte)0xFF &&
                block[2] == (byte)0xFF && block[3] == (byte)0xFF);
        }
    }

    // For null MAC only.
    private static class SSLNullAuthenticator extends SSLAuthenticator {
        private SSLNullAuthenticator() {
            super(new byte[8]);
        }
    }

    // For SSL 3.0
    private static class SSL30Authenticator extends SSLAuthenticator {
        // Block size of SSL v3.0:
        //     sequence number + record type + + record length
        private static final int BLOCK_SIZE = 11;   // 8 + 1 + 2

        private SSL30Authenticator() {
            super(new byte[BLOCK_SIZE]);
        }

        @Override
        byte[] acquireAuthenticationBytes(
                byte type, int length, byte[] sequence) {
            byte[] ad = block.clone();

            // Increase the implicit sequence number in the block array.
            increaseSequenceNumber();

            ad[8] = type;
            ad[9] = (byte)(length >> 8);
            ad[10] = (byte)(length);

            return ad;
        }
    }

    // For TLS 1.0 - 1.2
    private static class TLS10Authenticator extends SSLAuthenticator {
        // Block size of TLS v1.0/1.1/1.2.
        //     sequence number + record type + protocol version + record length
        private static final int BLOCK_SIZE = 13;   // 8 + 1 + 2 + 2

        private TLS10Authenticator(ProtocolVersion protocolVersion) {
            super(new byte[BLOCK_SIZE]);
            block[9] = protocolVersion.major;
            block[10] = protocolVersion.minor;
        }

        @Override
        byte[] acquireAuthenticationBytes(
                byte type, int length, byte[] sequence) {
            byte[] ad = block.clone();
            if (sequence != null) {
                if (sequence.length != 8) {
                    throw new RuntimeException(
                            "Insufficient explicit sequence number bytes");
                }

                System.arraycopy(sequence, 0, ad, 0, sequence.length);
            } else {    // Otherwise, use the implicit sequence number.
                // Increase the implicit sequence number in the block array.
                increaseSequenceNumber();
            }

            ad[8] = type;
            ad[11] = (byte)(length >> 8);
            ad[12] = (byte)(length);

            return ad;
        }
    }

    // For TLS 1.3
    private static final class TLS13Authenticator extends SSLAuthenticator {
        // Block size of TLS v1.3:
        //     record type + protocol version + record length + sequence number
        private static final int BLOCK_SIZE = 13;   // 1 + 2 + 2 + 8

        private TLS13Authenticator(ProtocolVersion protocolVersion) {
            super(new byte[BLOCK_SIZE]);
            block[9] = ProtocolVersion.TLS12.major;
            block[10] = ProtocolVersion.TLS12.minor;
        }

        @Override
        byte[] acquireAuthenticationBytes(
                byte type, int length, byte[] sequence) {
            byte[] ad = Arrays.copyOfRange(block, 8, 13);

            // Increase the implicit sequence number in the block array.
            increaseSequenceNumber();

            ad[0] = type;
            ad[3] = (byte)(length >> 8);
            ad[4] = (byte)(length & 0xFF);

            return ad;
        }
    }

    private static class DTLSAuthenticator extends Authenticator {
        private DTLSAuthenticator(byte[] block) {
            super(block);
        }

        @Override
        boolean seqNumOverflow() {
            /*
             * Conservatively, we don't allow more records to be generated
             * when there are only 2^8 sequence numbers left.
             */
            return (block.length != 0 &&
                // no epoch bytes, block[0] and block[1]
                block[2] == (byte)0xFF && block[3] == (byte)0xFF &&
                block[4] == (byte)0xFF && block[5] == (byte)0xFF &&
                block[6] == (byte)0xFF);
        }

        @Override
        boolean seqNumIsHuge() {
            return (block.length != 0 &&
                // no epoch bytes, block[0] and block[1]
                block[2] == (byte)0xFF && block[3] == (byte)0xFF);
        }

        @Override
        void setEpochNumber(int epoch) {
            block[0] = (byte)((epoch >> 8) & 0xFF);
            block[1] = (byte)(epoch & 0xFF);
        }
    }

    // For null MAC only.
    private static class DTLSNullAuthenticator extends DTLSAuthenticator {
        private DTLSNullAuthenticator() {
            // For DTLS protocols, plaintexts use explicit epoch and
            // sequence number in each record.  The first 8 byte of
            // the block is initialized for null MAC so that the
            // epoch and sequence number can be acquired to generate
            // plaintext records.
            super(new byte[8]);
        }
    }

    // DTLS 1.0/1.2
    private static class DTLS10Authenticator extends DTLSAuthenticator {
        // Block size of DTLS v1.0 and later:
        //     epoch + sequence number +
        //     record type + protocol version + record length
        private static final int BLOCK_SIZE = 13;  // 2 + 6 + 1 + 2 + 2;

        private DTLS10Authenticator(ProtocolVersion protocolVersion) {
            super(new byte[BLOCK_SIZE]);
            block[9] = protocolVersion.major;
            block[10] = protocolVersion.minor;
        }

        @Override
        byte[] acquireAuthenticationBytes(
                byte type, int length, byte[] sequence) {
            byte[] ad = block.clone();
            if (sequence != null) {
                if (sequence.length != 8) {
                    throw new RuntimeException(
                            "Insufficient explicit sequence number bytes");
                }

                System.arraycopy(sequence, 0, ad, 0, sequence.length);
            } else {    // Otherwise, use the implicit sequence number.
                // Increase the implicit sequence number in the block array.
                increaseSequenceNumber();
            }

            ad[8] = type;
            ad[11] = (byte)(length >> 8);
            ad[12] = (byte)(length);

            return ad;
        }
    }

    // DTLS 1.3
    private static final class DTLS13Authenticator extends DTLSAuthenticator {
        // Block size of DTLS v1.0 and later:
        //     epoch + sequence number +
        //     record type + protocol version + record length
        private static final int BLOCK_SIZE = 13;  // 2 + 6 + 1 + 2 + 2;

        private DTLS13Authenticator(ProtocolVersion protocolVersion) {
            super(new byte[BLOCK_SIZE]);
            block[9] = ProtocolVersion.TLS12.major;
            block[10] = ProtocolVersion.TLS12.minor;
        }

        @Override
        byte[] acquireAuthenticationBytes(
                byte type, int length, byte[] sequence) {
            byte[] ad = Arrays.copyOfRange(block, 8, 13);

            // Increase the implicit sequence number in the block array.
            increaseSequenceNumber();

            ad[0] = type;
            ad[3] = (byte)(length >> 8);
            ad[4] = (byte)(length & 0xFF);

            return ad;
        }
    }

    interface MAC {
        MacAlg macAlg();

        /**
         * Compute and returns the MAC for the remaining data
         * in this ByteBuffer.
         *
         * On return, the bb position == limit, and limit will
         * have not changed.
         *
         * @param type record type
         * @param bb a ByteBuffer in which the position and limit
         *          demarcate the data to be MAC'd.
         * @param isSimulated if true, simulate the MAC computation
         * @param sequence the explicit sequence number, or null if using
         *        the implicit sequence number for the computation
         *
         * @return the MAC result
         */
        byte[] compute(byte type, ByteBuffer bb,
                byte[] sequence, boolean isSimulated);


        /**
         * Compute and returns the MAC for the remaining data
         * in this ByteBuffer.
         *
         * On return, the bb position == limit, and limit will
         * have not changed.
         *
         * @param type record type
         * @param bb a ByteBuffer in which the position and limit
         *        demarcate the data to be MAC'd.
         * @param isSimulated if true, simulate the MAC computation
         *
         * @return the MAC result
         */
        default byte[] compute(byte type, ByteBuffer bb, boolean isSimulated) {
            return compute(type, bb, null, isSimulated);
        }
    }

    private class MacImpl implements MAC {
        // internal identifier for the MAC algorithm
        private final MacAlg macAlg;

        // JCE Mac object
        private final Mac mac;

        private MacImpl() {
            macAlg = MacAlg.M_NULL;
            mac = null;
        }

        private MacImpl(ProtocolVersion protocolVersion, MacAlg macAlg,
                SecretKey key) throws NoSuchAlgorithmException,
                        InvalidKeyException {
            if (macAlg == null) {
                throw new RuntimeException("Null MacAlg");
            }

            // using SSL MAC computation?
            boolean useSSLMac = (protocolVersion.id < ProtocolVersion.TLS10.id);
            String algorithm;
            switch (macAlg) {
                case M_MD5:
                    algorithm = useSSLMac ? "SslMacMD5" : "HmacMD5";
                    break;
                case M_SHA:
                    algorithm = useSSLMac ? "SslMacSHA1" : "HmacSHA1";
                    break;
                case M_SHA256:
                    algorithm = "HmacSHA256";    // TLS 1.2+
                    break;
                case M_SHA384:
                    algorithm = "HmacSHA384";    // TLS 1.2+
                    break;
                default:
                    throw new RuntimeException("Unknown MacAlg " + macAlg);
            }

            Mac m = Mac.getInstance(algorithm);
            m.init(key);
            this.macAlg = macAlg;
            this.mac = m;
        }

        @Override
        public MacAlg macAlg() {
            return macAlg;
        }

        @Override
        public byte[] compute(byte type, ByteBuffer bb,
                byte[] sequence, boolean isSimulated) {

            if (macAlg.size == 0) {
                return new byte[0];
            }

            if (!isSimulated) {
                // Uses the explicit sequence number for the computation.
                byte[] additional =
                    acquireAuthenticationBytes(type, bb.remaining(), sequence);
                mac.update(additional);
            }
            mac.update(bb);

            return mac.doFinal();
        }
    }

    // NULL SSL MAC
    private static final
            class SSLNullMac extends SSLNullAuthenticator implements MAC {
        private final MacImpl macImpl;
        public SSLNullMac() {
            super();
            this.macImpl = new MacImpl();
        }

        @Override
        public MacAlg macAlg() {
            return macImpl.macAlg;
        }

        @Override
        public byte[] compute(byte type, ByteBuffer bb,
                byte[] sequence, boolean isSimulated) {
            return macImpl.compute(type, bb, sequence, isSimulated);
        }
    }

    // For SSL 3.0
    private static final
            class SSL30Mac extends SSL30Authenticator implements MAC {
        private final MacImpl macImpl;
        public SSL30Mac(ProtocolVersion protocolVersion,
                MacAlg macAlg, SecretKey key) throws NoSuchAlgorithmException,
                        InvalidKeyException {
            super();
            this.macImpl = new MacImpl(protocolVersion, macAlg, key);
        }

        @Override
        public MacAlg macAlg() {
            return macImpl.macAlg;
        }

        @Override
        public byte[] compute(byte type, ByteBuffer bb,
                byte[] sequence, boolean isSimulated) {
            return macImpl.compute(type, bb, sequence, isSimulated);
        }
    }

    // For TLS 1.0 - 1.2
    private static final
            class TLS10Mac extends TLS10Authenticator implements MAC {
        private final MacImpl macImpl;
        public TLS10Mac(ProtocolVersion protocolVersion,
                MacAlg macAlg, SecretKey key) throws NoSuchAlgorithmException,
                        InvalidKeyException {
            super(protocolVersion);
            this.macImpl = new MacImpl(protocolVersion, macAlg, key);
        }

        @Override
        public MacAlg macAlg() {
            return macImpl.macAlg;
        }

        @Override
        public byte[] compute(byte type, ByteBuffer bb,
                byte[] sequence, boolean isSimulated) {
            return macImpl.compute(type, bb, sequence, isSimulated);
        }
    }

    // NULL DTLS MAC
    private static final
            class DTLSNullMac extends DTLSNullAuthenticator implements MAC {
        private final MacImpl macImpl;
        public DTLSNullMac() {
            super();
            this.macImpl = new MacImpl();
        }

        @Override
        public MacAlg macAlg() {
            return macImpl.macAlg;
        }

        @Override
        public byte[] compute(byte type, ByteBuffer bb,
                byte[] sequence, boolean isSimulated) {
            return macImpl.compute(type, bb, sequence, isSimulated);
        }
    }

    // DTLS 1.0/1.2
    private static final class DTLS10Mac
            extends DTLS10Authenticator implements MAC {
        private final MacImpl macImpl;
        public DTLS10Mac(ProtocolVersion protocolVersion,
                MacAlg macAlg, SecretKey key) throws NoSuchAlgorithmException,
                        InvalidKeyException {
            super(protocolVersion);
            this.macImpl = new MacImpl(protocolVersion, macAlg, key);
        }

        @Override
        public MacAlg macAlg() {
            return macImpl.macAlg;
        }

        @Override
        public byte[] compute(byte type, ByteBuffer bb,
                byte[] sequence, boolean isSimulated) {
            return macImpl.compute(type, bb, sequence, isSimulated);
        }
    }

    static final long toLong(byte[] recordEnS) {
        if (recordEnS != null && recordEnS.length == 8) {
            return ((recordEnS[0] & 0xFFL) << 56) |
                   ((recordEnS[1] & 0xFFL) << 48) |
                   ((recordEnS[2] & 0xFFL) << 40) |
                   ((recordEnS[3] & 0xFFL) << 32) |
                   ((recordEnS[4] & 0xFFL) << 24) |
                   ((recordEnS[5] & 0xFFL) << 16) |
                   ((recordEnS[6] & 0xFFL) <<  8) |
                    (recordEnS[7] & 0xFFL);
        }

        return -1L;
    }
}
