/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.spi.NumberFormatProvider;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleServiceProviderPool;

/**
 * {@code NumberFormat} is the abstract base class for all number
 * formats. This class provides the interface for formatting and parsing
 * numbers. {@code NumberFormat} also provides methods for determining
 * which locales have number formats, and what their names are.
 *
 * <p>
 * {@code NumberFormat} helps you to format and parse numbers for any locale.
 * Your code can be completely independent of the locale conventions for
 * decimal points, thousands-separators, or even the particular decimal
 * digits used, or whether the number format is even decimal.
 *
 * <p>
 * To format a number for the current Locale, use one of the factory
 * class methods:
 * <blockquote>
 * <pre>{@code
 * myString = NumberFormat.getInstance().format(myNumber);
 * }</pre>
 * </blockquote>
 * If you are formatting multiple numbers, it is
 * more efficient to get the format and use it multiple times so that
 * the system doesn't have to fetch the information about the local
 * language and country conventions multiple times.
 * <blockquote>
 * <pre>{@code
 * NumberFormat nf = NumberFormat.getInstance();
 * for (int i = 0; i < myNumber.length; ++i) {
 *     output.println(nf.format(myNumber[i]) + "; ");
 * }
 * }</pre>
 * </blockquote>
 * To format a number for a different Locale, specify it in the
 * call to {@code getInstance}.
 * <blockquote>
 * <pre>{@code
 * NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
 * }</pre>
 * </blockquote>
 *
 * <p>If the locale contains "nu" (numbers) and/or "rg" (region override)
 * <a href="../util/Locale.html#def_locale_extension">Unicode extensions</a>,
 * the decimal digits, and/or the country used for formatting are overridden.
 * If both "nu" and "rg" are specified, the decimal digits from the "nu"
 * extension supersedes the implicit one from the "rg" extension.
 *
 * <p>You can also use a {@code NumberFormat} to parse numbers:
 * <blockquote>
 * <pre>{@code
 * myNumber = nf.parse(myString);
 * }</pre>
 * </blockquote>
 * Use {@code getInstance} or {@code getNumberInstance} to get the
 * normal number format. Use {@code getIntegerInstance} to get an
 * integer number format. Use {@code getCurrencyInstance} to get the
 * currency number format. Use {@code getCompactNumberInstance} to get the
 * compact number format to format a number in shorter form. For example,
 * {@code 2000} can be formatted as {@code "2K"} in
 * {@link java.util.Locale#US US locale}. Use {@code getPercentInstance}
 * to get a format for displaying percentages. With this format, a fraction
 * like 0.53 is displayed as 53%.
 *
 * <p>
 * You can also control the display of numbers with such methods as
 * {@code setMinimumFractionDigits}.
 * If you want even more control over the format or parsing,
 * or want to give your users more control,
 * you can try casting the {@code NumberFormat} you get from the factory methods
 * to a {@code DecimalFormat} or {@code CompactNumberFormat} depending on
 * the factory method used. This will work for the vast majority of locales;
 * just remember to put it in a {@code try} block in case you encounter
 * an unusual one.
 *
 * <p>
 * NumberFormat and DecimalFormat are designed such that some controls
 * work for formatting and others work for parsing.  The following is
 * the detailed description for each these control methods,
 * <p>
 * setParseIntegerOnly : only affects parsing, e.g.
 * if true,  "3456.78" &rarr; 3456 (and leaves the parse position just after index 6)
 * if false, "3456.78" &rarr; 3456.78 (and leaves the parse position just after index 8)
 * This is independent of formatting.  If you want to not show a decimal point
 * where there might be no digits after the decimal point, use
 * setDecimalSeparatorAlwaysShown.
 * <p>
 * setDecimalSeparatorAlwaysShown : only affects formatting, and only where
 * there might be no digits after the decimal point, such as with a pattern
 * like "#,##0.##", e.g.,
 * if true,  3456.00 &rarr; "3,456."
 * if false, 3456.00 &rarr; "3456"
 * This is independent of parsing.  If you want parsing to stop at the decimal
 * point, use setParseIntegerOnly.
 *
 * <p>
 * You can also use forms of the {@code parse} and {@code format}
 * methods with {@code ParsePosition} and {@code FieldPosition} to
 * allow you to:
 * <ul>
 * <li> progressively parse through pieces of a string
 * <li> align the decimal point and other areas
 * </ul>
 * For example, you can align numbers in two ways:
 * <ol>
 * <li> If you are using a monospaced font with spacing for alignment,
 *      you can pass the {@code FieldPosition} in your format call, with
 *      {@code field} = {@code INTEGER_FIELD}. On output,
 *      {@code getEndIndex} will be set to the offset between the
 *      last character of the integer and the decimal. Add
 *      (desiredSpaceCount - getEndIndex) spaces at the front of the string.
 *
 * <li> If you are using proportional fonts,
 *      instead of padding with spaces, measure the width
 *      of the string in pixels from the start to {@code getEndIndex}.
 *      Then move the pen by
 *      (desiredPixelWidth - widthToAlignmentPoint) before drawing the text.
 *      It also works where there is no decimal, but possibly additional
 *      characters at the end, e.g., with parentheses in negative
 *      numbers: "(12)" for -12.
 * </ol>
 *
 * <h2><a id="synchronization">Synchronization</a></h2>
 *
 * <p>
 * Number formats are generally not synchronized.
 * It is recommended to create separate format instances for each thread.
 * If multiple threads access a format concurrently, it must be synchronized
 * externally.
 *
 * @implSpec The {@link #format(double, StringBuffer, FieldPosition)},
 * {@link #format(long, StringBuffer, FieldPosition)} and
 * {@link #parse(String, ParsePosition)} methods may throw
 * {@code NullPointerException}, if any of their parameter is {@code null}.
 * The subclass may provide its own implementation and specification about
 * {@code NullPointerException}.
 *
 * <p>
 * The default implementation provides rounding modes defined
 * in {@link java.math.RoundingMode} for formatting numbers. It
 * uses the {@linkplain java.math.RoundingMode#HALF_EVEN
 * round half-even algorithm}. To change the rounding mode use
 * {@link #setRoundingMode(java.math.RoundingMode) setRoundingMode}.
 * The {@code NumberFormat} returned by the static factory methods is
 * configured to round floating point numbers using half-even
 * rounding (see {@link java.math.RoundingMode#HALF_EVEN
 * RoundingMode.HALF_EVEN}) for formatting.
 *
 * @see          DecimalFormat
 * @see          ChoiceFormat
 * @see          CompactNumberFormat
 * @author       Mark Davis
 * @author       Helena Shih
 * @since 1.1
 */
