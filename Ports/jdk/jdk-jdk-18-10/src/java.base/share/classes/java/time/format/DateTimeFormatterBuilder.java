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
 * Copyright (c) 2008-2012, Stephen Colebourne & Michael Nascimento Santos
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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.ERA;

import java.lang.ref.SoftReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeTextProvider.LocaleStore;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.time.zone.ZoneRulesProvider;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.text.spi.JavaTimeDateTimePatternProvider;
import sun.util.locale.provider.CalendarDataUtility;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleResources;
import sun.util.locale.provider.TimeZoneNameUtility;

/**
 * Builder to create date-time formatters.
 * <p>
 * This allows a {@code DateTimeFormatter} to be created.
 * All date-time formatters are created ultimately using this builder.
 * <p>
 * The basic elements of date-time can all be added:
 * <ul>
 * <li>Value - a numeric value</li>
 * <li>Fraction - a fractional value including the decimal place. Always use this when
 * outputting fractions to ensure that the fraction is parsed correctly</li>
 * <li>Text - the textual equivalent for the value</li>
 * <li>OffsetId/Offset - the {@linkplain ZoneOffset zone offset}</li>
 * <li>ZoneId - the {@linkplain ZoneId time-zone} id</li>
 * <li>ZoneText - the name of the time-zone</li>
 * <li>ChronologyId - the {@linkplain Chronology chronology} id</li>
 * <li>ChronologyText - the name of the chronology</li>
 * <li>Literal - a text literal</li>
 * <li>Nested and Optional - formats can be nested or made optional</li>
 * </ul>
 * In addition, any of the elements may be decorated by padding, either with spaces or any other character.
 * <p>
 * Finally, a shorthand pattern, mostly compatible with {@code java.text.SimpleDateFormat SimpleDateFormat}
 * can be used, see {@link #appendPattern(String)}.
 * In practice, this simply parses the pattern and calls other methods on the builder.
 *
 * @implSpec
 * This class is a mutable builder intended for use from a single thread.
 *
 * @since 1.8
 */
public final class DateTimeFormatterBuilder {

    /**
     * Query for a time-zone that is region-only.
     */
    private static final TemporalQuery<ZoneId> QUERY_REGION_ONLY = (temporal) -> {
        ZoneId zone = temporal.query(TemporalQueries.zoneId());
        return zone instanceof ZoneOffset ? null : zone;
    };

    /**
     * The currently active builder, used by the outermost builder.
     */
    private DateTimeFormatterBuilder active = this;
    /**
     * The parent builder, null for the outermost builder.
     */
    private final DateTimeFormatterBuilder parent;
    /**
     * The list of printers that will be used.
     */
    private final List<DateTimePrinterParser> printerParsers = new ArrayList<>();
    /**
     * Whether this builder produces an optional formatter.
     */
    private final boolean optional;
    /**
     * The width to pad the next field to.
     */
    private int padNextWidth;
    /**
     * The character to pad the next field with.
     */
    private char padNextChar;
    /**
     * The index of the last variable width value parser.
     */
    private int valueParserIndex = -1;

