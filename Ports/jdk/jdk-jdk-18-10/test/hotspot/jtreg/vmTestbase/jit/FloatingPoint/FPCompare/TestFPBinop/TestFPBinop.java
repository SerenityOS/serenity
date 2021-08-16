/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/FloatingPoint/FPCompare/TestFPBinop.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.FPCompare.TestFPBinop.TestFPBinop
 */

package jit.FloatingPoint.FPCompare.TestFPBinop;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

/** Test of Floating Point Binary Ops.
 ** This is intended to be run on a known-correct system and the
 ** answer compared with the golden answer with diff or equivalent.
 */
public class TestFPBinop {
    public static final GoldChecker goldChecker = new GoldChecker( "TestFPBinop" );

    static float floatValues [] = {
        Float.MIN_VALUE, Float.MAX_VALUE,
        -Float.MIN_VALUE, -Float.MAX_VALUE,
        -1.0f, 1.0f, -0.0f, 0.0f,
        Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
        Float.NaN
    };
    static double doubleValues [] = {
        Double.MIN_VALUE, Double.MAX_VALUE,
        -Double.MIN_VALUE, -Double.MAX_VALUE,
        -1.0, 1.0, -0.0, 0.0,
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.NaN
    };

    static int nValues = floatValues.length;

    static float fOne, fZero;
    static double dOne, dZero;

    /* This is intended to thrwart an optimizing compiler
     * from simplifying some of the expressions by using algebraic
     * identities. */
    static {
        fOne = Integer.valueOf(1).floatValue();
        fZero = Integer.valueOf(0).floatValue();
        dOne = Integer.valueOf(1).doubleValue();
        dZero = Integer.valueOf(0).doubleValue();
    }

    static final boolean DEBUG = false;

    static String operandType = "";

    // static values
    static float xs, ys;
    static double xS, yS;

    /** Test of Floating Point Binary operators.
     ** The following orthogonal variables need to be tested.
     ** <ul>
     ** <li> Data type: float or double
     ** <li> Operator: +, -, *, /
     ** <li> Data values: +-normal, +-zero, NaN, +-infinity, +- min, +-max
     ** <li> Operand: variable, parameter, static, field, array element,
     **      function reference, expression, explicit constant.
     ** </ul>
     */
    public static void main (String [] args) {
        testFloats();
        testDoubles();
        TestFPBinop.goldChecker.check();
    }

    static void testFloats() {
        for (int i = 0; i < floatValues.length; i++) {
            float iVal = floatValues[i];
            for (int j = 0; j < floatValues.length; j++) {
                float jVal = floatValues[j];
                testFloat(iVal, jVal);
            }
        }
    }
    static void testDoubles() {
        for (int i = 0; i < doubleValues.length; i++) {
            double iVal = doubleValues[i];
            for (int j = 0; j < doubleValues.length; j++) {
                double jVal = doubleValues[j];
                testDouble(iVal, jVal);
            }
        }
    }

    static void testFloat (float x, float y) {

        testFloatP(x, y);
        testFloatL(x, y);
        testFloatS(x, y);
        testFloatF(x, y);
        testFloatA(x, y);
        testFloatM(x, y);
        testFloat1(x, y);
        testFloat2(x, y);
        testFloat3(x, y);
    }

    static void testFloatP(float x, float y) {

        check(x, y, x + y, "param", "+");
        check(x, y, x - y, "param", "-");
        check(x, y, x * y, "param", "*");
        check(x, y, x / y, "param", "/");

    }

    static void testFloatL(float x, float y) {

        float xl = x;
        float yl = y;

        check(xl, yl, xl + yl, "local", "+");
        check(xl, yl, xl - yl, "local", "-");
        check(xl, yl, xl * yl, "local", "*");
        check(xl, yl, xl / yl, "local", "/");

    }

    static void testFloatS(float x, float y) {

        xs = x;
        ys = y;

        check(xs, ys, xs + ys, "static", "+");
        check(xs, ys, xs - ys, "static", "-");
        check(xs, ys, xs * ys, "static", "*");
        check(xs, ys, xs / ys, "static", "/");

    }

