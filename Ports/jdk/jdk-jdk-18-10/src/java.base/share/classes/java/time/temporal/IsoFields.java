/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import sun.util.locale.provider.CalendarDataUtility;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleResources;

/**
 * Fields and units specific to the ISO-8601 calendar system,
 * including quarter-of-year and week-based-year.
 * <p>
 * This class defines fields and units that are specific to the ISO calendar system.
 *
 * <h2>Quarter of year</h2>
 * The ISO-8601 standard is based on the standard civic 12 month year.
 * This is commonly divided into four quarters, often abbreviated as Q1, Q2, Q3 and Q4.
 * <p>
 * January, February and March are in Q1.
 * April, May and June are in Q2.
 * July, August and September are in Q3.
 * October, November and December are in Q4.
 * <p>
 * The complete date is expressed using three fields:
 * <ul>
 * <li>{@link #DAY_OF_QUARTER DAY_OF_QUARTER} - the day within the quarter, from 1 to 90, 91 or 92
 * <li>{@link #QUARTER_OF_YEAR QUARTER_OF_YEAR} - the quarter within the year, from 1 to 4
 * <li>{@link ChronoField#YEAR YEAR} - the standard ISO year
 * </ul>
 *
 * <h2>Week based years</h2>
 * The ISO-8601 standard was originally intended as a data interchange format,
 * defining a string format for dates and times. However, it also defines an
 * alternate way of expressing the date, based on the concept of week-based-year.
 * <p>
 * The date is expressed using three fields:
 * <ul>
 * <li>{@link ChronoField#DAY_OF_WEEK DAY_OF_WEEK} - the standard field defining the
 *  day-of-week from Monday (1) to Sunday (7)
 * <li>{@link #WEEK_OF_WEEK_BASED_YEAR} - the week within the week-based-year
 * <li>{@link #WEEK_BASED_YEAR WEEK_BASED_YEAR} - the week-based-year
 * </ul>
 * The week-based-year itself is defined relative to the standard ISO proleptic year.
 * It differs from the standard year in that it always starts on a Monday.
 * <p>
 * The first week of a week-based-year is the first Monday-based week of the standard
 * ISO year that has at least 4 days in the new year.
 * <ul>
 * <li>If January 1st is Monday then week 1 starts on January 1st
 * <li>If January 1st is Tuesday then week 1 starts on December 31st of the previous standard year
 * <li>If January 1st is Wednesday then week 1 starts on December 30th of the previous standard year
 * <li>If January 1st is Thursday then week 1 starts on December 29th of the previous standard year
 * <li>If January 1st is Friday then week 1 starts on January 4th
 * <li>If January 1st is Saturday then week 1 starts on January 3rd
 * <li>If January 1st is Sunday then week 1 starts on January 2nd
 * </ul>
 * There are 52 weeks in most week-based years, however on occasion there are 53 weeks.
 * <p>
 * For example:
 *
 * <table class=striped style="text-align: left">
 * <caption>Examples of Week based Years</caption>
 * <thead>
 * <tr><th scope="col">Date</th><th scope="col">Day-of-week</th><th scope="col">Field values</th></tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row">2008-12-28</th><td>Sunday</td><td>Week 52 of week-based-year 2008</td></tr>
 * <tr><th scope="row">2008-12-29</th><td>Monday</td><td>Week 1 of week-based-year 2009</td></tr>
 * <tr><th scope="row">2008-12-31</th><td>Wednesday</td><td>Week 1 of week-based-year 2009</td></tr>
 * <tr><th scope="row">2009-01-01</th><td>Thursday</td><td>Week 1 of week-based-year 2009</td></tr>
 * <tr><th scope="row">2009-01-04</th><td>Sunday</td><td>Week 1 of week-based-year 2009</td></tr>
 * <tr><th scope="row">2009-01-05</th><td>Monday</td><td>Week 2 of week-based-year 2009</td></tr>
 * </tbody>
 * </table>
 *
 * @implSpec
 * <p>
 * This class is immutable and thread-safe.
 *
 * @since 1.8
 */
public final class IsoFields {

