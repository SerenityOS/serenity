/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7044060 8055351 8181048
 * @summary verify that DSA parameter generation works
 * @run main/timeout=600 TestAlgParameterGenerator
 */

import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.spec.DSAGenParameterSpec;
import java.security.spec.DSAParameterSpec;

public class TestAlgParameterGenerator {

    private static void checkParamStrength(AlgorithmParameters param,
            int strength) throws Exception {
        String algo = param.getAlgorithm();
        if (!algo.equalsIgnoreCase("DSA")) {
            throw new RuntimeException("Unexpected type of parameters: " + algo);
        }
        DSAParameterSpec spec = param.getParameterSpec(DSAParameterSpec.class);
        int valueL = spec.getP().bitLength();
        if (strength != valueL) {
            System.out.println("Expected " + strength + " but actual " + valueL);
            throw new RuntimeException("Wrong P strength");
        }
    }

    private static void checkParamStrength(AlgorithmParameters param,
            DSAGenParameterSpec genParam)
            throws Exception {
        String algo = param.getAlgorithm();
        if (!algo.equalsIgnoreCase("DSA")) {
            throw new RuntimeException("Unexpected type of parameters: " + algo);
        }
        DSAParameterSpec spec = param.getParameterSpec(DSAParameterSpec.class);
        int valueL = spec.getP().bitLength();
        int strength = genParam.getPrimePLength();
        if (strength != valueL) {
            System.out.println("P: Expected " + strength + " but actual " + valueL);
            throw new RuntimeException("Wrong P strength");
        }
        int valueN = spec.getQ().bitLength();
        strength = genParam.getSubprimeQLength();
        if (strength != valueN) {
            System.out.println("Q: Expected " + strength + " but actual " + valueN);
            throw new RuntimeException("Wrong Q strength");
        }
    }

    public static void main(String[] args) throws Exception {
        AlgorithmParameterGenerator apg
                = AlgorithmParameterGenerator.getInstance("DSA", "SUN");
        long start, stop;

        // make sure no-init still works
        start = System.currentTimeMillis();
        AlgorithmParameters param = apg.generateParameters();
        stop = System.currentTimeMillis();
        System.out.println("Time: " + (stop - start) + " ms.");

        // make sure the old model works
        int[] strengths = {512, 768, 1024};
        for (int sizeP : strengths) {
            System.out.println("Generating " + sizeP + "-bit DSA Parameters");
            start = System.currentTimeMillis();
            apg.init(sizeP);
            param = apg.generateParameters();
            stop = System.currentTimeMillis();
            System.out.println("Time: " + (stop - start) + " ms.");
            checkParamStrength(param, sizeP);
        }

        // now the newer model
        DSAGenParameterSpec[] specSet = {
            new DSAGenParameterSpec(1024, 160),
            new DSAGenParameterSpec(2048, 224),
            new DSAGenParameterSpec(2048, 256)
            // no support for prime size 3072
            // ,new DSAGenParameterSpec(3072, 256)
        };

        for (DSAGenParameterSpec genParam : specSet) {
            System.out.println("Generating (" + genParam.getPrimePLength()
                    + ", " + genParam.getSubprimeQLength() + ") DSA Parameters");
            start = System.currentTimeMillis();
            apg.init(genParam, null);
            param = apg.generateParameters();
            stop = System.currentTimeMillis();
            System.out.println("Time: " + (stop - start) + " ms.");
            checkParamStrength(param, genParam);
        }
    }
}
