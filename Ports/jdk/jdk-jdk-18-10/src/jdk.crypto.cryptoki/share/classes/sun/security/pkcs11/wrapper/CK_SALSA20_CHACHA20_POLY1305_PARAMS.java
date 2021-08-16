/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HexFormat;

/**
 * This class represents the necessary parameters required by the
 * CKM_CHACHA20_POLY1305 and CKM_SALSA20_POLY1305 mechanisms as defined in
 * CK_SALSA20_CHACHA20_POLY1305_PARAMS structure.<p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 * typedef struct CK_SALSA20_CHACHA20_POLY1305_PARAMS {
 *   CK_BYTE_PTR  pNonce;
 *   CK_ULONG     ulNonceLen;
 *   CK_BYTE_PTR  pAAD;
 *   CK_ULONG     ulAADLen;
 * } CK_SALSA20_CHACHA20_POLY1305_PARAMS;
 * </PRE>
 *
 * @since   17
 */
public class CK_SALSA20_CHACHA20_POLY1305_PARAMS {

    private final byte[] nonce;
    private final byte[] aad;

    public CK_SALSA20_CHACHA20_POLY1305_PARAMS(byte[] nonce, byte[] aad) {
        this.nonce = nonce;
        this.aad = aad;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(Constants.INDENT);
        sb.append("Nonce: ");
        sb.append((nonce == null? "null" :
            "0x" + HexFormat.of().formatHex(nonce)));
        sb.append(Constants.NEWLINE);
        sb.append(Constants.INDENT);
        sb.append("AAD: ");
        sb.append((aad == null? "null" : "0x" + HexFormat.of().formatHex(aad)));
        return sb.toString();
    }
}
