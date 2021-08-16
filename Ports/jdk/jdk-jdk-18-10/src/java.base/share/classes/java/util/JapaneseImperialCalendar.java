/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import sun.util.locale.provider.CalendarDataUtility;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;
import sun.util.calendar.Era;
import sun.util.calendar.Gregorian;
import sun.util.calendar.LocalGregorianCalendar;
import sun.util.calendar.ZoneInfo;

/**
 * {@code JapaneseImperialCalendar} implements a Japanese
 * calendar system in which the imperial era-based year numbering is
 * supported from the Meiji era. The following are the eras supported
 * by this calendar system.
 * <pre>{@code
 * ERA value   Era name    Since (in Gregorian)
 * ------------------------------------------------------
 *     0       N/A         N/A
 *     1       Meiji       1868-01-01T00:00:00 local time
 *     2       Taisho      1912-07-30T00:00:00 local time
 *     3       Showa       1926-12-25T00:00:00 local time
 *     4       Heisei      1989-01-08T00:00:00 local time
 *     5       Reiwa       2019-05-01T00:00:00 local time
 * ------------------------------------------------------
 * }</pre>
 *
 * <p>{@code ERA} value 0 specifies the years before Meiji and
 * the Gregorian year values are used. Unlike
 * {@link GregorianCalendar}, the Julian to Gregorian transition is not
 * supported because it doesn't make any sense to the Japanese
 * calendar systems used before Meiji. To represent the years before
 * Gregorian year 1, 0 and negative values are used. The Japanese
 * Imperial rescripts and government decrees don't specify how to deal
 * with time differences for applying the era transitions. This
 * calendar implementation assumes local time for all transitions.
 *
 * <p>A new era can be specified using property
 * jdk.calendar.japanese.supplemental.era. The new era is added to the
 * predefined eras. The syntax of the property is as follows.
 * <pre>
 *   {@code name=<name>,abbr=<abbr>,since=<time['u']>}
 * </pre>
 * where
 * <dl>
 * <dt>{@code <name>:}<dd>the full name of the new era (non-ASCII characters allowed,
 * either in platform's native encoding or in Unicode escape notation, {@code \\uXXXX})
 * <dt>{@code <abbr>:}<dd>the abbreviation of the new era (non-ASCII characters allowed,
 * either in platform's native encoding or in Unicode escape notation, {@code \\uXXXX})
 * <dt>{@code <time['u']>:}<dd>the start time of the new era represented by
 * milliseconds from 1970-01-01T00:00:00 local time or UTC if {@code 'u'} is
 * appended to the milliseconds value. (ASCII digits only)
 * </dl>
 *
 * <p>If the given era is invalid, such as the since value before the
 * beginning of the last predefined era, the given era will be
 * ignored.
 *
 * <p>The following is an example of the property usage.
 * <pre>
 *   java -Djdk.calendar.japanese.supplemental.era="name=NewEra,abbr=N,since=253374307200000"
 * </pre>
 * The property specifies an era change to NewEra at 9999-02-11T00:00:00 local time.
 *
 * @author Masayoshi Okutsu
 * @since 1.6
 */
class JapaneseImperialCalendar extends Calendar {
    /*
     * Implementation Notes
     *
     * This implementation uses
     * sun.util.calendar.LocalGregorianCalendar to perform most of the
     * calendar calculations.
     */

    /**
     * The ERA constant designating the era before Meiji.
     */
    public static final int BEFORE_MEIJI = 0;

    /**
     * The ERA constant designating the Meiji era.
     */
    public static final int MEIJI = 1;

    /**
     * The ERA constant designating the Taisho era.
     */
    public static final int TAISHO = 2;

    /**
     * The ERA constant designating the Showa era.
     */
    public static final int SHOWA = 3;

    /**
     * The ERA constant designating the Heisei era.
     */
    public static final int HEISEI = 4;

    /**
     * The ERA constant designating the Reiwa era.
     */
    private static final int REIWA = 5;

    private static final int EPOCH_OFFSET   = 719163; // Fixed date of January 1, 1970 (Gregorian)

    // Useful millisecond constants.  Although ONE_DAY and ONE_WEEK can fit
    // into ints, they must be longs in order to prevent arithmetic overflow
    // when performing (bug 4173516).
    private static final int  ONE_SECOND = 1000;
    private static final int  ONE_MINUTE = 60*ONE_SECOND;
    private static final int  ONE_HOUR   = 60*ONE_MINUTE;
    private static final long ONE_DAY    = 24*ONE_HOUR;

    // Reference to the sun.util.calendar.LocalGregorianCalendar instance (singleton).
    private static final LocalGregorianCalendar jcal
        = (LocalGregorianCalendar) CalendarSystem.forName("japanese");

    // Gregorian calendar instance. This is required because era
    // transition dates are given in Gregorian dates.
    private static final Gregorian gcal = CalendarSystem.getGregorianCalendar();

    // The Era instance representing "before Meiji".
    private static final Era BEFORE_MEIJI_ERA = new Era("BeforeMeiji", "BM", Long.MIN_VALUE, false);

    // Imperial eras. The sun.util.calendar.LocalGregorianCalendar
    // doesn't have an Era representing before Meiji, which is
    // inconvenient for a Calendar. So, era[0] is a reference to
    // BEFORE_MEIJI_ERA.
    private static final Era[] eras;

    // Fixed date of the first date of each era.
    private static final long[] sinceFixedDates;

    // The current era
    private static final int currentEra;

    /*
     * <pre>
     *                                 Greatest       Least
     * Field name             Minimum   Minimum     Maximum     Maximum
     * ----------             -------   -------     -------     -------
     * ERA                          0         0           1           1
     * YEAR                -292275055         1           ?           ?
     * MONTH                        0         0          11          11
     * WEEK_OF_YEAR                 1         1          52*         53
     * WEEK_OF_MONTH                0         0           4*          6
     * DAY_OF_MONTH                 1         1          28*         31
     * DAY_OF_YEAR                  1         1         365*        366
     * DAY_OF_WEEK                  1         1           7           7
     * DAY_OF_WEEK_IN_MONTH        -1        -1           4*          6
     * AM_PM                        0         0           1           1
     * HOUR                         0         0          11          11
     * HOUR_OF_DAY                  0         0          23          23
     * MINUTE                       0         0          59          59
     * SECOND                       0         0          59          59
     * MILLISECOND                  0         0         999         999
     * ZONE_OFFSET             -13:00    -13:00       14:00       14:00
     * DST_OFFSET                0:00      0:00        0:20        2:00
     * </pre>
     * *: depends on eras
     */
    static final int MIN_VALUES[] = {
        0,              // ERA
        -292275055,     // YEAR
        JANUARY,        // MONTH
        1,              // WEEK_OF_YEAR
        0,              // WEEK_OF_MONTH
        1,              // DAY_OF_MONTH
        1,              // DAY_OF_YEAR
        SUNDAY,         // DAY_OF_WEEK
        1,              // DAY_OF_WEEK_IN_MONTH
        AM,             // AM_PM
        0,              // HOUR
        0,              // HOUR_OF_DAY
        0,              // MINUTE
        0,              // SECOND
        0,              // MILLISECOND
        -13*ONE_HOUR,   // ZONE_OFFSET (UNIX compatibility)
        0               // DST_OFFSET
    };
    static final int LEAST_MAX_VALUES[] = {
        0,              // ERA (initialized later)
        0,              // YEAR (initialized later)
        JANUARY,        // MONTH (Showa 64 ended in January.)
        0,              // WEEK_OF_YEAR (Showa 1 has only 6 days which could be 0 weeks.)
        4,              // WEEK_OF_MONTH
        28,             // DAY_OF_MONTH
        0,              // DAY_OF_YEAR (initialized later)
        SATURDAY,       // DAY_OF_WEEK
        4,              // DAY_OF_WEEK_IN
        PM,             // AM_PM
        11,             // HOUR
        23,             // HOUR_OF_DAY
        59,             // MINUTE
        59,             // SECOND
        999,            // MILLISECOND
        14*ONE_HOUR,    // ZONE_OFFSET
        20*ONE_MINUTE   // DST_OFFSET (historical least maximum)
    };
    static final int MAX_VALUES[] = {
        0,              // ERA
        292278994,      // YEAR
        DECEMBER,       // MONTH
        53,             // WEEK_OF_YEAR
        6,              // WEEK_OF_MONTH
        31,             // DAY_OF_MONTH
        366,            // DAY_OF_YEAR
        SATURDAY,       // DAY_OF_WEEK
        6,              // DAY_OF_WEEK_IN
        PM,             // AM_PM
        11,             // HOUR
        23,             // HOUR_OF_DAY
        59,             // MINUTE
        59,             // SECOND
        999,            // MILLISECOND
        14*ONE_HOUR,    // ZONE_OFFSET
        2*ONE_HOUR      // DST_OFFSET (double summer time)
    };

    // Proclaim serialization compatibility with JDK 1.6
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    @java.io.Serial
    private static final long serialVersionUID = -3364572813905467929L;

    static {
        Era[] es = jcal.getEras();
        int length = es.length + 1;
        eras = new Era[length];
        sinceFixedDates = new long[length];

        // eras[BEFORE_MEIJI] and sinceFixedDate[BEFORE_MEIJI] are the
        // same as Gregorian.
        int index = BEFORE_MEIJI;
        int current = index;
        sinceFixedDates[index] = gcal.getFixedDate(BEFORE_MEIJI_ERA.getSinceDate());
        eras[index++] = BEFORE_MEIJI_ERA;
        for (Era e : es) {
            if(e.getSince(TimeZone.NO_TIMEZONE) < System.currentTimeMillis()) {
                current = index;
            }
            CalendarDate d = e.getSinceDate();
            sinceFixedDates[index] = gcal.getFixedDate(d);
            eras[index++] = e;
        }
        currentEra = current;

        LEAST_MAX_VALUES[ERA] = MAX_VALUES[ERA] = eras.length - 1;

        // Calculate the least maximum year and least day of Year
        // values. The following code assumes that there's at most one
        // era transition in a Gregorian year.
        int year = Integer.MAX_VALUE;
        int dayOfYear = Integer.MAX_VALUE;
        CalendarDate date = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        for (int i = 1; i < eras.length; i++) {
            long fd = sinceFixedDates[i];
            CalendarDate transitionDate = eras[i].getSinceDate();
            date.setDate(transitionDate.getYear(), BaseCalendar.JANUARY, 1);
            long fdd = gcal.getFixedDate(date);
            if (fd != fdd) {
                dayOfYear = Math.min((int)(fd - fdd) + 1, dayOfYear);
            }
            date.setDate(transitionDate.getYear(), BaseCalendar.DECEMBER, 31);
            fdd = gcal.getFixedDate(date);
            if (fd != fdd) {
                dayOfYear = Math.min((int)(fdd - fd) + 1, dayOfYear);
            }
            LocalGregorianCalendar.Date lgd = getCalendarDate(fd - 1);
            int y = lgd.getYear();
            // Unless the first year starts from January 1, the actual
            // max value could be one year short. For example, if it's
            // Showa 63 January 8, 63 is the actual max value since
            // Showa 64 January 8 doesn't exist.
            if (!(lgd.getMonth() == BaseCalendar.JANUARY && lgd.getDayOfMonth() == 1)) {
                y--;
            }
            year = Math.min(y, year);
        }
        LEAST_MAX_VALUES[YEAR] = year; // Max year could be smaller than this value.
        LEAST_MAX_VALUES[DAY_OF_YEAR] = dayOfYear;
    }