public abstract class NumberFormat extends Format  {

    /**
     * Field constant used to construct a FieldPosition object. Signifies that
     * the position of the integer part of a formatted number should be returned.
     * @see java.text.FieldPosition
     */
    public static final int INTEGER_FIELD = 0;

    /**
     * Field constant used to construct a FieldPosition object. Signifies that
     * the position of the fraction part of a formatted number should be returned.
     * @see java.text.FieldPosition
     */
    public static final int FRACTION_FIELD = 1;

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected NumberFormat() {
    }

    /**
     * Formats a number and appends the resulting text to the given string
     * buffer.
     * The number can be of any subclass of {@link java.lang.Number}.
     * <p>
     * This implementation extracts the number's value using
     * {@link java.lang.Number#longValue()} for all integral type values that
     * can be converted to {@code long} without loss of information,
     * including {@code BigInteger} values with a
     * {@link java.math.BigInteger#bitLength() bit length} of less than 64,
     * and {@link java.lang.Number#doubleValue()} for all other types. It
     * then calls
     * {@link #format(long,java.lang.StringBuffer,java.text.FieldPosition)}
     * or {@link #format(double,java.lang.StringBuffer,java.text.FieldPosition)}.
     * This may result in loss of magnitude information and precision for
     * {@code BigInteger} and {@code BigDecimal} values.
     * @param number     the number to format
     * @param toAppendTo the {@code StringBuffer} to which the formatted
     *                   text is to be appended
     * @param pos        keeps track on the position of the field within the
     *                   returned string. For example, for formatting a number
     *                   {@code 1234567.89} in {@code Locale.US} locale,
     *                   if the given {@code fieldPosition} is
     *                   {@link NumberFormat#INTEGER_FIELD}, the begin index
     *                   and end index of {@code fieldPosition} will be set
     *                   to 0 and 9, respectively for the output string
     *                   {@code 1,234,567.89}.
     * @return           the value passed in as {@code toAppendTo}
     * @throws           IllegalArgumentException if {@code number} is
     *                   null or not an instance of {@code Number}.
     * @throws           NullPointerException if {@code toAppendTo} or
     *                   {@code pos} is null
     * @throws           ArithmeticException if rounding is needed with rounding
     *                   mode being set to RoundingMode.UNNECESSARY
     * @see              java.text.FieldPosition
     */
    @Override
    public StringBuffer format(Object number,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        if (number instanceof Long || number instanceof Integer ||
            number instanceof Short || number instanceof Byte ||
            number instanceof AtomicInteger || number instanceof AtomicLong ||
            (number instanceof BigInteger &&
             ((BigInteger)number).bitLength() < 64)) {
            return format(((Number)number).longValue(), toAppendTo, pos);
        } else if (number instanceof Number) {
            return format(((Number)number).doubleValue(), toAppendTo, pos);
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Number");
        }
    }

    /**
     * Parses text from a string to produce a {@code Number}.
     * <p>
     * The method attempts to parse text starting at the index given by
     * {@code pos}.
     * If parsing succeeds, then the index of {@code pos} is updated
     * to the index after the last character used (parsing does not necessarily
     * use all characters up to the end of the string), and the parsed
     * number is returned. The updated {@code pos} can be used to
     * indicate the starting point for the next call to this method.
     * If an error occurs, then the index of {@code pos} is not
     * changed, the error index of {@code pos} is set to the index of
     * the character where the error occurred, and null is returned.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on number parsing.
     *
     * @param source A {@code String}, part of which should be parsed.
     * @param pos A {@code ParsePosition} object with index and error
     *            index information as described above.
     * @return A {@code Number} parsed from the string. In case of
     *         error, returns null.
     * @throws NullPointerException if {@code source} or {@code pos} is null.
     */
    @Override
    public final Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    /**
     * Specialization of format.
     *
     * @param number the double number to format
     * @return the formatted String
     * @throws           ArithmeticException if rounding is needed with rounding
     *                   mode being set to RoundingMode.UNNECESSARY
     * @see java.text.Format#format
     */
    public final String format(double number) {
        // Use fast-path for double result if that works
        String result = fastFormat(number);
        if (result != null)
            return result;

        return format(number, new StringBuffer(),
                      DontCareFieldPosition.INSTANCE).toString();
    }

