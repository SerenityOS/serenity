/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6405536
 * @summary Verify that we can use ECGenParameterSpec
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestECGenSpec
 * @run main/othervm -Djava.security.manager=allow TestECGenSpec sm
 */

import java.security.AlgorithmParameters;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;

public class TestECGenSpec extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new TestECGenSpec(), args);
    }

    @Override
    protected boolean skipTest(Provider p) {
        if (p.getService("Signature", "SHA1withECDSA") == null) {
            System.out.println("Provider does not support ECDSA, skipping...");
            return true;
        }

        if (isBadNSSVersion(p)) {
            return true;
        }

        return false;
    }

    @Override
    public void main(Provider p) throws Exception {
        String[] names = { "secp256r1", "NIST P-192", "sect163k1", "1.3.132.0.26",
            "X9.62 c2tnb239v1"};
        int curves = 1;
        if (getNSSECC() == ECCState.Extended) {
            curves = names.length;
        }
        int[] lengths = {256, 192, 163, 233, 239};
        for (int i = 0; i < curves; i++) {
            String name = names[i];
            int len = lengths[i];
            System.out.println("Testing " + name + "...");
            ECGenParameterSpec spec = new ECGenParameterSpec(name);

            AlgorithmParameters algParams = AlgorithmParameters.getInstance("EC", p);
            algParams.init(spec);
            ECParameterSpec ecSpec = algParams.getParameterSpec(ECParameterSpec.class);
            System.out.println(ecSpec);
            // no public API to get the curve name, so rely on toString();
            if (ecSpec.toString().contains(name) == false) {
                throw new Exception("wrong curve");
            }

            algParams = AlgorithmParameters.getInstance("EC", p);
            algParams.init(ecSpec);
            ECGenParameterSpec genSpec = algParams.getParameterSpec(ECGenParameterSpec.class);
            System.out.println(genSpec.getName());

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", p);
            kpg.initialize(spec);
            KeyPair kp = kpg.generateKeyPair();
            System.out.println(kp.getPrivate());
            ECPublicKey publicKey = (ECPublicKey)kp.getPublic();
            if (publicKey.getParams().getCurve().getField().getFieldSize() != len) {
                throw new Exception("wrong curve");
            }
            System.out.println();
        }
    }

}
