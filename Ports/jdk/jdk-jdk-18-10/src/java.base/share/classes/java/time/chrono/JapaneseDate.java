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
package java.time.chrono;

import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Calendar;
import java.util.Objects;

import sun.util.calendar.CalendarDate;
import sun.util.calendar.LocalGregorianCalendar;

/**
 * A date in the Japanese Imperial calendar system.
 * <p>
 * This date operates using the {@linkplain JapaneseChronology Japanese Imperial calendar}.
 * This calendar system is primarily used in Japan.
 * <p>
 * The Japanese Imperial calendar system is the same as the ISO calendar system
 * apart from the era-based year numbering. The proleptic-year is defined to be
 * equal to the ISO proleptic-year.
 * <p>
 * Japan introduced the Gregorian calendar starting with Meiji 6.
 * Only Meiji and later eras are supported;
 * dates before Meiji 6, January 1 are not supported.
 * <p>
 * For example, the Japanese year "Heisei 24" corresponds to ISO year "2012".<br>
 * Calling {@code japaneseDate.get(YEAR_OF_ERA)} will return 24.<br>
 * Calling {@code japaneseDate.get(YEAR)} will return 2012.<br>
 * Calling {@code japaneseDate.get(ERA)} will return 2, corresponding to
 * {@code JapaneseChronology.ERA_HEISEI}.<br>
 * <p>
 * This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; programmers should treat instances that are
 * {@linkplain #equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may
 * occur. For example, in a future release, synchronization may fail.
 * The {@code equals} method should be used for comparisons.
 *
 * @implSpec
 * This class is immutable and thread-safe.
 *
 * @since 1.8
 */