    /**
     * jdate always has a sun.util.calendar.LocalGregorianCalendar.Date instance to
     * avoid overhead of creating it for each calculation.
     */
    private transient LocalGregorianCalendar.Date jdate;

    /**
     * Temporary int[2] to get time zone offsets. zoneOffsets[0] gets
     * the GMT offset value and zoneOffsets[1] gets the daylight saving
     * value.
     */
    private transient int[] zoneOffsets;

    /**
     * Temporary storage for saving original fields[] values in
     * non-lenient mode.
     */
    private transient int[] originalFields;

    /**
     * Constructs a {@code JapaneseImperialCalendar} based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     * @param aLocale the given locale.
     */
    JapaneseImperialCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        jdate = jcal.newCalendarDate(zone);
        setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Constructs an "empty" {@code JapaneseImperialCalendar}.
     *
     * @param zone    the given time zone
     * @param aLocale the given locale
     * @param flag    the flag requesting an empty instance
     */
    JapaneseImperialCalendar(TimeZone zone, Locale aLocale, boolean flag) {
        super(zone, aLocale);
        jdate = jcal.newCalendarDate(zone);
    }

    /**
     * Returns {@code "japanese"} as the calendar type of this {@code
     * JapaneseImperialCalendar}.
     *
     * @return {@code "japanese"}
     */
    @Override
    public String getCalendarType() {
        return "japanese";
    }

    /**
     * Compares this {@code JapaneseImperialCalendar} to the specified
     * {@code Object}. The result is {@code true} if and
     * only if the argument is a {@code JapaneseImperialCalendar} object
     * that represents the same time value (millisecond offset from
     * the <a href="Calendar.html#Epoch">Epoch</a>) under the same
     * {@code Calendar} parameters.
     *
     * @param obj the object to compare with.
     * @return {@code true} if this object is equal to {@code obj};
     * {@code false} otherwise.
     * @see Calendar#compareTo(Calendar)
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof JapaneseImperialCalendar &&
            super.equals(obj);
    }

    /**
     * Generates the hash code for this
     * {@code JapaneseImperialCalendar} object.
     */
    @Override
    public int hashCode() {
        return super.hashCode() ^ jdate.hashCode();
    }

    /**
     * Adds the specified (signed) amount of time to the given calendar field,
     * based on the calendar's rules.
     *
     * <p><em>Add rule 1</em>. The value of {@code field}
     * after the call minus the value of {@code field} before the
     * call is {@code amount}, modulo any overflow that has occurred in
     * {@code field}. Overflow occurs when a field value exceeds its
     * range and, as a result, the next larger field is incremented or
     * decremented and the field value is adjusted back into its range.</p>
     *
     * <p><em>Add rule 2</em>. If a smaller field is expected to be
     * invariant, but it is impossible for it to be equal to its
     * prior value because of changes in its minimum or maximum after
     * {@code field} is changed, then its value is adjusted to be as close
     * as possible to its expected value. A smaller field represents a
     * smaller unit of time. {@code HOUR} is a smaller field than
     * {@code DAY_OF_MONTH}. No adjustment is made to smaller fields
     * that are not expected to be invariant. The calendar system
     * determines what fields are expected to be invariant.</p>
     *
     * @param field the calendar field.
     * @param amount the amount of date or time to be added to the field.
     * @throws    IllegalArgumentException if {@code field} is
     * {@code ZONE_OFFSET}, {@code DST_OFFSET}, or unknown,
     * or if any calendar fields have out-of-range values in
     * non-lenient mode.
     */
    @Override
    public void add(int field, int amount) {
        // If amount == 0, do nothing even the given field is out of
        // range. This is tested by JCK.
        if (amount == 0) {
            return;   // Do nothing!
        }

        if (field < 0 || field >= ZONE_OFFSET) {
            throw new IllegalArgumentException();
        }

        // Sync the time and calendar fields.
        complete();

        if (field == YEAR) {
            LocalGregorianCalendar.Date d = (LocalGregorianCalendar.Date) jdate.clone();
            d.addYear(amount);
            pinDayOfMonth(d);
            set(ERA, getEraIndex(d));
            set(YEAR, d.getYear());
            set(MONTH, d.getMonth() - 1);
            set(DAY_OF_MONTH, d.getDayOfMonth());
        } else if (field == MONTH) {
            LocalGregorianCalendar.Date d = (LocalGregorianCalendar.Date) jdate.clone();
            d.addMonth(amount);
            pinDayOfMonth(d);
            set(ERA, getEraIndex(d));
            set(YEAR, d.getYear());
            set(MONTH, d.getMonth() - 1);
            set(DAY_OF_MONTH, d.getDayOfMonth());
        } else if (field == ERA) {
            int era = internalGet(ERA) + amount;
            if (era < 0) {
                era = 0;
            } else if (era > eras.length - 1) {
                era = eras.length - 1;
            }
            set(ERA, era);
        } else {
            long delta = amount;
            long timeOfDay = 0;
            switch (field) {
            // Handle the time fields here. Convert the given
            // amount to milliseconds and call setTimeInMillis.
            case HOUR:
            case HOUR_OF_DAY:
                delta *= 60 * 60 * 1000;        // hours to milliseconds
                break;

            case MINUTE:
                delta *= 60 * 1000;             // minutes to milliseconds
                break;

            case SECOND:
                delta *= 1000;                  // seconds to milliseconds
                break;

            case MILLISECOND:
                break;

            // Handle week, day and AM_PM fields which involves
            // time zone offset change adjustment. Convert the
            // given amount to the number of days.
            case WEEK_OF_YEAR:
            case WEEK_OF_MONTH:
            case DAY_OF_WEEK_IN_MONTH:
                delta *= 7;
                break;

            case DAY_OF_MONTH: // synonym of DATE
            case DAY_OF_YEAR:
            case DAY_OF_WEEK:
                break;

            case AM_PM:
                // Convert the amount to the number of days (delta)
                // and +12 or -12 hours (timeOfDay).
                delta = amount / 2;
                timeOfDay = 12 * (amount % 2);
                break;
            }

            // The time fields don't require time zone offset change
            // adjustment.
            if (field >= HOUR) {
                setTimeInMillis(time + delta);
                return;
            }

            // The rest of the fields (week, day or AM_PM fields)
            // require time zone offset (both GMT and DST) change
            // adjustment.

            // Translate the current time to the fixed date and time
            // of the day.
            long fd = cachedFixedDate;
            timeOfDay += internalGet(HOUR_OF_DAY);
            timeOfDay *= 60;
            timeOfDay += internalGet(MINUTE);
            timeOfDay *= 60;
            timeOfDay += internalGet(SECOND);
            timeOfDay *= 1000;
            timeOfDay += internalGet(MILLISECOND);
            if (timeOfDay >= ONE_DAY) {
                fd++;
                timeOfDay -= ONE_DAY;
            } else if (timeOfDay < 0) {
                fd--;
                timeOfDay += ONE_DAY;
            }

            fd += delta; // fd is the expected fixed date after the calculation
            int zoneOffset = internalGet(ZONE_OFFSET) + internalGet(DST_OFFSET);
            setTimeInMillis((fd - EPOCH_OFFSET) * ONE_DAY + timeOfDay - zoneOffset);
            zoneOffset -= internalGet(ZONE_OFFSET) + internalGet(DST_OFFSET);
            // If the time zone offset has changed, then adjust the difference.
            if (zoneOffset != 0) {
                setTimeInMillis(time + zoneOffset);
                long fd2 = cachedFixedDate;
                // If the adjustment has changed the date, then take
                // the previous one.
                if (fd2 != fd) {
                    setTimeInMillis(time - zoneOffset);
                }
            }
        }
    }

    @Override
    public void roll(int field, boolean up) {
        roll(field, up ? +1 : -1);
    }

