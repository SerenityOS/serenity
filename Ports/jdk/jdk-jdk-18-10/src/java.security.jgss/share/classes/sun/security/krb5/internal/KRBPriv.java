/*
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

package sun.security.krb5.internal;

import sun.security.krb5.EncryptedData;
import sun.security.krb5.Asn1Exception;
import sun.security.util.*;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Implements the ASN.1 KRB-PRIV type.
 *
 * <pre>{@code
 * KRB-PRIV        ::= [APPLICATION 21] SEQUENCE {
 *         pvno            [0] INTEGER (5),
 *         msg-type        [1] INTEGER (21),
 *                           -- NOTE: there is no [2] tag
 *         enc-part        [3] EncryptedData -- EncKrbPrivPart
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */

public class KRBPriv {
    public int pvno;
    public int msgType;
    public EncryptedData encPart;

    public KRBPriv(EncryptedData new_encPart) {
        pvno = Krb5.PVNO;
        msgType = Krb5.KRB_PRIV;
        encPart = new_encPart;
    }

    public KRBPriv(byte[] data) throws Asn1Exception,
    KrbApErrException, IOException {
        init(new DerValue(data));
    }

    public KRBPriv(DerValue encoding) throws Asn1Exception,
    KrbApErrException, IOException {
        init(encoding);
    }


    /**
     * Initializes an KRBPriv object.
     * @param encoding a single DER-encoded value.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception KrbApErrException if the value read from the DER-encoded data
     *  stream does not match the pre-defined value.
     */
    private void init(DerValue encoding) throws Asn1Exception,
    KrbApErrException, IOException {
        DerValue der, subDer;
        if (((encoding.getTag() & (byte)0x1F) != (byte)0x15)
            || (encoding.isApplication() != true)
            || (encoding.isConstructed() != true))
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        der = encoding.getData().getDerValue();
        if (der.getTag() != DerValue.tag_Sequence)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & 0x1F) == 0x00) {
            pvno = subDer.getData().getBigInteger().intValue();
            if (pvno != Krb5.PVNO) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_BADVERSION);
            }
        }
        else
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & 0x1F) == 0x01) {
            msgType = subDer.getData().getBigInteger().intValue();
            if (msgType != Krb5.KRB_PRIV)
                throw new KrbApErrException(Krb5.KRB_AP_ERR_MSG_TYPE);
        }
        else
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        encPart = EncryptedData.parse(der.getData(), (byte)0x03, false);
        if (der.getData().available() >0)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
    }

    /**
     * Encodes an KRBPriv object.
     * @return byte array of encoded EncAPRepPart object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream temp, bytes;
        temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(pvno));
        bytes = new DerOutputStream();
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x00), temp);
        temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(msgType));
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x01), temp);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x03), encPart.asn1Encode());
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        bytes = new DerOutputStream();
        bytes.write(DerValue.createTag(DerValue.TAG_APPLICATION, true, (byte)0x15), temp);
        return bytes.toByteArray();
    }

}
