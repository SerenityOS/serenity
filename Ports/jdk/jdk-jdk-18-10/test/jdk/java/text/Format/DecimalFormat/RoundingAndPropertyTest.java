/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7050528
 * @summary Test java.text.DecimalFormat fast-path for format(double...)
 * @author Olivier Lagneau
 * @build GoldenDoubleValues GoldenFormattedValues
 * @run main RoundingAndPropertyTest
 *
 */

/* -----------------------------------------------------------------------------
 * Note :
 *  Since fast-path algorithm   does not modify  any feature  of DecimalFormat,
 *  some tests or  values in this program  may have to be adapted/added/removed
 *  when any change has been done in the fast-path source  code, because the
 *  conditions for exercising fast-path may change.
 *
 *  This is specially true if the set of constraints to fall in the fast-path
 *  case is relaxed in any manner.
 *
 * Usage :
 *  - Run main without any argument to test against a set of golden values and
 *    associated results hard-coded in the source code. That will do the tests
 *    described below
 *    See below comment section named "Description".
 *
 *  or
 *
 *  - Run main with string argument "-gengold" to output source code of
 *    GoldenFormattedValues.java class file with the jdk version used while
 *    generating the code.
 *    See below comment section named : "Modifying Golden Values".
 *
 *  In case of error while running the test, a Runtime exception is generated
 *  providing the numbers of errors detected (format of golden values checks and
 *  property changes checks), and the program exit.
 *
 * Description :
 *
 *  This test first checks that localization of digits is done correctly when
 *  calling DecimalFormat.format() on the array of values DecimalLocalizationValues
 *  found in GoldenDoubleValues, using the locale FullLocalizationTestLocale
 *  (from GoldenDoubleValues) that implies localization of digits. it checks the
 *  the results against expected returned string. In case of formatting error,
 *  it provides a message informing which value was wrongly formatted.
 *
 *  Then it checks the results of  calling NumberFormat.format(double) on a set
 *  of  predefined golden values and  checks results against expected returned
 *  string.  It does this both for the  decimal case, with an instance returned
 *  NumberFormat.getInstance() call and for the currency case, with an instance
 *  returned by NumberFormat.getCurrencyInstance(). Almost all the tested  double
 *  values satisfy the constraints assumed by the fast-path algorithm for
 *  format(double ...). Some  are voluntarily outside the scope of fast-path to
 *  check that the algorithm correctly eliminate them. In case of formatting
 *  error a message provides information on the golden value raising the error
 *  (value, exact decimal value (using BidDecimal), expected result, formatted result).
 *
 *  Last  the test checks the status and  behavior of a DecimalFormat instance
 *  when changing  properties that  make this  instance  satisfy/invalidate its
 *  fast-path status, depending on the predefined  set of fast-path constraints.
 *
 *  The golden  results are predefined arrays  of  int[] containing the unicode
 *  ints of the chars  in  the expected  formatted  string, when  using  locale
 *  provided in  GoldenDoubleValues class. The   results are those obtained  by
 *  using a reference jdk  version (for example  one that does not contains the
 *  DecimalFormat fast-path algorithm, like jdk80-b25).
 *
 *  The double values from which we get golden results are stored inside two
 *  arrays of double values:
 *  - DecimalGoldenValues  for testing NumberFormat.getInstance().
 *  - CurrencyGoldenValues for testing NumberFormat.getCurrencyInstance().
 *  These arrays are located in GoldenDoubleValues.java source file.
 *
 *  For each double value in the arrays above, there is an associated golden
 *  result. These results are stored in arrays of int[]:
 *  - DecimalGoldenFormattedValues  for expected decimal golden results.
 *  - CurrencyGoldenFormattedValues for expected currency golden results.
 *  - DecimalDigitsLocalizedFormattedValues for expected localized digit results.
 *
 *  We store the results in int[] arrays containing the expected unicode values
 *  because the  compiler that will compile the  containing java file may use a
 *  different locale than the one registered in GoldenDoubleValues.java.  These
 *  arrays are  located in  a  separate GoldenFormattedValues.java  source file
 *  that is generated  by  RoundingAndPropertyTest using  "-gengold"  parameter.
 *  See below "Modifying Golden Values".
 *
 *  The golden value arrays can be expanded, modified ... to test additional
 *  or different double values. In that case, the source file of class
 *  GoldenFormattedValues must be regenerated to replace the existing one..
 *
 * Modifying Golden Values :
 *
 *  In order to ease further modification of the list of double values checked
 *  and associated golden results, the test includes the method
 *  generatesGoldenFormattedValuesClass() that writes on standard output stream
 *  the source code for GoldenFormattedValues class that includes the expected
 *  results arrays.
 *
 *  Here are the steps to follow for updating/modifying golden values and results:
 *   1- Edit GoldenDoubleValues.java to add/remove/modify golden or localization
 *      values.
 *   2- Run main with "-gengold" string argument with a target jdk.
 *      (at the creation of this test file, the target jdk used was jdk1.8.0-ea).
 *   2- Copy this java code that has been writen on standard output and replace
 *      GoldenFormattedValues.java contents by the generated output.
 *   3- Check that this updated code compiles.
 *  [4]- If needed replaces existing GoldenDoubleValues and GoldenFormattedValues
 *      files in jdk/test section, respectively by the one modified at step 1 and
 *      generated at step 2.
 * -----------------------------------------------------------------------------
 */