    /**
     * Adds a signed amount to the specified calendar field without changing larger fields.
     * A negative roll amount means to subtract from field without changing
     * larger fields. If the specified amount is 0, this method performs nothing.
     *
     * <p>This method calls {@link #complete()} before adding the
     * amount so that all the calendar fields are normalized. If there
     * is any calendar field having an out-of-range value in non-lenient mode, then an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param field the calendar field.
     * @param amount the signed amount to add to {@code field}.
     * @throws    IllegalArgumentException if {@code field} is
     * {@code ZONE_OFFSET}, {@code DST_OFFSET}, or unknown,
     * or if any calendar fields have out-of-range values in
     * non-lenient mode.
     * @see #roll(int,boolean)
     * @see #add(int,int)
     * @see #set(int,int)
     */
    @Override
    public void roll(int field, int amount) {
        // If amount == 0, do nothing even the given field is out of
        // range. This is tested by JCK.
        if (amount == 0) {
            return;
        }

        if (field < 0 || field >= ZONE_OFFSET) {
            throw new IllegalArgumentException();
        }

        // Sync the time and calendar fields.
        complete();

        int min = getMinimum(field);
        int max = getMaximum(field);

        switch (field) {
        case ERA:
        case AM_PM:
        case MINUTE:
        case SECOND:
        case MILLISECOND:
            // These fields are handled simply, since they have fixed
            // minima and maxima. Other fields are complicated, since
            // the range within they must roll varies depending on the
            // date, a time zone and the era transitions.
            break;

        case HOUR:
        case HOUR_OF_DAY:
            {
                int unit = max + 1; // 12 or 24 hours
                int h = internalGet(field);
                int nh = (h + amount) % unit;
                if (nh < 0) {
                    nh += unit;
                }
                time += ONE_HOUR * (nh - h);

                // The day might have changed, which could happen if
                // the daylight saving time transition brings it to
                // the next day, although it's very unlikely. But we
                // have to make sure not to change the larger fields.
                CalendarDate d = jcal.getCalendarDate(time, getZone());
                if (internalGet(DAY_OF_MONTH) != d.getDayOfMonth()) {
                    d.setEra(jdate.getEra());
                    d.setDate(internalGet(YEAR),
                              internalGet(MONTH) + 1,
                              internalGet(DAY_OF_MONTH));
                    if (field == HOUR) {
                        assert (internalGet(AM_PM) == PM);
                        d.addHours(+12); // restore PM
                    }
                    time = jcal.getTime(d);
                }
                int hourOfDay = d.getHours();
                internalSet(field, hourOfDay % unit);
                if (field == HOUR) {
                    internalSet(HOUR_OF_DAY, hourOfDay);
                } else {
                    internalSet(AM_PM, hourOfDay / 12);
                    internalSet(HOUR, hourOfDay % 12);
                }

                // Time zone offset and/or daylight saving might have changed.
                int zoneOffset = d.getZoneOffset();
                int saving = d.getDaylightSaving();
                internalSet(ZONE_OFFSET, zoneOffset - saving);
                internalSet(DST_OFFSET, saving);
                return;
            }

        case YEAR:
            min = getActualMinimum(field);
            max = getActualMaximum(field);
            break;

        case MONTH:
            // Rolling the month involves both pinning the final value to [0, 11]
            // and adjusting the DAY_OF_MONTH if necessary.  We only adjust the
            // DAY_OF_MONTH if, after updating the MONTH field, it is illegal.
            // E.g., <jan31>.roll(MONTH, 1) -> <feb28> or <feb29>.
            {
                if (!isTransitionYear(jdate.getNormalizedYear())) {
                    int year = jdate.getYear();
                    if (year == getMaximum(YEAR)) {
                        CalendarDate jd = jcal.getCalendarDate(time, getZone());
                        CalendarDate d = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                        max = d.getMonth() - 1;
                        int n = getRolledValue(internalGet(field), amount, min, max);
                        if (n == max) {
                            // To avoid overflow, use an equivalent year.
                            jd.addYear(-400);
                            jd.setMonth(n + 1);
                            if (jd.getDayOfMonth() > d.getDayOfMonth()) {
                                jd.setDayOfMonth(d.getDayOfMonth());
                                jcal.normalize(jd);
                            }
                            if (jd.getDayOfMonth() == d.getDayOfMonth()
                                && jd.getTimeOfDay() > d.getTimeOfDay()) {
                                jd.setMonth(n + 1);
                                jd.setDayOfMonth(d.getDayOfMonth() - 1);
                                jcal.normalize(jd);
                                // Month may have changed by the normalization.
                                n = jd.getMonth() - 1;
                            }
                            set(DAY_OF_MONTH, jd.getDayOfMonth());
                        }
                        set(MONTH, n);
                    } else if (year == getMinimum(YEAR)) {
                        CalendarDate jd = jcal.getCalendarDate(time, getZone());
                        CalendarDate d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                        min = d.getMonth() - 1;
                        int n = getRolledValue(internalGet(field), amount, min, max);
                        if (n == min) {
                            // To avoid underflow, use an equivalent year.
                            jd.addYear(+400);
                            jd.setMonth(n + 1);
                            if (jd.getDayOfMonth() < d.getDayOfMonth()) {
                                jd.setDayOfMonth(d.getDayOfMonth());
                                jcal.normalize(jd);
                            }
                            if (jd.getDayOfMonth() == d.getDayOfMonth()
                                && jd.getTimeOfDay() < d.getTimeOfDay()) {
                                jd.setMonth(n + 1);
                                jd.setDayOfMonth(d.getDayOfMonth() + 1);
                                jcal.normalize(jd);
                                // Month may have changed by the normalization.
                                n = jd.getMonth() - 1;
                            }
                            set(DAY_OF_MONTH, jd.getDayOfMonth());
                        }
                        set(MONTH, n);
                    } else {
                        int mon = (internalGet(MONTH) + amount) % 12;
                        if (mon < 0) {
                            mon += 12;
                        }
                        set(MONTH, mon);

                        // Keep the day of month in the range.  We
                        // don't want to spill over into the next
                        // month; e.g., we don't want jan31 + 1 mo ->
                        // feb31 -> mar3.
                        int monthLen = monthLength(mon);
                        if (internalGet(DAY_OF_MONTH) > monthLen) {
                            set(DAY_OF_MONTH, monthLen);
                        }
                    }
                } else {
                    int eraIndex = getEraIndex(jdate);
                    CalendarDate transition = null;
                    if (jdate.getYear() == 1) {
                        transition = eras[eraIndex].getSinceDate();
                        min = transition.getMonth() - 1;
                    } else {
                        if (eraIndex < eras.length - 1) {
                            transition = eras[eraIndex + 1].getSinceDate();
                            if (transition.getYear() == jdate.getNormalizedYear()) {
                                max = transition.getMonth() - 1;
                                if (transition.getDayOfMonth() == 1) {
                                    max--;
                                }
                            }
                        }
                    }

                    if (min == max) {
                        // The year has only one month. No need to
                        // process further. (Showa Gan-nen (year 1)
                        // and the last year have only one month.)
                        return;
                    }
                    int n = getRolledValue(internalGet(field), amount, min, max);
                    set(MONTH, n);
                    if (n == min) {
                        if (!(transition.getMonth() == BaseCalendar.JANUARY
                              && transition.getDayOfMonth() == 1)) {
                            if (jdate.getDayOfMonth() < transition.getDayOfMonth()) {
                                set(DAY_OF_MONTH, transition.getDayOfMonth());
                            }
                        }
                    } else if (n == max && (transition.getMonth() - 1 == n)) {
                        int dom = transition.getDayOfMonth();
                        if (jdate.getDayOfMonth() >= dom) {
                            set(DAY_OF_MONTH, dom - 1);
                        }
                    }
                }
                return;
            }

        case WEEK_OF_YEAR:
            {
                int y = jdate.getNormalizedYear();
                max = getActualMaximum(WEEK_OF_YEAR);
                set(DAY_OF_WEEK, internalGet(DAY_OF_WEEK)); // update stamp[field]
                int woy = internalGet(WEEK_OF_YEAR);
                int value = woy + amount;
                if (!isTransitionYear(jdate.getNormalizedYear())) {
                    int year = jdate.getYear();
                    if (year == getMaximum(YEAR)) {
                        max = getActualMaximum(WEEK_OF_YEAR);
                    } else if (year == getMinimum(YEAR)) {
                        min = getActualMinimum(WEEK_OF_YEAR);
                        max = getActualMaximum(WEEK_OF_YEAR);
                        if (value > min && value < max) {
                            set(WEEK_OF_YEAR, value);
                            return;
                        }

                    }
                    // If the new value is in between min and max
                    // (exclusive), then we can use the value.
                    if (value > min && value < max) {
                        set(WEEK_OF_YEAR, value);
                        return;
                    }
                    long fd = cachedFixedDate;
                    // Make sure that the min week has the current DAY_OF_WEEK
                    long day1 = fd - (7 * (woy - min));
                    if (year != getMinimum(YEAR)) {
                        if (gcal.getYearFromFixedDate(day1) != y) {
                            min++;
                        }
                    } else {
                        CalendarDate d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                        if (day1 < jcal.getFixedDate(d)) {
                            min++;
                        }
                    }

                    // Make sure the same thing for the max week
                    fd += 7 * (max - internalGet(WEEK_OF_YEAR));
                    if (gcal.getYearFromFixedDate(fd) != y) {
                        max--;
                    }
                    break;
                }

                // Handle transition here.
                long fd = cachedFixedDate;
                long day1 = fd - (7 * (woy - min));
                // Make sure that the min week has the current DAY_OF_WEEK
                LocalGregorianCalendar.Date d = getCalendarDate(day1);
                if (!(d.getEra() == jdate.getEra() && d.getYear() == jdate.getYear())) {
                    min++;
                }

                // Make sure the same thing for the max week
                fd += 7 * (max - woy);
                jcal.getCalendarDateFromFixedDate(d, fd);
                if (!(d.getEra() == jdate.getEra() && d.getYear() == jdate.getYear())) {
                    max--;
                }
                // value: the new WEEK_OF_YEAR which must be converted
                // to month and day of month.
                value = getRolledValue(woy, amount, min, max) - 1;
                d = getCalendarDate(day1 + value * 7);
                set(MONTH, d.getMonth() - 1);
                set(DAY_OF_MONTH, d.getDayOfMonth());
                return;
            }

        case WEEK_OF_MONTH:
            {
                boolean isTransitionYear = isTransitionYear(jdate.getNormalizedYear());
                // dow: relative day of week from the first day of week
                int dow = internalGet(DAY_OF_WEEK) - getFirstDayOfWeek();
                if (dow < 0) {
                    dow += 7;
                }

                long fd = cachedFixedDate;
                long month1;     // fixed date of the first day (usually 1) of the month
                int monthLength; // actual month length
                if (isTransitionYear) {
                    month1 = getFixedDateMonth1(jdate, fd);
                    monthLength = actualMonthLength();
                } else {
                    month1 = fd - internalGet(DAY_OF_MONTH) + 1;
                    monthLength = jcal.getMonthLength(jdate);
                }

                // the first day of week of the month.
                long monthDay1st = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(month1 + 6,
                                                                                     getFirstDayOfWeek());
                // if the week has enough days to form a week, the
                // week starts from the previous month.
                if ((int)(monthDay1st - month1) >= getMinimalDaysInFirstWeek()) {
                    monthDay1st -= 7;
                }
                max = getActualMaximum(field);

                // value: the new WEEK_OF_MONTH value
                int value = getRolledValue(internalGet(field), amount, 1, max) - 1;

                // nfd: fixed date of the rolled date
                long nfd = monthDay1st + value * 7 + dow;

                // Unlike WEEK_OF_YEAR, we need to change day of week if the
                // nfd is out of the month.
                if (nfd < month1) {
                    nfd = month1;
                } else if (nfd >= (month1 + monthLength)) {
                    nfd = month1 + monthLength - 1;
                }
                set(DAY_OF_MONTH, getCalendarDate(nfd).getDayOfMonth());
                return;
            }

        case DAY_OF_MONTH:
            {
                if (!isTransitionYear(jdate.getNormalizedYear())) {
                    max = jcal.getMonthLength(jdate);
                    break;
                }

                // TODO: Need to change the spec to be usable DAY_OF_MONTH rolling...

                // Transition handling. We can't change year and era
                // values here due to the Calendar roll spec!
                long month1 = getFixedDateMonth1(jdate, cachedFixedDate);

                // It may not be a regular month. Convert the date and range to
                // the relative values, perform the roll, and
                // convert the result back to the rolled date.
                int value = getRolledValue((int)(cachedFixedDate - month1), amount,
                                           0, actualMonthLength() - 1);
                LocalGregorianCalendar.Date d = getCalendarDate(month1 + value);
                assert getEraIndex(d) == internalGetEra()
                    && d.getYear() == internalGet(YEAR) && d.getMonth()-1 == internalGet(MONTH);
                set(DAY_OF_MONTH, d.getDayOfMonth());
                return;
            }

        case DAY_OF_YEAR:
            {
                max = getActualMaximum(field);
                if (!isTransitionYear(jdate.getNormalizedYear())) {
                    break;
                }

                // Handle transition. We can't change year and era values
                // here due to the Calendar roll spec.
                int value = getRolledValue(internalGet(DAY_OF_YEAR), amount, min, max);
                long jan0 = cachedFixedDate - internalGet(DAY_OF_YEAR);
                LocalGregorianCalendar.Date d = getCalendarDate(jan0 + value);
                assert getEraIndex(d) == internalGetEra() && d.getYear() == internalGet(YEAR);
                set(MONTH, d.getMonth() - 1);
                set(DAY_OF_MONTH, d.getDayOfMonth());
                return;
            }

        case DAY_OF_WEEK:
            {
                int normalizedYear = jdate.getNormalizedYear();
                if (!isTransitionYear(normalizedYear) && !isTransitionYear(normalizedYear - 1)) {
                    // If the week of year is in the same year, we can
                    // just change DAY_OF_WEEK.
                    int weekOfYear = internalGet(WEEK_OF_YEAR);
                    if (weekOfYear > 1 && weekOfYear < 52) {
                        set(WEEK_OF_YEAR, internalGet(WEEK_OF_YEAR));
                        max = SATURDAY;
                        break;
                    }
                }

                // We need to handle it in a different way around year
                // boundaries and in the transition year. Note that
                // changing era and year values violates the roll
                // rule: not changing larger calendar fields...
                amount %= 7;
                if (amount == 0) {
                    return;
                }
                long fd = cachedFixedDate;
                long dowFirst = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(fd, getFirstDayOfWeek());
                fd += amount;
                if (fd < dowFirst) {
                    fd += 7;
                } else if (fd >= dowFirst + 7) {
                    fd -= 7;
                }
                LocalGregorianCalendar.Date d = getCalendarDate(fd);
                set(ERA, getEraIndex(d));
                set(d.getYear(), d.getMonth() - 1, d.getDayOfMonth());
                return;
            }

        case DAY_OF_WEEK_IN_MONTH:
            {
                min = 1; // after having normalized, min should be 1.
                if (!isTransitionYear(jdate.getNormalizedYear())) {
                    int dom = internalGet(DAY_OF_MONTH);
                    int monthLength = jcal.getMonthLength(jdate);
                    int lastDays = monthLength % 7;
                    max = monthLength / 7;
                    int x = (dom - 1) % 7;
                    if (x < lastDays) {
                        max++;
                    }
                    set(DAY_OF_WEEK, internalGet(DAY_OF_WEEK));
                    break;
                }

                // Transition year handling.
                long fd = cachedFixedDate;
                long month1 = getFixedDateMonth1(jdate, fd);
                int monthLength = actualMonthLength();
                int lastDays = monthLength % 7;
                max = monthLength / 7;
                int x = (int)(fd - month1) % 7;
                if (x < lastDays) {
                    max++;
                }
                int value = getRolledValue(internalGet(field), amount, min, max) - 1;
                fd = month1 + value * 7 + x;
                LocalGregorianCalendar.Date d = getCalendarDate(fd);
                set(DAY_OF_MONTH, d.getDayOfMonth());
                return;
            }
        }

        set(field, getRolledValue(internalGet(field), amount, min, max));
    }

