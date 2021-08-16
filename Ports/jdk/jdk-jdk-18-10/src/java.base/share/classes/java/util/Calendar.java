/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996-1998 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996-1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.util.BuddhistCalendar;
import sun.util.calendar.ZoneInfo;
import sun.util.locale.provider.CalendarDataUtility;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.TimeZoneNameUtility;
import sun.util.spi.CalendarProvider;

/**
 * The {@code Calendar} class is an abstract class that provides methods
 * for converting between a specific instant in time and a set of {@link
 * #fields calendar fields} such as {@code YEAR}, {@code MONTH},
 * {@code DAY_OF_MONTH}, {@code HOUR}, and so on, and for
 * manipulating the calendar fields, such as getting the date of the next
 * week. An instant in time can be represented by a millisecond value that is
 * an offset from the <a id="Epoch"><em>Epoch</em></a>, January 1, 1970
 * 00:00:00.000 GMT (Gregorian).
 *
 * <p>The class also provides additional fields and methods for
 * implementing a concrete calendar system outside the package. Those
 * fields and methods are defined as {@code protected}.
 *
 * <p>
 * Like other locale-sensitive classes, {@code Calendar} provides a
 * class method, {@code getInstance}, for getting a generally useful
 * object of this type. {@code Calendar}'s {@code getInstance} method
 * returns a {@code Calendar} object whose
 * calendar fields have been initialized with the current date and time:
 * <blockquote>
 * <pre>
 *     Calendar rightNow = Calendar.getInstance();
 * </pre>
 * </blockquote>
 *
 * <p>A {@code Calendar} object can produce all the calendar field values
 * needed to implement the date-time formatting for a particular language and
 * calendar style (for example, Japanese-Gregorian, Japanese-Traditional).
 * {@code Calendar} defines the range of values returned by
 * certain calendar fields, as well as their meaning.  For example,
 * the first month of the calendar system has value <code>MONTH ==
 * JANUARY</code> for all calendars.  Other values are defined by the
 * concrete subclass, such as {@code ERA}.  See individual field
 * documentation and subclass documentation for details.
 *
 * <h2>Getting and Setting Calendar Field Values</h2>
 *
 * <p>The calendar field values can be set by calling the {@code set}
 * methods. Any field values set in a {@code Calendar} will not be
 * interpreted until it needs to calculate its time value (milliseconds from
 * the Epoch) or values of the calendar fields. Calling the
 * {@code get}, {@code getTimeInMillis}, {@code getTime},
 * {@code add} and {@code roll} involves such calculation.
 *
 * <h3>Leniency</h3>
 *
 * <p>{@code Calendar} has two modes for interpreting the calendar
 * fields, <em>lenient</em> and <em>non-lenient</em>.  When a
 * {@code Calendar} is in lenient mode, it accepts a wider range of
 * calendar field values than it produces.  When a {@code Calendar}
 * recomputes calendar field values for return by {@code get()}, all of
 * the calendar fields are normalized. For example, a lenient
 * {@code GregorianCalendar} interprets {@code MONTH == JANUARY},
 * {@code DAY_OF_MONTH == 32} as February 1.
 *
 * <p>When a {@code Calendar} is in non-lenient mode, it throws an
 * exception if there is any inconsistency in its calendar fields. For
 * example, a {@code GregorianCalendar} always produces
 * {@code DAY_OF_MONTH} values between 1 and the length of the month. A
 * non-lenient {@code GregorianCalendar} throws an exception upon
 * calculating its time or calendar field values if any out-of-range field
 * value has been set.
 *
 * <h3><a id="first_week">First Week</a></h3>
 *
 * {@code Calendar} defines a locale-specific seven day week using two
 * parameters: the first day of the week and the minimal days in first week
 * (from 1 to 7).  These numbers are taken from the locale resource data or the
 * locale itself when a {@code Calendar} is constructed. If the designated
 * locale contains "fw" and/or "rg" <a href="./Locale.html#def_locale_extension">
 * Unicode extensions</a>, the first day of the week will be obtained according to
 * those extensions. If both "fw" and "rg" are specified, the value from the "fw"
 * extension supersedes the implicit one from the "rg" extension.
 * They may also be specified explicitly through the methods for setting their
 * values.
 *
 * <p>When setting or getting the {@code WEEK_OF_MONTH} or
 * {@code WEEK_OF_YEAR} fields, {@code Calendar} must determine the
 * first week of the month or year as a reference point.  The first week of a
 * month or year is defined as the earliest seven day period beginning on
 * {@code getFirstDayOfWeek()} and containing at least
 * {@code getMinimalDaysInFirstWeek()} days of that month or year.  Weeks
 * numbered ..., -1, 0 precede the first week; weeks numbered 2, 3,... follow
 * it.  Note that the normalized numbering returned by {@code get()} may be
 * different.  For example, a specific {@code Calendar} subclass may
 * designate the week before week 1 of a year as week <code><i>n</i></code> of
 * the previous year.
 *
 * <h3>Calendar Fields Resolution</h3>
 *
 * When computing a date and time from the calendar fields, there
 * may be insufficient information for the computation (such as only
 * year and month with no day of month), or there may be inconsistent
 * information (such as Tuesday, July 15, 1996 (Gregorian) -- July 15,
 * 1996 is actually a Monday). {@code Calendar} will resolve
 * calendar field values to determine the date and time in the
 * following way.
 *
 * <p><a id="resolution">If there is any conflict in calendar field values,
 * {@code Calendar} gives priorities to calendar fields that have been set
 * more recently.</a> The following are the default combinations of the
 * calendar fields. The most recent combination, as determined by the
 * most recently set single field, will be used.
 *
 * <p><a id="date_resolution">For the date fields</a>:
 * <blockquote>
 * <pre>
 * YEAR + MONTH + DAY_OF_MONTH
 * YEAR + MONTH + WEEK_OF_MONTH + DAY_OF_WEEK
 * YEAR + MONTH + DAY_OF_WEEK_IN_MONTH + DAY_OF_WEEK
 * YEAR + DAY_OF_YEAR
 * YEAR + DAY_OF_WEEK + WEEK_OF_YEAR
 * </pre></blockquote>
 *
 * <a id="time_resolution">For the time of day fields</a>:
 * <blockquote>
 * <pre>
 * HOUR_OF_DAY
 * AM_PM + HOUR
 * </pre></blockquote>
 *
 * <p>If there are any calendar fields whose values haven't been set in the selected
 * field combination, {@code Calendar} uses their default values. The default
 * value of each field may vary by concrete calendar systems. For example, in
 * {@code GregorianCalendar}, the default of a field is the same as that
 * of the start of the Epoch: i.e., {@code YEAR = 1970}, <code>MONTH =
 * JANUARY</code>, {@code DAY_OF_MONTH = 1}, etc.
 *
 * <p>
 * <strong>Note:</strong> There are certain possible ambiguities in
 * interpretation of certain singular times, which are resolved in the
 * following ways:
 * <ol>
 *     <li> 23:59 is the last minute of the day and 00:00 is the first
 *          minute of the next day. Thus, 23:59 on Dec 31, 1999 &lt; 00:00 on
 *          Jan 1, 2000 &lt; 00:01 on Jan 1, 2000.
 *
 *     <li> Although historically not precise, midnight also belongs to "am",
 *          and noon belongs to "pm", so on the same day,
 *          12:00 am (midnight) &lt; 12:01 am, and 12:00 pm (noon) &lt; 12:01 pm
 * </ol>
 *
 * <p>
 * The date or time format strings are not part of the definition of a
 * calendar, as those must be modifiable or overridable by the user at
 * runtime. Use {@link DateFormat}
 * to format dates.
 *
 * <h3>Field Manipulation</h3>
 *
 * The calendar fields can be changed using three methods:
 * {@code set()}, {@code add()}, and {@code roll()}.
 *
 * <p><strong>{@code set(f, value)}</strong> changes calendar field
 * {@code f} to {@code value}.  In addition, it sets an
 * internal member variable to indicate that calendar field {@code f} has
 * been changed. Although calendar field {@code f} is changed immediately,
 * the calendar's time value in milliseconds is not recomputed until the next call to
 * {@code get()}, {@code getTime()}, {@code getTimeInMillis()},
 * {@code add()}, or {@code roll()} is made. Thus, multiple calls to
 * {@code set()} do not trigger multiple, unnecessary
 * computations. As a result of changing a calendar field using
 * {@code set()}, other calendar fields may also change, depending on the
 * calendar field, the calendar field value, and the calendar system. In addition,
 * {@code get(f)} will not necessarily return {@code value} set by
 * the call to the {@code set} method
 * after the calendar fields have been recomputed. The specifics are determined by
 * the concrete calendar class.</p>
 *
 * <p><em>Example</em>: Consider a {@code GregorianCalendar}
 * originally set to August 31, 1999. Calling <code>set(Calendar.MONTH,
 * Calendar.SEPTEMBER)</code> sets the date to September 31,
 * 1999. This is a temporary internal representation that resolves to
 * October 1, 1999 if {@code getTime()} is then called. However, a
 * call to {@code set(Calendar.DAY_OF_MONTH, 30)} before the call to
 * {@code getTime()} sets the date to September 30, 1999, since
 * no recomputation occurs after {@code set()} itself.</p>
 *
 * <p><strong>{@code add(f, delta)}</strong> adds {@code delta}
 * to field {@code f}.  This is equivalent to calling <code>set(f,
 * get(f) + delta)</code> with two adjustments:</p>
 *
 * <blockquote>
 *   <p><strong>Add rule 1</strong>. The value of field {@code f}
 *   after the call minus the value of field {@code f} before the
 *   call is {@code delta}, modulo any overflow that has occurred in
 *   field {@code f}. Overflow occurs when a field value exceeds its
 *   range and, as a result, the next larger field is incremented or
 *   decremented and the field value is adjusted back into its range.</p>
 *
 *   <p><strong>Add rule 2</strong>. If a smaller field is expected to be
 *   invariant, but it is impossible for it to be equal to its
 *   prior value because of changes in its minimum or maximum after field
 *   {@code f} is changed or other constraints, such as time zone
 *   offset changes, then its value is adjusted to be as close
 *   as possible to its expected value. A smaller field represents a
 *   smaller unit of time. {@code HOUR} is a smaller field than
 *   {@code DAY_OF_MONTH}. No adjustment is made to smaller fields
 *   that are not expected to be invariant. The calendar system
 *   determines what fields are expected to be invariant.</p>
 * </blockquote>
 *
 * <p>In addition, unlike {@code set()}, {@code add()} forces
 * an immediate recomputation of the calendar's milliseconds and all
 * fields.</p>
 *
 * <p><em>Example</em>: Consider a {@code GregorianCalendar}
 * originally set to August 31, 1999. Calling <code>add(Calendar.MONTH,
 * 13)</code> sets the calendar to September 30, 2000. <strong>Add rule
 * 1</strong> sets the {@code MONTH} field to September, since
 * adding 13 months to August gives September of the next year. Since
 * {@code DAY_OF_MONTH} cannot be 31 in September in a
 * {@code GregorianCalendar}, <strong>add rule 2</strong> sets the
 * {@code DAY_OF_MONTH} to 30, the closest possible value. Although
 * it is a smaller field, {@code DAY_OF_WEEK} is not adjusted by
 * rule 2, since it is expected to change when the month changes in a
 * {@code GregorianCalendar}.</p>
 *
 * <p><strong>{@code roll(f, delta)}</strong> adds
 * {@code delta} to field {@code f} without changing larger
 * fields. This is equivalent to calling {@code add(f, delta)} with
 * the following adjustment:</p>
 *
 * <blockquote>
 *   <p><strong>Roll rule</strong>. Larger fields are unchanged after the
 *   call. A larger field represents a larger unit of
 *   time. {@code DAY_OF_MONTH} is a larger field than
 *   {@code HOUR}.</p>
 * </blockquote>
 *
 * <p><em>Example</em>: See {@link java.util.GregorianCalendar#roll(int, int)}.
 *
 * <p><strong>Usage model</strong>. To motivate the behavior of
 * {@code add()} and {@code roll()}, consider a user interface
 * component with increment and decrement buttons for the month, day, and
 * year, and an underlying {@code GregorianCalendar}. If the
 * interface reads January 31, 1999 and the user presses the month
 * increment button, what should it read? If the underlying
 * implementation uses {@code set()}, it might read March 3, 1999. A
 * better result would be February 28, 1999. Furthermore, if the user
 * presses the month increment button again, it should read March 31,
 * 1999, not March 28, 1999. By saving the original date and using either
 * {@code add()} or {@code roll()}, depending on whether larger
 * fields should be affected, the user interface can behave as most users
 * will intuitively expect.</p>
 *
 * @see          java.lang.System#currentTimeMillis()
 * @see          Date
 * @see          GregorianCalendar
 * @see          TimeZone
 * @see          java.text.DateFormat
 * @author Mark Davis, David Goldsmith, Chen-Lieh Huang, Alan Liu
 * @since 1.1
 */
public abstract class Calendar implements Serializable, Cloneable, Comparable<Calendar> {

    // Data flow in Calendar
    // ---------------------

    // The current time is represented in two ways by Calendar: as UTC
    // milliseconds from the epoch (1 January 1970 0:00 UTC), and as local
    // fields such as MONTH, HOUR, AM_PM, etc.  It is possible to compute the
    // millis from the fields, and vice versa.  The data needed to do this
    // conversion is encapsulated by a TimeZone object owned by the Calendar.
    // The data provided by the TimeZone object may also be overridden if the
    // user sets the ZONE_OFFSET and/or DST_OFFSET fields directly. The class
    // keeps track of what information was most recently set by the caller, and
    // uses that to compute any other information as needed.

    // If the user sets the fields using set(), the data flow is as follows.
    // This is implemented by the Calendar subclass's computeTime() method.
    // During this process, certain fields may be ignored.  The disambiguation
    // algorithm for resolving which fields to pay attention to is described
    // in the class documentation.

    //   local fields (YEAR, MONTH, DATE, HOUR, MINUTE, etc.)
    //           |
    //           | Using Calendar-specific algorithm
    //           V
    //   local standard millis
    //           |
    //           | Using TimeZone or user-set ZONE_OFFSET / DST_OFFSET
    //           V
    //   UTC millis (in time data member)

    // If the user sets the UTC millis using setTime() or setTimeInMillis(),
    // the data flow is as follows.  This is implemented by the Calendar
    // subclass's computeFields() method.

    //   UTC millis (in time data member)
    //           |
    //           | Using TimeZone getOffset()
    //           V
    //   local standard millis
    //           |
    //           | Using Calendar-specific algorithm
    //           V
    //   local fields (YEAR, MONTH, DATE, HOUR, MINUTE, etc.)

    // In general, a round trip from fields, through local and UTC millis, and
    // back out to fields is made when necessary.  This is implemented by the
    // complete() method.  Resolving a partial set of fields into a UTC millis
    // value allows all remaining fields to be generated from that value.  If
    // the Calendar is lenient, the fields are also renormalized to standard
    // ranges when they are regenerated.

