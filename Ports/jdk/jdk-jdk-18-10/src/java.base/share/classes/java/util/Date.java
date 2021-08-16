/*
 * Copyright (c) 1994, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.text.DateFormat;
import java.time.LocalDate;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.lang.ref.SoftReference;
import java.time.Instant;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;
import sun.util.calendar.Era;
import sun.util.calendar.Gregorian;
import sun.util.calendar.ZoneInfo;

/**
 * The class {@code Date} represents a specific instant
 * in time, with millisecond precision.
 * <p>
 * Prior to JDK&nbsp;1.1, the class {@code Date} had two additional
 * functions.  It allowed the interpretation of dates as year, month, day, hour,
 * minute, and second values.  It also allowed the formatting and parsing
 * of date strings.  Unfortunately, the API for these functions was not
 * amenable to internationalization.  As of JDK&nbsp;1.1, the
 * {@code Calendar} class should be used to convert between dates and time
 * fields and the {@code DateFormat} class should be used to format and
 * parse date strings.
 * The corresponding methods in {@code Date} are deprecated.
 * <p>
 * Although the {@code Date} class is intended to reflect
 * coordinated universal time (UTC), it may not do so exactly,
 * depending on the host environment of the Java Virtual Machine.
 * Nearly all modern operating systems assume that 1&nbsp;day&nbsp;=
 * 24&nbsp;&times;&nbsp;60&nbsp;&times;&nbsp;60&nbsp;= 86400 seconds
 * in all cases. In UTC, however, about once every year or two there
 * is an extra second, called a "leap second." The leap
 * second is always added as the last second of the day, and always
 * on December 31 or June 30. For example, the last minute of the
 * year 1995 was 61 seconds long, thanks to an added leap second.
 * Most computer clocks are not accurate enough to be able to reflect
 * the leap-second distinction.
 * <p>
 * Some computer standards are defined in terms of Greenwich mean
 * time (GMT), which is equivalent to universal time (UT).  GMT is
 * the "civil" name for the standard; UT is the
 * "scientific" name for the same standard. The
 * distinction between UTC and UT is that UTC is based on an atomic
 * clock and UT is based on astronomical observations, which for all
 * practical purposes is an invisibly fine hair to split. Because the
 * earth's rotation is not uniform (it slows down and speeds up
 * in complicated ways), UT does not always flow uniformly. Leap
 * seconds are introduced as needed into UTC so as to keep UTC within
 * 0.9 seconds of UT1, which is a version of UT with certain
 * corrections applied. There are other time and date systems as
 * well; for example, the time scale used by the satellite-based
 * global positioning system (GPS) is synchronized to UTC but is
 * <i>not</i> adjusted for leap seconds. An interesting source of
 * further information is the United States Naval Observatory (USNO):
 * <blockquote><pre>
 *     <a href="https://www.usno.navy.mil/USNO">https://www.usno.navy.mil/USNO</a>
 * </pre></blockquote>
 * <p>
 * and the material regarding "Systems of Time" at:
 * <blockquote><pre>
 *     <a href="https://www.usno.navy.mil/USNO/time/master-clock/systems-of-time">https://www.usno.navy.mil/USNO/time/master-clock/systems-of-time</a>
 * </pre></blockquote>
 * <p>
 * which has descriptions of various different time systems including
 * UT, UT1, and UTC.
 * <p>
 * In all methods of class {@code Date} that accept or return
 * year, month, date, hours, minutes, and seconds values, the
 * following representations are used:
 * <ul>
 * <li>A year <i>y</i> is represented by the integer
 *     <i>y</i>&nbsp;{@code - 1900}.
 * <li>A month is represented by an integer from 0 to 11; 0 is January,
 *     1 is February, and so forth; thus 11 is December.
 * <li>A date (day of month) is represented by an integer from 1 to 31
 *     in the usual manner.
 * <li>An hour is represented by an integer from 0 to 23. Thus, the hour
 *     from midnight to 1 a.m. is hour 0, and the hour from noon to 1
 *     p.m. is hour 12.
 * <li>A minute is represented by an integer from 0 to 59 in the usual manner.
 * <li>A second is represented by an integer from 0 to 61; the values 60 and
 *     61 occur only for leap seconds and even then only in Java
 *     implementations that actually track leap seconds correctly. Because
 *     of the manner in which leap seconds are currently introduced, it is
 *     extremely unlikely that two leap seconds will occur in the same
 *     minute, but this specification follows the date and time conventions
 *     for ISO C.
 * </ul>
 * <p>
 * In all cases, arguments given to methods for these purposes need
 * not fall within the indicated ranges; for example, a date may be
 * specified as January 32 and is interpreted as meaning February 1.
 *
 * @author  James Gosling
 * @author  Arthur van Hoff
 * @author  Alan Liu
 * @see     java.text.DateFormat
 * @see     java.util.Calendar
 * @see     java.util.TimeZone
 * @since   1.0
 */
