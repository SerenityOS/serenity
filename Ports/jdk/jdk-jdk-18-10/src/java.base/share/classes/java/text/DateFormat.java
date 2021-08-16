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
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.io.InvalidObjectException;
import java.text.spi.DateFormatProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.spi.LocaleServiceProvider;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleServiceProviderPool;

/**
 * {@code DateFormat} is an abstract class for date/time formatting subclasses which
 * formats and parses dates or time in a language-independent manner.
 * The date/time formatting subclass, such as {@link SimpleDateFormat}, allows for
 * formatting (i.e., date &rarr; text), parsing (text &rarr; date), and
 * normalization.  The date is represented as a {@code Date} object or
 * as the milliseconds since January 1, 1970, 00:00:00 GMT.
 *
 * <p>{@code DateFormat} provides many class methods for obtaining default date/time
 * formatters based on the default or a given locale and a number of formatting
 * styles. The formatting styles include {@link #FULL}, {@link #LONG}, {@link #MEDIUM}, and {@link #SHORT}. More
 * detail and examples of using these styles are provided in the method
 * descriptions.
 *
 * <p>{@code DateFormat} helps you to format and parse dates for any locale.
 * Your code can be completely independent of the locale conventions for
 * months, days of the week, or even the calendar format: lunar vs. solar.
 *
 * <p>To format a date for the current Locale, use one of the
 * static factory methods:
 * <blockquote>
 * <pre>{@code
 * myString = DateFormat.getDateInstance().format(myDate);
 * }</pre>
 * </blockquote>
 * <p>If you are formatting multiple dates, it is
 * more efficient to get the format and use it multiple times so that
 * the system doesn't have to fetch the information about the local
 * language and country conventions multiple times.
 * <blockquote>
 * <pre>{@code
 * DateFormat df = DateFormat.getDateInstance();
 * for (int i = 0; i < myDate.length; ++i) {
 *     output.println(df.format(myDate[i]) + "; ");
 * }
 * }</pre>
 * </blockquote>
 * <p>To format a date for a different Locale, specify it in the
 * call to {@link #getDateInstance(int, Locale) getDateInstance()}.
 * <blockquote>
 * <pre>{@code
 * DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);
 * }</pre>
 * </blockquote>
 *
 * <p>If the specified locale contains "ca" (calendar), "rg" (region override),
 * and/or "tz" (timezone) <a href="../util/Locale.html#def_locale_extension">Unicode
 * extensions</a>, the calendar, the country and/or the time zone for formatting
 * are overridden. If both "ca" and "rg" are specified, the calendar from the "ca"
 * extension supersedes the implicit one from the "rg" extension.
 *
 * <p>You can use a DateFormat to parse also.
 * <blockquote>
 * <pre>{@code
 * myDate = df.parse(myString);
 * }</pre>
 * </blockquote>
 * <p>Use {@code getDateInstance} to get the normal date format for that country.
 * There are other static factory methods available.
 * Use {@code getTimeInstance} to get the time format for that country.
 * Use {@code getDateTimeInstance} to get a date and time format. You can pass in
 * different options to these factory methods to control the length of the
 * result; from {@link #SHORT} to {@link #MEDIUM} to {@link #LONG} to {@link #FULL}. The exact result depends
 * on the locale, but generally:
 * <ul><li>{@link #SHORT} is completely numeric, such as {@code 12.13.52} or {@code 3:30pm}
 * <li>{@link #MEDIUM} is longer, such as {@code Jan 12, 1952}
 * <li>{@link #LONG} is longer, such as {@code January 12, 1952} or {@code 3:30:32pm}
 * <li>{@link #FULL} is pretty completely specified, such as
 * {@code Tuesday, April 12, 1952 AD or 3:30:42pm PST}.
 * </ul>
 *
 * <p>You can also set the time zone on the format if you wish.
 * If you want even more control over the format or parsing,
 * (or want to give your users more control),
 * you can try casting the {@code DateFormat} you get from the factory methods
 * to a {@link SimpleDateFormat}. This will work for the majority
 * of countries; just remember to put it in a {@code try} block in case you
 * encounter an unusual one.
 *
 * <p>You can also use forms of the parse and format methods with
 * {@link ParsePosition} and {@link FieldPosition} to
 * allow you to
 * <ul><li>progressively parse through pieces of a string.
 * <li>align any particular field, or find out where it is for selection
 * on the screen.
 * </ul>
 *
 * <h2><a id="synchronization">Synchronization</a></h2>
 *
 * <p>
 * Date formats are not synchronized.
 * It is recommended to create separate format instances for each thread.
 * If multiple threads access a format concurrently, it must be synchronized
 * externally.
 * @apiNote Consider using {@link java.time.format.DateTimeFormatter} as an
 * immutable and thread-safe alternative.
 *
 * @implSpec
 * <ul><li>The {@link #format(Date, StringBuffer, FieldPosition)} and
 * {@link #parse(String, ParsePosition)} methods may throw
 * {@code NullPointerException}, if any of their parameter is {@code null}.
 * The subclass may provide its own implementation and specification about
 * {@code NullPointerException}.</li>
 * <li>The {@link #setCalendar(Calendar)}, {@link
 * #setNumberFormat(NumberFormat)} and {@link #setTimeZone(TimeZone)} methods
 * do not throw {@code NullPointerException} when their parameter is
 * {@code null}, but any subsequent operations on the same instance may throw
 * {@code NullPointerException}.</li>
 * <li>The {@link #getCalendar()}, {@link #getNumberFormat()} and
 * {@link getTimeZone()} methods may return {@code null}, if the respective
 * values of this instance is set to {@code null} through the corresponding
 * setter methods. For Example: {@link #getTimeZone()} may return {@code null},
 * if the {@code TimeZone} value of this instance is set as
 * {@link #setTimeZone(java.util.TimeZone) setTimeZone(null)}.</li>
 * </ul>
 *
 * @see          Format
 * @see          NumberFormat
 * @see          SimpleDateFormat
 * @see          java.util.Calendar
 * @see          java.util.GregorianCalendar
 * @see          java.util.TimeZone
 * @see          java.time.format.DateTimeFormatter
 * @author       Mark Davis, Chen-Lieh Huang, Alan Liu
 * @since 1.1
 */
