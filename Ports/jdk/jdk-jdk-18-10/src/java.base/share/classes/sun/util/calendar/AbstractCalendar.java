/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.calendar;

import java.util.Locale;
import java.util.TimeZone;

/**
 * The <code>AbstractCalendar</code> class provides a framework for
 * implementing a concrete calendar system.
 *
 * <p><a name="fixed_date"></a><B>Fixed Date</B><br>
 *
 * For implementing a concrete calendar system, each calendar must
 * have the common date numbering, starting from midnight the onset of
 * Monday, January 1, 1 (Gregorian). It is called a <I>fixed date</I>
 * in this class. January 1, 1 (Gregorian) is fixed date 1. (See
 * Nachum Dershowitz and Edward M. Reingold, <I>CALENDRICAL
 * CALCULATION The Millennium Edition</I>, Section 1.2 for details.)
 *
 * @author Masayoshi Okutsu
 * @since 1.5
 */

public abstract class AbstractCalendar extends CalendarSystem {

    // The constants assume no leap seconds support.
    static final int SECOND_IN_MILLIS = 1000;
    static final int MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    static final int HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    static final int DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;

    // The number of days between January 1, 1 and January 1, 1970 (Gregorian)
    static final int EPOCH_OFFSET = 719163;

    private Era[] eras;

    protected AbstractCalendar() {
    }

    public Era getEra(String eraName) {
        if (eras != null) {
            for (Era era : eras) {
                if (era.getName().equals(eraName)) {
                    return era;
                }
            }
        }
        return null;
    }

    public Era[] getEras() {
        Era[] e = null;
        if (eras != null) {
            e = new Era[eras.length];
            System.arraycopy(eras, 0, e, 0, eras.length);
        }
        return e;
    }

    public void setEra(CalendarDate date, String eraName) {
        if (eras == null) {
            return; // should report an error???
        }
        for (int i = 0; i < eras.length; i++) {
            Era e = eras[i];
            if (e != null && e.getName().equals(eraName)) {
                date.setEra(e);
                return;
            }
        }
        throw new IllegalArgumentException("unknown era name: " + eraName);
    }

    protected void setEras(Era[] eras) {
        this.eras = eras;
    }

    public CalendarDate getCalendarDate() {
        return getCalendarDate(System.currentTimeMillis(), newCalendarDate());
    }

    public CalendarDate getCalendarDate(long millis) {
        return getCalendarDate(millis, newCalendarDate());
    }

    public CalendarDate getCalendarDate(long millis, TimeZone zone) {
        CalendarDate date = newCalendarDate(zone);
        return getCalendarDate(millis, date);
    }

    public CalendarDate getCalendarDate(long millis, CalendarDate date) {
        int ms = 0;             // time of day
        int zoneOffset = 0;
        int saving = 0;
        long days = 0;          // fixed date

        // adjust to local time if `date' has time zone.
        TimeZone zi = date.getZone();
        if (zi != null) {
            int[] offsets = new int[2];
            if (zi instanceof ZoneInfo) {
                zoneOffset = ((ZoneInfo)zi).getOffsets(millis, offsets);
            } else {
                zoneOffset = zi.getOffset(millis);
                offsets[0] = zi.getRawOffset();
                offsets[1] = zoneOffset - offsets[0];
            }

            // We need to calculate the given millis and time zone
            // offset separately for java.util.GregorianCalendar
            // compatibility. (i.e., millis + zoneOffset could cause
            // overflow or underflow, which must be avoided.) Usually
            // days should be 0 and ms is in the range of -13:00 to
            // +14:00. However, we need to deal with extreme cases.
            days = zoneOffset / DAY_IN_MILLIS;
            ms = zoneOffset % DAY_IN_MILLIS;
            saving = offsets[1];
        }
        date.setZoneOffset(zoneOffset);
        date.setDaylightSaving(saving);

        days += millis / DAY_IN_MILLIS;
        ms += (int) (millis % DAY_IN_MILLIS);
        if (ms >= DAY_IN_MILLIS) {
            // at most ms is (DAY_IN_MILLIS - 1) * 2.
            ms -= DAY_IN_MILLIS;
            ++days;
        } else {
            // at most ms is (1 - DAY_IN_MILLIS) * 2. Adding one
            // DAY_IN_MILLIS results in still negative.
            while (ms < 0) {
                ms += DAY_IN_MILLIS;
                --days;
            }
        }

        // convert to fixed date (offset from Jan. 1, 1 (Gregorian))
        days += EPOCH_OFFSET;

        // calculate date fields from the fixed date
        getCalendarDateFromFixedDate(date, days);

        // calculate time fields from the time of day
        setTimeOfDay(date, ms);
        date.setLeapYear(isLeapYear(date));
        date.setNormalized(true);
        return date;
    }

