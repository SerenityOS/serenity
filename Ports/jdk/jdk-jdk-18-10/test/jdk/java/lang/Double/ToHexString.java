/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4826774 4926547
 * @summary Tests for {Float, Double}.toHexString methods
 * @library ../Math
 * @build DoubleConsts
 * @run main ToHexString
 * @author Joseph D. Darcy
 */

import java.util.regex.*;

public class ToHexString {
    private ToHexString() {}

    /*
     * Given a double value, create a hexadecimal floating-point
     * string via an intermediate long hex string.
     */
    static String doubleToHexString(double d) {
        return hexLongStringtoHexDoubleString(Long.toHexString(Double.doubleToLongBits(d)));
    }

    /*
     * Transform the hexadecimal long output into the equivalent
     * hexadecimal double value.
     */
    static String hexLongStringtoHexDoubleString(String transString) {
        transString = transString.toLowerCase();

        String zeros = "";
        StringBuffer result = new StringBuffer(24);

        for(int i = 0; i < (16 - transString.length()); i++, zeros += "0");
        transString = zeros + transString;

        // assert transString.length == 16;

            char topChar;
            // Extract sign
            if((topChar=transString.charAt(0)) >= '8' ) {// 8, 9, a, A, b, B, ...
                result.append("-");
                // clear sign bit
                transString =
                    Character.toString(Character.forDigit(Character.digit(topChar, 16) - 8, 16)) +
                    transString.substring(1,16);
            }

            // check for NaN and infinity
            String signifString = transString.substring(3,16);

            if( transString.substring(0,3).equals("7ff") ) {
                if(signifString.equals("0000000000000")) {
                    result.append("Infinity");
                }
                else
                    result.append("NaN");
            }
            else { // finite value
                // Extract exponent
                int exponent = Integer.parseInt(transString.substring(0,3), 16) -
                    DoubleConsts.EXP_BIAS;
                result.append("0x");

                if (exponent == Double.MIN_EXPONENT - 1) { // zero or subnormal
                    if(signifString.equals("0000000000000")) {
                        result.append("0.0p0");
                    }
                    else {
                        result.append("0." + signifString.replaceFirst("0+$", "").replaceFirst("^$", "0") +
                                      "p-1022");
                    }
                }
                else {  // normal value
                    result.append("1." + signifString.replaceFirst("0+$", "").replaceFirst("^$", "0") +
                                  "p" + exponent);
                }
            }
            return result.toString();
    }

