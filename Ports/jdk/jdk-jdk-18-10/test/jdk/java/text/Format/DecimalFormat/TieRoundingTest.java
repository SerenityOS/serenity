/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 *
 * @bug 7131459 8039915
 * @summary test various situations of NumberFormat rounding when close to tie
 * @author Olivier Lagneau
 * @run main TieRoundingTest
 *
 */

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.util.Locale;

public class TieRoundingTest {

    static int testCounter = 0;
    static int errorCounter = 0;
    static boolean allPassed = true;

    static void formatOutputTestDouble(NumberFormat nf,
                                       double doubleToTest,
                                       String tiePosition,
                                       String inputDigits,
                                       String expectedOutput) {

        int mfd = nf.getMaximumFractionDigits();
        RoundingMode rm = nf.getRoundingMode();
        String result = nf.format(doubleToTest);

        if (!result.equals(expectedOutput)) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("***Failure : error formatting value from string : " +
                               inputDigits);
            System.out.println("NumberFormat pattern is  : " +
                               ((DecimalFormat ) nf).toPattern());
            System.out.println("Maximum number of fractional digits : " + mfd);
            System.out.println("Fractional rounding digit : " + (mfd + 1));
            System.out.println("Position of value relative to tie : " + tiePosition);
            System.out.println("Rounding Mode : " + rm);
            System.out.println("BigDecimal output : " +
                               new BigDecimal(doubleToTest).toString());
            System.out.println("FloatingDecimal output : " + doubleToTest);
            System.out.println(
               "Error. Formatted result different from expected." +
               "\nExpected output is : \"" + expectedOutput + "\"" +
               "\nFormated output is : \"" + result + "\"");
            System.out.println("========================================");
            System.out.println();

            errorCounter++;
            allPassed = false;
        } else {
            testCounter++;
            System.out.println("\nSuccess for double value : " + doubleToTest + " :");
            System.out.println(" Input digits :" + inputDigits +
                               ", BigDecimal value : " +
                               new BigDecimal(doubleToTest).toString());
            System.out.print(" Rounding mode: " + rm);
            System.out.print(", fract digits : " + mfd);
            System.out.print(", position : " + tiePosition + " tie");
            System.out.print(", result : " + result);
            System.out.println(", expected : " + expectedOutput);
        }
    }

    static void formatOutputTestLong(NumberFormat nf,
                                     long longToTest,
                                     String tiePosition,
                                     String inputDigits,
                                     String expectedOutput) {

        int mfd = nf.getMaximumFractionDigits();
        RoundingMode rm = nf.getRoundingMode();
        String result = nf.format(longToTest);

        if (!result.equals(expectedOutput)) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("***Failure : error formatting value from string : " +
                               inputDigits);
            System.out.println("NumberFormat pattern is  : " +
                               ((DecimalFormat ) nf).toPattern());
            System.out.println("Maximum number of fractional digits : " + mfd);
            System.out.println("Fractional rounding digit : " + (mfd + 1));
            System.out.println("Position of value relative to tie : " + tiePosition);
            System.out.println("Rounding Mode : " + rm);
            System.out.println(
               "Error. Formatted result different from expected." +
               "\nExpected output is : \"" + expectedOutput + "\"" +
               "\nFormated output is : \"" + result + "\"");
            System.out.println("========================================");
            System.out.println();

            errorCounter++;
            allPassed = false;
        } else {
            testCounter++;
            System.out.print("Success. Long input :" + inputDigits);
            System.out.print(", rounding : " + rm);
            System.out.print(", fract digits : " + mfd);
            System.out.print(", tie position : " + tiePosition);
            System.out.println(", expected : " + expectedOutput);

        }
    }

    static void formatOutputTestObject(NumberFormat nf,
                                       Object someNumber,
                                       String tiePosition,
                                       String inputDigits,
                                       String expectedOutput) {

        int mfd = nf.getMaximumFractionDigits();
        RoundingMode rm = nf.getRoundingMode();
        String result = nf.format(someNumber);

        if (!result.equals(expectedOutput)) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("***Failure : error formatting value from string : " +
                               inputDigits);
            System.out.println("NumberFormat pattern is  : " +
                               ((DecimalFormat ) nf).toPattern());
            System.out.println("Maximum number of fractional digits : " + mfd);
            System.out.println("Fractional rounding digit : " + (mfd + 1));
            System.out.println("Position of value relative to tie : " + tiePosition);
            System.out.println("Rounding Mode : " + rm);
            System.out.println("Number self output representation: " + someNumber);
            System.out.println(
               "Error. Formatted result different from expected." +
               "\nExpected output is : \"" + expectedOutput + "\"" +
               "\nFormated output is : \"" + result + "\"");
            System.out.println("========================================");
            System.out.println();

            errorCounter++;
            allPassed = false;
        } else {
            testCounter++;
            System.out.print("Success. Number input :" + inputDigits);
            System.out.print(", rounding : " + rm);
            System.out.print(", fract digits : " + mfd);
            System.out.print(", tie position : " + tiePosition);
            System.out.println(", expected : " + expectedOutput);
        }
    }

    public static void main(String[] args) {

        // The 3 HALF_* rounding modes are impacted by bugs 7131459, 8039915.
        // So we do not test the other rounding modes.
        RoundingMode[] roundingModes = {
            RoundingMode.HALF_DOWN,
            RoundingMode.HALF_EVEN,
            RoundingMode.HALF_UP
        };

        // Precise the relative position of input value against its closest tie.
        // The double values tested below for 3 and 5 fractional digits must follow
        // this scheme (position toward tie).
        String[] tieRelativePositions = {
            "below", "exact", "above",
            "below", "exact", "above",
            "below", "exact", "above",
            "below", "above", "above",
            "below", "below", "above",
            "below", "exact", "above"
        };

        // =============== Testing double (and thus float) value cases =========

        System.out.println("\n===== testing 3 digits rounding position =====");
        double[] values3FractDigits = {
            // unimpacting values close to tie, with less than 3 input fract digits
            1.115d, 1.125d, 1.135d,
            // HALF_* impacting close to tie values covering all 6 tie cases
            0.3115d, 0.3125d, 0.3135d,
            0.6865d, 0.6875d, 0.6885d,
            // specific HALF_UP close to tie values
            0.3124d, 0.3126d, 0.3128d,
            // specific HALF_DOWN close to tie values
            0.6864d, 0.6865d, 0.6868d,
            // unimpacting values close to tie, with more than 3 input fract digits
            1.46885d, 2.46875d, 1.46865d
        };

        String[] inputs3FractDigits = {
            "1.115d", "1.125d", "1.135d",
            "0.3115d", "0.3125d", "0.3135d",
            "0.6865d", "0.6875d", "0.6885d",
            "0.3124d", "0.3126d", "0.3128d",
            "0.6864d", "0.6865d", "0.6868d",
            "1.46885d", "2.46875d", "1.46865d"
        };

        String[][] expected3FractDigits = {
            {"1.115", "1.125", "1.135",
             "0.311", "0.312", "0.314",
             "0.686", "0.687", "0.689",
             "0.312", "0.313", "0.313",
             "0.686", "0.686", "0.687",
             "1.469", "2.469", "1.469"
            },
            {"1.115", "1.125", "1.135",
             "0.311", "0.312", "0.314",
             "0.686", "0.688", "0.689",
             "0.312", "0.313", "0.313",
             "0.686", "0.686", "0.687",
             "1.469", "2.469", "1.469"
            },
            {"1.115", "1.125", "1.135",
             "0.311", "0.313", "0.314",
             "0.686", "0.688", "0.689",
             "0.312", "0.313", "0.313",
             "0.686", "0.686", "0.687",
             "1.469", "2.469", "1.469"
            },
        };


        for (int r = 0; r < roundingModes.length; r++) {
            NumberFormat dfDefault = NumberFormat.getInstance(Locale.US);
            RoundingMode rmode = roundingModes[r];
            dfDefault.setRoundingMode(rmode);
            System.out.println("\n----- Now checking " + rmode +
                               " rounding mode -----");

            for (int i = 0; i < values3FractDigits.length; i++) {
                double d = values3FractDigits[i];
                String tiePosition = tieRelativePositions[i];
                String input = inputs3FractDigits[i];
                String expected = expected3FractDigits[r][i];

                formatOutputTestDouble(dfDefault, d, tiePosition, input, expected);
            }
        }

        System.out.println("\n===== testing 5 digits rounding position =====");
        double[] values5FractDigits = {
            // unimpacting values close to tie, with less than 5 input fract digits
            1.3135d, 1.3125d, 1.3115d,
            // HALF_* impacting values close to tie, covering all 6 cases
            1.328115d, 1.328125d, 1.328135d,
            1.796865d, 1.796875d, 1.796885d,
            // specific HALF_UP close to tie values
            1.328124d, 1.798876d, 1.796889d,
            // specific HALF_DOWN close to tie values
            1.328114d, 1.796865d, 1.328138d,
            // unimpacting values close to tie, with more than 5 input fract digits
            1.3281149999999d, 1.75390625d, 1.7968750000001d
        };

        String[] inputs5FractDigits = {
            "1.3135d", "1.3125d", "1.3115d",
            "1.328115d", "1.328125d", "1.328135d",
            "1.796865d", "1.796875d", "1.796885d",
            "1.328124d", "1.798876d", "1.796889d",
            "1.328114d", "1.796865d", "1.328138d",
            "1.3281149999999d", "1.75390625d", "1.7968750000001d"
        };

        String[][] expected5FractDigits = {
            {"1.3135", "1.3125", "1.3115",
             "1.32811", "1.32812", "1.32814",
             "1.79686", "1.79687", "1.79689",
             "1.32812", "1.79888", "1.79689",
             "1.32811", "1.79686", "1.32814",
             "1.32811", "1.75391", "1.79688"
            },
            {"1.3135", "1.3125", "1.3115",
             "1.32811", "1.32812", "1.32814",
             "1.79686", "1.79688", "1.79689",
             "1.32812", "1.79888", "1.79689",
             "1.32811", "1.79686", "1.32814",
             "1.32811", "1.75391", "1.79688"
            },
            {"1.3135", "1.3125", "1.3115",
             "1.32811", "1.32813", "1.32814",
             "1.79686", "1.79688", "1.79689",
             "1.32812", "1.79888", "1.79689",
             "1.32811", "1.79686", "1.32814",
             "1.32811", "1.75391", "1.79688"
            }
        };


        for (int r = 0; r < roundingModes.length; r++) {
            DecimalFormat df5 = (DecimalFormat) NumberFormat.getInstance(Locale.US);
            RoundingMode rmode = roundingModes[r];
            df5.setRoundingMode(rmode);
            System.out.println("\n----- Now checking " + rmode +
                               " rounding mode -----");
            df5.applyPattern("#,###.#####");

            for (int i = 0; i < values5FractDigits.length; i++) {
                double d = values5FractDigits[i];
                String tiePosition = tieRelativePositions[i];
                String input = inputs5FractDigits[i];
                String expected = expected5FractDigits[r][i];

                formatOutputTestDouble(df5, d, tiePosition, input, expected);
            }
        }

        // ==================== Testing long value cases ====================

        System.out.println("\n===== testing long values =====");
        long l = 123456789012345L;
        DecimalFormat dfLong = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        String tiePosition = "exact";
        String input = "123456789012345L";
        String expected = "123,456,789,012,345";
        String result = dfLong.format(l);
        formatOutputTestLong(dfLong, l, tiePosition, input, expected);

        dfLong.applyPattern("0.###E0");
        expected = "1.235E14";
        formatOutputTestLong(dfLong, l, tiePosition, input, expected);

        l = 123450000000000L;
        input = "123450000000000L";
        expected = "1.234E14";
        formatOutputTestLong(dfLong, l, tiePosition, input, expected);

        l = 987750000000000L;
        input = "987750000000000L";
        expected = "9.878E14";
        formatOutputTestLong(dfLong, l, tiePosition, input, expected);

        dfLong.applyPattern("#,###.0E0");
        l = 987755000000000L;
        input = "987755000000000L";
        expected = "987.76E12";

        formatOutputTestLong(dfLong, l, tiePosition, input, expected);


        // ================= Testing BigInteger value cases =================

        System.out.println("\n===== testing BigInteger values =====");
        String stringValue = "12345678901234567890123456789012345";
        BigInteger bi = new BigInteger(stringValue);
        DecimalFormat dfBig = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        tiePosition = "exact";
        input = stringValue;
        expected = "12,345,678,901,234,567,890,123,456,789,012,345";
        formatOutputTestObject(dfBig, bi, tiePosition, input, expected);

        dfBig.applyPattern("0.###E0");
        expected = "1.235E34";
        formatOutputTestObject(dfBig, bi, tiePosition, input, expected);

        stringValue = "12345000000000000000000000000000000";
        input = stringValue;
        bi = new BigInteger(stringValue);
        expected = "1.234E34";
        formatOutputTestObject(dfBig, bi, tiePosition, input, expected);

        stringValue = "12345000000000000000000000000000001";
        input = stringValue;
        bi = new BigInteger(stringValue);
        expected = "1.235E34";
        formatOutputTestObject(dfBig, bi, tiePosition, input, expected);

        stringValue = "98755000000000000000000000000000000";
        input = stringValue;
        bi = new BigInteger(stringValue);
        expected = "9.876E34";
        formatOutputTestObject(dfBig, bi, tiePosition, input, expected);

        dfLong.applyPattern("#,###.0E0");
        stringValue = "98775500000000000000000000000000000";
        input = stringValue;
        expected = "987.76E34";

        // =============== Testing BigDecimal value cases ================

        System.out.println("\n===== testing BigDecimal values =====");
        dfBig = (DecimalFormat) NumberFormat.getInstance(Locale.US);

        stringValue = "0.68850000000000000088817841970012523233890533447265625";
        BigDecimal bd = new BigDecimal(stringValue);
        tiePosition = "exact";
        input = stringValue;
        expected = "0.689";
        formatOutputTestObject(dfBig, bd, tiePosition, input, expected);

        stringValue = "0.31149999999999999911182158029987476766109466552734375";
        bd = new BigDecimal(stringValue);
        dfBig.applyPattern("#,##0.####");
        tiePosition = "exact";
        input = stringValue;
        expected = "0.3115";
        formatOutputTestObject(dfBig, bd, tiePosition, input, expected);

        // ==================== Printing results and exiting ===================

        System.out.println();
        System.out.println("==> " + testCounter + " tests passed successfully");
        System.out.println("==> " + errorCounter + " tests failed");

        System.out.println();
        if (allPassed) {
            System.out.println(
                "Success in formating all the values with the given parameters");
        } else {
            String s = "Test failed with " + errorCounter + " formating error(s).";
            System.out.println(s);
            throw new RuntimeException(s);
        }
    }
}
