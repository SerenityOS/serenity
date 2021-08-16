/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4096278
   @summary Math.abs(+0.0) wrong
   @author Anand Palaniswamy
 */
public class AbsPositiveZero {
    private static boolean isPositiveZero(float f) {
        return Float.floatToIntBits(f) == Float.floatToIntBits(0.0f);
    }

    private static boolean isPositiveZero(double d) {
        return Double.doubleToLongBits(d) == Double.doubleToLongBits(0.0d);
    }

    public static void main(String[] args) throws Exception {
        if (!isPositiveZero(Math.abs(-0.0d))) {
            throw new Exception("abs(-0.0d) failed");
        }
        if (!isPositiveZero(Math.abs(+0.0d))) {
            throw new Exception("abs(+0.0d) failed");
        }
        if (Math.abs(Double.POSITIVE_INFINITY) != Double.POSITIVE_INFINITY) {
            throw new Exception("abs(+Inf) failed");
        }
        if (Math.abs(Double.NEGATIVE_INFINITY) != Double.POSITIVE_INFINITY) {
            throw new Exception("abs(-Inf) failed");
        }
        double dnanval = Math.abs(Double.NaN);
        if (dnanval == dnanval) {
            throw new Exception("abs(NaN) failed");
        }

        if (!isPositiveZero(Math.abs(-0.0f))) {
            throw new Exception("abs(-0.0f) failed");
        }
        if (!isPositiveZero(Math.abs(+0.0f))) {
            throw new Exception("abs(+0.0f) failed");
        }
        if (Math.abs(Float.POSITIVE_INFINITY) != Float.POSITIVE_INFINITY) {
            throw new Exception("abs(+Inf) failed");
        }
        if (Math.abs(Float.NEGATIVE_INFINITY) != Float.POSITIVE_INFINITY) {
            throw new Exception("abs(-Inf) failed");
        }
        float fnanval = Math.abs(Float.NaN);
        if (fnanval == fnanval) {
            throw new Exception("abs(NaN) failed");
        }
    }
}
