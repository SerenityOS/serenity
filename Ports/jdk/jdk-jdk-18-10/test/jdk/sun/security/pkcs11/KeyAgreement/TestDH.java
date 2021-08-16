/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4921804 6324825
 * @summary Verify that DH works properly
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm -Djdk.crypto.KeyAgreement.legacyKDF=true TestDH
 * @run main/othervm -Djava.security.manager=allow -Djdk.crypto.KeyAgreement.legacyKDF=true TestDH sm
 */

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.util.Arrays;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

public class TestDH extends PKCS11Test {

    @Override
    public void main(Provider p) throws Exception {
        if (p.getService("KeyAgreement", "DH") == null) {
            System.out.println("DH not supported, skipping");
            return;
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH", p);
        kpg.initialize(512);
        KeyPair kp1 = kpg.generateKeyPair();
        KeyPair kp2 = kpg.generateKeyPair();

        KeyAgreement ka1, ka2;
        ka1 = KeyAgreement.getInstance("DH", p);
        ka1.init(kp1.getPrivate());
        ka1.doPhase(kp2.getPublic(), true);
        System.out.println("Derive 1...");
        byte[] secret1 = ka1.generateSecret();

        ka1.init(kp2.getPrivate());
        ka1.doPhase(kp1.getPublic(), true);
        System.out.println("Derive 2...");
        byte[] secret2 = ka1.generateSecret();

        if (Arrays.equals(secret1, secret2) == false) {
            throw new Exception("Secrets (1,2) do not match");
        }

        ka2 = KeyAgreement.getInstance("DH", "SunJCE");
        ka2.init(kp1.getPrivate());
        ka2.doPhase(kp2.getPublic(), true);
        System.out.println("Derive 3...");
        byte[] secret3 = ka2.generateSecret();

        if (Arrays.equals(secret1, secret3) == false) {
            throw new Exception("Secrets (1,3) do not match");
        }

        ka2.init(kp2.getPrivate());
        ka2.doPhase(kp1.getPublic(), true);
        System.out.println("Derive 4...");
        byte[] secret4 = ka2.generateSecret();

        if (Arrays.equals(secret1, secret4) == false) {
            throw new Exception("Secrets (1,4) do not match");
        }

        testAlgorithm(ka2, kp2, ka1, kp1, "DES");
        testAlgorithm(ka2, kp2, ka1, kp1, "DESede");
//      testAlgorithm(ka2, kp2, ka1, kp1, "AES");
//      testAlgorithm(ka2, kp2, ka1, kp1, "RC4");
        testAlgorithm(ka2, kp2, ka1, kp1, "Blowfish");
        testAlgorithm(ka2, kp2, ka1, kp1, "TlsPremasterSecret");
    }

    private static void testAlgorithm(KeyAgreement ka1, KeyPair kp1,
            KeyAgreement ka2, KeyPair kp2, String algorithm) throws Exception {
        SecretKey key1;

        ka1.init(kp1.getPrivate());
        ka1.doPhase(kp2.getPublic(), true);
        System.out.println("Derive " + algorithm + " using SunJCE...");
        key1 = ka1.generateSecret(algorithm);

        ka2.init(kp1.getPrivate());
        ka2.doPhase(kp2.getPublic(), true);
        System.out.println("Derive " + algorithm + " using PKCS#11...");
        SecretKey key2 = ka2.generateSecret(algorithm);

        byte[] b1 = key1.getEncoded();
        byte[] b2 = key2.getEncoded();

        if (Arrays.equals(b1, b2) == false) {
            System.out.println(b1.length + " bytes: " + toString(b1));
            System.out.println(b2.length + " bytes: " + toString(b2));
            throw new Exception(algorithm + " secret mismatch");
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestDH(), args);
    }

}