public class Date
    implements java.io.Serializable, Cloneable, Comparable<Date>
{
    private static final BaseCalendar gcal =
                                CalendarSystem.getGregorianCalendar();
    private static BaseCalendar jcal;

    private transient long fastTime;

    /*
     * If cdate is null, then fastTime indicates the time in millis.
     * If cdate.isNormalized() is true, then fastTime and cdate are in
     * synch. Otherwise, fastTime is ignored, and cdate indicates the
     * time.
     */
    private transient BaseCalendar.Date cdate;

    // Initialized just before the value is used. See parse().
    private static int defaultCenturyStart;

    /* use serialVersionUID from modified java.util.Date for
     * interoperability with JDK1.1. The Date was modified to write
     * and read only the UTC time.
     */
    @java.io.Serial
    private static final long serialVersionUID = 7523967970034938905L;

    /**
     * Allocates a {@code Date} object and initializes it so that
     * it represents the time at which it was allocated, measured to the
     * nearest millisecond.
     *
     * @see     java.lang.System#currentTimeMillis()
     */
    public Date() {
        this(System.currentTimeMillis());
    }

    /**
     * Allocates a {@code Date} object and initializes it to
     * represent the specified number of milliseconds since the
     * standard base time known as "the epoch", namely January 1,
     * 1970, 00:00:00 GMT.
     *
     * @param   date   the milliseconds since January 1, 1970, 00:00:00 GMT.
     * @see     java.lang.System#currentTimeMillis()
     */
    public Date(long date) {
        fastTime = date;
    }

    /**
     * Allocates a {@code Date} object and initializes it so that
     * it represents midnight, local time, at the beginning of the day
     * specified by the {@code year}, {@code month}, and
     * {@code date} arguments.
     *
     * @param   year    the year minus 1900.
     * @param   month   the month between 0-11.
     * @param   date    the day of the month between 1-31.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(year + 1900, month, date)}
     * or {@code GregorianCalendar(year + 1900, month, date)}.
     */
    @Deprecated
    public Date(int year, int month, int date) {
        this(year, month, date, 0, 0, 0);
    }

    /**
     * Allocates a {@code Date} object and initializes it so that
     * it represents the instant at the start of the minute specified by
     * the {@code year}, {@code month}, {@code date},
     * {@code hrs}, and {@code min} arguments, in the local
     * time zone.
     *
     * @param   year    the year minus 1900.
     * @param   month   the month between 0-11.
     * @param   date    the day of the month between 1-31.
     * @param   hrs     the hours between 0-23.
     * @param   min     the minutes between 0-59.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(year + 1900, month, date, hrs, min)}
     * or {@code GregorianCalendar(year + 1900, month, date, hrs, min)}.
     */
    @Deprecated
    public Date(int year, int month, int date, int hrs, int min) {
        this(year, month, date, hrs, min, 0);
    }

    /**
     * Allocates a {@code Date} object and initializes it so that
     * it represents the instant at the start of the second specified
     * by the {@code year}, {@code month}, {@code date},
     * {@code hrs}, {@code min}, and {@code sec} arguments,
     * in the local time zone.
     *
     * @param   year    the year minus 1900.
     * @param   month   the month between 0-11.
     * @param   date    the day of the month between 1-31.
     * @param   hrs     the hours between 0-23.
     * @param   min     the minutes between 0-59.
     * @param   sec     the seconds between 0-59.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(year + 1900, month, date, hrs, min, sec)}
     * or {@code GregorianCalendar(year + 1900, month, date, hrs, min, sec)}.
     */
    @Deprecated
    public Date(int year, int month, int date, int hrs, int min, int sec) {
        int y = year + 1900;
        // month is 0-based. So we have to normalize month to support Long.MAX_VALUE.
        if (month >= 12) {
            y += month / 12;
            month %= 12;
        } else if (month < 0) {
            y += CalendarUtils.floorDivide(month, 12);
            month = CalendarUtils.mod(month, 12);
        }
        BaseCalendar cal = getCalendarSystem(y);
        cdate = (BaseCalendar.Date) cal.newCalendarDate(TimeZone.getDefaultRef());
        cdate.setNormalizedDate(y, month + 1, date).setTimeOfDay(hrs, min, sec, 0);
        getTimeImpl();
        cdate = null;
    }

    /**
     * Allocates a {@code Date} object and initializes it so that
     * it represents the date and time indicated by the string
     * {@code s}, which is interpreted as if by the
     * {@link Date#parse} method.
     *
     * @param   s   a string representation of the date.
     * @see     java.text.DateFormat
     * @see     java.util.Date#parse(java.lang.String)
     * @deprecated As of JDK version 1.1,
     * replaced by {@code DateFormat.parse(String s)}.
     */
    @Deprecated
    public Date(String s) {
        this(parse(s));
    }

    /**
     * Return a copy of this object.
     */
    public Object clone() {
        Date d = null;
        try {
            d = (Date)super.clone();
            if (cdate != null) {
                d.cdate = (BaseCalendar.Date) cdate.clone();
            }
        } catch (CloneNotSupportedException e) {} // Won't happen
        return d;
    }

    /**
     * Determines the date and time based on the arguments. The
     * arguments are interpreted as a year, month, day of the month,
     * hour of the day, minute within the hour, and second within the
     * minute, exactly as for the {@code Date} constructor with six
     * arguments, except that the arguments are interpreted relative
     * to UTC rather than to the local time zone. The time indicated is
     * returned represented as the distance, measured in milliseconds,
     * of that time from the epoch (00:00:00 GMT on January 1, 1970).
     *
     * @param   year    the year minus 1900.
     * @param   month   the month between 0-11.
     * @param   date    the day of the month between 1-31.
     * @param   hrs     the hours between 0-23.
     * @param   min     the minutes between 0-59.
     * @param   sec     the seconds between 0-59.
     * @return  the number of milliseconds since January 1, 1970, 00:00:00 GMT for
     *          the date and time specified by the arguments.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(year + 1900, month, date, hrs, min, sec)}
     * or {@code GregorianCalendar(year + 1900, month, date, hrs, min, sec)}, using a UTC
     * {@code TimeZone}, followed by {@code Calendar.getTime().getTime()}.
     */
    @Deprecated
    public static long UTC(int year, int month, int date,
                           int hrs, int min, int sec) {
        int y = year + 1900;
        // month is 0-based. So we have to normalize month to support Long.MAX_VALUE.
        if (month >= 12) {
            y += month / 12;
            month %= 12;
        } else if (month < 0) {
            y += CalendarUtils.floorDivide(month, 12);
            month = CalendarUtils.mod(month, 12);
        }
        int m = month + 1;
        BaseCalendar cal = getCalendarSystem(y);
        BaseCalendar.Date udate = (BaseCalendar.Date) cal.newCalendarDate(null);
        udate.setNormalizedDate(y, m, date).setTimeOfDay(hrs, min, sec, 0);

        // Use a Date instance to perform normalization. Its fastTime
        // is the UTC value after the normalization.
        Date d = new Date(0);
        d.normalize(udate);
        return d.fastTime;
    }

    /**
     * Attempts to interpret the string {@code s} as a representation
     * of a date and time. If the attempt is successful, the time
     * indicated is returned represented as the distance, measured in
     * milliseconds, of that time from the epoch (00:00:00 GMT on
     * January 1, 1970). If the attempt fails, an
     * {@code IllegalArgumentException} is thrown.
     * <p>
     * It accepts many syntaxes; in particular, it recognizes the IETF
     * standard date syntax: "Sat, 12 Aug 1995 13:30:00 GMT". It also
     * understands the continental U.S. time-zone abbreviations, but for
     * general use, a time-zone offset should be used: "Sat, 12 Aug 1995
     * 13:30:00 GMT+0430" (4 hours, 30 minutes west of the Greenwich
     * meridian). If no time zone is specified, the local time zone is
     * assumed. GMT and UTC are considered equivalent.
     * <p>
     * The string {@code s} is processed from left to right, looking for
     * data of interest. Any material in {@code s} that is within the
     * ASCII parenthesis characters {@code (} and {@code )} is ignored.
     * Parentheses may be nested. Otherwise, the only characters permitted
     * within {@code s} are these ASCII characters:
     * <blockquote><pre>
     * abcdefghijklmnopqrstuvwxyz
     * ABCDEFGHIJKLMNOPQRSTUVWXYZ
     * 0123456789,+-:/</pre></blockquote>
     * and whitespace characters.<p>
     * A consecutive sequence of decimal digits is treated as a decimal
     * number:<ul>
     * <li>If a number is preceded by {@code +} or {@code -} and a year
     *     has already been recognized, then the number is a time-zone
     *     offset. If the number is less than 24, it is an offset measured
     *     in hours. Otherwise, it is regarded as an offset in minutes,
     *     expressed in 24-hour time format without punctuation. A
     *     preceding {@code -} means a westward offset. Time zone offsets
     *     are always relative to UTC (Greenwich). Thus, for example,
     *     {@code -5} occurring in the string would mean "five hours west
     *     of Greenwich" and {@code +0430} would mean "four hours and
     *     thirty minutes east of Greenwich." It is permitted for the
     *     string to specify {@code GMT}, {@code UT}, or {@code UTC}
     *     redundantly-for example, {@code GMT-5} or {@code utc+0430}.
     * <li>The number is regarded as a year number if one of the
     *     following conditions is true:
     * <ul>
     *     <li>The number is equal to or greater than 70 and followed by a
     *         space, comma, slash, or end of string
     *     <li>The number is less than 70, and both a month and a day of
     *         the month have already been recognized</li>
     * </ul>
     *     If the recognized year number is less than 100, it is
     *     interpreted as an abbreviated year relative to a century of
     *     which dates are within 80 years before and 19 years after
     *     the time when the Date class is initialized.
     *     After adjusting the year number, 1900 is subtracted from
     *     it. For example, if the current year is 1999 then years in
     *     the range 19 to 99 are assumed to mean 1919 to 1999, while
     *     years from 0 to 18 are assumed to mean 2000 to 2018.  Note
     *     that this is slightly different from the interpretation of
     *     years less than 100 that is used in {@link java.text.SimpleDateFormat}.
     * <li>If the number is followed by a colon, it is regarded as an hour,
     *     unless an hour has already been recognized, in which case it is
     *     regarded as a minute.
     * <li>If the number is followed by a slash, it is regarded as a month
     *     (it is decreased by 1 to produce a number in the range {@code 0}
     *     to {@code 11}), unless a month has already been recognized, in
     *     which case it is regarded as a day of the month.
     * <li>If the number is followed by whitespace, a comma, a hyphen, or
     *     end of string, then if an hour has been recognized but not a
     *     minute, it is regarded as a minute; otherwise, if a minute has
     *     been recognized but not a second, it is regarded as a second;
     *     otherwise, it is regarded as a day of the month. </ul><p>
     * A consecutive sequence of letters is regarded as a word and treated
     * as follows:<ul>
     * <li>A word that matches {@code AM}, ignoring case, is ignored (but
     *     the parse fails if an hour has not been recognized or is less
     *     than {@code 1} or greater than {@code 12}).
     * <li>A word that matches {@code PM}, ignoring case, adds {@code 12}
     *     to the hour (but the parse fails if an hour has not been
     *     recognized or is less than {@code 1} or greater than {@code 12}).
     * <li>Any word that matches any prefix of {@code SUNDAY, MONDAY, TUESDAY,
     *     WEDNESDAY, THURSDAY, FRIDAY}, or {@code SATURDAY}, ignoring
     *     case, is ignored. For example, {@code sat, Friday, TUE}, and
     *     {@code Thurs} are ignored.
     * <li>Otherwise, any word that matches any prefix of {@code JANUARY,
     *     FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER,
     *     OCTOBER, NOVEMBER}, or {@code DECEMBER}, ignoring case, and
     *     considering them in the order given here, is recognized as
     *     specifying a month and is converted to a number ({@code 0} to
     *     {@code 11}). For example, {@code aug, Sept, april}, and
     *     {@code NOV} are recognized as months. So is {@code Ma}, which
     *     is recognized as {@code MARCH}, not {@code MAY}.
     * <li>Any word that matches {@code GMT, UT}, or {@code UTC}, ignoring
     *     case, is treated as referring to UTC.
     * <li>Any word that matches {@code EST, CST, MST}, or {@code PST},
     *     ignoring case, is recognized as referring to the time zone in
     *     North America that is five, six, seven, or eight hours west of
     *     Greenwich, respectively. Any word that matches {@code EDT, CDT,
     *     MDT}, or {@code PDT}, ignoring case, is recognized as
     *     referring to the same time zone, respectively, during daylight
     *     saving time.</ul><p>
     * Once the entire string s has been scanned, it is converted to a time
     * result in one of two ways. If a time zone or time-zone offset has been
     * recognized, then the year, month, day of month, hour, minute, and
     * second are interpreted in UTC and then the time-zone offset is
     * applied. Otherwise, the year, month, day of month, hour, minute, and
     * second are interpreted in the local time zone.
     *
     * @param   s   a string to be parsed as a date.
     * @return  the number of milliseconds since January 1, 1970, 00:00:00 GMT
     *          represented by the string argument.
     * @see     java.text.DateFormat
     * @deprecated As of JDK version 1.1,
     * replaced by {@code DateFormat.parse(String s)}.
     */
    @Deprecated
    public static long parse(String s) {
        int year = Integer.MIN_VALUE;
        int mon = -1;
        int mday = -1;
        int hour = -1;
        int min = -1;
        int sec = -1;
        int millis = -1;
        int c = -1;
        int i = 0;
        int n = -1;
        int wst = -1;
        int tzoffset = -1;
        int prevc = 0;
    syntax:
        {
            if (s == null)
                break syntax;
            int limit = s.length();
            while (i < limit) {
                c = s.charAt(i);
                i++;
                if (c <= ' ' || c == ',')
                    continue;
                if (c == '(') { // skip comments
                    int depth = 1;
                    while (i < limit) {
                        c = s.charAt(i);
                        i++;
                        if (c == '(') depth++;
                        else if (c == ')')
                            if (--depth <= 0)
                                break;
                    }
                    continue;
                }
                if ('0' <= c && c <= '9') {
                    n = c - '0';
                    while (i < limit && '0' <= (c = s.charAt(i)) && c <= '9') {
                        n = n * 10 + c - '0';
                        i++;
                    }
                    if (prevc == '+' || prevc == '-' && year != Integer.MIN_VALUE) {
                        // timezone offset
                        if (n < 24)
                            n = n * 60; // EG. "GMT-3"
                        else
                            n = n % 100 + n / 100 * 60; // eg "GMT-0430"
                        if (prevc == '+')   // plus means east of GMT
                            n = -n;
                        if (tzoffset != 0 && tzoffset != -1)
                            break syntax;
                        tzoffset = n;
                    } else if (n >= 70)
                        if (year != Integer.MIN_VALUE)
                            break syntax;
                        else if (c <= ' ' || c == ',' || c == '/' || i >= limit)
                            // year = n < 1900 ? n : n - 1900;
                            year = n;
                        else
                            break syntax;
                    else if (c == ':')
                        if (hour < 0)
                            hour = (byte) n;
                        else if (min < 0)
                            min = (byte) n;
                        else
                            break syntax;
                    else if (c == '/')
                        if (mon < 0)
                            mon = (byte) (n - 1);
                        else if (mday < 0)
                            mday = (byte) n;
                        else
                            break syntax;
                    else if (i < limit && c != ',' && c > ' ' && c != '-')
                        break syntax;
                    else if (hour >= 0 && min < 0)
                        min = (byte) n;
                    else if (min >= 0 && sec < 0)
                        sec = (byte) n;
                    else if (mday < 0)
                        mday = (byte) n;
                    // Handle two-digit years < 70 (70-99 handled above).
                    else if (year == Integer.MIN_VALUE && mon >= 0 && mday >= 0)
                        year = n;
                    else
                        break syntax;
                    prevc = 0;
                } else if (c == '/' || c == ':' || c == '+' || c == '-')
                    prevc = c;
                else {
                    int st = i - 1;
                    while (i < limit) {
                        c = s.charAt(i);
                        if (!('A' <= c && c <= 'Z' || 'a' <= c && c <= 'z'))
                            break;
                        i++;
                    }
                    if (i <= st + 1)
                        break syntax;
                    int k;
                    for (k = wtb.length; --k >= 0;)
                        if (wtb[k].regionMatches(true, 0, s, st, i - st)) {
                            int action = ttb[k];
                            if (action != 0) {
                                if (action == 1) {  // pm
                                    if (hour > 12 || hour < 1)
                                        break syntax;
                                    else if (hour < 12)
                                        hour += 12;
                                } else if (action == 14) {  // am
                                    if (hour > 12 || hour < 1)
                                        break syntax;
                                    else if (hour == 12)
                                        hour = 0;
                                } else if (action <= 13) {  // month!
                                    if (mon < 0)
                                        mon = (byte) (action - 2);
                                    else
                                        break syntax;
                                } else {
                                    tzoffset = action - 10000;
                                }
                            }
                            break;
                        }
                    if (k < 0)
                        break syntax;
                    prevc = 0;
                }
            }
            if (year == Integer.MIN_VALUE || mon < 0 || mday < 0)
                break syntax;
            // Parse 2-digit years within the correct default century.
            if (year < 100) {
                synchronized (Date.class) {
                    if (defaultCenturyStart == 0) {
                        defaultCenturyStart = gcal.getCalendarDate().getYear() - 80;
                    }
                }
                year += (defaultCenturyStart / 100) * 100;
                if (year < defaultCenturyStart) year += 100;
            }
            if (sec < 0)
                sec = 0;
            if (min < 0)
                min = 0;
            if (hour < 0)
                hour = 0;
            BaseCalendar cal = getCalendarSystem(year);
            if (tzoffset == -1)  { // no time zone specified, have to use local
                BaseCalendar.Date ldate = (BaseCalendar.Date) cal.newCalendarDate(TimeZone.getDefaultRef());
                ldate.setDate(year, mon + 1, mday);
                ldate.setTimeOfDay(hour, min, sec, 0);
                return cal.getTime(ldate);
            }
            BaseCalendar.Date udate = (BaseCalendar.Date) cal.newCalendarDate(null); // no time zone
            udate.setDate(year, mon + 1, mday);
            udate.setTimeOfDay(hour, min, sec, 0);
            return cal.getTime(udate) + tzoffset * (60 * 1000);
        }
        // syntax error
        throw new IllegalArgumentException();
    }
    private static final String wtb[] = {
        "am", "pm",
        "monday", "tuesday", "wednesday", "thursday", "friday",
        "saturday", "sunday",
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december",
        "gmt", "ut", "utc", "est", "edt", "cst", "cdt",
        "mst", "mdt", "pst", "pdt"
    };
    private static final int ttb[] = {
        14, 1, 0, 0, 0, 0, 0, 0, 0,
        2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        10000 + 0, 10000 + 0, 10000 + 0,    // GMT/UT/UTC
        10000 + 5 * 60, 10000 + 4 * 60,     // EST/EDT
        10000 + 6 * 60, 10000 + 5 * 60,     // CST/CDT
        10000 + 7 * 60, 10000 + 6 * 60,     // MST/MDT
        10000 + 8 * 60, 10000 + 7 * 60      // PST/PDT
    };

    /**
     * Returns a value that is the result of subtracting 1900 from the
     * year that contains or begins with the instant in time represented
     * by this {@code Date} object, as interpreted in the local
     * time zone.
     *
     * @return  the year represented by this date, minus 1900.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.get(Calendar.YEAR) - 1900}.
     */
    @Deprecated
    public int getYear() {
        return normalize().getYear() - 1900;
    }

    /**
     * Sets the year of this {@code Date} object to be the specified
     * value plus 1900. This {@code Date} object is modified so
     * that it represents a point in time within the specified year,
     * with the month, date, hour, minute, and second the same as
     * before, as interpreted in the local time zone. (Of course, if
     * the date was February 29, for example, and the year is set to a
     * non-leap year, then the new date will be treated as if it were
     * on March 1.)
     *
     * @param   year    the year value.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(Calendar.YEAR, year + 1900)}.
     */
    @Deprecated
    public void setYear(int year) {
        getCalendarDate().setNormalizedYear(year + 1900);
    }

    /**
     * Returns a number representing the month that contains or begins
     * with the instant in time represented by this {@code Date} object.
     * The value returned is between {@code 0} and {@code 11},
     * with the value {@code 0} representing January.
     *
     * @return  the month represented by this date.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.get(Calendar.MONTH)}.
     */
    @Deprecated
    public int getMonth() {
        return normalize().getMonth() - 1; // adjust 1-based to 0-based
    }

    /**
     * Sets the month of this date to the specified value. This
     * {@code Date} object is modified so that it represents a point
     * in time within the specified month, with the year, date, hour,
     * minute, and second the same as before, as interpreted in the
     * local time zone. If the date was October 31, for example, and
     * the month is set to June, then the new date will be treated as
     * if it were on July 1, because June has only 30 days.
     *
     * @param   month   the month value between 0-11.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(Calendar.MONTH, int month)}.
     */
    @Deprecated
    public void setMonth(int month) {
        int y = 0;
        if (month >= 12) {
            y = month / 12;
            month %= 12;
        } else if (month < 0) {
            y = CalendarUtils.floorDivide(month, 12);
            month = CalendarUtils.mod(month, 12);
        }
        BaseCalendar.Date d = getCalendarDate();
        if (y != 0) {
            d.setNormalizedYear(d.getNormalizedYear() + y);
        }
        d.setMonth(month + 1); // adjust 0-based to 1-based month numbering
    }

    /**
     * Returns the day of the month represented by this {@code Date} object.
     * The value returned is between {@code 1} and {@code 31}
     * representing the day of the month that contains or begins with the
     * instant in time represented by this {@code Date} object, as
     * interpreted in the local time zone.
     *
     * @return  the day of the month represented by this date.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.get(Calendar.DAY_OF_MONTH)}.
     */
    @Deprecated
    public int getDate() {
        return normalize().getDayOfMonth();
    }

    /**
     * Sets the day of the month of this {@code Date} object to the
     * specified value. This {@code Date} object is modified so that
     * it represents a point in time within the specified day of the
     * month, with the year, month, hour, minute, and second the same
     * as before, as interpreted in the local time zone. If the date
     * was April 30, for example, and the date is set to 31, then it
     * will be treated as if it were on May 1, because April has only
     * 30 days.
     *
     * @param   date   the day of the month value between 1-31.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(Calendar.DAY_OF_MONTH, int date)}.
     */
    @Deprecated
    public void setDate(int date) {
        getCalendarDate().setDayOfMonth(date);
    }

    /**
     * Returns the day of the week represented by this date. The
     * returned value ({@code 0} = Sunday, {@code 1} = Monday,
     * {@code 2} = Tuesday, {@code 3} = Wednesday, {@code 4} =
     * Thursday, {@code 5} = Friday, {@code 6} = Saturday)
     * represents the day of the week that contains or begins with
     * the instant in time represented by this {@code Date} object,
     * as interpreted in the local time zone.
     *
     * @return  the day of the week represented by this date.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.get(Calendar.DAY_OF_WEEK)}.
     */
    @Deprecated
    public int getDay() {
        return normalize().getDayOfWeek() - BaseCalendar.SUNDAY;
    }

    /**
     * Returns the hour represented by this {@code Date} object. The
     * returned value is a number ({@code 0} through {@code 23})
     * representing the hour within the day that contains or begins
     * with the instant in time represented by this {@code Date}
     * object, as interpreted in the local time zone.
     *
     * @return  the hour represented by this date.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.get(Calendar.HOUR_OF_DAY)}.
     */
    @Deprecated
    public int getHours() {
        return normalize().getHours();
    }

    /**
     * Sets the hour of this {@code Date} object to the specified value.
     * This {@code Date} object is modified so that it represents a point
     * in time within the specified hour of the day, with the year, month,
     * date, minute, and second the same as before, as interpreted in the
     * local time zone.
     *
     * @param   hours   the hour value.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(Calendar.HOUR_OF_DAY, int hours)}.
     */
    @Deprecated
    public void setHours(int hours) {
        getCalendarDate().setHours(hours);
    }

    /**
     * Returns the number of minutes past the hour represented by this date,
     * as interpreted in the local time zone.
     * The value returned is between {@code 0} and {@code 59}.
     *
     * @return  the number of minutes past the hour represented by this date.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.get(Calendar.MINUTE)}.
     */
    @Deprecated
    public int getMinutes() {
        return normalize().getMinutes();
    }

    /**
     * Sets the minutes of this {@code Date} object to the specified value.
     * This {@code Date} object is modified so that it represents a point
     * in time within the specified minute of the hour, with the year, month,
     * date, hour, and second the same as before, as interpreted in the
     * local time zone.
     *
     * @param   minutes   the value of the minutes.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(Calendar.MINUTE, int minutes)}.
     */
    @Deprecated
    public void setMinutes(int minutes) {
        getCalendarDate().setMinutes(minutes);
    }

    /**
     * Returns the number of seconds past the minute represented by this date.
     * The value returned is between {@code 0} and {@code 61}. The
     * values {@code 60} and {@code 61} can only occur on those
     * Java Virtual Machines that take leap seconds into account.
     *
     * @return  the number of seconds past the minute represented by this date.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.get(Calendar.SECOND)}.
     */
    @Deprecated
    public int getSeconds() {
        return normalize().getSeconds();
    }

    /**
     * Sets the seconds of this {@code Date} to the specified value.
     * This {@code Date} object is modified so that it represents a
     * point in time within the specified second of the minute, with
     * the year, month, date, hour, and minute the same as before, as
     * interpreted in the local time zone.
     *
     * @param   seconds   the seconds value.
     * @see     java.util.Calendar
     * @deprecated As of JDK version 1.1,
     * replaced by {@code Calendar.set(Calendar.SECOND, int seconds)}.
     */
    @Deprecated
    public void setSeconds(int seconds) {
        getCalendarDate().setSeconds(seconds);
    }

    /**
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
     * represented by this {@code Date} object.
     *
     * @return  the number of milliseconds since January 1, 1970, 00:00:00 GMT
     *          represented by this date.
     */
    public long getTime() {
        return getTimeImpl();
    }

    private final long getTimeImpl() {
        if (cdate != null && !cdate.isNormalized()) {
            normalize();
        }
        return fastTime;
    }

    /**
     * Sets this {@code Date} object to represent a point in time that is
     * {@code time} milliseconds after January 1, 1970 00:00:00 GMT.
     *
     * @param   time   the number of milliseconds.
     */
    public void setTime(long time) {
        fastTime = time;
        cdate = null;
    }

    /**
     * Tests if this date is before the specified date.
     *
     * @param   when   a date.
     * @return  {@code true} if and only if the instant of time
     *            represented by this {@code Date} object is strictly
     *            earlier than the instant represented by {@code when};
     *          {@code false} otherwise.
     * @throws    NullPointerException if {@code when} is null.
     */
    public boolean before(Date when) {
        return getMillisOf(this) < getMillisOf(when);
    }

    /**
     * Tests if this date is after the specified date.
     *
     * @param   when   a date.
     * @return  {@code true} if and only if the instant represented
     *          by this {@code Date} object is strictly later than the
     *          instant represented by {@code when};
     *          {@code false} otherwise.
     * @throws    NullPointerException if {@code when} is null.
     */
    public boolean after(Date when) {
        return getMillisOf(this) > getMillisOf(when);
    }

    /**
     * Compares two dates for equality.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and is a {@code Date} object that
     * represents the same point in time, to the millisecond, as this object.
     * <p>
     * Thus, two {@code Date} objects are equal if and only if the
     * {@code getTime} method returns the same {@code long}
     * value for both.
     *
     * @param   obj   the object to compare with.
     * @return  {@code true} if the objects are the same;
     *          {@code false} otherwise.
     * @see     java.util.Date#getTime()
     */
    public boolean equals(Object obj) {
        return obj instanceof Date && getTime() == ((Date) obj).getTime();
    }

    /**
     * Returns the millisecond value of this {@code Date} object
     * without affecting its internal state.
     */
    static final long getMillisOf(Date date) {
        if (date.getClass() != Date.class) {
            return date.getTime();
        }
        if (date.cdate == null || date.cdate.isNormalized()) {
            return date.fastTime;
        }
        BaseCalendar.Date d = (BaseCalendar.Date) date.cdate.clone();
        return gcal.getTime(d);
    }

    /**
     * Compares two Dates for ordering.
     *
     * @param   anotherDate   the {@code Date} to be compared.
     * @return  the value {@code 0} if the argument Date is equal to
     *          this Date; a value less than {@code 0} if this Date
     *          is before the Date argument; and a value greater than
     *      {@code 0} if this Date is after the Date argument.
     * @since   1.2
     * @throws    NullPointerException if {@code anotherDate} is null.
     */
    public int compareTo(Date anotherDate) {
        long thisTime = getMillisOf(this);
        long anotherTime = getMillisOf(anotherDate);
        return (thisTime<anotherTime ? -1 : (thisTime==anotherTime ? 0 : 1));
    }

    /**
     * Returns a hash code value for this object. The result is the
     * exclusive OR of the two halves of the primitive {@code long}
     * value returned by the {@link Date#getTime}
     * method. That is, the hash code is the value of the expression:
     * <blockquote><pre>{@code
     * (int)(this.getTime()^(this.getTime() >>> 32))
     * }</pre></blockquote>
     *
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        long ht = this.getTime();
        return (int) ht ^ (int) (ht >> 32);
    }

    /**
     * Converts this {@code Date} object to a {@code String}
     * of the form:
     * <blockquote><pre>
     * dow mon dd hh:mm:ss zzz yyyy</pre></blockquote>
     * where:<ul>
     * <li>{@code dow} is the day of the week ({@code Sun, Mon, Tue, Wed,
     *     Thu, Fri, Sat}).
     * <li>{@code mon} is the month ({@code Jan, Feb, Mar, Apr, May, Jun,
     *     Jul, Aug, Sep, Oct, Nov, Dec}).
     * <li>{@code dd} is the day of the month ({@code 01} through
     *     {@code 31}), as two decimal digits.
     * <li>{@code hh} is the hour of the day ({@code 00} through
     *     {@code 23}), as two decimal digits.
     * <li>{@code mm} is the minute within the hour ({@code 00} through
     *     {@code 59}), as two decimal digits.
     * <li>{@code ss} is the second within the minute ({@code 00} through
     *     {@code 61}, as two decimal digits.
     * <li>{@code zzz} is the time zone (and may reflect daylight saving
     *     time). Standard time zone abbreviations include those
     *     recognized by the method {@code parse}. If time zone
     *     information is not available, then {@code zzz} is empty -
     *     that is, it consists of no characters at all.
     * <li>{@code yyyy} is the year, as four decimal digits.
     * </ul>
     *
     * @return  a string representation of this date.
     * @see     java.util.Date#toLocaleString()
     * @see     java.util.Date#toGMTString()
     */
    public String toString() {
        // "EEE MMM dd HH:mm:ss zzz yyyy";
        BaseCalendar.Date date = normalize();
        StringBuilder sb = new StringBuilder(28);
        int index = date.getDayOfWeek();
        if (index == BaseCalendar.SUNDAY) {
            index = 8;
        }
        convertToAbbr(sb, wtb[index]).append(' ');                        // EEE
        convertToAbbr(sb, wtb[date.getMonth() - 1 + 2 + 7]).append(' ');  // MMM
        CalendarUtils.sprintf0d(sb, date.getDayOfMonth(), 2).append(' '); // dd

        CalendarUtils.sprintf0d(sb, date.getHours(), 2).append(':');   // HH
        CalendarUtils.sprintf0d(sb, date.getMinutes(), 2).append(':'); // mm
        CalendarUtils.sprintf0d(sb, date.getSeconds(), 2).append(' '); // ss
        TimeZone zi = date.getZone();
        if (zi != null) {
            sb.append(zi.getDisplayName(date.isDaylightTime(), TimeZone.SHORT, Locale.US)); // zzz
        } else {
            sb.append("GMT");
        }
        sb.append(' ').append(date.getYear());  // yyyy
        return sb.toString();
    }

    /**
     * Converts the given name to its 3-letter abbreviation (e.g.,
     * "monday" -> "Mon") and stored the abbreviation in the given
     * {@code StringBuilder}.
     */
    private static final StringBuilder convertToAbbr(StringBuilder sb, String name) {
        sb.append(Character.toUpperCase(name.charAt(0)));
        sb.append(name.charAt(1)).append(name.charAt(2));
        return sb;
    }

    /**
     * Creates a string representation of this {@code Date} object in an
     * implementation-dependent form. The intent is that the form should
     * be familiar to the user of the Java application, wherever it may
     * happen to be running. The intent is comparable to that of the
     * "{@code %c}" format supported by the {@code strftime()}
     * function of ISO&nbsp;C.
     *
     * @return  a string representation of this date, using the locale
     *          conventions.
     * @see     java.text.DateFormat
     * @see     java.util.Date#toString()
     * @see     java.util.Date#toGMTString()
     * @deprecated As of JDK version 1.1,
     * replaced by {@code DateFormat.format(Date date)}.
     */
    @Deprecated
    public String toLocaleString() {
        DateFormat formatter = DateFormat.getDateTimeInstance();
        return formatter.format(this);
    }

    /**
     * Creates a string representation of this {@code Date} object of
     * the form:
     * <blockquote><pre>
     * d mon yyyy hh:mm:ss GMT</pre></blockquote>
     * where:<ul>
     * <li><i>d</i> is the day of the month ({@code 1} through {@code 31}),
     *     as one or two decimal digits.
     * <li><i>mon</i> is the month ({@code Jan, Feb, Mar, Apr, May, Jun, Jul,
     *     Aug, Sep, Oct, Nov, Dec}).
     * <li><i>yyyy</i> is the year, as four decimal digits.
     * <li><i>hh</i> is the hour of the day ({@code 00} through {@code 23}),
     *     as two decimal digits.
     * <li><i>mm</i> is the minute within the hour ({@code 00} through
     *     {@code 59}), as two decimal digits.
     * <li><i>ss</i> is the second within the minute ({@code 00} through
     *     {@code 61}), as two decimal digits.
     * <li><i>GMT</i> is exactly the ASCII letters "{@code GMT}" to indicate
     *     Greenwich Mean Time.
     * </ul><p>
     * The result does not depend on the local time zone.
     *
     * @return  a string representation of this date, using the Internet GMT
     *          conventions.
     * @see     java.text.DateFormat
     * @see     java.util.Date#toString()
     * @see     java.util.Date#toLocaleString()
     * @deprecated As of JDK version 1.1,
     * replaced by {@code DateFormat.format(Date date)}, using a
     * GMT {@code TimeZone}.
     */
    @Deprecated
    public String toGMTString() {
        // d MMM yyyy HH:mm:ss 'GMT'
        long t = getTime();
        BaseCalendar cal = getCalendarSystem(t);
        BaseCalendar.Date date =
            (BaseCalendar.Date) cal.getCalendarDate(getTime(), (TimeZone)null);
        StringBuilder sb = new StringBuilder(32);
        CalendarUtils.sprintf0d(sb, date.getDayOfMonth(), 1).append(' '); // d
        convertToAbbr(sb, wtb[date.getMonth() - 1 + 2 + 7]).append(' ');  // MMM
        sb.append(date.getYear()).append(' ');                            // yyyy
        CalendarUtils.sprintf0d(sb, date.getHours(), 2).append(':');      // HH
        CalendarUtils.sprintf0d(sb, date.getMinutes(), 2).append(':');    // mm
        CalendarUtils.sprintf0d(sb, date.getSeconds(), 2);                // ss
        sb.append(" GMT");                                                // ' GMT'
        return sb.toString();
    }

    /**
     * Returns the offset, measured in minutes, for the local time zone
     * relative to UTC that is appropriate for the time represented by
     * this {@code Date} object.
     * <p>
     * For example, in Massachusetts, five time zones west of Greenwich:
     * <blockquote><pre>
     * new Date(96, 1, 14).getTimezoneOffset() returns 300</pre></blockquote>
     * because on February 14, 1996, standard time (Eastern Standard Time)
     * is in use, which is offset five hours from UTC; but:
     * <blockquote><pre>
     * new Date(96, 5, 1).getTimezoneOffset() returns 240</pre></blockquote>
     * because on June 1, 1996, daylight saving time (Eastern Daylight Time)
     * is in use, which is offset only four hours from UTC.<p>
     * This method produces the same result as if it computed:
     * <blockquote><pre>
     * (this.getTime() - UTC(this.getYear(),
     *                       this.getMonth(),
     *                       this.getDate(),
     *                       this.getHours(),
     *                       this.getMinutes(),
     *                       this.getSeconds())) / (60 * 1000)
     * </pre></blockquote>
     *
     * @return  the time-zone offset, in minutes, for the current time zone.
     * @see     java.util.Calendar#ZONE_OFFSET
     * @see     java.util.Calendar#DST_OFFSET
     * @see     java.util.TimeZone#getDefault
     * @deprecated As of JDK version 1.1,
     * replaced by {@code -(Calendar.get(Calendar.ZONE_OFFSET) +
     * Calendar.get(Calendar.DST_OFFSET)) / (60 * 1000)}.
     */
    @Deprecated
    public int getTimezoneOffset() {
        int zoneOffset;
        if (cdate == null) {
            TimeZone tz = TimeZone.getDefaultRef();
            if (tz instanceof ZoneInfo) {
                zoneOffset = ((ZoneInfo)tz).getOffsets(fastTime, null);
            } else {
                zoneOffset = tz.getOffset(fastTime);
            }
        } else {
            normalize();
            zoneOffset = cdate.getZoneOffset();
        }
        return -zoneOffset/60000;  // convert to minutes
    }

    private final BaseCalendar.Date getCalendarDate() {
        if (cdate == null) {
            BaseCalendar cal = getCalendarSystem(fastTime);
            cdate = (BaseCalendar.Date) cal.getCalendarDate(fastTime,
                                                            TimeZone.getDefaultRef());
        }
        return cdate;
    }

    private final BaseCalendar.Date normalize() {
        if (cdate == null) {
            BaseCalendar cal = getCalendarSystem(fastTime);
            cdate = (BaseCalendar.Date) cal.getCalendarDate(fastTime,
                                                            TimeZone.getDefaultRef());
            return cdate;
        }

        // Normalize cdate with the TimeZone in cdate first. This is
        // required for the compatible behavior.
        if (!cdate.isNormalized()) {
            cdate = normalize(cdate);
        }

        // If the default TimeZone has changed, then recalculate the
        // fields with the new TimeZone.
        TimeZone tz = TimeZone.getDefaultRef();
        if (tz != cdate.getZone()) {
            cdate.setZone(tz);
            CalendarSystem cal = getCalendarSystem(cdate);
            cal.getCalendarDate(fastTime, cdate);
        }
        return cdate;
    }

    // fastTime and the returned data are in sync upon return.
    private final BaseCalendar.Date normalize(BaseCalendar.Date date) {
        int y = date.getNormalizedYear();
        int m = date.getMonth();
        int d = date.getDayOfMonth();
        int hh = date.getHours();
        int mm = date.getMinutes();
        int ss = date.getSeconds();
        int ms = date.getMillis();
        TimeZone tz = date.getZone();

        // If the specified year can't be handled using a long value
        // in milliseconds, GregorianCalendar is used for full
        // compatibility with underflow and overflow. This is required
        // by some JCK tests. The limits are based max year values -
        // years that can be represented by max values of d, hh, mm,
        // ss and ms. Also, let GregorianCalendar handle the default
        // cutover year so that we don't need to worry about the
        // transition here.
        if (y == 1582 || y > 280000000 || y < -280000000) {
            if (tz == null) {
                tz = TimeZone.getTimeZone("GMT");
            }
            GregorianCalendar gc = new GregorianCalendar(tz);
            gc.clear();
            gc.set(GregorianCalendar.MILLISECOND, ms);
            gc.set(y, m-1, d, hh, mm, ss);
            fastTime = gc.getTimeInMillis();
            BaseCalendar cal = getCalendarSystem(fastTime);
            date = (BaseCalendar.Date) cal.getCalendarDate(fastTime, tz);
            return date;
        }

        BaseCalendar cal = getCalendarSystem(y);
        if (cal != getCalendarSystem(date)) {
            date = (BaseCalendar.Date) cal.newCalendarDate(tz);
            date.setNormalizedDate(y, m, d).setTimeOfDay(hh, mm, ss, ms);
        }
        // Perform the GregorianCalendar-style normalization.
        fastTime = cal.getTime(date);

        // In case the normalized date requires the other calendar
        // system, we need to recalculate it using the other one.
        BaseCalendar ncal = getCalendarSystem(fastTime);
        if (ncal != cal) {
            date = (BaseCalendar.Date) ncal.newCalendarDate(tz);
            date.setNormalizedDate(y, m, d).setTimeOfDay(hh, mm, ss, ms);
            fastTime = ncal.getTime(date);
        }
        return date;
    }

    /**
     * Returns the Gregorian or Julian calendar system to use with the
     * given date. Use Gregorian from October 15, 1582.
     *
     * @param year normalized calendar year (not -1900)
     * @return the CalendarSystem to use for the specified date
     */
    private static final BaseCalendar getCalendarSystem(int year) {
        if (year >= 1582) {
            return gcal;
        }
        return getJulianCalendar();
    }

    private static final BaseCalendar getCalendarSystem(long utc) {
        // Quickly check if the time stamp given by `utc' is the Epoch
        // or later. If it's before 1970, we convert the cutover to
        // local time to compare.
        if (utc >= 0
            || utc >= GregorianCalendar.DEFAULT_GREGORIAN_CUTOVER
                        - TimeZone.getDefaultRef().getOffset(utc)) {
            return gcal;
        }
        return getJulianCalendar();
    }

    private static final BaseCalendar getCalendarSystem(BaseCalendar.Date cdate) {
        if (jcal == null) {
            return gcal;
        }
        if (cdate.getEra() != null) {
            return jcal;
        }
        return gcal;
    }

    private static final synchronized BaseCalendar getJulianCalendar() {
        if (jcal == null) {
            jcal = (BaseCalendar) CalendarSystem.forName("julian");
        }
        return jcal;
    }

    /**
     * Save the state of this object to a stream (i.e., serialize it).
     *
     * @serialData The value returned by {@code getTime()}
     *             is emitted (long).  This represents the offset from
     *             January 1, 1970, 00:00:00 GMT in milliseconds.
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream s)
         throws IOException
    {
        s.defaultWriteObject();
        s.writeLong(getTimeImpl());
    }

    /**
     * Reconstitute this object from a stream (i.e., deserialize it).
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        fastTime = s.readLong();
    }

    /**
     * Obtains an instance of {@code Date} from an {@code Instant} object.
     * <p>
     * {@code Instant} uses a precision of nanoseconds, whereas {@code Date}
     * uses a precision of milliseconds.  The conversion will truncate any
     * excess precision information as though the amount in nanoseconds was
     * subject to integer division by one million.
     * <p>
     * {@code Instant} can store points on the time-line further in the future
     * and further in the past than {@code Date}. In this scenario, this method
     * will throw an exception.
     *
     * @param instant  the instant to convert
     * @return a {@code Date} representing the same point on the time-line as
     *  the provided instant
     * @throws    NullPointerException if {@code instant} is null.
     * @throws    IllegalArgumentException if the instant is too large to
     *  represent as a {@code Date}
     * @since 1.8
     */
    public static Date from(Instant instant) {
        try {
            return new Date(instant.toEpochMilli());
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Converts this {@code Date} object to an {@code Instant}.
     * <p>
     * The conversion creates an {@code Instant} that represents the same
     * point on the time-line as this {@code Date}.
     *
     * @return an instant representing the same point on the time-line as
     *  this {@code Date} object
     * @since 1.8
     */
    public Instant toInstant() {
        return Instant.ofEpochMilli(getTime());
    }
}
