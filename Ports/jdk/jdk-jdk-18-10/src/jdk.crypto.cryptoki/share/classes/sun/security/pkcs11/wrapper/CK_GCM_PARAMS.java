/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This class represents the necessary parameters required by
 * the CKM_AES_GCM mechanism as defined in CK_GCM_PARAMS structure.<p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 * typedef struct CK_GCM_PARAMS {
 *    CK_BYTE_PTR       pIv;
 *    CK_ULONG          ulIvLen;
 *    CK_BYTE_PTR       pAAD;
 *    CK_ULONG          ulAADLen;
 *    CK_ULONG          ulTagBits;
 * } CK_GCM_PARAMS;
 * </PRE>
 *
 * @since   10
 */
public class CK_GCM_PARAMS {

    private final byte[] iv;
    private final byte[] aad;
    private final long tagBits;

    public CK_GCM_PARAMS(int tagLenInBits, byte[] iv, byte[] aad) {
        this.iv = iv;
        this.aad = aad;
        this.tagBits = tagLenInBits;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(Constants.INDENT);
        sb.append("iv: ");
        sb.append(Functions.toHexString(iv));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("aad: ");
        sb.append(Functions.toHexString(aad));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("tagLen(in bits): ");
        sb.append(tagBits);

        return sb.toString();
    }
}
