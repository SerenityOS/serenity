/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.krb5;

import org.ietf.jgss.*;
import sun.security.jgss.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;

/**
 * This class is a base class for other token definitions that pertain to
 * per-message GSS-API calls. Conceptually GSS-API has two types of
 * per-message tokens: WrapToken and MicToken. They differ in the respect
 * that a WrapToken carries additional plaintext or ciphertext application
 * data besides just the sequence number and checksum. This class
 * encapsulates the commonality in the structure of the WrapToken and the
 * MicToken. This structure can be represented as:
 * <p>
 * <pre>
 *     0..1           TOK_ID          Identification field.
 *                                    01 01 - Mic token
 *                                    02 01 - Wrap token
 *     2..3           SGN_ALG         Checksum algorithm indicator.
 *                                    00 00 - DES MAC MD5
 *                                    01 00 - MD2.5
 *                                    02 00 - DES MAC
 *                                    04 00 - HMAC SHA1 DES3-KD
 *                                    11 00 - RC4-HMAC
 *     4..5           SEAL_ALG        ff ff - none
 *                                    00 00 - DES
 *                                    02 00 - DES3-KD
 *                                    10 00 - RC4-HMAC
 *     6..7           Filler          Contains ff ff
 *     8..15          SND_SEQ         Encrypted sequence number field.
 *     16..s+15       SGN_CKSUM       Checksum of plaintext padded data,
 *                                   calculated according to algorithm
 *                                  specified in SGN_ALG field.
 *     s+16..last     Data            encrypted or plaintext padded data
 * </pre>
 * Where "s" indicates the size of the checksum.
 * <p>
 * As always, this is preceeded by a GSSHeader.
 *
 * @author Mayank Upadhyay
 * @author Ram Marti
 * @see sun.security.jgss.GSSHeader
 */

abstract class MessageToken extends Krb5Token {
    /* Fields in header minus checksum size */
    private static final int TOKEN_NO_CKSUM_SIZE = 16;

    /**
     * Filler data as defined in the specification of the Kerberos v5 GSS-API
     * Mechanism.
     */
    private static final int FILLER = 0xffff;

     // Signing algorithm values (for the SNG_ALG field)

     // From RFC 1964
     /* Use a DES MAC MD5 checksum */
    static final int SGN_ALG_DES_MAC_MD5 = 0x0000;

     /* Use DES MAC checksum. */
    static final int SGN_ALG_DES_MAC     = 0x0200;

     // From draft-raeburn-cat-gssapi-krb5-3des-00
     /* Use a HMAC SHA1 DES3 -KD checksum */
    static final int SGN_ALG_HMAC_SHA1_DES3_KD = 0x0400;

     // Sealing algorithm values (for the SEAL_ALG field)

     // RFC 1964
    /**
     * A value for the SEAL_ALG field that indicates that no encryption was
     * used.
     */
    static final int SEAL_ALG_NONE    = 0xffff;
     /* Use DES CBC encryption algorithm. */
    static final int SEAL_ALG_DES = 0x0000;

    // From draft-raeburn-cat-gssapi-krb5-3des-00
    /**
     * Use DES3-KD sealing algorithm. (draft-raeburn-cat-gssapi-krb5-3des-00)
     * This algorithm uses triple-DES with key derivation, with a usage
     * value KG_USAGE_SEAL.  Padding is still to 8-byte multiples, and the
     * IV for encrypting application data is zero.
     */
    static final int SEAL_ALG_DES3_KD = 0x0200;

    // draft draft-brezak-win2k-krb-rc4-hmac-04.txt
    static final int SEAL_ALG_ARCFOUR_HMAC = 0x1000;
    static final int SGN_ALG_HMAC_MD5_ARCFOUR = 0x1100;

    private static final int TOKEN_ID_POS = 0;
    private static final int SIGN_ALG_POS = 2;
    private static final int SEAL_ALG_POS = 4;

    private int seqNumber;

    private boolean confState = true;
    private boolean initiator = true;

    private int tokenId = 0;
    private GSSHeader gssHeader = null;
    private MessageTokenHeader tokenHeader = null;
    private byte[] checksum = null;
    private byte[] encSeqNumber = null;
    private byte[] seqNumberData = null;

    /* cipher instance used by the corresponding GSSContext */
    CipherHelper cipherHelper = null;