import java.util.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.math.RoundingMode;
import java.math.BigDecimal;


public class RoundingAndPropertyTest {


    // Prints on standard output stream the unicode values of chars as a
    // comma-separated list of int values
    private static void printUnicodeValuesArray(char[] chars) {
        for (int i = 0; i < chars.length; i++) {
            System.out.print((int) chars[i]);
            if (i != (chars.length - 1))
                System.out.print(", ");
        }
    }

    // Converts given array of unicode values as an array of chars.
    // Returns this converted array.
    private static char[] getCharsFromUnicodeArray(int[] unicodeValues) {
        char[] chars = new char[unicodeValues.length];

        for (int i = 0; i < unicodeValues.length; i++) {
            chars[i] = (char) unicodeValues[i];
        }
        return chars;
    }

    /* Prints on standard output stream the java code of resulting
     * GoldenFormattedValues class for the golden values found in
     * class GoldenDoubleValues.
     */
    private static void generatesGoldenFormattedValuesClass() {

        String fourWhiteSpaces    = "    ";
        String eightWhiteSpaces   = "        ";

        // Prints header without Copyright header.
        System.out.println("/* This is a machine generated file - Please DO NOT EDIT !");
        System.out.println(" * Change RoundingAndPropertyTest instead,");
        System.out.println(" * and run with \"-gengold\" argument to regenerate (without copyright header).");
        System.out.println(" */");
        System.out.println();

        System.out.println("/* This file contains the set of result Strings expected from calling inside");
        System.out.println(" * RoundingAndPropertyTest the method NumberFormat.format() upon the set of");
        System.out.println(" * double values provided in GoldenDoubleValues.java. It contains three arrays,");
        System.out.println(" * each containing arrays of unicode values representing the expected string");
        System.out.println(" * result when calling format() on the corresponding (i.e. same index) double");
        System.out.println(" * value found in GoldenDoubleValues arrays :");
        System.out.println(" * - DecimalDigitsLocalizedFormattedValues corresponds to DecimalLocalizationValues,");
        System.out.println(" *   when using FullLocalizationTestLocale to format.");
        System.out.println(" * - DecimalGoldenFormattedValues corresponds to DecimalGoldenValues, when used");
        System.out.println(" *   in the decimal pattern case together with TestLocale.");
        System.out.println(" * - CurrencyGoldenFormattedValues corresponds to CurrencyGoldenValues. when used");
        System.out.println(" *   in the currency pattern case together with TestLocale.");
        System.out.println(" * Please see documentation in RoundingAndPropertyTest.java for more details.");
        System.out.println(" *");
        System.out.println(" * This file generated by running RoundingAndPropertyTest with \"-gengold\" argument.");
        System.out.println(" */");
        System.out.println();

        // Prints beginning of class GoldenFormattedValues.
        System.out.println("class GoldenFormattedValues {");
        System.out.println();
        System.out.println(
            fourWhiteSpaces +
            "// The formatted values below were generated from golden values");
        System.out.print(
            fourWhiteSpaces +
            "// listed in GoldenDoubleValues.java,");
        System.out.println(" using the following jvm version :");
        System.out.println(
            fourWhiteSpaces + "//   " +
            System.getProperty("java.vendor") +
            " " +
            System.getProperty("java.vm.name") +
            " " +
            System.getProperty("java.version"));
        System.out.println(
            fourWhiteSpaces +
            "//   locale for golden double values : " + GoldenDoubleValues.TestLocale);
        System.out.println(
            fourWhiteSpaces +
            "//   locale for testing digit localization : " + GoldenDoubleValues.FullLocalizationTestLocale);
        System.out.println();

        // Prints the expected results when digit localization happens
        System.out.println(
            fourWhiteSpaces +
            "// The array of int[] unicode values storing the expected results");
        System.out.print(
            fourWhiteSpaces +
            "// when experiencing full localization of digits");
        System.out.println(" on DecimalLocalizationValues.");
        System.out.println(
            fourWhiteSpaces +
            "static int[][] DecimalDigitsLocalizedFormattedValues = {");
        NumberFormat df =
            NumberFormat.getInstance(GoldenDoubleValues.FullLocalizationTestLocale);
        for (int i = 0;
             i < GoldenDoubleValues.DecimalLocalizationValues.length;
             i++) {
            double d = GoldenDoubleValues.DecimalLocalizationValues[i];
            String formatted = df.format(d);
            char[] decFmtChars = formatted.toCharArray();

            System.out.print(eightWhiteSpaces + "{ ");
            printUnicodeValuesArray(decFmtChars);
            System.out.println(" },");
        }
        System.out.println(fourWhiteSpaces + "};");
        System.out.println();

        // Prints the golden expected results for the decimal pattern case
        System.out.println(
            fourWhiteSpaces +
            "// The array of int[] unicode values storing the expected results");
        System.out.print(
            fourWhiteSpaces +
            "// when calling Decimal.format(double)");
        System.out.println(" on the decimal GoldenDoubleValues.");
        System.out.println(
            fourWhiteSpaces +
            "static int[][] DecimalGoldenFormattedValues = {");
        df = NumberFormat.getInstance(GoldenDoubleValues.TestLocale);
        for (int i = 0;
             i < GoldenDoubleValues.DecimalGoldenValues.length;
             i++) {
            double d = GoldenDoubleValues.DecimalGoldenValues[i];
            String formatted = df.format(d);
            char[] decFmtChars = formatted.toCharArray();

            System.out.print(eightWhiteSpaces + "{ ");
            printUnicodeValuesArray(decFmtChars);
            System.out.println(" },");
        }
        System.out.println(fourWhiteSpaces + "};");
        System.out.println();

        // Prints the golden expected results for the currency pattern case
        System.out.println(
            fourWhiteSpaces +
            "// The array of int[] unicode values storing the expected results");
        System.out.print(
            fourWhiteSpaces +
            "// when calling Decimal.format(double)");
        System.out.println(" on the currency GoldenDoubleValues.");
        System.out.println(
            fourWhiteSpaces +
            "static int[][] CurrencyGoldenFormattedValues = {");
        NumberFormat cf =
            NumberFormat.getCurrencyInstance(GoldenDoubleValues.TestLocale);
        for (int i = 0;
             i < GoldenDoubleValues.CurrencyGoldenValues.length;
             i++) {
            double d = GoldenDoubleValues.CurrencyGoldenValues[i];
            String formatted = cf.format(d);
            char[] decFmtChars = formatted.toCharArray();

            System.out.print(eightWhiteSpaces + "{ ");
            printUnicodeValuesArray(decFmtChars);
            System.out.println(" },");
        }
        System.out.println(fourWhiteSpaces + "};");
        System.out.println();

        // Prints end of GoldenFormattedValues class.
        System.out.println("}");
    }

