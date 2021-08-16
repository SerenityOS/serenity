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
import sun.security.krb5.internal.crypto.KeyUsage;
import sun.security.util.DerInputStream;

abstract class KrbKdcRep {

    static void check(
                      boolean isAsReq,
                      KDCReq req,
                      KDCRep rep,
                      EncryptionKey replyKey
                      ) throws KrbApErrException {

        // cname change in AS-REP is allowed only if the client
        // sent CANONICALIZE or an NT-ENTERPRISE cname in the request, and the
        // server supports RFC 6806 - Section 11 FAST scheme (ENC-PA-REP flag).
        if (isAsReq && !req.reqBody.cname.equals(rep.cname) &&
                ((!req.reqBody.kdcOptions.get(KDCOptions.CANONICALIZE) &&
                req.reqBody.cname.getNameType() !=
                PrincipalName.KRB_NT_ENTERPRISE) ||
                !rep.encKDCRepPart.flags.get(Krb5.TKT_OPTS_ENC_PA_REP))) {
            rep.encKDCRepPart.key.destroy();
            throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
        }

        // sname change in TGS-REP is allowed only if client
        // sent CANONICALIZE and new sname is a referral of
        // the form krbtgt/TO-REALM.COM@FROM-REALM.COM.
        if (!req.reqBody.sname.equals(rep.encKDCRepPart.sname)) {
            String[] snameStrings = rep.encKDCRepPart.sname.getNameStrings();
            if (isAsReq || !req.reqBody.kdcOptions.get(KDCOptions.CANONICALIZE) ||
                    snameStrings == null || snameStrings.length != 2 ||
                    !snameStrings[0].equals(PrincipalName.TGS_DEFAULT_SRV_NAME) ||
                    !rep.encKDCRepPart.sname.getRealmString().equals(
                            req.reqBody.sname.getRealmString())) {
                rep.encKDCRepPart.key.destroy();
                throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
            }
        }

        if (req.reqBody.getNonce() != rep.encKDCRepPart.nonce) {
            rep.encKDCRepPart.key.destroy();
            throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
        }

        if (
            ((req.reqBody.addresses != null && rep.encKDCRepPart.caddr != null) &&
             !req.reqBody.addresses.equals(rep.encKDCRepPart.caddr))) {
            rep.encKDCRepPart.key.destroy();
            throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
        }

        // We allow KDC to return a non-forwardable ticket if request has -f
        for (int i = 2; i < 6; i++) {
            if (req.reqBody.kdcOptions.get(i) !=
                   rep.encKDCRepPart.flags.get(i)) {
                if (Krb5.DEBUG) {
                    System.out.println("> KrbKdcRep.check: at #" + i
                            + ". request for " + req.reqBody.kdcOptions.get(i)
                            + ", received " + rep.encKDCRepPart.flags.get(i));
                }
                throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
            }
        }

        // Reply to a renewable request should be renewable, but if request does
        // not contain renewable, KDC is free to issue a renewable ticket (for
        // example, if ticket_lifetime is too big).
        if (req.reqBody.kdcOptions.get(KDCOptions.RENEWABLE) &&
                !rep.encKDCRepPart.flags.get(KDCOptions.RENEWABLE)) {
            throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
        }

        if ((req.reqBody.from == null) || req.reqBody.from.isZero()) {
            // verify this is allowed
            if ((rep.encKDCRepPart.starttime != null) &&
                    !rep.encKDCRepPart.starttime.inClockSkew()) {
                rep.encKDCRepPart.key.destroy();
                throw new KrbApErrException(Krb5.KRB_AP_ERR_SKEW);
            }
        }

        if ((req.reqBody.from != null) && !req.reqBody.from.isZero()) {
            // verify this is allowed
            if ((rep.encKDCRepPart.starttime != null) &&
                    !req.reqBody.from.equals(rep.encKDCRepPart.starttime)) {
                rep.encKDCRepPart.key.destroy();
                throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
            }
        }

        if (!req.reqBody.till.isZero() &&
                rep.encKDCRepPart.endtime.greaterThan(req.reqBody.till)) {
            rep.encKDCRepPart.key.destroy();
            throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
        }

        if (req.reqBody.kdcOptions.get(KDCOptions.RENEWABLE)) {
            if (req.reqBody.rtime != null && !req.reqBody.rtime.isZero()) {
                // verify this is required
                if ((rep.encKDCRepPart.renewTill == null) ||
                        rep.encKDCRepPart.renewTill.greaterThan(req.reqBody.rtime)
                        ) {
                    rep.encKDCRepPart.key.destroy();
                    throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
                }
            }
        }

        // RFC 6806 - Section 11 mechanism check
        // The availability of the ENC-PA-REP flag in the KDC response is
        // mandatory on some cases (see Krb5.TKT_OPTS_ENC_PA_REP check above).
        if (rep.encKDCRepPart.flags.get(Krb5.TKT_OPTS_ENC_PA_REP)) {
            boolean reqPaReqEncPaRep = false;
            boolean repPaReqEncPaRepValid = false;

            if (req.pAData != null) {
                for (PAData pa : req.pAData) {
                    if (pa.getType() == Krb5.PA_REQ_ENC_PA_REP) {
                        // The KDC supports RFC 6806 and ENC-PA-REP was sent in
                        // the request (AS-REQ). A valid checksum is now required.
                        reqPaReqEncPaRep = true;
                        break;
                    }
                }
            }

            if (rep.encKDCRepPart.pAData != null) {
                for (PAData pa : rep.encKDCRepPart.pAData) {
                    if (pa.getType() == Krb5.PA_REQ_ENC_PA_REP) {
                        try {
                            Checksum repCksum = new Checksum(
                                    new DerInputStream(
                                            pa.getValue()).getDerValue());
                            // The checksum is inside encKDCRepPart so we don't
                            // care if it's keyed or not.
                            repPaReqEncPaRepValid =
                                    repCksum.verifyAnyChecksum(
                                            req.asn1Encode(), replyKey,
                                            KeyUsage.KU_AS_REQ);
                        } catch (Exception e) {
                            if (Krb5.DEBUG) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }

            if (reqPaReqEncPaRep && !repPaReqEncPaRepValid) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_MODIFIED);
            }
        }
    }
}
