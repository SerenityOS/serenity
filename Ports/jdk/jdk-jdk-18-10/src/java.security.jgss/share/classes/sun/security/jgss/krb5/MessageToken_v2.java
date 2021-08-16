/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * This class is a base class for new GSS token definitions, as defined
 * in RFC 4121, that pertain to per-message GSS-API calls. Conceptually
 * GSS-API has two types of per-message tokens: WrapToken and MicToken.
 * They differ in the respect that a WrapToken carries additional plaintext
 * or ciphertext application data besides just the sequence number and
 * checksum. This class encapsulates the commonality in the structure of
 * the WrapToken and the MicToken. This structure can be represented as:
 * <p>
 * <pre>
 * Wrap Tokens
 *
 *     Octet no   Name        Description
 *    ---------------------------------------------------------------
 *      0..1     TOK_ID     Identification field.  Tokens emitted by
 *                          GSS_Wrap() contain the hex value 05 04
 *                          expressed in big-endian order in this field.
 *      2        Flags      Attributes field, as described in section
 *                          4.2.2.
 *      3        Filler     Contains the hex value FF.
 *      4..5     EC         Contains the "extra count" field, in big-
 *                          endian order as described in section 4.2.3.
 *      6..7     RRC        Contains the "right rotation count" in big
 *                          endian order, as described in section 4.2.5.
 *      8..15    SND_SEQ    Sequence number field in clear text,
 *                          expressed in big-endian order.
 *      16..last Data       Encrypted data for Wrap tokens with
 *                          confidentiality, or plaintext data followed
 *                          by the checksum for Wrap tokens without
 *                          confidentiality, as described in section
 *                          4.2.4.
 * MIC Tokens
 *
 *     Octet no   Name        Description
 *     -----------------------------------------------------------------
 *      0..1     TOK_ID     Identification field.  Tokens emitted by
 *                          GSS_GetMIC() contain the hex value 04 04
 *                          expressed in big-endian order in this field.
 *      2        Flags      Attributes field, as described in section
 *                          4.2.2.
 *      3..7     Filler     Contains five octets of hex value FF.
 *      8..15    SND_SEQ    Sequence number field in clear text,
 *                          expressed in big-endian order.
 *      16..last SGN_CKSUM  Checksum of the "to-be-signed" data and
 *                          octet 0..15, as described in section 4.2.4.
 *
 * </pre>
 * <p>
 * This class is the super class of WrapToken_v2 and MicToken_v2. The token's
 * header (bytes[0..15]) and data (byte[16..]) are saved in tokenHeader and
 * tokenData fields. Since there is no easy way to find out the exact length
 * of a WrapToken_v2 token from any header info, in the case of reading from
 * stream, we read all available() bytes into the token.
 * <p>
 * All read actions are performed in this super class. On the write part, the
 * super class only write the tokenHeader, and the content writing is inside
 * child classes.
 *
 * @author Seema Malkani
 */

abstract class MessageToken_v2 extends Krb5Token {

    protected static final int TOKEN_HEADER_SIZE = 16;
    private static final int TOKEN_ID_POS = 0;
    private static final int TOKEN_FLAG_POS = 2;
    private static final int TOKEN_EC_POS = 4;
    private static final int TOKEN_RRC_POS = 6;

    /**
     * The size of the random confounder used in a WrapToken.
     */
    protected static final int CONFOUNDER_SIZE = 16;

    // RFC 4121, key usage values
    static final int KG_USAGE_ACCEPTOR_SEAL = 22;
    static final int KG_USAGE_ACCEPTOR_SIGN = 23;
    static final int KG_USAGE_INITIATOR_SEAL = 24;
    static final int KG_USAGE_INITIATOR_SIGN = 25;

    // RFC 4121, Flags Field
    private static final int FLAG_SENDER_IS_ACCEPTOR = 1;
    private static final int FLAG_WRAP_CONFIDENTIAL  = 2;
    private static final int FLAG_ACCEPTOR_SUBKEY    = 4;
    private static final int FILLER = 0xff;

    private MessageTokenHeader tokenHeader = null;

    // Common field
    private int tokenId = 0;
    private int seqNumber;
    protected byte[] tokenData; // content of token, without the header
    protected int tokenDataLen;

    // Key usage number for crypto action
    private int key_usage = 0;

    // EC and RRC fields, WrapToken only
    private int ec = 0;
    private int rrc = 0;

    // Checksum. Always in MicToken, might be in WrapToken
    byte[] checksum = null;

    // Context properties
    private boolean confState = true;
    private boolean initiator = true;
    private boolean have_acceptor_subkey = false;

    /* cipher instance used by the corresponding GSSContext */
    CipherHelper cipherHelper = null;

