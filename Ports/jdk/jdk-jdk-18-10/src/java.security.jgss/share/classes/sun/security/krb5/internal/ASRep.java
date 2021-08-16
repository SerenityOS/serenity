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

public class ASRep extends KDCRep {

    public ASRep(
            PAData[] new_pAData,
            PrincipalName new_cname,
            Ticket new_ticket,
            EncryptedData new_encPart) throws IOException {
        super(new_pAData, new_cname, new_ticket,
                new_encPart, Krb5.KRB_AS_REP);
    }

    public ASRep(byte[] data) throws Asn1Exception,
            RealmException, KrbApErrException, IOException {
        init(new DerValue(data));
    }

    public ASRep(DerValue encoding) throws Asn1Exception,
            RealmException, KrbApErrException, IOException {
        init(encoding);
    }

    private void init(DerValue encoding) throws Asn1Exception,
            RealmException, KrbApErrException, IOException {
        init(encoding, Krb5.KRB_AS_REP);
    }
}
