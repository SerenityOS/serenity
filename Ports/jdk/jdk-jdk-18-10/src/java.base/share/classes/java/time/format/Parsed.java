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
 * Copyright (c) 2008-2013, Stephen Colebourne & Michael Nascimento Santos
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
package java.time.format;

import static java.time.format.DateTimeFormatterBuilder.DayPeriod;
import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_DAY;
import static java.time.temporal.ChronoField.HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.MICRO_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Chronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * A store of parsed data.
 * <p>
 * This class is used during parsing to collect the data. Part of the parsing process
 * involves handling optional blocks and multiple copies of the data get created to
 * support the necessary backtracking.
 * <p>
 * Once parsing is completed, this class can be used as the resultant {@code TemporalAccessor}.
 * In most cases, it is only exposed once the fields have been resolved.
 *
 * @implSpec
 * This class is a mutable context intended for use from a single thread.
 * Usage of the class is thread-safe within standard parsing as a new instance of this class
 * is automatically created for each parse and parsing is single-threaded
 *
 * @since 1.8
 */
final class Parsed implements TemporalAccessor {
    // some fields are accessed using package scope from DateTimeParseContext

    /**
     * The parsed fields.
     */
    final Map<TemporalField, Long> fieldValues = new HashMap<>();
    /**
     * The parsed zone.
     */
    ZoneId zone;
    /**
     * The parsed chronology.
     */
    Chronology chrono;
    /**
     * Whether a leap-second is parsed.
     */
    boolean leapSecond;
    /**
     * The resolver style to use.
     */
    private ResolverStyle resolverStyle;
    /**
     * The resolved date.
     */
    private ChronoLocalDate date;
    /**
     * The resolved time.
     */
    private LocalTime time;
    /**
     * The excess period from time-only parsing.
     */
    Period excessDays = Period.ZERO;
    /**
     * The parsed day period.
     */
    DayPeriod dayPeriod;

    /**
     * Creates an instance.
     */
    Parsed() {
    }

    /**
     * Creates a copy.
     */
    Parsed copy() {
        // only copy fields used in parsing stage
        Parsed cloned = new Parsed();
        cloned.fieldValues.putAll(this.fieldValues);
        cloned.zone = this.zone;
        cloned.chrono = this.chrono;
        cloned.leapSecond = this.leapSecond;
        cloned.dayPeriod = this.dayPeriod;
        return cloned;
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TemporalField field) {
        if (fieldValues.containsKey(field) ||
                (date != null && date.isSupported(field)) ||
                (time != null && time.isSupported(field))) {
            return true;
        }
        return field != null && (!(field instanceof ChronoField)) && field.isSupportedBy(this);
    }

