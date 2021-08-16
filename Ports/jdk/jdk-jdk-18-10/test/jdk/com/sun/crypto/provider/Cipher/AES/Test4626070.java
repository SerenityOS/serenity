/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4626070
 * @summary Verify that AES cipher can wrap and unwrap keys
 * @author Valerie Peng
 */
import java.security.*;
import javax.crypto.*;
import java.util.*;


public class Test4626070 {
    private static final String ALGO = "AES";
    private static final int KEYSIZE = 16; // in bytes

    public void execute(String mode, String padding) throws Exception {
        String transformation = ALGO + "/" + mode + "/" + padding;
        Cipher ci = Cipher.getInstance(transformation, "SunJCE");
        KeyGenerator kg = KeyGenerator.getInstance(ALGO, "SunJCE");
        kg.init(KEYSIZE*8);
        SecretKey key = kg.generateKey();

        AlgorithmParameters params = ci.getParameters();

        // TEST FIX 4626070
        ci.init(Cipher.WRAP_MODE, key, params);
        byte[] wrappedKeyEncoding = ci.wrap(key);
        params = ci.getParameters();
        ci.init(Cipher.UNWRAP_MODE, key, params);
        Key recoveredKey = ci.unwrap(wrappedKeyEncoding, "AES",
                                     Cipher.SECRET_KEY);
        if (!key.equals(recoveredKey)) {
            throw new Exception(
                "key after wrap/unwrap is different from the original!");
        }
        System.out.println(transformation + ": Passed");
    }

    public static void main (String[] args) throws Exception {
        Test4626070 test = new Test4626070();
        test.execute("CBC", "PKCS5Padding");
        test.execute("GCM", "NoPadding");
    }
}