    /**
     * The field that represents the day-of-quarter.
     * <p>
     * This field allows the day-of-quarter value to be queried and set.
     * The day-of-quarter has values from 1 to 90 in Q1 of a standard year, from 1 to 91
     * in Q1 of a leap year, from 1 to 91 in Q2 and from 1 to 92 in Q3 and Q4.
     * <p>
     * The day-of-quarter can only be calculated if the day-of-year, month-of-year and year
     * are available.
     * <p>
     * When setting this field, the value is allowed to be partially lenient, taking any
     * value from 1 to 92. If the quarter has less than 92 days, then day 92, and
     * potentially day 91, is in the following quarter.
     * <p>
     * In the resolving phase of parsing, a date can be created from a year,
     * quarter-of-year and day-of-quarter.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all three fields are
     * validated against their range of valid values. The day-of-quarter field
     * is validated from 1 to 90, 91 or 92 depending on the year and quarter.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all three fields are
     * validated against their range of valid values. The day-of-quarter field is
     * validated between 1 and 92, ignoring the actual range based on the year and quarter.
     * If the day-of-quarter exceeds the actual range by one day, then the resulting date
     * is one day later. If the day-of-quarter exceeds the actual range by two days,
     * then the resulting date is two days later.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, only the year is validated
     * against the range of valid values. The resulting date is calculated equivalent to
     * the following three stage approach. First, create a date on the first of January
     * in the requested year. Then take the quarter-of-year, subtract one, and add the
     * amount in quarters to the date. Finally, take the day-of-quarter, subtract one,
     * and add the amount in days to the date.
     * <p>
     * This unit is an immutable and thread-safe singleton.
     */
    public static final TemporalField DAY_OF_QUARTER = Field.DAY_OF_QUARTER;
    /**
     * The field that represents the quarter-of-year.
     * <p>
     * This field allows the quarter-of-year value to be queried and set.
     * The quarter-of-year has values from 1 to 4.
     * <p>
     * The quarter-of-year can only be calculated if the month-of-year is available.
     * <p>
     * In the resolving phase of parsing, a date can be created from a year,
     * quarter-of-year and day-of-quarter.
     * See {@link #DAY_OF_QUARTER} for details.
     * <p>
     * This unit is an immutable and thread-safe singleton.
     */
    public static final TemporalField QUARTER_OF_YEAR = Field.QUARTER_OF_YEAR;
    /**
     * The field that represents the week-of-week-based-year.
     * <p>
     * This field allows the week of the week-based-year value to be queried and set.
     * The week-of-week-based-year has values from 1 to 52, or 53 if the
     * week-based-year has 53 weeks.
     * <p>
     * In the resolving phase of parsing, a date can be created from a
     * week-based-year, week-of-week-based-year and day-of-week.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all three fields are
     * validated against their range of valid values. The week-of-week-based-year
     * field is validated from 1 to 52 or 53 depending on the week-based-year.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all three fields are
     * validated against their range of valid values. The week-of-week-based-year
     * field is validated between 1 and 53, ignoring the week-based-year.
     * If the week-of-week-based-year is 53, but the week-based-year only has
     * 52 weeks, then the resulting date is in week 1 of the following week-based-year.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, only the week-based-year
     * is validated against the range of valid values. If the day-of-week is outside
     * the range 1 to 7, then the resulting date is adjusted by a suitable number of
     * weeks to reduce the day-of-week to the range 1 to 7. If the week-of-week-based-year
     * value is outside the range 1 to 52, then any excess weeks are added or subtracted
     * from the resulting date.
     * <p>
     * This unit is an immutable and thread-safe singleton.
     */
    public static final TemporalField WEEK_OF_WEEK_BASED_YEAR = Field.WEEK_OF_WEEK_BASED_YEAR;
    /**
     * The field that represents the week-based-year.
     * <p>
     * This field allows the week-based-year value to be queried and set.
     * <p>
     * The field has a range that matches {@link LocalDate#MAX} and {@link LocalDate#MIN}.
     * <p>
     * In the resolving phase of parsing, a date can be created from a
     * week-based-year, week-of-week-based-year and day-of-week.
     * See {@link #WEEK_OF_WEEK_BASED_YEAR} for details.
     * <p>
     * This unit is an immutable and thread-safe singleton.
     */
    public static final TemporalField WEEK_BASED_YEAR = Field.WEEK_BASED_YEAR;
    /**
     * The unit that represents week-based-years for the purpose of addition and subtraction.
     * <p>
     * This allows a number of week-based-years to be added to, or subtracted from, a date.
     * The unit is equal to either 52 or 53 weeks.
     * The estimated duration of a week-based-year is the same as that of a standard ISO
     * year at {@code 365.2425 Days}.
     * <p>
     * The rules for addition add the number of week-based-years to the existing value
     * for the week-based-year field. If the resulting week-based-year only has 52 weeks,
     * then the date will be in week 1 of the following week-based-year.
     * <p>
     * This unit is an immutable and thread-safe singleton.
     */
    public static final TemporalUnit WEEK_BASED_YEARS = Unit.WEEK_BASED_YEARS;
    /**
     * Unit that represents the concept of a quarter-year.
     * For the ISO calendar system, it is equal to 3 months.
     * The estimated duration of a quarter-year is one quarter of {@code 365.2425 Days}.
     * <p>
     * This unit is an immutable and thread-safe singleton.
     */
    public static final TemporalUnit QUARTER_YEARS = Unit.QUARTER_YEARS;

