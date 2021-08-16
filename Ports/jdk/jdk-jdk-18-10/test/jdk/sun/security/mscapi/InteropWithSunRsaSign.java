/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8205445
 * @summary Interop test between SunMSCAPI and SunRsaSign on RSASSA-PSS
 * @requires os.family == "windows"
 */

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Random;

public class InteropWithSunRsaSign {

    private static final SecureRandom NOT_SECURE_RANDOM = new SecureRandom() {
        Random r = new Random();
        @Override
        public void nextBytes(byte[] bytes) {
            r.nextBytes(bytes);
        }
    };

    private static boolean allResult = true;
    private static byte[] msg = "hello".getBytes();

    public static void main(String[] args) throws Exception {

        matrix(new PSSParameterSpec(
                "SHA-1",
                "MGF1",
                MGF1ParameterSpec.SHA1,
                20,
                PSSParameterSpec.TRAILER_FIELD_BC));

        matrix(new PSSParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                32,
                PSSParameterSpec.TRAILER_FIELD_BC));

        matrix(new PSSParameterSpec(
                "SHA-384",
                "MGF1",
                MGF1ParameterSpec.SHA384,
                48,
                PSSParameterSpec.TRAILER_FIELD_BC));

        matrix(new PSSParameterSpec(
                "SHA-512",
                "MGF1",
                MGF1ParameterSpec.SHA512,
                64,
                PSSParameterSpec.TRAILER_FIELD_BC));

        // non-typical salt length
        matrix(new PSSParameterSpec(
                "SHA-1",
                "MGF1",
                MGF1ParameterSpec.SHA1,
                17,
                PSSParameterSpec.TRAILER_FIELD_BC));

        if (!allResult) {
            throw new Exception("Failed");
        }
    }

    static void matrix(PSSParameterSpec pss) throws Exception {

        System.out.printf("\n%10s%20s%20s%20s  %s\n", pss.getDigestAlgorithm(),
                "KeyPairGenerator", "signer", "verifier", "result");
        System.out.printf("%10s%20s%20s%20s  %s\n",
                "-------", "----------------", "------", "--------", "------");

        // KeyPairGenerator chooses SPI when getInstance() is called.
        String[] provsForKPG = {"SunRsaSign", "SunMSCAPI"};

        // "-" means no preferred provider. In this case, SPI is chosen
        // when initSign/initVerify is called. Worth testing.
        String[] provsForSignature = {"SunRsaSign", "SunMSCAPI", "-"};

        int pos = 0;
        for (String pg : provsForKPG) {
            for (String ps : provsForSignature) {
                for (String pv : provsForSignature) {
                    System.out.printf("%10d%20s%20s%20s  ", ++pos, pg, ps, pv);
                    try {
                        boolean result = test(pg, ps, pv, pss);
                        System.out.println(result);
                        if (!result) {
                            allResult = false;
                        }
                    } catch (Exception e) {
                        if (pg.equals("-") || pg.equals(ps)) {
                            // When Signature provider is automatically
                            // chosen or the same with KeyPairGenerator,
                            // this is an error.
                            allResult = false;
                            System.out.println("X " + e.getMessage());
                        } else {
                            // Known restriction: SunRsaSign and SunMSCAPI can't
                            // use each other's private key for signing.
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    static boolean test(String pg, String ps, String pv, PSSParameterSpec pss)
            throws Exception {

        KeyPairGenerator kpg = pg.length() == 1
                ? KeyPairGenerator.getInstance("RSA")
                :KeyPairGenerator.getInstance("RSA", pg);
        kpg.initialize(
                pss.getDigestAlgorithm().equals("SHA-512") ? 2048: 1024,
                NOT_SECURE_RANDOM);
        KeyPair kp = kpg.generateKeyPair();
        PrivateKey pr = kp.getPrivate();
        PublicKey pu = kp.getPublic();

        Signature s = ps.length() == 1
                ? Signature.getInstance("RSASSA-PSS")
                : Signature.getInstance("RSASSA-PSS", ps);
        s.initSign(pr);
        s.setParameter(pss);
        s.update(msg);
        byte[] sig = s.sign();

        Signature s2 = pv.length() == 1
                ? Signature.getInstance("RSASSA-PSS")
                : Signature.getInstance("RSASSA-PSS", pv);
        s2.initVerify(pu);
        s2.setParameter(pss);
        s2.update(msg);

        return s2.verify(sig);
    }
}
