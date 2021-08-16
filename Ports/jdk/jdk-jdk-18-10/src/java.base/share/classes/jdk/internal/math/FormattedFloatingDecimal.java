/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.math;

import java.util.Arrays;

public class FormattedFloatingDecimal{

    public enum Form { SCIENTIFIC, COMPATIBLE, DECIMAL_FLOAT, GENERAL };


    public static FormattedFloatingDecimal valueOf(double d, int precision, Form form){
        FloatingDecimal.BinaryToASCIIConverter fdConverter =
                FloatingDecimal.getBinaryToASCIIConverter(d, form == Form.COMPATIBLE);
        return new FormattedFloatingDecimal(precision,form, fdConverter);
    }

    private int decExponentRounded;
    private char[] mantissa;
    private char[] exponent;

    private static final ThreadLocal<Object> threadLocalCharBuffer =
            new ThreadLocal<Object>() {
                @Override
                protected Object initialValue() {
                    return new char[20];
                }
            };

    private static char[] getBuffer(){
        return (char[]) threadLocalCharBuffer.get();
    }

    private FormattedFloatingDecimal(int precision, Form form, FloatingDecimal.BinaryToASCIIConverter fdConverter) {
        if (fdConverter.isExceptional()) {
            this.mantissa = fdConverter.toJavaFormatString().toCharArray();
            this.exponent = null;
            return;
        }
        char[] digits = getBuffer();
        int nDigits = fdConverter.getDigits(digits);
        int decExp = fdConverter.getDecimalExponent();
        int exp;
        boolean isNegative = fdConverter.isNegative();
        switch (form) {
            case COMPATIBLE:
                exp = decExp;
                this.decExponentRounded = exp;
                fillCompatible(precision, digits, nDigits, exp, isNegative);
                break;
            case DECIMAL_FLOAT:
                exp = applyPrecision(decExp, digits, nDigits, decExp + precision);
                fillDecimal(precision, digits, nDigits, exp, isNegative);
                this.decExponentRounded = exp;
                break;
            case SCIENTIFIC:
                exp = applyPrecision(decExp, digits, nDigits, precision + 1);
                fillScientific(precision, digits, nDigits, exp, isNegative);
                this.decExponentRounded = exp;
                break;
            case GENERAL:
                exp = applyPrecision(decExp, digits, nDigits, precision);
                // adjust precision to be the number of digits to right of decimal
                // the real exponent to be output is actually exp - 1, not exp
                if (exp - 1 < -4 || exp - 1 >= precision) {
                    // form = Form.SCIENTIFIC;
                    precision--;
                    fillScientific(precision, digits, nDigits, exp, isNegative);
                } else {
                    // form = Form.DECIMAL_FLOAT;
                    precision = precision - exp;
                    fillDecimal(precision, digits, nDigits, exp, isNegative);
                }
                this.decExponentRounded = exp;
                break;
            default:
                assert false;
        }
    }

    // returns the exponent after rounding has been done by applyPrecision
    public int getExponentRounded() {
        return decExponentRounded - 1;
    }

    /**
     * Returns the mantissa as a {@code char[]}.  Note that the returned value
     * is a reference to the internal {@code char[]} containing the mantissa,
     * therefore code invoking this method should not pass the return value to
     * external code but should in that case make a copy.
     *
     * @return a reference to the internal {@code char[]} representing the
     *         mantissa.
     */
    public char[] getMantissa(){
        return mantissa;
    }

    /**
     * Returns the exponent as a {@code char[]}.  Note that the returned value
     * is a reference to the internal {@code char[]} containing the exponent,
     * therefore code invoking this method should not pass the return value to
     * external code but should in that case make a copy.
     *
     * @return a reference to the internal {@code char[]} representing the
     *         exponent.
     */
    public char[] getExponent(){
        return exponent;
    }