    /**
     * Constructs a MessageToken from a byte array.
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
    MessageToken_v2(int tokenId, Krb5Context context,
                 byte[] tokenBytes, int tokenOffset, int tokenLen,
                 MessageProp prop) throws GSSException {
        this(tokenId, context,
             new ByteArrayInputStream(tokenBytes, tokenOffset, tokenLen),
             prop);
    }

    /**
     * Constructs a MessageToken from an InputStream. Bytes will be read on
     * demand and the thread might block if there are not enough bytes to
     * complete the token. Please note there is no accurate way to find out
     * the size of a token, but we try our best to make sure there is
     * enough bytes to construct one.
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
    MessageToken_v2(int tokenId, Krb5Context context, InputStream is,
                 MessageProp prop) throws GSSException {
        init(tokenId, context);

        try {
            if (!confState) {
                prop.setPrivacy(false);
            }
            tokenHeader = new MessageTokenHeader(is, prop, tokenId);

            // set key_usage
            if (tokenId == Krb5Token.WRAP_ID_v2) {
                key_usage = (!initiator ? KG_USAGE_INITIATOR_SEAL
                                : KG_USAGE_ACCEPTOR_SEAL);
            } else if (tokenId == Krb5Token.MIC_ID_v2) {
                key_usage = (!initiator ? KG_USAGE_INITIATOR_SIGN
                                : KG_USAGE_ACCEPTOR_SIGN);
            }

            int minSize = 0;    // minimal size for token data
            if (tokenId == Krb5Token.WRAP_ID_v2 && prop.getPrivacy()) {
                minSize = CONFOUNDER_SIZE +
                        TOKEN_HEADER_SIZE + cipherHelper.getChecksumLength();
            } else {
                minSize = cipherHelper.getChecksumLength();
            }

            // Read token data
            if (tokenId == Krb5Token.MIC_ID_v2) {
                // The only case we can precisely predict the token data length
                tokenDataLen = minSize;
                tokenData = new byte[minSize];
                readFully(is, tokenData);
            } else {
                tokenDataLen = is.available();
                if (tokenDataLen >= minSize) {  // read in one shot
                    tokenData = new byte[tokenDataLen];
                    readFully(is, tokenData);
                } else {
                    byte[] tmp = new byte[minSize];
                    readFully(is, tmp);
                    // Hope while blocked in the read above, more data would
                    // come and is.available() below contains the whole token.
                    int more = is.available();
                    tokenDataLen = minSize + more;
                    tokenData = Arrays.copyOf(tmp, tokenDataLen);
                    readFully(is, tokenData, minSize, more);
                }
            }

            if (tokenId == Krb5Token.WRAP_ID_v2) {
                rotate();
            }

            if (tokenId == Krb5Token.MIC_ID_v2 ||
                    (tokenId == Krb5Token.WRAP_ID_v2 && !prop.getPrivacy())) {
                // Read checksum
                int chkLen = cipherHelper.getChecksumLength();
                checksum = new byte[chkLen];
                System.arraycopy(tokenData, tokenDataLen-chkLen,
                        checksum, 0, chkLen);

                // validate EC for Wrap tokens without confidentiality
                if (tokenId == Krb5Token.WRAP_ID_v2 && !prop.getPrivacy()) {
                    if (chkLen != ec) {
                        throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                            getTokenName(tokenId) + ":" + "EC incorrect!");
                    }
                }
            }
        } catch (IOException e) {
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                getTokenName(tokenId) + ":" + e.getMessage());
        }
    }

    /**
     * Used to obtain the token id that was contained in this token.
     * @return the token id in the token
     */
    public final int getTokenId() {
        return tokenId;
    }

    /**
     * Used to obtain the key_usage type for this token.
     * @return the key_usage for the token
     */
    public final int getKeyUsage() {
        return key_usage;
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
     * Generates the checksum field and the sequence number field.
     *
     * @param prop the MessageProp structure
     * @param data the application data to checksum
     * @param offset the offset where the data starts
     * @param len the length of the data
     *
     * @throws GSSException if an error occurs in the checksum calculation or
     * sequence number calculation.
     */
    public void genSignAndSeqNumber(MessageProp prop,
                                    byte[] data, int offset, int len)
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

        // Create a new gss token header as defined in RFC 4121
        tokenHeader = new MessageTokenHeader(tokenId, prop.getPrivacy());
        // debug("\n\t Message Header = " +
        // getHexBytes(tokenHeader.getBytes(), tokenHeader.getBytes().length));

        // set key_usage
        if (tokenId == Krb5Token.WRAP_ID_v2) {
            key_usage = (initiator ? KG_USAGE_INITIATOR_SEAL
                                : KG_USAGE_ACCEPTOR_SEAL);
        } else if (tokenId == Krb5Token.MIC_ID_v2) {
            key_usage = (initiator ? KG_USAGE_INITIATOR_SIGN
                                : KG_USAGE_ACCEPTOR_SIGN);
        }

        // Calculate SGN_CKSUM
        if ((tokenId == MIC_ID_v2) ||
            (!prop.getPrivacy() && (tokenId == WRAP_ID_v2))) {
           checksum = getChecksum(data, offset, len);
           // debug("\n\tCalc checksum=" +
           //  getHexBytes(checksum, checksum.length));
        }

        // In Wrap tokens without confidentiality, the EC field SHALL be used
        // to encode the number of octets in the trailing checksum
        if (!prop.getPrivacy() && (tokenId == WRAP_ID_v2)) {
            byte[] tok_header = tokenHeader.getBytes();
            tok_header[4] = (byte) (checksum.length >>> 8);
            tok_header[5] = (byte) (checksum.length);
        }
    }