@jdk.internal.ValueBased
public final class JapaneseDate
        extends ChronoLocalDateImpl<JapaneseDate>
        implements ChronoLocalDate, Serializable {

    /**
     * Serialization version.
     */
    @java.io.Serial
    private static final long serialVersionUID = -305327627230580483L;

    /**
     * The underlying ISO local date.
     */
    private final transient LocalDate isoDate;
    /**
     * The JapaneseEra of this date.
     */
    private final transient JapaneseEra era;
    /**
     * The Japanese imperial calendar year of this date.
     */
    private final transient int yearOfEra;

    /**
     * The first day supported by the JapaneseChronology is Meiji 6, January 1st.
     */
    static final LocalDate MEIJI_6_ISODATE = LocalDate.of(1873, 1, 1);

    //-----------------------------------------------------------------------
    /**
     * Obtains the current {@code JapaneseDate} from the system clock in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current date.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current date using the system clock and default time-zone, not null
     */
    public static JapaneseDate now() {
        return now(Clock.systemDefaultZone());
    }

    /**
     * Obtains the current {@code JapaneseDate} from the system clock in the specified time-zone.
     * <p>
     * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current date.
     * Specifying the time-zone avoids dependence on the default time-zone.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @param zone  the zone ID to use, not null
     * @return the current date using the system clock, not null
     */
    public static JapaneseDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    /**
     * Obtains the current {@code JapaneseDate} from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current date - today.
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@linkplain Clock dependency injection}.
     *
     * @param clock  the clock to use, not null
     * @return the current date, not null
     * @throws DateTimeException if the current date cannot be obtained
     */
    public static JapaneseDate now(Clock clock) {
        return new JapaneseDate(LocalDate.now(clock));
    }

    /**
     * Obtains a {@code JapaneseDate} representing a date in the Japanese calendar
     * system from the era, year-of-era, month-of-year and day-of-month fields.
     * <p>
     * This returns a {@code JapaneseDate} with the specified fields.
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     * <p>
     * The Japanese month and day-of-month are the same as those in the
     * ISO calendar system. They are not reset when the era changes.
     * For example:
     * <pre>
     *  6th Jan Showa 64 = ISO 1989-01-06
     *  7th Jan Showa 64 = ISO 1989-01-07
     *  8th Jan Heisei 1 = ISO 1989-01-08
     *  9th Jan Heisei 1 = ISO 1989-01-09
     * </pre>
     *
     * @param era  the Japanese era, not null
     * @param yearOfEra  the Japanese year-of-era
     * @param month  the Japanese month-of-year, from 1 to 12
     * @param dayOfMonth  the Japanese day-of-month, from 1 to 31
     * @return the date in Japanese calendar system, not null
     * @throws DateTimeException if the value of any field is out of range,
     *  or if the day-of-month is invalid for the month-year,
     *  or if the date is not a Japanese era
     */
    public static JapaneseDate of(JapaneseEra era, int yearOfEra, int month, int dayOfMonth) {
        Objects.requireNonNull(era, "era");
        LocalGregorianCalendar.Date jdate = JapaneseChronology.JCAL.newCalendarDate(null);
        jdate.setEra(era.getPrivateEra()).setDate(yearOfEra, month, dayOfMonth);
        if (!JapaneseChronology.JCAL.validate(jdate)) {
            throw new DateTimeException("year, month, and day not valid for Era");
        }
        LocalDate date = LocalDate.of(jdate.getNormalizedYear(), month, dayOfMonth);
        return new JapaneseDate(era, yearOfEra, date);
    }

    /**
     * Obtains a {@code JapaneseDate} representing a date in the Japanese calendar
     * system from the proleptic-year, month-of-year and day-of-month fields.
     * <p>
     * This returns a {@code JapaneseDate} with the specified fields.
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     * <p>
     * The Japanese proleptic year, month and day-of-month are the same as those
     * in the ISO calendar system. They are not reset when the era changes.
     *
     * @param prolepticYear  the Japanese proleptic-year
     * @param month  the Japanese month-of-year, from 1 to 12
     * @param dayOfMonth  the Japanese day-of-month, from 1 to 31
     * @return the date in Japanese calendar system, not null
     * @throws DateTimeException if the value of any field is out of range,
     *  or if the day-of-month is invalid for the month-year
     */
    public static JapaneseDate of(int prolepticYear, int month, int dayOfMonth) {
        return new JapaneseDate(LocalDate.of(prolepticYear, month, dayOfMonth));
    }

    /**
     * Obtains a {@code JapaneseDate} representing a date in the Japanese calendar
     * system from the era, year-of-era and day-of-year fields.
     * <p>
     * This returns a {@code JapaneseDate} with the specified fields.
     * The day must be valid for the year, otherwise an exception will be thrown.
     * <p>
     * The day-of-year in this factory is expressed relative to the start of the year-of-era.
     * This definition changes the normal meaning of day-of-year only in those years
     * where the year-of-era is reset to one due to a change in the era.
     * For example:
     * <pre>
     *  6th Jan Showa 64 = day-of-year 6
     *  7th Jan Showa 64 = day-of-year 7
     *  8th Jan Heisei 1 = day-of-year 1
     *  9th Jan Heisei 1 = day-of-year 2
     * </pre>
     *
     * @param era  the Japanese era, not null
     * @param yearOfEra  the Japanese year-of-era
     * @param dayOfYear  the chronology day-of-year, from 1 to 366
     * @return the date in Japanese calendar system, not null
     * @throws DateTimeException if the value of any field is out of range,
     *  or if the day-of-year is invalid for the year
     */
    static JapaneseDate ofYearDay(JapaneseEra era, int yearOfEra, int dayOfYear) {
        Objects.requireNonNull(era, "era");
        CalendarDate firstDay = era.getPrivateEra().getSinceDate();
        LocalGregorianCalendar.Date jdate = JapaneseChronology.JCAL.newCalendarDate(null);
        jdate.setEra(era.getPrivateEra());
        if (yearOfEra == 1) {
            jdate.setDate(yearOfEra, firstDay.getMonth(), firstDay.getDayOfMonth() + dayOfYear - 1);
        } else {
            jdate.setDate(yearOfEra, 1, dayOfYear);
        }
        JapaneseChronology.JCAL.normalize(jdate);
        if (era.getPrivateEra() != jdate.getEra() || yearOfEra != jdate.getYear()) {
            throw new DateTimeException("Invalid parameters");
        }
        LocalDate localdate = LocalDate.of(jdate.getNormalizedYear(),
                                      jdate.getMonth(), jdate.getDayOfMonth());
        return new JapaneseDate(era, yearOfEra, localdate);
    }

    /**
     * Obtains a {@code JapaneseDate} from a temporal object.
     * <p>
     * This obtains a date in the Japanese calendar system based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code JapaneseDate}.
     * <p>
     * The conversion typically uses the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
     * field, which is standardized across calendar systems.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code JapaneseDate::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the date in Japanese calendar system, not null
     * @throws DateTimeException if unable to convert to a {@code JapaneseDate}
     */
    public static JapaneseDate from(TemporalAccessor temporal) {
        return JapaneseChronology.INSTANCE.date(temporal);
    }

    //-----------------------------------------------------------------------
    /**
     * Creates an instance from an ISO date.
     *
     * @param isoDate  the standard local date, validated not null
     */
    JapaneseDate(LocalDate isoDate) {
        if (isoDate.isBefore(MEIJI_6_ISODATE)) {
            throw new DateTimeException("JapaneseDate before Meiji 6 is not supported");
        }
        LocalGregorianCalendar.Date jdate = toPrivateJapaneseDate(isoDate);
        this.era = JapaneseEra.toJapaneseEra(jdate.getEra());
        this.yearOfEra = jdate.getYear();
        this.isoDate = isoDate;
    }

    /**
     * Constructs a {@code JapaneseDate}. This constructor does NOT validate the given parameters,
     * and {@code era} and {@code year} must agree with {@code isoDate}.
     *
     * @param era  the era, validated not null
     * @param year  the year-of-era, validated
     * @param isoDate  the standard local date, validated not null
     */
    JapaneseDate(JapaneseEra era, int year, LocalDate isoDate) {
        if (isoDate.isBefore(MEIJI_6_ISODATE)) {
            throw new DateTimeException("JapaneseDate before Meiji 6 is not supported");
        }
        this.era = era;
        this.yearOfEra = year;
        this.isoDate = isoDate;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the chronology of this date, which is the Japanese calendar system.
     * <p>
     * The {@code Chronology} represents the calendar system in use.
     * The era and other fields in {@link ChronoField} are defined by the chronology.
     *
     * @return the Japanese chronology, not null
     */
    @Override
    public JapaneseChronology getChronology() {
        return JapaneseChronology.INSTANCE;
    }

    /**
     * Gets the era applicable at this date.
     * <p>
     * The Japanese calendar system has multiple eras defined by {@link JapaneseEra}.
     *
     * @return the era applicable at this date, not null
     */
    @Override
    public JapaneseEra getEra() {
        return era;
    }

    /**
     * Returns the length of the month represented by this date.
     * <p>
     * This returns the length of the month in days.
     * Month lengths match those of the ISO calendar system.
     *
     * @return the length of the month in days
     */
    @Override
    public int lengthOfMonth() {
        return isoDate.lengthOfMonth();
    }

    @Override
    public int lengthOfYear() {
        Calendar jcal = Calendar.getInstance(JapaneseChronology.LOCALE);
        jcal.set(Calendar.ERA, era.getValue() + JapaneseEra.ERA_OFFSET);
        jcal.set(yearOfEra, isoDate.getMonthValue() - 1, isoDate.getDayOfMonth());
        return  jcal.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if this date can be queried for the specified field.
     * If false, then calling the {@link #range(TemporalField) range} and
     * {@link #get(TemporalField) get} methods will throw an exception.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The supported fields are:
     * <ul>
     * <li>{@code DAY_OF_WEEK}
     * <li>{@code DAY_OF_MONTH}
     * <li>{@code DAY_OF_YEAR}
     * <li>{@code EPOCH_DAY}
     * <li>{@code MONTH_OF_YEAR}
     * <li>{@code PROLEPTIC_MONTH}
     * <li>{@code YEAR_OF_ERA}
     * <li>{@code YEAR}
     * <li>{@code ERA}
     * </ul>
     * All other {@code ChronoField} instances will return false.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.isSupportedBy(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the field is supported is determined by the field.
     *
     * @param field  the field to check, null returns false
     * @return true if the field is supported on this date, false if not
     */
    @Override
    public boolean isSupported(TemporalField field) {
        if (field == ALIGNED_DAY_OF_WEEK_IN_MONTH || field == ALIGNED_DAY_OF_WEEK_IN_YEAR ||
                field == ALIGNED_WEEK_OF_MONTH || field == ALIGNED_WEEK_OF_YEAR) {
            return false;
        }
        return super.isSupported(field);
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField chronoField) {
            if (isSupported(field)) {
                return switch (chronoField) {
                    case DAY_OF_MONTH -> ValueRange.of(1, lengthOfMonth());
                    case DAY_OF_YEAR -> ValueRange.of(1, lengthOfYear());
                    case YEAR_OF_ERA -> {
                        Calendar jcal = Calendar.getInstance(JapaneseChronology.LOCALE);
                        jcal.set(Calendar.ERA, era.getValue() + JapaneseEra.ERA_OFFSET);
                        jcal.set(yearOfEra, isoDate.getMonthValue() - 1, isoDate.getDayOfMonth());
                        yield ValueRange.of(1, jcal.getActualMaximum(Calendar.YEAR));
                    }
                    default -> getChronology().range(chronoField);
                };
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            // same as ISO:
            // DAY_OF_WEEK, DAY_OF_MONTH, EPOCH_DAY, MONTH_OF_YEAR, PROLEPTIC_MONTH, YEAR
            //
            // calendar specific fields
            // DAY_OF_YEAR, YEAR_OF_ERA, ERA
            switch ((ChronoField) field) {
                case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                case ALIGNED_WEEK_OF_MONTH:
                case ALIGNED_WEEK_OF_YEAR:
                    throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
                case YEAR_OF_ERA:
                    return yearOfEra;
                case ERA:
                    return era.getValue();
                case DAY_OF_YEAR:
                    Calendar jcal = Calendar.getInstance(JapaneseChronology.LOCALE);
                    jcal.set(Calendar.ERA, era.getValue() + JapaneseEra.ERA_OFFSET);
                    jcal.set(yearOfEra, isoDate.getMonthValue() - 1, isoDate.getDayOfMonth());
                    return jcal.get(Calendar.DAY_OF_YEAR);
            }
            return isoDate.getLong(field);
        }
        return field.getFrom(this);
    }

    /**
     * Returns a {@code LocalGregorianCalendar.Date} converted from the given {@code isoDate}.
     *
     * @param isoDate  the local date, not null
     * @return a {@code LocalGregorianCalendar.Date}, not null
     */
    private static LocalGregorianCalendar.Date toPrivateJapaneseDate(LocalDate isoDate) {
        LocalGregorianCalendar.Date jdate = JapaneseChronology.JCAL.newCalendarDate(null);
        sun.util.calendar.Era sunEra = JapaneseEra.privateEraFrom(isoDate);
        int year = isoDate.getYear();
        if (sunEra != null) {
            year -= sunEra.getSinceDate().getYear() - 1;
        }
        jdate.setEra(sunEra).setYear(year).setMonth(isoDate.getMonthValue()).setDayOfMonth(isoDate.getDayOfMonth());
        JapaneseChronology.JCAL.normalize(jdate);
        return jdate;
    }

    //-----------------------------------------------------------------------
    @Override
    public JapaneseDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chronoField) {
            if (getLong(chronoField) == newValue) {  // getLong() validates for supported fields
                return this;
            }
            switch (chronoField) {
                case YEAR_OF_ERA:
                case YEAR:
                case ERA: {
                    int nvalue = getChronology().range(chronoField).checkValidIntValue(newValue, chronoField);
                    switch (chronoField) {
                        case YEAR_OF_ERA:
                            return this.withYear(nvalue);
                        case YEAR:
                            return with(isoDate.withYear(nvalue));
                        case ERA: {
                            return this.withYear(JapaneseEra.of(nvalue), yearOfEra);
                        }
                    }
                }
            }
            // YEAR, PROLEPTIC_MONTH and others are same as ISO
            return with(isoDate.with(field, newValue));
        }
        return super.with(field, newValue);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public  JapaneseDate with(TemporalAdjuster adjuster) {
        return super.with(adjuster);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public JapaneseDate plus(TemporalAmount amount) {
        return super.plus(amount);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public JapaneseDate minus(TemporalAmount amount) {
        return super.minus(amount);
    }
    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this date with the year altered.
     * <p>
     * This method changes the year of the date.
     * If the month-day is invalid for the year, then the previous valid day
     * will be selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param era  the era to set in the result, not null
     * @param yearOfEra  the year-of-era to set in the returned date
     * @return a {@code JapaneseDate} based on this date with the requested year, never null
     * @throws DateTimeException if {@code year} is invalid
     */
    private JapaneseDate withYear(JapaneseEra era, int yearOfEra) {
        int year = JapaneseChronology.INSTANCE.prolepticYear(era, yearOfEra);
        return with(isoDate.withYear(year));
    }

    /**
     * Returns a copy of this date with the year-of-era altered.
     * <p>
     * This method changes the year-of-era of the date.
     * If the month-day is invalid for the year, then the previous valid day
     * will be selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param year  the year to set in the returned date
     * @return a {@code JapaneseDate} based on this date with the requested year-of-era, never null
     * @throws DateTimeException if {@code year} is invalid
     */
    private JapaneseDate withYear(int year) {
        return withYear(getEra(), year);
    }

    //-----------------------------------------------------------------------
    @Override
    JapaneseDate plusYears(long years) {
        return with(isoDate.plusYears(years));
    }

    @Override
    JapaneseDate plusMonths(long months) {
        return with(isoDate.plusMonths(months));
    }

    @Override
    JapaneseDate plusWeeks(long weeksToAdd) {
        return with(isoDate.plusWeeks(weeksToAdd));
    }

    @Override
    JapaneseDate plusDays(long days) {
        return with(isoDate.plusDays(days));
    }

    @Override
    public JapaneseDate plus(long amountToAdd, TemporalUnit unit) {
        return super.plus(amountToAdd, unit);
    }

    @Override
    public JapaneseDate minus(long amountToAdd, TemporalUnit unit) {
        return super.minus(amountToAdd, unit);
    }

    @Override
    JapaneseDate minusYears(long yearsToSubtract) {
        return super.minusYears(yearsToSubtract);
    }

    @Override
    JapaneseDate minusMonths(long monthsToSubtract) {
        return super.minusMonths(monthsToSubtract);
    }

    @Override
    JapaneseDate minusWeeks(long weeksToSubtract) {
        return super.minusWeeks(weeksToSubtract);
    }

    @Override
    JapaneseDate minusDays(long daysToSubtract) {
        return super.minusDays(daysToSubtract);
    }

    private JapaneseDate with(LocalDate newDate) {
        return (newDate.equals(isoDate) ? this : new JapaneseDate(newDate));
    }

    @Override        // for javadoc and covariant return type
    @SuppressWarnings("unchecked")
    public final ChronoLocalDateTime<JapaneseDate> atTime(LocalTime localTime) {
        return (ChronoLocalDateTime<JapaneseDate>)super.atTime(localTime);
    }

    @Override
    public ChronoPeriod until(ChronoLocalDate endDate) {
        Period period = isoDate.until(endDate);
        return getChronology().period(period.getYears(), period.getMonths(), period.getDays());
    }

    @Override  // override for performance
    public long toEpochDay() {
        return isoDate.toEpochDay();
    }

    //-------------------------------------------------------------------------
    /**
     * Compares this date to another date, including the chronology.
     * <p>
     * Compares this {@code JapaneseDate} with another ensuring that the date is the same.
     * <p>
     * Only objects of type {@code JapaneseDate} are compared, other types return false.
     * To compare the dates of two {@code TemporalAccessor} instances, including dates
     * in two different chronologies, use {@link ChronoField#EPOCH_DAY} as a comparator.
     *
     * @param obj  the object to check, null returns false
     * @return true if this is equal to the other date
     */
    @Override  // override for performance
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof JapaneseDate otherDate)
            && this.isoDate.equals(otherDate.isoDate);
    }

    /**
     * A hash code for this date.
     *
     * @return a suitable hash code based only on the Chronology and the date
     */
    @Override  // override for performance
    public int hashCode() {
        return getChronology().getId().hashCode() ^ isoDate.hashCode();
    }

    //-----------------------------------------------------------------------
    /**
     * Defend against malicious streams.
     *
     * @param s the stream to read
     * @throws InvalidObjectException always
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    /**
     * Writes the object using a
     * <a href="{@docRoot}/serialized-form.html#java.time.chrono.Ser">dedicated serialized form</a>.
     * @serialData
     * <pre>
     *  out.writeByte(4);                 // identifies a JapaneseDate
     *  out.writeInt(get(YEAR));
     *  out.writeByte(get(MONTH_OF_YEAR));
     *  out.writeByte(get(DAY_OF_MONTH));
     * </pre>
     *
     * @return the instance of {@code Ser}, not null
     */
    @java.io.Serial
    private Object writeReplace() {
        return new Ser(Ser.JAPANESE_DATE_TYPE, this);
    }

    void writeExternal(DataOutput out) throws IOException {
        // JapaneseChronology is implicit in the JAPANESE_DATE_TYPE
        out.writeInt(get(YEAR));
        out.writeByte(get(MONTH_OF_YEAR));
        out.writeByte(get(DAY_OF_MONTH));
    }

    static JapaneseDate readExternal(DataInput in) throws IOException {
        int year = in.readInt();
        int month = in.readByte();
        int dayOfMonth = in.readByte();
        return JapaneseChronology.INSTANCE.date(year, month, dayOfMonth);
    }

}