public abstract class DateFormat extends Format {

    /**
     * The {@link Calendar} instance used for calculating the date-time fields
     * and the instant of time. This field is used for both formatting and
     * parsing.
     *
     * <p>Subclasses should initialize this field to a {@link Calendar}
     * appropriate for the {@link Locale} associated with this
     * {@code DateFormat}.
     * @serial
     */
    protected Calendar calendar;

    /**
     * The number formatter that {@code DateFormat} uses to format numbers
     * in dates and times.  Subclasses should initialize this to a number format
     * appropriate for the locale associated with this {@code DateFormat}.
     * @serial
     */
    protected NumberFormat numberFormat;

    /**
     * Useful constant for ERA field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int ERA_FIELD = 0;
    /**
     * Useful constant for YEAR field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int YEAR_FIELD = 1;
    /**
     * Useful constant for MONTH field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int MONTH_FIELD = 2;
    /**
     * Useful constant for DATE field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int DATE_FIELD = 3;
    /**
     * Useful constant for one-based HOUR_OF_DAY field alignment.
     * Used in FieldPosition of date/time formatting.
     * HOUR_OF_DAY1_FIELD is used for the one-based 24-hour clock.
     * For example, 23:59 + 01:00 results in 24:59.
     */
    public static final int HOUR_OF_DAY1_FIELD = 4;
    /**
     * Useful constant for zero-based HOUR_OF_DAY field alignment.
     * Used in FieldPosition of date/time formatting.
     * HOUR_OF_DAY0_FIELD is used for the zero-based 24-hour clock.
     * For example, 23:59 + 01:00 results in 00:59.
     */
    public static final int HOUR_OF_DAY0_FIELD = 5;
    /**
     * Useful constant for MINUTE field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int MINUTE_FIELD = 6;
    /**
     * Useful constant for SECOND field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int SECOND_FIELD = 7;
    /**
     * Useful constant for MILLISECOND field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int MILLISECOND_FIELD = 8;
    /**
     * Useful constant for DAY_OF_WEEK field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int DAY_OF_WEEK_FIELD = 9;
    /**
     * Useful constant for DAY_OF_YEAR field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int DAY_OF_YEAR_FIELD = 10;
    /**
     * Useful constant for DAY_OF_WEEK_IN_MONTH field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
    /**
     * Useful constant for WEEK_OF_YEAR field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int WEEK_OF_YEAR_FIELD = 12;
    /**
     * Useful constant for WEEK_OF_MONTH field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int WEEK_OF_MONTH_FIELD = 13;
    /**
     * Useful constant for AM_PM field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int AM_PM_FIELD = 14;
    /**
     * Useful constant for one-based HOUR field alignment.
     * Used in FieldPosition of date/time formatting.
     * HOUR1_FIELD is used for the one-based 12-hour clock.
     * For example, 11:30 PM + 1 hour results in 12:30 AM.
     */
    public static final int HOUR1_FIELD = 15;
    /**
     * Useful constant for zero-based HOUR field alignment.
     * Used in FieldPosition of date/time formatting.
     * HOUR0_FIELD is used for the zero-based 12-hour clock.
     * For example, 11:30 PM + 1 hour results in 00:30 AM.
     */
    public static final int HOUR0_FIELD = 16;
    /**
     * Useful constant for TIMEZONE field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public static final int TIMEZONE_FIELD = 17;

    // Proclaim serial compatibility with 1.1 FCS
    @java.io.Serial
    private static final long serialVersionUID = 7218322306649953788L;

    /**
     * Formats the given {@code Object} into a date-time string. The formatted
     * string is appended to the given {@code StringBuffer}.
     *
     * @param obj Must be a {@code Date} or a {@code Number} representing a
     * millisecond offset from the <a href="../util/Calendar.html#Epoch">Epoch</a>.
     * @param toAppendTo The string buffer for the returning date-time string.
     * @param fieldPosition keeps track on the position of the field within
     * the returned string. For example, given a date-time text
     * {@code "1996.07.10 AD at 15:08:56 PDT"}, if the given {@code fieldPosition}
     * is {@link DateFormat#YEAR_FIELD}, the begin index and end index of
     * {@code fieldPosition} will be set to 0 and 4, respectively.
     * Notice that if the same date-time field appears more than once in a
     * pattern, the {@code fieldPosition} will be set for the first occurrence
     * of that date-time field. For instance, formatting a {@code Date} to the
     * date-time string {@code "1 PM PDT (Pacific Daylight Time)"} using the
     * pattern {@code "h a z (zzzz)"} and the alignment field
     * {@link DateFormat#TIMEZONE_FIELD}, the begin index and end index of
     * {@code fieldPosition} will be set to 5 and 8, respectively, for the
     * first occurrence of the timezone pattern character {@code 'z'}.
     * @return the string buffer passed in as {@code toAppendTo},
     *         with formatted text appended.
     * @throws    IllegalArgumentException if the {@code Format} cannot format
     *            the given {@code obj}.
     * @see java.text.Format
     */
    public final StringBuffer format(Object obj, StringBuffer toAppendTo,
                                     FieldPosition fieldPosition)
    {
        if (obj instanceof Date)
            return format( (Date)obj, toAppendTo, fieldPosition );
        else if (obj instanceof Number)
            return format( new Date(((Number)obj).longValue()),
                          toAppendTo, fieldPosition );
        else
            throw new IllegalArgumentException("Cannot format given Object as a Date");
    }

