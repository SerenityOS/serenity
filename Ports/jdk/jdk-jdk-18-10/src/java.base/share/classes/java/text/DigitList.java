/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import jdk.internal.math.FloatingDecimal;

/**
 * Digit List. Private to DecimalFormat.
 * Handles the transcoding
 * between numeric values and strings of characters.  Only handles
 * non-negative numbers.  The division of labor between DigitList and
 * DecimalFormat is that DigitList handles the radix 10 representation
 * issues; DecimalFormat handles the locale-specific issues such as
 * positive/negative, grouping, decimal point, currency, and so on.
 *
 * A DigitList is really a representation of a floating point value.
 * It may be an integer value; we assume that a double has sufficient
 * precision to represent all digits of a long.
 *
 * The DigitList representation consists of a string of characters,
 * which are the digits radix 10, from '0' to '9'.  It also has a radix
 * 10 exponent associated with it.  The value represented by a DigitList
 * object can be computed by mulitplying the fraction f, where 0 <= f < 1,
 * derived by placing all the digits of the list to the right of the
 * decimal point, by 10^exponent.
 *
 * @see  Locale
 * @see  Format
 * @see  NumberFormat
 * @see  DecimalFormat
 * @see  ChoiceFormat
 * @see  MessageFormat
 * @author       Mark Davis, Alan Liu
 */
final class DigitList implements Cloneable {
    /**
     * The maximum number of significant digits in an IEEE 754 double, that
     * is, in a Java double.  This must not be increased, or garbage digits
     * will be generated, and should not be decreased, or accuracy will be lost.
     */
    public static final int MAX_COUNT = 19; // == Long.toString(Long.MAX_VALUE).length()

    /**
     * These data members are intentionally public and can be set directly.
     *
     * The value represented is given by placing the decimal point before
     * digits[decimalAt].  If decimalAt is < 0, then leading zeros between
     * the decimal point and the first nonzero digit are implied.  If decimalAt
     * is > count, then trailing zeros between the digits[count-1] and the
     * decimal point are implied.
     *
     * Equivalently, the represented value is given by f * 10^decimalAt.  Here
     * f is a value 0.1 <= f < 1 arrived at by placing the digits in Digits to
     * the right of the decimal.
     *
     * DigitList is normalized, so if it is non-zero, figits[0] is non-zero.  We
     * don't allow denormalized numbers because our exponent is effectively of
     * unlimited magnitude.  The count value contains the number of significant
     * digits present in digits[].
     *
     * Zero is represented by any DigitList with count == 0 or with each digits[i]
     * for all i <= count == '0'.
     */
    public int decimalAt = 0;
    public int count = 0;
    public char[] digits = new char[MAX_COUNT];

    private char[] data;
    private RoundingMode roundingMode = RoundingMode.HALF_EVEN;
    private boolean isNegative = false;