    @Override
    public long getLong(TemporalField field) {
        Objects.requireNonNull(field, "field");
        Long value = fieldValues.get(field);
        if (value != null) {
            return value;
        }
        if (date != null && date.isSupported(field)) {
            return date.getLong(field);
        }
        if (time != null && time.isSupported(field)) {
            return time.getLong(field);
        }
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId()) {
            return (R) zone;
        } else if (query == TemporalQueries.chronology()) {
            return (R) chrono;
        } else if (query == TemporalQueries.localDate()) {
            return (R) (date != null ? LocalDate.from(date) : null);
        } else if (query == TemporalQueries.localTime()) {
            return (R) time;
        } else if (query == TemporalQueries.offset()) {
            Long offsetSecs = fieldValues.get(OFFSET_SECONDS);
            if (offsetSecs != null) {
                return (R) ZoneOffset.ofTotalSeconds(offsetSecs.intValue());
            }
            if (zone instanceof ZoneOffset) {
                return (R)zone;
            }
            return query.queryFrom(this);
        } else if (query == TemporalQueries.zone()) {
            return query.queryFrom(this);
        } else if (query == TemporalQueries.precision()) {
            return null;  // not a complete date/time
        }
        // inline TemporalAccessor.super.query(query) as an optimization
        // non-JDK classes are not permitted to make this optimization
        return query.queryFrom(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Resolves the fields in this context.
     *
     * @param resolverStyle  the resolver style, not null
     * @param resolverFields  the fields to use for resolving, null for all fields
     * @return this, for method chaining
     * @throws DateTimeException if resolving one field results in a value for
     *  another field that is in conflict
     */
    TemporalAccessor resolve(ResolverStyle resolverStyle, Set<TemporalField> resolverFields) {
        if (resolverFields != null) {
            fieldValues.keySet().retainAll(resolverFields);
        }
        this.resolverStyle = resolverStyle;
        resolveFields();
        resolveTimeLenient();
        crossCheck();
        resolvePeriod();
        resolveFractional();
        resolveInstant();
        return this;
    }

    //-----------------------------------------------------------------------
    private void resolveFields() {
        // resolve ChronoField
        resolveInstantFields();
        resolveDateFields();
        resolveTimeFields();

        // if any other fields, handle them
        // any lenient date resolution should return epoch-day
        if (fieldValues.size() > 0) {
            int changedCount = 0;
            outer:
            while (changedCount < 50) {
                for (Map.Entry<TemporalField, Long> entry : fieldValues.entrySet()) {
                    TemporalField targetField = entry.getKey();
                    TemporalAccessor resolvedObject = targetField.resolve(fieldValues, this, resolverStyle);
                    if (resolvedObject != null) {
                        if (resolvedObject instanceof ChronoZonedDateTime<?> czdt) {
                            if (zone == null) {
                                zone = czdt.getZone();
                            } else if (zone.equals(czdt.getZone()) == false) {
                                throw new DateTimeException("ChronoZonedDateTime must use the effective parsed zone: " + zone);
                            }
                            resolvedObject = czdt.toLocalDateTime();
                        }
                        if (resolvedObject instanceof ChronoLocalDateTime<?> cldt) {
                            updateCheckConflict(cldt.toLocalTime(), Period.ZERO);
                            updateCheckConflict(cldt.toLocalDate());
                            changedCount++;
                            continue outer;  // have to restart to avoid concurrent modification
                        }
                        if (resolvedObject instanceof ChronoLocalDate) {
                            updateCheckConflict((ChronoLocalDate) resolvedObject);
                            changedCount++;
                            continue outer;  // have to restart to avoid concurrent modification
                        }
                        if (resolvedObject instanceof LocalTime) {
                            updateCheckConflict((LocalTime) resolvedObject, Period.ZERO);
                            changedCount++;
                            continue outer;  // have to restart to avoid concurrent modification
                        }
                        throw new DateTimeException("Method resolve() can only return ChronoZonedDateTime, " +
                                "ChronoLocalDateTime, ChronoLocalDate or LocalTime");
                    } else if (fieldValues.containsKey(targetField) == false) {
                        changedCount++;
                        continue outer;  // have to restart to avoid concurrent modification
                    }
                }
                break;
            }
            if (changedCount == 50) {  // catch infinite loops
                throw new DateTimeException("One of the parsed fields has an incorrectly implemented resolve method");
            }
            // if something changed then have to redo ChronoField resolve
            if (changedCount > 0) {
                resolveInstantFields();
                resolveDateFields();
                resolveTimeFields();
            }
        }
    }

    private void updateCheckConflict(TemporalField targetField, TemporalField changeField, Long changeValue) {
        Long old = fieldValues.put(changeField, changeValue);
        if (old != null && old.longValue() != changeValue.longValue()) {
            throw new DateTimeException("Conflict found: " + changeField + " " + old +
                    " differs from " + changeField + " " + changeValue +
                    " while resolving  " + targetField);
        }
    }


//-----------------------------------------------------------------------
    private void resolveInstantFields() {
        // resolve parsed instant seconds to date and time if zone available
        if (fieldValues.containsKey(INSTANT_SECONDS)) {
            if (zone != null) {
                resolveInstantFields0(zone);
            } else {
                Long offsetSecs = fieldValues.get(OFFSET_SECONDS);
                if (offsetSecs != null) {
                    ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetSecs.intValue());
                    resolveInstantFields0(offset);
                }
            }
        }
    }

    private void resolveInstantFields0(ZoneId selectedZone) {
        Instant instant = Instant.ofEpochSecond(fieldValues.remove(INSTANT_SECONDS));
        ChronoZonedDateTime<?> zdt = chrono.zonedDateTime(instant, selectedZone);
        updateCheckConflict(zdt.toLocalDate());
        updateCheckConflict(INSTANT_SECONDS, SECOND_OF_DAY, (long) zdt.toLocalTime().toSecondOfDay());
    }

    //-----------------------------------------------------------------------
    private void resolveDateFields() {
        updateCheckConflict(chrono.resolveDate(fieldValues, resolverStyle));
    }

