/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8181048
 * @summary verify that when the returned DSA KeyPairGenerator is
 * an instance of java.security.interfaces.DSAKeyPairGenerator,
 * the behavior is compliant with the javadoc spec.
 * @run main/othervm -Djdk.security.legacyDSAKeyPairGenerator=tRUe TestLegacyDSAKeyPairGenerator
 */

import java.security.*;
import java.security.interfaces.*;

public class TestLegacyDSAKeyPairGenerator {

    private static void checkKeyLength(KeyPair kp, int len) throws Exception {
        DSAPublicKey key = (DSAPublicKey)kp.getPublic();
        int n = key.getParams().getP().bitLength();
        System.out.println("Key length: " + n);
        if (len != n) {
            throw new Exception("Wrong key length");
        }
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA", "SUN");
        // check the returned object implements the legacy interface
        if (!(kpg instanceof DSAKeyPairGenerator)) {
            throw new Exception("Should be an instance of DSAKeyPairGenerator");
        }
        System.out.println("Returned an instance of DSAKeyPairGenerator");
        // check the default key size is 1024 when initiaize(..) is not called
        KeyPair kp1 = kpg.generateKeyPair();
        checkKeyLength(kp1, 1024);
        KeyPair kp2 = kpg.generateKeyPair();
        checkKeyLength(kp2, 1024);
        System.out.println("Used 1024 default key size");

        // check kp1 and kp2 uses the same DSA parameters p, q, g
        DSAParams param1 = ((DSAPublicKey)kp1.getPublic()).getParams();
        DSAParams param2 = ((DSAPublicKey)kp2.getPublic()).getParams();
        if ((param1.getP().compareTo(param2.getP()) != 0) ||
            (param1.getQ().compareTo(param2.getQ()) != 0) ||
            (param1.getG().compareTo(param2.getG()) != 0)) {
            throw new RuntimeException("Key params mismatch");
        }
        System.out.println("Used same default params");

        // check that the documented exception is thrown if no cached parameters
        int sizeNotInCache = (1024 - 64);
        try {
            ((DSAKeyPairGenerator)kpg).initialize(sizeNotInCache, false, null);
            throw new RuntimeException("Expected IPE not thrown");
        } catch (InvalidParameterException ipe) {
            System.out.println("Throwed expected IPE");
        }
        ((DSAKeyPairGenerator)kpg).initialize(sizeNotInCache, true, null);
        KeyPair kp = kpg.generateKeyPair();
        checkKeyLength(kp, sizeNotInCache);
        System.out.println("Generated requested key size");
    }
}
