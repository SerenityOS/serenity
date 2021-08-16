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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
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

/**
 * A date in the Hijrah calendar system.
 * <p>
 * This date operates using one of several variants of the
 * {@linkplain HijrahChronology Hijrah calendar}.
 * <p>
 * The Hijrah calendar has a different total of days in a year than
 * Gregorian calendar, and the length of each month is based on the period
 * of a complete revolution of the moon around the earth
 * (as between successive new moons).
 * Refer to the {@link HijrahChronology} for details of supported variants.
 * <p>
 * Each HijrahDate is created bound to a particular HijrahChronology,
 * The same chronology is propagated to each HijrahDate computed from the date.
 * To use a different Hijrah variant, its HijrahChronology can be used
 * to create new HijrahDate instances.
 * Alternatively, the {@link #withVariant} method can be used to convert
 * to a new HijrahChronology.
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
public final class HijrahDate
        extends ChronoLocalDateImpl<HijrahDate>
        implements ChronoLocalDate, Serializable {

    /**
     * Serialization version.
     */
    @java.io.Serial
    private static final long serialVersionUID = -5207853542612002020L;
    /**
     * The Chronology of this HijrahDate.
     */
    private final transient HijrahChronology chrono;
    /**
     * The proleptic year.
     */
    private final transient int prolepticYear;
    /**
     * The month-of-year.
     */
    private final transient int monthOfYear;
    /**
     * The day-of-month.
     */
    private final transient int dayOfMonth;

    //-------------------------------------------------------------------------
    /**
     * Obtains an instance of {@code HijrahDate} from the Hijrah proleptic year,
     * month-of-year and day-of-month.
     *
     * @param prolepticYear  the proleptic year to represent in the Hijrah calendar
     * @param monthOfYear  the month-of-year to represent, from 1 to 12
     * @param dayOfMonth  the day-of-month to represent, from 1 to 30
     * @return the Hijrah date, never null
     * @throws DateTimeException if the value of any field is out of range
     */
    static HijrahDate of(HijrahChronology chrono, int prolepticYear, int monthOfYear, int dayOfMonth) {
        return new HijrahDate(chrono, prolepticYear, monthOfYear, dayOfMonth);
    }

    /**
     * Returns a HijrahDate for the chronology and epochDay.
     * @param chrono The Hijrah chronology
     * @param epochDay the epoch day
     * @return a HijrahDate for the epoch day; non-null
     */
    static HijrahDate ofEpochDay(HijrahChronology chrono, long epochDay) {
        return new HijrahDate(chrono, epochDay);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains the current {@code HijrahDate} of the Islamic Umm Al-Qura calendar
     * in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current date.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current date using the system clock and default time-zone, not null
     */
    public static HijrahDate now() {
        return now(Clock.systemDefaultZone());
    }

    /**
     * Obtains the current {@code HijrahDate} of the Islamic Umm Al-Qura calendar
     * in the specified time-zone.
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
    public static HijrahDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    /**
     * Obtains the current {@code HijrahDate} of the Islamic Umm Al-Qura calendar
     * from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current date - today.
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@linkplain Clock dependency injection}.
     *
     * @param clock  the clock to use, not null
     * @return the current date, not null
     * @throws DateTimeException if the current date cannot be obtained
     */
    public static HijrahDate now(Clock clock) {
        return HijrahDate.ofEpochDay(HijrahChronology.INSTANCE, LocalDate.now(clock).toEpochDay());
    }

    /**
     * Obtains a {@code HijrahDate} of the Islamic Umm Al-Qura calendar
     * from the proleptic-year, month-of-year and day-of-month fields.
     * <p>
     * This returns a {@code HijrahDate} with the specified fields.
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     *
     * @param prolepticYear  the Hijrah proleptic-year
     * @param month  the Hijrah month-of-year, from 1 to 12
     * @param dayOfMonth  the Hijrah day-of-month, from 1 to 30
     * @return the date in Hijrah calendar system, not null
     * @throws DateTimeException if the value of any field is out of range,
     *  or if the day-of-month is invalid for the month-year
     */
    public static HijrahDate of(int prolepticYear, int month, int dayOfMonth) {
        return HijrahChronology.INSTANCE.date(prolepticYear, month, dayOfMonth);
    }

    /**
     * Obtains a {@code HijrahDate} of the Islamic Umm Al-Qura calendar from a temporal object.
     * <p>
     * This obtains a date in the Hijrah calendar system based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code HijrahDate}.
     * <p>
     * The conversion typically uses the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
     * field, which is standardized across calendar systems.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code HijrahDate::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the date in Hijrah calendar system, not null
     * @throws DateTimeException if unable to convert to a {@code HijrahDate}
     */
    public static HijrahDate from(TemporalAccessor temporal) {
        return HijrahChronology.INSTANCE.date(temporal);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructs an {@code HijrahDate} with the proleptic-year, month-of-year and
     * day-of-month fields.
     *
     * @param chrono The chronology to create the date with
     * @param prolepticYear the proleptic year
     * @param monthOfYear the month of year
     * @param dayOfMonth the day of month
     */
    private HijrahDate(HijrahChronology chrono, int prolepticYear, int monthOfYear, int dayOfMonth) {
        // Computing the Gregorian day checks the valid ranges
        chrono.getEpochDay(prolepticYear, monthOfYear, dayOfMonth);

        this.chrono = chrono;
        this.prolepticYear = prolepticYear;
        this.monthOfYear = monthOfYear;
        this.dayOfMonth = dayOfMonth;
    }

    /**
     * Constructs an instance with the Epoch Day.
     *
     * @param epochDay  the epochDay
     */
    private HijrahDate(HijrahChronology chrono, long epochDay) {
        int[] dateInfo = chrono.getHijrahDateInfo((int)epochDay);

        this.chrono = chrono;
        this.prolepticYear = dateInfo[0];
        this.monthOfYear = dateInfo[1];
        this.dayOfMonth = dateInfo[2];
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the chronology of this date, which is the Hijrah calendar system.
     * <p>
     * The {@code Chronology} represents the calendar system in use.
     * The era and other fields in {@link ChronoField} are defined by the chronology.
     *
     * @return the Hijrah chronology, not null
     */
    @Override
    public HijrahChronology getChronology() {
        return chrono;
    }

    /**
     * Gets the era applicable at this date.
     * <p>
     * The Hijrah calendar system has one era, 'AH',
     * defined by {@link HijrahEra}.
     *
     * @return the era applicable at this date, not null
     */
    @Override
    public HijrahEra getEra() {
        return HijrahEra.AH;
    }

    /**
     * Returns the length of the month represented by this date.
     * <p>
     * This returns the length of the month in days.
     * Month lengths in the Hijrah calendar system vary between 29 and 30 days.
     *
     * @return the length of the month in days
     */
    @Override
    public int lengthOfMonth() {
        return chrono.getMonthLength(prolepticYear, monthOfYear);
    }

    /**
     * Returns the length of the year represented by this date.
     * <p>
     * This returns the length of the year in days.
     * A Hijrah calendar system year is typically shorter than
     * that of the ISO calendar system.
     *
     * @return the length of the year in days
     */
    @Override
    public int lengthOfYear() {
        return chrono.getYearLength(prolepticYear);
    }

    //-----------------------------------------------------------------------
    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            if (isSupported(field)) {
                ChronoField f = (ChronoField) field;
                return switch (f) {
                    case DAY_OF_MONTH -> ValueRange.of(1, lengthOfMonth());
                    case DAY_OF_YEAR -> ValueRange.of(1, lengthOfYear());
                    case ALIGNED_WEEK_OF_MONTH -> ValueRange.of(1, 5); // TODO
                    // TODO does the limited range of valid years cause years to
                    // start/end part way through? that would affect range
                    default -> getChronology().range(f);
                };
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            return switch ((ChronoField) field) {
                case DAY_OF_WEEK                  ->  getDayOfWeek();
                case ALIGNED_DAY_OF_WEEK_IN_MONTH ->  ((dayOfMonth - 1) % 7) + 1;
                case ALIGNED_DAY_OF_WEEK_IN_YEAR  ->  ((getDayOfYear() - 1) % 7) + 1;
                case DAY_OF_MONTH                 ->  this.dayOfMonth;
                case DAY_OF_YEAR                  ->  this.getDayOfYear();
                case EPOCH_DAY                    ->  toEpochDay();
                case ALIGNED_WEEK_OF_MONTH        ->  ((dayOfMonth - 1) / 7) + 1;
                case ALIGNED_WEEK_OF_YEAR         ->  ((getDayOfYear() - 1) / 7) + 1;
                case MONTH_OF_YEAR                ->  monthOfYear;
                case PROLEPTIC_MONTH              ->  getProlepticMonth();
                case YEAR_OF_ERA                  ->  prolepticYear;
                case YEAR                         ->  prolepticYear;
                case ERA                          ->  getEraValue();
                default -> throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            };
        }
        return field.getFrom(this);
    }

    private long getProlepticMonth() {
        return prolepticYear * 12L + monthOfYear - 1;
    }

    @Override
    public HijrahDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chronoField) {
            // not using checkValidIntValue so EPOCH_DAY and PROLEPTIC_MONTH work
            chrono.range(chronoField).checkValidValue(newValue, chronoField);    // TODO: validate value
            int nvalue = (int) newValue;
            return switch (chronoField) {
                case DAY_OF_WEEK                  ->  plusDays(newValue - getDayOfWeek());
                case ALIGNED_DAY_OF_WEEK_IN_MONTH ->  plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                case ALIGNED_DAY_OF_WEEK_IN_YEAR  ->  plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                case DAY_OF_MONTH                 ->  resolvePreviousValid(prolepticYear, monthOfYear, nvalue);
                case DAY_OF_YEAR                  ->  plusDays(Math.min(nvalue, lengthOfYear()) - getDayOfYear());
                case EPOCH_DAY                    ->  new HijrahDate(chrono, newValue);
                case ALIGNED_WEEK_OF_MONTH        ->  plusDays((newValue - getLong(ALIGNED_WEEK_OF_MONTH)) * 7);
                case ALIGNED_WEEK_OF_YEAR         ->  plusDays((newValue - getLong(ALIGNED_WEEK_OF_YEAR)) * 7);
                case MONTH_OF_YEAR                ->  resolvePreviousValid(prolepticYear, nvalue, dayOfMonth);
                case PROLEPTIC_MONTH              ->  plusMonths(newValue - getProlepticMonth());
                case YEAR_OF_ERA                  ->  resolvePreviousValid(prolepticYear >= 1 ? nvalue : 1 - nvalue, monthOfYear, dayOfMonth);
                case YEAR                         ->  resolvePreviousValid(nvalue, monthOfYear, dayOfMonth);
                case ERA                          ->  resolvePreviousValid(1 - prolepticYear, monthOfYear, dayOfMonth);
                default -> throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            };
        }
        return super.with(field, newValue);
    }

    private HijrahDate resolvePreviousValid(int prolepticYear, int month, int day) {
        int monthDays = chrono.getMonthLength(prolepticYear, month);
        if (day > monthDays) {
            day = monthDays;
        }
        return HijrahDate.of(chrono, prolepticYear, month, day);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException if unable to make the adjustment.
     *     For example, if the adjuster requires an ISO chronology
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public  HijrahDate with(TemporalAdjuster adjuster) {
        return super.with(adjuster);
    }

    /**
     * Returns a {@code HijrahDate} with the Chronology requested.
     * <p>
     * The year, month, and day are checked against the new requested
     * HijrahChronology.  If the chronology has a shorter month length
     * for the month, the day is reduced to be the last day of the month.
     *
     * @param chronology the new HijrahChonology, non-null
     * @return a HijrahDate with the requested HijrahChronology, non-null
     */
    public HijrahDate withVariant(HijrahChronology chronology) {
        if (chrono == chronology) {
            return this;
        }
        // Like resolvePreviousValid the day is constrained to stay in the same month
        int monthDays = chronology.getDayOfYear(prolepticYear, monthOfYear);
        return HijrahDate.of(chronology, prolepticYear, monthOfYear,(dayOfMonth > monthDays) ? monthDays : dayOfMonth );
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public HijrahDate plus(TemporalAmount amount) {
        return super.plus(amount);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public HijrahDate minus(TemporalAmount amount) {
        return super.minus(amount);
    }

    @Override
    public long toEpochDay() {
        return chrono.getEpochDay(prolepticYear, monthOfYear, dayOfMonth);
    }

    /**
     * Gets the day-of-year field.
     * <p>
     * This method returns the primitive {@code int} value for the day-of-year.
     *
     * @return the day-of-year
     */
    private int getDayOfYear() {
        return chrono.getDayOfYear(prolepticYear, monthOfYear) + dayOfMonth;
    }

    /**
     * Gets the day-of-week value.
     *
     * @return the day-of-week; computed from the epochday
     */
    private int getDayOfWeek() {
        int dow0 = Math.floorMod(toEpochDay() + 3, 7);
        return dow0 + 1;
    }

    /**
     * Gets the Era of this date.
     *
     * @return the Era of this date; computed from epochDay
     */
    private int getEraValue() {
        return (prolepticYear > 1 ? 1 : 0);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the year is a leap year, according to the Hijrah calendar system rules.
     *
     * @return true if this date is in a leap year
     */
    @Override
    public boolean isLeapYear() {
        return chrono.isLeapYear(prolepticYear);
    }

    //-----------------------------------------------------------------------
    @Override
    HijrahDate plusYears(long years) {
        if (years == 0) {
            return this;
        }
        int newYear = Math.addExact(this.prolepticYear, (int)years);
        return resolvePreviousValid(newYear, monthOfYear, dayOfMonth);
    }

    @Override
    HijrahDate plusMonths(long monthsToAdd) {
        if (monthsToAdd == 0) {
            return this;
        }
        long monthCount = prolepticYear * 12L + (monthOfYear - 1);
        long calcMonths = monthCount + monthsToAdd;  // safe overflow
        int newYear = chrono.checkValidYear(Math.floorDiv(calcMonths, 12L));
        int newMonth = (int)Math.floorMod(calcMonths, 12L) + 1;
        return resolvePreviousValid(newYear, newMonth, dayOfMonth);
    }

    @Override
    HijrahDate plusWeeks(long weeksToAdd) {
        return super.plusWeeks(weeksToAdd);
    }

    @Override
    HijrahDate plusDays(long days) {
        return new HijrahDate(chrono, toEpochDay() + days);
    }

    @Override
    public HijrahDate plus(long amountToAdd, TemporalUnit unit) {
        return super.plus(amountToAdd, unit);
    }

    @Override
    public HijrahDate minus(long amountToSubtract, TemporalUnit unit) {
        return super.minus(amountToSubtract, unit);
    }

    @Override
    HijrahDate minusYears(long yearsToSubtract) {
        return super.minusYears(yearsToSubtract);
    }

    @Override
    HijrahDate minusMonths(long monthsToSubtract) {
        return super.minusMonths(monthsToSubtract);
    }

    @Override
    HijrahDate minusWeeks(long weeksToSubtract) {
        return super.minusWeeks(weeksToSubtract);
    }

    @Override
    HijrahDate minusDays(long daysToSubtract) {
        return super.minusDays(daysToSubtract);
    }

    @Override        // for javadoc and covariant return type
    @SuppressWarnings("unchecked")
    public final ChronoLocalDateTime<HijrahDate> atTime(LocalTime localTime) {
        return (ChronoLocalDateTime<HijrahDate>)super.atTime(localTime);
    }

    @Override
    public ChronoPeriod until(ChronoLocalDate endDate) {
        // TODO: untested
        HijrahDate end = getChronology().date(endDate);
        long totalMonths = (end.prolepticYear - this.prolepticYear) * 12 + (end.monthOfYear - this.monthOfYear);  // safe
        int days = end.dayOfMonth - this.dayOfMonth;
        if (totalMonths > 0 && days < 0) {
            totalMonths--;
            HijrahDate calcDate = this.plusMonths(totalMonths);
            days = (int) (end.toEpochDay() - calcDate.toEpochDay());  // safe
        } else if (totalMonths < 0 && days > 0) {
            totalMonths++;
            days -= end.lengthOfMonth();
        }
        long years = totalMonths / 12;  // safe
        int months = (int) (totalMonths % 12);  // safe
        return getChronology().period(Math.toIntExact(years), months, days);
    }

    //-------------------------------------------------------------------------
    /**
     * Compares this date to another date, including the chronology.
     * <p>
     * Compares this {@code HijrahDate} with another ensuring that the date is the same.
     * <p>
     * Only objects of type {@code HijrahDate} are compared, other types return false.
     * To compare the dates of two {@code TemporalAccessor} instances, including dates
     * in two different chronologies, use {@link ChronoField#EPOCH_DAY} as a comparator.
     *
     * @param obj  the object to check, null returns false
     * @return true if this is equal to the other date and the Chronologies are equal
     */
    @Override  // override for performance
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof HijrahDate otherDate)
                && prolepticYear == otherDate.prolepticYear
                && this.monthOfYear == otherDate.monthOfYear
                && this.dayOfMonth == otherDate.dayOfMonth
                && getChronology().equals(otherDate.getChronology());
    }

    /**
     * A hash code for this date.
     *
     * @return a suitable hash code based only on the Chronology and the date
     */
    @Override  // override for performance
    public int hashCode() {
        int yearValue = prolepticYear;
        int monthValue = monthOfYear;
        int dayValue = dayOfMonth;
        return getChronology().getId().hashCode() ^ (yearValue & 0xFFFFF800)
                ^ ((yearValue << 11) + (monthValue << 6) + (dayValue));
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
     *  out.writeByte(6);                 // identifies a HijrahDate
     *  out.writeObject(chrono);          // the HijrahChronology variant
     *  out.writeInt(get(YEAR));
     *  out.writeByte(get(MONTH_OF_YEAR));
     *  out.writeByte(get(DAY_OF_MONTH));
     * </pre>
     *
     * @return the instance of {@code Ser}, not null
     */
    @java.io.Serial
    private Object writeReplace() {
        return new Ser(Ser.HIJRAH_DATE_TYPE, this);
    }

    void writeExternal(ObjectOutput out) throws IOException {
        // HijrahChronology is implicit in the Hijrah_DATE_TYPE
        out.writeObject(getChronology());
        out.writeInt(get(YEAR));
        out.writeByte(get(MONTH_OF_YEAR));
        out.writeByte(get(DAY_OF_MONTH));
    }

    static HijrahDate readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        HijrahChronology chrono = (HijrahChronology) in.readObject();
        int year = in.readInt();
        int month = in.readByte();
        int dayOfMonth = in.readByte();
        return chrono.date(year, month, dayOfMonth);
    }

}
