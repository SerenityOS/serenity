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
package java.time.temporal;

import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;

/**
 * Common implementations of {@code TemporalQuery}.
 * <p>
 * This class provides common implementations of {@link TemporalQuery}.
 * These are defined here as they must be constants, and the definition
 * of lambdas does not guarantee that. By assigning them once here,
 * they become 'normal' Java constants.
 * <p>
 * Queries are a key tool for extracting information from temporal objects.
 * They exist to externalize the process of querying, permitting different
 * approaches, as per the strategy design pattern.
 * Examples might be a query that checks if the date is the day before February 29th
 * in a leap year, or calculates the number of days to your next birthday.
 * <p>
 * The {@link TemporalField} interface provides another mechanism for querying
 * temporal objects. That interface is limited to returning a {@code long}.
 * By contrast, queries can return any type.
 * <p>
 * There are two equivalent ways of using a {@code TemporalQuery}.
 * The first is to invoke the method on this interface directly.
 * The second is to use {@link TemporalAccessor#query(TemporalQuery)}:
 * <pre>
 *   // these two lines are equivalent, but the second approach is recommended
 *   temporal = thisQuery.queryFrom(temporal);
 *   temporal = temporal.query(thisQuery);
 * </pre>
 * It is recommended to use the second approach, {@code query(TemporalQuery)},
 * as it is a lot clearer to read in code.
 * <p>
 * The most common implementations are method references, such as
 * {@code LocalDate::from} and {@code ZoneId::from}.
 * Additional common queries are provided to return:
 * <ul>
 * <li> a Chronology,
 * <li> a LocalDate,
 * <li> a LocalTime,
 * <li> a ZoneOffset,
 * <li> a precision,
 * <li> a zone, or
 * <li> a zoneId.
 * </ul>
 *
 * @since 1.8
 */
public final class TemporalQueries {
    // note that it is vital that each method supplies a constant, not a
    // calculated value, as they will be checked for using ==
    // it is also vital that each constant is different (due to the == checking)
    // as such, alterations to this code must be done with care

    /**
     * Private constructor since this is a utility class.
     */
    private TemporalQueries() {
    }

    //-----------------------------------------------------------------------
    // special constants should be used to extract information from a TemporalAccessor
    // that cannot be derived in other ways
    // Javadoc added here, so as to pretend they are more normal than they really are

    /**
     * A strict query for the {@code ZoneId}.
     * <p>
     * This queries a {@code TemporalAccessor} for the zone.
     * The zone is only returned if the date-time conceptually contains a {@code ZoneId}.
     * It will not be returned if the date-time only conceptually has an {@code ZoneOffset}.
     * Thus a {@link java.time.ZonedDateTime} will return the result of {@code getZone()},
     * but an {@link java.time.OffsetDateTime} will return null.
     * <p>
     * In most cases, applications should use {@link #zone()} as this query is too strict.
     * <p>
     * The result from JDK classes implementing {@code TemporalAccessor} is as follows:<br>
     * {@code LocalDate} returns null<br>
     * {@code LocalTime} returns null<br>
     * {@code LocalDateTime} returns null<br>
     * {@code ZonedDateTime} returns the associated zone<br>
     * {@code OffsetTime} returns null<br>
     * {@code OffsetDateTime} returns null<br>
     * {@code ChronoLocalDate} returns null<br>
     * {@code ChronoLocalDateTime} returns null<br>
     * {@code ChronoZonedDateTime} returns the associated zone<br>
     * {@code Era} returns null<br>
     * {@code DayOfWeek} returns null<br>
     * {@code Month} returns null<br>
     * {@code Year} returns null<br>
     * {@code YearMonth} returns null<br>
     * {@code MonthDay} returns null<br>
     * {@code ZoneOffset} returns null<br>
     * {@code Instant} returns null<br>
     *
     * @return a query that can obtain the zone ID of a temporal, not null
     */
    public static TemporalQuery<ZoneId> zoneId() {
        return TemporalQueries.ZONE_ID;
    }

