/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4160406 4705734 4707389 4826774 4895911 4421494 6358355 7021568 7039369 4396272
 * @summary Test for Double.parseDouble method and acceptance regex
 */

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.*;

public class ParseDouble {

    private static final BigDecimal HALF = BigDecimal.valueOf(0.5);

    private static void fail(String val, double n) {
        throw new RuntimeException("Double.parseDouble failed. String:" +
                                                val + " Result:" + n);
    }

    private static void check(String val) {
        double n = Double.parseDouble(val);
        boolean isNegativeN = n < 0 || n == 0 && 1/n < 0;
        double na = Math.abs(n);
        String s = val.trim().toLowerCase();
        switch (s.charAt(s.length() - 1)) {
            case 'd':
            case 'f':
                s = s.substring(0, s.length() - 1);
                break;
        }
        boolean isNegative = false;
        if (s.charAt(0) == '+') {
            s = s.substring(1);
        } else if (s.charAt(0) == '-') {
            s = s.substring(1);
            isNegative = true;
        }
        if (s.equals("nan")) {
            if (!Double.isNaN(n)) {
                fail(val, n);
            }
            return;
        }
        if (Double.isNaN(n)) {
            fail(val, n);
        }
        if (isNegativeN != isNegative)
            fail(val, n);
        if (s.equals("infinity")) {
            if (na != Double.POSITIVE_INFINITY) {
                fail(val, n);
            }
            return;
        }
        BigDecimal bd;
        if (s.startsWith("0x")) {
            s = s.substring(2);
            int indP = s.indexOf('p');
            long exp = Long.parseLong(s.substring(indP + 1));
            int indD = s.indexOf('.');
            String significand;
            if (indD >= 0) {
                significand = s.substring(0, indD) + s.substring(indD + 1, indP);
                exp -= 4*(indP - indD - 1);
            } else {
                significand = s.substring(0, indP);
            }
            bd = new BigDecimal(new BigInteger(significand, 16));
            if (exp >= 0) {
                bd = bd.multiply(BigDecimal.valueOf(2).pow((int)exp));
            } else {
                bd = bd.divide(BigDecimal.valueOf(2).pow((int)-exp));
            }
        } else {
            bd = new BigDecimal(s);
        }
        BigDecimal l, u;
        if (Double.isInfinite(na)) {
            l = new BigDecimal(Double.MAX_VALUE).add(new BigDecimal(Math.ulp(Double.MAX_VALUE)).multiply(HALF));
            u = null;
        } else {
            l = new BigDecimal(na).subtract(new BigDecimal(Math.ulp(Math.nextUp(-na))).multiply(HALF));
            u = new BigDecimal(na).add(new BigDecimal(Math.ulp(n)).multiply(HALF));
        }
        int cmpL = bd.compareTo(l);
        int cmpU = u != null ? bd.compareTo(u) : -1;
        if ((Double.doubleToLongBits(n) & 1) != 0) {
            if (cmpL <= 0 || cmpU >= 0) {
                fail(val, n);
            }
        } else {
            if (cmpL < 0 || cmpU > 0) {
                fail(val, n);
            }
        }
    }

    private static void check(String val, double expected) {
        double n = Double.parseDouble(val);
        if (n != expected)
            fail(val, n);
        check(val);
    }

    private static void rudimentaryTest() {
        check(new String(""+Double.MIN_VALUE), Double.MIN_VALUE);
        check(new String(""+Double.MAX_VALUE), Double.MAX_VALUE);

        check("10",     (double)  10.0);
        check("10.0",   (double)  10.0);
        check("10.01",  (double)  10.01);

        check("-10",    (double) -10.0);
        check("-10.00", (double) -10.0);
        check("-10.01", (double) -10.01);
    }


