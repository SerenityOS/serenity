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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import sun.util.calendar.CalendarSystem;
import sun.util.calendar.LocalGregorianCalendar;

/**
 * The Japanese Imperial calendar system.
 * <p>
 * This chronology defines the rules of the Japanese Imperial calendar system.
 * This calendar system is primarily used in Japan.
 * The Japanese Imperial calendar system is the same as the ISO calendar system
 * apart from the era-based year numbering.
 * <p>
 * Japan introduced the Gregorian calendar starting with Meiji 6.
 * Only Meiji and later eras are supported;
 * dates before Meiji 6, January 1 are not supported.
 * <p>
 * The supported {@code ChronoField} instances are:
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
 *
 * @implSpec
 * This class is immutable and thread-safe.
 *
 * @since 1.8
 */
public final class JapaneseChronology extends AbstractChronology implements Serializable {

    static final LocalGregorianCalendar JCAL =
        (LocalGregorianCalendar) CalendarSystem.forName("japanese");

    // Locale for creating a JapaneseImpericalCalendar.
    static final Locale LOCALE = Locale.forLanguageTag("ja-JP-u-ca-japanese");

    /**
     * Singleton instance for Japanese chronology.
     */
    public static final JapaneseChronology INSTANCE = new JapaneseChronology();

    /**
     * Serialization version.
     */
    @java.io.Serial
    private static final long serialVersionUID = 459996390165777884L;

