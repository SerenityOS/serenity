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
package java.time.temporal;

import java.time.DateTimeException;

/**
 * Framework-level interface defining read-write access to a temporal object,
 * such as a date, time, offset or some combination of these.
 * <p>
 * This is the base interface type for date, time and offset objects that
 * are complete enough to be manipulated using plus and minus.
 * It is implemented by those classes that can provide and manipulate information
 * as {@linkplain TemporalField fields} or {@linkplain TemporalQuery queries}.
 * See {@link TemporalAccessor} for the read-only version of this interface.
 * <p>
 * Most date and time information can be represented as a number.
 * These are modeled using {@code TemporalField} with the number held using
 * a {@code long} to handle large values. Year, month and day-of-month are
 * simple examples of fields, but they also include instant and offsets.
 * See {@link ChronoField} for the standard set of fields.
 * <p>
 * Two pieces of date/time information cannot be represented by numbers,
 * the {@linkplain java.time.chrono.Chronology chronology} and the
 * {@linkplain java.time.ZoneId time-zone}.
 * These can be accessed via {@link #query(TemporalQuery) queries} using
 * the static methods defined on {@link TemporalQuery}.
 * <p>
 * This interface is a framework-level interface that should not be widely
 * used in application code. Instead, applications should create and pass
 * around instances of concrete types, such as {@code LocalDate}.
 * There are many reasons for this, part of which is that implementations
 * of this interface may be in calendar systems other than ISO.
 * See {@link java.time.chrono.ChronoLocalDate} for a fuller discussion of the issues.
 *
 * <h2>When to implement</h2>
 * <p>
 * A class should implement this interface if it meets three criteria:
 * <ul>
 * <li>it provides access to date/time/offset information, as per {@code TemporalAccessor}
 * <li>the set of fields are contiguous from the largest to the smallest
 * <li>the set of fields are complete, such that no other field is needed to define the
 *  valid range of values for the fields that are represented
 * </ul>
 * <p>
 * Four examples make this clear:
 * <ul>
 * <li>{@code LocalDate} implements this interface as it represents a set of fields
 *  that are contiguous from days to forever and require no external information to determine
 *  the validity of each date. It is therefore able to implement plus/minus correctly.
 * <li>{@code LocalTime} implements this interface as it represents a set of fields
 *  that are contiguous from nanos to within days and require no external information to determine
 *  validity. It is able to implement plus/minus correctly, by wrapping around the day.
 * <li>{@code MonthDay}, the combination of month-of-year and day-of-month, does not implement
 *  this interface.  While the combination is contiguous, from days to months within years,
 *  the combination does not have sufficient information to define the valid range of values
 *  for day-of-month.  As such, it is unable to implement plus/minus correctly.
 * <li>The combination day-of-week and day-of-month ("Friday the 13th") should not implement
 *  this interface. It does not represent a contiguous set of fields, as days to weeks overlaps
 *  days to months.
 * </ul>
 *
 * @implSpec
 * This interface places no restrictions on the mutability of implementations,
 * however immutability is strongly recommended.
 * All implementations must be {@link Comparable}.
 *
 * @since 1.8
 */
public interface Temporal extends TemporalAccessor {

    /**
     * Checks if the specified unit is supported.
     * <p>
     * This checks if the specified unit can be added to, or subtracted from, this date-time.
     * If false, then calling the {@link #plus(long, TemporalUnit)} and
     * {@link #minus(long, TemporalUnit) minus} methods will throw an exception.
     *
     * @implSpec
     * Implementations must check and handle all units defined in {@link ChronoUnit}.
     * If the unit is supported, then true must be returned, otherwise false must be returned.
     * <p>
     * If the field is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.isSupportedBy(Temporal)}
     * passing {@code this} as the argument.
     * <p>
     * Implementations must ensure that no observable state is altered when this
     * read-only method is invoked.
     *
     * @param unit  the unit to check, null returns false
     * @return true if the unit can be added/subtracted, false if not
     */
    boolean isSupported(TemporalUnit unit);

    /**
     * Returns an adjusted object of the same type as this object with the adjustment made.
     * <p>
     * This adjusts this date-time according to the rules of the specified adjuster.
     * A simple adjuster might simply set the one of the fields, such as the year field.
     * A more complex adjuster might set the date to the last day of the month.
     * A selection of common adjustments is provided in
     * {@link java.time.temporal.TemporalAdjusters TemporalAdjusters}.
     * These include finding the "last day of the month" and "next Wednesday".
     * The adjuster is responsible for handling special cases, such as the varying
     * lengths of month and leap years.
     * <p>
     * Some example code indicating how and why this method is used:
     * <pre>
     *  date = date.with(Month.JULY);        // most key classes implement TemporalAdjuster
     *  date = date.with(lastDayOfMonth());  // static import from Adjusters
     *  date = date.with(next(WEDNESDAY));   // static import from Adjusters and DayOfWeek
     * </pre>
     *
     * @implSpec
     * <p>
     * Implementations must not alter either this object or the specified temporal object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     * <p>
     * The default implementation must behave equivalent to this code:
     * <pre>
     *  return adjuster.adjustInto(this);
     * </pre>
     *
     * @param adjuster  the adjuster to use, not null
     * @return an object of the same type with the specified adjustment made, not null
     * @throws DateTimeException if unable to make the adjustment
     * @throws ArithmeticException if numeric overflow occurs
     */
    default Temporal with(TemporalAdjuster adjuster) {
        return adjuster.adjustInto(this);
    }