    /**
     * Field number for {@code get} and {@code set} indicating the
     * era, e.g., AD or BC in the Julian calendar. This is a calendar-specific
     * value; see subclass documentation.
     *
     * @see GregorianCalendar#AD
     * @see GregorianCalendar#BC
     */
    public static final int ERA = 0;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * year. This is a calendar-specific value; see subclass documentation.
     */
    public static final int YEAR = 1;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * month. This is a calendar-specific value. The first month of
     * the year in the Gregorian and Julian calendars is
     * {@code JANUARY} which is 0; the last depends on the number
     * of months in a year.
     *
     * @see #JANUARY
     * @see #FEBRUARY
     * @see #MARCH
     * @see #APRIL
     * @see #MAY
     * @see #JUNE
     * @see #JULY
     * @see #AUGUST
     * @see #SEPTEMBER
     * @see #OCTOBER
     * @see #NOVEMBER
     * @see #DECEMBER
     * @see #UNDECIMBER
     */
    public static final int MONTH = 2;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * week number within the current year.  The first week of the year, as
     * defined by {@code getFirstDayOfWeek()} and
     * {@code getMinimalDaysInFirstWeek()}, has value 1.  Subclasses define
     * the value of {@code WEEK_OF_YEAR} for days before the first week of
     * the year.
     *
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    public static final int WEEK_OF_YEAR = 3;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * week number within the current month.  The first week of the month, as
     * defined by {@code getFirstDayOfWeek()} and
     * {@code getMinimalDaysInFirstWeek()}, has value 1.  Subclasses define
     * the value of {@code WEEK_OF_MONTH} for days before the first week of
     * the month.
     *
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    public static final int WEEK_OF_MONTH = 4;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * day of the month. This is a synonym for {@code DAY_OF_MONTH}.
     * The first day of the month has value 1.
     *
     * @see #DAY_OF_MONTH
     */
    public static final int DATE = 5;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * day of the month. This is a synonym for {@code DATE}.
     * The first day of the month has value 1.
     *
     * @see #DATE
     */
    public static final int DAY_OF_MONTH = 5;

    /**
     * Field number for {@code get} and {@code set} indicating the day
     * number within the current year.  The first day of the year has value 1.
     */
    public static final int DAY_OF_YEAR = 6;

    /**
     * Field number for {@code get} and {@code set} indicating the day
     * of the week.  This field takes values {@code SUNDAY},
     * {@code MONDAY}, {@code TUESDAY}, {@code WEDNESDAY},
     * {@code THURSDAY}, {@code FRIDAY}, and {@code SATURDAY}.
     *
     * @see #SUNDAY
     * @see #MONDAY
     * @see #TUESDAY
     * @see #WEDNESDAY
     * @see #THURSDAY
     * @see #FRIDAY
     * @see #SATURDAY
     */
    public static final int DAY_OF_WEEK = 7;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * ordinal number of the day of the week within the current month. Together
     * with the {@code DAY_OF_WEEK} field, this uniquely specifies a day
     * within a month.  Unlike {@code WEEK_OF_MONTH} and
     * {@code WEEK_OF_YEAR}, this field's value does <em>not</em> depend on
     * {@code getFirstDayOfWeek()} or
     * {@code getMinimalDaysInFirstWeek()}.  {@code DAY_OF_MONTH 1}
     * through {@code 7} always correspond to <code>DAY_OF_WEEK_IN_MONTH
     * 1</code>; {@code 8} through {@code 14} correspond to
     * {@code DAY_OF_WEEK_IN_MONTH 2}, and so on.
     * {@code DAY_OF_WEEK_IN_MONTH 0} indicates the week before
     * {@code DAY_OF_WEEK_IN_MONTH 1}.  Negative values count back from the
     * end of the month, so the last Sunday of a month is specified as
     * {@code DAY_OF_WEEK = SUNDAY, DAY_OF_WEEK_IN_MONTH = -1}.  Because
     * negative values count backward they will usually be aligned differently
     * within the month than positive values.  For example, if a month has 31
     * days, {@code DAY_OF_WEEK_IN_MONTH -1} will overlap
     * {@code DAY_OF_WEEK_IN_MONTH 5} and the end of {@code 4}.
     *
     * @see #DAY_OF_WEEK
     * @see #WEEK_OF_MONTH
     */
    public static final int DAY_OF_WEEK_IN_MONTH = 8;

    /**
     * Field number for {@code get} and {@code set} indicating
     * whether the {@code HOUR} is before or after noon.
     * E.g., at 10:04:15.250 PM the {@code AM_PM} is {@code PM}.
     *
     * @see #AM
     * @see #PM
     * @see #HOUR
     */
    public static final int AM_PM = 9;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * hour of the morning or afternoon. {@code HOUR} is used for the
     * 12-hour clock (0 - 11). Noon and midnight are represented by 0, not by 12.
     * E.g., at 10:04:15.250 PM the {@code HOUR} is 10.
     *
     * @see #AM_PM
     * @see #HOUR_OF_DAY
     */
    public static final int HOUR = 10;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * hour of the day. {@code HOUR_OF_DAY} is used for the 24-hour clock.
     * E.g., at 10:04:15.250 PM the {@code HOUR_OF_DAY} is 22.
     *
     * @see #HOUR
     */
    public static final int HOUR_OF_DAY = 11;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * minute within the hour.
     * E.g., at 10:04:15.250 PM the {@code MINUTE} is 4.
     */
    public static final int MINUTE = 12;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * second within the minute.
     * E.g., at 10:04:15.250 PM the {@code SECOND} is 15.
     */
    public static final int SECOND = 13;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * millisecond within the second.
     * E.g., at 10:04:15.250 PM the {@code MILLISECOND} is 250.
     */
    public static final int MILLISECOND = 14;

    /**
     * Field number for {@code get} and {@code set}
     * indicating the raw offset from GMT in milliseconds.
     * <p>
     * This field reflects the correct GMT offset value of the time
     * zone of this {@code Calendar} if the
     * {@code TimeZone} implementation subclass supports
     * historical GMT offset changes.
     */
    public static final int ZONE_OFFSET = 15;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * daylight saving offset in milliseconds.
     * <p>
     * This field reflects the correct daylight saving offset value of
     * the time zone of this {@code Calendar} if the
     * {@code TimeZone} implementation subclass supports
     * historical Daylight Saving Time schedule changes.
     */
    public static final int DST_OFFSET = 16;

    /**
     * The number of distinct fields recognized by {@code get} and {@code set}.
     * Field numbers range from {@code 0..FIELD_COUNT-1}.
     */
    public static final int FIELD_COUNT = 17;

    /**
     * Value of the {@link #DAY_OF_WEEK} field indicating
     * Sunday.
     */
    public static final int SUNDAY = 1;

    /**
     * Value of the {@link #DAY_OF_WEEK} field indicating
     * Monday.
     */
    public static final int MONDAY = 2;

    /**
     * Value of the {@link #DAY_OF_WEEK} field indicating
     * Tuesday.
     */
    public static final int TUESDAY = 3;

    /**
     * Value of the {@link #DAY_OF_WEEK} field indicating
     * Wednesday.
     */
    public static final int WEDNESDAY = 4;

    /**
     * Value of the {@link #DAY_OF_WEEK} field indicating
     * Thursday.
     */
    public static final int THURSDAY = 5;

    /**
     * Value of the {@link #DAY_OF_WEEK} field indicating
     * Friday.
     */
    public static final int FRIDAY = 6;

    /**
     * Value of the {@link #DAY_OF_WEEK} field indicating
     * Saturday.
     */
    public static final int SATURDAY = 7;

    /**
     * Value of the {@link #MONTH} field indicating the
     * first month of the year in the Gregorian and Julian calendars.
     */
    public static final int JANUARY = 0;

    /**
     * Value of the {@link #MONTH} field indicating the
     * second month of the year in the Gregorian and Julian calendars.
     */
    public static final int FEBRUARY = 1;

    /**
     * Value of the {@link #MONTH} field indicating the
     * third month of the year in the Gregorian and Julian calendars.
     */
    public static final int MARCH = 2;

    /**
     * Value of the {@link #MONTH} field indicating the
     * fourth month of the year in the Gregorian and Julian calendars.
     */
    public static final int APRIL = 3;

    /**
     * Value of the {@link #MONTH} field indicating the
     * fifth month of the year in the Gregorian and Julian calendars.
     */
    public static final int MAY = 4;

    /**
     * Value of the {@link #MONTH} field indicating the
     * sixth month of the year in the Gregorian and Julian calendars.
     */
    public static final int JUNE = 5;

    /**
     * Value of the {@link #MONTH} field indicating the
     * seventh month of the year in the Gregorian and Julian calendars.
     */
    public static final int JULY = 6;

    /**
     * Value of the {@link #MONTH} field indicating the
     * eighth month of the year in the Gregorian and Julian calendars.
     */
    public static final int AUGUST = 7;

    /**
     * Value of the {@link #MONTH} field indicating the
     * ninth month of the year in the Gregorian and Julian calendars.
     */
    public static final int SEPTEMBER = 8;

    /**
     * Value of the {@link #MONTH} field indicating the
     * tenth month of the year in the Gregorian and Julian calendars.
     */
    public static final int OCTOBER = 9;

    /**
     * Value of the {@link #MONTH} field indicating the
     * eleventh month of the year in the Gregorian and Julian calendars.
     */
    public static final int NOVEMBER = 10;

    /**
     * Value of the {@link #MONTH} field indicating the
     * twelfth month of the year in the Gregorian and Julian calendars.
     */
    public static final int DECEMBER = 11;

    /**
     * Value of the {@link #MONTH} field indicating the
     * thirteenth month of the year. Although {@code GregorianCalendar}
     * does not use this value, lunar calendars do.
     */
    public static final int UNDECIMBER = 12;

    /**
     * Value of the {@link #AM_PM} field indicating the
     * period of the day from midnight to just before noon.
     */
    public static final int AM = 0;

    /**
     * Value of the {@link #AM_PM} field indicating the
     * period of the day from noon to just before midnight.
     */
    public static final int PM = 1;

    /**
     * A style specifier for {@link #getDisplayNames(int, int, Locale)
     * getDisplayNames} indicating names in all styles, such as
     * "January" and "Jan".
     *
     * @see #SHORT_FORMAT
     * @see #LONG_FORMAT
     * @see #SHORT_STANDALONE
     * @see #LONG_STANDALONE
     * @see #SHORT
     * @see #LONG
     * @since 1.6
     */
    public static final int ALL_STYLES = 0;

    static final int STANDALONE_MASK = 0x8000;

    /**
     * A style specifier for {@link #getDisplayName(int, int, Locale)
     * getDisplayName} and {@link #getDisplayNames(int, int, Locale)
     * getDisplayNames} equivalent to {@link #SHORT_FORMAT}.
     *
     * @see #SHORT_STANDALONE
     * @see #LONG
     * @since 1.6
     */
    public static final int SHORT = 1;

    /**
     * A style specifier for {@link #getDisplayName(int, int, Locale)
     * getDisplayName} and {@link #getDisplayNames(int, int, Locale)
     * getDisplayNames} equivalent to {@link #LONG_FORMAT}.
     *
     * @see #LONG_STANDALONE
     * @see #SHORT
     * @since 1.6
     */
    public static final int LONG = 2;

    /**
     * A style specifier for {@link #getDisplayName(int, int, Locale)
     * getDisplayName} and {@link #getDisplayNames(int, int, Locale)
     * getDisplayNames} indicating a narrow name used for format. Narrow names
     * are typically single character strings, such as "M" for Monday.
     *
     * @see #NARROW_STANDALONE
     * @see #SHORT_FORMAT
     * @see #LONG_FORMAT
     * @since 1.8
     */
    public static final int NARROW_FORMAT = 4;

    /**
     * A style specifier for {@link #getDisplayName(int, int, Locale)
     * getDisplayName} and {@link #getDisplayNames(int, int, Locale)
     * getDisplayNames} indicating a narrow name independently. Narrow names
     * are typically single character strings, such as "M" for Monday.
     *
     * @see #NARROW_FORMAT
     * @see #SHORT_STANDALONE
     * @see #LONG_STANDALONE
     * @since 1.8
     */
    public static final int NARROW_STANDALONE = NARROW_FORMAT | STANDALONE_MASK;

    /**
     * A style specifier for {@link #getDisplayName(int, int, Locale)
     * getDisplayName} and {@link #getDisplayNames(int, int, Locale)
     * getDisplayNames} indicating a short name used for format.
     *
     * @see #SHORT_STANDALONE
     * @see #LONG_FORMAT
     * @see #LONG_STANDALONE
     * @since 1.8
     */
    public static final int SHORT_FORMAT = 1;

    /**
     * A style specifier for {@link #getDisplayName(int, int, Locale)
     * getDisplayName} and {@link #getDisplayNames(int, int, Locale)
     * getDisplayNames} indicating a long name used for format.
     *
     * @see #LONG_STANDALONE
     * @see #SHORT_FORMAT
     * @see #SHORT_STANDALONE
     * @since 1.8
     */
    public static final int LONG_FORMAT = 2;

    /**
     * A style specifier for {@link #getDisplayName(int, int, Locale)
     * getDisplayName} and {@link #getDisplayNames(int, int, Locale)
     * getDisplayNames} indicating a short name used independently,
     * such as a month abbreviation as calendar headers.
     *
     * @see #SHORT_FORMAT
     * @see #LONG_FORMAT
     * @see #LONG_STANDALONE
     * @since 1.8
     */
    public static final int SHORT_STANDALONE = SHORT | STANDALONE_MASK;

    /**
     * A style specifier for {@link #getDisplayName(int, int, Locale)
     * getDisplayName} and {@link #getDisplayNames(int, int, Locale)
     * getDisplayNames} indicating a long name used independently,
     * such as a month name as calendar headers.
     *
     * @see #LONG_FORMAT
     * @see #SHORT_FORMAT
     * @see #SHORT_STANDALONE
     * @since 1.8
     */
    public static final int LONG_STANDALONE = LONG | STANDALONE_MASK;

    // Internal notes:
    // Calendar contains two kinds of time representations: current "time" in
    // milliseconds, and a set of calendar "fields" representing the current time.
    // The two representations are usually in sync, but can get out of sync
    // as follows.
    // 1. Initially, no fields are set, and the time is invalid.
    // 2. If the time is set, all fields are computed and in sync.
    // 3. If a single field is set, the time is invalid.
    // Recomputation of the time and fields happens when the object needs
    // to return a result to the user, or use a result for a computation.

    /**
     * The calendar field values for the currently set time for this calendar.
     * This is an array of {@code FIELD_COUNT} integers, with index values
     * {@code ERA} through {@code DST_OFFSET}.
     * @serial
     */
    @SuppressWarnings("ProtectedField")
    protected int           fields[];

    /**
     * The flags which tell if a specified calendar field for the calendar is set.
     * A new object has no fields set.  After the first call to a method
     * which generates the fields, they all remain set after that.
     * This is an array of {@code FIELD_COUNT} booleans, with index values
     * {@code ERA} through {@code DST_OFFSET}.
     * @serial
     */
    @SuppressWarnings("ProtectedField")
    protected boolean       isSet[];

    /**
     * Pseudo-time-stamps which specify when each field was set. There
     * are two special values, UNSET and COMPUTED. Values from
     * MINIMUM_USER_SET to Integer.MAX_VALUE are legal user set values.
     */
    private transient int   stamp[];

    /**
     * The currently set time for this calendar, expressed in milliseconds after
     * January 1, 1970, 0:00:00 GMT.
     * @see #isTimeSet
     * @serial
     */
    @SuppressWarnings("ProtectedField")
    protected long          time;

    /**
     * True if then the value of {@code time} is valid.
     * The time is made invalid by a change to an item of {@code field[]}.
     * @see #time
     * @serial
     */
    @SuppressWarnings("ProtectedField")
    protected boolean       isTimeSet;

    /**
     * True if {@code fields[]} are in sync with the currently set time.
     * If false, then the next attempt to get the value of a field will
     * force a recomputation of all fields from the current value of
     * {@code time}.
     * @serial
     */
    @SuppressWarnings("ProtectedField")
    protected boolean       areFieldsSet;

    /**
     * True if all fields have been set.
     * @serial
     */
    transient boolean       areAllFieldsSet;

    /**
     * {@code True} if this calendar allows out-of-range field values during computation
     * of {@code time} from {@code fields[]}.
     * @see #setLenient
     * @see #isLenient
     * @serial
     */
    private boolean         lenient = true;

    /**
     * The {@code TimeZone} used by this calendar. {@code Calendar}
     * uses the time zone data to translate between locale and GMT time.
     * @serial
     */
    private TimeZone        zone;

