/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 */

package sun.security.krb5.internal.crypto;

/**
 * Key usages used for key derivation in Kerberos.
 */
public class KeyUsage {

    private KeyUsage() {
    }

    public static final int KU_UNKNOWN = 0;                     // Cannot be 0

    // Defined in draft-yu-krb-wg-kerberos-extensions-00.txt, Appendix A
    public static final int KU_PA_ENC_TS = 1;                   // KrbAsReq
    public static final int KU_TICKET = 2;                      // KrbApReq (ticket)
    public static final int KU_ENC_AS_REP_PART = 3;             // KrbAsRep
    public static final int KU_TGS_REQ_AUTH_DATA_SESSKEY= 4;    // KrbTgsReq
    public static final int KU_TGS_REQ_AUTH_DATA_SUBKEY = 5;    // KrbTgsReq
    public static final int KU_PA_TGS_REQ_CKSUM = 6;            // KrbTgsReq
    public static final int KU_PA_TGS_REQ_AUTHENTICATOR = 7;    // KrbApReq
    public static final int KU_ENC_TGS_REP_PART_SESSKEY = 8;    // KrbTgsRep
    public static final int KU_ENC_TGS_REP_PART_SUBKEY = 9;     // KrbTgsRep
    public static final int KU_AUTHENTICATOR_CKSUM = 10;
    public static final int KU_AP_REQ_AUTHENTICATOR = 11;       // KrbApReq
    public static final int KU_ENC_AP_REP_PART = 12;            // KrbApRep
    public static final int KU_ENC_KRB_PRIV_PART = 13;          // KrbPriv
    public static final int KU_ENC_KRB_CRED_PART = 14;          // KrbCred
    public static final int KU_KRB_SAFE_CKSUM = 15;             // KrbSafe
    public static final int KU_PA_FOR_USER_ENC_CKSUM = 17;      // S4U2user
    public static final int KU_AD_KDC_ISSUED_CKSUM = 19;
    public static final int KU_AS_REQ = 56;

    public static final boolean isValid(int usage) {
        return usage >= 0;
    }
}
