/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6341599 6383200
 * @summary JCE Reference Guide has recommendations, not requirements,
 * for algorithm names
 * @author Brad R. Wetmore
 */
import javax.crypto.*;
import java.security.spec.*;
import javax.crypto.spec.*;

public class PBEKeysAlgorithmNames {

    static String [] algs = {
        "PBEWithMD5AndDES",
        "PBEWithSHA1AndDESede",
        "PBEWithSHA1AndRC2_40",
        "PBEWithSHA1AndRC2_128",
        "PBEWithMD5AndTripleDES",
        "PBEWithSHA1AndRC4_40",
        "PBEWithSHA1AndRC4_128",
        "PBKDF2WithHmacSHA1",
        "PBKDF2WithHmacSHA224",
        "PBKDF2WithHmacSHA256",
        "PBKDF2WithHmacSHA384",
        "PBKDF2WithHmacSHA512"
    };

    public static void main(String[] argv) throws Exception {

        byte [] b = new byte[64];

        PBEKeySpec pbeks = new PBEKeySpec("password".toCharArray(), b, 20, 60);

        for (String s : algs) {
            System.out.println("Testing " + s);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(s, "SunJCE");

            System.out.println("    Checking skf.getAlgorithm()");
            if (!skf.getAlgorithm().equalsIgnoreCase(s)) {
                throw new Exception("getAlgorithm() \n\"" +
                    skf.getAlgorithm() + "\" != \"" + s + "\"");
            }

            System.out.println("    Checking skf.generateSecret()");
            SecretKey sk = skf.generateSecret(pbeks);
            if (!sk.getAlgorithm().equalsIgnoreCase(s)) {
                throw new Exception("getAlgorithm() \n\"" +
                    sk.getAlgorithm() + "\" != \"" + s + "\"");
            }

            System.out.println("    Checking skf.translateKey()");
            SecretKey sk1 = skf.translateKey(sk);
            if (!sk1.getAlgorithm().equalsIgnoreCase(s)) {
                throw new Exception("    getAlgorithm() \n\"" +
                    sk.getAlgorithm() + "\" != \"" + s + "\"");
            }

            System.out.println("    Checking skf.getKeySpec()");
            KeySpec ks = skf.getKeySpec(sk, PBEKeySpec.class);

            System.out.println("    passed.\n");
        }

        System.out.println("Test Passed");
    }
}
