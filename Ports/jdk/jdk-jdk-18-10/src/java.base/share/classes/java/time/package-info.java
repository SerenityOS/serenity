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
 * The main API for dates, times, instants, and durations.
 * </p>
 * <p>
 * The classes defined here represent the principle date-time concepts,
 * including instants, durations, dates, times, time-zones and periods.
 * They are based on the ISO calendar system, which is the <i>de facto</i> world
 * calendar following the proleptic Gregorian rules.
 * All the classes are immutable and thread-safe.
 * </p>
 * <p>
 * Each date time instance is composed of fields that are conveniently
 * made available by the APIs.  For lower level access to the fields refer
 * to the {@code java.time.temporal} package.
 * Each class includes support for printing and parsing all manner of dates and times.
 * Refer to the {@code java.time.format} package for customization options.
 * </p>
 * <p>
 * The {@code java.time.chrono} package contains the calendar neutral API
 * {@link java.time.chrono.ChronoLocalDate ChronoLocalDate},
 * {@link java.time.chrono.ChronoLocalDateTime ChronoLocalDateTime},
 * {@link java.time.chrono.ChronoZonedDateTime ChronoZonedDateTime} and
 * {@link java.time.chrono.Era Era}.
 * This is intended for use by applications that need to use localized calendars.
 * It is recommended that applications use the ISO-8601 date and time classes from
 * this package across system boundaries, such as to the database or across the network.
 * The calendar neutral API should be reserved for interactions with users.
 * </p>
 *
 * <h2>Dates and Times</h2>
 * <p>
 * {@link java.time.Instant} is essentially a numeric timestamp.
 * The current Instant can be retrieved from a {@link java.time.Clock}.
 * This is useful for logging and persistence of a point in time
 * and has in the past been associated with storing the result
 * from {@link java.lang.System#currentTimeMillis()}.
 * </p>
 * <p>
 * {@link java.time.LocalDate} stores a date without a time.
 * This stores a date like '2010-12-03' and could be used to store a birthday.
 * </p>
 * <p>
 * {@link java.time.LocalTime} stores a time without a date.
 * This stores a time like '11:30' and could be used to store an opening or closing time.
 * </p>
 * <p>
 * {@link java.time.LocalDateTime} stores a date and time.
 * This stores a date-time like '2010-12-03T11:30'.
 * </p>
 * <p>
 * {@link java.time.ZonedDateTime} stores a date and time with a time-zone.
 * This is useful if you want to perform accurate calculations of
 * dates and times taking into account the {@link java.time.ZoneId}, such as 'Europe/Paris'.
 * Where possible, it is recommended to use a simpler class without a time-zone.
 * The widespread use of time-zones tends to add considerable complexity to an application.
 * </p>
 *
 * <h2>Duration and Period</h2>
 * <p>
 * Beyond dates and times, the API also allows the storage of periods and durations of time.
 * A {@link java.time.Duration} is a simple measure of time along the time-line in nanoseconds.
 * A {@link java.time.Period} expresses an amount of time in units meaningful
 * to humans, such as years or days.
 * </p>
 *
 * <h2>Additional value types</h2>
 * <p>
 * {@link java.time.Month} stores a month on its own.
 * This stores a single month-of-year in isolation, such as 'DECEMBER'.
 * </p>
 * <p>
 * {@link java.time.DayOfWeek} stores a day-of-week on its own.
 * This stores a single day-of-week in isolation, such as 'TUESDAY'.
 * </p>
 * <p>
 * {@link java.time.Year} stores a year on its own.
 * This stores a single year in isolation, such as '2010'.
 * </p>
 * <p>
 * {@link java.time.YearMonth} stores a year and month without a day or time.
 * This stores a year and month, such as '2010-12' and could be used for a credit card expiry.
 * </p>
 * <p>
 * {@link java.time.MonthDay} stores a month and day without a year or time.
 * This stores a month and day-of-month, such as '--12-03' and
 * could be used to store an annual event like a birthday without storing the year.
 * </p>
 * <p>
 * {@link java.time.OffsetTime} stores a time and offset from UTC without a date.
 * This stores a date like '11:30+01:00'.
 * The {@link java.time.ZoneOffset ZoneOffset} is of the form '+01:00'.
 * </p>
 * <p>
 * {@link java.time.OffsetDateTime} stores a date and time and offset from UTC.
 * This stores a date-time like '2010-12-03T11:30+01:00'.
 * This is sometimes found in XML messages and other forms of persistence,
 * but contains less information than a full time-zone.
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
 *
 * <h2>Design notes (non normative)</h2>
 * <p>
 * The API has been designed to reject null early and to be clear about this behavior.
 * A key exception is any method that takes an object and returns a boolean, for the purpose
 * of checking or validating, will generally return false for null.
 * </p>
 * <p>
 * The API is designed to be type-safe where reasonable in the main high-level API.
 * Thus, there are separate classes for the distinct concepts of date, time and date-time,
 * plus variants for offset and time-zone.
 * This can seem like a lot of classes, but most applications can begin with just five date/time types.
 * <ul>
 * <li>{@link java.time.Instant} - a timestamp</li>
 * <li>{@link java.time.LocalDate} - a date without a time, or any reference to an offset or time-zone</li>
 * <li>{@link java.time.LocalTime} - a time without a date, or any reference to an offset or time-zone</li>
 * <li>{@link java.time.LocalDateTime} - combines date and time, but still without any offset or time-zone</li>
 * <li>{@link java.time.ZonedDateTime} - a "full" date-time with time-zone and resolved offset from UTC/Greenwich</li>
 * </ul>
 * <p>
 * {@code Instant} is the closest equivalent class to {@code java.util.Date}.
 * {@code ZonedDateTime} is the closest equivalent class to {@code java.util.GregorianCalendar}.
 * </p>
 * <p>
 * Where possible, applications should use {@code LocalDate}, {@code LocalTime} and {@code LocalDateTime}
 * to better model the domain. For example, a birthday should be stored in a code {@code LocalDate}.
 * Bear in mind that any use of a {@linkplain java.time.ZoneId time-zone}, such as 'Europe/Paris', adds
 * considerable complexity to a calculation.
 * Many applications can be written only using {@code LocalDate}, {@code LocalTime} and {@code Instant},
 * with the time-zone added at the user interface (UI) layer.
 * </p>
 * <p>
 * The offset-based date-time types {@code OffsetTime} and {@code OffsetDateTime},
 * are intended primarily for use with network protocols and database access.
 * For example, most databases cannot automatically store a time-zone like 'Europe/Paris', but
 * they can store an offset like '+02:00'.
 * </p>
 * <p>
 * Classes are also provided for the most important sub-parts of a date, including {@code Month},
 * {@code DayOfWeek}, {@code Year}, {@code YearMonth} and {@code MonthDay}.
 * These can be used to model more complex date-time concepts.
 * For example, {@code YearMonth} is useful for representing a credit card expiry.
 * </p>
 * <p>
 * Note that while there are a large number of classes representing different aspects of dates,
 * there are relatively few dealing with different aspects of time.
 * Following type-safety to its logical conclusion would have resulted in classes for
 * hour-minute, hour-minute-second and hour-minute-second-nanosecond.
 * While logically pure, this was not a practical option as it would have almost tripled the
 * number of classes due to the combinations of date and time.
 * Thus, {@code LocalTime} is used for all precisions of time, with zeroes used to imply lower precision.
 * </p>
 * <p>
 * Following full type-safety to its ultimate conclusion might also argue for a separate class
 * for each field in date-time, such as a class for HourOfDay and another for DayOfMonth.
 * This approach was tried, but was excessively complicated in the Java language, lacking usability.
 * A similar problem occurs with periods.
 * There is a case for a separate class for each period unit, such as a type for Years and a type for Minutes.
 * However, this yields a lot of classes and a problem of type conversion.
 * Thus, the set of date-time types provided is a compromise between purity and practicality.
 * </p>
 * <p>
 * The API has a relatively large surface area in terms of number of methods.
 * This is made manageable through the use of consistent method prefixes.
 * <ul>
 * <li>{@code of} - static factory method</li>
 * <li>{@code parse} - static factory method focussed on parsing</li>
 * <li>{@code get} - gets the value of something</li>
 * <li>{@code is} - checks if something is true</li>
 * <li>{@code with} - the immutable equivalent of a setter</li>
 * <li>{@code plus} - adds an amount to an object</li>
 * <li>{@code minus} - subtracts an amount from an object</li>
 * <li>{@code to} - converts this object to another type</li>
 * <li>{@code at} - combines this object with another, such as {@code date.atTime(time)}</li>
 * </ul>
 * <p>
 * Multiple calendar systems is an awkward addition to the design challenges.
 * The first principle is that most users want the standard ISO calendar system.
 * As such, the main classes are ISO-only. The second principle is that most of those that want a
 * non-ISO calendar system want it for user interaction, thus it is a UI localization issue.
 * As such, date and time objects should be held as ISO objects in the data model and persistent
 * storage, only being converted to and from a local calendar for display.
 * The calendar system would be stored separately in the user preferences.
 * </p>
 * <p>
 * There are, however, some limited use cases where users believe they need to store and use
 * dates in arbitrary calendar systems throughout the application.
 * This is supported by {@link java.time.chrono.ChronoLocalDate}, however it is vital to read
 * all the associated warnings in the Javadoc of that interface before using it.
 * In summary, applications that require general interoperation between multiple calendar systems
 * typically need to be written in a very different way to those only using the ISO calendar,
 * thus most applications should just use ISO and avoid {@code ChronoLocalDate}.
 * </p>
 * <p>
 * The API is also designed for user extensibility, as there are many ways of calculating time.
 * The {@linkplain java.time.temporal.TemporalField field} and {@linkplain java.time.temporal.TemporalUnit unit}
 * API, accessed via {@link java.time.temporal.TemporalAccessor TemporalAccessor} and
 * {@link java.time.temporal.Temporal Temporal} provide considerable flexibility to applications.
 * In addition, the {@link java.time.temporal.TemporalQuery TemporalQuery} and
 * {@link java.time.temporal.TemporalAdjuster TemporalAdjuster} interfaces provide day-to-day
 * power, allowing code to read close to business requirements:
 * </p>
 * <pre>
 *   LocalDate customerBirthday = customer.loadBirthdayFromDatabase();
 *   LocalDate today = LocalDate.now();
 *   if (customerBirthday.equals(today)) {
 *     LocalDate specialOfferExpiryDate = today.plusWeeks(2).with(next(FRIDAY));
 *     customer.sendBirthdaySpecialOffer(specialOfferExpiryDate);
 *   }
 *
 * </pre>
 *
 * @since 1.8
 */
package java.time;