    @Override
    public String getDisplayName(int field, int style, Locale locale) {
        if (!checkDisplayNameParams(field, style, SHORT, NARROW_FORMAT, locale,
                                    ERA_MASK|YEAR_MASK|MONTH_MASK|DAY_OF_WEEK_MASK|AM_PM_MASK)) {
            return null;
        }

        int fieldValue = get(field);

        // "GanNen" is supported only in the LONG style.
        if (field == YEAR
            && (getBaseStyle(style) != LONG || fieldValue != 1 || get(ERA) == 0)) {
            return null;
        }

        String name = CalendarDataUtility.retrieveFieldValueName(getCalendarType(), field,
                                                                 fieldValue, style, locale);
        // If the ERA value is null or empty, then
        // try to get its name or abbreviation from the Era instance.
        if ((name == null || name.isEmpty()) &&
                field == ERA &&
                fieldValue < eras.length) {
            Era era = eras[fieldValue];
            name = (style == SHORT) ? era.getAbbreviation() : era.getName();
        }
        return name;
    }

    @Override
    public Map<String,Integer> getDisplayNames(int field, int style, Locale locale) {
        if (!checkDisplayNameParams(field, style, ALL_STYLES, NARROW_FORMAT, locale,
                                    ERA_MASK|YEAR_MASK|MONTH_MASK|DAY_OF_WEEK_MASK|AM_PM_MASK)) {
            return null;
        }
        Map<String, Integer> names;
        names = CalendarDataUtility.retrieveFieldValueNames(getCalendarType(), field, style, locale);
        // If strings[] has fewer than eras[], get more names from eras[].
        if (names != null) {
            if (field == ERA) {
                int size = names.size();
                if (style == ALL_STYLES) {
                    Set<Integer> values = new HashSet<>();
                    // count unique era values
                    for (String key : names.keySet()) {
                        values.add(names.get(key));
                    }
                    size = values.size();
                }
                if (size < eras.length) {
                    int baseStyle = getBaseStyle(style);
                    for (int i = 0; i < eras.length; i++) {
                        if (!names.values().contains(i)) {
                            Era era = eras[i];
                            if (baseStyle == ALL_STYLES || baseStyle == SHORT
                                    || baseStyle == NARROW_FORMAT) {
                                names.put(era.getAbbreviation(), i);
                            }
                            if (baseStyle == ALL_STYLES || baseStyle == LONG) {
                                names.put(era.getName(), i);
                            }
                        }
                    }
                }
            }
        }
        return names;
    }

    /**
     * Returns the minimum value for the given calendar field of this
     * {@code Calendar} instance. The minimum value is
     * defined as the smallest value returned by the
     * {@link Calendar#get(int) get} method for any possible time value,
     * taking into consideration the current values of the
     * {@link Calendar#getFirstDayOfWeek() getFirstDayOfWeek},
     * {@link Calendar#getMinimalDaysInFirstWeek() getMinimalDaysInFirstWeek},
     * and {@link Calendar#getTimeZone() getTimeZone} methods.
     *
     * @param field the calendar field.
     * @return the minimum value for the given calendar field.
     * @see #getMaximum(int)
     * @see #getGreatestMinimum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMinimum(int)
     * @see #getActualMaximum(int)
     */
    public int getMinimum(int field) {
        return MIN_VALUES[field];
    }

    /**
     * Returns the maximum value for the given calendar field of this
     * {@code GregorianCalendar} instance. The maximum value is
     * defined as the largest value returned by the
     * {@link Calendar#get(int) get} method for any possible time value,
     * taking into consideration the current values of the
     * {@link Calendar#getFirstDayOfWeek() getFirstDayOfWeek},
     * {@link Calendar#getMinimalDaysInFirstWeek() getMinimalDaysInFirstWeek},
     * and {@link Calendar#getTimeZone() getTimeZone} methods.
     *
     * @param field the calendar field.
     * @return the maximum value for the given calendar field.
     * @see #getMinimum(int)
     * @see #getGreatestMinimum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMinimum(int)
     * @see #getActualMaximum(int)
     */
    public int getMaximum(int field) {
        return switch (field) {
            case YEAR -> {
                // The value should depend on the time zone of this calendar.
                LocalGregorianCalendar.Date d = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                yield Math.max(LEAST_MAX_VALUES[YEAR], d.getYear());
            }
            default -> MAX_VALUES[field];
        };
    }

    /**
     * Returns the highest minimum value for the given calendar field
     * of this {@code GregorianCalendar} instance. The highest
     * minimum value is defined as the largest value returned by
     * {@link #getActualMinimum(int)} for any possible time value,
     * taking into consideration the current values of the
     * {@link Calendar#getFirstDayOfWeek() getFirstDayOfWeek},
     * {@link Calendar#getMinimalDaysInFirstWeek() getMinimalDaysInFirstWeek},
     * and {@link Calendar#getTimeZone() getTimeZone} methods.
     *
     * @param field the calendar field.
     * @return the highest minimum value for the given calendar field.
     * @see #getMinimum(int)
     * @see #getMaximum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMinimum(int)
     * @see #getActualMaximum(int)
     */
    public int getGreatestMinimum(int field) {
        return field == YEAR ? 1 : MIN_VALUES[field];
    }

    /**
     * Returns the lowest maximum value for the given calendar field
     * of this {@code GregorianCalendar} instance. The lowest
     * maximum value is defined as the smallest value returned by
     * {@link #getActualMaximum(int)} for any possible time value,
     * taking into consideration the current values of the
     * {@link Calendar#getFirstDayOfWeek() getFirstDayOfWeek},
     * {@link Calendar#getMinimalDaysInFirstWeek() getMinimalDaysInFirstWeek},
     * and {@link Calendar#getTimeZone() getTimeZone} methods.
     *
     * @param field the calendar field
     * @return the lowest maximum value for the given calendar field.
     * @see #getMinimum(int)
     * @see #getMaximum(int)
     * @see #getGreatestMinimum(int)
     * @see #getActualMinimum(int)
     * @see #getActualMaximum(int)
     */
    public int getLeastMaximum(int field) {
        return switch (field) {
            case YEAR -> Math.min(LEAST_MAX_VALUES[YEAR], getMaximum(YEAR));
            default -> LEAST_MAX_VALUES[field];
        };
    }

    /**
     * Returns the minimum value that this calendar field could have,
     * taking into consideration the given time value and the current
     * values of the
     * {@link Calendar#getFirstDayOfWeek() getFirstDayOfWeek},
     * {@link Calendar#getMinimalDaysInFirstWeek() getMinimalDaysInFirstWeek},
     * and {@link Calendar#getTimeZone() getTimeZone} methods.
     *
     * @param field the calendar field
     * @return the minimum of the given field for the time value of
     * this {@code JapaneseImperialCalendar}
     * @see #getMinimum(int)
     * @see #getMaximum(int)
     * @see #getGreatestMinimum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMaximum(int)
     */
    public int getActualMinimum(int field) {
        if (!isFieldSet(YEAR_MASK|MONTH_MASK|WEEK_OF_YEAR_MASK, field)) {
            return getMinimum(field);
        }

        int value = 0;
        JapaneseImperialCalendar jc = getNormalizedCalendar();
        // Get a local date which includes time of day and time zone,
        // which are missing in jc.jdate.
        LocalGregorianCalendar.Date jd = jcal.getCalendarDate(jc.getTimeInMillis(),
                                                              getZone());
        int eraIndex = getEraIndex(jd);
        switch (field) {
            case YEAR -> {
                if (eraIndex > BEFORE_MEIJI) {
                    value = 1;
                    long since = eras[eraIndex].getSince(getZone());
                    CalendarDate d = jcal.getCalendarDate(since, getZone());
                    // Use the same year in jd to take care of leap
                    // years. i.e., both jd and d must agree on leap
                    // or common years.
                    jd.setYear(d.getYear());
                    jcal.normalize(jd);
                    assert jd.isLeapYear() == d.isLeapYear();
                    if (getYearOffsetInMillis(jd) < getYearOffsetInMillis(d)) {
                        value++;
                    }
                } else {
                    value = getMinimum(field);
                    CalendarDate d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                    // Use an equvalent year of d.getYear() if
                    // possible. Otherwise, ignore the leap year and
                    // common year difference.
                    int y = d.getYear();
                    if (y > 400) {
                        y -= 400;
                    }
                    jd.setYear(y);
                    jcal.normalize(jd);
                    if (getYearOffsetInMillis(jd) < getYearOffsetInMillis(d)) {
                        value++;
                    }
                }
            }
            case MONTH -> {
                // In Before Meiji and Meiji, January is the first month.
                if (eraIndex > MEIJI && jd.getYear() == 1) {
                    long since = eras[eraIndex].getSince(getZone());
                    CalendarDate d = jcal.getCalendarDate(since, getZone());
                    value = d.getMonth() - 1;
                    if (jd.getDayOfMonth() < d.getDayOfMonth()) {
                        value++;
                    }
                }
            }
            case WEEK_OF_YEAR -> {
                value = 1;
                CalendarDate d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                // shift 400 years to avoid underflow
                d.addYear(+400);
                jcal.normalize(d);
                jd.setEra(d.getEra());
                jd.setYear(d.getYear());
                jcal.normalize(jd);

                long jan1 = jcal.getFixedDate(d);
                long fd = jcal.getFixedDate(jd);
                int woy = getWeekNumber(jan1, fd);
                long day1 = fd - (7 * (woy - 1));
                if ((day1 < jan1) ||
                    (day1 == jan1 &&
                     jd.getTimeOfDay() < d.getTimeOfDay())) {
                    value++;
                }
            }
        }
        return value;
    }

