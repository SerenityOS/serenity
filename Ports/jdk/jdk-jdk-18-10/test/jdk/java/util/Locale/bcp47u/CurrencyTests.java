/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @test
 * @bug 8176841
 * @summary Tests Currency class instantiates correctly with Unicode
 *      extensions
 * @modules jdk.localedata
 * @run testng/othervm CurrencyTests
 */

import static org.testng.Assert.assertEquals;

import java.util.Currency;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Currency with BCP47 U extensions
 */
@Test
public class CurrencyTests {
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency CAD = Currency.getInstance("CAD");
    private static final Currency JPY = Currency.getInstance("JPY");

    @DataProvider(name="getInstanceData")
    Object[][] getInstanceData() {
        return new Object[][] {
            // Locale, Expected Currency
            // "cu"
            {Locale.forLanguageTag("en-US-u-cu-jpy"), JPY},
            {Locale.forLanguageTag("ja-JP-u-cu-usd"), USD},
            {Locale.forLanguageTag("en-US-u-cu-foobar"), USD},
            {Locale.forLanguageTag("en-US-u-cu-zzz"), USD},

            // "rg"
            {Locale.forLanguageTag("en-US-u-rg-jpzzzz"), JPY},
            {Locale.forLanguageTag("ja-JP-u-rg-uszzzz"), USD},
            {Locale.forLanguageTag("ja-JP-u-rg-001zzzz"), JPY},
            {Locale.forLanguageTag("en-US-u-rg-jpz"), USD},

            // "cu" and "rg". "cu" should win
            {Locale.forLanguageTag("en-CA-u-cu-jpy-rg-uszzzz"), JPY},

            // invaid "cu" and valid "rg". "rg" should win
            {Locale.forLanguageTag("en-CA-u-cu-jpyy-rg-uszzzz"), USD},
            {Locale.forLanguageTag("en-CA-u-cu-zzz-rg-uszzzz"), USD},

            // invaid "cu" and invalid "rg". both should be ignored
            {Locale.forLanguageTag("en-CA-u-cu-jpyy-rg-jpzz"), CAD},
        };
    }

    @DataProvider(name="getSymbolData")
    Object[][] getSymbolData() {
        return new Object[][] {
            // Currency, DisplayLocale, expected Symbol
            {USD, Locale.forLanguageTag("en-US-u-rg-jpzzzz"), "$"},
            {USD, Locale.forLanguageTag("en-US-u-rg-cazzzz"), "US$"},
            {USD, Locale.forLanguageTag("en-CA-u-rg-uszzzz"), "$"},

            {CAD, Locale.forLanguageTag("en-US-u-rg-jpzzzz"), "CA$"},
            {CAD, Locale.forLanguageTag("en-US-u-rg-cazzzz"), "$"},
            {CAD, Locale.forLanguageTag("en-CA-u-rg-uszzzz"), "CA$"},

            {JPY, Locale.forLanguageTag("ja-JP-u-rg-uszzzz"), "\uffe5"},
            {JPY, Locale.forLanguageTag("en-US-u-rg-jpzzzz"), "\u00a5"},
            {JPY, Locale.forLanguageTag("ko-KR-u-rg-jpzzzz"), "JP\u00a5"},
        };
    }

    @Test(dataProvider="getInstanceData")
    public void test_getInstance(Locale locale, Currency currencyExpected) {
        assertEquals(Currency.getInstance(locale), currencyExpected);
    }

    @Test(dataProvider="getSymbolData")
    public void test_getSymbol(Currency c, Locale locale, String expected) {
        assertEquals(c.getSymbol(locale), expected);
    }
}
