/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.mscapi;

/**
 * The handle for an RSA public/private keypair using the Microsoft Crypto API.
 *
 * @since 1.6
 */
class CKeyPair {

    private final CPrivateKey privateKey;

    private final CPublicKey publicKey;

    /**
     * This method is called by native codes in security.cpp.
     */
    CKeyPair(String alg, long hCryptProv, long hCryptKey, int keyLength) {
        CKey.NativeHandles handles = new CKey.NativeHandles(hCryptProv, hCryptKey);
        privateKey = CPrivateKey.of(alg, handles, keyLength);
        publicKey = CPublicKey.of(alg, handles, keyLength);
    }

    public CPrivateKey getPrivate() {
        return privateKey;
    }

    public CPublicKey getPublic() {
        return publicKey;
    }
}
