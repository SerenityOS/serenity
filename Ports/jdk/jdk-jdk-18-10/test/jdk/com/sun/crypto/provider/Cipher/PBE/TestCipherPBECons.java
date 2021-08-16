/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

/**
 * @test
 * @bug 8041787
 * @summary Verify that for PBEWithMD5AndDES cipher, only CBC mode and
 * PKCS#5Padding is allowed
 * @author Yun Ke
 * @author Bill Situ
 * @author Yu-Ching (Valerie) PENG
 */
public class TestCipherPBECons {

    private static final String[] PBEAlgorithms = {"pbeWithMD5ANDdes",
        "PBEWithMD5AndTripleDES"};
    private static final String[] cipherModes = {"ECb", "cbC", "cFB", "Cfb32",
        "OfB", "oFb64", "pCbC"};
    private static final String[] cipherPaddings = {"Pkcs5Padding", "NoPaDDing"};

    public static void main(String[] args) {
        TestCipherPBECons test = new TestCipherPBECons();
        Provider sunjce = Security.getProvider("SunJCE");

        if (!test.runAll(sunjce, System.out)) {
            throw new RuntimeException("One or more tests have failed....");
        }
    }

    public boolean runAll(Provider p, PrintStream out) {
        boolean finalResult = true;

        for (String algorithm : PBEAlgorithms) {
            for (String mode : cipherModes) {
                for (String padding : cipherPaddings) {
                    out.println("Running test with " + algorithm
                            + "/" + mode + "/" + padding);
                    try {
                        if (!runTest(p, algorithm, mode, padding, out)) {
                            finalResult = false;
                            out.println("STATUS: Failed");
                        } else {
                            out.println("STATUS: Passed");
                        }
                    } catch (Exception ex) {
                        finalResult = false;
                        ex.printStackTrace(out);
                        out.println("STATUS:Failed");
                    }
                }
            }
        }

        return finalResult;
    }

    public boolean runTest(Provider p, String algo, String mo, String pad,
            PrintStream out) throws Exception {
        try {
            // Initialization
            Cipher ci = Cipher.getInstance(algo + "/" + mo + "/" + pad, p);

            // No exception thrown, must be of the right mode and right
            // padding scheme
            return (mo.equalsIgnoreCase("CBC"))
                    && (pad.equalsIgnoreCase("PKCS5Padding"));
        } catch (NoSuchAlgorithmException ex) {
            if (p.getName().compareTo("SunJCE") == 0) {
                if (!(mo.equalsIgnoreCase("CBC")
                        && pad.equalsIgnoreCase("PKCS5Padding"))) {
                    out.println("NoSuchAlgorithmException is as expected");
                    return true;
                }
            }

            out.println("Caught exception: " + ex.getMessage());
            throw ex;
        } catch (NoSuchPaddingException ex) {
            if (mo.equalsIgnoreCase("CBC")
                    && pad.equalsIgnoreCase("NoPadding")) {
                out.println("NoSuchPaddingException is as expected");
                return true;
            } else {
                out.println("Caught unexpected exception: " + ex.getMessage());
                return false;
            }
        }
    }
}