    /**
     * Constructs a MessageToken from a byte array. If there are more bytes
     * in the array than needed, the extra bytes are simply ignroed.
     *
     * @param tokenId the token id that should be contained in this token as
     * it is read.
     * @param context the Kerberos context associated with this token
     * @param tokenBytes the byte array containing the token
     * @param tokenOffset the offset where the token begins
     * @param tokenLen the length of the token
     * @param prop the MessageProp structure in which the properties of the
     * token should be stored.
     * @throws GSSException if there is a problem parsing the token
     */
    MessageToken(int tokenId, Krb5Context context,
                 byte[] tokenBytes, int tokenOffset, int tokenLen,
                 MessageProp prop) throws GSSException {
        this(tokenId, context,
             new ByteArrayInputStream(tokenBytes, tokenOffset, tokenLen),
             prop);
    }

    /**
     * Constructs a MessageToken from an InputStream. Bytes will be read on
     * demand and the thread might block if there are not enough bytes to
     * complete the token.
     *
     * @param tokenId the token id that should be contained in this token as
     * it is read.
     * @param context the Kerberos context associated with this token
     * @param is the InputStream from which to read
     * @param prop the MessageProp structure in which the properties of the
     * token should be stored.
     * @throws GSSException if there is a problem reading from the
     * InputStream or parsing the token
     */
    MessageToken(int tokenId, Krb5Context context, InputStream is,
                 MessageProp prop) throws GSSException {
        init(tokenId, context);

        try {
            gssHeader = new GSSHeader(is);

            if (!gssHeader.getOid().equals(OID)) {
                throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                                       getTokenName(tokenId));
            }
            if (!confState) {
                prop.setPrivacy(false);
            }

            tokenHeader = new MessageTokenHeader(is, prop);

            encSeqNumber = new byte[8];
            readFully(is, encSeqNumber);

            // debug("\n\tRead EncSeq#=" +
            // getHexBytes(encSeqNumber, encSeqNumber.length));

            checksum = new byte[cipherHelper.getChecksumLength()];
            readFully(is, checksum);

            // debug("\n\tRead checksum=" +
            // getHexBytes(checksum, checksum.length));
            // debug("\nLeaving MessageToken.Cons\n");

        } catch (IOException e) {
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                getTokenName(tokenId) + ":" + e.getMessage());
        }
    }

    /**
     * Used to obtain the GSSHeader that was at the start of this
     * token.
     */
    public final GSSHeader getGSSHeader() {
        return gssHeader;
    }

    /**
     * Used to obtain the token id that was contained in this token.
     * @return the token id in the token
     */
    public final int getTokenId() {
        return tokenId;
    }

    /**
     * Used to obtain the encrypted sequence number in this token.
     * @return the encrypted sequence number in the token
     */
    public final byte[] getEncSeqNumber() {
        return encSeqNumber;
    }

    /**
     * Used to obtain the checksum that was contained in this token.
     * @return the checksum in the token
     */
    public final byte[] getChecksum() {
        return checksum;
    }

    /**
     * Used to determine if this token contains any encrypted data.
     * @return true if it contains any encrypted data, false if there is only
     * plaintext data or if there is no data.
     */
    public final boolean getConfState() {
        return confState;
    }

    /**
     * Generates the checksum field and the encrypted sequence number
     * field. The encrypted sequence number uses the 8 bytes of the checksum
     * as an initial vector in a fixed DesCbc algorithm.
     *
     * @param prop the MessageProp structure that determines what sort of
     * checksum and sealing algorithm should be used. The lower byte
     * of qop determines the checksum algorithm while the upper byte
     * determines the signing algorithm.
     *       Checksum values are:
     *           0 - default (DES_MAC)
     *           1 - MD5
     *           2 - DES_MD5
     *           3 - DES_MAC
     *           4 - HMAC_SHA1
     *       Sealing values are:
     *           0 - default (DES)
     *           1 - DES
     *           2 - DES3-KD
     *
     * @param optionalHeader an optional header that will be processed first
     * during  checksum calculation
     *
     * @param data the application data to checksum
     * @param offset the offset where the data starts
     * @param len the length of the data
     *
     * @param optionalTrailer an optional trailer that will be processed
     * last during checksum calculation. e.g., padding that should be
     * appended to the application data
     *
     * @throws GSSException if an error occurs in the checksum calculation or
     * encryption sequence number calculation.
     */
    public void genSignAndSeqNumber(MessageProp prop,
                                    byte[] optionalHeader,
                                    byte[] data, int offset, int len,
                                    byte[] optionalTrailer)
        throws GSSException {

        //    debug("Inside MessageToken.genSignAndSeqNumber:\n");

        int qop = prop.getQOP();
        if (qop != 0) {
            qop = 0;
            prop.setQOP(qop);
        }

        if (!confState) {
            prop.setPrivacy(false);
        }

        // Create a token header with the correct sign and seal algorithm
        // values.
        tokenHeader =
            new MessageTokenHeader(tokenId, prop.getPrivacy(), qop);

        // Calculate SGN_CKSUM

        checksum =
            getChecksum(optionalHeader, data, offset, len, optionalTrailer);

        // debug("\n\tCalc checksum=" +
        // getHexBytes(checksum, checksum.length));

        // Calculate SND_SEQ

        seqNumberData = new byte[8];

        // When using this RC4 based encryption type, the sequence number is
        // always sent in big-endian rather than little-endian order.
        if (cipherHelper.isArcFour()) {
            writeBigEndian(seqNumber, seqNumberData);
        } else {
            // for all other etypes
            writeLittleEndian(seqNumber, seqNumberData);
        }
        if (!initiator) {
            seqNumberData[4] = (byte)0xff;
            seqNumberData[5] = (byte)0xff;
            seqNumberData[6] = (byte)0xff;
            seqNumberData[7] = (byte)0xff;
        }

        encSeqNumber = cipherHelper.encryptSeq(checksum, seqNumberData, 0, 8);

        // debug("\n\tCalc seqNum=" +
        //    getHexBytes(seqNumberData, seqNumberData.length));
        // debug("\n\tCalc encSeqNum=" +
        //    getHexBytes(encSeqNumber, encSeqNumber.length));
    }

    /**
     * Verifies that the checksum field and sequence number direction bytes
     * are valid and consistent with the application data.
     *
     * @param optionalHeader an optional header that will be processed first
     * during checksum calculation.
     *
     * @param data the application data
     * @param offset the offset where the data begins
     * @param len the length of the application data
     *
     * @param optionalTrailer an optional trailer that will be processed last
     * during checksum calculation. e.g., padding that should be appended to
     * the application data
     *
     * @throws GSSException if an error occurs in the checksum calculation or
     * encryption sequence number calculation.
     */
    public final boolean verifySignAndSeqNumber(byte[] optionalHeader,
                                        byte[] data, int offset, int len,
                                        byte[] optionalTrailer)
        throws GSSException {
         // debug("\tIn verifySign:\n");

         // debug("\t\tchecksum:   [" + getHexBytes(checksum) + "]\n");

        byte[] myChecksum =
            getChecksum(optionalHeader, data, offset, len, optionalTrailer);

        // debug("\t\tmychecksum: [" + getHexBytes(myChecksum) +"]\n");
        // debug("\t\tchecksum:   [" + getHexBytes(checksum) + "]\n");

        if (MessageDigest.isEqual(checksum, myChecksum)) {

            seqNumberData = cipherHelper.decryptSeq(
                checksum, encSeqNumber, 0, 8);

            // debug("\t\tencSeqNumber:   [" + getHexBytes(encSeqNumber)
            //  + "]\n");
            // debug("\t\tseqNumberData:   [" + getHexBytes(seqNumberData)
            //  + "]\n");

            /*
             * The token from the initiator has direction bytes 0x00 and
             * the token from the acceptor has direction bytes 0xff.
             */
            byte directionByte = 0;
            if (initiator)
                directionByte = (byte) 0xff; // Received token from acceptor

            if ((seqNumberData[4] == directionByte) &&
                  (seqNumberData[5] == directionByte) &&
                  (seqNumberData[6] == directionByte) &&
                  (seqNumberData[7] == directionByte))
                return true;
        }

        return false;

    }

    public final int getSequenceNumber() {
        int sequenceNum = 0;
        if (cipherHelper.isArcFour()) {
            sequenceNum = readBigEndian(seqNumberData, 0, 4);
        } else {
            sequenceNum = readLittleEndian(seqNumberData, 0, 4);
        }
        return sequenceNum;
    }

    /**
     * Computes the checksum based on the algorithm stored in the
     * tokenHeader.
     *
     * @param optionalHeader an optional header that will be processed first
     * during checksum calculation.
     *
     * @param data the application data
     * @param offset the offset where the data begins
     * @param len the length of the application data
     *
     * @param optionalTrailer an optional trailer that will be processed last
     * during checksum calculation. e.g., padding that should be appended to
     * the application data
     *
     * @throws GSSException if an error occurs in the checksum calculation.
     */
    private byte[] getChecksum(byte[] optionalHeader,
                               byte[] data, int offset, int len,
                               byte[] optionalTrailer)
        throws GSSException {

        //      debug("Will do getChecksum:\n");

        /*
         * For checksum calculation the token header bytes i.e., the first 8
         * bytes following the GSSHeader, are logically prepended to the
         * application data to bind the data to this particular token.
         *
         * Note: There is no such requirement wrt adding padding to the
         * application data for checksumming, although the cryptographic
         * algorithm used might itself apply some padding.
         */

        byte[] tokenHeaderBytes = tokenHeader.getBytes();
        byte[] existingHeader = optionalHeader;
        byte[] checksumDataHeader = tokenHeaderBytes;

        if (existingHeader != null) {
            checksumDataHeader = new byte[tokenHeaderBytes.length +
                                         existingHeader.length];
            System.arraycopy(tokenHeaderBytes, 0,
                             checksumDataHeader, 0, tokenHeaderBytes.length);
            System.arraycopy(existingHeader, 0,
                             checksumDataHeader, tokenHeaderBytes.length,
                             existingHeader.length);
        }

        return cipherHelper.calculateChecksum(tokenHeader.getSignAlg(),
             checksumDataHeader, optionalTrailer, data, offset, len, tokenId);
    }


    /**
     * Constructs an empty MessageToken for the local context to send to
     * the peer. It also increments the local sequence number in the
     * Krb5Context instance it uses after obtaining the object lock for
     * it.
     *
     * @param tokenId the token id that should be contained in this token
     * @param context the Kerberos context associated with this token
     */
    MessageToken(int tokenId, Krb5Context context) throws GSSException {
        /*
          debug("\n============================");
          debug("\nMySessionKey=" +
          getHexBytes(context.getMySessionKey().getBytes()));
          debug("\nPeerSessionKey=" +
          getHexBytes(context.getPeerSessionKey().getBytes()));
          debug("\n============================\n");
        */
        init(tokenId, context);
        this.seqNumber = context.incrementMySequenceNumber();
    }

    private void init(int tokenId, Krb5Context context) throws GSSException {
        this.tokenId = tokenId;
        // Just for consistency check in Wrap
        this.confState = context.getConfState();

        this.initiator = context.isInitiator();

        this.cipherHelper = context.getCipherHelper(null);
        //    debug("In MessageToken.Cons");
    }

    /**
     * Encodes a GSSHeader and this token onto an OutputStream.
     *
     * @param os the OutputStream to which this should be written
     * @throws GSSException if an error occurs while writing to the OutputStream
     */
    public void encode(OutputStream os) throws IOException, GSSException {
        gssHeader = new GSSHeader(OID, getKrb5TokenSize());
        gssHeader.encode(os);
        tokenHeader.encode(os);
        // debug("Writing seqNumber: " + getHexBytes(encSeqNumber));
        os.write(encSeqNumber);
        // debug("Writing checksum: " + getHexBytes(checksum));
        os.write(checksum);
    }

    /**
     * Obtains the size of this token. Note that this excludes the size of
     * the GSSHeader.
     * @return token size
     */
    protected int getKrb5TokenSize() throws GSSException {
        return getTokenSize();
    }

    protected final int getTokenSize() throws GSSException {
        return TOKEN_NO_CKSUM_SIZE + cipherHelper.getChecksumLength();
    }

    protected static final int getTokenSize(CipherHelper ch)
        throws GSSException {
         return TOKEN_NO_CKSUM_SIZE + ch.getChecksumLength();
    }

    /**
     * Obtains the conext key that is associated with this token.
     * @return the context key
     */
    /*
    public final byte[] getContextKey() {
        return contextKey;
    }
    */

    /**
     * Obtains the encryption algorithm that should be used in this token
     * given the state of confidentiality the application requested.
     * Requested qop must be consistent with negotiated session key.
     * @param confRequested true if the application desired confidentiality
     * on this token, false otherwise
     * @param qop the qop requested by the application
     * @throws GSSException if qop is incompatible with the negotiated
     *         session key
     */
    protected abstract int getSealAlg(boolean confRequested, int qop)
        throws GSSException;

    // ******************************************* //
    //  I N N E R    C L A S S E S    F O L L O W
    // ******************************************* //

    /**
     * This inner class represents the initial portion of the message token
     * and contains information about the checksum and encryption algorithms
     * that are in use. It constitutes the first 8 bytes of the
     * message token:
     * <pre>
     *     0..1           TOK_ID          Identification field.
     *                                    01 01 - Mic token
     *                                    02 01 - Wrap token
     *     2..3           SGN_ALG         Checksum algorithm indicator.
     *                                    00 00 - DES MAC MD5
     *                                    01 00 - MD2.5
     *                                    02 00 - DES MAC
     *                                    04 00 - HMAC SHA1 DES3-KD
     *                                    11 00 - RC4-HMAC
     *     4..5           SEAL_ALG        ff ff - none
     *                                    00 00 - DES
     *                                    02 00 - DES3-KD
     *                                    10 00 - RC4-HMAC
     *     6..7           Filler          Contains ff ff
     * </pre>
     */
    class MessageTokenHeader {

         private int tokenId;
         private int signAlg;
         private int sealAlg;

         private byte[] bytes = new byte[8];

        /**
         * Constructs a MessageTokenHeader for the specified token type with
         * appropriate checksum and encryption algorithms fields.
         *
         * @param tokenId the token id for this message token
         * @param conf true if confidentiality will be resuested with this
         * message token, false otherwise.
         * @param qop the value of the quality of protection that will be
         * desired.
         */
        public MessageTokenHeader(int tokenId, boolean conf, int qop)
         throws GSSException {

            this.tokenId = tokenId;

            signAlg = MessageToken.this.getSgnAlg(qop);

            sealAlg = MessageToken.this.getSealAlg(conf, qop);

            bytes[0] = (byte) (tokenId >>> 8);
            bytes[1] = (byte) (tokenId);

            bytes[2] = (byte) (signAlg >>> 8);
            bytes[3] = (byte) (signAlg);

            bytes[4] = (byte) (sealAlg >>> 8);
            bytes[5] = (byte) (sealAlg);

            bytes[6] = (byte) (MessageToken.FILLER >>> 8);
            bytes[7] = (byte) (MessageToken.FILLER);
        }

        /**
         * Constructs a MessageTokenHeader by reading it from an InputStream
         * and sets the appropriate confidentiality and quality of protection
         * values in a MessageProp structure.
         *
         * @param is the InputStream to read from
         * @param prop the MessageProp to populate
         * @throws IOException is an error occurs while reading from the
         * InputStream
         */
        public MessageTokenHeader(InputStream is, MessageProp prop)
            throws IOException {
            readFully(is, bytes);
            tokenId = readInt(bytes, TOKEN_ID_POS);
            signAlg = readInt(bytes, SIGN_ALG_POS);
            sealAlg = readInt(bytes, SEAL_ALG_POS);
            //          debug("\nMessageTokenHeader read tokenId=" +
            //                getHexBytes(bytes) + "\n");
            // XXX compare to FILLER
            int temp = readInt(bytes, SEAL_ALG_POS + 2);

            //              debug("SIGN_ALG=" + signAlg);

            switch (sealAlg) {
            case SEAL_ALG_DES:
            case SEAL_ALG_DES3_KD:
            case SEAL_ALG_ARCFOUR_HMAC:
                prop.setPrivacy(true);
                break;

            default:
                prop.setPrivacy(false);
            }

            prop.setQOP(0);  // default
        }

        /**
         * Encodes this MessageTokenHeader onto an OutputStream
         * @param os the OutputStream to write to
         * @throws IOException is an error occurs while writing
         */
        public final void encode(OutputStream os) throws IOException {
            os.write(bytes);
        }


        /**
         * Returns the token id for the message token.
         * @return the token id
         * @see sun.security.jgss.krb5.Krb5Token#MIC_ID
         * @see sun.security.jgss.krb5.Krb5Token#WRAP_ID
         */
        public final int getTokenId() {
            return tokenId;
        }

        /**
         * Returns the sign algorithm for the message token.
         * @return the sign algorithm
         * @see sun.security.jgss.krb5.MessageToken#SIGN_DES_MAC
         * @see sun.security.jgss.krb5.MessageToken#SIGN_DES_MAC_MD5
         */
        public final int getSignAlg() {
            return signAlg;
        }

        /**
         * Returns the seal algorithm for the message token.
         * @return the seal algorithm
         * @see sun.security.jgss.krb5.MessageToken#SEAL_ALG_DES
         * @see sun.security.jgss.krb5.MessageToken#SEAL_ALG_NONE
         */
        public final int getSealAlg() {
            return sealAlg;
        }

        /**
         * Returns the bytes of this header.
         * @return 8 bytes that form this header
         */
        public final byte[] getBytes() {
            return bytes;
        }
    } // end of class MessageTokenHeader


    /**
     * Determine signing algorithm based on QOP.
     */
    protected int getSgnAlg(int qop) throws GSSException {
         // QOP ignored
         return cipherHelper.getSgnAlg();
    }
}