    public static int toHexStringTests() {
        int failures = 0;
        String [][] testCases1 = {
            {"Infinity",                "Infinity"},
            {"-Infinity",               "-Infinity"},
            {"NaN",                     "NaN"},
            {"-NaN",                    "NaN"},
            {"0.0",                     "0x0.0p0"},
            {"-0.0",                    "-0x0.0p0"},
            {"1.0",                     "0x1.0p0"},
            {"-1.0",                    "-0x1.0p0"},
            {"2.0",                     "0x1.0p1"},
            {"3.0",                     "0x1.8p1"},
            {"0.5",                     "0x1.0p-1"},
            {"0.25",                    "0x1.0p-2"},
            {"1.7976931348623157e+308", "0x1.fffffffffffffp1023"},      // MAX_VALUE
            {"2.2250738585072014E-308", "0x1.0p-1022"},                 // MIN_NORMAL
            {"2.225073858507201E-308",  "0x0.fffffffffffffp-1022"},     // MAX_SUBNORMAL
            {"4.9e-324",                "0x0.0000000000001p-1022"}      // MIN_VALUE
        };

        // Compare decimal string -> double -> hex string to hex string
        for (int i = 0; i < testCases1.length; i++) {
            String result;
            if(! (result=Double.toHexString(Double.parseDouble(testCases1[i][0]))).
               equals(testCases1[i][1])) {
                failures ++;
                System.err.println("For floating-point string " + testCases1[i][0] +
                                   ", expected hex output " + testCases1[i][1] + ", got " + result +".");
            }
        }


        // Except for float subnormals, the output for numerically
        // equal float and double values should be the same.
        // Therefore, we will explicitly test float subnormal values.
        String [][] floatTestCases = {
            {"Infinity",                "Infinity"},
            {"-Infinity",               "-Infinity"},
            {"NaN",                     "NaN"},
            {"-NaN",                    "NaN"},
            {"0.0",                     "0x0.0p0"},
            {"-0.0",                    "-0x0.0p0"},
            {"1.0",                     "0x1.0p0"},
            {"-1.0",                    "-0x1.0p0"},
            {"2.0",                     "0x1.0p1"},
            {"3.0",                     "0x1.8p1"},
            {"0.5",                     "0x1.0p-1"},
            {"0.25",                    "0x1.0p-2"},
            {"3.4028235e+38f",          "0x1.fffffep127"},      // MAX_VALUE
            {"1.17549435E-38f",         "0x1.0p-126"},          // MIN_NORMAL
            {"1.1754942E-38",           "0x0.fffffep-126"},     // MAX_SUBNORMAL
            {"1.4e-45f",                "0x0.000002p-126"}      // MIN_VALUE
        };
        // Compare decimal string -> double -> hex string to hex string
        for (int i = 0; i < floatTestCases.length; i++) {
            String result;
            if(! (result=Float.toHexString(Float.parseFloat(floatTestCases[i][0]))).
               equals(floatTestCases[i][1])) {
                failures++;
                System.err.println("For floating-point string " + floatTestCases[i][0] +
                                   ", expected hex output\n" + floatTestCases[i][1] + ", got\n" + result +".");
            }
        }

        // Particular floating-point values and hex equivalents, mostly
        // taken from fdlibm source.
        String [][] testCases2 = {
            {"+0.0",                                    "0000000000000000"},
            {"-0.0",                                    "8000000000000000"},
            {"+4.9e-324",                               "0000000000000001"},
            {"-4.9e-324",                               "8000000000000001"},

            // fdlibm k_sin.c
            {"+5.00000000000000000000e-01",             "3FE0000000000000"},
            {"-1.66666666666666324348e-01",             "BFC5555555555549"},
            {"+8.33333333332248946124e-03",             "3F8111111110F8A6"},
            {"-1.98412698298579493134e-04",             "BF2A01A019C161D5"},
            {"+2.75573137070700676789e-06",             "3EC71DE357B1FE7D"},
            {"-2.50507602534068634195e-08",             "BE5AE5E68A2B9CEB"},
            {"+1.58969099521155010221e-10",             "3DE5D93A5ACFD57C"},

            // fdlibm k_cos.c
            {"+4.16666666666666019037e-02",             "3FA555555555554C"},
            {"-1.38888888888741095749e-03",             "BF56C16C16C15177"},
            {"+2.48015872894767294178e-05",             "3EFA01A019CB1590"},
            {"-2.75573143513906633035e-07",             "BE927E4F809C52AD"},
            {"+2.08757232129817482790e-09",             "3E21EE9EBDB4B1C4"},
            {"-1.13596475577881948265e-11",             "BDA8FAE9BE8838D4"},

            // fdlibm e_rempio.c
            {"1.67772160000000000000e+07",              "4170000000000000"},
            {"6.36619772367581382433e-01",              "3FE45F306DC9C883"},
            {"1.57079632673412561417e+00",              "3FF921FB54400000"},
            {"6.07710050650619224932e-11",              "3DD0B4611A626331"},
            {"6.07710050630396597660e-11",              "3DD0B4611A600000"},
            {"2.02226624879595063154e-21",              "3BA3198A2E037073"},
            {"2.02226624871116645580e-21",              "3BA3198A2E000000"},
            {"8.47842766036889956997e-32",              "397B839A252049C1"},


            // fdlibm s_cbrt.c
            {"+5.42857142857142815906e-01",             "3FE15F15F15F15F1"},
            {"-7.05306122448979611050e-01",             "BFE691DE2532C834"},
            {"+1.41428571428571436819e+00",             "3FF6A0EA0EA0EA0F"},
            {"+1.60714285714285720630e+00",             "3FF9B6DB6DB6DB6E"},
            {"+3.57142857142857150787e-01",             "3FD6DB6DB6DB6DB7"},
        };

        // Compare decimal string -> double -> hex string to
        // long hex string -> double hex string
        for (int i = 0; i < testCases2.length; i++) {
            String result;
            String expected;
            if(! (result=Double.toHexString(Double.parseDouble(testCases2[i][0]))).
               equals( expected=hexLongStringtoHexDoubleString(testCases2[i][1]) )) {
                failures ++;
                System.err.println("For floating-point string " + testCases2[i][0] +
                                   ", expected hex output " + expected + ", got " + result +".");
            }
        }

        // Test random double values;
        // compare double -> Double.toHexString with local doubleToHexString
        java.util.Random rand = new java.util.Random(0);
        for (int i = 0; i < 1000; i++) {
            String result;
            String expected;
            double d = rand.nextDouble();
            if(! (expected=doubleToHexString(d)).equals(result=Double.toHexString(d)) ) {
                failures ++;
                System.err.println("For floating-point value " + d +
                                   ", expected hex output " + expected + ", got " + result +".");
            }
        }

        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures = toHexStringTests();

        if (failures != 0) {
            throw new RuntimeException("" + failures + " failures while testing Double.toHexString");
        }
    }
}
