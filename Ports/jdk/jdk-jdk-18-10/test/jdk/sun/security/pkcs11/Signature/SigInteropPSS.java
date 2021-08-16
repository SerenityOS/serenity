/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;

/*
 * @test
 * @bug 8080462 8226651 8242332
 * @summary testing interoperability of PSS signatures of PKCS11 provider
 *         against SunRsaSign provider
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm SigInteropPSS
 */
public class SigInteropPSS extends PKCS11Test {

    private static final byte[] MSG =
        "Interoperability test between SunRsaSign and SunPKCS11".getBytes();

    private static final String[] DIGESTS = {
        "SHA-224", "SHA-256", "SHA-384", "SHA-512"
    };

    public static void main(String[] args) throws Exception {
        main(new SigInteropPSS(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        Signature sigPkcs11;
        try {
            sigPkcs11 = Signature.getInstance("RSASSA-PSS", p);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Skip testing RSASSA-PSS" +
                " due to no support");
            return;
        }

        Signature sigSunRsaSign =
                Signature.getInstance("RSASSA-PSS", "SunRsaSign");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", p);
        kpg.initialize(3072);
        KeyPair kp = kpg.generateKeyPair();

        runTest(sigSunRsaSign, sigPkcs11, kp);
        runTest(sigPkcs11, sigSunRsaSign, kp);

        System.out.println("Test passed");
    }

    static void runTest(Signature signer, Signature verifier, KeyPair kp)
            throws Exception {
        System.out.println("\tSign using " + signer.getProvider().getName());
        System.out.println("\tVerify using " + verifier.getProvider().getName());

        for (String hash : DIGESTS) {
            for (String mgfHash : DIGESTS) {
                System.out.println("\tDigest = " + hash);
                System.out.println("\tMGF = MGF1_" + mgfHash);

                PSSParameterSpec params = new PSSParameterSpec(hash, "MGF1",
                    new MGF1ParameterSpec(mgfHash), 0, 1);

                signer.setParameter(params);
                signer.initSign(kp.getPrivate());
                verifier.setParameter(params);
                verifier.initVerify(kp.getPublic());

                signer.update(MSG);
                byte[] sigBytes = signer.sign();
                verifier.update(MSG);
                boolean isValid = verifier.verify(sigBytes);
                if (isValid) {
                    System.out.println("\tPSS Signature verified");
                } else {
                    throw new RuntimeException("ERROR verifying PSS Signature");
                }
            }
        }
    }
}
