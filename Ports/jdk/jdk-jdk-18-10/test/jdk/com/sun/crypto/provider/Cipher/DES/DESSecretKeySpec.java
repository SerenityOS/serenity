/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 6755701
 * @summary Change SecretKeyFactory.generateSecret to allow SecretKeySpec to
 * be passed and used for creating a DES and DESede keys. This avoids the error
 * of "InvalidKeySpecException: Inappropriate key specification"
 * @author Anthony Scarpino
 */

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

public class DESSecretKeySpec {

    public static void main(String arg[]) throws Exception {
        Cipher c;
        byte[] key = new byte[]{'1','2','3','4','5','6','7','8',
            '1','2','3','4','5','6','7','8',
            '1','2','3','4','5','6','7','8'};


        System.out.println("Testing DES key");
        SecretKeySpec skey = new SecretKeySpec(key, "DES");
        c = Cipher.getInstance("DES/CBC/PKCS5Padding", "SunJCE");
        SecretKeyFactory.getInstance("DES", "SunJCE").generateSecret(skey);

        System.out.println("Testing DESede key");
        skey = new SecretKeySpec(key, "DESede");
        c = Cipher.getInstance("DESede/CBC/PKCS5Padding", "SunJCE");
        SecretKeyFactory.getInstance("TripleDES", "SunJCE").generateSecret(skey);
    }
}
