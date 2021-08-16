/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayOutputStream;
import sun.security.krb5.Confounder;

/**
 * This class represents a token emitted by the GSSContext.wrap()
 * call. It is a MessageToken except that it also contains plaintext
 * or encrypted data at the end. A wrapToken has certain other rules
 * that are peculiar to it and different from a MICToken, which is
 * another type of MessageToken. All data in a WrapToken is prepended
 * by a random counfounder of 8 bytes. All data in a WrapToken is
 * also padded with one to eight bytes where all bytes are equal in
 * value to the number of bytes being padded. Thus, all application
 * data is replaced by (confounder || data || padding).
 *
 * @author Mayank Upadhyay
 */
class WrapToken extends MessageToken {
    /**
     * The size of the random confounder used in a WrapToken.
     */
    static final int CONFOUNDER_SIZE = 8;

    /*
     * The padding used with a WrapToken. All data is padded to the
     * next multiple of 8 bytes, even if its length is already
     * multiple of 8.
     * Use this table as a quick way to obtain padding bytes by
     * indexing it with the number of padding bytes required.
     */
    static final byte[][] pads = {
        null, // No, no one escapes padding
        {0x01},
        {0x02, 0x02},
        {0x03, 0x03, 0x03},
        {0x04, 0x04, 0x04, 0x04},
        {0x05, 0x05, 0x05, 0x05, 0x05},
        {0x06, 0x06, 0x06, 0x06, 0x06, 0x06},
        {0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07},
        {0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08}
    };

    /*
     * A token may come in either in an InputStream or as a
     * byte[]. Store a reference to it in either case and process
     * it's data only later when getData() is called and
     * decryption/copying is needed to be done. Note that JCE can
     * decrypt both from a byte[] and from an InputStream.
     */
    private boolean readTokenFromInputStream = true;
    private InputStream is = null;
    private byte[] tokenBytes = null;
    private int tokenOffset = 0;
    private int tokenLen = 0;

    /*
     * Application data may come from an InputStream or from a
     * byte[]. However, it will always be stored and processed as a
     * byte[] since
     * (a) the MessageDigest class only accepts a byte[] as input and
     * (b) It allows writing to an OuputStream via a CipherOutputStream.
     */
    private byte[] dataBytes = null;
    private int dataOffset = 0;
    private int dataLen = 0;

    // the len of the token data: (confounder || data || padding)
    private int dataSize = 0;

    // Accessed by CipherHelper
    byte[] confounder = null;
    byte[] padding = null;

    private boolean privacy = false;

    /**
     * Constructs a WrapToken from token bytes obtained from the
     * peer.
     * @param context the mechanism context associated with this
     * token
     * @param tokenBytes the bytes of the token
     * @param tokenOffset the offset of the token
     * @param tokenLen the length of the token
     * @param prop the MessageProp into which characteristics of the
     * parsed token will be stored.
     * @throws GSSException if the token is defective
     */
    public WrapToken(Krb5Context context,
                     byte[] tokenBytes, int tokenOffset, int tokenLen,
                     MessageProp prop)  throws GSSException {

        // Just parse the MessageToken part first
        super(Krb5Token.WRAP_ID, context,
              tokenBytes, tokenOffset, tokenLen, prop);

        this.readTokenFromInputStream = false;

        // Will need the token bytes again when extracting data
        this.tokenBytes = tokenBytes;
        this.tokenOffset = tokenOffset;
        this.tokenLen = tokenLen;
        this.privacy = prop.getPrivacy();
        dataSize =
            getGSSHeader().getMechTokenLength() - getKrb5TokenSize();
    }

