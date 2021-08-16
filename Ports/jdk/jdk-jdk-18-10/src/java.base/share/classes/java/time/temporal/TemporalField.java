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
import java.time.chrono.Chronology;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A field of date-time, such as month-of-year or minute-of-hour.
 * <p>
 * Date and time is expressed using fields which partition the time-line into something
 * meaningful for humans. Implementations of this interface represent those fields.
 * <p>
 * The most commonly used units are defined in {@link ChronoField}.
 * Further fields are supplied in {@link IsoFields}, {@link WeekFields} and {@link JulianFields}.
 * Fields can also be written by application code by implementing this interface.
 * <p>
 * The field works using double dispatch. Client code calls methods on a date-time like
 * {@code LocalDateTime} which check if the field is a {@code ChronoField}.
 * If it is, then the date-time must handle it.
 * Otherwise, the method call is re-dispatched to the matching method in this interface.
 *
 * @implSpec
 * This interface must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * Implementations should be {@code Serializable} where possible.
 * An enum is as effective implementation choice.
 *
 * @since 1.8
 */
public interface TemporalField {

    /**
     * Gets the display name for the field in the requested locale.
     * <p>
     * If there is no display name for the locale then a suitable default must be returned.
     * <p>
     * The default implementation must check the locale is not null
     * and return {@code toString()}.
     *
     * @param locale  the locale to use, not null
     * @return the display name for the locale or a suitable default, not null
     */
    default String getDisplayName(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        return toString();
    }

    /**
     * Gets the unit that the field is measured in.
     * <p>
     * The unit of the field is the period that varies within the range.
     * For example, in the field 'MonthOfYear', the unit is 'Months'.
     * See also {@link #getRangeUnit()}.
     *
     * @return the unit defining the base unit of the field, not null
     */
    TemporalUnit getBaseUnit();

    /**
     * Gets the range that the field is bound by.
     * <p>
     * The range of the field is the period that the field varies within.
     * For example, in the field 'MonthOfYear', the range is 'Years'.
     * See also {@link #getBaseUnit()}.
     * <p>
     * The range is never null. For example, the 'Year' field is shorthand for
     * 'YearOfForever'. It therefore has a unit of 'Years' and a range of 'Forever'.
     *
     * @return the unit defining the range of the field, not null
     */
    TemporalUnit getRangeUnit();

    /**
     * Gets the range of valid values for the field.
     * <p>
     * All fields can be expressed as a {@code long} integer.
     * This method returns an object that describes the valid range for that value.
     * This method is generally only applicable to the ISO-8601 calendar system.
     * <p>
     * Note that the result only describes the minimum and maximum valid values
     * and it is important not to read too much into them. For example, there
     * could be values within the range that are invalid for the field.
     *
     * @return the range of valid values for the field, not null
     */
    ValueRange range();

    //-----------------------------------------------------------------------
    /**
     * Checks if this field represents a component of a date.
     * <p>
     * A field is date-based if it can be derived from
     * {@link ChronoField#EPOCH_DAY EPOCH_DAY}.
     * Note that it is valid for both {@code isDateBased()} and {@code isTimeBased()}
     * to return false, such as when representing a field like minute-of-week.
     *
     * @return true if this field is a component of a date
     */
    boolean isDateBased();

    /**
     * Checks if this field represents a component of a time.
     * <p>
     * A field is time-based if it can be derived from
     * {@link ChronoField#NANO_OF_DAY NANO_OF_DAY}.
     * Note that it is valid for both {@code isDateBased()} and {@code isTimeBased()}
     * to return false, such as when representing a field like minute-of-week.
     *
     * @return true if this field is a component of a time
     */
    boolean isTimeBased();

