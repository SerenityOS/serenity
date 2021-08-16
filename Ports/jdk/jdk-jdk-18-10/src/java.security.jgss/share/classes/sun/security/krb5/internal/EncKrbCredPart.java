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
import sun.security.krb5.RealmException;
import java.util.Vector;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Implements the ASN.1 EncKrbCredPart type.
 *
 * <pre>{@code
 * EncKrbCredPart  ::= [APPLICATION 29] SEQUENCE {
 *         ticket-info     [0] SEQUENCE OF KrbCredInfo,
 *         nonce           [1] UInt32 OPTIONAL,
 *         timestamp       [2] KerberosTime OPTIONAL,
 *         usec            [3] Microseconds OPTIONAL,
 *         s-address       [4] HostAddress OPTIONAL,
 *         r-address       [5] HostAddress OPTIONAL
 *   }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */
public class EncKrbCredPart {

    public KrbCredInfo[] ticketInfo = null;
    public KerberosTime timeStamp; //optional
    private Integer nonce; //optional
    private Integer usec; //optional
    private HostAddress sAddress; //optional
    private HostAddresses rAddress; //optional

    public EncKrbCredPart(
            KrbCredInfo[] new_ticketInfo,
            KerberosTime new_timeStamp,
            Integer new_usec,
            Integer new_nonce,
            HostAddress new_sAddress,
            HostAddresses new_rAddress) throws IOException {
        if (new_ticketInfo != null) {
            ticketInfo = new KrbCredInfo[new_ticketInfo.length];
            for (int i = 0; i < new_ticketInfo.length; i++) {
                if (new_ticketInfo[i] == null) {
                    throw new IOException("Cannot create a EncKrbCredPart");
                } else {
                    ticketInfo[i] = (KrbCredInfo) new_ticketInfo[i].clone();
                }
            }
        }
        timeStamp = new_timeStamp;
        usec = new_usec;
        nonce = new_nonce;
        sAddress = new_sAddress;
        rAddress = new_rAddress;
    }

    public EncKrbCredPart(byte[] data) throws Asn1Exception,
            IOException, RealmException {
        init(new DerValue(data));
    }

    public EncKrbCredPart(DerValue encoding) throws Asn1Exception,
            IOException, RealmException {
        init(encoding);
    }

    /**
     * Initializes an EncKrbCredPart object.
     * @param encoding a single DER-encoded value.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception RealmException if an error occurs while parsing a Realm object.
     */
    private void init(DerValue encoding) throws Asn1Exception,
            IOException, RealmException {
        DerValue der, subDer;
        //may not be the correct error code for a tag
        //mismatch on an encrypted structure
        nonce = null;
        timeStamp = null;
        usec = null;
        sAddress = null;
        rAddress = null;
        if (((encoding.getTag() & (byte) 0x1F) != (byte) 0x1D)
                || (encoding.isApplication() != true)
                || (encoding.isConstructed() != true)) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if (der.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & (byte) 0x1F) == (byte) 0x00) {
            DerValue[] derValues = subDer.getData().getSequence(1);
            ticketInfo = new KrbCredInfo[derValues.length];
            for (int i = 0; i < derValues.length; i++) {
                ticketInfo[i] = new KrbCredInfo(derValues[i]);
            }
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        if (der.getData().available() > 0) {
            if (((byte) (der.getData().peekByte()) & (byte) 0x1F) == (byte) 0x01) {
                subDer = der.getData().getDerValue();
                nonce = subDer.getData().getBigInteger().intValue();
            }
        }
        if (der.getData().available() > 0) {
            timeStamp = KerberosTime.parse(der.getData(), (byte) 0x02, true);
        }
        if (der.getData().available() > 0) {
            if (((byte) (der.getData().peekByte()) & (byte) 0x1F) == (byte) 0x03) {
                subDer = der.getData().getDerValue();
                usec = subDer.getData().getBigInteger().intValue();
            }
        }
        if (der.getData().available() > 0) {
            sAddress = HostAddress.parse(der.getData(), (byte) 0x04, true);
        }
        if (der.getData().available() > 0) {
            rAddress = HostAddresses.parse(der.getData(), (byte) 0x05, true);
        }
        if (der.getData().available() > 0) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
    }

    /**
     * Encodes an EncKrbCredPart object.
     * @return byte array of encoded EncKrbCredPart object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     *
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();
        DerValue[] tickets = new DerValue[ticketInfo.length];
        for (int i = 0; i < ticketInfo.length; i++) {
            tickets[i] = new DerValue(ticketInfo[i].asn1Encode());
        }
        temp.putSequence(tickets);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x00), temp);

        if (nonce != null) {
            temp = new DerOutputStream();
            temp.putInteger(BigInteger.valueOf(nonce.intValue()));
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x01), temp);
        }
        if (timeStamp != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x02), timeStamp.asn1Encode());
        }
        if (usec != null) {
            temp = new DerOutputStream();
            temp.putInteger(BigInteger.valueOf(usec.intValue()));
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x03), temp);
        }
        if (sAddress != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x04), sAddress.asn1Encode());
        }
        if (rAddress != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x05), rAddress.asn1Encode());
        }
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        bytes = new DerOutputStream();
        bytes.write(DerValue.createTag(DerValue.TAG_APPLICATION,
                true, (byte) 0x1D), temp);
        return bytes.toByteArray();
    }
}