    static  String badStrings[] = {
        "",
        "+",
        "-",
        "+e",
        "-e",
        "+e170",
        "-e170",

        // Make sure intermediate white space is not deleted.
        "1234   e10",
        "-1234   e10",

        // Control characters in the interior of a string are not legal
        "1\u0007e1",
        "1e\u00071",

        // NaN and infinity can't have trailing type suffices or exponents
        "NaNf",
        "NaNF",
        "NaNd",
        "NaND",
        "-NaNf",
        "-NaNF",
        "-NaNd",
        "-NaND",
        "+NaNf",
        "+NaNF",
        "+NaNd",
        "+NaND",
        "Infinityf",
        "InfinityF",
        "Infinityd",
        "InfinityD",
        "-Infinityf",
        "-InfinityF",
        "-Infinityd",
        "-InfinityD",
        "+Infinityf",
        "+InfinityF",
        "+Infinityd",
        "+InfinityD",

        "NaNe10",
        "-NaNe10",
        "+NaNe10",
        "Infinitye10",
        "-Infinitye10",
        "+Infinitye10",

        // Non-ASCII digits are not recognized
        "\u0661e\u0661", // 1e1 in Arabic-Indic digits
        "\u06F1e\u06F1", // 1e1 in Extended Arabic-Indic digits
        "\u0967e\u0967", // 1e1 in Devanagari digits

        // JCK test lex03592m3
        ".",

        // JCK test lex03592m4
        "e42",

        // JCK test lex03592m5
        ".e42",

        // JCK test lex03592m6
        "d",

        // JCK test lex03592m7
        ".d",

        // JCK test lex03592m8
        "e42d",

        // JCK test lex03592m9
        ".e42d",

        // JCK test lex03593m10
        "1A01.01125e-10d",

        // JCK test lex03593m11
        "2;3.01125e-10d",

        // JCK test lex03593m12
        "1_34.01125e-10d",

        // JCK test lex03593m14
        "202..01125e-10d",

        // JCK test lex03593m15
        "202,01125e-10d",

        // JCK test lex03593m16
        "202.03b4e-10d",

        // JCK test lex03593m18
        "202.06_3e-10d",

        // JCK test lex03593m20
        "202.01125e-f0d",

        // JCK test lex03593m21
        "202.01125e_3d",

        // JCK test lex03593m22
        "202.01125e -5d",

        // JCK test lex03593m24
        "202.01125e-10r",

        // JCK test lex03593m25
        "202.01125e-10ff",

        // JCK test lex03593m26
        "1234L.01",

        // JCK test lex03593m27
        "12ee-2",

        // JCK test lex03593m28
        "12e-2.2.2",

        // JCK test lex03593m29
        "12.01e+",

        // JCK test lex03593m30
        "12.01E",

        // Bad hexadecimal-style strings

        // Two leading zeros
        "00x1.0p1",

        // Must have hex specifier
        "1.0p1",
        "00010p1",
        "deadbeefp1",

        // Need an explicit fully-formed exponent
        "0x1.0p",
        "0x1.0",

        // Exponent must be in decimal
        "0x1.0pa",
        "0x1.0pf",

        // Exponent separated by "p"
        "0x1.0e22",
        "0x1.0e22",

        // Need a signifcand
        "0xp22"
    };

