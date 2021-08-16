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
import sun.security.util.*;
import java.util.Vector;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Implements the ASN.1 EncKDCRepPart type.
 *
 * <pre>{@code
 * EncKDCRepPart          ::= SEQUENCE {
 *      key               [0] EncryptionKey,
 *      last-req          [1] LastReq,
 *      nonce             [2] UInt32,
 *      key-expiration    [3] KerberosTime OPTIONAL,
 *      flags             [4] TicketFlags,
 *      authtime          [5] KerberosTime,
 *      starttime         [6] KerberosTime OPTIONAL,
 *      endtime           [7] KerberosTime,
 *      renew-till        [8] KerberosTime OPTIONAL,
 *      srealm            [9] Realm,
 *      sname             [10] PrincipalName,
 *      caddr             [11] HostAddresses OPTIONAL,
 *      encrypted-pa-data [12] SEQUENCE OF PA-DATA OPTIONAL
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */
public class EncKDCRepPart {

    public EncryptionKey key;
    public LastReq lastReq;
    public int nonce;
    public KerberosTime keyExpiration; //optional
    public TicketFlags flags;
    public KerberosTime authtime;
    public KerberosTime starttime; //optional
    public KerberosTime endtime;
    public KerberosTime renewTill; //optional
    public PrincipalName sname;
    public HostAddresses caddr; //optional
    public PAData[] pAData; //optional
    public int msgType; //not included in sequence

    public EncKDCRepPart(
            EncryptionKey new_key,
            LastReq new_lastReq,
            int new_nonce,
            KerberosTime new_keyExpiration,
            TicketFlags new_flags,
            KerberosTime new_authtime,
            KerberosTime new_starttime,
            KerberosTime new_endtime,
            KerberosTime new_renewTill,
            PrincipalName new_sname,
            HostAddresses new_caddr,
            PAData[] new_pAData,
            int new_msgType) {
        key = new_key;
        lastReq = new_lastReq;
        nonce = new_nonce;
        keyExpiration = new_keyExpiration;
        flags = new_flags;
        authtime = new_authtime;
        starttime = new_starttime;
        endtime = new_endtime;
        renewTill = new_renewTill;
        sname = new_sname;
        caddr = new_caddr;
        pAData = new_pAData;
        msgType = new_msgType;
    }

    public EncKDCRepPart() {
    }

    public EncKDCRepPart(byte[] data, int rep_type)
            throws Asn1Exception, IOException, RealmException {
        init(new DerValue(data), rep_type);
    }

    public EncKDCRepPart(DerValue encoding, int rep_type)
            throws Asn1Exception, IOException, RealmException {
        init(encoding, rep_type);
    }

    /**
     * Initializes an EncKDCRepPart object.
     *
     * @param encoding a single DER-encoded value.
     * @param rep_type type of the encrypted reply message.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception RealmException if an error occurs while decoding an Realm object.
     */
    protected void init(DerValue encoding, int rep_type)
            throws Asn1Exception, IOException, RealmException {
        DerValue der, subDer;
        //implementations return the incorrect tag value, so
        //we don't use the above line; instead we use the following
        msgType = (encoding.getTag() & (byte) 0x1F);
        if (msgType != Krb5.KRB_ENC_AS_REP_PART &&
                msgType != Krb5.KRB_ENC_TGS_REP_PART) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if (der.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        key = EncryptionKey.parse(der.getData(), (byte) 0x00, false);
        lastReq = LastReq.parse(der.getData(), (byte) 0x01, false);
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & (byte) 0x1F) == (byte) 0x02) {
            nonce = subDer.getData().getBigInteger().intValue();
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        keyExpiration = KerberosTime.parse(der.getData(), (byte) 0x03, true);
        flags = TicketFlags.parse(der.getData(), (byte) 0x04, false);
        authtime = KerberosTime.parse(der.getData(), (byte) 0x05, false);
        starttime = KerberosTime.parse(der.getData(), (byte) 0x06, true);
        endtime = KerberosTime.parse(der.getData(), (byte) 0x07, false);
        renewTill = KerberosTime.parse(der.getData(), (byte) 0x08, true);
        Realm srealm = Realm.parse(der.getData(), (byte) 0x09, false);
        sname = PrincipalName.parse(der.getData(), (byte) 0x0A, false, srealm);
        if (der.getData().available() > 0) {
            caddr = HostAddresses.parse(der.getData(), (byte) 0x0B, true);
        }
        if (der.getData().available() > 0) {
            pAData = PAData.parseSequence(der.getData(), (byte) 0x0C, true);
        }
        // We observe extra data from MSAD
        /*if (der.getData().available() > 0) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }*/
    }

    /**
     * Encodes an EncKDCRepPart object.
     * @param rep_type type of encrypted reply message.
     * @return byte array of encoded EncKDCRepPart object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode(int rep_type) throws Asn1Exception,
            IOException {
        DerOutputStream bytes;
        DerOutputStream temp = new DerOutputStream();
        DerOutputStream out = new DerOutputStream();
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x00), key.asn1Encode());
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x01), lastReq.asn1Encode());
        temp.putInteger(BigInteger.valueOf(nonce));
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x02), temp);

        if (keyExpiration != null) {
            out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x03), keyExpiration.asn1Encode());
        }
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x04), flags.asn1Encode());
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x05), authtime.asn1Encode());
        if (starttime != null) {
            out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x06), starttime.asn1Encode());
        }
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x07), endtime.asn1Encode());
        if (renewTill != null) {
            out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x08), renewTill.asn1Encode());
        }
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x09), sname.getRealm().asn1Encode());
        out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x0A), sname.asn1Encode());
        if (caddr != null) {
            out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x0B), caddr.asn1Encode());
        }
        if (pAData != null && pAData.length > 0) {
            temp = new DerOutputStream();
            for (int i = 0; i < pAData.length; i++) {
                temp.write(pAData[i].asn1Encode());
            }
            bytes = new DerOutputStream();
            bytes.write(DerValue.tag_SequenceOf, temp);
            out.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x0C), bytes);
        }
        //should use the rep_type to build the encoding
        //but other implementations do not; it is ignored and
        //the cached msgType is used instead
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, out);
        bytes = new DerOutputStream();
        bytes.write(DerValue.createTag(DerValue.TAG_APPLICATION,
                true, (byte) msgType), temp);
        return bytes.toByteArray();
    }
}
