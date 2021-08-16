/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import sun.security.krb5.Confounder;

/**
 * This class represents the new format of GSS tokens, as specified in RFC
 * 4121, emitted by the GSSContext.wrap() call. It is a MessageToken except
 * that it also contains plaintext or encrypted data at the end. A WrapToken
 * has certain other rules that are peculiar to it and different from a
 * MICToken, which is another type of MessageToken. All data in a WrapToken is
 * prepended by a random confounder of 16 bytes. Thus, all application data
 * is replaced by (confounder || data || tokenHeader || checksum).
 *
 * @author Seema Malkani
 */
class WrapToken_v2 extends MessageToken_v2 {

    // Accessed by CipherHelper
    byte[] confounder = null;

    private final boolean privacy;

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
    public WrapToken_v2(Krb5Context context,
                     byte[] tokenBytes, int tokenOffset, int tokenLen,
                     MessageProp prop)  throws GSSException {

        super(Krb5Token.WRAP_ID_v2, context,
              tokenBytes, tokenOffset, tokenLen, prop);
        this.privacy = prop.getPrivacy();
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
    public WrapToken_v2(Krb5Context context,
                     InputStream is, MessageProp prop)
        throws GSSException {

        super(Krb5Token.WRAP_ID_v2, context, is, prop);
        this.privacy = prop.getPrivacy();
    }

    /**
     * Obtains the application data that was transmitted in this
     * WrapToken.
     * @return a byte array containing the application data
     * @throws GSSException if an error occurs while decrypting any
     * cipher text and checking for validity
     */
    public byte[] getData() throws GSSException {

        byte[] temp = new byte[tokenDataLen];
        int len = getData(temp, 0);
        return Arrays.copyOf(temp, len);
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

        // debug("WrapToken cons: data is token is [" +
        //      getHexBytes(tokenBytes, tokenOffset, tokenLen) + "]\n");

        // Do decryption if this token was privacy protected.
        if (privacy) {

            // decrypt data
            cipherHelper.decryptData(this, tokenData, 0, tokenDataLen,
                                dataBuf, dataBufOffset, getKeyUsage());

            return tokenDataLen - CONFOUNDER_SIZE -
                TOKEN_HEADER_SIZE - cipherHelper.getChecksumLength();
        } else {

            // Token data is in cleartext
            // debug("\t\tNo encryption was performed by peer.\n");

            // data
            int data_length = tokenDataLen - cipherHelper.getChecksumLength();
            System.arraycopy(tokenData, 0,
                             dataBuf, dataBufOffset,
                             data_length);
            // debug("\t\tData is: " + getHexBytes(dataBuf, data_length));

            /*
             * Make sure checksum is not corrupt
             */
            if (!verifySign(dataBuf, dataBufOffset, data_length)) {
                throw new GSSException(GSSException.BAD_MIC, -1,
                         "Corrupt checksum in Wrap token");
            }
            return data_length;
        }
    }

    /**
     * Writes a WrapToken_v2 object
     */
    public WrapToken_v2(Krb5Context context, MessageProp prop,
                     byte[] dataBytes, int dataOffset, int dataLen)
            throws GSSException {

        super(Krb5Token.WRAP_ID_v2, context);

        confounder = Confounder.bytes(CONFOUNDER_SIZE);

        // debug("\nWrapToken cons: data to wrap is [" +
        // getHexBytes(confounder) + " " +
        // getHexBytes(dataBytes, dataOffset, dataLen) + "]\n");

        genSignAndSeqNumber(prop, dataBytes, dataOffset, dataLen);

        /*
         * If the application decides to ask for privacy when the context
         * did not negotiate for it, do not provide it. The peer might not
         * have support for it. The app will realize this with a call to
         * pop.getPrivacy() after wrap().
         */
        if (!context.getConfState())
            prop.setPrivacy(false);

        privacy = prop.getPrivacy();

        if (!privacy) {
            // Wrap Tokens (without confidentiality) =
            // { 16 byte token_header | plaintext | 12-byte HMAC }
            // where HMAC is on { plaintext | token_header }

            tokenData = new byte[dataLen + checksum.length];
            System.arraycopy(dataBytes, dataOffset, tokenData, 0, dataLen);
            System.arraycopy(checksum, 0, tokenData, dataLen, checksum.length);
        } else {
            // Wrap Tokens (with confidentiality) =
            // { 16 byte token_header |
            // Encrypt(16-byte confounder | plaintext | token_header) |
            // 12-byte HMAC }

            tokenData = cipherHelper.encryptData(this, confounder, getTokenHeader(),
                dataBytes, dataOffset, dataLen, getKeyUsage());
        }
    }

    public void encode(OutputStream os) throws IOException {
        encodeHeader(os);
        os.write(tokenData);
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(
                MessageToken_v2.TOKEN_HEADER_SIZE + tokenData.length);
        encode(bos);
        return bos.toByteArray();
    }

    public int encode(byte[] outToken, int offset) throws IOException {
        byte[] token = encode();
        System.arraycopy(token, 0, outToken, offset, token.length);
        return token.length;
    }

    // This implementation is way to conservative. And it certainly
    // doesn't return the maximum limit.
    static int getSizeLimit(int qop, boolean confReq, int maxTokenSize,
        CipherHelper ch) throws GSSException {
        return (GSSHeader.getMaxMechTokenSize(OID, maxTokenSize) -
                (TOKEN_HEADER_SIZE + ch.getChecksumLength() + CONFOUNDER_SIZE)
                - 8 /* safety */);
    }
}