    /**
     * Formats a {@link Date} into a date-time string. The formatted
     * string is appended to the given {@code StringBuffer}.
     *
     * @param date a Date to be formatted into a date-time string.
     * @param toAppendTo the string buffer for the returning date-time string.
     * @param fieldPosition keeps track on the position of the field within
     * the returned string. For example, given a date-time text
     * {@code "1996.07.10 AD at 15:08:56 PDT"}, if the given {@code fieldPosition}
     * is {@link DateFormat#YEAR_FIELD}, the begin index and end index of
     * {@code fieldPosition} will be set to 0 and 4, respectively.
     * Notice that if the same date-time field appears more than once in a
     * pattern, the {@code fieldPosition} will be set for the first occurrence
     * of that date-time field. For instance, formatting a {@code Date} to the
     * date-time string {@code "1 PM PDT (Pacific Daylight Time)"} using the
     * pattern {@code "h a z (zzzz)"} and the alignment field
     * {@link DateFormat#TIMEZONE_FIELD}, the begin index and end index of
     * {@code fieldPosition} will be set to 5 and 8, respectively, for the
     * first occurrence of the timezone pattern character {@code 'z'}.
     * @return the string buffer passed in as {@code toAppendTo}, with formatted
     * text appended.
     */
    public abstract StringBuffer format(Date date, StringBuffer toAppendTo,
                                        FieldPosition fieldPosition);

