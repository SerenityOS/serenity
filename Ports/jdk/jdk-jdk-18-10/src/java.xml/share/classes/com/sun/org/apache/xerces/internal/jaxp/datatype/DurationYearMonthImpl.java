/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import javax.xml.datatype.DatatypeConstants;


/**
 * <p>Represent a subtype <code>xdt:yearMonthDuration</code> of a <code>Duration</code>
 * as specified in <a href="http://www.w3.org/TR/xpath-datamodel#yearMonthDuration">
 *   XQuery 1.0 and XPath 2.0 Data Model, xdt:yearMonthDuration</a>.</p>
 *
 *
 * <p>The DurationYearMonth object represents a period of Gregorian time,
 * with a lexical representation, "<em>PnYnM</em>" that contains only year and month components.
 * </p>
 *
 *
 * @author Vikram Aroskar
 * @see XMLGregorianCalendar#add(Duration)
 */

class DurationYearMonthImpl
        extends DurationImpl {
    private static final long serialVersionUID = -4430140662861507958L;

    /**
     * <p>Constructs a new Duration object by specifying each field individually.</p>
     *
     * <p>All the parameters are optional as long as at least one field is present.
     * If specified, parameters have to be zero or positive.</p>
     *
     * @param isPositive Set to <code>false</code> to create a negative duration. When the length
     *   of the duration is zero, this parameter will be ignored.
     * @param years of this <code>Duration</code>
     * @param months of this <code>Duration</code>
     *
     * @throws IllegalArgumentException
     *    If years, months parameters are all <code>null</code>. Or if any
     *    of those parameters are negative.
     */
    public DurationYearMonthImpl(
        boolean isPositive,
        BigInteger years,
        BigInteger months) {

        super(isPositive, years, months, null, null, null, null);
        convertToCanonicalYearMonth();
    }
        /**
         * <p>Construct a <code>Duration</code> of type <code>xdt:yearMonthDuration</code> using the specified
         * <code>year</code> and <code>month</code> as defined in
         * <a href="http://www.w3.org/TR/xpath-datamodel#yearMonthDuration">
         *   XQuery 1.0 and XPath 2.0 Data Model, xdt:yearMonthDuration</a>.</p>
         *
     * <p>A {@link DatatypeConstants#FIELD_UNDEFINED} value indicates that field is not set.</p>
     *
     * @param isPositive Set to <code>false</code> to create a negative duration. When the length
     *   of the duration is zero, this parameter will be ignored.
         * @param year Year of <code>Duration</code>.
         * @param month Month of <code>Duration</code>.
         *
         * @throws IllegalArgumentException If the values are not a valid representation of a
         * <code>Duration</code>: if any of the fields (year, month) is negative.
         */
    protected DurationYearMonthImpl(
        final boolean isPositive,
        final int years,
        final int months) {

        this(isPositive,
            wrap(years),
            wrap(months));


    }


        /**
         * <p>Construct a <code>Duration</code> of type <code>xdt:yearMonthDuration</code> using the specified milliseconds as defined in
         * <a href="http://www.w3.org/TR/xpath-datamodel#yearMonthDuration">
         *   XQuery 1.0 and XPath 2.0 Data Model, xdt:yearMonthDuration</a>.</p>
         *
         * <p>The datatype <code>xdt:yearMonthDuration</code> is a subtype of <code>xs:duration</code>
         * whose lexical representation contains only year and month components.
         * This datatype resides in the namespace {@link javax.xml.XMLConstants#W3C_XPATH_DATATYPE_NS_URI}.</p>
         *
     * <p>Both values are set by computing their values from the specified milliseconds
     * and are availabe using the <code>get</code> methods of  the created {@link Duration}.
     * The values conform to and are defined by:</p>
     * <ul>
     *   <li>ISO 8601:2000(E) Section 5.5.3.2 Alternative format</li>
     *   <li><a href="http://www.w3.org/TR/xmlschema-2/#isoformats">
     *     W3C XML Schema 1.0 Part 2, Appendix D, ISO 8601 Date and Time Formats</a>
     *   </li>
     *   <li>{@link XMLGregorianCalendar}  Date/Time Datatype Field Mapping Between XML Schema 1.0 and Java Representation</li>
     * </ul>
     *
         * <p>The default start instance is defined by {@link GregorianCalendar}'s use of the start of the epoch: i.e.,
         * {@link java.util.Calendar#YEAR} = 1970,
         * {@link java.util.Calendar#MONTH} = {@link java.util.Calendar#JANUARY},
         * {@link java.util.Calendar#DATE} = 1, etc.
         * This is important as there are variations in the Gregorian Calendar,
         * e.g. leap years have different days in the month = {@link java.util.Calendar#FEBRUARY}
         * so the result of {@link Duration#getMonths()} can be influenced.</p>
         *
     * <p>Any remaining milliseconds after determining the year and month are discarded.</p>
         *
         * @param durationInMilliseconds Milliseconds of <code>Duration</code> to create.
         */
    protected DurationYearMonthImpl(long durationInMilliseconds) {

        super(durationInMilliseconds);
        convertToCanonicalYearMonth();
        //Any remaining milliseconds after determining the year and month are discarded.
        days = null;
        hours = null;
        minutes = null;
        seconds = null;
        signum = calcSignum((signum<0)?false:true);
    }


        /**
         * <p>Construct a <code>Duration</code> of type <code>xdt:yearMonthDuration</code> by parsing its <code>String</code> representation,
         * "<em>PnYnM</em>", <a href="http://www.w3.org/TR/xpath-datamodel#yearMonthDuration">
         *   XQuery 1.0 and XPath 2.0 Data Model, xdt:yearMonthDuration</a>.</p>
         *
         * <p>The datatype <code>xdt:yearMonthDuration</code> is a subtype of <code>xs:duration</code>
         * whose lexical representation contains only year and month components.
         * This datatype resides in the namespace {@link javax.xml.XMLConstants#W3C_XPATH_DATATYPE_NS_URI}.</p>
         *
     * <p>Both values are set and availabe from the created {@link Duration}</p>
         *
     * <p>The XML Schema specification states that values can be of an arbitrary size.
     * Implementations may chose not to or be incapable of supporting arbitrarily large and/or small values.
     * An {@link UnsupportedOperationException} will be thrown with a message indicating implementation limits
     * if implementation capacities are exceeded.</p>
     *
         * @param lexicalRepresentation Lexical representation of a duration.
         *
         * @throws IllegalArgumentException If <code>lexicalRepresentation</code> is not a valid representation of a <code>Duration</code> expressed only in terms of years and months.
         * @throws UnsupportedOperationException If implementation cannot support requested values.
         * @throws NullPointerException If <code>lexicalRepresentation</code> is <code>null</code>.
         */
    protected DurationYearMonthImpl(String lexicalRepresentation) {
        super(lexicalRepresentation);
        if (getDays() > 0 || getHours() > 0
                || getMinutes() > 0 || getSeconds() > 0) {
            throw new IllegalArgumentException(
                    "Trying to create an xdt:yearMonthDuration with an invalid"
                    + " lexical representation of \"" + lexicalRepresentation
                    + "\", data model requires PnYnM.");
        }
        convertToCanonicalYearMonth();
    }

    /**
     * The value space of xs:yearMonthDuration is the set of xs:integer month values.
     * @return the value of yearMonthDuration
     */
    public int getValue() {
        return getYears() * 12 + getMonths();
    }

    private void convertToCanonicalYearMonth() {
        while (getMonths() >= 12)
        {
            months = months.subtract(BigInteger.valueOf(12));
            years = BigInteger.valueOf((long) getYears()).add(BigInteger.ONE);
        }
    }
}
