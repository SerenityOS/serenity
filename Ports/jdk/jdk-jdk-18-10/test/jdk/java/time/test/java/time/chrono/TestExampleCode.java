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
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.ThaiBuddhistDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test case verify that the example code in the package-info.java compiles
 * and runs.
 */
public class TestExampleCode {

    @Test
    public void test_chronoPackageExample() {
        // Print the Thai Buddhist date
        ChronoLocalDate now1 = Chronology.of("ThaiBuddhist").dateNow();
        int day = now1.get(ChronoField.DAY_OF_MONTH);
        int dow = now1.get(ChronoField.DAY_OF_WEEK);
        int month = now1.get(ChronoField.MONTH_OF_YEAR);
        int year = now1.get(ChronoField.YEAR);
        System.out.printf("  Today is %s %s %d-%s-%d%n", now1.getChronology().getId(),
                dow, day, month, year);

        // Enumerate the list of available calendars and print today for each
        Set<Chronology> chronos = Chronology.getAvailableChronologies();
        for (Chronology chrono : chronos) {
            ChronoLocalDate date = chrono.dateNow();
            System.out.printf("   %20s: %s%n", chrono.getId(), date.toString());
        }

        // Print today's date and the last day of the year for the Thai Buddhist Calendar.
        ChronoLocalDate first = now1
                .with(ChronoField.DAY_OF_MONTH, 1)
                .with(ChronoField.MONTH_OF_YEAR, 1);
        ChronoLocalDate last = first
                .plus(1, ChronoUnit.YEARS)
                .minus(1, ChronoUnit.DAYS);
        System.out.printf("  %s: 1st of year: %s; end of year: %s%n", last.getChronology().getId(),
                first, last);
    }

    //-----------------------------------------------------------------------
    // Data provider for Hijrah Type names
    //-----------------------------------------------------------------------
    @DataProvider(name = "HijrahTypeNames")
    Object[][] data_of_ummalqura() {
        return new Object[][]{
            { "Hijrah-umalqura", "islamic-umalqura"},
        };
    }

    @Test(dataProvider= "HijrahTypeNames")
    public void test_HijrahTypeViaLocale(String calendarId, String calendarType) {
        Locale.Builder builder = new Locale.Builder();
        builder.setLanguage("en").setRegion("US");
        builder.setUnicodeLocaleKeyword("ca", calendarType);
        Locale locale = builder.build();
        Chronology chrono = Chronology.ofLocale(locale);
        System.out.printf(" Locale language tag: %s, Chronology ID: %s, type: %s%n",
                locale.toLanguageTag(), chrono, chrono.getCalendarType());
        Chronology expected = Chronology.of(calendarId);
        assertEquals(chrono, expected, "Expected chronology not found");
    }

    @Test
    public void test_calendarPackageExample() {

        // Enumerate the list of available calendars and print today for each
        Set<Chronology> chronos = Chronology.getAvailableChronologies();
        for (Chronology chrono : chronos) {
            ChronoLocalDate date = chrono.dateNow();
            System.out.printf("   %20s: %s%n", chrono.getId(), date.toString());
        }

        // Print the Thai Buddhist date
        ThaiBuddhistDate now1 = ThaiBuddhistDate.now();
        int day = now1.get(ChronoField.DAY_OF_MONTH);
        int dow = now1.get(ChronoField.DAY_OF_WEEK);
        int month = now1.get(ChronoField.MONTH_OF_YEAR);
        int year = now1.get(ChronoField.YEAR);
        System.out.printf("  Today is %s %s %d-%s-%d%n", now1.getChronology().getId(),
                dow, day, month, year);

        // Print today's date and the last day of the year for the Thai Buddhist Calendar.
        ThaiBuddhistDate first = now1
                .with(ChronoField.DAY_OF_MONTH, 1)
                .with(ChronoField.MONTH_OF_YEAR, 1);
        ThaiBuddhistDate last = first
                .plus(1, ChronoUnit.YEARS)
                .minus(1, ChronoUnit.DAYS);
        System.out.printf("  %s: 1st of year: %s; end of year: %s%n", last.getChronology().getId(),
                first, last);
    }

    void HijrahExample1() {
        HijrahDate hd2 = HijrahChronology.INSTANCE.date(1200, 1, 1);

        ChronoLocalDateTime<HijrahDate> hdt = hd2.atTime(LocalTime.MIDNIGHT);
        ChronoZonedDateTime<HijrahDate> zhdt = hdt.atZone(ZoneId.of("GMT"));
        HijrahDate hd3 = zhdt.toLocalDate();
        ChronoLocalDateTime<HijrahDate> hdt2 = zhdt.toLocalDateTime();
        HijrahDate hd4 = hdt2.toLocalDate();

        HijrahDate hd5 = next(hd2);
    }

    void test_unknownChronologyWithDateTime() {
        ChronoLocalDate date = LocalDate.now();
        ChronoLocalDateTime<?> cldt = date.atTime(LocalTime.NOON);
        ChronoLocalDate ld = cldt.toLocalDate();
        ChronoLocalDateTime<?> noonTomorrow = tomorrowNoon(ld);
    }

    @Test
    public void test_library() {
        HijrahDate date = HijrahDate.now();
        HijrahDate next = next(date);
        ChronoLocalDateTime<HijrahDate> noonTomorrow = tomorrowNoon(date);
        HijrahDate hd3 = noonTomorrow.toLocalDate();
        System.out.printf("  now: %s, noon tomorrow: %s%n", date, noonTomorrow);
    }

    /**
     * Simple function based on a date, returning a ChronoDate of the same type.
     * @param <D> a parameterized ChronoLocalDate
     * @param date a specific date extending ChronoLocalDate
     * @return a new date in the same chronology.
     */
    @SuppressWarnings("unchecked")
    private <D extends ChronoLocalDate> D next(D date) {
        return (D) date.plus(1, ChronoUnit.DAYS);
    }

    /**
     * Simple function based on a date, returning a ChronoLocalDateTime of the
     * same chronology.
     * @param <D> a parameterized ChronoLocalDate
     * @param date a specific date extending ChronoLocalDate
     * @return a [@code ChronoLocalDateTime<D>} using the change chronology.
     */
    @SuppressWarnings("unchecked")
    private <D extends ChronoLocalDate> ChronoLocalDateTime<D> tomorrowNoon(D date) {
        return (ChronoLocalDateTime<D>) date.plus(1, ChronoUnit.DAYS).atTime(LocalTime.of(12, 0));
    }
}