    /**
     * {@code True} if zone references to a shared TimeZone object.
     */
    private transient boolean sharedZone = false;

    /**
     * The first day of the week, with possible values {@code SUNDAY},
     * {@code MONDAY}, etc.  This is a locale-dependent value.
     * @serial
     */
    private int             firstDayOfWeek;

    /**
     * The number of days required for the first week in a month or year,
     * with possible values from 1 to 7.  This is a locale-dependent value.
     * @serial
     */
    private int             minimalDaysInFirstWeek;

    /**
     * Cache to hold the firstDayOfWeek and minimalDaysInFirstWeek
     * of a Locale.
     */
    private static final ConcurrentMap<Locale, int[]> cachedLocaleData
        = new ConcurrentHashMap<>(3);

    // Special values of stamp[]
    /**
     * The corresponding fields[] has no value.
     */
    private static final int        UNSET = 0;

    /**
     * The value of the corresponding fields[] has been calculated internally.
     */
    private static final int        COMPUTED = 1;

    /**
     * The value of the corresponding fields[] has been set externally. Stamp
     * values which are greater than 1 represents the (pseudo) time when the
     * corresponding fields[] value was set.
     */
    private static final int        MINIMUM_USER_STAMP = 2;

    /**
     * The mask value that represents all of the fields.
     */
    static final int ALL_FIELDS = (1 << FIELD_COUNT) - 1;

    /**
     * The next available value for {@code stamp[]}, an internal array.
     * This actually should not be written out to the stream, and will probably
     * be removed from the stream in the near future.  In the meantime,
     * a value of {@code MINIMUM_USER_STAMP} should be used.
     * @serial
     */
    private int             nextStamp = MINIMUM_USER_STAMP;

    // the internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.5
    // - 1 for version from JDK 1.1.6, which writes a correct 'time' value
    //     as well as compatible values for other fields.  This is a
    //     transitional format.
    // - 2 (not implemented yet) a future version, in which fields[],
    //     areFieldsSet, and isTimeSet become transient, and isSet[] is
    //     removed. In JDK 1.1.6 we write a format compatible with version 2.
    static final int        currentSerialVersion = 1;

    /**
     * The version of the serialized data on the stream.  Possible values:
     * <dl>
     * <dt><b>0</b> or not present on stream</dt>
     * <dd>
     * JDK 1.1.5 or earlier.
     * </dd>
     * <dt><b>1</b></dt>
     * <dd>
     * JDK 1.1.6 or later.  Writes a correct 'time' value
     * as well as compatible values for other fields.  This is a
     * transitional format.
     * </dd>
     * </dl>
     * When streaming out this class, the most recent format
     * and the highest allowable {@code serialVersionOnStream}
     * is written.
     * @serial
     * @since 1.1.6
     */
    private int             serialVersionOnStream = currentSerialVersion;

    // Proclaim serialization compatibility with JDK 1.1
    @java.io.Serial
    static final long       serialVersionUID = -1807547505821590642L;

    // Mask values for calendar fields
    @SuppressWarnings("PointlessBitwiseExpression")
    static final int ERA_MASK           = (1 << ERA);
    static final int YEAR_MASK          = (1 << YEAR);
    static final int MONTH_MASK         = (1 << MONTH);
    static final int WEEK_OF_YEAR_MASK  = (1 << WEEK_OF_YEAR);
    static final int WEEK_OF_MONTH_MASK = (1 << WEEK_OF_MONTH);
    static final int DAY_OF_MONTH_MASK  = (1 << DAY_OF_MONTH);
    static final int DATE_MASK          = DAY_OF_MONTH_MASK;
    static final int DAY_OF_YEAR_MASK   = (1 << DAY_OF_YEAR);
    static final int DAY_OF_WEEK_MASK   = (1 << DAY_OF_WEEK);
    static final int DAY_OF_WEEK_IN_MONTH_MASK  = (1 << DAY_OF_WEEK_IN_MONTH);
    static final int AM_PM_MASK         = (1 << AM_PM);
    static final int HOUR_MASK          = (1 << HOUR);
    static final int HOUR_OF_DAY_MASK   = (1 << HOUR_OF_DAY);
    static final int MINUTE_MASK        = (1 << MINUTE);
    static final int SECOND_MASK        = (1 << SECOND);
    static final int MILLISECOND_MASK   = (1 << MILLISECOND);
    static final int ZONE_OFFSET_MASK   = (1 << ZONE_OFFSET);
    static final int DST_OFFSET_MASK    = (1 << DST_OFFSET);

    /**
     * {@code Calendar.Builder} is used for creating a {@code Calendar} from
     * various date-time parameters.
     *
     * <p>There are two ways to set a {@code Calendar} to a date-time value. One
     * is to set the instant parameter to a millisecond offset from the <a
     * href="Calendar.html#Epoch">Epoch</a>. The other is to set individual
     * field parameters, such as {@link Calendar#YEAR YEAR}, to their desired
     * values. These two ways can't be mixed. Trying to set both the instant and
     * individual fields will cause an {@link IllegalStateException} to be
     * thrown. However, it is permitted to override previous values of the
     * instant or field parameters.
     *
     * <p>If no enough field parameters are given for determining date and/or
     * time, calendar specific default values are used when building a
     * {@code Calendar}. For example, if the {@link Calendar#YEAR YEAR} value
     * isn't given for the Gregorian calendar, 1970 will be used. If there are
     * any conflicts among field parameters, the <a
     * href="Calendar.html#resolution"> resolution rules</a> are applied.
     * Therefore, the order of field setting matters.
     *
     * <p>In addition to the date-time parameters,
     * the {@linkplain #setLocale(Locale) locale},
     * {@linkplain #setTimeZone(TimeZone) time zone},
     * {@linkplain #setWeekDefinition(int, int) week definition}, and
     * {@linkplain #setLenient(boolean) leniency mode} parameters can be set.
     *
     * <p><b>Examples</b>
     * <p>The following are sample usages. Sample code assumes that the
     * {@code Calendar} constants are statically imported.
     *
     * <p>The following code produces a {@code Calendar} with date 2012-12-31
     * (Gregorian) because Monday is the first day of a week with the <a
     * href="GregorianCalendar.html#iso8601_compatible_setting"> ISO 8601
     * compatible week parameters</a>.
     * <pre>
     *   Calendar cal = new Calendar.Builder().setCalendarType("iso8601")
     *                        .setWeekDate(2013, 1, MONDAY).build();</pre>
     * <p>The following code produces a Japanese {@code Calendar} with date
     * 1989-01-08 (Gregorian), assuming that the default {@link Calendar#ERA ERA}
     * is <em>Heisei</em> that started on that day.
     * <pre>
     *   Calendar cal = new Calendar.Builder().setCalendarType("japanese")
     *                        .setFields(YEAR, 1, DAY_OF_YEAR, 1).build();</pre>
     *
     * @since 1.8
     * @see Calendar#getInstance(TimeZone, Locale)
     * @see Calendar#fields
     */
    public static class Builder {
        private static final int NFIELDS = FIELD_COUNT + 1; // +1 for WEEK_YEAR
        private static final int WEEK_YEAR = FIELD_COUNT;

        private long instant;
        // Calendar.stamp[] (lower half) and Calendar.fields[] (upper half) combined
        private int[] fields;
        // Pseudo timestamp starting from MINIMUM_USER_STAMP.
        // (COMPUTED is used to indicate that the instant has been set.)
        private int nextStamp;
        // maxFieldIndex keeps the max index of fields which have been set.
        // (WEEK_YEAR is never included.)
        private int maxFieldIndex;
        private String type;
        private TimeZone zone;
        private boolean lenient = true;
        private Locale locale;
        private int firstDayOfWeek, minimalDaysInFirstWeek;

        /**
         * Constructs a {@code Calendar.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets the instant parameter to the given {@code instant} value that is
         * a millisecond offset from <a href="Calendar.html#Epoch">the
         * Epoch</a>.
         *
         * @param instant a millisecond offset from the Epoch
         * @return this {@code Calendar.Builder}
         * @throws IllegalStateException if any of the field parameters have
         *                               already been set
         * @see Calendar#setTime(Date)
         * @see Calendar#setTimeInMillis(long)
         * @see Calendar#time
         */
        public Builder setInstant(long instant) {
            if (fields != null) {
                throw new IllegalStateException();
            }
            this.instant = instant;
            nextStamp = COMPUTED;
            return this;
        }

        /**
         * Sets the instant parameter to the {@code instant} value given by a
         * {@link Date}. This method is equivalent to a call to
         * {@link #setInstant(long) setInstant(instant.getTime())}.
         *
         * @param instant a {@code Date} representing a millisecond offset from
         *                the Epoch
         * @return this {@code Calendar.Builder}
         * @throws NullPointerException  if {@code instant} is {@code null}
         * @throws IllegalStateException if any of the field parameters have
         *                               already been set
         * @see Calendar#setTime(Date)
         * @see Calendar#setTimeInMillis(long)
         * @see Calendar#time
         */
        public Builder setInstant(Date instant) {
            return setInstant(instant.getTime()); // NPE if instant == null
        }

        /**
         * Sets the {@code field} parameter to the given {@code value}.
         * {@code field} is an index to the {@link Calendar#fields}, such as
         * {@link Calendar#DAY_OF_MONTH DAY_OF_MONTH}. Field value validation is
         * not performed in this method. Any out of range values are either
         * normalized in lenient mode or detected as an invalid value in
         * non-lenient mode when building a {@code Calendar}.
         *
         * @param field an index to the {@code Calendar} fields
         * @param value the field value
         * @return this {@code Calendar.Builder}
         * @throws IllegalArgumentException if {@code field} is invalid
         * @throws IllegalStateException if the instant value has already been set,
         *                      or if fields have been set too many
         *                      (approximately {@link Integer#MAX_VALUE}) times.
         * @see Calendar#set(int, int)
         */
        public Builder set(int field, int value) {
            // Note: WEEK_YEAR can't be set with this method.
            if (field < 0 || field >= FIELD_COUNT) {
                throw new IllegalArgumentException("field is invalid");
            }
            if (isInstantSet()) {
                throw new IllegalStateException("instant has been set");
            }
            allocateFields();
            internalSet(field, value);
            return this;
        }

        /**
         * Sets field parameters to their values given by
         * {@code fieldValuePairs} that are pairs of a field and its value.
         * For example,
         * <pre>
         *   setFields(Calendar.YEAR, 2013,
         *             Calendar.MONTH, Calendar.DECEMBER,
         *             Calendar.DAY_OF_MONTH, 23);</pre>
         * is equivalent to the sequence of the following
         * {@link #set(int, int) set} calls:
         * <pre>
         *   set(Calendar.YEAR, 2013)
         *   .set(Calendar.MONTH, Calendar.DECEMBER)
         *   .set(Calendar.DAY_OF_MONTH, 23);</pre>
         *
         * @param fieldValuePairs field-value pairs
         * @return this {@code Calendar.Builder}
         * @throws NullPointerException if {@code fieldValuePairs} is {@code null}
         * @throws IllegalArgumentException if any of fields are invalid,
         *             or if {@code fieldValuePairs.length} is an odd number.
         * @throws IllegalStateException    if the instant value has been set,
         *             or if fields have been set too many (approximately
         *             {@link Integer#MAX_VALUE}) times.
         */
        public Builder setFields(int... fieldValuePairs) {
            int len = fieldValuePairs.length;
            if ((len % 2) != 0) {
                throw new IllegalArgumentException();
            }
            if (isInstantSet()) {
                throw new IllegalStateException("instant has been set");
            }
            if ((nextStamp + len / 2) < 0) {
                throw new IllegalStateException("stamp counter overflow");
            }
            allocateFields();
            for (int i = 0; i < len; ) {
                int field = fieldValuePairs[i++];
                // Note: WEEK_YEAR can't be set with this method.
                if (field < 0 || field >= FIELD_COUNT) {
                    throw new IllegalArgumentException("field is invalid");
                }
                internalSet(field, fieldValuePairs[i++]);
            }
            return this;
        }

        /**
         * Sets the date field parameters to the values given by {@code year},
         * {@code month}, and {@code dayOfMonth}. This method is equivalent to
         * a call to:
         * <pre>
         *   setFields(Calendar.YEAR, year,
         *             Calendar.MONTH, month,
         *             Calendar.DAY_OF_MONTH, dayOfMonth);</pre>
         *
         * @param year       the {@link Calendar#YEAR YEAR} value
         * @param month      the {@link Calendar#MONTH MONTH} value
         *                   (the month numbering is <em>0-based</em>).
         * @param dayOfMonth the {@link Calendar#DAY_OF_MONTH DAY_OF_MONTH} value
         * @return this {@code Calendar.Builder}
         */
        public Builder setDate(int year, int month, int dayOfMonth) {
            return setFields(YEAR, year, MONTH, month, DAY_OF_MONTH, dayOfMonth);
        }

        /**
         * Sets the time of day field parameters to the values given by
         * {@code hourOfDay}, {@code minute}, and {@code second}. This method is
         * equivalent to a call to:
         * <pre>
         *   setTimeOfDay(hourOfDay, minute, second, 0);</pre>
         *
         * @param hourOfDay the {@link Calendar#HOUR_OF_DAY HOUR_OF_DAY} value
         *                  (24-hour clock)
         * @param minute    the {@link Calendar#MINUTE MINUTE} value
         * @param second    the {@link Calendar#SECOND SECOND} value
         * @return this {@code Calendar.Builder}
         */
        public Builder setTimeOfDay(int hourOfDay, int minute, int second) {
            return setTimeOfDay(hourOfDay, minute, second, 0);
        }

        /**
         * Sets the time of day field parameters to the values given by
         * {@code hourOfDay}, {@code minute}, {@code second}, and
         * {@code millis}. This method is equivalent to a call to:
         * <pre>
         *   setFields(Calendar.HOUR_OF_DAY, hourOfDay,
         *             Calendar.MINUTE, minute,
         *             Calendar.SECOND, second,
         *             Calendar.MILLISECOND, millis);</pre>
         *
         * @param hourOfDay the {@link Calendar#HOUR_OF_DAY HOUR_OF_DAY} value
         *                  (24-hour clock)
         * @param minute    the {@link Calendar#MINUTE MINUTE} value
         * @param second    the {@link Calendar#SECOND SECOND} value
         * @param millis    the {@link Calendar#MILLISECOND MILLISECOND} value
         * @return this {@code Calendar.Builder}
         */
        public Builder setTimeOfDay(int hourOfDay, int minute, int second, int millis) {
            return setFields(HOUR_OF_DAY, hourOfDay, MINUTE, minute,
                             SECOND, second, MILLISECOND, millis);
        }

        /**
         * Sets the week-based date parameters to the values with the given
         * date specifiers - week year, week of year, and day of week.
         *
         * <p>If the specified calendar doesn't support week dates, the
         * {@link #build() build} method will throw an {@link IllegalArgumentException}.
         *
         * @param weekYear   the week year
         * @param weekOfYear the week number based on {@code weekYear}
         * @param dayOfWeek  the day of week value: one of the constants
         *     for the {@link Calendar#DAY_OF_WEEK DAY_OF_WEEK} field:
         *     {@link Calendar#SUNDAY SUNDAY}, ..., {@link Calendar#SATURDAY SATURDAY}.
         * @return this {@code Calendar.Builder}
         * @see Calendar#setWeekDate(int, int, int)
         * @see Calendar#isWeekDateSupported()
         */
        public Builder setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
            allocateFields();
            internalSet(WEEK_YEAR, weekYear);
            internalSet(WEEK_OF_YEAR, weekOfYear);
            internalSet(DAY_OF_WEEK, dayOfWeek);
            return this;
        }

