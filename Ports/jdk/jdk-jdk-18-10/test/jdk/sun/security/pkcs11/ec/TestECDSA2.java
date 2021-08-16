/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary basic test of ECDSA signatures for P-256 and P-384 from the
 * example data in "Suite B Implementer's Guide to FIPS 186-3".
 * @library /test/lib ..
 * @library ../../../../java/security/testlibrary
 * @modules java.base/sun.security.util
 *          jdk.crypto.cryptoki
 * @compile -XDignore.symbol.file TestECDSA2.java
 * @run main/othervm TestECDSA2
 * @run main/othervm -Djava.security.manager=allow TestECDSA2 sm
 */

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;

public class TestECDSA2 extends PKCS11Test {

    // values of the keys we use for the tests

    // keypair using NIST P-256
    private final static String privD256 = "70a12c2db16845ed56ff68cfc21a472b3f04d7d6851bf6349f2d7d5b3452b38a";
    private final static String pubX256 = "8101ece47464a6ead70cf69a6e2bd3d88691a3262d22cba4f7635eaff26680a8";
    private final static String pubY256 = "d8a12ba61d599235f67d9cb4d58f1783d3ca43e78f0a5abaa624079936c0c3a9";

    // keypair using NIST P-384
    private final static String privD384 = "c838b85253ef8dc7394fa5808a5183981c7deef5a69ba8f4f2117ffea39cfcd90e95f6cbc854abacab701d50c1f3cf24";
    private final static String pubX384 = "1fbac8eebd0cbf35640b39efe0808dd774debff20a2a329e91713baf7d7f3c3e81546d883730bee7e48678f857b02ca0";
    private final static String pubY384 = "eb213103bd68ce343365a8a4c3d4555fa385f5330203bdd76ffad1f3affb95751c132007e1b240353cb0a4cf1693bdf9";

    // data to be signed
    private final static byte[] data = "This is only a test message. It is 48 bytes long".getBytes();

    private KeyFactory kf = null;

    private static void testSignAndVerify(String alg, KeyPair kp, Provider p) throws Exception {
        Signature s = Signature.getInstance(alg, p);
        s.initSign(kp.getPrivate());
        s.update(data);
        byte[] result = s.sign();

        s.initVerify(kp.getPublic());
        s.update(data);
        if (!s.verify(result)) {
            throw new Exception("Error: Signature verification failed");
        }
        System.out.println(p.getName() + ": " + alg + " Passed");
    }

    private KeyPair genECKeyPair(String curvName, String privD, String pubX,
            String pubY, Provider p) throws Exception {
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC", p);
        params.init(new ECGenParameterSpec(curvName));
        ECParameterSpec ecParams = params.getParameterSpec(ECParameterSpec.class);
        ECPrivateKeySpec privKeySpec =
            new ECPrivateKeySpec(new BigInteger(privD, 16), ecParams);
        ECPublicKeySpec pubKeySpec =
            new ECPublicKeySpec(new ECPoint(new BigInteger(pubX, 16), new BigInteger(pubY, 16)),
                                ecParams);
        PrivateKey privKey = kf.generatePrivate(privKeySpec);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        return new KeyPair(pubKey, privKey);
    }

    public static void main(String[] args) throws Exception {
        main(new TestECDSA2(), args);
    }

    @Override
    protected boolean skipTest(Provider provider) {
        boolean testP256 =
                provider.getService("Signature", "SHA256withECDSA") != null;

        boolean testP384 =
                provider.getService("Signature", "SHA384withECDSA") != null;

        if (!testP256 && !testP384) {
            System.out.println("ECDSA not supported, skipping");
            return true;
        }

        if (isBadNSSVersion(provider)) {
            return true;
        }

        return false;
    }

    @Override
    public void main(Provider provider) throws Exception {
        boolean testP256 =
            (provider.getService("Signature", "SHA256withECDSA") != null);

        boolean testP384 =
            (provider.getService("Signature", "SHA384withECDSA") != null);

        kf = KeyFactory.getInstance("EC", provider);

        long start = System.currentTimeMillis();
        if (testP256) {
            // can use secp256r1, NIST P-256, X9.62 prime256v1, or 1.2.840.10045.3.1.7
            KeyPair kp =
                genECKeyPair("secp256r1", privD256, pubX256, pubY256, provider);
            testSignAndVerify("SHA256withECDSA", kp, provider);
        }
        if (testP384) {
            // can use secp384r1, NIST P-384, 1.3.132.0.34
            KeyPair kp =
                genECKeyPair("secp384r1", privD384, pubX384, pubY384, provider);
            testSignAndVerify("SHA384withECDSA", kp, provider);
        }
        long stop = System.currentTimeMillis();
        System.out.println("All tests passed (" + (stop - start) + " ms).");
    }
}
