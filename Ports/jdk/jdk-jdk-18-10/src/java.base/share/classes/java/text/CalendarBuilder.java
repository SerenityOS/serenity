/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.text;

import java.util.Calendar;
import java.util.StringJoiner;
import static java.util.GregorianCalendar.*;

/**
 * {@code CalendarBuilder} keeps field-value pairs for setting
 * the calendar fields of the given {@code Calendar}. It has the
 * {@link Calendar#FIELD_COUNT FIELD_COUNT}-th field for the week year
 * support. Also {@code ISO_DAY_OF_WEEK} is used to specify
 * {@code DAY_OF_WEEK} in the ISO day of week numbering.
 *
 * <p>{@code CalendarBuilder} retains the semantic of the pseudo
 * timestamp for fields. {@code CalendarBuilder} uses a single
 * int array combining fields[] and stamp[] of {@code Calendar}.
 *
 * @author Masayoshi Okutsu
 */
class CalendarBuilder {
    /*
     * Pseudo time stamp constants used in java.util.Calendar
     */
    private static final int UNSET = 0;
    private static final int COMPUTED = 1;
    private static final int MINIMUM_USER_STAMP = 2;

    private static final int MAX_FIELD = FIELD_COUNT + 1;

    public static final int WEEK_YEAR = FIELD_COUNT;
    public static final int ISO_DAY_OF_WEEK = 1000; // pseudo field index

    // stamp[] (lower half) and field[] (upper half) combined
    private final int[] field;
    private int nextStamp;
    private int maxFieldIndex;

    CalendarBuilder() {
        field = new int[MAX_FIELD * 2];
        nextStamp = MINIMUM_USER_STAMP;
        maxFieldIndex = -1;
    }

    CalendarBuilder set(int index, int value) {
        if (index == ISO_DAY_OF_WEEK) {
            index = DAY_OF_WEEK;
            value = toCalendarDayOfWeek(value);
        }
        field[index] = nextStamp++;
        field[MAX_FIELD + index] = value;
        if (index > maxFieldIndex && index < FIELD_COUNT) {
            maxFieldIndex = index;
        }
        return this;
    }

    CalendarBuilder addYear(int value) {
        field[MAX_FIELD + YEAR] += value;
        field[MAX_FIELD + WEEK_YEAR] += value;
        return this;
    }

    boolean isSet(int index) {
        if (index == ISO_DAY_OF_WEEK) {
            index = DAY_OF_WEEK;
        }
        return field[index] > UNSET;
    }

    CalendarBuilder clear(int index) {
        if (index == ISO_DAY_OF_WEEK) {
            index = DAY_OF_WEEK;
        }
        field[index] = UNSET;
        field[MAX_FIELD + index] = 0;
        return this;
    }

    Calendar establish(Calendar cal) {
        boolean weekDate = isSet(WEEK_YEAR)
                            && field[WEEK_YEAR] > field[YEAR];
        if (weekDate && !cal.isWeekDateSupported()) {
            // Use YEAR instead
            if (!isSet(YEAR)) {
                set(YEAR, field[MAX_FIELD + WEEK_YEAR]);
            }
            weekDate = false;
        }

        cal.clear();
        // Set the fields from the min stamp to the max stamp so that
        // the field resolution works in the Calendar.
        for (int stamp = MINIMUM_USER_STAMP; stamp < nextStamp; stamp++) {
            for (int index = 0; index <= maxFieldIndex; index++) {
                if (field[index] == stamp) {
                    cal.set(index, field[MAX_FIELD + index]);
                    break;
                }
            }
        }

        if (weekDate) {
            int weekOfYear = isSet(WEEK_OF_YEAR) ? field[MAX_FIELD + WEEK_OF_YEAR] : 1;
            int dayOfWeek = isSet(DAY_OF_WEEK) ?
                                field[MAX_FIELD + DAY_OF_WEEK] : cal.getFirstDayOfWeek();
            if (!isValidDayOfWeek(dayOfWeek) && cal.isLenient()) {
                if (dayOfWeek >= 8) {
                    dayOfWeek--;
                    weekOfYear += dayOfWeek / 7;
                    dayOfWeek = (dayOfWeek % 7) + 1;
                } else {
                    while (dayOfWeek <= 0) {
                        dayOfWeek += 7;
                        weekOfYear--;
                    }
                }
                dayOfWeek = toCalendarDayOfWeek(dayOfWeek);
            }
            cal.setWeekDate(field[MAX_FIELD + WEEK_YEAR], weekOfYear, dayOfWeek);
        }
        return cal;
    }

    public String toString() {
        StringJoiner sj = new StringJoiner(",", "CalendarBuilder:[", "]");
        for (int i = 0; i < MAX_FIELD; i++) {
            if (isSet(i)) {
                sj.add(i + "=" + field[i] + ":" + field[MAX_FIELD + i]);
            }
        }
        return sj.toString();
    }

    static int toISODayOfWeek(int calendarDayOfWeek) {
        return calendarDayOfWeek == SUNDAY ? 7 : calendarDayOfWeek - 1;
    }

    static int toCalendarDayOfWeek(int isoDayOfWeek) {
        if (!isValidDayOfWeek(isoDayOfWeek)) {
            // adjust later for lenient mode
            return isoDayOfWeek;
        }
        return isoDayOfWeek == 7 ? SUNDAY : isoDayOfWeek + 1;
    }

    static boolean isValidDayOfWeek(int dayOfWeek) {
        return dayOfWeek > 0 && dayOfWeek <= 7;
    }
}
