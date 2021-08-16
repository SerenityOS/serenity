/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048604
 * @summary This test verifies the assertion "The encrypt/decrypt
 *  mechanism of cipher should perform correctly." for feature
 *  "CipherInputStream & CipherOutputStream".
 * @library ../
 * @run main CICOPBEFuncTest
 */

import java.util.Arrays;
import javax.crypto.Cipher;

public class CICOPBEFuncTest {

    public static void main(String[] args) throws Exception {
        for (PBEAlgorithm algorithm : PBEAlgorithm.values()) {
            // int buffertin test
            String algo = algorithm.baseAlgo.toUpperCase();
            if (!algo.contains("TRIPLEDES") && !algo.contains("AES_256")
                    || Cipher.getMaxAllowedKeyLength(algo) > 128) {
                // skip this if this key length is larger than what's
                // configured in the jce jurisdiction policy files
                System.out.println("Testing " + algorithm.getTransformation());
                for (String type : Arrays.asList(CICO_PBE_Test.INT_BYTE_BUFFER,
                        CICO_PBE_Test.BYTE_ARR_BUFFER)) {
                    new CICO_PBE_RW_Test(algorithm)
                        .proceedTest(type);
                    new CICO_PBE_SKIP_Test(algorithm)
                        .proceedTest(type);
                }
            }
        }
    }
}