    private void updateCheckConflict(ChronoLocalDate cld) {
        if (date != null) {
            if (cld != null && date.equals(cld) == false) {
                throw new DateTimeException("Conflict found: Fields resolved to two different dates: " + date + " " + cld);
            }
        } else if (cld != null) {
            if (chrono.equals(cld.getChronology()) == false) {
                throw new DateTimeException("ChronoLocalDate must use the effective parsed chronology: " + chrono);
            }
            date = cld;
        }
    }

    //-----------------------------------------------------------------------
    private void resolveTimeFields() {
        // simplify fields
        if (fieldValues.containsKey(CLOCK_HOUR_OF_DAY)) {
            // lenient allows anything, smart allows 0-24, strict allows 1-24
            long ch = fieldValues.remove(CLOCK_HOUR_OF_DAY);
            if (resolverStyle == ResolverStyle.STRICT || (resolverStyle == ResolverStyle.SMART && ch != 0)) {
                CLOCK_HOUR_OF_DAY.checkValidValue(ch);
            }
            updateCheckConflict(CLOCK_HOUR_OF_DAY, HOUR_OF_DAY, ch == 24 ? 0 : ch);
        }
        if (fieldValues.containsKey(CLOCK_HOUR_OF_AMPM)) {
            // lenient allows anything, smart allows 0-12, strict allows 1-12
            long ch = fieldValues.remove(CLOCK_HOUR_OF_AMPM);
            if (resolverStyle == ResolverStyle.STRICT || (resolverStyle == ResolverStyle.SMART && ch != 0)) {
                CLOCK_HOUR_OF_AMPM.checkValidValue(ch);
            }
            updateCheckConflict(CLOCK_HOUR_OF_AMPM, HOUR_OF_AMPM, ch == 12 ? 0 : ch);
        }
        if (fieldValues.containsKey(AMPM_OF_DAY) && fieldValues.containsKey(HOUR_OF_AMPM)) {
            long ap = fieldValues.remove(AMPM_OF_DAY);
            long hap = fieldValues.remove(HOUR_OF_AMPM);
            if (resolverStyle == ResolverStyle.LENIENT) {
                updateCheckConflict(AMPM_OF_DAY, HOUR_OF_DAY, Math.addExact(Math.multiplyExact(ap, 12), hap));
            } else {  // STRICT or SMART
                AMPM_OF_DAY.checkValidValue(ap);
                HOUR_OF_AMPM.checkValidValue(hap);
                updateCheckConflict(AMPM_OF_DAY, HOUR_OF_DAY, ap * 12 + hap);
            }
        }
        if (fieldValues.containsKey(NANO_OF_DAY)) {
            long nod = fieldValues.remove(NANO_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
                NANO_OF_DAY.checkValidValue(nod);
            }
            updateCheckConflict(NANO_OF_DAY, HOUR_OF_DAY, nod / 3600_000_000_000L);
            updateCheckConflict(NANO_OF_DAY, MINUTE_OF_HOUR, (nod / 60_000_000_000L) % 60);
            updateCheckConflict(NANO_OF_DAY, SECOND_OF_MINUTE, (nod / 1_000_000_000L) % 60);
            updateCheckConflict(NANO_OF_DAY, NANO_OF_SECOND, nod % 1_000_000_000L);
        }
        if (fieldValues.containsKey(MICRO_OF_DAY)) {
            long cod = fieldValues.remove(MICRO_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
                MICRO_OF_DAY.checkValidValue(cod);
            }
            updateCheckConflict(MICRO_OF_DAY, SECOND_OF_DAY, cod / 1_000_000L);
            updateCheckConflict(MICRO_OF_DAY, MICRO_OF_SECOND, cod % 1_000_000L);
        }
        if (fieldValues.containsKey(MILLI_OF_DAY)) {
            long lod = fieldValues.remove(MILLI_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
                MILLI_OF_DAY.checkValidValue(lod);
            }
            updateCheckConflict(MILLI_OF_DAY, SECOND_OF_DAY, lod / 1_000);
            updateCheckConflict(MILLI_OF_DAY, MILLI_OF_SECOND, lod % 1_000);
        }
        if (fieldValues.containsKey(SECOND_OF_DAY)) {
            long sod = fieldValues.remove(SECOND_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
                SECOND_OF_DAY.checkValidValue(sod);
            }
            updateCheckConflict(SECOND_OF_DAY, HOUR_OF_DAY, sod / 3600);
            updateCheckConflict(SECOND_OF_DAY, MINUTE_OF_HOUR, (sod / 60) % 60);
            updateCheckConflict(SECOND_OF_DAY, SECOND_OF_MINUTE, sod % 60);
        }
        if (fieldValues.containsKey(MINUTE_OF_DAY)) {
            long mod = fieldValues.remove(MINUTE_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
                MINUTE_OF_DAY.checkValidValue(mod);
            }
            updateCheckConflict(MINUTE_OF_DAY, HOUR_OF_DAY, mod / 60);
            updateCheckConflict(MINUTE_OF_DAY, MINUTE_OF_HOUR, mod % 60);
        }

        // combine partial second fields strictly, leaving lenient expansion to later
        if (fieldValues.containsKey(NANO_OF_SECOND)) {
            long nos = fieldValues.get(NANO_OF_SECOND);
            if (resolverStyle != ResolverStyle.LENIENT) {
                NANO_OF_SECOND.checkValidValue(nos);
            }
            if (fieldValues.containsKey(MICRO_OF_SECOND)) {
                long cos = fieldValues.remove(MICRO_OF_SECOND);
                if (resolverStyle != ResolverStyle.LENIENT) {
                    MICRO_OF_SECOND.checkValidValue(cos);
                }
                nos = cos * 1000 + (nos % 1000);
                updateCheckConflict(MICRO_OF_SECOND, NANO_OF_SECOND, nos);
            }
            if (fieldValues.containsKey(MILLI_OF_SECOND)) {
                long los = fieldValues.remove(MILLI_OF_SECOND);
                if (resolverStyle != ResolverStyle.LENIENT) {
                    MILLI_OF_SECOND.checkValidValue(los);
                }
                updateCheckConflict(MILLI_OF_SECOND, NANO_OF_SECOND, los * 1_000_000L + (nos % 1_000_000L));
            }
        }

        if (dayPeriod != null && fieldValues.containsKey(HOUR_OF_AMPM)) {
            long hoap = fieldValues.remove(HOUR_OF_AMPM);
            if (resolverStyle != ResolverStyle.LENIENT) {
                HOUR_OF_AMPM.checkValidValue(hoap);
            }
            Long mohObj = fieldValues.get(MINUTE_OF_HOUR);
            long moh = mohObj != null ? Math.floorMod(mohObj, 60) : 0;
            long excessHours = dayPeriod.includes((Math.floorMod(hoap, 12) + 12) * 60 + moh) ? 12 : 0;
            long hod = Math.addExact(hoap, excessHours);
            updateCheckConflict(HOUR_OF_AMPM, HOUR_OF_DAY, hod);
            dayPeriod = null;
        }

        // convert to time if all four fields available (optimization)
        if (fieldValues.containsKey(HOUR_OF_DAY) && fieldValues.containsKey(MINUTE_OF_HOUR) &&
                fieldValues.containsKey(SECOND_OF_MINUTE) && fieldValues.containsKey(NANO_OF_SECOND)) {
            long hod = fieldValues.remove(HOUR_OF_DAY);
            long moh = fieldValues.remove(MINUTE_OF_HOUR);
            long som = fieldValues.remove(SECOND_OF_MINUTE);
            long nos = fieldValues.remove(NANO_OF_SECOND);
            resolveTime(hod, moh, som, nos);
        }
    }