    /**
     * Returns the maximum value that this calendar field could have,
     * taking into consideration the given time value and the current
     * values of the
     * {@link Calendar#getFirstDayOfWeek() getFirstDayOfWeek},
     * {@link Calendar#getMinimalDaysInFirstWeek() getMinimalDaysInFirstWeek},
     * and
     * {@link Calendar#getTimeZone() getTimeZone} methods.
     * For example, if the date of this instance is Heisei 16February 1,
     * the actual maximum value of the {@code DAY_OF_MONTH} field
     * is 29 because Heisei 16 is a leap year, and if the date of this
     * instance is Heisei 17 February 1, it's 28.
     *
     * @param field the calendar field
     * @return the maximum of the given field for the time value of
     * this {@code JapaneseImperialCalendar}
     * @see #getMinimum(int)
     * @see #getMaximum(int)
     * @see #getGreatestMinimum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMinimum(int)
     */
    public int getActualMaximum(int field) {
        final int fieldsForFixedMax = ERA_MASK|DAY_OF_WEEK_MASK|HOUR_MASK|AM_PM_MASK|
            HOUR_OF_DAY_MASK|MINUTE_MASK|SECOND_MASK|MILLISECOND_MASK|
            ZONE_OFFSET_MASK|DST_OFFSET_MASK;
        if ((fieldsForFixedMax & (1<<field)) != 0) {
            return getMaximum(field);
        }

        JapaneseImperialCalendar jc = getNormalizedCalendar();
        LocalGregorianCalendar.Date date = jc.jdate;

        return switch (field) {
            case MONTH -> {
                int month = DECEMBER;
                if (isTransitionYear(date.getNormalizedYear())) {
                    // TODO: there may be multiple transitions in a year.
                    int eraIndex = getEraIndex(date);
                    if (date.getYear() != 1) {
                        eraIndex++;
                        assert eraIndex < eras.length;
                    }
                    long transition = sinceFixedDates[eraIndex];
                    long fd = jc.cachedFixedDate;
                    if (fd < transition) {
                        LocalGregorianCalendar.Date ldate
                            = (LocalGregorianCalendar.Date) date.clone();
                        jcal.getCalendarDateFromFixedDate(ldate, transition - 1);
                        month = ldate.getMonth() - 1;
                    }
                } else {
                    LocalGregorianCalendar.Date d = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                    if (date.getEra() == d.getEra() && date.getYear() == d.getYear()) {
                        month = d.getMonth() - 1;
                    }
                }
                yield month;
            }
            case DAY_OF_MONTH -> jcal.getMonthLength(date);
            case DAY_OF_YEAR -> {
                if (isTransitionYear(date.getNormalizedYear())) {
                    // Handle transition year.
                    // TODO: there may be multiple transitions in a year.
                    int eraIndex = getEraIndex(date);
                    if (date.getYear() != 1) {
                        eraIndex++;
                        assert eraIndex < eras.length;
                    }
                    long transition = sinceFixedDates[eraIndex];
                    long fd = jc.cachedFixedDate;
                    CalendarDate d = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
                    d.setDate(date.getNormalizedYear(), BaseCalendar.JANUARY, 1);
                    if (fd < transition) {
                        yield (int) (transition - gcal.getFixedDate(d));
                    }
                    d.addYear(1);
                    yield (int) (gcal.getFixedDate(d) - transition);
                }
                LocalGregorianCalendar.Date d = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                if (date.getEra() == d.getEra() && date.getYear() == d.getYear()) {
                    long fd = jcal.getFixedDate(d);
                    long jan1 = getFixedDateJan1(d, fd);
                    yield (int) (fd - jan1) + 1;
                } else if (date.getYear() == getMinimum(YEAR)) {
                    CalendarDate d1 = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                    long fd1 = jcal.getFixedDate(d1);
                    d1.addYear(1);
                    d1.setMonth(BaseCalendar.JANUARY).setDayOfMonth(1);
                    jcal.normalize(d1);
                    long fd2 = jcal.getFixedDate(d1);
                    yield (int) (fd2 - fd1);
                } else {
                    yield jcal.getYearLength(date);
                }
            }
            case WEEK_OF_YEAR -> {
                if (!isTransitionYear(date.getNormalizedYear())) {
                    LocalGregorianCalendar.Date jd = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                    if (date.getEra() == jd.getEra() && date.getYear() == jd.getYear()) {
                        long fd = jcal.getFixedDate(jd);
                        long jan1 = getFixedDateJan1(jd, fd);
                        yield getWeekNumber(jan1, fd);
                    } else if (date.getEra() == null && date.getYear() == getMinimum(YEAR)) {
                        CalendarDate d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                        // shift 400 years to avoid underflow
                        d.addYear(+400);
                        jcal.normalize(d);
                        jd.setEra(d.getEra());
                        jd.setDate(d.getYear() + 1, BaseCalendar.JANUARY, 1);
                        jcal.normalize(jd);
                        long jan1 = jcal.getFixedDate(d);
                        long nextJan1 = jcal.getFixedDate(jd);
                        long nextJan1st = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(nextJan1 + 6,
                                                                                            getFirstDayOfWeek());
                        int ndays = (int) (nextJan1st - nextJan1);
                        if (ndays >= getMinimalDaysInFirstWeek()) {
                            nextJan1st -= 7;
                        }
                        yield getWeekNumber(jan1, nextJan1st);
                    }
                    // Get the day of week of January 1 of the year
                    CalendarDate d = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
                    d.setDate(date.getNormalizedYear(), BaseCalendar.JANUARY, 1);
                    int dayOfWeek = gcal.getDayOfWeek(d);
                    // Normalize the day of week with the firstDayOfWeek value
                    dayOfWeek -= getFirstDayOfWeek();
                    if (dayOfWeek < 0) {
                        dayOfWeek += 7;
                    }
                    int magic = dayOfWeek + getMinimalDaysInFirstWeek() - 1;
                    if ((magic == 6) ||
                        (date.isLeapYear() && (magic == 5 || magic == 12))) {
                        yield 53;
                    }
                    yield 52;
                }

                if (jc == this) {
                    jc = (JapaneseImperialCalendar) jc.clone();
                }
                int max = getActualMaximum(DAY_OF_YEAR);
                jc.set(DAY_OF_YEAR, max);
                int weekOfYear = jc.get(WEEK_OF_YEAR);
                if (weekOfYear == 1 && max > 7) {
                    jc.add(WEEK_OF_YEAR, -1);
                    weekOfYear = jc.get(WEEK_OF_YEAR);
                }
                yield weekOfYear;
            }
            case WEEK_OF_MONTH -> {
                LocalGregorianCalendar.Date jd = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                if (date.getEra() == jd.getEra() && date.getYear() == jd.getYear()) {
                    long fd = jcal.getFixedDate(jd);
                    long month1 = fd - jd.getDayOfMonth() + 1;
                    yield getWeekNumber(month1, fd);
                }
                CalendarDate d = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
                d.setDate(date.getNormalizedYear(), date.getMonth(), 1);
                int dayOfWeek = gcal.getDayOfWeek(d);
                int monthLength = actualMonthLength();
                dayOfWeek -= getFirstDayOfWeek();
                if (dayOfWeek < 0) {
                    dayOfWeek += 7;
                }
                int nDaysFirstWeek = 7 - dayOfWeek; // # of days in the first week
                int weekOfMonth = 3;
                if (nDaysFirstWeek >= getMinimalDaysInFirstWeek()) {
                    weekOfMonth++;
                }
                monthLength -= nDaysFirstWeek + 7 * 3;
                if (monthLength > 0) {
                    weekOfMonth++;
                    if (monthLength > 7) {
                        weekOfMonth++;
                    }
                }
                yield weekOfMonth;
            }
            case DAY_OF_WEEK_IN_MONTH -> {
                int ndays, dow1;
                int dow = date.getDayOfWeek();
                BaseCalendar.Date d = (BaseCalendar.Date) date.clone();
                ndays = jcal.getMonthLength(d);
                d.setDayOfMonth(1);
                jcal.normalize(d);
                dow1 = d.getDayOfWeek();
                int x = dow - dow1;
                if (x < 0) {
                    x += 7;
                }
                ndays -= x;
                yield (ndays + 6) / 7;
            }
            case YEAR -> {
                CalendarDate jd = jcal.getCalendarDate(jc.getTimeInMillis(), getZone());
                CalendarDate d;
                int eraIndex = getEraIndex(date);
                int year;
                if (eraIndex == eras.length - 1) {
                    d = jcal.getCalendarDate(Long.MAX_VALUE, getZone());
                    year = d.getYear();
                    // Use an equivalent year for the
                    // getYearOffsetInMillis call to avoid overflow.
                    if (year > 400) {
                        jd.setYear(year - 400);
                    }
                } else {
                    d = jcal.getCalendarDate(eras[eraIndex + 1].getSince(getZone()) - 1, getZone());
                    year = d.getYear();
                    // Use the same year as d.getYear() to be
                    // consistent with leap and common years.
                    jd.setYear(year);
                }
                jcal.normalize(jd);
                if (getYearOffsetInMillis(jd) > getYearOffsetInMillis(d)) {
                    year--;
                }
                yield year;
            }
            default -> throw new ArrayIndexOutOfBoundsException(field);
        };
    }

    /**
     * Returns the millisecond offset from the beginning of the
     * year. In the year for Long.MIN_VALUE, it's a pseudo value
     * beyond the limit. The given CalendarDate object must have been
     * normalized before calling this method.
     */
    private long getYearOffsetInMillis(CalendarDate date) {
        long t = (jcal.getDayOfYear(date) - 1) * ONE_DAY;
        return t + date.getTimeOfDay() - date.getZoneOffset();
    }

