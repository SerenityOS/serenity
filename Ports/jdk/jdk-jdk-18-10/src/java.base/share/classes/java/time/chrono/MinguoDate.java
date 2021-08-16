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

import static java.time.chrono.MinguoChronology.YEARS_DIFFERENCE;
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
import java.util.Objects;

/**
 * A date in the Minguo calendar system.
 * <p>
 * This date operates using the {@linkplain MinguoChronology Minguo calendar}.
 * This calendar system is primarily used in the Republic of China, often known as Taiwan.
 * Dates are aligned such that {@code 0001-01-01 (Minguo)} is {@code 1912-01-01 (ISO)}.
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
public final class MinguoDate
        extends ChronoLocalDateImpl<MinguoDate>
        implements ChronoLocalDate, Serializable {

    /**
     * Serialization version.
     */
    @java.io.Serial
    private static final long serialVersionUID = 1300372329181994526L;

    /**
     * The underlying date.
     */
    private final transient LocalDate isoDate;

    //-----------------------------------------------------------------------
    /**
     * Obtains the current {@code MinguoDate} from the system clock in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current date.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current date using the system clock and default time-zone, not null
     */
    public static MinguoDate now() {
        return now(Clock.systemDefaultZone());
    }

    /**
     * Obtains the current {@code MinguoDate} from the system clock in the specified time-zone.
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
    public static MinguoDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    /**
     * Obtains the current {@code MinguoDate} from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current date - today.
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@linkplain Clock dependency injection}.
     *
     * @param clock  the clock to use, not null
     * @return the current date, not null
     * @throws DateTimeException if the current date cannot be obtained
     */
    public static MinguoDate now(Clock clock) {
        return new MinguoDate(LocalDate.now(clock));
    }

    /**
     * Obtains a {@code MinguoDate} representing a date in the Minguo calendar
     * system from the proleptic-year, month-of-year and day-of-month fields.
     * <p>
     * This returns a {@code MinguoDate} with the specified fields.
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     *
     * @param prolepticYear  the Minguo proleptic-year
     * @param month  the Minguo month-of-year, from 1 to 12
     * @param dayOfMonth  the Minguo day-of-month, from 1 to 31
     * @return the date in Minguo calendar system, not null
     * @throws DateTimeException if the value of any field is out of range,
     *  or if the day-of-month is invalid for the month-year
     */
    public static MinguoDate of(int prolepticYear, int month, int dayOfMonth) {
        return new MinguoDate(LocalDate.of(prolepticYear + YEARS_DIFFERENCE, month, dayOfMonth));
    }

    /**
     * Obtains a {@code MinguoDate} from a temporal object.
     * <p>
     * This obtains a date in the Minguo calendar system based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code MinguoDate}.
     * <p>
     * The conversion typically uses the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
     * field, which is standardized across calendar systems.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code MinguoDate::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the date in Minguo calendar system, not null
     * @throws DateTimeException if unable to convert to a {@code MinguoDate}
     */
    public static MinguoDate from(TemporalAccessor temporal) {
        return MinguoChronology.INSTANCE.date(temporal);
    }

    //-----------------------------------------------------------------------
    /**
     * Creates an instance from an ISO date.
     *
     * @param isoDate  the standard local date, validated not null
     */
    MinguoDate(LocalDate isoDate) {
        Objects.requireNonNull(isoDate, "isoDate");
        this.isoDate = isoDate;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the chronology of this date, which is the Minguo calendar system.
     * <p>
     * The {@code Chronology} represents the calendar system in use.
     * The era and other fields in {@link ChronoField} are defined by the chronology.
     *
     * @return the Minguo chronology, not null
     */
    @Override
    public MinguoChronology getChronology() {
        return MinguoChronology.INSTANCE;
    }

    /**
     * Gets the era applicable at this date.
     * <p>
     * The Minguo calendar system has two eras, 'ROC' and 'BEFORE_ROC',
     * defined by {@link MinguoEra}.
     *
     * @return the era applicable at this date, not null
     */
    @Override
    public MinguoEra getEra() {
        return (getProlepticYear() >= 1 ? MinguoEra.ROC : MinguoEra.BEFORE_ROC);
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

    //-----------------------------------------------------------------------
    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            if (isSupported(field)) {
                ChronoField f = (ChronoField) field;
                switch (f) {
                    case DAY_OF_MONTH:
                    case DAY_OF_YEAR:
                    case ALIGNED_WEEK_OF_MONTH:
                        return isoDate.range(field);
                    case YEAR_OF_ERA: {
                        ValueRange range = YEAR.range();
                        long max = (getProlepticYear() <= 0 ? -range.getMinimum() + 1 + YEARS_DIFFERENCE : range.getMaximum() - YEARS_DIFFERENCE);
                        return ValueRange.of(1, max);
                    }
                }
                return getChronology().range(f);
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            switch ((ChronoField) field) {
                case PROLEPTIC_MONTH:
                    return getProlepticMonth();
                case YEAR_OF_ERA: {
                    int prolepticYear = getProlepticYear();
                    return (prolepticYear >= 1 ? prolepticYear : 1 - prolepticYear);
                }
                case YEAR:
                    return getProlepticYear();
                case ERA:
                    return (getProlepticYear() >= 1 ? 1 : 0);
            }
            return isoDate.getLong(field);
        }
        return field.getFrom(this);
    }

    private long getProlepticMonth() {
        return getProlepticYear() * 12L + isoDate.getMonthValue() - 1;
    }

    private int getProlepticYear() {
        return isoDate.getYear() - YEARS_DIFFERENCE;
    }

    //-----------------------------------------------------------------------
    @Override
    public MinguoDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chronoField) {
            if (getLong(chronoField) == newValue) {
                return this;
            }
            return switch (chronoField) {
                case PROLEPTIC_MONTH -> {
                    getChronology().range(chronoField).checkValidValue(newValue, chronoField);
                    yield plusMonths(newValue - getProlepticMonth());
                }
                case YEAR_OF_ERA -> {
                    int nvalue = getChronology().range(chronoField).checkValidIntValue(newValue, chronoField);
                    yield with(isoDate.withYear(getProlepticYear() >= 1 ? nvalue + YEARS_DIFFERENCE : (1 - nvalue) + YEARS_DIFFERENCE));
                }
                case YEAR -> {
                    int nvalue = getChronology().range(chronoField).checkValidIntValue(newValue, chronoField);
                    yield with(isoDate.withYear(nvalue + YEARS_DIFFERENCE));
                }
                case ERA -> with(isoDate.withYear((1 - getProlepticYear()) + YEARS_DIFFERENCE));

                default -> with(isoDate.with(field, newValue));
            };
        }
        return super.with(field, newValue);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public  MinguoDate with(TemporalAdjuster adjuster) {
        return super.with(adjuster);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public MinguoDate plus(TemporalAmount amount) {
        return super.plus(amount);
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    public MinguoDate minus(TemporalAmount amount) {
        return super.minus(amount);
    }

    //-----------------------------------------------------------------------
    @Override
    MinguoDate plusYears(long years) {
        return with(isoDate.plusYears(years));
    }

    @Override
    MinguoDate plusMonths(long months) {
        return with(isoDate.plusMonths(months));
    }

    @Override
    MinguoDate plusWeeks(long weeksToAdd) {
        return super.plusWeeks(weeksToAdd);
    }

    @Override
    MinguoDate plusDays(long days) {
        return with(isoDate.plusDays(days));
    }

    @Override
    public MinguoDate plus(long amountToAdd, TemporalUnit unit) {
        return super.plus(amountToAdd, unit);
    }

    @Override
    public MinguoDate minus(long amountToAdd, TemporalUnit unit) {
        return super.minus(amountToAdd, unit);
    }

    @Override
    MinguoDate minusYears(long yearsToSubtract) {
        return super.minusYears(yearsToSubtract);
    }

    @Override
    MinguoDate minusMonths(long monthsToSubtract) {
        return super.minusMonths(monthsToSubtract);
    }

    @Override
    MinguoDate minusWeeks(long weeksToSubtract) {
        return super.minusWeeks(weeksToSubtract);
    }

    @Override
    MinguoDate minusDays(long daysToSubtract) {
        return super.minusDays(daysToSubtract);
    }

    private MinguoDate with(LocalDate newDate) {
        return (newDate.equals(isoDate) ? this : new MinguoDate(newDate));
    }

    @Override        // for javadoc and covariant return type
    @SuppressWarnings("unchecked")
    public final ChronoLocalDateTime<MinguoDate> atTime(LocalTime localTime) {
        return (ChronoLocalDateTime<MinguoDate>)super.atTime(localTime);
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
     * Compares this {@code MinguoDate} with another ensuring that the date is the same.
     * <p>
     * Only objects of type {@code MinguoDate} are compared, other types return false.
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
        return (obj instanceof MinguoDate otherDate)
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
     *  out.writeByte(8);                 // identifies a MinguoDate
     *  out.writeInt(get(YEAR));
     *  out.writeByte(get(MONTH_OF_YEAR));
     *  out.writeByte(get(DAY_OF_MONTH));
     * </pre>
     *
     * @return the instance of {@code Ser}, not null
     */
    @java.io.Serial
    private Object writeReplace() {
        return new Ser(Ser.MINGUO_DATE_TYPE, this);
    }

    void writeExternal(DataOutput out) throws IOException {
        // MinguoChronology is implicit in the MINGUO_DATE_TYPE
        out.writeInt(get(YEAR));
        out.writeByte(get(MONTH_OF_YEAR));
        out.writeByte(get(DAY_OF_MONTH));
    }

    static MinguoDate readExternal(DataInput in) throws IOException {
        int year = in.readInt();
        int month = in.readByte();
        int dayOfMonth = in.readByte();
        return MinguoChronology.INSTANCE.date(year, month, dayOfMonth);
    }

}
