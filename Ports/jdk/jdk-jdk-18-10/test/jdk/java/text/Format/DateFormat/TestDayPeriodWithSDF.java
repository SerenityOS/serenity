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
 * @bug 8209175
 * @summary Checks the 'B' character added in the CLDR date-time patterns is
 *          getting resolved with 'a' character (am/pm strings) for burmese locale.
 *          This test case assumes that the 'B' character is added in CLDRv33 update
 *          for burmese locale in the time patterns. Since it is not supported by
 *          SimpleDateFormat it is replaced with the 'a' while CLDR resource
 *          conversion.
 * @modules jdk.localedata
 * @run testng/othervm TestDayPeriodWithSDF
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class TestDayPeriodWithSDF {

    private static final Locale BURMESE = new Locale("my");
    private static final DateFormat FORMAT_SHORT_BURMESE = DateFormat.getTimeInstance(DateFormat.SHORT, BURMESE);
    private static final DateFormat FORMAT_MEDIUM_BURMESE = DateFormat.getTimeInstance(DateFormat.MEDIUM, BURMESE);

    private static final Date DATE_AM = new GregorianCalendar(2019, Calendar.FEBRUARY, 14, 10, 10, 10).getTime();
    private static final Date DATE_PM = new GregorianCalendar(2019, Calendar.FEBRUARY, 14, 12, 12, 12).getTime();

    @DataProvider(name = "timePatternData")
    Object[][] timePatternData() {
        return new Object[][] {
                {FORMAT_SHORT_BURMESE, DATE_AM, "\u1014\u1036\u1014\u1000\u103A \u1041\u1040:\u1041\u1040"},
                {FORMAT_SHORT_BURMESE, DATE_PM, "\u100A\u1014\u1031 \u1041\u1042:\u1041\u1042"},
                {FORMAT_MEDIUM_BURMESE, DATE_AM, "\u1014\u1036\u1014\u1000\u103A \u1041\u1040:\u1041\u1040:\u1041\u1040"},
                {FORMAT_MEDIUM_BURMESE, DATE_PM, "\u100A\u1014\u1031 \u1041\u1042:\u1041\u1042:\u1041\u1042"},
        };
    }

    @Test(dataProvider = "timePatternData")
    public void testTimePattern(DateFormat format, Date date, String expected) {
        String actual = format.format(date);
        assertEquals(actual, expected);
    }

}
