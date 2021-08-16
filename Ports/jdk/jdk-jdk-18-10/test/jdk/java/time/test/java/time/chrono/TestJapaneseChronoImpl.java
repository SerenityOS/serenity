/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Copyright (c) 2008-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package test.java.time.chrono;

import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseEra;
import java.time.chrono.JapaneseDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TestJapaneseChronoImpl {

    /**
     * Range of years to check consistency with java.util.Calendar
     */
    @DataProvider(name="RangeVersusCalendar")
    Object[][] provider_rangeVersusCalendar() {
        return new Object[][] {
            {LocalDate.of(1873, 1, 1), LocalDate.of(2100, 1, 1)},
        };
    }

    //-----------------------------------------------------------------------
    // Verify  Japanese Calendar matches java.util.Calendar for range
    //-----------------------------------------------------------------------
    @Test(dataProvider="RangeVersusCalendar")
    public void test_JapaneseChrono_vsCalendar(LocalDate isoStartDate, LocalDate isoEndDate) {
        Locale locale = Locale.forLanguageTag("ja-JP-u-ca-japanese");
        assertEquals(locale.toString(), "ja_JP_#u-ca-japanese", "Unexpected locale");

        Calendar cal = java.util.Calendar.getInstance(locale);
        assertEquals(cal.getCalendarType(), "japanese", "Unexpected calendar type");

        JapaneseDate jDate = JapaneseChronology.INSTANCE.date(isoStartDate);

        // Convert to millis and set Japanese Calendar to that start date (at GMT)
        OffsetDateTime jodt = OffsetDateTime.of(isoStartDate, LocalTime.MIN, ZoneOffset.UTC);
        long millis = jodt.toInstant().toEpochMilli();
        cal.setTimeZone(TimeZone.getTimeZone("GMT+00"));
        cal.setTimeInMillis(millis);

        while (jDate.isBefore(isoEndDate)) {
            assertEquals(jDate.get(ChronoField.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_MONTH), "Day mismatch in " + jDate + ";  cal: " + cal);
            assertEquals(jDate.get(ChronoField.MONTH_OF_YEAR), cal.get(Calendar.MONTH) + 1, "Month mismatch in " + jDate);
            assertEquals(jDate.get(ChronoField.YEAR_OF_ERA), cal.get(Calendar.YEAR), "Year mismatch in " + jDate);

            jDate = jDate.plus(1, ChronoUnit.DAYS);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    //-----------------------------------------------------------------------
    // Verify  Japanese Calendar matches java.util.Calendar for number of days
    // in years 1 and 2.
    //-----------------------------------------------------------------------
    @Test
    public void test_dayOfYearVsCalendar() {
        Locale locale = Locale.forLanguageTag("ja-JP-u-ca-japanese");
        Calendar cal = java.util.Calendar.getInstance(locale);

        for (JapaneseEra era : JapaneseEra.values()) {
            for (int year : new int[] {6, 7}) {
                JapaneseDate jd = JapaneseChronology.INSTANCE.dateYearDay(era, year, 1);
                OffsetDateTime jodt = OffsetDateTime.of(LocalDate.from(jd), LocalTime.MIN, ZoneOffset.UTC);
                long millis = jodt.toInstant().toEpochMilli();
                cal.setTimeZone(TimeZone.getTimeZone("GMT+00"));
                cal.setTimeInMillis(millis);

                assertEquals(jd.get(ChronoField.DAY_OF_YEAR), cal.get(Calendar.DAY_OF_YEAR),
                        "different DAY_OF_YEAR values in " + era + ", year: " + year);
                assertEquals(jd.range(ChronoField.DAY_OF_YEAR).getMaximum(), cal.getActualMaximum(Calendar.DAY_OF_YEAR),
                        "different maximum for DAY_OF_YEAR in " + era + ", year: " + year);
                assertEquals(jd.range(ChronoField.DAY_OF_YEAR).getMinimum(), cal.getActualMinimum(Calendar.DAY_OF_YEAR),
                        "different minimum for DAY_OF_YEAR in " + era + ",  year: " + year);
            }
        }

    }

}
