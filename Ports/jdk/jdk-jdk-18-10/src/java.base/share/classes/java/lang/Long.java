/*
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.lang.annotation.Native;
import java.lang.invoke.MethodHandles;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.math.*;
import java.util.Objects;
import java.util.Optional;

import jdk.internal.misc.CDS;
import jdk.internal.vm.annotation.IntrinsicCandidate;

import static java.lang.String.COMPACT_STRINGS;
import static java.lang.String.LATIN1;
import static java.lang.String.UTF16;

/**
 * The {@code Long} class wraps a value of the primitive type {@code
 * long} in an object. An object of type {@code Long} contains a
 * single field whose type is {@code long}.
 *
 * <p> In addition, this class provides several methods for converting
 * a {@code long} to a {@code String} and a {@code String} to a {@code
 * long}, as well as other constants and methods useful when dealing
 * with a {@code long}.
 *
 * <p>This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; programmers should treat instances that are
 * {@linkplain #equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may
 * occur. For example, in a future release, synchronization may fail.
 *
 * <p>Implementation note: The implementations of the "bit twiddling"
 * methods (such as {@link #highestOneBit(long) highestOneBit} and
 * {@link #numberOfTrailingZeros(long) numberOfTrailingZeros}) are
 * based on material from Henry S. Warren, Jr.'s <i>Hacker's
 * Delight</i>, (Addison Wesley, 2002).
 *
 * @author  Lee Boynton
 * @author  Arthur van Hoff
 * @author  Josh Bloch
 * @author  Joseph D. Darcy
 * @since   1.0
 */