        /**
         * Sets the time zone parameter to the given {@code zone}. If no time
         * zone parameter is given to this {@code Calendar.Builder}, the
         * {@linkplain TimeZone#getDefault() default
         * {@code TimeZone}} will be used in the {@link #build() build}
         * method.
         *
         * @param zone the {@link TimeZone}
         * @return this {@code Calendar.Builder}
         * @throws NullPointerException if {@code zone} is {@code null}
         * @see Calendar#setTimeZone(TimeZone)
         */
        public Builder setTimeZone(TimeZone zone) {
            if (zone == null) {
                throw new NullPointerException();
            }
            this.zone = zone;
            return this;
        }

        /**
         * Sets the lenient mode parameter to the value given by {@code lenient}.
         * If no lenient parameter is given to this {@code Calendar.Builder},
         * lenient mode will be used in the {@link #build() build} method.
         *
         * @param lenient {@code true} for lenient mode;
         *                {@code false} for non-lenient mode
         * @return this {@code Calendar.Builder}
         * @see Calendar#setLenient(boolean)
         */
        public Builder setLenient(boolean lenient) {
            this.lenient = lenient;
            return this;
        }

        /**
         * Sets the calendar type parameter to the given {@code type}. The
         * calendar type given by this method has precedence over any explicit
         * or implicit calendar type given by the
         * {@linkplain #setLocale(Locale) locale}.
         *
         * <p>In addition to the available calendar types returned by the
         * {@link Calendar#getAvailableCalendarTypes() Calendar.getAvailableCalendarTypes}
         * method, {@code "gregorian"} and {@code "iso8601"} as aliases of
         * {@code "gregory"} can be used with this method.
         *
         * @param type the calendar type
         * @return this {@code Calendar.Builder}
         * @throws NullPointerException if {@code type} is {@code null}
         * @throws IllegalArgumentException if {@code type} is unknown
         * @throws IllegalStateException if another calendar type has already been set
         * @see Calendar#getCalendarType()
         * @see Calendar#getAvailableCalendarTypes()
         */
        public Builder setCalendarType(String type) {
            if (type.equals("gregorian")) { // NPE if type == null
                type = "gregory";
            }
            if (!Calendar.getAvailableCalendarTypes().contains(type)
                    && !type.equals("iso8601")) {
                throw new IllegalArgumentException("unknown calendar type: " + type);
            }
            if (this.type == null) {
                this.type = type;
            } else {
                if (!this.type.equals(type)) {
                    throw new IllegalStateException("calendar type override");
                }
            }
            return this;
        }

        /**
         * Sets the locale parameter to the given {@code locale}. If no locale
         * is given to this {@code Calendar.Builder}, the {@linkplain
         * Locale#getDefault(Locale.Category) default {@code Locale}}
         * for {@link Locale.Category#FORMAT} will be used.
         *
         * <p>If no calendar type is explicitly given by a call to the
         * {@link #setCalendarType(String) setCalendarType} method,
         * the {@code Locale} value is used to determine what type of
         * {@code Calendar} to be built.
         *
         * <p>If no week definition parameters are explicitly given by a call to
         * the {@link #setWeekDefinition(int,int) setWeekDefinition} method, the
         * {@code Locale}'s default values are used.
         *
         * @param locale the {@link Locale}
         * @throws NullPointerException if {@code locale} is {@code null}
         * @return this {@code Calendar.Builder}
         * @see Calendar#getInstance(Locale)
         */
        public Builder setLocale(Locale locale) {
            if (locale == null) {
                throw new NullPointerException();
            }
            this.locale = locale;
            return this;
        }

        /**
         * Sets the week definition parameters to the values given by
         * {@code firstDayOfWeek} and {@code minimalDaysInFirstWeek} that are
         * used to determine the <a href="Calendar.html#first_week">first
         * week</a> of a year. The parameters given by this method have
         * precedence over the default values given by the
         * {@linkplain #setLocale(Locale) locale}.
         *
         * @param firstDayOfWeek the first day of a week; one of
         *                       {@link Calendar#SUNDAY} to {@link Calendar#SATURDAY}
         * @param minimalDaysInFirstWeek the minimal number of days in the first
         *                               week (1..7)
         * @return this {@code Calendar.Builder}
         * @throws IllegalArgumentException if {@code firstDayOfWeek} or
         *                                  {@code minimalDaysInFirstWeek} is invalid
         * @see Calendar#getFirstDayOfWeek()
         * @see Calendar#getMinimalDaysInFirstWeek()
         */
        public Builder setWeekDefinition(int firstDayOfWeek, int minimalDaysInFirstWeek) {
            if (!isValidWeekParameter(firstDayOfWeek)
                    || !isValidWeekParameter(minimalDaysInFirstWeek)) {
                throw new IllegalArgumentException();
            }
            this.firstDayOfWeek = firstDayOfWeek;
            this.minimalDaysInFirstWeek = minimalDaysInFirstWeek;
            return this;
        }

        /**
         * Returns a {@code Calendar} built from the parameters set by the
         * setter methods. The calendar type given by the {@link #setCalendarType(String)
         * setCalendarType} method or the {@linkplain #setLocale(Locale) locale} is
         * used to determine what {@code Calendar} to be created. If no explicit
         * calendar type is given, the locale's default calendar is created.
         *
         * <p>If the calendar type is {@code "iso8601"}, the
         * {@linkplain GregorianCalendar#setGregorianChange(Date) Gregorian change date}
         * of a {@link GregorianCalendar} is set to {@code Date(Long.MIN_VALUE)}
         * to be the <em>proleptic</em> Gregorian calendar. Its week definition
         * parameters are also set to be <a
         * href="GregorianCalendar.html#iso8601_compatible_setting">compatible
         * with the ISO 8601 standard</a>. Note that the
         * {@link GregorianCalendar#getCalendarType() getCalendarType} method of
         * a {@code GregorianCalendar} created with {@code "iso8601"} returns
         * {@code "gregory"}.
         *
         * <p>The default values are used for locale and time zone if these
         * parameters haven't been given explicitly.
         * <p>
         * If the locale contains the time zone with "tz"
         * <a href="Locale.html#def_locale_extension">Unicode extension</a>,
         * and time zone hasn't been given explicitly, time zone in the locale
         * is used.
         *
         * <p>Any out of range field values are either normalized in lenient
         * mode or detected as an invalid value in non-lenient mode.
         *
         * @return a {@code Calendar} built with parameters of this {@code
         *         Calendar.Builder}
         * @throws IllegalArgumentException if the calendar type is unknown, or
         *             if any invalid field values are given in non-lenient mode, or
         *             if a week date is given for the calendar type that doesn't
         *             support week dates.
         * @see Calendar#getInstance(TimeZone, Locale)
         * @see Locale#getDefault(Locale.Category)
         * @see TimeZone#getDefault()
         */
        public Calendar build() {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            if (zone == null) {
                zone = defaultTimeZone(locale);
            }
            if (type == null) {
                type = locale.getUnicodeLocaleType("ca");
            }
            if (type == null) {
                if (locale.getCountry() == "TH"
                    && locale.getLanguage() == "th") {
                    type = "buddhist";
                } else {
                    type = "gregory";
                }
            }
            final Calendar cal = switch (type) {
                case "gregory" -> new GregorianCalendar(zone, locale, true);
                case "iso8601" -> {
                    GregorianCalendar gcal = new GregorianCalendar(zone, locale, true);
                    // make gcal a proleptic Gregorian
                    gcal.setGregorianChange(new Date(Long.MIN_VALUE));
                    // and week definition to be compatible with ISO 8601
                    setWeekDefinition(MONDAY, 4);
                    yield gcal;
                }
                case "buddhist" -> {
                    var buddhistCalendar = new BuddhistCalendar(zone, locale);
                    buddhistCalendar.clear();
                    yield buddhistCalendar;
                }
                case "japanese" -> new JapaneseImperialCalendar(zone, locale, true);
                default -> throw new IllegalArgumentException("unknown calendar type: " + type);
            };
            cal.setLenient(lenient);
            if (firstDayOfWeek != 0) {
                cal.setFirstDayOfWeek(firstDayOfWeek);
                cal.setMinimalDaysInFirstWeek(minimalDaysInFirstWeek);
            }
            if (isInstantSet()) {
                cal.setTimeInMillis(instant);
                cal.complete();
                return cal;
            }

            if (fields != null) {
                boolean weekDate = isSet(WEEK_YEAR)
                                       && fields[WEEK_YEAR] > fields[YEAR];
                if (weekDate && !cal.isWeekDateSupported()) {
                    throw new IllegalArgumentException("week date is unsupported by " + type);
                }

                // Set the fields from the min stamp to the max stamp so that
                // the fields resolution works in the Calendar.
                for (int stamp = MINIMUM_USER_STAMP; stamp < nextStamp; stamp++) {
                    for (int index = 0; index <= maxFieldIndex; index++) {
                        if (fields[index] == stamp) {
                            cal.set(index, fields[NFIELDS + index]);
                            break;
                        }
                    }
                }

                if (weekDate) {
                    int weekOfYear = isSet(WEEK_OF_YEAR) ? fields[NFIELDS + WEEK_OF_YEAR] : 1;
                    int dayOfWeek = isSet(DAY_OF_WEEK)
                                    ? fields[NFIELDS + DAY_OF_WEEK] : cal.getFirstDayOfWeek();
                    cal.setWeekDate(fields[NFIELDS + WEEK_YEAR], weekOfYear, dayOfWeek);
                }
                cal.complete();
            }

            return cal;
        }

        private void allocateFields() {
            if (fields == null) {
                fields = new int[NFIELDS * 2];
                nextStamp = MINIMUM_USER_STAMP;
                maxFieldIndex = -1;
            }
        }

        private void internalSet(int field, int value) {
            fields[field] = nextStamp++;
            if (nextStamp < 0) {
                throw new IllegalStateException("stamp counter overflow");
            }
            fields[NFIELDS + field] = value;
            if (field > maxFieldIndex && field < WEEK_YEAR) {
                maxFieldIndex = field;
            }
        }

        private boolean isInstantSet() {
            return nextStamp == COMPUTED;
        }

        private boolean isSet(int index) {
            return fields != null && fields[index] > UNSET;
        }