    /**
     * Constructs a WrapToken from token bytes read on the fly from
     * an InputStream.
     * @param context the mechanism context associated with this
     * token
     * @param is the InputStream containing the token bytes
     * @param prop the MessageProp into which characteristics of the
     * parsed token will be stored.
     * @throws GSSException if the token is defective or if there is
     * a problem reading from the InputStream
     */
    public WrapToken(Krb5Context context,
                     InputStream is, MessageProp prop)
        throws GSSException {

        // Just parse the MessageToken part first
        super(Krb5Token.WRAP_ID, context, is, prop);

        // Will need the token bytes again when extracting data
        this.is = is;
        this.privacy = prop.getPrivacy();
        /*
          debug("WrapToken Cons: gssHeader.getMechTokenLength=" +
          getGSSHeader().getMechTokenLength());
          debug("\n                token size="
          + getTokenSize());
        */

        dataSize =
            getGSSHeader().getMechTokenLength() - getTokenSize();
        // debug("\n                dataSize=" + dataSize);
        // debug("\n");
    }

    /**
     * Obtains the application data that was transmitted in this
     * WrapToken.
     * @return a byte array containing the application data
     * @throws GSSException if an error occurs while decrypting any
     * cipher text and checking for validity
     */
    public byte[] getData() throws GSSException {

        byte[] temp = new byte[dataSize];
        getData(temp, 0);

        // Remove the confounder and the padding
        byte[] retVal = new byte[dataSize - confounder.length -
                                padding.length];
        System.arraycopy(temp, 0, retVal, 0, retVal.length);

        return retVal;
    }

    /**
     * Obtains the application data that was transmitted in this
     * WrapToken, writing it into an application provided output
     * array.
     * @param dataBuf the output buffer into which the data must be
     * written
     * @param dataBufOffset the offset at which to write the data
     * @return the size of the data written
     * @throws GSSException if an error occurs while decrypting any
     * cipher text and checking for validity
     */
    public int getData(byte[] dataBuf, int dataBufOffset)
        throws GSSException {

        if (readTokenFromInputStream)
            getDataFromStream(dataBuf, dataBufOffset);
        else
            getDataFromBuffer(dataBuf, dataBufOffset);

        return (dataSize - confounder.length - padding.length);
    }

    /**
     * Helper routine to obtain the application data transmitted in
     * this WrapToken. It is called if the WrapToken was constructed
     * with a byte array as input.
     * @param dataBuf the output buffer into which the data must be
     * written
     * @param dataBufOffset the offset at which to write the data
     * @throws GSSException if an error occurs while decrypting any
     * cipher text and checking for validity
     */
    private void getDataFromBuffer(byte[] dataBuf, int dataBufOffset)
        throws GSSException {

        GSSHeader gssHeader = getGSSHeader();
        int dataPos = tokenOffset +
            gssHeader.getLength() + getTokenSize();

        if (dataPos + dataSize > tokenOffset + tokenLen)
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                                   "Insufficient data in "
                                   + getTokenName(getTokenId()));

        // debug("WrapToken cons: data is token is [" +
        //      getHexBytes(tokenBytes, tokenOffset, tokenLen) + "]\n");

        confounder = new byte[CONFOUNDER_SIZE];

        // Do decryption if this token was privacy protected.

        if (privacy) {
            cipherHelper.decryptData(this,
                tokenBytes, dataPos, dataSize, dataBuf, dataBufOffset);
            /*
            debug("\t\tDecrypted data is [" +
                getHexBytes(confounder) + " " +
                getHexBytes(dataBuf, dataBufOffset,
                        dataSize - CONFOUNDER_SIZE - padding.length) +
                getHexBytes(padding) +
            "]\n");
            */

        } else {

            // Token data is in cleartext
            // debug("\t\tNo encryption was performed by peer.\n");
            System.arraycopy(tokenBytes, dataPos,
                             confounder, 0, CONFOUNDER_SIZE);
            int padSize = tokenBytes[dataPos + dataSize - 1];
            if (padSize < 0)
                padSize = 0;
            if (padSize > 8)
                padSize %= 8;

            padding = pads[padSize];
            // debug("\t\tPadding applied was: " + padSize + "\n");

            System.arraycopy(tokenBytes, dataPos + CONFOUNDER_SIZE,
                             dataBuf, dataBufOffset, dataSize -
                             CONFOUNDER_SIZE - padSize);

           // byte[] debugbuf = new byte[dataSize - CONFOUNDER_SIZE - padSize];
           // System.arraycopy(tokenBytes, dataPos + CONFOUNDER_SIZE,
           //                debugbuf, 0, debugbuf.length);
           // debug("\t\tData is: " + getHexBytes(debugbuf, debugbuf.length));
        }