    /**
     * Restricted constructor.
     */
    private IsoFields() {
        throw new AssertionError("Not instantiable");
    }

    //-----------------------------------------------------------------------
    /**
     * Implementation of the field.
     */
    private static enum Field implements TemporalField {
        DAY_OF_QUARTER {
            @Override
            public TemporalUnit getBaseUnit() {
                return DAYS;
            }
            @Override
            public TemporalUnit getRangeUnit() {
                return QUARTER_YEARS;
            }
            @Override
            public ValueRange range() {
                return ValueRange.of(1, 90, 92);
            }
            @Override
            public boolean isSupportedBy(TemporalAccessor temporal) {
                return temporal.isSupported(DAY_OF_YEAR) && temporal.isSupported(MONTH_OF_YEAR) &&
                        temporal.isSupported(YEAR) && isIso(temporal);
            }
            @Override
            public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
                if (isSupportedBy(temporal) == false) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: DayOfQuarter");
                }
                long qoy = temporal.getLong(QUARTER_OF_YEAR);
                if (qoy == 1) {
                    long year = temporal.getLong(YEAR);
                    return (IsoChronology.INSTANCE.isLeapYear(year) ? ValueRange.of(1, 91) : ValueRange.of(1, 90));
                } else if (qoy == 2) {
                    return ValueRange.of(1, 91);
                } else if (qoy == 3 || qoy == 4) {
                    return ValueRange.of(1, 92);
                } // else value not from 1 to 4, so drop through
                return range();
            }
            @Override
            public long getFrom(TemporalAccessor temporal) {
                if (isSupportedBy(temporal) == false) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: DayOfQuarter");
                }
                int doy = temporal.get(DAY_OF_YEAR);
                int moy = temporal.get(MONTH_OF_YEAR);
                long year = temporal.getLong(YEAR);
                return doy - QUARTER_DAYS[((moy - 1) / 3) + (IsoChronology.INSTANCE.isLeapYear(year) ? 4 : 0)];
            }
            @SuppressWarnings("unchecked")
            @Override
            public <R extends Temporal> R adjustInto(R temporal, long newValue) {
                // calls getFrom() to check if supported
                long curValue = getFrom(temporal);
                range().checkValidValue(newValue, this);  // leniently check from 1 to 92 TODO: check
                return (R) temporal.with(DAY_OF_YEAR, temporal.getLong(DAY_OF_YEAR) + (newValue - curValue));
            }
            @Override
            public ChronoLocalDate resolve(
                    Map<TemporalField, Long> fieldValues, TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
                Long yearLong = fieldValues.get(YEAR);
                Long qoyLong = fieldValues.get(QUARTER_OF_YEAR);
                if (yearLong == null || qoyLong == null) {
                    return null;
                }
                int y = YEAR.checkValidIntValue(yearLong);  // always validate
                long doq = fieldValues.get(DAY_OF_QUARTER);
                ensureIso(partialTemporal);
                LocalDate date;
                if (resolverStyle == ResolverStyle.LENIENT) {
                    date = LocalDate.of(y, 1, 1).plusMonths(Math.multiplyExact(Math.subtractExact(qoyLong, 1), 3));
                    doq = Math.subtractExact(doq, 1);
                } else {
                    int qoy = QUARTER_OF_YEAR.range().checkValidIntValue(qoyLong, QUARTER_OF_YEAR);  // validated
                    date = LocalDate.of(y, ((qoy - 1) * 3) + 1, 1);
                    if (doq < 1 || doq > 90) {
                        if (resolverStyle == ResolverStyle.STRICT) {
                            rangeRefinedBy(date).checkValidValue(doq, this);  // only allow exact range
                        } else {  // SMART
                            range().checkValidValue(doq, this);  // allow 1-92 rolling into next quarter
                        }
                    }
                    doq--;
                }
                fieldValues.remove(this);
                fieldValues.remove(YEAR);
                fieldValues.remove(QUARTER_OF_YEAR);
                return date.plusDays(doq);
            }
            @Override
            public String toString() {
                return "DayOfQuarter";
            }
        },
        QUARTER_OF_YEAR {
            @Override
            public TemporalUnit getBaseUnit() {
                return QUARTER_YEARS;
            }
            @Override
            public TemporalUnit getRangeUnit() {
                return YEARS;
            }
            @Override
            public ValueRange range() {
                return ValueRange.of(1, 4);
            }
            @Override
            public boolean isSupportedBy(TemporalAccessor temporal) {
                return temporal.isSupported(MONTH_OF_YEAR) && isIso(temporal);
            }
            @Override
            public long getFrom(TemporalAccessor temporal) {
                if (isSupportedBy(temporal) == false) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: QuarterOfYear");
                }
                long moy = temporal.getLong(MONTH_OF_YEAR);
                return ((moy + 2) / 3);
            }
            public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
                if (isSupportedBy(temporal) == false) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: QuarterOfYear");
                }
                return super.rangeRefinedBy(temporal);
            }
            @SuppressWarnings("unchecked")
            @Override
            public <R extends Temporal> R adjustInto(R temporal, long newValue) {
                // calls getFrom() to check if supported
                long curValue = getFrom(temporal);
                range().checkValidValue(newValue, this);  // strictly check from 1 to 4
                return (R) temporal.with(MONTH_OF_YEAR, temporal.getLong(MONTH_OF_YEAR) + (newValue - curValue) * 3);
            }
            @Override
            public String toString() {
                return "QuarterOfYear";
            }
        },
        WEEK_OF_WEEK_BASED_YEAR {
            @Override
            public String getDisplayName(Locale locale) {
                Objects.requireNonNull(locale, "locale");
                LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased()
                                            .getLocaleResources(
                                                CalendarDataUtility
                                                    .findRegionOverride(locale));
                ResourceBundle rb = lr.getJavaTimeFormatData();
                return rb.containsKey("field.week") ? rb.getString("field.week") : toString();
            }

            @Override
            public TemporalUnit getBaseUnit() {
                return WEEKS;
            }
            @Override
            public TemporalUnit getRangeUnit() {
                return WEEK_BASED_YEARS;
            }
            @Override
            public ValueRange range() {
                return ValueRange.of(1, 52, 53);
            }
            @Override
            public boolean isSupportedBy(TemporalAccessor temporal) {
                return temporal.isSupported(EPOCH_DAY) && isIso(temporal);
            }
            @Override
            public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
                if (isSupportedBy(temporal) == false) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: WeekOfWeekBasedYear");
                }
                return getWeekRange(LocalDate.from(temporal));
            }
            @Override
            public long getFrom(TemporalAccessor temporal) {
                if (isSupportedBy(temporal) == false) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: WeekOfWeekBasedYear");
                }
                return getWeek(LocalDate.from(temporal));
            }
            @SuppressWarnings("unchecked")
            @Override
            public <R extends Temporal> R adjustInto(R temporal, long newValue) {
                // calls getFrom() to check if supported
                range().checkValidValue(newValue, this);  // lenient range
                return (R) temporal.plus(Math.subtractExact(newValue, getFrom(temporal)), WEEKS);
            }
            @Override
            public ChronoLocalDate resolve(
                    Map<TemporalField, Long> fieldValues, TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
                Long wbyLong = fieldValues.get(WEEK_BASED_YEAR);
                Long dowLong = fieldValues.get(DAY_OF_WEEK);
                if (wbyLong == null || dowLong == null) {
                    return null;
                }
                int wby = WEEK_BASED_YEAR.range().checkValidIntValue(wbyLong, WEEK_BASED_YEAR);  // always validate
                long wowby = fieldValues.get(WEEK_OF_WEEK_BASED_YEAR);
                ensureIso(partialTemporal);
                LocalDate date = LocalDate.of(wby, 1, 4);
                if (resolverStyle == ResolverStyle.LENIENT) {
                    long dow = dowLong;  // unvalidated
                    if (dow > 7) {
                        date = date.plusWeeks((dow - 1) / 7);
                        dow = ((dow - 1) % 7) + 1;
                    } else if (dow < 1) {
                        date = date.plusWeeks(Math.subtractExact(dow,  7) / 7);
                        dow = ((dow + 6) % 7) + 1;
                    }
                    date = date.plusWeeks(Math.subtractExact(wowby, 1)).with(DAY_OF_WEEK, dow);
                } else {
                    int dow = DAY_OF_WEEK.checkValidIntValue(dowLong);  // validated
                    if (wowby < 1 || wowby > 52) {
                        if (resolverStyle == ResolverStyle.STRICT) {
                            getWeekRange(date).checkValidValue(wowby, this);  // only allow exact range
                        } else {  // SMART
                            range().checkValidValue(wowby, this);  // allow 1-53 rolling into next year
                        }
                    }
                    date = date.plusWeeks(wowby - 1).with(DAY_OF_WEEK, dow);
                }
                fieldValues.remove(this);
                fieldValues.remove(WEEK_BASED_YEAR);
                fieldValues.remove(DAY_OF_WEEK);
                return date;
            }
            @Override
            public String toString() {
                return "WeekOfWeekBasedYear";
            }
        },
        WEEK_BASED_YEAR {
            @Override
            public TemporalUnit getBaseUnit() {
                return WEEK_BASED_YEARS;
            }
            @Override
            public TemporalUnit getRangeUnit() {
                return FOREVER;
            }
            @Override
            public ValueRange range() {
                return YEAR.range();
            }
            @Override
            public boolean isSupportedBy(TemporalAccessor temporal) {
                return temporal.isSupported(EPOCH_DAY) && isIso(temporal);
            }
            @Override
            public long getFrom(TemporalAccessor temporal) {
                if (isSupportedBy(temporal) == false) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: WeekBasedYear");
                }
                return getWeekBasedYear(LocalDate.from(temporal));
            }
            public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
                if (isSupportedBy(temporal) == false) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: WeekBasedYear");
                }
                return super.rangeRefinedBy(temporal);
            }
            @SuppressWarnings("unchecked")
            @Override
            public <R extends Temporal> R adjustInto(R temporal, long newValue) {
                if (isSupportedBy(temporal) == false) {
                    throw new UnsupportedTemporalTypeException("Unsupported field: WeekBasedYear");
                }
                int newWby = range().checkValidIntValue(newValue, WEEK_BASED_YEAR);  // strict check
                LocalDate date = LocalDate.from(temporal);
                int dow = date.get(DAY_OF_WEEK);
                int week = getWeek(date);
                if (week == 53 && getWeekRange(newWby) == 52) {
                    week = 52;
                }
                LocalDate resolved = LocalDate.of(newWby, 1, 4);  // 4th is guaranteed to be in week one
                int days = (dow - resolved.get(DAY_OF_WEEK)) + ((week - 1) * 7);
                resolved = resolved.plusDays(days);
                return (R) temporal.with(resolved);
            }
            @Override
            public String toString() {
                return "WeekBasedYear";
            }
        };

        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
            return range();
        }

        //-------------------------------------------------------------------------
        private static final int[] QUARTER_DAYS = {0, 90, 181, 273, 0, 91, 182, 274};


        private static void ensureIso(TemporalAccessor temporal) {
            if (isIso(temporal) == false) {
                throw new DateTimeException("Resolve requires IsoChronology");
            }
        }

        private static ValueRange getWeekRange(LocalDate date) {
            int wby = getWeekBasedYear(date);
            return ValueRange.of(1, getWeekRange(wby));
        }

        private static int getWeekRange(int wby) {
            LocalDate date = LocalDate.of(wby, 1, 1);
            // 53 weeks if standard year starts on Thursday, or Wed in a leap year
            if (date.getDayOfWeek() == THURSDAY || (date.getDayOfWeek() == WEDNESDAY && date.isLeapYear())) {
                return 53;
            }
            return 52;
        }

        private static int getWeek(LocalDate date) {
            int dow0 = date.getDayOfWeek().ordinal();
            int doy0 = date.getDayOfYear() - 1;
            int doyThu0 = doy0 + (3 - dow0);  // adjust to mid-week Thursday (which is 3 indexed from zero)
            int alignedWeek = doyThu0 / 7;
            int firstThuDoy0 = doyThu0 - (alignedWeek * 7);
            int firstMonDoy0 = firstThuDoy0 - 3;
            if (firstMonDoy0 < -3) {
                firstMonDoy0 += 7;
            }
            if (doy0 < firstMonDoy0) {
                return (int) getWeekRange(date.withDayOfYear(180).minusYears(1)).getMaximum();
            }
            int week = ((doy0 - firstMonDoy0) / 7) + 1;
            if (week == 53) {
                if ((firstMonDoy0 == -3 || (firstMonDoy0 == -2 && date.isLeapYear())) == false) {
                    week = 1;
                }
            }
            return week;
        }

        private static int getWeekBasedYear(LocalDate date) {
            int year = date.getYear();
            int doy = date.getDayOfYear();
            if (doy <= 3) {
                int dow = date.getDayOfWeek().ordinal();
                if (doy - dow < -2) {
                    year--;
                }
            } else if (doy >= 363) {
                int dow = date.getDayOfWeek().ordinal();
                doy = doy - 363 - (date.isLeapYear() ? 1 : 0);
                if (doy - dow >= 0) {
                    year++;
                }
            }
            return year;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Implementation of the unit.
     */
    private static enum Unit implements TemporalUnit {

        /**
         * Unit that represents the concept of a week-based-year.
         */
        WEEK_BASED_YEARS("WeekBasedYears", Duration.ofSeconds(31556952L)),
        /**
         * Unit that represents the concept of a quarter-year.
         */
        QUARTER_YEARS("QuarterYears", Duration.ofSeconds(31556952L / 4));

        private final String name;
        private final Duration duration;

        private Unit(String name, Duration estimatedDuration) {
            this.name = name;
            this.duration = estimatedDuration;
        }

        @Override
        public Duration getDuration() {
            return duration;
        }

        @Override
        public boolean isDurationEstimated() {
            return true;
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
        public boolean isSupportedBy(Temporal temporal) {
            return temporal.isSupported(EPOCH_DAY) && isIso(temporal);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R addTo(R temporal, long amount) {
            return switch (this) {
                case WEEK_BASED_YEARS -> (R) temporal.with(WEEK_BASED_YEAR,
                                          Math.addExact(temporal.get(WEEK_BASED_YEAR), amount));
                case QUARTER_YEARS -> (R) temporal.plus(amount / 4, YEARS)
                                       .plus((amount % 4) * 3, MONTHS);
                default -> throw new IllegalStateException("Unreachable");
            };
        }

        @Override
        public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
            if (temporal1Inclusive.getClass() != temporal2Exclusive.getClass()) {
                return temporal1Inclusive.until(temporal2Exclusive, this);
            }
            return switch (this) {
                case WEEK_BASED_YEARS -> Math.subtractExact(temporal2Exclusive.getLong(WEEK_BASED_YEAR),
                                          temporal1Inclusive.getLong(WEEK_BASED_YEAR));
                case QUARTER_YEARS -> temporal1Inclusive.until(temporal2Exclusive, MONTHS) / 3;
                default -> throw new IllegalStateException("Unreachable");
            };
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static boolean isIso(TemporalAccessor temporal) {
        return Chronology.from(temporal).equals(IsoChronology.INSTANCE);
    }
}