    static void testFloatF(float x, float y) {

        FloatObject xo = new FloatObject(x);
        FloatObject yo = new FloatObject(y);

        check(xo.f, yo.f, xo.f + yo.f, "field", "+");
        check(xo.f, yo.f, xo.f - yo.f, "field", "-");
        check(xo.f, yo.f, xo.f * yo.f, "field", "*");
        check(xo.f, yo.f, xo.f / yo.f, "field", "/");

    }

    static void testFloatA(float x, float y) {

        int i = index(x);
        int j = index(y);
        float a [] = floatValues;

        check(a[i], a[j], a[i] + a[j], "a[i]", "+");
        check(a[i], a[j], a[i] - a[j], "a[i]", "-");
        check(a[i], a[j], a[i] * a[j], "a[i]", "*");
        check(a[i], a[j], a[i] / a[j], "a[i]", "/");

    }

    static void testFloatM(float x, float y) {

        check(i(x), i(y), i(x) + i(y), "f(x)", "+");
        check(i(x), i(y), i(x) - i(y), "f(x)", "-");
        check(i(x), i(y), i(x) * i(y), "f(x)", "*");
        check(i(x), i(y), i(x) / i(y), "f(x)", "/");

    }

    static void testFloat1(float x, float y) {

        float zero = fZero;
        float one = fOne;

        check(((x + zero) * one), y, ((x + zero) * one) + y, "lExpr", "+");
        check(((x + zero) * one), y, ((x + zero) * one) - y, "lExpr", "-");
        check(((x + zero) * one), y, ((x + zero) * one) * y, "lExpr", "*");
        check(((x + zero) * one), y, ((x + zero) * one) / y, "lExpr", "/");

    }

    static void testFloat3(float x, float y) {

        float zero = fZero;
        float one = fOne;

        check(((x + zero) * one), (zero + one * y), ((x + zero) * one) + (zero + one * y), "exprs", "+");
        check(((x + zero) * one), (zero + one * y), ((x + zero) * one) - (zero + one * y), "exprs", "-");
        check(((x + zero) * one), (zero + one * y), ((x + zero) * one) * (zero + one * y), "exprs", "*");
        check(((x + zero) * one), (zero + one * y), ((x + zero) * one) / (zero + one * y), "exprs", "/");

    }

    static void testFloat2(float x, float y) {

        float zero = fZero;
        float one = fOne;

        operandType = "rExpr";

        check(x, (zero + one * y), x + (zero + one * y), "rExpr", "+");
        check(x, (zero + one * y), x - (zero + one * y), "rExpr", "-");
        check(x, (zero + one * y), x * (zero + one * y), "rExpr", "*");
        check(x, (zero + one * y), x / (zero + one * y), "rExpr", "/");

    }

    static void testDouble (double x, double y) {

        testDoubleP(x, y);
        testDoubleL(x, y);
        testDoubleS(x, y);
        testDoubleF(x, y);
        testDoubleA(x, y);
        testDoubleM(x, y);
        testDouble1(x, y);
        testDouble2(x, y);
        testDouble3(x, y);
    }

    static void testDoubleP (double x, double y) {

        check(x, y, x + y, "param", "+");
        check(x, y, x - y, "param", "-");
        check(x, y, x * y, "param", "*");
        check(x, y, x / y, "param", "/");

    }

    static void testDoubleL (double x, double y) {

        double xl = x;
        double yl = y;

        check(xl, yl, xl + yl, "local", "+");
        check(xl, yl, xl - yl, "local", "-");
        check(xl, yl, xl * yl, "local", "*");
        check(xl, yl, xl / yl, "local", "/");

    }

    static void testDoubleS (double x, double y) {

        xS = x;
        yS = y;

        check(xS, yS, xS + yS, "static", "+");
        check(xS, yS, xS - yS, "static", "-");
        check(xS, yS, xS * yS, "static", "*");
        check(xS, yS, xS / yS, "static", "/");

    }

