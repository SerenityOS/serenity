/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm SigInteropPSS2
 */
public class SigInteropPSS2 extends PKCS11Test {

    private static final byte[] MSG =
        "Interoperability test between SunRsaSign and SunPKCS11".getBytes();

    private static final String[] DIGESTS = {
        "SHA224", "SHA256", "SHA384", "SHA512",
        "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512"
    };

    public static void main(String[] args) throws Exception {
        main(new SigInteropPSS2(), args);
    }

    @Override
    public void main(Provider p) throws Exception {

        Signature sigPkcs11;
        Signature sigSunRsaSign =
                Signature.getInstance("RSASSA-PSS", "SunRsaSign");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", p);
            kpg.initialize(3072);
        KeyPair kp = kpg.generateKeyPair();

        for (String digest : DIGESTS) {
            try {
                sigPkcs11 = Signature.getInstance(digest + "withRSASSA-PSS", p);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Skip testing " + digest + "withRSASSA-PSS" +
                    " due to no support");
                continue;
            }

            runTest(sigPkcs11, sigSunRsaSign, kp);
        }
        System.out.println("Test passed");
    }

    static void runTest(Signature signer, Signature verifier, KeyPair kp)
            throws Exception {
        System.out.println("\tSign: " + signer.getProvider().getName());
        System.out.println("\tVerify: " + verifier.getProvider().getName());

        signer.initSign(kp.getPrivate());
        signer.update(MSG);
        byte[] sigBytes = signer.sign();

        AlgorithmParameters signParams = signer.getParameters();
        verifier.setParameter(signParams.getParameterSpec
                (PSSParameterSpec.class));
        verifier.initVerify(kp.getPublic());

        verifier.update(MSG);
        boolean isValid = verifier.verify(sigBytes);
        if (isValid) {
            System.out.println("\tPSS Signature verified");
        } else {
            throw new RuntimeException("ERROR verifying PSS Signature");
        }
    }
}