    /**
     * A query for the {@code Chronology}.
     * <p>
     * This queries a {@code TemporalAccessor} for the chronology.
     * If the target {@code TemporalAccessor} represents a date, or part of a date,
     * then it should return the chronology that the date is expressed in.
     * As a result of this definition, objects only representing time, such as
     * {@code LocalTime}, will return null.
     * <p>
     * The result from JDK classes implementing {@code TemporalAccessor} is as follows:<br>
     * {@code LocalDate} returns {@code IsoChronology.INSTANCE}<br>
     * {@code LocalTime} returns null (does not represent a date)<br>
     * {@code LocalDateTime} returns {@code IsoChronology.INSTANCE}<br>
     * {@code ZonedDateTime} returns {@code IsoChronology.INSTANCE}<br>
     * {@code OffsetTime} returns null (does not represent a date)<br>
     * {@code OffsetDateTime} returns {@code IsoChronology.INSTANCE}<br>
     * {@code ChronoLocalDate} returns the associated chronology<br>
     * {@code ChronoLocalDateTime} returns the associated chronology<br>
     * {@code ChronoZonedDateTime} returns the associated chronology<br>
     * {@code Era} returns the associated chronology<br>
     * {@code DayOfWeek} returns null (shared across chronologies)<br>
     * {@code Month} returns {@code IsoChronology.INSTANCE}<br>
     * {@code Year} returns {@code IsoChronology.INSTANCE}<br>
     * {@code YearMonth} returns {@code IsoChronology.INSTANCE}<br>
     * {@code MonthDay} returns null {@code IsoChronology.INSTANCE}<br>
     * {@code ZoneOffset} returns null (does not represent a date)<br>
     * {@code Instant} returns null (does not represent a date)<br>
     * <p>
     * The method {@link java.time.chrono.Chronology#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code Chronology::from}.
     * That method is equivalent to this query, except that it throws an
     * exception if a chronology cannot be obtained.
     *
     * @return a query that can obtain the chronology of a temporal, not null
     */
    public static TemporalQuery<Chronology> chronology() {
        return TemporalQueries.CHRONO;
    }

    /**
     * A query for the smallest supported unit.
     * <p>
     * This queries a {@code TemporalAccessor} for the time precision.
     * If the target {@code TemporalAccessor} represents a consistent or complete date-time,
     * date or time then this must return the smallest precision actually supported.
     * Note that fields such as {@code NANO_OF_DAY} and {@code NANO_OF_SECOND}
     * are defined to always return ignoring the precision, thus this is the only
     * way to find the actual smallest supported unit.
     * For example, were {@code GregorianCalendar} to implement {@code TemporalAccessor}
     * it would return a precision of {@code MILLIS}.
     * <p>
     * The result from JDK classes implementing {@code TemporalAccessor} is as follows:<br>
     * {@code LocalDate} returns {@code DAYS}<br>
     * {@code LocalTime} returns {@code NANOS}<br>
     * {@code LocalDateTime} returns {@code NANOS}<br>
     * {@code ZonedDateTime} returns {@code NANOS}<br>
     * {@code OffsetTime} returns {@code NANOS}<br>
     * {@code OffsetDateTime} returns {@code NANOS}<br>
     * {@code ChronoLocalDate} returns {@code DAYS}<br>
     * {@code ChronoLocalDateTime} returns {@code NANOS}<br>
     * {@code ChronoZonedDateTime} returns {@code NANOS}<br>
     * {@code Era} returns {@code ERAS}<br>
     * {@code DayOfWeek} returns {@code DAYS}<br>
     * {@code Month} returns {@code MONTHS}<br>
     * {@code Year} returns {@code YEARS}<br>
     * {@code YearMonth} returns {@code MONTHS}<br>
     * {@code MonthDay} returns null (does not represent a complete date or time)<br>
     * {@code ZoneOffset} returns null (does not represent a date or time)<br>
     * {@code Instant} returns {@code NANOS}<br>
     *
     * @return a query that can obtain the precision of a temporal, not null
     */
    public static TemporalQuery<TemporalUnit> precision() {
        return TemporalQueries.PRECISION;
    }

    //-----------------------------------------------------------------------
    // non-special constants are standard queries that derive information from other information
    /**
     * A lenient query for the {@code ZoneId}, falling back to the {@code ZoneOffset}.
     * <p>
     * This queries a {@code TemporalAccessor} for the zone.
     * It first tries to obtain the zone, using {@link #zoneId()}.
     * If that is not found it tries to obtain the {@link #offset()}.
     * Thus a {@link java.time.ZonedDateTime} will return the result of {@code getZone()},
     * while an {@link java.time.OffsetDateTime} will return the result of {@code getOffset()}.
     * <p>
     * In most cases, applications should use this query rather than {@code #zoneId()}.
     * <p>
     * The method {@link ZoneId#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code ZoneId::from}.
     * That method is equivalent to this query, except that it throws an
     * exception if a zone cannot be obtained.
     *
     * @return a query that can obtain the zone ID or offset of a temporal, not null
     */
    public static TemporalQuery<ZoneId> zone() {
        return TemporalQueries.ZONE;
    }

    /**
     * A query for {@code ZoneOffset} returning null if not found.
     * <p>
     * This returns a {@code TemporalQuery} that can be used to query a temporal
     * object for the offset. The query will return null if the temporal
     * object cannot supply an offset.
     * <p>
     * The query implementation examines the {@link ChronoField#OFFSET_SECONDS OFFSET_SECONDS}
     * field and uses it to create a {@code ZoneOffset}.
     * <p>
     * The method {@link java.time.ZoneOffset#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code ZoneOffset::from}.
     * This query and {@code ZoneOffset::from} will return the same result if the
     * temporal object contains an offset. If the temporal object does not contain
     * an offset, then the method reference will throw an exception, whereas this
     * query will return null.
     *
     * @return a query that can obtain the offset of a temporal, not null
     */
    public static TemporalQuery<ZoneOffset> offset() {
        return TemporalQueries.OFFSET;
    }

