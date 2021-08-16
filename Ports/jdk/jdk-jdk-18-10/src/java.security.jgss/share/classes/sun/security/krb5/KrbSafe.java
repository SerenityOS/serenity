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

import sun.security.krb5.EncryptionKey;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.crypto.*;
import java.io.IOException;

class KrbSafe extends KrbAppMessage {

    private byte[] obuf;
    private byte[] userData;

    public KrbSafe(byte[] userData,
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

        obuf = mk_safe(userData,
                       reqKey,
                       timestamp,
                       seqNumber,
                       saddr,
                       raddr
                       );
    }

    public KrbSafe(byte[] msg,
                   Credentials creds,
                   EncryptionKey subKey,
                   SeqNumber seqNumber,
                   HostAddress saddr,
                   HostAddress raddr,
                   boolean timestampRequired,
                   boolean seqNumberRequired
                   )  throws KrbException, IOException {

        KRBSafe krb_safe = new KRBSafe(msg);

        EncryptionKey reqKey = null;
        if (subKey != null)
            reqKey = subKey;
        else
            reqKey = creds.key;

        userData = rd_safe(
                           krb_safe,
                           reqKey,
                           seqNumber,
                           saddr,
                           raddr,
                           timestampRequired,
                           seqNumberRequired,
                           creds.client
                           );
    }

    public byte[] getMessage() {
        return obuf;
    }

    public byte[] getData() {
        return userData;
    }

    private  byte[] mk_safe(byte[] userData,
                            EncryptionKey key,
                            KerberosTime timestamp,
                            SeqNumber seqNumber,
                            HostAddress sAddress,
                            HostAddress rAddress
                            ) throws Asn1Exception, IOException, KdcErrException,
                            KrbApErrException, KrbCryptoException {

                                Integer usec = null;
                                Integer seqno = null;

                                if (timestamp != null)
                                usec = timestamp.getMicroSeconds();

                                if (seqNumber != null) {
                                    seqno = seqNumber.current();
                                    seqNumber.step();
                                }

                                KRBSafeBody krb_safeBody =
                                new KRBSafeBody(userData,
                                                timestamp,
                                                usec,
                                                seqno,
                                                sAddress,
                                                rAddress
                                                );

                                byte[] temp = krb_safeBody.asn1Encode();
                                Checksum cksum = new Checksum(
                                    Checksum.SAFECKSUMTYPE_DEFAULT,
                                    temp,
                                    key,
                                    KeyUsage.KU_KRB_SAFE_CKSUM
                                    );

                                KRBSafe krb_safe = new KRBSafe(krb_safeBody, cksum);

                                temp = krb_safe.asn1Encode();

                                return krb_safe.asn1Encode();
                            }

    private byte[] rd_safe(KRBSafe krb_safe,
                           EncryptionKey key,
                           SeqNumber seqNumber,
                           HostAddress sAddress,
                           HostAddress rAddress,
                           boolean timestampRequired,
                           boolean seqNumberRequired,
                           PrincipalName cname
                           ) throws Asn1Exception, KdcErrException,
                           KrbApErrException, IOException, KrbCryptoException {

                               byte[] temp = krb_safe.safeBody.asn1Encode();

                               if (!krb_safe.cksum.verifyKeyedChecksum(temp, key,
                                   KeyUsage.KU_KRB_SAFE_CKSUM)) {
                                       throw new KrbApErrException(
                                           Krb5.KRB_AP_ERR_MODIFIED);
                               }

                               check(krb_safe.safeBody.timestamp,
                                     krb_safe.safeBody.usec,
                                     krb_safe.safeBody.seqNumber,
                                     krb_safe.safeBody.sAddress,
                                     krb_safe.safeBody.rAddress,
                                     seqNumber,
                                     sAddress,
                                     rAddress,
                                     timestampRequired,
                                     seqNumberRequired,
                                     cname
                                     );

                               return krb_safe.safeBody.userData;
                           }
}
