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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
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

import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoUnit.DAYS;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Comparator;
import java.util.Objects;

/**
 * A date without time-of-day or time-zone in an arbitrary chronology, intended
 * for advanced globalization use cases.
 * <p>
 * <b>Most applications should declare method signatures, fields and variables
 * as {@link LocalDate}, not this interface.</b>
 * <p>
 * A {@code ChronoLocalDate} is the abstract representation of a date where the
 * {@code Chronology chronology}, or calendar system, is pluggable.
 * The date is defined in terms of fields expressed by {@link TemporalField},
 * where most common implementations are defined in {@link ChronoField}.
 * The chronology defines how the calendar system operates and the meaning of
 * the standard fields.
 *
 * <h2>When to use this interface</h2>
 * The design of the API encourages the use of {@code LocalDate} rather than this
 * interface, even in the case where the application needs to deal with multiple
 * calendar systems.
 * <p>
 * This concept can seem surprising at first, as the natural way to globalize an
 * application might initially appear to be to abstract the calendar system.
 * However, as explored below, abstracting the calendar system is usually the wrong
 * approach, resulting in logic errors and hard to find bugs.
 * As such, it should be considered an application-wide architectural decision to choose
 * to use this interface as opposed to {@code LocalDate}.
 *
 * <h3>Architectural issues to consider</h3>
 * These are some of the points that must be considered before using this interface
 * throughout an application.
 * <p>
 * 1) Applications using this interface, as opposed to using just {@code LocalDate},
 * face a significantly higher probability of bugs. This is because the calendar system
 * in use is not known at development time. A key cause of bugs is where the developer
 * applies assumptions from their day-to-day knowledge of the ISO calendar system
 * to code that is intended to deal with any arbitrary calendar system.
 * The section below outlines how those assumptions can cause problems
 * The primary mechanism for reducing this increased risk of bugs is a strong code review process.
 * This should also be considered a extra cost in maintenance for the lifetime of the code.
 * <p>
 * 2) This interface does not enforce immutability of implementations.
 * While the implementation notes indicate that all implementations must be immutable
 * there is nothing in the code or type system to enforce this. Any method declared
 * to accept a {@code ChronoLocalDate} could therefore be passed a poorly or
 * maliciously written mutable implementation.
 * <p>
 * 3) Applications using this interface  must consider the impact of eras.
 * {@code LocalDate} shields users from the concept of eras, by ensuring that {@code getYear()}
 * returns the proleptic year. That decision ensures that developers can think of
 * {@code LocalDate} instances as consisting of three fields - year, month-of-year and day-of-month.
 * By contrast, users of this interface must think of dates as consisting of four fields -
 * era, year-of-era, month-of-year and day-of-month. The extra era field is frequently
 * forgotten, yet it is of vital importance to dates in an arbitrary calendar system.
 * For example, in the Japanese calendar system, the era represents the reign of an Emperor.
 * Whenever one reign ends and another starts, the year-of-era is reset to one.
 * <p>
 * 4) The only agreed international standard for passing a date between two systems
 * is the ISO-8601 standard which requires the ISO calendar system. Using this interface
 * throughout the application will inevitably lead to the requirement to pass the date
 * across a network or component boundary, requiring an application specific protocol or format.
 * <p>
 * 5) Long term persistence, such as a database, will almost always only accept dates in the
 * ISO-8601 calendar system (or the related Julian-Gregorian). Passing around dates in other
 * calendar systems increases the complications of interacting with persistence.
 * <p>
 * 6) Most of the time, passing a {@code ChronoLocalDate} throughout an application
 * is unnecessary, as discussed in the last section below.
 *
 * <h3>False assumptions causing bugs in multi-calendar system code</h3>
 * As indicated above, there are many issues to consider when try to use and manipulate a
 * date in an arbitrary calendar system. These are some of the key issues.
 * <p>
 * Code that queries the day-of-month and assumes that the value will never be more than
 * 31 is invalid. Some calendar systems have more than 31 days in some months.
 * <p>
 * Code that adds 12 months to a date and assumes that a year has been added is invalid.
 * Some calendar systems have a different number of months, such as 13 in the Coptic or Ethiopic.
 * <p>
 * Code that adds one month to a date and assumes that the month-of-year value will increase
 * by one or wrap to the next year is invalid. Some calendar systems have a variable number
 * of months in a year, such as the Hebrew.
 * <p>
 * Code that adds one month, then adds a second one month and assumes that the day-of-month
 * will remain close to its original value is invalid. Some calendar systems have a large difference
 * between the length of the longest month and the length of the shortest month.
 * For example, the Coptic or Ethiopic have 12 months of 30 days and 1 month of 5 days.
 * <p>
 * Code that adds seven days and assumes that a week has been added is invalid.
 * Some calendar systems have weeks of other than seven days, such as the French Revolutionary.
 * <p>
 * Code that assumes that because the year of {@code date1} is greater than the year of {@code date2}
 * then {@code date1} is after {@code date2} is invalid. This is invalid for all calendar systems
 * when referring to the year-of-era, and especially untrue of the Japanese calendar system
 * where the year-of-era restarts with the reign of every new Emperor.
 * <p>
 * Code that treats month-of-year one and day-of-month one as the start of the year is invalid.
 * Not all calendar systems start the year when the month value is one.
 * <p>
 * In general, manipulating a date, and even querying a date, is wide open to bugs when the
 * calendar system is unknown at development time. This is why it is essential that code using
 * this interface is subjected to additional code reviews. It is also why an architectural
 * decision to avoid this interface type is usually the correct one.
 *
 * <h3>Using LocalDate instead</h3>
 * The primary alternative to using this interface throughout your application is as follows.
 * <ul>
 * <li>Declare all method signatures referring to dates in terms of {@code LocalDate}.
 * <li>Either store the chronology (calendar system) in the user profile or lookup
 *  the chronology from the user locale
 * <li>Convert the ISO {@code LocalDate} to and from the user's preferred calendar system during
 *  printing and parsing
 * </ul>
 * This approach treats the problem of globalized calendar systems as a localization issue
 * and confines it to the UI layer. This approach is in keeping with other localization
 * issues in the java platform.
 * <p>
 * As discussed above, performing calculations on a date where the rules of the calendar system
 * are pluggable requires skill and is not recommended.
 * Fortunately, the need to perform calculations on a date in an arbitrary calendar system
 * is extremely rare. For example, it is highly unlikely that the business rules of a library
 * book rental scheme will allow rentals to be for one month, where meaning of the month
 * is dependent on the user's preferred calendar system.
 * <p>
 * A key use case for calculations on a date in an arbitrary calendar system is producing
 * a month-by-month calendar for display and user interaction. Again, this is a UI issue,
 * and use of this interface solely within a few methods of the UI layer may be justified.
 * <p>
 * In any other part of the system, where a date must be manipulated in a calendar system
 * other than ISO, the use case will generally specify the calendar system to use.
 * For example, an application may need to calculate the next Islamic or Hebrew holiday
 * which may require manipulating the date.
 * This kind of use case can be handled as follows:
 * <ul>
 * <li>start from the ISO {@code LocalDate} being passed to the method
 * <li>convert the date to the alternate calendar system, which for this use case is known
 *  rather than arbitrary
 * <li>perform the calculation
 * <li>convert back to {@code LocalDate}
 * </ul>
 * Developers writing low-level frameworks or libraries should also avoid this interface.
 * Instead, one of the two general purpose access interfaces should be used.
 * Use {@link TemporalAccessor} if read-only access is required, or use {@link Temporal}
 * if read-write access is required.
 *
 * @implSpec
 * This interface must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * Subclasses should be Serializable wherever possible.
 * <p>
 * Additional calendar systems may be added to the system.
 * See {@link Chronology} for more details.
 *
 * @since 1.8
 */
