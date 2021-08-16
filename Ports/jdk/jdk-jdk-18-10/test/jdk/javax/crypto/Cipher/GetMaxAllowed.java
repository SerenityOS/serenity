/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4807942 7033170
 * @summary Test the Cipher.getMaxAllowedKeyLength(String) and
 * getMaxAllowedParameterSpec(String) methods
 * @author Valerie Peng
 */

import java.util.*;
import java.nio.*;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class GetMaxAllowed {

    private static void runTest1(boolean isUnlimited) throws Exception {
        System.out.println("Testing " + (isUnlimited? "un":"") +
                           "limited policy...");

        String algo = "Blowfish";
        int keyLength = Cipher.getMaxAllowedKeyLength(algo);
        AlgorithmParameterSpec spec = Cipher.getMaxAllowedParameterSpec(algo);
        if (isUnlimited) {
            if ((keyLength != Integer.MAX_VALUE) || (spec != null)) {
                throw new Exception("Check for " + algo +
                                    " failed under unlimited policy");
            }
        } else {
            if ((keyLength != 128) || (spec != null)) {
                throw new Exception("Check for " + algo +
                                    " failed under default policy");
            }
        }
        algo = "RC5";
        keyLength = Cipher.getMaxAllowedKeyLength(algo);
        RC5ParameterSpec rc5param = (RC5ParameterSpec)
            Cipher.getMaxAllowedParameterSpec(algo);
        if (isUnlimited) {
            if ((keyLength != Integer.MAX_VALUE) || (rc5param != null)) {
                throw new Exception("Check for " + algo +
                                    " failed under unlimited policy");
            }
        } else {
            if ((keyLength != 128) || (rc5param.getRounds() != 12) ||
                (rc5param.getVersion() != Integer.MAX_VALUE) ||
                (rc5param.getWordSize() != Integer.MAX_VALUE)) {
                throw new Exception("Check for " + algo +
                                    " failed under default policy");
            }
        }
        System.out.println("All tests passed");
    }

    private static void runTest2() throws Exception {
        System.out.println("Testing against Security.getAlgorithms()");

        Set<String> algorithms = Security.getAlgorithms("Cipher");

        for (String algorithm: algorithms) {
            int keylength = -1;

            // if 7033170 is not fixed, NoSuchAlgorithmException is thrown
            keylength = Cipher.getMaxAllowedKeyLength(algorithm);

        }
    }

    public static void main(String[] args) throws Exception {
        // decide if the installed jurisdiction policy file is the
        // unlimited version
        boolean isUnlimited = true;
        Cipher c = Cipher.getInstance("AES", "SunJCE");
        try {
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[24], "AES"));
        } catch (InvalidKeyException ike) {
            isUnlimited = false;
        }
        runTest1(isUnlimited);

        // test using the set of algorithms returned by Security.getAlgorithms()
        runTest2();
    }
}
