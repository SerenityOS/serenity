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

import sun.security.krb5.PrincipalName;
import sun.security.krb5.EncryptedData;
import sun.security.krb5.Asn1Exception;
import sun.security.krb5.Realm;
import sun.security.krb5.RealmException;
import sun.security.util.*;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Implements the ASN.1 Ticket type.
 *
 * <pre>{@code
 * Ticket               ::= [APPLICATION 1] SEQUENCE {
 *      tkt-vno         [0] INTEGER (5),
 *      realm           [1] Realm,
 *      sname           [2] PrincipalName,
 *      enc-part        [3] EncryptedData -- EncTicketPart
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */

public class Ticket implements Cloneable {
    public int tkt_vno;
    public PrincipalName sname;
    public EncryptedData encPart;

    private Ticket() {
    }

    public Object clone() {
        Ticket new_ticket = new Ticket();
        new_ticket.sname = (PrincipalName)sname.clone();
        new_ticket.encPart = (EncryptedData)encPart.clone();
        new_ticket.tkt_vno = tkt_vno;
        return new_ticket;
    }

    public Ticket(
                  PrincipalName new_sname,
                  EncryptedData new_encPart
                      ) {
        tkt_vno = Krb5.TICKET_VNO;
        sname = new_sname;
        encPart = new_encPart;
    }

    // Warning: called by NativeCreds.c and nativeccache.c
    public Ticket(byte[] data) throws Asn1Exception,
    RealmException, KrbApErrException, IOException {
        init(new DerValue(data));
    }

    public Ticket(DerValue encoding) throws Asn1Exception,
    RealmException, KrbApErrException, IOException {
        init(encoding);
    }

    /**
     * Initializes a Ticket object.
     * @param encoding a single DER-encoded value.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception KrbApErrException if the value read from the DER-encoded data stream does not match the pre-defined value.
     * @exception RealmException if an error occurs while parsing a Realm object.
     */

    private void init(DerValue encoding) throws Asn1Exception,
    RealmException, KrbApErrException, IOException {
        DerValue der;
        DerValue subDer;
        if (((encoding.getTag() & (byte)0x1F) != Krb5.KRB_TKT)
            || (encoding.isApplication() != true)
            || (encoding.isConstructed() != true))
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        der = encoding.getData().getDerValue();
        if (der.getTag() != DerValue.tag_Sequence)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & (byte)0x1F) != (byte)0x00)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        tkt_vno = subDer.getData().getBigInteger().intValue();
        if (tkt_vno != Krb5.TICKET_VNO)
            throw new KrbApErrException(Krb5.KRB_AP_ERR_BADVERSION);
        Realm srealm = Realm.parse(der.getData(), (byte)0x01, false);
        sname = PrincipalName.parse(der.getData(), (byte)0x02, false, srealm);
        encPart = EncryptedData.parse(der.getData(), (byte)0x03, false);
        if (der.getData().available() > 0)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
    }

    /**
     * Encodes a Ticket object.
     * @return byte array of encoded ticket object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();
        DerValue[] der = new DerValue[4];
        temp.putInteger(BigInteger.valueOf(tkt_vno));
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x00), temp);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x01), sname.getRealm().asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x02), sname.asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x03), encPart.asn1Encode());
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        DerOutputStream ticket = new DerOutputStream();
        ticket.write(DerValue.createTag(DerValue.TAG_APPLICATION, true, (byte)0x01), temp);
        return ticket.toByteArray();
    }

    /**
     * Parse (unmarshal) a Ticket from a DER input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @exception Asn1Exception on error.
     * @param data the Der input stream value, which contains one or more marshaled value.
     * @param explicitTag tag number.
     * @param optional indicate if this data field is optional
     * @return an instance of Ticket.
     */
    public static Ticket parse(DerInputStream data, byte explicitTag, boolean optional) throws Asn1Exception, IOException, RealmException, KrbApErrException {
        if ((optional) && (((byte)data.peekByte() & (byte)0x1F)!= explicitTag))
            return null;
        DerValue der = data.getDerValue();
        if (explicitTag != (der.getTag() & (byte)0x1F))  {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        else {
            DerValue subDer = der.getData().getDerValue();
            return new Ticket(subDer);
        }
    }


}
