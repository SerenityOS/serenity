/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4800108 8072452 8181048
 * @summary verify that precomputed DSA parameters are always used (512, 768,
 *          1024, 2048, 3072 bit)
 * @run main/othervm/timeout=15 TestKeyPairGenerator
 */

//
// This fix is really a performance fix, so this test is not foolproof.
// Without the precomputed parameters, it will take a minute or more
// (unless you have a very fast machine).  With the fix, the test should
// complete in less than 2 seconds.  Use 15 second timeout to leave some room.
//

import java.security.*;
import java.security.interfaces.*;

public class TestKeyPairGenerator {

    private static void checkKeyLength(KeyPair kp, int len) throws Exception {
        DSAPublicKey key = (DSAPublicKey)kp.getPublic();
        int n = key.getParams().getP().bitLength();
        System.out.println("Key length: " + n);
        if (len != n) {
            throw new Exception("Wrong key length");
        }
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        KeyPairGenerator kpg;
        KeyPair kp;
        // problem was when not calling initialize()
        // do that twice to artifically inflate the time
        // on JDKs that do not have the fix
        kpg = KeyPairGenerator.getInstance("DSA", "SUN");
        kp = kpg.generateKeyPair();

        kpg = KeyPairGenerator.getInstance("DSA", "SUN");
        kp = kpg.generateKeyPair();

        // some other basic tests
        kp = kpg.generateKeyPair();

        kpg.initialize(1024);
        kp = kpg.generateKeyPair();
        checkKeyLength(kp, 1024);

        kpg.initialize(768);
        kp = kpg.generateKeyPair();
        checkKeyLength(kp, 768);

        kpg.initialize(512);
        kp = kpg.generateKeyPair();
        checkKeyLength(kp, 512);

        kpg.initialize(2048);
        kp = kpg.generateKeyPair();
        checkKeyLength(kp, 2048);

        kpg.initialize(3072);
        kp = kpg.generateKeyPair();
        checkKeyLength(kp, 3072);

        long stop = System.currentTimeMillis();
        System.out.println("Time: " + (stop - start) + " ms.");
    }
}