    public long getTime(CalendarDate date) {
        long gd = getFixedDate(date);
        long ms = (gd - EPOCH_OFFSET) * DAY_IN_MILLIS + getTimeOfDay(date);
        int zoneOffset = 0;
        TimeZone zi = date.getZone();
        if (zi != null) {
            if (date.isNormalized()) {
                return ms - date.getZoneOffset();
            }
            // adjust time zone and daylight saving
            int[] offsets = new int[2];
            if (date.isStandardTime()) {
                // 1) 2:30am during starting-DST transition is
                //    intrepreted as 2:30am ST
                // 2) 5:00pm during DST is still interpreted as 5:00pm ST
                // 3) 1:30am during ending-DST transition is interpreted
                //    as 1:30am ST (after transition)
                if (zi instanceof ZoneInfo) {
                    ((ZoneInfo)zi).getOffsetsByStandard(ms, offsets);
                    zoneOffset = offsets[0];
                } else {
                    zoneOffset = zi.getOffset(ms - zi.getRawOffset());
                }
            } else {
                // 1) 2:30am during starting-DST transition is
                //    intrepreted as 3:30am DT
                // 2) 5:00pm during DST is intrepreted as 5:00pm DT
                // 3) 1:30am during ending-DST transition is interpreted
                //    as 1:30am DT/0:30am ST (before transition)
                if (zi instanceof ZoneInfo) {
                    zoneOffset = ((ZoneInfo)zi).getOffsetsByWall(ms, offsets);
                } else {
                    zoneOffset = zi.getOffset(ms - zi.getRawOffset());
                }
            }
        }
        ms -= zoneOffset;
        getCalendarDate(ms, date);
        return ms;
    }

    protected long getTimeOfDay(CalendarDate date) {
        long fraction = date.getTimeOfDay();
        if (fraction != CalendarDate.TIME_UNDEFINED) {
            return fraction;
        }
        fraction = getTimeOfDayValue(date);
        date.setTimeOfDay(fraction);
        return fraction;
    }

    public long getTimeOfDayValue(CalendarDate date) {
        long fraction = date.getHours();
        fraction *= 60;
        fraction += date.getMinutes();
        fraction *= 60;
        fraction += date.getSeconds();
        fraction *= 1000;
        fraction += date.getMillis();
        return fraction;
    }

    public CalendarDate setTimeOfDay(CalendarDate cdate, int fraction) {
        if (fraction < 0) {
            throw new IllegalArgumentException();
        }
        boolean normalizedState = cdate.isNormalized();
        int time = fraction;
        int hours = time / HOUR_IN_MILLIS;
        time %= HOUR_IN_MILLIS;
        int minutes = time / MINUTE_IN_MILLIS;
        time %= MINUTE_IN_MILLIS;
        int seconds = time / SECOND_IN_MILLIS;
        time %= SECOND_IN_MILLIS;
        cdate.setHours(hours);
        cdate.setMinutes(minutes);
        cdate.setSeconds(seconds);
        cdate.setMillis(time);
        cdate.setTimeOfDay(fraction);
        if (hours < 24 && normalizedState) {
            // If this time of day setting doesn't affect the date,
            // then restore the normalized state.
            cdate.setNormalized(normalizedState);
        }
        return cdate;
    }

    /**
     * Returns 7 in this default implementation.
     *
     * @return 7
     */
    public int getWeekLength() {
        return 7;
    }

    protected abstract boolean isLeapYear(CalendarDate date);

