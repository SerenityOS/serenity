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
 * Portions Copyright IBM Corporation, 1997, 2001. All Rights Reserved.
 */

package java.math;
import java.io.*;

/**
 * Immutable objects which encapsulate the context settings which
 * describe certain rules for numerical operators, such as those
 * implemented by the {@link BigDecimal} class.
 *
 * <p>The base-independent settings are:
 * <ol>
 * <li>{@code precision}:
 * the number of digits to be used for an operation; results are
 * rounded to this precision
 *
 * <li>{@code roundingMode}:
 * a {@link RoundingMode} object which specifies the algorithm to be
 * used for rounding.
 * </ol>
 *
 * @see     BigDecimal
 * @see     RoundingMode
 * @author  Mike Cowlishaw
 * @author  Joseph D. Darcy
 * @since 1.5
 */

public final class MathContext implements Serializable {

    /* ----- Constants ----- */

    // defaults for constructors
    private static final int DEFAULT_DIGITS = 9;
    private static final RoundingMode DEFAULT_ROUNDINGMODE = RoundingMode.HALF_UP;
    // Smallest values for digits (Maximum is Integer.MAX_VALUE)
    private static final int MIN_DIGITS = 0;

    // Serialization version
    @java.io.Serial
    private static final long serialVersionUID = 5579720004786848255L;

    /* ----- Public Properties ----- */
    /**
     * A {@code MathContext} object whose settings have the values
     * required for unlimited precision arithmetic.
     * The values of the settings are: {@code precision=0 roundingMode=HALF_UP}
     */
    public static final MathContext UNLIMITED =
        new MathContext(0, RoundingMode.HALF_UP);

    /**
     * A {@code MathContext} object with a precision setting
     * matching the precision of the IEEE 754-2019 decimal32 format, 7 digits, and a
     * rounding mode of {@link RoundingMode#HALF_EVEN HALF_EVEN}.
     * Note the exponent range of decimal32 is <em>not</em> used for
     * rounding.
     */
    public static final MathContext DECIMAL32 =
        new MathContext(7, RoundingMode.HALF_EVEN);

    /**
     * A {@code MathContext} object with a precision setting
     * matching the precision of the IEEE 754-2019 decimal64 format, 16 digits, and a
     * rounding mode of {@link RoundingMode#HALF_EVEN HALF_EVEN}.
     * Note the exponent range of decimal64 is <em>not</em> used for
     * rounding.
     */
    public static final MathContext DECIMAL64 =
        new MathContext(16, RoundingMode.HALF_EVEN);

    /**
     * A {@code MathContext} object with a precision setting
     * matching the precision of the IEEE 754-2019 decimal128 format, 34 digits, and a
     * rounding mode of {@link RoundingMode#HALF_EVEN HALF_EVEN}.
     * Note the exponent range of decimal64 is <em>not</em> used for
     * rounding.
     */
    public static final MathContext DECIMAL128 =
        new MathContext(34, RoundingMode.HALF_EVEN);

    /* ----- Shared Properties ----- */
    /**
     * The number of digits to be used for an operation.  A value of 0
     * indicates that unlimited precision (as many digits as are
     * required) will be used.  Note that leading zeros (in the
     * coefficient of a number) are never significant.
     *
     * <p>{@code precision} will always be non-negative.
     *
     * @serial
     */
    final int precision;

    /**
     * The rounding algorithm to be used for an operation.
     *
     * @see RoundingMode
     * @serial
     */
    final RoundingMode roundingMode;

    /* ----- Constructors ----- */

    /**
     * Constructs a new {@code MathContext} with the specified
     * precision and the {@link RoundingMode#HALF_UP HALF_UP} rounding
     * mode.
     *
     * @param setPrecision The non-negative {@code int} precision setting.
     * @throws IllegalArgumentException if the {@code setPrecision} parameter is less
     *         than zero.
     */
    public MathContext(int setPrecision) {
        this(setPrecision, DEFAULT_ROUNDINGMODE);
        return;
    }

    /**
     * Constructs a new {@code MathContext} with a specified
     * precision and rounding mode.
     *
     * @param setPrecision The non-negative {@code int} precision setting.
     * @param setRoundingMode The rounding mode to use.
     * @throws IllegalArgumentException if the {@code setPrecision} parameter is less
     *         than zero.
     * @throws NullPointerException if the rounding mode argument is {@code null}
     */
    public MathContext(int setPrecision,
                       RoundingMode setRoundingMode) {
        if (setPrecision < MIN_DIGITS)
            throw new IllegalArgumentException("Digits < 0");
        if (setRoundingMode == null)
            throw new NullPointerException("null RoundingMode");

        precision = setPrecision;
        roundingMode = setRoundingMode;
        return;
    }

