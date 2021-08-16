/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6371401
 * @summary Tests of shiftLeft and shiftRight on Integer.MIN_VALUE
 * @requires os.maxMemory >= 1g
 * @run main/othervm -Xmx512m ExtremeShiftingTests
 * @author Joseph D. Darcy
 */
import java.math.BigInteger;
import static java.math.BigInteger.*;

public class ExtremeShiftingTests {
    public static void main(String... args) {
        BigInteger bi = ONE.shiftLeft(Integer.MIN_VALUE);
        if (!bi.equals(ZERO))
            throw new RuntimeException("1 << " + Integer.MIN_VALUE);

        bi = ZERO.shiftLeft(Integer.MIN_VALUE);
        if (!bi.equals(ZERO))
            throw new RuntimeException("0 << " + Integer.MIN_VALUE);

        bi = BigInteger.valueOf(-1);
        bi = bi.shiftLeft(Integer.MIN_VALUE);
        if (!bi.equals(BigInteger.valueOf(-1)))
            throw new RuntimeException("-1 << " + Integer.MIN_VALUE);

        try {
            ONE.shiftRight(Integer.MIN_VALUE);
            throw new RuntimeException("1 >> " + Integer.MIN_VALUE);
        } catch (ArithmeticException ae) {
            ; // Expected
        }

        bi = ZERO.shiftRight(Integer.MIN_VALUE);
        if (!bi.equals(ZERO))
            throw new RuntimeException("0 >> " + Integer.MIN_VALUE);

        try {
            BigInteger.valueOf(-1).shiftRight(Integer.MIN_VALUE);
            throw new RuntimeException("-1 >> " + Integer.MIN_VALUE);
        } catch (ArithmeticException ae) {
            ; // Expected
        }

    }
}
