/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Portions Copyright IBM Corporation, 2001. All Rights Reserved.
 */
package java.math;

/**
 * Specifies a <i>rounding policy</i> for numerical operations capable
 * of discarding precision. Each rounding mode indicates how the least
 * significant returned digit of a rounded result is to be calculated.
 * If fewer digits are returned than the digits needed to represent
 * the exact numerical result, the discarded digits will be referred
 * to as the <i>discarded fraction</i> regardless the digits'
 * contribution to the value of the number.  In other words,
 * considered as a numerical value, the discarded fraction could have
 * an absolute value greater than one.
 *
 * <p>Each rounding mode description includes a table listing how
 * different two-digit decimal values would round to a one digit
 * decimal value under the rounding mode in question.  The result
 * column in the tables could be gotten by creating a
 * {@code BigDecimal} number with the specified value, forming a
 * {@link MathContext} object with the proper settings
 * ({@code precision} set to {@code 1}, and the
 * {@code roundingMode} set to the rounding mode in question), and
 * calling {@link BigDecimal#round round} on this number with the
 * proper {@code MathContext}.  A summary table showing the results
 * of these rounding operations for all rounding modes appears below.
 *
 *<table class="striped">
 * <caption><b>Summary of Rounding Operations Under Different Rounding Modes</b></caption>
 * <thead>
 * <tr><th scope="col" rowspan="2">Input Number</th><th scope="col"colspan=8>Result of rounding input to one digit with the given
 *                           rounding mode</th>
 * <tr style="vertical-align:top">
 *                               <th>{@code UP}</th>
 *                                           <th>{@code DOWN}</th>
 *                                                        <th>{@code CEILING}</th>
 *                                                                       <th>{@code FLOOR}</th>
 *                                                                                    <th>{@code HALF_UP}</th>
 *                                                                                                   <th>{@code HALF_DOWN}</th>
 *                                                                                                                    <th>{@code HALF_EVEN}</th>
 *                                                                                                                                     <th>{@code UNNECESSARY}</th>
 * </thead>
 * <tbody style="text-align:right">
 *
 * <tr><th scope="row">5.5</th>  <td>6</td>  <td>5</td>    <td>6</td>    <td>5</td>  <td>6</td>      <td>5</td>       <td>6</td>       <td>throw {@code ArithmeticException}</td>
 * <tr><th scope="row">2.5</th>  <td>3</td>  <td>2</td>    <td>3</td>    <td>2</td>  <td>3</td>      <td>2</td>       <td>2</td>       <td>throw {@code ArithmeticException}</td>
 * <tr><th scope="row">1.6</th>  <td>2</td>  <td>1</td>    <td>2</td>    <td>1</td>  <td>2</td>      <td>2</td>       <td>2</td>       <td>throw {@code ArithmeticException}</td>
 * <tr><th scope="row">1.1</th>  <td>2</td>  <td>1</td>    <td>2</td>    <td>1</td>  <td>1</td>      <td>1</td>       <td>1</td>       <td>throw {@code ArithmeticException}</td>
 * <tr><th scope="row">1.0</th>  <td>1</td>  <td>1</td>    <td>1</td>    <td>1</td>  <td>1</td>      <td>1</td>       <td>1</td>       <td>1</td>
 * <tr><th scope="row">-1.0</th> <td>-1</td> <td>-1</td>   <td>-1</td>   <td>-1</td> <td>-1</td>     <td>-1</td>      <td>-1</td>      <td>-1</td>
 * <tr><th scope="row">-1.1</th> <td>-2</td> <td>-1</td>   <td>-1</td>   <td>-2</td> <td>-1</td>     <td>-1</td>      <td>-1</td>      <td>throw {@code ArithmeticException}</td>
 * <tr><th scope="row">-1.6</th> <td>-2</td> <td>-1</td>   <td>-1</td>   <td>-2</td> <td>-2</td>     <td>-2</td>      <td>-2</td>      <td>throw {@code ArithmeticException}</td>
 * <tr><th scope="row">-2.5</th> <td>-3</td> <td>-2</td>   <td>-2</td>   <td>-3</td> <td>-3</td>     <td>-2</td>      <td>-2</td>      <td>throw {@code ArithmeticException}</td>
 * <tr><th scope="row">-5.5</th> <td>-6</td> <td>-5</td>   <td>-5</td>   <td>-6</td> <td>-6</td>     <td>-5</td>      <td>-6</td>      <td>throw {@code ArithmeticException}</td>
 * </tbody>
 * </table>
 *
 *
 * <p>This {@code enum} is intended to replace the integer-based
 * enumeration of rounding mode constants in {@link BigDecimal}
 * ({@link BigDecimal#ROUND_UP}, {@link BigDecimal#ROUND_DOWN},
 * etc. ).
 *
 * @apiNote
 * Five of the rounding modes declared in this class correspond to
 * rounding-direction attributes defined in the <cite>IEEE Standard
 * for Floating-Point Arithmetic</cite>, IEEE 754-2019. Where present,
 * this correspondence will be noted in the documentation of the
 * particular constant.
 *
 * @see     BigDecimal
 * @see     MathContext
 * @author  Josh Bloch
 * @author  Mike Cowlishaw
 * @author  Joseph D. Darcy
 * @since 1.5
 */