    /*
     * fastFormat() is supposed to be implemented in concrete subclasses only.
     * Default implem always returns null.
     */
    String fastFormat(double number) { return null; }

    /**
     * Specialization of format.
     *
     * @param number the long number to format
     * @return the formatted String
     * @throws           ArithmeticException if rounding is needed with rounding
     *                   mode being set to RoundingMode.UNNECESSARY
     * @see java.text.Format#format
     */
    public final String format(long number) {
        return format(number, new StringBuffer(),
                      DontCareFieldPosition.INSTANCE).toString();
    }

    /**
     * Specialization of format.
     *
     * @param number     the double number to format
     * @param toAppendTo the StringBuffer to which the formatted text is to be
     *                   appended
     * @param pos        keeps track on the position of the field within the
     *                   returned string. For example, for formatting a number
     *                   {@code 1234567.89} in {@code Locale.US} locale,
     *                   if the given {@code fieldPosition} is
     *                   {@link NumberFormat#INTEGER_FIELD}, the begin index
     *                   and end index of {@code fieldPosition} will be set
     *                   to 0 and 9, respectively for the output string
     *                   {@code 1,234,567.89}.
     * @return the formatted StringBuffer
     * @throws           ArithmeticException if rounding is needed with rounding
     *                   mode being set to RoundingMode.UNNECESSARY
     * @see java.text.Format#format
     */
    public abstract StringBuffer format(double number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);

    /**
     * Specialization of format.
     *
     * @param number     the long number to format
     * @param toAppendTo the StringBuffer to which the formatted text is to be
     *                   appended
     * @param pos        keeps track on the position of the field within the
     *                   returned string. For example, for formatting a number
     *                   {@code 123456789} in {@code Locale.US} locale,
     *                   if the given {@code fieldPosition} is
     *                   {@link NumberFormat#INTEGER_FIELD}, the begin index
     *                   and end index of {@code fieldPosition} will be set
     *                   to 0 and 11, respectively for the output string
     *                   {@code 123,456,789}.
     * @return the formatted StringBuffer
     * @throws           ArithmeticException if rounding is needed with rounding
     *                   mode being set to RoundingMode.UNNECESSARY
     * @see java.text.Format#format
     */
    public abstract StringBuffer format(long number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);

    /**
     * Returns a Long if possible (e.g., within the range [Long.MIN_VALUE,
     * Long.MAX_VALUE] and with no decimals), otherwise a Double.
     * If IntegerOnly is set, will stop at a decimal
     * point (or equivalent; e.g., for rational numbers "1 2/3", will stop
     * after the 1).
     * Does not throw an exception; if no object can be parsed, index is
     * unchanged!
     *
     * @param source the String to parse
     * @param parsePosition the parse position
     * @return the parsed value
     * @see java.text.NumberFormat#isParseIntegerOnly
     * @see java.text.Format#parseObject
     */
    public abstract Number parse(String source, ParsePosition parsePosition);

