/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202088 8207152 8217609 8219890
 * @summary Test the localized Japanese new era name (May 1st. 2019-)
 *      is retrieved no matter CLDR provider contains the name or not.
 * @modules jdk.localedata
 * @run testng/othervm JapaneseEraNameTest
 * @run testng/othervm -Djava.locale.providers=CLDR JapaneseEraNameTest
 */

import static java.util.Calendar.*;
import static java.util.Locale.*;
import java.util.Calendar;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

@Test
public class JapaneseEraNameTest {
    static final Calendar c = new Calendar.Builder()
            .setCalendarType("japanese")
            .setFields(ERA, 5, YEAR, 1, MONTH, MAY, DAY_OF_MONTH, 1)
            .build();

    @DataProvider(name="names")
    Object[][] names() {
        return new Object[][] {
            // type,    locale,  name
            { LONG,     JAPAN,   "\u4ee4\u548c" },
            { LONG,     US,      "Reiwa" },
            { LONG,     CHINA,   "\u4ee4\u548c" },
            { SHORT,    JAPAN,   "\u4ee4\u548c" },
            { SHORT,    US,      "Reiwa" },
            { SHORT,    CHINA,   "\u4ee4\u548c" },
        };
    }

    @Test(dataProvider="names")
    public void testJapaneseNewEraName(int type, Locale locale, String expected) {
        assertEquals(c.getDisplayName(ERA, type, locale), expected);
    }
}