    private static int testLocalizationValues() {

        DecimalFormat df = (DecimalFormat)
            NumberFormat.getInstance(GoldenDoubleValues.FullLocalizationTestLocale);

        double[] localizationValues = GoldenDoubleValues.DecimalLocalizationValues;
        int size = localizationValues.length;
        int successCounter = 0;
        int failureCounter = 0;
        for (int i = 0; i < size; i++) {

            double d = localizationValues[i];
            String formatted = df.format(d);

            char[] expectedUnicodeArray =
                getCharsFromUnicodeArray(
                    GoldenFormattedValues.DecimalDigitsLocalizedFormattedValues[i]);
            String expected = new String(expectedUnicodeArray);

            if (!formatted.equals(expected)) {
                failureCounter++;
                System.out.println(
                    "--- Localization error for value d = " + d +
                    ". Exact value = " + new BigDecimal(d).toString() +
                    ". Expected result = " + expected +
                    ". Output result = " + formatted);
            } else successCounter++;
        }
        System.out.println("Checked positively " + successCounter +
                           " golden decimal values out of " + size +
                           " tests. There were " + failureCounter +
                           " format failure");

        return failureCounter;
    }

    private static int testGoldenValues(java.text.DecimalFormat df,
                                        java.text.DecimalFormat cf) {

        double[] goldenDecimalValues = GoldenDoubleValues.DecimalGoldenValues;
        int decimalSize = goldenDecimalValues.length;
        int decimalSuccessCounter = 0;
        int decimalFailureCounter = 0;
        for (int i = 0; i < decimalSize; i++) {

            double d = goldenDecimalValues[i];
            String formatted = df.format(d);

            char[] expectedUnicodeArray =
                getCharsFromUnicodeArray(
                    GoldenFormattedValues.DecimalGoldenFormattedValues[i]);
            String expected = new String(expectedUnicodeArray);

            if (!formatted.equals(expected)) {
                decimalFailureCounter++;
                System.out.println(
                    "--- Error for golden value d = " + d +
                    ". Exact value = " + new BigDecimal(d).toString() +
                    ". Expected result = " + expected +
                    ". Output result = " + formatted);
            } else decimalSuccessCounter++;
        }
        System.out.println("Checked positively " + decimalSuccessCounter +
                           " golden decimal values out of " + decimalSize +
                           " tests. There were " + decimalFailureCounter +
                           " format failure");

        double[] goldenCurrencyValues = GoldenDoubleValues.CurrencyGoldenValues;
        int currencySize = goldenCurrencyValues.length;
        int currencySuccessCounter = 0;
        int currencyFailureCounter = 0;
        for (int i = 0; i < currencySize; i++) {
            double d = goldenCurrencyValues[i];
            String formatted = cf.format(d);

            char[] expectedUnicodeArray =
                getCharsFromUnicodeArray(
                    GoldenFormattedValues.CurrencyGoldenFormattedValues[i]);
            String expected = new String(expectedUnicodeArray);

            if (!formatted.equals(expected)) {
                currencyFailureCounter++;
                System.out.println(
                    "--- Error for golden value d = " + d +
                    ". Exact value = " + new BigDecimal(d).toString() +
                    ". Expected result = " + expected +
                    ". Output result = " + formatted);
            } else currencySuccessCounter++;
        }
        System.out.println("Checked positively " + currencySuccessCounter +
                           " golden currency values out of " + currencySize +
                           " tests. There were " + currencyFailureCounter +
                           " format failure");

        return (decimalFailureCounter + currencyFailureCounter);
    }