        private boolean isValidWeekParameter(int value) {
            return value > 0 && value <= 7;
        }
    }

    /**
     * Constructs a Calendar with the default time zone
     * and the default {@link java.util.Locale.Category#FORMAT FORMAT}
     * locale.
     * @see     TimeZone#getDefault
     */
    protected Calendar()
    {
        this(TimeZone.getDefaultRef(), Locale.getDefault(Locale.Category.FORMAT));
        sharedZone = true;
    }

    /**
     * Constructs a calendar with the specified time zone and locale.
     *
     * @param zone the time zone to use
     * @param aLocale the locale for the week data
     */
    protected Calendar(TimeZone zone, Locale aLocale)
    {
        fields = new int[FIELD_COUNT];
        isSet = new boolean[FIELD_COUNT];
        stamp = new int[FIELD_COUNT];

        this.zone = zone;
        setWeekCountData(aLocale);
    }

    /**
     * Gets a calendar using the default time zone and locale. The
     * {@code Calendar} returned is based on the current time
     * in the default time zone with the default
     * {@link Locale.Category#FORMAT FORMAT} locale.
     * <p>
     * If the locale contains the time zone with "tz"
     * <a href="Locale.html#def_locale_extension">Unicode extension</a>,
     * that time zone is used instead.
     *
     * @return a Calendar.
     */
    public static Calendar getInstance()
    {
        Locale aLocale = Locale.getDefault(Locale.Category.FORMAT);
        return createCalendar(defaultTimeZone(aLocale), aLocale);
    }

    /**
     * Gets a calendar using the specified time zone and default locale.
     * The {@code Calendar} returned is based on the current time
     * in the given time zone with the default
     * {@link Locale.Category#FORMAT FORMAT} locale.
     *
     * @param zone the time zone to use
     * @return a Calendar.
     */
    public static Calendar getInstance(TimeZone zone)
    {
        return createCalendar(zone, Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Gets a calendar using the default time zone and specified locale.
     * The {@code Calendar} returned is based on the current time
     * in the default time zone with the given locale.
     * <p>
     * If the locale contains the time zone with "tz"
     * <a href="Locale.html#def_locale_extension">Unicode extension</a>,
     * that time zone is used instead.
     *
     * @param aLocale the locale for the week data
     * @return a Calendar.
     */
    public static Calendar getInstance(Locale aLocale)
    {
        return createCalendar(defaultTimeZone(aLocale), aLocale);
    }

    /**
     * Gets a calendar with the specified time zone and locale.
     * The {@code Calendar} returned is based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the time zone to use
     * @param aLocale the locale for the week data
     * @return a Calendar.
     */
    public static Calendar getInstance(TimeZone zone,
                                       Locale aLocale)
    {
        return createCalendar(zone, aLocale);
    }

    private static TimeZone defaultTimeZone(Locale l) {
        TimeZone defaultTZ = TimeZone.getDefault();
        String shortTZID = l.getUnicodeLocaleType("tz");
        return shortTZID != null ?
            TimeZoneNameUtility.convertLDMLShortID(shortTZID)
                .map(TimeZone::getTimeZone)
                .orElse(defaultTZ) :
            defaultTZ;
    }

    private static Calendar createCalendar(TimeZone zone,
                                           Locale aLocale)
    {
        CalendarProvider provider =
            LocaleProviderAdapter.getAdapter(CalendarProvider.class, aLocale)
                                 .getCalendarProvider();
        if (provider != null) {
            try {
                return provider.getInstance(zone, aLocale);
            } catch (IllegalArgumentException iae) {
                // fall back to the default instantiation
            }
        }

        Calendar cal = null;

        if (aLocale.hasExtensions()) {
            String caltype = aLocale.getUnicodeLocaleType("ca");
            if (caltype != null) {
                cal = switch (caltype) {
                    case "buddhist" -> new BuddhistCalendar(zone, aLocale);
                    case "japanese" -> new JapaneseImperialCalendar(zone, aLocale);
                    case "gregory"  -> new GregorianCalendar(zone, aLocale);
                    default         -> null;
                };
            }
        }
        if (cal == null) {
            // If no known calendar type is explicitly specified,
            // perform the traditional way to create a Calendar:
            // create a BuddhistCalendar for th_TH locale,
            // a JapaneseImperialCalendar for ja_JP_JP locale, or
            // a GregorianCalendar for any other locales.
            // NOTE: The language, country and variant strings are interned.
            if (aLocale.getLanguage() == "th" && aLocale.getCountry() == "TH") {
                cal = new BuddhistCalendar(zone, aLocale);
            } else if (aLocale.getVariant() == "JP" && aLocale.getLanguage() == "ja"
                       && aLocale.getCountry() == "JP") {
                cal = new JapaneseImperialCalendar(zone, aLocale);
            } else {
                cal = new GregorianCalendar(zone, aLocale);
            }
        }
        return cal;
    }

    /**
     * Returns an array of all locales for which the {@code getInstance}
     * methods of this class can return localized instances.
     * The array returned must contain at least a {@code Locale}
     * instance equal to {@link java.util.Locale#US Locale.US}.
     *
     * @return An array of locales for which localized
     *         {@code Calendar} instances are available.
     */
    public static synchronized Locale[] getAvailableLocales()
    {
        return DateFormat.getAvailableLocales();
    }

    /**
     * Converts the current calendar field values in {@link #fields fields[]}
     * to the millisecond time value
     * {@link #time}.
     *
     * @see #complete()
     * @see #computeFields()
     */
    protected abstract void computeTime();

    /**
     * Converts the current millisecond time value {@link #time}
     * to calendar field values in {@link #fields fields[]}.
     * This allows you to sync up the calendar field values with
     * a new time that is set for the calendar.  The time is <em>not</em>
     * recomputed first; to recompute the time, then the fields, call the
     * {@link #complete()} method.
     *
     * @see #computeTime()
     */
    protected abstract void computeFields();

    /**
     * Returns a {@code Date} object representing this
     * {@code Calendar}'s time value (millisecond offset from the <a
     * href="#Epoch">Epoch</a>").
     *
     * @return a {@code Date} representing the time value.
     * @see #setTime(Date)
     * @see #getTimeInMillis()
     */
    public final Date getTime() {
        return new Date(getTimeInMillis());
    }

    /**
     * Sets this Calendar's time with the given {@code Date}.
     * <p>
     * Note: Calling {@code setTime()} with
     * {@code Date(Long.MAX_VALUE)} or {@code Date(Long.MIN_VALUE)}
     * may yield incorrect field values from {@code get()}.
     *
     * @param date the given Date.
     * @see #getTime()
     * @see #setTimeInMillis(long)
     * @throws NullPointerException if {@code date} is {@code null}
     */
    public final void setTime(Date date) {
        Objects.requireNonNull(date, "date must not be null");
        setTimeInMillis(date.getTime());
    }

    /**
     * Returns this Calendar's time value in milliseconds.
     *
     * @return the current time as UTC milliseconds from the epoch.
     * @see #getTime()
     * @see #setTimeInMillis(long)
     */
    public long getTimeInMillis() {
        if (!isTimeSet) {
            updateTime();
        }
        return time;
    }

    /**
     * Sets this Calendar's current time from the given long value.
     *
     * @param millis the new time in UTC milliseconds from the epoch.
     * @see #setTime(Date)
     * @see #getTimeInMillis()
     */
    public void setTimeInMillis(long millis) {
        // If we don't need to recalculate the calendar field values,
        // do nothing.
        if (time == millis && isTimeSet && areFieldsSet && areAllFieldsSet
            && (zone instanceof ZoneInfo) && !((ZoneInfo)zone).isDirty()) {
            return;
        }
        time = millis;
        isTimeSet = true;
        areFieldsSet = false;
        computeFields();
        areAllFieldsSet = areFieldsSet = true;
    }

    /**
     * Returns the value of the given calendar field. In lenient mode,
     * all calendar fields are normalized. In non-lenient mode, all
     * calendar fields are validated and this method throws an
     * exception if any calendar fields have out-of-range values. The
     * normalization and validation are handled by the
     * {@link #complete()} method, which process is calendar
     * system dependent.
     *
     * @param field the given calendar field.
     * @return the value for the given calendar field.
     * @throws ArrayIndexOutOfBoundsException if the specified field is out of range
     *             (<code>field &lt; 0 || field &gt;= FIELD_COUNT</code>).
     * @see #set(int,int)
     * @see #complete()
     */
    public int get(int field)
    {
        complete();
        return internalGet(field);
    }

    /**
     * Returns the value of the given calendar field. This method does
     * not involve normalization or validation of the field value.
     *
     * @param field the given calendar field.
     * @return the value for the given calendar field.
     * @see #get(int)
     */
    protected final int internalGet(int field)
    {
        return fields[field];
    }

    /**
     * Sets the value of the given calendar field. This method does
     * not affect any setting state of the field in this
     * {@code Calendar} instance.
     *
     * @throws IndexOutOfBoundsException if the specified field is out of range
     *             (<code>field &lt; 0 || field &gt;= FIELD_COUNT</code>).
     * @see #areFieldsSet
     * @see #isTimeSet
     * @see #areAllFieldsSet
     * @see #set(int,int)
     */
    final void internalSet(int field, int value)
    {
        fields[field] = value;
    }

    /**
     * Sets the given calendar field to the given value. The value is not
     * interpreted by this method regardless of the leniency mode.
     *
     * @param field the given calendar field.
     * @param value the value to be set for the given calendar field.
     * @throws ArrayIndexOutOfBoundsException if the specified field is out of range
     *             (<code>field &lt; 0 || field &gt;= FIELD_COUNT</code>).
     * in non-lenient mode.
     * @see #set(int,int,int)
     * @see #set(int,int,int,int,int)
     * @see #set(int,int,int,int,int,int)
     * @see #get(int)
     */
    public void set(int field, int value)
    {
        // If the fields are partially normalized, calculate all the
        // fields before changing any fields.
        if (areFieldsSet && !areAllFieldsSet) {
            computeFields();
        }
        internalSet(field, value);
        isTimeSet = false;
        areFieldsSet = false;
        isSet[field] = true;
        stamp[field] = nextStamp++;
        if (nextStamp == Integer.MAX_VALUE) {
            adjustStamp();
        }
    }

    /**
     * Sets the values for the calendar fields {@code YEAR},
     * {@code MONTH}, and {@code DAY_OF_MONTH}.
     * Previous values of other calendar fields are retained.  If this is not desired,
     * call {@link #clear()} first.
     *
     * @param year the value used to set the {@code YEAR} calendar field.
     * @param month the value used to set the {@code MONTH} calendar field.
     * Month value is 0-based. e.g., 0 for January.
     * @param date the value used to set the {@code DAY_OF_MONTH} calendar field.
     * @see #set(int,int)
     * @see #set(int,int,int,int,int)
     * @see #set(int,int,int,int,int,int)
     */
    public final void set(int year, int month, int date)
    {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, date);
    }

    /**
     * Sets the values for the calendar fields {@code YEAR},
     * {@code MONTH}, {@code DAY_OF_MONTH},
     * {@code HOUR_OF_DAY}, and {@code MINUTE}.
     * Previous values of other fields are retained.  If this is not desired,
     * call {@link #clear()} first.
     *
     * @param year the value used to set the {@code YEAR} calendar field.
     * @param month the value used to set the {@code MONTH} calendar field.
     * Month value is 0-based. e.g., 0 for January.
     * @param date the value used to set the {@code DAY_OF_MONTH} calendar field.
     * @param hourOfDay the value used to set the {@code HOUR_OF_DAY} calendar field.
     * @param minute the value used to set the {@code MINUTE} calendar field.
     * @see #set(int,int)
     * @see #set(int,int,int)
     * @see #set(int,int,int,int,int,int)
     */
    public final void set(int year, int month, int date, int hourOfDay, int minute)
    {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, date);
        set(HOUR_OF_DAY, hourOfDay);
        set(MINUTE, minute);
    }

    /**
     * Sets the values for the fields {@code YEAR}, {@code MONTH},
     * {@code DAY_OF_MONTH}, {@code HOUR_OF_DAY}, {@code MINUTE}, and
     * {@code SECOND}.
     * Previous values of other fields are retained.  If this is not desired,
     * call {@link #clear()} first.
     *
     * @param year the value used to set the {@code YEAR} calendar field.
     * @param month the value used to set the {@code MONTH} calendar field.
     * Month value is 0-based. e.g., 0 for January.
     * @param date the value used to set the {@code DAY_OF_MONTH} calendar field.
     * @param hourOfDay the value used to set the {@code HOUR_OF_DAY} calendar field.
     * @param minute the value used to set the {@code MINUTE} calendar field.
     * @param second the value used to set the {@code SECOND} calendar field.
     * @see #set(int,int)
     * @see #set(int,int,int)
     * @see #set(int,int,int,int,int)
     */
    public final void set(int year, int month, int date, int hourOfDay, int minute,
                          int second)
    {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, date);
        set(HOUR_OF_DAY, hourOfDay);
        set(MINUTE, minute);
        set(SECOND, second);
    }

    /**
     * Sets all the calendar field values and the time value
     * (millisecond offset from the <a href="#Epoch">Epoch</a>) of
     * this {@code Calendar} undefined. This means that {@link
     * #isSet(int) isSet()} will return {@code false} for all the
     * calendar fields, and the date and time calculations will treat
     * the fields as if they had never been set. A
     * {@code Calendar} implementation class may use its specific
     * default field values for date/time calculations. For example,
     * {@code GregorianCalendar} uses 1970 if the
     * {@code YEAR} field value is undefined.
     *
     * @see #clear(int)
     */
    public final void clear()
    {
        for (int i = 0; i < fields.length; ) {
            stamp[i] = fields[i] = 0; // UNSET == 0
            isSet[i++] = false;
        }
        areAllFieldsSet = areFieldsSet = false;
        isTimeSet = false;
    }

    /**
     * Sets the given calendar field value and the time value
     * (millisecond offset from the <a href="#Epoch">Epoch</a>) of
     * this {@code Calendar} undefined. This means that {@link
     * #isSet(int) isSet(field)} will return {@code false}, and
     * the date and time calculations will treat the field as if it
     * had never been set. A {@code Calendar} implementation
     * class may use the field's specific default value for date and
     * time calculations.
     *
     * <p>The {@link #HOUR_OF_DAY}, {@link #HOUR} and {@link #AM_PM}
     * fields are handled independently and the <a
     * href="#time_resolution">the resolution rule for the time of
     * day</a> is applied. Clearing one of the fields doesn't reset
     * the hour of day value of this {@code Calendar}. Use {@link
     * #set(int,int) set(Calendar.HOUR_OF_DAY, 0)} to reset the hour
     * value.
     *
     * @param field the calendar field to be cleared.
     * @see #clear()
     */
    public final void clear(int field)
    {
        fields[field] = 0;
        stamp[field] = UNSET;
        isSet[field] = false;

        areAllFieldsSet = areFieldsSet = false;
        isTimeSet = false;
    }

    /**
     * Determines if the given calendar field has a value set,
     * including cases that the value has been set by internal fields
     * calculations triggered by a {@code get} method call.
     *
     * @param field the calendar field to test
     * @return {@code true} if the given calendar field has a value set;
     * {@code false} otherwise.
     */
    public final boolean isSet(int field)
    {
        return stamp[field] != UNSET;
    }

    /**
     * Returns the string representation of the calendar
     * {@code field} value in the given {@code style} and
     * {@code locale}.  If no string representation is
     * applicable, {@code null} is returned. This method calls
     * {@link Calendar#get(int) get(field)} to get the calendar
     * {@code field} value if the string representation is
     * applicable to the given calendar {@code field}.
     *
     * <p>For example, if this {@code Calendar} is a
     * {@code GregorianCalendar} and its date is 2005-01-01, then
     * the string representation of the {@link #MONTH} field would be
     * "January" in the long style in an English locale or "Jan" in
     * the short style. However, no string representation would be
     * available for the {@link #DAY_OF_MONTH} field, and this method
     * would return {@code null}.
     *
     * <p>The default implementation supports the calendar fields for
     * which a {@link DateFormatSymbols} has names in the given
     * {@code locale}.
     *
     * @param field
     *        the calendar field for which the string representation
     *        is returned
     * @param style
     *        the style applied to the string representation; one of {@link
     *        #SHORT_FORMAT} ({@link #SHORT}), {@link #SHORT_STANDALONE},
     *        {@link #LONG_FORMAT} ({@link #LONG}), {@link #LONG_STANDALONE},
     *        {@link #NARROW_FORMAT}, or {@link #NARROW_STANDALONE}.
     * @param locale
     *        the locale for the string representation
     *        (any calendar types specified by {@code locale} are ignored)
     * @return the string representation of the given
     *        {@code field} in the given {@code style}, or
     *        {@code null} if no string representation is
     *        applicable.
     * @throws    IllegalArgumentException
     *        if {@code field} or {@code style} is invalid,
     *        or if this {@code Calendar} is non-lenient and any
     *        of the calendar fields have invalid values
     * @throws    NullPointerException
     *        if {@code locale} is null
     * @since 1.6
     */
    public String getDisplayName(int field, int style, Locale locale) {
        if (!checkDisplayNameParams(field, style, SHORT, NARROW_FORMAT, locale,
                            ERA_MASK|MONTH_MASK|DAY_OF_WEEK_MASK|AM_PM_MASK)) {
            return null;
        }

        String calendarType = getCalendarType();
        int fieldValue = get(field);
        // the standalone/narrow styles and short era are supported only through
        // CalendarNameProviders.
        if (isStandaloneStyle(style) || isNarrowFormatStyle(style) ||
            field == ERA && (style & SHORT) == SHORT) {
            String val = CalendarDataUtility.retrieveFieldValueName(calendarType,
                                                                    field, fieldValue,
                                                                    style, locale);
            // Perform fallback here to follow the CLDR rules
            if (val == null) {
                if (isNarrowFormatStyle(style)) {
                    val = CalendarDataUtility.retrieveFieldValueName(calendarType,
                                                                     field, fieldValue,
                                                                     toStandaloneStyle(style),
                                                                     locale);
                } else if (isStandaloneStyle(style)) {
                    val = CalendarDataUtility.retrieveFieldValueName(calendarType,
                                                                     field, fieldValue,
                                                                     getBaseStyle(style),
                                                                     locale);
                }
            }
            return val;
        }

        DateFormatSymbols symbols = DateFormatSymbols.getInstance(locale);
        String[] strings = getFieldStrings(field, style, symbols);
        if (strings != null) {
            if (fieldValue < strings.length) {
                return strings[fieldValue];
            }
        }
        return null;
    }

    /**
     * Returns a {@code Map} containing all names of the calendar
     * {@code field} in the given {@code style} and
     * {@code locale} and their corresponding field values. For
     * example, if this {@code Calendar} is a {@link
     * GregorianCalendar}, the returned map would contain "Jan" to
     * {@link #JANUARY}, "Feb" to {@link #FEBRUARY}, and so on, in the
     * {@linkplain #SHORT short} style in an English locale.
     *
     * <p>Narrow names may not be unique due to use of single characters,
     * such as "S" for Sunday and Saturday. In that case narrow names are not
     * included in the returned {@code Map}.
     *
     * <p>The values of other calendar fields may be taken into
     * account to determine a set of display names. For example, if
     * this {@code Calendar} is a lunisolar calendar system and
     * the year value given by the {@link #YEAR} field has a leap
     * month, this method would return month names containing the leap
     * month name, and month names are mapped to their values specific
     * for the year.
     *
     * <p>The default implementation supports display names contained in
     * a {@link DateFormatSymbols}. For example, if {@code field}
     * is {@link #MONTH} and {@code style} is {@link
     * #ALL_STYLES}, this method returns a {@code Map} containing
     * all strings returned by {@link DateFormatSymbols#getShortMonths()}
     * and {@link DateFormatSymbols#getMonths()}.
     *
     * @param field
     *        the calendar field for which the display names are returned
     * @param style
     *        the style applied to the string representation; one of {@link
     *        #SHORT_FORMAT} ({@link #SHORT}), {@link #SHORT_STANDALONE},
     *        {@link #LONG_FORMAT} ({@link #LONG}), {@link #LONG_STANDALONE},
     *        {@link #NARROW_FORMAT}, or {@link #NARROW_STANDALONE}
     * @param locale
     *        the locale for the display names
     * @return a {@code Map} containing all display names in
     *        {@code style} and {@code locale} and their
     *        field values, or {@code null} if no display names
     *        are defined for {@code field}
     * @throws    IllegalArgumentException
     *        if {@code field} or {@code style} is invalid,
     *        or if this {@code Calendar} is non-lenient and any
     *        of the calendar fields have invalid values
     * @throws    NullPointerException
     *        if {@code locale} is null
     * @since 1.6
     */
    public Map<String, Integer> getDisplayNames(int field, int style, Locale locale) {
        if (!checkDisplayNameParams(field, style, ALL_STYLES, NARROW_FORMAT, locale,
                                    ERA_MASK|MONTH_MASK|DAY_OF_WEEK_MASK|AM_PM_MASK)) {
            return null;
        }

        String calendarType = getCalendarType();
        if (style == ALL_STYLES || isStandaloneStyle(style) || isNarrowFormatStyle(style) ||
            field == ERA && (style & SHORT) == SHORT) {
            Map<String, Integer> map;
            map = CalendarDataUtility.retrieveFieldValueNames(calendarType, field, style, locale);

            // Perform fallback here to follow the CLDR rules
            if (map == null) {
                if (isNarrowFormatStyle(style)) {
                    map = CalendarDataUtility.retrieveFieldValueNames(calendarType, field,
                                                                      toStandaloneStyle(style), locale);
                } else if (style != ALL_STYLES) {
                    map = CalendarDataUtility.retrieveFieldValueNames(calendarType, field,
                                                                      getBaseStyle(style), locale);
                }
            }
            return map;
        }

        // SHORT or LONG
        return getDisplayNamesImpl(field, style, locale);
    }

    private Map<String,Integer> getDisplayNamesImpl(int field, int style, Locale locale) {
        DateFormatSymbols symbols = DateFormatSymbols.getInstance(locale);
        String[] strings = getFieldStrings(field, style, symbols);
        if (strings != null) {
            Map<String,Integer> names = new HashMap<>();
            for (int i = 0; i < strings.length; i++) {
                if (strings[i].isEmpty()) {
                    continue;
                }
                names.put(strings[i], i);
            }
            return names;
        }
        return null;
    }

    boolean checkDisplayNameParams(int field, int style, int minStyle, int maxStyle,
                                   Locale locale, int fieldMask) {
        int baseStyle = getBaseStyle(style); // Ignore the standalone mask
        if (field < 0 || field >= fields.length ||
            baseStyle < minStyle || baseStyle > maxStyle || baseStyle == 3) {
            throw new IllegalArgumentException();
        }
        if (locale == null) {
            throw new NullPointerException();
        }
        return isFieldSet(fieldMask, field);
    }

    private String[] getFieldStrings(int field, int style, DateFormatSymbols symbols) {
        int baseStyle = getBaseStyle(style); // ignore the standalone mask

        // DateFormatSymbols doesn't support any narrow names.
        if (baseStyle == NARROW_FORMAT) {
            return null;
        }

        return switch (field) {
            case ERA         -> symbols.getEras();
            case MONTH       -> (baseStyle == LONG) ? symbols.getMonths() : symbols.getShortMonths();
            case DAY_OF_WEEK -> (baseStyle == LONG) ? symbols.getWeekdays() : symbols.getShortWeekdays();
            case AM_PM       -> symbols.getAmPmStrings();
            default -> null;
        };
    }

    /**
     * Fills in any unset fields in the calendar fields. First, the {@link
     * #computeTime()} method is called if the time value (millisecond offset
     * from the <a href="#Epoch">Epoch</a>) has not been calculated from
     * calendar field values. Then, the {@link #computeFields()} method is
     * called to calculate all calendar field values.
     */
    protected void complete()
    {
        if (!isTimeSet) {
            updateTime();
        }
        if (!areFieldsSet || !areAllFieldsSet) {
            computeFields(); // fills in unset fields
            areAllFieldsSet = areFieldsSet = true;
        }
    }

    /**
     * Returns whether the value of the specified calendar field has been set
     * externally by calling one of the setter methods rather than by the
     * internal time calculation.
     *
     * @return {@code true} if the field has been set externally,
     * {@code false} otherwise.
     * @throws    IndexOutOfBoundsException if the specified
     *                {@code field} is out of range
     *               (<code>field &lt; 0 || field &gt;= FIELD_COUNT</code>).
     * @see #selectFields()
     * @see #setFieldsComputed(int)
     */
    final boolean isExternallySet(int field) {
        return stamp[field] >= MINIMUM_USER_STAMP;
    }

    /**
     * Returns a field mask (bit mask) indicating all calendar fields that
     * have the state of externally or internally set.
     *
     * @return a bit mask indicating set state fields
     */
    final int getSetStateFields() {
        int mask = 0;
        for (int i = 0; i < fields.length; i++) {
            if (stamp[i] != UNSET) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /**
     * Sets the state of the specified calendar fields to
     * <em>computed</em>. This state means that the specified calendar fields
     * have valid values that have been set by internal time calculation
     * rather than by calling one of the setter methods.
     *
     * @param fieldMask the field to be marked as computed.
     * @throws    IndexOutOfBoundsException if the specified
     *                {@code field} is out of range
     *               (<code>field &lt; 0 || field &gt;= FIELD_COUNT</code>).
     * @see #isExternallySet(int)
     * @see #selectFields()
     */
    final void setFieldsComputed(int fieldMask) {
        if (fieldMask == ALL_FIELDS) {
            for (int i = 0; i < fields.length; i++) {
                stamp[i] = COMPUTED;
                isSet[i] = true;
            }
            areFieldsSet = areAllFieldsSet = true;
        } else {
            for (int i = 0; i < fields.length; i++) {
                if ((fieldMask & 1) == 1) {
                    stamp[i] = COMPUTED;
                    isSet[i] = true;
                } else {
                    if (areAllFieldsSet && !isSet[i]) {
                        areAllFieldsSet = false;
                    }
                }
                fieldMask >>>= 1;
            }
        }
    }

    /**
     * Sets the state of the calendar fields that are <em>not</em> specified
     * by {@code fieldMask} to <em>unset</em>. If {@code fieldMask}
     * specifies all the calendar fields, then the state of this
     * {@code Calendar} becomes that all the calendar fields are in sync
     * with the time value (millisecond offset from the Epoch).
     *
     * @param fieldMask the field mask indicating which calendar fields are in
     * sync with the time value.
     * @throws    IndexOutOfBoundsException if the specified
     *                {@code field} is out of range
     *               (<code>field &lt; 0 || field &gt;= FIELD_COUNT</code>).
     * @see #isExternallySet(int)
     * @see #selectFields()
     */
    final void setFieldsNormalized(int fieldMask) {
        if (fieldMask != ALL_FIELDS) {
            for (int i = 0; i < fields.length; i++) {
                if ((fieldMask & 1) == 0) {
                    stamp[i] = fields[i] = 0; // UNSET == 0
                    isSet[i] = false;
                }
                fieldMask >>= 1;
            }
        }

        // Some or all of the fields are in sync with the
        // milliseconds, but the stamp values are not normalized yet.
        areFieldsSet = true;
        areAllFieldsSet = false;
    }

    /**
     * Returns whether the calendar fields are partially in sync with the time
     * value or fully in sync but not stamp values are not normalized yet.
     */
    final boolean isPartiallyNormalized() {
        return areFieldsSet && !areAllFieldsSet;
    }

    /**
     * Returns whether the calendar fields are fully in sync with the time
     * value.
     */
    final boolean isFullyNormalized() {
        return areFieldsSet && areAllFieldsSet;
    }

    /**
     * Marks this Calendar as not sync'd.
     */
    final void setUnnormalized() {
        areFieldsSet = areAllFieldsSet = false;
    }

    /**
     * Returns whether the specified {@code field} is on in the
     * {@code fieldMask}.
     */
    static boolean isFieldSet(int fieldMask, int field) {
        return (fieldMask & (1 << field)) != 0;
    }

    /**
     * Returns a field mask indicating which calendar field values
     * to be used to calculate the time value. The calendar fields are
     * returned as a bit mask, each bit of which corresponds to a field, i.e.,
     * the mask value of {@code field} is <code>(1 &lt;&lt;
     * field)</code>. For example, 0x26 represents the {@code YEAR},
     * {@code MONTH}, and {@code DAY_OF_MONTH} fields (i.e., 0x26 is
     * equal to
     * <code>(1&lt;&lt;YEAR)|(1&lt;&lt;MONTH)|(1&lt;&lt;DAY_OF_MONTH))</code>.
     *
     * <p>This method supports the calendar fields resolution as described in
     * the class description. If the bit mask for a given field is on and its
     * field has not been set (i.e., {@code isSet(field)} is
     * {@code false}), then the default value of the field has to be
     * used, which case means that the field has been selected because the
     * selected combination involves the field.
     *
     * @return a bit mask of selected fields
     * @see #isExternallySet(int)
     */
    final int selectFields() {
        // This implementation has been taken from the GregorianCalendar class.

        // The YEAR field must always be used regardless of its SET
        // state because YEAR is a mandatory field to determine the date
        // and the default value (EPOCH_YEAR) may change through the
        // normalization process.
        int fieldMask = YEAR_MASK;

        if (stamp[ERA] != UNSET) {
            fieldMask |= ERA_MASK;
        }
        // Find the most recent group of fields specifying the day within
        // the year.  These may be any of the following combinations:
        //   MONTH + DAY_OF_MONTH
        //   MONTH + WEEK_OF_MONTH + DAY_OF_WEEK
        //   MONTH + DAY_OF_WEEK_IN_MONTH + DAY_OF_WEEK
        //   DAY_OF_YEAR
        //   WEEK_OF_YEAR + DAY_OF_WEEK
        // We look for the most recent of the fields in each group to determine
        // the age of the group.  For groups involving a week-related field such
        // as WEEK_OF_MONTH, DAY_OF_WEEK_IN_MONTH, or WEEK_OF_YEAR, both the
        // week-related field and the DAY_OF_WEEK must be set for the group as a
        // whole to be considered.  (See bug 4153860 - liu 7/24/98.)
        int dowStamp = stamp[DAY_OF_WEEK];
        int monthStamp = stamp[MONTH];
        int domStamp = stamp[DAY_OF_MONTH];
        int womStamp = aggregateStamp(stamp[WEEK_OF_MONTH], dowStamp);
        int dowimStamp = aggregateStamp(stamp[DAY_OF_WEEK_IN_MONTH], dowStamp);
        int doyStamp = stamp[DAY_OF_YEAR];
        int woyStamp = aggregateStamp(stamp[WEEK_OF_YEAR], dowStamp);

        int bestStamp = domStamp;
        if (womStamp > bestStamp) {
            bestStamp = womStamp;
        }
        if (dowimStamp > bestStamp) {
            bestStamp = dowimStamp;
        }
        if (doyStamp > bestStamp) {
            bestStamp = doyStamp;
        }
        if (woyStamp > bestStamp) {
            bestStamp = woyStamp;
        }

        /* No complete combination exists.  Look for WEEK_OF_MONTH,
         * DAY_OF_WEEK_IN_MONTH, or WEEK_OF_YEAR alone.  Treat DAY_OF_WEEK alone
         * as DAY_OF_WEEK_IN_MONTH.
         */
        if (bestStamp == UNSET) {
            womStamp = stamp[WEEK_OF_MONTH];
            dowimStamp = Math.max(stamp[DAY_OF_WEEK_IN_MONTH], dowStamp);
            woyStamp = stamp[WEEK_OF_YEAR];
            bestStamp = Math.max(Math.max(womStamp, dowimStamp), woyStamp);

            /* Treat MONTH alone or no fields at all as DAY_OF_MONTH.  This may
             * result in bestStamp = domStamp = UNSET if no fields are set,
             * which indicates DAY_OF_MONTH.
             */
            if (bestStamp == UNSET) {
                bestStamp = domStamp = monthStamp;
            }
        }

        if (bestStamp == domStamp ||
           (bestStamp == womStamp && stamp[WEEK_OF_MONTH] >= stamp[WEEK_OF_YEAR]) ||
           (bestStamp == dowimStamp && stamp[DAY_OF_WEEK_IN_MONTH] >= stamp[WEEK_OF_YEAR])) {
            fieldMask |= MONTH_MASK;
            if (bestStamp == domStamp) {
                fieldMask |= DAY_OF_MONTH_MASK;
            } else {
                assert (bestStamp == womStamp || bestStamp == dowimStamp);
                if (dowStamp != UNSET) {
                    fieldMask |= DAY_OF_WEEK_MASK;
                }
                if (womStamp == dowimStamp) {
                    // When they are equal, give the priority to
                    // WEEK_OF_MONTH for compatibility.
                    if (stamp[WEEK_OF_MONTH] >= stamp[DAY_OF_WEEK_IN_MONTH]) {
                        fieldMask |= WEEK_OF_MONTH_MASK;
                    } else {
                        fieldMask |= DAY_OF_WEEK_IN_MONTH_MASK;
                    }
                } else {
                    if (bestStamp == womStamp) {
                        fieldMask |= WEEK_OF_MONTH_MASK;
                    } else {
                        assert (bestStamp == dowimStamp);
                        if (stamp[DAY_OF_WEEK_IN_MONTH] != UNSET) {
                            fieldMask |= DAY_OF_WEEK_IN_MONTH_MASK;
                        }
                    }
                }
            }
        } else {
            assert (bestStamp == doyStamp || bestStamp == woyStamp ||
                    bestStamp == UNSET);
            if (bestStamp == doyStamp) {
                fieldMask |= DAY_OF_YEAR_MASK;
            } else {
                assert (bestStamp == woyStamp);
                if (dowStamp != UNSET) {
                    fieldMask |= DAY_OF_WEEK_MASK;
                }
                fieldMask |= WEEK_OF_YEAR_MASK;
            }
        }

        // Find the best set of fields specifying the time of day.  There
        // are only two possibilities here; the HOUR_OF_DAY or the
        // AM_PM and the HOUR.
        int hourOfDayStamp = stamp[HOUR_OF_DAY];
        int hourStamp = aggregateStamp(stamp[HOUR], stamp[AM_PM]);
        bestStamp = (hourStamp > hourOfDayStamp) ? hourStamp : hourOfDayStamp;

        // if bestStamp is still UNSET, then take HOUR or AM_PM. (See 4846659)
        if (bestStamp == UNSET) {
            bestStamp = Math.max(stamp[HOUR], stamp[AM_PM]);
        }

        // Hours
        if (bestStamp != UNSET) {
            if (bestStamp == hourOfDayStamp) {
                fieldMask |= HOUR_OF_DAY_MASK;
            } else {
                fieldMask |= HOUR_MASK;
                if (stamp[AM_PM] != UNSET) {
                    fieldMask |= AM_PM_MASK;
                }
            }
        }
        if (stamp[MINUTE] != UNSET) {
            fieldMask |= MINUTE_MASK;
        }
        if (stamp[SECOND] != UNSET) {
            fieldMask |= SECOND_MASK;
        }
        if (stamp[MILLISECOND] != UNSET) {
            fieldMask |= MILLISECOND_MASK;
        }
        if (stamp[ZONE_OFFSET] >= MINIMUM_USER_STAMP) {
                fieldMask |= ZONE_OFFSET_MASK;
        }
        if (stamp[DST_OFFSET] >= MINIMUM_USER_STAMP) {
            fieldMask |= DST_OFFSET_MASK;
        }

        return fieldMask;
    }

    int getBaseStyle(int style) {
        return style & ~STANDALONE_MASK;
    }

    private int toStandaloneStyle(int style) {
        return style | STANDALONE_MASK;
    }

    private boolean isStandaloneStyle(int style) {
        return (style & STANDALONE_MASK) != 0;
    }

    private boolean isNarrowStyle(int style) {
        return style == NARROW_FORMAT || style == NARROW_STANDALONE;
    }

    private boolean isNarrowFormatStyle(int style) {
        return style == NARROW_FORMAT;
    }

    /**
     * Returns the pseudo-time-stamp for two fields, given their
     * individual pseudo-time-stamps.  If either of the fields
     * is unset, then the aggregate is unset.  Otherwise, the
     * aggregate is the later of the two stamps.
     */
    private static int aggregateStamp(int stamp_a, int stamp_b) {
        if (stamp_a == UNSET || stamp_b == UNSET) {
            return UNSET;
        }
        return (stamp_a > stamp_b) ? stamp_a : stamp_b;
    }

    /**
     * Returns an unmodifiable {@code Set} containing all calendar types
     * supported by {@code Calendar} in the runtime environment. The available
     * calendar types can be used for the <a
     * href="Locale.html#def_locale_extension">Unicode locale extensions</a>.
     * The {@code Set} returned contains at least {@code "gregory"}. The
     * calendar types don't include aliases, such as {@code "gregorian"} for
     * {@code "gregory"}.
     *
     * @return an unmodifiable {@code Set} containing all available calendar types
     * @since 1.8
     * @see #getCalendarType()
     * @see Calendar.Builder#setCalendarType(String)
     * @see Locale#getUnicodeLocaleType(String)
     */
    public static Set<String> getAvailableCalendarTypes() {
        return AvailableCalendarTypes.SET;
    }

    private static class AvailableCalendarTypes {
        private static final Set<String> SET;
        static {
            Set<String> set = new HashSet<>(3);
            set.add("gregory");
            set.add("buddhist");
            set.add("japanese");
            SET = Collections.unmodifiableSet(set);
        }
        private AvailableCalendarTypes() {
        }
    }

    /**
     * Returns the calendar type of this {@code Calendar}. Calendar types are
     * defined by the <em>Unicode Locale Data Markup Language (LDML)</em>
     * specification.
     *
     * <p>The default implementation of this method returns the class name of
     * this {@code Calendar} instance. Any subclasses that implement
     * LDML-defined calendar systems should override this method to return
     * appropriate calendar types.
     *
     * @return the LDML-defined calendar type or the class name of this
     *         {@code Calendar} instance
     * @since 1.8
     * @see <a href="Locale.html#def_extensions">Locale extensions</a>
     * @see Locale.Builder#setLocale(Locale)
     * @see Locale.Builder#setUnicodeLocaleKeyword(String, String)
     */
    public String getCalendarType() {
        return this.getClass().getName();
    }

    /**
     * Compares this {@code Calendar} to the specified
     * {@code Object}.  The result is {@code true} if and only if
     * the argument is a {@code Calendar} object of the same calendar
     * system that represents the same time value (millisecond offset from the
     * <a href="#Epoch">Epoch</a>) under the same
     * {@code Calendar} parameters as this object.
     *
     * <p>The {@code Calendar} parameters are the values represented
     * by the {@code isLenient}, {@code getFirstDayOfWeek},
     * {@code getMinimalDaysInFirstWeek} and {@code getTimeZone}
     * methods. If there is any difference in those parameters
     * between the two {@code Calendar}s, this method returns
     * {@code false}.
     *
     * <p>Use the {@link #compareTo(Calendar) compareTo} method to
     * compare only the time values.
     *
     * @param obj the object to compare with.
     * @return {@code true} if this object is equal to {@code obj};
     * {@code false} otherwise.
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        try {
            Calendar that = (Calendar)obj;
            return compareTo(getMillisOf(that)) == 0 &&
                lenient == that.lenient &&
                firstDayOfWeek == that.firstDayOfWeek &&
                minimalDaysInFirstWeek == that.minimalDaysInFirstWeek &&
                (zone instanceof ZoneInfo ?
                    zone.equals(that.zone) :
                    zone.equals(that.getTimeZone()));
        } catch (Exception e) {
            // Note: GregorianCalendar.computeTime throws
            // IllegalArgumentException if the ERA value is invalid
            // even it's in lenient mode.
        }
        return false;
    }

    /**
     * Returns a hash code for this calendar.
     *
     * @return a hash code value for this object.
     * @since 1.2
     */
    @Override
    public int hashCode() {
        // 'otheritems' represents the hash code for the previous versions.
        int otheritems = (lenient ? 1 : 0)
            | (firstDayOfWeek << 1)
            | (minimalDaysInFirstWeek << 4)
            | (zone.hashCode() << 7);
        long t = getMillisOf(this);
        return (int) t ^ (int)(t >> 32) ^ otheritems;
    }

    /**
     * Returns whether this {@code Calendar} represents a time
     * before the time represented by the specified
     * {@code Object}. This method is equivalent to:
     * <pre>{@code
     *         compareTo(when) < 0
     * }</pre>
     * if and only if {@code when} is a {@code Calendar}
     * instance. Otherwise, the method returns {@code false}.
     *
     * @param when the {@code Object} to be compared
     * @return {@code true} if the time of this
     * {@code Calendar} is before the time represented by
     * {@code when}; {@code false} otherwise.
     * @see     #compareTo(Calendar)
     */
    public boolean before(Object when) {
        return when instanceof Calendar
            && compareTo((Calendar)when) < 0;
    }

    /**
     * Returns whether this {@code Calendar} represents a time
     * after the time represented by the specified
     * {@code Object}. This method is equivalent to:
     * <pre>{@code
     *         compareTo(when) > 0
     * }</pre>
     * if and only if {@code when} is a {@code Calendar}
     * instance. Otherwise, the method returns {@code false}.
     *
     * @param when the {@code Object} to be compared
     * @return {@code true} if the time of this {@code Calendar} is
     * after the time represented by {@code when}; {@code false}
     * otherwise.
     * @see     #compareTo(Calendar)
     */
    public boolean after(Object when) {
        return when instanceof Calendar
            && compareTo((Calendar)when) > 0;
    }

    /**
     * Compares the time values (millisecond offsets from the <a
     * href="#Epoch">Epoch</a>) represented by two
     * {@code Calendar} objects.
     *
     * @param anotherCalendar the {@code Calendar} to be compared.
     * @return the value {@code 0} if the time represented by the argument
     * is equal to the time represented by this {@code Calendar}; a value
     * less than {@code 0} if the time of this {@code Calendar} is
     * before the time represented by the argument; and a value greater than
     * {@code 0} if the time of this {@code Calendar} is after the
     * time represented by the argument.
     * @throws    NullPointerException if the specified {@code Calendar} is
     *            {@code null}.
     * @throws    IllegalArgumentException if the time value of the
     * specified {@code Calendar} object can't be obtained due to
     * any invalid calendar values.
     * @since   1.5
     */
    @Override
    public int compareTo(Calendar anotherCalendar) {
        return compareTo(getMillisOf(anotherCalendar));
    }

    /**
     * Adds or subtracts the specified amount of time to the given calendar field,
     * based on the calendar's rules. For example, to subtract 5 days from
     * the current time of the calendar, you can achieve it by calling:
     * <p>{@code add(Calendar.DAY_OF_MONTH, -5)}.
     *
     * @param field the calendar field.
     * @param amount the amount of date or time to be added to the field.
     * @see #roll(int,int)
     * @see #set(int,int)
     */
    public abstract void add(int field, int amount);

    /**
     * Adds or subtracts (up/down) a single unit of time on the given time
     * field without changing larger fields. For example, to roll the current
     * date up by one day, you can achieve it by calling:
     * <p>roll(Calendar.DATE, true).
     * When rolling on the year or Calendar.YEAR field, it will roll the year
     * value in the range between 1 and the value returned by calling
     * {@code getMaximum(Calendar.YEAR)}.
     * When rolling on the month or Calendar.MONTH field, other fields like
     * date might conflict and, need to be changed. For instance,
     * rolling the month on the date 01/31/96 will result in 02/29/96.
     * When rolling on the hour-in-day or Calendar.HOUR_OF_DAY field, it will
     * roll the hour value in the range between 0 and 23, which is zero-based.
     *
     * @param field the time field.
     * @param up indicates if the value of the specified time field is to be
     * rolled up or rolled down. Use true if rolling up, false otherwise.
     * @see Calendar#add(int,int)
     * @see Calendar#set(int,int)
     */
    public abstract void roll(int field, boolean up);

    /**
     * Adds the specified (signed) amount to the specified calendar field
     * without changing larger fields.  A negative amount means to roll
     * down.
     *
     * <p>NOTE:  This default implementation on {@code Calendar} just repeatedly calls the
     * version of {@link #roll(int,boolean) roll()} that rolls by one unit.  This may not
     * always do the right thing.  For example, if the {@code DAY_OF_MONTH} field is 31,
     * rolling through February will leave it set to 28.  The {@code GregorianCalendar}
     * version of this function takes care of this problem.  Other subclasses
     * should also provide overrides of this function that do the right thing.
     *
     * @param field the calendar field.
     * @param amount the signed amount to add to the calendar {@code field}.
     * @since 1.2
     * @see #roll(int,boolean)
     * @see #add(int,int)
     * @see #set(int,int)
     */
    public void roll(int field, int amount)
    {
        while (amount > 0) {
            roll(field, true);
            amount--;
        }
        while (amount < 0) {
            roll(field, false);
            amount++;
        }
    }

    /**
     * Sets the time zone with the given time zone value.
     *
     * @param value the given time zone.
     */
    public void setTimeZone(TimeZone value)
    {
        zone = value;
        sharedZone = false;
        /* Recompute the fields from the time using the new zone.  This also
         * works if isTimeSet is false (after a call to set()).  In that case
         * the time will be computed from the fields using the new zone, then
         * the fields will get recomputed from that.  Consider the sequence of
         * calls: cal.setTimeZone(EST); cal.set(HOUR, 1); cal.setTimeZone(PST).
         * Is cal set to 1 o'clock EST or 1 o'clock PST?  Answer: PST.  More
         * generally, a call to setTimeZone() affects calls to set() BEFORE AND
         * AFTER it up to the next call to complete().
         */
        areAllFieldsSet = areFieldsSet = false;
    }

    /**
     * Gets the time zone.
     *
     * @return the time zone object associated with this calendar.
     */
    public TimeZone getTimeZone()
    {
        // If the TimeZone object is shared by other Calendar instances, then
        // create a clone.
        if (sharedZone) {
            zone = (TimeZone) zone.clone();
            sharedZone = false;
        }
        return zone;
    }

    /**
     * Returns the time zone (without cloning).
     */
    TimeZone getZone() {
        return zone;
    }

    /**
     * Sets the sharedZone flag to {@code shared}.
     */
    void setZoneShared(boolean shared) {
        sharedZone = shared;
    }

    /**
     * Specifies whether or not date/time interpretation is to be lenient.  With
     * lenient interpretation, a date such as "February 942, 1996" will be
     * treated as being equivalent to the 941st day after February 1, 1996.
     * With strict (non-lenient) interpretation, such dates will cause an exception to be
     * thrown. The default is lenient.
     *
     * @param lenient {@code true} if the lenient mode is to be turned
     * on; {@code false} if it is to be turned off.
     * @see #isLenient()
     * @see java.text.DateFormat#setLenient
     */
    public void setLenient(boolean lenient)
    {
        this.lenient = lenient;
    }

    /**
     * Tells whether date/time interpretation is to be lenient.
     *
     * @return {@code true} if the interpretation mode of this calendar is lenient;
     * {@code false} otherwise.
     * @see #setLenient(boolean)
     */
    public boolean isLenient()
    {
        return lenient;
    }

    /**
     * Sets what the first day of the week is; e.g., {@code SUNDAY} in the U.S.,
     * {@code MONDAY} in France.
     *
     * @param value the given first day of the week.
     * @see #getFirstDayOfWeek()
     * @see #getMinimalDaysInFirstWeek()
     */
    public void setFirstDayOfWeek(int value)
    {
        if (firstDayOfWeek == value) {
            return;
        }
        firstDayOfWeek = value;
        invalidateWeekFields();
    }

    /**
     * Gets what the first day of the week is; e.g., {@code SUNDAY} in the U.S.,
     * {@code MONDAY} in France.
     *
     * @return the first day of the week.
     * @see #setFirstDayOfWeek(int)
     * @see #getMinimalDaysInFirstWeek()
     */
    public int getFirstDayOfWeek()
    {
        return firstDayOfWeek;
    }

    /**
     * Sets what the minimal days required in the first week of the year are;
     * For example, if the first week is defined as one that contains the first
     * day of the first month of a year, call this method with value 1. If it
     * must be a full week, use value 7.
     *
     * @param value the given minimal days required in the first week
     * of the year.
     * @see #getMinimalDaysInFirstWeek()
     */
    public void setMinimalDaysInFirstWeek(int value)
    {
        if (minimalDaysInFirstWeek == value) {
            return;
        }
        minimalDaysInFirstWeek = value;
        invalidateWeekFields();
    }

    /**
     * Gets what the minimal days required in the first week of the year are;
     * e.g., if the first week is defined as one that contains the first day
     * of the first month of a year, this method returns 1. If
     * the minimal days required must be a full week, this method
     * returns 7.
     *
     * @return the minimal days required in the first week of the year.
     * @see #setMinimalDaysInFirstWeek(int)
     */
    public int getMinimalDaysInFirstWeek()
    {
        return minimalDaysInFirstWeek;
    }

    /**
     * Returns whether this {@code Calendar} supports week dates.
     *
     * <p>The default implementation of this method returns {@code false}.
     *
     * @return {@code true} if this {@code Calendar} supports week dates;
     *         {@code false} otherwise.
     * @see #getWeekYear()
     * @see #setWeekDate(int,int,int)
     * @see #getWeeksInWeekYear()
     * @since 1.7
     */
    public boolean isWeekDateSupported() {
        return false;
    }

    /**
     * Returns the week year represented by this {@code Calendar}. The
     * week year is in sync with the week cycle. The {@linkplain
     * #getFirstDayOfWeek() first day of the first week} is the first
     * day of the week year.
     *
     * <p>The default implementation of this method throws an
     * {@link UnsupportedOperationException}.
     *
     * @return the week year of this {@code Calendar}
     * @throws    UnsupportedOperationException
     *            if any week year numbering isn't supported
     *            in this {@code Calendar}.
     * @see #isWeekDateSupported()
     * @see #getFirstDayOfWeek()
     * @see #getMinimalDaysInFirstWeek()
     * @since 1.7
     */
    public int getWeekYear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the date of this {@code Calendar} with the given date
     * specifiers - week year, week of year, and day of week.
     *
     * <p>Unlike the {@code set} method, all of the calendar fields
     * and {@code time} values are calculated upon return.
     *
     * <p>If {@code weekOfYear} is out of the valid week-of-year range
     * in {@code weekYear}, the {@code weekYear} and {@code
     * weekOfYear} values are adjusted in lenient mode, or an {@code
     * IllegalArgumentException} is thrown in non-lenient mode.
     *
     * <p>The default implementation of this method throws an
     * {@code UnsupportedOperationException}.
     *
     * @param weekYear   the week year
     * @param weekOfYear the week number based on {@code weekYear}
     * @param dayOfWeek  the day of week value: one of the constants
     *                   for the {@link #DAY_OF_WEEK} field: {@link
     *                   #SUNDAY}, ..., {@link #SATURDAY}.
     * @throws    IllegalArgumentException
     *            if any of the given date specifiers is invalid
     *            or any of the calendar fields are inconsistent
     *            with the given date specifiers in non-lenient mode
     * @throws    UnsupportedOperationException
     *            if any week year numbering isn't supported in this
     *            {@code Calendar}.
     * @see #isWeekDateSupported()
     * @see #getFirstDayOfWeek()
     * @see #getMinimalDaysInFirstWeek()
     * @since 1.7
     */
    public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the number of weeks in the week year represented by this
     * {@code Calendar}.
     *
     * <p>The default implementation of this method throws an
     * {@code UnsupportedOperationException}.
     *
     * @return the number of weeks in the week year.
     * @throws    UnsupportedOperationException
     *            if any week year numbering isn't supported in this
     *            {@code Calendar}.
     * @see #WEEK_OF_YEAR
     * @see #isWeekDateSupported()
     * @see #getWeekYear()
     * @see #getActualMaximum(int)
     * @since 1.7
     */
    public int getWeeksInWeekYear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the minimum value for the given calendar field of this
     * {@code Calendar} instance. The minimum value is defined as
     * the smallest value returned by the {@link #get(int) get} method
     * for any possible time value.  The minimum value depends on
     * calendar system specific parameters of the instance.
     *
     * @param field the calendar field.
     * @return the minimum value for the given calendar field.
     * @see #getMaximum(int)
     * @see #getGreatestMinimum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMinimum(int)
     * @see #getActualMaximum(int)
     */
    public abstract int getMinimum(int field);

    /**
     * Returns the maximum value for the given calendar field of this
     * {@code Calendar} instance. The maximum value is defined as
     * the largest value returned by the {@link #get(int) get} method
     * for any possible time value. The maximum value depends on
     * calendar system specific parameters of the instance.
     *
     * @param field the calendar field.
     * @return the maximum value for the given calendar field.
     * @see #getMinimum(int)
     * @see #getGreatestMinimum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMinimum(int)
     * @see #getActualMaximum(int)
     */
    public abstract int getMaximum(int field);

    /**
     * Returns the highest minimum value for the given calendar field
     * of this {@code Calendar} instance. The highest minimum
     * value is defined as the largest value returned by {@link
     * #getActualMinimum(int)} for any possible time value. The
     * greatest minimum value depends on calendar system specific
     * parameters of the instance.
     *
     * @param field the calendar field.
     * @return the highest minimum value for the given calendar field.
     * @see #getMinimum(int)
     * @see #getMaximum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMinimum(int)
     * @see #getActualMaximum(int)
     */
    public abstract int getGreatestMinimum(int field);

    /**
     * Returns the lowest maximum value for the given calendar field
     * of this {@code Calendar} instance. The lowest maximum
     * value is defined as the smallest value returned by {@link
     * #getActualMaximum(int)} for any possible time value. The least
     * maximum value depends on calendar system specific parameters of
     * the instance. For example, a {@code Calendar} for the
     * Gregorian calendar system returns 28 for the
     * {@code DAY_OF_MONTH} field, because the 28th is the last
     * day of the shortest month of this calendar, February in a
     * common year.
     *
     * @param field the calendar field.
     * @return the lowest maximum value for the given calendar field.
     * @see #getMinimum(int)
     * @see #getMaximum(int)
     * @see #getGreatestMinimum(int)
     * @see #getActualMinimum(int)
     * @see #getActualMaximum(int)
     */
    public abstract int getLeastMaximum(int field);

    /**
     * Returns the minimum value that the specified calendar field
     * could have, given the time value of this {@code Calendar}.
     *
     * <p>The default implementation of this method uses an iterative
     * algorithm to determine the actual minimum value for the
     * calendar field. Subclasses should, if possible, override this
     * with a more efficient implementation - in many cases, they can
     * simply return {@code getMinimum()}.
     *
     * @param field the calendar field
     * @return the minimum of the given calendar field for the time
     * value of this {@code Calendar}
     * @see #getMinimum(int)
     * @see #getMaximum(int)
     * @see #getGreatestMinimum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMaximum(int)
     * @since 1.2
     */
    public int getActualMinimum(int field) {
        int fieldValue = getGreatestMinimum(field);
        int endValue = getMinimum(field);

        // if we know that the minimum value is always the same, just return it
        if (fieldValue == endValue) {
            return fieldValue;
        }

        // clone the calendar so we don't mess with the real one, and set it to
        // accept anything for the field values
        Calendar work = (Calendar)this.clone();
        work.setLenient(true);

        // now try each value from getLeastMaximum() to getMaximum() one by one until
        // we get a value that normalizes to another value.  The last value that
        // normalizes to itself is the actual minimum for the current date
        int result = fieldValue;

        do {
            work.set(field, fieldValue);
            if (work.get(field) != fieldValue) {
                break;
            } else {
                result = fieldValue;
                fieldValue--;
            }
        } while (fieldValue >= endValue);

        return result;
    }

    /**
     * Returns the maximum value that the specified calendar field
     * could have, given the time value of this
     * {@code Calendar}. For example, the actual maximum value of
     * the {@code MONTH} field is 12 in some years, and 13 in
     * other years in the Hebrew calendar system.
     *
     * <p>The default implementation of this method uses an iterative
     * algorithm to determine the actual maximum value for the
     * calendar field. Subclasses should, if possible, override this
     * with a more efficient implementation.
     *
     * @param field the calendar field
     * @return the maximum of the given calendar field for the time
     * value of this {@code Calendar}
     * @see #getMinimum(int)
     * @see #getMaximum(int)
     * @see #getGreatestMinimum(int)
     * @see #getLeastMaximum(int)
     * @see #getActualMinimum(int)
     * @since 1.2
     */
    public int getActualMaximum(int field) {
        int fieldValue = getLeastMaximum(field);
        int endValue = getMaximum(field);

        // if we know that the maximum value is always the same, just return it.
        if (fieldValue == endValue) {
            return fieldValue;
        }

        // clone the calendar so we don't mess with the real one, and set it to
        // accept anything for the field values.
        Calendar work = (Calendar)this.clone();
        work.setLenient(true);

        // if we're counting weeks, set the day of the week to Sunday.  We know the
        // last week of a month or year will contain the first day of the week.
        if (field == WEEK_OF_YEAR || field == WEEK_OF_MONTH) {
            work.set(DAY_OF_WEEK, firstDayOfWeek);
        }

        // now try each value from getLeastMaximum() to getMaximum() one by one until
        // we get a value that normalizes to another value.  The last value that
        // normalizes to itself is the actual maximum for the current date
        int result = fieldValue;

        do {
            work.set(field, fieldValue);
            if (work.get(field) != fieldValue) {
                break;
            } else {
                result = fieldValue;
                fieldValue++;
            }
        } while (fieldValue <= endValue);

        return result;
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object.
     */
    @Override
    public Object clone()
    {
        try {
            Calendar other = (Calendar) super.clone();

            other.fields = new int[FIELD_COUNT];
            other.isSet = new boolean[FIELD_COUNT];
            other.stamp = new int[FIELD_COUNT];
            for (int i = 0; i < FIELD_COUNT; i++) {
                other.fields[i] = fields[i];
                other.stamp[i] = stamp[i];
                other.isSet[i] = isSet[i];
            }
            if (!sharedZone) {
                other.zone = (TimeZone) zone.clone();
            }
            return other;
        }
        catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }

    private static final String[] FIELD_NAME = {
        "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH",
        "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR",
        "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET",
        "DST_OFFSET"
    };

    /**
     * Returns the name of the specified calendar field.
     *
     * @param field the calendar field
     * @return the calendar field name
     * @throws    IndexOutOfBoundsException if {@code field} is negative,
     * equal to or greater than {@code FIELD_COUNT}.
     */
    static String getFieldName(int field) {
        return FIELD_NAME[field];
    }

    /**
     * Return a string representation of this calendar. This method
     * is intended to be used only for debugging purposes, and the
     * format of the returned string may vary between implementations.
     * The returned string may be empty but may not be {@code null}.
     *
     * @return  a string representation of this calendar.
     */
    @Override
    public String toString() {
        // NOTE: BuddhistCalendar.toString() interprets the string
        // produced by this method so that the Gregorian year number
        // is substituted by its B.E. year value. It relies on
        // "...,YEAR=<year>,..." or "...,YEAR=?,...".
        StringBuilder buffer = new StringBuilder(800);
        buffer.append(getClass().getName()).append('[');
        appendValue(buffer, "time", isTimeSet, time);
        buffer.append(",areFieldsSet=").append(areFieldsSet);
        buffer.append(",areAllFieldsSet=").append(areAllFieldsSet);
        buffer.append(",lenient=").append(lenient);
        buffer.append(",zone=").append(zone);
        appendValue(buffer, ",firstDayOfWeek", true, (long) firstDayOfWeek);
        appendValue(buffer, ",minimalDaysInFirstWeek", true, (long) minimalDaysInFirstWeek);
        for (int i = 0; i < FIELD_COUNT; ++i) {
            buffer.append(',');
            appendValue(buffer, FIELD_NAME[i], isSet(i), (long) fields[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    // =======================privates===============================

    private static void appendValue(StringBuilder sb, String item, boolean valid, long value) {
        sb.append(item).append('=');
        if (valid) {
            sb.append(value);
        } else {
            sb.append('?');
        }
    }

    /**
     * Both firstDayOfWeek and minimalDaysInFirstWeek are locale-dependent.
     * They are used to figure out the week count for a specific date for
     * a given locale. These must be set when a Calendar is constructed.
     * @param desiredLocale the given locale.
     */
    private void setWeekCountData(Locale desiredLocale)
    {
        /* try to get the Locale data from the cache */
        int[] data = cachedLocaleData.get(desiredLocale);
        if (data == null) {  /* cache miss */
            data = new int[2];
            data[0] = CalendarDataUtility.retrieveFirstDayOfWeek(desiredLocale);
            data[1] = CalendarDataUtility.retrieveMinimalDaysInFirstWeek(desiredLocale);
            cachedLocaleData.putIfAbsent(desiredLocale, data);
        }
        firstDayOfWeek = data[0];
        minimalDaysInFirstWeek = data[1];
    }

    /**
     * Recomputes the time and updates the status fields isTimeSet
     * and areFieldsSet.  Callers should check isTimeSet and only
     * call this method if isTimeSet is false.
     */
    private void updateTime() {
        computeTime();
        // The areFieldsSet and areAllFieldsSet values are no longer
        // controlled here (as of 1.5).
        isTimeSet = true;
    }

    private int compareTo(long t) {
        long thisTime = getMillisOf(this);
        return (thisTime > t) ? 1 : (thisTime == t) ? 0 : -1;
    }

    private static long getMillisOf(Calendar calendar) {
        if (calendar.isTimeSet) {
            return calendar.time;
        }
        Calendar cal = (Calendar) calendar.clone();
        cal.setLenient(true);
        return cal.getTimeInMillis();
    }

    /**
     * Adjusts the stamp[] values before nextStamp overflow. nextStamp
     * is set to the next stamp value upon the return.
     */
    private void adjustStamp() {
        int max = MINIMUM_USER_STAMP;
        int newStamp = MINIMUM_USER_STAMP;

        for (;;) {
            int min = Integer.MAX_VALUE;
            for (int v : stamp) {
                if (v >= newStamp && min > v) {
                    min = v;
                }
                if (max < v) {
                    max = v;
                }
            }
            if (max != min && min == Integer.MAX_VALUE) {
                break;
            }
            for (int i = 0; i < stamp.length; i++) {
                if (stamp[i] == min) {
                    stamp[i] = newStamp;
                }
            }
            newStamp++;
            if (min == max) {
                break;
            }
        }
        nextStamp = newStamp;
    }

    /**
     * Sets the WEEK_OF_MONTH and WEEK_OF_YEAR fields to new values with the
     * new parameter value if they have been calculated internally.
     */
    private void invalidateWeekFields()
    {
        if (stamp[WEEK_OF_MONTH] != COMPUTED &&
            stamp[WEEK_OF_YEAR] != COMPUTED) {
            return;
        }

        // We have to check the new values of these fields after changing
        // firstDayOfWeek and/or minimalDaysInFirstWeek. If the field values
        // have been changed, then set the new values. (4822110)
        Calendar cal = (Calendar) clone();
        cal.setLenient(true);
        cal.clear(WEEK_OF_MONTH);
        cal.clear(WEEK_OF_YEAR);

        if (stamp[WEEK_OF_MONTH] == COMPUTED) {
            int weekOfMonth = cal.get(WEEK_OF_MONTH);
            if (fields[WEEK_OF_MONTH] != weekOfMonth) {
                fields[WEEK_OF_MONTH] = weekOfMonth;
            }
        }

        if (stamp[WEEK_OF_YEAR] == COMPUTED) {
            int weekOfYear = cal.get(WEEK_OF_YEAR);
            if (fields[WEEK_OF_YEAR] != weekOfYear) {
                fields[WEEK_OF_YEAR] = weekOfYear;
            }
        }
    }

    /**
     * Save the state of this object to a stream (i.e., serialize it).
     *
     * Ideally, {@code Calendar} would only write out its state data and
     * the current time, and not write any field data out, such as
     * {@code fields[]}, {@code isTimeSet}, {@code areFieldsSet},
     * and {@code isSet[]}.  {@code nextStamp} also should not be part
     * of the persistent state. Unfortunately, this didn't happen before JDK 1.1
     * shipped. To be compatible with JDK 1.1, we will always have to write out
     * the field values and state flags.  However, {@code nextStamp} can be
     * removed from the serialization stream; this will probably happen in the
     * near future.
     */
    @java.io.Serial
    private synchronized void writeObject(ObjectOutputStream stream)
         throws IOException
    {
        // Try to compute the time correctly, for the future (stream
        // version 2) in which we don't write out fields[] or isSet[].
        if (!isTimeSet) {
            try {
                updateTime();
            }
            catch (IllegalArgumentException e) {}
        }

        // If this Calendar has a ZoneInfo, save it and set a
        // SimpleTimeZone equivalent (as a single DST schedule) for
        // backward compatibility.
        TimeZone savedZone = null;
        if (zone instanceof ZoneInfo) {
            SimpleTimeZone stz = ((ZoneInfo)zone).getLastRuleInstance();
            if (stz == null) {
                stz = new SimpleTimeZone(zone.getRawOffset(), zone.getID());
            }
            savedZone = zone;
            zone = stz;
        }

        // Write out the 1.1 FCS object.
        stream.defaultWriteObject();

        // Write out the ZoneInfo object
        // 4802409: we write out even if it is null, a temporary workaround
        // the real fix for bug 4844924 in corba-iiop
        stream.writeObject(savedZone);
        if (savedZone != null) {
            zone = savedZone;
        }
    }

    @SuppressWarnings("removal")
    private static class CalendarAccessControlContext {
        private static final AccessControlContext INSTANCE;
        static {
            RuntimePermission perm = new RuntimePermission("accessClassInPackage.sun.util.calendar");
            PermissionCollection perms = perm.newPermissionCollection();
            perms.add(perm);
            INSTANCE = new AccessControlContext(new ProtectionDomain[] {
                                                    new ProtectionDomain(null, perms)
                                                });
        }
        private CalendarAccessControlContext() {
        }
    }

    /**
     * Reconstitutes this object from a stream (i.e., deserialize it).
     */
    @SuppressWarnings("removal")
    @java.io.Serial
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        final ObjectInputStream input = stream;
        input.defaultReadObject();

        stamp = new int[FIELD_COUNT];

        // Starting with version 2 (not implemented yet), we expect that
        // fields[], isSet[], isTimeSet, and areFieldsSet may not be
        // streamed out anymore.  We expect 'time' to be correct.
        if (serialVersionOnStream >= 2)
        {
            isTimeSet = true;
            if (fields == null) {
                fields = new int[FIELD_COUNT];
            }
            if (isSet == null) {
                isSet = new boolean[FIELD_COUNT];
            }
        }
        else if (serialVersionOnStream >= 0)
        {
            for (int i=0; i<FIELD_COUNT; ++i) {
                stamp[i] = isSet[i] ? COMPUTED : UNSET;
            }
        }

        serialVersionOnStream = currentSerialVersion;

        // If there's a ZoneInfo object, use it for zone.
        ZoneInfo zi = null;
        try {
            zi = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<>() {
                        @Override
                        public ZoneInfo run() throws Exception {
                            return (ZoneInfo) input.readObject();
                        }
                    },
                    CalendarAccessControlContext.INSTANCE);
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if (!(e instanceof OptionalDataException)) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else if (e instanceof IOException) {
                    throw (IOException) e;
                } else if (e instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) e;
                }
                throw new RuntimeException(e);
            }
        }
        if (zi != null) {
            zone = zi;
        }

        // If the deserialized object has a SimpleTimeZone, try to
        // replace it with a ZoneInfo equivalent (as of 1.4) in order
        // to be compatible with the SimpleTimeZone-based
        // implementation as much as possible.
        if (zone instanceof SimpleTimeZone) {
            String id = zone.getID();
            TimeZone tz = TimeZone.getTimeZone(id);
            if (tz != null && tz.hasSameRules(zone) && tz.getID().equals(id)) {
                zone = tz;
            }
        }
    }

    /**
     * Converts this object to an {@link Instant}.
     * <p>
     * The conversion creates an {@code Instant} that represents the
     * same point on the time-line as this {@code Calendar}.
     *
     * @return the instant representing the same point on the time-line
     * @since 1.8
     */
    public final Instant toInstant() {
        return Instant.ofEpochMilli(getTimeInMillis());
    }
}