    static String goodStrings[] = {
        "NaN",
        "+NaN",
        "-NaN",
        "Infinity",
        "+Infinity",
        "-Infinity",
        "1.1e-23f",
        ".1e-23f",
        "1e-23",
        "1f",
        "0",
        "-0",
        "+0",
        "00",
        "00",
        "-00",
        "+00",
        "0000000000",
        "-0000000000",
        "+0000000000",
        "1",
        "2",
        "1234",
        "-1234",
        "+1234",
        "2147483647",   // Integer.MAX_VALUE
        "2147483648",
        "-2147483648",  // Integer.MIN_VALUE
        "-2147483649",

        "16777215",
        "16777216",     // 2^24
        "16777217",

        "-16777215",
        "-16777216",    // -2^24
        "-16777217",

        "9007199254740991",
        "9007199254740992",     // 2^53
        "9007199254740993",

        "-9007199254740991",
        "-9007199254740992",    // -2^53
        "-9007199254740993",

        "9223372036854775807",
        "9223372036854775808",  // Long.MAX_VALUE
        "9223372036854775809",

        "-9223372036854775808",
        "-9223372036854775809", // Long.MIN_VALUE
        "-9223372036854775810",

        // Culled from JCK test lex03591m1
        "54.07140d",
        "7.01e-324d",
        "2147483647.01d",
        "1.2147483647f",
        "000000000000000000000000001.F",
        "1.00000000000000000000000000e-2F",

        // Culled from JCK test lex03592m2
        "2.",
        ".0909",
        "122112217090.0",
        "7090e-5",
        "2.E-20",
        ".0909e42",
        "122112217090.0E+100",
        "7090f",
        "2.F",
        ".0909d",
        "122112217090.0D",
        "7090e-5f",
        "2.E-20F",
        ".0909e42d",
        "122112217090.0E+100D",

        // Culled from JCK test lex03594m31 -- unicode escapes
        "\u0035\u0031\u0034\u0039\u0032\u0033\u0036\u0037\u0038\u0030.1102E-209D",
        "1290873\u002E12301e100",
        "1.1E-10\u0066",

        // Culled from JCK test lex03595m1
        "0.0E-10",
        "1E10",

        // Culled from JCK test lex03691m1
        "0.f",
        "1f",
        "0.F",
        "1F",
        "0.12d",
        "1e-0d",
        "12.e+1D",
        "0e-0D",
        "12.e+01",
        "1e-01",

        // Good hex strings
        // Vary capitalization of separators.

        "0x1p1",
        "0X1p1",
        "0x1P1",
        "0X1P1",
        "0x1p1f",
        "0X1p1f",
        "0x1P1f",
        "0X1P1f",
        "0x1p1F",
        "0X1p1F",
        "0x1P1F",
        "0X1P1F",
        "0x1p1d",
        "0X1p1d",
        "0x1P1d",
        "0X1P1d",
        "0x1p1D",
        "0X1p1D",
        "0x1P1D",
        "0X1P1D",

        "-0x1p1",
        "-0X1p1",
        "-0x1P1",
        "-0X1P1",
        "-0x1p1f",
        "-0X1p1f",
        "-0x1P1f",
        "-0X1P1f",
        "-0x1p1F",
        "-0X1p1F",
        "-0x1P1F",
        "-0X1P1F",
        "-0x1p1d",
        "-0X1p1d",
        "-0x1P1d",
        "-0X1P1d",
        "-0x1p1D",
        "-0X1p1D",
        "-0x1P1D",
        "-0X1P1D",

        "0x1p-1",
        "0X1p-1",
        "0x1P-1",
        "0X1P-1",
        "0x1p-1f",
        "0X1p-1f",
        "0x1P-1f",
        "0X1P-1f",
        "0x1p-1F",
        "0X1p-1F",
        "0x1P-1F",
        "0X1P-1F",
        "0x1p-1d",
        "0X1p-1d",
        "0x1P-1d",
        "0X1P-1d",
        "0x1p-1D",
        "0X1p-1D",
        "0x1P-1D",
        "0X1P-1D",

        "-0x1p-1",
        "-0X1p-1",
        "-0x1P-1",
        "-0X1P-1",
        "-0x1p-1f",
        "-0X1p-1f",
        "-0x1P-1f",
        "-0X1P-1f",
        "-0x1p-1F",
        "-0X1p-1F",
        "-0x1P-1F",
        "-0X1P-1F",
        "-0x1p-1d",
        "-0X1p-1d",
        "-0x1P-1d",
        "-0X1P-1d",
        "-0x1p-1D",
        "-0X1p-1D",
        "-0x1P-1D",
        "-0X1P-1D",


        // Try different significand combinations
        "0xap1",
        "0xbp1",
        "0xcp1",
        "0xdp1",
        "0xep1",
        "0xfp1",

        "0x1p1",
        "0x.1p1",
        "0x1.1p1",

        "0x001p23",
        "0x00.1p1",
        "0x001.1p1",

        "0x100p1",
        "0x.100p1",
        "0x1.100p1",

        "0x00100p1",
        "0x00.100p1",
        "0x001.100p1",

        // Limits

        "1.7976931348623157E308",     // Double.MAX_VALUE
        "4.9e-324",                   // Double.MIN_VALUE
        "2.2250738585072014e-308",    // Double.MIN_NORMAL

        "2.2250738585072012e-308",    // near Double.MIN_NORMAL

        "1.7976931348623158e+308",    // near MAX_VALUE + ulp(MAX_VALUE)/2
        "1.7976931348623159e+308",    // near MAX_VALUE + ulp(MAX_VALUE)

        "2.4703282292062329e-324",    // above MIN_VALUE/2
        "2.4703282292062327e-324",    // MIN_VALUE/2
        "2.4703282292062325e-324",    // below MIN_VALUE/2

        // 1e308 with leading zeros

        "0.0000000000001e321",
        "00.000000000000000001e326",
        "00000.000000000000000001e326",
        "000.0000000000000000001e327",
        "0.00000000000000000001e328",
    };

    static String paddedBadStrings[];
    static String paddedGoodStrings[];
    static {
        String pad = " \t\n\r\f\u0001\u000b\u001f";
        paddedBadStrings = new String[badStrings.length];
        for(int i = 0 ; i <  badStrings.length; i++)
            paddedBadStrings[i] = pad + badStrings[i] + pad;

        paddedGoodStrings = new String[goodStrings.length];
        for(int i = 0 ; i <  goodStrings.length; i++)
            paddedGoodStrings[i] = pad + goodStrings[i] + pad;

    }


