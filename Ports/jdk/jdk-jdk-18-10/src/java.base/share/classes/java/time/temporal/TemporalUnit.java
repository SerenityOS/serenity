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
import java.time.Duration;
import java.time.LocalTime;
import java.time.Period;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;

/**
 * A unit of date-time, such as Days or Hours.
 * <p>
 * Measurement of time is built on units, such as years, months, days, hours, minutes and seconds.
 * Implementations of this interface represent those units.
 * <p>
 * An instance of this interface represents the unit itself, rather than an amount of the unit.
 * See {@link Period} for a class that represents an amount in terms of the common units.
 * <p>
 * The most commonly used units are defined in {@link ChronoUnit}.
 * Further units are supplied in {@link IsoFields}.
 * Units can also be written by application code by implementing this interface.
 * <p>
 * The unit works using double dispatch. Client code calls methods on a date-time like
 * {@code LocalDateTime} which check if the unit is a {@code ChronoUnit}.
 * If it is, then the date-time must handle it.
 * Otherwise, the method call is re-dispatched to the matching method in this interface.
 *
 * @implSpec
 * This interface must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * It is recommended to use an enum where possible.
 *
 * @since 1.8
 */
public interface TemporalUnit {

    /**
     * Gets the duration of this unit, which may be an estimate.
     * <p>
     * All units return a duration measured in standard nanoseconds from this method.
     * The duration will be positive and non-zero.
     * For example, an hour has a duration of {@code 60 * 60 * 1,000,000,000ns}.
     * <p>
     * Some units may return an accurate duration while others return an estimate.
     * For example, days have an estimated duration due to the possibility of
     * daylight saving time changes.
     * To determine if the duration is an estimate, use {@link #isDurationEstimated()}.
     *
     * @return the duration of this unit, which may be an estimate, not null
     */
    Duration getDuration();

    /**
     * Checks if the duration of the unit is an estimate.
     * <p>
     * All units have a duration, however the duration is not always accurate.
     * For example, days have an estimated duration due to the possibility of
     * daylight saving time changes.
     * This method returns true if the duration is an estimate and false if it is
     * accurate. Note that accurate/estimated ignores leap seconds.
     *
     * @return true if the duration is estimated, false if accurate
     */
    boolean isDurationEstimated();

    //-----------------------------------------------------------------------
    /**
     * Checks if this unit represents a component of a date.
     * <p>
     * A date is time-based if it can be used to imply meaning from a date.
     * It must have a {@linkplain #getDuration() duration} that is an integral
     * multiple of the length of a standard day.
     * Note that it is valid for both {@code isDateBased()} and {@code isTimeBased()}
     * to return false, such as when representing a unit like 36 hours.
     *
     * @return true if this unit is a component of a date
     */
    boolean isDateBased();

    /**
     * Checks if this unit represents a component of a time.
     * <p>
     * A unit is time-based if it can be used to imply meaning from a time.
     * It must have a {@linkplain #getDuration() duration} that divides into
     * the length of a standard day without remainder.
     * Note that it is valid for both {@code isDateBased()} and {@code isTimeBased()}
     * to return false, such as when representing a unit like 36 hours.
     *
     * @return true if this unit is a component of a time
     */
    boolean isTimeBased();

    //-----------------------------------------------------------------------
    /**
     * Checks if this unit is supported by the specified temporal object.
     * <p>
     * This checks that the implementing date-time can add/subtract this unit.
     * This can be used to avoid throwing an exception.
     * <p>
     * This default implementation derives the value using
     * {@link Temporal#plus(long, TemporalUnit)}.
     *
     * @param temporal  the temporal object to check, not null
     * @return true if the unit is supported
     */
    default boolean isSupportedBy(Temporal temporal) {
        if (temporal instanceof LocalTime) {
            return isTimeBased();
        }
        if (temporal instanceof ChronoLocalDate) {
            return isDateBased();
        }
        if (temporal instanceof ChronoLocalDateTime || temporal instanceof ChronoZonedDateTime) {
            return true;
        }
        try {
            temporal.plus(1, this);
            return true;
        } catch (UnsupportedTemporalTypeException ex) {
            return false;
        } catch (RuntimeException ex) {
            try {
                temporal.plus(-1, this);
                return true;
            } catch (RuntimeException ex2) {
                return false;
            }
        }
    }