    static void testDoubleF (double x, double y) {

        DoubleObject xo = new DoubleObject(x);
        DoubleObject yo = new DoubleObject(y);

        check(xo.f, yo.f, xo.f + yo.f, "field", "+");
        check(xo.f, yo.f, xo.f - yo.f, "field", "-");
        check(xo.f, yo.f, xo.f * yo.f, "field", "*");
        check(xo.f, yo.f, xo.f / yo.f, "field", "/");

    }

    static void testDoubleA (double x, double y) {

        int i = index(x);
        int j = index(y);
        double a [] = doubleValues;

        check(a[i], a[j], a[i] + a[j], "a[i]", "+");
        check(a[i], a[j], a[i] - a[j], "a[i]", "-");
        check(a[i], a[j], a[i] * a[j], "a[i]", "*");
        check(a[i], a[j], a[i] / a[j], "a[i]", "/");

    }

    static void testDoubleM (double x, double y) {

        check(i(x), i(y), i(x) + i(y), "f(x)", "+");
        check(i(x), i(y), i(x) - i(y), "f(x)", "-");
        check(i(x), i(y), i(x) * i(y), "f(x)", "*");
        check(i(x), i(y), i(x) / i(y), "f(x)", "/");

    }

    static void testDouble1 (double x, double y) {

        double zero = dZero;
        double one = dOne;

        check(((x + zero) * one), y, ((x + zero) * one) + y, "lExpr", "+");
        check(((x + zero) * one), y, ((x + zero) * one) - y, "lExpr", "-");
        check(((x + zero) * one), y, ((x + zero) * one) * y, "lExpr", "*");
        check(((x + zero) * one), y, ((x + zero) * one) / y, "lExpr", "/");

    }

    static void testDouble3 (double x, double y) {

        double zero = dZero;
        double one = dOne;

        check(((x + zero) * one), (zero + one * y), ((x + zero) * one) + (zero + one * y), "exprs", "+");
        check(((x + zero) * one), (zero + one * y), ((x + zero) * one) - (zero + one * y), "exprs", "-");
        check(((x + zero) * one), (zero + one * y), ((x + zero) * one) * (zero + one * y), "exprs", "*");
        check(((x + zero) * one), (zero + one * y), ((x + zero) * one) / (zero + one * y), "exprs", "/");

    }

    static void testDouble2 (double x, double y) {

        double zero = dZero;
        double one = dOne;

        check(x, (zero + one * y), x + (zero + one * y), "rExpr", "+");
        check(x, (zero + one * y), x - (zero + one * y), "rExpr", "-");
        check(x, (zero + one * y), x * (zero + one * y), "rExpr", "*");
        check(x, (zero + one * y), x / (zero + one * y), "rExpr", "/");

    }


    /* The convoluted coding is intended to prevent inlining */
    static float i(float x) {
        while (Float.isNaN(x) && Float.floatToIntBits(x) == 0) {
            x = 0.0f;
        }
        return x;
    }
    static double i(double x) {
        while (Double.isNaN(x) && Double.doubleToLongBits(x) == 0L) {
            x = 0.0;
        }
        return x;
    }

    static int index(float x) {
        for (int i = 0; i < floatValues.length; i++) {
            if (Float.valueOf(x).equals(Float.valueOf(floatValues[i])))
                return i;
        }
        throw new TestFailure("ERROR: can't find " + x + " in floatValues.");
    }

    static int index(double x) {
        for (int i = 0; i < doubleValues.length; i++) {
            if (Double.valueOf(x).equals(Double.valueOf(doubleValues[i])))
                return i;
        }
        throw new TestFailure("ERROR: can't find " + x + " in doubleValues.");
    }

    static void check (float x, float y, float result,
                       String operands, String operator) {
      TestFPBinop.goldChecker.println(x + " " + operator + " " + y +
                         " = " + result + ", with float " +
                         operands + " operands");
    }

    static void check (double x, double y, double result,
                       String operands, String operator) {
      TestFPBinop.goldChecker.println(x + " " + operator + " " + y +
                         " = " + result + ", with double " +
                         operands + " operands");
    }

}

class FloatObject {
    public float f;

    public FloatObject(float x) {
        f = x;
    }
}

class DoubleObject {
    public double f;

    public DoubleObject(double x) {
        f = x;
    }
}
