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
 * @bug 4923484 8146293
 * @summary test ASN.1 encoding generation/parsing for the OAEPParameters
 * implementation in SunJCE provider.
 * @author Valerie Peng
 */
import java.math.BigInteger;
import java.util.*;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.*;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class TestOAEPParameterSpec {

    private static Provider cp;

    private static boolean runTest(String mdName, MGF1ParameterSpec mgfSpec,
        byte[] p) throws Exception {
        OAEPParameterSpec spec = new OAEPParameterSpec(mdName, "MGF1",
            mgfSpec, new PSource.PSpecified(p));
        cp = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + cp.getName() + "...");
        AlgorithmParameters ap = AlgorithmParameters.getInstance("OAEP", cp);

        ap.init(spec);
        byte[] encoding = ap.getEncoded();

        AlgorithmParameters ap2 = AlgorithmParameters.getInstance("OAEP", cp);
        ap2.init(encoding);

        OAEPParameterSpec spec2 = (OAEPParameterSpec) ap2.getParameterSpec
                (OAEPParameterSpec.class);
        return compareSpec(spec, spec2);
    }

    private static boolean compareMD(OAEPParameterSpec s1,
        OAEPParameterSpec s2) {
        boolean result = false;
        String alg1 = s1.getDigestAlgorithm().toUpperCase().trim();
        String alg2 = s2.getDigestAlgorithm().toUpperCase().trim();
        alg1 = alg1.replaceAll("\\-", "");
        alg2 = alg2.replaceAll("\\-", "");
        if (alg1.equals("SHA") || alg1.equals("SHA1")) {
            result = (alg2.equals("SHA") || alg2.equals("SHA1"));
        } else {
            result = (alg1.equals(alg2));
        }
        return result;
    }


    private static boolean compareMGF(OAEPParameterSpec s1,
        OAEPParameterSpec s2) {
        String alg1 = s1.getMGFAlgorithm();
        String alg2 = s2.getMGFAlgorithm();
        if (alg1.equals(alg2)) {
            MGF1ParameterSpec mp1 = (MGF1ParameterSpec)s1.getMGFParameters();
            MGF1ParameterSpec mp2 = (MGF1ParameterSpec)s2.getMGFParameters();
            alg1 = mp1.getDigestAlgorithm();
            alg2 = mp2.getDigestAlgorithm();
            if (alg1.equals(alg2)) {
                return true;
            } else {
                System.out.println("MGF's MD algos: " + alg1 + " vs " + alg2);
                return false;
            }
        } else {
            System.out.println("MGF algos: " + alg1 + " vs " + alg2);
            return false;
        }
    }

    private static boolean comparePSource(OAEPParameterSpec s1,
        OAEPParameterSpec s2) {
        PSource src1 = s1.getPSource();
        PSource src2 = s2.getPSource();
        String alg1 = src1.getAlgorithm();
        String alg2 = src2.getAlgorithm();
        if (alg1.equals(alg2)) {
            // assumes they are PSource.PSpecified
            return Arrays.equals(((PSource.PSpecified) src1).getValue(),
                ((PSource.PSpecified) src2).getValue());
        } else {
            System.out.println("PSource algos: " + alg1 + " vs " + alg2);
            return false;
        }
    }

    private static boolean compareSpec(OAEPParameterSpec s1,
        OAEPParameterSpec s2) {
        return (compareMD(s1, s2) && compareMGF(s1, s2) &&
                comparePSource(s1, s2));
    }

    public static void main(String[] argv) throws Exception {
        boolean status = true;
        byte[] p = { (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04 };
        status &= runTest("SHA-224", MGF1ParameterSpec.SHA224, p);
        status &= runTest("SHA-256", MGF1ParameterSpec.SHA256, p);
        status &= runTest("SHA-384", MGF1ParameterSpec.SHA384, p);
        status &= runTest("SHA-512", MGF1ParameterSpec.SHA512, p);
        status &= runTest("SHA-512/224", MGF1ParameterSpec.SHA512_224, p);
        status &= runTest("SHA-512/256", MGF1ParameterSpec.SHA512_256, p);
        status &= runTest("SHA", MGF1ParameterSpec.SHA1, new byte[0]);
        status &= runTest("SHA-1", MGF1ParameterSpec.SHA1, new byte[0]);
        status &= runTest("SHA1", MGF1ParameterSpec.SHA1, new byte[0]);
        if (status) {
            System.out.println("Test Passed");
        } else {
            throw new Exception("One or More Test Failed");
        }
    }
}
