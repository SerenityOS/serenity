/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.java2d.marlin;

/**
 * Faster Math ceil / floor routines derived from StrictMath
 */
public final class FloatMath implements MarlinConst {

    // overflow / NaN handling enabled:
    static final boolean CHECK_OVERFLOW = true;
    static final boolean CHECK_NAN = true;
    // Copied from sun.misc.FloatConsts:
    public static final int FLOAT_SIGNIFICAND_WIDTH = 24;   // sun.misc.FloatConsts.SIGNIFICAND_WIDTH
    public static final int FLOAT_EXP_BIAS = 127;           // sun.misc.FloatConsts.EXP_BIAS
    public static final int FLOAT_EXP_BIT_MASK = 2139095040;// sun.misc.FloatConsts.EXP_BIT_MASK
    public static final int FLOAT_SIGNIF_BIT_MASK = 8388607;// sun.misc.FloatConsts.SIGNIF_BIT_MASK

    private FloatMath() {
        // utility class
    }

    // faster inlined min/max functions in the branch prediction is high
    static int max(final int a, final int b) {
        return (a >= b) ? a : b;
    }

    static int min(final int a, final int b) {
        return (a <= b) ? a : b;
    }

    /**
     * Returns the smallest (closest to negative infinity) {@code float} value
     * that is greater than or equal to the argument and is equal to a
     * mathematical integer. Special cases:
     * <ul><li>If the argument value is already equal to a mathematical integer,
     * then the result is the same as the argument.  <li>If the argument is NaN
     * or an infinity or positive zero or negative zero, then the result is the
     * same as the argument.  <li>If the argument value is less than zero but
     * greater than -1.0, then the result is negative zero.</ul> Note that the
     * value of {@code StrictMath.ceil(x)} is exactly the value of
     * {@code -StrictMath.floor(-x)}.
     *
     * @param a a value.
     * @return the smallest (closest to negative infinity) floating-point value
     * that is greater than or equal to the argument and is equal to a
     * mathematical integer.
     */
    public static float ceil_f(final float a) {
        // Derived from StrictMath.ceil(double):

        // Inline call to Math.getExponent(a) to
        // compute only once Float.floatToRawIntBits(a)
        final int doppel = Float.floatToRawIntBits(a);

        final int exponent = ((doppel & FLOAT_EXP_BIT_MASK)
                >> (FLOAT_SIGNIFICAND_WIDTH - 1))
                - FLOAT_EXP_BIAS;

        if (exponent < 0) {
            /*
             * Absolute value of argument is less than 1.
             * floorOrceil(-0.0) => -0.0
             * floorOrceil(+0.0) => +0.0
             */
            return ((a == 0.0f) ? a :
                    ( (a < 0.0f) ? -0.0f : 1.0f) );
        }
        if (CHECK_OVERFLOW && (exponent >= 23)) { // 52 for double
            /*
             * Infinity, NaN, or a value so large it must be integral.
             */
            return a;
        }
        // Else the argument is either an integral value already XOR it
        // has to be rounded to one.
        assert exponent >= 0 && exponent <= 22; // 51 for double

        final int intpart = doppel
                & (~(FLOAT_SIGNIF_BIT_MASK >> exponent));

        if (intpart == doppel) {
            return a; // integral value (including 0)
        }

        // 0 handled above as an integer
        // sign: 1 for negative, 0 for positive numbers
        // add : 0 for negative and 1 for positive numbers
        return Float.intBitsToFloat(intpart) + ((~intpart) >>> 31);
    }