    /**
     * Return true if the represented number is zero.
     */
    boolean isZero() {
        for (int i=0; i < count; ++i) {
            if (digits[i] != '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * Set the rounding mode
     */
    void setRoundingMode(RoundingMode r) {
        roundingMode = r;
    }

    /**
     * Clears out the digits.
     * Use before appending them.
     * Typically, you set a series of digits with append, then at the point
     * you hit the decimal point, you set myDigitList.decimalAt = myDigitList.count;
     * then go on appending digits.
     */
    public void clear () {
        decimalAt = 0;
        count = 0;
    }

    /**
     * Appends a digit to the list, extending the list when necessary.
     */
    public void append(char digit) {
        if (count == digits.length) {
            char[] data = new char[count + 100];
            System.arraycopy(digits, 0, data, 0, count);
            digits = data;
        }
        digits[count++] = digit;
    }

    /**
     * Utility routine to get the value of the digit list
     * If (count == 0) this throws a NumberFormatException, which
     * mimics Long.parseLong().
     */
    public final double getDouble() {
        if (count == 0) {
            return 0.0;
        }

        StringBuffer temp = getStringBuffer();
        temp.append('.');
        temp.append(digits, 0, count);
        temp.append('E');
        temp.append(decimalAt);
        return Double.parseDouble(temp.toString());
    }

    /**
     * Utility routine to get the value of the digit list.
     * If (count == 0) this returns 0, unlike Long.parseLong().
     */
    public final long getLong() {
        // for now, simple implementation; later, do proper IEEE native stuff

        if (count == 0) {
            return 0;
        }

        // We have to check for this, because this is the one NEGATIVE value
        // we represent.  If we tried to just pass the digits off to parseLong,
        // we'd get a parse failure.
        if (isLongMIN_VALUE()) {
            return Long.MIN_VALUE;
        }

        StringBuffer temp = getStringBuffer();
        temp.append(digits, 0, count);
        for (int i = count; i < decimalAt; ++i) {
            temp.append('0');
        }
        return Long.parseLong(temp.toString());
    }

    public final BigDecimal getBigDecimal() {
        if (count == 0) {
            if (decimalAt == 0) {
                return BigDecimal.ZERO;
            } else {
                return new BigDecimal("0E" + decimalAt);
            }
        }

       if (decimalAt == count) {
           return new BigDecimal(digits, 0, count);
       } else {
           return new BigDecimal(digits, 0, count).scaleByPowerOfTen(decimalAt - count);
       }
    }

    /**
     * Return true if the number represented by this object can fit into
     * a long.
     * @param isPositive true if this number should be regarded as positive
     * @param ignoreNegativeZero true if -0 should be regarded as identical to
     * +0; otherwise they are considered distinct
     * @return true if this number fits into a Java long
     */
    boolean fitsIntoLong(boolean isPositive, boolean ignoreNegativeZero) {
        // Figure out if the result will fit in a long.  We have to
        // first look for nonzero digits after the decimal point;
        // then check the size.  If the digit count is 18 or less, then
        // the value can definitely be represented as a long.  If it is 19
        // then it may be too large.

        // Trim trailing zeros.  This does not change the represented value.
        while (count > 0 && digits[count - 1] == '0') {
            --count;
        }

        if (count == 0) {
            // Positive zero fits into a long, but negative zero can only
            // be represented as a double. - bug 4162852
            return isPositive || ignoreNegativeZero;
        }

        if (decimalAt < count || decimalAt > MAX_COUNT) {
            return false;
        }

        if (decimalAt < MAX_COUNT) return true;

        // At this point we have decimalAt == count, and count == MAX_COUNT.
        // The number will overflow if it is larger than 9223372036854775807
        // or smaller than -9223372036854775808.
        for (int i=0; i<count; ++i) {
            char dig = digits[i], max = LONG_MIN_REP[i];
            if (dig > max) return false;
            if (dig < max) return true;
        }

        // At this point the first count digits match.  If decimalAt is less
        // than count, then the remaining digits are zero, and we return true.
        if (count < decimalAt) return true;

        // Now we have a representation of Long.MIN_VALUE, without the leading
        // negative sign.  If this represents a positive value, then it does
        // not fit; otherwise it fits.
        return !isPositive;
    }

    /**
     * Set the digit list to a representation of the given double value.
     * This method supports fixed-point notation.
     * @param isNegative Boolean value indicating whether the number is negative.
     * @param source Value to be converted; must not be Inf, -Inf, Nan,
     * or a value <= 0.
     * @param maximumFractionDigits The most fractional digits which should
     * be converted.
     */
    final void set(boolean isNegative, double source, int maximumFractionDigits) {
        set(isNegative, source, maximumFractionDigits, true);
    }

    /**
     * Set the digit list to a representation of the given double value.
     * This method supports both fixed-point and exponential notation.
     * @param isNegative Boolean value indicating whether the number is negative.
     * @param source Value to be converted; must not be Inf, -Inf, Nan,
     * or a value <= 0.
     * @param maximumDigits The most fractional or total digits which should
     * be converted.
     * @param fixedPoint If true, then maximumDigits is the maximum
     * fractional digits to be converted.  If false, total digits.
     */
    final void set(boolean isNegative, double source, int maximumDigits, boolean fixedPoint) {

        FloatingDecimal.BinaryToASCIIConverter fdConverter  = FloatingDecimal.getBinaryToASCIIConverter(source);
        boolean hasBeenRoundedUp = fdConverter.digitsRoundedUp();
        boolean valueExactAsDecimal = fdConverter.decimalDigitsExact();
        assert !fdConverter.isExceptional();
        String digitsString = fdConverter.toJavaFormatString();

        set(isNegative, digitsString,
            hasBeenRoundedUp, valueExactAsDecimal,
            maximumDigits, fixedPoint);
    }

    /**
     * Generate a representation of the form DDDDD, DDDDD.DDDDD, or
     * DDDDDE+/-DDDDD.
     * @param roundedUp whether or not rounding up has already happened.
     * @param valueExactAsDecimal whether or not collected digits provide
     * an exact decimal representation of the value.
     */
    private void set(boolean isNegative, String s,
                     boolean roundedUp, boolean valueExactAsDecimal,
                     int maximumDigits, boolean fixedPoint) {

        this.isNegative = isNegative;
        int len = s.length();
        char[] source = getDataChars(len);
        s.getChars(0, len, source, 0);

        decimalAt = -1;
        count = 0;
        int exponent = 0;
        // Number of zeros between decimal point and first non-zero digit after
        // decimal point, for numbers < 1.
        int leadingZerosAfterDecimal = 0;
        boolean nonZeroDigitSeen = false;

        for (int i = 0; i < len; ) {
            char c = source[i++];
            if (c == '.') {
                decimalAt = count;
            } else if (c == 'e' || c == 'E') {
                exponent = parseInt(source, i, len);
                break;
            } else {
                if (!nonZeroDigitSeen) {
                    nonZeroDigitSeen = (c != '0');
                    if (!nonZeroDigitSeen && decimalAt != -1)
                        ++leadingZerosAfterDecimal;
                }
                if (nonZeroDigitSeen) {
                    digits[count++] = c;
                }
            }
        }
        if (decimalAt == -1) {
            decimalAt = count;
        }
        if (nonZeroDigitSeen) {
            decimalAt += exponent - leadingZerosAfterDecimal;
        }

        if (fixedPoint) {
            // The negative of the exponent represents the number of leading
            // zeros between the decimal and the first non-zero digit, for
            // a value < 0.1 (e.g., for 0.00123, -decimalAt == 2).  If this
            // is more than the maximum fraction digits, then we have an underflow
            // for the printed representation.
            if (-decimalAt > maximumDigits) {
                // Handle an underflow to zero when we round something like
                // 0.0009 to 2 fractional digits.
                count = 0;
                return;
            } else if (-decimalAt == maximumDigits) {
                // If we round 0.0009 to 3 fractional digits, then we have to
                // create a new one digit in the least significant location.
                if (shouldRoundUp(0, roundedUp, valueExactAsDecimal)) {
                    count = 1;
                    ++decimalAt;
                    digits[0] = '1';
                } else {
                    count = 0;
                }
                return;
            }
            // else fall through
        }

        // Eliminate trailing zeros.
        while (count > 1 && digits[count - 1] == '0') {
            --count;
        }

        // Eliminate digits beyond maximum digits to be displayed.
        // Round up if appropriate.
        round(fixedPoint ? (maximumDigits + decimalAt) : maximumDigits,
              roundedUp, valueExactAsDecimal);

     }

    /**
     * Round the representation to the given number of digits.
     * @param maximumDigits The maximum number of digits to be shown.
     * @param alreadyRounded whether or not rounding up has already happened.
     * @param valueExactAsDecimal whether or not collected digits provide
     * an exact decimal representation of the value.
     *
     * Upon return, count will be less than or equal to maximumDigits.
     */
    private final void round(int maximumDigits,
                             boolean alreadyRounded,
                             boolean valueExactAsDecimal) {
        // Eliminate digits beyond maximum digits to be displayed.
        // Round up if appropriate.
        if (maximumDigits >= 0 && maximumDigits < count) {
            if (shouldRoundUp(maximumDigits, alreadyRounded, valueExactAsDecimal)) {
                // Rounding up involved incrementing digits from LSD to MSD.
                // In most cases this is simple, but in a worst case situation
                // (9999..99) we have to adjust the decimalAt value.
                for (;;) {
                    --maximumDigits;
                    if (maximumDigits < 0) {
                        // We have all 9's, so we increment to a single digit
                        // of one and adjust the exponent.
                        digits[0] = '1';
                        ++decimalAt;
                        maximumDigits = 0; // Adjust the count
                        break;
                    }

                    ++digits[maximumDigits];
                    if (digits[maximumDigits] <= '9') break;
                    // digits[maximumDigits] = '0'; // Unnecessary since we'll truncate this
                }
                ++maximumDigits; // Increment for use as count
            }
            count = maximumDigits;

            // Eliminate trailing zeros.
            while (count > 1 && digits[count-1] == '0') {
                --count;
            }
        }
    }


    /**
     * Return true if truncating the representation to the given number
     * of digits will result in an increment to the last digit.  This
     * method implements the rounding modes defined in the
     * java.math.RoundingMode class.
     * [bnf]
     * @param maximumDigits the number of digits to keep, from 0 to
     * {@code count-1}.  If 0, then all digits are rounded away, and
     * this method returns true if a one should be generated (e.g., formatting
     * 0.09 with "#.#").
     * @param alreadyRounded whether or not rounding up has already happened.
     * @param valueExactAsDecimal whether or not collected digits provide
     * an exact decimal representation of the value.
     * @throws    ArithmeticException if rounding is needed with rounding
     *            mode being set to RoundingMode.UNNECESSARY
     * @return true if digit {@code maximumDigits-1} should be
     * incremented
     */
    private boolean shouldRoundUp(int maximumDigits,
                                  boolean alreadyRounded,
                                  boolean valueExactAsDecimal) {
        if (maximumDigits < count) {
            /*
             * To avoid erroneous double-rounding or truncation when converting
             * a binary double value to text, information about the exactness
             * of the conversion result in FloatingDecimal, as well as any
             * rounding done, is needed in this class.
             *
             * - For the  HALF_DOWN, HALF_EVEN, HALF_UP rounding rules below:
             *   In the case of formating float or double, We must take into
             *   account what FloatingDecimal has done in the binary to decimal
             *   conversion.
             *
             *   Considering the tie cases, FloatingDecimal may round up the
             *   value (returning decimal digits equal to tie when it is below),
             *   or "truncate" the value to the tie while value is above it,
             *   or provide the exact decimal digits when the binary value can be
             *   converted exactly to its decimal representation given formating
             *   rules of FloatingDecimal ( we have thus an exact decimal
             *   representation of the binary value).
             *
             *   - If the double binary value was converted exactly as a decimal
             *     value, then DigitList code must apply the expected rounding
             *     rule.
             *
             *   - If FloatingDecimal already rounded up the decimal value,
             *     DigitList should neither round up the value again in any of
             *     the three rounding modes above.
             *
             *   - If FloatingDecimal has truncated the decimal value to
             *     an ending '5' digit, DigitList should round up the value in
             *     all of the three rounding modes above.
             *
             *
             *   This has to be considered only if digit at maximumDigits index
             *   is exactly the last one in the set of digits, otherwise there are
             *   remaining digits after that position and we don't have to consider
             *   what FloatingDecimal did.
             *
             * - Other rounding modes are not impacted by these tie cases.
             *
             * - For other numbers that are always converted to exact digits
             *   (like BigInteger, Long, ...), the passed alreadyRounded boolean
             *   have to be  set to false, and valueExactAsDecimal has to be set to
             *   true in the upper DigitList call stack, providing the right state
             *   for those situations..
             */

            switch(roundingMode) {
            case UP:
                for (int i=maximumDigits; i<count; ++i) {
                    if (digits[i] != '0') {
                        return true;
                    }
                }
                break;
            case DOWN:
                break;
            case CEILING:
                for (int i=maximumDigits; i<count; ++i) {
                    if (digits[i] != '0') {
                        return !isNegative;
                    }
                }
                break;
            case FLOOR:
                for (int i=maximumDigits; i<count; ++i) {
                    if (digits[i] != '0') {
                        return isNegative;
                    }
                }
                break;
            case HALF_UP:
            case HALF_DOWN:
                if (digits[maximumDigits] > '5') {
                    // Value is above tie ==> must round up
                    return true;
                } else if (digits[maximumDigits] == '5') {
                    // Digit at rounding position is a '5'. Tie cases.
                    if (maximumDigits != (count - 1)) {
                        // There are remaining digits. Above tie => must round up
                        return true;
                    } else {
                        // Digit at rounding position is the last one !
                        if (valueExactAsDecimal) {
                            // Exact binary representation. On the tie.
                            // Apply rounding given by roundingMode.
                            return roundingMode == RoundingMode.HALF_UP;
                        } else {
                            // Not an exact binary representation.
                            // Digit sequence either rounded up or truncated.
                            // Round up only if it was truncated.
                            return !alreadyRounded;
                        }
                    }
                }
                // Digit at rounding position is < '5' ==> no round up.
                // Just let do the default, which is no round up (thus break).
                break;
            case HALF_EVEN:
                // Implement IEEE half-even rounding
                if (digits[maximumDigits] > '5') {
                    return true;
                } else if (digits[maximumDigits] == '5' ) {
                    if (maximumDigits == (count - 1)) {
                        // the rounding position is exactly the last index :
                        if (alreadyRounded)
                            // If FloatingDecimal rounded up (value was below tie),
                            // then we should not round up again.
                            return false;

                        if (!valueExactAsDecimal)
                            // Otherwise if the digits don't represent exact value,
                            // value was above tie and FloatingDecimal truncated
                            // digits to tie. We must round up.
                            return true;
                        else {
                            // This is an exact tie value, and FloatingDecimal
                            // provided all of the exact digits. We thus apply
                            // HALF_EVEN rounding rule.
                            return ((maximumDigits > 0) &&
                                    (digits[maximumDigits-1] % 2 != 0));
                        }
                    } else {
                        // Rounds up if it gives a non null digit after '5'
                        for (int i=maximumDigits+1; i<count; ++i) {
                            if (digits[i] != '0')
                                return true;
                        }
                    }
                }
                break;
            case UNNECESSARY:
                for (int i=maximumDigits; i<count; ++i) {
                    if (digits[i] != '0') {
                        throw new ArithmeticException(
                            "Rounding needed with the rounding mode being set to RoundingMode.UNNECESSARY");
                    }
                }
                break;
            default:
                assert false;
            }
        }
        return false;
    }

    /**
     * Utility routine to set the value of the digit list from a long
     */
    final void set(boolean isNegative, long source) {
        set(isNegative, source, 0);
    }

    /**
     * Set the digit list to a representation of the given long value.
     * @param isNegative Boolean value indicating whether the number is negative.
     * @param source Value to be converted; must be >= 0 or ==
     * Long.MIN_VALUE.
     * @param maximumDigits The most digits which should be converted.
     * If maximumDigits is lower than the number of significant digits
     * in source, the representation will be rounded.  Ignored if <= 0.
     */
    final void set(boolean isNegative, long source, int maximumDigits) {
        this.isNegative = isNegative;

        // This method does not expect a negative number. However,
        // "source" can be a Long.MIN_VALUE (-9223372036854775808),
        // if the number being formatted is a Long.MIN_VALUE.  In that
        // case, it will be formatted as -Long.MIN_VALUE, a number
        // which is outside the legal range of a long, but which can
        // be represented by DigitList.
        if (source <= 0) {
            if (source == Long.MIN_VALUE) {
                decimalAt = count = MAX_COUNT;
                System.arraycopy(LONG_MIN_REP, 0, digits, 0, count);
            } else {
                decimalAt = count = 0; // Values <= 0 format as zero
            }
        } else {
            // Rewritten to improve performance.  I used to call
            // Long.toString(), which was about 4x slower than this code.
            int left = MAX_COUNT;
            int right;
            while (source > 0) {
                digits[--left] = (char)('0' + (source % 10));
                source /= 10;
            }
            decimalAt = MAX_COUNT - left;
            // Don't copy trailing zeros.  We are guaranteed that there is at
            // least one non-zero digit, so we don't have to check lower bounds.
            for (right = MAX_COUNT - 1; digits[right] == '0'; --right)
                ;
            count = right - left + 1;
            System.arraycopy(digits, left, digits, 0, count);
        }
        if (maximumDigits > 0) round(maximumDigits, false, true);
    }

    /**
     * Set the digit list to a representation of the given BigDecimal value.
     * This method supports both fixed-point and exponential notation.
     * @param isNegative Boolean value indicating whether the number is negative.
     * @param source Value to be converted; must not be a value <= 0.
     * @param maximumDigits The most fractional or total digits which should
     * be converted.
     * @param fixedPoint If true, then maximumDigits is the maximum
     * fractional digits to be converted.  If false, total digits.
     */
    final void set(boolean isNegative, BigDecimal source, int maximumDigits, boolean fixedPoint) {
        String s = source.toString();
        extendDigits(s.length());

        set(isNegative, s,
            false, true,
            maximumDigits, fixedPoint);
    }

    /**
     * Set the digit list to a representation of the given BigInteger value.
     * @param isNegative Boolean value indicating whether the number is negative.
     * @param source Value to be converted; must be >= 0.
     * @param maximumDigits The most digits which should be converted.
     * If maximumDigits is lower than the number of significant digits
     * in source, the representation will be rounded.  Ignored if <= 0.
     */
    final void set(boolean isNegative, BigInteger source, int maximumDigits) {
        this.isNegative = isNegative;
        String s = source.toString();
        int len = s.length();
        extendDigits(len);
        s.getChars(0, len, digits, 0);

        decimalAt = len;
        int right;
        for (right = len - 1; right >= 0 && digits[right] == '0'; --right)
            ;
        count = right + 1;

        if (maximumDigits > 0) {
            round(maximumDigits, false, true);
        }
    }

    /**
     * equality test between two digit lists.
     */
    public boolean equals(Object obj) {
        if (this == obj)                      // quick check
            return true;
        if (!(obj instanceof DigitList other))         // (1) same object?
            return false;
        if (count != other.count ||
        decimalAt != other.decimalAt)
            return false;
        for (int i = 0; i < count; i++)
            if (digits[i] != other.digits[i])
                return false;
        return true;
    }

    /**
     * Generates the hash code for the digit list.
     */
    public int hashCode() {
        int hashcode = decimalAt;

        for (int i = 0; i < count; i++) {
            hashcode = hashcode * 37 + digits[i];
        }

        return hashcode;
    }

    /**
     * Creates a copy of this object.
     * @return a clone of this instance.
     */
    public Object clone() {
        try {
            DigitList other = (DigitList) super.clone();
            char[] newDigits = new char[digits.length];
            System.arraycopy(digits, 0, newDigits, 0, digits.length);
            other.digits = newDigits;
            other.tempBuffer = null;
            return other;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Returns true if this DigitList represents Long.MIN_VALUE;
     * false, otherwise.  This is required so that getLong() works.
     */
    private boolean isLongMIN_VALUE() {
        if (decimalAt != count || count != MAX_COUNT) {
            return false;
        }

        for (int i = 0; i < count; ++i) {
            if (digits[i] != LONG_MIN_REP[i]) return false;
        }

        return true;
    }

    private static final int parseInt(char[] str, int offset, int strLen) {
        char c;
        boolean positive = true;
        if ((c = str[offset]) == '-') {
            positive = false;
            offset++;
        } else if (c == '+') {
            offset++;
        }

        int value = 0;
        while (offset < strLen) {
            c = str[offset++];
            if (c >= '0' && c <= '9') {
                value = value * 10 + (c - '0');
            } else {
                break;
            }
        }
        return positive ? value : -value;
    }

    // The digit part of -9223372036854775808L
    private static final char[] LONG_MIN_REP = "9223372036854775808".toCharArray();

    public String toString() {
        if (isZero()) {
            return "0";
        }
        StringBuffer buf = getStringBuffer();
        buf.append("0.");
        buf.append(digits, 0, count);
        buf.append("x10^");
        buf.append(decimalAt);
        return buf.toString();
    }

    private StringBuffer tempBuffer;

    private StringBuffer getStringBuffer() {
        if (tempBuffer == null) {
            tempBuffer = new StringBuffer(MAX_COUNT);
        } else {
            tempBuffer.setLength(0);
        }
        return tempBuffer;
    }

    private void extendDigits(int len) {
        if (len > digits.length) {
            digits = new char[len];
        }
    }

    private final char[] getDataChars(int length) {
        if (data == null || data.length < length) {
            data = new char[length];
        }
        return data;
    }
}
