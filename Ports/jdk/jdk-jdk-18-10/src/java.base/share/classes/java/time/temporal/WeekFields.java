/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2011-2012, Stephen Colebourne & Michael Nascimento Santos
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
package java.time.temporal;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.util.locale.provider.CalendarDataUtility;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleResources;

/**
 * Localized definitions of the day-of-week, week-of-month and week-of-year fields.
 * <p>
 * A standard week is seven days long, but cultures have different definitions for some
 * other aspects of a week. This class represents the definition of the week, for the
 * purpose of providing {@link TemporalField} instances.
 * <p>
 * WeekFields provides five fields,
 * {@link #dayOfWeek()}, {@link #weekOfMonth()}, {@link #weekOfYear()},
 * {@link #weekOfWeekBasedYear()}, and {@link #weekBasedYear()}
 * that provide access to the values from any {@linkplain Temporal temporal object}.
 * <p>
 * The computations for day-of-week, week-of-month, and week-of-year are based
 * on the  {@linkplain ChronoField#YEAR proleptic-year},
 * {@linkplain ChronoField#MONTH_OF_YEAR month-of-year},
 * {@linkplain ChronoField#DAY_OF_MONTH day-of-month}, and
 * {@linkplain ChronoField#DAY_OF_WEEK ISO day-of-week} which are based on the
 * {@linkplain ChronoField#EPOCH_DAY epoch-day} and the chronology.
 * The values may not be aligned with the {@linkplain ChronoField#YEAR_OF_ERA year-of-Era}
 * depending on the Chronology.
 * <p>A week is defined by:
 * <ul>
 * <li>The first day-of-week.
 * For example, the ISO-8601 standard considers Monday to be the first day-of-week.
 * <li>The minimal number of days in the first week.
 * For example, the ISO-8601 standard counts the first week as needing at least 4 days.
 * </ul>
 * Together these two values allow a year or month to be divided into weeks.
 *
 * <h2>Week of Month</h2>
 * One field is used: week-of-month.
 * The calculation ensures that weeks never overlap a month boundary.
 * The month is divided into periods where each period starts on the defined first day-of-week.
 * The earliest period is referred to as week 0 if it has less than the minimal number of days
 * and week 1 if it has at least the minimal number of days.
 *
 * <table class=striped style="text-align: left">
 * <caption>Examples of WeekFields</caption>
 * <thead>
 * <tr><th scope="col">Date</th><th scope="col">Day-of-week</th>
 *  <th scope="col">First day: Monday<br>Minimal days: 4</th><th scope="col">First day: Monday<br>Minimal days: 5</th></tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row">2008-12-31</th><td>Wednesday</td>
 *  <td>Week 5 of December 2008</td><td>Week 5 of December 2008</td></tr>
 * <tr><th scope="row">2009-01-01</th><td>Thursday</td>
 *  <td>Week 1 of January 2009</td><td>Week 0 of January 2009</td></tr>
 * <tr><th scope="row">2009-01-04</th><td>Sunday</td>
 *  <td>Week 1 of January 2009</td><td>Week 0 of January 2009</td></tr>
 * <tr><th scope="row">2009-01-05</th><td>Monday</td>
 *  <td>Week 2 of January 2009</td><td>Week 1 of January 2009</td></tr>
 * </tbody>
 * </table>
 *
 * <h2>Week of Year</h2>
 * One field is used: week-of-year.
 * The calculation ensures that weeks never overlap a year boundary.
 * The year is divided into periods where each period starts on the defined first day-of-week.
 * The earliest period is referred to as week 0 if it has less than the minimal number of days
 * and week 1 if it has at least the minimal number of days.
 *
 * <h2>Week Based Year</h2>
 * Two fields are used for week-based-year, one for the
 * {@link #weekOfWeekBasedYear() week-of-week-based-year} and one for
 * {@link #weekBasedYear() week-based-year}.  In a week-based-year, each week
 * belongs to only a single year.  Week 1 of a year is the first week that
 * starts on the first day-of-week and has at least the minimum number of days.
 * The first and last weeks of a year may contain days from the
 * previous calendar year or next calendar year respectively.
 *
 * <table class=striped style="text-align: left;">
 * <caption>Examples of WeekFields for week-based-year</caption>
 * <thead>
 * <tr><th scope="col">Date</th><th scope="col">Day-of-week</th>
 *  <th scope="col">First day: Monday<br>Minimal days: 4</th><th scope="col">First day: Monday<br>Minimal days: 5</th></tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row">2008-12-31</th><td>Wednesday</td>
 *  <td>Week 1 of 2009</td><td>Week 53 of 2008</td></tr>
 * <tr><th scope="row">2009-01-01</th><td>Thursday</td>
 *  <td>Week 1 of 2009</td><td>Week 53 of 2008</td></tr>
 * <tr><th scope="row">2009-01-04</th><td>Sunday</td>
 *  <td>Week 1 of 2009</td><td>Week 53 of 2008</td></tr>
 * <tr><th scope="row">2009-01-05</th><td>Monday</td>
 *  <td>Week 2 of 2009</td><td>Week 1 of 2009</td></tr>
 * </tbody>
 * </table>
 *
 * @implSpec
 * This class is immutable and thread-safe.
 *
 * @since 1.8
 */
public final class WeekFields implements Serializable {
    // implementation notes
    // querying week-of-month or week-of-year should return the week value bound within the month/year
    // however, setting the week value should be lenient (use plus/minus weeks)
    // allow week-of-month outer range [0 to 6]
    // allow week-of-year outer range [0 to 54]
    // this is because callers shouldn't be expected to know the details of validity