    /**
     * Returns the largest (closest to positive infinity) {@code float} value
     * that is less than or equal to the argument and is equal to a mathematical
     * integer. Special cases:
     * <ul><li>If the argument value is already equal to a mathematical integer,
     * then the result is the same as the argument.  <li>If the argument is NaN
     * or an infinity or positive zero or negative zero, then the result is the
     * same as the argument.</ul>
     *
     * @param a a value.
     * @return the largest (closest to positive infinity) floating-point value
     * that less than or equal to the argument and is equal to a mathematical
     * integer.
     */
    public static float floor_f(final float a) {
        // Derived from StrictMath.floor(double):

        // Inline call to Math.getExponent(a) to
        // compute only once Float.floatToRawIntBits(a)
        final int doppel = Float.floatToRawIntBits(a);

        final int exponent = ((doppel & FLOAT_EXP_BIT_MASK)
                >> (FLOAT_SIGNIFICAND_WIDTH - 1))
                - FLOAT_EXP_BIAS;

        if (exponent < 0) {
            /*
             * Absolute value of argument is less than 1.
             * floorOrceil(-0.0) => -0.0
             * floorOrceil(+0.0) => +0.0
             */
            return ((a == 0.0f) ? a :
                    ( (a < 0.0f) ? -1.0f : 0.0f) );
        }
        if (CHECK_OVERFLOW && (exponent >= 23)) { // 52 for double
            /*
             * Infinity, NaN, or a value so large it must be integral.
             */
            return a;
        }
        // Else the argument is either an integral value already XOR it
        // has to be rounded to one.
        assert exponent >= 0 && exponent <= 22; // 51 for double

        final int intpart = doppel
                & (~(FLOAT_SIGNIF_BIT_MASK >> exponent));

        if (intpart == doppel) {
            return a; // integral value (including 0)
        }

        // 0 handled above as an integer
        // sign: 1 for negative, 0 for positive numbers
        // add : -1 for negative and 0 for positive numbers
        return Float.intBitsToFloat(intpart) + (intpart >> 31);
    }

    /**
     * Faster alternative to ceil(float) optimized for the integer domain
     * and supporting NaN and +/-Infinity.
     *
     * @param a a value.
     * @return the largest (closest to positive infinity) integer value
     * that less than or equal to the argument and is equal to a mathematical
     * integer.
     */
    public static int ceil_int(final float a) {
        final int intpart = (int) a;

        if (a <= intpart
                || (CHECK_OVERFLOW && intpart == Integer.MAX_VALUE)
                || CHECK_NAN && Float.isNaN(a)) {
            return intpart;
        }
        return intpart + 1;
    }

    /**
     * Faster alternative to ceil(double) optimized for the integer domain
     * and supporting NaN and +/-Infinity.
     *
     * @param a a value.
     * @return the largest (closest to positive infinity) integer value
     * that less than or equal to the argument and is equal to a mathematical
     * integer.
     */
    public static int ceil_int(final double a) {
        final int intpart = (int) a;

        if (a <= intpart
                || (CHECK_OVERFLOW && intpart == Integer.MAX_VALUE)
                || CHECK_NAN && Double.isNaN(a)) {
            return intpart;
        }
        return intpart + 1;
    }

    /**
     * Faster alternative to floor(float) optimized for the integer domain
     * and supporting NaN and +/-Infinity.
     *
     * @param a a value.
     * @return the largest (closest to positive infinity) floating-point value
     * that less than or equal to the argument and is equal to a mathematical
     * integer.
     */
    public static int floor_int(final float a) {
        final int intpart = (int) a;

        if (a >= intpart
                || (CHECK_OVERFLOW && intpart == Integer.MIN_VALUE)
                || CHECK_NAN && Float.isNaN(a)) {
            return intpart;
        }
        return intpart - 1;
    }

    /**
     * Faster alternative to floor(double) optimized for the integer domain
     * and supporting NaN and +/-Infinity.
     *
     * @param a a value.
     * @return the largest (closest to positive infinity) floating-point value
     * that less than or equal to the argument and is equal to a mathematical
     * integer.
     */
    public static int floor_int(final double a) {
        final int intpart = (int) a;

        if (a >= intpart
                || (CHECK_OVERFLOW && intpart == Integer.MIN_VALUE)
                || CHECK_NAN && Double.isNaN(a)) {
            return intpart;
        }
        return intpart - 1;
    }
}
