/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>
 * Access to date and time using fields and units, and date time adjusters.
 * </p>
 * <p>
 * This package expands on the base package to provide additional functionality for
 * more powerful use cases. Support is included for:
 * </p>
 * <ul>
 * <li>Units of date-time, such as years, months, days and hours</li>
 * <li>Fields of date-time, such as month-of-year, day-of-week or hour-of-day</li>
 * <li>Date-time adjustment functions</li>
 * <li>Different definitions of weeks</li>
 * </ul>
 *
 * <h2>Fields and Units</h2>
 * <p>
 * Dates and times are expressed in terms of fields and units.
 * A unit is used to measure an amount of time, such as years, days or minutes.
 * All units implement {@link java.time.temporal.TemporalUnit}.
 * The set of well known units is defined in {@link java.time.temporal.ChronoUnit}, such as {@code DAYS}.
 * The unit interface is designed to allow application defined units.
 * </p>
 * <p>
 * A field is used to express part of a larger date-time, such as year, month-of-year or second-of-minute.
 * All fields implement {@link java.time.temporal.TemporalField}.
 * The set of well known fields are defined in {@link java.time.temporal.ChronoField}, such as {@code HOUR_OF_DAY}.
 * Additional fields are defined by {@link java.time.temporal.JulianFields}, {@link java.time.temporal.WeekFields}
 * and {@link java.time.temporal.IsoFields}.
 * The field interface is designed to allow application defined fields.
 * </p>
 * <p>
 * This package provides tools that allow the units and fields of date and time to be accessed
 * in a general way most suited for frameworks.
 * {@link java.time.temporal.Temporal} provides the abstraction for date time types that support fields.
 * Its methods support getting the value of a field, creating a new date time with the value of
 * a field modified, and querying for additional information, typically used to extract the offset or time-zone.
 * </p>
 * <p>
 * One use of fields in application code is to retrieve fields for which there is no convenience method.
 * For example, getting the day-of-month is common enough that there is a method on {@code LocalDate}
 * called {@code getDayOfMonth()}. However for more unusual fields it is necessary to use the field.
 * For example, {@code date.get(ChronoField.ALIGNED_WEEK_OF_MONTH)}.
 * The fields also provide access to the range of valid values.
 * </p>
 *
 * <h2>Adjustment and Query</h2>
 * <p>
 * A key part of the date-time problem space is adjusting a date to a new, related value,
 * such as the "last day of the month", or "next Wednesday".
 * These are modeled as functions that adjust a base date-time.
 * The functions implement {@link java.time.temporal.TemporalAdjuster} and operate on {@code Temporal}.
 * A set of common functions are provided in {@link java.time.temporal.TemporalAdjusters}.
 * For example, to find the first occurrence of a day-of-week after a given date, use
 * {@link java.time.temporal.TemporalAdjusters#next(DayOfWeek)}, such as
 * {@code date.with(next(MONDAY))}.
 * Applications can also define adjusters by implementing {@link java.time.temporal.TemporalAdjuster}.
 * </p>
 * <p>
 * The {@link java.time.temporal.TemporalAmount} interface models amounts of relative time.
 * </p>
 * <p>
 * In addition to adjusting a date-time, an interface is provided to enable querying via
 * {@link java.time.temporal.TemporalQuery}.
 * The most common implementations of the query interface are method references.
 * The {@code from(TemporalAccessor)} methods on major classes can all be used, such as
 * {@code LocalDate::from} or {@code Month::from}.
 * Further implementations are provided in {@link java.time.temporal.TemporalQueries} as static methods.
 * Applications can also define queries by implementing {@link java.time.temporal.TemporalQuery}.
 * </p>
 *
 * <h2>Weeks</h2>
 * <p>
 * Different locales have different definitions of the week.
 * For example, in Europe the week typically starts on a Monday, while in the US it starts on a Sunday.
 * The {@link java.time.temporal.WeekFields} class models this distinction.
 * </p>
 * <p>
 * The ISO calendar system defines an additional week-based division of years.
 * This defines a year based on whole Monday to Monday weeks.
 * This is modeled in {@link java.time.temporal.IsoFields}.
 * </p>
 *
 * <h2>Package specification</h2>
 * <p>
 * Unless otherwise noted, passing a null argument to a constructor or method in any class or interface
 * in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 * The Javadoc "@param" definition is used to summarise the null-behavior.
 * The "@throws {@link java.lang.NullPointerException}" is not explicitly documented in each method.
 * </p>
 * <p>
 * All calculations should check for numeric overflow and throw either an {@link java.lang.ArithmeticException}
 * or a {@link java.time.DateTimeException}.
 * </p>
 * @since 1.8
 */
package java.time.temporal;
