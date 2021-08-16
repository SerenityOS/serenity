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
 * Copyright (c) 2012, Stephen Colebourne & Michael Nascimento Santos
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

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.ERAS;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.HALF_DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import sun.util.locale.provider.CalendarDataUtility;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleResources;

/**
 * A standard set of fields.
 * <p>
 * This set of fields provide field-based access to manipulate a date, time or date-time.
 * The standard set of fields can be extended by implementing {@link TemporalField}.
 * <p>
 * These fields are intended to be applicable in multiple calendar systems.
 * For example, most non-ISO calendar systems define dates as a year, month and day,
 * just with slightly different rules.
 * The documentation of each field explains how it operates.
 *
 * @implSpec
 * This is a final, immutable and thread-safe enum.
 *
 * @since 1.8
 */
public enum ChronoField implements TemporalField {

    /**
     * The nano-of-second.
     * <p>
     * This counts the nanosecond within the second, from 0 to 999,999,999.
     * This field has the same meaning for all calendar systems.
     * <p>
     * This field is used to represent the nano-of-second handling any fraction of the second.
     * Implementations of {@code TemporalAccessor} should provide a value for this field if
     * they can return a value for {@link #SECOND_OF_MINUTE}, {@link #SECOND_OF_DAY} or
     * {@link #INSTANT_SECONDS} filling unknown precision with zero.
     * <p>
     * When this field is used for setting a value, it should set as much precision as the
     * object stores, using integer division to remove excess precision.
     * For example, if the {@code TemporalAccessor} stores time to millisecond precision,
     * then the nano-of-second must be divided by 1,000,000 before replacing the milli-of-second.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     * The field is resolved in combination with {@code MILLI_OF_SECOND} and {@code MICRO_OF_SECOND}.
     */
    NANO_OF_SECOND("NanoOfSecond", NANOS, SECONDS, ValueRange.of(0, 999_999_999)),
    /**
     * The nano-of-day.
     * <p>
     * This counts the nanosecond within the day, from 0 to (24 * 60 * 60 * 1,000,000,000) - 1.
     * This field has the same meaning for all calendar systems.
     * <p>
     * This field is used to represent the nano-of-day handling any fraction of the second.
     * Implementations of {@code TemporalAccessor} should provide a value for this field if
     * they can return a value for {@link #SECOND_OF_DAY} filling unknown precision with zero.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     * The value is split to form {@code NANO_OF_SECOND}, {@code SECOND_OF_MINUTE},
     * {@code MINUTE_OF_HOUR} and {@code HOUR_OF_DAY} fields.
     */
    NANO_OF_DAY("NanoOfDay", NANOS, DAYS, ValueRange.of(0, 86400L * 1000_000_000L - 1)),
    /**
     * The micro-of-second.
     * <p>
     * This counts the microsecond within the second, from 0 to 999,999.
     * This field has the same meaning for all calendar systems.
     * <p>
     * This field is used to represent the micro-of-second handling any fraction of the second.
     * Implementations of {@code TemporalAccessor} should provide a value for this field if
     * they can return a value for {@link #SECOND_OF_MINUTE}, {@link #SECOND_OF_DAY} or
     * {@link #INSTANT_SECONDS} filling unknown precision with zero.
     * <p>
     * When this field is used for setting a value, it should behave in the same way as
     * setting {@link #NANO_OF_SECOND} with the value multiplied by 1,000.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     * The field is resolved in combination with {@code MILLI_OF_SECOND} to produce
     * {@code NANO_OF_SECOND}.
     */
    MICRO_OF_SECOND("MicroOfSecond", MICROS, SECONDS, ValueRange.of(0, 999_999)),
    /**
     * The micro-of-day.
     * <p>
     * This counts the microsecond within the day, from 0 to (24 * 60 * 60 * 1,000,000) - 1.
     * This field has the same meaning for all calendar systems.
     * <p>
     * This field is used to represent the micro-of-day handling any fraction of the second.
     * Implementations of {@code TemporalAccessor} should provide a value for this field if
     * they can return a value for {@link #SECOND_OF_DAY} filling unknown precision with zero.
     * <p>
     * When this field is used for setting a value, it should behave in the same way as
     * setting {@link #NANO_OF_DAY} with the value multiplied by 1,000.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     * The value is split to form {@code MICRO_OF_SECOND}, {@code SECOND_OF_MINUTE},
     * {@code MINUTE_OF_HOUR} and {@code HOUR_OF_DAY} fields.
     */
    MICRO_OF_DAY("MicroOfDay", MICROS, DAYS, ValueRange.of(0, 86400L * 1000_000L - 1)),
    /**
     * The milli-of-second.
     * <p>
     * This counts the millisecond within the second, from 0 to 999.
     * This field has the same meaning for all calendar systems.
     * <p>
     * This field is used to represent the milli-of-second handling any fraction of the second.
     * Implementations of {@code TemporalAccessor} should provide a value for this field if
     * they can return a value for {@link #SECOND_OF_MINUTE}, {@link #SECOND_OF_DAY} or
     * {@link #INSTANT_SECONDS} filling unknown precision with zero.
     * <p>
     * When this field is used for setting a value, it should behave in the same way as
     * setting {@link #NANO_OF_SECOND} with the value multiplied by 1,000,000.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     * The field is resolved in combination with {@code MICRO_OF_SECOND} to produce
     * {@code NANO_OF_SECOND}.
     */
    MILLI_OF_SECOND("MilliOfSecond", MILLIS, SECONDS, ValueRange.of(0, 999)),
    /**
     * The milli-of-day.
     * <p>
     * This counts the millisecond within the day, from 0 to (24 * 60 * 60 * 1,000) - 1.
     * This field has the same meaning for all calendar systems.
     * <p>
     * This field is used to represent the milli-of-day handling any fraction of the second.
     * Implementations of {@code TemporalAccessor} should provide a value for this field if
     * they can return a value for {@link #SECOND_OF_DAY} filling unknown precision with zero.
     * <p>
     * When this field is used for setting a value, it should behave in the same way as
     * setting {@link #NANO_OF_DAY} with the value multiplied by 1,000,000.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     * The value is split to form {@code MILLI_OF_SECOND}, {@code SECOND_OF_MINUTE},
     * {@code MINUTE_OF_HOUR} and {@code HOUR_OF_DAY} fields.
     */
    MILLI_OF_DAY("MilliOfDay", MILLIS, DAYS, ValueRange.of(0, 86400L * 1000L - 1)),
    /**
     * The second-of-minute.
     * <p>
     * This counts the second within the minute, from 0 to 59.
     * This field has the same meaning for all calendar systems.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     */
    SECOND_OF_MINUTE("SecondOfMinute", SECONDS, MINUTES, ValueRange.of(0, 59), "second"),
    /**
     * The second-of-day.
     * <p>
     * This counts the second within the day, from 0 to (24 * 60 * 60) - 1.
     * This field has the same meaning for all calendar systems.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     * The value is split to form {@code SECOND_OF_MINUTE}, {@code MINUTE_OF_HOUR}
     * and {@code HOUR_OF_DAY} fields.
     */
    SECOND_OF_DAY("SecondOfDay", SECONDS, DAYS, ValueRange.of(0, 86400L - 1)),
    /**
     * The minute-of-hour.
     * <p>
     * This counts the minute within the hour, from 0 to 59.
     * This field has the same meaning for all calendar systems.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     */
    MINUTE_OF_HOUR("MinuteOfHour", MINUTES, HOURS, ValueRange.of(0, 59), "minute"),
    /**
     * The minute-of-day.
     * <p>
     * This counts the minute within the day, from 0 to (24 * 60) - 1.
     * This field has the same meaning for all calendar systems.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     * The value is split to form {@code MINUTE_OF_HOUR} and {@code HOUR_OF_DAY} fields.
     */
    MINUTE_OF_DAY("MinuteOfDay", MINUTES, DAYS, ValueRange.of(0, (24 * 60) - 1)),
    /**
     * The hour-of-am-pm.
     * <p>
     * This counts the hour within the AM/PM, from 0 to 11.
     * This is the hour that would be observed on a standard 12-hour digital clock.
     * This field has the same meaning for all calendar systems.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated from 0 to 11 in strict and smart mode.
     * In lenient mode the value is not validated. It is combined with
     * {@code AMPM_OF_DAY} to form {@code HOUR_OF_DAY} by multiplying
     * the {@code AMPM_OF_DAY} value by 12.
     * <p>
     * See {@link #CLOCK_HOUR_OF_AMPM} for the related field that counts hours from 1 to 12.
     */
    HOUR_OF_AMPM("HourOfAmPm", HOURS, HALF_DAYS, ValueRange.of(0, 11)),
    /**
     * The clock-hour-of-am-pm.
     * <p>
     * This counts the hour within the AM/PM, from 1 to 12.
     * This is the hour that would be observed on a standard 12-hour analog wall clock.
     * This field has the same meaning for all calendar systems.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated from 1 to 12 in strict mode and from
     * 0 to 12 in smart mode. In lenient mode the value is not validated.
     * The field is converted to an {@code HOUR_OF_AMPM} with the same value,
     * unless the value is 12, in which case it is converted to 0.
     * <p>
     * See {@link #HOUR_OF_AMPM} for the related field that counts hours from 0 to 11.
     */
    CLOCK_HOUR_OF_AMPM("ClockHourOfAmPm", HOURS, HALF_DAYS, ValueRange.of(1, 12)),
    /**
     * The hour-of-day.
     * <p>
     * This counts the hour within the day, from 0 to 23.
     * This is the hour that would be observed on a standard 24-hour digital clock.
     * This field has the same meaning for all calendar systems.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated in strict and smart mode but not in lenient mode.
     * The field is combined with {@code MINUTE_OF_HOUR}, {@code SECOND_OF_MINUTE} and
     * {@code NANO_OF_SECOND} to produce a {@code LocalTime}.
     * In lenient mode, any excess days are added to the parsed date, or
     * made available via {@link java.time.format.DateTimeFormatter#parsedExcessDays()}.
     * <p>
     * See {@link #CLOCK_HOUR_OF_DAY} for the related field that counts hours from 1 to 24.
     */
    HOUR_OF_DAY("HourOfDay", HOURS, DAYS, ValueRange.of(0, 23), "hour"),
    /**
     * The clock-hour-of-day.
     * <p>
     * This counts the hour within the day, from 1 to 24.
     * This is the hour that would be observed on a 24-hour analog wall clock.
     * This field has the same meaning for all calendar systems.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated from 1 to 24 in strict mode and from
     * 0 to 24 in smart mode. In lenient mode the value is not validated.
     * The field is converted to an {@code HOUR_OF_DAY} with the same value,
     * unless the value is 24, in which case it is converted to 0.
     * <p>
     * See {@link #HOUR_OF_DAY} for the related field that counts hours from 0 to 23.
     */
    CLOCK_HOUR_OF_DAY("ClockHourOfDay", HOURS, DAYS, ValueRange.of(1, 24)),
    /**
     * The am-pm-of-day.
     * <p>
     * This counts the AM/PM within the day, from 0 (AM) to 1 (PM).
     * This field has the same meaning for all calendar systems.
     * <p>
     * When parsing this field it behaves equivalent to the following:
     * The value is validated from 0 to 1 in strict and smart mode.
     * In lenient mode the value is not validated. It is combined with
     * {@code HOUR_OF_AMPM} (if not present, it defaults to '6') to form
     * {@code HOUR_OF_DAY} by multiplying the {@code AMPM_OF_DAY} value
     * by 12.
     */
    AMPM_OF_DAY("AmPmOfDay", HALF_DAYS, DAYS, ValueRange.of(0, 1), "dayperiod"),
    /**
     * The day-of-week, such as Tuesday.
     * <p>
     * This represents the standard concept of the day of the week.
     * In the default ISO calendar system, this has values from Monday (1) to Sunday (7).
     * The {@link DayOfWeek} class can be used to interpret the result.
     * <p>
     * Most non-ISO calendar systems also define a seven day week that aligns with ISO.
     * Those calendar systems must also use the same numbering system, from Monday (1) to
     * Sunday (7), which allows {@code DayOfWeek} to be used.
     * <p>
     * Calendar systems that do not have a standard seven day week should implement this field
     * if they have a similar concept of named or numbered days within a period similar
     * to a week. It is recommended that the numbering starts from 1.
     */
    DAY_OF_WEEK("DayOfWeek", DAYS, WEEKS, ValueRange.of(1, 7), "weekday"),
    /**
     * The aligned day-of-week within a month.
     * <p>
     * This represents concept of the count of days within the period of a week
     * where the weeks are aligned to the start of the month.
     * This field is typically used with {@link #ALIGNED_WEEK_OF_MONTH}.
     * <p>
     * For example, in a calendar systems with a seven day week, the first aligned-week-of-month
     * starts on day-of-month 1, the second aligned-week starts on day-of-month 8, and so on.
     * Within each of these aligned-weeks, the days are numbered from 1 to 7 and returned
     * as the value of this field.
     * As such, day-of-month 1 to 7 will have aligned-day-of-week values from 1 to 7.
     * And day-of-month 8 to 14 will repeat this with aligned-day-of-week values from 1 to 7.
     * <p>
     * Calendar systems that do not have a seven day week should typically implement this
     * field in the same way, but using the alternate week length.
     */
    ALIGNED_DAY_OF_WEEK_IN_MONTH("AlignedDayOfWeekInMonth", DAYS, WEEKS, ValueRange.of(1, 7)),
    /**
     * The aligned day-of-week within a year.
     * <p>
     * This represents concept of the count of days within the period of a week
     * where the weeks are aligned to the start of the year.
     * This field is typically used with {@link #ALIGNED_WEEK_OF_YEAR}.
     * <p>
     * For example, in a calendar systems with a seven day week, the first aligned-week-of-year
     * starts on day-of-year 1, the second aligned-week starts on day-of-year 8, and so on.
     * Within each of these aligned-weeks, the days are numbered from 1 to 7 and returned
     * as the value of this field.
     * As such, day-of-year 1 to 7 will have aligned-day-of-week values from 1 to 7.
     * And day-of-year 8 to 14 will repeat this with aligned-day-of-week values from 1 to 7.
     * <p>
     * Calendar systems that do not have a seven day week should typically implement this
     * field in the same way, but using the alternate week length.
     */
    ALIGNED_DAY_OF_WEEK_IN_YEAR("AlignedDayOfWeekInYear", DAYS, WEEKS, ValueRange.of(1, 7)),
    /**
     * The day-of-month.
     * <p>
     * This represents the concept of the day within the month.
     * In the default ISO calendar system, this has values from 1 to 31 in most months.
     * April, June, September, November have days from 1 to 30, while February has days
     * from 1 to 28, or 29 in a leap year.
     * <p>
     * Non-ISO calendar systems should implement this field using the most recognized
     * day-of-month values for users of the calendar system.
     * Normally, this is a count of days from 1 to the length of the month.
     */
    DAY_OF_MONTH("DayOfMonth", DAYS, MONTHS, ValueRange.of(1, 28, 31), "day"),
    /**
     * The day-of-year.
     * <p>
     * This represents the concept of the day within the year.
     * In the default ISO calendar system, this has values from 1 to 365 in standard
     * years and 1 to 366 in leap years.
     * <p>
     * Non-ISO calendar systems should implement this field using the most recognized
     * day-of-year values for users of the calendar system.
     * Normally, this is a count of days from 1 to the length of the year.
     * <p>
     * Note that a non-ISO calendar system may have year numbering system that changes
     * at a different point to the natural reset in the month numbering. An example
     * of this is the Japanese calendar system where a change of era, which resets
     * the year number to 1, can happen on any date. The era and year reset also cause
     * the day-of-year to be reset to 1, but not the month-of-year or day-of-month.
     */
    DAY_OF_YEAR("DayOfYear", DAYS, YEARS, ValueRange.of(1, 365, 366)),
    /**
     * The epoch-day, based on the Java epoch of 1970-01-01 (ISO).
     * <p>
     * This field is the sequential count of days where 1970-01-01 (ISO) is zero.
     * Note that this uses the <i>local</i> time-line, ignoring offset and time-zone.
     * <p>
     * This field is strictly defined to have the same meaning in all calendar systems.
     * This is necessary to ensure interoperation between calendars.
     * <p>
     * Range of EpochDay is between (LocalDate.MIN.toEpochDay(), LocalDate.MAX.toEpochDay())
     * both inclusive.
     */
    EPOCH_DAY("EpochDay", DAYS, FOREVER, ValueRange.of(-365243219162L, 365241780471L)),
    /**
     * The aligned week within a month.
     * <p>
     * This represents concept of the count of weeks within the period of a month
     * where the weeks are aligned to the start of the month.
     * This field is typically used with {@link #ALIGNED_DAY_OF_WEEK_IN_MONTH}.
     * <p>
     * For example, in a calendar systems with a seven day week, the first aligned-week-of-month
     * starts on day-of-month 1, the second aligned-week starts on day-of-month 8, and so on.
     * Thus, day-of-month values 1 to 7 are in aligned-week 1, while day-of-month values
     * 8 to 14 are in aligned-week 2, and so on.
     * <p>
     * Calendar systems that do not have a seven day week should typically implement this
     * field in the same way, but using the alternate week length.
     */
    ALIGNED_WEEK_OF_MONTH("AlignedWeekOfMonth", WEEKS, MONTHS, ValueRange.of(1, 4, 5)),
    /**
     * The aligned week within a year.
     * <p>
     * This represents concept of the count of weeks within the period of a year
     * where the weeks are aligned to the start of the year.
     * This field is typically used with {@link #ALIGNED_DAY_OF_WEEK_IN_YEAR}.
     * <p>
     * For example, in a calendar systems with a seven day week, the first aligned-week-of-year
     * starts on day-of-year 1, the second aligned-week starts on day-of-year 8, and so on.
     * Thus, day-of-year values 1 to 7 are in aligned-week 1, while day-of-year values
     * 8 to 14 are in aligned-week 2, and so on.
     * <p>
     * Calendar systems that do not have a seven day week should typically implement this
     * field in the same way, but using the alternate week length.
     */
    ALIGNED_WEEK_OF_YEAR("AlignedWeekOfYear", WEEKS, YEARS, ValueRange.of(1, 53)),
    /**
     * The month-of-year, such as March.
     * <p>
     * This represents the concept of the month within the year.
     * In the default ISO calendar system, this has values from January (1) to December (12).
     * <p>
     * Non-ISO calendar systems should implement this field using the most recognized
     * month-of-year values for users of the calendar system.
     * Normally, this is a count of months starting from 1.
     */
    MONTH_OF_YEAR("MonthOfYear", MONTHS, YEARS, ValueRange.of(1, 12), "month"),
    /**
     * The proleptic-month based, counting months sequentially from year 0.
     * <p>
     * This field is the sequential count of months where the first month
     * in proleptic-year zero has the value zero.
     * Later months have increasingly larger values.
     * Earlier months have increasingly small values.
     * There are no gaps or breaks in the sequence of months.
     * Note that this uses the <i>local</i> time-line, ignoring offset and time-zone.
     * <p>
     * In the default ISO calendar system, June 2012 would have the value
     * {@code (2012 * 12 + 6 - 1)}. This field is primarily for internal use.
     * <p>
     * Non-ISO calendar systems must implement this field as per the definition above.
     * It is just a simple zero-based count of elapsed months from the start of proleptic-year 0.
     * All calendar systems with a full proleptic-year definition will have a year zero.
     * If the calendar system has a minimum year that excludes year zero, then one must
     * be extrapolated in order for this method to be defined.
     */
    PROLEPTIC_MONTH("ProlepticMonth", MONTHS, FOREVER, ValueRange.of(Year.MIN_VALUE * 12L, Year.MAX_VALUE * 12L + 11)),
    /**
     * The year within the era.
     * <p>
     * This represents the concept of the year within the era.
     * This field is typically used with {@link #ERA}.
     * <p>
     * The standard mental model for a date is based on three concepts - year, month and day.
     * These map onto the {@code YEAR}, {@code MONTH_OF_YEAR} and {@code DAY_OF_MONTH} fields.
     * Note that there is no reference to eras.
     * The full model for a date requires four concepts - era, year, month and day. These map onto
     * the {@code ERA}, {@code YEAR_OF_ERA}, {@code MONTH_OF_YEAR} and {@code DAY_OF_MONTH} fields.
     * Whether this field or {@code YEAR} is used depends on which mental model is being used.
     * See {@link ChronoLocalDate} for more discussion on this topic.
     * <p>
     * In the default ISO calendar system, there are two eras defined, 'BCE' and 'CE'.
     * The era 'CE' is the one currently in use and year-of-era runs from 1 to the maximum value.
     * The era 'BCE' is the previous era, and the year-of-era runs backwards.
     * <p>
     * For example, subtracting a year each time yield the following:<br>
     * - year-proleptic 2  = 'CE' year-of-era 2<br>
     * - year-proleptic 1  = 'CE' year-of-era 1<br>
     * - year-proleptic 0  = 'BCE' year-of-era 1<br>
     * - year-proleptic -1 = 'BCE' year-of-era 2<br>
     * <p>
     * Note that the ISO-8601 standard does not actually define eras.
     * Note also that the ISO eras do not align with the well-known AD/BC eras due to the
     * change between the Julian and Gregorian calendar systems.
     * <p>
     * Non-ISO calendar systems should implement this field using the most recognized
     * year-of-era value for users of the calendar system.
     * Since most calendar systems have only two eras, the year-of-era numbering approach
     * will typically be the same as that used by the ISO calendar system.
     * The year-of-era value should typically always be positive, however this is not required.
     */
    YEAR_OF_ERA("YearOfEra", YEARS, FOREVER, ValueRange.of(1, Year.MAX_VALUE, Year.MAX_VALUE + 1)),
    /**
     * The proleptic year, such as 2012.
     * <p>
     * This represents the concept of the year, counting sequentially and using negative numbers.
     * The proleptic year is not interpreted in terms of the era.
     * See {@link #YEAR_OF_ERA} for an example showing the mapping from proleptic year to year-of-era.
     * <p>
     * The standard mental model for a date is based on three concepts - year, month and day.
     * These map onto the {@code YEAR}, {@code MONTH_OF_YEAR} and {@code DAY_OF_MONTH} fields.
     * Note that there is no reference to eras.
     * The full model for a date requires four concepts - era, year, month and day. These map onto
     * the {@code ERA}, {@code YEAR_OF_ERA}, {@code MONTH_OF_YEAR} and {@code DAY_OF_MONTH} fields.
     * Whether this field or {@code YEAR_OF_ERA} is used depends on which mental model is being used.
     * See {@link ChronoLocalDate} for more discussion on this topic.
     * <p>
     * Non-ISO calendar systems should implement this field as follows.
     * If the calendar system has only two eras, before and after a fixed date, then the
     * proleptic-year value must be the same as the year-of-era value for the later era,
     * and increasingly negative for the earlier era.
     * If the calendar system has more than two eras, then the proleptic-year value may be
     * defined with any appropriate value, although defining it to be the same as ISO may be
     * the best option.
     */
    YEAR("Year", YEARS, FOREVER, ValueRange.of(Year.MIN_VALUE, Year.MAX_VALUE), "year"),
    /**
     * The era.
     * <p>
     * This represents the concept of the era, which is the largest division of the time-line.
     * This field is typically used with {@link #YEAR_OF_ERA}.
     * <p>
     * In the default ISO calendar system, there are two eras defined, 'BCE' and 'CE'.
     * The era 'CE' is the one currently in use and year-of-era runs from 1 to the maximum value.
     * The era 'BCE' is the previous era, and the year-of-era runs backwards.
     * See {@link #YEAR_OF_ERA} for a full example.
     * <p>
     * Non-ISO calendar systems should implement this field to define eras.
     * The value of the era that was active on 1970-01-01 (ISO) must be assigned the value 1.
     * Earlier eras must have sequentially smaller values.
     * Later eras must have sequentially larger values,
     */
    ERA("Era", ERAS, FOREVER, ValueRange.of(0, 1), "era"),
    /**
     * The instant epoch-seconds.
     * <p>
     * This represents the concept of the sequential count of seconds where
     * 1970-01-01T00:00Z (ISO) is zero.
     * This field may be used with {@link #NANO_OF_SECOND} to represent the fraction of the second.
     * <p>
     * An {@link Instant} represents an instantaneous point on the time-line.
     * On their own, an instant has insufficient information to allow a local date-time to be obtained.
     * Only when paired with an offset or time-zone can the local date or time be calculated.
     * <p>
     * This field is strictly defined to have the same meaning in all calendar systems.
     * This is necessary to ensure interoperation between calendars.
     */
    INSTANT_SECONDS("InstantSeconds", SECONDS, FOREVER, ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)),
    /**
     * The offset from UTC/Greenwich.
     * <p>
     * This represents the concept of the offset in seconds of local time from UTC/Greenwich.
     * <p>
     * A {@link ZoneOffset} represents the period of time that local time differs from UTC/Greenwich.
     * This is usually a fixed number of hours and minutes.
     * It is equivalent to the {@link ZoneOffset#getTotalSeconds() total amount} of the offset in seconds.
     * For example, during the winter Paris has an offset of {@code +01:00}, which is 3600 seconds.
     * <p>
     * This field is strictly defined to have the same meaning in all calendar systems.
     * This is necessary to ensure interoperation between calendars.
     */
    OFFSET_SECONDS("OffsetSeconds", SECONDS, FOREVER, ValueRange.of(-18 * 3600, 18 * 3600));

    private final String name;
    private final TemporalUnit baseUnit;
    private final TemporalUnit rangeUnit;
    private final ValueRange range;
    private final String displayNameKey;

    private ChronoField(String name, TemporalUnit baseUnit, TemporalUnit rangeUnit, ValueRange range) {
        this.name = name;
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = range;
        this.displayNameKey = null;
    }

    private ChronoField(String name, TemporalUnit baseUnit, TemporalUnit rangeUnit,
            ValueRange range, String displayNameKey) {
        this.name = name;
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = range;
        this.displayNameKey = displayNameKey;
    }

    @Override
    public String getDisplayName(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        if (displayNameKey == null) {
            return name;
        }

        LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased()
                                    .getLocaleResources(
                                        CalendarDataUtility
                                            .findRegionOverride(locale));
        ResourceBundle rb = lr.getJavaTimeFormatData();
        String key = "field." + displayNameKey;
        return rb.containsKey(key) ? rb.getString(key) : name;
    }

    @Override
    public TemporalUnit getBaseUnit() {
        return baseUnit;
    }

    @Override
    public TemporalUnit getRangeUnit() {
        return rangeUnit;
    }

    /**
     * Gets the range of valid values for the field.
     * <p>
     * All fields can be expressed as a {@code long} integer.
     * This method returns an object that describes the valid range for that value.
     * <p>
     * This method returns the range of the field in the ISO-8601 calendar system.
     * This range may be incorrect for other calendar systems.
     * Use {@link Chronology#range(ChronoField)} to access the correct range
     * for a different calendar system.
     * <p>
     * Note that the result only describes the minimum and maximum valid values
     * and it is important not to read too much into them. For example, there
     * could be values within the range that are invalid for the field.
     *
     * @return the range of valid values for the field, not null
     */
    @Override
    public ValueRange range() {
        return range;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this field represents a component of a date.
     * <p>
     * Fields from day-of-week to era are date-based.
     *
     * @return true if it is a component of a date
     */
    @Override
    public boolean isDateBased() {
        return ordinal() >= DAY_OF_WEEK.ordinal() && ordinal() <= ERA.ordinal();
    }

    /**
     * Checks if this field represents a component of a time.
     * <p>
     * Fields from nano-of-second to am-pm-of-day are time-based.
     *
     * @return true if it is a component of a time
     */
    @Override
    public boolean isTimeBased() {
        return ordinal() < DAY_OF_WEEK.ordinal();
    }

    //-----------------------------------------------------------------------
    /**
     * Checks that the specified value is valid for this field.
     * <p>
     * This validates that the value is within the outer range of valid values
     * returned by {@link #range()}.
     * <p>
     * This method checks against the range of the field in the ISO-8601 calendar system.
     * This range may be incorrect for other calendar systems.
     * Use {@link Chronology#range(ChronoField)} to access the correct range
     * for a different calendar system.
     *
     * @param value  the value to check
     * @return the value that was passed in
     */
    public long checkValidValue(long value) {
        return range().checkValidValue(value, this);
    }

    /**
     * Checks that the specified value is valid and fits in an {@code int}.
     * <p>
     * This validates that the value is within the outer range of valid values
     * returned by {@link #range()}.
     * It also checks that all valid values are within the bounds of an {@code int}.
     * <p>
     * This method checks against the range of the field in the ISO-8601 calendar system.
     * This range may be incorrect for other calendar systems.
     * Use {@link Chronology#range(ChronoField)} to access the correct range
     * for a different calendar system.
     *
     * @param value  the value to check
     * @return the value that was passed in
     */
    public int checkValidIntValue(long value) {
        return range().checkValidIntValue(value, this);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupportedBy(TemporalAccessor temporal) {
        return temporal.isSupported(this);
    }

    @Override
    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        return temporal.range(this);
    }

    @Override
    public long getFrom(TemporalAccessor temporal) {
        return temporal.getLong(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        return (R) temporal.with(this, newValue);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        return name;
    }

}
