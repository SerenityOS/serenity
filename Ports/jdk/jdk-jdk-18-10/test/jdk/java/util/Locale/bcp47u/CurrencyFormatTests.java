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
 *
 * @test
 * @bug 8215181 8230284 8231273
 * @summary Tests the "u-cf" extension
 * @modules jdk.localedata
 * @run testng/othervm -Djava.locale.providers=CLDR CurrencyFormatTests
 */

import static org.testng.Assert.assertEquals;

import java.text.NumberFormat;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test NumberFormat with BCP47 u-cf extensions. Note that this test depends
 * on the particular CLDR release. Results may vary on other CLDR releases.
 */
@Test
public class CurrencyFormatTests {

    @DataProvider(name="getInstanceData")
    Object[][] getInstanceData() {
        return new Object[][] {
            // Locale, amount, expected
            // US dollar
            {Locale.US, -100, "-$100.00"},
            {Locale.forLanguageTag("en-US-u-cf-standard"), -100, "-$100.00"},
            {Locale.forLanguageTag("en-US-u-cf-account"), -100, "($100.00)"},
            {Locale.forLanguageTag("en-US-u-cf-bogus"), -100, "-$100.00"},

            // Euro
            {Locale.forLanguageTag("en-AT"), -100, "-\u20ac\u00a0100,00"},
            {Locale.forLanguageTag("en-AT-u-cf-standard"), -100, "-\u20ac\u00a0100,00"},
            {Locale.forLanguageTag("en-AT-u-cf-account"), -100, "-\u20ac\u00a0100,00"},
            {Locale.forLanguageTag("en-AT-u-cf-bogus"), -100, "-\u20ac\u00a0100,00"},

            // Rupee
            {Locale.forLanguageTag("en-IN"), -100, "-\u20b9100.00"},
            {Locale.forLanguageTag("en-IN-u-cf-standard"), -100, "-\u20b9100.00"},
            {Locale.forLanguageTag("en-IN-u-cf-account"), -100, "(\u20b9100.00)"},
            {Locale.forLanguageTag("en-IN-u-cf-bogus"), -100, "-\u20b9100.00"},

            // Swiss franc
            {Locale.forLanguageTag("en-CH"), -100, "CHF-100.00"},
            {Locale.forLanguageTag("en-CH-u-cf-standard"), -100, "CHF-100.00"},
            {Locale.forLanguageTag("en-CH-u-cf-account"), -100, "CHF-100.00"},
            {Locale.forLanguageTag("en-CH-u-cf-bogus"), -100, "CHF-100.00"},

            // Region override
            {Locale.forLanguageTag("en-US-u-rg-CHZZZZ"), -100, "CHF-100.00"},
            {Locale.forLanguageTag("en-US-u-rg-CHZZZZ-cf-standard"), -100, "CHF-100.00"},
            {Locale.forLanguageTag("en-US-u-rg-CHZZZZ-cf-account"), -100, "CHF-100.00"},
            {Locale.forLanguageTag("en-US-u-rg-CHZZZZ-cf-bogus"), -100, "CHF-100.00"},

            // Numbering systems
            // explicit
            {Locale.forLanguageTag("zh-CN-u-nu-arab"), -100, "\u061c-\u00a5\u0661\u0660\u0660\u066b\u0660\u0660"},
            {Locale.forLanguageTag("zh-CN-u-nu-arab-cf-standard"), -100, "\u061c-\u00a5\u0661\u0660\u0660\u066b\u0660\u0660"},
            {Locale.forLanguageTag("zh-CN-u-nu-arab-cf-account"), -100, "\u061c-\u00a5\u0661\u0660\u0660\u066b\u0660\u0660"},
            {Locale.forLanguageTag("zh-CN-u-nu-arab-cf-bogus"), -100, "\u061c-\u00a5\u0661\u0660\u0660\u066b\u0660\u0660"},
            // implicit
            {Locale.forLanguageTag("zh-CN"), -100, "-\u00a5100.00"},
            {Locale.forLanguageTag("zh-CN-u-cf-standard"), -100, "-\u00a5100.00"},
            {Locale.forLanguageTag("zh-CN-u-cf-account"), -100, "(\u00a5100.00)"},
            {Locale.forLanguageTag("zh-CN-u-cf-bogus"), -100, "-\u00a5100.00"},
            {Locale.forLanguageTag("ar-SA"), -100, "\u061c-\u0661\u0660\u0660\u066b\u0660\u0660\u00a0\u0631.\u0633.\u200f"},
            {Locale.forLanguageTag("ar-SA-u-cf-standard"), -100, "\u061c-\u0661\u0660\u0660\u066b\u0660\u0660\u00a0\u0631.\u0633.\u200f"},
            {Locale.forLanguageTag("ar-SA-u-cf-account"), -100, "\u061c-\u0661\u0660\u0660\u066b\u0660\u0660\u00a0\u0631.\u0633.\u200f"},
            {Locale.forLanguageTag("ar-SA-u-cf-bogus"), -100, "\u061c-\u0661\u0660\u0660\u066b\u0660\u0660\u00a0\u0631.\u0633.\u200f"},
        };
    }

    @Test(dataProvider="getInstanceData")
    public void test_getInstance(Locale locale, int amount, String expected) {
        assertEquals(NumberFormat.getCurrencyInstance(locale).format(amount), expected);
    }
}