    /**
     * Verifies the validity of checksum field
     *
     * @param data the application data
     * @param offset the offset where the data begins
     * @param len the length of the application data
     *
     * @throws GSSException if an error occurs in the checksum calculation
     */
    public final boolean verifySign(byte[] data, int offset, int len)
        throws GSSException {

        // debug("\t====In verifySign:====\n");
        // debug("\t\t checksum:   [" + getHexBytes(checksum) + "]\n");
        // debug("\t\t data = [" + getHexBytes(data) + "]\n");

        byte[] myChecksum = getChecksum(data, offset, len);
        // debug("\t\t mychecksum: [" + getHexBytes(myChecksum) +"]\n");

        if (MessageDigest.isEqual(checksum, myChecksum)) {
            // debug("\t\t====Checksum PASS:====\n");
            return true;
        }
        return false;
    }

    /**
     * Rotate bytes as per the "RRC" (Right Rotation Count) received.
     * Our implementation does not do any rotates when sending, only
     * when receiving, we rotate left as per the RRC count, to revert it.
     */
    private void rotate() {
        if (rrc % tokenDataLen != 0) {
           rrc = rrc % tokenDataLen;
           byte[] newBytes = new byte[tokenDataLen];

           System.arraycopy(tokenData, rrc, newBytes, 0, tokenDataLen-rrc);
           System.arraycopy(tokenData, 0, newBytes, tokenDataLen-rrc, rrc);

           tokenData = newBytes;
        }
    }

    public final int getSequenceNumber() {
        return seqNumber;
    }