    // Checks that the two passed s1 and s2 string are equal, and prints
    // out message in case of error.
    private static boolean resultsEqual(String propertyName,
                                        String s1,
                                        String s2) {

        boolean equality = s1.equals(s2);
        if (!equality)
            System.out.println(
                "\n*** Error while reverting to default " +
                propertyName + " property.\n" +
                "    initial output = " + s1 +
                ". reverted output = " + s2 + ".");
        else System.out.println(" Test passed.");

        return equality;

    }

    /* This methods checks the behaviour of the management of properties
     * of a DecimalFormat instance that satisfies fast-path constraints.
     *
     * It does this by comparing the results of the format(double) output
     * obtained from initial fast-path state with the output provided by
     * the same instance that has been pushed and exercised outside
     * fast-path rules and finally "reverted" to its initial fast-path state.
     *
     * The schema of actions is this :
     *  - Call format(double) on a known DecimalFormat fast-path instance,
     *    and store this result.
     *  - Record the current state of a given property.
     *  - Change the property to invalidate the fast-path state.
     *  - Call again format(double) on the instance.
     *  - Revert state of property to validate again fast-path context.
     *  - Call format(double) again.
     *  - Check that first and last call to format(double) provide same result
     *  - Record failure if any.
     *  - Do the same for another property with the same instance.
     * So all the property changes are chained one after the other on only the
     * same instance.
     *
     * Some properties that currently do not influence the fast-path state
     * are also tested. This is not useful with current fast-path source
     * but is here for testing the whole set of properties. This is the case
     * for prefixes and suffixes, and parseBigDecimal properties.
     */
    private static int testSettersAndFastPath(DecimalFormat df,
                                               boolean isCurrency) {

        final double d1 = GoldenDoubleValues.PROPERTY_CHECK_POSITIVE_VALUE;
        final double d2 = GoldenDoubleValues.PROPERTY_CHECK_NEGATIVE_VALUE;

        int errors = 0;
        boolean testSucceeded = false;
        String firstFormatResult;
        String secondFormatResult;
        String propertyName;

        // ---- positivePrefix property test ----
        testSucceeded = false;
        propertyName = "positivePrefix";
        System.out.print("Checking " + propertyName + " property.");
        String initialPrefix = df.getPositivePrefix();
        firstFormatResult = df.format(d1);
        df.setPositivePrefix("positivePrefix:");
        df.format(d1);
        df.setPositivePrefix(initialPrefix);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- positiveSuffix property test ----
        testSucceeded = false;
        propertyName = "positiveSuffix";
        System.out.print("Checking " + propertyName + " property.");
        String initialSuffix = df.getPositiveSuffix();
        firstFormatResult = df.format(d1);
        df.setPositiveSuffix("positiveSuffix:");
        df.format(d1);
        df.setPositiveSuffix(initialSuffix);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName,firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- negativePrefix property test ----
        testSucceeded = false;
        propertyName = "negativePrefix";
        System.out.print("Checking " + propertyName + " property.");
        initialPrefix = df.getNegativePrefix();
        firstFormatResult = df.format(d1);
        df.setNegativePrefix("negativePrefix:");
        df.format(d1);
        df.setNegativePrefix(initialPrefix);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- negativeSuffix property test ----
        testSucceeded = false;
        propertyName = "negativeSuffix";
        System.out.print("Checking " + propertyName + " property.");
        initialSuffix = df.getNegativeSuffix();
        firstFormatResult = df.format(d1);
        df.setNegativeSuffix("negativeSuffix:");
        df.format(d1);
        df.setNegativeSuffix(initialSuffix);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- multiplier property test ----
        testSucceeded = false;
        propertyName = "multiplier";
        System.out.print("Checking " + propertyName + " property.");
        int initialMultiplier = df.getMultiplier();
        firstFormatResult = df.format(d1);
        df.setMultiplier(10);
        df.format(d1);
        df.setMultiplier(initialMultiplier);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- groupingUsed property test ----
        testSucceeded = false;
        propertyName = "groupingUsed";
        System.out.print("Checking " + propertyName + " property.");
        boolean initialGroupingUsed = df.isGroupingUsed();
        firstFormatResult = df.format(d1);
        df.setGroupingUsed(!initialGroupingUsed);
        df.format(d1);
        df.setGroupingUsed(initialGroupingUsed);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- groupingSize property test ----
        testSucceeded = false;
        propertyName = "groupingSize";
        System.out.print("Checking " + propertyName + " property.");
        int initialGroupingSize = df.getGroupingSize();
        firstFormatResult = df.format(d1);
        df.setGroupingSize(initialGroupingSize + 1);
        df.format(d1);
        df.setGroupingSize(initialGroupingSize);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- decimalSeparatorAlwaysShown property test ----
        testSucceeded = false;
        propertyName = "decimalSeparatorAlwaysShown";
        System.out.print("Checking " + propertyName + " property.");
        boolean initialDSShown = df.isDecimalSeparatorAlwaysShown();
        firstFormatResult = df.format(d1);
        df.setDecimalSeparatorAlwaysShown(!initialDSShown);
        df.format(d1);
        df.setDecimalSeparatorAlwaysShown(initialDSShown);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- parseBigDecimal property test ----
        testSucceeded = false;
        propertyName = "parseBigDecimal";
        System.out.print("Checking " + propertyName + " property.");
        boolean initialParseBigdecimal = df.isParseBigDecimal();
        firstFormatResult = df.format(d1);
        df.setParseBigDecimal(!initialParseBigdecimal);
        df.format(d1);
        df.setParseBigDecimal(initialParseBigdecimal);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- maximumIntegerDigits property test ----
        testSucceeded = false;
        propertyName = "maximumIntegerDigits";
        System.out.print("Checking " + propertyName + " property.");
        int initialMaxIDs = df.getMaximumIntegerDigits();
        firstFormatResult = df.format(d1);
        df.setMaximumIntegerDigits(8);
        df.format(d1);
        df.setMaximumIntegerDigits(initialMaxIDs);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- minimumIntegerDigits property test ----
        testSucceeded = false;
        propertyName = "minimumIntegerDigits";
        System.out.print("Checking " + propertyName + " property.");
        int initialMinIDs = df.getMinimumIntegerDigits();
        firstFormatResult = df.format(d1);
        df.setMinimumIntegerDigits(2);
        df.format(d1);
        df.setMinimumIntegerDigits(initialMinIDs);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- maximumFractionDigits property test ----
        testSucceeded = false;
        propertyName = "maximumFractionDigits";
        System.out.print("Checking " + propertyName + " property.");
        firstFormatResult = df.format(d1);
        df.setMaximumFractionDigits(8);
        df.format(d1);
        if (isCurrency) {
            df.setMinimumFractionDigits(2);
            df.setMaximumFractionDigits(2);
        } else {
            df.setMinimumFractionDigits(0);
            df.setMaximumFractionDigits(3);
        }
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- minimumFractionDigits property test ----
        testSucceeded = false;
        propertyName = "minimumFractionDigits";
        System.out.print("Checking " + propertyName + " property.");
        firstFormatResult = df.format(d1);
        df.setMinimumFractionDigits(1);
        df.format(d1);
        if (isCurrency) {
            df.setMinimumFractionDigits(2);
            df.setMaximumFractionDigits(2);
        } else {
            df.setMinimumFractionDigits(0);
            df.setMaximumFractionDigits(3);
        }
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- currency property test ----
        testSucceeded = false;
        propertyName = "currency";
        System.out.print("Checking " + propertyName + " property.");
        Currency initialCurrency = df.getCurrency();
        Currency japanCur = java.util.Currency.getInstance(Locale.JAPAN);
        firstFormatResult = df.format(d1);
        df.setCurrency(japanCur);
        df.format(d1);
        df.setCurrency(initialCurrency);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- roundingMode property test ----
        testSucceeded = false;
        propertyName = "roundingMode";
        System.out.print("Checking " + propertyName + " property.");
        RoundingMode initialRMode = df.getRoundingMode();
        firstFormatResult = df.format(d1);
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.format(d1);
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        // ---- decimalFormatSymbols property test ----
        testSucceeded = false;
        propertyName = "decimalFormatSymbols";
        System.out.print("Checking " + propertyName + " property.");
        DecimalFormatSymbols initialDecimalFormatSymbols = df.getDecimalFormatSymbols();
        firstFormatResult = df.format(d1);
        Locale bizarreLocale = new Locale("fr", "FR");
        DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols(bizarreLocale);
        unusualSymbols.setDecimalSeparator('@');
        unusualSymbols.setGroupingSeparator('|');
        df.setDecimalFormatSymbols(unusualSymbols);
        df.format(d1);
        df.setDecimalFormatSymbols(initialDecimalFormatSymbols);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        testSucceeded = false;
        System.out.print("Checking " + propertyName + " property.");
        initialDecimalFormatSymbols = df.getDecimalFormatSymbols();
        firstFormatResult = df.format(d1);
        Locale japanLocale = Locale.JAPAN;
        unusualSymbols = new DecimalFormatSymbols(japanLocale);
        unusualSymbols.setDecimalSeparator('9');
        unusualSymbols.setGroupingSeparator('0');
        df.setDecimalFormatSymbols(unusualSymbols);
        df.format(d1);
        df.setDecimalFormatSymbols(initialDecimalFormatSymbols);
        secondFormatResult = df.format(d1);
        testSucceeded =
            resultsEqual(propertyName, firstFormatResult, secondFormatResult);
        if (!testSucceeded)
            errors++;

        return errors;
    }

