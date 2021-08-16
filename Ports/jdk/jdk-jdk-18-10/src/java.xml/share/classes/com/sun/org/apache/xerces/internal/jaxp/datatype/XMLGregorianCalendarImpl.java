/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.jaxp.datatype;

import com.sun.org.apache.xerces.internal.util.DatatypeMessageFormatter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import jdk.xml.internal.SecuritySupport;

/**
 * <p>Representation for W3C XML Schema 1.0 date/time datatypes.
 * Specifically, these date/time datatypes are
 * {@link DatatypeConstants#DATETIME dateTime},
 * {@link DatatypeConstants#TIME time},
 * {@link DatatypeConstants#DATE date},
 * {@link DatatypeConstants#GYEARMONTH gYearMonth},
 * {@link DatatypeConstants#GMONTHDAY gMonthDay},
 * {@link DatatypeConstants#GYEAR gYear},
 * {@link DatatypeConstants#GMONTH gMonth} and
 * {@link DatatypeConstants#GDAY gDay}
 * defined in the XML Namespace
 * <code>"http://www.w3.org/2001/XMLSchema"</code>.
 * These datatypes are normatively defined in
 * <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">W3C XML Schema 1.0 Part 2, Section 3.2.7-14</a>.</p>
 *
 * <p>The table below defines the mapping between XML Schema 1.0
 * date/time datatype fields and this class' fields. It also summarizes
 * the value constraints for the date and time fields defined in
 * <a href="http://www.w3.org/TR/xmlschema-2/#isoformats">W3C XML Schema 1.0 Part 2, Appendix D,
 * <i>ISO 8601 Date and Time Formats</i></a>.</p>
 *
 * <a name="datetimefieldsmapping"/>
 * <table border="2" rules="all" cellpadding="2">
 *   <thead>
 *     <tr>
 *       <th align="center" colspan="3">
 *         Date/time datatype field mapping between XML Schema 1.0 and Java representation
 *       </th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <th>XML Schema 1.0<br/>
 *           datatype<br/>
 *            field</th>
 *       <th>Related<br/>XMLGregorianCalendar<br/>Accessor(s)</th>
 *       <th>Value Range</th>
 *     </tr>
 *     <a name="datetimefield-year"/>
 *     <tr>
 *       <td> year </td>
 *       <td> {@link #getYear()} + {@link #getEon()} or<br/>
 *            {@link #getEonAndYear}
 *       </td>
 *       <td> <code>getYear()</code> is a value between -(10^9-1) to (10^9)-1
 *            or {@link DatatypeConstants#FIELD_UNDEFINED}.<br/>
 *            {@link #getEon()} is high order year value in billion of years.<br/>
 *            <code>getEon()</code> has values greater than or equal to (10^9) or less than or equal to -(10^9).
 *            A value of null indicates field is undefined.</br>
 *            Given that <a href="http://www.w3.org/2001/05/xmlschema-errata#e2-63">XML Schema 1.0 errata</a> states that the year zero
 *            will be a valid lexical value in a future version of XML Schema,
 *            this class allows the year field to be set to zero. Otherwise,
 *            the year field value is handled exactly as described
 *            in the errata and [ISO-8601-1988]. Note that W3C XML Schema 1.0
 *            validation does not allow for the year field to have a value of zero.
 *            </td>
 *     </tr>
 *     <a name="datetimefield-month"/>
 *     <tr>
 *       <td> month </td>
 *       <td> {@link #getMonth()} </td>
 *       <td> 1 to 12 or {@link DatatypeConstants#FIELD_UNDEFINED} </td>
 *     </tr>
 *     <a name="datetimefield-day"/>
 *     <tr>
 *       <td> day </td>
 *       <td> {@link #getDay()} </td>
 *       <td> Independent of month, max range is 1 to 31 or {@link DatatypeConstants#FIELD_UNDEFINED}.<br/>
 *            The normative value constraint stated relative to month
 *            field's value is in <a href="http://www.w3.org/TR/xmlschema-2/#isoformats">W3C XML Schema 1.0 Part 2, Appendix D</a>.
 *       </td>
 *     </tr>
 *     <a name="datetimefield-hour"/>
 *     <tr>
 *       <td> hour </td>
 *       <td> {@link #getHour()} </td>
 *       <td>
 *         0 to 23 or {@link DatatypeConstants#FIELD_UNDEFINED}.
 *         An hour value of 24 is allowed to be set in the lexical space provided the minute and second
 *         field values are zero. However, an hour value of 24 is not allowed in value space and will be
 *         transformed to represent the value of the first instance of the following day as per
 *         <a href="http://www.w3.org/TR/xmlschema-2/#built-in-primitive-datatypes">
 *         XML Schema Part 2: Datatypes Second Edition, 3.2 Primitive datatypes</a>.
 *       </td>
 *     </tr>
 *     <a name="datetimefield-minute"/>
 *     <tr>
 *       <td> minute </td>
 *       <td> {@link #getMinute()} </td>
 *       <td> 0 to 59 or {@link DatatypeConstants#FIELD_UNDEFINED} </td>
 *     </tr>
 *     <a name="datetimefield-second"/>
 *     <tr>
 *       <td>second</td>
 *       <td>
 *         {@link #getSecond()} + {@link #getMillisecond()}/1000 or<br/>
 *         {@link #getSecond()} + {@link #getFractionalSecond()}
 *       </td>
 *       <td>
 *         {@link #getSecond()} from 0 to 60 or {@link DatatypeConstants#FIELD_UNDEFINED}.<br/>
 *         <i>(Note: 60 only allowable for leap second.)</i><br/>
 *         {@link #getFractionalSecond()} allows for infinite precision over the range from 0.0 to 1.0 when
 *         the {@link #getSecond()} is defined.<br/>
 *         <code>FractionalSecond</code> is optional and has a value of <code>null</code> when it is undefined.<br />
 *            {@link #getMillisecond()} is the convenience
 *            millisecond precision of value of {@link #getFractionalSecond()}.
 *       </td>
 *     </tr>
 *     <tr id="datetimefield-timezone">
 *       <td> timezone </td>
 *       <td> {@link #getTimezone()} </td>
 *       <td> Number of minutes or {@link DatatypeConstants#FIELD_UNDEFINED}.
 *         Value range from -14 hours (-14 * 60 minutes) to 14 hours (14 * 60 minutes).
 *       </td>
 *     </tr>
 *   </tbody>
 *  </table>
 *
 * <p>All maximum value space constraints listed for the fields in the table
 * above are checked by factory methods, setter methods and parse methods of
 * this class. <code>IllegalArgumentException</code> is thrown when
 * parameter's value is outside the maximum value constraint for the field.
 * Validation checks, for example, whether days in month should be
 * limited to 29, 30 or 31 days, that are dependent on the values of other
 * fields are not checked by these methods.
 * </p>
 *
 * <p>The following operations are defined for this class:
 * <ul>
 *   <li>factory methods to create instances</li>
 *   <li>accessors/mutators for independent date/time fields</li>
 *   <li>conversion between this class and W3C XML Schema 1.0 lexical representation</li>
 *   <li>conversion between this class and <code>java.util.GregorianCalendar</code></li>
 *   <li>partial order relation comparator method, {@link #compare(XMLGregorianCalendar)}</li>
 *   <li>{@link #equals(Object)} defined relative to {@link #compare(XMLGregorianCalendar)}.</li>
 *   <li> addition operation with {@link javax.xml.datatype.Duration}.
 * instance as defined in <a href="http://www.w3.org/TR/xmlschema-2/#adding-durations-to-dateTimes">
 * W3C XML Schema 1.0 Part 2, Appendix E, <i>Adding durations to dateTimes</i></a>.</li>
 * </ul>
 * </p>
 *
 * @author Kohsuke Kawaguchi
 * @author Joseph Fialli
 * @author Sunitha Reddy
 * @see javax.xml.datatype.Duration
 * @since 1.5
 * @LastModified: Aug 2020
 */