    /**
     * Computes the checksum based on the algorithm stored in the
     * tokenHeader.
     *
     * @param data the application data
     * @param offset the offset where the data begins
     * @param len the length of the application data
     *
     * @throws GSSException if an error occurs in the checksum calculation.
     */
    byte[] getChecksum(byte[] data, int offset, int len)
        throws GSSException {

        //      debug("Will do getChecksum:\n");

        /*
         * For checksum calculation the token header bytes i.e., the first 16
         * bytes following the GSSHeader, are logically prepended to the
         * application data to bind the data to this particular token.
         *
         * Note: There is no such requirement wrt adding padding to the
         * application data for checksumming, although the cryptographic
         * algorithm used might itself apply some padding.
         */

        byte[] tokenHeaderBytes = tokenHeader.getBytes();

        // check confidentiality
        int conf_flag = tokenHeaderBytes[TOKEN_FLAG_POS] &
                                FLAG_WRAP_CONFIDENTIAL;

        // clear EC and RRC in token header for checksum calculation
        if ((conf_flag == 0) && (tokenId == WRAP_ID_v2)) {
            tokenHeaderBytes[4] = 0;
            tokenHeaderBytes[5] = 0;
            tokenHeaderBytes[6] = 0;
            tokenHeaderBytes[7] = 0;
        }
        return cipherHelper.calculateChecksum(tokenHeaderBytes, data,
                                                offset, len, key_usage);
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
    MessageToken_v2(int tokenId, Krb5Context context) throws GSSException {
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

        this.have_acceptor_subkey = context.getKeySrc() == Krb5Context.ACCEPTOR_SUBKEY;

        this.cipherHelper = context.getCipherHelper(null);
        //    debug("In MessageToken.Cons");
    }

    /**
     * Encodes a MessageTokenHeader onto an OutputStream.
     *
     * @param os the OutputStream to which this should be written
     * @throws IOException is an error occurs while writing to the OutputStream
     */
    protected void encodeHeader(OutputStream os) throws IOException {
        tokenHeader.encode(os);
    }

    /**
     * Encodes a MessageToken_v2 onto an OutputStream.
     *
     * @param os the OutputStream to which this should be written
     * @throws IOException is an error occurs while encoding the token
     */
    public abstract void encode(OutputStream os) throws IOException;

    protected final byte[] getTokenHeader() {
        return (tokenHeader.getBytes());
    }

    // ******************************************* //
    //  I N N E R    C L A S S E S    F O L L O W
    // ******************************************* //

    /**
     * This inner class represents the initial portion of the message token.
     * It constitutes the first 16 bytes of the message token.
     */
    class MessageTokenHeader {

        private int tokenId;
        private byte[] bytes = new byte[TOKEN_HEADER_SIZE];

        // Writes a new token header
        public MessageTokenHeader(int tokenId, boolean conf) throws GSSException {

            this.tokenId = tokenId;

            bytes[0] = (byte) (tokenId >>> 8);
            bytes[1] = (byte) (tokenId);

            // Flags (Note: MIT impl requires subkey)
            int flags = 0;
            flags = (initiator ? 0 : FLAG_SENDER_IS_ACCEPTOR) |
                     ((conf && tokenId != MIC_ID_v2) ?
                                FLAG_WRAP_CONFIDENTIAL : 0) |
                     (have_acceptor_subkey ? FLAG_ACCEPTOR_SUBKEY : 0);
            bytes[2] = (byte) flags;

            // filler
            bytes[3] = (byte) FILLER;

            if (tokenId == WRAP_ID_v2) {
                // EC field
                bytes[4] = (byte) 0;
                bytes[5] = (byte) 0;
                // RRC field
                bytes[6] = (byte) 0;
                bytes[7] = (byte) 0;
            } else if (tokenId == MIC_ID_v2) {
                // more filler for MicToken
                for (int i = 4; i < 8; i++) {
                    bytes[i] = (byte) FILLER;
                }
            }

            // Calculate SND_SEQ, only write 4 bytes from the 12th position
            writeBigEndian(seqNumber, bytes, 12);
        }

        /**
         * Reads a MessageTokenHeader from an InputStream and sets the
         * appropriate confidentiality and quality of protection
         * values in a MessageProp structure.
         *
         * @param is the InputStream to read from
         * @param prop the MessageProp to populate
         * @throws IOException is an error occurs while reading from the
         * InputStream
         */
        public MessageTokenHeader(InputStream is, MessageProp prop, int tokId)
            throws IOException, GSSException {

            readFully(is, bytes, 0, TOKEN_HEADER_SIZE);
            tokenId = readInt(bytes, TOKEN_ID_POS);

            // validate Token ID
            if (tokenId != tokId) {
                throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                    getTokenName(tokenId) + ":" + "Defective Token ID!");
            }

            /*
             * Validate new GSS TokenHeader
             */

            // valid acceptor_flag
            // If I am initiator, the received token should have ACCEPTOR on
            int acceptor_flag = (initiator ? FLAG_SENDER_IS_ACCEPTOR : 0);
            int flag = bytes[TOKEN_FLAG_POS] & FLAG_SENDER_IS_ACCEPTOR;
            if (flag != acceptor_flag) {
                throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                        getTokenName(tokenId) + ":" + "Acceptor Flag Error!");
            }

            // check for confidentiality
            int conf_flag = bytes[TOKEN_FLAG_POS] & FLAG_WRAP_CONFIDENTIAL;
            if ((conf_flag == FLAG_WRAP_CONFIDENTIAL) &&
                (tokenId == WRAP_ID_v2)) {
                prop.setPrivacy(true);
            } else {
                prop.setPrivacy(false);
            }

            if (tokenId == WRAP_ID_v2) {
                // validate filler
                if ((bytes[3] & 0xff) != FILLER) {
                    throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                        getTokenName(tokenId) + ":" + "Defective Token Filler!");
                }

                // read EC field
                ec = readBigEndian(bytes, TOKEN_EC_POS, 2);

                // read RRC field
                rrc = readBigEndian(bytes, TOKEN_RRC_POS, 2);
            } else if (tokenId == MIC_ID_v2) {
                for (int i = 3; i < 8; i++) {
                    if ((bytes[i] & 0xff) != FILLER) {
                        throw new GSSException(GSSException.DEFECTIVE_TOKEN,
                                -1, getTokenName(tokenId) + ":" +
                                "Defective Token Filler!");
                    }
                }
            }

            // set default QOP
            prop.setQOP(0);

            // sequence number
            seqNumber = readBigEndian(bytes, 12, 4);
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
         * @see sun.security.jgss.krb5.Krb5Token#MIC_ID_v2
         * @see sun.security.jgss.krb5.Krb5Token#WRAP_ID_v2
         */
        public final int getTokenId() {
            return tokenId;
        }

        /**
         * Returns the bytes of this header.
         * @return 8 bytes that form this header
         */
        public final byte[] getBytes() {
            return bytes;
        }
    } // end of class MessageTokenHeader
}