@SuppressWarnings("deprecation") // Legacy rounding mode constants in BigDecimal
public enum RoundingMode {

        /**
         * Rounding mode to round away from zero.  Always increments the
         * digit prior to a non-zero discarded fraction.  Note that this
         * rounding mode never decreases the magnitude of the calculated
         * value.
         *
         *<p>Example:
         *<table class="striped">
         * <caption>Rounding mode UP Examples</caption>
         *<thead>
         *<tr style="vertical-align:top"><th scope="col">Input Number</th>
         *    <th scope="col">Input rounded to one digit<br> with {@code UP} rounding
         *</thead>
         *<tbody style="text-align:right">
         *<tr><th scope="row">5.5</th>  <td>6</td>
         *<tr><th scope="row">2.5</th>  <td>3</td>
         *<tr><th scope="row">1.6</th>  <td>2</td>
         *<tr><th scope="row">1.1</th>  <td>2</td>
         *<tr><th scope="row">1.0</th>  <td>1</td>
         *<tr><th scope="row">-1.0</th> <td>-1</td>
         *<tr><th scope="row">-1.1</th> <td>-2</td>
         *<tr><th scope="row">-1.6</th> <td>-2</td>
         *<tr><th scope="row">-2.5</th> <td>-3</td>
         *<tr><th scope="row">-5.5</th> <td>-6</td>
         *</tbody>
         *</table>
         */
    UP(BigDecimal.ROUND_UP),

        /**
         * Rounding mode to round towards zero.  Never increments the digit
         * prior to a discarded fraction (i.e., truncates).  Note that this
         * rounding mode never increases the magnitude of the calculated value.
         * This mode corresponds to the IEEE 754-2019 rounding-direction
         * attribute roundTowardZero.
         *
         *<p>Example:
         *<table class="striped">
         * <caption>Rounding mode DOWN Examples</caption>
         *<thead>
         *<tr style="vertical-align:top"><th scope="col">Input Number</th>
         *    <th scope="col">Input rounded to one digit<br> with {@code DOWN} rounding
         *</thead>
         *<tbody style="text-align:right">
         *<tr><th scope="row">5.5</th>  <td>5</td>
         *<tr><th scope="row">2.5</th>  <td>2</td>
         *<tr><th scope="row">1.6</th>  <td>1</td>
         *<tr><th scope="row">1.1</th>  <td>1</td>
         *<tr><th scope="row">1.0</th>  <td>1</td>
         *<tr><th scope="row">-1.0</th> <td>-1</td>
         *<tr><th scope="row">-1.1</th> <td>-1</td>
         *<tr><th scope="row">-1.6</th> <td>-1</td>
         *<tr><th scope="row">-2.5</th> <td>-2</td>
         *<tr><th scope="row">-5.5</th> <td>-5</td>
         *</tbody>
         *</table>
         */
    DOWN(BigDecimal.ROUND_DOWN),