    /*
     * Throws an exception if <code>Input</code> is
     * <code>exceptionalInput</code> and {@link Double.parseDouble
     * parseDouble} does <em>not</em> throw an exception or if
     * <code>Input</code> is not <code>exceptionalInput</code> and
     * <code>parseDouble</code> throws an exception.  This method does
     * not attempt to test whether the string is converted to the
     * proper value; just whether the input is accepted appropriately
     * or not.
     */
    private static void testParsing(String [] input,
                                    boolean exceptionalInput) {
        for(int i = 0; i < input.length; i++) {
            double d;

            try {
                d = Double.parseDouble(input[i]);
                check(input[i]);
            }
            catch (NumberFormatException e) {
                if (! exceptionalInput) {
                    throw new RuntimeException("Double.parseDouble rejected " +
                                               "good string `" + input[i] +
                                               "'.");
                }
                break;
            }
            if (exceptionalInput) {
                throw new RuntimeException("Double.parseDouble accepted " +
                                           "bad string `" + input[i] +
                                           "'.");
            }
        }
    }

    /*
     * Throws an exception if <code>Input</code> is
     * <code>exceptionalInput</code> and the regular expression
     * matches one of the strings or if <code>Input</code> is not
     * <code>exceptionalInput</code> and the regular expression fails
     * to match an input string.
     */
    private static void testRegex(String [] input, boolean exceptionalInput) {
        /*
         * The regex below is taken from the JavaDoc for
         * Double.valueOf.
         */

        final String Digits     = "(\\p{Digit}+)";
        final String HexDigits  = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp        = "[eE][+-]?"+Digits;
        final String fpRegex    =
            ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
             "[+-]?(" + // Optional sign character
             "NaN|" +           // "NaN" string
             "Infinity|" +      // "Infinity" string

             // A floating-point string representing a finite positive
             // number without a leading sign has at most five basic pieces:
             // Digits . Digits ExponentPart FloatTypeSuffix
             //
             // Since this method allows integer-only strings as input
             // in addition to strings of floating-point literals, the
             // two sub-patterns below are simplifications of the grammar
             // productions from the Java Language Specification, 2nd
             // edition, section 3.10.2.


             // A decimal floating-point string representing a finite positive
             // number without a leading sign has at most five basic pieces:
             // Digits . Digits ExponentPart FloatTypeSuffix
             //
             // Since this method allows integer-only strings as input
             // in addition to strings of floating-point literals, the
             // two sub-patterns below are simplifications of the grammar
             // productions from the Java Language Specification, 2nd
             // edition, section 3.10.2.

             // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
             "(((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

             // . Digits ExponentPart_opt FloatTypeSuffix_opt
             "(\\.("+Digits+")("+Exp+")?))|"+

            // Hexadecimal strings
            "((" +
             // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
             "(0[xX]" + HexDigits + "(\\.)?)|" +

             // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
             "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

             ")[pP][+-]?" + Digits + "))" +
             "[fFdD]?))" +
             "[\\x00-\\x20]*");// Optional trailing "whitespace"
        Pattern fpPattern = Pattern.compile(fpRegex);

