/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8222756
 * @summary Checks the plurals work with SPI provider
 * @modules jdk.localedata
 * @library provider
 * @build provider/module-info provider/test.NumberFormatProviderImpl
 * @run main/othervm -Djava.locale.providers=SPI,CLDR SPIProviderTest
 */

import java.text.CompactNumberFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;

public class SPIProviderTest {
    private static final Locale QAA = Locale.forLanguageTag("qaa");
    private static final Locale QAB = Locale.forLanguageTag("qab");

    public static void main(String... args) {
        new SPIProviderTest();
    }

    SPIProviderTest() {
        Arrays.stream(testData())
            .forEach(SPIProviderTest::testSPIProvider);
    }

    Object[][] testData() {
        return new Object[][]{
            // Locale, Number, expected
            {QAA, 1_000, "1K"},
            {QAA, -1_000, "-1K"},
            {QAA, 2_000, "2K"},
            {QAA, -2_000, "-2K"},
            {QAA, 1_000_000, "1M"},
            {QAA, -1_000_000, "-1M"},
            {QAA, 2_000_000, "2M"},
            {QAA, -2_000_000, "-2M"},

            {QAB, 1_000, "1K"},
            {QAB, -1_000, "(1K)"},
            {QAB, 2_000, "2KK"},
            {QAB, -2_000, "-2KK"},
            {QAB, 3_000, "3KKK"},
            {QAB, -3_000, "-3KKK"},
            {QAB, 5_000, "5KKKK"},
            {QAB, -5_000, "-5KKKK"},

            {QAB, 10_000, "10000"},
            {QAB, -10_000, "-10000"},

            {QAB, 1_000_000, "1 M"},
            {QAB, -1_000_000, "(1 M)"},
            {QAB, 2_000_000, "2 MM"},
            {QAB, -2_000_000, "(2 MM)"},
            {QAB, 3_000_000, "3 MMM"},
            {QAB, -3_000_000, "-3 MMM"},
            {QAB, 5_000_000, "5 MMMM"},
            {QAB, -5_000_000, "-5 MMMM"},

        };
    }

    public static void testSPIProvider(Object... args) {
        Locale loc = (Locale)args[0];
        Number number = (Number)args[1];
        String expected = (String)args[2];
        System.out.printf("Testing locale: %s, number: %d, expected: %s\n", loc, number, expected);

        NumberFormat nf =
            NumberFormat.getCompactNumberInstance(loc, NumberFormat.Style.SHORT);
        String formatted = nf.format(number);
        System.out.printf("    formatted: %s\n", formatted);
        if (!formatted.equals(expected)) {
            throw new RuntimeException("formatted and expected strings do not match.");
        }

        try {
            Number parsed = nf.parse(formatted);
            System.out.printf("    parsed: %s\n", parsed);
            if (parsed.intValue() != number.intValue()) {
                throw new RuntimeException("parsed and input numbers do not match.");
            }
        } catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
    }
}