    /**
     * Formats a {@link Date} into a date-time string.
     *
     * @param date the time value to be formatted into a date-time string.
     * @return the formatted date-time string.
     */
    public final String format(Date date)
    {
        return format(date, new StringBuffer(),
                      DontCareFieldPosition.INSTANCE).toString();
    }

    /**
     * Parses text from the beginning of the given string to produce a date.
     * The method may not use the entire text of the given string.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on date parsing.
     *
     * @param source A {@code String} whose beginning should be parsed.
     * @return A {@code Date} parsed from the string.
     * @throws    ParseException if the beginning of the specified string
     *            cannot be parsed.
     */
    public Date parse(String source) throws ParseException
    {
        ParsePosition pos = new ParsePosition(0);
        Date result = parse(source, pos);
        if (pos.index == 0)
            throw new ParseException("Unparseable date: \"" + source + "\"" ,
                pos.errorIndex);
        return result;
    }

    /**
     * Parse a date/time string according to the given parse position.  For
     * example, a time text {@code "07/10/96 4:5 PM, PDT"} will be parsed into a {@code Date}
     * that is equivalent to {@code Date(837039900000L)}.
     *
     * <p> By default, parsing is lenient: If the input is not in the form used
     * by this object's format method but can still be parsed as a date, then
     * the parse succeeds.  Clients may insist on strict adherence to the
     * format by calling {@link #setLenient(boolean) setLenient(false)}.
     *
     * <p>This parsing operation uses the {@link #calendar} to produce
     * a {@code Date}. As a result, the {@code calendar}'s date-time
     * fields and the {@code TimeZone} value may have been
     * overwritten, depending on subclass implementations. Any {@code
     * TimeZone} value that has previously been set by a call to
     * {@link #setTimeZone(java.util.TimeZone) setTimeZone} may need
     * to be restored for further operations.
     *
     * @param source  The date/time string to be parsed
     *
     * @param pos   On input, the position at which to start parsing; on
     *              output, the position at which parsing terminated, or the
     *              start position if the parse failed.
     *
     * @return      A {@code Date}, or {@code null} if the input could not be parsed
     */
    public abstract Date parse(String source, ParsePosition pos);

    /**
     * Parses text from a string to produce a {@code Date}.
     * <p>
     * The method attempts to parse text starting at the index given by
     * {@code pos}.
     * If parsing succeeds, then the index of {@code pos} is updated
     * to the index after the last character used (parsing does not necessarily
     * use all characters up to the end of the string), and the parsed
     * date is returned. The updated {@code pos} can be used to
     * indicate the starting point for the next call to this method.
     * If an error occurs, then the index of {@code pos} is not
     * changed, the error index of {@code pos} is set to the index of
     * the character where the error occurred, and null is returned.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on date parsing.
     *
     * @param source A {@code String}, part of which should be parsed.
     * @param pos A {@code ParsePosition} object with index and error
     *            index information as described above.
     * @return A {@code Date} parsed from the string. In case of
     *         error, returns null.
     * @throws NullPointerException if {@code source} or {@code pos} is null.
     */
    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    /**
     * Constant for full style pattern.
     */
    public static final int FULL = 0;
    /**
     * Constant for long style pattern.
     */
    public static final int LONG = 1;
    /**
     * Constant for medium style pattern.
     */
    public static final int MEDIUM = 2;
    /**
     * Constant for short style pattern.
     */
    public static final int SHORT = 3;
    /**
     * Constant for default style pattern.  Its value is MEDIUM.
     */
    public static final int DEFAULT = MEDIUM;