    public CalendarDate getNthDayOfWeek(int nth, int dayOfWeek, CalendarDate date) {
        CalendarDate ndate = (CalendarDate) date.clone();
        normalize(ndate);
        long fd = getFixedDate(ndate);
        long nfd;
        if (nth > 0) {
            nfd = 7 * nth + getDayOfWeekDateBefore(fd, dayOfWeek);
        } else {
            nfd = 7 * nth + getDayOfWeekDateAfter(fd, dayOfWeek);
        }
        getCalendarDateFromFixedDate(ndate, nfd);
        return ndate;
    }

    /**
     * Returns a date of the given day of week before the given fixed
     * date.
     *
     * @param fixedDate the fixed date
     * @param dayOfWeek the day of week
     * @return the calculated date
     */
    static long getDayOfWeekDateBefore(long fixedDate, int dayOfWeek) {
        return getDayOfWeekDateOnOrBefore(fixedDate - 1, dayOfWeek);
    }

    /**
     * Returns a date of the given day of week that is closest to and
     * after the given fixed date.
     *
     * @param fixedDate the fixed date
     * @param dayOfWeek the day of week
     * @return the calculated date
     */
    static long getDayOfWeekDateAfter(long fixedDate, int dayOfWeek) {
        return getDayOfWeekDateOnOrBefore(fixedDate + 7, dayOfWeek);
    }

    /**
     * Returns a date of the given day of week on or before the given fixed
     * date.
     *
     * @param fixedDate the fixed date
     * @param dayOfWeek the day of week
     * @return the calculated date
     */
    // public for java.util.GregorianCalendar
    public static long getDayOfWeekDateOnOrBefore(long fixedDate, int dayOfWeek) {
        long fd = fixedDate - (dayOfWeek - 1);
        if (fd >= 0) {
            return fixedDate - (fd % 7);
        }
        return fixedDate - CalendarUtils.mod(fd, 7);
    }

    /**
     * Returns the fixed date calculated with the specified calendar
     * date. If the specified date is not normalized, its date fields
     * are normalized.
     *
     * @param date a <code>CalendarDate</code> with which the fixed
     * date is calculated
     * @return the calculated fixed date
     * @see AbstractCalendar.html#fixed_date
     */
    protected abstract long getFixedDate(CalendarDate date);

    /**
     * Calculates calendar fields from the specified fixed date. This
     * method stores the calculated calendar field values in the specified
     * <code>CalendarDate</code>.
     *
     * @param date a <code>CalendarDate</code> to stored the
     * calculated calendar fields.
     * @param fixedDate a fixed date to calculate calendar fields
     * @see AbstractCalendar.html#fixed_date
     */
    protected abstract void getCalendarDateFromFixedDate(CalendarDate date,
                                                         long fixedDate);

    public boolean validateTime(CalendarDate date) {
        int t = date.getHours();
        if (t < 0 || t >= 24) {
            return false;
        }
        t = date.getMinutes();
        if (t < 0 || t >= 60) {
            return false;
        }
        t = date.getSeconds();
        // TODO: Leap second support.
        if (t < 0 || t >= 60) {
            return false;
        }
        t = date.getMillis();
        if (t < 0 || t >= 1000) {
            return false;
        }
        return true;
    }


    int normalizeTime(CalendarDate date) {
        long fraction = getTimeOfDay(date);
        long days = 0;

        if (fraction >= DAY_IN_MILLIS) {
            days = fraction / DAY_IN_MILLIS;
            fraction %= DAY_IN_MILLIS;
        } else if (fraction < 0) {
            days = CalendarUtils.floorDivide(fraction, DAY_IN_MILLIS);
            if (days != 0) {
                fraction -= DAY_IN_MILLIS * days; // mod(fraction, DAY_IN_MILLIS)
            }
        }
        if (days != 0) {
            date.setTimeOfDay(fraction);
        }
        date.setMillis((int)(fraction % 1000));
        fraction /= 1000;
        date.setSeconds((int)(fraction % 60));
        fraction /= 60;
        date.setMinutes((int)(fraction % 60));
        date.setHours((int)(fraction / 60));
        return (int)days;
    }
}
