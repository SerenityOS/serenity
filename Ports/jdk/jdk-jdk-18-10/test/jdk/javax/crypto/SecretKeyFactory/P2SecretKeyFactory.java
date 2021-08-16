/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * test
 * @bug 6370923
 * @summary SecretKeyFactory failover does not work
 * @author Brad R. Wetmore
 */

package com.p2;

import java.security.*;
import java.security.spec.*;
import javax.crypto.*;

public class P2SecretKeyFactory extends SecretKeyFactorySpi {

    public P2SecretKeyFactory() {
        System.out.println("Creating a P2SecretKeyFactory");
    }

    protected SecretKey engineGenerateSecret(KeySpec keySpec)
            throws InvalidKeySpecException {
        System.out.println("Trying the good provider");
        return null;
    }

    protected KeySpec engineGetKeySpec(SecretKey key, Class keySpec)
            throws InvalidKeySpecException {
        System.out.println("Trying the good provider");
        return null;
    }

    protected SecretKey engineTranslateKey(SecretKey key)
            throws InvalidKeyException {
        System.out.println("Trying the good provider");
        return null;
    }
}