    public Object clone() {
        JapaneseImperialCalendar other = (JapaneseImperialCalendar) super.clone();

        other.jdate = (LocalGregorianCalendar.Date) jdate.clone();
        other.originalFields = null;
        other.zoneOffsets = null;
        return other;
    }

    public TimeZone getTimeZone() {
        TimeZone zone = super.getTimeZone();
        // To share the zone by the CalendarDate
        jdate.setZone(zone);
        return zone;
    }

    public void setTimeZone(TimeZone zone) {
        super.setTimeZone(zone);
        // To share the zone by the CalendarDate
        jdate.setZone(zone);
    }

    /**
     * The fixed date corresponding to jdate. If the value is
     * Long.MIN_VALUE, the fixed date value is unknown.
     */
    private transient long cachedFixedDate = Long.MIN_VALUE;

    /**
     * Converts the time value (millisecond offset from the <a
     * href="Calendar.html#Epoch">Epoch</a>) to calendar field values.
     * The time is <em>not</em>
     * recomputed first; to recompute the time, then the fields, call the
     * {@code complete} method.
     *
     * @see Calendar#complete
     */
    protected void computeFields() {
        int mask = 0;
        if (isPartiallyNormalized()) {
            // Determine which calendar fields need to be computed.
            mask = getSetStateFields();
            int fieldMask = ~mask & ALL_FIELDS;
            if (fieldMask != 0 || cachedFixedDate == Long.MIN_VALUE) {
                mask |= computeFields(fieldMask,
                                      mask & (ZONE_OFFSET_MASK|DST_OFFSET_MASK));
                assert mask == ALL_FIELDS;
            }
        } else {
            // Specify all fields
            mask = ALL_FIELDS;
            computeFields(mask, 0);
        }
        // After computing all the fields, set the field state to `COMPUTED'.
        setFieldsComputed(mask);
    }

    /**
     * This computeFields implements the conversion from UTC
     * (millisecond offset from the Epoch) to calendar
     * field values. fieldMask specifies which fields to change the
     * setting state to COMPUTED, although all fields are set to
     * the correct values. This is required to fix 4685354.
     *
     * @param fieldMask a bit mask to specify which fields to change
     * the setting state.
     * @param tzMask a bit mask to specify which time zone offset
     * fields to be used for time calculations
     * @return a new field mask that indicates what field values have
     * actually been set.
     */
    private int computeFields(int fieldMask, int tzMask) {
        int zoneOffset = 0;
        TimeZone tz = getZone();
        if (zoneOffsets == null) {
            zoneOffsets = new int[2];
        }
        if (tzMask != (ZONE_OFFSET_MASK|DST_OFFSET_MASK)) {
            if (tz instanceof ZoneInfo) {
                zoneOffset = ((ZoneInfo)tz).getOffsets(time, zoneOffsets);
            } else {
                zoneOffset = tz.getOffset(time);
                zoneOffsets[0] = tz.getRawOffset();
                zoneOffsets[1] = zoneOffset - zoneOffsets[0];
            }
        }
        if (tzMask != 0) {
            if (isFieldSet(tzMask, ZONE_OFFSET)) {
                zoneOffsets[0] = internalGet(ZONE_OFFSET);
            }
            if (isFieldSet(tzMask, DST_OFFSET)) {
                zoneOffsets[1] = internalGet(DST_OFFSET);
            }
            zoneOffset = zoneOffsets[0] + zoneOffsets[1];
        }

        // By computing time and zoneOffset separately, we can take
        // the wider range of time+zoneOffset than the previous
        // implementation.
        long fixedDate = zoneOffset / ONE_DAY;
        int timeOfDay = zoneOffset % (int)ONE_DAY;
        fixedDate += time / ONE_DAY;
        timeOfDay += (int) (time % ONE_DAY);
        if (timeOfDay >= ONE_DAY) {
            timeOfDay -= ONE_DAY;
            ++fixedDate;
        } else {
            while (timeOfDay < 0) {
                timeOfDay += ONE_DAY;
                --fixedDate;
            }
        }
        fixedDate += EPOCH_OFFSET;

        // See if we can use jdate to avoid date calculation.
        if (fixedDate != cachedFixedDate || fixedDate < 0) {
            jcal.getCalendarDateFromFixedDate(jdate, fixedDate);
            cachedFixedDate = fixedDate;
        }
        int era = getEraIndex(jdate);
        int year = jdate.getYear();

        // Always set the ERA and YEAR values.
        internalSet(ERA, era);
        internalSet(YEAR, year);
        int mask = fieldMask | (ERA_MASK|YEAR_MASK);

        int month =  jdate.getMonth() - 1; // 0-based
        int dayOfMonth = jdate.getDayOfMonth();

        // Set the basic date fields.
        if ((fieldMask & (MONTH_MASK|DAY_OF_MONTH_MASK|DAY_OF_WEEK_MASK))
            != 0) {
            internalSet(MONTH, month);
            internalSet(DAY_OF_MONTH, dayOfMonth);
            internalSet(DAY_OF_WEEK, jdate.getDayOfWeek());
            mask |= MONTH_MASK|DAY_OF_MONTH_MASK|DAY_OF_WEEK_MASK;
        }

        if ((fieldMask & (HOUR_OF_DAY_MASK|AM_PM_MASK|HOUR_MASK
                          |MINUTE_MASK|SECOND_MASK|MILLISECOND_MASK)) != 0) {
            if (timeOfDay != 0) {
                int hours = timeOfDay / ONE_HOUR;
                internalSet(HOUR_OF_DAY, hours);
                internalSet(AM_PM, hours / 12); // Assume AM == 0
                internalSet(HOUR, hours % 12);
                int r = timeOfDay % ONE_HOUR;
                internalSet(MINUTE, r / ONE_MINUTE);
                r %= ONE_MINUTE;
                internalSet(SECOND, r / ONE_SECOND);
                internalSet(MILLISECOND, r % ONE_SECOND);
            } else {
                internalSet(HOUR_OF_DAY, 0);
                internalSet(AM_PM, AM);
                internalSet(HOUR, 0);
                internalSet(MINUTE, 0);
                internalSet(SECOND, 0);
                internalSet(MILLISECOND, 0);
            }
            mask |= (HOUR_OF_DAY_MASK|AM_PM_MASK|HOUR_MASK
                     |MINUTE_MASK|SECOND_MASK|MILLISECOND_MASK);
        }

        if ((fieldMask & (ZONE_OFFSET_MASK|DST_OFFSET_MASK)) != 0) {
            internalSet(ZONE_OFFSET, zoneOffsets[0]);
            internalSet(DST_OFFSET, zoneOffsets[1]);
            mask |= (ZONE_OFFSET_MASK|DST_OFFSET_MASK);
        }

        if ((fieldMask & (DAY_OF_YEAR_MASK|WEEK_OF_YEAR_MASK
                          |WEEK_OF_MONTH_MASK|DAY_OF_WEEK_IN_MONTH_MASK)) != 0) {
            int normalizedYear = jdate.getNormalizedYear();
            // If it's a year of an era transition, we need to handle
            // irregular year boundaries.
            boolean transitionYear = isTransitionYear(jdate.getNormalizedYear());
            int dayOfYear;
            long fixedDateJan1;
            if (transitionYear) {
                fixedDateJan1 = getFixedDateJan1(jdate, fixedDate);
                dayOfYear = (int)(fixedDate - fixedDateJan1) + 1;
            } else if (normalizedYear == MIN_VALUES[YEAR]) {
                CalendarDate dx = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
                fixedDateJan1 = jcal.getFixedDate(dx);
                dayOfYear = (int)(fixedDate - fixedDateJan1) + 1;
            } else {
                dayOfYear = (int) jcal.getDayOfYear(jdate);
                fixedDateJan1 = fixedDate - dayOfYear + 1;
            }
            long fixedDateMonth1 = transitionYear ?
                getFixedDateMonth1(jdate, fixedDate) : fixedDate - dayOfMonth + 1;

            internalSet(DAY_OF_YEAR, dayOfYear);
            internalSet(DAY_OF_WEEK_IN_MONTH, (dayOfMonth - 1) / 7 + 1);

            int weekOfYear = getWeekNumber(fixedDateJan1, fixedDate);

            // The spec is to calculate WEEK_OF_YEAR in the
            // ISO8601-style. This creates problems, though.
            if (weekOfYear == 0) {
                // If the date belongs to the last week of the
                // previous year, use the week number of "12/31" of
                // the "previous" year. Again, if the previous year is
                // a transition year, we need to take care of it.
                // Usually the previous day of the first day of a year
                // is December 31, which is not always true in the
                // Japanese imperial calendar system.
                long fixedDec31 = fixedDateJan1 - 1;
                long prevJan1;
                LocalGregorianCalendar.Date d = getCalendarDate(fixedDec31);
                if (!(transitionYear || isTransitionYear(d.getNormalizedYear()))) {
                    prevJan1 = fixedDateJan1 - 365;
                    if (d.isLeapYear()) {
                        --prevJan1;
                    }
                } else if (transitionYear) {
                    if (jdate.getYear() == 1) {
                        // As of Reiwa (since Meiji) there's no case
                        // that there are multiple transitions in a
                        // year.  Historically there was such
                        // case. There might be such case again in the
                        // future.
                        if (era > REIWA) {
                            CalendarDate pd = eras[era - 1].getSinceDate();
                            if (normalizedYear == pd.getYear()) {
                                d.setMonth(pd.getMonth()).setDayOfMonth(pd.getDayOfMonth());
                            }
                        } else {
                            d.setMonth(LocalGregorianCalendar.JANUARY).setDayOfMonth(1);
                        }
                        jcal.normalize(d);
                        prevJan1 = jcal.getFixedDate(d);
                    } else {
                        prevJan1 = fixedDateJan1 - 365;
                        if (d.isLeapYear()) {
                            --prevJan1;
                        }
                    }
                } else {
                    CalendarDate cd = eras[getEraIndex(jdate)].getSinceDate();
                    d.setMonth(cd.getMonth()).setDayOfMonth(cd.getDayOfMonth());
                    jcal.normalize(d);
                    prevJan1 = jcal.getFixedDate(d);
                }
                weekOfYear = getWeekNumber(prevJan1, fixedDec31);
            } else {
                if (!transitionYear) {
                    // Regular years
                    if (weekOfYear >= 52) {
                        long nextJan1 = fixedDateJan1 + 365;
                        if (jdate.isLeapYear()) {
                            nextJan1++;
                        }
                        long nextJan1st = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(nextJan1 + 6,
                                                                                            getFirstDayOfWeek());
                        int ndays = (int)(nextJan1st - nextJan1);
                        if (ndays >= getMinimalDaysInFirstWeek() && fixedDate >= (nextJan1st - 7)) {
                            // The first days forms a week in which the date is included.
                            weekOfYear = 1;
                        }
                    }
                } else {
                    LocalGregorianCalendar.Date d = (LocalGregorianCalendar.Date) jdate.clone();
                    long nextJan1;
                    if (jdate.getYear() == 1) {
                        d.addYear(+1);
                        d.setMonth(LocalGregorianCalendar.JANUARY).setDayOfMonth(1);
                        nextJan1 = jcal.getFixedDate(d);
                    } else {
                        int nextEraIndex = getEraIndex(d) + 1;
                        CalendarDate cd = eras[nextEraIndex].getSinceDate();
                        d.setEra(eras[nextEraIndex]);
                        d.setDate(1, cd.getMonth(), cd.getDayOfMonth());
                        jcal.normalize(d);
                        nextJan1 = jcal.getFixedDate(d);
                    }
                    long nextJan1st = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(nextJan1 + 6,
                                                                                        getFirstDayOfWeek());
                    int ndays = (int)(nextJan1st - nextJan1);
                    if (ndays >= getMinimalDaysInFirstWeek() && fixedDate >= (nextJan1st - 7)) {
                        // The first days forms a week in which the date is included.
                        weekOfYear = 1;
                    }
                }
            }
            internalSet(WEEK_OF_YEAR, weekOfYear);
            internalSet(WEEK_OF_MONTH, getWeekNumber(fixedDateMonth1, fixedDate));
            mask |= (DAY_OF_YEAR_MASK|WEEK_OF_YEAR_MASK|WEEK_OF_MONTH_MASK|DAY_OF_WEEK_IN_MONTH_MASK);
        }
        return mask;
    }

