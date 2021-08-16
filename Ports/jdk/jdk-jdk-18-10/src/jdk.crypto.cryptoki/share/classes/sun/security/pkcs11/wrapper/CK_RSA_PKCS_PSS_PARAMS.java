/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11.wrapper;

import java.security.ProviderException;
import java.security.spec.PSSParameterSpec;
import java.security.spec.MGF1ParameterSpec;


/**
 * This class represents the necessary parameters required by the
 * CKM_RSA_PKCS_PSS mechanism as defined in CK_RSA_PKCS_PSS_PARAMS structure.<p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 * typedef struct CK_RSA_PKCS_PSS_PARAMS {
 *    CK_MECHANISM_TYPE    hashAlg;
 *    CK_RSA_PKCS_MGF_TYPE mgf;
 *    CK_ULONG             sLen;
 * } CK_RSA_PKCS_PSS_PARAMS;
 * </PRE>
 *
 * @since   13
 */
public class CK_RSA_PKCS_PSS_PARAMS {

    private final long hashAlg;
    private final long mgf;
    private final long sLen;

    public CK_RSA_PKCS_PSS_PARAMS(String hashAlg, String mgfAlg,
            String mgfHash, int sLen) {
        this.hashAlg = Functions.getHashMechId(hashAlg);
        if (!mgfAlg.equals("MGF1")) {
            throw new ProviderException("Only MGF1 is supported");
        }
        // no dash in PKCS#11 mechanism names
        if (mgfHash.startsWith("SHA3-")) {
            mgfHash = mgfHash.replaceFirst("-", "_");
        } else {
            mgfHash = mgfHash.replaceFirst("-", "");
        }
        this.mgf = Functions.getMGFId("CKG_MGF1_" + mgfHash);
        this.sLen = sLen;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof CK_RSA_PKCS_PSS_PARAMS)) {
            return false;
        }

        CK_RSA_PKCS_PSS_PARAMS other = (CK_RSA_PKCS_PSS_PARAMS) o;
        return ((other.hashAlg == hashAlg) &&
                (other.mgf == mgf) &&
                (other.sLen == sLen));
    }

    @Override
    public int hashCode() {
        return (int)(hashAlg << 2 + mgf << 1 + sLen);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(Constants.INDENT);
        sb.append("hashAlg: ");
        sb.append(Functions.toFullHexString(hashAlg));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("mgf: ");
        sb.append(Functions.toFullHexString(mgf));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("sLen(in bytes): ");
        sb.append(sLen);

        return sb.toString();
    }
}