    /**
     * A query for {@code LocalDate} returning null if not found.
     * <p>
     * This returns a {@code TemporalQuery} that can be used to query a temporal
     * object for the local date. The query will return null if the temporal
     * object cannot supply a local date.
     * <p>
     * The query implementation examines the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
     * field and uses it to create a {@code LocalDate}.
     * <p>
     * The method {@link ZoneOffset#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code LocalDate::from}.
     * This query and {@code LocalDate::from} will return the same result if the
     * temporal object contains a date. If the temporal object does not contain
     * a date, then the method reference will throw an exception, whereas this
     * query will return null.
     *
     * @return a query that can obtain the date of a temporal, not null
     */
    public static TemporalQuery<LocalDate> localDate() {
        return TemporalQueries.LOCAL_DATE;
    }

    /**
     * A query for {@code LocalTime} returning null if not found.
     * <p>
     * This returns a {@code TemporalQuery} that can be used to query a temporal
     * object for the local time. The query will return null if the temporal
     * object cannot supply a local time.
     * <p>
     * The query implementation examines the {@link ChronoField#NANO_OF_DAY NANO_OF_DAY}
     * field and uses it to create a {@code LocalTime}.
     * <p>
     * The method {@link ZoneOffset#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code LocalTime::from}.
     * This query and {@code LocalTime::from} will return the same result if the
     * temporal object contains a time. If the temporal object does not contain
     * a time, then the method reference will throw an exception, whereas this
     * query will return null.
     *
     * @return a query that can obtain the time of a temporal, not null
     */
    public static TemporalQuery<LocalTime> localTime() {
        return TemporalQueries.LOCAL_TIME;
    }

    //-----------------------------------------------------------------------
    /**
     * A strict query for the {@code ZoneId}.
     */
    static final TemporalQuery<ZoneId> ZONE_ID = new TemporalQuery<>() {
        @Override
        public ZoneId queryFrom(TemporalAccessor temporal) {
            return temporal.query(TemporalQueries.ZONE_ID);
        }

        @Override
        public String toString() {
            return "ZoneId";
        }
    };

    /**
     * A query for the {@code Chronology}.
     */
    static final TemporalQuery<Chronology> CHRONO = new TemporalQuery<>() {
        @Override
        public Chronology queryFrom(TemporalAccessor temporal) {
            return temporal.query(TemporalQueries.CHRONO);
        }

        @Override
        public String toString() {
            return "Chronology";
        }
    };


    /**
     * A query for the smallest supported unit.
     */
    static final TemporalQuery<TemporalUnit> PRECISION = new TemporalQuery<>() {
        @Override
        public TemporalUnit queryFrom(TemporalAccessor temporal) {
            return temporal.query(TemporalQueries.PRECISION);
        }

        @Override
        public String toString() {
            return "Precision";
        }
    };

    //-----------------------------------------------------------------------
    /**
     * A query for {@code ZoneOffset} returning null if not found.
     */
    static final TemporalQuery<ZoneOffset> OFFSET = new TemporalQuery<>() {
        @Override
        public ZoneOffset queryFrom(TemporalAccessor temporal) {
            if (temporal.isSupported(OFFSET_SECONDS)) {
                return ZoneOffset.ofTotalSeconds(temporal.get(OFFSET_SECONDS));
            }
            return null;
        }

        @Override
        public String toString() {
            return "ZoneOffset";
        }
    };

    /**
     * A lenient query for the {@code ZoneId}, falling back to the {@code ZoneOffset}.
     */
    static final TemporalQuery<ZoneId> ZONE = new TemporalQuery<>() {
        @Override
        public ZoneId queryFrom(TemporalAccessor temporal) {
            ZoneId zone = temporal.query(ZONE_ID);
            return (zone != null ? zone : temporal.query(OFFSET));
        }

        @Override
        public String toString() {
            return "Zone";
        }
    };

    /**
     * A query for {@code LocalDate} returning null if not found.
     */
    static final TemporalQuery<LocalDate> LOCAL_DATE = new TemporalQuery<>() {
        @Override
        public LocalDate queryFrom(TemporalAccessor temporal) {
            if (temporal.isSupported(EPOCH_DAY)) {
                return LocalDate.ofEpochDay(temporal.getLong(EPOCH_DAY));
            }
            return null;
        }

        @Override
        public String toString() {
            return "LocalDate";
        }
    };

    /**
     * A query for {@code LocalTime} returning null if not found.
     */
    static final TemporalQuery<LocalTime> LOCAL_TIME = new TemporalQuery<>() {
        @Override
        public LocalTime queryFrom(TemporalAccessor temporal) {
            if (temporal.isSupported(NANO_OF_DAY)) {
                return LocalTime.ofNanoOfDay(temporal.getLong(NANO_OF_DAY));
            }
            return null;
        }

        @Override
        public String toString() {
            return "LocalTime";
        }
    };

}