        /*
         * Make sure sign and sequence number are not corrupt
         */

        if (!verifySignAndSeqNumber(confounder,
                                    dataBuf, dataBufOffset,
                                    dataSize - CONFOUNDER_SIZE
                                    - padding.length,
                                    padding))
            throw new GSSException(GSSException.BAD_MIC, -1,
                         "Corrupt checksum or sequence number in Wrap token");
    }

    /**
     * Helper routine to obtain the application data transmitted in
     * this WrapToken. It is called if the WrapToken was constructed
     * with an Inputstream.
     * @param dataBuf the output buffer into which the data must be
     * written
     * @param dataBufOffset the offset at which to write the data
     * @throws GSSException if an error occurs while decrypting any
     * cipher text and checking for validity
     */
    private void getDataFromStream(byte[] dataBuf, int dataBufOffset)
        throws GSSException {

        GSSHeader gssHeader = getGSSHeader();

        // Don't check the token length. Data will be read on demand from
        // the InputStream.

        // debug("WrapToken cons: data will be read from InputStream.\n");

        confounder = new byte[CONFOUNDER_SIZE];

        try {

            // Do decryption if this token was privacy protected.

            if (privacy) {
                cipherHelper.decryptData(this, is, dataSize,
                    dataBuf, dataBufOffset);

                // debug("\t\tDecrypted data is [" +
                //     getHexBytes(confounder) + " " +
                //     getHexBytes(dataBuf, dataBufOffset,
                // dataSize - CONFOUNDER_SIZE - padding.length) +
                //     getHexBytes(padding) +
                //     "]\n");

            } else {

                // Token data is in cleartext
                // debug("\t\tNo encryption was performed by peer.\n");
                readFully(is, confounder);

                if (cipherHelper.isArcFour()) {
                    padding = pads[1];
                    readFully(is, dataBuf, dataBufOffset, dataSize-CONFOUNDER_SIZE-1);
                } else {
                    // Data is always a multiple of 8 with this GSS Mech
                    // Copy all but last block as they are
                    int numBlocks = (dataSize - CONFOUNDER_SIZE)/8 - 1;
                    int offset = dataBufOffset;
                    for (int i = 0; i < numBlocks; i++) {
                        readFully(is, dataBuf, offset, 8);
                        offset += 8;
                    }

                    byte[] finalBlock = new byte[8];
                    readFully(is, finalBlock);

                    int padSize = finalBlock[7];
                    padding = pads[padSize];

                    // debug("\t\tPadding applied was: " + padSize + "\n");
                    System.arraycopy(finalBlock, 0, dataBuf, offset,
                                     finalBlock.length - padSize);
                }
            }
        } catch (IOException e) {
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                                   getTokenName(getTokenId())
                                   + ": " + e.getMessage());
        }

        /*
         * Make sure sign and sequence number are not corrupt
         */

        if (!verifySignAndSeqNumber(confounder,
                                    dataBuf, dataBufOffset,
                                    dataSize - CONFOUNDER_SIZE
                                    - padding.length,
                                    padding))
            throw new GSSException(GSSException.BAD_MIC, -1,
                         "Corrupt checksum or sequence number in Wrap token");
    }


    /**
     * Helper routine to pick the right padding for a certain length
     * of application data. Every application message has some
     * padding between 1 and 8 bytes.
     * @param len the length of the application data
     * @return the padding to be applied
     */
    private byte[] getPadding(int len) {
        int padSize = 0;
        // For RC4-HMAC, all padding is rounded up to 1 byte.
        // One byte is needed to say that there is 1 byte of padding.
        if (cipherHelper.isArcFour()) {
            padSize = 1;
        } else {
            padSize = len % 8;
            padSize = 8 - padSize;
        }
        return pads[padSize];
    }

    public WrapToken(Krb5Context context, MessageProp prop,
                     byte[] dataBytes, int dataOffset, int dataLen)
        throws GSSException {

        super(Krb5Token.WRAP_ID, context);

        confounder = Confounder.bytes(CONFOUNDER_SIZE);

        padding = getPadding(dataLen);
        dataSize = confounder.length + dataLen + padding.length;
        this.dataBytes = dataBytes;
        this.dataOffset = dataOffset;
        this.dataLen = dataLen;

        /*
          debug("\nWrapToken cons: data to wrap is [" +
          getHexBytes(confounder) + " " +
          getHexBytes(dataBytes, dataOffset, dataLen) + " " +
          // padding is never null for Wrap
          getHexBytes(padding) + "]\n");
         */

        genSignAndSeqNumber(prop,
                            confounder,
                            dataBytes, dataOffset, dataLen,
                            padding);

        /*
         * If the application decides to ask for privacy when the context
         * did not negotiate for it, do not provide it. The peer might not
         * have support for it. The app will realize this with a call to
         * pop.getPrivacy() after wrap().
         */
        if (!context.getConfState())
            prop.setPrivacy(false);

        privacy = prop.getPrivacy();
    }

    public void encode(OutputStream os) throws IOException, GSSException {

        super.encode(os);

        // debug("Writing data: [");
        if (!privacy) {

            // debug(getHexBytes(confounder, confounder.length));
            os.write(confounder);

            // debug(" " + getHexBytes(dataBytes, dataOffset, dataLen));
            os.write(dataBytes, dataOffset, dataLen);

            // debug(" " + getHexBytes(padding, padding.length));
            os.write(padding);

        } else {

            cipherHelper.encryptData(this, confounder,
                dataBytes, dataOffset, dataLen, padding, os);
        }
        // debug("]\n");
    }

    public byte[] encode() throws IOException, GSSException {
        // XXX Fine tune this initial size
        ByteArrayOutputStream bos = new ByteArrayOutputStream(dataSize + 50);
        encode(bos);
        return bos.toByteArray();
    }

    public int encode(byte[] outToken, int offset)
        throws IOException, GSSException  {

        // Token header is small
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        super.encode(bos);
        byte[] header = bos.toByteArray();
        System.arraycopy(header, 0, outToken, offset, header.length);
        offset += header.length;

        // debug("WrapToken.encode: Writing data: [");
        if (!privacy) {

            // debug(getHexBytes(confounder, confounder.length));
            System.arraycopy(confounder, 0, outToken, offset,
                             confounder.length);
            offset += confounder.length;

            // debug(" " + getHexBytes(dataBytes, dataOffset, dataLen));
            System.arraycopy(dataBytes, dataOffset, outToken, offset,
                             dataLen);
            offset += dataLen;

            // debug(" " + getHexBytes(padding, padding.length));
            System.arraycopy(padding, 0, outToken, offset, padding.length);

        } else {

            cipherHelper.encryptData(this, confounder, dataBytes,
                dataOffset, dataLen, padding, outToken, offset);

            // debug(getHexBytes(outToken, offset, dataSize));
        }

        // debug("]\n");

        // %%% assume that plaintext length == ciphertext len
        return (header.length + confounder.length + dataLen + padding.length);

    }

    protected int getKrb5TokenSize() throws GSSException {
        return (getTokenSize() + dataSize);
    }

    protected int getSealAlg(boolean conf, int qop) throws GSSException {
        if (!conf) {
            return SEAL_ALG_NONE;
        }

        // ignore QOP
        return cipherHelper.getSealAlg();
    }

    // This implementation is way too conservative. And it certainly
    // doesn't return the maximum limit.
    static int getSizeLimit(int qop, boolean confReq, int maxTokenSize,
        CipherHelper ch) throws GSSException {
        return (GSSHeader.getMaxMechTokenSize(OID, maxTokenSize) -
                (getTokenSize(ch) + CONFOUNDER_SIZE) - 8); /* safety */
    }

}