    private void resolveTimeLenient() {
        // leniently create a time from incomplete information
        // done after everything else as it creates information from nothing
        // which would break updateCheckConflict(field)

        if (time == null) {
            // NANO_OF_SECOND merged with MILLI/MICRO above
            if (fieldValues.containsKey(MILLI_OF_SECOND)) {
                long los = fieldValues.remove(MILLI_OF_SECOND);
                if (fieldValues.containsKey(MICRO_OF_SECOND)) {
                    // merge milli-of-second and micro-of-second for better error message
                    long cos = los * 1_000 + (fieldValues.get(MICRO_OF_SECOND) % 1_000);
                    updateCheckConflict(MILLI_OF_SECOND, MICRO_OF_SECOND, cos);
                    fieldValues.remove(MICRO_OF_SECOND);
                    fieldValues.put(NANO_OF_SECOND, cos * 1_000L);
                } else {
                    // convert milli-of-second to nano-of-second
                    fieldValues.put(NANO_OF_SECOND, los * 1_000_000L);
                }
            } else if (fieldValues.containsKey(MICRO_OF_SECOND)) {
                // convert micro-of-second to nano-of-second
                long cos = fieldValues.remove(MICRO_OF_SECOND);
                fieldValues.put(NANO_OF_SECOND, cos * 1_000L);
            }

            // Set the hour-of-day, if not exist and not in STRICT, to the mid point of the day period or am/pm.
            if (!fieldValues.containsKey(HOUR_OF_DAY) &&
                    !fieldValues.containsKey(MINUTE_OF_HOUR) &&
                    !fieldValues.containsKey(SECOND_OF_MINUTE) &&
                    !fieldValues.containsKey(NANO_OF_SECOND) &&
                    resolverStyle != ResolverStyle.STRICT) {
                if (dayPeriod != null) {
                    long midpoint = dayPeriod.mid();
                    resolveTime(midpoint / 60, midpoint % 60, 0, 0);
                    dayPeriod = null;
                } else if (fieldValues.containsKey(AMPM_OF_DAY)) {
                    long ap = fieldValues.remove(AMPM_OF_DAY);
                    if (resolverStyle == ResolverStyle.LENIENT) {
                        resolveTime(Math.addExact(Math.multiplyExact(ap, 12), 6), 0, 0, 0);
                    } else {  // SMART
                        AMPM_OF_DAY.checkValidValue(ap);
                        resolveTime(ap * 12 + 6, 0, 0, 0);
                    }
                }
            }

            // merge hour/minute/second/nano leniently
            Long hod = fieldValues.get(HOUR_OF_DAY);
            if (hod != null) {
                Long moh = fieldValues.get(MINUTE_OF_HOUR);
                Long som = fieldValues.get(SECOND_OF_MINUTE);
                Long nos = fieldValues.get(NANO_OF_SECOND);

                // check for invalid combinations that cannot be defaulted
                if ((moh == null && (som != null || nos != null)) ||
                        (moh != null && som == null && nos != null)) {
                    return;
                }

                // default as necessary and build time
                long mohVal = (moh != null ? moh : 0);
                long somVal = (som != null ? som : 0);
                long nosVal = (nos != null ? nos : 0);

                if (dayPeriod != null && resolverStyle != ResolverStyle.LENIENT) {
                    // Check whether the hod/mohVal is within the day period
                    if (!dayPeriod.includes(hod * 60 + mohVal)) {
                        throw new DateTimeException("Conflict found: Resolved time %02d:%02d".formatted(hod, mohVal) +
                                " conflicts with " + dayPeriod);
                    }
                }

                resolveTime(hod, mohVal, somVal, nosVal);
                fieldValues.remove(HOUR_OF_DAY);
                fieldValues.remove(MINUTE_OF_HOUR);
                fieldValues.remove(SECOND_OF_MINUTE);
                fieldValues.remove(NANO_OF_SECOND);
            }
        }

        // validate remaining
        if (resolverStyle != ResolverStyle.LENIENT && fieldValues.size() > 0) {
            for (Entry<TemporalField, Long> entry : fieldValues.entrySet()) {
                TemporalField field = entry.getKey();
                if (field instanceof ChronoField && field.isTimeBased()) {
                    ((ChronoField) field).checkValidValue(entry.getValue());
                }
            }
        }
    }