    /**
     * Returns new decExp in case of overflow.
     */
    private static int applyPrecision(int decExp, char[] digits, int nDigits, int prec) {
        if (prec >= nDigits || prec < 0) {
            // no rounding necessary
            return decExp;
        }
        if (prec == 0) {
            // only one digit (0 or 1) is returned because the precision
            // excludes all significant digits
            if (digits[0] >= '5') {
                digits[0] = '1';
                Arrays.fill(digits, 1, nDigits, '0');
                return decExp + 1;
            } else {
                Arrays.fill(digits, 0, nDigits, '0');
                return decExp;
            }
        }
        int q = digits[prec];
        if (q >= '5') {
            int i = prec;
            q = digits[--i];
            if ( q == '9' ) {
                while ( q == '9' && i > 0 ){
                    q = digits[--i];
                }
                if ( q == '9' ){
                    // carryout! High-order 1, rest 0s, larger exp.
                    digits[0] = '1';
                    Arrays.fill(digits, 1, nDigits, '0');
                    return decExp+1;
                }
            }
            digits[i] = (char)(q + 1);
            Arrays.fill(digits, i+1, nDigits, '0');
        } else {
            Arrays.fill(digits, prec, nDigits, '0');
        }
        return decExp;
    }

    /**
     * Fills mantissa and exponent char arrays for compatible format.
     */
    private void fillCompatible(int precision, char[] digits, int nDigits, int exp, boolean isNegative) {
        int startIndex = isNegative ? 1 : 0;
        if (exp > 0 && exp < 8) {
            // print digits.digits.
            if (nDigits < exp) {
                int extraZeros = exp - nDigits;
                mantissa = create(isNegative, nDigits + extraZeros + 2);
                System.arraycopy(digits, 0, mantissa, startIndex, nDigits);
                Arrays.fill(mantissa, startIndex + nDigits, startIndex + nDigits + extraZeros, '0');
                mantissa[startIndex + nDigits + extraZeros] = '.';
                mantissa[startIndex + nDigits + extraZeros+1] = '0';
            } else if (exp < nDigits) {
                int t = Math.min(nDigits - exp, precision);
                mantissa = create(isNegative, exp + 1 + t);
                System.arraycopy(digits, 0, mantissa, startIndex, exp);
                mantissa[startIndex + exp ] = '.';
                System.arraycopy(digits, exp, mantissa, startIndex+exp+1, t);
            } else { // exp == digits.length
                mantissa = create(isNegative, nDigits + 2);
                System.arraycopy(digits, 0, mantissa, startIndex, nDigits);
                mantissa[startIndex + nDigits ] = '.';
                mantissa[startIndex + nDigits +1] = '0';
            }
        } else if (exp <= 0 && exp > -3) {
            int zeros = Math.max(0, Math.min(-exp, precision));
            int t = Math.max(0, Math.min(nDigits, precision + exp));
            // write '0' s before the significant digits
            if (zeros > 0) {
                mantissa = create(isNegative, zeros + 2 + t);
                mantissa[startIndex] = '0';
                mantissa[startIndex+1] = '.';
                Arrays.fill(mantissa, startIndex + 2, startIndex + 2 + zeros, '0');
                if (t > 0) {
                    // copy only when significant digits are within the precision
                    System.arraycopy(digits, 0, mantissa, startIndex + 2 + zeros, t);
                }
            } else if (t > 0) {
                mantissa = create(isNegative, zeros + 2 + t);
                mantissa[startIndex] = '0';
                mantissa[startIndex + 1] = '.';
                // copy only when significant digits are within the precision
                System.arraycopy(digits, 0, mantissa, startIndex + 2, t);
            } else {
                this.mantissa = create(isNegative, 1);
                this.mantissa[startIndex] = '0';
            }
        } else {
            if (nDigits > 1) {
                mantissa = create(isNegative, nDigits + 1);
                mantissa[startIndex] = digits[0];
                mantissa[startIndex + 1] = '.';
                System.arraycopy(digits, 1, mantissa, startIndex + 2, nDigits - 1);
            } else {
                mantissa = create(isNegative, 3);
                mantissa[startIndex] = digits[0];
                mantissa[startIndex + 1] = '.';
                mantissa[startIndex + 2] = '0';
            }
            int e, expStartIntex;
            boolean isNegExp = (exp <= 0);
            if (isNegExp) {
                e = -exp + 1;
                expStartIntex = 1;
            } else {
                e = exp - 1;
                expStartIntex = 0;
            }
            // decExponent has 1, 2, or 3, digits
            if (e <= 9) {
                exponent = create(isNegExp,1);
                exponent[expStartIntex] = (char) (e + '0');
            } else if (e <= 99) {
                exponent = create(isNegExp,2);
                exponent[expStartIntex] = (char) (e / 10 + '0');
                exponent[expStartIntex+1] = (char) (e % 10 + '0');
            } else {
                exponent = create(isNegExp,3);
                exponent[expStartIntex] = (char) (e / 100 + '0');
                e %= 100;
                exponent[expStartIntex+1] = (char) (e / 10 + '0');
                exponent[expStartIntex+2] = (char) (e % 10 + '0');
            }
        }
    }