    /**
     * Gets the formatting pattern for date and time styles for a locale and chronology.
     * The locale and chronology are used to lookup the locale specific format
     * for the requested dateStyle and/or timeStyle.
     * <p>
     * If the locale contains the "rg" (region override)
     * <a href="../../util/Locale.html#def_locale_extension">Unicode extensions</a>,
     * the formatting pattern is overridden with the one appropriate for the region.
     *
     * @param dateStyle  the FormatStyle for the date, null for time-only pattern
     * @param timeStyle  the FormatStyle for the time, null for date-only pattern
     * @param chrono  the Chronology, non-null
     * @param locale  the locale, non-null
     * @return the locale and Chronology specific formatting pattern
     * @throws IllegalArgumentException if both dateStyle and timeStyle are null
     */
    public static String getLocalizedDateTimePattern(FormatStyle dateStyle, FormatStyle timeStyle,
            Chronology chrono, Locale locale) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(chrono, "chrono");
        if (dateStyle == null && timeStyle == null) {
            throw new IllegalArgumentException("Either dateStyle or timeStyle must be non-null");
        }
        LocaleProviderAdapter adapter = LocaleProviderAdapter.getAdapter(JavaTimeDateTimePatternProvider.class, locale);
        JavaTimeDateTimePatternProvider provider = adapter.getJavaTimeDateTimePatternProvider();
        return provider.getJavaTimeDateTimePattern(convertStyle(timeStyle),
                         convertStyle(dateStyle), chrono.getCalendarType(),
                         CalendarDataUtility.findRegionOverride(locale));
    }

    /**
     * Converts the given FormatStyle to the java.text.DateFormat style.
     *
     * @param style  the FormatStyle style
     * @return the int style, or -1 if style is null, indicating un-required
     */
    private static int convertStyle(FormatStyle style) {
        if (style == null) {
            return -1;
        }
        return style.ordinal();  // indices happen to align
    }

    /**
     * Constructs a new instance of the builder.
     */
    public DateTimeFormatterBuilder() {
        super();
        parent = null;
        optional = false;
    }

    /**
     * Constructs a new instance of the builder.
     *
     * @param parent  the parent builder, not null
     * @param optional  whether the formatter is optional, not null
     */
    private DateTimeFormatterBuilder(DateTimeFormatterBuilder parent, boolean optional) {
        super();
        this.parent = parent;
        this.optional = optional;
    }

    //-----------------------------------------------------------------------
    /**
     * Changes the parse style to be case sensitive for the remainder of the formatter.
     * <p>
     * Parsing can be case sensitive or insensitive - by default it is case sensitive.
     * This method allows the case sensitivity setting of parsing to be changed.
     * <p>
     * Calling this method changes the state of the builder such that all
     * subsequent builder method calls will parse text in case sensitive mode.
     * See {@link #parseCaseInsensitive} for the opposite setting.
     * The parse case sensitive/insensitive methods may be called at any point
     * in the builder, thus the parser can swap between case parsing modes
     * multiple times during the parse.
     * <p>
     * Since the default is case sensitive, this method should only be used after
     * a previous call to {@code #parseCaseInsensitive}.
     *
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder parseCaseSensitive() {
        appendInternal(SettingsParser.SENSITIVE);
        return this;
    }

    /**
     * Changes the parse style to be case insensitive for the remainder of the formatter.
     * <p>
     * Parsing can be case sensitive or insensitive - by default it is case sensitive.
     * This method allows the case sensitivity setting of parsing to be changed.
     * <p>
     * Calling this method changes the state of the builder such that all
     * subsequent builder method calls will parse text in case insensitive mode.
     * See {@link #parseCaseSensitive()} for the opposite setting.
     * The parse case sensitive/insensitive methods may be called at any point
     * in the builder, thus the parser can swap between case parsing modes
     * multiple times during the parse.
     *
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder parseCaseInsensitive() {
        appendInternal(SettingsParser.INSENSITIVE);
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Changes the parse style to be strict for the remainder of the formatter.
     * <p>
     * Parsing can be strict or lenient - by default it is strict.
     * This controls the degree of flexibility in matching the text and sign styles.
     * <p>
     * When used, this method changes the parsing to be strict from this point onwards.
     * As strict is the default, this is normally only needed after calling {@link #parseLenient()}.
     * The change will remain in force until the end of the formatter that is eventually
     * constructed or until {@code parseLenient} is called.
     *
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder parseStrict() {
        appendInternal(SettingsParser.STRICT);
        return this;
    }

    /**
     * Changes the parse style to be lenient for the remainder of the formatter.
     * Note that case sensitivity is set separately to this method.
     * <p>
     * Parsing can be strict or lenient - by default it is strict.
     * This controls the degree of flexibility in matching the text and sign styles.
     * Applications calling this method should typically also call {@link #parseCaseInsensitive()}.
     * <p>
     * When used, this method changes the parsing to be lenient from this point onwards.
     * The change will remain in force until the end of the formatter that is eventually
     * constructed or until {@code parseStrict} is called.
     *
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder parseLenient() {
        appendInternal(SettingsParser.LENIENT);
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends a default value for a field to the formatter for use in parsing.
     * <p>
     * This appends an instruction to the builder to inject a default value
     * into the parsed result. This is especially useful in conjunction with
     * optional parts of the formatter.
     * <p>
     * For example, consider a formatter that parses the year, followed by
     * an optional month, with a further optional day-of-month. Using such a
     * formatter would require the calling code to check whether a full date,
     * year-month or just a year had been parsed. This method can be used to
     * default the month and day-of-month to a sensible value, such as the
     * first of the month, allowing the calling code to always get a date.
     * <p>
     * During formatting, this method has no effect.
     * <p>
     * During parsing, the current state of the parse is inspected.
     * If the specified field has no associated value, because it has not been
     * parsed successfully at that point, then the specified value is injected
     * into the parse result. Injection is immediate, thus the field-value pair
     * will be visible to any subsequent elements in the formatter.
     * As such, this method is normally called at the end of the builder.
     *
     * @param field  the field to default the value of, not null
     * @param value  the value to default the field to
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder parseDefaulting(TemporalField field, long value) {
        Objects.requireNonNull(field, "field");
        appendInternal(new DefaultValueParser(field, value));
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends the value of a date-time field to the formatter using a normal
     * output style.
     * <p>
     * The value of the field will be output during a format.
     * If the value cannot be obtained then an exception will be thrown.
     * <p>
     * The value will be printed as per the normal format of an integer value.
     * Only negative numbers will be signed. No padding will be added.
     * <p>
     * The parser for a variable width value such as this normally behaves greedily,
     * requiring one digit, but accepting as many digits as possible.
     * This behavior can be affected by 'adjacent value parsing'.
     * See {@link #appendValue(java.time.temporal.TemporalField, int)} for full details.
     *
     * @param field  the field to append, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendValue(TemporalField field) {
        Objects.requireNonNull(field, "field");
        appendValue(new NumberPrinterParser(field, 1, 19, SignStyle.NORMAL));
        return this;
    }

    /**
     * Appends the value of a date-time field to the formatter using a fixed
     * width, zero-padded approach.
     * <p>
     * The value of the field will be output during a format.
     * If the value cannot be obtained then an exception will be thrown.
     * <p>
     * The value will be zero-padded on the left. If the size of the value
     * means that it cannot be printed within the width then an exception is thrown.
     * If the value of the field is negative then an exception is thrown during formatting.
     * <p>
     * This method supports a special technique of parsing known as 'adjacent value parsing'.
     * This technique solves the problem where a value, variable or fixed width, is followed by one or more
     * fixed length values. The standard parser is greedy, and thus it would normally
     * steal the digits that are needed by the fixed width value parsers that follow the
     * variable width one.
     * <p>
     * No action is required to initiate 'adjacent value parsing'.
     * When a call to {@code appendValue} is made, the builder
     * enters adjacent value parsing setup mode. If the immediately subsequent method
     * call or calls on the same builder are for a fixed width value, then the parser will reserve
     * space so that the fixed width values can be parsed.
     * <p>
     * For example, consider {@code builder.appendValue(YEAR).appendValue(MONTH_OF_YEAR, 2);}
     * The year is a variable width parse of between 1 and 19 digits.
     * The month is a fixed width parse of 2 digits.
     * Because these were appended to the same builder immediately after one another,
     * the year parser will reserve two digits for the month to parse.
     * Thus, the text '201106' will correctly parse to a year of 2011 and a month of 6.
     * Without adjacent value parsing, the year would greedily parse all six digits and leave
     * nothing for the month.
     * <p>
     * Adjacent value parsing applies to each set of fixed width not-negative values in the parser
     * that immediately follow any kind of value, variable or fixed width.
     * Calling any other append method will end the setup of adjacent value parsing.
     * Thus, in the unlikely event that you need to avoid adjacent value parsing behavior,
     * simply add the {@code appendValue} to another {@code DateTimeFormatterBuilder}
     * and add that to this builder.
     * <p>
     * If adjacent parsing is active, then parsing must match exactly the specified
     * number of digits in both strict and lenient modes.
     * In addition, no positive or negative sign is permitted.
     *
     * @param field  the field to append, not null
     * @param width  the width of the printed field, from 1 to 19
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if the width is invalid
     */
    public DateTimeFormatterBuilder appendValue(TemporalField field, int width) {
        Objects.requireNonNull(field, "field");
        if (width < 1 || width > 19) {
            throw new IllegalArgumentException("The width must be from 1 to 19 inclusive but was " + width);
        }
        NumberPrinterParser pp = new NumberPrinterParser(field, width, width, SignStyle.NOT_NEGATIVE);
        appendValue(pp);
        return this;
    }

    /**
     * Appends the value of a date-time field to the formatter providing full
     * control over formatting.
     * <p>
     * The value of the field will be output during a format.
     * If the value cannot be obtained then an exception will be thrown.
     * <p>
     * This method provides full control of the numeric formatting, including
     * zero-padding and the positive/negative sign.
     * <p>
     * The parser for a variable width value such as this normally behaves greedily,
     * accepting as many digits as possible.
     * This behavior can be affected by 'adjacent value parsing'.
     * See {@link #appendValue(java.time.temporal.TemporalField, int)} for full details.
     * <p>
     * In strict parsing mode, the minimum number of parsed digits is {@code minWidth}
     * and the maximum is {@code maxWidth}.
     * In lenient parsing mode, the minimum number of parsed digits is one
     * and the maximum is 19 (except as limited by adjacent value parsing).
     * <p>
     * If this method is invoked with equal minimum and maximum widths and a sign style of
     * {@code NOT_NEGATIVE} then it delegates to {@code appendValue(TemporalField,int)}.
     * In this scenario, the formatting and parsing behavior described there occur.
     *
     * @param field  the field to append, not null
     * @param minWidth  the minimum field width of the printed field, from 1 to 19
     * @param maxWidth  the maximum field width of the printed field, from 1 to 19
     * @param signStyle  the positive/negative output style, not null
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if the widths are invalid
     */
    public DateTimeFormatterBuilder appendValue(
            TemporalField field, int minWidth, int maxWidth, SignStyle signStyle) {
        if (minWidth == maxWidth && signStyle == SignStyle.NOT_NEGATIVE) {
            return appendValue(field, maxWidth);
        }
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(signStyle, "signStyle");
        if (minWidth < 1 || minWidth > 19) {
            throw new IllegalArgumentException("The minimum width must be from 1 to 19 inclusive but was " + minWidth);
        }
        if (maxWidth < 1 || maxWidth > 19) {
            throw new IllegalArgumentException("The maximum width must be from 1 to 19 inclusive but was " + maxWidth);
        }
        if (maxWidth < minWidth) {
            throw new IllegalArgumentException("The maximum width must exceed or equal the minimum width but " +
                    maxWidth + " < " + minWidth);
        }
        NumberPrinterParser pp = new NumberPrinterParser(field, minWidth, maxWidth, signStyle);
        appendValue(pp);
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends the reduced value of a date-time field to the formatter.
     * <p>
     * Since fields such as year vary by chronology, it is recommended to use the
     * {@link #appendValueReduced(TemporalField, int, int, ChronoLocalDate)} date}
     * variant of this method in most cases. This variant is suitable for
     * simple fields or working with only the ISO chronology.
     * <p>
     * For formatting, the {@code width} and {@code maxWidth} are used to
     * determine the number of characters to format.
     * If they are equal then the format is fixed width.
     * If the value of the field is within the range of the {@code baseValue} using
     * {@code width} characters then the reduced value is formatted otherwise the value is
     * truncated to fit {@code maxWidth}.
     * The rightmost characters are output to match the width, left padding with zero.
     * <p>
     * For strict parsing, the number of characters allowed by {@code width} to {@code maxWidth} are parsed.
     * For lenient parsing, the number of characters must be at least 1 and less than 10.
     * If the number of digits parsed is equal to {@code width} and the value is positive,
     * the value of the field is computed to be the first number greater than
     * or equal to the {@code baseValue} with the same least significant characters,
     * otherwise the value parsed is the field value.
     * This allows a reduced value to be entered for values in range of the baseValue
     * and width and absolute values can be entered for values outside the range.
     * <p>
     * For example, a base value of {@code 1980} and a width of {@code 2} will have
     * valid values from {@code 1980} to {@code 2079}.
     * During parsing, the text {@code "12"} will result in the value {@code 2012} as that
     * is the value within the range where the last two characters are "12".
     * By contrast, parsing the text {@code "1915"} will result in the value {@code 1915}.
     *
     * @param field  the field to append, not null
     * @param width  the field width of the printed and parsed field, from 1 to 10
     * @param maxWidth  the maximum field width of the printed field, from 1 to 10
     * @param baseValue  the base value of the range of valid values
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if the width or base value is invalid
     */
    public DateTimeFormatterBuilder appendValueReduced(TemporalField field,
            int width, int maxWidth, int baseValue) {
        Objects.requireNonNull(field, "field");
        ReducedPrinterParser pp = new ReducedPrinterParser(field, width, maxWidth, baseValue, null);
        appendValue(pp);
        return this;
    }

    /**
     * Appends the reduced value of a date-time field to the formatter.
     * <p>
     * This is typically used for formatting and parsing a two digit year.
     * <p>
     * The base date is used to calculate the full value during parsing.
     * For example, if the base date is 1950-01-01 then parsed values for
     * a two digit year parse will be in the range 1950-01-01 to 2049-12-31.
     * Only the year would be extracted from the date, thus a base date of
     * 1950-08-25 would also parse to the range 1950-01-01 to 2049-12-31.
     * This behavior is necessary to support fields such as week-based-year
     * or other calendar systems where the parsed value does not align with
     * standard ISO years.
     * <p>
     * The exact behavior is as follows. Parse the full set of fields and
     * determine the effective chronology using the last chronology if
     * it appears more than once. Then convert the base date to the
     * effective chronology. Then extract the specified field from the
     * chronology-specific base date and use it to determine the
     * {@code baseValue} used below.
     * <p>
     * For formatting, the {@code width} and {@code maxWidth} are used to
     * determine the number of characters to format.
     * If they are equal then the format is fixed width.
     * If the value of the field is within the range of the {@code baseValue} using
     * {@code width} characters then the reduced value is formatted otherwise the value is
     * truncated to fit {@code maxWidth}.
     * The rightmost characters are output to match the width, left padding with zero.
     * <p>
     * For strict parsing, the number of characters allowed by {@code width} to {@code maxWidth} are parsed.
     * For lenient parsing, the number of characters must be at least 1 and less than 10.
     * If the number of digits parsed is equal to {@code width} and the value is positive,
     * the value of the field is computed to be the first number greater than
     * or equal to the {@code baseValue} with the same least significant characters,
     * otherwise the value parsed is the field value.
     * This allows a reduced value to be entered for values in range of the baseValue
     * and width and absolute values can be entered for values outside the range.
     * <p>
     * For example, a base value of {@code 1980} and a width of {@code 2} will have
     * valid values from {@code 1980} to {@code 2079}.
     * During parsing, the text {@code "12"} will result in the value {@code 2012} as that
     * is the value within the range where the last two characters are "12".
     * By contrast, parsing the text {@code "1915"} will result in the value {@code 1915}.
     *
     * @param field  the field to append, not null
     * @param width  the field width of the printed and parsed field, from 1 to 10
     * @param maxWidth  the maximum field width of the printed field, from 1 to 10
     * @param baseDate  the base date used to calculate the base value for the range
     *  of valid values in the parsed chronology, not null
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if the width or base value is invalid
     */
    public DateTimeFormatterBuilder appendValueReduced(
            TemporalField field, int width, int maxWidth, ChronoLocalDate baseDate) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(baseDate, "baseDate");
        ReducedPrinterParser pp = new ReducedPrinterParser(field, width, maxWidth, 0, baseDate);
        appendValue(pp);
        return this;
    }

    /**
     * Appends a fixed or variable width printer-parser handling adjacent value mode.
     * If a PrinterParser is not active then the new PrinterParser becomes
     * the active PrinterParser.
     * Otherwise, the active PrinterParser is modified depending on the new PrinterParser.
     * If the new PrinterParser is fixed width and has sign style {@code NOT_NEGATIVE}
     * then its width is added to the active PP and
     * the new PrinterParser is forced to be fixed width.
     * If the new PrinterParser is variable width, the active PrinterParser is changed
     * to be fixed width and the new PrinterParser becomes the active PP.
     *
     * @param pp  the printer-parser, not null
     * @return this, for chaining, not null
     */
    private DateTimeFormatterBuilder appendValue(NumberPrinterParser pp) {
        if (active.valueParserIndex >= 0) {
            final int activeValueParser = active.valueParserIndex;

            // adjacent parsing mode, update setting in previous parsers
            NumberPrinterParser basePP = (NumberPrinterParser) active.printerParsers.get(activeValueParser);
            if (pp.minWidth == pp.maxWidth && pp.signStyle == SignStyle.NOT_NEGATIVE) {
                // Append the width to the subsequentWidth of the active parser
                basePP = basePP.withSubsequentWidth(pp.maxWidth);
                // Append the new parser as a fixed width
                appendInternal(pp.withFixedWidth());
                // Retain the previous active parser
                active.valueParserIndex = activeValueParser;
            } else {
                // Modify the active parser to be fixed width
                basePP = basePP.withFixedWidth();
                // The new parser becomes the mew active parser
                active.valueParserIndex = appendInternal(pp);
            }
            // Replace the modified parser with the updated one
            active.printerParsers.set(activeValueParser, basePP);
        } else {
            // The new Parser becomes the active parser
            active.valueParserIndex = appendInternal(pp);
        }
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends the fractional value of a date-time field to the formatter.
     * <p>
     * The fractional value of the field will be output including the
     * preceding decimal point. The preceding value is not output.
     * For example, the second-of-minute value of 15 would be output as {@code .25}.
     * <p>
     * The width of the printed fraction can be controlled. Setting the
     * minimum width to zero will cause no output to be generated.
     * The printed fraction will have the minimum width necessary between
     * the minimum and maximum widths - trailing zeroes are omitted.
     * No rounding occurs due to the maximum width - digits are simply dropped.
     * <p>
     * When parsing in strict mode, the number of parsed digits must be between
     * the minimum and maximum width. In strict mode, if the minimum and maximum widths
     * are equal and there is no decimal point then the parser will
     * participate in adjacent value parsing, see
     * {@link #appendValue(java.time.temporal.TemporalField, int)}. When parsing in lenient mode,
     * the minimum width is considered to be zero and the maximum is nine.
     * <p>
     * If the value cannot be obtained then an exception will be thrown.
     * If the value is negative an exception will be thrown.
     * If the field does not have a fixed set of valid values then an
     * exception will be thrown.
     * If the field value in the date-time to be printed is invalid it
     * cannot be printed and an exception will be thrown.
     *
     * @param field  the field to append, not null
     * @param minWidth  the minimum width of the field excluding the decimal point, from 0 to 9
     * @param maxWidth  the maximum width of the field excluding the decimal point, from 1 to 9
     * @param decimalPoint  whether to output the localized decimal point symbol
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if the field has a variable set of valid values or
     *  either width is invalid
     */
    public DateTimeFormatterBuilder appendFraction(
            TemporalField field, int minWidth, int maxWidth, boolean decimalPoint) {
        if (minWidth == maxWidth && decimalPoint == false) {
            // adjacent parsing
            appendValue(new FractionPrinterParser(field, minWidth, maxWidth, decimalPoint));
        } else {
            appendInternal(new FractionPrinterParser(field, minWidth, maxWidth, decimalPoint));
        }
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends the text of a date-time field to the formatter using the full
     * text style.
     * <p>
     * The text of the field will be output during a format.
     * The value must be within the valid range of the field.
     * If the value cannot be obtained then an exception will be thrown.
     * If the field has no textual representation, then the numeric value will be used.
     * <p>
     * The value will be printed as per the normal format of an integer value.
     * Only negative numbers will be signed. No padding will be added.
     *
     * @param field  the field to append, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendText(TemporalField field) {
        return appendText(field, TextStyle.FULL);
    }

    /**
     * Appends the text of a date-time field to the formatter.
     * <p>
     * The text of the field will be output during a format.
     * The value must be within the valid range of the field.
     * If the value cannot be obtained then an exception will be thrown.
     * If the field has no textual representation, then the numeric value will be used.
     * <p>
     * The value will be printed as per the normal format of an integer value.
     * Only negative numbers will be signed. No padding will be added.
     *
     * @param field  the field to append, not null
     * @param textStyle  the text style to use, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendText(TemporalField field, TextStyle textStyle) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(textStyle, "textStyle");
        appendInternal(new TextPrinterParser(field, textStyle, DateTimeTextProvider.getInstance()));
        return this;
    }

    /**
     * Appends the text of a date-time field to the formatter using the specified
     * map to supply the text.
     * <p>
     * The standard text outputting methods use the localized text in the JDK.
     * This method allows that text to be specified directly.
     * The supplied map is not validated by the builder to ensure that formatting or
     * parsing is possible, thus an invalid map may throw an error during later use.
     * <p>
     * Supplying the map of text provides considerable flexibility in formatting and parsing.
     * For example, a legacy application might require or supply the months of the
     * year as "JNY", "FBY", "MCH" etc. These do not match the standard set of text
     * for localized month names. Using this method, a map can be created which
     * defines the connection between each value and the text:
     * <pre>
     * Map&lt;Long, String&gt; map = new HashMap&lt;&gt;();
     * map.put(1L, "JNY");
     * map.put(2L, "FBY");
     * map.put(3L, "MCH");
     * ...
     * builder.appendText(MONTH_OF_YEAR, map);
     * </pre>
     * <p>
     * Other uses might be to output the value with a suffix, such as "1st", "2nd", "3rd",
     * or as Roman numerals "I", "II", "III", "IV".
     * <p>
     * During formatting, the value is obtained and checked that it is in the valid range.
     * If text is not available for the value then it is output as a number.
     * During parsing, the parser will match against the map of text and numeric values.
     *
     * @param field  the field to append, not null
     * @param textLookup  the map from the value to the text
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendText(TemporalField field, Map<Long, String> textLookup) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(textLookup, "textLookup");
        Map<Long, String> copy = new LinkedHashMap<>(textLookup);
        Map<TextStyle, Map<Long, String>> map = Collections.singletonMap(TextStyle.FULL, copy);
        final LocaleStore store = new LocaleStore(map);
        DateTimeTextProvider provider = new DateTimeTextProvider() {
            @Override
            public String getText(Chronology chrono, TemporalField field,
                                  long value, TextStyle style, Locale locale) {
                return store.getText(value, style);
            }
            @Override
            public String getText(TemporalField field, long value, TextStyle style, Locale locale) {
                return store.getText(value, style);
            }
            @Override
            public Iterator<Entry<String, Long>> getTextIterator(Chronology chrono,
                    TemporalField field, TextStyle style, Locale locale) {
                return store.getTextIterator(style);
            }
            @Override
            public Iterator<Entry<String, Long>> getTextIterator(TemporalField field,
                    TextStyle style, Locale locale) {
                return store.getTextIterator(style);
            }
        };
        appendInternal(new TextPrinterParser(field, TextStyle.FULL, provider));
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends an instant using ISO-8601 to the formatter, formatting fractional
     * digits in groups of three.
     * <p>
     * Instants have a fixed output format.
     * They are converted to a date-time with a zone-offset of UTC and formatted
     * using the standard ISO-8601 format.
     * With this method, formatting nano-of-second outputs zero, three, six
     * or nine digits as necessary.
     * The localized decimal style is not used.
     * <p>
     * The instant is obtained using {@link ChronoField#INSTANT_SECONDS INSTANT_SECONDS}
     * and optionally {@code NANO_OF_SECOND}. The value of {@code INSTANT_SECONDS}
     * may be outside the maximum range of {@code LocalDateTime}.
     * <p>
     * The {@linkplain ResolverStyle resolver style} has no effect on instant parsing.
     * The end-of-day time of '24:00' is handled as midnight at the start of the following day.
     * The leap-second time of '23:59:59' is handled to some degree, see
     * {@link DateTimeFormatter#parsedLeapSecond()} for full details.
     * <p>
     * When formatting, the instant will always be suffixed by 'Z' to indicate UTC.
     * When parsing, the behaviour of {@link DateTimeFormatterBuilder#appendOffsetId()}
     * will be used to parse the offset, converting the instant to UTC as necessary.
     * <p>
     * An alternative to this method is to format/parse the instant as a single
     * epoch-seconds value. That is achieved using {@code appendValue(INSTANT_SECONDS)}.
     *
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendInstant() {
        appendInternal(new InstantPrinterParser(-2));
        return this;
    }

    /**
     * Appends an instant using ISO-8601 to the formatter with control over
     * the number of fractional digits.
     * <p>
     * Instants have a fixed output format, although this method provides some
     * control over the fractional digits. They are converted to a date-time
     * with a zone-offset of UTC and printed using the standard ISO-8601 format.
     * The localized decimal style is not used.
     * <p>
     * The {@code fractionalDigits} parameter allows the output of the fractional
     * second to be controlled. Specifying zero will cause no fractional digits
     * to be output. From 1 to 9 will output an increasing number of digits, using
     * zero right-padding if necessary. The special value -1 is used to output as
     * many digits as necessary to avoid any trailing zeroes.
     * <p>
     * When parsing in strict mode, the number of parsed digits must match the
     * fractional digits. When parsing in lenient mode, any number of fractional
     * digits from zero to nine are accepted.
     * <p>
     * The instant is obtained using {@link ChronoField#INSTANT_SECONDS INSTANT_SECONDS}
     * and optionally {@code NANO_OF_SECOND}. The value of {@code INSTANT_SECONDS}
     * may be outside the maximum range of {@code LocalDateTime}.
     * <p>
     * The {@linkplain ResolverStyle resolver style} has no effect on instant parsing.
     * The end-of-day time of '24:00' is handled as midnight at the start of the following day.
     * The leap-second time of '23:59:60' is handled to some degree, see
     * {@link DateTimeFormatter#parsedLeapSecond()} for full details.
     * <p>
     * An alternative to this method is to format/parse the instant as a single
     * epoch-seconds value. That is achieved using {@code appendValue(INSTANT_SECONDS)}.
     *
     * @param fractionalDigits  the number of fractional second digits to format with,
     *  from 0 to 9, or -1 to use as many digits as necessary
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if the number of fractional digits is invalid
     */
    public DateTimeFormatterBuilder appendInstant(int fractionalDigits) {
        if (fractionalDigits < -1 || fractionalDigits > 9) {
            throw new IllegalArgumentException("The fractional digits must be from -1 to 9 inclusive but was " + fractionalDigits);
        }
        appendInternal(new InstantPrinterParser(fractionalDigits));
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends the zone offset, such as '+01:00', to the formatter.
     * <p>
     * This appends an instruction to format/parse the offset ID to the builder.
     * This is equivalent to calling {@code appendOffset("+HH:mm:ss", "Z")}.
     * See {@link #appendOffset(String, String)} for details on formatting
     * and parsing.
     *
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendOffsetId() {
        appendInternal(OffsetIdPrinterParser.INSTANCE_ID_Z);
        return this;
    }

    /**
     * Appends the zone offset, such as '+01:00', to the formatter.
     * <p>
     * This appends an instruction to format/parse the offset ID to the builder.
     * <p>
     * During formatting, the offset is obtained using a mechanism equivalent
     * to querying the temporal with {@link TemporalQueries#offset()}.
     * It will be printed using the format defined below.
     * If the offset cannot be obtained then an exception is thrown unless the
     * section of the formatter is optional.
     * <p>
     * When parsing in strict mode, the input must contain the mandatory
     * and optional elements are defined by the specified pattern.
     * If the offset cannot be parsed then an exception is thrown unless
     * the section of the formatter is optional.
     * <p>
     * When parsing in lenient mode, only the hours are mandatory - minutes
     * and seconds are optional. The colons are required if the specified
     * pattern contains a colon. If the specified pattern is "+HH", the
     * presence of colons is determined by whether the character after the
     * hour digits is a colon or not.
     * If the offset cannot be parsed then an exception is thrown unless
     * the section of the formatter is optional.
     * <p>
     * The format of the offset is controlled by a pattern which must be one
     * of the following:
     * <ul>
     * <li>{@code +HH} - hour only, ignoring minute and second
     * <li>{@code +HHmm} - hour, with minute if non-zero, ignoring second, no colon
     * <li>{@code +HH:mm} - hour, with minute if non-zero, ignoring second, with colon
     * <li>{@code +HHMM} - hour and minute, ignoring second, no colon
     * <li>{@code +HH:MM} - hour and minute, ignoring second, with colon
     * <li>{@code +HHMMss} - hour and minute, with second if non-zero, no colon
     * <li>{@code +HH:MM:ss} - hour and minute, with second if non-zero, with colon
     * <li>{@code +HHMMSS} - hour, minute and second, no colon
     * <li>{@code +HH:MM:SS} - hour, minute and second, with colon
     * <li>{@code +HHmmss} - hour, with minute if non-zero or with minute and
     * second if non-zero, no colon
     * <li>{@code +HH:mm:ss} - hour, with minute if non-zero or with minute and
     * second if non-zero, with colon
     * <li>{@code +H} - hour only, ignoring minute and second
     * <li>{@code +Hmm} - hour, with minute if non-zero, ignoring second, no colon
     * <li>{@code +H:mm} - hour, with minute if non-zero, ignoring second, with colon
     * <li>{@code +HMM} - hour and minute, ignoring second, no colon
     * <li>{@code +H:MM} - hour and minute, ignoring second, with colon
     * <li>{@code +HMMss} - hour and minute, with second if non-zero, no colon
     * <li>{@code +H:MM:ss} - hour and minute, with second if non-zero, with colon
     * <li>{@code +HMMSS} - hour, minute and second, no colon
     * <li>{@code +H:MM:SS} - hour, minute and second, with colon
     * <li>{@code +Hmmss} - hour, with minute if non-zero or with minute and
     * second if non-zero, no colon
     * <li>{@code +H:mm:ss} - hour, with minute if non-zero or with minute and
     * second if non-zero, with colon
     * </ul>
     * Patterns containing "HH" will format and parse a two digit hour,
     * zero-padded if necessary. Patterns containing "H" will format with no
     * zero-padding, and parse either one or two digits.
     * In lenient mode, the parser will be greedy and parse the maximum digits possible.
     * The "no offset" text controls what text is printed when the total amount of
     * the offset fields to be output is zero.
     * Example values would be 'Z', '+00:00', 'UTC' or 'GMT'.
     * Three formats are accepted for parsing UTC - the "no offset" text, and the
     * plus and minus versions of zero defined by the pattern.
     *
     * @param pattern  the pattern to use, not null
     * @param noOffsetText  the text to use when the offset is zero, not null
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if the pattern is invalid
     */
    public DateTimeFormatterBuilder appendOffset(String pattern, String noOffsetText) {
        appendInternal(new OffsetIdPrinterParser(pattern, noOffsetText));
        return this;
    }

    /**
     * Appends the localized zone offset, such as 'GMT+01:00', to the formatter.
     * <p>
     * This appends a localized zone offset to the builder, the format of the
     * localized offset is controlled by the specified {@link FormatStyle style}
     * to this method:
     * <ul>
     * <li>{@link TextStyle#FULL full} - formats with localized offset text, such
     * as 'GMT, 2-digit hour and minute field, optional second field if non-zero,
     * and colon.
     * <li>{@link TextStyle#SHORT short} - formats with localized offset text,
     * such as 'GMT, hour without leading zero, optional 2-digit minute and
     * second if non-zero, and colon.
     * </ul>
     * <p>
     * During formatting, the offset is obtained using a mechanism equivalent
     * to querying the temporal with {@link TemporalQueries#offset()}.
     * If the offset cannot be obtained then an exception is thrown unless the
     * section of the formatter is optional.
     * <p>
     * During parsing, the offset is parsed using the format defined above.
     * If the offset cannot be parsed then an exception is thrown unless the
     * section of the formatter is optional.
     *
     * @param style  the format style to use, not null
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if style is neither {@link TextStyle#FULL
     * full} nor {@link TextStyle#SHORT short}
     */
    public DateTimeFormatterBuilder appendLocalizedOffset(TextStyle style) {
        Objects.requireNonNull(style, "style");
        if (style != TextStyle.FULL && style != TextStyle.SHORT) {
            throw new IllegalArgumentException("Style must be either full or short");
        }
        appendInternal(new LocalizedOffsetIdPrinterParser(style));
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends the time-zone ID, such as 'Europe/Paris' or '+02:00', to the formatter.
     * <p>
     * This appends an instruction to format/parse the zone ID to the builder.
     * The zone ID is obtained in a strict manner suitable for {@code ZonedDateTime}.
     * By contrast, {@code OffsetDateTime} does not have a zone ID suitable
     * for use with this method, see {@link #appendZoneOrOffsetId()}.
     * <p>
     * During formatting, the zone is obtained using a mechanism equivalent
     * to querying the temporal with {@link TemporalQueries#zoneId()}.
     * It will be printed using the result of {@link ZoneId#getId()}.
     * If the zone cannot be obtained then an exception is thrown unless the
     * section of the formatter is optional.
     * <p>
     * During parsing, the text must match a known zone or offset.
     * There are two types of zone ID, offset-based, such as '+01:30' and
     * region-based, such as 'Europe/London'. These are parsed differently.
     * If the parse starts with '+', '-', 'UT', 'UTC' or 'GMT', then the parser
     * expects an offset-based zone and will not match region-based zones.
     * The offset ID, such as '+02:30', may be at the start of the parse,
     * or prefixed by  'UT', 'UTC' or 'GMT'. The offset ID parsing is
     * equivalent to using {@link #appendOffset(String, String)} using the
     * arguments 'HH:MM:ss' and the no offset string '0'.
     * If the parse starts with 'UT', 'UTC' or 'GMT', and the parser cannot
     * match a following offset ID, then {@link ZoneOffset#UTC} is selected.
     * In all other cases, the list of known region-based zones is used to
     * find the longest available match. If no match is found, and the parse
     * starts with 'Z', then {@code ZoneOffset.UTC} is selected.
     * The parser uses the {@linkplain #parseCaseInsensitive() case sensitive} setting.
     * <p>
     * For example, the following will parse:
     * <pre>
     *   "Europe/London"           -- ZoneId.of("Europe/London")
     *   "Z"                       -- ZoneOffset.UTC
     *   "UT"                      -- ZoneId.of("UT")
     *   "UTC"                     -- ZoneId.of("UTC")
     *   "GMT"                     -- ZoneId.of("GMT")
     *   "+01:30"                  -- ZoneOffset.of("+01:30")
     *   "UT+01:30"                -- ZoneOffset.of("+01:30")
     *   "UTC+01:30"               -- ZoneOffset.of("+01:30")
     *   "GMT+01:30"               -- ZoneOffset.of("+01:30")
     * </pre>
     *
     * @return this, for chaining, not null
     * @see #appendZoneRegionId()
     */
    public DateTimeFormatterBuilder appendZoneId() {
        appendInternal(new ZoneIdPrinterParser(TemporalQueries.zoneId(), "ZoneId()"));
        return this;
    }

    /**
     * Appends the time-zone region ID, such as 'Europe/Paris', to the formatter,
     * rejecting the zone ID if it is a {@code ZoneOffset}.
     * <p>
     * This appends an instruction to format/parse the zone ID to the builder
     * only if it is a region-based ID.
     * <p>
     * During formatting, the zone is obtained using a mechanism equivalent
     * to querying the temporal with {@link TemporalQueries#zoneId()}.
     * If the zone is a {@code ZoneOffset} or it cannot be obtained then
     * an exception is thrown unless the section of the formatter is optional.
     * If the zone is not an offset, then the zone will be printed using
     * the zone ID from {@link ZoneId#getId()}.
     * <p>
     * During parsing, the text must match a known zone or offset.
     * There are two types of zone ID, offset-based, such as '+01:30' and
     * region-based, such as 'Europe/London'. These are parsed differently.
     * If the parse starts with '+', '-', 'UT', 'UTC' or 'GMT', then the parser
     * expects an offset-based zone and will not match region-based zones.
     * The offset ID, such as '+02:30', may be at the start of the parse,
     * or prefixed by  'UT', 'UTC' or 'GMT'. The offset ID parsing is
     * equivalent to using {@link #appendOffset(String, String)} using the
     * arguments 'HH:MM:ss' and the no offset string '0'.
     * If the parse starts with 'UT', 'UTC' or 'GMT', and the parser cannot
     * match a following offset ID, then {@link ZoneOffset#UTC} is selected.
     * In all other cases, the list of known region-based zones is used to
     * find the longest available match. If no match is found, and the parse
     * starts with 'Z', then {@code ZoneOffset.UTC} is selected.
     * The parser uses the {@linkplain #parseCaseInsensitive() case sensitive} setting.
     * <p>
     * For example, the following will parse:
     * <pre>
     *   "Europe/London"           -- ZoneId.of("Europe/London")
     *   "Z"                       -- ZoneOffset.UTC
     *   "UT"                      -- ZoneId.of("UT")
     *   "UTC"                     -- ZoneId.of("UTC")
     *   "GMT"                     -- ZoneId.of("GMT")
     *   "+01:30"                  -- ZoneOffset.of("+01:30")
     *   "UT+01:30"                -- ZoneOffset.of("+01:30")
     *   "UTC+01:30"               -- ZoneOffset.of("+01:30")
     *   "GMT+01:30"               -- ZoneOffset.of("+01:30")
     * </pre>
     * <p>
     * Note that this method is identical to {@code appendZoneId()} except
     * in the mechanism used to obtain the zone.
     * Note also that parsing accepts offsets, whereas formatting will never
     * produce one.
     *
     * @return this, for chaining, not null
     * @see #appendZoneId()
     */
    public DateTimeFormatterBuilder appendZoneRegionId() {
        appendInternal(new ZoneIdPrinterParser(QUERY_REGION_ONLY, "ZoneRegionId()"));
        return this;
    }

    /**
     * Appends the time-zone ID, such as 'Europe/Paris' or '+02:00', to
     * the formatter, using the best available zone ID.
     * <p>
     * This appends an instruction to format/parse the best available
     * zone or offset ID to the builder.
     * The zone ID is obtained in a lenient manner that first attempts to
     * find a true zone ID, such as that on {@code ZonedDateTime}, and
     * then attempts to find an offset, such as that on {@code OffsetDateTime}.
     * <p>
     * During formatting, the zone is obtained using a mechanism equivalent
     * to querying the temporal with {@link TemporalQueries#zone()}.
     * It will be printed using the result of {@link ZoneId#getId()}.
     * If the zone cannot be obtained then an exception is thrown unless the
     * section of the formatter is optional.
     * <p>
     * During parsing, the text must match a known zone or offset.
     * There are two types of zone ID, offset-based, such as '+01:30' and
     * region-based, such as 'Europe/London'. These are parsed differently.
     * If the parse starts with '+', '-', 'UT', 'UTC' or 'GMT', then the parser
     * expects an offset-based zone and will not match region-based zones.
     * The offset ID, such as '+02:30', may be at the start of the parse,
     * or prefixed by  'UT', 'UTC' or 'GMT'. The offset ID parsing is
     * equivalent to using {@link #appendOffset(String, String)} using the
     * arguments 'HH:MM:ss' and the no offset string '0'.
     * If the parse starts with 'UT', 'UTC' or 'GMT', and the parser cannot
     * match a following offset ID, then {@link ZoneOffset#UTC} is selected.
     * In all other cases, the list of known region-based zones is used to
     * find the longest available match. If no match is found, and the parse
     * starts with 'Z', then {@code ZoneOffset.UTC} is selected.
     * The parser uses the {@linkplain #parseCaseInsensitive() case sensitive} setting.
     * <p>
     * For example, the following will parse:
     * <pre>
     *   "Europe/London"           -- ZoneId.of("Europe/London")
     *   "Z"                       -- ZoneOffset.UTC
     *   "UT"                      -- ZoneId.of("UT")
     *   "UTC"                     -- ZoneId.of("UTC")
     *   "GMT"                     -- ZoneId.of("GMT")
     *   "+01:30"                  -- ZoneOffset.of("+01:30")
     *   "UT+01:30"                -- ZoneOffset.of("UT+01:30")
     *   "UTC+01:30"               -- ZoneOffset.of("UTC+01:30")
     *   "GMT+01:30"               -- ZoneOffset.of("GMT+01:30")
     * </pre>
     * <p>
     * Note that this method is identical to {@code appendZoneId()} except
     * in the mechanism used to obtain the zone.
     *
     * @return this, for chaining, not null
     * @see #appendZoneId()
     */
    public DateTimeFormatterBuilder appendZoneOrOffsetId() {
        appendInternal(new ZoneIdPrinterParser(TemporalQueries.zone(), "ZoneOrOffsetId()"));
        return this;
    }

    /**
     * Appends the time-zone name, such as 'British Summer Time', to the formatter.
     * <p>
     * This appends an instruction to format/parse the textual name of the zone to
     * the builder.
     * <p>
     * During formatting, the zone is obtained using a mechanism equivalent
     * to querying the temporal with {@link TemporalQueries#zoneId()}.
     * If the zone is a {@code ZoneOffset} it will be printed using the
     * result of {@link ZoneOffset#getId()}.
     * If the zone is not an offset, the textual name will be looked up
     * for the locale set in the {@link DateTimeFormatter}.
     * If the temporal object being printed represents an instant, or if it is a
     * local date-time that is not in a daylight saving gap or overlap then
     * the text will be the summer or winter time text as appropriate.
     * If the lookup for text does not find any suitable result, then the
     * {@link ZoneId#getId() ID} will be printed.
     * If the zone cannot be obtained then an exception is thrown unless the
     * section of the formatter is optional.
     * <p>
     * During parsing, either the textual zone name, the zone ID or the offset
     * is accepted. Many textual zone names are not unique, such as CST can be
     * for both "Central Standard Time" and "China Standard Time". In this
     * situation, the zone id will be determined by the region information from
     * formatter's  {@link DateTimeFormatter#getLocale() locale} and the standard
     * zone id for that area, for example, America/New_York for the America Eastern
     * zone. The {@link #appendZoneText(TextStyle, Set)} may be used
     * to specify a set of preferred {@link ZoneId} in this situation.
     *
     * @param textStyle  the text style to use, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendZoneText(TextStyle textStyle) {
        appendInternal(new ZoneTextPrinterParser(textStyle, null, false));
        return this;
    }

    /**
     * Appends the time-zone name, such as 'British Summer Time', to the formatter.
     * <p>
     * This appends an instruction to format/parse the textual name of the zone to
     * the builder.
     * <p>
     * During formatting, the zone is obtained using a mechanism equivalent
     * to querying the temporal with {@link TemporalQueries#zoneId()}.
     * If the zone is a {@code ZoneOffset} it will be printed using the
     * result of {@link ZoneOffset#getId()}.
     * If the zone is not an offset, the textual name will be looked up
     * for the locale set in the {@link DateTimeFormatter}.
     * If the temporal object being printed represents an instant, or if it is a
     * local date-time that is not in a daylight saving gap or overlap, then the text
     * will be the summer or winter time text as appropriate.
     * If the lookup for text does not find any suitable result, then the
     * {@link ZoneId#getId() ID} will be printed.
     * If the zone cannot be obtained then an exception is thrown unless the
     * section of the formatter is optional.
     * <p>
     * During parsing, either the textual zone name, the zone ID or the offset
     * is accepted. Many textual zone names are not unique, such as CST can be
     * for both "Central Standard Time" and "China Standard Time". In this
     * situation, the zone id will be determined by the region information from
     * formatter's  {@link DateTimeFormatter#getLocale() locale} and the standard
     * zone id for that area, for example, America/New_York for the America Eastern
     * zone. This method also allows a set of preferred {@link ZoneId} to be
     * specified for parsing. The matched preferred zone id will be used if the
     * textural zone name being parsed is not unique.
     * <p>
     * If the zone cannot be parsed then an exception is thrown unless the
     * section of the formatter is optional.
     *
     * @param textStyle  the text style to use, not null
     * @param preferredZones  the set of preferred zone ids, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendZoneText(TextStyle textStyle,
                                                   Set<ZoneId> preferredZones) {
        Objects.requireNonNull(preferredZones, "preferredZones");
        appendInternal(new ZoneTextPrinterParser(textStyle, preferredZones, false));
        return this;
    }
    //----------------------------------------------------------------------
    /**
     * Appends the generic time-zone name, such as 'Pacific Time', to the formatter.
     * <p>
     * This appends an instruction to format/parse the generic textual
     * name of the zone to the builder. The generic name is the same throughout the whole
     * year, ignoring any daylight saving changes. For example, 'Pacific Time' is the
     * generic name, whereas 'Pacific Standard Time' and 'Pacific Daylight Time' are the
     * specific names, see {@link #appendZoneText(TextStyle)}.
     * <p>
     * During formatting, the zone is obtained using a mechanism equivalent
     * to querying the temporal with {@link TemporalQueries#zoneId()}.
     * If the zone is a {@code ZoneOffset} it will be printed using the
     * result of {@link ZoneOffset#getId()}.
     * If the zone is not an offset, the textual name will be looked up
     * for the locale set in the {@link DateTimeFormatter}.
     * If the lookup for text does not find any suitable result, then the
     * {@link ZoneId#getId() ID} will be printed.
     * If the zone cannot be obtained then an exception is thrown unless the
     * section of the formatter is optional.
     * <p>
     * During parsing, either the textual zone name, the zone ID or the offset
     * is accepted. Many textual zone names are not unique, such as CST can be
     * for both "Central Standard Time" and "China Standard Time". In this
     * situation, the zone id will be determined by the region information from
     * formatter's  {@link DateTimeFormatter#getLocale() locale} and the standard
     * zone id for that area, for example, America/New_York for the America Eastern zone.
     * The {@link #appendGenericZoneText(TextStyle, Set)} may be used
     * to specify a set of preferred {@link ZoneId} in this situation.
     *
     * @param textStyle  the text style to use, not null
     * @return this, for chaining, not null
     * @since 9
     */
    public DateTimeFormatterBuilder appendGenericZoneText(TextStyle textStyle) {
        appendInternal(new ZoneTextPrinterParser(textStyle, null, true));
        return this;
    }

    /**
     * Appends the generic time-zone name, such as 'Pacific Time', to the formatter.
     * <p>
     * This appends an instruction to format/parse the generic textual
     * name of the zone to the builder. The generic name is the same throughout the whole
     * year, ignoring any daylight saving changes. For example, 'Pacific Time' is the
     * generic name, whereas 'Pacific Standard Time' and 'Pacific Daylight Time' are the
     * specific names, see {@link #appendZoneText(TextStyle)}.
     * <p>
     * This method also allows a set of preferred {@link ZoneId} to be
     * specified for parsing. The matched preferred zone id will be used if the
     * textural zone name being parsed is not unique.
     * <p>
     * See {@link #appendGenericZoneText(TextStyle)} for details about
     * formatting and parsing.
     *
     * @param textStyle  the text style to use, not null
     * @param preferredZones  the set of preferred zone ids, not null
     * @return this, for chaining, not null
     * @since 9
     */
    public DateTimeFormatterBuilder appendGenericZoneText(TextStyle textStyle,
                                                          Set<ZoneId> preferredZones) {
        appendInternal(new ZoneTextPrinterParser(textStyle, preferredZones, true));
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends the chronology ID, such as 'ISO' or 'ThaiBuddhist', to the formatter.
     * <p>
     * This appends an instruction to format/parse the chronology ID to the builder.
     * <p>
     * During formatting, the chronology is obtained using a mechanism equivalent
     * to querying the temporal with {@link TemporalQueries#chronology()}.
     * It will be printed using the result of {@link Chronology#getId()}.
     * If the chronology cannot be obtained then an exception is thrown unless the
     * section of the formatter is optional.
     * <p>
     * During parsing, the chronology is parsed and must match one of the chronologies
     * in {@link Chronology#getAvailableChronologies()}.
     * If the chronology cannot be parsed then an exception is thrown unless the
     * section of the formatter is optional.
     * The parser uses the {@linkplain #parseCaseInsensitive() case sensitive} setting.
     *
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendChronologyId() {
        appendInternal(new ChronoPrinterParser(null));
        return this;
    }

    /**
     * Appends the chronology name to the formatter.
     * <p>
     * The calendar system name will be output during a format.
     * If the chronology cannot be obtained then an exception will be thrown.
     *
     * @param textStyle  the text style to use, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendChronologyText(TextStyle textStyle) {
        Objects.requireNonNull(textStyle, "textStyle");
        appendInternal(new ChronoPrinterParser(textStyle));
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends a localized date-time pattern to the formatter.
     * <p>
     * This appends a localized section to the builder, suitable for outputting
     * a date, time or date-time combination. The format of the localized
     * section is lazily looked up based on four items:
     * <ul>
     * <li>the {@code dateStyle} specified to this method
     * <li>the {@code timeStyle} specified to this method
     * <li>the {@code Locale} of the {@code DateTimeFormatter}
     * <li>the {@code Chronology}, selecting the best available
     * </ul>
     * During formatting, the chronology is obtained from the temporal object
     * being formatted, which may have been overridden by
     * {@link DateTimeFormatter#withChronology(Chronology)}.
     * The {@code FULL} and {@code LONG} styles typically require a time-zone.
     * When formatting using these styles, a {@code ZoneId} must be available,
     * either by using {@code ZonedDateTime} or {@link DateTimeFormatter#withZone}.
     * <p>
     * During parsing, if a chronology has already been parsed, then it is used.
     * Otherwise the default from {@code DateTimeFormatter.withChronology(Chronology)}
     * is used, with {@code IsoChronology} as the fallback.
     * <p>
     * Note that this method provides similar functionality to methods on
     * {@code DateFormat} such as {@link java.text.DateFormat#getDateTimeInstance(int, int)}.
     *
     * @param dateStyle  the date style to use, null means no date required
     * @param timeStyle  the time style to use, null means no time required
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if both the date and time styles are null
     */
    public DateTimeFormatterBuilder appendLocalized(FormatStyle dateStyle, FormatStyle timeStyle) {
        if (dateStyle == null && timeStyle == null) {
            throw new IllegalArgumentException("Either the date or time style must be non-null");
        }
        appendInternal(new LocalizedPrinterParser(dateStyle, timeStyle));
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends a character literal to the formatter.
     * <p>
     * This character will be output during a format.
     *
     * @param literal  the literal to append, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendLiteral(char literal) {
        appendInternal(new CharLiteralPrinterParser(literal));
        return this;
    }

    /**
     * Appends a string literal to the formatter.
     * <p>
     * This string will be output during a format.
     * <p>
     * If the literal is empty, nothing is added to the formatter.
     *
     * @param literal  the literal to append, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendLiteral(String literal) {
        Objects.requireNonNull(literal, "literal");
        if (!literal.isEmpty()) {
            if (literal.length() == 1) {
                appendInternal(new CharLiteralPrinterParser(literal.charAt(0)));
            } else {
                appendInternal(new StringLiteralPrinterParser(literal));
            }
        }
        return this;
    }

    /**
     * Appends the day period text to the formatter.
     * <p>
     * This appends an instruction to format/parse the textual name of the day period
     * to the builder. Day periods are defined in LDML's
     * <a href="https://unicode.org/reports/tr35/tr35-dates.html#dayPeriods">"day periods"
     * </a> element.
     * <p>
     * During formatting, the day period is obtained from {@code HOUR_OF_DAY}, and
     * optionally {@code MINUTE_OF_HOUR} if exist. It will be mapped to a day period
     * type defined in LDML, such as "morning1" and then it will be translated into
     * text. Mapping to a day period type and its translation both depend on the
     * locale in the formatter.
     * <p>
     * During parsing, the text will be parsed into a day period type first. Then
     * the parsed day period is combined with other fields to make a {@code LocalTime} in
     * the resolving phase. If the {@code HOUR_OF_AMPM} field is present, it is combined
     * with the day period to make {@code HOUR_OF_DAY} taking into account any
     * {@code MINUTE_OF_HOUR} value. If {@code HOUR_OF_DAY} is present, it is validated
     * against the day period taking into account any {@code MINUTE_OF_HOUR} value. If a
     * day period is present without {@code HOUR_OF_DAY}, {@code MINUTE_OF_HOUR},
     * {@code SECOND_OF_MINUTE} and {@code NANO_OF_SECOND} then the midpoint of the
     * day period is set as the time in {@code SMART} and {@code LENIENT} mode.
     * For example, if the parsed day period type is "night1" and the period defined
     * for it in the formatter locale is from 21:00 to 06:00, then it results in
     * the {@code LocalTime} of 01:30.
     * If the resolved time conflicts with the day period, {@code DateTimeException} is
     * thrown in {@code STRICT} and {@code SMART} mode. In {@code LENIENT} mode, no
     * exception is thrown and the parsed day period is ignored.
     * <p>
     * The "midnight" type allows both "00:00" as the start-of-day and "24:00" as the
     * end-of-day, as long as they are valid with the resolved hour field.
     *
     * @param style the text style to use, not null
     * @return this, for chaining, not null
     * @since 16
     */
    public DateTimeFormatterBuilder appendDayPeriodText(TextStyle style) {
        Objects.requireNonNull(style, "style");
        switch (style) {
            // Stand-alone is not applicable. Convert to standard text style
            case FULL_STANDALONE -> style = TextStyle.FULL;
            case SHORT_STANDALONE -> style = TextStyle.SHORT;
            case NARROW_STANDALONE -> style = TextStyle.NARROW;
        }
        appendInternal(new DayPeriodPrinterParser(style));
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends all the elements of a formatter to the builder.
     * <p>
     * This method has the same effect as appending each of the constituent
     * parts of the formatter directly to this builder.
     *
     * @param formatter  the formatter to add, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder append(DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        appendInternal(formatter.toPrinterParser(false));
        return this;
    }

    /**
     * Appends a formatter to the builder which will optionally format/parse.
     * <p>
     * This method has the same effect as appending each of the constituent
     * parts directly to this builder surrounded by an {@link #optionalStart()} and
     * {@link #optionalEnd()}.
     * <p>
     * The formatter will format if data is available for all the fields contained within it.
     * The formatter will parse if the string matches, otherwise no error is returned.
     *
     * @param formatter  the formatter to add, not null
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder appendOptional(DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        appendInternal(formatter.toPrinterParser(true));
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends the elements defined by the specified pattern to the builder.
     * <p>
     * All letters 'A' to 'Z' and 'a' to 'z' are reserved as pattern letters.
     * The characters '#', '{' and '}' are reserved for future use.
     * The characters '[' and ']' indicate optional patterns.
     * The following pattern letters are defined:
     * <pre>
     *  Symbol  Meaning                     Presentation      Examples
     *  ------  -------                     ------------      -------
     *   G       era                         text              AD; Anno Domini; A
     *   u       year                        year              2004; 04
     *   y       year-of-era                 year              2004; 04
     *   D       day-of-year                 number            189
     *   M/L     month-of-year               number/text       7; 07; Jul; July; J
     *   d       day-of-month                number            10
     *   g       modified-julian-day         number            2451334
     *
     *   Q/q     quarter-of-year             number/text       3; 03; Q3; 3rd quarter
     *   Y       week-based-year             year              1996; 96
     *   w       week-of-week-based-year     number            27
     *   W       week-of-month               number            4
     *   E       day-of-week                 text              Tue; Tuesday; T
     *   e/c     localized day-of-week       number/text       2; 02; Tue; Tuesday; T
     *   F       day-of-week-in-month        number            3
     *
     *   a       am-pm-of-day                text              PM
     *   B       period-of-day               text              in the morning
     *   h       clock-hour-of-am-pm (1-12)  number            12
     *   K       hour-of-am-pm (0-11)        number            0
     *   k       clock-hour-of-day (1-24)    number            24
     *
     *   H       hour-of-day (0-23)          number            0
     *   m       minute-of-hour              number            30
     *   s       second-of-minute            number            55
     *   S       fraction-of-second          fraction          978
     *   A       milli-of-day                number            1234
     *   n       nano-of-second              number            987654321
     *   N       nano-of-day                 number            1234000000
     *
     *   V       time-zone ID                zone-id           America/Los_Angeles; Z; -08:30
     *   v       generic time-zone name      zone-name         PT, Pacific Time
     *   z       time-zone name              zone-name         Pacific Standard Time; PST
     *   O       localized zone-offset       offset-O          GMT+8; GMT+08:00; UTC-08:00;
     *   X       zone-offset 'Z' for zero    offset-X          Z; -08; -0830; -08:30; -083015; -08:30:15
     *   x       zone-offset                 offset-x          +0000; -08; -0830; -08:30; -083015; -08:30:15
     *   Z       zone-offset                 offset-Z          +0000; -0800; -08:00
     *
     *   p       pad next                    pad modifier      1
     *
     *   '       escape for text             delimiter
     *   ''      single quote                literal           '
     *   [       optional section start
     *   ]       optional section end
     *   #       reserved for future use
     *   {       reserved for future use
     *   }       reserved for future use
     * </pre>
     * <p>
     * The count of pattern letters determine the format.
     * See <a href="DateTimeFormatter.html#patterns">DateTimeFormatter</a> for a user-focused description of the patterns.
     * The following tables define how the pattern letters map to the builder.
     * <p>
     * <b>Date fields</b>: Pattern letters to output a date.
     * <pre>
     *  Pattern  Count  Equivalent builder methods
     *  -------  -----  --------------------------
     *    G       1      appendText(ChronoField.ERA, TextStyle.SHORT)
     *    GG      2      appendText(ChronoField.ERA, TextStyle.SHORT)
     *    GGG     3      appendText(ChronoField.ERA, TextStyle.SHORT)
     *    GGGG    4      appendText(ChronoField.ERA, TextStyle.FULL)
     *    GGGGG   5      appendText(ChronoField.ERA, TextStyle.NARROW)
     *
     *    u       1      appendValue(ChronoField.YEAR, 1, 19, SignStyle.NORMAL)
     *    uu      2      appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
     *    uuu     3      appendValue(ChronoField.YEAR, 3, 19, SignStyle.NORMAL)
     *    u..u    4..n   appendValue(ChronoField.YEAR, n, 19, SignStyle.EXCEEDS_PAD)
     *    y       1      appendValue(ChronoField.YEAR_OF_ERA, 1, 19, SignStyle.NORMAL)
     *    yy      2      appendValueReduced(ChronoField.YEAR_OF_ERA, 2, 2, 2000)
     *    yyy     3      appendValue(ChronoField.YEAR_OF_ERA, 3, 19, SignStyle.NORMAL)
     *    y..y    4..n   appendValue(ChronoField.YEAR_OF_ERA, n, 19, SignStyle.EXCEEDS_PAD)
     *    Y       1      append special localized WeekFields element for numeric week-based-year
     *    YY      2      append special localized WeekFields element for reduced numeric week-based-year 2 digits
     *    YYY     3      append special localized WeekFields element for numeric week-based-year (3, 19, SignStyle.NORMAL)
     *    Y..Y    4..n   append special localized WeekFields element for numeric week-based-year (n, 19, SignStyle.EXCEEDS_PAD)
     *
     *    Q       1      appendValue(IsoFields.QUARTER_OF_YEAR)
     *    QQ      2      appendValue(IsoFields.QUARTER_OF_YEAR, 2)
     *    QQQ     3      appendText(IsoFields.QUARTER_OF_YEAR, TextStyle.SHORT)
     *    QQQQ    4      appendText(IsoFields.QUARTER_OF_YEAR, TextStyle.FULL)
     *    QQQQQ   5      appendText(IsoFields.QUARTER_OF_YEAR, TextStyle.NARROW)
     *    q       1      appendValue(IsoFields.QUARTER_OF_YEAR)
     *    qq      2      appendValue(IsoFields.QUARTER_OF_YEAR, 2)
     *    qqq     3      appendText(IsoFields.QUARTER_OF_YEAR, TextStyle.SHORT_STANDALONE)
     *    qqqq    4      appendText(IsoFields.QUARTER_OF_YEAR, TextStyle.FULL_STANDALONE)
     *    qqqqq   5      appendText(IsoFields.QUARTER_OF_YEAR, TextStyle.NARROW_STANDALONE)
     *
     *    M       1      appendValue(ChronoField.MONTH_OF_YEAR)
     *    MM      2      appendValue(ChronoField.MONTH_OF_YEAR, 2)
     *    MMM     3      appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
     *    MMMM    4      appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL)
     *    MMMMM   5      appendText(ChronoField.MONTH_OF_YEAR, TextStyle.NARROW)
     *    L       1      appendValue(ChronoField.MONTH_OF_YEAR)
     *    LL      2      appendValue(ChronoField.MONTH_OF_YEAR, 2)
     *    LLL     3      appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT_STANDALONE)
     *    LLLL    4      appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL_STANDALONE)
     *    LLLLL   5      appendText(ChronoField.MONTH_OF_YEAR, TextStyle.NARROW_STANDALONE)
     *
     *    w       1      append special localized WeekFields element for numeric week-of-year
     *    ww      2      append special localized WeekFields element for numeric week-of-year, zero-padded
     *    W       1      append special localized WeekFields element for numeric week-of-month
     *    d       1      appendValue(ChronoField.DAY_OF_MONTH)
     *    dd      2      appendValue(ChronoField.DAY_OF_MONTH, 2)
     *    D       1      appendValue(ChronoField.DAY_OF_YEAR)
     *    DD      2      appendValue(ChronoField.DAY_OF_YEAR, 2, 3, SignStyle.NOT_NEGATIVE)
     *    DDD     3      appendValue(ChronoField.DAY_OF_YEAR, 3)
     *    F       1      appendValue(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH)
     *    g..g    1..n   appendValue(JulianFields.MODIFIED_JULIAN_DAY, n, 19, SignStyle.NORMAL)
     *    E       1      appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT)
     *    EE      2      appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT)
     *    EEE     3      appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT)
     *    EEEE    4      appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL)
     *    EEEEE   5      appendText(ChronoField.DAY_OF_WEEK, TextStyle.NARROW)
     *    e       1      append special localized WeekFields element for numeric day-of-week
     *    ee      2      append special localized WeekFields element for numeric day-of-week, zero-padded
     *    eee     3      appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT)
     *    eeee    4      appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL)
     *    eeeee   5      appendText(ChronoField.DAY_OF_WEEK, TextStyle.NARROW)
     *    c       1      append special localized WeekFields element for numeric day-of-week
     *    ccc     3      appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT_STANDALONE)
     *    cccc    4      appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL_STANDALONE)
     *    ccccc   5      appendText(ChronoField.DAY_OF_WEEK, TextStyle.NARROW_STANDALONE)
     * </pre>
     * <p>
     * <b>Time fields</b>: Pattern letters to output a time.
     * <pre>
     *  Pattern  Count  Equivalent builder methods
     *  -------  -----  --------------------------
     *    a       1      appendText(ChronoField.AMPM_OF_DAY, TextStyle.SHORT)
     *    h       1      appendValue(ChronoField.CLOCK_HOUR_OF_AMPM)
     *    hh      2      appendValue(ChronoField.CLOCK_HOUR_OF_AMPM, 2)
     *    H       1      appendValue(ChronoField.HOUR_OF_DAY)
     *    HH      2      appendValue(ChronoField.HOUR_OF_DAY, 2)
     *    k       1      appendValue(ChronoField.CLOCK_HOUR_OF_DAY)
     *    kk      2      appendValue(ChronoField.CLOCK_HOUR_OF_DAY, 2)
     *    K       1      appendValue(ChronoField.HOUR_OF_AMPM)
     *    KK      2      appendValue(ChronoField.HOUR_OF_AMPM, 2)
     *    m       1      appendValue(ChronoField.MINUTE_OF_HOUR)
     *    mm      2      appendValue(ChronoField.MINUTE_OF_HOUR, 2)
     *    s       1      appendValue(ChronoField.SECOND_OF_MINUTE)
     *    ss      2      appendValue(ChronoField.SECOND_OF_MINUTE, 2)
     *
     *    S..S    1..n   appendFraction(ChronoField.NANO_OF_SECOND, n, n, false)
     *    A..A    1..n   appendValue(ChronoField.MILLI_OF_DAY, n, 19, SignStyle.NOT_NEGATIVE)
     *    n..n    1..n   appendValue(ChronoField.NANO_OF_SECOND, n, 19, SignStyle.NOT_NEGATIVE)
     *    N..N    1..n   appendValue(ChronoField.NANO_OF_DAY, n, 19, SignStyle.NOT_NEGATIVE)
     * </pre>
     * <p>
     * <b>Day periods</b>: Pattern letters to output a day period.
     * <pre>
     *  Pattern  Count  Equivalent builder methods
     *  -------  -----  --------------------------
     *    B       1      appendDayPeriodText(TextStyle.SHORT)
     *    BBBB    4      appendDayPeriodText(TextStyle.FULL)
     *    BBBBB   5      appendDayPeriodText(TextStyle.NARROW)
     * </pre>
     * <p>
     * <b>Zone ID</b>: Pattern letters to output {@code ZoneId}.
     * <pre>
     *  Pattern  Count  Equivalent builder methods
     *  -------  -----  --------------------------
     *    VV      2      appendZoneId()
     *    v       1      appendGenericZoneText(TextStyle.SHORT)
     *    vvvv    4      appendGenericZoneText(TextStyle.FULL)
     *    z       1      appendZoneText(TextStyle.SHORT)
     *    zz      2      appendZoneText(TextStyle.SHORT)
     *    zzz     3      appendZoneText(TextStyle.SHORT)
     *    zzzz    4      appendZoneText(TextStyle.FULL)
     * </pre>
     * <p>
     * <b>Zone offset</b>: Pattern letters to output {@code ZoneOffset}.
     * <pre>
     *  Pattern  Count  Equivalent builder methods
     *  -------  -----  --------------------------
     *    O       1      appendLocalizedOffset(TextStyle.SHORT)
     *    OOOO    4      appendLocalizedOffset(TextStyle.FULL)
     *    X       1      appendOffset("+HHmm","Z")
     *    XX      2      appendOffset("+HHMM","Z")
     *    XXX     3      appendOffset("+HH:MM","Z")
     *    XXXX    4      appendOffset("+HHMMss","Z")
     *    XXXXX   5      appendOffset("+HH:MM:ss","Z")
     *    x       1      appendOffset("+HHmm","+00")
     *    xx      2      appendOffset("+HHMM","+0000")
     *    xxx     3      appendOffset("+HH:MM","+00:00")
     *    xxxx    4      appendOffset("+HHMMss","+0000")
     *    xxxxx   5      appendOffset("+HH:MM:ss","+00:00")
     *    Z       1      appendOffset("+HHMM","+0000")
     *    ZZ      2      appendOffset("+HHMM","+0000")
     *    ZZZ     3      appendOffset("+HHMM","+0000")
     *    ZZZZ    4      appendLocalizedOffset(TextStyle.FULL)
     *    ZZZZZ   5      appendOffset("+HH:MM:ss","Z")
     * </pre>
     * <p>
     * <b>Modifiers</b>: Pattern letters that modify the rest of the pattern:
     * <pre>
     *  Pattern  Count  Equivalent builder methods
     *  -------  -----  --------------------------
     *    [       1      optionalStart()
     *    ]       1      optionalEnd()
     *    p..p    1..n   padNext(n)
     * </pre>
     * <p>
     * Any sequence of letters not specified above, unrecognized letter or
     * reserved character will throw an exception.
     * Future versions may add to the set of patterns.
     * It is recommended to use single quotes around all characters that you want
     * to output directly to ensure that future changes do not break your application.
     * <p>
     * Note that the pattern string is similar, but not identical, to
     * {@link java.text.SimpleDateFormat SimpleDateFormat}.
     * The pattern string is also similar, but not identical, to that defined by the
     * Unicode Common Locale Data Repository (CLDR/LDML).
     * Pattern letters 'X' and 'u' are aligned with Unicode CLDR/LDML.
     * By contrast, {@code SimpleDateFormat} uses 'u' for the numeric day of week.
     * Pattern letters 'y' and 'Y' parse years of two digits and more than 4 digits differently.
     * Pattern letters 'n', 'A', 'N', and 'p' are added.
     * Number types will reject large numbers.
     *
     * @param pattern  the pattern to add, not null
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if the pattern is invalid
     */
    public DateTimeFormatterBuilder appendPattern(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        parsePattern(pattern);
        return this;
    }

    private void parsePattern(String pattern) {
        for (int pos = 0; pos < pattern.length(); pos++) {
            char cur = pattern.charAt(pos);
            if ((cur >= 'A' && cur <= 'Z') || (cur >= 'a' && cur <= 'z')) {
                int start = pos++;
                for ( ; pos < pattern.length() && pattern.charAt(pos) == cur; pos++);  // short loop
                int count = pos - start;
                // padding
                if (cur == 'p') {
                    int pad = 0;
                    if (pos < pattern.length()) {
                        cur = pattern.charAt(pos);
                        if ((cur >= 'A' && cur <= 'Z') || (cur >= 'a' && cur <= 'z')) {
                            pad = count;
                            start = pos++;
                            for ( ; pos < pattern.length() && pattern.charAt(pos) == cur; pos++);  // short loop
                            count = pos - start;
                        }
                    }
                    if (pad == 0) {
                        throw new IllegalArgumentException(
                                "Pad letter 'p' must be followed by valid pad pattern: " + pattern);
                    }
                    padNext(pad); // pad and continue parsing
                }
                // main rules
                TemporalField field = FIELD_MAP.get(cur);
                if (field != null) {
                    parseField(cur, count, field);
                } else if (cur == 'z') {
                    if (count > 4) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    } else if (count == 4) {
                        appendZoneText(TextStyle.FULL);
                    } else {
                        appendZoneText(TextStyle.SHORT);
                    }
                } else if (cur == 'V') {
                    if (count != 2) {
                        throw new IllegalArgumentException("Pattern letter count must be 2: " + cur);
                    }
                    appendZoneId();
                } else if (cur == 'v') {
                    if (count == 1) {
                        appendGenericZoneText(TextStyle.SHORT);
                    } else if (count == 4) {
                        appendGenericZoneText(TextStyle.FULL);
                    } else {
                        throw new IllegalArgumentException("Wrong number of pattern letters: " + cur);
                    }
                } else if (cur == 'Z') {
                    if (count < 4) {
                        appendOffset("+HHMM", "+0000");
                    } else if (count == 4) {
                        appendLocalizedOffset(TextStyle.FULL);
                    } else if (count == 5) {
                        appendOffset("+HH:MM:ss","Z");
                    } else {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                } else if (cur == 'O') {
                    if (count == 1) {
                        appendLocalizedOffset(TextStyle.SHORT);
                    } else if (count == 4) {
                        appendLocalizedOffset(TextStyle.FULL);
                    } else {
                        throw new IllegalArgumentException("Pattern letter count must be 1 or 4: " + cur);
                    }
                } else if (cur == 'X') {
                    if (count > 5) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                    appendOffset(OffsetIdPrinterParser.PATTERNS[count + (count == 1 ? 0 : 1)], "Z");
                } else if (cur == 'x') {
                    if (count > 5) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                    String zero = (count == 1 ? "+00" : (count % 2 == 0 ? "+0000" : "+00:00"));
                    appendOffset(OffsetIdPrinterParser.PATTERNS[count + (count == 1 ? 0 : 1)], zero);
                } else if (cur == 'W') {
                    // Fields defined by Locale
                    if (count > 1) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                    appendValue(new WeekBasedFieldPrinterParser(cur, count, count, count));
                } else if (cur == 'w') {
                    // Fields defined by Locale
                    if (count > 2) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                    }
                    appendValue(new WeekBasedFieldPrinterParser(cur, count, count, 2));
                } else if (cur == 'Y') {
                    // Fields defined by Locale
                    if (count == 2) {
                        appendValue(new WeekBasedFieldPrinterParser(cur, count, count, 2));
                    } else {
                        appendValue(new WeekBasedFieldPrinterParser(cur, count, count, 19));
                    }
                } else if (cur == 'B') {
                    switch (count) {
                        case 1 -> appendDayPeriodText(TextStyle.SHORT);
                        case 4 -> appendDayPeriodText(TextStyle.FULL);
                        case 5 -> appendDayPeriodText(TextStyle.NARROW);
                        default -> throw new IllegalArgumentException("Wrong number of pattern letters: " + cur);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown pattern letter: " + cur);
                }
                pos--;

            } else if (cur == '\'') {
                // parse literals
                int start = pos++;
                for ( ; pos < pattern.length(); pos++) {
                    if (pattern.charAt(pos) == '\'') {
                        if (pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '\'') {
                            pos++;
                        } else {
                            break;  // end of literal
                        }
                    }
                }
                if (pos >= pattern.length()) {
                    throw new IllegalArgumentException("Pattern ends with an incomplete string literal: " + pattern);
                }
                String str = pattern.substring(start + 1, pos);
                if (str.isEmpty()) {
                    appendLiteral('\'');
                } else {
                    appendLiteral(str.replace("''", "'"));
                }

            } else if (cur == '[') {
                optionalStart();

            } else if (cur == ']') {
                if (active.parent == null) {
                    throw new IllegalArgumentException("Pattern invalid as it contains ] without previous [");
                }
                optionalEnd();

            } else if (cur == '{' || cur == '}' || cur == '#') {
                throw new IllegalArgumentException("Pattern includes reserved character: '" + cur + "'");
            } else {
                appendLiteral(cur);
            }
        }
    }

    @SuppressWarnings("fallthrough")
    private void parseField(char cur, int count, TemporalField field) {
        boolean standalone = false;
        switch (cur) {
            case 'u':
            case 'y':
                if (count == 2) {
                    appendValueReduced(field, 2, 2, ReducedPrinterParser.BASE_DATE);
                } else if (count < 4) {
                    appendValue(field, count, 19, SignStyle.NORMAL);
                } else {
                    appendValue(field, count, 19, SignStyle.EXCEEDS_PAD);
                }
                break;
            case 'c':
                if (count == 1) {
                    appendValue(new WeekBasedFieldPrinterParser(cur, count, count, count));
                    break;
                } else if (count == 2) {
                    throw new IllegalArgumentException("Invalid pattern \"cc\"");
                }
                /*fallthrough*/
            case 'L':
            case 'q':
                standalone = true;
                /*fallthrough*/
            case 'M':
            case 'Q':
            case 'E':
            case 'e':
                switch (count) {
                    case 1:
                    case 2:
                        if (cur == 'e') {
                            appendValue(new WeekBasedFieldPrinterParser(cur, count, count, count));
                        } else if (cur == 'E') {
                            appendText(field, TextStyle.SHORT);
                        } else {
                            if (count == 1) {
                                appendValue(field);
                            } else {
                                appendValue(field, 2);
                            }
                        }
                        break;
                    case 3:
                        appendText(field, standalone ? TextStyle.SHORT_STANDALONE : TextStyle.SHORT);
                        break;
                    case 4:
                        appendText(field, standalone ? TextStyle.FULL_STANDALONE : TextStyle.FULL);
                        break;
                    case 5:
                        appendText(field, standalone ? TextStyle.NARROW_STANDALONE : TextStyle.NARROW);
                        break;
                    default:
                        throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'a':
                if (count == 1) {
                    appendText(field, TextStyle.SHORT);
                } else {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'G':
                switch (count) {
                    case 1, 2, 3 -> appendText(field, TextStyle.SHORT);
                    case 4 -> appendText(field, TextStyle.FULL);
                    case 5 -> appendText(field, TextStyle.NARROW);
                    default -> throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'S':
                appendFraction(NANO_OF_SECOND, count, count, false);
                break;
            case 'F':
                if (count == 1) {
                    appendValue(field);
                } else {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'd':
            case 'h':
            case 'H':
            case 'k':
            case 'K':
            case 'm':
            case 's':
                if (count == 1) {
                    appendValue(field);
                } else if (count == 2) {
                    appendValue(field, count);
                } else {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'D':
                if (count == 1) {
                    appendValue(field);
                } else if (count == 2 || count == 3) {
                    appendValue(field, count, 3, SignStyle.NOT_NEGATIVE);
                } else {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                break;
            case 'g':
                appendValue(field, count, 19, SignStyle.NORMAL);
                break;
            case 'A':
            case 'n':
            case 'N':
                appendValue(field, count, 19, SignStyle.NOT_NEGATIVE);
                break;
            default:
                if (count == 1) {
                    appendValue(field);
                } else {
                    appendValue(field, count);
                }
                break;
        }
    }

    /** Map of letters to fields. */
    private static final Map<Character, TemporalField> FIELD_MAP = new HashMap<>();
    static {
        // SDF = SimpleDateFormat
        FIELD_MAP.put('G', ChronoField.ERA);                       // SDF, LDML (different to both for 1/2 chars)
        FIELD_MAP.put('y', ChronoField.YEAR_OF_ERA);               // SDF, LDML
        FIELD_MAP.put('u', ChronoField.YEAR);                      // LDML (different in SDF)
        FIELD_MAP.put('Q', IsoFields.QUARTER_OF_YEAR);             // LDML (removed quarter from 310)
        FIELD_MAP.put('q', IsoFields.QUARTER_OF_YEAR);             // LDML (stand-alone)
        FIELD_MAP.put('M', ChronoField.MONTH_OF_YEAR);             // SDF, LDML
        FIELD_MAP.put('L', ChronoField.MONTH_OF_YEAR);             // SDF, LDML (stand-alone)
        FIELD_MAP.put('D', ChronoField.DAY_OF_YEAR);               // SDF, LDML
        FIELD_MAP.put('d', ChronoField.DAY_OF_MONTH);              // SDF, LDML
        FIELD_MAP.put('F', ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);  // SDF, LDML
        FIELD_MAP.put('E', ChronoField.DAY_OF_WEEK);               // SDF, LDML (different to both for 1/2 chars)
        FIELD_MAP.put('c', ChronoField.DAY_OF_WEEK);               // LDML (stand-alone)
        FIELD_MAP.put('e', ChronoField.DAY_OF_WEEK);               // LDML (needs localized week number)
        FIELD_MAP.put('a', ChronoField.AMPM_OF_DAY);               // SDF, LDML
        FIELD_MAP.put('H', ChronoField.HOUR_OF_DAY);               // SDF, LDML
        FIELD_MAP.put('k', ChronoField.CLOCK_HOUR_OF_DAY);         // SDF, LDML
        FIELD_MAP.put('K', ChronoField.HOUR_OF_AMPM);              // SDF, LDML
        FIELD_MAP.put('h', ChronoField.CLOCK_HOUR_OF_AMPM);        // SDF, LDML
        FIELD_MAP.put('m', ChronoField.MINUTE_OF_HOUR);            // SDF, LDML
        FIELD_MAP.put('s', ChronoField.SECOND_OF_MINUTE);          // SDF, LDML
        FIELD_MAP.put('S', ChronoField.NANO_OF_SECOND);            // LDML (SDF uses milli-of-second number)
        FIELD_MAP.put('A', ChronoField.MILLI_OF_DAY);              // LDML
        FIELD_MAP.put('n', ChronoField.NANO_OF_SECOND);            // 310 (proposed for LDML)
        FIELD_MAP.put('N', ChronoField.NANO_OF_DAY);               // 310 (proposed for LDML)
        FIELD_MAP.put('g', JulianFields.MODIFIED_JULIAN_DAY);
        // 310 - z - time-zone names, matches LDML and SimpleDateFormat 1 to 4
        // 310 - Z - matches SimpleDateFormat and LDML
        // 310 - V - time-zone id, matches LDML
        // 310 - v - general timezone names, not matching exactly with LDML because LDML specify to fall back
        //           to 'VVVV' if general-nonlocation unavailable but here it's not falling back because of lack of data
        // 310 - p - prefix for padding
        // 310 - X - matches LDML, almost matches SDF for 1, exact match 2&3, extended 4&5
        // 310 - x - matches LDML
        // 310 - w, W, and Y are localized forms matching LDML
        // LDML - B - day periods
        // LDML - U - cycle year name, not supported by 310 yet
        // LDML - l - deprecated
        // LDML - j - not relevant
    }

    //-----------------------------------------------------------------------
    /**
     * Causes the next added printer/parser to pad to a fixed width using a space.
     * <p>
     * This padding will pad to a fixed width using spaces.
     * <p>
     * During formatting, the decorated element will be output and then padded
     * to the specified width. An exception will be thrown during formatting if
     * the pad width is exceeded.
     * <p>
     * During parsing, the padding and decorated element are parsed.
     * If parsing is lenient, then the pad width is treated as a maximum.
     * The padding is parsed greedily. Thus, if the decorated element starts with
     * the pad character, it will not be parsed.
     *
     * @param padWidth  the pad width, 1 or greater
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if pad width is too small
     */
    public DateTimeFormatterBuilder padNext(int padWidth) {
        return padNext(padWidth, ' ');
    }

    /**
     * Causes the next added printer/parser to pad to a fixed width.
     * <p>
     * This padding is intended for padding other than zero-padding.
     * Zero-padding should be achieved using the appendValue methods.
     * <p>
     * During formatting, the decorated element will be output and then padded
     * to the specified width. An exception will be thrown during formatting if
     * the pad width is exceeded.
     * <p>
     * During parsing, the padding and decorated element are parsed.
     * If parsing is lenient, then the pad width is treated as a maximum.
     * If parsing is case insensitive, then the pad character is matched ignoring case.
     * The padding is parsed greedily. Thus, if the decorated element starts with
     * the pad character, it will not be parsed.
     *
     * @param padWidth  the pad width, 1 or greater
     * @param padChar  the pad character
     * @return this, for chaining, not null
     * @throws IllegalArgumentException if pad width is too small
     */
    public DateTimeFormatterBuilder padNext(int padWidth, char padChar) {
        if (padWidth < 1) {
            throw new IllegalArgumentException("The pad width must be at least one but was " + padWidth);
        }
        active.padNextWidth = padWidth;
        active.padNextChar = padChar;
        active.valueParserIndex = -1;
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Mark the start of an optional section.
     * <p>
     * The output of formatting can include optional sections, which may be nested.
     * An optional section is started by calling this method and ended by calling
     * {@link #optionalEnd()} or by ending the build process.
     * <p>
     * All elements in the optional section are treated as optional.
     * During formatting, the section is only output if data is available in the
     * {@code TemporalAccessor} for all the elements in the section.
     * During parsing, the whole section may be missing from the parsed string.
     * <p>
     * For example, consider a builder setup as
     * {@code builder.appendValue(HOUR_OF_DAY,2).optionalStart().appendValue(MINUTE_OF_HOUR,2)}.
     * The optional section ends automatically at the end of the builder.
     * During formatting, the minute will only be output if its value can be obtained from the date-time.
     * During parsing, the input will be successfully parsed whether the minute is present or not.
     *
     * @return this, for chaining, not null
     */
    public DateTimeFormatterBuilder optionalStart() {
        active.valueParserIndex = -1;
        active = new DateTimeFormatterBuilder(active, true);
        return this;
    }

    /**
     * Ends an optional section.
     * <p>
     * The output of formatting can include optional sections, which may be nested.
     * An optional section is started by calling {@link #optionalStart()} and ended
     * using this method (or at the end of the builder).
     * <p>
     * Calling this method without having previously called {@code optionalStart}
     * will throw an exception.
     * Calling this method immediately after calling {@code optionalStart} has no effect
     * on the formatter other than ending the (empty) optional section.
     * <p>
     * All elements in the optional section are treated as optional.
     * During formatting, the section is only output if data is available in the
     * {@code TemporalAccessor} for all the elements in the section.
     * During parsing, the whole section may be missing from the parsed string.
     * <p>
     * For example, consider a builder setup as
     * {@code builder.appendValue(HOUR_OF_DAY,2).optionalStart().appendValue(MINUTE_OF_HOUR,2).optionalEnd()}.
     * During formatting, the minute will only be output if its value can be obtained from the date-time.
     * During parsing, the input will be successfully parsed whether the minute is present or not.
     *
     * @return this, for chaining, not null
     * @throws IllegalStateException if there was no previous call to {@code optionalStart}
     */
    public DateTimeFormatterBuilder optionalEnd() {
        if (active.parent == null) {
            throw new IllegalStateException("Cannot call optionalEnd() as there was no previous call to optionalStart()");
        }
        if (active.printerParsers.size() > 0) {
            CompositePrinterParser cpp = new CompositePrinterParser(active.printerParsers, active.optional);
            active = active.parent;
            appendInternal(cpp);
        } else {
            active = active.parent;
        }
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Appends a printer and/or parser to the internal list handling padding.
     *
     * @param pp  the printer-parser to add, not null
     * @return the index into the active parsers list
     */
    private int appendInternal(DateTimePrinterParser pp) {
        Objects.requireNonNull(pp, "pp");
        if (active.padNextWidth > 0) {
            pp = new PadPrinterParserDecorator(pp, active.padNextWidth, active.padNextChar);
            active.padNextWidth = 0;
            active.padNextChar = 0;
        }
        active.printerParsers.add(pp);
        active.valueParserIndex = -1;
        return active.printerParsers.size() - 1;
    }

    //-----------------------------------------------------------------------
    /**
     * Completes this builder by creating the {@code DateTimeFormatter}
     * using the default locale.
     * <p>
     * This will create a formatter with the {@linkplain Locale#getDefault(Locale.Category) default FORMAT locale}.
     * Numbers will be printed and parsed using the standard DecimalStyle.
     * The resolver style will be {@link ResolverStyle#SMART SMART}.
     * <p>
     * Calling this method will end any open optional sections by repeatedly
     * calling {@link #optionalEnd()} before creating the formatter.
     * <p>
     * This builder can still be used after creating the formatter if desired,
     * although the state may have been changed by calls to {@code optionalEnd}.
     *
     * @return the created formatter, not null
     */
    public DateTimeFormatter toFormatter() {
        return toFormatter(Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Completes this builder by creating the {@code DateTimeFormatter}
     * using the specified locale.
     * <p>
     * This will create a formatter with the specified locale.
     * Numbers will be printed and parsed using the standard DecimalStyle.
     * The resolver style will be {@link ResolverStyle#SMART SMART}.
     * <p>
     * Calling this method will end any open optional sections by repeatedly
     * calling {@link #optionalEnd()} before creating the formatter.
     * <p>
     * This builder can still be used after creating the formatter if desired,
     * although the state may have been changed by calls to {@code optionalEnd}.
     *
     * @param locale  the locale to use for formatting, not null
     * @return the created formatter, not null
     */
    public DateTimeFormatter toFormatter(Locale locale) {
        return toFormatter(locale, ResolverStyle.SMART, null);
    }

    /**
     * Completes this builder by creating the formatter.
     * This uses the default locale.
     *
     * @param resolverStyle  the resolver style to use, not null
     * @return the created formatter, not null
     */
    DateTimeFormatter toFormatter(ResolverStyle resolverStyle, Chronology chrono) {
        return toFormatter(Locale.getDefault(Locale.Category.FORMAT), resolverStyle, chrono);
    }

    /**
     * Completes this builder by creating the formatter.
     *
     * @param locale  the locale to use for formatting, not null
     * @param chrono  the chronology to use, may be null
     * @return the created formatter, not null
     */
    private DateTimeFormatter toFormatter(Locale locale, ResolverStyle resolverStyle, Chronology chrono) {
        Objects.requireNonNull(locale, "locale");
        while (active.parent != null) {
            optionalEnd();
        }
        CompositePrinterParser pp = new CompositePrinterParser(printerParsers, false);
        return new DateTimeFormatter(pp, locale, DecimalStyle.STANDARD,
                resolverStyle, null, chrono, null);
    }

    //-----------------------------------------------------------------------
    /**
     * Strategy for formatting/parsing date-time information.
     * <p>
     * The printer may format any part, or the whole, of the input date-time object.
     * Typically, a complete format is constructed from a number of smaller
     * units, each outputting a single field.
     * <p>
     * The parser may parse any piece of text from the input, storing the result
     * in the context. Typically, each individual parser will just parse one
     * field, such as the day-of-month, storing the value in the context.
     * Once the parse is complete, the caller will then resolve the parsed values
     * to create the desired object, such as a {@code LocalDate}.
     * <p>
     * The parse position will be updated during the parse. Parsing will start at
     * the specified index and the return value specifies the new parse position
     * for the next parser. If an error occurs, the returned index will be negative
     * and will have the error position encoded using the complement operator.
     *
     * @implSpec
     * This interface must be implemented with care to ensure other classes operate correctly.
     * All implementations that can be instantiated must be final, immutable and thread-safe.
     * <p>
     * The context is not a thread-safe object and a new instance will be created
     * for each format that occurs. The context must not be stored in an instance
     * variable or shared with any other threads.
     */
    interface DateTimePrinterParser {

        /**
         * Prints the date-time object to the buffer.
         * <p>
         * The context holds information to use during the format.
         * It also contains the date-time information to be printed.
         * <p>
         * The buffer must not be mutated beyond the content controlled by the implementation.
         *
         * @param context  the context to format using, not null
         * @param buf  the buffer to append to, not null
         * @return false if unable to query the value from the date-time, true otherwise
         * @throws DateTimeException if the date-time cannot be printed successfully
         */
        boolean format(DateTimePrintContext context, StringBuilder buf);

        /**
         * Parses text into date-time information.
         * <p>
         * The context holds information to use during the parse.
         * It is also used to store the parsed date-time information.
         *
         * @param context  the context to use and parse into, not null
         * @param text  the input text to parse, not null
         * @param position  the position to start parsing at, from 0 to the text length
         * @return the new parse position, where negative means an error with the
         *  error position encoded using the complement ~ operator
         * @throws NullPointerException if the context or text is null
         * @throws IndexOutOfBoundsException if the position is invalid
         */
        int parse(DateTimeParseContext context, CharSequence text, int position);
    }

    //-----------------------------------------------------------------------
    /**
     * Composite printer and parser.
     */
    static final class CompositePrinterParser implements DateTimePrinterParser {
        private final DateTimePrinterParser[] printerParsers;
        private final boolean optional;

        CompositePrinterParser(List<DateTimePrinterParser> printerParsers, boolean optional) {
            this(printerParsers.toArray(new DateTimePrinterParser[0]), optional);
        }

        CompositePrinterParser(DateTimePrinterParser[] printerParsers, boolean optional) {
            this.printerParsers = printerParsers;
            this.optional = optional;
        }

        /**
         * Returns a copy of this printer-parser with the optional flag changed.
         *
         * @param optional  the optional flag to set in the copy
         * @return the new printer-parser, not null
         */
        public CompositePrinterParser withOptional(boolean optional) {
            if (optional == this.optional) {
                return this;
            }
            return new CompositePrinterParser(printerParsers, optional);
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            int length = buf.length();
            if (optional) {
                context.startOptional();
            }
            try {
                for (DateTimePrinterParser pp : printerParsers) {
                    if (pp.format(context, buf) == false) {
                        buf.setLength(length);  // reset buffer
                        return true;
                    }
                }
            } finally {
                if (optional) {
                    context.endOptional();
                }
            }
            return true;
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            if (optional) {
                context.startOptional();
                int pos = position;
                for (DateTimePrinterParser pp : printerParsers) {
                    pos = pp.parse(context, text, pos);
                    if (pos < 0) {
                        context.endOptional(false);
                        return position;  // return original position
                    }
                }
                context.endOptional(true);
                return pos;
            } else {
                for (DateTimePrinterParser pp : printerParsers) {
                    position = pp.parse(context, text, position);
                    if (position < 0) {
                        break;
                    }
                }
                return position;
            }
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            if (printerParsers != null) {
                buf.append(optional ? "[" : "(");
                for (DateTimePrinterParser pp : printerParsers) {
                    buf.append(pp);
                }
                buf.append(optional ? "]" : ")");
            }
            return buf.toString();
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Pads the output to a fixed width.
     */
    static final class PadPrinterParserDecorator implements DateTimePrinterParser {
        private final DateTimePrinterParser printerParser;
        private final int padWidth;
        private final char padChar;

        /**
         * Constructor.
         *
         * @param printerParser  the printer, not null
         * @param padWidth  the width to pad to, 1 or greater
         * @param padChar  the pad character
         */
        PadPrinterParserDecorator(DateTimePrinterParser printerParser, int padWidth, char padChar) {
            // input checked by DateTimeFormatterBuilder
            this.printerParser = printerParser;
            this.padWidth = padWidth;
            this.padChar = padChar;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            int preLen = buf.length();
            if (printerParser.format(context, buf) == false) {
                return false;
            }
            int len = buf.length() - preLen;
            if (len > padWidth) {
                throw new DateTimeException(
                    "Cannot print as output of " + len + " characters exceeds pad width of " + padWidth);
            }
            for (int i = 0; i < padWidth - len; i++) {
                buf.insert(preLen, padChar);
            }
            return true;
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            // cache context before changed by decorated parser
            final boolean strict = context.isStrict();
            // parse
            if (position > text.length()) {
                throw new IndexOutOfBoundsException();
            }
            if (position == text.length()) {
                return ~position;  // no more characters in the string
            }
            int endPos = position + padWidth;
            if (endPos > text.length()) {
                if (strict) {
                    return ~position;  // not enough characters in the string to meet the parse width
                }
                endPos = text.length();
            }
            int pos = position;
            while (pos < endPos && context.charEquals(text.charAt(pos), padChar)) {
                pos++;
            }
            text = text.subSequence(0, endPos);
            int resultPos = printerParser.parse(context, text, pos);
            if (resultPos != endPos && strict) {
                return ~(position + pos);  // parse of decorated field didn't parse to the end
            }
            return resultPos;
        }

        @Override
        public String toString() {
            return "Pad(" + printerParser + "," + padWidth + (padChar == ' ' ? ")" : ",'" + padChar + "')");
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Enumeration to apply simple parse settings.
     */
    static enum SettingsParser implements DateTimePrinterParser {
        SENSITIVE,
        INSENSITIVE,
        STRICT,
        LENIENT;

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            return true;  // nothing to do here
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            // using ordinals to avoid javac synthetic inner class
            switch (ordinal()) {
                case 0 -> context.setCaseSensitive(true);
                case 1 -> context.setCaseSensitive(false);
                case 2 -> context.setStrict(true);
                case 3 -> context.setStrict(false);
            }
            return position;
        }

        @Override
        public String toString() {
            // using ordinals to avoid javac synthetic inner class
            return switch (ordinal()) {
                case 0 -> "ParseCaseSensitive(true)";
                case 1 -> "ParseCaseSensitive(false)";
                case 2 -> "ParseStrict(true)";
                case 3 -> "ParseStrict(false)";
                default -> throw new IllegalStateException("Unreachable");
            };
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Defaults a value into the parse if not currently present.
     */
    static class DefaultValueParser implements DateTimePrinterParser {
        private final TemporalField field;
        private final long value;

        DefaultValueParser(TemporalField field, long value) {
            this.field = field;
            this.value = value;
        }

        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            return true;
        }

        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            if (context.getParsed(field) == null) {
                context.setParsedField(field, value, position, position);
            }
            return position;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses a character literal.
     */
    static final class CharLiteralPrinterParser implements DateTimePrinterParser {
        private final char literal;

        CharLiteralPrinterParser(char literal) {
            this.literal = literal;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            buf.append(literal);
            return true;
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int length = text.length();
            if (position == length) {
                return ~position;
            }
            char ch = text.charAt(position);
            if (ch != literal) {
                if (context.isCaseSensitive() ||
                        (Character.toUpperCase(ch) != Character.toUpperCase(literal) &&
                         Character.toLowerCase(ch) != Character.toLowerCase(literal))) {
                    return ~position;
                }
            }
            return position + 1;
        }

        @Override
        public String toString() {
            if (literal == '\'') {
                return "''";
            }
            return "'" + literal + "'";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses a string literal.
     */
    static final class StringLiteralPrinterParser implements DateTimePrinterParser {
        private final String literal;

        StringLiteralPrinterParser(String literal) {
            this.literal = literal;  // validated by caller
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            buf.append(literal);
            return true;
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int length = text.length();
            if (position > length || position < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (context.subSequenceEquals(text, position, literal, 0, literal.length()) == false) {
                return ~position;
            }
            return position + literal.length();
        }

        @Override
        public String toString() {
            String converted = literal.replace("'", "''");
            return "'" + converted + "'";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints and parses a numeric date-time field with optional padding.
     */
    static class NumberPrinterParser implements DateTimePrinterParser {

        /**
         * Array of 10 to the power of n.
         */
        static final long[] EXCEED_POINTS = new long[] {
            0L,
            10L,
            100L,
            1000L,
            10000L,
            100000L,
            1000000L,
            10000000L,
            100000000L,
            1000000000L,
            10000000000L,
        };

        final TemporalField field;
        final int minWidth;
        final int maxWidth;
        private final SignStyle signStyle;
        final int subsequentWidth;

        /**
         * Constructor.
         *
         * @param field  the field to format, not null
         * @param minWidth  the minimum field width, from 1 to 19
         * @param maxWidth  the maximum field width, from minWidth to 19
         * @param signStyle  the positive/negative sign style, not null
         */
        NumberPrinterParser(TemporalField field, int minWidth, int maxWidth, SignStyle signStyle) {
            // validated by caller
            this.field = field;
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
            this.signStyle = signStyle;
            this.subsequentWidth = 0;
        }

        /**
         * Constructor.
         *
         * @param field  the field to format, not null
         * @param minWidth  the minimum field width, from 1 to 19
         * @param maxWidth  the maximum field width, from minWidth to 19
         * @param signStyle  the positive/negative sign style, not null
         * @param subsequentWidth  the width of subsequent non-negative numbers, 0 or greater,
         *  -1 if fixed width due to active adjacent parsing
         */
        protected NumberPrinterParser(TemporalField field, int minWidth, int maxWidth, SignStyle signStyle, int subsequentWidth) {
            // validated by caller
            this.field = field;
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
            this.signStyle = signStyle;
            this.subsequentWidth = subsequentWidth;
        }

        /**
         * Returns a new instance with fixed width flag set.
         *
         * @return a new updated printer-parser, not null
         */
        NumberPrinterParser withFixedWidth() {
            if (subsequentWidth == -1) {
                return this;
            }
            return new NumberPrinterParser(field, minWidth, maxWidth, signStyle, -1);
        }

        /**
         * Returns a new instance with an updated subsequent width.
         *
         * @param subsequentWidth  the width of subsequent non-negative numbers, 0 or greater
         * @return a new updated printer-parser, not null
         */
        NumberPrinterParser withSubsequentWidth(int subsequentWidth) {
            return new NumberPrinterParser(field, minWidth, maxWidth, signStyle, this.subsequentWidth + subsequentWidth);
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long valueLong = context.getValue(field);
            if (valueLong == null) {
                return false;
            }
            long value = getValue(context, valueLong);
            DecimalStyle decimalStyle = context.getDecimalStyle();
            String str = (value == Long.MIN_VALUE ? "9223372036854775808" : Long.toString(Math.abs(value)));
            if (str.length() > maxWidth) {
                throw new DateTimeException("Field " + field +
                    " cannot be printed as the value " + value +
                    " exceeds the maximum print width of " + maxWidth);
            }
            str = decimalStyle.convertNumberToI18N(str);

            if (value >= 0) {
                switch (signStyle) {
                    case EXCEEDS_PAD:
                        if (minWidth < 19 && value >= EXCEED_POINTS[minWidth]) {
                            buf.append(decimalStyle.getPositiveSign());
                        }
                        break;
                    case ALWAYS:
                        buf.append(decimalStyle.getPositiveSign());
                        break;
                }
            } else {
                switch (signStyle) {
                    case NORMAL, EXCEEDS_PAD, ALWAYS -> buf.append(decimalStyle.getNegativeSign());
                    case NOT_NEGATIVE -> throw new DateTimeException("Field " + field +
                                             " cannot be printed as the value " + value +
                                             " cannot be negative according to the SignStyle");
                }
            }
            for (int i = 0; i < minWidth - str.length(); i++) {
                buf.append(decimalStyle.getZeroDigit());
            }
            buf.append(str);
            return true;
        }

        /**
         * Gets the value to output.
         *
         * @param context  the context
         * @param value  the value of the field, not null
         * @return the value
         */
        long getValue(DateTimePrintContext context, long value) {
            return value;
        }

        /**
         * For NumberPrinterParser, the width is fixed depending on the
         * minWidth, maxWidth, signStyle and whether subsequent fields are fixed.
         * @param context the context
         * @return true if the field is fixed width
         * @see DateTimeFormatterBuilder#appendValue(java.time.temporal.TemporalField, int)
         */
        boolean isFixedWidth(DateTimeParseContext context) {
            return subsequentWidth == -1 ||
                (subsequentWidth > 0 && minWidth == maxWidth && signStyle == SignStyle.NOT_NEGATIVE);
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int length = text.length();
            if (position == length) {
                return ~position;
            }
            char sign = text.charAt(position);  // IOOBE if invalid position
            boolean negative = false;
            boolean positive = false;
            if (sign == context.getDecimalStyle().getPositiveSign()) {
                if (signStyle.parse(true, context.isStrict(), minWidth == maxWidth) == false) {
                    return ~position;
                }
                positive = true;
                position++;
            } else if (sign == context.getDecimalStyle().getNegativeSign()) {
                if (signStyle.parse(false, context.isStrict(), minWidth == maxWidth) == false) {
                    return ~position;
                }
                negative = true;
                position++;
            } else {
                if (signStyle == SignStyle.ALWAYS && context.isStrict()) {
                    return ~position;
                }
            }
            int effMinWidth = (context.isStrict() || isFixedWidth(context) ? minWidth : 1);
            int minEndPos = position + effMinWidth;
            if (minEndPos > length) {
                return ~position;
            }
            int effMaxWidth = (context.isStrict() || isFixedWidth(context) ? maxWidth : 9) + Math.max(subsequentWidth, 0);
            long total = 0;
            BigInteger totalBig = null;
            int pos = position;
            for (int pass = 0; pass < 2; pass++) {
                int maxEndPos = Math.min(pos + effMaxWidth, length);
                while (pos < maxEndPos) {
                    char ch = text.charAt(pos++);
                    int digit = context.getDecimalStyle().convertToDigit(ch);
                    if (digit < 0) {
                        pos--;
                        if (pos < minEndPos) {
                            return ~position;  // need at least min width digits
                        }
                        break;
                    }
                    if ((pos - position) > 18) {
                        if (totalBig == null) {
                            totalBig = BigInteger.valueOf(total);
                        }
                        totalBig = totalBig.multiply(BigInteger.TEN).add(BigInteger.valueOf(digit));
                    } else {
                        total = total * 10 + digit;
                    }
                }
                if (subsequentWidth > 0 && pass == 0) {
                    // re-parse now we know the correct width
                    int parseLen = pos - position;
                    effMaxWidth = Math.max(effMinWidth, parseLen - subsequentWidth);
                    pos = position;
                    total = 0;
                    totalBig = null;
                } else {
                    break;
                }
            }
            if (negative) {
                if (totalBig != null) {
                    if (totalBig.equals(BigInteger.ZERO) && context.isStrict()) {
                        return ~(position - 1);  // minus zero not allowed
                    }
                    totalBig = totalBig.negate();
                } else {
                    if (total == 0 && context.isStrict()) {
                        return ~(position - 1);  // minus zero not allowed
                    }
                    total = -total;
                }
            } else if (signStyle == SignStyle.EXCEEDS_PAD && context.isStrict()) {
                int parseLen = pos - position;
                if (positive) {
                    if (parseLen <= minWidth) {
                        return ~(position - 1);  // '+' only parsed if minWidth exceeded
                    }
                } else {
                    if (parseLen > minWidth) {
                        return ~position;  // '+' must be parsed if minWidth exceeded
                    }
                }
            }
            if (totalBig != null) {
                if (totalBig.bitLength() > 63) {
                    // overflow, parse 1 less digit
                    totalBig = totalBig.divide(BigInteger.TEN);
                    pos--;
                }
                return setValue(context, totalBig.longValue(), position, pos);
            }
            return setValue(context, total, position, pos);
        }

        /**
         * Stores the value.
         *
         * @param context  the context to store into, not null
         * @param value  the value
         * @param errorPos  the position of the field being parsed
         * @param successPos  the position after the field being parsed
         * @return the new position
         */
        int setValue(DateTimeParseContext context, long value, int errorPos, int successPos) {
            return context.setParsedField(field, value, errorPos, successPos);
        }

        @Override
        public String toString() {
            if (minWidth == 1 && maxWidth == 19 && signStyle == SignStyle.NORMAL) {
                return "Value(" + field + ")";
            }
            if (minWidth == maxWidth && signStyle == SignStyle.NOT_NEGATIVE) {
                return "Value(" + field + "," + minWidth + ")";
            }
            return "Value(" + field + "," + minWidth + "," + maxWidth + "," + signStyle + ")";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints and parses a reduced numeric date-time field.
     */
    static final class ReducedPrinterParser extends NumberPrinterParser {
        /**
         * The base date for reduced value parsing.
         */
        static final LocalDate BASE_DATE = LocalDate.of(2000, 1, 1);

        private final int baseValue;
        private final ChronoLocalDate baseDate;

        /**
         * Constructor.
         *
         * @param field  the field to format, validated not null
         * @param minWidth  the minimum field width, from 1 to 10
         * @param maxWidth  the maximum field width, from 1 to 10
         * @param baseValue  the base value
         * @param baseDate  the base date
         */
        ReducedPrinterParser(TemporalField field, int minWidth, int maxWidth,
                int baseValue, ChronoLocalDate baseDate) {
            this(field, minWidth, maxWidth, baseValue, baseDate, 0);
            if (minWidth < 1 || minWidth > 10) {
                throw new IllegalArgumentException("The minWidth must be from 1 to 10 inclusive but was " + minWidth);
            }
            if (maxWidth < 1 || maxWidth > 10) {
                throw new IllegalArgumentException("The maxWidth must be from 1 to 10 inclusive but was " + minWidth);
            }
            if (maxWidth < minWidth) {
                throw new IllegalArgumentException("Maximum width must exceed or equal the minimum width but " +
                        maxWidth + " < " + minWidth);
            }
            if (baseDate == null) {
                if (field.range().isValidValue(baseValue) == false) {
                    throw new IllegalArgumentException("The base value must be within the range of the field");
                }
                if ((((long) baseValue) + EXCEED_POINTS[maxWidth]) > Integer.MAX_VALUE) {
                    throw new DateTimeException("Unable to add printer-parser as the range exceeds the capacity of an int");
                }
            }
        }

        /**
         * Constructor.
         * The arguments have already been checked.
         *
         * @param field  the field to format, validated not null
         * @param minWidth  the minimum field width, from 1 to 10
         * @param maxWidth  the maximum field width, from 1 to 10
         * @param baseValue  the base value
         * @param baseDate  the base date
         * @param subsequentWidth the subsequentWidth for this instance
         */
        private ReducedPrinterParser(TemporalField field, int minWidth, int maxWidth,
                int baseValue, ChronoLocalDate baseDate, int subsequentWidth) {
            super(field, minWidth, maxWidth, SignStyle.NOT_NEGATIVE, subsequentWidth);
            this.baseValue = baseValue;
            this.baseDate = baseDate;
        }

        @Override
        long getValue(DateTimePrintContext context, long value) {
            long absValue = Math.abs(value);
            int baseValue = this.baseValue;
            if (baseDate != null) {
                Chronology chrono = Chronology.from(context.getTemporal());
                baseValue = chrono.date(baseDate).get(field);
            }
            if (value >= baseValue && value < baseValue + EXCEED_POINTS[minWidth]) {
                // Use the reduced value if it fits in minWidth
                return absValue % EXCEED_POINTS[minWidth];
            }
            // Otherwise truncate to fit in maxWidth
            return absValue % EXCEED_POINTS[maxWidth];
        }

        @Override
        int setValue(DateTimeParseContext context, long value, int errorPos, int successPos) {
            int baseValue = this.baseValue;
            if (baseDate != null) {
                Chronology chrono = context.getEffectiveChronology();
                baseValue = chrono.date(baseDate).get(field);

                // In case the Chronology is changed later, add a callback when/if it changes
                final long initialValue = value;
                context.addChronoChangedListener(
                        (_unused) ->  {
                            /* Repeat the set of the field using the current Chronology
                             * The success/error position is ignored because the value is
                             * intentionally being overwritten.
                             */
                            setValue(context, initialValue, errorPos, successPos);
                        });
            }
            int parseLen = successPos - errorPos;
            if (parseLen == minWidth && value >= 0) {
                long range = EXCEED_POINTS[minWidth];
                long lastPart = baseValue % range;
                long basePart = baseValue - lastPart;
                if (baseValue > 0) {
                    value = basePart + value;
                } else {
                    value = basePart - value;
                }
                if (value < baseValue) {
                    value += range;
                }
            }
            return context.setParsedField(field, value, errorPos, successPos);
        }

        /**
         * Returns a new instance with fixed width flag set.
         *
         * @return a new updated printer-parser, not null
         */
        @Override
        ReducedPrinterParser withFixedWidth() {
            if (subsequentWidth == -1) {
                return this;
            }
            return new ReducedPrinterParser(field, minWidth, maxWidth, baseValue, baseDate, -1);
        }

        /**
         * Returns a new instance with an updated subsequent width.
         *
         * @param subsequentWidth  the width of subsequent non-negative numbers, 0 or greater
         * @return a new updated printer-parser, not null
         */
        @Override
        ReducedPrinterParser withSubsequentWidth(int subsequentWidth) {
            return new ReducedPrinterParser(field, minWidth, maxWidth, baseValue, baseDate,
                    this.subsequentWidth + subsequentWidth);
        }

        /**
         * For a ReducedPrinterParser, fixed width is false if the mode is strict,
         * otherwise it is set as for NumberPrinterParser.
         * @param context the context
         * @return if the field is fixed width
         * @see DateTimeFormatterBuilder#appendValueReduced(java.time.temporal.TemporalField, int, int, int)
         */
        @Override
        boolean isFixedWidth(DateTimeParseContext context) {
           if (context.isStrict() == false) {
               return false;
           }
           return super.isFixedWidth(context);
        }

        @Override
        public String toString() {
            return "ReducedValue(" + field + "," + minWidth + "," + maxWidth +
                    "," + Objects.requireNonNullElse(baseDate, baseValue) + ")";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints and parses a numeric date-time field with optional padding.
     */
    static final class FractionPrinterParser extends NumberPrinterParser {
        private final boolean decimalPoint;

        /**
         * Constructor.
         *
         * @param field  the field to output, not null
         * @param minWidth  the minimum width to output, from 0 to 9
         * @param maxWidth  the maximum width to output, from 0 to 9
         * @param decimalPoint  whether to output the localized decimal point symbol
         */
        FractionPrinterParser(TemporalField field, int minWidth, int maxWidth, boolean decimalPoint) {
            this(field, minWidth, maxWidth, decimalPoint, 0);
            Objects.requireNonNull(field, "field");
            if (field.range().isFixed() == false) {
                throw new IllegalArgumentException("Field must have a fixed set of values: " + field);
            }
            if (minWidth < 0 || minWidth > 9) {
                throw new IllegalArgumentException("Minimum width must be from 0 to 9 inclusive but was " + minWidth);
            }
            if (maxWidth < 1 || maxWidth > 9) {
                throw new IllegalArgumentException("Maximum width must be from 1 to 9 inclusive but was " + maxWidth);
            }
            if (maxWidth < minWidth) {
                throw new IllegalArgumentException("Maximum width must exceed or equal the minimum width but " +
                        maxWidth + " < " + minWidth);
            }
        }

        /**
         * Constructor.
         *
         * @param field  the field to output, not null
         * @param minWidth  the minimum width to output, from 0 to 9
         * @param maxWidth  the maximum width to output, from 0 to 9
         * @param decimalPoint  whether to output the localized decimal point symbol
         * @param subsequentWidth the subsequentWidth for this instance
         */
        FractionPrinterParser(TemporalField field, int minWidth, int maxWidth, boolean decimalPoint, int subsequentWidth) {
            super(field, minWidth, maxWidth, SignStyle.NOT_NEGATIVE, subsequentWidth);
            this.decimalPoint = decimalPoint;
        }

        /**
         * Returns a new instance with fixed width flag set.
         *
         * @return a new updated printer-parser, not null
         */
        @Override
        FractionPrinterParser withFixedWidth() {
            if (subsequentWidth == -1) {
                return this;
            }
            return new FractionPrinterParser(field, minWidth, maxWidth, decimalPoint, -1);
        }

        /**
         * Returns a new instance with an updated subsequent width.
         *
         * @param subsequentWidth  the width of subsequent non-negative numbers, 0 or greater
         * @return a new updated printer-parser, not null
         */
        @Override
        FractionPrinterParser withSubsequentWidth(int subsequentWidth) {
            return new FractionPrinterParser(field, minWidth, maxWidth, decimalPoint, this.subsequentWidth + subsequentWidth);
        }

        /**
         * For FractionPrinterPrinterParser, the width is fixed if context is strict,
         * minWidth equal to maxWidth and decimalpoint is absent.
         * @param context the context
         * @return if the field is fixed width
         * @see #appendFraction(java.time.temporal.TemporalField, int, int, boolean)
         */
        @Override
        boolean isFixedWidth(DateTimeParseContext context) {
            if (context.isStrict() && minWidth == maxWidth && decimalPoint == false) {
                return true;
            }
            return false;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long value = context.getValue(field);
            if (value == null) {
                return false;
            }
            DecimalStyle decimalStyle = context.getDecimalStyle();
            BigDecimal fraction = convertToFraction(value);
            if (fraction.scale() == 0) {  // scale is zero if value is zero
                if (minWidth > 0) {
                    if (decimalPoint) {
                        buf.append(decimalStyle.getDecimalSeparator());
                    }
                    for (int i = 0; i < minWidth; i++) {
                        buf.append(decimalStyle.getZeroDigit());
                    }
                }
            } else {
                int outputScale = Math.min(Math.max(fraction.scale(), minWidth), maxWidth);
                fraction = fraction.setScale(outputScale, RoundingMode.FLOOR);
                String str = fraction.toPlainString().substring(2);
                str = decimalStyle.convertNumberToI18N(str);
                if (decimalPoint) {
                    buf.append(decimalStyle.getDecimalSeparator());
                }
                buf.append(str);
            }
            return true;
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int effectiveMin = (context.isStrict() || isFixedWidth(context) ? minWidth : 0);
            int effectiveMax = (context.isStrict() || isFixedWidth(context) ? maxWidth : 9);
            int length = text.length();
            if (position == length) {
                // valid if whole field is optional, invalid if minimum width
                return (effectiveMin > 0 ? ~position : position);
            }
            if (decimalPoint) {
                if (text.charAt(position) != context.getDecimalStyle().getDecimalSeparator()) {
                    // valid if whole field is optional, invalid if minimum width
                    return (effectiveMin > 0 ? ~position : position);
                }
                position++;
            }
            int minEndPos = position + effectiveMin;
            if (minEndPos > length) {
                return ~position;  // need at least min width digits
            }
            int maxEndPos = Math.min(position + effectiveMax, length);
            int total = 0;  // can use int because we are only parsing up to 9 digits
            int pos = position;
            while (pos < maxEndPos) {
                char ch = text.charAt(pos++);
                int digit = context.getDecimalStyle().convertToDigit(ch);
                if (digit < 0) {
                    if (pos <= minEndPos) {
                        return ~position;  // need at least min width digits
                    }
                    pos--;
                    break;
                }
                total = total * 10 + digit;
            }
            BigDecimal fraction = new BigDecimal(total).movePointLeft(pos - position);
            long value = convertFromFraction(fraction);
            return context.setParsedField(field, value, position, pos);
        }

        /**
         * Converts a value for this field to a fraction between 0 and 1.
         * <p>
         * The fractional value is between 0 (inclusive) and 1 (exclusive).
         * It can only be returned if the {@link java.time.temporal.TemporalField#range() value range} is fixed.
         * The fraction is obtained by calculation from the field range using 9 decimal
         * places and a rounding mode of {@link RoundingMode#FLOOR FLOOR}.
         * The calculation is inaccurate if the values do not run continuously from smallest to largest.
         * <p>
         * For example, the second-of-minute value of 15 would be returned as 0.25,
         * assuming the standard definition of 60 seconds in a minute.
         *
         * @param value  the value to convert, must be valid for this rule
         * @return the value as a fraction within the range, from 0 to 1, not null
         * @throws DateTimeException if the value cannot be converted to a fraction
         */
        private BigDecimal convertToFraction(long value) {
            ValueRange range = field.range();
            range.checkValidValue(value, field);
            BigDecimal minBD = BigDecimal.valueOf(range.getMinimum());
            BigDecimal rangeBD = BigDecimal.valueOf(range.getMaximum()).subtract(minBD).add(BigDecimal.ONE);
            BigDecimal valueBD = BigDecimal.valueOf(value).subtract(minBD);
            BigDecimal fraction = valueBD.divide(rangeBD, 9, RoundingMode.FLOOR);
            // stripTrailingZeros bug
            return fraction.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : fraction.stripTrailingZeros();
        }

        /**
         * Converts a fraction from 0 to 1 for this field to a value.
         * <p>
         * The fractional value must be between 0 (inclusive) and 1 (exclusive).
         * It can only be returned if the {@link java.time.temporal.TemporalField#range() value range} is fixed.
         * The value is obtained by calculation from the field range and a rounding
         * mode of {@link RoundingMode#FLOOR FLOOR}.
         * The calculation is inaccurate if the values do not run continuously from smallest to largest.
         * <p>
         * For example, the fractional second-of-minute of 0.25 would be converted to 15,
         * assuming the standard definition of 60 seconds in a minute.
         *
         * @param fraction  the fraction to convert, not null
         * @return the value of the field, valid for this rule
         * @throws DateTimeException if the value cannot be converted
         */
        private long convertFromFraction(BigDecimal fraction) {
            ValueRange range = field.range();
            BigDecimal minBD = BigDecimal.valueOf(range.getMinimum());
            BigDecimal rangeBD = BigDecimal.valueOf(range.getMaximum()).subtract(minBD).add(BigDecimal.ONE);
            BigDecimal valueBD = fraction.multiply(rangeBD).setScale(0, RoundingMode.FLOOR).add(minBD);
            return valueBD.longValueExact();
        }

        @Override
        public String toString() {
            String decimal = (decimalPoint ? ",DecimalPoint" : "");
            return "Fraction(" + field + "," + minWidth + "," + maxWidth + decimal + ")";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses field text.
     */
    static final class TextPrinterParser implements DateTimePrinterParser {
        private final TemporalField field;
        private final TextStyle textStyle;
        private final DateTimeTextProvider provider;
        /**
         * The cached number printer parser.
         * Immutable and volatile, so no synchronization needed.
         */
        private volatile NumberPrinterParser numberPrinterParser;

        /**
         * Constructor.
         *
         * @param field  the field to output, not null
         * @param textStyle  the text style, not null
         * @param provider  the text provider, not null
         */
        TextPrinterParser(TemporalField field, TextStyle textStyle, DateTimeTextProvider provider) {
            // validated by caller
            this.field = field;
            this.textStyle = textStyle;
            this.provider = provider;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long value = context.getValue(field);
            if (value == null) {
                return false;
            }
            String text;
            Chronology chrono = context.getTemporal().query(TemporalQueries.chronology());
            if (chrono == null || chrono == IsoChronology.INSTANCE) {
                text = provider.getText(field, value, textStyle, context.getLocale());
            } else {
                text = provider.getText(chrono, field, value, textStyle, context.getLocale());
            }
            if (text == null) {
                return numberPrinterParser().format(context, buf);
            }
            buf.append(text);
            return true;
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence parseText, int position) {
            int length = parseText.length();
            if (position < 0 || position > length) {
                throw new IndexOutOfBoundsException();
            }
            TextStyle style = (context.isStrict() ? textStyle : null);
            Chronology chrono = context.getEffectiveChronology();
            Iterator<Entry<String, Long>> it;
            if (chrono == null || chrono == IsoChronology.INSTANCE) {
                it = provider.getTextIterator(field, style, context.getLocale());
            } else {
                it = provider.getTextIterator(chrono, field, style, context.getLocale());
            }
            if (it != null) {
                while (it.hasNext()) {
                    Entry<String, Long> entry = it.next();
                    String itText = entry.getKey();
                    if (context.subSequenceEquals(itText, 0, parseText, position, itText.length())) {
                        return context.setParsedField(field, entry.getValue(), position, position + itText.length());
                    }
                }
                if (field == ERA && !context.isStrict()) {
                    // parse the possible era name from era.toString()
                    List<Era> eras = chrono.eras();
                    for (Era era : eras) {
                        String name = era.toString();
                        if (context.subSequenceEquals(name, 0, parseText, position, name.length())) {
                            return context.setParsedField(field, era.getValue(), position, position + name.length());
                        }
                    }
                }
                if (context.isStrict()) {
                    return ~position;
                }
            }
            return numberPrinterParser().parse(context, parseText, position);
        }

        /**
         * Create and cache a number printer parser.
         * @return the number printer parser for this field, not null
         */
        private NumberPrinterParser numberPrinterParser() {
            if (numberPrinterParser == null) {
                numberPrinterParser = new NumberPrinterParser(field, 1, 19, SignStyle.NORMAL);
            }
            return numberPrinterParser;
        }

        @Override
        public String toString() {
            if (textStyle == TextStyle.FULL) {
                return "Text(" + field + ")";
            }
            return "Text(" + field + "," + textStyle + ")";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses an ISO-8601 instant.
     */
    static final class InstantPrinterParser implements DateTimePrinterParser {
        // days in a 400 year cycle = 146097
        // days in a 10,000 year cycle = 146097 * 25
        // seconds per day = 86400
        private static final long SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L;
        private static final long SECONDS_0000_TO_1970 = ((146097L * 5L) - (30L * 365L + 7L)) * 86400L;
        private final int fractionalDigits;

        InstantPrinterParser(int fractionalDigits) {
            this.fractionalDigits = fractionalDigits;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            // use INSTANT_SECONDS, thus this code is not bound by Instant.MAX
            Long inSecs = context.getValue(INSTANT_SECONDS);
            Long inNanos = null;
            if (context.getTemporal().isSupported(NANO_OF_SECOND)) {
                inNanos = context.getTemporal().getLong(NANO_OF_SECOND);
            }
            if (inSecs == null) {
                return false;
            }
            long inSec = inSecs;
            int inNano = NANO_OF_SECOND.checkValidIntValue(inNanos != null ? inNanos : 0);
            // format mostly using LocalDateTime.toString
            if (inSec >= -SECONDS_0000_TO_1970) {
                // current era
                long zeroSecs = inSec - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970;
                long hi = Math.floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1;
                long lo = Math.floorMod(zeroSecs, SECONDS_PER_10000_YEARS);
                LocalDateTime ldt = LocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, 0, ZoneOffset.UTC);
                if (hi > 0) {
                    buf.append('+').append(hi);
                }
                buf.append(ldt);
                if (ldt.getSecond() == 0) {
                    buf.append(":00");
                }
            } else {
                // before current era
                long zeroSecs = inSec + SECONDS_0000_TO_1970;
                long hi = zeroSecs / SECONDS_PER_10000_YEARS;
                long lo = zeroSecs % SECONDS_PER_10000_YEARS;
                LocalDateTime ldt = LocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, 0, ZoneOffset.UTC);
                int pos = buf.length();
                buf.append(ldt);
                if (ldt.getSecond() == 0) {
                    buf.append(":00");
                }
                if (hi < 0) {
                    if (ldt.getYear() == -10_000) {
                        buf.replace(pos, pos + 2, Long.toString(hi - 1));
                    } else if (lo == 0) {
                        buf.insert(pos, hi);
                    } else {
                        buf.insert(pos + 1, Math.abs(hi));
                    }
                }
            }
            // add fraction
            if ((fractionalDigits < 0 && inNano > 0) || fractionalDigits > 0) {
                buf.append('.');
                int div = 100_000_000;
                for (int i = 0; ((fractionalDigits == -1 && inNano > 0) ||
                                    (fractionalDigits == -2 && (inNano > 0 || (i % 3) != 0)) ||
                                    i < fractionalDigits); i++) {
                    int digit = inNano / div;
                    buf.append((char) (digit + '0'));
                    inNano = inNano - (digit * div);
                    div = div / 10;
                }
            }
            buf.append('Z');
            return true;
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            // new context to avoid overwriting fields like year/month/day
            int minDigits = (fractionalDigits < 0 ? 0 : fractionalDigits);
            int maxDigits = (fractionalDigits < 0 ? 9 : fractionalDigits);
            CompositePrinterParser parser = new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral('T')
                    .appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
                    .appendValue(MINUTE_OF_HOUR, 2).appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .appendFraction(NANO_OF_SECOND, minDigits, maxDigits, true)
                    .appendOffsetId()
                    .toFormatter().toPrinterParser(false);
            DateTimeParseContext newContext = context.copy();
            int pos = parser.parse(newContext, text, position);
            if (pos < 0) {
                return pos;
            }
            // parser restricts most fields to 2 digits, so definitely int
            // correctly parsed nano is also guaranteed to be valid
            long yearParsed = newContext.getParsed(YEAR);
            int month = newContext.getParsed(MONTH_OF_YEAR).intValue();
            int day = newContext.getParsed(DAY_OF_MONTH).intValue();
            int hour = newContext.getParsed(HOUR_OF_DAY).intValue();
            int min = newContext.getParsed(MINUTE_OF_HOUR).intValue();
            Long secVal = newContext.getParsed(SECOND_OF_MINUTE);
            Long nanoVal = newContext.getParsed(NANO_OF_SECOND);
            int sec = (secVal != null ? secVal.intValue() : 0);
            int nano = (nanoVal != null ? nanoVal.intValue() : 0);
            int offset = newContext.getParsed(OFFSET_SECONDS).intValue();
            int days = 0;
            if (hour == 24 && min == 0 && sec == 0 && nano == 0) {
                hour = 0;
                days = 1;
            } else if (hour == 23 && min == 59 && sec == 60) {
                context.setParsedLeapSecond();
                sec = 59;
            }
            int year = (int) yearParsed % 10_000;
            long instantSecs;
            try {
                LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, min, sec, 0).plusDays(days);
                instantSecs = ldt.toEpochSecond(ZoneOffset.ofTotalSeconds(offset));
                instantSecs += Math.multiplyExact(yearParsed / 10_000L, SECONDS_PER_10000_YEARS);
            } catch (RuntimeException ex) {
                return ~position;
            }
            int successPos = pos;
            successPos = context.setParsedField(INSTANT_SECONDS, instantSecs, position, successPos);
            return context.setParsedField(NANO_OF_SECOND, nano, position, successPos);
        }

        @Override
        public String toString() {
            return "Instant()";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses an offset ID.
     */
    static final class OffsetIdPrinterParser implements DateTimePrinterParser {
        static final String[] PATTERNS = new String[] {
                "+HH", "+HHmm", "+HH:mm", "+HHMM", "+HH:MM", "+HHMMss", "+HH:MM:ss", "+HHMMSS", "+HH:MM:SS", "+HHmmss", "+HH:mm:ss",
                "+H",  "+Hmm",  "+H:mm",  "+HMM",  "+H:MM",  "+HMMss",  "+H:MM:ss",  "+HMMSS",  "+H:MM:SS",  "+Hmmss",  "+H:mm:ss",
        };  // order used in pattern builder
        static final OffsetIdPrinterParser INSTANCE_ID_Z = new OffsetIdPrinterParser("+HH:MM:ss", "Z");
        static final OffsetIdPrinterParser INSTANCE_ID_ZERO = new OffsetIdPrinterParser("+HH:MM:ss", "0");

        private final String noOffsetText;
        private final int type;
        private final int style;

        /**
         * Constructor.
         *
         * @param pattern  the pattern
         * @param noOffsetText  the text to use for UTC, not null
         */
        OffsetIdPrinterParser(String pattern, String noOffsetText) {
            Objects.requireNonNull(pattern, "pattern");
            Objects.requireNonNull(noOffsetText, "noOffsetText");
            this.type = checkPattern(pattern);
            this.style = type % 11;
            this.noOffsetText = noOffsetText;
        }

        private int checkPattern(String pattern) {
            for (int i = 0; i < PATTERNS.length; i++) {
                if (PATTERNS[i].equals(pattern)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("Invalid zone offset pattern: " + pattern);
        }

        private boolean isPaddedHour() {
            return type < 11;
        }

        private boolean isColon() {
            return style > 0 && (style % 2) == 0;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long offsetSecs = context.getValue(OFFSET_SECONDS);
            if (offsetSecs == null) {
                return false;
            }
            int totalSecs = Math.toIntExact(offsetSecs);
            if (totalSecs == 0) {
                buf.append(noOffsetText);
            } else {
                int absHours = Math.abs((totalSecs / 3600) % 100);  // anything larger than 99 silently dropped
                int absMinutes = Math.abs((totalSecs / 60) % 60);
                int absSeconds = Math.abs(totalSecs % 60);
                int bufPos = buf.length();
                int output = absHours;
                buf.append(totalSecs < 0 ? "-" : "+");
                if (isPaddedHour() || absHours >= 10) {
                    formatZeroPad(false, absHours, buf);
                } else {
                    buf.append((char) (absHours + '0'));
                }
                if ((style >= 3 && style <= 8) || (style >= 9 && absSeconds > 0) || (style >= 1 && absMinutes > 0)) {
                    formatZeroPad(isColon(), absMinutes, buf);
                    output += absMinutes;
                    if (style == 7 || style == 8 || (style >= 5 && absSeconds > 0)) {
                        formatZeroPad(isColon(), absSeconds, buf);
                        output += absSeconds;
                    }
                }
                if (output == 0) {
                    buf.setLength(bufPos);
                    buf.append(noOffsetText);
                }
            }
            return true;
        }

        private void formatZeroPad(boolean colon, int value, StringBuilder buf) {
            buf.append(colon ? ":" : "")
                    .append((char) (value / 10 + '0'))
                    .append((char) (value % 10 + '0'));
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int length = text.length();
            int noOffsetLen = noOffsetText.length();
            if (noOffsetLen == 0) {
                if (position == length) {
                    return context.setParsedField(OFFSET_SECONDS, 0, position, position);
                }
            } else {
                if (position == length) {
                    return ~position;
                }
                if (context.subSequenceEquals(text, position, noOffsetText, 0, noOffsetLen)) {
                    return context.setParsedField(OFFSET_SECONDS, 0, position, position + noOffsetLen);
                }
            }

            // parse normal plus/minus offset
            char sign = text.charAt(position);  // IOOBE if invalid position
            if (sign == '+' || sign == '-') {
                // starts
                int negative = (sign == '-' ? -1 : 1);
                boolean isColon = isColon();
                boolean paddedHour = isPaddedHour();
                int[] array = new int[4];
                array[0] = position + 1;
                int parseType = type;
                // select parse type when lenient
                if (!context.isStrict()) {
                    if (paddedHour) {
                        if (isColon || (parseType == 0 && length > position + 3 && text.charAt(position + 3) == ':')) {
                            isColon = true; // needed in cases like ("+HH", "+01:01")
                            parseType = 10;
                        } else {
                            parseType = 9;
                        }
                    } else {
                        if (isColon || (parseType == 11 && length > position + 3 && (text.charAt(position + 2) == ':' || text.charAt(position + 3) == ':'))) {
                            isColon = true;
                            parseType = 21;  // needed in cases like ("+H", "+1:01")
                        } else {
                            parseType = 20;
                        }
                    }
                }
                // parse according to the selected pattern
                switch (parseType) {
                    case 0: // +HH
                    case 11: // +H
                        parseHour(text, paddedHour, array);
                        break;
                    case 1: // +HHmm
                    case 2: // +HH:mm
                    case 13: // +H:mm
                        parseHour(text, paddedHour, array);
                        parseMinute(text, isColon, false, array);
                        break;
                    case 3: // +HHMM
                    case 4: // +HH:MM
                    case 15: // +H:MM
                        parseHour(text, paddedHour, array);
                        parseMinute(text, isColon, true, array);
                        break;
                    case 5: // +HHMMss
                    case 6: // +HH:MM:ss
                    case 17: // +H:MM:ss
                        parseHour(text, paddedHour, array);
                        parseMinute(text, isColon, true, array);
                        parseSecond(text, isColon, false, array);
                        break;
                    case 7: // +HHMMSS
                    case 8: // +HH:MM:SS
                    case 19: // +H:MM:SS
                        parseHour(text, paddedHour, array);
                        parseMinute(text, isColon, true, array);
                        parseSecond(text, isColon, true, array);
                        break;
                    case 9: // +HHmmss
                    case 10: // +HH:mm:ss
                    case 21: // +H:mm:ss
                        parseHour(text, paddedHour, array);
                        parseOptionalMinuteSecond(text, isColon, array);
                        break;
                    case 12: // +Hmm
                        parseVariableWidthDigits(text, 1, 4, array);
                        break;
                    case 14: // +HMM
                        parseVariableWidthDigits(text, 3, 4, array);
                        break;
                    case 16: // +HMMss
                        parseVariableWidthDigits(text, 3, 6, array);
                        break;
                    case 18: // +HMMSS
                        parseVariableWidthDigits(text, 5, 6, array);
                        break;
                    case 20: // +Hmmss
                        parseVariableWidthDigits(text, 1, 6, array);
                        break;
                }
                if (array[0] > 0) {
                    if (array[1] > 23 || array[2] > 59 || array[3] > 59) {
                        throw new DateTimeException("Value out of range: Hour[0-23], Minute[0-59], Second[0-59]");
                    }
                    long offsetSecs = negative * (array[1] * 3600L + array[2] * 60L + array[3]);
                    return context.setParsedField(OFFSET_SECONDS, offsetSecs, position, array[0]);
                }
            }
            // handle special case of empty no offset text
            if (noOffsetLen == 0) {
                return context.setParsedField(OFFSET_SECONDS, 0, position, position);
            }
            return ~position;
        }

        private void parseHour(CharSequence parseText, boolean paddedHour, int[] array) {
            if (paddedHour) {
                // parse two digits
                if (!parseDigits(parseText, false, 1, array)) {
                    array[0] = ~array[0];
                }
            } else {
                // parse one or two digits
                parseVariableWidthDigits(parseText, 1, 2, array);
            }
        }

        private void parseMinute(CharSequence parseText, boolean isColon, boolean mandatory, int[] array) {
            if (!parseDigits(parseText, isColon, 2, array)) {
                if (mandatory) {
                    array[0] = ~array[0];
                }
            }
        }

        private void parseSecond(CharSequence parseText, boolean isColon, boolean mandatory, int[] array) {
            if (!parseDigits(parseText, isColon, 3, array)) {
                if (mandatory) {
                    array[0] = ~array[0];
                }
            }
        }

        private void parseOptionalMinuteSecond(CharSequence parseText, boolean isColon, int[] array) {
            if (parseDigits(parseText, isColon, 2, array)) {
                parseDigits(parseText, isColon, 3, array);
            }
        }

        private boolean parseDigits(CharSequence parseText, boolean isColon, int arrayIndex, int[] array) {
            int pos = array[0];
            if (pos < 0) {
                return true;
            }
            if (isColon && arrayIndex != 1) { //  ':' will precede only in case of minute/second
                if (pos + 1 > parseText.length() || parseText.charAt(pos) != ':') {
                    return false;
                }
                pos++;
            }
            if (pos + 2 > parseText.length()) {
                return false;
            }
            char ch1 = parseText.charAt(pos++);
            char ch2 = parseText.charAt(pos++);
            if (ch1 < '0' || ch1 > '9' || ch2 < '0' || ch2 > '9') {
                return false;
            }
            int value = (ch1 - 48) * 10 + (ch2 - 48);
            if (value < 0 || value > 59) {
                return false;
            }
            array[arrayIndex] = value;
            array[0] = pos;
            return true;
        }

        private void parseVariableWidthDigits(CharSequence parseText, int minDigits, int maxDigits, int[] array) {
            // scan the text to find the available number of digits up to maxDigits
            // so long as the number available is minDigits or more, the input is valid
            // then parse the number of available digits
            int pos = array[0];
            int available = 0;
            char[] chars = new char[maxDigits];
            for (int i = 0; i < maxDigits; i++) {
                if (pos + 1  > parseText.length()) {
                    break;
                }
                char ch = parseText.charAt(pos++);
                if (ch < '0' || ch > '9') {
                    pos--;
                    break;
                }
                chars[i] = ch;
                available++;
            }
            if (available < minDigits) {
                array[0] = ~array[0];
                return;
            }
            switch (available) {
                case 1:
                    array[1] = (chars[0] - 48);
                    break;
                case 2:
                    array[1] = ((chars[0] - 48) * 10 + (chars[1] - 48));
                    break;
                case 3:
                    array[1] = (chars[0] - 48);
                    array[2] = ((chars[1] - 48) * 10 + (chars[2] - 48));
                    break;
                case 4:
                    array[1] = ((chars[0] - 48) * 10 + (chars[1] - 48));
                    array[2] = ((chars[2] - 48) * 10 + (chars[3] - 48));
                    break;
                case 5:
                    array[1] = (chars[0] - 48);
                    array[2] = ((chars[1] - 48) * 10 + (chars[2] - 48));
                    array[3] = ((chars[3] - 48) * 10 + (chars[4] - 48));
                    break;
                case 6:
                    array[1] = ((chars[0] - 48) * 10 + (chars[1] - 48));
                    array[2] = ((chars[2] - 48) * 10 + (chars[3] - 48));
                    array[3] = ((chars[4] - 48) * 10 + (chars[5] - 48));
                    break;
            }
            array[0] = pos;
        }

        @Override
        public String toString() {
            String converted = noOffsetText.replace("'", "''");
            return "Offset(" + PATTERNS[type] + ",'" + converted + "')";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses an offset ID.
     */
    static final class LocalizedOffsetIdPrinterParser implements DateTimePrinterParser {
        private final TextStyle style;

        /**
         * Constructor.
         *
         * @param style  the style, not null
         */
        LocalizedOffsetIdPrinterParser(TextStyle style) {
            this.style = style;
        }

        private static StringBuilder appendHMS(StringBuilder buf, int t) {
            return buf.append((char)(t / 10 + '0'))
                      .append((char)(t % 10 + '0'));
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long offsetSecs = context.getValue(OFFSET_SECONDS);
            if (offsetSecs == null) {
                return false;
            }
            String key = "timezone.gmtZeroFormat";
            String gmtText = DateTimeTextProvider.getLocalizedResource(key, context.getLocale());
            if (gmtText == null) {
                gmtText = "GMT";  // Default to "GMT"
            }
            buf.append(gmtText);
            int totalSecs = Math.toIntExact(offsetSecs);
            if (totalSecs != 0) {
                int absHours = Math.abs((totalSecs / 3600) % 100);  // anything larger than 99 silently dropped
                int absMinutes = Math.abs((totalSecs / 60) % 60);
                int absSeconds = Math.abs(totalSecs % 60);
                buf.append(totalSecs < 0 ? "-" : "+");
                if (style == TextStyle.FULL) {
                    appendHMS(buf, absHours);
                    buf.append(':');
                    appendHMS(buf, absMinutes);
                    if (absSeconds != 0) {
                       buf.append(':');
                       appendHMS(buf, absSeconds);
                    }
                } else {
                    if (absHours >= 10) {
                        buf.append((char)(absHours / 10 + '0'));
                    }
                    buf.append((char)(absHours % 10 + '0'));
                    if (absMinutes != 0 || absSeconds != 0) {
                        buf.append(':');
                        appendHMS(buf, absMinutes);
                        if (absSeconds != 0) {
                            buf.append(':');
                            appendHMS(buf, absSeconds);
                        }
                    }
                }
            }
            return true;
        }

        int getDigit(CharSequence text, int position) {
            char c = text.charAt(position);
            if (c < '0' || c > '9') {
                return -1;
            }
            return c - '0';
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int pos = position;
            int end = text.length();
            String key = "timezone.gmtZeroFormat";
            String gmtText = DateTimeTextProvider.getLocalizedResource(key, context.getLocale());
            if (gmtText == null) {
                gmtText = "GMT";  // Default to "GMT"
            }
            if (!context.subSequenceEquals(text, pos, gmtText, 0, gmtText.length())) {
                    return ~position;
                }
            pos += gmtText.length();
            // parse normal plus/minus offset
            int negative = 0;
            if (pos == end) {
                return context.setParsedField(OFFSET_SECONDS, 0, position, pos);
            }
            char sign = text.charAt(pos);  // IOOBE if invalid position
            if (sign == '+') {
                negative = 1;
            } else if (sign == '-') {
                negative = -1;
            } else {
                return context.setParsedField(OFFSET_SECONDS, 0, position, pos);
            }
            pos++;
            int h = 0;
            int m = 0;
            int s = 0;
            if (style == TextStyle.FULL) {
                int h1 = getDigit(text, pos++);
                int h2 = getDigit(text, pos++);
                if (h1 < 0 || h2 < 0 || text.charAt(pos++) != ':') {
                    return ~position;
                }
                h = h1 * 10 + h2;
                int m1 = getDigit(text, pos++);
                int m2 = getDigit(text, pos++);
                if (m1 < 0 || m2 < 0) {
                    return ~position;
                }
                m = m1 * 10 + m2;
                if (pos + 2 < end && text.charAt(pos) == ':') {
                    int s1 = getDigit(text, pos + 1);
                    int s2 = getDigit(text, pos + 2);
                    if (s1 >= 0 && s2 >= 0) {
                        s = s1 * 10 + s2;
                        pos += 3;
                    }
                }
            } else {
                h = getDigit(text, pos++);
                if (h < 0) {
                    return ~position;
                }
                if (pos < end) {
                    int h2 = getDigit(text, pos);
                    if (h2 >=0) {
                        h = h * 10 + h2;
                        pos++;
                    }
                    if (pos + 2 < end && text.charAt(pos) == ':') {
                        if (pos + 2 < end && text.charAt(pos) == ':') {
                            int m1 = getDigit(text, pos + 1);
                            int m2 = getDigit(text, pos + 2);
                            if (m1 >= 0 && m2 >= 0) {
                                m = m1 * 10 + m2;
                                pos += 3;
                                if (pos + 2 < end && text.charAt(pos) == ':') {
                                    int s1 = getDigit(text, pos + 1);
                                    int s2 = getDigit(text, pos + 2);
                                    if (s1 >= 0 && s2 >= 0) {
                                        s = s1 * 10 + s2;
                                        pos += 3;
                                   }
                                }
                            }
                        }
                    }
                }
            }
            long offsetSecs = negative * (h * 3600L + m * 60L + s);
            return context.setParsedField(OFFSET_SECONDS, offsetSecs, position, pos);
        }

        @Override
        public String toString() {
            return "LocalizedOffset(" + style + ")";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses a zone ID.
     */
    static final class ZoneTextPrinterParser extends ZoneIdPrinterParser {

        /** The text style to output. */
        private final TextStyle textStyle;

        /** The preferred zoneid map */
        private Set<String> preferredZones;

        /**  Display in generic time-zone format. True in case of pattern letter 'v' */
        private final boolean isGeneric;
        ZoneTextPrinterParser(TextStyle textStyle, Set<ZoneId> preferredZones, boolean isGeneric) {
            super(TemporalQueries.zone(), "ZoneText(" + textStyle + ")");
            this.textStyle = Objects.requireNonNull(textStyle, "textStyle");
            this.isGeneric = isGeneric;
            if (preferredZones != null && preferredZones.size() != 0) {
                this.preferredZones = new HashSet<>();
                for (ZoneId id : preferredZones) {
                    this.preferredZones.add(id.getId());
                }
            }
        }

        private static final int STD = 0;
        private static final int DST = 1;
        private static final int GENERIC = 2;
        private static final Map<String, SoftReference<Map<Locale, String[]>>> cache =
            new ConcurrentHashMap<>();

        private String getDisplayName(String id, int type, Locale locale) {
            if (textStyle == TextStyle.NARROW) {
                return null;
            }
            String[] names;
            SoftReference<Map<Locale, String[]>> ref = cache.get(id);
            Map<Locale, String[]> perLocale = null;
            if (ref == null || (perLocale = ref.get()) == null ||
                (names = perLocale.get(locale)) == null) {
                names = TimeZoneNameUtility.retrieveDisplayNames(id, locale);
                if (names == null) {
                    return null;
                }
                names = Arrays.copyOfRange(names, 0, 7);
                names[5] =
                    TimeZoneNameUtility.retrieveGenericDisplayName(id, TimeZone.LONG, locale);
                if (names[5] == null) {
                    names[5] = names[0]; // use the id
                }
                names[6] =
                    TimeZoneNameUtility.retrieveGenericDisplayName(id, TimeZone.SHORT, locale);
                if (names[6] == null) {
                    names[6] = names[0];
                }
                if (perLocale == null) {
                    perLocale = new ConcurrentHashMap<>();
                }
                perLocale.put(locale, names);
                cache.put(id, new SoftReference<>(perLocale));
            }
            return switch (type) {
                case STD -> names[textStyle.zoneNameStyleIndex() + 1];
                case DST -> names[textStyle.zoneNameStyleIndex() + 3];
                default  -> names[textStyle.zoneNameStyleIndex() + 5];
            };
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            ZoneId zone = context.getValue(TemporalQueries.zoneId());
            if (zone == null) {
                return false;
            }
            String zname = zone.getId();
            if (!(zone instanceof ZoneOffset)) {
                TemporalAccessor dt = context.getTemporal();
                int type = GENERIC;
                if (!isGeneric) {
                    if (dt.isSupported(ChronoField.INSTANT_SECONDS)) {
                        type = zone.getRules().isDaylightSavings(Instant.from(dt)) ? DST : STD;
                    } else if (dt.isSupported(ChronoField.EPOCH_DAY) &&
                               dt.isSupported(ChronoField.NANO_OF_DAY)) {
                        LocalDate date = LocalDate.ofEpochDay(dt.getLong(ChronoField.EPOCH_DAY));
                        LocalTime time = LocalTime.ofNanoOfDay(dt.getLong(ChronoField.NANO_OF_DAY));
                        LocalDateTime ldt = date.atTime(time);
                        if (zone.getRules().getTransition(ldt) == null) {
                            type = zone.getRules().isDaylightSavings(ldt.atZone(zone).toInstant()) ? DST : STD;
                        }
                    }
                }
                String name = getDisplayName(zname, type, context.getLocale());
                if (name != null) {
                    zname = name;
                }
            }
            buf.append(zname);
            return true;
        }

        // cache per instance for now
        private final Map<Locale, Entry<Integer, SoftReference<PrefixTree>>>
            cachedTree = new HashMap<>();
        private final Map<Locale, Entry<Integer, SoftReference<PrefixTree>>>
            cachedTreeCI = new HashMap<>();

        @Override
        protected PrefixTree getTree(DateTimeParseContext context) {
            if (textStyle == TextStyle.NARROW) {
                return super.getTree(context);
            }
            Locale locale = context.getLocale();
            boolean isCaseSensitive = context.isCaseSensitive();
            Set<String> regionIds = new HashSet<>(ZoneRulesProvider.getAvailableZoneIds());
            Set<String> nonRegionIds = new HashSet<>(64);
            int regionIdsSize = regionIds.size();

            Map<Locale, Entry<Integer, SoftReference<PrefixTree>>> cached =
                isCaseSensitive ? cachedTree : cachedTreeCI;

            Entry<Integer, SoftReference<PrefixTree>> entry = null;
            PrefixTree tree = null;
            String[][] zoneStrings = null;
            if ((entry = cached.get(locale)) == null ||
                (entry.getKey() != regionIdsSize ||
                (tree = entry.getValue().get()) == null)) {
                tree = PrefixTree.newTree(context);
                zoneStrings = TimeZoneNameUtility.getZoneStrings(locale);
                for (String[] names : zoneStrings) {
                    String zid = names[0];
                    if (!regionIds.remove(zid)) {
                        nonRegionIds.add(zid);
                        continue;
                    }
                    tree.add(zid, zid);    // don't convert zid -> metazone
                    zid = ZoneName.toZid(zid, locale);
                    int i = textStyle == TextStyle.FULL ? 1 : 2;
                    for (; i < names.length; i += 2) {
                        tree.add(names[i], zid);
                    }
                }

                // add names for provider's custom ids
                final PrefixTree t = tree;
                regionIds.stream()
                    .filter(zid -> !zid.startsWith("Etc") && !zid.startsWith("GMT"))
                    .forEach(cid -> {
                        String[] cidNames = TimeZoneNameUtility.retrieveDisplayNames(cid, locale);
                        int i = textStyle == TextStyle.FULL ? 1 : 2;
                        for (; i < cidNames.length; i += 2) {
                            if (cidNames[i] != null && !cidNames[i].isEmpty()) {
                                t.add(cidNames[i], cid);
                            }
                        }
                    });

                // if we have a set of preferred zones, need a copy and
                // add the preferred zones again to overwrite
                if (preferredZones != null) {
                    for (String[] names : zoneStrings) {
                        String zid = names[0];
                        if (!preferredZones.contains(zid) || nonRegionIds.contains(zid)) {
                            continue;
                        }
                        int i = textStyle == TextStyle.FULL ? 1 : 2;
                        for (; i < names.length; i += 2) {
                            tree.add(names[i], zid);
                       }
                    }
                }
                cached.put(locale, new SimpleImmutableEntry<>(regionIdsSize, new SoftReference<>(tree)));
            }
            return tree;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses a zone ID.
     */
    static class ZoneIdPrinterParser implements DateTimePrinterParser {
        private final TemporalQuery<ZoneId> query;
        private final String description;

        ZoneIdPrinterParser(TemporalQuery<ZoneId> query, String description) {
            this.query = query;
            this.description = description;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            ZoneId zone = context.getValue(query);
            if (zone == null) {
                return false;
            }
            buf.append(zone.getId());
            return true;
        }

        /**
         * The cached tree to speed up parsing.
         */
        private static volatile Entry<Integer, PrefixTree> cachedPrefixTree;
        private static volatile Entry<Integer, PrefixTree> cachedPrefixTreeCI;

        protected PrefixTree getTree(DateTimeParseContext context) {
            // prepare parse tree
            Set<String> regionIds = ZoneRulesProvider.getAvailableZoneIds();
            final int regionIdsSize = regionIds.size();
            Entry<Integer, PrefixTree> cached = context.isCaseSensitive()
                                                ? cachedPrefixTree : cachedPrefixTreeCI;
            if (cached == null || cached.getKey() != regionIdsSize) {
                synchronized (this) {
                    cached = context.isCaseSensitive() ? cachedPrefixTree : cachedPrefixTreeCI;
                    if (cached == null || cached.getKey() != regionIdsSize) {
                        cached = new SimpleImmutableEntry<>(regionIdsSize, PrefixTree.newTree(regionIds, context));
                        if (context.isCaseSensitive()) {
                            cachedPrefixTree = cached;
                        } else {
                            cachedPrefixTreeCI = cached;
                        }
                    }
                }
            }
            return cached.getValue();
        }

        /**
         * This implementation looks for the longest matching string.
         * For example, parsing Etc/GMT-2 will return Etc/GMC-2 rather than just
         * Etc/GMC although both are valid.
         */
        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            int length = text.length();
            if (position > length) {
                throw new IndexOutOfBoundsException();
            }
            if (position == length) {
                return ~position;
            }

            // handle fixed time-zone IDs
            char nextChar = text.charAt(position);
            if (nextChar == '+' || nextChar == '-') {
                return parseOffsetBased(context, text, position, position, OffsetIdPrinterParser.INSTANCE_ID_Z);
            } else if (length >= position + 2) {
                char nextNextChar = text.charAt(position + 1);
                if (context.charEquals(nextChar, 'U') && context.charEquals(nextNextChar, 'T')) {
                    if (length >= position + 3 && context.charEquals(text.charAt(position + 2), 'C')) {
                        // There are localized zone texts that start with "UTC", e.g.
                        // "UTC\u221210:00" (MINUS SIGN instead of HYPHEN-MINUS) in French.
                        // Exclude those ZoneText cases.
                        if (!(this instanceof ZoneTextPrinterParser)) {
                            return parseOffsetBased(context, text, position, position + 3, OffsetIdPrinterParser.INSTANCE_ID_ZERO);
                        }
                    } else {
                        return parseOffsetBased(context, text, position, position + 2, OffsetIdPrinterParser.INSTANCE_ID_ZERO);
                    }
                } else if (context.charEquals(nextChar, 'G') && length >= position + 3 &&
                        context.charEquals(nextNextChar, 'M') && context.charEquals(text.charAt(position + 2), 'T')) {
                    if (length >= position + 4 && context.charEquals(text.charAt(position + 3), '0')) {
                        context.setParsed(ZoneId.of("GMT0"));
                        return position + 4;
                    }
                    return parseOffsetBased(context, text, position, position + 3, OffsetIdPrinterParser.INSTANCE_ID_ZERO);
                }
            }

            // parse
            PrefixTree tree = getTree(context);
            ParsePosition ppos = new ParsePosition(position);
            String parsedZoneId = tree.match(text, ppos);
            if (parsedZoneId == null) {
                if (context.charEquals(nextChar, 'Z')) {
                    context.setParsed(ZoneOffset.UTC);
                    return position + 1;
                }
                return ~position;
            }
            context.setParsed(ZoneId.of(parsedZoneId));
            return ppos.getIndex();
        }

        /**
         * Parse an offset following a prefix and set the ZoneId if it is valid.
         * To matching the parsing of ZoneId.of the values are not normalized
         * to ZoneOffsets.
         *
         * @param context the parse context
         * @param text the input text
         * @param prefixPos start of the prefix
         * @param position start of text after the prefix
         * @param parser parser for the value after the prefix
         * @return the position after the parse
         */
        private int parseOffsetBased(DateTimeParseContext context, CharSequence text, int prefixPos, int position, OffsetIdPrinterParser parser) {
            String prefix = text.subSequence(prefixPos, position).toString().toUpperCase();
            if (position >= text.length()) {
                context.setParsed(ZoneId.of(prefix));
                return position;
            }

            // '0' or 'Z' after prefix is not part of a valid ZoneId; use bare prefix
            if (text.charAt(position) == '0' ||
                context.charEquals(text.charAt(position), 'Z')) {
                context.setParsed(ZoneId.of(prefix));
                return position;
            }

            DateTimeParseContext newContext = context.copy();
            int endPos = parser.parse(newContext, text, position);
            try {
                if (endPos < 0) {
                    if (parser == OffsetIdPrinterParser.INSTANCE_ID_Z) {
                        return ~prefixPos;
                    }
                    context.setParsed(ZoneId.of(prefix));
                    return position;
                }
                int offset = (int) newContext.getParsed(OFFSET_SECONDS).longValue();
                ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(offset);
                context.setParsed(ZoneId.ofOffset(prefix, zoneOffset));
                return endPos;
            } catch (DateTimeException dte) {
                return ~prefixPos;
            }
        }

        @Override
        public String toString() {
            return description;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * A String based prefix tree for parsing time-zone names.
     */
    static class PrefixTree {
        protected String key;
        protected String value;
        protected char c0;    // performance optimization to avoid the
                              // boundary check cost of key.charat(0)
        protected PrefixTree child;
        protected PrefixTree sibling;

        private PrefixTree(String k, String v, PrefixTree child) {
            this.key = k;
            this.value = v;
            this.child = child;
            if (k.isEmpty()) {
                c0 = 0xffff;
            } else {
                c0 = key.charAt(0);
            }
        }

        /**
         * Creates a new prefix parsing tree based on parse context.
         *
         * @param context  the parse context
         * @return the tree, not null
         */
        public static PrefixTree newTree(DateTimeParseContext context) {
            //if (!context.isStrict()) {
            //    return new LENIENT("", null, null);
            //}
            if (context.isCaseSensitive()) {
                return new PrefixTree("", null, null);
            }
            return new CI("", null, null);
        }

        /**
         * Creates a new prefix parsing tree.
         *
         * @param keys  a set of strings to build the prefix parsing tree, not null
         * @param context  the parse context
         * @return the tree, not null
         */
        public static  PrefixTree newTree(Set<String> keys, DateTimeParseContext context) {
            PrefixTree tree = newTree(context);
            for (String k : keys) {
                tree.add0(k, k);
            }
            return tree;
        }

        /**
         * Clone a copy of this tree
         */
        public PrefixTree copyTree() {
            PrefixTree copy = new PrefixTree(key, value, null);
            if (child != null) {
                copy.child = child.copyTree();
            }
            if (sibling != null) {
                copy.sibling = sibling.copyTree();
            }
            return copy;
        }


        /**
         * Adds a pair of {key, value} into the prefix tree.
         *
         * @param k  the key, not null
         * @param v  the value, not null
         * @return  true if the pair is added successfully
         */
        public boolean add(String k, String v) {
            return add0(k, v);
        }

        private boolean add0(String k, String v) {
            k = toKey(k);
            int prefixLen = prefixLength(k);
            if (prefixLen == key.length()) {
                if (prefixLen < k.length()) {  // down the tree
                    String subKey = k.substring(prefixLen);
                    PrefixTree c = child;
                    while (c != null) {
                        if (isEqual(c.c0, subKey.charAt(0))) {
                            return c.add0(subKey, v);
                        }
                        c = c.sibling;
                    }
                    // add the node as the child of the current node
                    c = newNode(subKey, v, null);
                    c.sibling = child;
                    child = c;
                    return true;
                }
                // have an existing <key, value> already, overwrite it
                // if (value != null) {
                //    return false;
                //}
                value = v;
                return true;
            }
            // split the existing node
            PrefixTree n1 = newNode(key.substring(prefixLen), value, child);
            key = k.substring(0, prefixLen);
            child = n1;
            if (prefixLen < k.length()) {
                PrefixTree n2 = newNode(k.substring(prefixLen), v, null);
                child.sibling = n2;
                value = null;
            } else {
                value = v;
            }
            return true;
        }

        /**
         * Match text with the prefix tree.
         *
         * @param text  the input text to parse, not null
         * @param off  the offset position to start parsing at
         * @param end  the end position to stop parsing
         * @return the resulting string, or null if no match found.
         */
        public String match(CharSequence text, int off, int end) {
            if (!prefixOf(text, off, end)){
                return null;
            }
            if (child != null && (off += key.length()) != end) {
                PrefixTree c = child;
                do {
                    if (isEqual(c.c0, text.charAt(off))) {
                        String found = c.match(text, off, end);
                        if (found != null) {
                            return found;
                        }
                        return value;
                    }
                    c = c.sibling;
                } while (c != null);
            }
            return value;
        }

        /**
         * Match text with the prefix tree.
         *
         * @param text  the input text to parse, not null
         * @param pos  the position to start parsing at, from 0 to the text
         *  length. Upon return, position will be updated to the new parse
         *  position, or unchanged, if no match found.
         * @return the resulting string, or null if no match found.
         */
        public String match(CharSequence text, ParsePosition pos) {
            int off = pos.getIndex();
            int end = text.length();
            if (!prefixOf(text, off, end)){
                return null;
            }
            off += key.length();
            if (child != null && off != end) {
                PrefixTree c = child;
                do {
                    if (isEqual(c.c0, text.charAt(off))) {
                        pos.setIndex(off);
                        String found = c.match(text, pos);
                        if (found != null) {
                            return found;
                        }
                        break;
                    }
                    c = c.sibling;
                } while (c != null);
            }
            pos.setIndex(off);
            return value;
        }

        protected String toKey(String k) {
            return k;
        }

        protected PrefixTree newNode(String k, String v, PrefixTree child) {
            return new PrefixTree(k, v, child);
        }

        protected boolean isEqual(char c1, char c2) {
            return c1 == c2;
        }

        protected boolean prefixOf(CharSequence text, int off, int end) {
            if (text instanceof String) {
                return ((String)text).startsWith(key, off);
            }
            int len = key.length();
            if (len > end - off) {
                return false;
            }
            int off0 = 0;
            while (len-- > 0) {
                if (!isEqual(key.charAt(off0++), text.charAt(off++))) {
                    return false;
                }
            }
            return true;
        }

        private int prefixLength(String k) {
            int off = 0;
            while (off < k.length() && off < key.length()) {
                if (!isEqual(k.charAt(off), key.charAt(off))) {
                    return off;
                }
                off++;
            }
            return off;
        }

        /**
         * Case Insensitive prefix tree.
         */
        private static class CI extends PrefixTree {

            private CI(String k, String v, PrefixTree child) {
                super(k, v, child);
            }

            @Override
            protected CI newNode(String k, String v, PrefixTree child) {
                return new CI(k, v, child);
            }

            @Override
            protected boolean isEqual(char c1, char c2) {
                return DateTimeParseContext.charEqualsIgnoreCase(c1, c2);
            }

            @Override
            protected boolean prefixOf(CharSequence text, int off, int end) {
                int len = key.length();
                if (len > end - off) {
                    return false;
                }
                int off0 = 0;
                while (len-- > 0) {
                    if (!isEqual(key.charAt(off0++), text.charAt(off++))) {
                        return false;
                    }
                }
                return true;
            }
        }

        /**
         * Lenient prefix tree. Case insensitive and ignores characters
         * like space, underscore and slash.
         */
        private static class LENIENT extends CI {

            private LENIENT(String k, String v, PrefixTree child) {
                super(k, v, child);
            }

            @Override
            protected CI newNode(String k, String v, PrefixTree child) {
                return new LENIENT(k, v, child);
            }

            private boolean isLenientChar(char c) {
                return c == ' ' || c == '_' || c == '/';
            }

            protected String toKey(String k) {
                for (int i = 0; i < k.length(); i++) {
                    if (isLenientChar(k.charAt(i))) {
                        StringBuilder sb = new StringBuilder(k.length());
                        sb.append(k, 0, i);
                        i++;
                        while (i < k.length()) {
                            if (!isLenientChar(k.charAt(i))) {
                                sb.append(k.charAt(i));
                            }
                            i++;
                        }
                        return sb.toString();
                    }
                }
                return k;
            }

            @Override
            public String match(CharSequence text, ParsePosition pos) {
                int off = pos.getIndex();
                int end = text.length();
                int len = key.length();
                int koff = 0;
                while (koff < len && off < end) {
                    if (isLenientChar(text.charAt(off))) {
                        off++;
                        continue;
                    }
                    if (!isEqual(key.charAt(koff++), text.charAt(off++))) {
                        return null;
                    }
                }
                if (koff != len) {
                    return null;
                }
                if (child != null && off != end) {
                    int off0 = off;
                    while (off0 < end && isLenientChar(text.charAt(off0))) {
                        off0++;
                    }
                    if (off0 < end) {
                        PrefixTree c = child;
                        do {
                            if (isEqual(c.c0, text.charAt(off0))) {
                                pos.setIndex(off0);
                                String found = c.match(text, pos);
                                if (found != null) {
                                    return found;
                                }
                                break;
                            }
                            c = c.sibling;
                        } while (c != null);
                    }
                }
                pos.setIndex(off);
                return value;
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses a chronology.
     */
    static final class ChronoPrinterParser implements DateTimePrinterParser {
        /** The text style to output, null means the ID. */
        private final TextStyle textStyle;

        ChronoPrinterParser(TextStyle textStyle) {
            // validated by caller
            this.textStyle = textStyle;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Chronology chrono = context.getValue(TemporalQueries.chronology());
            if (chrono == null) {
                return false;
            }
            if (textStyle == null) {
                buf.append(chrono.getId());
            } else {
                buf.append(getChronologyName(chrono, context.getLocale()));
            }
            return true;
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            // simple looping parser to find the chronology
            if (position < 0 || position > text.length()) {
                throw new IndexOutOfBoundsException();
            }
            Set<Chronology> chronos = Chronology.getAvailableChronologies();
            Chronology bestMatch = null;
            int matchLen = -1;
            for (Chronology chrono : chronos) {
                String name;
                if (textStyle == null) {
                    name = chrono.getId();
                } else {
                    name = getChronologyName(chrono, context.getLocale());
                }
                int nameLen = name.length();
                if (nameLen > matchLen && context.subSequenceEquals(text, position, name, 0, nameLen)) {
                    bestMatch = chrono;
                    matchLen = nameLen;
                }
            }
            if (bestMatch == null) {
                return ~position;
            }
            context.setParsed(bestMatch);
            return position + matchLen;
        }

        /**
         * Returns the chronology name of the given chrono in the given locale
         * if available, or the chronology Id otherwise. The regular ResourceBundle
         * search path is used for looking up the chronology name.
         *
         * @param chrono  the chronology, not null
         * @param locale  the locale, not null
         * @return the chronology name of chrono in locale, or the id if no name is available
         * @throws NullPointerException if chrono or locale is null
         */
        private String getChronologyName(Chronology chrono, Locale locale) {
            String key = "calendarname." + chrono.getCalendarType();
            String name = DateTimeTextProvider.getLocalizedResource(key, locale);
            return Objects.requireNonNullElseGet(name, () -> chrono.getId());
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses a localized pattern.
     */
    static final class LocalizedPrinterParser implements DateTimePrinterParser {
        /** Cache of formatters. */
        private static final ConcurrentMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>(16, 0.75f, 2);

        private final FormatStyle dateStyle;
        private final FormatStyle timeStyle;

        /**
         * Constructor.
         *
         * @param dateStyle  the date style to use, may be null
         * @param timeStyle  the time style to use, may be null
         */
        LocalizedPrinterParser(FormatStyle dateStyle, FormatStyle timeStyle) {
            // validated by caller
            this.dateStyle = dateStyle;
            this.timeStyle = timeStyle;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Chronology chrono = Chronology.from(context.getTemporal());
            return formatter(context.getLocale(), chrono).toPrinterParser(false).format(context, buf);
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            Chronology chrono = context.getEffectiveChronology();
            return formatter(context.getLocale(), chrono).toPrinterParser(false).parse(context, text, position);
        }

        /**
         * Gets the formatter to use.
         * <p>
         * The formatter will be the most appropriate to use for the date and time style in the locale.
         * For example, some locales will use the month name while others will use the number.
         *
         * @param locale  the locale to use, not null
         * @param chrono  the chronology to use, not null
         * @return the formatter, not null
         * @throws IllegalArgumentException if the formatter cannot be found
         */
        private DateTimeFormatter formatter(Locale locale, Chronology chrono) {
            String key = chrono.getId() + '|' + locale.toString() + '|' + dateStyle + timeStyle;
            DateTimeFormatter formatter = FORMATTER_CACHE.get(key);
            if (formatter == null) {
                String pattern = getLocalizedDateTimePattern(dateStyle, timeStyle, chrono, locale);
                formatter = new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter(locale);
                DateTimeFormatter old = FORMATTER_CACHE.putIfAbsent(key, formatter);
                if (old != null) {
                    formatter = old;
                }
            }
            return formatter;
        }

        @Override
        public String toString() {
            return "Localized(" + (dateStyle != null ? dateStyle : "") + "," +
                (timeStyle != null ? timeStyle : "") + ")";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Prints or parses a localized pattern from a localized field.
     * The specific formatter and parameters is not selected until
     * the field is to be printed or parsed.
     * The locale is needed to select the proper WeekFields from which
     * the field for day-of-week, week-of-month, or week-of-year is selected.
     * Hence the inherited field NumberPrinterParser.field is unused.
     */
    static final class WeekBasedFieldPrinterParser extends NumberPrinterParser {
        private char chr;
        private int count;

        /**
         * Constructor.
         *
         * @param chr the pattern format letter that added this PrinterParser.
         * @param count the repeat count of the format letter
         * @param minWidth  the minimum field width, from 1 to 19
         * @param maxWidth  the maximum field width, from minWidth to 19
         */
        WeekBasedFieldPrinterParser(char chr, int count, int minWidth, int maxWidth) {
            this(chr, count, minWidth, maxWidth, 0);
        }

        /**
         * Constructor.
         *
         * @param chr the pattern format letter that added this PrinterParser.
         * @param count the repeat count of the format letter
         * @param minWidth  the minimum field width, from 1 to 19
         * @param maxWidth  the maximum field width, from minWidth to 19
         * @param subsequentWidth  the width of subsequent non-negative numbers, 0 or greater,
         * -1 if fixed width due to active adjacent parsing
         */
        WeekBasedFieldPrinterParser(char chr, int count, int minWidth, int maxWidth,
                int subsequentWidth) {
            super(null, minWidth, maxWidth, SignStyle.NOT_NEGATIVE, subsequentWidth);
            this.chr = chr;
            this.count = count;
        }

        /**
         * Returns a new instance with fixed width flag set.
         *
         * @return a new updated printer-parser, not null
         */
        @Override
        WeekBasedFieldPrinterParser withFixedWidth() {
            if (subsequentWidth == -1) {
                return this;
            }
            return new WeekBasedFieldPrinterParser(chr, count, minWidth, maxWidth, -1);
        }

        /**
         * Returns a new instance with an updated subsequent width.
         *
         * @param subsequentWidth  the width of subsequent non-negative numbers, 0 or greater
         * @return a new updated printer-parser, not null
         */
        @Override
        WeekBasedFieldPrinterParser withSubsequentWidth(int subsequentWidth) {
            return new WeekBasedFieldPrinterParser(chr, count, minWidth, maxWidth,
                    this.subsequentWidth + subsequentWidth);
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            return printerParser(context.getLocale()).format(context, buf);
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence text, int position) {
            return printerParser(context.getLocale()).parse(context, text, position);
        }

        /**
         * Gets the printerParser to use based on the field and the locale.
         *
         * @param locale  the locale to use, not null
         * @return the formatter, not null
         * @throws IllegalArgumentException if the formatter cannot be found
         */
        private DateTimePrinterParser printerParser(Locale locale) {
            WeekFields weekDef = WeekFields.of(locale);
            TemporalField field = null;
            switch (chr) {
                case 'Y':
                    field = weekDef.weekBasedYear();
                    if (count == 2) {
                        return new ReducedPrinterParser(field, 2, 2, 0, ReducedPrinterParser.BASE_DATE,
                                this.subsequentWidth);
                    } else {
                        return new NumberPrinterParser(field, count, 19,
                                (count < 4) ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD,
                                this.subsequentWidth);
                    }
                case 'e':
                case 'c':
                    field = weekDef.dayOfWeek();
                    break;
                case 'w':
                    field = weekDef.weekOfWeekBasedYear();
                    break;
                case 'W':
                    field = weekDef.weekOfMonth();
                    break;
                default:
                    throw new IllegalStateException("unreachable");
            }
            return new NumberPrinterParser(field, minWidth, maxWidth, SignStyle.NOT_NEGATIVE,
                    this.subsequentWidth);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(30);
            sb.append("Localized(");
            if (chr == 'Y') {
                if (count == 1) {
                    sb.append("WeekBasedYear");
                } else if (count == 2) {
                    sb.append("ReducedValue(WeekBasedYear,2,2,2000-01-01)");
                } else {
                    sb.append("WeekBasedYear,").append(count).append(",")
                            .append(19).append(",")
                            .append((count < 4) ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD);
                }
            } else {
                switch (chr) {
                    case 'c':
                    case 'e':
                        sb.append("DayOfWeek");
                        break;
                    case 'w':
                        sb.append("WeekOfWeekBasedYear");
                        break;
                    case 'W':
                        sb.append("WeekOfMonth");
                        break;
                    default:
                        break;
                }
                sb.append(",");
                sb.append(count);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Prints or parses day periods.
     */
    static final class DayPeriodPrinterParser implements DateTimePrinterParser {
        private final TextStyle textStyle;
        private static final ConcurrentMap<Locale, LocaleStore> DAYPERIOD_LOCALESTORE = new ConcurrentHashMap<>();

        /**
         * Constructor.
         *
         * @param textStyle  the text style, not null
         */
        DayPeriodPrinterParser(TextStyle textStyle) {
            // validated by caller
            this.textStyle = textStyle;
        }

        @Override
        public boolean format(DateTimePrintContext context, StringBuilder buf) {
            Long hod = context.getValue(HOUR_OF_DAY);
            if (hod == null) {
                return false;
            }
            Long moh = context.getValue(MINUTE_OF_HOUR);
            long value = Math.floorMod(hod, 24) * 60 + (moh != null ? Math.floorMod(moh, 60) : 0);
            Locale locale = context.getLocale();
            LocaleStore store = findDayPeriodStore(locale);
            final long val = value;
            final var map = DayPeriod.getDayPeriodMap(locale);
            value = map.keySet().stream()
                    .filter(k -> k.includes(val))
                    .min(DayPeriod.DPCOMPARATOR)
                    .map(map::get)
                    .orElse(val / 720); // fall back to am/pm
            String text = store.getText(value, textStyle);
            buf.append(text);
            return true;
        }

        @Override
        public int parse(DateTimeParseContext context, CharSequence parseText, int position) {
            int length = parseText.length();
            if (position < 0 || position > length) {
                throw new IndexOutOfBoundsException();
            }
            TextStyle style = (context.isStrict() ? textStyle : null);
            Iterator<Entry<String, Long>> it;
            LocaleStore store = findDayPeriodStore(context.getLocale());
            it = store.getTextIterator(style);
            if (it != null) {
                while (it.hasNext()) {
                    Entry<String, Long> entry = it.next();
                    String itText = entry.getKey();
                    if (context.subSequenceEquals(itText, 0, parseText, position, itText.length())) {
                        context.setParsedDayPeriod(DayPeriod.ofLocale(context.getLocale(), entry.getValue()));
                        return position + itText.length();
                    }
                }
            }
            return ~position;
        }

        @Override
        public String toString() {
            return "DayPeriod(" + textStyle + ")";
        }

        /**
         * Returns the day period locale store for the locale
         * @param locale locale to be examined
         * @return locale store for the locale
         */
        private static LocaleStore findDayPeriodStore(Locale locale) {
            return DAYPERIOD_LOCALESTORE.computeIfAbsent(locale, loc -> {
                Map<TextStyle, Map<Long, String>> styleMap = new HashMap<>();

                for (TextStyle textStyle : TextStyle.values()) {
                    if (textStyle.isStandalone()) {
                        // Stand-alone isn't applicable to day period.
                        continue;
                    }

                    Map<Long, String> map = new HashMap<>();
                    int calStyle = textStyle.toCalendarStyle();
                    var periodMap = DayPeriod.getDayPeriodMap(loc);
                    periodMap.forEach((key, value) -> {
                        String displayName = CalendarDataUtility.retrieveJavaTimeFieldValueName(
                                "gregory", Calendar.AM_PM, value.intValue(), calStyle, loc);
                        if (displayName != null) {
                            map.put(value, displayName);
                        } else {
                            periodMap.remove(key);
                        }
                    });
                    if (!map.isEmpty()) {
                        styleMap.put(textStyle, map);
                    }
                }
                return new LocaleStore(styleMap);
            });
        }
    }

    /**
     * DayPeriod class that represents a
     * <a href="https://www.unicode.org/reports/tr35/tr35-dates.html#dayPeriods">DayPeriod</a> defined in CLDR.
     * This is a value-based class.
     */
    static final class DayPeriod {
        /**
         *  DayPeriod cache
         */
        private static final Map<Locale, Map<DayPeriod, Long>> DAYPERIOD_CACHE = new ConcurrentHashMap<>();
        /**
         * comparator based on the duration of the day period.
         */
        private static final Comparator<DayPeriod> DPCOMPARATOR = (dp1, dp2) -> (int)(dp1.duration() - dp2.duration());
        /**
         * Pattern to parse day period rules
         */
        private static final Pattern RULE = Pattern.compile("(?<type>[a-z12]+):(?<from>\\d{2}):00(-(?<to>\\d{2}))*");
        /**
         * minute-of-day of "at" or "from" attribute
         */
        private final long from;
        /**
         * minute-of-day of "before" attribute (exclusive), or if it is
         * the same value with "from", it indicates this day period
         * designates "fixed" periods, i.e, "midnight" or "noon"
         */
        private final long to;
        /**
         * day period type index. (cf. {@link #mapToIndex})
         */
        private final long index;

        /**
         * Sole constructor
         *
         * @param from "from" in minute-of-day
         * @param to "to" in minute-of-day
         * @param index day period type index
         */
        private DayPeriod(long from, long to, long index) {
            this.from = from;
            this.to = to;
            this.index = index;
        }

        /**
         * Gets the index of this day period
         *
         * @return index
         */
        long getIndex() {
            return index;
        }

        /**
         * Returns the midpoint of this day period in minute-of-day
         * @return midpoint
         */
        long mid() {
            return (from + duration() / 2) % 1_440;
        }

        /**
         * Checks whether the passed minute-of-day is within this
         * day period or not.
         *
         * @param mod minute-of-day to check
         * @return true if {@code mod} is within this day period
         */
        boolean includes(long mod) {
            // special check for 24:00 for midnight in hour-of-day
            if (from == 0 && to == 0 && mod == 1_440) {
                return true;
            }
            return (from == mod && to == mod || // midnight/noon
                    from <= mod && mod < to || // contiguous from-to
                    from > to && (from <= mod || to > mod)); // beyond midnight
        }

        /**
         * Calculates the duration of this day period
         * @return the duration in minutes
         */
        private long duration() {
            return from > to ? 1_440 - from + to: to - from;
        }

        /**
         * Maps the day period type defined in LDML to the index to the am/pm array
         * returned from the Calendar resource bundle.
         *
         * @param type day period type defined in LDML
         * @return the array index
         */
        static long mapToIndex(String type) {
            return switch (type) {
                case "am"           -> Calendar.AM;
                case "pm"           -> Calendar.PM;
                case "midnight"     -> 2;
                case "noon"         -> 3;
                case "morning1"     -> 4;
                case "morning2"     -> 5;
                case "afternoon1"   -> 6;
                case "afternoon2"   -> 7;
                case "evening1"     -> 8;
                case "evening2"     -> 9;
                case "night1"       -> 10;
                case "night2"       -> 11;
                default -> throw new InternalError("invalid day period type");
            };
        }

        /**
         * Returns the DayPeriod to array index map for a locale.
         *
         * @param locale  the locale, not null
         * @return the DayPeriod to type index map
         */
        static Map<DayPeriod, Long> getDayPeriodMap(Locale locale) {
            return DAYPERIOD_CACHE.computeIfAbsent(locale, l -> {
                LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased()
                        .getLocaleResources(CalendarDataUtility.findRegionOverride(l));
                String dayPeriodRules = lr.getRules()[1];
                final Map<DayPeriod, Long> periodMap = new ConcurrentHashMap<>();
                Arrays.stream(dayPeriodRules.split(";"))
                    .forEach(rule -> {
                        Matcher m = RULE.matcher(rule);
                        if (m.find()) {
                            String from = m.group("from");
                            String to = m.group("to");
                            long index = DayPeriod.mapToIndex(m.group("type"));
                            if (to == null) {
                                to = from;
                            }
                            periodMap.putIfAbsent(
                                new DayPeriod(
                                    Long.parseLong(from) * 60,
                                    Long.parseLong(to) * 60,
                                        index),
                                index);
                        }
                    });

                // add am/pm
                periodMap.putIfAbsent(new DayPeriod(0, 720, 0), 0L);
                periodMap.putIfAbsent(new DayPeriod(720, 1_440, 1), 1L);
                return periodMap;
            });
        }

        /**
         * Returns the DayPeriod singleton for the locale and index.
         * @param locale desired locale
         * @param index resource bundle array index
         * @return a DayPeriod instance
         */
        static DayPeriod ofLocale(Locale locale, long index) {
            return getDayPeriodMap(locale).keySet().stream()
                .filter(dp -> dp.getIndex() == index)
                .findAny()
                .orElseThrow(() -> new DateTimeException(
                    "DayPeriod could not be determined for the locale " +
                    locale + " at type index " + index));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DayPeriod dayPeriod = (DayPeriod) o;
            return from == dayPeriod.from &&
                    to == dayPeriod.to &&
                    index == dayPeriod.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to, index);
        }

        @Override
        public String toString() {
            return "DayPeriod(%02d:%02d".formatted(from / 60, from % 60) +
                    (from == to ? ")" : "-%02d:%02d)".formatted(to / 60, to % 60));
        }
    }
}
