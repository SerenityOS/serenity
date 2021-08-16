/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5;

import sun.security.util.*;
import sun.security.krb5.internal.crypto.*;
import sun.security.krb5.internal.*;
import java.io.IOException;
import java.math.BigInteger;

/**
 * This class encapsulates Kerberos encrypted data. It allows
 * callers access to both the ASN.1 encoded form of the EncryptedData
 * type as well as the raw cipher text.
 */

public class EncryptedData implements Cloneable {
    int eType;
    Integer kvno; // optional
    byte[] cipher;
    byte[] plain; // not part of ASN.1 encoding

    // ----------------+-----------+----------+----------------+---------------
    // Encryption type |etype value|block size|minimum pad size|confounder size
    // ----------------+-----------+----------+----------------+---------------
    public static final int
        ETYPE_NULL        = 0;       // 1          0                0
    public static final int
        ETYPE_DES_CBC_CRC = 1;       // 8          4                8
    public static final int
        ETYPE_DES_CBC_MD4 = 2;       // 8          0                8
    public static final int
        ETYPE_DES_CBC_MD5 = 3;       // 8          0                8

    // draft-brezak-win2k-krb-rc4-hmac-04.txt
    public static final int
        ETYPE_ARCFOUR_HMAC = 23;     // 1
    // NOTE: the exportable RC4-HMAC is not supported;
    // it is no longer a usable encryption type
    public static final int
        ETYPE_ARCFOUR_HMAC_EXP = 24; // 1

     // draft-ietf-krb-wg-crypto-07.txt
    public static final int
        ETYPE_DES3_CBC_HMAC_SHA1_KD = 16; // 8     0                8

    // draft-raeburn-krb-rijndael-krb-07.txt
    public static final int
         ETYPE_AES128_CTS_HMAC_SHA1_96 = 17; // 16      0           16
    public static final int
         ETYPE_AES256_CTS_HMAC_SHA1_96 = 18; // 16      0           16

    // rfc8009
    public static final int
        ETYPE_AES128_CTS_HMAC_SHA256_128 = 19; // 16      0           16
    public static final int
        ETYPE_AES256_CTS_HMAC_SHA384_192 = 20; // 16      0           16

    /* used by self */
    private EncryptedData() {
    }

    public Object clone() {
        EncryptedData new_encryptedData = new EncryptedData();
        new_encryptedData.eType = eType;
        if (kvno != null) {
            new_encryptedData.kvno = kvno.intValue();
        }
        if (cipher != null) {
            new_encryptedData.cipher = new byte[cipher.length];
            System.arraycopy(cipher, 0, new_encryptedData.cipher,
                             0, cipher.length);
        }
        return new_encryptedData;
    }

    // Used by test
    public EncryptedData(
                         int new_eType,
                         Integer new_kvno,
                         byte[] new_cipher) {
        eType = new_eType;
        kvno = new_kvno;
        cipher = new_cipher;
    }

    /*
    // Not used.
    public EncryptedData(
                         EncryptionKey key,
                         byte[] plaintext)
        throws KdcErrException, KrbCryptoException {
        EType etypeEngine = EType.getInstance(key.getEType());
        cipher = etypeEngine.encrypt(plaintext, key.getBytes());
        eType = key.getEType();
        kvno = key.getKeyVersionNumber();
    }
    */

    // used in KrbApRep, KrbApReq, KrbAsReq, KrbCred, KrbPriv
    public EncryptedData(
                         EncryptionKey key,
                         byte[] plaintext,
                         int usage)
        throws KdcErrException, KrbCryptoException {
        EType etypeEngine = EType.getInstance(key.getEType());
        cipher = etypeEngine.encrypt(plaintext, key.getBytes(), usage);
        eType = key.getEType();
        kvno = key.getKeyVersionNumber();
    }

    /*
    // Not used.
    public EncryptedData(
                         EncryptionKey key,
                         byte[] ivec,
                         byte[] plaintext)
        throws KdcErrException, KrbCryptoException {
        EType etypeEngine = EType.getInstance(key.getEType());
        cipher = etypeEngine.encrypt(plaintext, key.getBytes(), ivec);
        eType = key.getEType();
        kvno = key.getKeyVersionNumber();
    }
    */

    /*
    // Not used.
    EncryptedData(
                  StringBuffer password,
                  byte[] plaintext)
        throws KdcErrException, KrbCryptoException {
        EncryptionKey key = new EncryptionKey(password);
        EType etypeEngine = EType.getInstance(key.getEType());
        cipher = etypeEngine.encrypt(plaintext, key.getBytes());
        eType = key.getEType();
        kvno = key.getKeyVersionNumber();
    }
    */
    public byte[] decrypt(
                          EncryptionKey key, int usage)
        throws KdcErrException, KrbApErrException, KrbCryptoException {
            if (eType != key.getEType()) {
                throw new KrbCryptoException(
                    "EncryptedData is encrypted using keytype " +
                    EType.toString(eType) +
                    " but decryption key is of type " +
                    EType.toString(key.getEType()));
            }

            EType etypeEngine = EType.getInstance(eType);
            plain = etypeEngine.decrypt(cipher, key.getBytes(), usage);
            // The service ticket will be used in S4U2proxy request. Therefore
            // the raw ticket is still needed.
            //cipher = null;
            return etypeEngine.decryptedData(plain);
        }