    /**
     * Returns an object of the same type as this object with the specified field altered.
     * <p>
     * This returns a new object based on this one with the value for the specified field changed.
     * For example, on a {@code LocalDate}, this could be used to set the year, month or day-of-month.
     * The returned object will have the same observable type as this object.
     * <p>
     * In some cases, changing a field is not fully defined. For example, if the target object is
     * a date representing the 31st January, then changing the month to February would be unclear.
     * In cases like this, the field is responsible for resolving the result. Typically it will choose
     * the previous valid date, which would be the last valid day of February in this example.
     *
     * @implSpec
     * Implementations must check and handle all fields defined in {@link ChronoField}.
     * If the field is supported, then the adjustment must be performed.
     * If unsupported, then an {@code UnsupportedTemporalTypeException} must be thrown.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.adjustInto(Temporal, long)}
     * passing {@code this} as the first argument.
     * <p>
     * Implementations must not alter this object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     *
     * @param field  the field to set in the result, not null
     * @param newValue  the new value of the field in the result
     * @return an object of the same type with the specified field set, not null
     * @throws DateTimeException if the field cannot be set
     * @throws UnsupportedTemporalTypeException if the field is not supported
     * @throws ArithmeticException if numeric overflow occurs
     */
    Temporal with(TemporalField field, long newValue);

    //-----------------------------------------------------------------------
    /**
     * Returns an object of the same type as this object with an amount added.
     * <p>
     * This adjusts this temporal, adding according to the rules of the specified amount.
     * The amount is typically a {@link java.time.Period} but may be any other type implementing
     * the {@link TemporalAmount} interface, such as {@link java.time.Duration}.
     * <p>
     * Some example code indicating how and why this method is used:
     * <pre>
     *  date = date.plus(period);                // add a Period instance
     *  date = date.plus(duration);              // add a Duration instance
     *  date = date.plus(workingDays(6));        // example user-written workingDays method
     * </pre>
     * <p>
     * Note that calling {@code plus} followed by {@code minus} is not guaranteed to
     * return the same date-time.
     *
     * @implSpec
     * <p>
     * Implementations must not alter either this object or the specified temporal object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     * <p>
     * The default implementation must behave equivalent to this code:
     * <pre>
     *  return amount.addTo(this);
     * </pre>
     *
     * @param amount  the amount to add, not null
     * @return an object of the same type with the specified adjustment made, not null
     * @throws DateTimeException if the addition cannot be made
     * @throws ArithmeticException if numeric overflow occurs
     */
    default Temporal plus(TemporalAmount amount) {
        return amount.addTo(this);
    }

    /**
     * Returns an object of the same type as this object with the specified period added.
     * <p>
     * This method returns a new object based on this one with the specified period added.
     * For example, on a {@code LocalDate}, this could be used to add a number of years, months or days.
     * The returned object will have the same observable type as this object.
     * <p>
     * In some cases, changing a field is not fully defined. For example, if the target object is
     * a date representing the 31st January, then adding one month would be unclear.
     * In cases like this, the field is responsible for resolving the result. Typically it will choose
     * the previous valid date, which would be the last valid day of February in this example.
     *
     * @implSpec
     * Implementations must check and handle all units defined in {@link ChronoUnit}.
     * If the unit is supported, then the addition must be performed.
     * If unsupported, then an {@code UnsupportedTemporalTypeException} must be thrown.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.addTo(Temporal, long)}
     * passing {@code this} as the first argument.
     * <p>
     * Implementations must not alter this object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     *
     * @param amountToAdd  the amount of the specified unit to add, may be negative
     * @param unit  the unit of the amount to add, not null
     * @return an object of the same type with the specified period added, not null
     * @throws DateTimeException if the unit cannot be added
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException if numeric overflow occurs
     */
    Temporal plus(long amountToAdd, TemporalUnit unit);

