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
 * Copyright (c) 2007-2012, Stephen Colebourne & Michael Nascimento Santos
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

import static java.time.temporal.ChronoField.EPOCH_DAY;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.util.Objects;

/**
 * A date-time without a time-zone for the calendar neutral API.
 * <p>
 * {@code ChronoLocalDateTime} is an immutable date-time object that represents a date-time, often
 * viewed as year-month-day-hour-minute-second. This object can also access other
 * fields such as day-of-year, day-of-week and week-of-year.
 * <p>
 * This class stores all date and time fields, to a precision of nanoseconds.
 * It does not store or represent a time-zone. For example, the value
 * "2nd October 2007 at 13:45.30.123456789" can be stored in an {@code ChronoLocalDateTime}.
 *
 * @implSpec
 * This class is immutable and thread-safe.
 * @serial
 * @param <D> the concrete type for the date of this date-time
 * @since 1.8
 */
final class ChronoLocalDateTimeImpl<D extends ChronoLocalDate>
        implements  ChronoLocalDateTime<D>, Temporal, TemporalAdjuster, Serializable {

    /**
     * Serialization version.
     */
    @java.io.Serial
    private static final long serialVersionUID = 4556003607393004514L;
    /**
     * Hours per day.
     */
    static final int HOURS_PER_DAY = 24;
    /**
     * Minutes per hour.
     */
    static final int MINUTES_PER_HOUR = 60;
    /**
     * Minutes per day.
     */
    static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;
    /**
     * Seconds per minute.
     */
    static final int SECONDS_PER_MINUTE = 60;
    /**
     * Seconds per hour.
     */
    static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    /**
     * Seconds per day.
     */
    static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
    /**
     * Milliseconds per day.
     */
    static final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L;
    /**
     * Microseconds per day.
     */
    static final long MICROS_PER_DAY = SECONDS_PER_DAY * 1000_000L;
    /**
     * Nanos per second.
     */
    static final long NANOS_PER_SECOND = 1000_000_000L;
    /**
     * Nanos per minute.
     */
    static final long NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE;
    /**
     * Nanos per hour.
     */
    static final long NANOS_PER_HOUR = NANOS_PER_MINUTE * MINUTES_PER_HOUR;
    /**
     * Nanos per day.
     */
    static final long NANOS_PER_DAY = NANOS_PER_HOUR * HOURS_PER_DAY;

    /**
     * The date part.
     */
    private final transient D date;
    /**
     * The time part.
     */
    private final transient LocalTime time;

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code ChronoLocalDateTime} from a date and time.
     *
     * @param date  the local date, not null
     * @param time  the local time, not null
     * @return the local date-time, not null
     */
    static <R extends ChronoLocalDate> ChronoLocalDateTimeImpl<R> of(R date, LocalTime time) {
        return new ChronoLocalDateTimeImpl<>(date, time);
    }

    /**
     * Casts the {@code Temporal} to {@code ChronoLocalDateTime} ensuring it bas the specified chronology.
     *
     * @param chrono  the chronology to check for, not null
     * @param temporal   a date-time to cast, not null
     * @return the date-time checked and cast to {@code ChronoLocalDateTime}, not null
     * @throws ClassCastException if the date-time cannot be cast to ChronoLocalDateTimeImpl
     *  or the chronology is not equal this Chronology
     */
    static <R extends ChronoLocalDate> ChronoLocalDateTimeImpl<R> ensureValid(Chronology chrono, Temporal temporal) {
        @SuppressWarnings("unchecked")
        ChronoLocalDateTimeImpl<R> other = (ChronoLocalDateTimeImpl<R>) temporal;
        if (chrono.equals(other.getChronology()) == false) {
            throw new ClassCastException("Chronology mismatch, required: " + chrono.getId()
                    + ", actual: " + other.getChronology().getId());
        }
        return other;
    }

    /**
     * Constructor.
     *
     * @param date  the date part of the date-time, not null
     * @param time  the time part of the date-time, not null
     */
    private ChronoLocalDateTimeImpl(D date, LocalTime time) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(time, "time");
        this.date = date;
        this.time = time;
    }

    /**
     * Returns a copy of this date-time with the new date and time, checking
     * to see if a new object is in fact required.
     *
     * @param newDate  the date of the new date-time, not null
     * @param newTime  the time of the new date-time, not null
     * @return the date-time, not null
     */
    private ChronoLocalDateTimeImpl<D> with(Temporal newDate, LocalTime newTime) {
        if (date == newDate && time == newTime) {
            return this;
        }
        // Validate that the new Temporal is a ChronoLocalDate (and not something else)
        D cd = ChronoLocalDateImpl.ensureValid(date.getChronology(), newDate);
        return new ChronoLocalDateTimeImpl<>(cd, newTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public D toLocalDate() {
        return date;
    }

    @Override
    public LocalTime toLocalTime() {
        return time;
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField chronoField) {
            return chronoField.isDateBased() || chronoField.isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField chronoField) {
            return (chronoField.isTimeBased() ? time.range(field) : date.range(field));
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TemporalField field) {
        if (field instanceof ChronoField chronoField) {
            return (chronoField.isTimeBased() ? time.get(field) : date.get(field));
        }
        return range(field).checkValidIntValue(getLong(field), field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField chronoField) {
            return (chronoField.isTimeBased() ? time.getLong(field) : date.getLong(field));
        }
        return field.getFrom(this);
    }

    //-----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Override
    public ChronoLocalDateTimeImpl<D> with(TemporalAdjuster adjuster) {
        if (adjuster instanceof ChronoLocalDate) {
            // The Chronology is checked in with(date,time)
            return with((ChronoLocalDate) adjuster, time);
        } else if (adjuster instanceof LocalTime) {
            return with(date, (LocalTime) adjuster);
        } else if (adjuster instanceof ChronoLocalDateTimeImpl) {
            return ChronoLocalDateTimeImpl.ensureValid(date.getChronology(), (ChronoLocalDateTimeImpl<?>) adjuster);
        }
        return ChronoLocalDateTimeImpl.ensureValid(date.getChronology(), (ChronoLocalDateTimeImpl<?>) adjuster.adjustInto(this));
    }

    @Override
    public ChronoLocalDateTimeImpl<D> with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chronoField) {
            if (chronoField.isTimeBased()) {
                return with(date, time.with(field, newValue));
            } else {
                return with(date.with(field, newValue), time);
            }
        }
        return ChronoLocalDateTimeImpl.ensureValid(date.getChronology(), field.adjustInto(this, newValue));
    }

    //-----------------------------------------------------------------------
    @Override
    public ChronoLocalDateTimeImpl<D> plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit chronoUnit) {
            return switch (chronoUnit) {
                case NANOS     -> plusNanos(amountToAdd);
                case MICROS    -> plusDays(amountToAdd / MICROS_PER_DAY).plusNanos((amountToAdd % MICROS_PER_DAY) * 1000);
                case MILLIS    -> plusDays(amountToAdd / MILLIS_PER_DAY).plusNanos((amountToAdd % MILLIS_PER_DAY) * 1000000);
                case SECONDS   -> plusSeconds(amountToAdd);
                case MINUTES   -> plusMinutes(amountToAdd);
                case HOURS     -> plusHours(amountToAdd);
                case HALF_DAYS -> plusDays(amountToAdd / 256).plusHours((amountToAdd % 256) * 12); // no overflow (256 is multiple of 2)
                default -> with(date.plus(amountToAdd, unit), time);
            };
        }
        return ChronoLocalDateTimeImpl.ensureValid(date.getChronology(), unit.addTo(this, amountToAdd));
    }

    private ChronoLocalDateTimeImpl<D> plusDays(long days) {
        return with(date.plus(days, ChronoUnit.DAYS), time);
    }

    private ChronoLocalDateTimeImpl<D> plusHours(long hours) {
        return plusWithOverflow(date, hours, 0, 0, 0);
    }

    private ChronoLocalDateTimeImpl<D> plusMinutes(long minutes) {
        return plusWithOverflow(date, 0, minutes, 0, 0);
    }

    ChronoLocalDateTimeImpl<D> plusSeconds(long seconds) {
        return plusWithOverflow(date, 0, 0, seconds, 0);
    }

    private ChronoLocalDateTimeImpl<D> plusNanos(long nanos) {
        return plusWithOverflow(date, 0, 0, 0, nanos);
    }

    //-----------------------------------------------------------------------
    private ChronoLocalDateTimeImpl<D> plusWithOverflow(D newDate, long hours, long minutes, long seconds, long nanos) {
        // 9223372036854775808 long, 2147483648 int
        if ((hours | minutes | seconds | nanos) == 0) {
            return with(newDate, time);
        }
        long totDays = nanos / NANOS_PER_DAY +             //   max/24*60*60*1B
                seconds / SECONDS_PER_DAY +                //   max/24*60*60
                minutes / MINUTES_PER_DAY +                //   max/24*60
                hours / HOURS_PER_DAY;                     //   max/24
        long totNanos = nanos % NANOS_PER_DAY +                    //   max  86400000000000
                (seconds % SECONDS_PER_DAY) * NANOS_PER_SECOND +   //   max  86400000000000
                (minutes % MINUTES_PER_DAY) * NANOS_PER_MINUTE +   //   max  86400000000000
                (hours % HOURS_PER_DAY) * NANOS_PER_HOUR;          //   max  86400000000000
        long curNoD = time.toNanoOfDay();                          //   max  86400000000000
        totNanos = totNanos + curNoD;                              // total 432000000000000
        totDays += Math.floorDiv(totNanos, NANOS_PER_DAY);
        long newNoD = Math.floorMod(totNanos, NANOS_PER_DAY);
        LocalTime newTime = (newNoD == curNoD ? time : LocalTime.ofNanoOfDay(newNoD));
        return with(newDate.plus(totDays, ChronoUnit.DAYS), newTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public ChronoZonedDateTime<D> atZone(ZoneId zone) {
        return ChronoZonedDateTimeImpl.ofBest(this, zone, null);
    }

    //-----------------------------------------------------------------------
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        Objects.requireNonNull(endExclusive, "endExclusive");
        @SuppressWarnings("unchecked")
        ChronoLocalDateTime<D> end = (ChronoLocalDateTime<D>) getChronology().localDateTime(endExclusive);
        if (unit instanceof ChronoUnit chronoUnit) {
            if (unit.isTimeBased()) {
                long amount = end.getLong(EPOCH_DAY) - date.getLong(EPOCH_DAY);
                switch (chronoUnit) {
                    case NANOS: amount = Math.multiplyExact(amount, NANOS_PER_DAY); break;
                    case MICROS: amount = Math.multiplyExact(amount, MICROS_PER_DAY); break;
                    case MILLIS: amount = Math.multiplyExact(amount, MILLIS_PER_DAY); break;
                    case SECONDS: amount = Math.multiplyExact(amount, SECONDS_PER_DAY); break;
                    case MINUTES: amount = Math.multiplyExact(amount, MINUTES_PER_DAY); break;
                    case HOURS: amount = Math.multiplyExact(amount, HOURS_PER_DAY); break;
                    case HALF_DAYS: amount = Math.multiplyExact(amount, 2); break;
                }
                return Math.addExact(amount, time.until(end.toLocalTime(), unit));
            }
            ChronoLocalDate endDate = end.toLocalDate();
            if (end.toLocalTime().isBefore(time)) {
                endDate = endDate.minus(1, ChronoUnit.DAYS);
            }
            return date.until(endDate, unit);
        }
        Objects.requireNonNull(unit, "unit");
        return unit.between(this, end);
    }

    //-----------------------------------------------------------------------
    /**
     * Writes the ChronoLocalDateTime using a
     * <a href="{@docRoot}/serialized-form.html#java.time.chrono.Ser">dedicated serialized form</a>.
     * @serialData
     * <pre>
     *  out.writeByte(2);              // identifies a ChronoLocalDateTime
     *  out.writeObject(toLocalDate());
     *  out.writeObject(toLocalTime());
     * </pre>
     *
     * @return the instance of {@code Ser}, not null
     */
   @java.io.Serial
    private Object writeReplace() {
        return new Ser(Ser.CHRONO_LOCAL_DATE_TIME_TYPE, this);
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

    void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(date);
        out.writeObject(time);
    }

    static ChronoLocalDateTime<?> readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ChronoLocalDate date = (ChronoLocalDate) in.readObject();
        LocalTime time = (LocalTime) in.readObject();
        return date.atTime(time);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChronoLocalDateTime) {
            return compareTo((ChronoLocalDateTime<?>) obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toLocalDate().hashCode() ^ toLocalTime().hashCode();
    }

    @Override
    public String toString() {
        return toLocalDate().toString() + 'T' + toLocalTime().toString();
    }

}