    /**
     * Parses text from the beginning of the given string to produce a number.
     * The method may not use the entire text of the given string.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on number parsing.
     *
     * @param source A {@code String} whose beginning should be parsed.
     * @return A {@code Number} parsed from the string.
     * @throws    ParseException if the beginning of the specified string
     *            cannot be parsed.
     */
    public Number parse(String source) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Number result = parse(source, parsePosition);
        if (parsePosition.index == 0) {
            throw new ParseException("Unparseable number: \"" + source + "\"",
                                     parsePosition.errorIndex);
        }
        return result;
    }

    /**
     * Returns true if this format will parse numbers as integers only.
     * For example in the English locale, with ParseIntegerOnly true, the
     * string "1234." would be parsed as the integer value 1234 and parsing
     * would stop at the "." character.  Of course, the exact format accepted
     * by the parse operation is locale dependent and determined by sub-classes
     * of NumberFormat.
     *
     * @return {@code true} if numbers should be parsed as integers only;
     *         {@code false} otherwise
     */
    public boolean isParseIntegerOnly() {
        return parseIntegerOnly;
    }

    /**
     * Sets whether or not numbers should be parsed as integers only.
     *
     * @param value {@code true} if numbers should be parsed as integers only;
     *              {@code false} otherwise
     * @see #isParseIntegerOnly
     */
    public void setParseIntegerOnly(boolean value) {
        parseIntegerOnly = value;
    }

    //============== Locale Stuff =====================

    /**
     * Returns a general-purpose number format for the current default
     * {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * This is the same as calling
     * {@link #getNumberInstance() getNumberInstance()}.
     *
     * @return the {@code NumberFormat} instance for general-purpose number
     * formatting
     */
    public static final NumberFormat getInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT), null, NUMBERSTYLE);
    }

    /**
     * Returns a general-purpose number format for the specified locale.
     * This is the same as calling
     * {@link #getNumberInstance(java.util.Locale) getNumberInstance(inLocale)}.
     *
     * @param inLocale the desired locale
     * @return the {@code NumberFormat} instance for general-purpose number
     * formatting
     */
    public static NumberFormat getInstance(Locale inLocale) {
        return getInstance(inLocale, null, NUMBERSTYLE);
    }

    /**
     * Returns a general-purpose number format for the current default
     * {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * <p>This is equivalent to calling
     * {@link #getNumberInstance(Locale)
     *     getNumberInstance(Locale.getDefault(Locale.Category.FORMAT))}.
     *
     * @return the {@code NumberFormat} instance for general-purpose number
     * formatting
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     */
    public static final NumberFormat getNumberInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT), null, NUMBERSTYLE);
    }

    /**
     * Returns a general-purpose number format for the specified locale.
     *
     * @param inLocale the desired locale
     * @return the {@code NumberFormat} instance for general-purpose number
     * formatting
     */
    public static NumberFormat getNumberInstance(Locale inLocale) {
        return getInstance(inLocale, null, NUMBERSTYLE);
    }

    /**
     * Returns an integer number format for the current default
     * {@link java.util.Locale.Category#FORMAT FORMAT} locale. The
     * returned number format is configured to round floating point numbers
     * to the nearest integer using half-even rounding (see {@link
     * java.math.RoundingMode#HALF_EVEN RoundingMode.HALF_EVEN}) for formatting,
     * and to parse only the integer part of an input string (see {@link
     * #isParseIntegerOnly isParseIntegerOnly}).
     * <p>This is equivalent to calling
     * {@link #getIntegerInstance(Locale)
     *     getIntegerInstance(Locale.getDefault(Locale.Category.FORMAT))}.
     *
     * @see #getRoundingMode()
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @return a number format for integer values
     * @since 1.4
     */
    public static final NumberFormat getIntegerInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT), null, INTEGERSTYLE);
    }

    /**
     * Returns an integer number format for the specified locale. The
     * returned number format is configured to round floating point numbers
     * to the nearest integer using half-even rounding (see {@link
     * java.math.RoundingMode#HALF_EVEN RoundingMode.HALF_EVEN}) for formatting,
     * and to parse only the integer part of an input string (see {@link
     * #isParseIntegerOnly isParseIntegerOnly}).
     *
     * @param inLocale the desired locale
     * @see #getRoundingMode()
     * @return a number format for integer values
     * @since 1.4
     */
    public static NumberFormat getIntegerInstance(Locale inLocale) {
        return getInstance(inLocale, null, INTEGERSTYLE);
    }

    /**
     * Returns a currency format for the current default
     * {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * <p>This is equivalent to calling
     * {@link #getCurrencyInstance(Locale)
     *     getCurrencyInstance(Locale.getDefault(Locale.Category.FORMAT))}.
     *
     * @return the {@code NumberFormat} instance for currency formatting
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     */
    public static final NumberFormat getCurrencyInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT), null, CURRENCYSTYLE);
    }

    /**
     * Returns a currency format for the specified locale.
     *
     * <p>If the specified locale contains the "{@code cf}" (
     * <a href="https://www.unicode.org/reports/tr35/tr35.html#UnicodeCurrencyFormatIdentifier">
     * currency format style</a>)
     * <a href="../util/Locale.html#def_locale_extension">Unicode extension</a>,
     * the returned currency format uses the style if it is available.
     * Otherwise, the style uses the default "{@code standard}" currency format.
     * For example, if the style designates "{@code account}", negative
     * currency amounts use a pair of parentheses in some locales.
     *
     * @param inLocale the desired locale
     * @return the {@code NumberFormat} instance for currency formatting
     */
    public static NumberFormat getCurrencyInstance(Locale inLocale) {
        return getInstance(inLocale, null, CURRENCYSTYLE);
    }

    /**
     * Returns a percentage format for the current default
     * {@link java.util.Locale.Category#FORMAT FORMAT} locale.
     * <p>This is equivalent to calling
     * {@link #getPercentInstance(Locale)
     *     getPercentInstance(Locale.getDefault(Locale.Category.FORMAT))}.
     *
     * @return the {@code NumberFormat} instance for percentage formatting
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     */
    public static final NumberFormat getPercentInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT), null, PERCENTSTYLE);
    }

    /**
     * Returns a percentage format for the specified locale.
     *
     * @param inLocale the desired locale
     * @return the {@code NumberFormat} instance for percentage formatting
     */
    public static NumberFormat getPercentInstance(Locale inLocale) {
        return getInstance(inLocale, null, PERCENTSTYLE);
    }

    /**
     * Returns a scientific format for the current default locale.
     */
    /*public*/ static final NumberFormat getScientificInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT), null, SCIENTIFICSTYLE);
    }

    /**
     * Returns a scientific format for the specified locale.
     *
     * @param inLocale the desired locale
     */
    /*public*/ static NumberFormat getScientificInstance(Locale inLocale) {
        return getInstance(inLocale, null, SCIENTIFICSTYLE);
    }

    /**
     * Returns a compact number format for the default
     * {@link java.util.Locale.Category#FORMAT FORMAT} locale with
     * {@link NumberFormat.Style#SHORT "SHORT"} format style.
     *
     * @return A {@code NumberFormat} instance for compact number
     *         formatting
     *
     * @see CompactNumberFormat
     * @see NumberFormat.Style
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @since 12
     */
    public static NumberFormat getCompactNumberInstance() {
        return getInstance(Locale.getDefault(
                Locale.Category.FORMAT), NumberFormat.Style.SHORT, COMPACTSTYLE);
    }

    /**
     * Returns a compact number format for the specified {@link java.util.Locale locale}
     * and {@link NumberFormat.Style formatStyle}.
     *
     * @param locale the desired locale
     * @param formatStyle the style for formatting a number
     * @return A {@code NumberFormat} instance for compact number
     *         formatting
     * @throws NullPointerException if {@code locale} or {@code formatStyle}
     *                              is {@code null}
     *
     * @see CompactNumberFormat
     * @see NumberFormat.Style
     * @see java.util.Locale
     * @since 12
     */
    public static NumberFormat getCompactNumberInstance(Locale locale,
            NumberFormat.Style formatStyle) {

        Objects.requireNonNull(locale);
        Objects.requireNonNull(formatStyle);
        return getInstance(locale, formatStyle, COMPACTSTYLE);
    }

    /**
     * Returns an array of all locales for which the
     * {@code get*Instance} methods of this class can return
     * localized instances.
     * The returned array represents the union of locales supported by the Java
     * runtime and by installed
     * {@link java.text.spi.NumberFormatProvider NumberFormatProvider} implementations.
     * It must contain at least a {@code Locale} instance equal to
     * {@link java.util.Locale#US Locale.US}.
     *
     * @return An array of locales for which localized
     *         {@code NumberFormat} instances are available.
     */
    public static Locale[] getAvailableLocales() {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(NumberFormatProvider.class);
        return pool.getAvailableLocales();
    }

    /**
     * Overrides hashCode.
     */
    @Override
    public int hashCode() {
        return maximumIntegerDigits * 37 + maxFractionDigits;
        // just enough fields for a reasonable distribution
    }

    /**
     * Overrides equals.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NumberFormat other = (NumberFormat) obj;
        return (maximumIntegerDigits == other.maximumIntegerDigits
            && minimumIntegerDigits == other.minimumIntegerDigits
            && maximumFractionDigits == other.maximumFractionDigits
            && minimumFractionDigits == other.minimumFractionDigits
            && groupingUsed == other.groupingUsed
            && parseIntegerOnly == other.parseIntegerOnly);
    }

    /**
     * Overrides Cloneable.
     */
    @Override
    public Object clone() {
        NumberFormat other = (NumberFormat) super.clone();
        return other;
    }

    /**
     * Returns true if grouping is used in this format. For example, in the
     * English locale, with grouping on, the number 1234567 might be formatted
     * as "1,234,567". The grouping separator as well as the size of each group
     * is locale dependent and is determined by sub-classes of NumberFormat.
     *
     * @return {@code true} if grouping is used;
     *         {@code false} otherwise
     * @see #setGroupingUsed
     */
    public boolean isGroupingUsed() {
        return groupingUsed;
    }

    /**
     * Set whether or not grouping will be used in this format.
     *
     * @param newValue {@code true} if grouping is used;
     *                 {@code false} otherwise
     * @see #isGroupingUsed
     */
    public void setGroupingUsed(boolean newValue) {
        groupingUsed = newValue;
    }

    /**
     * Returns the maximum number of digits allowed in the integer portion of a
     * number.
     *
     * @return the maximum number of digits
     * @see #setMaximumIntegerDigits
     */
    public int getMaximumIntegerDigits() {
        return maximumIntegerDigits;
    }

    /**
     * Sets the maximum number of digits allowed in the integer portion of a
     * number. maximumIntegerDigits must be &ge; minimumIntegerDigits.  If the
     * new value for maximumIntegerDigits is less than the current value
     * of minimumIntegerDigits, then minimumIntegerDigits will also be set to
     * the new value.
     *
     * @param newValue the maximum number of integer digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMaximumIntegerDigits
     */
    public void setMaximumIntegerDigits(int newValue) {
        maximumIntegerDigits = Math.max(0,newValue);
        if (minimumIntegerDigits > maximumIntegerDigits) {
            minimumIntegerDigits = maximumIntegerDigits;
        }
    }

    /**
     * Returns the minimum number of digits allowed in the integer portion of a
     * number.
     *
     * @return the minimum number of digits
     * @see #setMinimumIntegerDigits
     */
    public int getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    /**
     * Sets the minimum number of digits allowed in the integer portion of a
     * number. minimumIntegerDigits must be &le; maximumIntegerDigits.  If the
     * new value for minimumIntegerDigits exceeds the current value
     * of maximumIntegerDigits, then maximumIntegerDigits will also be set to
     * the new value
     *
     * @param newValue the minimum number of integer digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMinimumIntegerDigits
     */
    public void setMinimumIntegerDigits(int newValue) {
        minimumIntegerDigits = Math.max(0,newValue);
        if (minimumIntegerDigits > maximumIntegerDigits) {
            maximumIntegerDigits = minimumIntegerDigits;
        }
    }

    /**
     * Returns the maximum number of digits allowed in the fraction portion of a
     * number.
     *
     * @return the maximum number of digits.
     * @see #setMaximumFractionDigits
     */
    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    /**
     * Sets the maximum number of digits allowed in the fraction portion of a
     * number. maximumFractionDigits must be &ge; minimumFractionDigits.  If the
     * new value for maximumFractionDigits is less than the current value
     * of minimumFractionDigits, then minimumFractionDigits will also be set to
     * the new value.
     *
     * @param newValue the maximum number of fraction digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMaximumFractionDigits
     */
    public void setMaximumFractionDigits(int newValue) {
        maximumFractionDigits = Math.max(0,newValue);
        if (maximumFractionDigits < minimumFractionDigits) {
            minimumFractionDigits = maximumFractionDigits;
        }
    }

    /**
     * Returns the minimum number of digits allowed in the fraction portion of a
     * number.
     *
     * @return the minimum number of digits
     * @see #setMinimumFractionDigits
     */
    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    /**
     * Sets the minimum number of digits allowed in the fraction portion of a
     * number. minimumFractionDigits must be &le; maximumFractionDigits.  If the
     * new value for minimumFractionDigits exceeds the current value
     * of maximumFractionDigits, then maximumFractionDigits will also be set to
     * the new value
     *
     * @param newValue the minimum number of fraction digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMinimumFractionDigits
     */
    public void setMinimumFractionDigits(int newValue) {
        minimumFractionDigits = Math.max(0,newValue);
        if (maximumFractionDigits < minimumFractionDigits) {
            maximumFractionDigits = minimumFractionDigits;
        }
    }

    /**
     * Gets the currency used by this number format when formatting
     * currency values. The initial value is derived in a locale dependent
     * way. The returned value may be null if no valid
     * currency could be determined and no currency has been set using
     * {@link #setCurrency(java.util.Currency) setCurrency}.
     * <p>
     * The default implementation throws
     * {@code UnsupportedOperationException}.
     *
     * @return the currency used by this number format, or {@code null}
     * @throws    UnsupportedOperationException if the number format class
     * doesn't implement currency formatting
     * @since 1.4
     */
    public Currency getCurrency() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the currency used by this number format when formatting
     * currency values. This does not update the minimum or maximum
     * number of fraction digits used by the number format.
     * <p>
     * The default implementation throws
     * {@code UnsupportedOperationException}.
     *
     * @param currency the new currency to be used by this number format
     * @throws    UnsupportedOperationException if the number format class
     * doesn't implement currency formatting
     * @throws    NullPointerException if {@code currency} is null
     * @since 1.4
     */
    public void setCurrency(Currency currency) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the {@link java.math.RoundingMode} used in this NumberFormat.
     * The default implementation of this method in NumberFormat
     * always throws {@link java.lang.UnsupportedOperationException}.
     * Subclasses which handle different rounding modes should override
     * this method.
     *
     * @throws    UnsupportedOperationException The default implementation
     *     always throws this exception
     * @return The {@code RoundingMode} used for this NumberFormat.
     * @see #setRoundingMode(RoundingMode)
     * @since 1.6
     */
    public RoundingMode getRoundingMode() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the {@link java.math.RoundingMode} used in this NumberFormat.
     * The default implementation of this method in NumberFormat always
     * throws {@link java.lang.UnsupportedOperationException}.
     * Subclasses which handle different rounding modes should override
     * this method.
     *
     * @throws    UnsupportedOperationException The default implementation
     *     always throws this exception
     * @throws    NullPointerException if {@code roundingMode} is null
     * @param roundingMode The {@code RoundingMode} to be used
     * @see #getRoundingMode()
     * @since 1.6
     */
    public void setRoundingMode(RoundingMode roundingMode) {
        throw new UnsupportedOperationException();
    }

    // =======================privates===============================

    private static NumberFormat getInstance(Locale desiredLocale,
                                            Style formatStyle, int choice) {
        LocaleProviderAdapter adapter;
        adapter = LocaleProviderAdapter.getAdapter(NumberFormatProvider.class,
                desiredLocale);
        NumberFormat numberFormat = getInstance(adapter, desiredLocale,
                formatStyle, choice);
        if (numberFormat == null) {
            numberFormat = getInstance(LocaleProviderAdapter.forJRE(),
                    desiredLocale, formatStyle, choice);
        }
        return numberFormat;
    }

    private static NumberFormat getInstance(LocaleProviderAdapter adapter,
                                            Locale locale, Style formatStyle,
                                            int choice) {
        NumberFormatProvider provider = adapter.getNumberFormatProvider();
        return switch (choice) {
            case NUMBERSTYLE   -> provider.getNumberInstance(locale);
            case PERCENTSTYLE  -> provider.getPercentInstance(locale);
            case CURRENCYSTYLE -> provider.getCurrencyInstance(locale);
            case INTEGERSTYLE  -> provider.getIntegerInstance(locale);
            case COMPACTSTYLE  -> provider.getCompactNumberInstance(locale, formatStyle);
            default            -> null;
        };
    }

    /**
     * First, read in the default serializable data.
     *
     * Then, if {@code serialVersionOnStream} is less than 1, indicating that
     * the stream was written by JDK 1.1,
     * set the {@code int} fields such as {@code maximumIntegerDigits}
     * to be equal to the {@code byte} fields such as {@code maxIntegerDigits},
     * since the {@code int} fields were not present in JDK 1.1.
     * Finally, set serialVersionOnStream back to the maximum allowed value so that
     * default serialization will work properly if this object is streamed out again.
     *
     * <p>If {@code minimumIntegerDigits} is greater than
     * {@code maximumIntegerDigits} or {@code minimumFractionDigits}
     * is greater than {@code maximumFractionDigits}, then the stream data
     * is invalid and this method throws an {@code InvalidObjectException}.
     * In addition, if any of these values is negative, then this method throws
     * an {@code InvalidObjectException}.
     *
     * @since 1.2
     */
    @java.io.Serial
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        if (serialVersionOnStream < 1) {
            // Didn't have additional int fields, reassign to use them.
            maximumIntegerDigits = maxIntegerDigits;
            minimumIntegerDigits = minIntegerDigits;
            maximumFractionDigits = maxFractionDigits;
            minimumFractionDigits = minFractionDigits;
        }
        if (minimumIntegerDigits > maximumIntegerDigits ||
            minimumFractionDigits > maximumFractionDigits ||
            minimumIntegerDigits < 0 || minimumFractionDigits < 0) {
            throw new InvalidObjectException("Digit count range invalid");
        }
        serialVersionOnStream = currentSerialVersion;
    }

    /**
     * Write out the default serializable data, after first setting
     * the {@code byte} fields such as {@code maxIntegerDigits} to be
     * equal to the {@code int} fields such as {@code maximumIntegerDigits}
     * (or to {@code Byte.MAX_VALUE}, whichever is smaller), for compatibility
     * with the JDK 1.1 version of the stream format.
     *
     * @since 1.2
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream stream)
         throws IOException
    {
        maxIntegerDigits = (maximumIntegerDigits > Byte.MAX_VALUE) ?
                           Byte.MAX_VALUE : (byte)maximumIntegerDigits;
        minIntegerDigits = (minimumIntegerDigits > Byte.MAX_VALUE) ?
                           Byte.MAX_VALUE : (byte)minimumIntegerDigits;
        maxFractionDigits = (maximumFractionDigits > Byte.MAX_VALUE) ?
                            Byte.MAX_VALUE : (byte)maximumFractionDigits;
        minFractionDigits = (minimumFractionDigits > Byte.MAX_VALUE) ?
                            Byte.MAX_VALUE : (byte)minimumFractionDigits;
        stream.defaultWriteObject();
    }

    // Constants used by factory methods to specify a style of format.
    private static final int NUMBERSTYLE = 0;
    private static final int CURRENCYSTYLE = 1;
    private static final int PERCENTSTYLE = 2;
    private static final int SCIENTIFICSTYLE = 3;
    private static final int INTEGERSTYLE = 4;
    private static final int COMPACTSTYLE = 5;

    /**
     * True if the grouping (i.e. thousands) separator is used when
     * formatting and parsing numbers.
     *
     * @serial
     * @see #isGroupingUsed
     */
    private boolean groupingUsed = true;

    /**
     * The maximum number of digits allowed in the integer portion of a
     * number.  {@code maxIntegerDigits} must be greater than or equal to
     * {@code minIntegerDigits}.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In Java platform 2 v1.2 and higher, the new
     * {@code int} field {@code maximumIntegerDigits} is used instead.
     * When writing to a stream, {@code maxIntegerDigits} is set to
     * {@code maximumIntegerDigits} or {@code Byte.MAX_VALUE},
     * whichever is smaller.  When reading from a stream, this field is used
     * only if {@code serialVersionOnStream} is less than 1.
     *
     * @serial
     * @see #getMaximumIntegerDigits
     */
    private byte    maxIntegerDigits = 40;

    /**
     * The minimum number of digits allowed in the integer portion of a
     * number.  {@code minimumIntegerDigits} must be less than or equal to
     * {@code maximumIntegerDigits}.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In Java platform 2 v1.2 and higher, the new
     * {@code int} field {@code minimumIntegerDigits} is used instead.
     * When writing to a stream, {@code minIntegerDigits} is set to
     * {@code minimumIntegerDigits} or {@code Byte.MAX_VALUE},
     * whichever is smaller.  When reading from a stream, this field is used
     * only if {@code serialVersionOnStream} is less than 1.
     *
     * @serial
     * @see #getMinimumIntegerDigits
     */
    private byte    minIntegerDigits = 1;

    /**
     * The maximum number of digits allowed in the fractional portion of a
     * number.  {@code maximumFractionDigits} must be greater than or equal to
     * {@code minimumFractionDigits}.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In Java platform 2 v1.2 and higher, the new
     * {@code int} field {@code maximumFractionDigits} is used instead.
     * When writing to a stream, {@code maxFractionDigits} is set to
     * {@code maximumFractionDigits} or {@code Byte.MAX_VALUE},
     * whichever is smaller.  When reading from a stream, this field is used
     * only if {@code serialVersionOnStream} is less than 1.
     *
     * @serial
     * @see #getMaximumFractionDigits
     */
    private byte    maxFractionDigits = 3;    // invariant, >= minFractionDigits

    /**
     * The minimum number of digits allowed in the fractional portion of a
     * number.  {@code minimumFractionDigits} must be less than or equal to
     * {@code maximumFractionDigits}.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In Java platform 2 v1.2 and higher, the new
     * {@code int} field {@code minimumFractionDigits} is used instead.
     * When writing to a stream, {@code minFractionDigits} is set to
     * {@code minimumFractionDigits} or {@code Byte.MAX_VALUE},
     * whichever is smaller.  When reading from a stream, this field is used
     * only if {@code serialVersionOnStream} is less than 1.
     *
     * @serial
     * @see #getMinimumFractionDigits
     */
    private byte    minFractionDigits = 0;

    /**
     * True if this format will parse numbers as integers only.
     *
     * @serial
     * @see #isParseIntegerOnly
     */
    private boolean parseIntegerOnly = false;

    // new fields for 1.2.  byte is too small for integer digits.

    /**
     * The maximum number of digits allowed in the integer portion of a
     * number.  {@code maximumIntegerDigits} must be greater than or equal to
     * {@code minimumIntegerDigits}.
     *
     * @serial
     * @since 1.2
     * @see #getMaximumIntegerDigits
     */
    private int    maximumIntegerDigits = 40;

    /**
     * The minimum number of digits allowed in the integer portion of a
     * number.  {@code minimumIntegerDigits} must be less than or equal to
     * {@code maximumIntegerDigits}.
     *
     * @serial
     * @since 1.2
     * @see #getMinimumIntegerDigits
     */
    private int    minimumIntegerDigits = 1;

    /**
     * The maximum number of digits allowed in the fractional portion of a
     * number.  {@code maximumFractionDigits} must be greater than or equal to
     * {@code minimumFractionDigits}.
     *
     * @serial
     * @since 1.2
     * @see #getMaximumFractionDigits
     */
    private int    maximumFractionDigits = 3;    // invariant, >= minFractionDigits

    /**
     * The minimum number of digits allowed in the fractional portion of a
     * number.  {@code minimumFractionDigits} must be less than or equal to
     * {@code maximumFractionDigits}.
     *
     * @serial
     * @since 1.2
     * @see #getMinimumFractionDigits
     */
    private int    minimumFractionDigits = 0;

    static final int currentSerialVersion = 1;

    /**
     * Describes the version of {@code NumberFormat} present on the stream.
     * Possible values are:
     * <ul>
     * <li><b>0</b> (or uninitialized): the JDK 1.1 version of the stream format.
     *     In this version, the {@code int} fields such as
     *     {@code maximumIntegerDigits} were not present, and the {@code byte}
     *     fields such as {@code maxIntegerDigits} are used instead.
     *
     * <li><b>1</b>: the 1.2 version of the stream format.  The values of the
     *     {@code byte} fields such as {@code maxIntegerDigits} are ignored,
     *     and the {@code int} fields such as {@code maximumIntegerDigits}
     *     are used instead.
     * </ul>
     * When streaming out a {@code NumberFormat}, the most recent format
     * (corresponding to the highest allowable {@code serialVersionOnStream})
     * is always written.
     *
     * @serial
     * @since 1.2
     */
    private int serialVersionOnStream = currentSerialVersion;

    // Removed "implements Cloneable" clause.  Needs to update serialization
    // ID for backward compatibility.
    @java.io.Serial
    static final long serialVersionUID = -2308460125733713944L;


    //
    // class for AttributedCharacterIterator attributes
    //
    /**
     * Defines constants that are used as attribute keys in the
     * {@code AttributedCharacterIterator} returned
     * from {@code NumberFormat.formatToCharacterIterator} and as
     * field identifiers in {@code FieldPosition}.
     *
     * @since 1.4
     */
    public static class Field extends Format.Field {

        // Proclaim serial compatibility with 1.4 FCS
        @java.io.Serial
        private static final long serialVersionUID = 7494728892700160890L;

        // table of all instances in this class, used by readResolve
        private static final Map<String, Field> instanceMap = new HashMap<>(11);

        /**
         * Creates a Field instance with the specified
         * name.
         *
         * @param name Name of the attribute
         */
        protected Field(String name) {
            super(name);
            if (this.getClass() == NumberFormat.Field.class) {
                instanceMap.put(name, this);
            }
        }

        /**
         * Resolves instances being deserialized to the predefined constants.
         *
         * @throws InvalidObjectException if the constant could not be resolved.
         * @return resolved NumberFormat.Field constant
         */
        @Override
        @java.io.Serial
        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != NumberFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }

            Object instance = instanceMap.get(getName());
            if (instance != null) {
                return instance;
            } else {
                throw new InvalidObjectException("unknown attribute name");
            }
        }

        /**
         * Constant identifying the integer field.
         */
        public static final Field INTEGER = new Field("integer");

        /**
         * Constant identifying the fraction field.
         */
        public static final Field FRACTION = new Field("fraction");

        /**
         * Constant identifying the exponent field.
         */
        public static final Field EXPONENT = new Field("exponent");

        /**
         * Constant identifying the decimal separator field.
         */
        public static final Field DECIMAL_SEPARATOR =
                            new Field("decimal separator");

        /**
         * Constant identifying the sign field.
         */
        public static final Field SIGN = new Field("sign");

        /**
         * Constant identifying the grouping separator field.
         */
        public static final Field GROUPING_SEPARATOR =
                            new Field("grouping separator");

        /**
         * Constant identifying the exponent symbol field.
         */
        public static final Field EXPONENT_SYMBOL = new
                            Field("exponent symbol");

        /**
         * Constant identifying the percent field.
         */
        public static final Field PERCENT = new Field("percent");

        /**
         * Constant identifying the permille field.
         */
        public static final Field PERMILLE = new Field("per mille");

        /**
         * Constant identifying the currency field.
         */
        public static final Field CURRENCY = new Field("currency");

        /**
         * Constant identifying the exponent sign field.
         */
        public static final Field EXPONENT_SIGN = new Field("exponent sign");

        /**
         * Constant identifying the prefix field.
         *
         * @since 12
         */
        public static final Field PREFIX = new Field("prefix");

        /**
         * Constant identifying the suffix field.
         *
         * @since 12
         */
        public static final Field SUFFIX = new Field("suffix");
    }

    /**
     * A number format style.
     * <p>
     * {@code Style} is an enum which represents the style for formatting
     * a number within a given {@code NumberFormat} instance.
     *
     * @see CompactNumberFormat
     * @see NumberFormat#getCompactNumberInstance(Locale, Style)
     * @since 12
     */
    public enum Style {

        /**
         * The {@code SHORT} number format style.
         */
        SHORT,

        /**
         * The {@code LONG} number format style.
         */
        LONG

    }
}
