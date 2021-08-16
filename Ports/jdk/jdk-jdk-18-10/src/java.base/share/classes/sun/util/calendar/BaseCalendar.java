/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.TimeZone;

/**
 * The {@code BaseCalendar} provides basic calendar calculation
 * functions to support the Julian, Gregorian, and Gregorian-based
 * calendar systems.
 *
 * @author Masayoshi Okutsu
 * @since 1.5
 */

public abstract class BaseCalendar extends AbstractCalendar {

    public static final int JANUARY = 1;
    public static final int FEBRUARY = 2;
    public static final int MARCH = 3;
    public static final int APRIL = 4;
    public static final int MAY = 5;
    public static final int JUNE = 6;
    public static final int JULY = 7;
    public static final int AUGUST = 8;
    public static final int SEPTEMBER = 9;
    public static final int OCTOBER = 10;
    public static final int NOVEMBER = 11;
    public static final int DECEMBER = 12;

    // day of week constants
    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;
    public static final int THURSDAY = 5;
    public static final int FRIDAY = 6;
    public static final int SATURDAY = 7;

    // The base Gregorian year of FIXED_DATES[]
    private static final int BASE_YEAR = 1970;

    // Pre-calculated fixed dates of January 1 from BASE_YEAR
    // (Gregorian). This table covers all the years that can be
    // supported by the POSIX time_t (32-bit) after the Epoch. Note
    // that the data type is int[].
    private static final int[] FIXED_DATES = {
        719163, // 1970
        719528, // 1971
        719893, // 1972
        720259, // 1973
        720624, // 1974
        720989, // 1975
        721354, // 1976
        721720, // 1977
        722085, // 1978
        722450, // 1979
        722815, // 1980
        723181, // 1981
        723546, // 1982
        723911, // 1983
        724276, // 1984
        724642, // 1985
        725007, // 1986
        725372, // 1987
        725737, // 1988
        726103, // 1989
        726468, // 1990
        726833, // 1991
        727198, // 1992
        727564, // 1993
        727929, // 1994
        728294, // 1995
        728659, // 1996
        729025, // 1997
        729390, // 1998
        729755, // 1999
        730120, // 2000
        730486, // 2001
        730851, // 2002
        731216, // 2003
        731581, // 2004
        731947, // 2005
        732312, // 2006
        732677, // 2007
        733042, // 2008
        733408, // 2009
        733773, // 2010
        734138, // 2011
        734503, // 2012
        734869, // 2013
        735234, // 2014
        735599, // 2015
        735964, // 2016
        736330, // 2017
        736695, // 2018
        737060, // 2019
        737425, // 2020
        737791, // 2021
        738156, // 2022
        738521, // 2023
        738886, // 2024
        739252, // 2025
        739617, // 2026
        739982, // 2027
        740347, // 2028
        740713, // 2029
        741078, // 2030
        741443, // 2031
        741808, // 2032
        742174, // 2033
        742539, // 2034
        742904, // 2035
        743269, // 2036
        743635, // 2037
        744000, // 2038
        744365, // 2039
    };

    public abstract static class Date extends CalendarDate {
        protected Date() {
            super();
        }
        protected Date(TimeZone zone) {
            super(zone);
        }

        public Date setNormalizedDate(int normalizedYear, int month, int dayOfMonth) {
            setNormalizedYear(normalizedYear);
            setMonth(month).setDayOfMonth(dayOfMonth);
            return this;
        }

        public abstract int getNormalizedYear();

        public abstract void setNormalizedYear(int normalizedYear);

        // Cache for the fixed date of January 1 and year length of the
        // cachedYear. A simple benchmark showed 7% performance
        // improvement with >90% cache hit. The initial values are for Gregorian.
        int cachedYear = 2004;
        long cachedFixedDateJan1 = 731581L;
        long cachedFixedDateNextJan1 = cachedFixedDateJan1 + 366;

        protected final boolean hit(int year) {
            return year == cachedYear;
        }

        protected final boolean hit(long fixedDate) {
            return (fixedDate >= cachedFixedDateJan1 &&
                    fixedDate < cachedFixedDateNextJan1);
        }
        protected int getCachedYear() {
            return cachedYear;
        }

