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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * {@code MessageFormat} provides a means to produce concatenated
 * messages in a language-neutral way. Use this to construct messages
 * displayed for end users.
 *
 * <p>
 * {@code MessageFormat} takes a set of objects, formats them, then
 * inserts the formatted strings into the pattern at the appropriate places.
 *
 * <p>
 * <strong>Note:</strong>
 * {@code MessageFormat} differs from the other {@code Format}
 * classes in that you create a {@code MessageFormat} object with one
 * of its constructors (not with a {@code getInstance} style factory
 * method). The factory methods aren't necessary because {@code MessageFormat}
 * itself doesn't implement locale specific behavior. Any locale specific
 * behavior is defined by the pattern that you provide as well as the
 * subformats used for inserted arguments.
 *
 * <h2><a id="patterns">Patterns and Their Interpretation</a></h2>
 *
 * {@code MessageFormat} uses patterns of the following form:
 * <blockquote><pre>
 * <i>MessageFormatPattern:</i>
 *         <i>String</i>
 *         <i>MessageFormatPattern</i> <i>FormatElement</i> <i>String</i>
 *
 * <i>FormatElement:</i>
 *         { <i>ArgumentIndex</i> }
 *         { <i>ArgumentIndex</i> , <i>FormatType</i> }
 *         { <i>ArgumentIndex</i> , <i>FormatType</i> , <i>FormatStyle</i> }
 *
 * <i>FormatType: one of </i>
 *         number date time choice
 *
 * <i>FormatStyle:</i>
 *         short
 *         medium
 *         long
 *         full
 *         integer
 *         currency
 *         percent
 *         <i>SubformatPattern</i>
 * </pre></blockquote>
 *
 * <p>Within a <i>String</i>, a pair of single quotes can be used to
 * quote any arbitrary characters except single quotes. For example,
 * pattern string <code>"'{0}'"</code> represents string
 * <code>"{0}"</code>, not a <i>FormatElement</i>. A single quote itself
 * must be represented by doubled single quotes {@code ''} throughout a
 * <i>String</i>.  For example, pattern string <code>"'{''}'"</code> is
 * interpreted as a sequence of <code>'{</code> (start of quoting and a
 * left curly brace), {@code ''} (a single quote), and
 * <code>}'</code> (a right curly brace and end of quoting),
 * <em>not</em> <code>'{'</code> and <code>'}'</code> (quoted left and
 * right curly braces): representing string <code>"{'}"</code>,
 * <em>not</em> <code>"{}"</code>.
 *
 * <p>A <i>SubformatPattern</i> is interpreted by its corresponding
 * subformat, and subformat-dependent pattern rules apply. For example,
 * pattern string <code>"{1,number,<u>$'#',##</u>}"</code>
 * (<i>SubformatPattern</i> with underline) will produce a number format
 * with the pound-sign quoted, with a result such as: {@code
 * "$#31,45"}. Refer to each {@code Format} subclass documentation for
 * details.
 *
 * <p>Any unmatched quote is treated as closed at the end of the given
 * pattern. For example, pattern string {@code "'{0}"} is treated as
 * pattern {@code "'{0}'"}.
 *
 * <p>Any curly braces within an unquoted pattern must be balanced. For
 * example, <code>"ab {0} de"</code> and <code>"ab '}' de"</code> are
 * valid patterns, but <code>"ab {0'}' de"</code>, <code>"ab } de"</code>
 * and <code>"''{''"</code> are not.
 *
 * <dl><dt><b>Warning:</b><dd>The rules for using quotes within message
 * format patterns unfortunately have shown to be somewhat confusing.
 * In particular, it isn't always obvious to localizers whether single
 * quotes need to be doubled or not. Make sure to inform localizers about
 * the rules, and tell them (for example, by using comments in resource
 * bundle source files) which strings will be processed by {@code MessageFormat}.
 * Note that localizers may need to use single quotes in translated
 * strings where the original version doesn't have them.
 * </dl>
 * <p>
 * The <i>ArgumentIndex</i> value is a non-negative integer written
 * using the digits {@code '0'} through {@code '9'}, and represents an index into the
 * {@code arguments} array passed to the {@code format} methods
 * or the result array returned by the {@code parse} methods.
 * <p>
 * The <i>FormatType</i> and <i>FormatStyle</i> values are used to create
 * a {@code Format} instance for the format element. The following
 * table shows how the values map to {@code Format} instances. Combinations not
 * shown in the table are illegal. A <i>SubformatPattern</i> must
 * be a valid pattern string for the {@code Format} subclass used.
 *
 * <table class="plain">
 * <caption style="display:none">Shows how FormatType and FormatStyle values map to Format instances</caption>
 * <thead>
 *    <tr>
 *       <th scope="col" class="TableHeadingColor">FormatType
 *       <th scope="col" class="TableHeadingColor">FormatStyle
 *       <th scope="col" class="TableHeadingColor">Subformat Created
 * </thead>
 * <tbody>
 *    <tr>
 *       <th scope="row" style="text-weight: normal"><i>(none)</i>
 *       <th scope="row" style="text-weight: normal"><i>(none)</i>
 *       <td>{@code null}
 *    <tr>
 *       <th scope="row" style="text-weight: normal" rowspan=5>{@code number}
 *       <th scope="row" style="text-weight: normal"><i>(none)</i>
 *       <td>{@link NumberFormat#getInstance(Locale) NumberFormat.getInstance}{@code (getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code integer}
 *       <td>{@link NumberFormat#getIntegerInstance(Locale) NumberFormat.getIntegerInstance}{@code (getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code currency}
 *       <td>{@link NumberFormat#getCurrencyInstance(Locale) NumberFormat.getCurrencyInstance}{@code (getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code percent}
 *       <td>{@link NumberFormat#getPercentInstance(Locale) NumberFormat.getPercentInstance}{@code (getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal"><i>SubformatPattern</i>
 *       <td>{@code new} {@link DecimalFormat#DecimalFormat(String,DecimalFormatSymbols) DecimalFormat}{@code (subformatPattern,} {@link DecimalFormatSymbols#getInstance(Locale) DecimalFormatSymbols.getInstance}{@code (getLocale()))}
 *    <tr>
 *       <th scope="row" style="text-weight: normal" rowspan=6>{@code date}
 *       <th scope="row" style="text-weight: normal"><i>(none)</i>
 *       <td>{@link DateFormat#getDateInstance(int,Locale) DateFormat.getDateInstance}{@code (}{@link DateFormat#DEFAULT}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code short}
 *       <td>{@link DateFormat#getDateInstance(int,Locale) DateFormat.getDateInstance}{@code (}{@link DateFormat#SHORT}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code medium}
 *       <td>{@link DateFormat#getDateInstance(int,Locale) DateFormat.getDateInstance}{@code (}{@link DateFormat#DEFAULT}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code long}
 *       <td>{@link DateFormat#getDateInstance(int,Locale) DateFormat.getDateInstance}{@code (}{@link DateFormat#LONG}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code full}
 *       <td>{@link DateFormat#getDateInstance(int,Locale) DateFormat.getDateInstance}{@code (}{@link DateFormat#FULL}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal"><i>SubformatPattern</i>
 *       <td>{@code new} {@link SimpleDateFormat#SimpleDateFormat(String,Locale) SimpleDateFormat}{@code (subformatPattern, getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal" rowspan=6>{@code time}
 *       <th scope="row" style="text-weight: normal"><i>(none)</i>
 *       <td>{@link DateFormat#getTimeInstance(int,Locale) DateFormat.getTimeInstance}{@code (}{@link DateFormat#DEFAULT}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code short}
 *       <td>{@link DateFormat#getTimeInstance(int,Locale) DateFormat.getTimeInstance}{@code (}{@link DateFormat#SHORT}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code medium}
 *       <td>{@link DateFormat#getTimeInstance(int,Locale) DateFormat.getTimeInstance}{@code (}{@link DateFormat#DEFAULT}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code long}
 *       <td>{@link DateFormat#getTimeInstance(int,Locale) DateFormat.getTimeInstance}{@code (}{@link DateFormat#LONG}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code full}
 *       <td>{@link DateFormat#getTimeInstance(int,Locale) DateFormat.getTimeInstance}{@code (}{@link DateFormat#FULL}{@code , getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal"><i>SubformatPattern</i>
 *       <td>{@code new} {@link SimpleDateFormat#SimpleDateFormat(String,Locale) SimpleDateFormat}{@code (subformatPattern, getLocale())}
 *    <tr>
 *       <th scope="row" style="text-weight: normal">{@code choice}
 *       <th scope="row" style="text-weight: normal"><i>SubformatPattern</i>
 *       <td>{@code new} {@link ChoiceFormat#ChoiceFormat(String) ChoiceFormat}{@code (subformatPattern)}
 * </tbody>
 * </table>
 *
 * <h3>Usage Information</h3>
 *
 * <p>
 * Here are some examples of usage.
 * In real internationalized programs, the message format pattern and other
 * static strings will, of course, be obtained from resource bundles.
 * Other parameters will be dynamically determined at runtime.
 * <p>
 * The first example uses the static method {@code MessageFormat.format},
 * which internally creates a {@code MessageFormat} for one-time use:
 * <blockquote><pre>
 * int planet = 7;
 * String event = "a disturbance in the Force";
 *
 * String result = MessageFormat.format(
 *     "At {1,time} on {1,date}, there was {2} on planet {0,number,integer}.",
 *     planet, new Date(), event);
 * </pre></blockquote>
 * The output is:
 * <blockquote><pre>
 * At 12:30 PM on Jul 3, 2053, there was a disturbance in the Force on planet 7.
 * </pre></blockquote>
 *
 * <p>
 * The following example creates a {@code MessageFormat} instance that
 * can be used repeatedly:
 * <blockquote><pre>
 * int fileCount = 1273;
 * String diskName = "MyDisk";
 * Object[] testArgs = {new Long(fileCount), diskName};
 *
 * MessageFormat form = new MessageFormat(
 *     "The disk \"{1}\" contains {0} file(s).");
 *
 * System.out.println(form.format(testArgs));
 * </pre></blockquote>
 * The output with different values for {@code fileCount}:
 * <blockquote><pre>
 * The disk "MyDisk" contains 0 file(s).
 * The disk "MyDisk" contains 1 file(s).
 * The disk "MyDisk" contains 1,273 file(s).
 * </pre></blockquote>
 *
 * <p>
 * For more sophisticated patterns, you can use a {@code ChoiceFormat}
 * to produce correct forms for singular and plural:
 * <blockquote><pre>
 * MessageFormat form = new MessageFormat("The disk \"{1}\" contains {0}.");
 * double[] filelimits = {0,1,2};
 * String[] filepart = {"no files","one file","{0,number} files"};
 * ChoiceFormat fileform = new ChoiceFormat(filelimits, filepart);
 * form.setFormatByArgumentIndex(0, fileform);
 *
 * int fileCount = 1273;
 * String diskName = "MyDisk";
 * Object[] testArgs = {new Long(fileCount), diskName};
 *
 * System.out.println(form.format(testArgs));
 * </pre></blockquote>
 * The output with different values for {@code fileCount}:
 * <blockquote><pre>
 * The disk "MyDisk" contains no files.
 * The disk "MyDisk" contains one file.
 * The disk "MyDisk" contains 1,273 files.
 * </pre></blockquote>
 *
 * <p>
 * You can create the {@code ChoiceFormat} programmatically, as in the
 * above example, or by using a pattern. See {@link ChoiceFormat}
 * for more information.
 * <blockquote><pre>{@code
 * form.applyPattern(
 *    "There {0,choice,0#are no files|1#is one file|1<are {0,number,integer} files}.");
 * }</pre></blockquote>
 *
 * <p>
 * <strong>Note:</strong> As we see above, the string produced
 * by a {@code ChoiceFormat} in {@code MessageFormat} is treated as special;
 * occurrences of '{' are used to indicate subformats, and cause recursion.
 * If you create both a {@code MessageFormat} and {@code ChoiceFormat}
 * programmatically (instead of using the string patterns), then be careful not to
 * produce a format that recurses on itself, which will cause an infinite loop.
 * <p>
 * When a single argument is parsed more than once in the string, the last match
 * will be the final result of the parsing.  For example,
 * <blockquote><pre>
 * MessageFormat mf = new MessageFormat("{0,number,#.##}, {0,number,#.#}");
 * Object[] objs = {new Double(3.1415)};
 * String result = mf.format( objs );
 * // result now equals "3.14, 3.1"
 * objs = null;
 * objs = mf.parse(result, new ParsePosition(0));
 * // objs now equals {new Double(3.1)}
 * </pre></blockquote>
 *
 * <p>
 * Likewise, parsing with a {@code MessageFormat} object using patterns containing
 * multiple occurrences of the same argument would return the last match.  For
 * example,
 * <blockquote><pre>
 * MessageFormat mf = new MessageFormat("{0}, {0}, {0}");
 * String forParsing = "x, y, z";
 * Object[] objs = mf.parse(forParsing, new ParsePosition(0));
 * // result now equals {new String("z")}
 * </pre></blockquote>
 *
 * <h3><a id="synchronization">Synchronization</a></h3>
 *
 * <p>
 * Message formats are not synchronized.
 * It is recommended to create separate format instances for each thread.
 * If multiple threads access a format concurrently, it must be synchronized
 * externally.
 *
 * @see          java.util.Locale
 * @see          Format
 * @see          NumberFormat
 * @see          DecimalFormat
 * @see          DecimalFormatSymbols
 * @see          ChoiceFormat
 * @see          DateFormat
 * @see          SimpleDateFormat
 *
 * @author       Mark Davis
 * @since 1.1
 */

public class MessageFormat extends Format {

    @java.io.Serial
    private static final long serialVersionUID = 6479157306784022952L;

    /**
     * Constructs a MessageFormat for the default
     * {@link java.util.Locale.Category#FORMAT FORMAT} locale and the
     * specified pattern.
     * The constructor first sets the locale, then parses the pattern and
     * creates a list of subformats for the format elements contained in it.
     * Patterns and their interpretation are specified in the
     * <a href="#patterns">class description</a>.
     *
     * @param pattern the pattern for this message format
     * @throws    IllegalArgumentException if the pattern is invalid
     * @throws    NullPointerException if {@code pattern} is
     *            {@code null}
     */
    public MessageFormat(String pattern) {
        this.locale = Locale.getDefault(Locale.Category.FORMAT);
        applyPattern(pattern);
    }

    /**
     * Constructs a MessageFormat for the specified locale and
     * pattern.
     * The constructor first sets the locale, then parses the pattern and
     * creates a list of subformats for the format elements contained in it.
     * Patterns and their interpretation are specified in the
     * <a href="#patterns">class description</a>.
     *
     * @param pattern the pattern for this message format
     * @param locale the locale for this message format
     * @throws    IllegalArgumentException if the pattern is invalid
     * @throws    NullPointerException if {@code pattern} is
     *            {@code null}
     * @since 1.4
     */
    public MessageFormat(String pattern, Locale locale) {
        this.locale = locale;
        applyPattern(pattern);
    }

    /**
     * Sets the locale to be used when creating or comparing subformats.
     * This affects subsequent calls
     * <ul>
     * <li>to the {@link #applyPattern applyPattern}
     *     and {@link #toPattern toPattern} methods if format elements specify
     *     a format type and therefore have the subformats created in the
     *     {@code applyPattern} method, as well as
     * <li>to the {@code format} and
     *     {@link #formatToCharacterIterator formatToCharacterIterator} methods
     *     if format elements do not specify a format type and therefore have
     *     the subformats created in the formatting methods.
     * </ul>
     * Subformats that have already been created are not affected.
     *
     * @param locale the locale to be used when creating or comparing subformats
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Gets the locale that's used when creating or comparing subformats.
     *
     * @return the locale used when creating or comparing subformats
     */
    public Locale getLocale() {
        return locale;
    }


    /**
     * Sets the pattern used by this message format.
     * The method parses the pattern and creates a list of subformats
     * for the format elements contained in it.
     * Patterns and their interpretation are specified in the
     * <a href="#patterns">class description</a>.
     *
     * @param pattern the pattern for this message format
     * @throws    IllegalArgumentException if the pattern is invalid
     * @throws    NullPointerException if {@code pattern} is
     *            {@code null}
     */
    @SuppressWarnings("fallthrough") // fallthrough in switch is expected, suppress it
    public void applyPattern(String pattern) {
            StringBuilder[] segments = new StringBuilder[4];
            // Allocate only segments[SEG_RAW] here. The rest are
            // allocated on demand.
            segments[SEG_RAW] = new StringBuilder();

            int part = SEG_RAW;
            int formatNumber = 0;
            boolean inQuote = false;
            int braceStack = 0;
            maxOffset = -1;
            for (int i = 0; i < pattern.length(); ++i) {
                char ch = pattern.charAt(i);
                if (part == SEG_RAW) {
                    if (ch == '\'') {
                        if (i + 1 < pattern.length()
                            && pattern.charAt(i+1) == '\'') {
                            segments[part].append(ch);  // handle doubles
                            ++i;
                        } else {
                            inQuote = !inQuote;
                        }
                    } else if (ch == '{' && !inQuote) {
                        part = SEG_INDEX;
                        if (segments[SEG_INDEX] == null) {
                            segments[SEG_INDEX] = new StringBuilder();
                        }
                    } else {
                        segments[part].append(ch);
                    }
                } else  {
                    if (inQuote) {              // just copy quotes in parts
                        segments[part].append(ch);
                        if (ch == '\'') {
                            inQuote = false;
                        }
                    } else {
                        switch (ch) {
                        case ',':
                            if (part < SEG_MODIFIER) {
                                if (segments[++part] == null) {
                                    segments[part] = new StringBuilder();
                                }
                            } else {
                                segments[part].append(ch);
                            }
                            break;
                        case '{':
                            ++braceStack;
                            segments[part].append(ch);
                            break;
                        case '}':
                            if (braceStack == 0) {
                                part = SEG_RAW;
                                makeFormat(i, formatNumber, segments);
                                formatNumber++;
                                // throw away other segments
                                segments[SEG_INDEX] = null;
                                segments[SEG_TYPE] = null;
                                segments[SEG_MODIFIER] = null;
                            } else {
                                --braceStack;
                                segments[part].append(ch);
                            }
                            break;
                        case ' ':
                            // Skip any leading space chars for SEG_TYPE.
                            if (part != SEG_TYPE || segments[SEG_TYPE].length() > 0) {
                                segments[part].append(ch);
                            }
                            break;
                        case '\'':
                            inQuote = true;
                            // fall through, so we keep quotes in other parts
                        default:
                            segments[part].append(ch);
                            break;
                        }
                    }
                }
            }
            if (braceStack == 0 && part != 0) {
                maxOffset = -1;
                throw new IllegalArgumentException("Unmatched braces in the pattern.");
            }
            this.pattern = segments[0].toString();
    }


    /**
     * Returns a pattern representing the current state of the message format.
     * The string is constructed from internal information and therefore
     * does not necessarily equal the previously applied pattern.
     *
     * @return a pattern representing the current state of the message format
     */
    public String toPattern() {
        // later, make this more extensible
        int lastOffset = 0;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i <= maxOffset; ++i) {
            copyAndFixQuotes(pattern, lastOffset, offsets[i], result);
            lastOffset = offsets[i];
            result.append('{').append(argumentNumbers[i]);
            Format fmt = formats[i];
            if (fmt == null) {
                // do nothing, string format
            } else if (fmt instanceof NumberFormat) {
                if (fmt.equals(NumberFormat.getInstance(locale))) {
                    result.append(",number");
                } else if (fmt.equals(NumberFormat.getCurrencyInstance(locale))) {
                    result.append(",number,currency");
                } else if (fmt.equals(NumberFormat.getPercentInstance(locale))) {
                    result.append(",number,percent");
                } else if (fmt.equals(NumberFormat.getIntegerInstance(locale))) {
                    result.append(",number,integer");
                } else {
                    if (fmt instanceof DecimalFormat) {
                        result.append(",number,").append(((DecimalFormat)fmt).toPattern());
                    } else if (fmt instanceof ChoiceFormat) {
                        result.append(",choice,").append(((ChoiceFormat)fmt).toPattern());
                    } else {
                        // UNKNOWN
                    }
                }
            } else if (fmt instanceof DateFormat) {
                int index;
                for (index = MODIFIER_DEFAULT; index < DATE_TIME_MODIFIERS.length; index++) {
                    DateFormat df = DateFormat.getDateInstance(DATE_TIME_MODIFIERS[index],
                                                               locale);
                    if (fmt.equals(df)) {
                        result.append(",date");
                        break;
                    }
                    df = DateFormat.getTimeInstance(DATE_TIME_MODIFIERS[index],
                                                    locale);
                    if (fmt.equals(df)) {
                        result.append(",time");
                        break;
                    }
                }
                if (index >= DATE_TIME_MODIFIERS.length) {
                    if (fmt instanceof SimpleDateFormat) {
                        result.append(",date,").append(((SimpleDateFormat)fmt).toPattern());
                    } else {
                        // UNKNOWN
                    }
                } else if (index != MODIFIER_DEFAULT) {
                    result.append(',').append(DATE_TIME_MODIFIER_KEYWORDS[index]);
                }
            } else {
                //result.append(", unknown");
            }
            result.append('}');
        }
        copyAndFixQuotes(pattern, lastOffset, pattern.length(), result);
        return result.toString();
    }

    /**
     * Sets the formats to use for the values passed into
     * {@code format} methods or returned from {@code parse}
     * methods. The indices of elements in {@code newFormats}
     * correspond to the argument indices used in the previously set
     * pattern string.
     * The order of formats in {@code newFormats} thus corresponds to
     * the order of elements in the {@code arguments} array passed
     * to the {@code format} methods or the result array returned
     * by the {@code parse} methods.
     * <p>
     * If an argument index is used for more than one format element
     * in the pattern string, then the corresponding new format is used
     * for all such format elements. If an argument index is not used
     * for any format element in the pattern string, then the
     * corresponding new format is ignored. If fewer formats are provided
     * than needed, then only the formats for argument indices less
     * than {@code newFormats.length} are replaced.
     *
     * @param newFormats the new formats to use
     * @throws    NullPointerException if {@code newFormats} is null
     * @since 1.4
     */
    public void setFormatsByArgumentIndex(Format[] newFormats) {
        for (int i = 0; i <= maxOffset; i++) {
            int j = argumentNumbers[i];
            if (j < newFormats.length) {
                formats[i] = newFormats[j];
            }
        }
    }

    /**
     * Sets the formats to use for the format elements in the
     * previously set pattern string.
     * The order of formats in {@code newFormats} corresponds to
     * the order of format elements in the pattern string.
     * <p>
     * If more formats are provided than needed by the pattern string,
     * the remaining ones are ignored. If fewer formats are provided
     * than needed, then only the first {@code newFormats.length}
     * formats are replaced.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it is generally better to use the
     * {@link #setFormatsByArgumentIndex setFormatsByArgumentIndex}
     * method, which assumes an order of formats corresponding to the
     * order of elements in the {@code arguments} array passed to
     * the {@code format} methods or the result array returned by
     * the {@code parse} methods.
     *
     * @param newFormats the new formats to use
     * @throws    NullPointerException if {@code newFormats} is null
     */
    public void setFormats(Format[] newFormats) {
        int runsToCopy = newFormats.length;
        if (runsToCopy > maxOffset + 1) {
            runsToCopy = maxOffset + 1;
        }
        for (int i = 0; i < runsToCopy; i++) {
            formats[i] = newFormats[i];
        }
    }

    /**
     * Sets the format to use for the format elements within the
     * previously set pattern string that use the given argument
     * index.
     * The argument index is part of the format element definition and
     * represents an index into the {@code arguments} array passed
     * to the {@code format} methods or the result array returned
     * by the {@code parse} methods.
     * <p>
     * If the argument index is used for more than one format element
     * in the pattern string, then the new format is used for all such
     * format elements. If the argument index is not used for any format
     * element in the pattern string, then the new format is ignored.
     *
     * @param argumentIndex the argument index for which to use the new format
     * @param newFormat the new format to use
     * @since 1.4
     */
    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        for (int j = 0; j <= maxOffset; j++) {
            if (argumentNumbers[j] == argumentIndex) {
                formats[j] = newFormat;
            }
        }
    }

    /**
     * Sets the format to use for the format element with the given
     * format element index within the previously set pattern string.
     * The format element index is the zero-based number of the format
     * element counting from the start of the pattern string.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it is generally better to use the
     * {@link #setFormatByArgumentIndex setFormatByArgumentIndex}
     * method, which accesses format elements based on the argument
     * index they specify.
     *
     * @param formatElementIndex the index of a format element within the pattern
     * @param newFormat the format to use for the specified format element
     * @throws    ArrayIndexOutOfBoundsException if {@code formatElementIndex} is equal to or
     *            larger than the number of format elements in the pattern string
     */
    public void setFormat(int formatElementIndex, Format newFormat) {

        if (formatElementIndex > maxOffset) {
            throw new ArrayIndexOutOfBoundsException(formatElementIndex);
        }
        formats[formatElementIndex] = newFormat;
    }

    /**
     * Gets the formats used for the values passed into
     * {@code format} methods or returned from {@code parse}
     * methods. The indices of elements in the returned array
     * correspond to the argument indices used in the previously set
     * pattern string.
     * The order of formats in the returned array thus corresponds to
     * the order of elements in the {@code arguments} array passed
     * to the {@code format} methods or the result array returned
     * by the {@code parse} methods.
     * <p>
     * If an argument index is used for more than one format element
     * in the pattern string, then the format used for the last such
     * format element is returned in the array. If an argument index
     * is not used for any format element in the pattern string, then
     * null is returned in the array.
     *
     * @return the formats used for the arguments within the pattern
     * @since 1.4
     */
    public Format[] getFormatsByArgumentIndex() {
        int maximumArgumentNumber = -1;
        for (int i = 0; i <= maxOffset; i++) {
            if (argumentNumbers[i] > maximumArgumentNumber) {
                maximumArgumentNumber = argumentNumbers[i];
            }
        }
        Format[] resultArray = new Format[maximumArgumentNumber + 1];
        for (int i = 0; i <= maxOffset; i++) {
            resultArray[argumentNumbers[i]] = formats[i];
        }
        return resultArray;
    }

    /**
     * Gets the formats used for the format elements in the
     * previously set pattern string.
     * The order of formats in the returned array corresponds to
     * the order of format elements in the pattern string.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it's generally better to use the
     * {@link #getFormatsByArgumentIndex getFormatsByArgumentIndex}
     * method, which assumes an order of formats corresponding to the
     * order of elements in the {@code arguments} array passed to
     * the {@code format} methods or the result array returned by
     * the {@code parse} methods.
     *
     * @return the formats used for the format elements in the pattern
     */
    public Format[] getFormats() {
        Format[] resultArray = new Format[maxOffset + 1];
        System.arraycopy(formats, 0, resultArray, 0, maxOffset + 1);
        return resultArray;
    }

    /**
     * Formats an array of objects and appends the {@code MessageFormat}'s
     * pattern, with format elements replaced by the formatted objects, to the
     * provided {@code StringBuffer}.
     * <p>
     * The text substituted for the individual format elements is derived from
     * the current subformat of the format element and the
     * {@code arguments} element at the format element's argument index
     * as indicated by the first matching line of the following table. An
     * argument is <i>unavailable</i> if {@code arguments} is
     * {@code null} or has fewer than argumentIndex+1 elements.
     *
     * <table class="plain">
     * <caption style="display:none">Examples of subformat,argument,and formatted text</caption>
     * <thead>
     *    <tr>
     *       <th scope="col">Subformat
     *       <th scope="col">Argument
     *       <th scope="col">Formatted Text
     * </thead>
     * <tbody>
     *    <tr>
     *       <th scope="row" style="text-weight-normal" rowspan=2><i>any</i>
     *       <th scope="row" style="text-weight-normal"><i>unavailable</i>
     *       <td><code>"{" + argumentIndex + "}"</code>
     *    <tr>
     *       <th scope="row" style="text-weight-normal">{@code null}
     *       <td>{@code "null"}
     *    <tr>
     *       <th scope="row" style="text-weight-normal">{@code instanceof ChoiceFormat}
     *       <th scope="row" style="text-weight-normal"><i>any</i>
     *       <td><code>subformat.format(argument).indexOf('{') &gt;= 0 ?<br>
     *           (new MessageFormat(subformat.format(argument), getLocale())).format(argument) :
     *           subformat.format(argument)</code>
     *    <tr>
     *       <th scope="row" style="text-weight-normal">{@code != null}
     *       <th scope="row" style="text-weight-normal"><i>any</i>
     *       <td>{@code subformat.format(argument)}
     *    <tr>
     *       <th scope="row" style="text-weight-normal" rowspan=4>{@code null}
     *       <th scope="row" style="text-weight-normal">{@code instanceof Number}
     *       <td>{@code NumberFormat.getInstance(getLocale()).format(argument)}
     *    <tr>
     *       <th scope="row" style="text-weight-normal">{@code instanceof Date}
     *       <td>{@code DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale()).format(argument)}
     *    <tr>
     *       <th scope="row" style="text-weight-normal">{@code instanceof String}
     *       <td>{@code argument}
     *    <tr>
     *       <th scope="row" style="text-weight-normal"><i>any</i>
     *       <td>{@code argument.toString()}
     * </tbody>
     * </table>
     * <p>
     * If {@code pos} is non-null, and refers to
     * {@code Field.ARGUMENT}, the location of the first formatted
     * string will be returned.
     *
     * @param arguments an array of objects to be formatted and substituted.
     * @param result where text is appended.
     * @param pos keeps track on the position of the first replaced argument
     *            in the output string.
     * @return the string buffer passed in as {@code result}, with formatted
     * text appended
     * @throws    IllegalArgumentException if an argument in the
     *            {@code arguments} array is not of the type
     *            expected by the format element(s) that use it.
     * @throws    NullPointerException if {@code result} is {@code null}
     */
    public final StringBuffer format(Object[] arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        return subformat(arguments, result, pos, null);
    }

    /**
     * Creates a MessageFormat with the given pattern and uses it
     * to format the given arguments. This is equivalent to
     * <blockquote>
     *     <code>(new {@link #MessageFormat(String) MessageFormat}(pattern)).{@link #format(java.lang.Object[], java.lang.StringBuffer, java.text.FieldPosition) format}(arguments, new StringBuffer(), null).toString()</code>
     * </blockquote>
     *
     * @param pattern   the pattern string
     * @param arguments object(s) to format
     * @return the formatted string
     * @throws    IllegalArgumentException if the pattern is invalid,
     *            or if an argument in the {@code arguments} array
     *            is not of the type expected by the format element(s)
     *            that use it.
     * @throws    NullPointerException if {@code pattern} is {@code null}
     */
    public static String format(String pattern, Object ... arguments) {
        MessageFormat temp = new MessageFormat(pattern);
        return temp.format(arguments);
    }

    // Overrides
    /**
     * Formats an array of objects and appends the {@code MessageFormat}'s
     * pattern, with format elements replaced by the formatted objects, to the
     * provided {@code StringBuffer}.
     * This is equivalent to
     * <blockquote>
     *     <code>{@link #format(java.lang.Object[], java.lang.StringBuffer, java.text.FieldPosition) format}((Object[]) arguments, result, pos)</code>
     * </blockquote>
     *
     * @param arguments an array of objects to be formatted and substituted.
     * @param result where text is appended.
     * @param pos keeps track on the position of the first replaced argument
     *            in the output string.
     * @throws    IllegalArgumentException if an argument in the
     *            {@code arguments} array is not of the type
     *            expected by the format element(s) that use it.
     * @throws    NullPointerException if {@code result} is {@code null}
     */
    public final StringBuffer format(Object arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        return subformat((Object[]) arguments, result, pos, null);
    }

    /**
     * Formats an array of objects and inserts them into the
     * {@code MessageFormat}'s pattern, producing an
     * {@code AttributedCharacterIterator}.
     * You can use the returned {@code AttributedCharacterIterator}
     * to build the resulting String, as well as to determine information
     * about the resulting String.
     * <p>
     * The text of the returned {@code AttributedCharacterIterator} is
     * the same that would be returned by
     * <blockquote>
     *     <code>{@link #format(java.lang.Object[], java.lang.StringBuffer, java.text.FieldPosition) format}(arguments, new StringBuffer(), null).toString()</code>
     * </blockquote>
     * <p>
     * In addition, the {@code AttributedCharacterIterator} contains at
     * least attributes indicating where text was generated from an
     * argument in the {@code arguments} array. The keys of these attributes are of
     * type {@code MessageFormat.Field}, their values are
     * {@code Integer} objects indicating the index in the {@code arguments}
     * array of the argument from which the text was generated.
     * <p>
     * The attributes/value from the underlying {@code Format}
     * instances that {@code MessageFormat} uses will also be
     * placed in the resulting {@code AttributedCharacterIterator}.
     * This allows you to not only find where an argument is placed in the
     * resulting String, but also which fields it contains in turn.
     *
     * @param arguments an array of objects to be formatted and substituted.
     * @return AttributedCharacterIterator describing the formatted value.
     * @throws    NullPointerException if {@code arguments} is null.
     * @throws    IllegalArgumentException if an argument in the
     *            {@code arguments} array is not of the type
     *            expected by the format element(s) that use it.
     * @since 1.4
     */
    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
        StringBuffer result = new StringBuffer();
        ArrayList<AttributedCharacterIterator> iterators = new ArrayList<>();

        if (arguments == null) {
            throw new NullPointerException(
                   "formatToCharacterIterator must be passed non-null object");
        }
        subformat((Object[]) arguments, result, null, iterators);
        if (iterators.size() == 0) {
            return createAttributedCharacterIterator("");
        }
        return createAttributedCharacterIterator(
                     iterators.toArray(
                     new AttributedCharacterIterator[iterators.size()]));
    }

    /**
     * Parses the string.
     *
     * <p>Caveats: The parse may fail in a number of circumstances.
     * For example:
     * <ul>
     * <li>If one of the arguments does not occur in the pattern.
     * <li>If the format of an argument loses information, such as
     *     with a choice format where a large number formats to "many".
     * <li>Does not yet handle recursion (where
     *     the substituted strings contain {n} references.)
     * <li>Will not always find a match (or the correct match)
     *     if some part of the parse is ambiguous.
     *     For example, if the pattern "{1},{2}" is used with the
     *     string arguments {"a,b", "c"}, it will format as "a,b,c".
     *     When the result is parsed, it will return {"a", "b,c"}.
     * <li>If a single argument is parsed more than once in the string,
     *     then the later parse wins.
     * </ul>
     * When the parse fails, use ParsePosition.getErrorIndex() to find out
     * where in the string the parsing failed.  The returned error
     * index is the starting offset of the sub-patterns that the string
     * is comparing with.  For example, if the parsing string "AAA {0} BBB"
     * is comparing against the pattern "AAD {0} BBB", the error index is
     * 0. When an error occurs, the call to this method will return null.
     * If the source is null, return an empty array.
     *
     * @param source the string to parse
     * @param pos    the parse position
     * @return an array of parsed objects
     * @throws    NullPointerException if {@code pos} is {@code null}
     *            for a non-null {@code source} string.
     */
    public Object[] parse(String source, ParsePosition pos) {
        if (source == null) {
            Object[] empty = {};
            return empty;
        }

        int maximumArgumentNumber = -1;
        for (int i = 0; i <= maxOffset; i++) {
            if (argumentNumbers[i] > maximumArgumentNumber) {
                maximumArgumentNumber = argumentNumbers[i];
            }
        }
        Object[] resultArray = new Object[maximumArgumentNumber + 1];

        int patternOffset = 0;
        int sourceOffset = pos.index;
        ParsePosition tempStatus = new ParsePosition(0);
        for (int i = 0; i <= maxOffset; ++i) {
            // match up to format
            int len = offsets[i] - patternOffset;
            if (len == 0 || pattern.regionMatches(patternOffset,
                                                  source, sourceOffset, len)) {
                sourceOffset += len;
                patternOffset += len;
            } else {
                pos.errorIndex = sourceOffset;
                return null; // leave index as is to signal error
            }

            // now use format
            if (formats[i] == null) {   // string format
                // if at end, use longest possible match
                // otherwise uses first match to intervening string
                // does NOT recursively try all possibilities
                int tempLength = (i != maxOffset) ? offsets[i+1] : pattern.length();

                int next;
                if (patternOffset >= tempLength) {
                    next = source.length();
                }else{
                    next = source.indexOf(pattern.substring(patternOffset, tempLength),
                                          sourceOffset);
                }

                if (next < 0) {
                    pos.errorIndex = sourceOffset;
                    return null; // leave index as is to signal error
                } else {
                    String strValue= source.substring(sourceOffset,next);
                    if (!strValue.equals("{"+argumentNumbers[i]+"}"))
                        resultArray[argumentNumbers[i]]
                            = source.substring(sourceOffset,next);
                    sourceOffset = next;
                }
            } else {
                tempStatus.index = sourceOffset;
                resultArray[argumentNumbers[i]]
                    = formats[i].parseObject(source,tempStatus);
                if (tempStatus.index == sourceOffset) {
                    pos.errorIndex = sourceOffset;
                    return null; // leave index as is to signal error
                }
                sourceOffset = tempStatus.index; // update
            }
        }
        int len = pattern.length() - patternOffset;
        if (len == 0 || pattern.regionMatches(patternOffset,
                                              source, sourceOffset, len)) {
            pos.index = sourceOffset + len;
        } else {
            pos.errorIndex = sourceOffset;
            return null; // leave index as is to signal error
        }
        return resultArray;
    }

    /**
     * Parses text from the beginning of the given string to produce an object
     * array.
     * The method may not use the entire text of the given string.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on message parsing.
     *
     * @param source A {@code String} whose beginning should be parsed.
     * @return An {@code Object} array parsed from the string.
     * @throws    ParseException if the beginning of the specified string
     *            cannot be parsed.
     */
    public Object[] parse(String source) throws ParseException {
        ParsePosition pos  = new ParsePosition(0);
        Object[] result = parse(source, pos);
        if (pos.index == 0)  // unchanged, returned object is null
            throw new ParseException("MessageFormat parse error!", pos.errorIndex);

        return result;
    }

    /**
     * Parses text from a string to produce an object array.
     * <p>
     * The method attempts to parse text starting at the index given by
     * {@code pos}.
     * If parsing succeeds, then the index of {@code pos} is updated
     * to the index after the last character used (parsing does not necessarily
     * use all characters up to the end of the string), and the parsed
     * object array is returned. The updated {@code pos} can be used to
     * indicate the starting point for the next call to this method.
     * If an error occurs, then the index of {@code pos} is not
     * changed, the error index of {@code pos} is set to the index of
     * the character where the error occurred, and null is returned.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on message parsing.
     *
     * @param source A {@code String}, part of which should be parsed.
     * @param pos A {@code ParsePosition} object with index and error
     *            index information as described above.
     * @return An {@code Object} array parsed from the string. In case of
     *         error, returns null.
     * @throws NullPointerException if {@code pos} is null.
     */
    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a clone of this instance.
     */
    public Object clone() {
        MessageFormat other = (MessageFormat) super.clone();

        // clone arrays. Can't do with utility because of bug in Cloneable
        other.formats = formats.clone(); // shallow clone
        for (int i = 0; i < formats.length; ++i) {
            if (formats[i] != null)
                other.formats[i] = (Format)formats[i].clone();
        }
        // for primitives or immutables, shallow clone is enough
        other.offsets = offsets.clone();
        other.argumentNumbers = argumentNumbers.clone();

        return other;
    }

    /**
     * Equality comparison between two message format objects
     */
    public boolean equals(Object obj) {
        if (this == obj)                      // quick check
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MessageFormat other = (MessageFormat) obj;
        return (maxOffset == other.maxOffset
                && pattern.equals(other.pattern)
                && ((locale != null && locale.equals(other.locale))
                 || (locale == null && other.locale == null))
                && Arrays.equals(offsets,other.offsets)
                && Arrays.equals(argumentNumbers,other.argumentNumbers)
                && Arrays.equals(formats,other.formats));
    }

    /**
     * Generates a hash code for the message format object.
     */
    public int hashCode() {
        return pattern.hashCode(); // enough for reasonable distribution
    }


    /**
     * Defines constants that are used as attribute keys in the
     * {@code AttributedCharacterIterator} returned
     * from {@code MessageFormat.formatToCharacterIterator}.
     *
     * @since 1.4
     */
    public static class Field extends Format.Field {

        // Proclaim serial compatibility with 1.4 FCS
        @java.io.Serial
        private static final long serialVersionUID = 7899943957617360810L;

        /**
         * Creates a Field with the specified name.
         *
         * @param name Name of the attribute
         */
        protected Field(String name) {
            super(name);
        }

        /**
         * Resolves instances being deserialized to the predefined constants.
         *
         * @throws InvalidObjectException if the constant could not be
         *         resolved.
         * @return resolved MessageFormat.Field constant
         */
        @java.io.Serial
        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != MessageFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }

            return ARGUMENT;
        }

        //
        // The constants
        //

        /**
         * Constant identifying a portion of a message that was generated
         * from an argument passed into {@code formatToCharacterIterator}.
         * The value associated with the key will be an {@code Integer}
         * indicating the index in the {@code arguments} array of the
         * argument from which the text was generated.
         */
        public static final Field ARGUMENT =
                           new Field("message argument field");
    }

    // ===========================privates============================

    /**
     * The locale to use for formatting numbers and dates.
     * @serial
     */
    private Locale locale;

    /**
     * The string that the formatted values are to be plugged into.  In other words, this
     * is the pattern supplied on construction with all of the {} expressions taken out.
     * @serial
     */
    private String pattern = "";

    /** The initially expected number of subformats in the format */
    private static final int INITIAL_FORMATS = 10;

    /**
     * An array of formatters, which are used to format the arguments.
     * @serial
     */
    private Format[] formats = new Format[INITIAL_FORMATS];

    /**
     * The positions where the results of formatting each argument are to be inserted
     * into the pattern.
     * @serial
     */
    private int[] offsets = new int[INITIAL_FORMATS];

    /**
     * The argument numbers corresponding to each formatter.  (The formatters are stored
     * in the order they occur in the pattern, not in the order in which the arguments
     * are specified.)
     * @serial
     */
    private int[] argumentNumbers = new int[INITIAL_FORMATS];

    /**
     * One less than the number of entries in {@code offsets}.  Can also be thought of
     * as the index of the highest-numbered element in {@code offsets} that is being used.
     * All of these arrays should have the same number of elements being used as {@code offsets}
     * does, and so this variable suffices to tell us how many entries are in all of them.
     * @serial
     */
    private int maxOffset = -1;

    /**
     * Internal routine used by format. If {@code characterIterators} is
     * {@code non-null}, AttributedCharacterIterator will be created from the
     * subformats as necessary. If {@code characterIterators} is {@code null}
     * and {@code fp} is {@code non-null} and identifies
     * {@code Field.ARGUMENT} as the field attribute, the location of
     * the first replaced argument will be set in it.
     *
     * @throws    IllegalArgumentException if an argument in the
     *            {@code arguments} array is not of the type
     *            expected by the format element(s) that use it.
     */
    private StringBuffer subformat(Object[] arguments, StringBuffer result,
                                   FieldPosition fp, List<AttributedCharacterIterator> characterIterators) {
        // note: this implementation assumes a fast substring & index.
        // if this is not true, would be better to append chars one by one.
        int lastOffset = 0;
        int last = result.length();
        for (int i = 0; i <= maxOffset; ++i) {
            result.append(pattern, lastOffset, offsets[i]);
            lastOffset = offsets[i];
            int argumentNumber = argumentNumbers[i];
            if (arguments == null || argumentNumber >= arguments.length) {
                result.append('{').append(argumentNumber).append('}');
                continue;
            }
            // int argRecursion = ((recursionProtection >> (argumentNumber*2)) & 0x3);
            if (false) { // if (argRecursion == 3){
                // prevent loop!!!
                result.append('\uFFFD');
            } else {
                Object obj = arguments[argumentNumber];
                String arg = null;
                Format subFormatter = null;
                if (obj == null) {
                    arg = "null";
                } else if (formats[i] != null) {
                    subFormatter = formats[i];
                    if (subFormatter instanceof ChoiceFormat) {
                        arg = formats[i].format(obj);
                        if (arg.indexOf('{') >= 0) {
                            subFormatter = new MessageFormat(arg, locale);
                            obj = arguments;
                            arg = null;
                        }
                    }
                } else if (obj instanceof Number) {
                    // format number if can
                    subFormatter = NumberFormat.getInstance(locale);
                } else if (obj instanceof Date) {
                    // format a Date if can
                    subFormatter = DateFormat.getDateTimeInstance(
                             DateFormat.SHORT, DateFormat.SHORT, locale);//fix
                } else if (obj instanceof String) {
                    arg = (String) obj;

                } else {
                    arg = obj.toString();
                    if (arg == null) arg = "null";
                }

                // At this point we are in two states, either subFormatter
                // is non-null indicating we should format obj using it,
                // or arg is non-null and we should use it as the value.

                if (characterIterators != null) {
                    // If characterIterators is non-null, it indicates we need
                    // to get the CharacterIterator from the child formatter.
                    if (last != result.length()) {
                        characterIterators.add(
                            createAttributedCharacterIterator(result.substring
                                                              (last)));
                        last = result.length();
                    }
                    if (subFormatter != null) {
                        AttributedCharacterIterator subIterator =
                                   subFormatter.formatToCharacterIterator(obj);

                        append(result, subIterator);
                        if (last != result.length()) {
                            characterIterators.add(
                                         createAttributedCharacterIterator(
                                         subIterator, Field.ARGUMENT,
                                         Integer.valueOf(argumentNumber)));
                            last = result.length();
                        }
                        arg = null;
                    }
                    if (arg != null && !arg.isEmpty()) {
                        result.append(arg);
                        characterIterators.add(
                                 createAttributedCharacterIterator(
                                 arg, Field.ARGUMENT,
                                 Integer.valueOf(argumentNumber)));
                        last = result.length();
                    }
                }
                else {
                    if (subFormatter != null) {
                        arg = subFormatter.format(obj);
                    }
                    last = result.length();
                    result.append(arg);
                    if (i == 0 && fp != null && Field.ARGUMENT.equals(
                                  fp.getFieldAttribute())) {
                        fp.setBeginIndex(last);
                        fp.setEndIndex(result.length());
                    }
                    last = result.length();
                }
            }
        }
        result.append(pattern, lastOffset, pattern.length());
        if (characterIterators != null && last != result.length()) {
            characterIterators.add(createAttributedCharacterIterator(
                                   result.substring(last)));
        }
        return result;
    }

    /**
     * Convenience method to append all the characters in
     * {@code iterator} to the StringBuffer {@code result}.
     */
    private void append(StringBuffer result, CharacterIterator iterator) {
        if (iterator.first() != CharacterIterator.DONE) {
            char aChar;

            result.append(iterator.first());
            while ((aChar = iterator.next()) != CharacterIterator.DONE) {
                result.append(aChar);
            }
        }
    }

    // Indices for segments
    private static final int SEG_RAW      = 0;
    private static final int SEG_INDEX    = 1;
    private static final int SEG_TYPE     = 2;
    private static final int SEG_MODIFIER = 3; // modifier or subformat

    // Indices for type keywords
    private static final int TYPE_NULL    = 0;
    private static final int TYPE_NUMBER  = 1;
    private static final int TYPE_DATE    = 2;
    private static final int TYPE_TIME    = 3;
    private static final int TYPE_CHOICE  = 4;

    private static final String[] TYPE_KEYWORDS = {
        "",
        "number",
        "date",
        "time",
        "choice"
    };

    // Indices for number modifiers
    private static final int MODIFIER_DEFAULT  = 0; // common in number and date-time
    private static final int MODIFIER_CURRENCY = 1;
    private static final int MODIFIER_PERCENT  = 2;
    private static final int MODIFIER_INTEGER  = 3;

    private static final String[] NUMBER_MODIFIER_KEYWORDS = {
        "",
        "currency",
        "percent",
        "integer"
    };

    // Indices for date-time modifiers
    private static final int MODIFIER_SHORT   = 1;
    private static final int MODIFIER_MEDIUM  = 2;
    private static final int MODIFIER_LONG    = 3;
    private static final int MODIFIER_FULL    = 4;

    private static final String[] DATE_TIME_MODIFIER_KEYWORDS = {
        "",
        "short",
        "medium",
        "long",
        "full"
    };

    // Date-time style values corresponding to the date-time modifiers.
    private static final int[] DATE_TIME_MODIFIERS = {
        DateFormat.DEFAULT,
        DateFormat.SHORT,
        DateFormat.MEDIUM,
        DateFormat.LONG,
        DateFormat.FULL,
    };

    private void makeFormat(int position, int offsetNumber,
                            StringBuilder[] textSegments)
    {
        String[] segments = new String[textSegments.length];
        for (int i = 0; i < textSegments.length; i++) {
            StringBuilder oneseg = textSegments[i];
            segments[i] = (oneseg != null) ? oneseg.toString() : "";
        }

        // get the argument number
        int argumentNumber;
        try {
            argumentNumber = Integer.parseInt(segments[SEG_INDEX]); // always unlocalized!
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("can't parse argument number: "
                                               + segments[SEG_INDEX], e);
        }
        if (argumentNumber < 0) {
            throw new IllegalArgumentException("negative argument number: "
                                               + argumentNumber);
        }

        // resize format information arrays if necessary
        if (offsetNumber >= formats.length) {
            int newLength = formats.length * 2;
            Format[] newFormats = new Format[newLength];
            int[] newOffsets = new int[newLength];
            int[] newArgumentNumbers = new int[newLength];
            System.arraycopy(formats, 0, newFormats, 0, maxOffset + 1);
            System.arraycopy(offsets, 0, newOffsets, 0, maxOffset + 1);
            System.arraycopy(argumentNumbers, 0, newArgumentNumbers, 0, maxOffset + 1);
            formats = newFormats;
            offsets = newOffsets;
            argumentNumbers = newArgumentNumbers;
        }
        int oldMaxOffset = maxOffset;
        maxOffset = offsetNumber;
        offsets[offsetNumber] = segments[SEG_RAW].length();
        argumentNumbers[offsetNumber] = argumentNumber;

        // now get the format
        Format newFormat = null;
        if (!segments[SEG_TYPE].isEmpty()) {
            int type = findKeyword(segments[SEG_TYPE], TYPE_KEYWORDS);
            switch (type) {
            case TYPE_NULL:
                // Type "" is allowed. e.g., "{0,}", "{0,,}", and "{0,,#}"
                // are treated as "{0}".
                break;

            case TYPE_NUMBER:
                switch (findKeyword(segments[SEG_MODIFIER], NUMBER_MODIFIER_KEYWORDS)) {
                case MODIFIER_DEFAULT:
                    newFormat = NumberFormat.getInstance(locale);
                    break;
                case MODIFIER_CURRENCY:
                    newFormat = NumberFormat.getCurrencyInstance(locale);
                    break;
                case MODIFIER_PERCENT:
                    newFormat = NumberFormat.getPercentInstance(locale);
                    break;
                case MODIFIER_INTEGER:
                    newFormat = NumberFormat.getIntegerInstance(locale);
                    break;
                default: // DecimalFormat pattern
                    try {
                        newFormat = new DecimalFormat(segments[SEG_MODIFIER],
                                                      DecimalFormatSymbols.getInstance(locale));
                    } catch (IllegalArgumentException e) {
                        maxOffset = oldMaxOffset;
                        throw e;
                    }
                    break;
                }
                break;

            case TYPE_DATE:
            case TYPE_TIME:
                int mod = findKeyword(segments[SEG_MODIFIER], DATE_TIME_MODIFIER_KEYWORDS);
                if (mod >= 0 && mod < DATE_TIME_MODIFIER_KEYWORDS.length) {
                    if (type == TYPE_DATE) {
                        newFormat = DateFormat.getDateInstance(DATE_TIME_MODIFIERS[mod],
                                                               locale);
                    } else {
                        newFormat = DateFormat.getTimeInstance(DATE_TIME_MODIFIERS[mod],
                                                               locale);
                    }
                } else {
                    // SimpleDateFormat pattern
                    try {
                        newFormat = new SimpleDateFormat(segments[SEG_MODIFIER], locale);
                    } catch (IllegalArgumentException e) {
                        maxOffset = oldMaxOffset;
                        throw e;
                    }
                }
                break;

            case TYPE_CHOICE:
                try {
                    // ChoiceFormat pattern
                    newFormat = new ChoiceFormat(segments[SEG_MODIFIER]);
                } catch (Exception e) {
                    maxOffset = oldMaxOffset;
                    throw new IllegalArgumentException("Choice Pattern incorrect: "
                                                       + segments[SEG_MODIFIER], e);
                }
                break;

            default:
                maxOffset = oldMaxOffset;
                throw new IllegalArgumentException("unknown format type: " +
                                                   segments[SEG_TYPE]);
            }
        }
        formats[offsetNumber] = newFormat;
    }

    private static final int findKeyword(String s, String[] list) {
        for (int i = 0; i < list.length; ++i) {
            if (s.equals(list[i]))
                return i;
        }

        // Try trimmed lowercase.
        String ls = s.trim().toLowerCase(Locale.ROOT);
        if (ls != s) {
            for (int i = 0; i < list.length; ++i) {
                if (ls.equals(list[i]))
                    return i;
            }
        }
        return -1;
    }

    private static final void copyAndFixQuotes(String source, int start, int end,
                                               StringBuilder target) {
        boolean quoted = false;

        for (int i = start; i < end; ++i) {
            char ch = source.charAt(i);
            if (ch == '{') {
                if (!quoted) {
                    target.append('\'');
                    quoted = true;
                }
                target.append(ch);
            } else if (ch == '\'') {
                target.append("''");
            } else {
                if (quoted) {
                    target.append('\'');
                    quoted = false;
                }
                target.append(ch);
            }
        }
        if (quoted) {
            target.append('\'');
        }
    }

    /**
     * After reading an object from the input stream, do a simple verification
     * to maintain class invariants.
     * @throws InvalidObjectException if the objects read from the stream is invalid.
     */
    @java.io.Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        boolean isValid = maxOffset >= -1
                && formats.length > maxOffset
                && offsets.length > maxOffset
                && argumentNumbers.length > maxOffset;
        if (isValid) {
            int lastOffset = pattern.length() + 1;
            for (int i = maxOffset; i >= 0; --i) {
                if ((offsets[i] < 0) || (offsets[i] > lastOffset)) {
                    isValid = false;
                    break;
                } else {
                    lastOffset = offsets[i];
                }
            }
        }
        if (!isValid) {
            throw new InvalidObjectException("Could not reconstruct MessageFormat from corrupt stream.");
        }
    }
}
