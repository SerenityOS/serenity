/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072452 8163498
 * @summary Support DHE sizes up to 8192-bits and DSA sizes up to 3072-bits
 *          This test has been split based on lower/higher key sizes in order to
 *          reduce individual execution times and run in parallel
 *          (see SupportedDSAParamGenLongKey.java)
 * @run main/timeout=300 SupportedDSAParamGen 1024 160
 * @run main/timeout=300 SupportedDSAParamGen 2048 224
 * @run main/timeout=300 SupportedDSAParamGen 2048 256
 */
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;

public class SupportedDSAParamGen {

    public static void main(String[] args) throws Exception {
        AlgorithmParameterGenerator apg =
            AlgorithmParameterGenerator.getInstance("DSA", "SUN");

        DSAGenParameterSpec spec = new DSAGenParameterSpec(
                Integer.valueOf(args[0]).intValue(),
                Integer.valueOf(args[1]).intValue());

        System.out.println("Generating (" + spec.getPrimePLength() +
                ", " + spec.getSubprimeQLength() + ") DSA Parameters");
        long start = System.currentTimeMillis();
        apg.init(spec, null);
        AlgorithmParameters param = apg.generateParameters();
        long stop = System.currentTimeMillis();
        System.out.println("Time: " + (stop - start) + " ms.");
        checkParamStrength(param, spec);
    }

    private static void checkParamStrength(AlgorithmParameters param,
            DSAGenParameterSpec genParam) throws Exception {

        String algo = param.getAlgorithm();
        if (!algo.equalsIgnoreCase("DSA")) {
            throw new Exception("Unexpected type of parameters: " + algo);
        }

        DSAParameterSpec spec = param.getParameterSpec(DSAParameterSpec.class);
        int valueL = spec.getP().bitLength();
        int strength = genParam.getPrimePLength();
        if (strength != valueL) {
            System.out.println(
                    "P: Expected " + strength + " but actual " + valueL);
            throw new Exception("Wrong P strength");
        }

        int valueN = spec.getQ().bitLength();
        strength = genParam.getSubprimeQLength();
        if (strength != valueN) {
            System.out.println(
                    "Q: Expected " + strength + " but actual " + valueN);
            throw new Exception("Wrong Q strength");
        }
    }
}
