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

import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.TemporalAdjusters.nextOrSame;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import sun.util.logging.PlatformLogger;

/**
 * An abstract implementation of a calendar system, used to organize and identify dates.
 * <p>
 * The main date and time API is built on the ISO calendar system.
 * The chronology operates behind the scenes to represent the general concept of a calendar system.
 * <p>
 * See {@link Chronology} for more details.
 *
 * @implSpec
 * This class is separated from the {@code Chronology} interface so that the static methods
 * are not inherited. While {@code Chronology} can be implemented directly, it is strongly
 * recommended to extend this abstract class instead.
 * <p>
 * This class must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * Subclasses should be Serializable wherever possible.
 *
 * @since 1.8
 */
public abstract class AbstractChronology implements Chronology {

    /**
     * Map of available calendars by ID.
     */
    private static final ConcurrentHashMap<String, Chronology> CHRONOS_BY_ID = new ConcurrentHashMap<>();
    /**
     * Map of available calendars by calendar type.
     */
    private static final ConcurrentHashMap<String, Chronology> CHRONOS_BY_TYPE = new ConcurrentHashMap<>();

    /**
     * Register a Chronology by its ID and type for lookup by {@link #of(String)}.
     * Chronologies must not be registered until they are completely constructed.
     * Specifically, not in the constructor of Chronology.
     *
     * @param chrono the chronology to register; not null
     * @return the already registered Chronology if any, may be null
     */
    static Chronology registerChrono(Chronology chrono) {
        return registerChrono(chrono, chrono.getId());
    }

    /**
     * Register a Chronology by ID and type for lookup by {@link #of(String)}.
     * Chronos must not be registered until they are completely constructed.
     * Specifically, not in the constructor of Chronology.
     *
     * @param chrono the chronology to register; not null
     * @param id the ID to register the chronology; not null
     * @return the already registered Chronology if any, may be null
     */
    static Chronology registerChrono(Chronology chrono, String id) {
        Chronology prev = CHRONOS_BY_ID.putIfAbsent(id, chrono);
        if (prev == null) {
            String type = chrono.getCalendarType();
            if (type != null) {
                CHRONOS_BY_TYPE.putIfAbsent(type, chrono);
            }
        }
        return prev;
    }

