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

import sun.security.util.*;
import sun.security.krb5.Asn1Exception;
import java.util.Vector;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Implements the ASN.1 KRBSafeBody type.
 *
 * <pre>{@code
 * KRB-SAFE-BODY   ::= SEQUENCE {
 *         user-data       [0] OCTET STRING,
 *         timestamp       [1] KerberosTime OPTIONAL,
 *         usec            [2] Microseconds OPTIONAL,
 *         seq-number      [3] UInt32 OPTIONAL,
 *         s-address       [4] HostAddress,
 *         r-address       [5] HostAddress OPTIONAL
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */

public class KRBSafeBody {
    public byte[] userData = null;
    public KerberosTime timestamp; //optional
    public Integer usec; //optional
    public Integer seqNumber; //optional
    public HostAddress sAddress;
    public HostAddress rAddress; //optional

    public KRBSafeBody(
                       byte[] new_userData,
                       KerberosTime new_timestamp,
                       Integer new_usec,
                       Integer new_seqNumber,
                       HostAddress new_sAddress,
                       HostAddress new_rAddress
                           ) {
        if (new_userData != null) {
            userData = new_userData.clone();
        }
        timestamp = new_timestamp;
        usec = new_usec;
        seqNumber = new_seqNumber;
        sAddress = new_sAddress;
        rAddress = new_rAddress;
    }


    /**
     * Constructs a KRBSafeBody object.
     * @param encoding a Der-encoded data.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public KRBSafeBody(DerValue encoding) throws Asn1Exception, IOException {
        DerValue der;
        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x00) {
            userData = der.getData().getOctetString();
        }
        else
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        timestamp = KerberosTime.parse(encoding.getData(), (byte)0x01, true);
        if ((encoding.getData().peekByte() & 0x1F) == 0x02) {
            der = encoding.getData().getDerValue();
            usec = der.getData().getBigInteger().intValue();
        }
        if ((encoding.getData().peekByte() & 0x1F) == 0x03) {
            der = encoding.getData().getDerValue();
            seqNumber = der.getData().getBigInteger().intValue();
        }
        sAddress = HostAddress.parse(encoding.getData(), (byte)0x04, false);
        if (encoding.getData().available() > 0)
            rAddress = HostAddress.parse(encoding.getData(), (byte)0x05, true);
        if (encoding.getData().available() > 0)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
    }

    /**
     * Encodes an KRBSafeBody object.
     * @return the byte array of encoded KRBSafeBody object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();
        temp.putOctetString(userData);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x00), temp);
        if (timestamp != null)
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x01), timestamp.asn1Encode());
        if (usec != null) {
            temp = new DerOutputStream();
            temp.putInteger(BigInteger.valueOf(usec.intValue()));
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x02), temp);
        }
        if (seqNumber != null) {
            temp = new DerOutputStream();
            // encode as an unsigned integer (UInt32)
            temp.putInteger(BigInteger.valueOf(seqNumber.longValue()));
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x03), temp);
        }
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x04), sAddress.asn1Encode());
        if (rAddress != null)
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }

    /**
     * Parse (unmarshal) a KRBSafeBody from a DER input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @exception Asn1Exception on error.
     * @param data the Der input stream value, which contains one or more marshaled value.
     * @param explicitTag tag number.
     * @param optional indicates if this data field is optional
     * @return an instance of KRBSafeBody.
     *
     */
    public static KRBSafeBody parse(DerInputStream data, byte explicitTag, boolean optional) throws Asn1Exception, IOException {
        if ((optional) && (((byte)data.peekByte() & (byte)0x1F) != explicitTag))
            return null;
        DerValue der = data.getDerValue();
        if (explicitTag != (der.getTag() & (byte)0x1F))
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        else {
            DerValue subDer = der.getData().getDerValue();
            return new KRBSafeBody(subDer);
        }
    }



}
