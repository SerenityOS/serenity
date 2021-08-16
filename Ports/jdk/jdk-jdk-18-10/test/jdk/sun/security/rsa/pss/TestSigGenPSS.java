/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.security.*;
import java.security.spec.*;
import java.util.HexFormat;
import java.util.List;

/*
 * @test
 * @bug 8146293
 * @summary Known Answer Tests based on NIST 186-3 at:
 * @compile SigRecord.java
 * @run main/othervm TestSigGenPSS
 */
public class TestSigGenPSS {

    private static final String[] testFiles = {
        "SigGenPSS_186-3.txt", "SigGenPSS_186-3_TruncatedSHAs.txt"
    };

    static final class MyKnownRandomSrc extends SecureRandom {
        final byte[] srcBytes;
        int numBytes;

        MyKnownRandomSrc(String srcString) {
            this.srcBytes = HexFormat.of().parseHex(srcString);
            this.numBytes = this.srcBytes.length;
        }
        @Override
        public void nextBytes(byte[] bytes) {
            if (bytes.length > numBytes) {
                throw new RuntimeException("Not enough bytes, need "
                    + bytes.length + ", got " + numBytes);
            }
            System.arraycopy(this.srcBytes, this.srcBytes.length - numBytes, bytes, 0, bytes.length);
            numBytes -= bytes.length;
        }

    }

    public static void main(String[] args) throws Exception {
        //for (Provider provider : Security.getProviders()) {
        Provider p = Security.getProvider("SunRsaSign");
        Signature sig;
        try {
            sig = Signature.getInstance("RSASSA-PSS", p);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Skip testing RSASSA-PSS" +
                " due to no support");
            return;
        }

        boolean success = true;
        for (String f : testFiles) {
            System.out.println("[INPUT FILE " + f + "]");
            try {
                success &= runTest(SigRecord.read(f), sig);
            } catch (IOException e) {
                System.out.println("Unexpected exception: " + e);
                e.printStackTrace(System.out);
                success = false;
            }
        }

        if (!success) {
            throw new RuntimeException("One or more test failed");
        }
        System.out.println("Test passed");
    }

    /*
     * Run all the tests in the data list with specified algorithm
     */
    static boolean runTest(List<SigRecord> records, Signature sig) throws Exception {
        boolean success = true;
        KeyFactory kf = KeyFactory.getInstance("RSA", sig.getProvider());
        for (SigRecord sr : records) {
            System.out.println("==Testing Record : " + sr + "==");
            PrivateKey privKey = kf.generatePrivate(sr.privKeySpec);
            PublicKey pubKey = kf.generatePublic(sr.pubKeySpec);
            success &= check(sig, privKey, pubKey, sr.testVectors);
            System.out.println("==Done==");
        }
        return success;
    }

    /*
     * Generate the signature, check against known values and verify.
     */
    static boolean check(Signature sig, PrivateKey privKey, PublicKey pubKey,
        List<SigRecord.SigVector> vectors) throws Exception {

        boolean success = true;
        for (SigRecord.SigVector v : vectors) {
            System.out.println("\tAgainst " + v.mdAlg);
            byte[] msgBytes = HexFormat.of().parseHex(v.msg);
            byte[] expSigBytes = HexFormat.of().parseHex(v.sig);

            MyKnownRandomSrc saltSrc = new MyKnownRandomSrc(v.salt);
            sig.initSign(privKey, saltSrc);
            PSSParameterSpec params = new PSSParameterSpec(v.mdAlg, "MGF1",
                new MGF1ParameterSpec(v.mdAlg), saltSrc.numBytes, 1);
            sig.setParameter(params);
            sig.update(msgBytes);
            byte[] actualSigBytes = sig.sign();

            // Check if the supplied salt bytes are used up
            if (saltSrc.numBytes != 0) {
                throw new RuntimeException("Error: salt length mismatch! "
                    + saltSrc.numBytes + " bytes leftover");
            }

            success &= MessageDigest.isEqual(actualSigBytes, expSigBytes);

            if (!success) {
                System.out.println("\tFailed:");
                System.out.println("\tSHAALG       = " + v.mdAlg);
                System.out.println("\tMsg          = " + v.msg);
                System.out.println("\tSalt          = " + v.salt);
                System.out.println("\tExpected Sig = " + v.sig);
                System.out.println("\tActual Sig   = " + HexFormat.of().formatHex(actualSigBytes));
            } else {
                System.out.println("\t" + v.mdAlg + " Test Vector Passed");
            }
        }
        return success;
    }
}