        for(int i = 0; i < input.length; i++) {
             Matcher m = fpPattern.matcher(input[i]);
             if (m.matches() != ! exceptionalInput) {
                 throw new RuntimeException("Regular expression " +
                                            (exceptionalInput?
                                             "accepted bad":
                                             "rejected good") +
                                            " string `" +
                                            input[i] + "'.");
             }
        }

    }

    /**
     * For each subnormal power of two, test at boundaries of
     * region that should convert to that value.
     */
    private static void testSubnormalPowers() {
        boolean failed = false;
        BigDecimal TWO = BigDecimal.valueOf(2);
        // An ulp is the same for all subnormal values
        BigDecimal ulp_BD = new BigDecimal(Double.MIN_VALUE);

        // Test subnormal powers of two (except Double.MIN_VALUE)
        for(int i = -1073; i <= -1022; i++) {
            double d = Math.scalb(1.0, i);

            /*
             * The region [d - ulp/2, d + ulp/2] should round to d.
             */
            BigDecimal d_BD = new BigDecimal(d);

            BigDecimal lowerBound = d_BD.subtract(ulp_BD.divide(TWO));
            BigDecimal upperBound = d_BD.add(ulp_BD.divide(TWO));

            double convertedLowerBound = Double.parseDouble(lowerBound.toString());
            double convertedUpperBound = Double.parseDouble(upperBound.toString());
            if (convertedLowerBound != d) {
                failed = true;
                System.out.printf("2^%d lowerBound converts as %a %s%n",
                                  i, convertedLowerBound, lowerBound);
            }
            if (convertedUpperBound != d) {
                failed = true;
                System.out.printf("2^%d upperBound converts as %a %s%n",
                                  i, convertedUpperBound, upperBound);
            }
        }
        /*
         * Double.MIN_VALUE
         * The region ]0.5*Double.MIN_VALUE, 1.5*Double.MIN_VALUE[ should round to Double.MIN_VALUE .
         */
        BigDecimal minValue = new BigDecimal(Double.MIN_VALUE);
        if (Double.parseDouble(minValue.multiply(new BigDecimal(0.5)).toString()) != 0.0) {
            failed = true;
            System.out.printf("0.5*MIN_VALUE doesn't convert 0%n");
        }
        if (Double.parseDouble(minValue.multiply(new BigDecimal(0.50000000001)).toString()) != Double.MIN_VALUE) {
            failed = true;
            System.out.printf("0.50000000001*MIN_VALUE doesn't convert to MIN_VALUE%n");
        }
        if (Double.parseDouble(minValue.multiply(new BigDecimal(1.49999999999)).toString()) != Double.MIN_VALUE) {
            failed = true;
            System.out.printf("1.49999999999*MIN_VALUE doesn't convert to MIN_VALUE%n");
        }
        if (Double.parseDouble(minValue.multiply(new BigDecimal(1.5)).toString()) != 2*Double.MIN_VALUE) {
            failed = true;
            System.out.printf("1.5*MIN_VALUE doesn't convert to 2*MIN_VALUE%n");
        }

        if (failed)
            throw new RuntimeException("Inconsistent conversion");
    }

    /**
     * For each power of two, test at boundaries of
     * region that should convert to that value.
     */
    private static void testPowers() {
        for(int i = -1074; i <= +1023; i++) {
            double d = Math.scalb(1.0, i);
            BigDecimal d_BD = new BigDecimal(d);

            BigDecimal lowerBound = d_BD.subtract(new BigDecimal(Math.ulp(Math.nextUp(-d))).multiply(HALF));
            BigDecimal upperBound = d_BD.add(new BigDecimal(Math.ulp(d)).multiply(HALF));

            check(lowerBound.toString());
            check(upperBound.toString());
        }
        check(new BigDecimal(Double.MAX_VALUE).add(new BigDecimal(Math.ulp(Double.MAX_VALUE)).multiply(HALF)).toString());
    }

    private static void testStrictness() {
        final double expected = 0x0.0000008000000p-1022;
//        final double expected = 0x0.0000008000001p-1022;
        boolean failed = false;
        double conversion = 0.0;
        double sum = 0.0; // Prevent conversion from being optimized away

        //2^-1047 + 2^-1075 rounds to 2^-1047
        String decimal = "6.631236871469758276785396630275967243399099947355303144249971758736286630139265439618068200788048744105960420552601852889715006376325666595539603330361800519107591783233358492337208057849499360899425128640718856616503093444922854759159988160304439909868291973931426625698663157749836252274523485312442358651207051292453083278116143932569727918709786004497872322193856150225415211997283078496319412124640111777216148110752815101775295719811974338451936095907419622417538473679495148632480391435931767981122396703443803335529756003353209830071832230689201383015598792184172909927924176339315507402234836120730914783168400715462440053817592702766213559042115986763819482654128770595766806872783349146967171293949598850675682115696218943412532098591327667236328125E-316";

        for(int i = 0; i <= 12_000; i++) {
            conversion = Double.parseDouble(decimal);
            sum += conversion;
            if (conversion != expected) {
                failed = true;
                System.out.printf("Iteration %d converts as %a%n",
                                  i, conversion);
            }
        }

        System.out.println("Sum = "  + sum);
        if (failed)
            throw new RuntimeException("Inconsistent conversion");
    }

    public static void main(String[] args) throws Exception {
        rudimentaryTest();

        testParsing(goodStrings, false);
        testParsing(paddedGoodStrings, false);
        testParsing(badStrings, true);
        testParsing(paddedBadStrings, true);

        testRegex(goodStrings, false);
        testRegex(paddedGoodStrings, false);
        testRegex(badStrings, true);
        testRegex(paddedBadStrings, true);

        testSubnormalPowers();
        testPowers();
        testStrictness();
    }
}