    /**
     * The cache of rules by firstDayOfWeek plus minimalDays.
     * Initialized first to be available for definition of ISO, etc.
     */
    private static final ConcurrentMap<String, WeekFields> CACHE = new ConcurrentHashMap<>(4, 0.75f, 2);

    /**
     * The ISO-8601 definition, where a week starts on Monday and the first week
     * has a minimum of 4 days.
     * <p>
     * The ISO-8601 standard defines a calendar system based on weeks.
     * It uses the week-based-year and week-of-week-based-year concepts to split
     * up the passage of days instead of the standard year/month/day.
     * <p>
     * Note that the first week may start in the previous calendar year.
     * Note also that the first few days of a calendar year may be in the
     * week-based-year corresponding to the previous calendar year.
     */
    public static final WeekFields ISO = WeekFields.of(DayOfWeek.MONDAY, 4);

    /**
     * The common definition of a week that starts on Sunday and the first week
     * has a minimum of 1 day.
     * <p>
     * Defined as starting on Sunday and with a minimum of 1 day in the month.
     * This week definition is in use in the US and other European countries.
     */
    public static final WeekFields SUNDAY_START = WeekFields.of(DayOfWeek.SUNDAY, 1);

    /**
     * The unit that represents week-based-years for the purpose of addition and subtraction.
     * <p>
     * This allows a number of week-based-years to be added to, or subtracted from, a date.
     * The unit is equal to either 52 or 53 weeks.
     * The estimated duration of a week-based-year is the same as that of a standard ISO
     * year at {@code 365.2425 Days}.
     * <p>
     * The rules for addition add the number of week-based-years to the existing value
     * for the week-based-year field retaining the week-of-week-based-year
     * and day-of-week, unless the week number it too large for the target year.
     * In that case, the week is set to the last week of the year
     * with the same day-of-week.
     * <p>
     * This unit is an immutable and thread-safe singleton.
     */
    public static final TemporalUnit WEEK_BASED_YEARS = IsoFields.WEEK_BASED_YEARS;

    /**
     * Serialization version.
     */
    @java.io.Serial
    private static final long serialVersionUID = -1177360819670808121L;

