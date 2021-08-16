/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.krb5;

import sun.security.krb5.internal.*;
import sun.security.krb5.internal.crypto.*;
import sun.security.util.*;
import java.io.IOException;

/** XXX This class does not appear to be used. **/

class KrbPriv extends KrbAppMessage {
    private byte[] obuf;
    private byte[] userData;

    private KrbPriv(byte[] userData,
                   Credentials creds,
                   EncryptionKey subKey,
                   KerberosTime timestamp,
                   SeqNumber seqNumber,
                   HostAddress saddr,
                   HostAddress raddr
                   )  throws KrbException, IOException {
        EncryptionKey reqKey = null;
        if (subKey != null)
            reqKey = subKey;
        else
            reqKey = creds.key;

        obuf = mk_priv(
                       userData,
                       reqKey,
                       timestamp,
                       seqNumber,
                       saddr,
                       raddr
                       );
    }

    private KrbPriv(byte[] msg,
                   Credentials creds,
                   EncryptionKey subKey,
                   SeqNumber seqNumber,
                   HostAddress saddr,
                   HostAddress raddr,
                   boolean timestampRequired,
                   boolean seqNumberRequired
                   )  throws KrbException, IOException {

        KRBPriv krb_priv = new KRBPriv(msg);
        EncryptionKey reqKey = null;
        if (subKey != null)
            reqKey = subKey;
        else
            reqKey = creds.key;
        userData = rd_priv(krb_priv,
                           reqKey,
                           seqNumber,
                           saddr,
                           raddr,
                           timestampRequired,
                           seqNumberRequired,
                           creds.client
                           );
    }

    public byte[] getMessage() throws KrbException {
        return obuf;
    }

    public byte[] getData() {
        return userData;
    }

    private byte[] mk_priv(byte[] userData,
                           EncryptionKey key,
                           KerberosTime timestamp,
                           SeqNumber seqNumber,
                           HostAddress sAddress,
                           HostAddress rAddress
                           ) throws Asn1Exception, IOException,
                           KdcErrException, KrbCryptoException {

                               Integer usec = null;
                               Integer seqno = null;

                               if (timestamp != null)
                               usec = timestamp.getMicroSeconds();

                               if (seqNumber != null) {
                                   seqno = seqNumber.current();
                                   seqNumber.step();
                               }

                               EncKrbPrivPart unenc_encKrbPrivPart =
                               new EncKrbPrivPart(userData,
                                                  timestamp,
                                                  usec,
                                                  seqno,
                                                  sAddress,
                                                  rAddress
                                                  );

                               byte[] temp = unenc_encKrbPrivPart.asn1Encode();

                               EncryptedData encKrbPrivPart =
                               new EncryptedData(key, temp,
                                   KeyUsage.KU_ENC_KRB_PRIV_PART);

                               KRBPriv krb_priv = new KRBPriv(encKrbPrivPart);

                               temp = krb_priv.asn1Encode();

                               return krb_priv.asn1Encode();
                           }

    private byte[] rd_priv(KRBPriv krb_priv,
                           EncryptionKey key,
                           SeqNumber seqNumber,
                           HostAddress sAddress,
                           HostAddress rAddress,
                           boolean timestampRequired,
                           boolean seqNumberRequired,
                           PrincipalName cname
                           ) throws Asn1Exception, KdcErrException,
                           KrbApErrException, IOException, KrbCryptoException {

                               byte[] bytes = krb_priv.encPart.decrypt(key,
                                   KeyUsage.KU_ENC_KRB_PRIV_PART);
                               byte[] temp = krb_priv.encPart.reset(bytes);
                               DerValue ref = new DerValue(temp);
                               EncKrbPrivPart enc_part = new EncKrbPrivPart(ref);

                               check(enc_part.timestamp,
                                     enc_part.usec,
                                     enc_part.seqNumber,
                                     enc_part.sAddress,
                                     enc_part.rAddress,
                                     seqNumber,
                                     sAddress,
                                     rAddress,
                                     timestampRequired,
                                     seqNumberRequired,
                                     cname
                                     );

                               return enc_part.userData;
                           }
}