    // Main for RoundingAndPropertyTest. We test first the golden values,
    // and then the property setters and getters.
    public static void main(String[] args) {

        if ((args.length >= 1) &&
            (args[0].equals("-gengold")))
            generatesGoldenFormattedValuesClass();
        else {
            System.out.println("\nChecking correctness of formatting with digit localization.");
            System.out.println("=============================================================");
            int localizationErrors = testLocalizationValues();
            if (localizationErrors != 0)
                System.out.println("*** Failure in localization tests : " +
                                   localizationErrors + " errors detected ");
            else System.out.println(" Tests for full localization of digits all passed.");

            DecimalFormat df = (DecimalFormat)
                NumberFormat.getInstance(GoldenDoubleValues.TestLocale);
            DecimalFormat cf = (DecimalFormat)
                NumberFormat.getCurrencyInstance(GoldenDoubleValues.TestLocale);

            System.out.println("\nChecking correctness of formating for golden values.");
            System.out.println("=============================================================");
            int goldenValuesErrors = testGoldenValues(df,cf);
            if (goldenValuesErrors != 0)
                System.out.println("*** Failure in goldenValues tests : " +
                                   goldenValuesErrors + " errors detected ");
            else System.out.println(" Tests for golden values all passed.");

            System.out.println("\nChecking behavior of property changes for decimal case.");
            System.out.println("=============================================================");
            int decimalTestsErrors = testSettersAndFastPath(df, false);
            if (decimalTestsErrors != 0)
                System.out.println("*** Failure in decimal property changes tests : " +
                                   decimalTestsErrors + " errors detected ");
            else System.out.println(" Tests for decimal property changes all passed.");

            System.out.println("\nChecking behavior of property changes for currency case.");
            System.out.println("=============================================================");
            int currencyTestsErrors = testSettersAndFastPath(cf, true);
            if (currencyTestsErrors != 0)
                System.out.println("*** Failure in currency property changes tests : " +
                                   currencyTestsErrors + " errors detected ");
            else System.out.println(" Tests for currency property chamges all passed.");

            if ((localizationErrors > 0) ||
                (goldenValuesErrors > 0) ||
                (decimalTestsErrors > 0) ||
                (currencyTestsErrors > 0))
                throw new RuntimeException(
                    "Failed with " +
                    (localizationErrors + goldenValuesErrors +
                     decimalTestsErrors + currencyTestsErrors) +
                    " error(s).");
        }
    }
}