@jdk.internal.ValueBased
public final class Long extends Number
        implements Comparable<Long>, Constable, ConstantDesc {
    /**
     * A constant holding the minimum value a {@code long} can
     * have, -2<sup>63</sup>.
     */
    @Native public static final long MIN_VALUE = 0x8000000000000000L;

    /**
     * A constant holding the maximum value a {@code long} can
     * have, 2<sup>63</sup>-1.
     */
    @Native public static final long MAX_VALUE = 0x7fffffffffffffffL;

    /**
     * The {@code Class} instance representing the primitive type
     * {@code long}.
     *
     * @since   1.1
     */
    @SuppressWarnings("unchecked")
    public static final Class<Long>     TYPE = (Class<Long>) Class.getPrimitiveClass("long");

    /**
     * Returns a string representation of the first argument in the
     * radix specified by the second argument.
     *
     * <p>If the radix is smaller than {@code Character.MIN_RADIX}
     * or larger than {@code Character.MAX_RADIX}, then the radix
     * {@code 10} is used instead.
     *
     * <p>If the first argument is negative, the first element of the
     * result is the ASCII minus sign {@code '-'}
     * ({@code '\u005Cu002d'}). If the first argument is not
     * negative, no sign character appears in the result.
     *
     * <p>The remaining characters of the result represent the magnitude
     * of the first argument. If the magnitude is zero, it is
     * represented by a single zero character {@code '0'}
     * ({@code '\u005Cu0030'}); otherwise, the first character of
     * the representation of the magnitude will not be the zero
     * character.  The following ASCII characters are used as digits:
     *
     * <blockquote>
     *   {@code 0123456789abcdefghijklmnopqrstuvwxyz}
     * </blockquote>
     *
     * These are {@code '\u005Cu0030'} through
     * {@code '\u005Cu0039'} and {@code '\u005Cu0061'} through
     * {@code '\u005Cu007a'}. If {@code radix} is
     * <var>N</var>, then the first <var>N</var> of these characters
     * are used as radix-<var>N</var> digits in the order shown. Thus,
     * the digits for hexadecimal (radix 16) are
     * {@code 0123456789abcdef}. If uppercase letters are
     * desired, the {@link java.lang.String#toUpperCase()} method may
     * be called on the result:
     *
     * <blockquote>
     *  {@code Long.toString(n, 16).toUpperCase()}
     * </blockquote>
     *
     * @param   i       a {@code long} to be converted to a string.
     * @param   radix   the radix to use in the string representation.
     * @return  a string representation of the argument in the specified radix.
     * @see     java.lang.Character#MAX_RADIX
     * @see     java.lang.Character#MIN_RADIX
     */
    public static String toString(long i, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            radix = 10;
        if (radix == 10)
            return toString(i);

        if (COMPACT_STRINGS) {
            byte[] buf = new byte[65];
            int charPos = 64;
            boolean negative = (i < 0);

            if (!negative) {
                i = -i;
            }

            while (i <= -radix) {
                buf[charPos--] = (byte)Integer.digits[(int)(-(i % radix))];
                i = i / radix;
            }
            buf[charPos] = (byte)Integer.digits[(int)(-i)];

            if (negative) {
                buf[--charPos] = '-';
            }
            return StringLatin1.newString(buf, charPos, (65 - charPos));
        }
        return toStringUTF16(i, radix);
    }

    private static String toStringUTF16(long i, int radix) {
        byte[] buf = new byte[65 * 2];
        int charPos = 64;
        boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }
        while (i <= -radix) {
            StringUTF16.putChar(buf, charPos--, Integer.digits[(int)(-(i % radix))]);
            i = i / radix;
        }
        StringUTF16.putChar(buf, charPos, Integer.digits[(int)(-i)]);
        if (negative) {
            StringUTF16.putChar(buf, --charPos, '-');
        }
        return StringUTF16.newString(buf, charPos, (65 - charPos));
    }

    /**
     * Returns a string representation of the first argument as an
     * unsigned integer value in the radix specified by the second
     * argument.
     *
     * <p>If the radix is smaller than {@code Character.MIN_RADIX}
     * or larger than {@code Character.MAX_RADIX}, then the radix
     * {@code 10} is used instead.
     *
     * <p>Note that since the first argument is treated as an unsigned
     * value, no leading sign character is printed.
     *
     * <p>If the magnitude is zero, it is represented by a single zero
     * character {@code '0'} ({@code '\u005Cu0030'}); otherwise,
     * the first character of the representation of the magnitude will
     * not be the zero character.
     *
     * <p>The behavior of radixes and the characters used as digits
     * are the same as {@link #toString(long, int) toString}.
     *
     * @param   i       an integer to be converted to an unsigned string.
     * @param   radix   the radix to use in the string representation.
     * @return  an unsigned string representation of the argument in the specified radix.
     * @see     #toString(long, int)
     * @since 1.8
     */
    public static String toUnsignedString(long i, int radix) {
        if (i >= 0)
            return toString(i, radix);
        else {
            return switch (radix) {
                case 2  -> toBinaryString(i);
                case 4  -> toUnsignedString0(i, 2);
                case 8  -> toOctalString(i);
                case 10 -> {
                    /*
                     * We can get the effect of an unsigned division by 10
                     * on a long value by first shifting right, yielding a
                     * positive value, and then dividing by 5.  This
                     * allows the last digit and preceding digits to be
                     * isolated more quickly than by an initial conversion
                     * to BigInteger.
                     */
                    long quot = (i >>> 1) / 5;
                    long rem = i - quot * 10;
                    yield toString(quot) + rem;
                }
                case 16 -> toHexString(i);
                case 32 -> toUnsignedString0(i, 5);
                default -> toUnsignedBigInteger(i).toString(radix);
            };
        }
    }

    /**
     * Return a BigInteger equal to the unsigned value of the
     * argument.
     */
    private static BigInteger toUnsignedBigInteger(long i) {
        if (i >= 0L)
            return BigInteger.valueOf(i);
        else {
            int upper = (int) (i >>> 32);
            int lower = (int) i;

            // return (upper << 32) + lower
            return (BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32).
                add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
        }
    }

    /**
     * Returns a string representation of the {@code long}
     * argument as an unsigned integer in base&nbsp;16.
     *
     * <p>The unsigned {@code long} value is the argument plus
     * 2<sup>64</sup> if the argument is negative; otherwise, it is
     * equal to the argument.  This value is converted to a string of
     * ASCII digits in hexadecimal (base&nbsp;16) with no extra
     * leading {@code 0}s.
     *
     * <p>The value of the argument can be recovered from the returned
     * string {@code s} by calling {@link
     * Long#parseUnsignedLong(String, int) Long.parseUnsignedLong(s,
     * 16)}.
     *
     * <p>If the unsigned magnitude is zero, it is represented by a
     * single zero character {@code '0'} ({@code '\u005Cu0030'});
     * otherwise, the first character of the representation of the
     * unsigned magnitude will not be the zero character. The
     * following characters are used as hexadecimal digits:
     *
     * <blockquote>
     *  {@code 0123456789abcdef}
     * </blockquote>
     *
     * These are the characters {@code '\u005Cu0030'} through
     * {@code '\u005Cu0039'} and  {@code '\u005Cu0061'} through
     * {@code '\u005Cu0066'}.  If uppercase letters are desired,
     * the {@link java.lang.String#toUpperCase()} method may be called
     * on the result:
     *
     * <blockquote>
     *  {@code Long.toHexString(n).toUpperCase()}
     * </blockquote>
     *
     * @apiNote
     * The {@link java.util.HexFormat} class provides formatting and parsing
     * of byte arrays and primitives to return a string or adding to an {@link Appendable}.
     * {@code HexFormat} formats and parses uppercase or lowercase hexadecimal characters,
     * with leading zeros and for byte arrays includes for each byte
     * a delimiter, prefix, and suffix.
     *
     * @param   i   a {@code long} to be converted to a string.
     * @return  the string representation of the unsigned {@code long}
     *          value represented by the argument in hexadecimal
     *          (base&nbsp;16).
     * @see java.util.HexFormat
     * @see #parseUnsignedLong(String, int)
     * @see #toUnsignedString(long, int)
     * @since   1.0.2
     */
    public static String toHexString(long i) {
        return toUnsignedString0(i, 4);
    }

    /**
     * Returns a string representation of the {@code long}
     * argument as an unsigned integer in base&nbsp;8.
     *
     * <p>The unsigned {@code long} value is the argument plus
     * 2<sup>64</sup> if the argument is negative; otherwise, it is
     * equal to the argument.  This value is converted to a string of
     * ASCII digits in octal (base&nbsp;8) with no extra leading
     * {@code 0}s.
     *
     * <p>The value of the argument can be recovered from the returned
     * string {@code s} by calling {@link
     * Long#parseUnsignedLong(String, int) Long.parseUnsignedLong(s,
     * 8)}.
     *
     * <p>If the unsigned magnitude is zero, it is represented by a
     * single zero character {@code '0'} ({@code '\u005Cu0030'});
     * otherwise, the first character of the representation of the
     * unsigned magnitude will not be the zero character. The
     * following characters are used as octal digits:
     *
     * <blockquote>
     *  {@code 01234567}
     * </blockquote>
     *
     * These are the characters {@code '\u005Cu0030'} through
     * {@code '\u005Cu0037'}.
     *
     * @param   i   a {@code long} to be converted to a string.
     * @return  the string representation of the unsigned {@code long}
     *          value represented by the argument in octal (base&nbsp;8).
     * @see #parseUnsignedLong(String, int)
     * @see #toUnsignedString(long, int)
     * @since   1.0.2
     */
    public static String toOctalString(long i) {
        return toUnsignedString0(i, 3);
    }

    /**
     * Returns a string representation of the {@code long}
     * argument as an unsigned integer in base&nbsp;2.
     *
     * <p>The unsigned {@code long} value is the argument plus
     * 2<sup>64</sup> if the argument is negative; otherwise, it is
     * equal to the argument.  This value is converted to a string of
     * ASCII digits in binary (base&nbsp;2) with no extra leading
     * {@code 0}s.
     *
     * <p>The value of the argument can be recovered from the returned
     * string {@code s} by calling {@link
     * Long#parseUnsignedLong(String, int) Long.parseUnsignedLong(s,
     * 2)}.
     *
     * <p>If the unsigned magnitude is zero, it is represented by a
     * single zero character {@code '0'} ({@code '\u005Cu0030'});
     * otherwise, the first character of the representation of the
     * unsigned magnitude will not be the zero character. The
     * characters {@code '0'} ({@code '\u005Cu0030'}) and {@code
     * '1'} ({@code '\u005Cu0031'}) are used as binary digits.
     *
     * @param   i   a {@code long} to be converted to a string.
     * @return  the string representation of the unsigned {@code long}
     *          value represented by the argument in binary (base&nbsp;2).
     * @see #parseUnsignedLong(String, int)
     * @see #toUnsignedString(long, int)
     * @since   1.0.2
     */
    public static String toBinaryString(long i) {
        return toUnsignedString0(i, 1);
    }

    /**
     * Format a long (treated as unsigned) into a String.
     * @param val the value to format
     * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
     */
    static String toUnsignedString0(long val, int shift) {
        // assert shift > 0 && shift <=5 : "Illegal shift value";
        int mag = Long.SIZE - Long.numberOfLeadingZeros(val);
        int chars = Math.max(((mag + (shift - 1)) / shift), 1);
        if (COMPACT_STRINGS) {
            byte[] buf = new byte[chars];
            formatUnsignedLong0(val, shift, buf, 0, chars);
            return new String(buf, LATIN1);
        } else {
            byte[] buf = new byte[chars * 2];
            formatUnsignedLong0UTF16(val, shift, buf, 0, chars);
            return new String(buf, UTF16);
        }
    }

    /**
     * Format a long (treated as unsigned) into a byte buffer (LATIN1 version). If
     * {@code len} exceeds the formatted ASCII representation of {@code val},
     * {@code buf} will be padded with leading zeroes.
     *
     * @param val the unsigned long to format
     * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
     * @param buf the byte buffer to write to
     * @param offset the offset in the destination buffer to start at
     * @param len the number of characters to write
     */
    private static void formatUnsignedLong0(long val, int shift, byte[] buf, int offset, int len) {
        int charPos = offset + len;
        int radix = 1 << shift;
        int mask = radix - 1;
        do {
            buf[--charPos] = (byte)Integer.digits[((int) val) & mask];
            val >>>= shift;
        } while (charPos > offset);
    }

    /**
     * Format a long (treated as unsigned) into a byte buffer (UTF16 version). If
     * {@code len} exceeds the formatted ASCII representation of {@code val},
     * {@code buf} will be padded with leading zeroes.
     *
     * @param val the unsigned long to format
     * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
     * @param buf the byte buffer to write to
     * @param offset the offset in the destination buffer to start at
     * @param len the number of characters to write
     */
    private static void formatUnsignedLong0UTF16(long val, int shift, byte[] buf, int offset, int len) {
        int charPos = offset + len;
        int radix = 1 << shift;
        int mask = radix - 1;
        do {
            StringUTF16.putChar(buf, --charPos, Integer.digits[((int) val) & mask]);
            val >>>= shift;
        } while (charPos > offset);
    }

    static String fastUUID(long lsb, long msb) {
        if (COMPACT_STRINGS) {
            byte[] buf = new byte[36];
            formatUnsignedLong0(lsb,        4, buf, 24, 12);
            formatUnsignedLong0(lsb >>> 48, 4, buf, 19, 4);
            formatUnsignedLong0(msb,        4, buf, 14, 4);
            formatUnsignedLong0(msb >>> 16, 4, buf, 9,  4);
            formatUnsignedLong0(msb >>> 32, 4, buf, 0,  8);

            buf[23] = '-';
            buf[18] = '-';
            buf[13] = '-';
            buf[8]  = '-';

            return new String(buf, LATIN1);
        } else {
            byte[] buf = new byte[72];

            formatUnsignedLong0UTF16(lsb,        4, buf, 24, 12);
            formatUnsignedLong0UTF16(lsb >>> 48, 4, buf, 19, 4);
            formatUnsignedLong0UTF16(msb,        4, buf, 14, 4);
            formatUnsignedLong0UTF16(msb >>> 16, 4, buf, 9,  4);
            formatUnsignedLong0UTF16(msb >>> 32, 4, buf, 0,  8);

            StringUTF16.putChar(buf, 23, '-');
            StringUTF16.putChar(buf, 18, '-');
            StringUTF16.putChar(buf, 13, '-');
            StringUTF16.putChar(buf,  8, '-');

            return new String(buf, UTF16);
        }
    }

    /**
     * Returns a {@code String} object representing the specified
     * {@code long}.  The argument is converted to signed decimal
     * representation and returned as a string, exactly as if the
     * argument and the radix 10 were given as arguments to the {@link
     * #toString(long, int)} method.
     *
     * @param   i   a {@code long} to be converted.
     * @return  a string representation of the argument in base&nbsp;10.
     */
    public static String toString(long i) {
        int size = stringSize(i);
        if (COMPACT_STRINGS) {
            byte[] buf = new byte[size];
            getChars(i, size, buf);
            return new String(buf, LATIN1);
        } else {
            byte[] buf = new byte[size * 2];
            StringUTF16.getChars(i, size, buf);
            return new String(buf, UTF16);
        }
    }

    /**
     * Returns a string representation of the argument as an unsigned
     * decimal value.
     *
     * The argument is converted to unsigned decimal representation
     * and returned as a string exactly as if the argument and radix
     * 10 were given as arguments to the {@link #toUnsignedString(long,
     * int)} method.
     *
     * @param   i  an integer to be converted to an unsigned string.
     * @return  an unsigned string representation of the argument.
     * @see     #toUnsignedString(long, int)
     * @since 1.8
     */
    public static String toUnsignedString(long i) {
        return toUnsignedString(i, 10);
    }

    /**
     * Places characters representing the long i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * @implNote This method converts positive inputs into negative
     * values, to cover the Long.MIN_VALUE case. Converting otherwise
     * (negative to positive) will expose -Long.MIN_VALUE that overflows
     * long.
     *
     * @param i     value to convert
     * @param index next index, after the least significant digit
     * @param buf   target buffer, Latin1-encoded
     * @return index of the most significant digit or minus sign, if present
     */
    static int getChars(long i, int index, byte[] buf) {
        long q;
        int r;
        int charPos = index;

        boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i <= Integer.MIN_VALUE) {
            q = i / 100;
            r = (int)((q * 100) - i);
            i = q;
            buf[--charPos] = Integer.DigitOnes[r];
            buf[--charPos] = Integer.DigitTens[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 <= -100) {
            q2 = i2 / 100;
            r  = (q2 * 100) - i2;
            i2 = q2;
            buf[--charPos] = Integer.DigitOnes[r];
            buf[--charPos] = Integer.DigitTens[r];
        }

        // We know there are at most two digits left at this point.
        q2 = i2 / 10;
        r  = (q2 * 10) - i2;
        buf[--charPos] = (byte)('0' + r);

        // Whatever left is the remaining digit.
        if (q2 < 0) {
            buf[--charPos] = (byte)('0' - q2);
        }

        if (negative) {
            buf[--charPos] = (byte)'-';
        }
        return charPos;
    }

    /**
     * Returns the string representation size for a given long value.
     *
     * @param x long value
     * @return string size
     *
     * @implNote There are other ways to compute this: e.g. binary search,
     * but values are biased heavily towards zero, and therefore linear search
     * wins. The iteration results are also routinely inlined in the generated
     * code after loop unrolling.
     */
    static int stringSize(long x) {
        int d = 1;
        if (x >= 0) {
            d = 0;
            x = -x;
        }
        long p = -10;
        for (int i = 1; i < 19; i++) {
            if (x > p)
                return i + d;
            p = 10 * p;
        }
        return 19 + d;
    }

    /**
     * Parses the string argument as a signed {@code long} in the
     * radix specified by the second argument. The characters in the
     * string must all be digits of the specified radix (as determined
     * by whether {@link java.lang.Character#digit(char, int)} returns
     * a nonnegative value), except that the first character may be an
     * ASCII minus sign {@code '-'} ({@code '\u005Cu002D'}) to
     * indicate a negative value or an ASCII plus sign {@code '+'}
     * ({@code '\u005Cu002B'}) to indicate a positive value. The
     * resulting {@code long} value is returned.
     *
     * <p>Note that neither the character {@code L}
     * ({@code '\u005Cu004C'}) nor {@code l}
     * ({@code '\u005Cu006C'}) is permitted to appear at the end
     * of the string as a type indicator, as would be permitted in
     * Java programming language source code - except that either
     * {@code L} or {@code l} may appear as a digit for a
     * radix greater than or equal to 22.
     *
     * <p>An exception of type {@code NumberFormatException} is
     * thrown if any of the following situations occurs:
     * <ul>
     *
     * <li>The first argument is {@code null} or is a string of
     * length zero.
     *
     * <li>The {@code radix} is either smaller than {@link
     * java.lang.Character#MIN_RADIX} or larger than {@link
     * java.lang.Character#MAX_RADIX}.
     *
     * <li>Any character of the string is not a digit of the specified
     * radix, except that the first character may be a minus sign
     * {@code '-'} ({@code '\u005Cu002d'}) or plus sign {@code
     * '+'} ({@code '\u005Cu002B'}) provided that the string is
     * longer than length 1.
     *
     * <li>The value represented by the string is not a value of type
     *      {@code long}.
     * </ul>
     *
     * <p>Examples:
     * <blockquote><pre>
     * parseLong("0", 10) returns 0L
     * parseLong("473", 10) returns 473L
     * parseLong("+42", 10) returns 42L
     * parseLong("-0", 10) returns 0L
     * parseLong("-FF", 16) returns -255L
     * parseLong("1100110", 2) returns 102L
     * parseLong("99", 8) throws a NumberFormatException
     * parseLong("Hazelnut", 10) throws a NumberFormatException
     * parseLong("Hazelnut", 36) returns 1356099454469L
     * </pre></blockquote>
     *
     * @param      s       the {@code String} containing the
     *                     {@code long} representation to be parsed.
     * @param      radix   the radix to be used while parsing {@code s}.
     * @return     the {@code long} represented by the string argument in
     *             the specified radix.
     * @throws     NumberFormatException  if the string does not contain a
     *             parsable {@code long}.
     */
    public static long parseLong(String s, int radix)
              throws NumberFormatException
    {
        if (s == null) {
            throw new NumberFormatException("Cannot parse null string");
        }

        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix +
                                            " less than Character.MIN_RADIX");
        }
        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix +
                                            " greater than Character.MAX_RADIX");
        }

        boolean negative = false;
        int i = 0, len = s.length();
        long limit = -Long.MAX_VALUE;

        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Long.MIN_VALUE;
                } else if (firstChar != '+') {
                    throw NumberFormatException.forInputString(s, radix);
                }

                if (len == 1) { // Cannot have lone "+" or "-"
                    throw NumberFormatException.forInputString(s, radix);
                }
                i++;
            }
            long multmin = limit / radix;
            long result = 0;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                int digit = Character.digit(s.charAt(i++),radix);
                if (digit < 0 || result < multmin) {
                    throw NumberFormatException.forInputString(s, radix);
                }
                result *= radix;
                if (result < limit + digit) {
                    throw NumberFormatException.forInputString(s, radix);
                }
                result -= digit;
            }
            return negative ? result : -result;
        } else {
            throw NumberFormatException.forInputString(s, radix);
        }
    }

    /**
     * Parses the {@link CharSequence} argument as a signed {@code long} in
     * the specified {@code radix}, beginning at the specified
     * {@code beginIndex} and extending to {@code endIndex - 1}.
     *
     * <p>The method does not take steps to guard against the
     * {@code CharSequence} being mutated while parsing.
     *
     * @param      s   the {@code CharSequence} containing the {@code long}
     *                  representation to be parsed
     * @param      beginIndex   the beginning index, inclusive.
     * @param      endIndex     the ending index, exclusive.
     * @param      radix   the radix to be used while parsing {@code s}.
     * @return     the signed {@code long} represented by the subsequence in
     *             the specified radix.
     * @throws     NullPointerException  if {@code s} is null.
     * @throws     IndexOutOfBoundsException  if {@code beginIndex} is
     *             negative, or if {@code beginIndex} is greater than
     *             {@code endIndex} or if {@code endIndex} is greater than
     *             {@code s.length()}.
     * @throws     NumberFormatException  if the {@code CharSequence} does not
     *             contain a parsable {@code long} in the specified
     *             {@code radix}, or if {@code radix} is either smaller than
     *             {@link java.lang.Character#MIN_RADIX} or larger than
     *             {@link java.lang.Character#MAX_RADIX}.
     * @since  9
     */
    public static long parseLong(CharSequence s, int beginIndex, int endIndex, int radix)
                throws NumberFormatException {
        Objects.requireNonNull(s);
        Objects.checkFromToIndex(beginIndex, endIndex, s.length());

        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix +
                    " less than Character.MIN_RADIX");
        }
        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix +
                    " greater than Character.MAX_RADIX");
        }

        boolean negative = false;
        int i = beginIndex;
        long limit = -Long.MAX_VALUE;

        if (i < endIndex) {
            char firstChar = s.charAt(i);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Long.MIN_VALUE;
                } else if (firstChar != '+') {
                    throw NumberFormatException.forCharSequence(s, beginIndex,
                            endIndex, i);
                }
                i++;
            }
            if (i >= endIndex) { // Cannot have lone "+", "-" or ""
                throw NumberFormatException.forCharSequence(s, beginIndex,
                        endIndex, i);
            }
            long multmin = limit / radix;
            long result = 0;
            while (i < endIndex) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                int digit = Character.digit(s.charAt(i), radix);
                if (digit < 0 || result < multmin) {
                    throw NumberFormatException.forCharSequence(s, beginIndex,
                            endIndex, i);
                }
                result *= radix;
                if (result < limit + digit) {
                    throw NumberFormatException.forCharSequence(s, beginIndex,
                            endIndex, i);
                }
                i++;
                result -= digit;
            }
            return negative ? result : -result;
        } else {
            throw new NumberFormatException("");
        }
    }

    /**
     * Parses the string argument as a signed decimal {@code long}.
     * The characters in the string must all be decimal digits, except
     * that the first character may be an ASCII minus sign {@code '-'}
     * ({@code \u005Cu002D'}) to indicate a negative value or an
     * ASCII plus sign {@code '+'} ({@code '\u005Cu002B'}) to
     * indicate a positive value. The resulting {@code long} value is
     * returned, exactly as if the argument and the radix {@code 10}
     * were given as arguments to the {@link
     * #parseLong(java.lang.String, int)} method.
     *
     * <p>Note that neither the character {@code L}
     * ({@code '\u005Cu004C'}) nor {@code l}
     * ({@code '\u005Cu006C'}) is permitted to appear at the end
     * of the string as a type indicator, as would be permitted in
     * Java programming language source code.
     *
     * @param      s   a {@code String} containing the {@code long}
     *             representation to be parsed
     * @return     the {@code long} represented by the argument in
     *             decimal.
     * @throws     NumberFormatException  if the string does not contain a
     *             parsable {@code long}.
     */
    public static long parseLong(String s) throws NumberFormatException {
        return parseLong(s, 10);
    }

    /**
     * Parses the string argument as an unsigned {@code long} in the
     * radix specified by the second argument.  An unsigned integer
     * maps the values usually associated with negative numbers to
     * positive numbers larger than {@code MAX_VALUE}.
     *
     * The characters in the string must all be digits of the
     * specified radix (as determined by whether {@link
     * java.lang.Character#digit(char, int)} returns a nonnegative
     * value), except that the first character may be an ASCII plus
     * sign {@code '+'} ({@code '\u005Cu002B'}). The resulting
     * integer value is returned.
     *
     * <p>An exception of type {@code NumberFormatException} is
     * thrown if any of the following situations occurs:
     * <ul>
     * <li>The first argument is {@code null} or is a string of
     * length zero.
     *
     * <li>The radix is either smaller than
     * {@link java.lang.Character#MIN_RADIX} or
     * larger than {@link java.lang.Character#MAX_RADIX}.
     *
     * <li>Any character of the string is not a digit of the specified
     * radix, except that the first character may be a plus sign
     * {@code '+'} ({@code '\u005Cu002B'}) provided that the
     * string is longer than length 1.
     *
     * <li>The value represented by the string is larger than the
     * largest unsigned {@code long}, 2<sup>64</sup>-1.
     *
     * </ul>
     *
     *
     * @param      s   the {@code String} containing the unsigned integer
     *                  representation to be parsed
     * @param      radix   the radix to be used while parsing {@code s}.
     * @return     the unsigned {@code long} represented by the string
     *             argument in the specified radix.
     * @throws     NumberFormatException if the {@code String}
     *             does not contain a parsable {@code long}.
     * @since 1.8
     */
    public static long parseUnsignedLong(String s, int radix)
                throws NumberFormatException {
        if (s == null)  {
            throw new NumberFormatException("Cannot parse null string");
        }

        int len = s.length();
        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar == '-') {
                throw new
                    NumberFormatException(String.format("Illegal leading minus sign " +
                                                       "on unsigned string %s.", s));
            } else {
                if (len <= 12 || // Long.MAX_VALUE in Character.MAX_RADIX is 13 digits
                    (radix == 10 && len <= 18) ) { // Long.MAX_VALUE in base 10 is 19 digits
                    return parseLong(s, radix);
                }

                // No need for range checks on len due to testing above.
                long first = parseLong(s, 0, len - 1, radix);
                int second = Character.digit(s.charAt(len - 1), radix);
                if (second < 0) {
                    throw new NumberFormatException("Bad digit at end of " + s);
                }
                long result = first * radix + second;

                /*
                 * Test leftmost bits of multiprecision extension of first*radix
                 * for overflow. The number of bits needed is defined by
                 * GUARD_BIT = ceil(log2(Character.MAX_RADIX)) + 1 = 7. Then
                 * int guard = radix*(int)(first >>> (64 - GUARD_BIT)) and
                 * overflow is tested by splitting guard in the ranges
                 * guard < 92, 92 <= guard < 128, and 128 <= guard, where
                 * 92 = 128 - Character.MAX_RADIX. Note that guard cannot take
                 * on a value which does not include a prime factor in the legal
                 * radix range.
                 */
                int guard = radix * (int) (first >>> 57);
                if (guard >= 128 ||
                    (result >= 0 && guard >= 128 - Character.MAX_RADIX)) {
                    /*
                     * For purposes of exposition, the programmatic statements
                     * below should be taken to be multi-precision, i.e., not
                     * subject to overflow.
                     *
                     * A) Condition guard >= 128:
                     * If guard >= 128 then first*radix >= 2^7 * 2^57 = 2^64
                     * hence always overflow.
                     *
                     * B) Condition guard < 92:
                     * Define left7 = first >>> 57.
                     * Given first = (left7 * 2^57) + (first & (2^57 - 1)) then
                     * result <= (radix*left7)*2^57 + radix*(2^57 - 1) + second.
                     * Thus if radix*left7 < 92, radix <= 36, and second < 36,
                     * then result < 92*2^57 + 36*(2^57 - 1) + 36 = 2^64 hence
                     * never overflow.
                     *
                     * C) Condition 92 <= guard < 128:
                     * first*radix + second >= radix*left7*2^57 + second
                     * so that first*radix + second >= 92*2^57 + 0 > 2^63
                     *
                     * D) Condition guard < 128:
                     * radix*first <= (radix*left7) * 2^57 + radix*(2^57 - 1)
                     * so
                     * radix*first + second <= (radix*left7) * 2^57 + radix*(2^57 - 1) + 36
                     * thus
                     * radix*first + second < 128 * 2^57 + 36*2^57 - radix + 36
                     * whence
                     * radix*first + second < 2^64 + 2^6*2^57 = 2^64 + 2^63
                     *
                     * E) Conditions C, D, and result >= 0:
                     * C and D combined imply the mathematical result
                     * 2^63 < first*radix + second < 2^64 + 2^63. The lower
                     * bound is therefore negative as a signed long, but the
                     * upper bound is too small to overflow again after the
                     * signed long overflows to positive above 2^64 - 1. Hence
                     * result >= 0 implies overflow given C and D.
                     */
                    throw new NumberFormatException(String.format("String value %s exceeds " +
                                                                  "range of unsigned long.", s));
                }
                return result;
            }
        } else {
            throw NumberFormatException.forInputString(s, radix);
        }
    }

    /**
     * Parses the {@link CharSequence} argument as an unsigned {@code long} in
     * the specified {@code radix}, beginning at the specified
     * {@code beginIndex} and extending to {@code endIndex - 1}.
     *
     * <p>The method does not take steps to guard against the
     * {@code CharSequence} being mutated while parsing.
     *
     * @param      s   the {@code CharSequence} containing the unsigned
     *                 {@code long} representation to be parsed
     * @param      beginIndex   the beginning index, inclusive.
     * @param      endIndex     the ending index, exclusive.
     * @param      radix   the radix to be used while parsing {@code s}.
     * @return     the unsigned {@code long} represented by the subsequence in
     *             the specified radix.
     * @throws     NullPointerException  if {@code s} is null.
     * @throws     IndexOutOfBoundsException  if {@code beginIndex} is
     *             negative, or if {@code beginIndex} is greater than
     *             {@code endIndex} or if {@code endIndex} is greater than
     *             {@code s.length()}.
     * @throws     NumberFormatException  if the {@code CharSequence} does not
     *             contain a parsable unsigned {@code long} in the specified
     *             {@code radix}, or if {@code radix} is either smaller than
     *             {@link java.lang.Character#MIN_RADIX} or larger than
     *             {@link java.lang.Character#MAX_RADIX}.
     * @since  9
     */
    public static long parseUnsignedLong(CharSequence s, int beginIndex, int endIndex, int radix)
                throws NumberFormatException {
        Objects.requireNonNull(s);
        Objects.checkFromToIndex(beginIndex, endIndex, s.length());

        int start = beginIndex, len = endIndex - beginIndex;

        if (len > 0) {
            char firstChar = s.charAt(start);
            if (firstChar == '-') {
                throw new NumberFormatException(String.format("Illegal leading minus sign " +
                        "on unsigned string %s.", s.subSequence(start, start + len)));
            } else {
                if (len <= 12 || // Long.MAX_VALUE in Character.MAX_RADIX is 13 digits
                    (radix == 10 && len <= 18) ) { // Long.MAX_VALUE in base 10 is 19 digits
                    return parseLong(s, start, start + len, radix);
                }

                // No need for range checks on end due to testing above.
                long first = parseLong(s, start, start + len - 1, radix);
                int second = Character.digit(s.charAt(start + len - 1), radix);
                if (second < 0) {
                    throw new NumberFormatException("Bad digit at end of " +
                            s.subSequence(start, start + len));
                }
                long result = first * radix + second;

                /*
                 * Test leftmost bits of multiprecision extension of first*radix
                 * for overflow. The number of bits needed is defined by
                 * GUARD_BIT = ceil(log2(Character.MAX_RADIX)) + 1 = 7. Then
                 * int guard = radix*(int)(first >>> (64 - GUARD_BIT)) and
                 * overflow is tested by splitting guard in the ranges
                 * guard < 92, 92 <= guard < 128, and 128 <= guard, where
                 * 92 = 128 - Character.MAX_RADIX. Note that guard cannot take
                 * on a value which does not include a prime factor in the legal
                 * radix range.
                 */
                int guard = radix * (int) (first >>> 57);
                if (guard >= 128 ||
                        (result >= 0 && guard >= 128 - Character.MAX_RADIX)) {
                    /*
                     * For purposes of exposition, the programmatic statements
                     * below should be taken to be multi-precision, i.e., not
                     * subject to overflow.
                     *
                     * A) Condition guard >= 128:
                     * If guard >= 128 then first*radix >= 2^7 * 2^57 = 2^64
                     * hence always overflow.
                     *
                     * B) Condition guard < 92:
                     * Define left7 = first >>> 57.
                     * Given first = (left7 * 2^57) + (first & (2^57 - 1)) then
                     * result <= (radix*left7)*2^57 + radix*(2^57 - 1) + second.
                     * Thus if radix*left7 < 92, radix <= 36, and second < 36,
                     * then result < 92*2^57 + 36*(2^57 - 1) + 36 = 2^64 hence
                     * never overflow.
                     *
                     * C) Condition 92 <= guard < 128:
                     * first*radix + second >= radix*left7*2^57 + second
                     * so that first*radix + second >= 92*2^57 + 0 > 2^63
                     *
                     * D) Condition guard < 128:
                     * radix*first <= (radix*left7) * 2^57 + radix*(2^57 - 1)
                     * so
                     * radix*first + second <= (radix*left7) * 2^57 + radix*(2^57 - 1) + 36
                     * thus
                     * radix*first + second < 128 * 2^57 + 36*2^57 - radix + 36
                     * whence
                     * radix*first + second < 2^64 + 2^6*2^57 = 2^64 + 2^63
                     *
                     * E) Conditions C, D, and result >= 0:
                     * C and D combined imply the mathematical result
                     * 2^63 < first*radix + second < 2^64 + 2^63. The lower
                     * bound is therefore negative as a signed long, but the
                     * upper bound is too small to overflow again after the
                     * signed long overflows to positive above 2^64 - 1. Hence
                     * result >= 0 implies overflow given C and D.
                     */
                    throw new NumberFormatException(String.format("String value %s exceeds " +
                            "range of unsigned long.", s.subSequence(start, start + len)));
                }
                return result;
            }
        } else {
            throw NumberFormatException.forInputString("", radix);
        }
    }

    /**
     * Parses the string argument as an unsigned decimal {@code long}. The
     * characters in the string must all be decimal digits, except
     * that the first character may be an ASCII plus sign {@code
     * '+'} ({@code '\u005Cu002B'}). The resulting integer value
     * is returned, exactly as if the argument and the radix 10 were
     * given as arguments to the {@link
     * #parseUnsignedLong(java.lang.String, int)} method.
     *
     * @param s   a {@code String} containing the unsigned {@code long}
     *            representation to be parsed
     * @return    the unsigned {@code long} value represented by the decimal string argument
     * @throws    NumberFormatException  if the string does not contain a
     *            parsable unsigned integer.
     * @since 1.8
     */
    public static long parseUnsignedLong(String s) throws NumberFormatException {
        return parseUnsignedLong(s, 10);
    }

    /**
     * Returns a {@code Long} object holding the value
     * extracted from the specified {@code String} when parsed
     * with the radix given by the second argument.  The first
     * argument is interpreted as representing a signed
     * {@code long} in the radix specified by the second
     * argument, exactly as if the arguments were given to the {@link
     * #parseLong(java.lang.String, int)} method. The result is a
     * {@code Long} object that represents the {@code long}
     * value specified by the string.
     *
     * <p>In other words, this method returns a {@code Long} object equal
     * to the value of:
     *
     * <blockquote>
     *  {@code new Long(Long.parseLong(s, radix))}
     * </blockquote>
     *
     * @param      s       the string to be parsed
     * @param      radix   the radix to be used in interpreting {@code s}
     * @return     a {@code Long} object holding the value
     *             represented by the string argument in the specified
     *             radix.
     * @throws     NumberFormatException  If the {@code String} does not
     *             contain a parsable {@code long}.
     */
    public static Long valueOf(String s, int radix) throws NumberFormatException {
        return Long.valueOf(parseLong(s, radix));
    }

    /**
     * Returns a {@code Long} object holding the value
     * of the specified {@code String}. The argument is
     * interpreted as representing a signed decimal {@code long},
     * exactly as if the argument were given to the {@link
     * #parseLong(java.lang.String)} method. The result is a
     * {@code Long} object that represents the integer value
     * specified by the string.
     *
     * <p>In other words, this method returns a {@code Long} object
     * equal to the value of:
     *
     * <blockquote>
     *  {@code new Long(Long.parseLong(s))}
     * </blockquote>
     *
     * @param      s   the string to be parsed.
     * @return     a {@code Long} object holding the value
     *             represented by the string argument.
     * @throws     NumberFormatException  If the string cannot be parsed
     *             as a {@code long}.
     */
    public static Long valueOf(String s) throws NumberFormatException
    {
        return Long.valueOf(parseLong(s, 10));
    }

    private static class LongCache {
        private LongCache() {}

        static final Long[] cache;
        static Long[] archivedCache;

        static {
            int size = -(-128) + 127 + 1;

            // Load and use the archived cache if it exists
            CDS.initializeFromArchive(LongCache.class);
            if (archivedCache == null || archivedCache.length != size) {
                Long[] c = new Long[size];
                long value = -128;
                for(int i = 0; i < size; i++) {
                    c[i] = new Long(value++);
                }
                archivedCache = c;
            }
            cache = archivedCache;
        }
    }

    /**
     * Returns a {@code Long} instance representing the specified
     * {@code long} value.
     * If a new {@code Long} instance is not required, this method
     * should generally be used in preference to the constructor
     * {@link #Long(long)}, as this method is likely to yield
     * significantly better space and time performance by caching
     * frequently requested values.
     *
     * This method will always cache values in the range -128 to 127,
     * inclusive, and may cache other values outside of this range.
     *
     * @param  l a long value.
     * @return a {@code Long} instance representing {@code l}.
     * @since  1.5
     */
    @IntrinsicCandidate
    public static Long valueOf(long l) {
        final int offset = 128;
        if (l >= -128 && l <= 127) { // will cache
            return LongCache.cache[(int)l + offset];
        }
        return new Long(l);
    }

    /**
     * Decodes a {@code String} into a {@code Long}.
     * Accepts decimal, hexadecimal, and octal numbers given by the
     * following grammar:
     *
     * <blockquote>
     * <dl>
     * <dt><i>DecodableString:</i>
     * <dd><i>Sign<sub>opt</sub> DecimalNumeral</i>
     * <dd><i>Sign<sub>opt</sub></i> {@code 0x} <i>HexDigits</i>
     * <dd><i>Sign<sub>opt</sub></i> {@code 0X} <i>HexDigits</i>
     * <dd><i>Sign<sub>opt</sub></i> {@code #} <i>HexDigits</i>
     * <dd><i>Sign<sub>opt</sub></i> {@code 0} <i>OctalDigits</i>
     *
     * <dt><i>Sign:</i>
     * <dd>{@code -}
     * <dd>{@code +}
     * </dl>
     * </blockquote>
     *
     * <i>DecimalNumeral</i>, <i>HexDigits</i>, and <i>OctalDigits</i>
     * are as defined in section {@jls 3.10.1} of
     * <cite>The Java Language Specification</cite>,
     * except that underscores are not accepted between digits.
     *
     * <p>The sequence of characters following an optional
     * sign and/or radix specifier ("{@code 0x}", "{@code 0X}",
     * "{@code #}", or leading zero) is parsed as by the {@code
     * Long.parseLong} method with the indicated radix (10, 16, or 8).
     * This sequence of characters must represent a positive value or
     * a {@link NumberFormatException} will be thrown.  The result is
     * negated if first character of the specified {@code String} is
     * the minus sign.  No whitespace characters are permitted in the
     * {@code String}.
     *
     * @param     nm the {@code String} to decode.
     * @return    a {@code Long} object holding the {@code long}
     *            value represented by {@code nm}
     * @throws    NumberFormatException  if the {@code String} does not
     *            contain a parsable {@code long}.
     * @see java.lang.Long#parseLong(String, int)
     * @since 1.2
     */
    public static Long decode(String nm) throws NumberFormatException {
        int radix = 10;
        int index = 0;
        boolean negative = false;
        Long result;

        if (nm.isEmpty())
            throw new NumberFormatException("Zero length string");
        char firstChar = nm.charAt(0);
        // Handle sign, if present
        if (firstChar == '-') {
            negative = true;
            index++;
        } else if (firstChar == '+')
            index++;

        // Handle radix specifier, if present
        if (nm.startsWith("0x", index) || nm.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        }
        else if (nm.startsWith("#", index)) {
            index ++;
            radix = 16;
        }
        else if (nm.startsWith("0", index) && nm.length() > 1 + index) {
            index ++;
            radix = 8;
        }

        if (nm.startsWith("-", index) || nm.startsWith("+", index))
            throw new NumberFormatException("Sign character in wrong position");

        try {
            result = Long.valueOf(nm.substring(index), radix);
            result = negative ? Long.valueOf(-result.longValue()) : result;
        } catch (NumberFormatException e) {
            // If number is Long.MIN_VALUE, we'll end up here. The next line
            // handles this case, and causes any genuine format error to be
            // rethrown.
            String constant = negative ? ("-" + nm.substring(index))
                                       : nm.substring(index);
            result = Long.valueOf(constant, radix);
        }
        return result;
    }

    /**
     * The value of the {@code Long}.
     *
     * @serial
     */
    private final long value;

    /**
     * Constructs a newly allocated {@code Long} object that
     * represents the specified {@code long} argument.
     *
     * @param   value   the value to be represented by the
     *          {@code Long} object.
     *
     * @deprecated
     * It is rarely appropriate to use this constructor. The static factory
     * {@link #valueOf(long)} is generally a better choice, as it is
     * likely to yield significantly better space and time performance.
     */
    @Deprecated(since="9", forRemoval = true)
    public Long(long value) {
        this.value = value;
    }

    /**
     * Constructs a newly allocated {@code Long} object that
     * represents the {@code long} value indicated by the
     * {@code String} parameter. The string is converted to a
     * {@code long} value in exactly the manner used by the
     * {@code parseLong} method for radix 10.
     *
     * @param      s   the {@code String} to be converted to a
     *             {@code Long}.
     * @throws     NumberFormatException  if the {@code String} does not
     *             contain a parsable {@code long}.
     *
     * @deprecated
     * It is rarely appropriate to use this constructor.
     * Use {@link #parseLong(String)} to convert a string to a
     * {@code long} primitive, or use {@link #valueOf(String)}
     * to convert a string to a {@code Long} object.
     */
    @Deprecated(since="9", forRemoval = true)
    public Long(String s) throws NumberFormatException {
        this.value = parseLong(s, 10);
    }

    /**
     * Returns the value of this {@code Long} as a {@code byte} after
     * a narrowing primitive conversion.
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    public byte byteValue() {
        return (byte)value;
    }

    /**
     * Returns the value of this {@code Long} as a {@code short} after
     * a narrowing primitive conversion.
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    public short shortValue() {
        return (short)value;
    }

    /**
     * Returns the value of this {@code Long} as an {@code int} after
     * a narrowing primitive conversion.
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    public int intValue() {
        return (int)value;
    }

    /**
     * Returns the value of this {@code Long} as a
     * {@code long} value.
     */
    @IntrinsicCandidate
    public long longValue() {
        return value;
    }

    /**
     * Returns the value of this {@code Long} as a {@code float} after
     * a widening primitive conversion.
     * @jls 5.1.2 Widening Primitive Conversion
     */
    public float floatValue() {
        return (float)value;
    }

    /**
     * Returns the value of this {@code Long} as a {@code double}
     * after a widening primitive conversion.
     * @jls 5.1.2 Widening Primitive Conversion
     */
    public double doubleValue() {
        return (double)value;
    }

    /**
     * Returns a {@code String} object representing this
     * {@code Long}'s value.  The value is converted to signed
     * decimal representation and returned as a string, exactly as if
     * the {@code long} value were given as an argument to the
     * {@link java.lang.Long#toString(long)} method.
     *
     * @return  a string representation of the value of this object in
     *          base&nbsp;10.
     */
    public String toString() {
        return toString(value);
    }

    /**
     * Returns a hash code for this {@code Long}. The result is
     * the exclusive OR of the two halves of the primitive
     * {@code long} value held by this {@code Long}
     * object. That is, the hashcode is the value of the expression:
     *
     * <blockquote>
     *  {@code (int)(this.longValue()^(this.longValue()>>>32))}
     * </blockquote>
     *
     * @return  a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    /**
     * Returns a hash code for a {@code long} value; compatible with
     * {@code Long.hashCode()}.
     *
     * @param value the value to hash
     * @return a hash code value for a {@code long} value.
     * @since 1.8
     */
    public static int hashCode(long value) {
        return (int)(value ^ (value >>> 32));
    }

    /**
     * Compares this object to the specified object.  The result is
     * {@code true} if and only if the argument is not
     * {@code null} and is a {@code Long} object that
     * contains the same {@code long} value as this object.
     *
     * @param   obj   the object to compare with.
     * @return  {@code true} if the objects are the same;
     *          {@code false} otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Long) {
            return value == ((Long)obj).longValue();
        }
        return false;
    }

    /**
     * Determines the {@code long} value of the system property
     * with the specified name.
     *
     * <p>The first argument is treated as the name of a system
     * property.  System properties are accessible through the {@link
     * java.lang.System#getProperty(java.lang.String)} method. The
     * string value of this property is then interpreted as a {@code
     * long} value using the grammar supported by {@link Long#decode decode}
     * and a {@code Long} object representing this value is returned.
     *
     * <p>If there is no property with the specified name, if the
     * specified name is empty or {@code null}, or if the property
     * does not have the correct numeric format, then {@code null} is
     * returned.
     *
     * <p>In other words, this method returns a {@code Long} object
     * equal to the value of:
     *
     * <blockquote>
     *  {@code getLong(nm, null)}
     * </blockquote>
     *
     * @param   nm   property name.
     * @return  the {@code Long} value of the property.
     * @throws  SecurityException for the same reasons as
     *          {@link System#getProperty(String) System.getProperty}
     * @see     java.lang.System#getProperty(java.lang.String)
     * @see     java.lang.System#getProperty(java.lang.String, java.lang.String)
     */
    public static Long getLong(String nm) {
        return getLong(nm, null);
    }

    /**
     * Determines the {@code long} value of the system property
     * with the specified name.
     *
     * <p>The first argument is treated as the name of a system
     * property.  System properties are accessible through the {@link
     * java.lang.System#getProperty(java.lang.String)} method. The
     * string value of this property is then interpreted as a {@code
     * long} value using the grammar supported by {@link Long#decode decode}
     * and a {@code Long} object representing this value is returned.
     *
     * <p>The second argument is the default value. A {@code Long} object
     * that represents the value of the second argument is returned if there
     * is no property of the specified name, if the property does not have
     * the correct numeric format, or if the specified name is empty or null.
     *
     * <p>In other words, this method returns a {@code Long} object equal
     * to the value of:
     *
     * <blockquote>
     *  {@code getLong(nm, new Long(val))}
     * </blockquote>
     *
     * but in practice it may be implemented in a manner such as:
     *
     * <blockquote><pre>
     * Long result = getLong(nm, null);
     * return (result == null) ? new Long(val) : result;
     * </pre></blockquote>
     *
     * to avoid the unnecessary allocation of a {@code Long} object when
     * the default value is not needed.
     *
     * @param   nm    property name.
     * @param   val   default value.
     * @return  the {@code Long} value of the property.
     * @throws  SecurityException for the same reasons as
     *          {@link System#getProperty(String) System.getProperty}
     * @see     java.lang.System#getProperty(java.lang.String)
     * @see     java.lang.System#getProperty(java.lang.String, java.lang.String)
     */
    public static Long getLong(String nm, long val) {
        Long result = Long.getLong(nm, null);
        return (result == null) ? Long.valueOf(val) : result;
    }

    /**
     * Returns the {@code long} value of the system property with
     * the specified name.  The first argument is treated as the name
     * of a system property.  System properties are accessible through
     * the {@link java.lang.System#getProperty(java.lang.String)}
     * method. The string value of this property is then interpreted
     * as a {@code long} value, as per the
     * {@link Long#decode decode} method, and a {@code Long} object
     * representing this value is returned; in summary:
     *
     * <ul>
     * <li>If the property value begins with the two ASCII characters
     * {@code 0x} or the ASCII character {@code #}, not followed by
     * a minus sign, then the rest of it is parsed as a hexadecimal integer
     * exactly as for the method {@link #valueOf(java.lang.String, int)}
     * with radix 16.
     * <li>If the property value begins with the ASCII character
     * {@code 0} followed by another character, it is parsed as
     * an octal integer exactly as by the method {@link
     * #valueOf(java.lang.String, int)} with radix 8.
     * <li>Otherwise the property value is parsed as a decimal
     * integer exactly as by the method
     * {@link #valueOf(java.lang.String, int)} with radix 10.
     * </ul>
     *
     * <p>Note that, in every case, neither {@code L}
     * ({@code '\u005Cu004C'}) nor {@code l}
     * ({@code '\u005Cu006C'}) is permitted to appear at the end
     * of the property value as a type indicator, as would be
     * permitted in Java programming language source code.
     *
     * <p>The second argument is the default value. The default value is
     * returned if there is no property of the specified name, if the
     * property does not have the correct numeric format, or if the
     * specified name is empty or {@code null}.
     *
     * @param   nm   property name.
     * @param   val   default value.
     * @return  the {@code Long} value of the property.
     * @throws  SecurityException for the same reasons as
     *          {@link System#getProperty(String) System.getProperty}
     * @see     System#getProperty(java.lang.String)
     * @see     System#getProperty(java.lang.String, java.lang.String)
     */
    public static Long getLong(String nm, Long val) {
        String v = null;
        try {
            v = System.getProperty(nm);
        } catch (IllegalArgumentException | NullPointerException e) {
        }
        if (v != null) {
            try {
                return Long.decode(v);
            } catch (NumberFormatException e) {
            }
        }
        return val;
    }

    /**
     * Compares two {@code Long} objects numerically.
     *
     * @param   anotherLong   the {@code Long} to be compared.
     * @return  the value {@code 0} if this {@code Long} is
     *          equal to the argument {@code Long}; a value less than
     *          {@code 0} if this {@code Long} is numerically less
     *          than the argument {@code Long}; and a value greater
     *          than {@code 0} if this {@code Long} is numerically
     *           greater than the argument {@code Long} (signed
     *           comparison).
     * @since   1.2
     */
    public int compareTo(Long anotherLong) {
        return compare(this.value, anotherLong.value);
    }

    /**
     * Compares two {@code long} values numerically.
     * The value returned is identical to what would be returned by:
     * <pre>
     *    Long.valueOf(x).compareTo(Long.valueOf(y))
     * </pre>
     *
     * @param  x the first {@code long} to compare
     * @param  y the second {@code long} to compare
     * @return the value {@code 0} if {@code x == y};
     *         a value less than {@code 0} if {@code x < y}; and
     *         a value greater than {@code 0} if {@code x > y}
     * @since 1.7
     */
    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    /**
     * Compares two {@code long} values numerically treating the values
     * as unsigned.
     *
     * @param  x the first {@code long} to compare
     * @param  y the second {@code long} to compare
     * @return the value {@code 0} if {@code x == y}; a value less
     *         than {@code 0} if {@code x < y} as unsigned values; and
     *         a value greater than {@code 0} if {@code x > y} as
     *         unsigned values
     * @since 1.8
     */
    public static int compareUnsigned(long x, long y) {
        return compare(x + MIN_VALUE, y + MIN_VALUE);
    }


    /**
     * Returns the unsigned quotient of dividing the first argument by
     * the second where each argument and the result is interpreted as
     * an unsigned value.
     *
     * <p>Note that in two's complement arithmetic, the three other
     * basic arithmetic operations of add, subtract, and multiply are
     * bit-wise identical if the two operands are regarded as both
     * being signed or both being unsigned.  Therefore separate {@code
     * addUnsigned}, etc. methods are not provided.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned quotient of the first argument divided by
     * the second argument
     * @see #remainderUnsigned
     * @since 1.8
     */
    public static long divideUnsigned(long dividend, long divisor) {
        /* See Hacker's Delight (2nd ed), section 9.3 */
        if (divisor >= 0) {
            final long q = (dividend >>> 1) / divisor << 1;
            final long r = dividend - q * divisor;
            return q + ((r | ~(r - divisor)) >>> (Long.SIZE - 1));
        }
        return (dividend & ~(dividend - divisor)) >>> (Long.SIZE - 1);
    }

    /**
     * Returns the unsigned remainder from dividing the first argument
     * by the second where each argument and the result is interpreted
     * as an unsigned value.
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned remainder of the first argument divided by
     * the second argument
     * @see #divideUnsigned
     * @since 1.8
     */
    public static long remainderUnsigned(long dividend, long divisor) {
        /* See Hacker's Delight (2nd ed), section 9.3 */
        if (divisor >= 0) {
            final long q = (dividend >>> 1) / divisor << 1;
            final long r = dividend - q * divisor;
            /*
             * Here, 0 <= r < 2 * divisor
             * (1) When 0 <= r < divisor, the remainder is simply r.
             * (2) Otherwise the remainder is r - divisor.
             *
             * In case (1), r - divisor < 0. Applying ~ produces a long with
             * sign bit 0, so >> produces 0. The returned value is thus r.
             *
             * In case (2), a similar reasoning shows that >> produces -1,
             * so the returned value is r - divisor.
             */
            return r - ((~(r - divisor) >> (Long.SIZE - 1)) & divisor);
        }
        /*
         * (1) When dividend >= 0, the remainder is dividend.
         * (2) Otherwise
         *      (2.1) When dividend < divisor, the remainder is dividend.
         *      (2.2) Otherwise the remainder is dividend - divisor
         *
         * A reasoning similar to the above shows that the returned value
         * is as expected.
         */
        return dividend - (((dividend & ~(dividend - divisor)) >> (Long.SIZE - 1)) & divisor);
    }

    // Bit Twiddling

    /**
     * The number of bits used to represent a {@code long} value in two's
     * complement binary form.
     *
     * @since 1.5
     */
    @Native public static final int SIZE = 64;

    /**
     * The number of bytes used to represent a {@code long} value in two's
     * complement binary form.
     *
     * @since 1.8
     */
    public static final int BYTES = SIZE / Byte.SIZE;

    /**
     * Returns a {@code long} value with at most a single one-bit, in the
     * position of the highest-order ("leftmost") one-bit in the specified
     * {@code long} value.  Returns zero if the specified value has no
     * one-bits in its two's complement binary representation, that is, if it
     * is equal to zero.
     *
     * @param i the value whose highest one bit is to be computed
     * @return a {@code long} value with a single one-bit, in the position
     *     of the highest-order one-bit in the specified value, or zero if
     *     the specified value is itself equal to zero.
     * @since 1.5
     */
    public static long highestOneBit(long i) {
        return i & (MIN_VALUE >>> numberOfLeadingZeros(i));
    }

    /**
     * Returns a {@code long} value with at most a single one-bit, in the
     * position of the lowest-order ("rightmost") one-bit in the specified
     * {@code long} value.  Returns zero if the specified value has no
     * one-bits in its two's complement binary representation, that is, if it
     * is equal to zero.
     *
     * @param i the value whose lowest one bit is to be computed
     * @return a {@code long} value with a single one-bit, in the position
     *     of the lowest-order one-bit in the specified value, or zero if
     *     the specified value is itself equal to zero.
     * @since 1.5
     */
    public static long lowestOneBit(long i) {
        // HD, Section 2-1
        return i & -i;
    }

    /**
     * Returns the number of zero bits preceding the highest-order
     * ("leftmost") one-bit in the two's complement binary representation
     * of the specified {@code long} value.  Returns 64 if the
     * specified value has no one-bits in its two's complement representation,
     * in other words if it is equal to zero.
     *
     * <p>Note that this method is closely related to the logarithm base 2.
     * For all positive {@code long} values x:
     * <ul>
     * <li>floor(log<sub>2</sub>(x)) = {@code 63 - numberOfLeadingZeros(x)}
     * <li>ceil(log<sub>2</sub>(x)) = {@code 64 - numberOfLeadingZeros(x - 1)}
     * </ul>
     *
     * @param i the value whose number of leading zeros is to be computed
     * @return the number of zero bits preceding the highest-order
     *     ("leftmost") one-bit in the two's complement binary representation
     *     of the specified {@code long} value, or 64 if the value
     *     is equal to zero.
     * @since 1.5
     */
    @IntrinsicCandidate
    public static int numberOfLeadingZeros(long i) {
        int x = (int)(i >>> 32);
        return x == 0 ? 32 + Integer.numberOfLeadingZeros((int)i)
                : Integer.numberOfLeadingZeros(x);
    }

    /**
     * Returns the number of zero bits following the lowest-order ("rightmost")
     * one-bit in the two's complement binary representation of the specified
     * {@code long} value.  Returns 64 if the specified value has no
     * one-bits in its two's complement representation, in other words if it is
     * equal to zero.
     *
     * @param i the value whose number of trailing zeros is to be computed
     * @return the number of zero bits following the lowest-order ("rightmost")
     *     one-bit in the two's complement binary representation of the
     *     specified {@code long} value, or 64 if the value is equal
     *     to zero.
     * @since 1.5
     */
    @IntrinsicCandidate
    public static int numberOfTrailingZeros(long i) {
        int x = (int)i;
        return x == 0 ? 32 + Integer.numberOfTrailingZeros((int)(i >>> 32))
                : Integer.numberOfTrailingZeros(x);
    }

    /**
     * Returns the number of one-bits in the two's complement binary
     * representation of the specified {@code long} value.  This function is
     * sometimes referred to as the <i>population count</i>.
     *
     * @param i the value whose bits are to be counted
     * @return the number of one-bits in the two's complement binary
     *     representation of the specified {@code long} value.
     * @since 1.5
     */
     @IntrinsicCandidate
     public static int bitCount(long i) {
        // HD, Figure 5-2
        i = i - ((i >>> 1) & 0x5555555555555555L);
        i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
        i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        i = i + (i >>> 32);
        return (int)i & 0x7f;
     }

    /**
     * Returns the value obtained by rotating the two's complement binary
     * representation of the specified {@code long} value left by the
     * specified number of bits.  (Bits shifted out of the left hand, or
     * high-order, side reenter on the right, or low-order.)
     *
     * <p>Note that left rotation with a negative distance is equivalent to
     * right rotation: {@code rotateLeft(val, -distance) == rotateRight(val,
     * distance)}.  Note also that rotation by any multiple of 64 is a
     * no-op, so all but the last six bits of the rotation distance can be
     * ignored, even if the distance is negative: {@code rotateLeft(val,
     * distance) == rotateLeft(val, distance & 0x3F)}.
     *
     * @param i the value whose bits are to be rotated left
     * @param distance the number of bit positions to rotate left
     * @return the value obtained by rotating the two's complement binary
     *     representation of the specified {@code long} value left by the
     *     specified number of bits.
     * @since 1.5
     */
    public static long rotateLeft(long i, int distance) {
        return (i << distance) | (i >>> -distance);
    }

    /**
     * Returns the value obtained by rotating the two's complement binary
     * representation of the specified {@code long} value right by the
     * specified number of bits.  (Bits shifted out of the right hand, or
     * low-order, side reenter on the left, or high-order.)
     *
     * <p>Note that right rotation with a negative distance is equivalent to
     * left rotation: {@code rotateRight(val, -distance) == rotateLeft(val,
     * distance)}.  Note also that rotation by any multiple of 64 is a
     * no-op, so all but the last six bits of the rotation distance can be
     * ignored, even if the distance is negative: {@code rotateRight(val,
     * distance) == rotateRight(val, distance & 0x3F)}.
     *
     * @param i the value whose bits are to be rotated right
     * @param distance the number of bit positions to rotate right
     * @return the value obtained by rotating the two's complement binary
     *     representation of the specified {@code long} value right by the
     *     specified number of bits.
     * @since 1.5
     */
    public static long rotateRight(long i, int distance) {
        return (i >>> distance) | (i << -distance);
    }

    /**
     * Returns the value obtained by reversing the order of the bits in the
     * two's complement binary representation of the specified {@code long}
     * value.
     *
     * @param i the value to be reversed
     * @return the value obtained by reversing order of the bits in the
     *     specified {@code long} value.
     * @since 1.5
     */
    public static long reverse(long i) {
        // HD, Figure 7-1
        i = (i & 0x5555555555555555L) << 1 | (i >>> 1) & 0x5555555555555555L;
        i = (i & 0x3333333333333333L) << 2 | (i >>> 2) & 0x3333333333333333L;
        i = (i & 0x0f0f0f0f0f0f0f0fL) << 4 | (i >>> 4) & 0x0f0f0f0f0f0f0f0fL;

        return reverseBytes(i);
    }

    /**
     * Returns the signum function of the specified {@code long} value.  (The
     * return value is -1 if the specified value is negative; 0 if the
     * specified value is zero; and 1 if the specified value is positive.)
     *
     * @param i the value whose signum is to be computed
     * @return the signum function of the specified {@code long} value.
     * @since 1.5
     */
    public static int signum(long i) {
        // HD, Section 2-7
        return (int) ((i >> 63) | (-i >>> 63));
    }

    /**
     * Returns the value obtained by reversing the order of the bytes in the
     * two's complement representation of the specified {@code long} value.
     *
     * @param i the value whose bytes are to be reversed
     * @return the value obtained by reversing the bytes in the specified
     *     {@code long} value.
     * @since 1.5
     */
    @IntrinsicCandidate
    public static long reverseBytes(long i) {
        i = (i & 0x00ff00ff00ff00ffL) << 8 | (i >>> 8) & 0x00ff00ff00ff00ffL;
        return (i << 48) | ((i & 0xffff0000L) << 16) |
            ((i >>> 16) & 0xffff0000L) | (i >>> 48);
    }

    /**
     * Adds two {@code long} values together as per the + operator.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the sum of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     * @since 1.8
     */
    public static long sum(long a, long b) {
        return a + b;
    }

    /**
     * Returns the greater of two {@code long} values
     * as if by calling {@link Math#max(long, long) Math.max}.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the greater of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     * @since 1.8
     */
    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    /**
     * Returns the smaller of two {@code long} values
     * as if by calling {@link Math#min(long, long) Math.min}.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the smaller of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     * @since 1.8
     */
    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    /**
     * Returns an {@link Optional} containing the nominal descriptor for this
     * instance, which is the instance itself.
     *
     * @return an {@link Optional} describing the {@linkplain Long} instance
     * @since 12
     */
    @Override
    public Optional<Long> describeConstable() {
        return Optional.of(this);
    }

    /**
     * Resolves this instance as a {@link ConstantDesc}, the result of which is
     * the instance itself.
     *
     * @param lookup ignored
     * @return the {@linkplain Long} instance
     * @since 12
     */
    @Override
    public Long resolveConstantDesc(MethodHandles.Lookup lookup) {
        return this;
    }

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    @java.io.Serial
    @Native private static final long serialVersionUID = 4290774380558885855L;
}