    private void resolveTime(long hod, long moh, long som, long nos) {
        if (resolverStyle == ResolverStyle.LENIENT) {
            long totalNanos = Math.multiplyExact(hod, 3600_000_000_000L);
            totalNanos = Math.addExact(totalNanos, Math.multiplyExact(moh, 60_000_000_000L));
            totalNanos = Math.addExact(totalNanos, Math.multiplyExact(som, 1_000_000_000L));
            totalNanos = Math.addExact(totalNanos, nos);
            int excessDays = (int) Math.floorDiv(totalNanos, 86400_000_000_000L);  // safe int cast
            long nod = Math.floorMod(totalNanos, 86400_000_000_000L);
            updateCheckConflict(LocalTime.ofNanoOfDay(nod), Period.ofDays(excessDays));
        } else {  // STRICT or SMART
            int mohVal = MINUTE_OF_HOUR.checkValidIntValue(moh);
            int nosVal = NANO_OF_SECOND.checkValidIntValue(nos);
            // handle 24:00 end of day
            if (resolverStyle == ResolverStyle.SMART && hod == 24 && mohVal == 0 && som == 0 && nosVal == 0) {
                updateCheckConflict(LocalTime.MIDNIGHT, Period.ofDays(1));
            } else {
                int hodVal = HOUR_OF_DAY.checkValidIntValue(hod);
                int somVal = SECOND_OF_MINUTE.checkValidIntValue(som);
                updateCheckConflict(LocalTime.of(hodVal, mohVal, somVal, nosVal), Period.ZERO);
            }
        }
    }

