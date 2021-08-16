/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8050374 8146293
 * @summary Verify a chain of signed objects
 * @library /test/lib
 * @build jdk.test.lib.SigTestUtil
 * @compile ../../../../java/security/SignedObject/Chain.java
 * @run main SignedObjectChain
 */

public class SignedObjectChain {

    private static class Test extends Chain.Test {

        public Test(Chain.SigAlg sigAlg) {
            super(sigAlg, Chain.KeyAlg.RSA, Chain.Provider.SunJSSE);
        }
    }

    private static final Test[] tests = {
        new Test(Chain.SigAlg.MD5andSHA1withRSA),
    };

    public static void main(String argv[]) {
        boolean resutl = java.util.Arrays.stream(tests).allMatch(
                (test) -> Chain.runTest(test));

        if(resutl) {
            System.out.println("All tests passed");
        } else {
            throw new RuntimeException("Some tests failed");
        }
    }
}