    /**
     * Gets the time formatter with the default formatting style
     * for the default {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * <p>This is equivalent to calling
     * {@link #getTimeInstance(int, Locale) getTimeInstance(DEFAULT,
     *     Locale.getDefault(Locale.Category.FORMAT))}.
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @return a time formatter.
     */
    public static final DateFormat getTimeInstance()
    {
        return get(DEFAULT, 0, 1, Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Gets the time formatter with the given formatting style
     * for the default {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * <p>This is equivalent to calling
     * {@link #getTimeInstance(int, Locale) getTimeInstance(style,
     *     Locale.getDefault(Locale.Category.FORMAT))}.
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @param style the given formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @return a time formatter.
     */
    public static final DateFormat getTimeInstance(int style)
    {
        return get(style, 0, 1, Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Gets the time formatter with the given formatting style
     * for the given locale.
     * @param style the given formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @param aLocale the given locale.
     * @return a time formatter.
     */
    public static final DateFormat getTimeInstance(int style,
                                                 Locale aLocale)
    {
        return get(style, 0, 1, aLocale);
    }

    /**
     * Gets the date formatter with the default formatting style
     * for the default {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * <p>This is equivalent to calling
     * {@link #getDateInstance(int, Locale) getDateInstance(DEFAULT,
     *     Locale.getDefault(Locale.Category.FORMAT))}.
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @return a date formatter.
     */
    public static final DateFormat getDateInstance()
    {
        return get(0, DEFAULT, 2, Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Gets the date formatter with the given formatting style
     * for the default {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * <p>This is equivalent to calling
     * {@link #getDateInstance(int, Locale) getDateInstance(style,
     *     Locale.getDefault(Locale.Category.FORMAT))}.
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @param style the given formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @return a date formatter.
     */
    public static final DateFormat getDateInstance(int style)
    {
        return get(0, style, 2, Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Gets the date formatter with the given formatting style
     * for the given locale.
     * @param style the given formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @param aLocale the given locale.
     * @return a date formatter.
     */
    public static final DateFormat getDateInstance(int style,
                                                 Locale aLocale)
    {
        return get(0, style, 2, aLocale);
    }

    /**
     * Gets the date/time formatter with the default formatting style
     * for the default {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * <p>This is equivalent to calling
     * {@link #getDateTimeInstance(int, int, Locale) getDateTimeInstance(DEFAULT,
     *     DEFAULT, Locale.getDefault(Locale.Category.FORMAT))}.
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @return a date/time formatter.
     */
    public static final DateFormat getDateTimeInstance()
    {
        return get(DEFAULT, DEFAULT, 3, Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Gets the date/time formatter with the given date and time
     * formatting styles for the default {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * <p>This is equivalent to calling
     * {@link #getDateTimeInstance(int, int, Locale) getDateTimeInstance(dateStyle,
     *     timeStyle, Locale.getDefault(Locale.Category.FORMAT))}.
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @param dateStyle the given date formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @param timeStyle the given time formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @return a date/time formatter.
     */
    public static final DateFormat getDateTimeInstance(int dateStyle,
                                                       int timeStyle)
    {
        return get(timeStyle, dateStyle, 3, Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Gets the date/time formatter with the given formatting styles
     * for the given locale.
     * @param dateStyle the given date formatting style.
     * @param timeStyle the given time formatting style.
     * @param aLocale the given locale.
     * @return a date/time formatter.
     */
    public static final DateFormat
        getDateTimeInstance(int dateStyle, int timeStyle, Locale aLocale)
    {
        return get(timeStyle, dateStyle, 3, aLocale);
    }

    /**
     * Get a default date/time formatter that uses the SHORT style for both the
     * date and the time.
     *
     * @return a date/time formatter
     */
    public static final DateFormat getInstance() {
        return getDateTimeInstance(SHORT, SHORT);
    }

    /**
     * Returns an array of all locales for which the
     * {@code get*Instance} methods of this class can return
     * localized instances.
     * The returned array represents the union of locales supported by the Java
     * runtime and by installed
     * {@link java.text.spi.DateFormatProvider DateFormatProvider} implementations.
     * It must contain at least a {@code Locale} instance equal to
     * {@link java.util.Locale#US Locale.US}.
     *
     * @return An array of locales for which localized
     *         {@code DateFormat} instances are available.
     */
    public static Locale[] getAvailableLocales()
    {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(DateFormatProvider.class);
        return pool.getAvailableLocales();
    }

    /**
     * Set the calendar to be used by this date format.  Initially, the default
     * calendar for the specified or default locale is used.
     *
     * <p>Any {@link java.util.TimeZone TimeZone} and {@linkplain
     * #isLenient() leniency} values that have previously been set are
     * overwritten by {@code newCalendar}'s values.
     *
     * @param newCalendar the new {@code Calendar} to be used by the date format
     */
    public void setCalendar(Calendar newCalendar)
    {
        this.calendar = newCalendar;
    }

    /**
     * Gets the calendar associated with this date/time formatter.
     *
     * @return the calendar associated with this date/time formatter.
     */
    public Calendar getCalendar()
    {
        return calendar;
    }

    /**
     * Allows you to set the number formatter.
     * @param newNumberFormat the given new NumberFormat.
     */
    public void setNumberFormat(NumberFormat newNumberFormat)
    {
        this.numberFormat = newNumberFormat;
    }

    /**
     * Gets the number formatter which this date/time formatter uses to
     * format and parse a time.
     * @return the number formatter which this date/time formatter uses.
     */
    public NumberFormat getNumberFormat()
    {
        return numberFormat;
    }

    /**
     * Sets the time zone for the calendar of this {@code DateFormat} object.
     * This method is equivalent to the following call.
     * <blockquote><pre>{@code
     * getCalendar().setTimeZone(zone)
     * }</pre></blockquote>
     *
     * <p>The {@code TimeZone} set by this method is overwritten by a
     * {@link #setCalendar(java.util.Calendar) setCalendar} call.
     *
     * <p>The {@code TimeZone} set by this method may be overwritten as
     * a result of a call to the parse method.
     *
     * @param zone the given new time zone.
     */
    public void setTimeZone(TimeZone zone)
    {
        calendar.setTimeZone(zone);
    }

    /**
     * Gets the time zone.
     * This method is equivalent to the following call.
     * <blockquote><pre>{@code
     * getCalendar().getTimeZone()
     * }</pre></blockquote>
     *
     * @return the time zone associated with the calendar of DateFormat.
     */
    public TimeZone getTimeZone()
    {
        return calendar.getTimeZone();
    }

    /**
     * Specify whether or not date/time parsing is to be lenient.  With
     * lenient parsing, the parser may use heuristics to interpret inputs that
     * do not precisely match this object's format.  With strict parsing,
     * inputs must match this object's format.
     *
     * <p>This method is equivalent to the following call.
     * <blockquote><pre>{@code
     * getCalendar().setLenient(lenient)
     * }</pre></blockquote>
     *
     * <p>This leniency value is overwritten by a call to {@link
     * #setCalendar(java.util.Calendar) setCalendar()}.
     *
     * @param lenient when {@code true}, parsing is lenient
     * @see java.util.Calendar#setLenient(boolean)
     */
    public void setLenient(boolean lenient)
    {
        calendar.setLenient(lenient);
    }

    /**
     * Tell whether date/time parsing is to be lenient.
     * This method is equivalent to the following call.
     * <blockquote><pre>{@code
     * getCalendar().isLenient()
     * }</pre></blockquote>
     *
     * @return {@code true} if the {@link #calendar} is lenient;
     *         {@code false} otherwise.
     * @see java.util.Calendar#isLenient()
     */
    public boolean isLenient()
    {
        return calendar.isLenient();
    }

    /**
     * Overrides hashCode
     */
    public int hashCode() {
        return numberFormat.hashCode();
        // just enough fields for a reasonable distribution
    }

    /**
     * Overrides equals
     */
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DateFormat other = (DateFormat) obj;
        return (// calendar.equivalentTo(other.calendar) // THIS API DOESN'T EXIST YET!
                calendar.getFirstDayOfWeek() == other.calendar.getFirstDayOfWeek() &&
                calendar.getMinimalDaysInFirstWeek() == other.calendar.getMinimalDaysInFirstWeek() &&
                calendar.isLenient() == other.calendar.isLenient() &&
                calendar.getTimeZone().equals(other.calendar.getTimeZone()) &&
                numberFormat.equals(other.numberFormat));
    }

    /**
     * Overrides Cloneable
     */
    public Object clone()
    {
        DateFormat other = (DateFormat) super.clone();
        other.calendar = (Calendar) calendar.clone();
        other.numberFormat = (NumberFormat) numberFormat.clone();
        return other;
    }

    /**
     * Creates a DateFormat with the given time and/or date style in the given
     * locale.
     * @param timeStyle a value from 0 to 3 indicating the time format,
     * ignored if flags is 2
     * @param dateStyle a value from 0 to 3 indicating the time format,
     * ignored if flags is 1
     * @param flags either 1 for a time format, 2 for a date format,
     * or 3 for a date/time format
     * @param loc the locale for the format
     */
    private static DateFormat get(int timeStyle, int dateStyle,
                                  int flags, Locale loc) {
        if ((flags & 1) != 0) {
            if (timeStyle < 0 || timeStyle > 3) {
                throw new IllegalArgumentException("Illegal time style " + timeStyle);
            }
        } else {
            timeStyle = -1;
        }
        if ((flags & 2) != 0) {
            if (dateStyle < 0 || dateStyle > 3) {
                throw new IllegalArgumentException("Illegal date style " + dateStyle);
            }
        } else {
            dateStyle = -1;
        }

        LocaleProviderAdapter adapter = LocaleProviderAdapter.getAdapter(DateFormatProvider.class, loc);
        DateFormat dateFormat = get(adapter, timeStyle, dateStyle, loc);
        if (dateFormat == null) {
            dateFormat = get(LocaleProviderAdapter.forJRE(), timeStyle, dateStyle, loc);
        }
        return dateFormat;
    }

    private static DateFormat get(LocaleProviderAdapter adapter, int timeStyle, int dateStyle, Locale loc) {
        DateFormatProvider provider = adapter.getDateFormatProvider();
        DateFormat dateFormat;
        if (timeStyle == -1) {
            dateFormat = provider.getDateInstance(dateStyle, loc);
        } else {
            if (dateStyle == -1) {
                dateFormat = provider.getTimeInstance(timeStyle, loc);
            } else {
                dateFormat = provider.getDateTimeInstance(dateStyle, timeStyle, loc);
            }
        }
        return dateFormat;
    }

    /**
     * Create a new date format.
     */
    protected DateFormat() {}

    /**
     * Defines constants that are used as attribute keys in the
     * {@code AttributedCharacterIterator} returned
     * from {@code DateFormat.formatToCharacterIterator} and as
     * field identifiers in {@code FieldPosition}.
     * <p>
     * The class also provides two methods to map
     * between its constants and the corresponding Calendar constants.
     *
     * @since 1.4
     * @see java.util.Calendar
     */
    public static class Field extends Format.Field {

        // Proclaim serial compatibility with 1.4 FCS
        @java.io.Serial
        private static final long serialVersionUID = 7441350119349544720L;

        // table of all instances in this class, used by readResolve
        private static final Map<String, Field> instanceMap = new HashMap<>(18);
        // Maps from Calendar constant (such as Calendar.ERA) to Field
        // constant (such as Field.ERA).
        private static final Field[] calendarToFieldMapping =
                                             new Field[Calendar.FIELD_COUNT];

        /** Calendar field. */
        private int calendarField;

        /**
         * Returns the {@code Field} constant that corresponds to
         * the {@code Calendar} constant {@code calendarField}.
         * If there is no direct mapping between the {@code Calendar}
         * constant and a {@code Field}, null is returned.
         *
         * @throws IllegalArgumentException if {@code calendarField} is
         *         not the value of a {@code Calendar} field constant.
         * @param calendarField Calendar field constant
         * @return Field instance representing calendarField.
         * @see java.util.Calendar
         */
        public static Field ofCalendarField(int calendarField) {
            if (calendarField < 0 || calendarField >=
                        calendarToFieldMapping.length) {
                throw new IllegalArgumentException("Unknown Calendar constant "
                                                   + calendarField);
            }
            return calendarToFieldMapping[calendarField];
        }

        /**
         * Creates a {@code Field}.
         *
         * @param name the name of the {@code Field}
         * @param calendarField the {@code Calendar} constant this
         *        {@code Field} corresponds to; any value, even one
         *        outside the range of legal {@code Calendar} values may
         *        be used, but {@code -1} should be used for values
         *        that don't correspond to legal {@code Calendar} values
         */
        protected Field(String name, int calendarField) {
            super(name);
            this.calendarField = calendarField;
            if (this.getClass() == DateFormat.Field.class) {
                instanceMap.put(name, this);
                if (calendarField >= 0) {
                    // assert(calendarField < Calendar.FIELD_COUNT);
                    calendarToFieldMapping[calendarField] = this;
                }
            }
        }

        /**
         * Returns the {@code Calendar} field associated with this
         * attribute. For example, if this represents the hours field of
         * a {@code Calendar}, this would return
         * {@code Calendar.HOUR}. If there is no corresponding
         * {@code Calendar} constant, this will return -1.
         *
         * @return Calendar constant for this field
         * @see java.util.Calendar
         */
        public int getCalendarField() {
            return calendarField;
        }

        /**
         * Resolves instances being deserialized to the predefined constants.
         *
         * @throws InvalidObjectException if the constant could not be
         *         resolved.
         * @return resolved DateFormat.Field constant
         */
        @Override
        @java.io.Serial
        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != DateFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }

            Object instance = instanceMap.get(getName());
            if (instance != null) {
                return instance;
            } else {
                throw new InvalidObjectException("unknown attribute name");
            }
        }

        //
        // The constants
        //

        /**
         * Constant identifying the era field.
         */
        public static final Field ERA = new Field("era", Calendar.ERA);

        /**
         * Constant identifying the year field.
         */
        public static final Field YEAR = new Field("year", Calendar.YEAR);

        /**
         * Constant identifying the month field.
         */
        public static final Field MONTH = new Field("month", Calendar.MONTH);

        /**
         * Constant identifying the day of month field.
         */
        public static final Field DAY_OF_MONTH = new
                            Field("day of month", Calendar.DAY_OF_MONTH);

        /**
         * Constant identifying the hour of day field, where the legal values
         * are 1 to 24.
         */
        public static final Field HOUR_OF_DAY1 = new Field("hour of day 1",-1);

        /**
         * Constant identifying the hour of day field, where the legal values
         * are 0 to 23.
         */
        public static final Field HOUR_OF_DAY0 = new
               Field("hour of day", Calendar.HOUR_OF_DAY);

        /**
         * Constant identifying the minute field.
         */
        public static final Field MINUTE =new Field("minute", Calendar.MINUTE);

        /**
         * Constant identifying the second field.
         */
        public static final Field SECOND =new Field("second", Calendar.SECOND);

        /**
         * Constant identifying the millisecond field.
         */
        public static final Field MILLISECOND = new
                Field("millisecond", Calendar.MILLISECOND);

        /**
         * Constant identifying the day of week field.
         */
        public static final Field DAY_OF_WEEK = new
                Field("day of week", Calendar.DAY_OF_WEEK);

        /**
         * Constant identifying the day of year field.
         */
        public static final Field DAY_OF_YEAR = new
                Field("day of year", Calendar.DAY_OF_YEAR);

        /**
         * Constant identifying the day of week field.
         */
        public static final Field DAY_OF_WEEK_IN_MONTH =
                     new Field("day of week in month",
                                            Calendar.DAY_OF_WEEK_IN_MONTH);

        /**
         * Constant identifying the week of year field.
         */
        public static final Field WEEK_OF_YEAR = new
              Field("week of year", Calendar.WEEK_OF_YEAR);

        /**
         * Constant identifying the week of month field.
         */
        public static final Field WEEK_OF_MONTH = new
            Field("week of month", Calendar.WEEK_OF_MONTH);

        /**
         * Constant identifying the time of day indicator
         * (e.g. "a.m." or "p.m.") field.
         */
        public static final Field AM_PM = new
                            Field("am pm", Calendar.AM_PM);

        /**
         * Constant identifying the hour field, where the legal values are
         * 1 to 12.
         */
        public static final Field HOUR1 = new Field("hour 1", -1);

        /**
         * Constant identifying the hour field, where the legal values are
         * 0 to 11.
         */
        public static final Field HOUR0 = new
                            Field("hour", Calendar.HOUR);

        /**
         * Constant identifying the time zone field.
         */
        public static final Field TIME_ZONE = new Field("time zone", -1);
    }
}
