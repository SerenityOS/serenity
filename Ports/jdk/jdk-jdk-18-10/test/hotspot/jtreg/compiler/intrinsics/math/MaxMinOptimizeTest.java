/*
 * Copyright (c) 2021, Huawei Technologies Co. Ltd. All rights reserved.
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
 * @bug 8263006
 * @summary Test the result of 8263006's optimization
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation
 *      compiler.intrinsics.math.MaxMinOptimizeTest
 */

package compiler.intrinsics.math;

import java.util.Arrays;

public class MaxMinOptimizeTest {

    private static final float fPos     =  15280.0f;
    private static final float fNeg     = -55555.5f;
    private static final float fPosZero =      0.0f;
    private static final float fNegZero =     -0.0f;
    private static final float fPosInf  = Float.POSITIVE_INFINITY;
    private static final float fNegInf  = Float.NEGATIVE_INFINITY;
    private static final float fNaN     = Float.NaN;

    private static final float fPosPosAdd = Float.intBitsToFloat(1190051840);
    private static final float fNegNegAdd = Float.intBitsToFloat(-942079104);
    private static final float fPosNegAdd = Float.intBitsToFloat(-954379392);
    private static final float fPosPosMul = Float.intBitsToFloat(1298049424);
    private static final float fNegNegMul = Float.intBitsToFloat(1329067759);
    private static final float fPosNegMul = Float.intBitsToFloat(-833985532);

    private static final double dPos     =  482390926662501720.0;
    private static final double dNeg     = -333333333333333333.3;
    private static final double dPosZero =                   0.0;
    private static final double dNegZero =                  -0.0;
    private static final double dPosInf  = Double.POSITIVE_INFINITY;
    private static final double dNegInf  = Double.NEGATIVE_INFINITY;
    private static final double dNaN     = Double.NaN;

    private static final double dPosPosAdd = Double.longBitsToDouble(4875928555416607765L);
    private static final double dNegNegAdd = Double.longBitsToDouble(-4349772506333936299L);
    private static final double dPosNegAdd = Double.longBitsToDouble(4864042047724301696L);
    private static final double dPosPosMul = Double.longBitsToDouble(5135907348984537565L);
    private static final double dNegNegMul = Double.longBitsToDouble(5131119721350321694L);
    private static final double dPosNegMul = Double.longBitsToDouble(-4089558839395905027L);

    private static final float[][] f_cases = {
        //     a         b         min       max        add          mul
        {     fPos,     fPos,     fPos,     fPos, fPosPosAdd, fPosPosMul},
        {     fNeg,     fNeg,     fNeg,     fNeg, fNegNegAdd, fNegNegMul},
        {     fPos,     fNeg,     fNeg,     fPos, fPosNegAdd, fPosNegMul},
        {     fNeg,     fPos,     fNeg,     fPos, fPosNegAdd, fPosNegMul},

        { fPosZero, fNegZero, fNegZero, fPosZero,   fPosZero,   fNegZero},
        { fNegZero, fPosZero, fNegZero, fPosZero,   fPosZero,   fNegZero},
        { fNegZero, fNegZero, fNegZero, fNegZero,   fNegZero,   fPosZero},

        {     fPos,  fPosInf,     fPos,  fPosInf,    fPosInf,    fPosInf},
        {     fNeg,  fNegInf,  fNegInf,     fNeg,    fNegInf,    fPosInf},

        {     fPos,     fNaN,     fNaN,     fNaN,       fNaN,       fNaN},
        {     fNaN,     fPos,     fNaN,     fNaN,       fNaN,       fNaN},
        {     fNeg,     fNaN,     fNaN,     fNaN,       fNaN,       fNaN},
        {     fNaN,     fNeg,     fNaN,     fNaN,       fNaN,       fNaN},

        {  fPosInf,     fNaN,     fNaN,     fNaN,       fNaN,       fNaN},
        {     fNaN,  fPosInf,     fNaN,     fNaN,       fNaN,       fNaN},
        {  fNegInf,     fNaN,     fNaN,     fNaN,       fNaN,       fNaN},
        {     fNaN,  fNegInf,     fNaN,     fNaN,       fNaN,       fNaN}
    };

