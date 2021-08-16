/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8057758
 * @summary MultiplyToLen sets its return type to have a bottom offset which confuses code generation
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *      -XX:TypeProfileLevel=222
 *      compiler.intrinsics.bigInteger.TestMultiplyToLenReturnProfile
 */

package compiler.intrinsics.bigInteger;

import java.math.BigInteger;

public class TestMultiplyToLenReturnProfile {

    static BigInteger m(BigInteger i1, BigInteger i2) {
        BigInteger res = BigInteger.valueOf(0);
        for (int i = 0; i < 100; i++) {
            res.add(i1.multiply(i2));
        }
        return res;
    }

    static public void main(String[] args) {
        BigInteger v = BigInteger.valueOf(Integer.MAX_VALUE).pow(2);
        for (int i = 0; i < 20000; i++) {
            m(v, v.add(BigInteger.valueOf(1)));
        }
    }
}