    //-----------------------------------------------------------------------
    /**
     * Checks if this field is supported by the temporal object.
     * <p>
     * This determines whether the temporal accessor supports this field.
     * If this returns false, then the temporal cannot be queried for this field.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link TemporalAccessor#isSupported(TemporalField)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisField.isSupportedBy(temporal);
     *   temporal = temporal.isSupported(thisField);
     * </pre>
     * It is recommended to use the second approach, {@code isSupported(TemporalField)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should determine whether they are supported using the fields
     * available in {@link ChronoField}.
     *
     * @param temporal  the temporal object to query, not null
     * @return true if the date-time can be queried for this field, false if not
     */
    boolean isSupportedBy(TemporalAccessor temporal);

    /**
     * Get the range of valid values for this field using the temporal object to
     * refine the result.
     * <p>
     * This uses the temporal object to find the range of valid values for the field.
     * This is similar to {@link #range()}, however this method refines the result
     * using the temporal. For example, if the field is {@code DAY_OF_MONTH} the
     * {@code range} method is not accurate as there are four possible month lengths,
     * 28, 29, 30 and 31 days. Using this method with a date allows the range to be
     * accurate, returning just one of those four options.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link TemporalAccessor#range(TemporalField)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisField.rangeRefinedBy(temporal);
     *   temporal = temporal.range(thisField);
     * </pre>
     * It is recommended to use the second approach, {@code range(TemporalField)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should perform any queries or calculations using the fields
     * available in {@link ChronoField}.
     * If the field is not supported an {@code UnsupportedTemporalTypeException} must be thrown.
     *
     * @param temporal  the temporal object used to refine the result, not null
     * @return the range of valid values for this field, not null
     * @throws DateTimeException if the range for the field cannot be obtained
     * @throws UnsupportedTemporalTypeException if the field is not supported by the temporal
     */
    ValueRange rangeRefinedBy(TemporalAccessor temporal);

    /**
     * Gets the value of this field from the specified temporal object.
     * <p>
     * This queries the temporal object for the value of this field.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link TemporalAccessor#getLong(TemporalField)}
     * (or {@link TemporalAccessor#get(TemporalField)}):
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisField.getFrom(temporal);
     *   temporal = temporal.getLong(thisField);
     * </pre>
     * It is recommended to use the second approach, {@code getLong(TemporalField)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should perform any queries or calculations using the fields
     * available in {@link ChronoField}.
     * If the field is not supported an {@code UnsupportedTemporalTypeException} must be thrown.
     *
     * @param temporal  the temporal object to query, not null
     * @return the value of this field, not null
     * @throws DateTimeException if a value for the field cannot be obtained
     * @throws UnsupportedTemporalTypeException if the field is not supported by the temporal
     * @throws ArithmeticException if numeric overflow occurs
     */
    long getFrom(TemporalAccessor temporal);

    /**
     * Returns a copy of the specified temporal object with the value of this field set.
     * <p>
     * This returns a new temporal object based on the specified one with the value for
     * this field changed. For example, on a {@code LocalDate}, this could be used to
     * set the year, month or day-of-month.
     * The returned object has the same observable type as the specified object.
     * <p>
     * In some cases, changing a field is not fully defined. For example, if the target object is
     * a date representing the 31st January, then changing the month to February would be unclear.
     * In cases like this, the implementation is responsible for resolving the result.
     * Typically it will choose the previous valid date, which would be the last valid
     * day of February in this example.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#with(TemporalField, long)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisField.adjustInto(temporal);
     *   temporal = temporal.with(thisField);
     * </pre>
     * It is recommended to use the second approach, {@code with(TemporalField)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should perform any queries or calculations using the fields
     * available in {@link ChronoField}.
     * If the field is not supported an {@code UnsupportedTemporalTypeException} must be thrown.
     * <p>
     * Implementations must not alter the specified temporal object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     *
     * @param <R>  the type of the Temporal object
     * @param temporal the temporal object to adjust, not null
     * @param newValue the new value of the field
     * @return the adjusted temporal object, not null
     * @throws DateTimeException if the field cannot be set
     * @throws UnsupportedTemporalTypeException if the field is not supported by the temporal
     * @throws ArithmeticException if numeric overflow occurs
     */
    <R extends Temporal> R adjustInto(R temporal, long newValue);

