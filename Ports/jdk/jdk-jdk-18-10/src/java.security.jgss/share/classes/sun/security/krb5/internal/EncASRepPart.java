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
import java.io.IOException;

public class EncASRepPart extends EncKDCRepPart {

    public EncASRepPart(
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
            PAData[] new_pAData) {
        super(
                new_key,
                new_lastReq,
                new_nonce,
                new_keyExpiration,
                new_flags,
                new_authtime,
                new_starttime,
                new_endtime,
                new_renewTill,
                new_sname,
                new_caddr,
                new_pAData,
                Krb5.KRB_ENC_AS_REP_PART
                );
        //may need to use Krb5.KRB_ENC_TGS_REP_PART to mimic
        //behavior of other implementaions, instead of above
    }

    public EncASRepPart(byte[] data) throws Asn1Exception,
            IOException, KrbException {
        init(new DerValue(data));
    }

    public EncASRepPart(DerValue encoding) throws Asn1Exception,
            IOException, KrbException {
        init(encoding);
    }

    private void init(DerValue encoding) throws Asn1Exception,
            IOException, KrbException {
        init(encoding, Krb5.KRB_ENC_AS_REP_PART);
    }

    public byte[] asn1Encode() throws Asn1Exception,
            IOException {
        return asn1Encode(Krb5.KRB_ENC_AS_REP_PART);
    }
}
