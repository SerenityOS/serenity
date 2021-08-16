/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.math.BigDecimal;
import javax.xml.datatype.DatatypeConstants;

/**
 * <p>Represent a subtype <code>xdt:dayTimeDuration</code> of a <code>Duration</code>
 * as specified in <a href="http://www.w3.org/TR/xpath-datamodel#dayTimeDuration">
 *   XQuery 1.0 and XPath 2.0 Data Model, xdt:dayTimeDuration</a>.</p>
 *
 *
 * <p>The DurationYearMonth object represents a period of Gregorian time,
 * with a lexical representation, "<em>PnDTnHnMnS</em>" that contains only year and month components.
 * </p>
 *
 *
 * @author Vikram Aroskar
 * @see XMLGregorianCalendar#add(Duration)
 */

class DurationDayTimeImpl
        extends DurationImpl {
    private static final long serialVersionUID = 844792794952655204L;

    public DurationDayTimeImpl(
        boolean isPositive,
        BigInteger days,
        BigInteger hours,
        BigInteger minutes,
        BigDecimal seconds) {

        super(isPositive, null, null, days, hours, minutes, seconds);
        convertToCanonicalDayTime();
    }

    public DurationDayTimeImpl(
        boolean isPositive,
        int days,
        int hours,
        int minutes,
        int seconds) {

        this(
            isPositive,
            wrap(days),
            wrap(hours),
            wrap(minutes),
            (seconds != DatatypeConstants.FIELD_UNDEFINED ? new BigDecimal(String.valueOf(seconds)) : null));
    }

        /**
         * <p>Construct a <code>Duration</code> of type <code>xdt:dayTimeDuration</code> by parsing its <code>String</code> representation,
         * "<em>PnDTnHnMnS</em>", <a href="http://www.w3.org/TR/xpath-datamodel#dayTimeDuration">
         *   XQuery 1.0 and XPath 2.0 Data Model, xdt:dayTimeDuration</a>.</p>
         *
         * <p>The datatype <code>xdt:dayTimeDuration</code> is a subtype of <code>xs:duration</code>
         * whose lexical representation contains only day, hour, minute, and second components.
         * This datatype resides in the namespace <code>http://www.w3.org/2003/11/xpath-datatypes</code>.</p>
         *
     * <p>All four values are set and availabe from the created {@link Duration}</p>
         *
     * <p>The XML Schema specification states that values can be of an arbitrary size.
     * Implementations may chose not to or be incapable of supporting arbitrarily large and/or small values.
     * An {@link UnsupportedOperationException} will be thrown with a message indicating implementation limits
     * if implementation capacities are exceeded.</p>
     *
         * @param lexicalRepresentation Lexical representation of a duration.
         *
         * @throws IllegalArgumentException If <code>lexicalRepresentation</code> is not a valid representation of a <code>Duration</code> expressed only in terms of days and time.
         * @throws UnsupportedOperationException If implementation cannot support requested values.
         * @throws NullPointerException If <code>lexicalRepresentation</code> is <code>null</code>.
         */
    protected DurationDayTimeImpl(String lexicalRepresentation) {
        super(lexicalRepresentation);

        if (getYears() > 0 || getMonths() > 0) {
            throw new IllegalArgumentException(
                    "Trying to create an xdt:dayTimeDuration with an invalid"
                    + " lexical representation of \"" + lexicalRepresentation
                    + "\", data model requires a format PnDTnHnMnS.");
        }

        convertToCanonicalDayTime();
    }
        /**
         * <p>Create a <code>Duration</code> of type <code>xdt:dayTimeDuration</code> using the specified milliseconds as defined in
         * <a href="http://www.w3.org/TR/xpath-datamodel#dayTimeDuration">
         *   XQuery 1.0 and XPath 2.0 Data Model, xdt:dayTimeDuration</a>.</p>
         *
         * <p>The datatype <code>xdt:dayTimeDuration</code> is a subtype of <code>xs:duration</code>
         * whose lexical representation contains only day, hour, minute, and second components.
         * This datatype resides in the namespace <code>http://www.w3.org/2003/11/xpath-datatypes</code>.</p>
         *
     * <p>All four values are set by computing their values from the specified milliseconds
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
         * so the result of {@link Duration#getDays()} can be influenced.</p>
         *
     * <p>Any remaining milliseconds after determining the day, hour, minute and second are discarded.</p>
     *
     * @param durationInMilliseconds Milliseconds of <code>Duration</code> to create.
     *
     * @return New <code>Duration</code> created with the specified <code>durationInMilliseconds</code>.
     *
     * @see <a href="http://www.w3.org/TR/xpath-datamodel#dayTimeDuration">
     *   XQuery 1.0 and XPath 2.0 Data Model, xdt:dayTimeDuration</a>
     */
    protected DurationDayTimeImpl(final long durationInMilliseconds) {
            super(durationInMilliseconds);
            convertToCanonicalDayTime();
            // only day, hour, minute, and second should have values
            years = null;
            months = null;
    }


    /**
     * The value space of xs:dayTimeDuration is the set of fractional second values.
     * @return fractional second values
     */
    public float getValue() {
        float sec = (seconds==null)?0:seconds.floatValue();
        return (((((getDays() * 24) +
                    getHours()) * 60) +
                    getMinutes())*60) +
                    sec;
    }

    private void convertToCanonicalDayTime() {

        while (getSeconds() >= 60)
        {
            seconds = seconds.subtract(BigDecimal.valueOf(60));
            minutes = BigInteger.valueOf((long) getMinutes()).add(BigInteger.ONE);
        }

        while (getMinutes() >= 60)
        {
            minutes = minutes.subtract(BigInteger.valueOf(60));
            hours = BigInteger.valueOf((long) getHours()).add(BigInteger.ONE);
        }

        while (getHours() >= 24)
        {
            hours = hours.subtract(BigInteger.valueOf(24));
            days = BigInteger.valueOf((long) getDays()).add(BigInteger.ONE);
        }
    }

}
