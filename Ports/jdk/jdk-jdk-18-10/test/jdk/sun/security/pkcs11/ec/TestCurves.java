/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6405536 6414980
 * @summary Basic consistency test for all curves using ECDSA and ECDH
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki/sun.security.pkcs11.wrapper
 * @run main/othervm TestCurves
 * @run main/othervm -Djava.security.manager=allow TestCurves sm
 * @key randomness
 */

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.ProviderException;
import java.security.Signature;
import java.security.spec.ECParameterSpec;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.crypto.KeyAgreement;

public class TestCurves extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new TestCurves(), args);
    }

    @Override
    protected boolean skipTest(Provider p) {
        if (p.getService("KeyAgreement", "ECDH") == null) {
            System.out.println("Not supported by provider, skipping");
            return true;
        }

        if (isBadNSSVersion(p)) {
            return true;
        }

        return false;
    }

    @Override
    public void main(Provider p) throws Exception {
        Random random = new Random();
        byte[] data = new byte[2048];
        random.nextBytes(data);

        List<ECParameterSpec> curves = getKnownCurves(p);
        for (ECParameterSpec params : curves) {
            System.out.println("Testing " + params + "...");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", p);
            kpg.initialize(params);
            KeyPair kp1, kp2;

            kp1 = kpg.generateKeyPair();
            kp2 = kpg.generateKeyPair();

            testSigning(p, "SHA1withECDSA", data, kp1, kp2);
            testSigning(p, "SHA224withECDSA", data, kp1, kp2);
            testSigning(p, "SHA256withECDSA", data, kp1, kp2);
            testSigning(p, "SHA384withECDSA", data, kp1, kp2);
            testSigning(p, "SHA512withECDSA", data, kp1, kp2);
            System.out.println();

            KeyAgreement ka1 = KeyAgreement.getInstance("ECDH", p);
            ka1.init(kp1.getPrivate());
            ka1.doPhase(kp2.getPublic(), true);
            byte[] secret1 = ka1.generateSecret();

            KeyAgreement ka2 = KeyAgreement.getInstance("ECDH", p);
            ka2.init(kp2.getPrivate());
            ka2.doPhase(kp1.getPublic(), true);
            byte[] secret2 = ka2.generateSecret();

            if (Arrays.equals(secret1, secret2) == false) {
                throw new Exception("Secrets do not match");
            }
        }

        System.out.println("OK");
    }

    private static void testSigning(Provider p, String algorithm,
            byte[] data, KeyPair kp1, KeyPair kp2) throws Exception {
        System.out.print("  " + algorithm);
        Signature s = Signature.getInstance(algorithm, p);
        s.initSign(kp1.getPrivate());
        s.update(data);
        byte[] sig = s.sign();

        s = Signature.getInstance(algorithm, p);
        s.initVerify(kp1.getPublic());
        s.update(data);
        boolean r = s.verify(sig);
        if (r == false) {
            throw new Exception("Signature did not verify");
        }

        s.initVerify(kp2.getPublic());
        s.update(data);
        r = s.verify(sig);
        if (r) {
            throw new Exception("Signature should not verify");
        }
    }
}
