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

package sun.security.krb5;

import sun.security.krb5.internal.*;

abstract class KrbAppMessage {

    private static boolean DEBUG = Krb5.DEBUG;
    /**
     * Common checks for KRB-PRIV and KRB-SAFE
     */
    void check(KerberosTime packetTimestamp,
               Integer packetUsec,
               Integer packetSeqNumber,
               HostAddress packetSAddress,
               HostAddress packetRAddress,
               SeqNumber seqNumber,
               HostAddress sAddress,
               HostAddress rAddress,
               boolean timestampRequired,
               boolean seqNumberRequired,
               PrincipalName packetPrincipal)
        throws KrbApErrException {

        if (!Krb5.AP_EMPTY_ADDRESSES_ALLOWED || sAddress != null) {
            if (packetSAddress == null || sAddress == null ||
                !packetSAddress.equals(sAddress)) {
                if (DEBUG && packetSAddress == null) {
                    System.out.println("packetSAddress is null");
                }
                if (DEBUG && sAddress == null) {
                    System.out.println("sAddress is null");
                }
                throw new KrbApErrException(Krb5.KRB_AP_ERR_BADADDR);
            }
        }

        if (!Krb5.AP_EMPTY_ADDRESSES_ALLOWED || rAddress != null) {
            if (packetRAddress == null || rAddress == null ||
                !packetRAddress.equals(rAddress))
                throw new KrbApErrException(Krb5.KRB_AP_ERR_BADADDR);
        }

        if (packetTimestamp != null) {
            if (packetUsec != null) {
                packetTimestamp =
                    packetTimestamp.withMicroSeconds(packetUsec.intValue());
            }
            if (!packetTimestamp.inClockSkew()) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_SKEW);
            }
        } else {
            if (timestampRequired) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_SKEW);
            }
        }

        // XXX check replay cache
        // if (rcache.repeated(packetTimestamp, packetUsec, packetSAddress))
        //      throw new KrbApErrException(Krb5.KRB_AP_ERR_REPEAT);

        // XXX consider moving up to api level
        if (seqNumber == null && seqNumberRequired == true)
            throw new KrbApErrException(Krb5.API_INVALID_ARG);

        if (packetSeqNumber != null && seqNumber != null) {
            if (packetSeqNumber.intValue() != seqNumber.current())
                throw new KrbApErrException(Krb5.KRB_AP_ERR_BADORDER);
            // should be done only when no more exceptions are possible
            seqNumber.step();
        } else {
            if (seqNumberRequired) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_BADORDER);
            }
        }

        // Must not be relaxed, per RFC 4120
        if (packetTimestamp == null && packetSeqNumber == null)
            throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);

        // XXX check replay cache
        // rcache.save_identifier(packetTimestamp, packetUsec, packetSAddress,
        // packetPrincipal, pcaketRealm);
    }

}