public class XMLGregorianCalendarImpl
        extends XMLGregorianCalendar
        implements Serializable, Cloneable {

    /** Backup values **/
    transient private BigInteger orig_eon;
    transient private int orig_year = DatatypeConstants.FIELD_UNDEFINED;
    transient private int orig_month = DatatypeConstants.FIELD_UNDEFINED;
    transient private int orig_day = DatatypeConstants.FIELD_UNDEFINED;
    transient private int orig_hour = DatatypeConstants.FIELD_UNDEFINED;
    transient private int orig_minute = DatatypeConstants.FIELD_UNDEFINED;
    transient private int orig_second = DatatypeConstants.FIELD_UNDEFINED;
    transient private BigDecimal orig_fracSeconds;
    transient private int orig_timezone = DatatypeConstants.FIELD_UNDEFINED;

    /**
     * <p>Eon of this <code>XMLGregorianCalendar</code>.</p>
     */
    private BigInteger eon = null;

    /**
     * <p>Year of this <code>XMLGregorianCalendar</code>.</p>
     */
    private int year = DatatypeConstants.FIELD_UNDEFINED;

    /**
     * <p>Month of this <code>XMLGregorianCalendar</code>.</p>
     */
    private int month = DatatypeConstants.FIELD_UNDEFINED;

    /**
     * <p>Day of this <code>XMLGregorianCalendar</code>.</p>
     */
    private int day = DatatypeConstants.FIELD_UNDEFINED;

    /**
     * <p>Timezone of this <code>XMLGregorianCalendar</code> in minutes.</p>
     */
    private int timezone = DatatypeConstants.FIELD_UNDEFINED;

    /**
     * <p>Hour of this <code>XMLGregorianCalendar</code>.</p>
     */
    private int hour = DatatypeConstants.FIELD_UNDEFINED;

    /**
     * <p>Minute of this <code>XMLGregorianCalendar</code>.</p>
     */
    private int minute = DatatypeConstants.FIELD_UNDEFINED;

    /**
     * <p>Second of this <code>XMLGregorianCalendar</code>.</p>
     */
    private int second = DatatypeConstants.FIELD_UNDEFINED ;

    /**
     * <p>Fractional second of this <code>XMLGregorianCalendar</code>.</p>
     */
    private BigDecimal fractionalSecond = null;

    /**
     * <p>BigInteger constant; representing a billion.</p>
     */
    private static final BigInteger BILLION_B = new BigInteger("1000000000");

    /**
     * <p>int constant; representing a billion.</p>
     */
    private static final int BILLION_I = 1000000000;

    /**
     *   <p>Obtain a pure Gregorian Calendar by calling
     *   GregorianCalendar.setChange(PURE_GREGORIAN_CHANGE). </p>
     */
    private static final Date PURE_GREGORIAN_CHANGE =
        new Date(Long.MIN_VALUE);

    /**
     * Year index for MIN_ and MAX_FIELD_VALUES.
     */
    private static final int YEAR   = 0;

    /**
     * Month index for MIN_ and MAX_FIELD_VALUES.
     */
    private static final int MONTH  = 1;

    /**
     * Day index for MIN_ and MAX_FIELD_VALUES.
     */
    private static final int DAY    = 2;

    /**
     * Hour index for MIN_ and MAX_FIELD_VALUES.
     */
    private static final int HOUR   = 3;

    /**
     * Minute index for MIN_ and MAX_FIELD_VALUES.
     */
    private static final int MINUTE = 4;

    /**
     * Second index for MIN_ and MAX_FIELD_VALUES.
     */
    private static final int SECOND = 5;

    /**
     * Second index for MIN_ and MAX_FIELD_VALUES.
     */
    private static final int MILLISECOND = 6;

    /**
     * Timezone index for MIN_ and MAX_FIELD_VALUES
     */
    private static final int TIMEZONE = 7;


    /**
     * field names indexed by YEAR..TIMEZONE.
     */
    private static final String FIELD_NAME[] = {
        "Year",
        "Month",
        "Day",
        "Hour",
        "Minute",
        "Second",
        "Millisecond",
        "Timezone"
    };

    /**
     * <p>Stream Unique Identifier.</p>
     *
     * <p>TODO: Serialization should use the XML string representation as
     * the serialization format to ensure future compatibility.</p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * <p>Use as a template for default field values when
     * converting to a {@link GregorianCalendar}, set to a leap
     * year date of January 1, 0400 at midnight.</p>
     *
     * <p>Fields that are optional for an <code>xsd:dateTime</code> instances are defaulted to not being set to any value.
     * <code>XMLGregorianCalendar</code> fields millisecond, fractional second and timezone return the value indicating
     * that the field is not set, {@link DatatypeConstants#FIELD_UNDEFINED} for millisecond and timezone
     * and <code>null</code> for fractional second.</p>
     *
     * @see #toGregorianCalendar(TimeZone, Locale, XMLGregorianCalendar)
     */
    public static final XMLGregorianCalendar LEAP_YEAR_DEFAULT =
                createDateTime(
                        400,  //year
                DatatypeConstants.JANUARY,  //month
                        1,  // day
                    0,  // hour
                    0,  // minute
                    0,  // second
                    DatatypeConstants.FIELD_UNDEFINED,  // milliseconds
                    DatatypeConstants.FIELD_UNDEFINED // timezone
                );

    // Constructors

    /**
     * Constructs a new XMLGregorianCalendar object.
     *
     * String parsing documented by {@link #parse(String)}.
     *
     * Returns a non-null valid XMLGregorianCalendar object that holds the
     * value indicated by the lexicalRepresentation parameter.
     *
     * @param lexicalRepresentation
     *      Lexical representation of one the eight
     *      XML Schema date/time datatypes.
     * @throws IllegalArgumentException
     *      If the given string does not conform as documented in
     *      {@link #parse(String)}.
     * @throws NullPointerException
     *      If the given string is null.
     */
    protected XMLGregorianCalendarImpl(String lexicalRepresentation)
            throws IllegalArgumentException {

        // compute format string for this lexical representation.
        String format;
        String lexRep = lexicalRepresentation;
        final int NOT_FOUND = -1;
        int lexRepLength = lexRep.length();

        // current parser needs a format string,
        // use following heuristics to figure out what xml schema date/time
        // datatype this lexical string could represent.
        // Fix 4971612: invalid SCCS macro substitution in data string,
        //   no %{alpha}% to avoid SCCS maco substitution
        if (lexRep.indexOf('T') != NOT_FOUND) {
            // found Date Time separater, must be xsd:DateTime
            format = "%Y-%M-%DT%h:%m:%s" + "%z";
        } else if (lexRepLength >= 3 && lexRep.charAt(2) == ':') {
            // found ":", must be xsd:Time
            format = "%h:%m:%s" + "%z";
        } else if (lexRep.startsWith("--")) {
            // check for gDay || gMonth || gMonthDay
            if (lexRepLength >= 3 && lexRep.charAt(2) == '-') {
                // gDay, ---DD(z?)
                format = "---%D" + "%z";
            } else if (lexRepLength == 4     // --MM
                    || lexRepLength == 5     // --MMZ
                    || lexRepLength == 10) { // --MMSHH:MM
                // gMonth, --MM(z?),
                // per XML Schema Errata, used to be --MM--(z?)
                format = "--%M" + "%z";
            } else {
                // gMonthDay, --MM-DD(z?), (or invalid lexicalRepresentation)
                // length should be:
                //  7: --MM-DD
                //  8: --MM-DDZ
                // 13: --MM-DDSHH:MM
                format = "--%M-%D" + "%z";
            }
        } else {
            // check for Date || GYear | GYearMonth
            int countSeparator = 0;

            // start at index 1 to skip potential negative sign for year.


            int timezoneOffset = lexRep.indexOf(':');
            if (timezoneOffset != NOT_FOUND) {

                // found timezone, strip it off for distinguishing
                // between Date, GYear and GYearMonth so possible
                // negative sign in timezone is not mistaken as
                // a separator.
                lexRepLength -= 6;
            }

            for (int i = 1; i < lexRepLength; i++) {
                if (lexRep.charAt(i) == '-') {
                    countSeparator++;
                }
            }
            if (countSeparator == 0) {
                // GYear
                format = "%Y" + "%z";
            } else if (countSeparator == 1) {
                // GYearMonth
                format = "%Y-%M" + "%z";
            } else {
                // Date or invalid lexicalRepresentation
                // Fix 4971612: invalid SCCS macro substitution in data string
                format = "%Y-%M-%D" + "%z";
            }
        }
        Parser p = new Parser(format, lexRep);
        p.parse();

        // check for validity
        if (!isValid()) {
            throw new IllegalArgumentException(
                    DatatypeMessageFormatter.formatMessage(null, "InvalidXGCRepresentation", new Object[]{lexicalRepresentation})
                    //"\"" + lexicalRepresentation + "\" is not a valid representation of an XML Gregorian Calendar value."
            );
        }

        save();
    }

    /**
     * save original values
     */
    private void save() {
        orig_eon = eon;
        orig_year = year;
        orig_month = month;
        orig_day = day;
        orig_hour = hour;
        orig_minute = minute;
        orig_second = second;
        orig_fracSeconds = fractionalSecond;
        orig_timezone = timezone;
    }

    /**
     * <p>Create an instance with all date/time datatype fields set to
     * {@link DatatypeConstants#FIELD_UNDEFINED} or null respectively.</p>
     */
    public XMLGregorianCalendarImpl() {

        // field initializers already do the correct initialization.
    }

    /**
     * <p>Private constructor allowing for complete value spaces allowed by
     * W3C XML Schema 1.0 recommendation for xsd:dateTime and related
     * builtin datatypes. Note that <code>year</code> parameter supports
     * arbitrarily large numbers and fractionalSecond has infinite
     * precision.</p>
     *
     * @param year of <code>XMLGregorianCalendar</code> to be created.
     * @param month of <code>XMLGregorianCalendar</code> to be created.
     * @param day of <code>XMLGregorianCalendar</code> to be created.
     * @param hour of <code>XMLGregorianCalendar</code> to be created.
     * @param minute of <code>XMLGregorianCalendar</code> to be created.
     * @param second of <code>XMLGregorianCalendar</code> to be created.
     * @param fractionalSecond of <code>XMLGregorianCalendar</code> to be created.
     * @param timezone of <code>XMLGregorianCalendar</code> to be created.
     *
     */
    protected XMLGregorianCalendarImpl(
        BigInteger year,
        int month,
        int day,
        int hour,
        int minute,
        int second,
        BigDecimal fractionalSecond,
        int timezone) {

        setYear(year);
        setMonth(month);
        setDay(day);
        setTime(hour, minute, second, fractionalSecond);
        setTimezone(timezone);

        // check for validity
        if (!isValid()) {

            throw new IllegalArgumentException(
                DatatypeMessageFormatter.formatMessage(null,
                    "InvalidXGCValue-fractional",
                    new Object[] { year, month, day,
                    hour, minute, second,
                    fractionalSecond, timezone})
                        );
        }

        save();
    }

    /**
     * <p>Private constructor of value spaces that a
     * <code>java.util.GregorianCalendar</code> instance would need to convert to an
     * <code>XMLGregorianCalendar</code> instance.</p>
     *
     * <p><code>XMLGregorianCalendar eon</code> and
     * <code>fractionalSecond</code> are set to <code>null</code></p>
     *
     * @param year of <code>XMLGregorianCalendar</code> to be created.
     * @param month of <code>XMLGregorianCalendar</code> to be created.
     * @param day of <code>XMLGregorianCalendar</code> to be created.
     * @param hour of <code>XMLGregorianCalendar</code> to be created.
     * @param minute of <code>XMLGregorianCalendar</code> to be created.
     * @param second of <code>XMLGregorianCalendar</code> to be created.
     * @param millisecond of <code>XMLGregorianCalendar</code> to be created.
     * @param timezone of <code>XMLGregorianCalendar</code> to be created.
     */
    private XMLGregorianCalendarImpl(
        int year,
        int month,
        int day,
        int hour,
        int minute,
        int second,
        int millisecond,
        int timezone) {

        setYear(year);
        setMonth(month);
        setDay(day);
        setTime(hour, minute, second);
        setTimezone(timezone);
        BigDecimal realMilliseconds = null;
        if (millisecond != DatatypeConstants.FIELD_UNDEFINED) {
            realMilliseconds = BigDecimal.valueOf(millisecond, 3);
        }
        setFractionalSecond(realMilliseconds);

        if (!isValid()) {

            throw new IllegalArgumentException(
                DatatypeMessageFormatter.formatMessage(null,
                "InvalidXGCValue-milli",
                new Object[] { year, month, day,
                hour, minute, second,
                millisecond, timezone})
                        );
        }

        save();
    }

        /**
         * <p>Convert a <code>java.util.GregorianCalendar</code> to XML Schema 1.0
         * representation.</p>
         *
         * <table border="2" rules="all" cellpadding="2">
         *   <thead>
         *     <tr>
         *       <th align="center" colspan="2">
         *          Field by Field Conversion from
         *          <code>java.util.GregorianCalendar</code> to this class
         *       </th>
         *     </tr>
         *   </thead>
         *   <tbody>
         *     <tr>
         *        <th><code>javax.xml.datatype.XMLGregorianCalendar</code> field</th>
         *        <th><code>java.util.GregorianCalendar</code> field</th>
         *     </tr>
         *     <tr>
         *       <th>{@link #setYear(int)}</th>
         *       <th><code>ERA == GregorianCalendar.BC ? -YEAR : YEAR</code></th>
         *     </tr>
         *     <tr>
         *       <th>{@link #setMonth(int)}</th>
         *       <th><code>MONTH + 1</code></th>
         *     </tr>
         *     <tr>
         *       <th>{@link #setDay(int)}</th>
         *       <th><code>DAY_OF_MONTH</code></th>
         *     </tr>
         *     <tr>
         *       <th>{@link #setTime(int,int,int, BigDecimal)}</th>
         *       <th><code>HOUR_OF_DAY, MINUTE, SECOND, MILLISECOND</code></th>
         *     </tr>
         *     <tr>
         *       <th>{@link #setTimezone(int)}<i>*</i></th>
         *       <th><code>(ZONE_OFFSET + DST_OFFSET) / (60*1000)</code><br/>
         *       <i>(in minutes)</i>
         *       </th>
         *     </tr>
         *   </tbody>
         * </table>
         * <p><i>*</i>conversion loss of information. It is not possible to represent
         * a <code>java.util.GregorianCalendar</code> daylight savings timezone id in the
         * XML Schema 1.0 date/time datatype representation.</p>
         *
         * <p>To compute the return value's <code>TimeZone</code> field,
         * <ul>
         * <li>when <code>this.getTimezone() != DatatypeConstants.FIELD_UNDEFINED</code>,
         * create a <code>java.util.TimeZone</code> with a custom timezone id
         * using the <code>this.getTimezone()</code>.</li>
         * <li>else use the <code>GregorianCalendar</code> default timezone value
         * for the host is defined as specified by
         * <code>java.util.TimeZone.getDefault()</code>.</li></p>
         *
         * @param cal <code>java.util.GregorianCalendar</code> used to create <code>XMLGregorianCalendar</code>
         */
    public XMLGregorianCalendarImpl(GregorianCalendar cal) {

        int year1 = cal.get(Calendar.YEAR);
        if (cal.get(Calendar.ERA) == GregorianCalendar.BC) {
            year1 = -year1;
        }
        this.setYear(year1);

        // Calendar.MONTH is zero based, XSD Date datatype's month field starts
        // with JANUARY as 1.
        this.setMonth(cal.get(Calendar.MONTH) + 1);
        this.setDay(cal.get(Calendar.DAY_OF_MONTH));
        this.setTime(
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND),
                cal.get(Calendar.MILLISECOND));

        // Calendar ZONE_OFFSET and DST_OFFSET fields are in milliseconds.
        int offsetInMinutes = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000);
        this.setTimezone(offsetInMinutes);
        save();
    }

    // Factories

    /**
     * <p>Create a Java representation of XML Schema builtin datatype <code>dateTime</code>.
     * All possible fields are specified for this factory method.</p>
     *
     * @param year represents both high-order eons and low-order year.
     * @param month of <code>dateTime</code>
     * @param day of <code>dateTime</code>
     * @param hours of <code>dateTime</code>
     * @param minutes of <code>dateTime</code>
     * @param seconds of <code>dateTime</code>
     * @param fractionalSecond value of null indicates optional field is absent.
     * @param timezone offset in minutes. {@link DatatypeConstants#FIELD_UNDEFINED} indicates optional field is not set.
     *
     * @return <code>XMLGregorianCalendar</code> created from parameter values.
     *
     * @see DatatypeConstants#FIELD_UNDEFINED
     *
     * @throws IllegalArgumentException if any parameter is outside value
     * constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public static XMLGregorianCalendar createDateTime(
        BigInteger year,
        int month,
        int day,
        int hours,
        int minutes,
        int seconds,
        BigDecimal fractionalSecond,
        int timezone) {

        return new XMLGregorianCalendarImpl(
            year,
            month,
            day,
            hours,
            minutes,
            seconds,
            fractionalSecond,
            timezone);
    }

    /**
     * <p>Create a Java instance of XML Schema builtin datatype dateTime.</p>
     *
     * @param year represents both high-order eons and low-order year.
     * @param month of <code>dateTime</code>
     * @param day of <code>dateTime</code>
     * @param hour of <code>dateTime</code>
     * @param minute of <code>dateTime</code>
     * @param second of <code>dateTime</code>
     *
     * @return <code>XMLGregorianCalendar</code> created from parameter values.
     *
     * @throws IllegalArgumentException if any parameter is outside value constraints for the field as specified in
     *   <a href="#datetimefieldmapping">date/time field mapping table</a>.
     *
     * @see DatatypeConstants#FIELD_UNDEFINED
     */
    public static XMLGregorianCalendar createDateTime(
        int year,
        int month,
        int day,
        int hour,
        int minute,
        int second) {

        return new XMLGregorianCalendarImpl(
            year,
            month,
            day,
            hour,
            minute,
            second,
            DatatypeConstants.FIELD_UNDEFINED,  //millisecond
                DatatypeConstants.FIELD_UNDEFINED //timezone
        );
    }

    /**
     * <p>Create a Java representation of XML Schema builtin datatype <code>dateTime</code>.
     * All possible fields are specified for this factory method.</p>
     *
     * @param year represents low-order year.
     * @param month of <code>dateTime</code>
     * @param day of <code>dateTime</code>
     * @param hours of <code>dateTime</code>
     * @param minutes of <code>dateTime</code>
     * @param seconds of <code>dateTime</code>
     * @param milliseconds of <code>dateTime</code>. {@link DatatypeConstants#FIELD_UNDEFINED} indicates optional field is not set.
     * @param timezone offset in minutes. {@link DatatypeConstants#FIELD_UNDEFINED} indicates optional field is not set.
     *
     * @return <code>XMLGregorianCalendar</code> created from parameter values.
     *
     * @throws IllegalArgumentException if any parameter is outside value constraints for the field as specified in
     *   <a href="#datetimefieldmapping">date/time field mapping table</a>.
     *
     * @see DatatypeConstants#FIELD_UNDEFINED
     */
    public static XMLGregorianCalendar createDateTime(
        int year,
        int month,
        int day,
        int hours,
        int minutes,
        int seconds,
        int milliseconds,
        int timezone) {

        return new XMLGregorianCalendarImpl(
            year,
            month,
            day,
            hours,
            minutes,
            seconds,
            milliseconds,
            timezone);
    }

    /**
     * <p>Create a Java representation of XML Schema builtin datatype <code>date</code> or <code>g*</code>.</p>
     *
     * <p>For example, an instance of <code>gYear</code> can be created invoking this factory
     * with <code>month</code> and <code>day</code> parameters set to
     * {@link DatatypeConstants#FIELD_UNDEFINED}.</p>
     *
     * @param year of <code>XMLGregorianCalendar</code> to be created.
     * @param month of <code>XMLGregorianCalendar</code> to be created.
     * @param day of <code>XMLGregorianCalendar</code> to be created.
     * @param timezone offset in minutes. {@link DatatypeConstants#FIELD_UNDEFINED} indicates optional field is not set.
     *
     * @return <code>XMLGregorianCalendar</code> created from parameter values.
     *
     * @see DatatypeConstants#FIELD_UNDEFINED
     *
     * @throws IllegalArgumentException if any parameter is outside value
     * constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public static XMLGregorianCalendar createDate(
        int year,
        int month,
        int day,
        int timezone) {

        return new XMLGregorianCalendarImpl(
            year,
            month,
            day,
            DatatypeConstants.FIELD_UNDEFINED, // hour
            DatatypeConstants.FIELD_UNDEFINED, // minute
            DatatypeConstants.FIELD_UNDEFINED, // second
                DatatypeConstants.FIELD_UNDEFINED, // millisecond
            timezone);
    }

    /**
     * Create a Java instance of XML Schema builtin datatype <code>time</code>.
     * @param hours number of hours
     * @param minutes number of minutes
     * @param seconds number of seconds
     * @param timezone offset in minutes. {@link DatatypeConstants#FIELD_UNDEFINED} indicates optional field is not set.
     *
     * @return <code>XMLGregorianCalendar</code> created from parameter values.
     *
     * @see DatatypeConstants#FIELD_UNDEFINED
     *
     * @throws IllegalArgumentException if any parameter is outside value
     * constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public static XMLGregorianCalendar createTime(
        int hours,
        int minutes,
        int seconds,
                int timezone) {

                return new XMLGregorianCalendarImpl(
                        DatatypeConstants.FIELD_UNDEFINED, // Year
                        DatatypeConstants.FIELD_UNDEFINED, // Month
                        DatatypeConstants.FIELD_UNDEFINED, // Day
                        hours,
                        minutes,
                        seconds,
                        DatatypeConstants.FIELD_UNDEFINED, //Millisecond
                        timezone);
    }

    /**
     * <p>Create a Java instance of XML Schema builtin datatype time.</p>
     *
     * @param hours number of hours
     * @param minutes number of minutes
     * @param seconds number of seconds
     * @param fractionalSecond value of <code>null</code> indicates that this optional field is not set.
     * @param timezone offset in minutes. {@link DatatypeConstants#FIELD_UNDEFINED} indicates optional field is not set.
     *
     * @return <code>XMLGregorianCalendar</code> created from parameter values.
     *
     * @see DatatypeConstants#FIELD_UNDEFINED
     *
     * @throws IllegalArgumentException if any parameter is outside value
     * constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public static XMLGregorianCalendar createTime(
        int hours,
        int minutes,
        int seconds,
        BigDecimal fractionalSecond,
        int timezone) {

        return new XMLGregorianCalendarImpl(
            null,            // Year
            DatatypeConstants.FIELD_UNDEFINED, // month
            DatatypeConstants.FIELD_UNDEFINED, // day
            hours,
            minutes,
            seconds,
            fractionalSecond,
            timezone);
    }

    /**
     * <p>Create a Java instance of XML Schema builtin datatype time.</p>
     *
     * @param hours number of hours
     * @param minutes number of minutes
     * @param seconds number of seconds
     * @param milliseconds number of milliseconds
     * @param timezone offset in minutes. {@link DatatypeConstants#FIELD_UNDEFINED} indicates optional field is not set.
     *
     * @return <code>XMLGregorianCalendar</code> created from parameter values.
     *
     * @see DatatypeConstants#FIELD_UNDEFINED
     *
     * @throws IllegalArgumentException if any parameter is outside value
     * constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public static XMLGregorianCalendar createTime(
        int hours,
        int minutes,
        int seconds,
        int milliseconds,
        int timezone) {

        return new XMLGregorianCalendarImpl(
                DatatypeConstants.FIELD_UNDEFINED, // year
                DatatypeConstants.FIELD_UNDEFINED, // month
                DatatypeConstants.FIELD_UNDEFINED, // day
                hours,
                minutes,
                seconds,
                milliseconds,
                timezone);
    }

    // Accessors

    /**
     * <p>Return high order component for XML Schema 1.0 dateTime datatype field for
     * <code>year</code>.
     * <code>null</code> if this optional part of the year field is not defined.</p>
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-year">year field of date/time field mapping table</a>.</p>
     * @return eon of this <code>XMLGregorianCalendar</code>. The value
     * returned is an integer multiple of 10^9.
     *
     * @see #getYear()
     * @see #getEonAndYear()
     */
    public BigInteger getEon() {
           return eon;
    }

    /**
     * <p>Return low order component for XML Schema 1.0 dateTime datatype field for
     * <code>year</code> or {@link DatatypeConstants#FIELD_UNDEFINED}.</p>
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-year">year field of date/time field mapping table</a>.</p>
     *
     * @return year  of this <code>XMLGregorianCalendar</code>.
     *
     * @see #getEon()
     * @see #getEonAndYear()
     */
    public int getYear() {
           return year;
    }

    /**
     * <p>Return XML Schema 1.0 dateTime datatype field for
     * <code>year</code>.</p>
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-year">year field of date/time field mapping table</a>.</p>
     *
     * @return sum of <code>eon</code> and <code>BigInteger.valueOf(year)</code>
     * when both fields are defined. When only <code>year</code> is defined,
     * return it. When both <code>eon</code> and <code>year</code> are not
     * defined, return <code>null</code>.
     *
     * @see #getEon()
     * @see #getYear()
     */
    public BigInteger getEonAndYear() {

                // both are defined
                if (year != DatatypeConstants.FIELD_UNDEFINED
                        && eon != null) {

                        return eon.add(BigInteger.valueOf((long) year));
                }

                // only year is defined
                if (year != DatatypeConstants.FIELD_UNDEFINED
                        && eon == null) {

                        return BigInteger.valueOf((long) year);
                }

        // neither are defined
        // or only eon is defined which is not valid without a year
                return null;
    }

    /**
     * <p>Return number of month or {@link DatatypeConstants#FIELD_UNDEFINED}.</p>
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-month">month field of date/time field mapping table</a>.</p>
     *
     * @return year  of this <code>XMLGregorianCalendar</code>.
     *
     */
    public int getMonth() {
        return month;
    }

    /**
     * Return day in month or {@link DatatypeConstants#FIELD_UNDEFINED}.</p>
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-day">day field of date/time field mapping table</a>.</p>
     *
     * @see #setDay(int)
     */
    public int getDay() {
        return day;
    }

    /**
     * Return timezone offset in minutes or
     * {@link DatatypeConstants#FIELD_UNDEFINED} if this optional field is not defined.
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-timezone">timezone field of date/time field mapping table</a>.</p>
     *
     * @see #setTimezone(int)
     */
    public int getTimezone() {
        return timezone;
    }

    /**
     * Return hours or {@link DatatypeConstants#FIELD_UNDEFINED}.
     * Returns {@link DatatypeConstants#FIELD_UNDEFINED} if this field is not defined.
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-hour">hour field of date/time field mapping table</a>.</p>
     * @see #setTime(int, int, int)
     */
    public int getHour() {
        return hour;
    }

    /**
     * Return minutes or {@link DatatypeConstants#FIELD_UNDEFINED}.<\p>
     * Returns {@link DatatypeConstants#FIELD_UNDEFINED} if this field is not defined.
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-minute">minute field of date/time field mapping table</a>.</p>
     * @see #setTime(int, int, int)
     */
    public int getMinute() {
        return minute;
    }

    /**
     * <p>Return seconds or {@link DatatypeConstants#FIELD_UNDEFINED}.<\p>
     *
     * <p>Returns {@link DatatypeConstants#FIELD_UNDEFINED} if this field is not defined.
     * When this field is not defined, the optional xs:dateTime
     * fractional seconds field, represented by
     * {@link #getFractionalSecond()} and {@link #getMillisecond()},
     * must not be defined.</p>
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-second">second field of date/time field mapping table</a>.</p>
     *
     * @return Second  of this <code>XMLGregorianCalendar</code>.
     *
     * @see #getFractionalSecond()
     * @see #getMillisecond()
     * @see #setTime(int, int, int)
     */
    public int getSecond() {
           return second;
    }

    /**
     * @return result of adding second and fractional second field
     */
    private BigDecimal getSeconds() {
        if (second == DatatypeConstants.FIELD_UNDEFINED) {
            return DECIMAL_ZERO;
        }
        BigDecimal result = BigDecimal.valueOf((long) second);
        if (fractionalSecond != null) {
            return result.add(fractionalSecond);
        } else {
            return result;
        }
    }


    /**
     * <p>Return millisecond precision of {@link #getFractionalSecond()}.<\p>
     *
     * <p>This method represents a convenience accessor to infinite
     * precision fractional second value returned by
     * {@link #getFractionalSecond()}. The returned value is the rounded
     * down to milliseconds value of
     * {@link #getFractionalSecond()}. When {@link #getFractionalSecond()}
     * returns <code>null</code>, this method must return
     * {@link DatatypeConstants#FIELD_UNDEFINED}.</p>
     *
     * <p>Value constraints for this value are summarized in
     * <a href="#datetimefield-second">second field of date/time field mapping table</a>.</p>
     *
     * @return Millisecond  of this <code>XMLGregorianCalendar</code>.
     *
     * @see #getFractionalSecond()
     * @see #setTime(int, int, int)
     */
    public int getMillisecond() {
        if (fractionalSecond == null) {
            return DatatypeConstants.FIELD_UNDEFINED;
        } else {
            // TODO: Non-optimal solution for now.
            // Efficient implementation would only store as BigDecimal
            // when needed and millisecond otherwise.
            return fractionalSecond.movePointRight(3).intValue();
        }
    }

    /**
     * <p>Return fractional seconds.</p>
     *
     * <p><code>null</code> is returned when this optional field is not defined.</p>
     *
     * <p>Value constraints are detailed in
     * <a href="#datetimefield-second">second field of date/time field mapping table</a>.</p>
     *
     * <p>This optional field can only have a defined value when the
     * xs:dateTime second field, represented by ({@link #getSecond()},
     * does not return {@link DatatypeConstants#FIELD_UNDEFINED}).</p>
     *
     * @return fractional seconds  of this <code>XMLGregorianCalendar</code>.
     *
     * @see #getSecond()
     * @see #setTime(int, int, int, BigDecimal)
     */
    public BigDecimal getFractionalSecond() {
           return fractionalSecond;
    }

    // setters

    /**
     * <p>Set low and high order component of XSD <code>dateTime</code> year field.</p>
     *
     * <p>Unset this field by invoking the setter with a parameter value of <code>null</code>.</p>
     *
     * @param year value constraints summarized in <a href="#datetimefield-year">year field of date/time field mapping table</a>.
     *
     * @throws IllegalArgumentException if <code>year</code> parameter is
     * outside value constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public final void setYear(BigInteger year) {
        if (year == null) {
            this.eon = null;
            this.year = DatatypeConstants.FIELD_UNDEFINED;
        } else {
            BigInteger temp = year.remainder(BILLION_B);
            this.year = temp.intValue();
            setEon(year.subtract(temp));
        }
    }

    /**
     * <p>Set year of XSD <code>dateTime</code> year field.</p>
     *
     * <p>Unset this field by invoking the setter with a parameter value of
     * {@link DatatypeConstants#FIELD_UNDEFINED}.</p>
     *
     * <p>Note: if the absolute value of the <code>year</code> parameter
     * is less than 10^9, the eon component of the XSD year field is set to
     * <code>null</code> by this method.</p>
     *
     * @param year value constraints are summarized in <a href="#datetimefield-year">year field of date/time field mapping table</a>.
     *   If year is {@link DatatypeConstants#FIELD_UNDEFINED}, then eon is set to <code>null</code>.
     */
    public final void setYear(int year) {
        if (year == DatatypeConstants.FIELD_UNDEFINED) {
            this.year = DatatypeConstants.FIELD_UNDEFINED;
            this.eon = null;
        }
        else if (Math.abs(year) < BILLION_I) {
            this.year = year;
            this.eon = null;
        } else {
            BigInteger theYear = BigInteger.valueOf((long) year);
            BigInteger remainder = theYear.remainder(BILLION_B);
            this.year = remainder.intValue();
            setEon(theYear.subtract(remainder));
        }
    }

    /**
     * <p>Set high order part of XSD <code>dateTime</code> year field.</p>
     *
     * <p>Unset this field by invoking the setter with a parameter value of
     * <code>null</code>.</p>
     *
     * @param eon value constraints summarized in <a href="#datetimefield-year">year field of date/time field mapping table</a>.
     */
    private void setEon(BigInteger eon) {
        if (eon != null && eon.compareTo(BigInteger.ZERO) == 0) {
            // Treat ZERO as field being undefined.
            this.eon = null;
        } else {
            this.eon = eon;
        }
    }

    /**
     * <p>Set month.</p>
     *
     * <p>Unset this field by invoking the setter with a parameter value of {@link DatatypeConstants#FIELD_UNDEFINED}.</p>
     *
     * @param month value constraints summarized in <a href="#datetimefield-month">month field of date/time field mapping table</a>.
     *
     * @throws IllegalArgumentException if <code>month</code> parameter is
     * outside value constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public final void setMonth(int month) {
        if(month<DatatypeConstants.JANUARY || DatatypeConstants.DECEMBER<month)
            if(month!=DatatypeConstants.FIELD_UNDEFINED)
                invalidFieldValue(MONTH, month);
        this.month = month;
    }

    /**
     * <p>Set days in month.</p>
     *
     * <p>Unset this field by invoking the setter with a parameter value of {@link DatatypeConstants#FIELD_UNDEFINED}.</p>
     *
     * @param day value constraints summarized in <a href="#datetimefield-day">day field of date/time field mapping table</a>.
     *
     * @throws IllegalArgumentException if <code>day</code> parameter is
     * outside value constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public final void setDay(int day) {
        if(day<1 || 31<day)
            if(day!=DatatypeConstants.FIELD_UNDEFINED)
                invalidFieldValue(DAY,day);
        this.day = day;
    }

    /**
     * <p>Set the number of minutes in the timezone offset.</p>
     *
     * <p>Unset this field by invoking the setter with a parameter value of {@link DatatypeConstants#FIELD_UNDEFINED}.</p>
     *
     * @param offset value constraints summarized in <a href="#datetimefield-timezone">
     *   timezone field of date/time field mapping table</a>.
     *
     * @throws IllegalArgumentException if <code>offset</code> parameter is
     * outside value constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public final void setTimezone(int offset) {
            if(offset<-14*60 || 14*60<offset)
            if(offset!=DatatypeConstants.FIELD_UNDEFINED)
                invalidFieldValue(TIMEZONE,offset);
        this.timezone = offset;
    }

    /**
     * <p>Set time as one unit.</p>
     *
     * @param hour value constraints are summarized in
     * <a href="#datetimefield-hour">hour field of date/time field mapping table</a>.
     * @param minute value constraints are summarized in
     * <a href="#datetimefield-minute">minute field of date/time field mapping table</a>.
     * @param second value constraints are summarized in
     * <a href="#datetimefield-second">second field of date/time field mapping table</a>.
     *
     * @see #setTime(int, int, int, BigDecimal)
     *
     * @throws IllegalArgumentException if any parameter is
     * outside value constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public final void setTime(int hour, int minute, int second) {
        setTime(hour, minute, second, null);
    }

    private void invalidFieldValue(int field, int value) {
        throw new IllegalArgumentException(
            DatatypeMessageFormatter.formatMessage(null, "InvalidFieldValue",
                new Object[]{ value, FIELD_NAME[field]})
        );
    }

    private void testHour() {

        // http://www.w3.org/2001/05/xmlschema-errata#e2-45
        if (getHour() == 24) {
            if (getMinute() != 0
                    || getSecond() != 0) {
                invalidFieldValue(HOUR, getHour());
            }
            // while 0-24 is acceptable in the lexical space, 24 is not valid in value space
            // W3C XML Schema Part 2, Section 3.2.7.1
            setHour(0, false);
            add(new DurationImpl(true, 0, 0, 1, 0, 0, 0));
        }
    }

    public void setHour(int hour) {

        setHour(hour, true);
    }

    private void setHour(int hour, boolean validate) {

        if (hour < 0 || hour > 24) {
            if (hour != DatatypeConstants.FIELD_UNDEFINED) {
                invalidFieldValue(HOUR, hour);
            }
        }

        this.hour = hour;

        if (validate) {
            testHour();
        }
    }

    public void setMinute(int minute) {
        if(minute<0 || 59<minute)
            if(minute!=DatatypeConstants.FIELD_UNDEFINED)
                invalidFieldValue(MINUTE, minute);
        this.minute = minute;
    }

    public void setSecond(int second) {
        if(second<0 || 60<second)   // leap second allows for 60
            if(second!=DatatypeConstants.FIELD_UNDEFINED)
                invalidFieldValue(SECOND, second);
        this.second  = second;
    }

    /**
     * <p>Set time as one unit, including the optional infinite precison
     * fractional seconds.</p>
     *
     * @param hour value constraints are summarized in
     * <a href="#datetimefield-hour">hour field of date/time field mapping table</a>.
     * @param minute value constraints are summarized in
     * <a href="#datetimefield-minute">minute field of date/time field mapping table</a>.
     * @param second value constraints are summarized in
     * <a href="#datetimefield-second">second field of date/time field mapping table</a>.
     * @param fractional value of <code>null</code> indicates this optional
     *                   field is not set.
     *
     * @throws IllegalArgumentException if any parameter is
     * outside value constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public final void setTime(
            int hour,
            int minute,
            int second,
            BigDecimal fractional) {

        setHour(hour, false);

        setMinute(minute);
        if (second != 60) {
            setSecond(second);
        } else if ((hour == 23 && minute == 59) || (hour == 0 && minute == 0)) {
            setSecond(second);
        } else {
            invalidFieldValue(SECOND, second);
        }

        setFractionalSecond(fractional);

        // must test hour after setting seconds
        testHour();
    }


    /**
     * <p>Set time as one unit, including optional milliseconds.</p>
     *
     * @param hour value constraints are summarized in
     * <a href="#datetimefield-hour">hour field of date/time field mapping table</a>.
     * @param minute value constraints are summarized in
     * <a href="#datetimefield-minute">minute field of date/time field mapping table</a>.
     * @param second value constraints are summarized in
     * <a href="#datetimefield-second">second field of date/time field mapping table</a>.
     * @param millisecond value of {@link DatatypeConstants#FIELD_UNDEFINED} indicates this
     *                    optional field is not set.
     *
     * @throws IllegalArgumentException if any parameter is
     * outside value constraints for the field as specified in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.
     */
    public final void setTime(int hour, int minute, int second, int millisecond) {

        setHour(hour, false);

        setMinute(minute);
        if (second != 60) {
            setSecond(second);
        } else if ((hour == 23 && minute == 59) || (hour == 0 && minute == 0)) {
            setSecond(second);
        } else {
            invalidFieldValue(SECOND, second);
        }
        setMillisecond(millisecond);

        // must test hour after setting seconds
        testHour();
    }

    // comparisons
    /**
     * <p>Compare two instances of W3C XML Schema 1.0 date/time datatypes
     * according to partial order relation defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/#dateTime-order">W3C XML Schema 1.0 Part 2, Section 3.2.7.3,
     * <i>Order relation on dateTime</i></a>.</p>
     *
     * <p><code>xsd:dateTime</code> datatype field mapping to accessors of
     * this class are defined in
     * <a href="#datetimefieldmapping">date/time field mapping table</a>.</p>
     *
     * @param rhs instance of <code>XMLGregorianCalendar</code> to compare
     *
     * @return the relationship between <code>lhs</code> and <code>rhs</code> as
     *   {@link DatatypeConstants#LESSER},
     *   {@link DatatypeConstants#EQUAL},
     *   {@link DatatypeConstants#GREATER} or
     *   {@link DatatypeConstants#INDETERMINATE}.
     *
     * @throws NullPointerException if <code>lhs</code> or <code>rhs</code>
     * parameters are null.
     */
    public int compare(XMLGregorianCalendar rhs) {

        XMLGregorianCalendar lhs = this;

        int result = DatatypeConstants.INDETERMINATE;
        XMLGregorianCalendarImpl P = (XMLGregorianCalendarImpl) lhs;
        XMLGregorianCalendarImpl Q = (XMLGregorianCalendarImpl) rhs;

        if (P.getTimezone() == Q.getTimezone()) {
            // Optimization:
            // both instances are in same timezone or
            // both are FIELD_UNDEFINED.
            // Avoid costly normalization of timezone to 'Z' time.
            return internalCompare(P, Q);

        } else if (P.getTimezone() != DatatypeConstants.FIELD_UNDEFINED &&
                Q.getTimezone() != DatatypeConstants.FIELD_UNDEFINED) {

            // Both instances have different timezones.
            // Normalize to UTC time and compare.
            P = (XMLGregorianCalendarImpl) P.normalize();
            Q = (XMLGregorianCalendarImpl) Q.normalize();
            return internalCompare(P, Q);
        } else if (P.getTimezone() != DatatypeConstants.FIELD_UNDEFINED) {

            if (P.getTimezone() != 0) {
                P = (XMLGregorianCalendarImpl) P.normalize();
            }

            // C. step 1
            XMLGregorianCalendar MinQ = Q.normalizeToTimezone(DatatypeConstants.MIN_TIMEZONE_OFFSET);
            result = internalCompare(P, MinQ);
            if (result == DatatypeConstants.LESSER) {
                return result;
            }

            // C. step 2
            XMLGregorianCalendar MaxQ = Q.normalizeToTimezone(DatatypeConstants.MAX_TIMEZONE_OFFSET);
            result = internalCompare(P, MaxQ);
            if (result == DatatypeConstants.GREATER) {
                return result;
            } else {
                // C. step 3
                return DatatypeConstants.INDETERMINATE;
            }
        } else { // Q.getTimezone() != DatatypeConstants.FIELD_UNDEFINED
            // P has no timezone and Q does.
            if (Q.getTimezone() != 0) {
                Q = (XMLGregorianCalendarImpl) Q.normalizeToTimezone(Q.getTimezone());
            }

            // D. step 1
            XMLGregorianCalendar MaxP = P.normalizeToTimezone(DatatypeConstants.MAX_TIMEZONE_OFFSET);
            result = internalCompare(MaxP, Q);
            if (result == DatatypeConstants.LESSER) {
                return result;
            }

            // D. step 2
            XMLGregorianCalendar MinP = P.normalizeToTimezone(DatatypeConstants.MIN_TIMEZONE_OFFSET);
            result = internalCompare(MinP, Q);
            if (result == DatatypeConstants.GREATER) {
                return result;
            } else {
                // D. step 3
                return DatatypeConstants.INDETERMINATE;
            }
        }
    }

    /**
     * <p>Normalize this instance to UTC.</p>
     *
     * <p>2000-03-04T23:00:00+03:00 normalizes to 2000-03-04T20:00:00Z</p>
     * <p>Implements W3C XML Schema Part 2, Section 3.2.7.3 (A).</p>
     */
    public XMLGregorianCalendar normalize() {

        XMLGregorianCalendar normalized = normalizeToTimezone(timezone);

        // if timezone was undefined, leave it undefined
        if (getTimezone() == DatatypeConstants.FIELD_UNDEFINED) {
            normalized.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
        }

        // if milliseconds was undefined, leave it undefined
        if (getMillisecond() == DatatypeConstants.FIELD_UNDEFINED) {
            normalized.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        }

        return normalized;
    }

        /**
         * <p>Normalize this instance to UTC.</p>
         *
         * <p>2000-03-04T23:00:00+03:00 normalizes to 2000-03-04T20:00:00Z</p>
         * <p>Implements W3C XML Schema Part 2, Section 3.2.7.3 (A).</p>
         */
    private XMLGregorianCalendar normalizeToTimezone(int timezone) {

        int minutes = timezone;
        XMLGregorianCalendar result = (XMLGregorianCalendar) this.clone();

        // normalizing to UTC time negates the timezone offset before
        // addition.
        minutes = -minutes;
        Duration d = new DurationImpl(minutes >= 0, // isPositive
                0, //years
                0, //months
                0, //days
                0, //hours
                minutes < 0 ? -minutes : minutes, // absolute
                0  //seconds
        );
        result.add(d);

        // set to zulu UTC time.
        result.setTimezone(0);
        return result;
    }

    /**
     *
     *  <p>Implements Step B from http://www.w3.org/TR/xmlschema-2/#dateTime-order </p>
     * @param P calendar instance with normalized timezone offset or
     *          having same timezone as Q
     * @param Q calendar instance with normalized timezone offset or
     *          having same timezone as P
     *
     * @return result of comparing P and Q, value of
     *   {@link DatatypeConstants#EQUAL},
     *   {@link DatatypeConstants#LESSER},
     *   {@link DatatypeConstants#GREATER} or
     *   {@link DatatypeConstants#INDETERMINATE}.
     */
    private static int internalCompare(XMLGregorianCalendar P,
                                       XMLGregorianCalendar Q) {

        int result;

        // compare Year.
        if (P.getEon() == Q.getEon()) {

            // Eon field is only equal when null.
            // optimized case for comparing year not requiring eon field.
            result = compareField(P.getYear(), Q.getYear());
            if (result != DatatypeConstants.EQUAL) {
                return result;
            }
        } else {
            result = compareField(P.getEonAndYear(), Q.getEonAndYear());
            if (result != DatatypeConstants.EQUAL) {
                return result;
            }
        }

        result = compareField(P.getMonth(), Q.getMonth());
        if (result != DatatypeConstants.EQUAL) {
            return result;
        }

        result = compareField(P.getDay(), Q.getDay());
        if (result != DatatypeConstants.EQUAL) {
            return result;
        }

        result = compareField(P.getHour(), Q.getHour());
        if (result != DatatypeConstants.EQUAL) {
            return result;
        }

        result = compareField(P.getMinute(), Q.getMinute());
        if (result != DatatypeConstants.EQUAL) {
            return result;
        }
        result = compareField(P.getSecond(), Q.getSecond());
        if (result != DatatypeConstants.EQUAL) {
            return result;
        }

        result = compareField(P.getFractionalSecond(), Q.getFractionalSecond());
        return result;
    }

    /**
     * <p>Implement Step B from
     * http://www.w3.org/TR/xmlschema-2/#dateTime-order.</p>
     */
    private static int compareField(int Pfield, int Qfield) {
        if (Pfield == Qfield) {

            //fields are either equal in value or both undefined.
            // Step B. 1.1 AND optimized result of performing 1.1-1.4.
            return DatatypeConstants.EQUAL;
        } else {
            if (Pfield == DatatypeConstants.FIELD_UNDEFINED || Qfield == DatatypeConstants.FIELD_UNDEFINED) {
                // Step B. 1.2
                return DatatypeConstants.INDETERMINATE;
            } else {
                // Step B. 1.3-4.
                return (Pfield < Qfield ? DatatypeConstants.LESSER : DatatypeConstants.GREATER);
            }
        }
    }

    private static int compareField(BigInteger Pfield, BigInteger Qfield) {
        if (Pfield == null) {
            return (Qfield == null ? DatatypeConstants.EQUAL : DatatypeConstants.INDETERMINATE);
        }
        if (Qfield == null) {
            return DatatypeConstants.INDETERMINATE;
        }
        return Pfield.compareTo(Qfield);
    }

    private static int compareField(BigDecimal Pfield, BigDecimal Qfield) {
        // optimization. especially when both arguments are null.
        if (Pfield == Qfield) {
            return DatatypeConstants.EQUAL;
        }

        if (Pfield == null) {
            Pfield = DECIMAL_ZERO;
        }

        if (Qfield == null) {
            Qfield = DECIMAL_ZERO;
        }

        return Pfield.compareTo(Qfield);
    }

    /**
     * <p>Constructs a new XMLGregorianCalendar object by
     * parsing its lexical string representation as defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/#dateTime-order">XML Schema 1.0 Part 2, Section 3.2.[7-14].1,
     * <i>Lexical Representation</i>.</a></p>
     *
     * <p>The string representation may not have any leading and trailing whitespaces.</p>
     *
     * <p>The parsing is done field by field so that
     * the following holds for any lexically correct string x:</p>
     * <pre>
     * new XMLGregorianCalendar(x).toXMLFormat().equals(x)
     * </pre>
     * Except for the noted lexical/canonical representation mismatches
     * listed in <a href="http://www.w3.org/2001/05/xmlschema-errata#e2-45">
     * XML Schema 1.0 errata, Section 3.2.7.2</a>.
     *
     * <p>Returns a non-null valid XMLGregorianCalendar object that holds the value
     * indicated by the lexicalRepresentation parameter.</p>
     *
     * @param lexicalRepresentation Lexical representation of one the 8 XML Schema calendar datatypes.
     *
     * @return <code>XMLGregorianCalendar</code> created from parsing <code>lexicalRepresentation</code> parameter.
     *
     * @throws IllegalArgumentException
     *      If the given string does not conform to the aforementioned
     *      specification.
     * @throws NullPointerException
     *      If the given string is null.
     */
    public static XMLGregorianCalendar parse(String lexicalRepresentation) {

                return new XMLGregorianCalendarImpl(lexicalRepresentation);
    }

    /**
     * <p>Return the lexical representation of <code>this</code> instance.
     * The format is specified in
     * <a href="http://www.w3.org/TR/xmlschema-2/#dateTime-order">XML Schema 1.0 Part 2, Section 3.2.[7-14].1,
     * <i>Lexical Representation</i>".</a></p>
     *
     * <p>Specific target lexical representation format is determined by
     * {@link #getXMLSchemaType()}.</p>
     *
     * @return XML, as <code>String</code>, representation of this <code>XMLGregorianCalendar</code>
     *
     * @throws java.lang.IllegalStateException if the combination of set fields
     *    does not match one of the eight defined XML Schema builtin date/time datatypes.
     */
    public String toXMLFormat() {

        QName typekind = getXMLSchemaType();

        String formatString = null;
        // Fix 4971612: invalid SCCS macro substitution in data string
        //   no %{alpha}% to avoid SCCS macro substitution
        if (typekind == DatatypeConstants.DATETIME) {
            formatString = "%Y-%M-%DT%h:%m:%s" + "%z";
        } else if (typekind == DatatypeConstants.DATE) {
            formatString = "%Y-%M-%D" + "%z";
        } else if (typekind == DatatypeConstants.TIME) {
            formatString = "%h:%m:%s" + "%z";
        } else if (typekind == DatatypeConstants.GMONTH) {
            formatString = "--%M" + "%z";
        } else if (typekind == DatatypeConstants.GDAY) {
            formatString = "---%D" + "%z";
        } else if (typekind == DatatypeConstants.GYEAR) {
            formatString = "%Y" + "%z";
        } else if (typekind == DatatypeConstants.GYEARMONTH) {
            formatString = "%Y-%M" + "%z";
        } else if (typekind == DatatypeConstants.GMONTHDAY) {
            formatString = "--%M-%D" + "%z";
        }
        return format(formatString);
    }

    /**
     * <p>Return the name of the XML Schema date/time type that this instance
     * maps to. Type is computed based on fields that are set.</p>
     *
     * <table border="2" rules="all" cellpadding="2">
     *   <thead>
     *     <tr>
     *       <th align="center" colspan="7">
     *         Required fields for XML Schema 1.0 Date/Time Datatypes.<br/>
     *         <i>(timezone is optional for all date/time datatypes)</i>
     *       </th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *       <td>Datatype</td>
     *       <td>year</td>
     *       <td>month</td>
     *       <td>day</td>
     *       <td>hour</td>
     *       <td>minute</td>
     *       <td>second</td>
     *     </tr>
     *     <tr>
     *       <td>{@link DatatypeConstants#DATETIME}</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *     </tr>
     *     <tr>
     *       <td>{@link DatatypeConstants#DATE}</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *     </tr>
     *     <tr>
     *       <td>{@link DatatypeConstants#TIME}</td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *     </tr>
     *     <tr>
     *       <td>{@link DatatypeConstants#GYEARMONTH}</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *     </tr>
     *     <tr>
     *       <td>{@link DatatypeConstants#GMONTHDAY}</td>
     *       <td></td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *     </tr>
     *     <tr>
     *       <td>{@link DatatypeConstants#GYEAR}</td>
     *       <td>X</td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *     </tr>
     *     <tr>
     *       <td>{@link DatatypeConstants#GMONTH}</td>
     *       <td></td>
     *       <td>X</td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *     </tr>
     *     <tr>
     *       <td>{@link DatatypeConstants#GDAY}</td>
     *       <td></td>
     *       <td></td>
     *       <td>X</td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *     </tr>
     *   </tbody>
     * </table>
     *
     * @throws java.lang.IllegalStateException if the combination of set fields
     *    does not match one of the eight defined XML Schema builtin
     *    date/time datatypes.
     * @return One of the following class constants:
     *   {@link DatatypeConstants#DATETIME},
     *   {@link DatatypeConstants#TIME},
     *   {@link DatatypeConstants#DATE},
     *   {@link DatatypeConstants#GYEARMONTH},
     *   {@link DatatypeConstants#GMONTHDAY},
     *   {@link DatatypeConstants#GYEAR},
     *   {@link DatatypeConstants#GMONTH} or
     *   {@link DatatypeConstants#GDAY}.
     */
    public QName getXMLSchemaType() {

        int mask =
            (year != DatatypeConstants.FIELD_UNDEFINED ?   0x20 : 0 )|
            (month != DatatypeConstants.FIELD_UNDEFINED ?  0x10 : 0 )|
            (day != DatatypeConstants.FIELD_UNDEFINED ?    0x08 : 0 )|
            (hour != DatatypeConstants.FIELD_UNDEFINED ?   0x04 : 0 )|
            (minute != DatatypeConstants.FIELD_UNDEFINED ? 0x02 : 0 )|
            (second != DatatypeConstants.FIELD_UNDEFINED ? 0x01 : 0 );

        switch(mask) {
        case 0x3F:
                return DatatypeConstants.DATETIME;
        case 0x38:
                return DatatypeConstants.DATE;
        case 0x07:
                return DatatypeConstants.TIME;
        case 0x30:
                return DatatypeConstants.GYEARMONTH;
        case 0x18:
                return DatatypeConstants.GMONTHDAY;
        case 0x20:
                return DatatypeConstants.GYEAR;
        case 0x10:
                return DatatypeConstants.GMONTH;
        case 0x08:
                return DatatypeConstants.GDAY;
        default:
            throw new IllegalStateException(
                this.getClass().getName()
                + "#getXMLSchemaType() :"
                + DatatypeMessageFormatter.formatMessage(null, "InvalidXGCFields", null)
            );
        }
    }


    /**
     * Validate instance by <code>getXMLSchemaType()</code> constraints.
     * @return true if data values are valid.
     */
    public final boolean isValid() {
        // since setters do not allow for invalid values,
        // (except for exceptional case of year field of zero),
        // no need to check for anything except for constraints
        // between fields.

        // check if days in month is valid. Can be dependent on leap year.
        if (month != DatatypeConstants.FIELD_UNDEFINED && day != DatatypeConstants.FIELD_UNDEFINED) {
            if (year != DatatypeConstants.FIELD_UNDEFINED) {
                if (eon == null) {
                    if (day > maximumDayInMonthFor(year, month)) {
                        return false;
                    }
                }
                else if (day > maximumDayInMonthFor(getEonAndYear(), month)) {
                    return false;
                }
            }
            // Use 2000 as a default since it's a leap year.
            else if (day > maximumDayInMonthFor(2000, month)) {
                return false;
            }
        }

        // http://www.w3.org/2001/05/xmlschema-errata#e2-45
        if (hour == 24 && (minute != 0 || second != 0 ||
                (fractionalSecond != null && fractionalSecond.compareTo(DECIMAL_ZERO) != 0))) {
            return false;
        }

        // XML Schema 1.0 specification defines year value of zero as
        // invalid. Allow this class to set year field to zero
        // since XML Schema 1.0 errata states that lexical zero will
        // be allowed in next version and treated as 1 B.C.E.
        if (eon == null && year == 0) {
            return false;
        }
        return true;
    }

    /**
     * <p>Add <code>duration</code> to this instance.<\p>
     *
     * <p>The computation is specified in
     * <a href="http://www.w3.org/TR/xmlschema-2/#adding-durations-to-dateTimes">XML Schema 1.0 Part 2, Appendix E,
     * <i>Adding durations to dateTimes</i>></a>.
     * <a href="#datetimefieldsmapping">date/time field mapping table</a>
     * defines the mapping from XML Schema 1.0 <code>dateTime</code> fields
     * to this class' representation of those fields.</p>
     *
     * @param duration Duration to add to this <code>XMLGregorianCalendar</code>.
     *
     * @throws NullPointerException  when <code>duration</code> parameter is <code>null</code>.
     */
    public void add(Duration duration) {

        /*
           * Extracted from
           * http://www.w3.org/TR/xmlschema-2/#adding-durations-to-dateTimes
           * to ensure implemented properly. See spec for definitions of methods
           * used in algorithm.
           *
           * Given a dateTime S and a duration D, specifies how to compute a
           * dateTime E where E is the end of the time period with start S and
           * duration D i.e. E = S + D.
           *
           * The following is the precise specification.
           * These steps must be followed in the same order.
           * If a field in D is not specified, it is treated as if it were zero.
           * If a field in S is not specified, it is treated in the calculation
           * as if it were the minimum allowed value in that field, however,
           * after the calculation is concluded, the corresponding field in
           * E is removed (set to unspecified).
           *
           * Months (may be modified additionally below)
               *  temp := S[month] + D[month]
               *  E[month] := modulo(temp, 1, 13)
               *  carry := fQuotient(temp, 1, 13)
           */

        boolean fieldUndefined[] = {
                false,
                false,
                false,
                false,
                false,
                false
        };

        int signum = duration.getSign();

        int startMonth = getMonth();
        if (startMonth == DatatypeConstants.FIELD_UNDEFINED) {
            startMonth = DatatypeConstants.JANUARY;
            fieldUndefined[MONTH] = true;
        }

        BigInteger dMonths = sanitize(duration.getField(DatatypeConstants.MONTHS), signum);
        BigInteger temp = BigInteger.valueOf((long) startMonth).add(dMonths);
        setMonth(temp.subtract(BigInteger.ONE).mod(TWELVE).intValue() + 1);
        BigInteger carry =
                new BigDecimal(temp.subtract(BigInteger.ONE))
                        .divide(DECIMAL_TWELVE, RoundingMode.FLOOR).toBigInteger();

        /* Years (may be modified additionally below)
            *  E[year] := S[year] + D[year] + carry
            */
        BigInteger startYear = getEonAndYear();
        if (startYear == null) {
            fieldUndefined[YEAR] = true;
            startYear = BigInteger.ZERO;
        }
        BigInteger dYears = sanitize(duration.getField(DatatypeConstants.YEARS), signum);
        BigInteger endYear = startYear.add(dYears).add(carry);
        setYear(endYear);

        /* Zone
               *  E[zone] := S[zone]
           *
           * no-op since adding to this, not to a new end point.
           */

        /* Seconds
            *  temp := S[second] + D[second]
            *  E[second] := modulo(temp, 60)
            *  carry := fQuotient(temp, 60)
            */
        BigDecimal startSeconds;
        if (getSecond() == DatatypeConstants.FIELD_UNDEFINED) {
            fieldUndefined[SECOND] = true;
            startSeconds = DECIMAL_ZERO;
        } else {
            // seconds + fractionalSeconds
            startSeconds = getSeconds();
        }

        // Duration seconds is SECONDS + FRACTIONALSECONDS.
        BigDecimal dSeconds = DurationImpl.sanitize((BigDecimal) duration.getField(DatatypeConstants.SECONDS), signum);
        BigDecimal tempBD = startSeconds.add(dSeconds);
        BigDecimal fQuotient =
                new BigDecimal(new BigDecimal(tempBD.toBigInteger()).divide(DECIMAL_SIXTY, RoundingMode.FLOOR).toBigInteger());
        BigDecimal endSeconds = tempBD.subtract(fQuotient.multiply(DECIMAL_SIXTY));

        carry = fQuotient.toBigInteger();
        setSecond(endSeconds.intValue());
        BigDecimal tempFracSeconds = endSeconds.subtract(new BigDecimal(BigInteger.valueOf((long) getSecond())));
        if (tempFracSeconds.compareTo(DECIMAL_ZERO) < 0) {
            setFractionalSecond(DECIMAL_ONE.add(tempFracSeconds));
            if (getSecond() == 0) {
                setSecond(59);
                carry = carry.subtract(BigInteger.ONE);
            } else {
                setSecond(getSecond() - 1);
            }
        } else {
            setFractionalSecond(tempFracSeconds);
        }

        /* Minutes
               *  temp := S[minute] + D[minute] + carry
               *  E[minute] := modulo(temp, 60)
               *  carry := fQuotient(temp, 60)
           */
        int startMinutes = getMinute();
        if (startMinutes == DatatypeConstants.FIELD_UNDEFINED) {
            fieldUndefined[MINUTE] = true;
            startMinutes = 0;
        }
        BigInteger dMinutes = sanitize(duration.getField(DatatypeConstants.MINUTES), signum);

        temp = BigInteger.valueOf(startMinutes).add(dMinutes).add(carry);
        setMinute(temp.mod(SIXTY).intValue());
        carry = new BigDecimal(temp).divide(DECIMAL_SIXTY, RoundingMode.FLOOR).toBigInteger();

        /* Hours
               *  temp := S[hour] + D[hour] + carry
               *  E[hour] := modulo(temp, 24)
               *  carry := fQuotient(temp, 24)
           */
        int startHours = getHour();
        if (startHours == DatatypeConstants.FIELD_UNDEFINED) {
            fieldUndefined[HOUR] = true;
            startHours = 0;
        }
        BigInteger dHours = sanitize(duration.getField(DatatypeConstants.HOURS), signum);

        temp = BigInteger.valueOf(startHours).add(dHours).add(carry);
        setHour(temp.mod(TWENTY_FOUR).intValue(), false);
        carry = new BigDecimal(temp).divide(DECIMAL_TWENTY_FOUR,
                RoundingMode.FLOOR).toBigInteger();

        /* Days
           *  if S[day] > maximumDayInMonthFor(E[year], E[month])
           *       + tempDays := maximumDayInMonthFor(E[year], E[month])
           *  else if S[day] < 1
           *       + tempDays := 1
           *  else
           *       + tempDays := S[day]
           *  E[day] := tempDays + D[day] + carry
           *  START LOOP
           *       + IF E[day] < 1
           *             # E[day] := E[day] +
            *                 maximumDayInMonthFor(E[year], E[month] - 1)
           *             # carry := -1
           *       + ELSE IF E[day] > maximumDayInMonthFor(E[year], E[month])
           *             # E[day] :=
            *                    E[day] - maximumDayInMonthFor(E[year], E[month])
           *             # carry := 1
           *       + ELSE EXIT LOOP
           *       + temp := E[month] + carry
           *       + E[month] := modulo(temp, 1, 13)
           *       + E[year] := E[year] + fQuotient(temp, 1, 13)
           *       + GOTO START LOOP
           */
        BigInteger tempDays;
        int startDay = getDay();
        if (startDay == DatatypeConstants.FIELD_UNDEFINED) {
            fieldUndefined[DAY] = true;
            startDay = 1;
        }
        BigInteger dDays = sanitize(duration.getField(DatatypeConstants.DAYS), signum);
        int maxDayInMonth = maximumDayInMonthFor(getEonAndYear(), getMonth());
        if (startDay > maxDayInMonth) {
            tempDays = BigInteger.valueOf(maxDayInMonth);
        } else if (startDay < 1) {
            tempDays = BigInteger.ONE;
        } else {
            tempDays = BigInteger.valueOf(startDay);
        }
        BigInteger endDays = tempDays.add(dDays).add(carry);
        int monthCarry;
        int intTemp;
        while (true) {
            if (endDays.compareTo(BigInteger.ONE) < 0) {
                // calculate days in previous month, watch for month roll over
                BigInteger mdimf = null;
                if (month >= 2) {
                    mdimf = BigInteger.valueOf(maximumDayInMonthFor(getEonAndYear(),
                            getMonth() - 1));
                } else {
                    // roll over to December of previous year
                    mdimf = BigInteger.valueOf(maximumDayInMonthFor(getEonAndYear()
                            .subtract(BigInteger.ONE), 12));
                }
                endDays = endDays.add(mdimf);
                monthCarry = -1;
            } else if (endDays.compareTo(BigInteger.valueOf(
                    maximumDayInMonthFor(getEonAndYear(), getMonth()))) > 0) {
                endDays = endDays.add(BigInteger.valueOf(
                        -maximumDayInMonthFor(getEonAndYear(), getMonth())));
                monthCarry = 1;
            } else {
                break;
            }

            intTemp = getMonth() + monthCarry;
            int endMonth = (intTemp - 1) % (13 - 1);
            int quotient;
            if (endMonth < 0) {
                endMonth = (13 - 1) + endMonth + 1;
                quotient = BigDecimal.valueOf(intTemp - 1)
                        .divide(DECIMAL_TWELVE, RoundingMode.UP).intValue();
            } else {
                quotient = (intTemp - 1) / (13 - 1);
                endMonth += 1;
            }
            setMonth(endMonth);
            if (quotient != 0) {
                setYear(getEonAndYear().add(BigInteger.valueOf(quotient)));
            }
        }
        setDay(endDays.intValue());

        // set fields that where undefined before this addition, back to undefined.
        for (int i = YEAR; i <= SECOND; i++) {
            if (fieldUndefined[i]) {
                switch (i) {
                case YEAR:
                    setYear(DatatypeConstants.FIELD_UNDEFINED);
                    break;
                case MONTH:
                    setMonth(DatatypeConstants.FIELD_UNDEFINED);
                    break;
                case DAY:
                    setDay(DatatypeConstants.FIELD_UNDEFINED);
                    break;
                case HOUR:
                    setHour(DatatypeConstants.FIELD_UNDEFINED, false);
                    break;
                case MINUTE:
                    setMinute(DatatypeConstants.FIELD_UNDEFINED);
                    break;
                case SECOND:
                    setSecond(DatatypeConstants.FIELD_UNDEFINED);
                    setFractionalSecond(null);
                    break;
                }
            }
        }
    }

    private static final BigInteger FOUR = BigInteger.valueOf(4);
    private static final BigInteger HUNDRED = BigInteger.valueOf(100);
    private static final BigInteger FOUR_HUNDRED = BigInteger.valueOf(400);
    private static final BigInteger SIXTY = BigInteger.valueOf(60);
    private static final BigInteger TWENTY_FOUR = BigInteger.valueOf(24);
    private static final BigInteger TWELVE = BigInteger.valueOf(12);
    private static final BigDecimal DECIMAL_ZERO = BigDecimal.valueOf(0);
    private static final BigDecimal DECIMAL_ONE = BigDecimal.valueOf(1);
    private static final BigDecimal DECIMAL_TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal DECIMAL_TWENTY_FOUR = BigDecimal.valueOf(24);
    private static final BigDecimal DECIMAL_SIXTY = BigDecimal.valueOf(60);


    private static class DaysInMonth {
        private static final int [] table = { 0,  // XML Schema months start at 1.
            31, 28, 31, 30, 31, 30,
            31, 31, 30, 31, 30, 31};
    }

    private static int maximumDayInMonthFor(BigInteger year, int month) {
        if (month != DatatypeConstants.FEBRUARY) {
            return DaysInMonth.table[month];
        } else {
            if (year.mod(FOUR_HUNDRED).equals(BigInteger.ZERO) ||
                    (!year.mod(HUNDRED).equals(BigInteger.ZERO) &&
                            year.mod(FOUR).equals(BigInteger.ZERO))) {
                // is a leap year.
                return 29;
            } else {
                return DaysInMonth.table[month];
            }
        }
    }

    private static int maximumDayInMonthFor(int year, int month) {
        if (month != DatatypeConstants.FEBRUARY) {
            return DaysInMonth.table[month];
        } else {
            if (((year % 400) == 0) ||
                    (((year % 100) != 0) && ((year % 4) == 0))) {
                // is a leap year.
                return 29;
            } else {
                return DaysInMonth.table[DatatypeConstants.FEBRUARY];
            }
        }
    }

    /**
     * <p>Convert <code>this</code> to <code>java.util.GregorianCalendar</code>.</p>
     *
     * <p>When <code>this</code> instance has an undefined field, this
     * conversion relies on the <code>java.util.GregorianCalendar</code> default
     * for its corresponding field. A notable difference between
     * XML Schema 1.0 date/time datatypes and <code>java.util.GregorianCalendar</code>
     * is that Timezone value is optional for date/time datatypes and it is
     * a required field for <code>java.util.GregorianCalendar</code>. See javadoc
     * for <code>java.util.TimeZone.getDefault()</code> on how the default
     * is determined. To explicitly specify the <code>TimeZone</code>
     * instance, see
     * {@link #toGregorianCalendar(TimeZone, Locale, XMLGregorianCalendar)}.</p>
     *
     * <table border="2" rules="all" cellpadding="2">
     *   <thead>
     *     <tr>
     *       <th align="center" colspan="2">
     *          Field by Field Conversion from this class to
     *          <code>java.util.GregorianCalendar</code>
     *       </th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *        <th><code>java.util.GregorianCalendar</code> field</th>
     *        <th><code>javax.xml.datatype.XMLGregorianCalendar</code> field</th>
     *     </tr>
     *     <tr>
     *       <th><code>ERA</code></th>
     *       <th>{@link #getEonAndYear()}<code>.signum() < 0 ? GregorianCalendar.BC : GregorianCalendar.AD</code></th>
     *     </tr>
     *     <tr>
     *       <th><code>YEAR</code></th>
     *       <th>{@link #getEonAndYear()}<code>.abs().intValue()</code><i>*</i></th>
     *     </tr>
     *     <tr>
     *       <th><code>MONTH</code></th>
     *       <th>{@link #getMonth()}<code> - 1</code></th>
     *     </tr>
     *     <tr>
     *       <th><code>DAY_OF_MONTH</code></th>
     *       <th>{@link #getDay()}</th>
     *     </tr>
     *     <tr>
     *       <th><code>AM_PM</code></th>
     *       <th>{@link #getHour()} < 12 : Calendar.AM : Calendar.PM</th>
     *     </tr>
     *     <tr>
     *       <th><code>HOUR_OF_DAY</code></th>
     *       <th>{@link #getHour()}</th>
     *     </tr>
     *     <tr>
     *       <th><code>MINUTE</code></th>
     *       <th>{@link #getMinute()}</th>
     *     </tr>
     *     <tr>
     *       <th><code>SECOND</code></th>
     *       <th>{@link #getSecond()}</th>
     *     </tr>
     *     <tr>
     *       <th><code>MILLISECOND</code></th>
     *       <th>get millisecond order from {@link #getFractionalSecond()}<i>*</i> </th>
     *     </tr>
     *     <tr>
     *       <th><code>GregorianCalendar.setTimeZone(TimeZone)</code></th>
     *       <th>{@link #getTimezone()} formatted into Custom timezone id</th>
     *     </tr>
     *   </tbody>
     * </table>
     * <i>*</i> designates possible loss of precision during the conversion due
     * to source datatype having higer precison than target datatype.
     *
     * <p>To ensure consistency in conversion implementations, the new
     * <code>GregorianCalendar</code> should be instantiated in following
     * manner.
     * <ul>
     *   <li>Using <code>timeZone</code> value as defined above, create a new
     * <code>java.util.GregorianCalendar(timeZone,Locale.getDefault())</code>.
     *   </li>
     *   <li>Initialize all GregorianCalendar fields by calling {(@link GegorianCalendar#clear()}.</li>
     *   <li>Obtain a pure Gregorian Calendar by invoking
     *   <code>GregorianCalendar.setGregorianChange(
     *   new Date(Long.MIN_VALUE))</code>.</li>
     *   <li>Its fields ERA, YEAR, MONTH, DAY_OF_MONTH, HOUR_OF_DAY,
     *       MINUTE, SECOND and MILLISECOND are set using the method
     *       <code>Calendar.set(int,int)</code></li>
     * </ul>
     * </p>
     *
     * @see #toGregorianCalendar(java.util.TimeZone, java.util.Locale, XMLGregorianCalendar)
     */
    public java.util.GregorianCalendar toGregorianCalendar() {

        GregorianCalendar result = null;
        final int DEFAULT_TIMEZONE_OFFSET = DatatypeConstants.FIELD_UNDEFINED;
        TimeZone tz = getTimeZone(DEFAULT_TIMEZONE_OFFSET);
        /** Use the following instead for JDK7 only:
         * Locale locale = Locale.getDefault(Locale.Category.FORMAT);
         */
        Locale locale = getDefaultLocale();

        result = new GregorianCalendar(tz, locale);
        result.clear();
        result.setGregorianChange(PURE_GREGORIAN_CHANGE);

        // if year( and eon) are undefined, leave default Calendar values
        if (year != DatatypeConstants.FIELD_UNDEFINED) {
            if (eon == null) {
                result.set(Calendar.ERA, year < 0 ? GregorianCalendar.BC : GregorianCalendar.AD);
                result.set(Calendar.YEAR, Math.abs(year));
            }
            else {
                BigInteger eonAndYear = getEonAndYear();
                result.set(Calendar.ERA, eonAndYear.signum() == -1 ? GregorianCalendar.BC : GregorianCalendar.AD);
                result.set(Calendar.YEAR, eonAndYear.abs().intValue());
            }
        }

        // only set month if it is set
        if (month != DatatypeConstants.FIELD_UNDEFINED) {
            // Calendar.MONTH is zero based while XMLGregorianCalendar month field is not.
            result.set(Calendar.MONTH, month - 1);
        }

        // only set day if it is set
        if (day != DatatypeConstants.FIELD_UNDEFINED) {
            result.set(Calendar.DAY_OF_MONTH, day);
        }

        // only set hour if it is set
        if (hour != DatatypeConstants.FIELD_UNDEFINED) {
            result.set(Calendar.HOUR_OF_DAY, hour);
        }

        // only set minute if it is set
        if (minute != DatatypeConstants.FIELD_UNDEFINED) {
            result.set(Calendar.MINUTE, minute);
        }

        // only set second if it is set
        if (second != DatatypeConstants.FIELD_UNDEFINED) {
            result.set(Calendar.SECOND, second);
        }

        // only set millisend if it is set
        if (fractionalSecond != null) {
            result.set(Calendar.MILLISECOND, getMillisecond());
        }

        return result;
    }

    /**
     *
     * @return default locale
     */
    private Locale getDefaultLocale() {

        String lang = SecuritySupport.getSystemProperty("user.language.format");
        String country = SecuritySupport.getSystemProperty("user.country.format");
        String variant = SecuritySupport.getSystemProperty("user.variant.format");
        Locale locale = null;
        if (lang != null) {
            if (country != null) {
                if (variant != null) {
                    locale = new Locale(lang, country, variant);
                } else {
                    locale = new Locale(lang, country);
                }
            } else {
                locale = new Locale(lang);
            }
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }

    /**
     * <p>Convert <code>this</code> along with provided parameters
     * to <code>java.util.GregorianCalendar</code> instance.</p>
     *
     * <p> Since XML Schema 1.0 date/time datetypes has no concept of
     * timezone ids or daylight savings timezone ids, this conversion operation
     * allows the user to explicitly specify one with
     * <code>timezone</code> parameter.</p>
     *
     * <p>To compute the return value's <code>TimeZone</code> field,
     * <ul>
     * <li>when parameter <code>timeZone</code> is non-null,
     * it is the timezone field.</li>
     * <li>else when <code>this.getTimezone() != DatatypeConstants.FIELD_UNDEFINED</code>,
     * create a <code>java.util.TimeZone</code> with a custom timezone id
     * using the <code>this.getTimezone()</code>.</li>
     * <li>else when <code>defaults.getTimezone() != DatatypeConstants.FIELD_UNDEFINED</code>,
     * create a <code>java.util.TimeZone</code> with a custom timezone id
     * using <code>defaults.getTimezone()</code>.</li>
     * <li>else use the <code>GregorianCalendar</code> default timezone value
     * for the host is definedas specified by
     * <code>java.util.TimeZone.getDefault()</code>.</li></p>
     *
     * <p>To ensure consistency in conversion implementations, the new
     * <code>GregorianCalendar</code> should be instantiated in following
     * manner.
     * <ul>
     *   <li>Create a new <code>java.util.GregorianCalendar(TimeZone,
     *       Locale)</code> with TimeZone set as specified above and the
     *       <code>Locale</code> parameter.
     *   </li>
     *   <li>Initialize all GregorianCalendar fields by calling {(@link GegorianCalendar#clear()}.</li>
     *   <li>Obtain a pure Gregorian Calendar by invoking
     *   <code>GregorianCalendar.setGregorianChange(
     *   new Date(Long.MIN_VALUE))</code>.</li>
     *   <li>Its fields ERA, YEAR, MONTH, DAY_OF_MONTH, HOUR_OF_DAY,
     *       MINUTE, SECOND and MILLISECOND are set using the method
     *       <code>Calendar.set(int,int)</code></li>
     * </ul>
     *
     * @param timezone provide Timezone. <code>null</code> is a legal value.
     * @param aLocale  provide explicit Locale. Use default GregorianCalendar locale if
     *                 value is <code>null</code>.
     * @param defaults provide default field values to use when corresponding
     *                 field for this instance is DatatypeConstants.FIELD_UNDEFINED or null.
     *                 If <code>defaults</code>is <code>null</code> or a field
     *                 within the specified <code>defaults</code> is undefined,
     *                 just use <code>java.util.GregorianCalendar</code> defaults.
     * @return a java.util.GregorianCalendar conversion of this instance.
     *
     * @see #LEAP_YEAR_DEFAULT
     */
    public GregorianCalendar toGregorianCalendar(TimeZone timezone,
                                                 Locale aLocale,
                                                 XMLGregorianCalendar defaults) {
        GregorianCalendar result = null;
        TimeZone tz = timezone;
        if (tz == null) {
            int defaultZoneoffset = DatatypeConstants.FIELD_UNDEFINED;
            if (defaults != null) {
                defaultZoneoffset = defaults.getTimezone();
            }
            tz = getTimeZone(defaultZoneoffset);
        }
        if (aLocale == null) {
            aLocale = Locale.getDefault();
        }
        result = new GregorianCalendar(tz, aLocale);
        result.clear();
        result.setGregorianChange(PURE_GREGORIAN_CHANGE);

        // if year( and eon) are undefined, leave default Calendar values
        if (year != DatatypeConstants.FIELD_UNDEFINED) {
            if (eon == null) {
                result.set(Calendar.ERA, year < 0 ? GregorianCalendar.BC : GregorianCalendar.AD);
                result.set(Calendar.YEAR, Math.abs(year));
            }
            else {
                final BigInteger eonAndYear = getEonAndYear();
                result.set(Calendar.ERA, eonAndYear.signum() == -1 ? GregorianCalendar.BC : GregorianCalendar.AD);
                result.set(Calendar.YEAR, eonAndYear.abs().intValue());
            }
        } else {
            // use default if set
            if (defaults != null) {
                final int defaultYear = defaults.getYear();
                if (defaultYear != DatatypeConstants.FIELD_UNDEFINED) {
                    if (defaults.getEon() == null) {
                        result.set(Calendar.ERA, defaultYear < 0 ? GregorianCalendar.BC : GregorianCalendar.AD);
                        result.set(Calendar.YEAR, Math.abs(defaultYear));
                    }
                    else {
                        final BigInteger defaultEonAndYear = defaults.getEonAndYear();
                        result.set(Calendar.ERA, defaultEonAndYear.signum() == -1 ? GregorianCalendar.BC : GregorianCalendar.AD);
                        result.set(Calendar.YEAR, defaultEonAndYear.abs().intValue());
                    }
                }
            }
        }

        // only set month if it is set
        if (month != DatatypeConstants.FIELD_UNDEFINED) {
            // Calendar.MONTH is zero based while XMLGregorianCalendar month field is not.
            result.set(Calendar.MONTH, month - 1);
        } else {
            // use default if set
            final int defaultMonth = (defaults != null) ? defaults.getMonth() : DatatypeConstants.FIELD_UNDEFINED;
            if (defaultMonth != DatatypeConstants.FIELD_UNDEFINED) {
                // Calendar.MONTH is zero based while XMLGregorianCalendar month field is not.
                result.set(Calendar.MONTH, defaultMonth - 1);
            }
        }

        // only set day if it is set
        if (day != DatatypeConstants.FIELD_UNDEFINED) {
            result.set(Calendar.DAY_OF_MONTH, day);
        } else {
            // use default if set
            final int defaultDay = (defaults != null) ? defaults.getDay() : DatatypeConstants.FIELD_UNDEFINED;
            if (defaultDay != DatatypeConstants.FIELD_UNDEFINED) {
                result.set(Calendar.DAY_OF_MONTH, defaultDay);
            }
        }

        // only set hour if it is set
        if (hour != DatatypeConstants.FIELD_UNDEFINED) {
            result.set(Calendar.HOUR_OF_DAY, hour);
        } else {
            // use default if set
            int defaultHour = (defaults != null) ? defaults.getHour() : DatatypeConstants.FIELD_UNDEFINED;
            if (defaultHour != DatatypeConstants.FIELD_UNDEFINED) {
                result.set(Calendar.HOUR_OF_DAY, defaultHour);
            }
        }

        // only set minute if it is set
        if (minute != DatatypeConstants.FIELD_UNDEFINED) {
            result.set(Calendar.MINUTE, minute);
        } else {
            // use default if set
            final int defaultMinute = (defaults != null) ? defaults.getMinute() : DatatypeConstants.FIELD_UNDEFINED;
            if (defaultMinute != DatatypeConstants.FIELD_UNDEFINED) {
                result.set(Calendar.MINUTE, defaultMinute);
            }
        }

        // only set second if it is set
        if (second != DatatypeConstants.FIELD_UNDEFINED) {
            result.set(Calendar.SECOND, second);
        } else {
            // use default if set
            final int defaultSecond = (defaults != null) ? defaults.getSecond() : DatatypeConstants.FIELD_UNDEFINED;
            if (defaultSecond != DatatypeConstants.FIELD_UNDEFINED) {
                result.set(Calendar.SECOND, defaultSecond);
            }
        }

        // only set millisend if it is set
        if (fractionalSecond != null) {
            result.set(Calendar.MILLISECOND, getMillisecond());
        } else {
            // use default if set
            final BigDecimal defaultFractionalSecond = (defaults != null) ? defaults.getFractionalSecond() : null;
            if (defaultFractionalSecond != null) {
                result.set(Calendar.MILLISECOND, defaults.getMillisecond());
            }
        }

        return result;
    }

    /**
     * <p>Returns a <code>java.util.TimeZone</code> for this class.</p>
     *
     * <p>If timezone field is defined for this instance,
     * returns TimeZone initialized with custom timezone id
     * of zoneoffset. If timezone field is undefined,
     * try the defaultZoneoffset that was passed in.
     * If defaultZoneoffset is DatatypeConstants.FIELD_UNDEFINED, return
     * default timezone for this host.
     * (Same default as java.util.GregorianCalendar).</p>
     *
     * @param defaultZoneoffset default zoneoffset if this zoneoffset is
     * {@link DatatypeConstants#FIELD_UNDEFINED}.
     *
     * @return TimeZone for this.
     */
    public TimeZone getTimeZone(int defaultZoneoffset) {
        TimeZone result = null;
        int zoneoffset = getTimezone();

        if (zoneoffset == DatatypeConstants.FIELD_UNDEFINED) {
            zoneoffset = defaultZoneoffset;
        }
        if (zoneoffset == DatatypeConstants.FIELD_UNDEFINED) {
            result = TimeZone.getDefault();
        } else {
            // zoneoffset is in minutes. Convert to custom timezone id format.
            char sign = zoneoffset < 0 ? '-' : '+';
            if (sign == '-') {
                zoneoffset = -zoneoffset;
            }
            int hour = zoneoffset / 60;
            int minutes = zoneoffset - (hour * 60);

            // Javadoc for java.util.TimeZone documents max length
            // for customTimezoneId is 8 when optional ':' is not used.
            // Format is
            // "GMT" ('-'|''+') (digit digit?) (digit digit)?
            //                   hour          minutes
            StringBuffer customTimezoneId = new StringBuffer(8);
            customTimezoneId.append("GMT");
            customTimezoneId.append(sign);
            customTimezoneId.append(hour);
            if (minutes != 0) {
                if (minutes < 10) {
                    customTimezoneId.append('0');
                }
                customTimezoneId.append(minutes);
            }
            result = TimeZone.getTimeZone(customTimezoneId.toString());
        }
        return result;
    }

    /**
     * <p>Creates and returns a copy of this object.</p>
     *
     * @return copy of this <code>Object</code>
     */
   public Object clone() {
        // Both this.eon and this.fractionalSecond are instances
        // of immutable classes, so they do not need to be cloned.
       return new XMLGregorianCalendarImpl(getEonAndYear(),
                        this.month, this.day,
                        this.hour, this.minute, this.second,
                        this.fractionalSecond,
                        this.timezone);
    }

    /**
     * <p>Unset all fields to undefined.</p>
     *
     * <p>Set all int fields to {@link DatatypeConstants#FIELD_UNDEFINED} and reference fields
     * to null.</p>
     */
    public void clear() {
        eon = null;
        year = DatatypeConstants.FIELD_UNDEFINED;
        month = DatatypeConstants.FIELD_UNDEFINED;
        day = DatatypeConstants.FIELD_UNDEFINED;
        timezone = DatatypeConstants.FIELD_UNDEFINED;  // in minutes
        hour = DatatypeConstants.FIELD_UNDEFINED;
        minute = DatatypeConstants.FIELD_UNDEFINED;
        second = DatatypeConstants.FIELD_UNDEFINED;
        fractionalSecond = null;
    }

    public void setMillisecond(int millisecond) {
        if (millisecond == DatatypeConstants.FIELD_UNDEFINED) {
            fractionalSecond = null;
        } else {
            if(millisecond<0 || 999<millisecond)
                if(millisecond!=DatatypeConstants.FIELD_UNDEFINED)
                    invalidFieldValue(MILLISECOND, millisecond);
            fractionalSecond = BigDecimal.valueOf(millisecond, 3);
        }
    }

    public final void setFractionalSecond(BigDecimal fractional) {
        if (fractional != null) {
            if ((fractional.compareTo(DECIMAL_ZERO) < 0) ||
                    (fractional.compareTo(DECIMAL_ONE) > 0)) {
                throw new IllegalArgumentException(DatatypeMessageFormatter.formatMessage(null,
                        "InvalidFractional", new Object[]{fractional.toString()}));
            }
        }
        this.fractionalSecond = fractional;
    }

    private final class Parser {
        private final String format;
        private final String value;

        private final int flen;
        private final int vlen;

        private int fidx;
        private int vidx;

        private Parser(String format, String value) {
            this.format = format;
            this.value = value;
            this.flen = format.length();
            this.vlen = value.length();
        }

        /**
         * <p>Parse a formated <code>String</code> into an <code>XMLGregorianCalendar</code>.</p>
         *
         * <p>If <code>String</code> is not formated as a legal <code>XMLGregorianCalendar</code> value,
         * an <code>IllegalArgumentException</code> is thrown.</p>
         *
         * @throws IllegalArgumentException If <code>String</code> is not formated as a legal <code>XMLGregorianCalendar</code> value.
         */
        public void parse() throws IllegalArgumentException {
            while (fidx < flen) {
                char fch = format.charAt(fidx++);

                if (fch != '%') { // not a meta character
                    skip(fch);
                    continue;
                }

                // seen meta character. we don't do error check against the format
                switch (format.charAt(fidx++)) {
                    case 'Y' : // year
                        parseYear();
                        break;

                    case 'M' : // month
                        setMonth(parseInt(2, 2));
                        break;

                    case 'D' : // days
                        setDay(parseInt(2, 2));
                        break;

                    case 'h' : // hours
                        setHour(parseInt(2, 2), false);
                        break;

                    case 'm' : // minutes
                        setMinute(parseInt(2, 2));
                        break;

                    case 's' : // parse seconds.
                        setSecond(parseInt(2, 2));

                        if (peek() == '.') {
                            setFractionalSecond(parseBigDecimal());
                        }
                        break;

                    case 'z' : // time zone. missing, 'Z', or [+-]nn:nn
                        char vch = peek();
                        if (vch == 'Z') {
                            vidx++;
                            setTimezone(0);
                        } else if (vch == '+' || vch == '-') {
                            vidx++;
                            int h = parseInt(2, 2);
                            skip(':');
                            int m = parseInt(2, 2);
                            setTimezone((h * 60 + m) * (vch == '+' ? 1 : -1));
                        }

                        break;

                    default :
                        // illegal meta character. impossible.
                        throw new InternalError();
                }
            }

            if (vidx != vlen) {
                // some tokens are left in the input
                throw new IllegalArgumentException(value); //,vidx);
            }
            testHour();
        }

        private char peek() throws IllegalArgumentException {
            if (vidx == vlen) {
                return (char) -1;
            }
            return value.charAt(vidx);
        }

        private char read() throws IllegalArgumentException {
            if (vidx == vlen) {
                throw new IllegalArgumentException(value); //,vidx);
            }
            return value.charAt(vidx++);
        }

        private void skip(char ch) throws IllegalArgumentException {
            if (read() != ch) {
                throw new IllegalArgumentException(value); //,vidx-1);
            }
        }

        private int parseInt(int minDigits, int maxDigits)
            throws IllegalArgumentException {

            int n = 0;
            char ch;
            int vstart = vidx;
            while (isDigit(ch=peek()) && (vidx - vstart) < maxDigits) {
                vidx++;
                n = n*10 + ch-'0';
            }
            if ((vidx - vstart) < minDigits) {
                // we are expecting more digits
                throw new IllegalArgumentException(value); //,vidx);
            }

            return n;
        }

        private void parseYear()
            throws IllegalArgumentException {
            int vstart = vidx;
            int sign = 0;

            // skip leading negative, if it exists
            if (peek() == '-') {
                vidx++;
                sign = 1;
            }
            while (isDigit(peek())) {
                vidx++;
            }
            final int digits = vidx - vstart - sign;
            if (digits < 4) {
                // we are expecting more digits
                throw new IllegalArgumentException(value); //,vidx);
            }
            final String yearString = value.substring(vstart, vidx);
            if (digits < 10) {
                setYear(Integer.parseInt(yearString));
            }
            else {
                setYear(new BigInteger(yearString));
            }
        }

        private BigDecimal parseBigDecimal()
                throws IllegalArgumentException {
            int vstart = vidx;

            if (peek() == '.') {
                vidx++;
            } else {
                throw new IllegalArgumentException(value);
            }
            while (isDigit(peek())) {
                vidx++;
            }
            return new BigDecimal(value.substring(vstart, vidx));
        }
    }

    private static boolean isDigit(char ch) {
        return '0' <= ch && ch <= '9';
    }

    /**
     * Prints this object according to the format specification.
     *
     * <p>
     * StringBuffer -> StringBuilder change had a very visible impact.
     * It almost cut the execution time to half.
     * Diff from Xerces:
     * Xerces use StringBuffer due to the requirement to support
     * JDKs older than JDK 1.5
     */
    private String format( String format ) {
        StringBuilder buf = new StringBuilder();
        int fidx=0,flen=format.length();

        while(fidx<flen) {
            char fch = format.charAt(fidx++);
            if(fch!='%') {// not a meta char
                buf.append(fch);
                continue;
            }

            switch(format.charAt(fidx++)) {
                case 'Y':
                    if (eon == null) {
                        int absYear = year;
                        if (absYear < 0) {
                            buf.append('-');
                            absYear = -year;
                        }
                        printNumber(buf, absYear, 4);
                    }
                    else {
                        printNumber(buf, getEonAndYear(), 4);
                    }
                    break;
                case 'M':
                    printNumber(buf,getMonth(),2);
                    break;
                case 'D':
                    printNumber(buf,getDay(),2);
                    break;
                case 'h':
                    printNumber(buf,getHour(),2);
                    break;
                case 'm':
                    printNumber(buf,getMinute(),2);
                    break;
                case 's':
                    printNumber(buf,getSecond(),2);
                    if (getFractionalSecond() != null) {
                        //Xerces uses a custom method toString instead of
                        //toPlainString() since it needs to support JDKs older than 1.5
                        String frac = getFractionalSecond().toPlainString();
                        //skip leading zero.
                        buf.append(frac.substring(1, frac.length()));
                    }
                    break;
                case 'z':
                    int offset = getTimezone();
                    if (offset == 0) {
                        buf.append('Z');
                    }
                    else if (offset != DatatypeConstants.FIELD_UNDEFINED) {
                        if (offset < 0) {
                            buf.append('-');
                            offset *= -1;
                        }
                        else {
                            buf.append('+');
                        }
                        printNumber(buf,offset/60,2);
                        buf.append(':');
                        printNumber(buf,offset%60,2);
                    }
                    break;
                default:
                    throw new InternalError();  // impossible
            }
        }

        return buf.toString();
    }

    /**
     * Prints an integer as a String.
     *
     * @param out
     *      The formatted string will be appended into this buffer.
     * @param number
     *      The integer to be printed.
     * @param nDigits
     *      The field will be printed by using at least this
     *      number of digits. For example, 5 will be printed as "0005"
     *      if nDigits==4.
     */
    private void printNumber( StringBuilder out, int number, int nDigits ) {
        String s = String.valueOf(number);
        for (int i = s.length(); i < nDigits; i++) {
            out.append('0');
        }
        out.append(s);
    }

    /**
     * Prints an BigInteger as a String.
     *
     * @param out
     *      The formatted string will be appended into this buffer.
     * @param number
     *      The integer to be printed.
     * @param nDigits
     *      The field will be printed by using at least this
     *      number of digits. For example, 5 will be printed as "0005"
     *      if nDigits==4.
     */
    private void printNumber( StringBuilder out, BigInteger number, int nDigits) {
        String s = number.toString();
        for (int i=s.length(); i < nDigits; i++) {
            out.append('0');
        }
        out.append(s);
    }

    /**
     * Compute <code>value*signum</code> where value==null is treated as
     * value==0.
     * @return non-null {@link BigInteger}.
     */
    static BigInteger sanitize(Number value, int signum) {
        if (signum == 0 || value == null) {
            return BigInteger.ZERO;
        }
        return (signum <  0)? ((BigInteger)value).negate() : (BigInteger)value;
    }

    /** <p><code>reset()</code> is designed to allow the reuse of existing
     * <code>XMLGregorianCalendar</code>s thus saving resources associated
     *  with the creation of new <code>XMLGregorianCalendar</code>s.</p>
     */
    public void reset() {
        eon = orig_eon;
        year = orig_year;
        month = orig_month;
        day = orig_day;
        hour = orig_hour;
        minute = orig_minute;
        second = orig_second;
        fractionalSecond = orig_fracSeconds;
        timezone = orig_timezone;
    }

    /** Deserialize Calendar. */
    private void readObject(ObjectInputStream ois)
        throws ClassNotFoundException, IOException {

        // perform default deseralization
        ois.defaultReadObject();

        // initialize orig_* fields
        save();

    } // readObject(ObjectInputStream)
}
