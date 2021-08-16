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
 * Copyright (c) 2012, 2013 Stephen Colebourne & Michael Nascimento Santos
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
import java.time.Period;
import java.util.List;

/**
 * Framework-level interface defining an amount of time, such as
 * "6 hours", "8 days" or "2 years and 3 months".
 * <p>
 * This is the base interface type for amounts of time.
 * An amount is distinct from a date or time-of-day in that it is not tied
 * to any specific point on the time-line.
 * <p>
 * The amount can be thought of as a {@code Map} of {@link TemporalUnit} to
 * {@code long}, exposed via {@link #getUnits()} and {@link #get(TemporalUnit)}.
 * A simple case might have a single unit-value pair, such as "6 hours".
 * A more complex case may have multiple unit-value pairs, such as
 * "7 years, 3 months and 5 days".
 * <p>
 * There are two common implementations.
 * {@link Period} is a date-based implementation, storing years, months and days.
 * {@link Duration} is a time-based implementation, storing seconds and nanoseconds,
 * but providing some access using other duration based units such as minutes,
 * hours and fixed 24-hour days.
 * <p>
 * This interface is a framework-level interface that should not be widely
 * used in application code. Instead, applications should create and pass
 * around instances of concrete types, such as {@code Period} and {@code Duration}.
 *
 * @implSpec
 * This interface places no restrictions on the mutability of implementations,
 * however immutability is strongly recommended.
 *
 * @since 1.8
 */
public interface TemporalAmount {

    /**
     * Returns the value of the requested unit.
     * The units returned from {@link #getUnits()} uniquely define the
     * value of the {@code TemporalAmount}.  A value must be returned
     * for each unit listed in {@code getUnits}.
     *
     * @implSpec
     * Implementations may declare support for units not listed by {@link #getUnits()}.
     * Typically, the implementation would define additional units
     * as conversions for the convenience of developers.
     *
     * @param unit the {@code TemporalUnit} for which to return the value
     * @return the long value of the unit
     * @throws DateTimeException if a value for the unit cannot be obtained
     * @throws UnsupportedTemporalTypeException if the {@code unit} is not supported
     */
    long get(TemporalUnit unit);

    /**
     * Returns the list of units uniquely defining the value of this TemporalAmount.
     * The list of {@code TemporalUnits} is defined by the implementation class.
     * The list is a snapshot of the units at the time {@code getUnits}
     * is called and is not mutable.
     * The units are ordered from longest duration to the shortest duration
     * of the unit.
     *
     * @implSpec
     * The list of units completely and uniquely represents the
     * state of the object without omissions, overlaps or duplication.
     * The units are in order from longest duration to shortest.
     *
     * @return the List of {@code TemporalUnits}; not null
     */
    List<TemporalUnit> getUnits();

    /**
     * Adds to the specified temporal object.
     * <p>
     * Adds the amount to the specified temporal object using the logic
     * encapsulated in the implementing class.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#plus(TemporalAmount)}:
     * <pre>
     *   // These two lines are equivalent, but the second approach is recommended
     *   dateTime = amount.addTo(dateTime);
     *   dateTime = dateTime.plus(adder);
     * </pre>
     * It is recommended to use the second approach, {@code plus(TemporalAmount)},
     * as it is a lot clearer to read in code.
     *
     * @implSpec
     * The implementation must take the input object and add to it.
     * The implementation defines the logic of the addition and is responsible for
     * documenting that logic. It may use any method on {@code Temporal} to
     * query the temporal object and perform the addition.
     * The returned object must have the same observable type as the input object
     * <p>
     * The input object must not be altered.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable temporal objects.
     * <p>
     * The input temporal object may be in a calendar system other than ISO.
     * Implementations may choose to document compatibility with other calendar systems,
     * or reject non-ISO temporal objects by {@link TemporalQueries#chronology() querying the chronology}.
     * <p>
     * This method may be called from multiple threads in parallel.
     * It must be thread-safe when invoked.
     *
     * @param temporal  the temporal object to add the amount to, not null
     * @return an object of the same observable type with the addition made, not null
     * @throws DateTimeException if unable to add
     * @throws ArithmeticException if numeric overflow occurs
     */
    Temporal addTo(Temporal temporal);

    /**
     * Subtracts this object from the specified temporal object.
     * <p>
     * Subtracts the amount from the specified temporal object using the logic
     * encapsulated in the implementing class.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#minus(TemporalAmount)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   dateTime = amount.subtractFrom(dateTime);
     *   dateTime = dateTime.minus(amount);
     * </pre>
     * It is recommended to use the second approach, {@code minus(TemporalAmount)},
     * as it is a lot clearer to read in code.
     *
     * @implSpec
     * The implementation must take the input object and subtract from it.
     * The implementation defines the logic of the subtraction and is responsible for
     * documenting that logic. It may use any method on {@code Temporal} to
     * query the temporal object and perform the subtraction.
     * The returned object must have the same observable type as the input object
     * <p>
     * The input object must not be altered.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable temporal objects.
     * <p>
     * The input temporal object may be in a calendar system other than ISO.
     * Implementations may choose to document compatibility with other calendar systems,
     * or reject non-ISO temporal objects by {@link TemporalQueries#chronology() querying the chronology}.
     * <p>
     * This method may be called from multiple threads in parallel.
     * It must be thread-safe when invoked.
     *
     * @param temporal  the temporal object to subtract the amount from, not null
     * @return an object of the same observable type with the subtraction made, not null
     * @throws DateTimeException if unable to subtract
     * @throws ArithmeticException if numeric overflow occurs
     */
    Temporal subtractFrom(Temporal temporal);
}
