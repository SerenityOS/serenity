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
 * Implements the ASN.1 Authenticator type.
 *
 * <pre>{@code
 * Authenticator   ::= [APPLICATION 2] SEQUENCE  {
 *         authenticator-vno       [0] INTEGER (5),
 *         crealm                  [1] Realm,
 *         cname                   [2] PrincipalName,
 *         cksum                   [3] Checksum OPTIONAL,
 *         cusec                   [4] Microseconds,
 *         ctime                   [5] KerberosTime,
 *         subkey                  [6] EncryptionKey OPTIONAL,
 *         seq-number              [7] UInt32 OPTIONAL,
 *         authorization-data      [8] AuthorizationData OPTIONAL
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */
public class Authenticator {

    public int authenticator_vno;
    public PrincipalName cname;
    Checksum cksum; //optional
    public int cusec;
    public KerberosTime ctime;
    EncryptionKey subKey; //optional
    Integer seqNumber; //optional
    public AuthorizationData authorizationData; //optional

    public Authenticator(
            PrincipalName new_cname,
            Checksum new_cksum,
            int new_cusec,
            KerberosTime new_ctime,
            EncryptionKey new_subKey,
            Integer new_seqNumber,
            AuthorizationData new_authorizationData) {
        authenticator_vno = Krb5.AUTHNETICATOR_VNO;
        cname = new_cname;
        cksum = new_cksum;
        cusec = new_cusec;
        ctime = new_ctime;
        subKey = new_subKey;
        seqNumber = new_seqNumber;
        authorizationData = new_authorizationData;
    }

    public Authenticator(byte[] data)
            throws Asn1Exception, IOException, KrbApErrException, RealmException {
        init(new DerValue(data));
    }

    public Authenticator(DerValue encoding)
            throws Asn1Exception, IOException, KrbApErrException, RealmException {
        init(encoding);
    }

    /**
     * Initializes an Authenticator object.
     * @param encoding a single DER-encoded value.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception KrbApErrException if the value read from the DER-encoded data
     *  stream does not match the pre-defined value.
     * @exception RealmException if an error occurs while parsing a Realm object.
     */
    private void init(DerValue encoding)
            throws Asn1Exception, IOException, KrbApErrException, RealmException {
        DerValue der, subDer;
        //may not be the correct error code for a tag
        //mismatch on an encrypted structure
        if (((encoding.getTag() & (byte) 0x1F) != (byte) 0x02)
                || (encoding.isApplication() != true)
                || (encoding.isConstructed() != true)) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if (der.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & (byte) 0x1F) != (byte) 0x00) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        authenticator_vno = subDer.getData().getBigInteger().intValue();
        if (authenticator_vno != 5) {
            throw new KrbApErrException(Krb5.KRB_AP_ERR_BADVERSION);
        }
        Realm crealm = Realm.parse(der.getData(), (byte) 0x01, false);
        cname = PrincipalName.parse(der.getData(), (byte) 0x02, false, crealm);
        cksum = Checksum.parse(der.getData(), (byte) 0x03, true);
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & (byte) 0x1F) == 0x04) {
            cusec = subDer.getData().getBigInteger().intValue();
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        ctime = KerberosTime.parse(der.getData(), (byte) 0x05, false);
        if (der.getData().available() > 0) {
            subKey = EncryptionKey.parse(der.getData(), (byte) 0x06, true);
        } else {
            subKey = null;
            seqNumber = null;
            authorizationData = null;
        }
        if (der.getData().available() > 0) {
            if ((der.getData().peekByte() & 0x1F) == 0x07) {
                subDer = der.getData().getDerValue();
                if ((subDer.getTag() & (byte) 0x1F) == (byte) 0x07) {
                    seqNumber = subDer.getData().getBigInteger().intValue();
                }
            }
        } else {
            seqNumber = null;
            authorizationData = null;
        }
        if (der.getData().available() > 0) {
            authorizationData = AuthorizationData.parse(der.getData(), (byte) 0x08, true);
        } else {
            authorizationData = null;
        }
        if (der.getData().available() > 0) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
    }

    /**
     * Encodes an Authenticator object.
     * @return byte array of encoded Authenticator object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        Vector<DerValue> v = new Vector<>();
        DerOutputStream temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(authenticator_vno));
        v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x00), temp.toByteArray()));
        v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x01), cname.getRealm().asn1Encode()));
        v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x02), cname.asn1Encode()));
        if (cksum != null) {
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x03), cksum.asn1Encode()));
        }
        temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(cusec));
        v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x04), temp.toByteArray()));
        v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x05), ctime.asn1Encode()));
        if (subKey != null) {
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x06), subKey.asn1Encode()));
        }
        if (seqNumber != null) {
            temp = new DerOutputStream();
            // encode as an unsigned integer (UInt32)
            temp.putInteger(BigInteger.valueOf(seqNumber.longValue()));
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x07), temp.toByteArray()));
        }
        if (authorizationData != null) {
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x08), authorizationData.asn1Encode()));
        }
        DerValue[] der = new DerValue[v.size()];
        v.copyInto(der);
        temp = new DerOutputStream();
        temp.putSequence(der);
        DerOutputStream out = new DerOutputStream();
        out.write(DerValue.createTag(DerValue.TAG_APPLICATION, true, (byte) 0x02), temp);
        return out.toByteArray();
    }

    public final Checksum getChecksum() {
        return cksum;
    }

    public final Integer getSeqNumber() {
        return seqNumber;
    }

    public final EncryptionKey getSubKey() {
        return subKey;
    }
}