    private static final double[][] d_cases = {
        //     a         b         min       max        add          mul
        {     dPos,     dPos,     dPos,     dPos, dPosPosAdd, dPosPosMul},
        {     dNeg,     dNeg,     dNeg,     dNeg, dNegNegAdd, dNegNegMul},
        {     dPos,     dNeg,     dNeg,     dPos, dPosNegAdd, dPosNegMul},
        {     dNeg,     dPos,     dNeg,     dPos, dPosNegAdd, dPosNegMul},

        { dPosZero, dNegZero, dNegZero, dPosZero,   dPosZero,   dNegZero},
        { dNegZero, dPosZero, dNegZero, dPosZero,   dPosZero,   dNegZero},
        { dNegZero, dNegZero, dNegZero, dNegZero,   dNegZero,   dPosZero},

        {     dPos,  dPosInf,     dPos,  dPosInf,    dPosInf,    dPosInf},
        {     dNeg,  dNegInf,  dNegInf,     dNeg,    dNegInf,    dPosInf},

        {     dPos,     dNaN,     dNaN,     dNaN,       dNaN,       dNaN},
        {     dNaN,     dPos,     dNaN,     dNaN,       dNaN,       dNaN},
        {     dNeg,     dNaN,     dNaN,     dNaN,       dNaN,       dNaN},
        {     dNaN,     dNeg,     dNaN,     dNaN,       dNaN,       dNaN},

        {  dPosInf,     dNaN,     dNaN,     dNaN,       dNaN,       dNaN},
        {     dNaN,  dPosInf,     dNaN,     dNaN,       dNaN,       dNaN},
        {  dNegInf,     dNaN,     dNaN,     dNaN,       dNaN,       dNaN},
        {     dNaN,  dNegInf,     dNaN,     dNaN,       dNaN,       dNaN}
    };

    static float add_opt_float(float a, float b) {
      return Math.max(a, b) + Math.min(a, b);
    }

    static float mul_opt_float(float a, float b) {
      return Math.max(a, b) * Math.min(a, b);
    }

    static float max_opt_float(float a, float b) {
      return Math.max(Math.max(a, b), Math.min(a, b));
    }

    static float min_opt_float(float a, float b) {
      return Math.min(Math.max(a, b), Math.min(a, b));
    }

    static double add_opt_double(double a, double b) {
      return Math.max(a, b) + Math.min(a, b);
    }

    static double mul_opt_double(double a, double b) {
      return Math.max(a, b) * Math.min(a, b);
    }

    static double max_opt_double(double a, double b) {
      return Math.max(Math.max(a, b), Math.min(a, b));
    }

    static double min_opt_double(double a, double b) {
      return Math.min(Math.max(a, b), Math.min(a, b));
    }

    private static void fTest(float[] row) {
        fCheck(row[0], row[1],
               min_opt_float(row[0], row[1]),
               max_opt_float(row[0], row[1]),
               add_opt_float(row[0], row[1]),
               mul_opt_float(row[0], row[1]),
               row[2], row[3], row[4], row[5]);
    }

    private static void fCheck(float a, float b, float fmin, float fmax, float fadd, float fmul, float efmin, float efmax, float efadd, float efmul) {
        int min = Float.floatToRawIntBits(fmin);
        int max = Float.floatToRawIntBits(fmax);
        int add = Float.floatToRawIntBits(fadd);
        int mul = Float.floatToRawIntBits(fmul);
        int emin = Float.floatToRawIntBits(efmin);
        int emax = Float.floatToRawIntBits(efmax);
        int eadd = Float.floatToRawIntBits(efadd);
        int emul = Float.floatToRawIntBits(efmul);

        if (min != emin || max != emax || add != eadd || mul != emul) {
            throw new AssertionError("Unexpected result of float test: " +
                    "a = " + a + ", b = " + b + ", " +
                    "result = (" + fmin + ", " + fmax + ", " + fadd + ", " + fmul + "), " +
                    "expected = (" + efmin + ", " + efmax + ", " + efadd + ", " + efmul + ")");
        }
    }

    private static void dTest(double[] row) {
        dCheck(row[0], row[1],
               min_opt_double(row[0], row[1]),
               max_opt_double(row[0], row[1]),
               add_opt_double(row[0], row[1]),
               mul_opt_double(row[0], row[1]),
               row[2], row[3], row[4], row[5]);
    }

    private static void dCheck(double a, double b, double dmin, double dmax, double dadd, double dmul, double edmin, double edmax, double edadd, double edmul) {
        long min = Double.doubleToRawLongBits(dmin);
        long max = Double.doubleToRawLongBits(dmax);
        long add = Double.doubleToRawLongBits(dadd);
        long mul = Double.doubleToRawLongBits(dmul);
        long emin = Double.doubleToRawLongBits(edmin);
        long emax = Double.doubleToRawLongBits(edmax);
        long eadd = Double.doubleToRawLongBits(edadd);
        long emul = Double.doubleToRawLongBits(edmul);

        if (min != emin || max != emax || add != eadd || mul != emul) {
            throw new AssertionError("Unexpected result of double test: " +
                    "a = " + a + ", b = " + b + ", " +
                    "result = (" + dmin + ", " + dmax + ", " + dadd + ", " + dmul + "), " +
                    "expected = (" + edmin + ", " + edmax + ", " + edadd + ", " + edmul + ")");
        }
    }

    public static void main(String[] args) {
      Arrays.stream(f_cases).forEach(MaxMinOptimizeTest::fTest);
      Arrays.stream(d_cases).forEach(MaxMinOptimizeTest::dTest);
    }
}