        protected long getCachedJan1() {
            return cachedFixedDateJan1;
        }

        protected void setCache(int year, long jan1, int len) {
            cachedYear = year;
            cachedFixedDateJan1 = jan1;
            cachedFixedDateNextJan1 = jan1 + len;
        }
    }

    public boolean validate(CalendarDate date) {
        Date bdate = (Date) date;
        if (bdate.isNormalized()) {
            return true;
        }
        int month = bdate.getMonth();
        if (month < JANUARY || month > DECEMBER) {
            return false;
        }
        int d = bdate.getDayOfMonth();
        if (d <= 0 || d > getMonthLength(bdate.getNormalizedYear(), month)) {
            return false;
        }
        int dow = bdate.getDayOfWeek();
        if (dow != Date.FIELD_UNDEFINED && dow != getDayOfWeek(bdate)) {
            return false;
        }

        if (!validateTime(date)) {
            return false;
        }

        bdate.setNormalized(true);
        return true;
    }

    public boolean normalize(CalendarDate date) {
        if (date.isNormalized()) {
            return true;
        }

        Date bdate = (Date) date;
        TimeZone zi = bdate.getZone();

        // If the date has a time zone, then we need to recalculate
        // the calendar fields. Let getTime() do it.
        if (zi != null) {
            getTime(date);
            return true;
        }

        int days = normalizeTime(bdate);
        normalizeMonth(bdate);
        long d = (long)bdate.getDayOfMonth() + days;
        int m = bdate.getMonth();
        int y = bdate.getNormalizedYear();
        int ml = getMonthLength(y, m);

        if (!(d > 0 && d <= ml)) {
            if (d <= 0 && d > -28) {
                ml = getMonthLength(y, --m);
                d += ml;
                bdate.setDayOfMonth((int) d);
                if (m == 0) {
                    m = DECEMBER;
                    bdate.setNormalizedYear(y - 1);
                }
                bdate.setMonth(m);
            } else if (d > ml && d < (ml + 28)) {
                d -= ml;
                ++m;
                bdate.setDayOfMonth((int)d);
                if (m > DECEMBER) {
                    bdate.setNormalizedYear(y + 1);
                    m = JANUARY;
                }
                bdate.setMonth(m);
            } else {
                long fixedDate = d + getFixedDate(y, m, 1, bdate) - 1L;
                getCalendarDateFromFixedDate(bdate, fixedDate);
            }
        } else {
            bdate.setDayOfWeek(getDayOfWeek(bdate));
        }
        date.setLeapYear(isLeapYear(bdate.getNormalizedYear()));
        date.setZoneOffset(0);
        date.setDaylightSaving(0);
        bdate.setNormalized(true);
        return true;
    }

    void normalizeMonth(CalendarDate date) {
        Date bdate = (Date) date;
        int year = bdate.getNormalizedYear();
        long month = bdate.getMonth();
        if (month <= 0) {
            long xm = 1L - month;
            year -= (int)((xm / 12) + 1);
            month = 13 - (xm % 12);
            bdate.setNormalizedYear(year);
            bdate.setMonth((int) month);
        } else if (month > DECEMBER) {
            year += (int)((month - 1) / 12);
            month = ((month - 1)) % 12 + 1;
            bdate.setNormalizedYear(year);
            bdate.setMonth((int) month);
        }
    }

    /**
     * Returns 366 if the specified date is in a leap year, or 365
     * otherwise This method does not perform the normalization with
     * the specified {@code CalendarDate}. The
     * {@code CalendarDate} must be normalized to get a correct
     * value.
     *
     * @param date a {@code CalendarDate}
     * @return a year length in days
     * @throws ClassCastException if the specified date is not a
     * {@link BaseCalendar.Date}
     */
    public int getYearLength(CalendarDate date) {
        return isLeapYear(((Date)date).getNormalizedYear()) ? 366 : 365;
    }

    public int getYearLengthInMonths(CalendarDate date) {
        return 12;
    }