    /**
     * Returns the number of weeks in a period between fixedDay1 and
     * fixedDate. The getFirstDayOfWeek-getMinimalDaysInFirstWeek rule
     * is applied to calculate the number of weeks.
     *
     * @param fixedDay1 the fixed date of the first day of the period
     * @param fixedDate the fixed date of the last day of the period
     * @return the number of weeks of the given period
     */
    private int getWeekNumber(long fixedDay1, long fixedDate) {
        // We can always use `jcal' since Julian and Gregorian are the
        // same thing for this calculation.
        long fixedDay1st = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(fixedDay1 + 6,
                                                                             getFirstDayOfWeek());
        int ndays = (int)(fixedDay1st - fixedDay1);
        assert ndays <= 7;
        if (ndays >= getMinimalDaysInFirstWeek()) {
            fixedDay1st -= 7;
        }
        int normalizedDayOfPeriod = (int)(fixedDate - fixedDay1st);
        if (normalizedDayOfPeriod >= 0) {
            return normalizedDayOfPeriod / 7 + 1;
        }
        return CalendarUtils.floorDivide(normalizedDayOfPeriod, 7) + 1;
    }

    /**
     * Converts calendar field values to the time value (millisecond
     * offset from the <a href="Calendar.html#Epoch">Epoch</a>).
     *
     * @throws    IllegalArgumentException if any calendar fields are invalid.
     */
    protected void computeTime() {
        // In non-lenient mode, perform brief checking of calendar
        // fields which have been set externally. Through this
        // checking, the field values are stored in originalFields[]
        // to see if any of them are normalized later.
        if (!isLenient()) {
            if (originalFields == null) {
                originalFields = new int[FIELD_COUNT];
            }
            for (int field = 0; field < FIELD_COUNT; field++) {
                int value = internalGet(field);
                if (isExternallySet(field)) {
                    // Quick validation for any out of range values
                    if (value < getMinimum(field) || value > getMaximum(field)) {
                        throw new IllegalArgumentException(getFieldName(field));
                    }
                }
                originalFields[field] = value;
            }
        }

        // Let the super class determine which calendar fields to be
        // used to calculate the time.
        int fieldMask = selectFields();

        int year;
        int era;

        if (isSet(ERA)) {
            era = internalGet(ERA);
            year = isSet(YEAR) ? internalGet(YEAR) : 1;
        } else {
            if (isSet(YEAR)) {
                era = currentEra;
                year = internalGet(YEAR);
            } else {
                // Equivalent to 1970 (Gregorian)
                era = SHOWA;
                year = 45;
            }
        }

        // Calculate the time of day. We rely on the convention that
        // an UNSET field has 0.
        long timeOfDay = 0;
        if (isFieldSet(fieldMask, HOUR_OF_DAY)) {
            timeOfDay += (long) internalGet(HOUR_OF_DAY);
        } else {
            timeOfDay += internalGet(HOUR);
            // The default value of AM_PM is 0 which designates AM.
            if (isFieldSet(fieldMask, AM_PM)) {
                timeOfDay += 12 * internalGet(AM_PM);
            }
        }
        timeOfDay *= 60;
        timeOfDay += internalGet(MINUTE);
        timeOfDay *= 60;
        timeOfDay += internalGet(SECOND);
        timeOfDay *= 1000;
        timeOfDay += internalGet(MILLISECOND);

        // Convert the time of day to the number of days and the
        // millisecond offset from midnight.
        long fixedDate = timeOfDay / ONE_DAY;
        timeOfDay %= ONE_DAY;
        while (timeOfDay < 0) {
            timeOfDay += ONE_DAY;
            --fixedDate;
        }

        // Calculate the fixed date since January 1, 1 (Gregorian).
        fixedDate += getFixedDate(era, year, fieldMask);

        // millis represents local wall-clock time in milliseconds.
        long millis = (fixedDate - EPOCH_OFFSET) * ONE_DAY + timeOfDay;

        // Compute the time zone offset and DST offset.  There are two potential
        // ambiguities here.  We'll assume a 2:00 am (wall time) switchover time
        // for discussion purposes here.
        // 1. The transition into DST.  Here, a designated time of 2:00 am - 2:59 am
        //    can be in standard or in DST depending.  However, 2:00 am is an invalid
        //    representation (the representation jumps from 1:59:59 am Std to 3:00:00 am DST).
        //    We assume standard time.
        // 2. The transition out of DST.  Here, a designated time of 1:00 am - 1:59 am
        //    can be in standard or DST.  Both are valid representations (the rep
        //    jumps from 1:59:59 DST to 1:00:00 Std).
        //    Again, we assume standard time.
        // We use the TimeZone object, unless the user has explicitly set the ZONE_OFFSET
        // or DST_OFFSET fields; then we use those fields.
        TimeZone zone = getZone();
        if (zoneOffsets == null) {
            zoneOffsets = new int[2];
        }
        int tzMask = fieldMask & (ZONE_OFFSET_MASK|DST_OFFSET_MASK);
        if (tzMask != (ZONE_OFFSET_MASK|DST_OFFSET_MASK)) {
            if (zone instanceof ZoneInfo) {
                ((ZoneInfo)zone).getOffsetsByWall(millis, zoneOffsets);
            } else {
                zone.getOffsets(millis - zone.getRawOffset(), zoneOffsets);
            }
        }
        if (tzMask != 0) {
            if (isFieldSet(tzMask, ZONE_OFFSET)) {
                zoneOffsets[0] = internalGet(ZONE_OFFSET);
            }
            if (isFieldSet(tzMask, DST_OFFSET)) {
                zoneOffsets[1] = internalGet(DST_OFFSET);
            }
        }

        // Adjust the time zone offset values to get the UTC time.
        millis -= zoneOffsets[0] + zoneOffsets[1];

        // Set this calendar's time in milliseconds
        time = millis;

        int mask = computeFields(fieldMask | getSetStateFields(), tzMask);

        if (!isLenient()) {
            for (int field = 0; field < FIELD_COUNT; field++) {
                if (!isExternallySet(field)) {
                    continue;
                }
                if (originalFields[field] != internalGet(field)) {
                    int wrongValue = internalGet(field);
                    // Restore the original field values
                    System.arraycopy(originalFields, 0, fields, 0, fields.length);
                    throw new IllegalArgumentException(getFieldName(field) + "=" + wrongValue
                                                       + ", expected " + originalFields[field]);
                }
            }
        }
        setFieldsNormalized(mask);
    }

    /**
     * Computes the fixed date under either the Gregorian or the
     * Julian calendar, using the given year and the specified calendar fields.
     *
     * @param era era index
     * @param year the normalized year number, with 0 indicating the
     * year 1 BCE, -1 indicating 2 BCE, etc.
     * @param fieldMask the calendar fields to be used for the date calculation
     * @return the fixed date
     * @see Calendar#selectFields
     */
    private long getFixedDate(int era, int year, int fieldMask) {
        int month = JANUARY;
        int firstDayOfMonth = 1;
        if (isFieldSet(fieldMask, MONTH)) {
            // No need to check if MONTH has been set (no isSet(MONTH)
            // call) since its unset value happens to be JANUARY (0).
            month = internalGet(MONTH);

            // If the month is out of range, adjust it into range.
            if (month > DECEMBER) {
                year += month / 12;
                month %= 12;
            } else if (month < JANUARY) {
                int[] rem = new int[1];
                year += CalendarUtils.floorDivide(month, 12, rem);
                month = rem[0];
            }
        } else {
            if (year == 1 && era != 0) {
                CalendarDate d = eras[era].getSinceDate();
                month = d.getMonth() - 1;
                firstDayOfMonth = d.getDayOfMonth();
            }
        }

        // Adjust the base date if year is the minimum value.
        if (year == MIN_VALUES[YEAR]) {
            CalendarDate dx = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
            int m = dx.getMonth() - 1;
            if (month < m) {
                month = m;
            }
            if (month == m) {
                firstDayOfMonth = dx.getDayOfMonth();
            }
        }

        LocalGregorianCalendar.Date date = jcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        date.setEra(era > 0 ? eras[era] : null);
        date.setDate(year, month + 1, firstDayOfMonth);
        jcal.normalize(date);

        // Get the fixed date since Jan 1, 1 (Gregorian). We are on
        // the first day of either `month' or January in 'year'.
        long fixedDate = jcal.getFixedDate(date);

        if (isFieldSet(fieldMask, MONTH)) {
            // Month-based calculations
            if (isFieldSet(fieldMask, DAY_OF_MONTH)) {
                // We are on the "first day" of the month (which may
                // not be 1). Just add the offset if DAY_OF_MONTH is
                // set. If the isSet call returns false, that means
                // DAY_OF_MONTH has been selected just because of the
                // selected combination. We don't need to add any
                // since the default value is the "first day".
                if (isSet(DAY_OF_MONTH)) {
                    // To avoid underflow with DAY_OF_MONTH-firstDayOfMonth, add
                    // DAY_OF_MONTH, then subtract firstDayOfMonth.
                    fixedDate += internalGet(DAY_OF_MONTH);
                    fixedDate -= firstDayOfMonth;
                }
            } else {
                if (isFieldSet(fieldMask, WEEK_OF_MONTH)) {
                    long firstDayOfWeek = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(fixedDate + 6,
                                                                                            getFirstDayOfWeek());
                    // If we have enough days in the first week, then
                    // move to the previous week.
                    if ((firstDayOfWeek - fixedDate) >= getMinimalDaysInFirstWeek()) {
                        firstDayOfWeek -= 7;
                    }
                    if (isFieldSet(fieldMask, DAY_OF_WEEK)) {
                        firstDayOfWeek = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(firstDayOfWeek + 6,
                                                                                           internalGet(DAY_OF_WEEK));
                    }
                    // In lenient mode, we treat days of the previous
                    // months as a part of the specified
                    // WEEK_OF_MONTH. See 4633646.
                    fixedDate = firstDayOfWeek + 7 * (internalGet(WEEK_OF_MONTH) - 1);
                } else {
                    int dayOfWeek;
                    if (isFieldSet(fieldMask, DAY_OF_WEEK)) {
                        dayOfWeek = internalGet(DAY_OF_WEEK);
                    } else {
                        dayOfWeek = getFirstDayOfWeek();
                    }
                    // We are basing this on the day-of-week-in-month.  The only
                    // trickiness occurs if the day-of-week-in-month is
                    // negative.
                    int dowim;
                    if (isFieldSet(fieldMask, DAY_OF_WEEK_IN_MONTH)) {
                        dowim = internalGet(DAY_OF_WEEK_IN_MONTH);
                    } else {
                        dowim = 1;
                    }
                    if (dowim >= 0) {
                        fixedDate = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(fixedDate + (7 * dowim) - 1,
                                                                                      dayOfWeek);
                    } else {
                        // Go to the first day of the next week of
                        // the specified week boundary.
                        int lastDate = monthLength(month, year) + (7 * (dowim + 1));
                        // Then, get the day of week date on or before the last date.
                        fixedDate = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(fixedDate + lastDate - 1,
                                                                                      dayOfWeek);
                    }
                }
            }
        } else {
            // We are on the first day of the year.
            if (isFieldSet(fieldMask, DAY_OF_YEAR)) {
                if (isTransitionYear(date.getNormalizedYear())) {
                    fixedDate = getFixedDateJan1(date, fixedDate);
                }
                // Add the offset, then subtract 1. (Make sure to avoid underflow.)
                fixedDate += internalGet(DAY_OF_YEAR);
                fixedDate--;
            } else {
                long firstDayOfWeek = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(fixedDate + 6,
                                                                                        getFirstDayOfWeek());
                // If we have enough days in the first week, then move
                // to the previous week.
                if ((firstDayOfWeek - fixedDate) >= getMinimalDaysInFirstWeek()) {
                    firstDayOfWeek -= 7;
                }
                if (isFieldSet(fieldMask, DAY_OF_WEEK)) {
                    int dayOfWeek = internalGet(DAY_OF_WEEK);
                    if (dayOfWeek != getFirstDayOfWeek()) {
                        firstDayOfWeek = LocalGregorianCalendar.getDayOfWeekDateOnOrBefore(firstDayOfWeek + 6,
                                                                                           dayOfWeek);
                    }
                }
                fixedDate = firstDayOfWeek + 7 * ((long)internalGet(WEEK_OF_YEAR) - 1);
            }
        }
        return fixedDate;
    }