    /**
     * Initialization of the maps from id and type to Chronology.
     * The ServiceLoader is used to find and register any implementations
     * of {@link java.time.chrono.AbstractChronology} found in the bootclass loader.
     * The built-in chronologies are registered explicitly.
     * Calendars configured via the Thread's context classloader are local
     * to that thread and are ignored.
     * <p>
     * The initialization is done only once using the registration
     * of the IsoChronology as the test and the final step.
     * Multiple threads may perform the initialization concurrently.
     * Only the first registration of each Chronology is retained by the
     * ConcurrentHashMap.
     * @return true if the cache was initialized
     */
    private static boolean initCache() {
        if (CHRONOS_BY_ID.get("ISO") == null) {
            // Initialization is incomplete

            // Register built-in Chronologies
            registerChrono(HijrahChronology.INSTANCE);
            registerChrono(JapaneseChronology.INSTANCE);
            registerChrono(MinguoChronology.INSTANCE);
            registerChrono(ThaiBuddhistChronology.INSTANCE);

            // Register Chronologies from the ServiceLoader
            @SuppressWarnings("rawtypes")
            ServiceLoader<AbstractChronology> loader =  ServiceLoader.load(AbstractChronology.class, null);
            for (AbstractChronology chrono : loader) {
                String id = chrono.getId();
                if (id.equals("ISO") || registerChrono(chrono) != null) {
                    // Log the attempt to replace an existing Chronology
                    PlatformLogger logger = PlatformLogger.getLogger("java.time.chrono");
                    logger.warning("Ignoring duplicate Chronology, from ServiceLoader configuration "  + id);
                }
            }

            // finally, register IsoChronology to mark initialization is complete
            registerChrono(IsoChronology.INSTANCE);
            return true;
        }
        return false;
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Chronology} from a locale.
     * <p>
     * See {@link Chronology#ofLocale(Locale)}.
     *
     * @param locale  the locale to use to obtain the calendar system, not null
     * @return the calendar system associated with the locale, not null
     * @throws java.time.DateTimeException if the locale-specified calendar cannot be found
     */
    static Chronology ofLocale(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        String type = locale.getUnicodeLocaleType("ca");
        if (type == null || "iso".equals(type) || "iso8601".equals(type)) {
            return IsoChronology.INSTANCE;
        }
        // Not pre-defined; lookup by the type
        do {
            Chronology chrono = CHRONOS_BY_TYPE.get(type);
            if (chrono != null) {
                return chrono;
            }
            // If not found, do the initialization (once) and repeat the lookup
        } while (initCache());

        // Look for a Chronology using ServiceLoader of the Thread's ContextClassLoader
        // Application provided Chronologies must not be cached
        @SuppressWarnings("rawtypes")
        ServiceLoader<Chronology> loader = ServiceLoader.load(Chronology.class);
        for (Chronology chrono : loader) {
            if (type.equals(chrono.getCalendarType())) {
                return chrono;
            }
        }
        throw new DateTimeException("Unknown calendar system: " + type);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Chronology} from a chronology ID or
     * calendar system type.
     * <p>
     * See {@link Chronology#of(String)}.
     *
     * @param id  the chronology ID or calendar system type, not null
     * @return the chronology with the identifier requested, not null
     * @throws java.time.DateTimeException if the chronology cannot be found
     */
    static Chronology of(String id) {
        Objects.requireNonNull(id, "id");
        do {
            Chronology chrono = of0(id);
            if (chrono != null) {
                return chrono;
            }
            // If not found, do the initialization (once) and repeat the lookup
        } while (initCache());

        // Look for a Chronology using ServiceLoader of the Thread's ContextClassLoader
        // Application provided Chronologies must not be cached
        @SuppressWarnings("rawtypes")
        ServiceLoader<Chronology> loader = ServiceLoader.load(Chronology.class);
        for (Chronology chrono : loader) {
            if (id.equals(chrono.getId()) || id.equals(chrono.getCalendarType())) {
                return chrono;
            }
        }
        throw new DateTimeException("Unknown chronology: " + id);
    }

    /**
     * Obtains an instance of {@code Chronology} from a chronology ID or
     * calendar system type.
     *
     * @param id  the chronology ID or calendar system type, not null
     * @return the chronology with the identifier requested, or {@code null} if not found
     */
    private static Chronology of0(String id) {
        Chronology chrono = CHRONOS_BY_ID.get(id);
        if (chrono == null) {
            chrono = CHRONOS_BY_TYPE.get(id);
        }
        return chrono;
    }

    /**
     * Returns the available chronologies.
     * <p>
     * Each returned {@code Chronology} is available for use in the system.
     * The set of chronologies includes the system chronologies and
     * any chronologies provided by the application via ServiceLoader
     * configuration.
     *
     * @return the independent, modifiable set of the available chronology IDs, not null
     */
    static Set<Chronology> getAvailableChronologies() {
        initCache();       // force initialization
        HashSet<Chronology> chronos = new HashSet<>(CHRONOS_BY_ID.values());

        /// Add in Chronologies from the ServiceLoader configuration
        @SuppressWarnings("rawtypes")
        ServiceLoader<Chronology> loader = ServiceLoader.load(Chronology.class);
        for (Chronology chrono : loader) {
            chronos.add(chrono);
        }
        return chronos;
    }

    //-----------------------------------------------------------------------
    /**
     * Creates an instance.
     */
    protected AbstractChronology() {
    }

    //-----------------------------------------------------------------------
    /**
     * Resolves parsed {@code ChronoField} values into a date during parsing.
     * <p>
     * Most {@code TemporalField} implementations are resolved using the
     * resolve method on the field. By contrast, the {@code ChronoField} class
     * defines fields that only have meaning relative to the chronology.
     * As such, {@code ChronoField} date fields are resolved here in the
     * context of a specific chronology.
     * <p>
     * {@code ChronoField} instances are resolved by this method, which may
     * be overridden in subclasses.
     * <ul>
     * <li>{@code EPOCH_DAY} - If present, this is converted to a date and
     *  all other date fields are then cross-checked against the date.
     * <li>{@code PROLEPTIC_MONTH} - If present, then it is split into the
     *  {@code YEAR} and {@code MONTH_OF_YEAR}. If the mode is strict or smart
     *  then the field is validated.
     * <li>{@code YEAR_OF_ERA} and {@code ERA} - If both are present, then they
     *  are combined to form a {@code YEAR}. In lenient mode, the {@code YEAR_OF_ERA}
     *  range is not validated, in smart and strict mode it is. The {@code ERA} is
     *  validated for range in all three modes. If only the {@code YEAR_OF_ERA} is
     *  present, and the mode is smart or lenient, then the last available era
     *  is assumed. In strict mode, no era is assumed and the {@code YEAR_OF_ERA} is
     *  left untouched. If only the {@code ERA} is present, then it is left untouched.
     * <li>{@code YEAR}, {@code MONTH_OF_YEAR} and {@code DAY_OF_MONTH} -
     *  If all three are present, then they are combined to form a date.
     *  In all three modes, the {@code YEAR} is validated.
     *  If the mode is smart or strict, then the month and day are validated.
     *  If the mode is lenient, then the date is combined in a manner equivalent to
     *  creating a date on the first day of the first month in the requested year,
     *  then adding the difference in months, then the difference in days.
     *  If the mode is smart, and the day-of-month is greater than the maximum for
     *  the year-month, then the day-of-month is adjusted to the last day-of-month.
     *  If the mode is strict, then the three fields must form a valid date.
     * <li>{@code YEAR} and {@code DAY_OF_YEAR} -
     *  If both are present, then they are combined to form a date.
     *  In all three modes, the {@code YEAR} is validated.
     *  If the mode is lenient, then the date is combined in a manner equivalent to
     *  creating a date on the first day of the requested year, then adding
     *  the difference in days.
     *  If the mode is smart or strict, then the two fields must form a valid date.
     * <li>{@code YEAR}, {@code MONTH_OF_YEAR}, {@code ALIGNED_WEEK_OF_MONTH} and
     *  {@code ALIGNED_DAY_OF_WEEK_IN_MONTH} -
     *  If all four are present, then they are combined to form a date.
     *  In all three modes, the {@code YEAR} is validated.
     *  If the mode is lenient, then the date is combined in a manner equivalent to
     *  creating a date on the first day of the first month in the requested year, then adding
     *  the difference in months, then the difference in weeks, then in days.
     *  If the mode is smart or strict, then the all four fields are validated to
     *  their outer ranges. The date is then combined in a manner equivalent to
     *  creating a date on the first day of the requested year and month, then adding
     *  the amount in weeks and days to reach their values. If the mode is strict,
     *  the date is additionally validated to check that the day and week adjustment
     *  did not change the month.
     * <li>{@code YEAR}, {@code MONTH_OF_YEAR}, {@code ALIGNED_WEEK_OF_MONTH} and
     *  {@code DAY_OF_WEEK} - If all four are present, then they are combined to
     *  form a date. The approach is the same as described above for
     *  years, months and weeks in {@code ALIGNED_DAY_OF_WEEK_IN_MONTH}.
     *  The day-of-week is adjusted as the next or same matching day-of-week once
     *  the years, months and weeks have been handled.
     * <li>{@code YEAR}, {@code ALIGNED_WEEK_OF_YEAR} and {@code ALIGNED_DAY_OF_WEEK_IN_YEAR} -
     *  If all three are present, then they are combined to form a date.
     *  In all three modes, the {@code YEAR} is validated.
     *  If the mode is lenient, then the date is combined in a manner equivalent to
     *  creating a date on the first day of the requested year, then adding
     *  the difference in weeks, then in days.
     *  If the mode is smart or strict, then the all three fields are validated to
     *  their outer ranges. The date is then combined in a manner equivalent to
     *  creating a date on the first day of the requested year, then adding
     *  the amount in weeks and days to reach their values. If the mode is strict,
     *  the date is additionally validated to check that the day and week adjustment
     *  did not change the year.
     * <li>{@code YEAR}, {@code ALIGNED_WEEK_OF_YEAR} and {@code DAY_OF_WEEK} -
     *  If all three are present, then they are combined to form a date.
     *  The approach is the same as described above for years and weeks in
     *  {@code ALIGNED_DAY_OF_WEEK_IN_YEAR}. The day-of-week is adjusted as the
     *  next or same matching day-of-week once the years and weeks have been handled.
     * </ul>
     * <p>
     * The default implementation is suitable for most calendar systems.
     * If {@link java.time.temporal.ChronoField#YEAR_OF_ERA} is found without an {@link java.time.temporal.ChronoField#ERA}
     * then the last era in {@link #eras()} is used.
     * The implementation assumes a 7 day week, that the first day-of-month
     * has the value 1, that first day-of-year has the value 1, and that the
     * first of the month and year always exists.
     *
     * @param fieldValues  the map of fields to values, which can be updated, not null
     * @param resolverStyle  the requested type of resolve, not null
     * @return the resolved date, null if insufficient information to create a date
     * @throws java.time.DateTimeException if the date cannot be resolved, typically
     *  because of a conflict in the input data
     */
    @Override
    public ChronoLocalDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        // check epoch-day before inventing era
        if (fieldValues.containsKey(EPOCH_DAY)) {
            return dateEpochDay(fieldValues.remove(EPOCH_DAY));
        }

        // fix proleptic month before inventing era
        resolveProlepticMonth(fieldValues, resolverStyle);

        // invent era if necessary to resolve year-of-era
        ChronoLocalDate resolved = resolveYearOfEra(fieldValues, resolverStyle);
        if (resolved != null) {
            return resolved;
        }

        // build date
        if (fieldValues.containsKey(YEAR)) {
            if (fieldValues.containsKey(MONTH_OF_YEAR)) {
                if (fieldValues.containsKey(DAY_OF_MONTH)) {
                    return resolveYMD(fieldValues, resolverStyle);
                }
                if (fieldValues.containsKey(ALIGNED_WEEK_OF_MONTH)) {
                    if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_MONTH)) {
                        return resolveYMAA(fieldValues, resolverStyle);
                    }
                    if (fieldValues.containsKey(DAY_OF_WEEK)) {
                        return resolveYMAD(fieldValues, resolverStyle);
                    }
                }
            }
            if (fieldValues.containsKey(DAY_OF_YEAR)) {
                return resolveYD(fieldValues, resolverStyle);
            }
            if (fieldValues.containsKey(ALIGNED_WEEK_OF_YEAR)) {
                if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_YEAR)) {
                    return resolveYAA(fieldValues, resolverStyle);
                }
                if (fieldValues.containsKey(DAY_OF_WEEK)) {
                    return resolveYAD(fieldValues, resolverStyle);
                }
            }
        }
        return null;
    }

    void resolveProlepticMonth(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        Long pMonth = fieldValues.remove(PROLEPTIC_MONTH);
        if (pMonth != null) {
            if (resolverStyle != ResolverStyle.LENIENT) {
                PROLEPTIC_MONTH.checkValidValue(pMonth);
            }
            // first day-of-month is likely to be safest for setting proleptic-month
            // cannot add to year zero, as not all chronologies have a year zero
            ChronoLocalDate chronoDate = dateNow()
                    .with(DAY_OF_MONTH, 1).with(PROLEPTIC_MONTH, pMonth);
            addFieldValue(fieldValues, MONTH_OF_YEAR, chronoDate.get(MONTH_OF_YEAR));
            addFieldValue(fieldValues, YEAR, chronoDate.get(YEAR));
        }
    }

    ChronoLocalDate resolveYearOfEra(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        Long yoeLong = fieldValues.remove(YEAR_OF_ERA);
        if (yoeLong != null) {
            Long eraLong = fieldValues.remove(ERA);
            int yoe;
            if (resolverStyle != ResolverStyle.LENIENT) {
                yoe = range(YEAR_OF_ERA).checkValidIntValue(yoeLong, YEAR_OF_ERA);
            } else {
                yoe = Math.toIntExact(yoeLong);
            }
            if (eraLong != null) {
                Era eraObj = eraOf(range(ERA).checkValidIntValue(eraLong, ERA));
                addFieldValue(fieldValues, YEAR, prolepticYear(eraObj, yoe));
            } else {
                if (fieldValues.containsKey(YEAR)) {
                    int year = range(YEAR).checkValidIntValue(fieldValues.get(YEAR), YEAR);
                    ChronoLocalDate chronoDate = dateYearDay(year, 1);
                    addFieldValue(fieldValues, YEAR, prolepticYear(chronoDate.getEra(), yoe));
                } else if (resolverStyle == ResolverStyle.STRICT) {
                    // do not invent era if strict
                    // reinstate the field removed earlier, no cross-check issues
                    fieldValues.put(YEAR_OF_ERA, yoeLong);
                } else {
                    List<Era> eras = eras();
                    if (eras.isEmpty()) {
                        addFieldValue(fieldValues, YEAR, yoe);
                    } else {
                        Era eraObj = eras.get(eras.size() - 1);
                        addFieldValue(fieldValues, YEAR, prolepticYear(eraObj, yoe));
                    }
                }
            }
        } else if (fieldValues.containsKey(ERA)) {
            range(ERA).checkValidValue(fieldValues.get(ERA), ERA);  // always validated
        }
        return null;
    }

    ChronoLocalDate resolveYMD(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
            long days = Math.subtractExact(fieldValues.remove(DAY_OF_MONTH), 1);
            return date(y, 1, 1).plus(months, MONTHS).plus(days, DAYS);
        }
        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
        ValueRange domRange = range(DAY_OF_MONTH);
        int dom = domRange.checkValidIntValue(fieldValues.remove(DAY_OF_MONTH), DAY_OF_MONTH);
        if (resolverStyle == ResolverStyle.SMART) {  // previous valid
            try {
                return date(y, moy, dom);
            } catch (DateTimeException ex) {
                return date(y, moy, 1).with(TemporalAdjusters.lastDayOfMonth());
            }
        }
        return date(y, moy, dom);
    }

    ChronoLocalDate resolveYD(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            long days = Math.subtractExact(fieldValues.remove(DAY_OF_YEAR), 1);
            return dateYearDay(y, 1).plus(days, DAYS);
        }
        int doy = range(DAY_OF_YEAR).checkValidIntValue(fieldValues.remove(DAY_OF_YEAR), DAY_OF_YEAR);
        return dateYearDay(y, doy);  // smart is same as strict
    }

    ChronoLocalDate resolveYMAA(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
            long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
            long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH), 1);
            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
        }
        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
        int aw = range(ALIGNED_WEEK_OF_MONTH).checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), ALIGNED_WEEK_OF_MONTH);
        int ad = range(ALIGNED_DAY_OF_WEEK_IN_MONTH).checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH), ALIGNED_DAY_OF_WEEK_IN_MONTH);
        ChronoLocalDate date = date(y, moy, 1).plus((aw - 1) * 7 + (ad - 1), DAYS);
        if (resolverStyle == ResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
            throw new DateTimeException("Strict mode rejected resolved date as it is in a different month");
        }
        return date;
    }

    ChronoLocalDate resolveYMAD(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
            long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
            long dow = Math.subtractExact(fieldValues.remove(DAY_OF_WEEK), 1);
            return resolveAligned(date(y, 1, 1), months, weeks, dow);
        }
        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
        int aw = range(ALIGNED_WEEK_OF_MONTH).checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), ALIGNED_WEEK_OF_MONTH);
        int dow = range(DAY_OF_WEEK).checkValidIntValue(fieldValues.remove(DAY_OF_WEEK), DAY_OF_WEEK);
        ChronoLocalDate date = date(y, moy, 1).plus((aw - 1) * 7, DAYS).with(nextOrSame(DayOfWeek.of(dow)));
        if (resolverStyle == ResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
            throw new DateTimeException("Strict mode rejected resolved date as it is in a different month");
        }
        return date;
    }

    ChronoLocalDate resolveYAA(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
            long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR), 1);
            return dateYearDay(y, 1).plus(weeks, WEEKS).plus(days, DAYS);
        }
        int aw = range(ALIGNED_WEEK_OF_YEAR).checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), ALIGNED_WEEK_OF_YEAR);
        int ad = range(ALIGNED_DAY_OF_WEEK_IN_YEAR).checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR), ALIGNED_DAY_OF_WEEK_IN_YEAR);
        ChronoLocalDate date = dateYearDay(y, 1).plus((aw - 1) * 7 + (ad - 1), DAYS);
        if (resolverStyle == ResolverStyle.STRICT && date.get(YEAR) != y) {
            throw new DateTimeException("Strict mode rejected resolved date as it is in a different year");
        }
        return date;
    }

    ChronoLocalDate resolveYAD(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
        if (resolverStyle == ResolverStyle.LENIENT) {
            long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
            long dow = Math.subtractExact(fieldValues.remove(DAY_OF_WEEK), 1);
            return resolveAligned(dateYearDay(y, 1), 0, weeks, dow);
        }
        int aw = range(ALIGNED_WEEK_OF_YEAR).checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), ALIGNED_WEEK_OF_YEAR);
        int dow = range(DAY_OF_WEEK).checkValidIntValue(fieldValues.remove(DAY_OF_WEEK), DAY_OF_WEEK);
        ChronoLocalDate date = dateYearDay(y, 1).plus((aw - 1) * 7, DAYS).with(nextOrSame(DayOfWeek.of(dow)));
        if (resolverStyle == ResolverStyle.STRICT && date.get(YEAR) != y) {
            throw new DateTimeException("Strict mode rejected resolved date as it is in a different year");
        }
        return date;
    }

    ChronoLocalDate resolveAligned(ChronoLocalDate base, long months, long weeks, long dow) {
        ChronoLocalDate date = base.plus(months, MONTHS).plus(weeks, WEEKS);
        if (dow > 7) {
            date = date.plus((dow - 1) / 7, WEEKS);
            dow = ((dow - 1) % 7) + 1;
        } else if (dow < 1) {
            date = date.plus(Math.subtractExact(dow,  7) / 7, WEEKS);
            dow = ((dow + 6) % 7) + 1;
        }
        return date.with(nextOrSame(DayOfWeek.of((int) dow)));
    }

    /**
     * Adds a field-value pair to the map, checking for conflicts.
     * <p>
     * If the field is not already present, then the field-value pair is added to the map.
     * If the field is already present and it has the same value as that specified, no action occurs.
     * If the field is already present and it has a different value to that specified, then
     * an exception is thrown.
     *
     * @param field  the field to add, not null
     * @param value  the value to add, not null
     * @throws java.time.DateTimeException if the field is already present with a different value
     */
    void addFieldValue(Map<TemporalField, Long> fieldValues, ChronoField field, long value) {
        Long old = fieldValues.get(field);  // check first for better error message
        if (old != null && old.longValue() != value) {
            throw new DateTimeException("Conflict found: " + field + " " + old + " differs from " + field + " " + value);
        }
        fieldValues.put(field, value);
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this chronology to another chronology.
     * <p>
     * The comparison order first by the chronology ID string, then by any
     * additional information specific to the subclass.
     * It is "consistent with equals", as defined by {@link Comparable}.
     *
     * @implSpec
     * This implementation compares the chronology ID.
     * Subclasses must compare any additional state that they store.
     *
     * @param other  the other chronology to compare to, not null
     * @return the comparator value, negative if less, positive if greater
     */
    @Override
    public int compareTo(Chronology other) {
        return getId().compareTo(other.getId());
    }

    /**
     * Checks if this chronology is equal to another chronology.
     * <p>
     * The comparison is based on the entire state of the object.
     *
     * @implSpec
     * This implementation checks the type and calls
     * {@link #compareTo(java.time.chrono.Chronology)}.
     *
     * @param obj  the object to check, null returns false
     * @return true if this is equal to the other chronology
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
           return true;
        }
        if (obj instanceof AbstractChronology) {
            return compareTo((AbstractChronology) obj) == 0;
        }
        return false;
    }

    /**
     * A hash code for this chronology.
     * <p>
     * The hash code should be based on the entire state of the object.
     *
     * @implSpec
     * This implementation is based on the chronology ID and class.
     * Subclasses should add any additional state that they store.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return getClass().hashCode() ^ getId().hashCode();
    }

    //-----------------------------------------------------------------------
    /**
     * Outputs this chronology as a {@code String}, using the chronology ID.
     *
     * @return a string representation of this chronology, not null
     */
    @Override
    public String toString() {
        return getId();
    }

    //-----------------------------------------------------------------------
    /**
     * Writes the Chronology using a
     * <a href="{@docRoot}/serialized-form.html#java.time.chrono.Ser">dedicated serialized form</a>.
     * <pre>
     *  out.writeByte(1);  // identifies this as a Chronology
     *  out.writeUTF(getId());
     * </pre>
     *
     * @return the instance of {@code Ser}, not null
     */
    @java.io.Serial
    Object writeReplace() {
        return new Ser(Ser.CHRONO_TYPE, (Serializable)this);
    }

    /**
     * Defend against malicious streams.
     *
     * @param s the stream to read
     * @throws java.io.InvalidObjectException always
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s) throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeUTF(getId());
    }

    static Chronology readExternal(DataInput in) throws IOException {
        String id = in.readUTF();
        return Chronology.of(id);
    }

}