    //-----------------------------------------------------------------------
    /**
     * Restricted constructor.
     */
    private JapaneseChronology() {
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the ID of the chronology - 'Japanese'.
     * <p>
     * The ID uniquely identifies the {@code Chronology}.
     * It can be used to lookup the {@code Chronology} using {@link Chronology#of(String)}.
     *
     * @return the chronology ID - 'Japanese'
     * @see #getCalendarType()
     */
    @Override
    public String getId() {
        return "Japanese";
    }

    /**
     * Gets the calendar type of the underlying calendar system - 'japanese'.
     * <p>
     * The calendar type is an identifier defined by the
     * <em>Unicode Locale Data Markup Language (LDML)</em> specification.
     * It can be used to lookup the {@code Chronology} using {@link Chronology#of(String)}.
     * It can also be used as part of a locale, accessible via
     * {@link Locale#getUnicodeLocaleType(String)} with the key 'ca'.
     *
     * @return the calendar system type - 'japanese'
     * @see #getId()
     */
    @Override
    public String getCalendarType() {
        return "japanese";
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains a local date in Japanese calendar system from the
     * era, year-of-era, month-of-year and day-of-month fields.
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
     * @param yearOfEra  the year-of-era
     * @param month  the month-of-year
     * @param dayOfMonth  the day-of-month
     * @return the Japanese local date, not null
     * @throws DateTimeException if unable to create the date
     * @throws ClassCastException if the {@code era} is not a {@code JapaneseEra}
     */
    @Override
    public JapaneseDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        if (!(era instanceof JapaneseEra jera)) {
            throw new ClassCastException("Era must be JapaneseEra");
        }
        return JapaneseDate.of(jera, yearOfEra, month, dayOfMonth);
    }

    /**
     * Obtains a local date in Japanese calendar system from the
     * proleptic-year, month-of-year and day-of-month fields.
     * <p>
     * The Japanese proleptic year, month and day-of-month are the same as those
     * in the ISO calendar system. They are not reset when the era changes.
     *
     * @param prolepticYear  the proleptic-year
     * @param month  the month-of-year
     * @param dayOfMonth  the day-of-month
     * @return the Japanese local date, not null
     * @throws DateTimeException if unable to create the date
     */
    @Override
    public JapaneseDate date(int prolepticYear, int month, int dayOfMonth) {
        return new JapaneseDate(LocalDate.of(prolepticYear, month, dayOfMonth));
    }

    /**
     * Obtains a local date in Japanese calendar system from the
     * era, year-of-era and day-of-year fields.
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
     * @param yearOfEra  the year-of-era
     * @param dayOfYear  the day-of-year
     * @return the Japanese local date, not null
     * @throws DateTimeException if unable to create the date
     * @throws ClassCastException if the {@code era} is not a {@code JapaneseEra}
     */
    @Override
    public JapaneseDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return JapaneseDate.ofYearDay((JapaneseEra) era, yearOfEra, dayOfYear);
    }

    /**
     * Obtains a local date in Japanese calendar system from the
     * proleptic-year and day-of-year fields.
     * <p>
     * The day-of-year in this factory is expressed relative to the start of the proleptic year.
     * The Japanese proleptic year and day-of-year are the same as those in the ISO calendar system.
     * They are not reset when the era changes.
     *
     * @param prolepticYear  the proleptic-year
     * @param dayOfYear  the day-of-year
     * @return the Japanese local date, not null
     * @throws DateTimeException if unable to create the date
     */
    @Override
    public JapaneseDate dateYearDay(int prolepticYear, int dayOfYear) {
        return new JapaneseDate(LocalDate.ofYearDay(prolepticYear, dayOfYear));
    }

    /**
     * Obtains a local date in the Japanese calendar system from the epoch-day.
     *
     * @param epochDay  the epoch day
     * @return the Japanese local date, not null
     * @throws DateTimeException if unable to create the date
     */
    @Override  // override with covariant return type
    public JapaneseDate dateEpochDay(long epochDay) {
        return new JapaneseDate(LocalDate.ofEpochDay(epochDay));
    }

    @Override
    public JapaneseDate dateNow() {
        return dateNow(Clock.systemDefaultZone());
    }

    @Override
    public JapaneseDate dateNow(ZoneId zone) {
        return dateNow(Clock.system(zone));
    }

    @Override
    public JapaneseDate dateNow(Clock clock) {
        return date(LocalDate.now(clock));
    }

    @Override
    public JapaneseDate date(TemporalAccessor temporal) {
        if (temporal instanceof JapaneseDate) {
            return (JapaneseDate) temporal;
        }
        return new JapaneseDate(LocalDate.from(temporal));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChronoLocalDateTime<JapaneseDate> localDateTime(TemporalAccessor temporal) {
        return (ChronoLocalDateTime<JapaneseDate>)super.localDateTime(temporal);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChronoZonedDateTime<JapaneseDate> zonedDateTime(TemporalAccessor temporal) {
        return (ChronoZonedDateTime<JapaneseDate>)super.zonedDateTime(temporal);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChronoZonedDateTime<JapaneseDate> zonedDateTime(Instant instant, ZoneId zone) {
        return (ChronoZonedDateTime<JapaneseDate>)super.zonedDateTime(instant, zone);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the specified year is a leap year.
     * <p>
     * Japanese calendar leap years occur exactly in line with ISO leap years.
     * This method does not validate the year passed in, and only has a
     * well-defined result for years in the supported range.
     *
     * @param prolepticYear  the proleptic-year to check, not validated for range
     * @return true if the year is a leap year
     */
    @Override
    public boolean isLeapYear(long prolepticYear) {
        return IsoChronology.INSTANCE.isLeapYear(prolepticYear);
    }

    @Override
    public int prolepticYear(Era era, int yearOfEra) {
        if (era instanceof JapaneseEra == false) {
            throw new ClassCastException("Era must be JapaneseEra");
        }

        JapaneseEra jera = (JapaneseEra) era;
        int gregorianYear = jera.getPrivateEra().getSinceDate().getYear() + yearOfEra - 1;
        if (yearOfEra == 1) {
            return gregorianYear;
        }
        if (gregorianYear >= Year.MIN_VALUE && gregorianYear <= Year.MAX_VALUE) {
            LocalGregorianCalendar.Date jdate = JCAL.newCalendarDate(null);
            jdate.setEra(jera.getPrivateEra()).setDate(yearOfEra, 1, 1);
            if (JapaneseChronology.JCAL.validate(jdate)) {
                return gregorianYear;
            }
        }
        throw new DateTimeException("Invalid yearOfEra value");
    }

    /**
     * Returns the calendar system era object from the given numeric value.
     *
     * The numeric values supported by this method are the same as the
     * numeric values supported by {@link JapaneseEra#of(int)}.
     *
     * @param eraValue  the era value
     * @return the Japanese {@code Era} for the given numeric era value
     * @throws DateTimeException if {@code eraValue} is invalid
     */
    @Override
    public JapaneseEra eraOf(int eraValue) {
        return JapaneseEra.of(eraValue);
    }

    @Override
    public List<Era> eras() {
        return List.of(JapaneseEra.values());
    }

    JapaneseEra getCurrentEra() {
        // Assume that the last JapaneseEra is the current one.
        JapaneseEra[] eras = JapaneseEra.values();
        return eras[eras.length - 1];
    }

    //-----------------------------------------------------------------------
    @Override
    public ValueRange range(ChronoField field) {
        switch (field) {
            case ALIGNED_DAY_OF_WEEK_IN_MONTH:
            case ALIGNED_DAY_OF_WEEK_IN_YEAR:
            case ALIGNED_WEEK_OF_MONTH:
            case ALIGNED_WEEK_OF_YEAR:
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            case YEAR_OF_ERA: {
                Calendar jcal = Calendar.getInstance(LOCALE);
                int startYear = getCurrentEra().getPrivateEra().getSinceDate().getYear();
                return ValueRange.of(1, jcal.getGreatestMinimum(Calendar.YEAR),
                        jcal.getLeastMaximum(Calendar.YEAR) + 1, // +1 due to the different definitions
                        Year.MAX_VALUE - startYear);
            }
            case DAY_OF_YEAR: {
                Calendar jcal = Calendar.getInstance(LOCALE);
                int fieldIndex = Calendar.DAY_OF_YEAR;
                return ValueRange.of(jcal.getMinimum(fieldIndex), jcal.getGreatestMinimum(fieldIndex),
                        jcal.getLeastMaximum(fieldIndex), jcal.getMaximum(fieldIndex));
            }
            case YEAR:
                return ValueRange.of(JapaneseDate.MEIJI_6_ISODATE.getYear(), Year.MAX_VALUE);
            case ERA:
                return ValueRange.of(JapaneseEra.MEIJI.getValue(), getCurrentEra().getValue());
            default:
                return field.range();
        }
    }

    //-----------------------------------------------------------------------
    @Override  // override for return type
    public JapaneseDate resolveDate(Map <TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        return (JapaneseDate) super.resolveDate(fieldValues, resolverStyle);
    }

    @Override  // override for special Japanese behavior
    ChronoLocalDate resolveYearOfEra(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        // validate era and year-of-era
        Long eraLong = fieldValues.get(ERA);
        JapaneseEra era = null;
        if (eraLong != null) {
            era = eraOf(range(ERA).checkValidIntValue(eraLong, ERA));  // always validated
        }
        Long yoeLong = fieldValues.get(YEAR_OF_ERA);
        int yoe = 0;
        if (yoeLong != null) {
            yoe = range(YEAR_OF_ERA).checkValidIntValue(yoeLong, YEAR_OF_ERA);  // always validated
        }
        // if only year-of-era and no year then invent era unless strict
        if (era == null && yoeLong != null && fieldValues.containsKey(YEAR) == false && resolverStyle != ResolverStyle.STRICT) {
            era = JapaneseEra.values()[JapaneseEra.values().length - 1];
        }
        // if both present, then try to create date
        if (yoeLong != null && era != null) {
            if (fieldValues.containsKey(MONTH_OF_YEAR)) {
                if (fieldValues.containsKey(DAY_OF_MONTH)) {
                    return resolveYMD(era, yoe, fieldValues, resolverStyle);
                }
            }
            if (fieldValues.containsKey(DAY_OF_YEAR)) {
                return resolveYD(era, yoe, fieldValues, resolverStyle);
            }
        }
        return null;
    }

    private int prolepticYearLenient(JapaneseEra era, int yearOfEra) {
        return era.getPrivateEra().getSinceDate().getYear() + yearOfEra - 1;
    }

    private ChronoLocalDate resolveYMD(JapaneseEra era, int yoe, Map<TemporalField,Long> fieldValues, ResolverStyle resolverStyle) {
        fieldValues.remove(ERA);
        fieldValues.remove(YEAR_OF_ERA);
        if (resolverStyle == ResolverStyle.LENIENT) {
            int y = prolepticYearLenient(era, yoe);
            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
            long days = Math.subtractExact(fieldValues.remove(DAY_OF_MONTH), 1);
            return date(y, 1, 1).plus(months, MONTHS).plus(days, DAYS);
        }
        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
        int dom = range(DAY_OF_MONTH).checkValidIntValue(fieldValues.remove(DAY_OF_MONTH), DAY_OF_MONTH);
        if (resolverStyle == ResolverStyle.SMART) {  // previous valid
            if (yoe < 1) {
                throw new DateTimeException("Invalid YearOfEra: " + yoe);
            }
            int y = prolepticYearLenient(era, yoe);
            JapaneseDate result;
            try {
                result = date(y, moy, dom);
            } catch (DateTimeException ex) {
                result = date(y, moy, 1).with(TemporalAdjusters.lastDayOfMonth());
            }
            // handle the era being changed
            // only allow if the new date is in the same Jan-Dec as the era change
            // determine by ensuring either original yoe or result yoe is 1
            if (result.getEra() != era && result.get(YEAR_OF_ERA) > 1 && yoe > 1) {
                throw new DateTimeException("Invalid YearOfEra for Era: " + era + " " + yoe);
            }
            return result;
        }
        return date(era, yoe, moy, dom);
    }

    private ChronoLocalDate resolveYD(JapaneseEra era, int yoe, Map <TemporalField,Long> fieldValues, ResolverStyle resolverStyle) {
        fieldValues.remove(ERA);
        fieldValues.remove(YEAR_OF_ERA);
        if (resolverStyle == ResolverStyle.LENIENT) {
            int y = prolepticYearLenient(era, yoe);
            long days = Math.subtractExact(fieldValues.remove(DAY_OF_YEAR), 1);
            return dateYearDay(y, 1).plus(days, DAYS);
        }
        int doy = range(DAY_OF_YEAR).checkValidIntValue(fieldValues.remove(DAY_OF_YEAR), DAY_OF_YEAR);
        return dateYearDay(era, yoe, doy);  // smart is same as strict
    }

    //-----------------------------------------------------------------------
    /**
     * Writes the Chronology using a
     * <a href="{@docRoot}/serialized-form.html#java.time.chrono.Ser">dedicated serialized form</a>.
     * @serialData
     * <pre>
     *  out.writeByte(1);     // identifies a Chronology
     *  out.writeUTF(getId());
     * </pre>
     *
     * @return the instance of {@code Ser}, not null
     */
    @Override
    @java.io.Serial
    Object writeReplace() {
        return super.writeReplace();
    }

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
}