    /**
     * Constructs a new {@code MathContext} from a string.
     *
     * The string must be in the same format as that produced by the
     * {@link #toString} method.
     *
     * <p>An {@code IllegalArgumentException} is thrown if the precision
     * section of the string is out of range ({@code < 0}) or the string is
     * not in the format created by the {@link #toString} method.
     *
     * @param val The string to be parsed
     * @throws IllegalArgumentException if the precision section is out of range
     * or of incorrect format
     * @throws NullPointerException if the argument is {@code null}
     */
    public MathContext(String val) {
        boolean bad = false;
        int setPrecision;
        if (val == null)
            throw new NullPointerException("null String");
        try { // any error here is a string format problem
            if (!val.startsWith("precision=")) throw new RuntimeException();
            int fence = val.indexOf(' ');    // could be -1
            int off = 10;                     // where value starts
            setPrecision = Integer.parseInt(val.substring(10, fence));

            if (!val.startsWith("roundingMode=", fence+1))
                throw new RuntimeException();
            off = fence + 1 + 13;
            String str = val.substring(off, val.length());
            roundingMode = RoundingMode.valueOf(str);
        } catch (RuntimeException re) {
            throw new IllegalArgumentException("bad string format");
        }

        if (setPrecision < MIN_DIGITS)
            throw new IllegalArgumentException("Digits < 0");
        // the other parameters cannot be invalid if we got here
        precision = setPrecision;
    }

    /**
     * Returns the {@code precision} setting.
     * This value is always non-negative.
     *
     * @return an {@code int} which is the value of the {@code precision}
     *         setting
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Returns the roundingMode setting.
     * This will be one of
     * {@link  RoundingMode#CEILING},
     * {@link  RoundingMode#DOWN},
     * {@link  RoundingMode#FLOOR},
     * {@link  RoundingMode#HALF_DOWN},
     * {@link  RoundingMode#HALF_EVEN},
     * {@link  RoundingMode#HALF_UP},
     * {@link  RoundingMode#UNNECESSARY}, or
     * {@link  RoundingMode#UP}.
     *
     * @return a {@code RoundingMode} object which is the value of the
     *         {@code roundingMode} setting
     */

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    /**
     * Compares this {@code MathContext} with the specified
     * {@code Object} for equality.
     *
     * @param  x {@code Object} to which this {@code MathContext} is to
     *         be compared.
     * @return {@code true} if and only if the specified {@code Object} is
     *         a {@code MathContext} object which has exactly the same
     *         settings as this object
     */
    public boolean equals(Object x){
        if (!(x instanceof MathContext mc))
            return false;
        return mc.precision == this.precision
            && mc.roundingMode == this.roundingMode; // no need for .equals()
    }

    /**
     * Returns the hash code for this {@code MathContext}.
     *
     * @return hash code for this {@code MathContext}
     */
    public int hashCode() {
        return this.precision + roundingMode.hashCode() * 59;
    }

    /**
     * Returns the string representation of this {@code MathContext}.
     * The {@code String} returned represents the settings of the
     * {@code MathContext} object as two space-delimited words
     * (separated by a single space character, <code>'&#92;u0020'</code>,
     * and with no leading or trailing white space), as follows:
     * <ol>
     * <li>
     * The string {@code "precision="}, immediately followed
     * by the value of the precision setting as a numeric string as if
     * generated by the {@link Integer#toString(int) Integer.toString}
     * method.
     *
     * <li>
     * The string {@code "roundingMode="}, immediately
     * followed by the value of the {@code roundingMode} setting as a
     * word.  This word will be the same as the name of the
     * corresponding public constant in the {@link RoundingMode}
     * enum.
     * </ol>
     * <p>
     * For example:
     * <pre>
     * precision=9 roundingMode=HALF_UP
     * </pre>
     *
     * Additional words may be appended to the result of
     * {@code toString} in the future if more properties are added to
     * this class.
     *
     * @return a {@code String} representing the context settings
     */
    public java.lang.String toString() {
        return "precision=" +           precision + " " +
               "roundingMode=" +        roundingMode.toString();
    }

    // Private methods

    /**
     * Reconstitute the {@code MathContext} instance from a stream (that is,
     * deserialize it).
     *
     * @param  s the stream being read.
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();     // read in all fields
        // validate possibly bad fields
        if (precision < MIN_DIGITS) {
            String message = "MathContext: invalid digits in stream";
            throw new java.io.StreamCorruptedException(message);
        }
        if (roundingMode == null) {
            String message = "MathContext: null roundingMode in stream";
            throw new java.io.StreamCorruptedException(message);
        }
    }

}
