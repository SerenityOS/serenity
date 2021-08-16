/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8177552 8217254 8251499
 * @summary Checks the validity of compact number patterns specified through
 *          CompactNumberFormat constructor
 * @run testng/othervm TestCompactPatternsValidity
 */

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.CompactNumberFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestCompactPatternsValidity {

    // Max range 10^4
    private static final String[] COMPACT_PATTERN1 = new String[]{"0", "0", "0", "0K", "00K"};
    // Quoted special character '.' as prefix
    private static final String[] COMPACT_PATTERN2 = new String[]{"0", "'.'K0"};
    // Quoted special character '.' as suffix
    private static final String[] COMPACT_PATTERN3 = new String[]{"0", "0", "0", "0K", "00K'.'"};
    // Containing both prefix and suffix
    private static final String[] COMPACT_PATTERN4 = new String[]{"", "", "H0H", "0K", "00K", "H0G"};
    // Differing while specifying prefix and suffix
    private static final String[] COMPACT_PATTERN5 = new String[]{"", "", "", "0K", "K0"};
    // Containing both prefix ('.') and suffix (K)
    private static final String[] COMPACT_PATTERN6 = new String[]{"0", "", "", "'.'0K"};
    // Quoted special character ',' as suffix
    private static final String[] COMPACT_PATTERN7 = new String[]{"", "0", "0", "0K','"};
    // Most commonly used type of compact patterns with 15 elements
    private static final String[] COMPACT_PATTERN8 = new String[]{"", "", "", "0K", "00K", "000K", "0M",
        "00M", "000M", "0B", "00B", "000B", "0T", "00T", "000T"};
    // All empty or special patterns; checking the default formatting behaviour
    private static final String[] COMPACT_PATTERN9 = new String[]{"", "", "", "0", "0", "", "", "", "", "", "", "", "", "", ""};
    // Patterns beyond 10^19; divisors beyond long range
    private static final String[] COMPACT_PATTERN10 = new String[]{"", "", "", "0K", "00K", "000K", "0M", "00M",
        "000M", "0B", "00B", "000B", "0T", "00T", "000T", "0L", "00L", "000L", "0XL", "00XL"};
    // Containing positive;negative subpatterns
    private static final String[] COMPACT_PATTERN11 = new String[]{"", "", "", "elfu 0;elfu -0", "elfu 00;elfu -00",
        "elfu 000;elfu -000", "milioni 0;milioni -0", "milioni 00;milioni -00", "milioni 000;milioni -000"};
    // Containing both prefix and suffix and positive;negative subpatern
    private static final String[] COMPACT_PATTERN12 = new String[]{"", "", "H0H;H-0H", "0K;0K-", "00K;-00K", "H0G;-H0G"};
    // A non empty pattern containing no 0s (min integer digits)
    private static final String[] COMPACT_PATTERN13 =
        new String[]{"", "", "", "Thousand", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "BeyondLong"};
    // A non empty pattern containing no 0s (min integer digits), with plural rules
    private static final String[] COMPACT_PATTERN14 =
        new String[]{"", "", "", "{one:Kun other:0' 'Kun}"}; // from Somali in CLDR 38

    @DataProvider(name = "invalidPatterns")
    Object[][] invalidCompactPatterns() {
        return new Object[][] {
            // compact patterns
            // Pattern containing unquoted special character '.'
            {new String[]{"", "", "", "0K", "00K."}},
            // Pattern containing invalid single quote
            {new String[]{"", "", "", "0 'do", "00K"}},
            {new String[]{"", "", "", "0K", "00 don't"}},
            // 0s (min integer digits) exceeding for the range at index 3
            {new String[]{"", "", "0K", "00000K"}},
            // null as a compact pattern
            {new String[]{"", "", null, "00K"}},
        };
    }

    @DataProvider(name = "validPatternsFormat")
    Object[][] validPatternsFormat() {
        return new Object[][] {
            // compact patterns, numbers, expected output
            {COMPACT_PATTERN1, List.of(200, 1000, 3000, 500000), List.of("200", "1K", "3K", "500K")},
            {COMPACT_PATTERN2, List.of(1, 20, 3000), List.of("1", ".K2", ".K300")},
            {COMPACT_PATTERN3, List.of(100.99, 1000, 30000), List.of("101", "1K", "30K.")},
            {COMPACT_PATTERN4, List.of(0.0, 500, -500, 30000, 5000000), List.of("0", "H5H", "-H5H", "30K", "H50G")},
            {COMPACT_PATTERN5, List.of(100, 1000, 30000), List.of("100", "1K", "K3")},
            {COMPACT_PATTERN6, List.of(20.99, 1000, 30000), List.of("21", ".1K", ".30K")},
            {COMPACT_PATTERN7, List.of(100, 1000, new BigInteger("12345678987654321")), List.of("100", "1K,", "12345678987654K,")},
            {COMPACT_PATTERN8, List.of(new BigInteger("223565686837667632"), new BigDecimal("12322456774334.89766"), 30000, 3456.78),
                    List.of("223566T", "12T", "30K", "3K")},
            {COMPACT_PATTERN9, List.of(new BigInteger("223566000000000000"), new BigDecimal("12345678987654567"), 30000, 3000),
                    List.of("223,566,000,000,000,000", "12,345,678,987,654,567", "30,000", "3,000")},
            {COMPACT_PATTERN10, List.of(new BigInteger("100000000000000000"), new BigInteger("10000000000000000000"), new BigDecimal("555555555555555555555.89766"), 30000),
                    List.of("100L", "10XL", "556XL", "30K")},
            {COMPACT_PATTERN11, List.of(20.99, -20.99, 1000, -1000, 30000, -30000, new BigInteger("12345678987654321"), new BigInteger("-12345678987654321")),
                    List.of("21", "-21", "elfu 1", "elfu -1", "elfu 30", "elfu -30", "milioni 12345678988", "milioni -12345678988")},
            {COMPACT_PATTERN12, List.of(0, 500, -500, 30000, -3000, 5000000), List.of("0", "H5H", "H-5H", "30K", "3K-", "H50G")},
            {COMPACT_PATTERN13, List.of(1000, new BigInteger("10000000000000000000")), List.of("Thousand", "BeyondLong")},
        };
    }

    @DataProvider(name = "validPatternsParse")
    Object[][] validPatternsParse() {
        return new Object[][] {
            // compact patterns, parse string, expected output
            {COMPACT_PATTERN1, List.of(".56", "200", ".1K", "3K", "500K"), List.of(0.56, 200L, 100L, 3000L, 500000L)},
            {COMPACT_PATTERN2, List.of("1", ".K2", ".K300"), List.of(1L, 20L, 3000L)},
            {COMPACT_PATTERN3, List.of("101", "1K", "30K."), List.of(101L, 1000L, 30000L)},
            {COMPACT_PATTERN4, List.of("0", "H5H", "-H5H", "30K", "H50G"), List.of(0L, 500L, -500L, 30000L, 5000000L)},
            {COMPACT_PATTERN5, List.of("100", "1K", "K3"), List.of(100L, 1000L, 30000L)},
            {COMPACT_PATTERN6, List.of("21", ".1K", ".30K"), List.of(21L, 1000L, 30000L)},
            {COMPACT_PATTERN7, List.of("100", "1K,", "12345678987654K,"), List.of(100L, 1000L, 12345678987654000L)},
            {COMPACT_PATTERN8, List.of("223566T", "12T", "30K", "3K"), List.of(223566000000000000L, 12000000000000L, 30000L, 3000L)},
            {COMPACT_PATTERN10, List.of("1L", "100L", "10XL", "556XL", "30K"), List.of(1000000000000000L, 100000000000000000L, 1.0E19, 5.56E20, 30000L)},
            {COMPACT_PATTERN11, List.of("21", "-21", "100.90", "-100.90", "elfu 1", "elfu -1", "elfu 30", "elfu -30", "milioni 12345678988", "milioni -12345678988"),
                List.of(21L, -21L, 100.90, -100.90, 1000L, -1000L, 30000L, -30000L, 12345678988000000L, -12345678988000000L)},
            {COMPACT_PATTERN12, List.of("0", "H5H", "H-5H", "30K", "30K-", "H50G"), List.of(0L, 500L, -500L, 30000L, -30000L, 5000000L)},
            {COMPACT_PATTERN13, List.of("Thousand", "BeyondLong"), List.of(1000L, new BigInteger("10000000000000000000"))},
        };
    }

    @DataProvider(name = "validPatternsFormatWithPluralRules")
    Object[][] validPatternsFormatWithPluralRules() {
        return new Object[][] {
            // compact patterns, plural rules, numbers, expected output
            {COMPACT_PATTERN14, "one:n = 1", List.of(1000, 2345), List.of("Kun", "2 Kun")},
        };
    }

    @DataProvider(name = "validPatternsParseWithPluralRules")
    Object[][] validPatternsParseWithPluralRules() {
        return new Object[][] {
            // compact patterns, plural rules, parse string, expected output
            {COMPACT_PATTERN14, "one:n = 1", List.of("Kun", "2 Kun"), List.of(1000L, 2000L)},
        };
    }

    @Test(dataProvider = "invalidPatterns",
            expectedExceptions = IllegalArgumentException.class)
    public void testInvalidCompactPatterns(String[] compactPatterns) {
        new CompactNumberFormat("#,##0.0#", DecimalFormatSymbols
                .getInstance(Locale.US), compactPatterns);
    }

    @Test(dataProvider = "validPatternsFormat")
    public void testValidPatternsFormat(String[] compactPatterns,
            List<Object> numbers, List<String> expected) {
        CompactNumberFormat fmt = new CompactNumberFormat("#,##0.0#",
                DecimalFormatSymbols.getInstance(Locale.US), compactPatterns);
        for (int index = 0; index < numbers.size(); index++) {
            CompactFormatAndParseHelper.testFormat(fmt, numbers.get(index),
                    expected.get(index));
        }
    }

    @Test(dataProvider = "validPatternsParse")
    public void testValidPatternsParse(String[] compactPatterns,
            List<String> parseString, List<Number> numbers) throws ParseException {
        CompactNumberFormat fmt = new CompactNumberFormat("#,##0.0#",
                    DecimalFormatSymbols.getInstance(Locale.US), compactPatterns);
        for (int index = 0; index < parseString.size(); index++) {
            CompactFormatAndParseHelper.testParse(fmt, parseString.get(index),
                    numbers.get(index), null, null);
        }
    }

    @Test(dataProvider = "validPatternsFormatWithPluralRules")
    public void testValidPatternsFormatWithPluralRules(String[] compactPatterns, String pluralRules,
            List<Object> numbers, List<String> expected) {
        CompactNumberFormat fmt = new CompactNumberFormat("#,##0.0#",
                        DecimalFormatSymbols.getInstance(Locale.US), compactPatterns, pluralRules);
        for (int index = 0; index < numbers.size(); index++) {
            CompactFormatAndParseHelper.testFormat(fmt, numbers.get(index),
                    expected.get(index));
        }
    }

    @Test(dataProvider = "validPatternsParseWithPluralRules")
    public void testValidPatternsParsewithPluralRules(String[] compactPatterns, String pluralRules,
            List<String> parseString, List<Number> numbers) throws ParseException {
        CompactNumberFormat fmt = new CompactNumberFormat("#,##0.0#",
                        DecimalFormatSymbols.getInstance(Locale.US), compactPatterns, pluralRules);
        for (int index = 0; index < parseString.size(); index++) {
            CompactFormatAndParseHelper.testParse(fmt, parseString.get(index),
                    numbers.get(index), null, null);
        }
    }
}