        /**
         * Rounding mode to round towards positive infinity.  If the
         * result is positive, behaves as for {@code RoundingMode.UP};
         * if negative, behaves as for {@code RoundingMode.DOWN}.  Note
         * that this rounding mode never decreases the calculated value.
         * This mode corresponds to the IEEE 754-2019 rounding-direction
         * attribute roundTowardPositive.
         *
         *<p>Example:
         *<table class="striped">
         * <caption>Rounding mode CEILING Examples</caption>
         *<thead>
         *<tr style="vertical-align:top"><th>Input Number</th>
         *    <th>Input rounded to one digit<br> with {@code CEILING} rounding
         *</thead>
         *<tbody style="text-align:right">
         *<tr><th scope="row">5.5</th>  <td>6</td>
         *<tr><th scope="row">2.5</th>  <td>3</td>
         *<tr><th scope="row">1.6</th>  <td>2</td>
         *<tr><th scope="row">1.1</th>  <td>2</td>
         *<tr><th scope="row">1.0</th>  <td>1</td>
         *<tr><th scope="row">-1.0</th> <td>-1</td>
         *<tr><th scope="row">-1.1</th> <td>-1</td>
         *<tr><th scope="row">-1.6</th> <td>-1</td>
         *<tr><th scope="row">-2.5</th> <td>-2</td>
         *<tr><th scope="row">-5.5</th> <td>-5</td>
         *</tbody>
         *</table>
         */
    CEILING(BigDecimal.ROUND_CEILING),

        /**
         * Rounding mode to round towards negative infinity.  If the
         * result is positive, behave as for {@code RoundingMode.DOWN};
         * if negative, behave as for {@code RoundingMode.UP}.  Note that
         * this rounding mode never increases the calculated value.
         * This mode corresponds to the IEEE 754-2019 rounding-direction
         * attribute roundTowardNegative.
         *
         *<p>Example:
         *<table class="striped">
         * <caption>Rounding mode FLOOR Examples</caption>
         *<thead>
         *<tr style="vertical-align:top"><th scope="col">Input Number</th>
         *    <th scope="col">Input rounded to one digit<br> with {@code FLOOR} rounding
         *</thead>
         *<tbody style="text-align:right">
         *<tr><th scope="row">5.5</th>  <td>5</td>
         *<tr><th scope="row">2.5</th>  <td>2</td>
         *<tr><th scope="row">1.6</th>  <td>1</td>
         *<tr><th scope="row">1.1</th>  <td>1</td>
         *<tr><th scope="row">1.0</th>  <td>1</td>
         *<tr><th scope="row">-1.0</th> <td>-1</td>
         *<tr><th scope="row">-1.1</th> <td>-2</td>
         *<tr><th scope="row">-1.6</th> <td>-2</td>
         *<tr><th scope="row">-2.5</th> <td>-3</td>
         *<tr><th scope="row">-5.5</th> <td>-6</td>
         *</tbody>
         *</table>
         */
    FLOOR(BigDecimal.ROUND_FLOOR),

