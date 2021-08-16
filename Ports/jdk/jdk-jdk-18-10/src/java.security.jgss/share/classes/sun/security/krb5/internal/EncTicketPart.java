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
import java.io.*;

/**
 * Implements the ASN.1 EncTicketPart type.
 *
 * <pre>{@code
 * EncTicketPart   ::= [APPLICATION 3] SEQUENCE {
 *         flags                   [0] TicketFlags,
 *         key                     [1] EncryptionKey,
 *         crealm                  [2] Realm,
 *         cname                   [3] PrincipalName,
 *         transited               [4] TransitedEncoding,
 *         authtime                [5] KerberosTime,
 *         starttime               [6] KerberosTime OPTIONAL,
 *         endtime                 [7] KerberosTime,
 *         renew-till              [8] KerberosTime OPTIONAL,
 *         caddr                   [9] HostAddresses OPTIONAL,
 *         authorization-data      [10] AuthorizationData OPTIONAL
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */
public class EncTicketPart {

    public TicketFlags flags;
    public EncryptionKey key;
    public PrincipalName cname;
    public TransitedEncoding transited;
    public KerberosTime authtime;
    public KerberosTime starttime; //optional
    public KerberosTime endtime;
    public KerberosTime renewTill; //optional
    public HostAddresses caddr; //optional
    public AuthorizationData authorizationData; //optional

    public EncTicketPart(
            TicketFlags new_flags,
            EncryptionKey new_key,
            PrincipalName new_cname,
            TransitedEncoding new_transited,
            KerberosTime new_authtime,
            KerberosTime new_starttime,
            KerberosTime new_endtime,
            KerberosTime new_renewTill,
            HostAddresses new_caddr,
            AuthorizationData new_authorizationData) {
        flags = new_flags;
        key = new_key;
        cname = new_cname;
        transited = new_transited;
        authtime = new_authtime;
        starttime = new_starttime;
        endtime = new_endtime;
        renewTill = new_renewTill;
        caddr = new_caddr;
        authorizationData = new_authorizationData;
    }

    public EncTicketPart(byte[] data)
            throws Asn1Exception, KrbException, IOException {
        init(new DerValue(data));
    }

    public EncTicketPart(DerValue encoding)
            throws Asn1Exception, KrbException, IOException {
        init(encoding);
    }

    /**
     * Initializes an EncTicketPart object.
     * @param encoding a single DER-encoded value.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception RealmException if an error occurs while parsing a Realm object.
     */
    private static String getHexBytes(byte[] bytes, int len)
            throws IOException {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {

            int b1 = (bytes[i] >> 4) & 0x0f;
            int b2 = bytes[i] & 0x0f;

            sb.append(Integer.toHexString(b1));
            sb.append(Integer.toHexString(b2));
            sb.append(' ');
        }
        return sb.toString();
    }

    private void init(DerValue encoding)
            throws Asn1Exception, IOException, RealmException {
        DerValue der, subDer;

        renewTill = null;
        caddr = null;
        authorizationData = null;
        if (((encoding.getTag() & (byte) 0x1F) != (byte) 0x03)
                || (encoding.isApplication() != true)
                || (encoding.isConstructed() != true)) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if (der.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        flags = TicketFlags.parse(der.getData(), (byte) 0x00, false);
        key = EncryptionKey.parse(der.getData(), (byte) 0x01, false);
        Realm crealm = Realm.parse(der.getData(), (byte) 0x02, false);
        cname = PrincipalName.parse(der.getData(), (byte) 0x03, false, crealm);
        transited = TransitedEncoding.parse(der.getData(), (byte) 0x04, false);
        authtime = KerberosTime.parse(der.getData(), (byte) 0x05, false);
        starttime = KerberosTime.parse(der.getData(), (byte) 0x06, true);
        endtime = KerberosTime.parse(der.getData(), (byte) 0x07, false);
        if (der.getData().available() > 0) {
            renewTill = KerberosTime.parse(der.getData(), (byte) 0x08, true);
        }
        if (der.getData().available() > 0) {
            caddr = HostAddresses.parse(der.getData(), (byte) 0x09, true);
        }
        if (der.getData().available() > 0) {
            authorizationData = AuthorizationData.parse(der.getData(), (byte) 0x0A, true);
        }
        if (der.getData().available() > 0) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

    }

    /**
     * Encodes an EncTicketPart object.
     * @return byte array of encoded EncTicketPart object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x00), flags.asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x01), key.asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x02), cname.getRealm().asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x03), cname.asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x04), transited.asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x05), authtime.asn1Encode());
        if (starttime != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x06), starttime.asn1Encode());
        }
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte) 0x07), endtime.asn1Encode());

        if (renewTill != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x08), renewTill.asn1Encode());
        }

        if (caddr != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x09), caddr.asn1Encode());
        }

        if (authorizationData != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte) 0x0A), authorizationData.asn1Encode());
        }
        temp.write(DerValue.tag_Sequence, bytes);
        bytes = new DerOutputStream();
        bytes.write(DerValue.createTag(DerValue.TAG_APPLICATION,
                true, (byte) 0x03), temp);
        return bytes.toByteArray();
    }
}