    /**
     * The first day-of-week.
     */
    private final DayOfWeek firstDayOfWeek;
    /**
     * The minimal number of days in the first week.
     */
    private final int minimalDays;
    /**
     * The field used to access the computed DayOfWeek.
     */
    private final transient TemporalField dayOfWeek = ComputedDayOfField.ofDayOfWeekField(this);
    /**
     * The field used to access the computed WeekOfMonth.
     */
    private final transient TemporalField weekOfMonth = ComputedDayOfField.ofWeekOfMonthField(this);
    /**
     * The field used to access the computed WeekOfYear.
     */
    private final transient TemporalField weekOfYear = ComputedDayOfField.ofWeekOfYearField(this);
    /**
     * The field that represents the week-of-week-based-year.
     * <p>
     * This field allows the week of the week-based-year value to be queried and set.
     * <p>
     * This unit is an immutable and thread-safe singleton.
     */
    private final transient TemporalField weekOfWeekBasedYear = ComputedDayOfField.ofWeekOfWeekBasedYearField(this);
    /**
     * The field that represents the week-based-year.
     * <p>
     * This field allows the week-based-year value to be queried and set.
     * <p>
     * This unit is an immutable and thread-safe singleton.
     */
    private final transient TemporalField weekBasedYear = ComputedDayOfField.ofWeekBasedYearField(this);

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code WeekFields} appropriate for a locale.
     * <p>
     * This will look up appropriate values from the provider of localization data.
     * If the locale contains "fw" (First day of week) and/or "rg"
     * (Region Override) <a href="../../util/Locale.html#def_locale_extension">
     * Unicode extensions</a>, returned instance will reflect the values specified with
     * those extensions. If both "fw" and "rg" are specified, the value from
     * the "fw" extension supersedes the implicit one from the "rg" extension.
     *
     * @param locale  the locale to use, not null
     * @return the week-definition, not null
     */
    public static WeekFields of(Locale locale) {
        Objects.requireNonNull(locale, "locale");

        int calDow = CalendarDataUtility.retrieveFirstDayOfWeek(locale);
        DayOfWeek dow = DayOfWeek.SUNDAY.plus(calDow - 1);
        int minDays = CalendarDataUtility.retrieveMinimalDaysInFirstWeek(locale);
        return WeekFields.of(dow, minDays);
    }

    /**
     * Obtains an instance of {@code WeekFields} from the first day-of-week and minimal days.
     * <p>
     * The first day-of-week defines the ISO {@code DayOfWeek} that is day 1 of the week.
     * The minimal number of days in the first week defines how many days must be present
     * in a month or year, starting from the first day-of-week, before the week is counted
     * as the first week. A value of 1 will count the first day of the month or year as part
     * of the first week, whereas a value of 7 will require the whole seven days to be in
     * the new month or year.
     * <p>
     * WeekFields instances are singletons; for each unique combination
     * of {@code firstDayOfWeek} and {@code minimalDaysInFirstWeek}
     * the same instance will be returned.
     *
     * @param firstDayOfWeek  the first day of the week, not null
     * @param minimalDaysInFirstWeek  the minimal number of days in the first week, from 1 to 7
     * @return the week-definition, not null
     * @throws IllegalArgumentException if the minimal days value is less than one
     *      or greater than 7
     */
    public static WeekFields of(DayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {
        String key = firstDayOfWeek.toString() + minimalDaysInFirstWeek;
        WeekFields rules = CACHE.get(key);
        if (rules == null) {
            rules = new WeekFields(firstDayOfWeek, minimalDaysInFirstWeek);
            CACHE.putIfAbsent(key, rules);
            rules = CACHE.get(key);
        }
        return rules;
    }

    //-----------------------------------------------------------------------
    /**
     * Creates an instance of the definition.
     *
     * @param firstDayOfWeek  the first day of the week, not null
     * @param minimalDaysInFirstWeek  the minimal number of days in the first week, from 1 to 7
     * @throws IllegalArgumentException if the minimal days value is invalid
     */
    private WeekFields(DayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {
        Objects.requireNonNull(firstDayOfWeek, "firstDayOfWeek");
        if (minimalDaysInFirstWeek < 1 || minimalDaysInFirstWeek > 7) {
            throw new IllegalArgumentException("Minimal number of days is invalid");
        }
        this.firstDayOfWeek = firstDayOfWeek;
        this.minimalDays = minimalDaysInFirstWeek;
    }

    //-----------------------------------------------------------------------
    /**
     * Restore the state of a WeekFields from the stream.
     * Check that the values are valid.
     *
     * @param s the stream to read
     * @throws IOException if an I/O error occurs
     * @throws InvalidObjectException if the serialized object has an invalid
     *     value for firstDayOfWeek or minimalDays.
     * @throws ClassNotFoundException if a class cannot be resolved
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException, InvalidObjectException
    {
        s.defaultReadObject();
        if (firstDayOfWeek == null) {
            throw new InvalidObjectException("firstDayOfWeek is null");
        }

        if (minimalDays < 1 || minimalDays > 7) {
            throw new InvalidObjectException("Minimal number of days is invalid");
        }
    }

    /**
     * Return the singleton WeekFields associated with the
     * {@code firstDayOfWeek} and {@code minimalDays}.
     * @return the singleton WeekFields for the firstDayOfWeek and minimalDays.
     * @throws InvalidObjectException if the serialized object has invalid
     *     values for firstDayOfWeek or minimalDays.
     */
    @java.io.Serial
    private Object readResolve() throws InvalidObjectException {
        try {
            return WeekFields.of(firstDayOfWeek, minimalDays);
        } catch (IllegalArgumentException iae) {
            throw new InvalidObjectException("Invalid serialized WeekFields: " + iae.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the first day-of-week.
     * <p>
     * The first day-of-week varies by culture.
     * For example, the US uses Sunday, while France and the ISO-8601 standard use Monday.
     * This method returns the first day using the standard {@code DayOfWeek} enum.
     *
     * @return the first day-of-week, not null
     */
    public DayOfWeek getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    /**
     * Gets the minimal number of days in the first week.
     * <p>
     * The number of days considered to define the first week of a month or year
     * varies by culture.
     * For example, the ISO-8601 requires 4 days (more than half a week) to
     * be present before counting the first week.
     *
     * @return the minimal number of days in the first week of a month or year, from 1 to 7
     */
    public int getMinimalDaysInFirstWeek() {
        return minimalDays;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a field to access the day of week based on this {@code WeekFields}.
     * <p>
     * This is similar to {@link ChronoField#DAY_OF_WEEK} but uses values for
     * the day-of-week based on this {@code WeekFields}.
     * The days are numbered from 1 to 7 where the
     * {@link #getFirstDayOfWeek() first day-of-week} is assigned the value 1.
     * <p>
     * For example, if the first day-of-week is Sunday, then that will have the
     * value 1, with other days ranging from Monday as 2 to Saturday as 7.
     * <p>
     * In the resolving phase of parsing, a localized day-of-week will be converted
     * to a standardized {@code ChronoField} day-of-week.
     * The day-of-week must be in the valid range 1 to 7.
     * Other fields in this class build dates using the standardized day-of-week.
     *
     * @return a field providing access to the day-of-week with localized numbering, not null
     */
    public TemporalField dayOfWeek() {
        return dayOfWeek;
    }

    /**
     * Returns a field to access the week of month based on this {@code WeekFields}.
     * <p>
     * This represents the concept of the count of weeks within the month where weeks
     * start on a fixed day-of-week, such as Monday.
     * This field is typically used with {@link WeekFields#dayOfWeek()}.
     * <p>
     * Week one (1) is the week starting on the {@link WeekFields#getFirstDayOfWeek}
     * where there are at least {@link WeekFields#getMinimalDaysInFirstWeek()} days in the month.
     * Thus, week one may start up to {@code minDays} days before the start of the month.
     * If the first week starts after the start of the month then the period before is week zero (0).
     * <p>
     * For example:<br>
     * - if the 1st day of the month is a Monday, week one starts on the 1st and there is no week zero<br>
     * - if the 2nd day of the month is a Monday, week one starts on the 2nd and the 1st is in week zero<br>
     * - if the 4th day of the month is a Monday, week one starts on the 4th and the 1st to 3rd is in week zero<br>
     * - if the 5th day of the month is a Monday, week two starts on the 5th and the 1st to 4th is in week one<br>
     * <p>
     * This field can be used with any calendar system.
     * <p>
     * In the resolving phase of parsing, a date can be created from a year,
     * week-of-month, month-of-year and day-of-week.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all four fields are
     * validated against their range of valid values. The week-of-month field
     * is validated to ensure that the resulting month is the month requested.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all four fields are
     * validated against their range of valid values. The week-of-month field
     * is validated from 0 to 6, meaning that the resulting date can be in a
     * different month to that specified.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, the year and day-of-week
     * are validated against the range of valid values. The resulting date is calculated
     * equivalent to the following four stage approach.
     * First, create a date on the first day of the first week of January in the requested year.
     * Then take the month-of-year, subtract one, and add the amount in months to the date.
     * Then take the week-of-month, subtract one, and add the amount in weeks to the date.
     * Finally, adjust to the correct day-of-week within the localized week.
     *
     * @return a field providing access to the week-of-month, not null
     */
    public TemporalField weekOfMonth() {
        return weekOfMonth;
    }

    /**
     * Returns a field to access the week of year based on this {@code WeekFields}.
     * <p>
     * This represents the concept of the count of weeks within the year where weeks
     * start on a fixed day-of-week, such as Monday.
     * This field is typically used with {@link WeekFields#dayOfWeek()}.
     * <p>
     * Week one(1) is the week starting on the {@link WeekFields#getFirstDayOfWeek}
     * where there are at least {@link WeekFields#getMinimalDaysInFirstWeek()} days in the year.
     * Thus, week one may start up to {@code minDays} days before the start of the year.
     * If the first week starts after the start of the year then the period before is week zero (0).
     * <p>
     * For example:<br>
     * - if the 1st day of the year is a Monday, week one starts on the 1st and there is no week zero<br>
     * - if the 2nd day of the year is a Monday, week one starts on the 2nd and the 1st is in week zero<br>
     * - if the 4th day of the year is a Monday, week one starts on the 4th and the 1st to 3rd is in week zero<br>
     * - if the 5th day of the year is a Monday, week two starts on the 5th and the 1st to 4th is in week one<br>
     * <p>
     * This field can be used with any calendar system.
     * <p>
     * In the resolving phase of parsing, a date can be created from a year,
     * week-of-year and day-of-week.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all three fields are
     * validated against their range of valid values. The week-of-year field
     * is validated to ensure that the resulting year is the year requested.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all three fields are
     * validated against their range of valid values. The week-of-year field
     * is validated from 0 to 54, meaning that the resulting date can be in a
     * different year to that specified.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, the year and day-of-week
     * are validated against the range of valid values. The resulting date is calculated
     * equivalent to the following three stage approach.
     * First, create a date on the first day of the first week in the requested year.
     * Then take the week-of-year, subtract one, and add the amount in weeks to the date.
     * Finally, adjust to the correct day-of-week within the localized week.
     *
     * @return a field providing access to the week-of-year, not null
     */
    public TemporalField weekOfYear() {
        return weekOfYear;
    }

    /**
     * Returns a field to access the week of a week-based-year based on this {@code WeekFields}.
     * <p>
     * This represents the concept of the count of weeks within the year where weeks
     * start on a fixed day-of-week, such as Monday and each week belongs to exactly one year.
     * This field is typically used with {@link WeekFields#dayOfWeek()} and
     * {@link WeekFields#weekBasedYear()}.
     * <p>
     * Week one(1) is the week starting on the {@link WeekFields#getFirstDayOfWeek}
     * where there are at least {@link WeekFields#getMinimalDaysInFirstWeek()} days in the year.
     * If the first week starts after the start of the year then the period before
     * is in the last week of the previous year.
     * <p>
     * For example:<br>
     * - if the 1st day of the year is a Monday, week one starts on the 1st<br>
     * - if the 2nd day of the year is a Monday, week one starts on the 2nd and
     *   the 1st is in the last week of the previous year<br>
     * - if the 4th day of the year is a Monday, week one starts on the 4th and
     *   the 1st to 3rd is in the last week of the previous year<br>
     * - if the 5th day of the year is a Monday, week two starts on the 5th and
     *   the 1st to 4th is in week one<br>
     * <p>
     * This field can be used with any calendar system.
     * <p>
     * In the resolving phase of parsing, a date can be created from a week-based-year,
     * week-of-year and day-of-week.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all three fields are
     * validated against their range of valid values. The week-of-year field
     * is validated to ensure that the resulting week-based-year is the
     * week-based-year requested.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all three fields are
     * validated against their range of valid values. The week-of-week-based-year field
     * is validated from 1 to 53, meaning that the resulting date can be in the
     * following week-based-year to that specified.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, the year and day-of-week
     * are validated against the range of valid values. The resulting date is calculated
     * equivalent to the following three stage approach.
     * First, create a date on the first day of the first week in the requested week-based-year.
     * Then take the week-of-week-based-year, subtract one, and add the amount in weeks to the date.
     * Finally, adjust to the correct day-of-week within the localized week.
     *
     * @return a field providing access to the week-of-week-based-year, not null
     */
    public TemporalField weekOfWeekBasedYear() {
        return weekOfWeekBasedYear;
    }

    /**
     * Returns a field to access the year of a week-based-year based on this {@code WeekFields}.
     * <p>
     * This represents the concept of the year where weeks start on a fixed day-of-week,
     * such as Monday and each week belongs to exactly one year.
     * This field is typically used with {@link WeekFields#dayOfWeek()} and
     * {@link WeekFields#weekOfWeekBasedYear()}.
     * <p>
     * Week one(1) is the week starting on the {@link WeekFields#getFirstDayOfWeek}
     * where there are at least {@link WeekFields#getMinimalDaysInFirstWeek()} days in the year.
     * Thus, week one may start before the start of the year.
     * If the first week starts after the start of the year then the period before
     * is in the last week of the previous year.
     * <p>
     * This field can be used with any calendar system.
     * <p>
     * In the resolving phase of parsing, a date can be created from a week-based-year,
     * week-of-year and day-of-week.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all three fields are
     * validated against their range of valid values. The week-of-year field
     * is validated to ensure that the resulting week-based-year is the
     * week-based-year requested.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all three fields are
     * validated against their range of valid values. The week-of-week-based-year field
     * is validated from 1 to 53, meaning that the resulting date can be in the
     * following week-based-year to that specified.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, the year and day-of-week
     * are validated against the range of valid values. The resulting date is calculated
     * equivalent to the following three stage approach.
     * First, create a date on the first day of the first week in the requested week-based-year.
     * Then take the week-of-week-based-year, subtract one, and add the amount in weeks to the date.
     * Finally, adjust to the correct day-of-week within the localized week.
     *
     * @return a field providing access to the week-based-year, not null
     */
    public TemporalField weekBasedYear() {
        return weekBasedYear;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this {@code WeekFields} is equal to the specified object.
     * <p>
     * The comparison is based on the entire state of the rules, which is
     * the first day-of-week and minimal days.
     *
     * @param object  the other rules to compare to, null returns false
     * @return true if this is equal to the specified rules
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof WeekFields) {
            return hashCode() == object.hashCode();
        }
        return false;
    }

    /**
     * A hash code for this {@code WeekFields}.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return firstDayOfWeek.ordinal() * 7 + minimalDays;
    }

    //-----------------------------------------------------------------------
    /**
     * A string representation of this {@code WeekFields} instance.
     *
     * @return the string representation, not null
     */
    @Override
    public String toString() {
        return "WeekFields[" + firstDayOfWeek + ',' + minimalDays + ']';
    }

    //-----------------------------------------------------------------------
    /**
     * Field type that computes DayOfWeek, WeekOfMonth, and WeekOfYear
     * based on a WeekFields.
     * A separate Field instance is required for each different WeekFields;
     * combination of start of week and minimum number of days.
     * Constructors are provided to create fields for DayOfWeek, WeekOfMonth,
     * and WeekOfYear.
     */
    static class ComputedDayOfField implements TemporalField {

        /**
         * Returns a field to access the day of week,
         * computed based on a WeekFields.
         * <p>
         * The WeekDefintion of the first day of the week is used with
         * the ISO DAY_OF_WEEK field to compute week boundaries.
         */
        static ComputedDayOfField ofDayOfWeekField(WeekFields weekDef) {
            return new ComputedDayOfField("DayOfWeek", weekDef, DAYS, WEEKS, DAY_OF_WEEK_RANGE);
        }

        /**
         * Returns a field to access the week of month,
         * computed based on a WeekFields.
         * @see WeekFields#weekOfMonth()
         */
        static ComputedDayOfField ofWeekOfMonthField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekOfMonth", weekDef, WEEKS, MONTHS, WEEK_OF_MONTH_RANGE);
        }

        /**
         * Returns a field to access the week of year,
         * computed based on a WeekFields.
         * @see WeekFields#weekOfYear()
         */
        static ComputedDayOfField ofWeekOfYearField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekOfYear", weekDef, WEEKS, YEARS, WEEK_OF_YEAR_RANGE);
        }

        /**
         * Returns a field to access the week of week-based-year,
         * computed based on a WeekFields.
         * @see WeekFields#weekOfWeekBasedYear()
         */
        static ComputedDayOfField ofWeekOfWeekBasedYearField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekOfWeekBasedYear", weekDef, WEEKS, IsoFields.WEEK_BASED_YEARS, WEEK_OF_WEEK_BASED_YEAR_RANGE);
        }

        /**
         * Returns a field to access the week of week-based-year,
         * computed based on a WeekFields.
         * @see WeekFields#weekBasedYear()
         */
        static ComputedDayOfField ofWeekBasedYearField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekBasedYear", weekDef, IsoFields.WEEK_BASED_YEARS, FOREVER, ChronoField.YEAR.range());
        }

        /**
         * Return a new week-based-year date of the Chronology, year, week-of-year,
         * and dow of week.
         * @param chrono The chronology of the new date
         * @param yowby the year of the week-based-year
         * @param wowby the week of the week-based-year
         * @param dow the day of the week
         * @return a ChronoLocalDate for the requested year, week of year, and day of week
         */
        private ChronoLocalDate ofWeekBasedYear(Chronology chrono,
                int yowby, int wowby, int dow) {
            ChronoLocalDate date = chrono.date(yowby, 1, 1);
            int ldow = localizedDayOfWeek(date);
            int offset = startOfWeekOffset(1, ldow);

            // Clamp the week of year to keep it in the same year
            int yearLen = date.lengthOfYear();
            int newYearWeek = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());
            wowby = Math.min(wowby, newYearWeek - 1);

            int days = -offset + (dow - 1) + (wowby - 1) * 7;
            return date.plus(days, DAYS);
        }

        private final String name;
        private final WeekFields weekDef;
        private final TemporalUnit baseUnit;
        private final TemporalUnit rangeUnit;
        private final ValueRange range;

        private ComputedDayOfField(String name, WeekFields weekDef, TemporalUnit baseUnit, TemporalUnit rangeUnit, ValueRange range) {
            this.name = name;
            this.weekDef = weekDef;
            this.baseUnit = baseUnit;
            this.rangeUnit = rangeUnit;
            this.range = range;
        }

        private static final ValueRange DAY_OF_WEEK_RANGE = ValueRange.of(1, 7);
        private static final ValueRange WEEK_OF_MONTH_RANGE = ValueRange.of(0, 1, 4, 6);
        private static final ValueRange WEEK_OF_YEAR_RANGE = ValueRange.of(0, 1, 52, 54);
        private static final ValueRange WEEK_OF_WEEK_BASED_YEAR_RANGE = ValueRange.of(1, 52, 53);

        @Override
        public long getFrom(TemporalAccessor temporal) {
            if (rangeUnit == WEEKS) {  // day-of-week
                return localizedDayOfWeek(temporal);
            } else if (rangeUnit == MONTHS) {  // week-of-month
                return localizedWeekOfMonth(temporal);
            } else if (rangeUnit == YEARS) {  // week-of-year
                return localizedWeekOfYear(temporal);
            } else if (rangeUnit == WEEK_BASED_YEARS) {
                return localizedWeekOfWeekBasedYear(temporal);
            } else if (rangeUnit == FOREVER) {
                return localizedWeekBasedYear(temporal);
            } else {
                throw new IllegalStateException("unreachable, rangeUnit: " + rangeUnit + ", this: " + this);
            }
        }

        private int localizedDayOfWeek(TemporalAccessor temporal) {
            int sow = weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(DAY_OF_WEEK);
            return Math.floorMod(isoDow - sow, 7) + 1;
        }

        private int localizedDayOfWeek(int isoDow) {
            int sow = weekDef.getFirstDayOfWeek().getValue();
            return Math.floorMod(isoDow - sow, 7) + 1;
        }

        private long localizedWeekOfMonth(TemporalAccessor temporal) {
            int dow = localizedDayOfWeek(temporal);
            int dom = temporal.get(DAY_OF_MONTH);
            int offset = startOfWeekOffset(dom, dow);
            return computeWeek(offset, dom);
        }

        private long localizedWeekOfYear(TemporalAccessor temporal) {
            int dow = localizedDayOfWeek(temporal);
            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            return computeWeek(offset, doy);
        }

        /**
         * Returns the year of week-based-year for the temporal.
         * The year can be the previous year, the current year, or the next year.
         * @param temporal a date of any chronology, not null
         * @return the year of week-based-year for the date
         */
        private int localizedWeekBasedYear(TemporalAccessor temporal) {
            int dow = localizedDayOfWeek(temporal);
            int year = temporal.get(YEAR);
            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            int week = computeWeek(offset, doy);
            if (week == 0) {
                // Day is in end of week of previous year; return the previous year
                return year - 1;
            } else {
                // If getting close to end of year, use higher precision logic
                // Check if date of year is in partial week associated with next year
                ValueRange dayRange = temporal.range(DAY_OF_YEAR);
                int yearLen = (int)dayRange.getMaximum();
                int newYearWeek = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());
                if (week >= newYearWeek) {
                    return year + 1;
                }
            }
            return year;
        }

        /**
         * Returns the week of week-based-year for the temporal.
         * The week can be part of the previous year, the current year,
         * or the next year depending on the week start and minimum number
         * of days.
         * @param temporal  a date of any chronology
         * @return the week of the year
         * @see #localizedWeekBasedYear(java.time.temporal.TemporalAccessor)
         */
        private int localizedWeekOfWeekBasedYear(TemporalAccessor temporal) {
            int dow = localizedDayOfWeek(temporal);
            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            int week = computeWeek(offset, doy);
            if (week == 0) {
                // Day is in end of week of previous year
                // Recompute from the last day of the previous year
                ChronoLocalDate date = Chronology.from(temporal).date(temporal);
                date = date.minus(doy, DAYS);   // Back down into previous year
                return localizedWeekOfWeekBasedYear(date);
            } else if (week > 50) {
                // If getting close to end of year, use higher precision logic
                // Check if date of year is in partial week associated with next year
                ValueRange dayRange = temporal.range(DAY_OF_YEAR);
                int yearLen = (int)dayRange.getMaximum();
                int newYearWeek = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());
                if (week >= newYearWeek) {
                    // Overlaps with week of following year; reduce to week in following year
                    week = week - newYearWeek + 1;
                }
            }
            return week;
        }

        /**
         * Returns an offset to align week start with a day of month or day of year.
         *
         * @param day  the day; 1 through infinity
         * @param dow  the day of the week of that day; 1 through 7
         * @return  an offset in days to align a day with the start of the first 'full' week
         */
        private int startOfWeekOffset(int day, int dow) {
            // offset of first day corresponding to the day of week in first 7 days (zero origin)
            int weekStart = Math.floorMod(day - dow, 7);
            int offset = -weekStart;
            if (weekStart + 1 > weekDef.getMinimalDaysInFirstWeek()) {
                // The previous week has the minimum days in the current month to be a 'week'
                offset = 7 - weekStart;
            }
            return offset;
        }

        /**
         * Returns the week number computed from the reference day and reference dayOfWeek.
         *
         * @param offset the offset to align a date with the start of week
         *     from {@link #startOfWeekOffset}.
         * @param day  the day for which to compute the week number
         * @return the week number where zero is used for a partial week and 1 for the first full week
         */
        private int computeWeek(int offset, int day) {
            return ((7 + offset + (day - 1)) / 7);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R adjustInto(R temporal, long newValue) {
            // Check the new value and get the old value of the field
            int newVal = range.checkValidIntValue(newValue, this);  // lenient check range
            int currentVal = temporal.get(this);
            if (newVal == currentVal) {
                return temporal;
            }

            if (rangeUnit == FOREVER) {     // replace year of WeekBasedYear
                // Create a new date object with the same chronology,
                // the desired year and the same week and dow.
                int idow = temporal.get(weekDef.dayOfWeek);
                int wowby = temporal.get(weekDef.weekOfWeekBasedYear);
                return (R) ofWeekBasedYear(Chronology.from(temporal), (int)newValue, wowby, idow);
            } else {
                // Compute the difference and add that using the base unit of the field
                return (R) temporal.plus(newVal - currentVal, baseUnit);
            }
        }

        @Override
        public ChronoLocalDate resolve(
                Map<TemporalField, Long> fieldValues, TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
            final long value = fieldValues.get(this);
            final int newValue = Math.toIntExact(value);  // broad limit makes overflow checking lighter
            // first convert localized day-of-week to ISO day-of-week
            // doing this first handles case where both ISO and localized were parsed and might mismatch
            // day-of-week is always strict as two different day-of-week values makes lenient complex
            if (rangeUnit == WEEKS) {  // day-of-week
                final int checkedValue = range.checkValidIntValue(value, this);  // no leniency as too complex
                final int startDow = weekDef.getFirstDayOfWeek().getValue();
                long isoDow = Math.floorMod((startDow - 1) + (checkedValue - 1), 7) + 1;
                fieldValues.remove(this);
                fieldValues.put(DAY_OF_WEEK, isoDow);
                return null;
            }

            // can only build date if ISO day-of-week is present
            if (fieldValues.containsKey(DAY_OF_WEEK) == false) {
                return null;
            }
            int isoDow = DAY_OF_WEEK.checkValidIntValue(fieldValues.get(DAY_OF_WEEK));
            int dow = localizedDayOfWeek(isoDow);

            // build date
            Chronology chrono = Chronology.from(partialTemporal);
            if (fieldValues.containsKey(YEAR)) {
                int year = YEAR.checkValidIntValue(fieldValues.get(YEAR));  // validate
                if (rangeUnit == MONTHS && fieldValues.containsKey(MONTH_OF_YEAR)) {  // week-of-month
                    long month = fieldValues.get(MONTH_OF_YEAR);  // not validated yet
                    return resolveWoM(fieldValues, chrono, year, month, newValue, dow, resolverStyle);
                }
                if (rangeUnit == YEARS) {  // week-of-year
                    return resolveWoY(fieldValues, chrono, year, newValue, dow, resolverStyle);
                }
            } else if ((rangeUnit == WEEK_BASED_YEARS || rangeUnit == FOREVER) &&
                    fieldValues.containsKey(weekDef.weekBasedYear) &&
                    fieldValues.containsKey(weekDef.weekOfWeekBasedYear)) { // week-of-week-based-year and year-of-week-based-year
                return resolveWBY(fieldValues, chrono, dow, resolverStyle);
            }
            return null;
        }

        private ChronoLocalDate resolveWoM(
                Map<TemporalField, Long> fieldValues, Chronology chrono, int year, long month, long wom, int localDow, ResolverStyle resolverStyle) {
            ChronoLocalDate date;
            if (resolverStyle == ResolverStyle.LENIENT) {
                date = chrono.date(year, 1, 1).plus(Math.subtractExact(month, 1), MONTHS);
                long weeks = Math.subtractExact(wom, localizedWeekOfMonth(date));
                int days = localDow - localizedDayOfWeek(date);  // safe from overflow
                date = date.plus(Math.addExact(Math.multiplyExact(weeks, 7), days), DAYS);
            } else {
                int monthValid = MONTH_OF_YEAR.checkValidIntValue(month);  // validate
                date = chrono.date(year, monthValid, 1);
                int womInt = range.checkValidIntValue(wom, this);  // validate
                int weeks = (int) (womInt - localizedWeekOfMonth(date));  // safe from overflow
                int days = localDow - localizedDayOfWeek(date);  // safe from overflow
                date = date.plus(weeks * 7 + days, DAYS);
                if (resolverStyle == ResolverStyle.STRICT && date.getLong(MONTH_OF_YEAR) != month) {
                    throw new DateTimeException("Strict mode rejected resolved date as it is in a different month");
                }
            }
            fieldValues.remove(this);
            fieldValues.remove(YEAR);
            fieldValues.remove(MONTH_OF_YEAR);
            fieldValues.remove(DAY_OF_WEEK);
            return date;
        }

        private ChronoLocalDate resolveWoY(
                Map<TemporalField, Long> fieldValues, Chronology chrono, int year, long woy, int localDow, ResolverStyle resolverStyle) {
            ChronoLocalDate date = chrono.date(year, 1, 1);
            if (resolverStyle == ResolverStyle.LENIENT) {
                long weeks = Math.subtractExact(woy, localizedWeekOfYear(date));
                int days = localDow - localizedDayOfWeek(date);  // safe from overflow
                date = date.plus(Math.addExact(Math.multiplyExact(weeks, 7), days), DAYS);
            } else {
                int womInt = range.checkValidIntValue(woy, this);  // validate
                int weeks = (int) (womInt - localizedWeekOfYear(date));  // safe from overflow
                int days = localDow - localizedDayOfWeek(date);  // safe from overflow
                date = date.plus(weeks * 7 + days, DAYS);
                if (resolverStyle == ResolverStyle.STRICT && date.getLong(YEAR) != year) {
                    throw new DateTimeException("Strict mode rejected resolved date as it is in a different year");
                }
            }
            fieldValues.remove(this);
            fieldValues.remove(YEAR);
            fieldValues.remove(DAY_OF_WEEK);
            return date;
        }

        private ChronoLocalDate resolveWBY(
                Map<TemporalField, Long> fieldValues, Chronology chrono, int localDow, ResolverStyle resolverStyle) {
            int yowby = weekDef.weekBasedYear.range().checkValidIntValue(
                    fieldValues.get(weekDef.weekBasedYear), weekDef.weekBasedYear);
            ChronoLocalDate date;
            if (resolverStyle == ResolverStyle.LENIENT) {
                date = ofWeekBasedYear(chrono, yowby, 1, localDow);
                long wowby = fieldValues.get(weekDef.weekOfWeekBasedYear);
                long weeks = Math.subtractExact(wowby, 1);
                date = date.plus(weeks, WEEKS);
            } else {
                int wowby = weekDef.weekOfWeekBasedYear.range().checkValidIntValue(
                        fieldValues.get(weekDef.weekOfWeekBasedYear), weekDef.weekOfWeekBasedYear);  // validate
                date = ofWeekBasedYear(chrono, yowby, wowby, localDow);
                if (resolverStyle == ResolverStyle.STRICT && localizedWeekBasedYear(date) != yowby) {
                    throw new DateTimeException("Strict mode rejected resolved date as it is in a different week-based-year");
                }
            }
            fieldValues.remove(this);
            fieldValues.remove(weekDef.weekBasedYear);
            fieldValues.remove(weekDef.weekOfWeekBasedYear);
            fieldValues.remove(DAY_OF_WEEK);
            return date;
        }

        //-----------------------------------------------------------------------
        @Override
        public String getDisplayName(Locale locale) {
            Objects.requireNonNull(locale, "locale");
            if (rangeUnit == YEARS) {  // only have values for week-of-year
                LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased()
                        .getLocaleResources(
                            CalendarDataUtility.findRegionOverride(locale));
                ResourceBundle rb = lr.getJavaTimeFormatData();
                return rb.containsKey("field.week") ? rb.getString("field.week") : name;
            }
            return name;
        }

        @Override
        public TemporalUnit getBaseUnit() {
            return baseUnit;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return rangeUnit;
        }

        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        @Override
        public ValueRange range() {
            return range;
        }

        //-----------------------------------------------------------------------
        @Override
        public boolean isSupportedBy(TemporalAccessor temporal) {
            if (temporal.isSupported(DAY_OF_WEEK)) {
                if (rangeUnit == WEEKS) {  // day-of-week
                    return true;
                } else if (rangeUnit == MONTHS) {  // week-of-month
                    return temporal.isSupported(DAY_OF_MONTH);
                } else if (rangeUnit == YEARS) {  // week-of-year
                    return temporal.isSupported(DAY_OF_YEAR);
                } else if (rangeUnit == WEEK_BASED_YEARS) {
                    return temporal.isSupported(DAY_OF_YEAR);
                } else if (rangeUnit == FOREVER) {
                    return temporal.isSupported(YEAR);
                }
            }
            return false;
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
            if (rangeUnit == ChronoUnit.WEEKS) {  // day-of-week
                return range;
            } else if (rangeUnit == MONTHS) {  // week-of-month
                return rangeByWeek(temporal, DAY_OF_MONTH);
            } else if (rangeUnit == YEARS) {  // week-of-year
                return rangeByWeek(temporal, DAY_OF_YEAR);
            } else if (rangeUnit == WEEK_BASED_YEARS) {
                return rangeWeekOfWeekBasedYear(temporal);
            } else if (rangeUnit == FOREVER) {
                return YEAR.range();
            } else {
                throw new IllegalStateException("unreachable, rangeUnit: " + rangeUnit + ", this: " + this);
            }
        }

        /**
         * Map the field range to a week range
         * @param temporal the temporal
         * @param field the field to get the range of
         * @return the ValueRange with the range adjusted to weeks.
         */
        private ValueRange rangeByWeek(TemporalAccessor temporal, TemporalField field) {
            int dow = localizedDayOfWeek(temporal);
            int offset = startOfWeekOffset(temporal.get(field), dow);
            ValueRange fieldRange = temporal.range(field);
            return ValueRange.of(computeWeek(offset, (int) fieldRange.getMinimum()),
                    computeWeek(offset, (int) fieldRange.getMaximum()));
        }

        /**
         * Map the field range to a week range of a week year.
         * @param temporal  the temporal
         * @return the ValueRange with the range adjusted to weeks.
         */
        private ValueRange rangeWeekOfWeekBasedYear(TemporalAccessor temporal) {
            if (!temporal.isSupported(DAY_OF_YEAR)) {
                return WEEK_OF_YEAR_RANGE;
            }
            int dow = localizedDayOfWeek(temporal);
            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            int week = computeWeek(offset, doy);
            if (week == 0) {
                // Day is in end of week of previous year
                // Recompute from the last day of the previous year
                ChronoLocalDate date = Chronology.from(temporal).date(temporal);
                date = date.minus(doy + 7, DAYS);   // Back down into previous year
                return rangeWeekOfWeekBasedYear(date);
            }
            // Check if day of year is in partial week associated with next year
            ValueRange dayRange = temporal.range(DAY_OF_YEAR);
            int yearLen = (int)dayRange.getMaximum();
            int newYearWeek = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());

            if (week >= newYearWeek) {
                // Overlaps with weeks of following year; recompute from a week in following year
                ChronoLocalDate date = Chronology.from(temporal).date(temporal);
                date = date.plus(yearLen - doy + 1 + 7, ChronoUnit.DAYS);
                return rangeWeekOfWeekBasedYear(date);
            }
            return ValueRange.of(1, newYearWeek-1);
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            return name + "[" + weekDef.toString() + "]";
        }
    }
}
