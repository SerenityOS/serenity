/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7196382 8072452
 * @summary Ensure that DH key pairs can be generated for 512 - 8192 bits
 * @author Valerie Peng
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestDH2048
 * @run main/othervm -Djava.security.manager=allow TestDH2048 sm
 */

import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;

public class TestDH2048 extends PKCS11Test {

    private static void checkUnsupportedKeySize(KeyPairGenerator kpg, int ks)
        throws Exception {
        try {
            kpg.initialize(ks);
            throw new Exception("Expected IPE not thrown for " + ks);
        } catch (InvalidParameterException ipe) {
        }
    }

    @Override
    public void main(Provider p) throws Exception {
        if (p.getService("KeyPairGenerator", "DH") == null) {
            System.out.println("KPG for DH not supported, skipping");
            return;
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH", p);
        kpg.initialize(512);
        KeyPair kp1 = kpg.generateKeyPair();

        kpg.initialize(768);
        kp1 = kpg.generateKeyPair();

        kpg.initialize(1024);
        kp1 = kpg.generateKeyPair();

        kpg.initialize(1536);
        kp1 = kpg.generateKeyPair();

        kpg.initialize(2048);
        kp1 = kpg.generateKeyPair();

        try {
            kpg.initialize(3072);
            kp1 = kpg.generateKeyPair();

            kpg.initialize(4096);
            kp1 = kpg.generateKeyPair();

            kpg.initialize(6144);
            kp1 = kpg.generateKeyPair();

            kpg.initialize(8192);
            kp1 = kpg.generateKeyPair();
        } catch (InvalidParameterException ipe) {
            // NSS (as of version 3.13) has a hard coded maximum limit
            // of 2236 or 3072 bits for DHE keys.
            System.out.println("4096-bit DH key pair generation: " + ipe);
            if (!p.getName().equals("SunPKCS11-NSS")) {
                throw ipe;
            }
        }

        // key size must be multiples of 64 though
        checkUnsupportedKeySize(kpg, 2048 + 63);
        checkUnsupportedKeySize(kpg, 3072 + 32);
    }

    public static void main(String[] args) throws Exception {
        main(new TestDH2048(), args);
    }
}