public interface ChronoLocalDate
        extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDate> {

    /**
     * Gets a comparator that compares {@code ChronoLocalDate} in
     * time-line order ignoring the chronology.
     * <p>
     * This comparator differs from the comparison in {@link #compareTo} in that it
     * only compares the underlying date and not the chronology.
     * This allows dates in different calendar systems to be compared based
     * on the position of the date on the local time-line.
     * The underlying comparison is equivalent to comparing the epoch-day.
     *
     * @return a comparator that compares in time-line order ignoring the chronology
     * @see #isAfter
     * @see #isBefore
     * @see #isEqual
     */
    static Comparator<ChronoLocalDate> timeLineOrder() {
        return (Comparator<ChronoLocalDate> & Serializable) (date1, date2) -> {
            return Long.compare(date1.toEpochDay(), date2.toEpochDay());
        };
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code ChronoLocalDate} from a temporal object.
     * <p>
     * This obtains a local date based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code ChronoLocalDate}.
     * <p>
     * The conversion extracts and combines the chronology and the date
     * from the temporal object. The behavior is equivalent to using
     * {@link Chronology#date(TemporalAccessor)} with the extracted chronology.
     * Implementations are permitted to perform optimizations such as accessing
     * those fields that are equivalent to the relevant objects.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code ChronoLocalDate::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the date, not null
     * @throws DateTimeException if unable to convert to a {@code ChronoLocalDate}
     * @see Chronology#date(TemporalAccessor)
     */
    static ChronoLocalDate from(TemporalAccessor temporal) {
        if (temporal instanceof ChronoLocalDate) {
            return (ChronoLocalDate) temporal;
        }
        Objects.requireNonNull(temporal, "temporal");
        Chronology chrono = temporal.query(TemporalQueries.chronology());
        if (chrono == null) {
            throw new DateTimeException("Unable to obtain ChronoLocalDate from TemporalAccessor: " + temporal.getClass());
        }
        return chrono.date(temporal);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the chronology of this date.
     * <p>
     * The {@code Chronology} represents the calendar system in use.
     * The era and other fields in {@link ChronoField} are defined by the chronology.
     *
     * @return the chronology, not null
     */
    Chronology getChronology();

    /**
     * Gets the era, as defined by the chronology.
     * <p>
     * The era is, conceptually, the largest division of the time-line.
     * Most calendar systems have a single epoch dividing the time-line into two eras.
     * However, some have multiple eras, such as one for the reign of each leader.
     * The exact meaning is determined by the {@code Chronology}.
     * <p>
     * All correctly implemented {@code Era} classes are singletons, thus it
     * is valid code to write {@code date.getEra() == SomeChrono.ERA_NAME)}.
     * <p>
     * This default implementation uses {@link Chronology#eraOf(int)}.
     *
     * @return the chronology specific era constant applicable at this date, not null
     */
    default Era getEra() {
        return getChronology().eraOf(get(ERA));
    }

    /**
     * Checks if the year is a leap year, as defined by the calendar system.
     * <p>
     * A leap-year is a year of a longer length than normal.
     * The exact meaning is determined by the chronology with the constraint that
     * a leap-year must imply a year-length longer than a non leap-year.
     * <p>
     * This default implementation uses {@link Chronology#isLeapYear(long)}.
     *
     * @return true if this date is in a leap year, false otherwise
     */
    default boolean isLeapYear() {
        return getChronology().isLeapYear(getLong(YEAR));
    }

    /**
     * Returns the length of the month represented by this date, as defined by the calendar system.
     * <p>
     * This returns the length of the month in days.
     *
     * @return the length of the month in days
     */
    int lengthOfMonth();

    /**
     * Returns the length of the year represented by this date, as defined by the calendar system.
     * <p>
     * This returns the length of the year in days.
     * <p>
     * The default implementation uses {@link #isLeapYear()} and returns 365 or 366.
     *
     * @return the length of the year in days
     */
    default int lengthOfYear() {
        return (isLeapYear() ? 366 : 365);
    }

    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if the specified field can be queried on this date.
     * If false, then calling the {@link #range(TemporalField) range},
     * {@link #get(TemporalField) get} and {@link #with(TemporalField, long)}
     * methods will throw an exception.
     * <p>
     * The set of supported fields is defined by the chronology and normally includes
     * all {@code ChronoField} date fields.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.isSupportedBy(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the field is supported is determined by the field.
     *
     * @param field  the field to check, null returns false
     * @return true if the field can be queried, false if not
     */
    @Override
    default boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isDateBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    /**
     * Checks if the specified unit is supported.
     * <p>
     * This checks if the specified unit can be added to or subtracted from this date.
     * If false, then calling the {@link #plus(long, TemporalUnit)} and
     * {@link #minus(long, TemporalUnit) minus} methods will throw an exception.
     * <p>
     * The set of supported units is defined by the chronology and normally includes
     * all {@code ChronoUnit} date units except {@code FOREVER}.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.isSupportedBy(Temporal)}
     * passing {@code this} as the argument.
     * Whether the unit is supported is determined by the unit.
     *
     * @param unit  the unit to check, null returns false
     * @return true if the unit can be added/subtracted, false if not
     */
    @Override
    default boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isDateBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    //-----------------------------------------------------------------------
    // override for covariant return type
    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    default ChronoLocalDate with(TemporalAdjuster adjuster) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), Temporal.super.with(adjuster));
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws UnsupportedTemporalTypeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    default ChronoLocalDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return ChronoLocalDateImpl.ensureValid(getChronology(), field.adjustInto(this, newValue));
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    default ChronoLocalDate plus(TemporalAmount amount) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), Temporal.super.plus(amount));
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    default ChronoLocalDate plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return ChronoLocalDateImpl.ensureValid(getChronology(), unit.addTo(this, amountToAdd));
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    default ChronoLocalDate minus(TemporalAmount amount) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), Temporal.super.minus(amount));
    }

    /**
     * {@inheritDoc}
     * @throws DateTimeException {@inheritDoc}
     * @throws UnsupportedTemporalTypeException {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    @Override
    default ChronoLocalDate minus(long amountToSubtract, TemporalUnit unit) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), Temporal.super.minus(amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------
    /**
     * Queries this date using the specified query.
     * <p>
     * This queries this date using the specified query strategy object.
     * The {@code TemporalQuery} object defines the logic to be used to
     * obtain the result. Read the documentation of the query to understand
     * what the result of this method will be.
     * <p>
     * The result of this method is obtained by invoking the
     * {@link TemporalQuery#queryFrom(TemporalAccessor)} method on the
     * specified query passing {@code this} as the argument.
     *
     * @param <R> the type of the result
     * @param query  the query to invoke, not null
     * @return the query result, null may be returned (defined by the query)
     * @throws DateTimeException if unable to query (defined by the query)
     * @throws ArithmeticException if numeric overflow occurs (defined by the query)
     */
    @SuppressWarnings("unchecked")
    @Override
    default <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId() || query == TemporalQueries.zone() || query == TemporalQueries.offset()) {
            return null;
        } else if (query == TemporalQueries.localTime()) {
            return null;
        } else if (query == TemporalQueries.chronology()) {
            return (R) getChronology();
        } else if (query == TemporalQueries.precision()) {
            return (R) DAYS;
        }
        // inline TemporalAccessor.super.query(query) as an optimization
        // non-JDK classes are not permitted to make this optimization
        return query.queryFrom(this);
    }

    /**
     * Adjusts the specified temporal object to have the same date as this object.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with the date changed to be the same as this.
     * <p>
     * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
     * passing {@link ChronoField#EPOCH_DAY} as the field.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#with(TemporalAdjuster)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisLocalDate.adjustInto(temporal);
     *   temporal = temporal.with(thisLocalDate);
     * </pre>
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param temporal  the target object to be adjusted, not null
     * @return the adjusted object, not null
     * @throws DateTimeException if unable to make the adjustment
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(EPOCH_DAY, toEpochDay());
    }

    /**
     * Calculates the amount of time until another date in terms of the specified unit.
     * <p>
     * This calculates the amount of time between two {@code ChronoLocalDate}
     * objects in terms of a single {@code TemporalUnit}.
     * The start and end points are {@code this} and the specified date.
     * The result will be negative if the end is before the start.
     * The {@code Temporal} passed to this method is converted to a
     * {@code ChronoLocalDate} using {@link Chronology#date(TemporalAccessor)}.
     * The calculation returns a whole number, representing the number of
     * complete units between the two dates.
     * For example, the amount in days between two dates can be calculated
     * using {@code startDate.until(endDate, DAYS)}.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method.
     * The second is to use {@link TemporalUnit#between(Temporal, Temporal)}:
     * <pre>
     *   // these two lines are equivalent
     *   amount = start.until(end, MONTHS);
     *   amount = MONTHS.between(start, end);
     * </pre>
     * The choice should be made based on which makes the code more readable.
     * <p>
     * The calculation is implemented in this method for {@link ChronoUnit}.
     * The units {@code DAYS}, {@code WEEKS}, {@code MONTHS}, {@code YEARS},
     * {@code DECADES}, {@code CENTURIES}, {@code MILLENNIA} and {@code ERAS}
     * should be supported by all implementations.
     * Other {@code ChronoUnit} values will throw an exception.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.between(Temporal, Temporal)}
     * passing {@code this} as the first argument and the converted input temporal as
     * the second argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param endExclusive  the end date, exclusive, which is converted to a
     *  {@code ChronoLocalDate} in the same chronology, not null
     * @param unit  the unit to measure the amount in, not null
     * @return the amount of time between this date and the end date
     * @throws DateTimeException if the amount cannot be calculated, or the end
     *  temporal cannot be converted to a {@code ChronoLocalDate}
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override  // override for Javadoc
    long until(Temporal endExclusive, TemporalUnit unit);

    /**
     * Calculates the period between this date and another date as a {@code ChronoPeriod}.
     * <p>
     * This calculates the period between two dates. All supplied chronologies
     * calculate the period using years, months and days, however the
     * {@code ChronoPeriod} API allows the period to be represented using other units.
     * <p>
     * The start and end points are {@code this} and the specified date.
     * The result will be negative if the end is before the start.
     * The negative sign will be the same in each of year, month and day.
     * <p>
     * The calculation is performed using the chronology of this date.
     * If necessary, the input date will be converted to match.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param endDateExclusive  the end date, exclusive, which may be in any chronology, not null
     * @return the period between this date and the end date, not null
     * @throws DateTimeException if the period cannot be calculated
     * @throws ArithmeticException if numeric overflow occurs
     */
    ChronoPeriod until(ChronoLocalDate endDateExclusive);

    /**
     * Formats this date using the specified formatter.
     * <p>
     * This date will be passed to the formatter to produce a string.
     * <p>
     * The default implementation must behave as follows:
     * <pre>
     *  return formatter.format(this);
     * </pre>
     *
     * @param formatter  the formatter to use, not null
     * @return the formatted date string, not null
     * @throws DateTimeException if an error occurs during printing
     */
    default String format(DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Combines this date with a time to create a {@code ChronoLocalDateTime}.
     * <p>
     * This returns a {@code ChronoLocalDateTime} formed from this date at the specified time.
     * All possible combinations of date and time are valid.
     *
     * @param localTime  the local time to use, not null
     * @return the local date-time formed from this date and the specified time, not null
     */
    @SuppressWarnings("unchecked")
    default ChronoLocalDateTime<?> atTime(LocalTime localTime) {
        return ChronoLocalDateTimeImpl.of(this, localTime);
    }

    //-----------------------------------------------------------------------
    /**
     * Converts this date to the Epoch Day.
     * <p>
     * The {@link ChronoField#EPOCH_DAY Epoch Day count} is a simple
     * incrementing count of days where day 0 is 1970-01-01 (ISO).
     * This definition is the same for all chronologies, enabling conversion.
     * <p>
     * This default implementation queries the {@code EPOCH_DAY} field.
     *
     * @return the Epoch Day equivalent to this date
     */
    default long toEpochDay() {
        return getLong(EPOCH_DAY);
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this date to another date, including the chronology.
     * <p>
     * The comparison is based first on the underlying time-line date, then
     * on the chronology.
     * It is "consistent with equals", as defined by {@link Comparable}.
     * <p>
     * For example, the following is the comparator order:
     * <ol>
     * <li>{@code 2012-12-03 (ISO)}</li>
     * <li>{@code 2012-12-04 (ISO)}</li>
     * <li>{@code 2555-12-04 (ThaiBuddhist)}</li>
     * <li>{@code 2012-12-05 (ISO)}</li>
     * </ol>
     * Values #2 and #3 represent the same date on the time-line.
     * When two values represent the same date, the chronology ID is compared to distinguish them.
     * This step is needed to make the ordering "consistent with equals".
     * <p>
     * If all the date objects being compared are in the same chronology, then the
     * additional chronology stage is not required and only the local date is used.
     * To compare the dates of two {@code TemporalAccessor} instances, including dates
     * in two different chronologies, use {@link ChronoField#EPOCH_DAY} as a comparator.
     * <p>
     * This default implementation performs the comparison defined above.
     *
     * @param other  the other date to compare to, not null
     * @return the comparator value, negative if less, positive if greater
     */
    @Override
    default int compareTo(ChronoLocalDate other) {
        int cmp = Long.compare(toEpochDay(), other.toEpochDay());
        if (cmp == 0) {
            cmp = getChronology().compareTo(other.getChronology());
        }
        return cmp;
    }

    /**
     * Checks if this date is after the specified date ignoring the chronology.
     * <p>
     * This method differs from the comparison in {@link #compareTo} in that it
     * only compares the underlying date and not the chronology.
     * This allows dates in different calendar systems to be compared based
     * on the time-line position.
     * This is equivalent to using {@code date1.toEpochDay() > date2.toEpochDay()}.
     * <p>
     * This default implementation performs the comparison based on the epoch-day.
     *
     * @param other  the other date to compare to, not null
     * @return true if this is after the specified date
     */
    default boolean isAfter(ChronoLocalDate other) {
        return this.toEpochDay() > other.toEpochDay();
    }

    /**
     * Checks if this date is before the specified date ignoring the chronology.
     * <p>
     * This method differs from the comparison in {@link #compareTo} in that it
     * only compares the underlying date and not the chronology.
     * This allows dates in different calendar systems to be compared based
     * on the time-line position.
     * This is equivalent to using {@code date1.toEpochDay() < date2.toEpochDay()}.
     * <p>
     * This default implementation performs the comparison based on the epoch-day.
     *
     * @param other  the other date to compare to, not null
     * @return true if this is before the specified date
     */
    default boolean isBefore(ChronoLocalDate other) {
        return this.toEpochDay() < other.toEpochDay();
    }

    /**
     * Checks if this date is equal to the specified date ignoring the chronology.
     * <p>
     * This method differs from the comparison in {@link #compareTo} in that it
     * only compares the underlying date and not the chronology.
     * This allows dates in different calendar systems to be compared based
     * on the time-line position.
     * This is equivalent to using {@code date1.toEpochDay() == date2.toEpochDay()}.
     * <p>
     * This default implementation performs the comparison based on the epoch-day.
     *
     * @param other  the other date to compare to, not null
     * @return true if the underlying date is equal to the specified date
     */
    default boolean isEqual(ChronoLocalDate other) {
        return this.toEpochDay() == other.toEpochDay();
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this date is equal to another date, including the chronology.
     * <p>
     * Compares this date with another ensuring that the date and chronology are the same.
     * <p>
     * To compare the dates of two {@code TemporalAccessor} instances, including dates
     * in two different chronologies, use {@link ChronoField#EPOCH_DAY} as a comparator.
     *
     * @param obj  the object to check, null returns false
     * @return true if this is equal to the other date
     */
    @Override
    boolean equals(Object obj);

    /**
     * A hash code for this date.
     *
     * @return a suitable hash code
     */
    @Override
    int hashCode();

    //-----------------------------------------------------------------------
    /**
     * Outputs this date as a {@code String}.
     * <p>
     * The output will include the full local date.
     *
     * @return the formatted date, not null
     */
    @Override
    String toString();

}