    /**
     * Resolves this field to provide a simpler alternative or a date.
     * <p>
     * This method is invoked during the resolve phase of parsing.
     * It is designed to allow application defined fields to be simplified into
     * more standard fields, such as those on {@code ChronoField}, or into a date.
     * <p>
     * Applications should not normally invoke this method directly.
     *
     * @implSpec
     * If an implementation represents a field that can be simplified, or
     * combined with others, then this method must be implemented.
     * <p>
     * The specified map contains the current state of the parse.
     * The map is mutable and must be mutated to resolve the field and
     * any related fields. This method will only be invoked during parsing
     * if the map contains this field, and implementations should therefore
     * assume this field is present.
     * <p>
     * Resolving a field will consist of looking at the value of this field,
     * and potentially other fields, and either updating the map with a
     * simpler value, such as a {@code ChronoField}, or returning a
     * complete {@code ChronoLocalDate}. If a resolve is successful,
     * the code must remove all the fields that were resolved from the map,
     * including this field.
     * <p>
     * For example, the {@code IsoFields} class contains the quarter-of-year
     * and day-of-quarter fields. The implementation of this method in that class
     * resolves the two fields plus the {@link ChronoField#YEAR YEAR} into a
     * complete {@code LocalDate}. The resolve method will remove all three
     * fields from the map before returning the {@code LocalDate}.
     * <p>
     * A partially complete temporal is used to allow the chronology and zone
     * to be queried. In general, only the chronology will be needed.
     * Querying items other than the zone or chronology is undefined and
     * must not be relied on.
     * The behavior of other methods such as {@code get}, {@code getLong},
     * {@code range} and {@code isSupported} is unpredictable and the results undefined.
     * <p>
     * If resolution should be possible, but the data is invalid, the resolver
     * style should be used to determine an appropriate level of leniency, which
     * may require throwing a {@code DateTimeException} or {@code ArithmeticException}.
     * If no resolution is possible, the resolve method must return null.
     * <p>
     * When resolving time fields, the map will be altered and null returned.
     * When resolving date fields, the date is normally returned from the method,
     * with the map altered to remove the resolved fields. However, it would also
     * be acceptable for the date fields to be resolved into other {@code ChronoField}
     * instances that can produce a date, such as {@code EPOCH_DAY}.
     * <p>
     * Not all {@code TemporalAccessor} implementations are accepted as return values.
     * Implementations that call this method must accept {@code ChronoLocalDate},
     * {@code ChronoLocalDateTime}, {@code ChronoZonedDateTime} and {@code LocalTime}.
     * <p>
     * The default implementation must return null.
     *
     * @param fieldValues  the map of fields to values, which can be updated, not null
     * @param partialTemporal  the partially complete temporal to query for zone and
     *  chronology; querying for other things is undefined and not recommended, not null
     * @param resolverStyle  the requested type of resolve, not null
     * @return the resolved temporal object; null if resolving only
     *  changed the map, or no resolve occurred
     * @throws ArithmeticException if numeric overflow occurs
     * @throws DateTimeException if resolving results in an error. This must not be thrown
     *  by querying a field on the temporal without first checking if it is supported
     */
    default TemporalAccessor resolve(
            Map<TemporalField, Long> fieldValues,
            TemporalAccessor partialTemporal,
            ResolverStyle resolverStyle) {
        return null;
    }

    /**
     * Gets a descriptive name for the field.
     * <p>
     * The should be of the format 'BaseOfRange', such as 'MonthOfYear',
     * unless the field has a range of {@code FOREVER}, when only
     * the base unit is mentioned, such as 'Year' or 'Era'.
     *
     * @return the name of the field, not null
     */
    @Override
    String toString();


}