    static final int[] DAYS_IN_MONTH
        //  12   1   2   3   4   5   6   7   8   9  10  11  12
        = { 31, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    static final int[] ACCUMULATED_DAYS_IN_MONTH
        //  12/1 1/1 2/1 3/1 4/1 5/1 6/1 7/1 8/1 9/1 10/1 11/1 12/1
        = {  -30,  0, 31, 59, 90,120,151,181,212,243, 273, 304, 334};

    static final int[] ACCUMULATED_DAYS_IN_MONTH_LEAP
        //  12/1 1/1 2/1   3/1   4/1   5/1   6/1   7/1   8/1   9/1   10/1   11/1   12/1
        = {  -30,  0, 31, 59+1, 90+1,120+1,151+1,181+1,212+1,243+1, 273+1, 304+1, 334+1};

    public int getMonthLength(CalendarDate date) {
        Date gdate = (Date) date;
        int month = gdate.getMonth();
        if (month < JANUARY || month > DECEMBER) {
            throw new IllegalArgumentException("Illegal month value: " + month);
        }
        return getMonthLength(gdate.getNormalizedYear(), month);
    }

    // accepts 0 (December in the previous year) to 12.
    private int getMonthLength(int year, int month) {
        int days = DAYS_IN_MONTH[month];
        if (month == FEBRUARY && isLeapYear(year)) {
            days++;
        }
        return days;
    }

    public long getDayOfYear(CalendarDate date) {
        return getDayOfYear(((Date)date).getNormalizedYear(),
                            date.getMonth(),
                            date.getDayOfMonth());
    }

    final long getDayOfYear(int year, int month, int dayOfMonth) {
        return (long) dayOfMonth
            + (isLeapYear(year) ?
               ACCUMULATED_DAYS_IN_MONTH_LEAP[month] : ACCUMULATED_DAYS_IN_MONTH[month]);
    }

    // protected
    public long getFixedDate(CalendarDate date) {
        if (!date.isNormalized()) {
            normalizeMonth(date);
        }
        return getFixedDate(((Date)date).getNormalizedYear(),
                            date.getMonth(),
                            date.getDayOfMonth(),
                            (BaseCalendar.Date) date);
    }

    // public for java.util.GregorianCalendar
    public long getFixedDate(int year, int month, int dayOfMonth, BaseCalendar.Date cache) {
        boolean isJan1 = month == JANUARY && dayOfMonth == 1;

        // Look up the one year cache
        if (cache != null && cache.hit(year)) {
            if (isJan1) {
                return cache.getCachedJan1();
            }
            return cache.getCachedJan1() + getDayOfYear(year, month, dayOfMonth) - 1;
        }

        // Look up the pre-calculated fixed date table
        int n = year - BASE_YEAR;
        if (n >= 0 && n < FIXED_DATES.length) {
            long jan1 = FIXED_DATES[n];
            if (cache != null) {
                cache.setCache(year, jan1, isLeapYear(year) ? 366 : 365);
            }
            return isJan1 ? jan1 : jan1 + getDayOfYear(year, month, dayOfMonth) - 1;
        }

        long prevyear = (long)year - 1;
        long days = dayOfMonth;

        if (prevyear >= 0) {
            days += (365 * prevyear)
                   + (prevyear / 4)
                   - (prevyear / 100)
                   + (prevyear / 400)
                   + ((367 * month - 362) / 12);
        } else {
            days += (365 * prevyear)
                   + CalendarUtils.floorDivide(prevyear, 4)
                   - CalendarUtils.floorDivide(prevyear, 100)
                   + CalendarUtils.floorDivide(prevyear, 400)
                   + CalendarUtils.floorDivide((367 * month - 362), 12);
        }

        if (month > FEBRUARY) {
            days -=  isLeapYear(year) ? 1 : 2;
        }

        // If it's January 1, update the cache.
        if (cache != null && isJan1) {
            cache.setCache(year, days, isLeapYear(year) ? 366 : 365);
        }

        return days;
    }

    /**
     * Calculates calendar fields and store them in the specified
     * {@code CalendarDate}.
     */
    // should be 'protected'
    public void getCalendarDateFromFixedDate(CalendarDate date,
                                             long fixedDate) {
        Date gdate = (Date) date;
        int year;
        long jan1;
        boolean isLeap;
        if (gdate.hit(fixedDate)) {
            year = gdate.getCachedYear();
            jan1 = gdate.getCachedJan1();
            isLeap = isLeapYear(year);
        } else {
            // Looking up FIXED_DATES[] here didn't improve performance
            // much. So we calculate year and jan1. getFixedDate()
            // will look up FIXED_DATES[] actually.
            year = getGregorianYearFromFixedDate(fixedDate);
            jan1 = getFixedDate(year, JANUARY, 1, null);
            isLeap = isLeapYear(year);
            // Update the cache data
            gdate.setCache (year, jan1, isLeap ? 366 : 365);
        }

        int priorDays = (int)(fixedDate - jan1);
        long mar1 = jan1 + 31 + 28;
        if (isLeap) {
            ++mar1;
        }
        if (fixedDate >= mar1) {
            priorDays += isLeap ? 1 : 2;
        }
        int month = 12 * priorDays + 373;
        if (month > 0) {
            month /= 367;
        } else {
            month = CalendarUtils.floorDivide(month, 367);
        }
        long month1 = jan1 + ACCUMULATED_DAYS_IN_MONTH[month];
        if (isLeap && month >= MARCH) {
            ++month1;
        }
        int dayOfMonth = (int)(fixedDate - month1) + 1;
        int dayOfWeek = getDayOfWeekFromFixedDate(fixedDate);
        assert dayOfWeek > 0 : "negative day of week " + dayOfWeek;
        gdate.setNormalizedYear(year);
        gdate.setMonth(month);
        gdate.setDayOfMonth(dayOfMonth);
        gdate.setDayOfWeek(dayOfWeek);
        gdate.setLeapYear(isLeap);
        gdate.setNormalized(true);
    }

    /**
     * Returns the day of week of the given Gregorian date.
     */
    public int getDayOfWeek(CalendarDate date) {
        long fixedDate = getFixedDate(date);
        return getDayOfWeekFromFixedDate(fixedDate);
    }

    public static final int getDayOfWeekFromFixedDate(long fixedDate) {
        // The fixed day 1 (January 1, 1 Gregorian) is Monday.
        if (fixedDate >= 0) {
            return (int)(fixedDate % 7) + SUNDAY;
        }
        return (int)CalendarUtils.mod(fixedDate, 7) + SUNDAY;
    }

    public int getYearFromFixedDate(long fixedDate) {
        return getGregorianYearFromFixedDate(fixedDate);
    }

    /**
     * Returns the Gregorian year number of the given fixed date.
     */
    final int getGregorianYearFromFixedDate(long fixedDate) {
        long d0;
        int  d1, d2, d3, d4;
        int  n400, n100, n4, n1;
        int  year;

        if (fixedDate > 0) {
            d0 = fixedDate - 1;
            n400 = (int)(d0 / 146097);
            d1 = (int)(d0 % 146097);
            n100 = d1 / 36524;
            d2 = d1 % 36524;
            n4 = d2 / 1461;
            d3 = d2 % 1461;
            n1 = d3 / 365;
            d4 = (d3 % 365) + 1;
        } else {
            d0 = fixedDate - 1;
            n400 = (int)CalendarUtils.floorDivide(d0, 146097L);
            d1 = (int)CalendarUtils.mod(d0, 146097L);
            n100 = CalendarUtils.floorDivide(d1, 36524);
            d2 = CalendarUtils.mod(d1, 36524);
            n4 = CalendarUtils.floorDivide(d2, 1461);
            d3 = CalendarUtils.mod(d2, 1461);
            n1 = CalendarUtils.floorDivide(d3, 365);
            d4 = CalendarUtils.mod(d3, 365) + 1;
        }
        year = 400 * n400 + 100 * n100 + 4 * n4 + n1;
        if (!(n100 == 4 || n1 == 4)) {
            ++year;
        }
        return year;
    }

    /**
     * @return true if the specified year is a Gregorian leap year, or
     * false otherwise.
     * @see BaseCalendar#isGregorianLeapYear
     */
    protected boolean isLeapYear(CalendarDate date) {
        return isLeapYear(((Date)date).getNormalizedYear());
    }

    boolean isLeapYear(int normalizedYear) {
        return CalendarUtils.isGregorianLeapYear(normalizedYear);
    }
}
