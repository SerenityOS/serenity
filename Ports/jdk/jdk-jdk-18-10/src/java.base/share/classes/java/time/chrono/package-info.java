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

/**
 * <p>
 * Generic API for calendar systems other than the default ISO.
 * </p>
 * <p>
 * The main API is based around the calendar system defined in ISO-8601.
 * However, there are other calendar systems, and this package provides basic support for them.
 * The alternate calendars are provided in the {@link java.time.chrono} package.
 * </p>
 * <p>
 * A calendar system is defined by the {@link java.time.chrono.Chronology} interface,
 * while a date in a calendar system is defined by the {@link java.time.chrono.ChronoLocalDate} interface.
 * </p>
 * <p>
 * It is intended that applications use the main API whenever possible, including code to read and write
 * from a persistent data store, such as a database, and to send dates and times across a network.
 * The "chrono" classes are then used at the user interface level to deal with localized input/output.
 * See {@link java.time.chrono.ChronoLocalDate ChronoLocalDate}
 * for a full discussion of the issues.
 * </p>
 * <p>
 * Using non-ISO calendar systems in an application introduces significant extra complexity.
 * Ensure that the warnings and recommendations in {@code ChronoLocalDate} have been read before
 * working with the "chrono" interfaces.
 * </p>
 * <p>
 * The supported calendar systems includes:
 * </p>
 * <ul>
 * <li>{@link java.time.chrono.HijrahChronology Hijrah calendar}</li>
 * <li>{@link java.time.chrono.JapaneseChronology Japanese calendar}</li>
 * <li>{@link java.time.chrono.MinguoChronology Minguo calendar}</li>
 * <li>{@link java.time.chrono.ThaiBuddhistChronology Thai Buddhist calendar}</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <p>
 * This example lists today's date for all of the available calendars.
 * </p>
 * <pre>
 *   // Enumerate the list of available calendars and print today's date for each.
 *       Set&lt;Chronology&gt; chronos = Chronology.getAvailableChronologies();
 *       for (Chronology chrono : chronos) {
 *           ChronoLocalDate date = chrono.dateNow();
 *           System.out.printf("   %20s: %s%n", chrono.getId(), date.toString());
 *       }
 * </pre>
 *
 * <p>
 * This example creates and uses a date in a named non-ISO calendar system.
 * </p>
 * <pre>
 *   // Print the Thai Buddhist date
 *       ChronoLocalDate now1 = Chronology.of("ThaiBuddhist").dateNow();
 *       int day = now1.get(ChronoField.DAY_OF_MONTH);
 *       int dow = now1.get(ChronoField.DAY_OF_WEEK);
 *       int month = now1.get(ChronoField.MONTH_OF_YEAR);
 *       int year = now1.get(ChronoField.YEAR);
 *       System.out.printf("  Today is %s %s %d-%s-%d%n", now1.getChronology().getId(),
 *                 dow, day, month, year);
 *   // Print today's date and the last day of the year for the Thai Buddhist Calendar.
 *       ChronoLocalDate first = now1
 *                 .with(ChronoField.DAY_OF_MONTH, 1)
 *                 .with(ChronoField.MONTH_OF_YEAR, 1);
 *       ChronoLocalDate last = first
 *                 .plus(1, ChronoUnit.YEARS)
 *                 .minus(1, ChronoUnit.DAYS);
 *       System.out.printf("  %s: 1st of year: %s; end of year: %s%n", last.getChronology().getId(),
 *                 first, last);
 *  </pre>
 *
 * <p>
 * This example creates and uses a date in a specific ThaiBuddhist calendar system.
 * </p>
 * <pre>
 *   // Print the Thai Buddhist date
 *       ThaiBuddhistDate now1 = ThaiBuddhistDate.now();
 *       int day = now1.get(ChronoField.DAY_OF_MONTH);
 *       int dow = now1.get(ChronoField.DAY_OF_WEEK);
 *       int month = now1.get(ChronoField.MONTH_OF_YEAR);
 *       int year = now1.get(ChronoField.YEAR);
 *       System.out.printf("  Today is %s %s %d-%s-%d%n", now1.getChronology().getId(),
 *                 dow, day, month, year);
 *
 *   // Print today's date and the last day of the year for the Thai Buddhist Calendar.
 *       ThaiBuddhistDate first = now1
 *                 .with(ChronoField.DAY_OF_MONTH, 1)
 *                 .with(ChronoField.MONTH_OF_YEAR, 1);
 *       ThaiBuddhistDate last = first
 *                 .plus(1, ChronoUnit.YEARS)
 *                 .minus(1, ChronoUnit.DAYS);
 *       System.out.printf("  %s: 1st of year: %s; end of year: %s%n", last.getChronology().getId(),
 *                 first, last);
 *  </pre>
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
package java.time.chrono;