    //-----------------------------------------------------------------------
    /**
     * Returns an object of the same type as this object with an amount subtracted.
     * <p>
     * This adjusts this temporal, subtracting according to the rules of the specified amount.
     * The amount is typically a {@link java.time.Period} but may be any other type implementing
     * the {@link TemporalAmount} interface, such as {@link java.time.Duration}.
     * <p>
     * Some example code indicating how and why this method is used:
     * <pre>
     *  date = date.minus(period);               // subtract a Period instance
     *  date = date.minus(duration);             // subtract a Duration instance
     *  date = date.minus(workingDays(6));       // example user-written workingDays method
     * </pre>
     * <p>
     * Note that calling {@code plus} followed by {@code minus} is not guaranteed to
     * return the same date-time.
     *
     * @implSpec
     * <p>
     * Implementations must not alter either this object or the specified temporal object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     * <p>
     * The default implementation must behave equivalent to this code:
     * <pre>
     *  return amount.subtractFrom(this);
     * </pre>
     *
     * @param amount  the amount to subtract, not null
     * @return an object of the same type with the specified adjustment made, not null
     * @throws DateTimeException if the subtraction cannot be made
     * @throws ArithmeticException if numeric overflow occurs
     */
    default Temporal minus(TemporalAmount amount) {
        return amount.subtractFrom(this);
    }

    /**
     * Returns an object of the same type as this object with the specified period subtracted.
     * <p>
     * This method returns a new object based on this one with the specified period subtracted.
     * For example, on a {@code LocalDate}, this could be used to subtract a number of years, months or days.
     * The returned object will have the same observable type as this object.
     * <p>
     * In some cases, changing a field is not fully defined. For example, if the target object is
     * a date representing the 31st March, then subtracting one month would be unclear.
     * In cases like this, the field is responsible for resolving the result. Typically it will choose
     * the previous valid date, which would be the last valid day of February in this example.
     *
     * @implSpec
     * Implementations must behave in a manor equivalent to the default method behavior.
     * <p>
     * Implementations must not alter this object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     * <p>
     * The default implementation must behave equivalent to this code:
     * <pre>
     *  return (amountToSubtract == Long.MIN_VALUE ?
     *      plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit));
     * </pre>
     *
     * @param amountToSubtract  the amount of the specified unit to subtract, may be negative
     * @param unit  the unit of the amount to subtract, not null
     * @return an object of the same type with the specified period subtracted, not null
     * @throws DateTimeException if the unit cannot be subtracted
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException if numeric overflow occurs
     */
    default Temporal minus(long amountToSubtract, TemporalUnit unit) {
        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------
    /**
     * Calculates the amount of time until another temporal in terms of the specified unit.
     * <p>
     * This calculates the amount of time between two temporal objects
     * in terms of a single {@code TemporalUnit}.
     * The start and end points are {@code this} and the specified temporal.
     * The end point is converted to be of the same type as the start point if different.
     * The result will be negative if the end is before the start.
     * For example, the amount in hours between two temporal objects can be
     * calculated using {@code startTime.until(endTime, HOURS)}.
     * <p>
     * The calculation returns a whole number, representing the number of
     * complete units between the two temporals.
     * For example, the amount in hours between the times 11:30 and 13:29
     * will only be one hour as it is one minute short of two hours.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link TemporalUnit#between(Temporal, Temporal)}:
     * <pre>
     *   // these two lines are equivalent
     *   temporal = start.until(end, unit);
     *   temporal = unit.between(start, end);
     * </pre>
     * The choice should be made based on which makes the code more readable.
     * <p>
     * For example, this method allows the number of days between two dates to
     * be calculated:
     * <pre>
     *  long daysBetween = start.until(end, DAYS);
     *  // or alternatively
     *  long daysBetween = DAYS.between(start, end);
     * </pre>
     *
     * @implSpec
     * Implementations must begin by checking to ensure that the input temporal
     * object is of the same observable type as the implementation.
     * They must then perform the calculation for all instances of {@link ChronoUnit}.
     * An {@code UnsupportedTemporalTypeException} must be thrown for {@code ChronoUnit}
     * instances that are unsupported.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.between(Temporal, Temporal)}
     * passing {@code this} as the first argument and the converted input temporal as
     * the second argument.
     * <p>
     * In summary, implementations must behave in a manner equivalent to this pseudo-code:
     * <pre>
     *  // convert the end temporal to the same type as this class
     *  if (unit instanceof ChronoUnit) {
     *    // if unit is supported, then calculate and return result
     *    // else throw UnsupportedTemporalTypeException for unsupported units
     *  }
     *  return unit.between(this, convertedEndTemporal);
     * </pre>
     * <p>
     * Note that the unit's {@code between} method must only be invoked if the
     * two temporal objects have exactly the same type evaluated by {@code getClass()}.
     * <p>
     * Implementations must ensure that no observable state is altered when this
     * read-only method is invoked.
     *
     * @param endExclusive  the end temporal, exclusive, converted to be of the
     *  same type as this object, not null
     * @param unit  the unit to measure the amount in, not null
     * @return the amount of time between this temporal object and the specified one
     *  in terms of the unit; positive if the specified object is later than this one,
     *  negative if it is earlier than this one
     * @throws DateTimeException if the amount cannot be calculated, or the end
     *  temporal cannot be converted to the same type as this temporal
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException if numeric overflow occurs
     */
    long until(Temporal endExclusive, TemporalUnit unit);

}