    /**
     * Returns the fixed date of the first day of the year (usually
     * January 1) before the specified date.
     *
     * @param date the date for which the first day of the year is
     * calculated. The date has to be in the cut-over year.
     * @param fixedDate the fixed date representation of the date
     */
    private long getFixedDateJan1(LocalGregorianCalendar.Date date, long fixedDate) {
        Era era = date.getEra();
        if (date.getEra() != null && date.getYear() == 1) {
            for (int eraIndex = getEraIndex(date); eraIndex > 0; eraIndex--) {
                CalendarDate d = eras[eraIndex].getSinceDate();
                long fd = gcal.getFixedDate(d);
                // There might be multiple era transitions in a year.
                if (fd > fixedDate) {
                    continue;
                }
                return fd;
            }
        }
        CalendarDate d = gcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        d.setDate(date.getNormalizedYear(), Gregorian.JANUARY, 1);
        return gcal.getFixedDate(d);
    }

    /**
     * Returns the fixed date of the first date of the month (usually
     * the 1st of the month) before the specified date.
     *
     * @param date the date for which the first day of the month is
     * calculated. The date must be in the era transition year.
     * @param fixedDate the fixed date representation of the date
     */
    private long getFixedDateMonth1(LocalGregorianCalendar.Date date,
                                          long fixedDate) {
        int eraIndex = getTransitionEraIndex(date);
        if (eraIndex != -1) {
            long transition = sinceFixedDates[eraIndex];
            // If the given date is on or after the transition date, then
            // return the transition date.
            if (transition <= fixedDate) {
                return transition;
            }
        }

        // Otherwise, we can use the 1st day of the month.
        return fixedDate - date.getDayOfMonth() + 1;
    }

    /**
     * Returns a LocalGregorianCalendar.Date produced from the specified fixed date.
     *
     * @param fd the fixed date
     */
    private static LocalGregorianCalendar.Date getCalendarDate(long fd) {
        LocalGregorianCalendar.Date d = jcal.newCalendarDate(TimeZone.NO_TIMEZONE);
        jcal.getCalendarDateFromFixedDate(d, fd);
        return d;
    }

    /**
     * Returns the length of the specified month in the specified
     * Gregorian year. The year number must be normalized.
     *
     * @see GregorianCalendar#isLeapYear(int)
     */
    private int monthLength(int month, int gregorianYear) {
        return CalendarUtils.isGregorianLeapYear(gregorianYear) ?
            GregorianCalendar.LEAP_MONTH_LENGTH[month] : GregorianCalendar.MONTH_LENGTH[month];
    }

    /**
     * Returns the length of the specified month in the year provided
     * by internalGet(YEAR).
     *
     * @see GregorianCalendar#isLeapYear(int)
     */
    private int monthLength(int month) {
        assert jdate.isNormalized();
        return jdate.isLeapYear() ?
            GregorianCalendar.LEAP_MONTH_LENGTH[month] : GregorianCalendar.MONTH_LENGTH[month];
    }

    private int actualMonthLength() {
        int length = jcal.getMonthLength(jdate);
        int eraIndex = getTransitionEraIndex(jdate);
        if (eraIndex != -1) {
            long transitionFixedDate = sinceFixedDates[eraIndex];
            CalendarDate d = eras[eraIndex].getSinceDate();
            if (transitionFixedDate <= cachedFixedDate) {
                length -= d.getDayOfMonth() - 1;
            } else {
                length = d.getDayOfMonth() - 1;
            }
        }
        return length;
    }

    /**
     * Returns the index to the new era if the given date is in a
     * transition month.  For example, if the give date is Heisei 1
     * (1989) January 20, then the era index for Heisei is
     * returned. Likewise, if the given date is Showa 64 (1989)
     * January 3, then the era index for Heisei is returned. If the
     * given date is not in any transition month, then -1 is returned.
     */
    private static int getTransitionEraIndex(LocalGregorianCalendar.Date date) {
        int eraIndex = getEraIndex(date);
        CalendarDate transitionDate = eras[eraIndex].getSinceDate();
        if (transitionDate.getYear() == date.getNormalizedYear() &&
            transitionDate.getMonth() == date.getMonth()) {
            return eraIndex;
        }
        if (eraIndex < eras.length - 1) {
            transitionDate = eras[++eraIndex].getSinceDate();
            if (transitionDate.getYear() == date.getNormalizedYear() &&
                transitionDate.getMonth() == date.getMonth()) {
                return eraIndex;
            }
        }
        return -1;
    }

    private boolean isTransitionYear(int normalizedYear) {
        for (int i = eras.length - 1; i > 0; i--) {
            int transitionYear = eras[i].getSinceDate().getYear();
            if (normalizedYear == transitionYear) {
                return true;
            }
            if (normalizedYear > transitionYear) {
                break;
            }
        }
        return false;
    }

    private static int getEraIndex(LocalGregorianCalendar.Date date) {
        Era era = date.getEra();
        for (int i = eras.length - 1; i > 0; i--) {
            if (eras[i] == era) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Returns this object if it's normalized (all fields and time are
     * in sync). Otherwise, a cloned object is returned after calling
     * complete() in lenient mode.
     */
    private JapaneseImperialCalendar getNormalizedCalendar() {
        JapaneseImperialCalendar jc;
        if (isFullyNormalized()) {
            jc = this;
        } else {
            // Create a clone and normalize the calendar fields
            jc = (JapaneseImperialCalendar) this.clone();
            jc.setLenient(true);
            jc.complete();
        }
        return jc;
    }

    /**
     * After adjustments such as add(MONTH), add(YEAR), we don't want the
     * month to jump around.  E.g., we don't want Jan 31 + 1 month to go to Mar
     * 3, we want it to go to Feb 28.  Adjustments which might run into this
     * problem call this method to retain the proper month.
     */
    private void pinDayOfMonth(LocalGregorianCalendar.Date date) {
        int year = date.getYear();
        int dom = date.getDayOfMonth();
        if (year != getMinimum(YEAR)) {
            date.setDayOfMonth(1);
            jcal.normalize(date);
            int monthLength = jcal.getMonthLength(date);
            if (dom > monthLength) {
                date.setDayOfMonth(monthLength);
            } else {
                date.setDayOfMonth(dom);
            }
            jcal.normalize(date);
        } else {
            LocalGregorianCalendar.Date d = jcal.getCalendarDate(Long.MIN_VALUE, getZone());
            LocalGregorianCalendar.Date realDate = jcal.getCalendarDate(time, getZone());
            long tod = realDate.getTimeOfDay();
            // Use an equivalent year.
            realDate.addYear(+400);
            realDate.setMonth(date.getMonth());
            realDate.setDayOfMonth(1);
            jcal.normalize(realDate);
            int monthLength = jcal.getMonthLength(realDate);
            if (dom > monthLength) {
                realDate.setDayOfMonth(monthLength);
            } else {
                if (dom < d.getDayOfMonth()) {
                    realDate.setDayOfMonth(d.getDayOfMonth());
                } else {
                    realDate.setDayOfMonth(dom);
                }
            }
            if (realDate.getDayOfMonth() == d.getDayOfMonth() && tod < d.getTimeOfDay()) {
                realDate.setDayOfMonth(Math.min(dom + 1, monthLength));
            }
            // restore the year.
            date.setDate(year, realDate.getMonth(), realDate.getDayOfMonth());
            // Don't normalize date here so as not to cause underflow.
        }
    }

    /**
     * Returns the new value after 'roll'ing the specified value and amount.
     */
    private static int getRolledValue(int value, int amount, int min, int max) {
        assert value >= min && value <= max;
        int range = max - min + 1;
        amount %= range;
        int n = value + amount;
        if (n > max) {
            n -= range;
        } else if (n < min) {
            n += range;
        }
        assert n >= min && n <= max;
        return n;
    }

    /**
     * Returns the ERA.  We need a special method for this because the
     * default ERA is the current era, but a zero (unset) ERA means before Meiji.
     */
    private int internalGetEra() {
        return isSet(ERA) ? internalGet(ERA) : currentEra;
    }

    /**
     * Updates internal state.
     */
    @java.io.Serial
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (jdate == null) {
            jdate = jcal.newCalendarDate(getZone());
            cachedFixedDate = Long.MIN_VALUE;
        }
    }
}