    /*
    // currently destructive on cipher
    // Not used.
    public byte[] decrypt(
                          EncryptionKey key,
                          byte[] ivec, int usage)
        throws KdcErrException, KrbApErrException, KrbCryptoException {
            // XXX check for matching eType and kvno here
            EType etypeEngine = EType.getInstance(eType);
            plain = etypeEngine.decrypt(cipher, key.getBytes(), ivec, usage);
            cipher = null;
            return etypeEngine.decryptedData(plain);
        }

    // currently destructive on cipher
    // Not used.
    byte[] decrypt(StringBuffer password)
        throws KdcErrException, KrbApErrException, KrbCryptoException {
            EncryptionKey key = new EncryptionKey(password);
            // XXX check for matching eType here
            EType etypeEngine = EType.getInstance(eType);
            plain = etypeEngine.decrypt(cipher, key.getBytes());
            cipher = null;
            return etypeEngine.decryptedData(plain);
        }
    */

    private byte[] decryptedData() throws KdcErrException {
        if (plain != null) {
            EType etypeEngine = EType.getInstance(eType);
            return etypeEngine.decryptedData(plain);
        }
        return null;
    }

    /**
     * Constructs an instance of EncryptedData type.
     * @param encoding a single DER-encoded value.
     * @exception Asn1Exception if an error occurs while decoding an
     * ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded
     * data.
     *
     */
    /* Used by self */
    private EncryptedData(DerValue encoding)
        throws Asn1Exception, IOException {

        DerValue der = null;
        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if ((der.getTag() & (byte)0x1F) == (byte)0x00) {
            eType = (der.getData().getBigInteger()).intValue();
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        if ((encoding.getData().peekByte() & 0x1F) == 1) {
            der = encoding.getData().getDerValue();
            int i = (der.getData().getBigInteger()).intValue();
            kvno = i;
        } else {
            kvno = null;
        }
        der = encoding.getData().getDerValue();
        if ((der.getTag() & (byte)0x1F) == (byte)0x02) {
            cipher = der.getData().getOctetString();
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        if (encoding.getData().available() > 0) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
    }

    /**
     * Returns an ASN.1 encoded EncryptedData type.
     *
     * <pre>{@code
     * EncryptedData   ::= SEQUENCE {
     *     etype   [0] Int32 -- EncryptionType --,
     *     kvno    [1] UInt32 OPTIONAL,
     *     cipher  [2] OCTET STRING -- ciphertext
     * }
     * }</pre>
     *
     * <p>
     * This definition reflects the Network Working Group RFC 4120
     * specification available at
     * <a href="http://www.ietf.org/rfc/rfc4120.txt">
     * http://www.ietf.org/rfc/rfc4120.txt</a>.
     *
     * @return byte array of encoded EncryptedData object.
     * @exception Asn1Exception if an error occurs while decoding an
     * ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading
     * encoded data.
     *
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(this.eType));
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                       true, (byte)0x00), temp);
        temp = new DerOutputStream();
        if (kvno != null) {
            // encode as an unsigned integer (UInt32)
            temp.putInteger(BigInteger.valueOf(this.kvno.longValue()));
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                           true, (byte)0x01), temp);
            temp = new DerOutputStream();
        }
        temp.putOctetString(this.cipher);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true,
                        (byte)0x02), temp);
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }


    /**
     * Parse (unmarshal) an EncryptedData from a DER input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @param data the Der input stream value, which contains one or more
     *        marshaled value.
     * @param explicitTag tag number.
     * @param optional indicate if this data field is optional
     * @exception Asn1Exception if an error occurs while decoding an
     * ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading
     * encoded data.
     * @return an instance of EncryptedData.
     *
     */
    public static EncryptedData parse(DerInputStream data,
                                      byte explicitTag,
                                      boolean optional)
        throws Asn1Exception, IOException {
        if ((optional) &&
            (((byte)data.peekByte() & (byte)0x1F) != explicitTag))
            return null;
        DerValue der = data.getDerValue();
        if (explicitTag != (der.getTag() & (byte)0x1F))  {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        } else {
            DerValue subDer = der.getData().getDerValue();
            return new EncryptedData(subDer);
        }
    }

    /**
     * Reset asn.1 data stream after decryption, remove redundant bytes.
     * @param data the decrypted data from decrypt().
     * @return the reset byte array which holds exactly one asn1 datum
     * including its tag and length.
     *
     */
    public byte[] reset(byte[] data) {
        byte[]  bytes = null;
        // for asn.1 encoded data, we use length field to
        // determine the data length and remove redundant paddings.
        if ((data[1] & 0xFF) < 128) {
            bytes = new byte[data[1] + 2];
            System.arraycopy(data, 0, bytes, 0, data[1] + 2);
        } else {
            if ((data[1] & 0xFF) > 128) {
                int len = data[1] & (byte)0x7F;
                int result = 0;
                for (int i = 0; i < len; i++) {
                    result |= (data[i + 2] & 0xFF) << (8 * (len - i - 1));
                }
                bytes = new byte[result + len + 2];
                System.arraycopy(data, 0, bytes, 0, result + len + 2);
            }
        }
        return bytes;
    }

    public int getEType() {
        return eType;
    }

    public Integer getKeyVersionNumber() {
        return kvno;
    }

    /**
     * Returns the raw cipher text bytes, not in ASN.1 encoding.
     */
    public byte[] getBytes() {
        return cipher;
    }
}
