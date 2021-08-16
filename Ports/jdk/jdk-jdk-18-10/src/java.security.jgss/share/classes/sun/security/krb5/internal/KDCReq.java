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

import sun.security.krb5.*;
import java.util.Vector;
import sun.security.util.*;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Implements the ASN.1 KRB_KDC_REQ type.
 *
 * <pre>{@code
 * KDC-REQ              ::= SEQUENCE {
 *      -- NOTE: first tag is [1], not [0]
 *      pvno            [1] INTEGER (5) ,
 *      msg-type        [2] INTEGER (10 -- AS -- | 12 -- TGS --),
 *      padata          [3] SEQUENCE OF PA-DATA OPTIONAL
 *                            -- NOTE: not empty --,
 *      req-body        [4] KDC-REQ-BODY
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */
public class KDCReq {

    public KDCReqBody reqBody;
    public PAData[] pAData = null; //optional
    private int pvno;
    private int msgType;

    public KDCReq(PAData[] new_pAData, KDCReqBody new_reqBody,
            int req_type) throws IOException {
        pvno = Krb5.PVNO;
        msgType = req_type;
        if (new_pAData != null) {
            pAData = new PAData[new_pAData.length];
            for (int i = 0; i < new_pAData.length; i++) {
                if (new_pAData[i] == null) {
                    throw new IOException("Cannot create a KDCRep");
                } else {
                    pAData[i] = (PAData) new_pAData[i].clone();
                }
            }
        }
        reqBody = new_reqBody;
    }

    public KDCReq() {
    }

    public KDCReq(byte[] data, int req_type) throws Asn1Exception,
            IOException, KrbException {
        init(new DerValue(data), req_type);
    }

    /**
     * Creates an KDCReq object from a DerValue object and asn1 type.
     *
     * @param der a DER value of an KDCReq object.
     * @param req_type a encoded asn1 type value.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception KrbErrException
     */
    public KDCReq(DerValue der, int req_type) throws Asn1Exception,
            IOException, KrbException {
        init(der, req_type);
    }

    /**
     * Initializes a KDCReq object from a DerValue.  The DER encoding
     * must be in the format specified by the KRB_KDC_REQ ASN.1 notation.
     *
     * @param encoding a DER-encoded KDCReq object.
     * @param req_type an int indicating whether it's KRB_AS_REQ or KRB_TGS_REQ type
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception KrbException if an error occurs while constructing a Realm object,
     * or a Krb object from DER-encoded data.
     */
    protected void init(DerValue encoding, int req_type) throws Asn1Exception,
            IOException, KrbException {
        DerValue der, subDer;
        BigInteger bint;
        if ((encoding.getTag() & 0x1F) != req_type) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if (der.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & 0x01F) == 0x01) {
            bint = subDer.getData().getBigInteger();
            this.pvno = bint.intValue();
            if (this.pvno != Krb5.PVNO) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_BADVERSION);
            }
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & 0x01F) == 0x02) {
            bint = subDer.getData().getBigInteger();
            this.msgType = bint.intValue();
            if (this.msgType != req_type) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_MSG_TYPE);
            }
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        pAData = PAData.parseSequence(der.getData(), (byte) 0x03, true);
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & 0x01F) == 0x04) {
            DerValue subsubDer = subDer.getData().getDerValue();
            reqBody = new KDCReqBody(subsubDer, msgType);
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
    }

    /**
     * Encodes this object to a byte array.
     *
     * @return an byte array of encoded data.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     *
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream temp, bytes, out;
        temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(pvno));
        out = new DerOutputStream();
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x01), temp);
        temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(msgType));
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x02), temp);
        if (pAData != null && pAData.length > 0) {
            temp = new DerOutputStream();
            for (int i = 0; i < pAData.length; i++) {
                temp.write(pAData[i].asn1Encode());
            }
            bytes = new DerOutputStream();
            bytes.write(DerValue.tag_SequenceOf, temp);
            out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x03), bytes);
        }
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x04), reqBody.asn1Encode(msgType));
        bytes = new DerOutputStream();
        bytes.write(DerValue.tag_Sequence, out);
        out = new DerOutputStream();
        out.write(DerValue.createTag(DerValue.TAG_APPLICATION,
                true, (byte) msgType), bytes);
        return out.toByteArray();
    }

    public byte[] asn1EncodeReqBody() throws Asn1Exception, IOException {
        return reqBody.asn1Encode(msgType);
    }
}
