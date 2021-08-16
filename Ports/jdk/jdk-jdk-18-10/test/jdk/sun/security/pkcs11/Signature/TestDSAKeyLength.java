/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7200306 8029158
 * @summary verify that P11Signature impl will error out when initialized
 * with unsupported key sizes
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestDSAKeyLength
 * @run main/othervm -Djava.security.manager=allow TestDSAKeyLength sm
 */

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Signature;

public class TestDSAKeyLength extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new TestDSAKeyLength(), args);
    }

    @Override
    protected boolean skipTest(Provider provider) {
        if (isNSS(provider) && getNSSVersion() >= 3.14) {
            System.out.println("Skip testing NSS " + getNSSVersion());
            return true;
        }

        return false;
    }

    @Override
    public void main(Provider provider) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA", "SUN");
        kpg.initialize(2048, new SecureRandom());
        KeyPair pair = kpg.generateKeyPair();

        boolean status = true;
        Signature sig = Signature.getInstance("SHA1withDSA", provider);
        try {
            sig.initSign(pair.getPrivate());
            status = false;
        } catch (InvalidKeyException ike) {
            System.out.println("Expected IKE thrown for initSign()");
        }
        try {
            sig.initVerify(pair.getPublic());
            status = false;
        } catch (InvalidKeyException ike) {
            System.out.println("Expected IKE thrown for initVerify()");
        }
        if (status) {
            System.out.println("Test Passed");
        } else {
            throw new Exception("Test Failed - expected IKE not thrown");
        }
    }
}
