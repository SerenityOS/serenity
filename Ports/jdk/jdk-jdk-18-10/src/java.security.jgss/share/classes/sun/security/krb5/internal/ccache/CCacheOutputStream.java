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

package sun.security.krb5.internal.ccache;

import java.io.IOException;
import java.io.OutputStream;
import sun.security.krb5.internal.util.KrbDataOutputStream;
import sun.security.krb5.*;
import sun.security.krb5.internal.*;

/**
 * This class implements a buffered output stream. It provides functions to write FCC-format data to a disk file.
 *
 * @author Yanni Zhang
 *
 */
public class CCacheOutputStream extends KrbDataOutputStream implements FileCCacheConstants {
    public CCacheOutputStream(OutputStream os) {
        super(os);
    }

    public void writeHeader(PrincipalName p, int version) throws IOException {
        write((version & 0xff00) >> 8);
        write(version & 0x00ff);
        p.writePrincipal(this);
    }

    /**
     * Writes a credentials in FCC format to this cache output stream.
     *
     * @param creds the credentials to be written to the output stream.
     * @exception IOException if an I/O exception occurs.
     * @exception Asn1Exception  if an Asn1Exception occurs.
     */
    /*For object data fields which themselves have multiple data fields, such as PrincipalName, EncryptionKey
      HostAddresses, AuthorizationData, I created corresponding write methods (writePrincipal,
      writeKey,...) in each class, since converting the object into FCC format data stream
      should be encapsulated in object itself.
    */
    public void addCreds(Credentials creds) throws IOException, Asn1Exception {
        creds.cname.writePrincipal(this);
        creds.sname.writePrincipal(this);
        creds.key.writeKey(this);
        write32((int)(creds.authtime.getTime()/1000));
        if (creds.starttime != null)
            write32((int)(creds.starttime.getTime()/1000));
        else write32(0);
        write32((int)(creds.endtime.getTime()/1000));
        if (creds.renewTill != null)
            write32((int)(creds.renewTill.getTime()/1000));

        else write32(0);
        if (creds.isEncInSKey) {
            write8(1);
        }
        else write8(0);
        writeFlags(creds.flags);
        if (creds.caddr == null)
            write32(0);
        else
            creds.caddr.writeAddrs(this);

        if (creds.authorizationData == null) {
            write32(0);
        }
        else
            creds.authorizationData.writeAuth(this);
        writeTicket(creds.ticket);
        writeTicket(creds.secondTicket);
    }

    public void addConfigEntry(PrincipalName cname, CredentialsCache.ConfigEntry e)
            throws IOException {
        cname.writePrincipal(this);
        e.getSName().writePrincipal(this);
        write16(0); write16(0); write32(0);
        write32(0); write32(0); write32(0); write32(0);
        write8(0);
        write32(0);
        write32(0);
        write32(0);
        write32(e.getData().length);
        write(e.getData());
        write32(0);
    }

    void writeTicket(Ticket t) throws IOException, Asn1Exception {
        if (t == null) {
            write32(0);
        }
        else {
            byte[] bytes = t.asn1Encode();
            write32(bytes.length);
            write(bytes, 0, bytes.length);
        }
    }

    void writeFlags(TicketFlags flags) throws IOException {
        int tFlags = 0;
        boolean[] f = flags.toBooleanArray();
        if (f[1] == true) {
            tFlags |= TKT_FLG_FORWARDABLE;
        }
        if (f[2] == true) {
            tFlags |= TKT_FLG_FORWARDED;
        }
        if (f[3] == true) {
            tFlags |= TKT_FLG_PROXIABLE;
        }
        if (f[4] == true) {
            tFlags |= TKT_FLG_PROXY;
        }
        if (f[5] == true) {
            tFlags |= TKT_FLG_MAY_POSTDATE;
        }
        if (f[6] == true) {
            tFlags |= TKT_FLG_POSTDATED;
        }
        if (f[7] == true) {
            tFlags |= TKT_FLG_INVALID;
        }
        if (f[8] == true) {
            tFlags |= TKT_FLG_RENEWABLE;
        }
        if (f[9] == true) {
            tFlags |= TKT_FLG_INITIAL;
        }
        if (f[10] == true) {
            tFlags |= TKT_FLG_PRE_AUTH;
        }
        if (f[11] == true) {
            tFlags |= TKT_FLG_HW_AUTH;
        }
        write32(tFlags);

    }
}
