/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8209175 8247781
 * @summary Checks the 'B' character added in the CLDR date-time patterns is no longer
 *          getting resolved with 'a' character (am/pm strings) for burmese locale.
 *          This test case assumes that the 'B' character is added in CLDRv33 update
 *          for burmese locale in the time patterns. Since it is supported by
 *          DateTimeFormatter it should not be replaced with the 'a' while CLDR resource
 *          conversion.
 * @modules jdk.localedata
 */
package test.java.time.format;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

@Test
public class TestDayPeriodWithDTF {

    private static final Locale BURMESE = new Locale("my");

    private static final DateTimeFormatter FORMAT_SHORT_BURMESE = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(BURMESE);
    private static final DateTimeFormatter FORMAT_MEDIUM_BURMESE = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(BURMESE);

    private static final LocalTime LOCAL_TIME_AM = LocalTime.of(10, 10, 10);
    private static final LocalTime LOCAL_TIME_PM = LocalTime.of(12, 12, 12);

    @DataProvider(name = "timePatternData")
    Object[][] timePatternData() {
        return new Object[][] {
                // these messages are for day periods in Burmese.
                {FORMAT_SHORT_BURMESE, LOCAL_TIME_AM, "\u1014\u1036\u1014\u1000\u103A 10:10"},
                {FORMAT_SHORT_BURMESE, LOCAL_TIME_PM, "\u1014\u1031\u1037\u101c\u101a\u103a 12:12"},
                {FORMAT_MEDIUM_BURMESE, LOCAL_TIME_AM, "\u1014\u1036\u1014\u1000\u103A 10:10:10"},
                {FORMAT_MEDIUM_BURMESE, LOCAL_TIME_PM, "\u1014\u1031\u1037\u101c\u101a\u103a 12:12:12"},
        };
    }

    @Test(dataProvider = "timePatternData")
    public void testTimePattern(DateTimeFormatter formatter, LocalTime time, String expected) {
        String actual = formatter.format(time);
        assertEquals(actual, expected);
    }

}
