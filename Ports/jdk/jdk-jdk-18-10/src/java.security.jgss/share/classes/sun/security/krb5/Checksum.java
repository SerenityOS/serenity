/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5;

import java.util.Arrays;
import sun.security.util.*;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.crypto.*;
import java.io.IOException;
import java.math.BigInteger;

/**
 * This class encapsulates the concept of a Kerberos checksum.
 */
public class Checksum {

    private int cksumType;
    private byte[] checksum;

    // ----------------------------------------------+-------------+-----------
    //                      Checksum type            |sumtype      |checksum
    //                                               |value        | size
    // ----------------------------------------------+-------------+-----------
    public static final int CKSUMTYPE_NULL          = 0;               // 0
    public static final int CKSUMTYPE_CRC32         = 1;               // 4
    public static final int CKSUMTYPE_RSA_MD4       = 2;               // 16
    public static final int CKSUMTYPE_RSA_MD4_DES   = 3;               // 24
    public static final int CKSUMTYPE_DES_MAC       = 4;               // 16
    public static final int CKSUMTYPE_DES_MAC_K     = 5;               // 8
    public static final int CKSUMTYPE_RSA_MD4_DES_K = 6;               // 16
    public static final int CKSUMTYPE_RSA_MD5       = 7;               // 16
    public static final int CKSUMTYPE_RSA_MD5_DES   = 8;               // 24

     // draft-ietf-krb-wg-crypto-07.txt
    public static final int CKSUMTYPE_HMAC_SHA1_DES3_KD = 12;          // 20

    // draft-raeburn-krb-rijndael-krb-07.txt
    public static final int CKSUMTYPE_HMAC_SHA1_96_AES128 = 15;        // 96
    public static final int CKSUMTYPE_HMAC_SHA1_96_AES256 = 16;        // 96

    // rfc8009
    public static final int CKSUMTYPE_HMAC_SHA256_128_AES128 = 19;        // 96
    public static final int CKSUMTYPE_HMAC_SHA384_192_AES256 = 20;        // 96

    // draft-brezak-win2k-krb-rc4-hmac-04.txt
    public static final int CKSUMTYPE_HMAC_MD5_ARCFOUR = -138;

    // default checksum type, -1 if not set
    static int CKSUMTYPE_DEFAULT;
    static int SAFECKSUMTYPE_DEFAULT;

    private static boolean DEBUG = Krb5.DEBUG;
    static {
        initStatic();
    }

    public static void initStatic() {
        String temp = null;
        Config cfg = null;
        try {
            cfg = Config.getInstance();
            temp = cfg.get("libdefaults", "default_checksum");
            if (temp != null) {
                CKSUMTYPE_DEFAULT = Config.getType(temp);
            } else {
                CKSUMTYPE_DEFAULT = -1;
            }
        } catch (Exception exc) {
            if (DEBUG) {
                System.out.println("Exception in getting default checksum "+
                                   "value from the configuration. " +
                                   "No default checksum set.");
                exc.printStackTrace();
            }
            CKSUMTYPE_DEFAULT = -1;
        }


        try {
            temp = cfg.get("libdefaults", "safe_checksum_type");
            if (temp != null)
                {
                    SAFECKSUMTYPE_DEFAULT = Config.getType(temp);
                } else {
                    SAFECKSUMTYPE_DEFAULT = -1;
                }
        } catch (Exception exc) {
            if (DEBUG) {
                System.out.println("Exception in getting safe default " +
                                   "checksum value " +
                                   "from the configuration. " +
                                   "No safe default checksum set.");
                exc.printStackTrace();
            }
            SAFECKSUMTYPE_DEFAULT = -1;
        }
    }

    /**
     * Constructs a new Checksum using the raw data and type.
     *
     * This constructor is only used by Authenticator Checksum
     * {@link sun.security.jgss.krb5.InitialToken.OverloadedChecksum}
     * where the checksum type must be 0x8003
     * (see https://tools.ietf.org/html/rfc4121#section-4.1.1)
     * and checksum field/value is used to convey service flags,
     * channel bindings, and optional delegation information.
     * This special type does NOT have a {@link CksumType} and has its
     * own calculating and verification rules. It does has the same
     * ASN.1 encoding though.
     *
     * @param data the byte array of checksum.
     * @param new_cksumType the type of checksum.
     */
    public Checksum(byte[] data, int new_cksumType) {
        cksumType = new_cksumType;
        checksum = data;
    }