    private static char[] create(boolean isNegative, int size) {
        if(isNegative) {
            char[] r = new char[size +1];
            r[0] = '-';
            return r;
        } else {
            return new char[size];
        }
    }

    /*
     * Fills mantissa char arrays for DECIMAL_FLOAT format.
     * Exponent should be equal to null.
     */
    private void fillDecimal(int precision, char[] digits, int nDigits, int exp, boolean isNegative) {
        int startIndex = isNegative ? 1 : 0;
        if (exp > 0) {
            // print digits.digits.
            if (nDigits < exp) {
                mantissa = create(isNegative,exp);
                System.arraycopy(digits, 0, mantissa, startIndex, nDigits);
                Arrays.fill(mantissa, startIndex + nDigits, startIndex + exp, '0');
                // Do not append ".0" for formatted floats since the user
                // may request that it be omitted. It is added as necessary
                // by the Formatter.
            } else {
                int t = Math.min(nDigits - exp, precision);
                mantissa = create(isNegative, exp + (t > 0 ? (t + 1) : 0));
                System.arraycopy(digits, 0, mantissa, startIndex, exp);
                // Do not append ".0" for formatted floats since the user
                // may request that it be omitted. It is added as necessary
                // by the Formatter.
                if (t > 0) {
                    mantissa[startIndex + exp] = '.';
                    System.arraycopy(digits, exp, mantissa, startIndex + exp + 1, t);
                }
            }
        } else if (exp <= 0) {
            int zeros = Math.max(0, Math.min(-exp, precision));
            int t = Math.max(0, Math.min(nDigits, precision + exp));
            // write '0' s before the significant digits
            if (zeros > 0) {
                mantissa = create(isNegative, zeros + 2 + t);
                mantissa[startIndex] = '0';
                mantissa[startIndex+1] = '.';
                Arrays.fill(mantissa, startIndex + 2, startIndex + 2 + zeros, '0');
                if (t > 0) {
                    // copy only when significant digits are within the precision
                    System.arraycopy(digits, 0, mantissa, startIndex + 2 + zeros, t);
                }
            } else if (t > 0) {
                mantissa = create(isNegative, zeros + 2 + t);
                mantissa[startIndex] = '0';
                mantissa[startIndex + 1] = '.';
                // copy only when significant digits are within the precision
                System.arraycopy(digits, 0, mantissa, startIndex + 2, t);
            } else {
                this.mantissa = create(isNegative, 1);
                this.mantissa[startIndex] = '0';
            }
        }
    }

    /**
     * Fills mantissa and exponent char arrays for SCIENTIFIC format.
     */
    private void fillScientific(int precision, char[] digits, int nDigits, int exp, boolean isNegative) {
        int startIndex = isNegative ? 1 : 0;
        int t = Math.max(0, Math.min(nDigits - 1, precision));
        if (t > 0) {
            mantissa = create(isNegative, t + 2);
            mantissa[startIndex] = digits[0];
            mantissa[startIndex + 1] = '.';
            System.arraycopy(digits, 1, mantissa, startIndex + 2, t);
        } else {
            mantissa = create(isNegative, 1);
            mantissa[startIndex] = digits[0];
        }
        char expSign;
        int e;
        if (exp <= 0) {
            expSign = '-';
            e = -exp + 1;
        } else {
            expSign = '+' ;
            e = exp - 1;
        }
        // decExponent has 1, 2, or 3, digits
        if (e <= 9) {
            exponent = new char[] { expSign,
                    '0', (char) (e + '0') };
        } else if (e <= 99) {
            exponent = new char[] { expSign,
                    (char) (e / 10 + '0'), (char) (e % 10 + '0') };
        } else {
            char hiExpChar = (char) (e / 100 + '0');
            e %= 100;
            exponent = new char[] { expSign,
                    hiExpChar, (char) (e / 10 + '0'), (char) (e % 10 + '0') };
        }
    }
}
