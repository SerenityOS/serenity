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

/**
 * Implements the ASN.1 KrbCredInfo type.
 *
 * <pre>{@code
 * KrbCredInfo  ::= SEQUENCE {
 *      key             [0] EncryptionKey,
 *      prealm          [1] Realm OPTIONAL,
 *      pname           [2] PrincipalName OPTIONAL,
 *      flags           [3] TicketFlags OPTIONAL,
 *      authtime        [4] KerberosTime OPTIONAL,
 *      starttime       [5] KerberosTime OPTIONAL,
 *      endtime         [6] KerberosTime OPTIONAL,
 *      renew-till      [7] KerberosTime OPTIONAL,
 *      srealm          [8] Realm OPTIONAL,
 *      sname           [9] PrincipalName OPTIONAL,
 *      caddr           [10] HostAddresses OPTIONAL
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */

public class KrbCredInfo {
    public EncryptionKey key;
    public PrincipalName pname; //optional
    public TicketFlags flags; //optional
    public KerberosTime authtime; //optional
    public KerberosTime starttime; //optional
    public KerberosTime endtime; //optional
    public KerberosTime renewTill; //optional
    public PrincipalName sname; //optional
    public HostAddresses caddr; //optional

    private KrbCredInfo() {
    }

    public KrbCredInfo(
                       EncryptionKey new_key,
                       PrincipalName new_pname,
                       TicketFlags new_flags,
                       KerberosTime new_authtime,
                       KerberosTime new_starttime,
                       KerberosTime new_endtime,
                       KerberosTime new_renewTill,
                       PrincipalName new_sname,
                       HostAddresses new_caddr
                           ) {
        key = new_key;
        pname = new_pname;
        flags = new_flags;
        authtime = new_authtime;
        starttime = new_starttime;
        endtime = new_endtime;
        renewTill = new_renewTill;
        sname = new_sname;
        caddr = new_caddr;
    }

    /**
     * Constructs a KrbCredInfo object.
     * @param encoding a Der-encoded data.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception RealmException if an error occurs while parsing a Realm object.
     */
    public KrbCredInfo(DerValue encoding)
            throws Asn1Exception, IOException, RealmException{
        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        pname = null;
        flags = null;
        authtime = null;
        starttime = null;
        endtime = null;
        renewTill = null;
        sname = null;
        caddr = null;
        key = EncryptionKey.parse(encoding.getData(), (byte)0x00, false);
        Realm prealm = null, srealm = null;
        if (encoding.getData().available() > 0)
            prealm = Realm.parse(encoding.getData(), (byte)0x01, true);
        if (encoding.getData().available() > 0)
            pname = PrincipalName.parse(encoding.getData(), (byte)0x02, true, prealm);
        if (encoding.getData().available() > 0)
            flags = TicketFlags.parse(encoding.getData(), (byte)0x03, true);
        if (encoding.getData().available() > 0)
            authtime = KerberosTime.parse(encoding.getData(), (byte)0x04, true);
        if (encoding.getData().available() > 0)
            starttime = KerberosTime.parse(encoding.getData(), (byte)0x05, true);
        if (encoding.getData().available() > 0)
            endtime = KerberosTime.parse(encoding.getData(), (byte)0x06, true);
        if (encoding.getData().available() > 0)
            renewTill = KerberosTime.parse(encoding.getData(), (byte)0x07, true);
        if (encoding.getData().available() > 0)
            srealm = Realm.parse(encoding.getData(), (byte)0x08, true);
        if (encoding.getData().available() > 0)
            sname = PrincipalName.parse(encoding.getData(), (byte)0x09, true, srealm);
        if (encoding.getData().available() > 0)
            caddr = HostAddresses.parse(encoding.getData(), (byte)0x0A, true);
        if (encoding.getData().available() > 0)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
    }

    /**
     * Encodes an KrbCredInfo object.
     * @return the byte array of encoded KrbCredInfo object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        Vector<DerValue> v = new Vector<>();
        v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x00), key.asn1Encode()));
        if (pname != null) {
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x01), pname.getRealm().asn1Encode()));
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x02), pname.asn1Encode()));
        }
        if (flags != null)
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x03), flags.asn1Encode()));
        if (authtime != null)
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x04), authtime.asn1Encode()));
        if (starttime != null)
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x05), starttime.asn1Encode()));
        if (endtime != null)
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x06), endtime.asn1Encode()));
        if (renewTill != null)
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x07), renewTill.asn1Encode()));
        if (sname != null) {
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x08), sname.getRealm().asn1Encode()));
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x09), sname.asn1Encode()));
        }
        if (caddr != null)
            v.addElement(new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x0A), caddr.asn1Encode()));
        DerValue[] der = new DerValue[v.size()];
        v.copyInto(der);
        DerOutputStream out = new DerOutputStream();
        out.putSequence(der);
        return out.toByteArray();
    }

    public Object clone() {
        KrbCredInfo kcred = new KrbCredInfo();
        kcred.key = (EncryptionKey)key.clone();
        // optional fields
        if (pname != null)
            kcred.pname = (PrincipalName)pname.clone();
        if (flags != null)
            kcred.flags = (TicketFlags)flags.clone();
        kcred.authtime = authtime;
        kcred.starttime = starttime;
        kcred.endtime = endtime;
        kcred.renewTill = renewTill;
        if (sname != null)
            kcred.sname = (PrincipalName)sname.clone();
        if (caddr != null)
            kcred.caddr = (HostAddresses)caddr.clone();
        return kcred;
    }

}