    /**
     * Constructs a new Checksum by calculating over the data using
     * the specified checksum type. If the checksum is unkeyed, key
     * and usage are ignored.
     *
     * @param new_cksumType the type of checksum. If set to -1, the
     *      {@linkplain EType#checksumType() mandatory checksum type}
     *      for the encryption type of {@code key} will be used
     * @param data the data that needs to be performed a checksum calculation on
     * @param key the key used by a keyed checksum
     * @param usage the usage used by a keyed checksum
     */
    public Checksum(int new_cksumType, byte[] data,
                    EncryptionKey key, int usage)
            throws KdcErrException, KrbApErrException, KrbCryptoException {
        if (new_cksumType == -1) {
            cksumType = EType.getInstance(key.getEType()).checksumType();
        } else {
            cksumType = new_cksumType;
        }
        checksum = CksumType.getInstance(cksumType).calculateChecksum(
                    data, data.length, key.getBytes(), usage);
    }

    /**
     * Verifies the keyed checksum over the data passed in.
     */
    public boolean verifyKeyedChecksum(byte[] data, EncryptionKey key, int usage)
            throws KdcErrException, KrbApErrException, KrbCryptoException {
        CksumType cksumEngine = CksumType.getInstance(cksumType);
        if (!cksumEngine.isKeyed()) {
            throw new KrbApErrException(Krb5.KRB_AP_ERR_INAPP_CKSUM);
        } else {
            return cksumEngine.verifyChecksum(
                    data, data.length, key.getBytes(), checksum, usage);
        }
    }


    /**
     * Verifies the checksum over the data passed in. The checksum might
     * be a keyed or not.
     *
     * ===============  ATTENTION! Use with care  ==================
     * According to https://tools.ietf.org/html/rfc3961#section-6.1,
     * An unkeyed checksum should only be used "in limited circumstances
     * where the lack of a key does not provide a window for an attack,
     * preferably as part of an encrypted message".
     */
    public boolean verifyAnyChecksum(byte[] data, EncryptionKey key, int usage)
            throws KdcErrException, KrbCryptoException {
        return CksumType.getInstance(cksumType).verifyChecksum(
                data, data.length, key.getBytes(), checksum, usage);
    }

    boolean isEqual(Checksum cksum) throws KdcErrException {
        if (cksumType != cksum.cksumType) {
            return false;
        }
        return CksumType.isChecksumEqual(checksum, cksum.checksum);
    }

    /**
     * Constructs an instance of Checksum from an ASN.1 encoded representation.
     * @param encoding a single DER-encoded value.
     * @exception Asn1Exception if an error occurs while decoding an ASN1
     * encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     *
     */
    public Checksum(DerValue encoding) throws Asn1Exception, IOException {
        DerValue der;
        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if ((der.getTag() & (byte)0x1F) == (byte)0x00) {
            cksumType = der.getData().getBigInteger().intValue();
        }
        else
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        der = encoding.getData().getDerValue();
        if ((der.getTag() & (byte)0x1F) == (byte)0x01) {
            checksum = der.getData().getOctetString();
        }
        else
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        if (encoding.getData().available() > 0) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
    }

    /**
     * Encodes a Checksum object.
     * <pre>{@code
     * Checksum    ::= SEQUENCE {
     *         cksumtype   [0] Int32,
     *         checksum    [1] OCTET STRING
     * }
     * }</pre>
     *
     * <p>
     * This definition reflects the Network Working Group RFC 4120
     * specification available at
     * <a href="http://www.ietf.org/rfc/rfc4120.txt">
     * http://www.ietf.org/rfc/rfc4120.txt</a>.
     * @return byte array of enocded Checksum.
     * @exception Asn1Exception if an error occurs while decoding an
     * ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading
     * encoded data.
     *
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(cksumType));
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                       true, (byte)0x00), temp);
        temp = new DerOutputStream();
        temp.putOctetString(checksum);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                       true, (byte)0x01), temp);
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }


    /**
     * Parse (unmarshal) a checksum object from a DER input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @exception Asn1Exception if an error occurs while decoding an
     * ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading
     * encoded data.
     * @param data the Der input stream value, which contains one or more
     * marshaled value.
     * @param explicitTag tag number.
     * @param optional indicates if this data field is optional
     * @return an instance of Checksum.
     *
     */
    public static Checksum parse(DerInputStream data,
                                 byte explicitTag, boolean optional)
        throws Asn1Exception, IOException {

        if ((optional) &&
            (((byte)data.peekByte() & (byte)0x1F) != explicitTag)) {
            return null;
        }
        DerValue der = data.getDerValue();
        if (explicitTag != (der.getTag() & (byte)0x1F))  {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        } else {
            DerValue subDer = der.getData().getDerValue();
            return new Checksum(subDer);
        }
    }

    /**
     * Returns the raw bytes of the checksum, not in ASN.1 encoded form.
     */
    public final byte[] getBytes() {
        return checksum;
    }

    public final int getType() {
        return cksumType;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Checksum)) {
            return false;
        }

        try {
            return isEqual((Checksum)obj);
        } catch (KdcErrException kee) {
            return false;
        }
    }

    @Override public int hashCode() {
        int result = 17;
        result = 37 * result + cksumType;
        if (checksum != null) {
            result = 37 * result + Arrays.hashCode(checksum);
        }
        return result;
    }
}