        /**
         * Rounding mode to round towards {@literal "nearest neighbor"}
         * unless both neighbors are equidistant, in which case round up.
         * Behaves as for {@code RoundingMode.UP} if the discarded
         * fraction is &ge; 0.5; otherwise, behaves as for
         * {@code RoundingMode.DOWN}.  Note that this is the rounding
         * mode commonly taught at school.
         * This mode corresponds to the IEEE 754-2019 rounding-direction
         * attribute roundTiesToAway.
         *
         *<p>Example:
         *<table class="striped">
         * <caption>Rounding mode HALF_UP Examples</caption>
         *<thead>
         *<tr style="vertical-align:top"><th scope="col">Input Number</th>
         *    <th scope="col">Input rounded to one digit<br> with {@code HALF_UP} rounding
         *</thead>
         *<tbody style="text-align:right">
         *<tr><th scope="row">5.5</th>  <td>6</td>
         *<tr><th scope="row">2.5</th>  <td>3</td>
         *<tr><th scope="row">1.6</th>  <td>2</td>
         *<tr><th scope="row">1.1</th>  <td>1</td>
         *<tr><th scope="row">1.0</th>  <td>1</td>
         *<tr><th scope="row">-1.0</th> <td>-1</td>
         *<tr><th scope="row">-1.1</th> <td>-1</td>
         *<tr><th scope="row">-1.6</th> <td>-2</td>
         *<tr><th scope="row">-2.5</th> <td>-3</td>
         *<tr><th scope="row">-5.5</th> <td>-6</td>
         *</tbody>
         *</table>
         */
    HALF_UP(BigDecimal.ROUND_HALF_UP),

        /**
         * Rounding mode to round towards {@literal "nearest neighbor"}
         * unless both neighbors are equidistant, in which case round
         * down.  Behaves as for {@code RoundingMode.UP} if the discarded
         * fraction is &gt; 0.5; otherwise, behaves as for
         * {@code RoundingMode.DOWN}.
         *
         *<p>Example:
         *<table class="striped">
         * <caption>Rounding mode HALF_DOWN Examples</caption>
         *<thead>
         *<tr style="vertical-align:top"><th scope="col">Input Number</th>
         *    <th scope="col">Input rounded to one digit<br> with {@code HALF_DOWN} rounding
         *</thead>
         *<tbody style="text-align:right">
         *<tr><th scope="row">5.5</th>  <td>5</td>
         *<tr><th scope="row">2.5</th>  <td>2</td>
         *<tr><th scope="row">1.6</th>  <td>2</td>
         *<tr><th scope="row">1.1</th>  <td>1</td>
         *<tr><th scope="row">1.0</th>  <td>1</td>
         *<tr><th scope="row">-1.0</th> <td>-1</td>
         *<tr><th scope="row">-1.1</th> <td>-1</td>
         *<tr><th scope="row">-1.6</th> <td>-2</td>
         *<tr><th scope="row">-2.5</th> <td>-2</td>
         *<tr><th scope="row">-5.5</th> <td>-5</td>
         *</tbody>
         *</table>
         */
    HALF_DOWN(BigDecimal.ROUND_HALF_DOWN),

        /**
         * Rounding mode to round towards the {@literal "nearest neighbor"}
         * unless both neighbors are equidistant, in which case, round
         * towards the even neighbor.  Behaves as for
         * {@code RoundingMode.HALF_UP} if the digit to the left of the
         * discarded fraction is odd; behaves as for
         * {@code RoundingMode.HALF_DOWN} if it's even.  Note that this
         * is the rounding mode that statistically minimizes cumulative
         * error when applied repeatedly over a sequence of calculations.
         * It is sometimes known as {@literal "Banker's rounding,"} and is
         * chiefly used in the USA.  This rounding mode is analogous to
         * the rounding policy used for {@code float} and {@code double}
         * arithmetic in Java.
         * This mode corresponds to the IEEE 754-2019 rounding-direction
         * attribute roundTiesToEven.
         *
         *<p>Example:
         *<table class="striped">
         * <caption>Rounding mode HALF_EVEN Examples</caption>
         *<thead>
         *<tr style="vertical-align:top"><th scope="col">Input Number</th>
         *    <th scope="col">Input rounded to one digit<br> with {@code HALF_EVEN} rounding
         *</thead>
         *<tbody style="text-align:right">
         *<tr><th scope="row">5.5</th>  <td>6</td>
         *<tr><th scope="row">2.5</th>  <td>2</td>
         *<tr><th scope="row">1.6</th>  <td>2</td>
         *<tr><th scope="row">1.1</th>  <td>1</td>
         *<tr><th scope="row">1.0</th>  <td>1</td>
         *<tr><th scope="row">-1.0</th> <td>-1</td>
         *<tr><th scope="row">-1.1</th> <td>-1</td>
         *<tr><th scope="row">-1.6</th> <td>-2</td>
         *<tr><th scope="row">-2.5</th> <td>-2</td>
         *<tr><th scope="row">-5.5</th> <td>-6</td>
         *</tbody>
         *</table>
         */
    HALF_EVEN(BigDecimal.ROUND_HALF_EVEN),