    private void resolvePeriod() {
        // add whole days if we have both date and time
        if (date != null && time != null && excessDays.isZero() == false) {
            date = date.plus(excessDays);
            excessDays = Period.ZERO;
        }
    }

    private void resolveFractional() {
        // ensure fractional seconds available as ChronoField requires
        // resolveTimeLenient() will have merged MICRO_OF_SECOND/MILLI_OF_SECOND to NANO_OF_SECOND
        if (time == null &&
                (fieldValues.containsKey(INSTANT_SECONDS) ||
                    fieldValues.containsKey(SECOND_OF_DAY) ||
                    fieldValues.containsKey(SECOND_OF_MINUTE))) {
            if (fieldValues.containsKey(NANO_OF_SECOND)) {
                long nos = fieldValues.get(NANO_OF_SECOND);
                fieldValues.put(MICRO_OF_SECOND, nos / 1000);
                fieldValues.put(MILLI_OF_SECOND, nos / 1000000);
            } else {
                fieldValues.put(NANO_OF_SECOND, 0L);
                fieldValues.put(MICRO_OF_SECOND, 0L);
                fieldValues.put(MILLI_OF_SECOND, 0L);
            }
        }
    }

    private void resolveInstant() {
        // add instant seconds if we have date, time and zone
        // Offset (if present) will be given priority over the zone.
        if (date != null && time != null) {
            Long offsetSecs = fieldValues.get(OFFSET_SECONDS);
            if (offsetSecs != null) {
                ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetSecs.intValue());
                long instant = date.atTime(time).atZone(offset).toEpochSecond();
                fieldValues.put(INSTANT_SECONDS, instant);
            } else {
                if (zone != null) {
                    long instant = date.atTime(time).atZone(zone).toEpochSecond();
                    fieldValues.put(INSTANT_SECONDS, instant);
                }
            }
        }
    }

    private void updateCheckConflict(LocalTime timeToSet, Period periodToSet) {
        if (time != null) {
            if (time.equals(timeToSet) == false) {
                throw new DateTimeException("Conflict found: Fields resolved to different times: " + time + " " + timeToSet);
            }
            if (excessDays.isZero() == false && periodToSet.isZero() == false && excessDays.equals(periodToSet) == false) {
                throw new DateTimeException("Conflict found: Fields resolved to different excess periods: " + excessDays + " " + periodToSet);
            } else {
                excessDays = periodToSet;
            }
        } else {
            time = timeToSet;
            excessDays = periodToSet;
        }
    }

    //-----------------------------------------------------------------------
    private void crossCheck() {
        // only cross-check date, time and date-time
        // avoid object creation if possible
        if (date != null) {
            crossCheck(date);
        }
        if (time != null) {
            crossCheck(time);
            if (date != null && fieldValues.size() > 0) {
                crossCheck(date.atTime(time));
            }
        }
    }

    private void crossCheck(TemporalAccessor target) {
        for (Iterator<Entry<TemporalField, Long>> it = fieldValues.entrySet().iterator(); it.hasNext(); ) {
            Entry<TemporalField, Long> entry = it.next();
            TemporalField field = entry.getKey();
            if (target.isSupported(field)) {
                long val1;
                try {
                    val1 = target.getLong(field);
                } catch (RuntimeException ex) {
                    continue;
                }
                long val2 = entry.getValue();
                if (val1 != val2) {
                    throw new DateTimeException("Conflict found: Field " + field + " " + val1 +
                            " differs from " + field + " " + val2 + " derived from " + target);
                }
                it.remove();
            }
        }
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(64);
        buf.append(fieldValues).append(',').append(chrono);
        if (zone != null) {
            buf.append(',').append(zone);
        }
        if (date != null || time != null) {
            buf.append(" resolved to ");
            if (date != null) {
                buf.append(date);
                if (time != null) {
                    buf.append('T').append(time);
                }
            } else {
                buf.append(time);
            }
        }
        return buf.toString();
    }

}