    /**
     * Returns a copy of the specified temporal object with the specified period added.
     * <p>
     * The period added is a multiple of this unit. For example, this method
     * could be used to add "3 days" to a date by calling this method on the
     * instance representing "days", passing the date and the period "3".
     * The period to be added may be negative, which is equivalent to subtraction.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#plus(long, TemporalUnit)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisUnit.addTo(temporal);
     *   temporal = temporal.plus(thisUnit);
     * </pre>
     * It is recommended to use the second approach, {@code plus(TemporalUnit)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should perform any queries or calculations using the units
     * available in {@link ChronoUnit} or the fields available in {@link ChronoField}.
     * If the unit is not supported an {@code UnsupportedTemporalTypeException} must be thrown.
     * <p>
     * Implementations must not alter the specified temporal object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     *
     * @param <R>  the type of the Temporal object
     * @param temporal  the temporal object to adjust, not null
     * @param amount  the amount of this unit to add, positive or negative
     * @return the adjusted temporal object, not null
     * @throws DateTimeException if the amount cannot be added
     * @throws UnsupportedTemporalTypeException if the unit is not supported by the temporal
     */
    <R extends Temporal> R addTo(R temporal, long amount);

    //-----------------------------------------------------------------------
    /**
     * Calculates the amount of time between two temporal objects.
     * <p>
     * This calculates the amount in terms of this unit. The start and end
     * points are supplied as temporal objects and must be of compatible types.
     * The implementation will convert the second type to be an instance of the
     * first type before the calculating the amount.
     * The result will be negative if the end is before the start.
     * For example, the amount in hours between two temporal objects can be
     * calculated using {@code HOURS.between(startTime, endTime)}.
     * <p>
     * The calculation returns a whole number, representing the number of
     * complete units between the two temporals.
     * For example, the amount in hours between the times 11:30 and 13:29
     * will only be one hour as it is one minute short of two hours.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#until(Temporal, TemporalUnit)}:
     * <pre>
     *   // these two lines are equivalent
     *   between = thisUnit.between(start, end);
     *   between = start.until(end, thisUnit);
     * </pre>
     * The choice should be made based on which makes the code more readable.
     * <p>
     * For example, this method allows the number of days between two dates to
     * be calculated:
     * <pre>
     *  long daysBetween = DAYS.between(start, end);
     *  // or alternatively
     *  long daysBetween = start.until(end, DAYS);
     * </pre>
     * <p>
     * Implementations should perform any queries or calculations using the units
     * available in {@link ChronoUnit} or the fields available in {@link ChronoField}.
     * If the unit is not supported an {@code UnsupportedTemporalTypeException} must be thrown.
     * Implementations must not alter the specified temporal objects.
     *
     * @implSpec
     * Implementations must begin by checking to if the two temporals have the
     * same type using {@code getClass()}. If they do not, then the result must be
     * obtained by calling {@code temporal1Inclusive.until(temporal2Exclusive, this)}.
     *
     * @param temporal1Inclusive  the base temporal object, not null
     * @param temporal2Exclusive  the other temporal object, exclusive, not null
     * @return the amount of time between temporal1Inclusive and temporal2Exclusive
     *  in terms of this unit; positive if temporal2Exclusive is later than
     *  temporal1Inclusive, negative if earlier
     * @throws DateTimeException if the amount cannot be calculated, or the end
     *  temporal cannot be converted to the same type as the start temporal
     * @throws UnsupportedTemporalTypeException if the unit is not supported by the temporal
     * @throws ArithmeticException if numeric overflow occurs
     */
    long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive);

    //-----------------------------------------------------------------------
    /**
     * Gets a descriptive name for the unit.
     * <p>
     * This should be in the plural and upper-first camel case, such as 'Days' or 'Minutes'.
     *
     * @return the name of this unit, not null
     */
    @Override
    String toString();

}