        /**
         * Rounding mode to assert that the requested operation has an exact
         * result, hence no rounding is necessary.  If this rounding mode is
         * specified on an operation that yields an inexact result, an
         * {@code ArithmeticException} is thrown.
         *<p>Example:
         *<table class="striped">
         * <caption>Rounding mode UNNECESSARY Examples</caption>
         *<thead>
         *<tr style="vertical-align:top"><th scope="col">Input Number</th>
         *    <th scope="col">Input rounded to one digit<br> with {@code UNNECESSARY} rounding
         *</thead>
         *<tbody style="text-align:right">
         *<tr><th scope="row">5.5</th>  <td>throw {@code ArithmeticException}</td>
         *<tr><th scope="row">2.5</th>  <td>throw {@code ArithmeticException}</td>
         *<tr><th scope="row">1.6</th>  <td>throw {@code ArithmeticException}</td>
         *<tr><th scope="row">1.1</th>  <td>throw {@code ArithmeticException}</td>
         *<tr><th scope="row">1.0</th>  <td>1</td>
         *<tr><th scope="row">-1.0</th> <td>-1</td>
         *<tr><th scope="row">-1.1</th> <td>throw {@code ArithmeticException}</td>
         *<tr><th scope="row">-1.6</th> <td>throw {@code ArithmeticException}</td>
         *<tr><th scope="row">-2.5</th> <td>throw {@code ArithmeticException}</td>
         *<tr><th scope="row">-5.5</th> <td>throw {@code ArithmeticException}</td>
         *</tbody>
         *</table>
         */
    UNNECESSARY(BigDecimal.ROUND_UNNECESSARY);

    // Corresponding BigDecimal rounding constant
    final int oldMode;

    /**
     * Constructor
     *
     * @param oldMode The {@code BigDecimal} constant corresponding to
     *        this mode
     */
    private RoundingMode(int oldMode) {
        this.oldMode = oldMode;
    }

    /**
     * Returns the {@code RoundingMode} object corresponding to a
     * legacy integer rounding mode constant in {@link BigDecimal}.
     *
     * @param  rm legacy integer rounding mode to convert
     * @return {@code RoundingMode} corresponding to the given integer.
     * @throws IllegalArgumentException integer is out of range
     */
    public static RoundingMode valueOf(int rm) {
        return switch (rm) {
            case BigDecimal.ROUND_UP          -> UP;
            case BigDecimal.ROUND_DOWN        -> DOWN;
            case BigDecimal.ROUND_CEILING     -> CEILING;
            case BigDecimal.ROUND_FLOOR       -> FLOOR;
            case BigDecimal.ROUND_HALF_UP     -> HALF_UP;
            case BigDecimal.ROUND_HALF_DOWN   -> HALF_DOWN;
            case BigDecimal.ROUND_HALF_EVEN   -> HALF_EVEN;
            case BigDecimal.ROUND_UNNECESSARY -> UNNECESSARY;
            default -> throw new IllegalArgumentException("argument out of range");
        };
    }
}
