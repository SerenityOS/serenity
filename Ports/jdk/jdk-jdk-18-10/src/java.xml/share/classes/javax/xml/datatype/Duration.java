/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.namespace.QName;

/**
 * <p>Immutable representation of a time span as defined in
 * the W3C XML Schema 1.0 specification.
 *
 * <p>A Duration object represents a period of Gregorian time,
 * which consists of six fields (years, months, days, hours,
 * minutes, and seconds) plus a sign (+/-) field.
 *
 * <p>The first five fields have non-negative ({@literal >=}0) integers or null
 * (which represents that the field is not set),
 * and the seconds field has a non-negative decimal or null.
 * A negative sign indicates a negative duration.
 *
 * <p>This class provides a number of methods that make it easy
 * to use for the duration datatype of XML Schema 1.0 with
 * the errata.
 *
 * <h2>Order relationship</h2>
 * <p>Duration objects only have partial order, where two values A and B
 * maybe either:
 * <ol>
 *  <li>A{@literal <}B (A is shorter than B)
 *  <li>A{@literal >}B (A is longer than B)
 *  <li>A==B   (A and B are of the same duration)
 *  <li>A{@literal <>}B (Comparison between A and B is indeterminate)
 * </ol>
 *
 * <p>For example, 30 days cannot be meaningfully compared to one month.
 * The {@link #compare(Duration duration)} method implements this
 * relationship.
 *
 * <p>See the {@link #isLongerThan(Duration)} method for details about
 * the order relationship among {@code Duration} objects.
 *
 * <h2>Operations over Duration</h2>
 * <p>This class provides a set of basic arithmetic operations, such
 * as addition, subtraction and multiplication.
 * Because durations don't have total order, an operation could
 * fail for some combinations of operations. For example, you cannot
 * subtract 15 days from 1 month. See the javadoc of those methods
 * for detailed conditions where this could happen.
 *
 * <p>Also, division of a duration by a number is not provided because
 * the {@code Duration} class can only deal with finite precision
 * decimal numbers. For example, one cannot represent 1 sec divided by 3.
 *
 * <p>However, you could substitute a division by 3 with multiplying
 * by numbers such as 0.3 or 0.333.
 *
 * <h2>Range of allowed values</h2>
 * <p>
 * Because some operations of {@code Duration} rely on {@link Calendar}
 * even though {@link Duration} can hold very large or very small values,
 * some of the methods may not work correctly on such {@code Duration}s.
 * The impacted methods document their dependency on {@link Calendar}.
 *
 * @author Joseph Fialli
 * @author Kohsuke Kawaguchi
 * @author Jeff Suttor
 * @author Sunitha Reddy
 * @see XMLGregorianCalendar#add(Duration)
 * @since 1.5
 */
public abstract class Duration {

    /**
     * Debugging {@code true} or {@code false}.
     */
    private static final boolean DEBUG = true;

    /**
     * Default no-arg constructor.
     *
     * <p>Note: Always use the {@link DatatypeFactory} to
     * construct an instance of {@code Duration}.
     * The constructor on this class cannot be guaranteed to
     * produce an object with a consistent state and may be
     * removed in the future.
     */
    public Duration() {
    }

    /**
     * Return the name of the XML Schema date/time type that this instance
     * maps to. Type is computed based on fields that are set,
     * i.e. {@link #isSet(DatatypeConstants.Field field)} == {@code true}.
     *
     * <table class="striped">
     *   <caption>Required fields for XML Schema 1.0 Date/Time Datatypes.<br>
     *         <i>(timezone is optional for all date/time datatypes)</i></caption>
     *   <thead>
     *     <tr>
     *       <th scope="col">Datatype</th>
     *       <th scope="col">year</th>
     *       <th scope="col">month</th>
     *       <th scope="col">day</th>
     *       <th scope="col">hour</th>
     *       <th scope="col">minute</th>
     *       <th scope="col">second</th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *       <th scope="row">{@link DatatypeConstants#DURATION}</th>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *     </tr>
     *     <tr>
     *       <th scope="row">{@link DatatypeConstants#DURATION_DAYTIME}</th>
     *       <td></td>
     *       <td></td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *       <td>X</td>
     *     </tr>
     *     <tr>
     *       <th scope="row">{@link DatatypeConstants#DURATION_YEARMONTH}</th>
     *       <td>X</td>
     *       <td>X</td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *       <td></td>
     *     </tr>
     *   </tbody>
     * </table>
     *
     * @return one of the following constants:
     *   {@link DatatypeConstants#DURATION},
     *   {@link DatatypeConstants#DURATION_DAYTIME} or
     *   {@link DatatypeConstants#DURATION_YEARMONTH}.
     *
     * @throws IllegalStateException If the combination of set fields does not match one of the XML Schema date/time datatypes.
     */
    public QName getXMLSchemaType() {

        boolean yearSet = isSet(DatatypeConstants.YEARS);
        boolean monthSet = isSet(DatatypeConstants.MONTHS);
        boolean daySet = isSet(DatatypeConstants.DAYS);
        boolean hourSet = isSet(DatatypeConstants.HOURS);
        boolean minuteSet = isSet(DatatypeConstants.MINUTES);
        boolean secondSet = isSet(DatatypeConstants.SECONDS);

        // DURATION
        if (yearSet
            && monthSet
            && daySet
            && hourSet
            && minuteSet
            && secondSet) {
            return DatatypeConstants.DURATION;
        }

        // DURATION_DAYTIME
        if (!yearSet
            && !monthSet
            && daySet
            && hourSet
            && minuteSet
            && secondSet) {
            return DatatypeConstants.DURATION_DAYTIME;
        }

        // DURATION_YEARMONTH
        if (yearSet
            && monthSet
            && !daySet
            && !hourSet
            && !minuteSet
            && !secondSet) {
            return DatatypeConstants.DURATION_YEARMONTH;
        }

        // nothing matches
        throw new IllegalStateException(
                "javax.xml.datatype.Duration#getXMLSchemaType():"
                + " this Duration does not match one of the XML Schema date/time datatypes:"
                + " year set = " + yearSet
                + " month set = " + monthSet
                + " day set = " + daySet
                + " hour set = " + hourSet
                + " minute set = " + minuteSet
                + " second set = " + secondSet
        );
    }

    /**
     * Returns the sign of this duration in -1,0, or 1.
     *
     * @return
     *      -1 if this duration is negative, 0 if the duration is zero,
     *      and 1 if the duration is positive.
     */
    public abstract int getSign();

    /**
     * Get the years value of this {@code Duration} as an {@code int} or {@code 0} if not present.
     *
     * <p>{@code getYears()} is a convenience method for
     * {@link #getField(DatatypeConstants.Field field) getField(DatatypeConstants.YEARS)}.
     *
     * <p>As the return value is an {@code int}, an incorrect value will be returned for {@code Duration}s
     * with years that go beyond the range of an {@code int}.
     * Use {@link #getField(DatatypeConstants.Field field) getField(DatatypeConstants.YEARS)} to avoid possible loss of precision.
     *
     * @return If the years field is present, return its value as an {@code int}, else return {@code 0}.
     */
    public int getYears() {
        return getField(DatatypeConstants.YEARS).intValue();
    }

    /**
     * Obtains the value of the MONTHS field as an integer value,
     * or 0 if not present.
     *
     * This method works just like {@link #getYears()} except
     * that this method works on the MONTHS field.
     *
     * @return Months of this {@code Duration}.
     */
    public int getMonths() {
        return getField(DatatypeConstants.MONTHS).intValue();
    }

    /**
     * Obtains the value of the DAYS field as an integer value,
     * or 0 if not present.
     *
     * This method works just like {@link #getYears()} except
     * that this method works on the DAYS field.
     *
     * @return Days of this {@code Duration}.
     */
    public int getDays() {
        return getField(DatatypeConstants.DAYS).intValue();
    }

    /**
     * Obtains the value of the HOURS field as an integer value,
     * or 0 if not present.
     *
     * This method works just like {@link #getYears()} except
     * that this method works on the HOURS field.
     *
     * @return Hours of this {@code Duration}.
     *
     */
    public int getHours() {
        return getField(DatatypeConstants.HOURS).intValue();
    }

    /**
     * Obtains the value of the MINUTES field as an integer value,
     * or 0 if not present.
     *
     * This method works just like {@link #getYears()} except
     * that this method works on the MINUTES field.
     *
     * @return Minutes of this {@code Duration}.
     *
     */
    public int getMinutes() {
        return getField(DatatypeConstants.MINUTES).intValue();
    }

    /**
     * Obtains the value of the SECONDS field as an integer value,
     * or 0 if not present.
     *
     * This method works just like {@link #getYears()} except
     * that this method works on the SECONDS field.
     *
     * @return seconds in the integer value. The fraction of seconds
     *   will be discarded (for example, if the actual value is 2.5,
     *   this method returns 2)
     */
    public int getSeconds() {
        return getField(DatatypeConstants.SECONDS).intValue();
    }

    /**
     * Returns the length of the duration in milli-seconds.
     *
     * <p>If the seconds field carries more digits than milli-second order,
     * those will be simply discarded (or in other words, rounded to zero.)
     * For example, for any Calendar value {@code x},
     * <pre>
     * {@code new Duration("PT10.00099S").getTimeInMills(x) == 10000}
     * {@code new Duration("-PT10.00099S").getTimeInMills(x) == -10000}
     * </pre>
     *
     * <p>
     * Note that this method uses the {@link #addTo(Calendar)} method,
     * which may work incorrectly with {@code Duration} objects with
     * very large values in its fields. See the {@link #addTo(Calendar)}
     * method for details.
     *
     * @param startInstant
     *      The length of a month/year varies. The {@code startInstant} is
     *      used to disambiguate this variance. Specifically, this method
     *      returns the difference between {@code startInstant} and
     *      {@code startInstant+duration}
     *
     * @return milliseconds between {@code startInstant} and
     *   {@code startInstant} plus this {@code Duration}
     *
     * @throws NullPointerException if {@code startInstant} parameter
     * is null.
     *
     */
    public long getTimeInMillis(final Calendar startInstant) {
        Calendar cal = (Calendar) startInstant.clone();
        addTo(cal);
        return getCalendarTimeInMillis(cal)
                    - getCalendarTimeInMillis(startInstant);
    }

    /**
     * Returns the length of the duration in milli-seconds.
     *
     * <p>If the seconds field carries more digits than milli-second order,
     * those will be simply discarded (or in other words, rounded to zero.)
     * For example, for any {@code Date} value {@code x},
     * <pre>
     * {@code new Duration("PT10.00099S").getTimeInMills(x) == 10000}
     * {@code new Duration("-PT10.00099S").getTimeInMills(x) == -10000}
     * </pre>
     *
     * <p>
     * Note that this method uses the {@link #addTo(Date)} method,
     * which may work incorrectly with {@code Duration} objects with
     * very large values in its fields. See the {@link #addTo(Date)}
     * method for details.
     *
     * @param startInstant
     *      The length of a month/year varies. The {@code startInstant} is
     *      used to disambiguate this variance. Specifically, this method
     *      returns the difference between {@code startInstant} and
     *      {@code startInstant+duration}.
     *
     * @throws NullPointerException
     *      If the startInstant parameter is null.
     *
     * @return milliseconds between {@code startInstant} and
     *   {@code startInstant} plus this {@code Duration}
     *
     * @see #getTimeInMillis(Calendar)
     */
    public long getTimeInMillis(final Date startInstant) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(startInstant);
        this.addTo(cal);
        return getCalendarTimeInMillis(cal) - startInstant.getTime();
    }

    /**
     * Gets the value of a field.
     *
     * Fields of a duration object may contain arbitrary large value.
     * Therefore this method is designed to return a {@link Number} object.
     *
     * In case of YEARS, MONTHS, DAYS, HOURS, and MINUTES, the returned
     * number will be a non-negative integer. In case of seconds,
     * the returned number may be a non-negative decimal value.
     *
     * @param field
     *      one of the six Field constants (YEARS,MONTHS,DAYS,HOURS,
     *      MINUTES, or SECONDS.)
     * @return
     *      If the specified field is present, this method returns
     *      a non-null non-negative {@link Number} object that
     *      represents its value. If it is not present, return null.
     *      For YEARS, MONTHS, DAYS, HOURS, and MINUTES, this method
     *      returns a {@link java.math.BigInteger} object. For SECONDS, this
     *      method returns a {@link java.math.BigDecimal}.
     *
     * @throws NullPointerException If the {@code field} is {@code null}.
     */
    public abstract Number getField(final DatatypeConstants.Field field);

    /**
     * Checks if a field is set.
     *
     * A field of a duration object may or may not be present.
     * This method can be used to test if a field is present.
     *
     * @param field
     *      one of the six Field constants (YEARS,MONTHS,DAYS,HOURS,
     *      MINUTES, or SECONDS.)
     * @return
     *      true if the field is present. false if not.
     *
     * @throws NullPointerException
     *      If the field parameter is null.
     */
    public abstract boolean isSet(final DatatypeConstants.Field field);

    /**
     * Computes a new duration whose value is {@code this+rhs}.
     *
     * <p>For example,
     * <pre>
     * "1 day" + "-3 days" = "-2 days"
     * "1 year" + "1 day" = "1 year and 1 day"
     * "-(1 hour,50 minutes)" + "-20 minutes" = "-(1 hours,70 minutes)"
     * "15 hours" + "-3 days" = "-(2 days,9 hours)"
     * "1 year" + "-1 day" = IllegalStateException
     * </pre>
     *
     * <p>Since there's no way to meaningfully subtract 1 day from 1 month,
     * there are cases where the operation fails in
     * {@link IllegalStateException}.
     *
     * <p>
     * Formally, the computation is defined as follows.
     * <p>
     * Firstly, we can assume that two {@code Duration}s to be added
     * are both positive without losing generality (i.e.,
     * {@code (-X)+Y=Y-X}, {@code X+(-Y)=X-Y},
     * {@code (-X)+(-Y)=-(X+Y)})
     *
     * <p>
     * Addition of two positive {@code Duration}s are simply defined as
     * field by field addition where missing fields are treated as 0.
     * <p>
     * A field of the resulting {@code Duration} will be unset if and
     * only if respective fields of two input {@code Duration}s are unset.
     * <p>
     * Note that {@code lhs.add(rhs)} will be always successful if
     * {@code lhs.signum()*rhs.signum()!=-1} or both of them are
     * normalized.
     *
     * @param rhs {@code Duration} to add to this {@code Duration}
     *
     * @return
     *      non-null valid Duration object.
     *
     * @throws NullPointerException
     *      If the rhs parameter is null.
     * @throws IllegalStateException
     *      If two durations cannot be meaningfully added. For
     *      example, adding negative one day to one month causes
     *      this exception.
     *
     *
     * @see #subtract(Duration)
     */
    public abstract Duration add(final Duration rhs);

    /**
     * Adds this duration to a {@link Calendar} object.
     *
     * <p>
     * Calls {@link java.util.Calendar#add(int,int)} in the
     * order of YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS, and MILLISECONDS
     * if those fields are present. Because the {@link Calendar} class
     * uses int to hold values, there are cases where this method
     * won't work correctly (for example if values of fields
     * exceed the range of int.)
     *
     * <p>
     * Also, since this duration class is a Gregorian duration, this
     * method will not work correctly if the given {@link Calendar}
     * object is based on some other calendar systems.
     *
     * <p>
     * Any fractional parts of this {@code Duration} object
     * beyond milliseconds will be simply ignored. For example, if
     * this duration is "P1.23456S", then 1 is added to SECONDS,
     * 234 is added to MILLISECONDS, and the rest will be unused.
     *
     * <p>
     * Note that because {@link Calendar#add(int, int)} is using
     * {@code int}, {@code Duration} with values beyond the
     * range of {@code int} in its fields
     * will cause overflow/underflow to the given {@link Calendar}.
     * {@link XMLGregorianCalendar#add(Duration)} provides the same
     * basic operation as this method while avoiding
     * the overflow/underflow issues.
     *
     * @param calendar
     *      A calendar object whose value will be modified.
     * @throws NullPointerException
     *      if the calendar parameter is null.
     */
    public abstract void addTo(Calendar calendar);

    /**
     * Adds this duration to a {@link Date} object.
     *
     * <p>
     * The given date is first converted into
     * a {@link java.util.GregorianCalendar}, then the duration
     * is added exactly like the {@link #addTo(Calendar)} method.
     *
     * <p>
     * The updated time instant is then converted back into a
     * {@link Date} object and used to update the given {@link Date} object.
     *
     * <p>
     * This somewhat redundant computation is necessary to unambiguously
     * determine the duration of months and years.
     *
     * @param date
     *      A date object whose value will be modified.
     * @throws NullPointerException
     *      if the date parameter is null.
     */
    public void addTo(Date date) {

        // check data parameter
        if (date == null) {
            throw new NullPointerException(
                "Cannot call "
                + this.getClass().getName()
                + "#addTo(Date date) with date == null."
            );
        }

        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        this.addTo(cal);
        date.setTime(getCalendarTimeInMillis(cal));
    }

    /**
     * Computes a new duration whose value is {@code this-rhs}.
     *
     * <p>For example:
     * <pre>
     * "1 day" - "-3 days" = "4 days"
     * "1 year" - "1 day" = IllegalStateException
     * "-(1 hour,50 minutes)" - "-20 minutes" = "-(1hours,30 minutes)"
     * "15 hours" - "-3 days" = "3 days and 15 hours"
     * "1 year" - "-1 day" = "1 year and 1 day"
     * </pre>
     *
     * <p>Since there's no way to meaningfully subtract 1 day from 1 month,
     * there are cases where the operation fails in {@link IllegalStateException}.
     *
     * <p>Formally the computation is defined as follows.
     * First, we can assume that two {@code Duration}s are both positive
     * without losing generality.  (i.e.,
     * {@code (-X)-Y=-(X+Y)}, {@code X-(-Y)=X+Y},
     * {@code (-X)-(-Y)=-(X-Y)})
     *
     * <p>Then two durations are subtracted field by field.
     * If the sign of any non-zero field {@code F} is different from
     * the sign of the most significant field,
     * 1 (if {@code F} is negative) or -1 (otherwise)
     * will be borrowed from the next bigger unit of {@code F}.
     *
     * <p>This process is repeated until all the non-zero fields have
     * the same sign.
     *
     * <p>If a borrow occurs in the days field (in other words, if
     * the computation needs to borrow 1 or -1 month to compensate
     * days), then the computation fails by throwing an
     * {@link IllegalStateException}.
     *
     * @param rhs {@code Duration} to subtract from this {@code Duration}.
     *
     * @return New {@code Duration} created from subtracting {@code rhs} from this {@code Duration}.
     *
     * @throws IllegalStateException
     *      If two durations cannot be meaningfully subtracted. For
     *      example, subtracting one day from one month causes
     *      this exception.
     *
     * @throws NullPointerException
     *      If the rhs parameter is null.
     *
     * @see #add(Duration)
     */
    public Duration subtract(final Duration rhs) {
        return add(rhs.negate());
    }

    /**
     * Computes a new duration whose value is {@code factor} times
     * longer than the value of this duration.
     *
     * <p>This method is provided for the convenience.
     * It is functionally equivalent to the following code:
     * <pre>
     * multiply(new BigDecimal(String.valueOf(factor)))
     * </pre>
     *
     * @param factor Factor times longer of new {@code Duration} to create.
     *
     * @return New {@code Duration} that is {@code factor}times longer than this {@code Duration}.
     *
     * @see #multiply(BigDecimal)
     */
    public Duration multiply(int factor) {
        return multiply(new BigDecimal(String.valueOf(factor)));
    }

    /**
     * Computes a new duration whose value is {@code factor} times
     * longer than the value of this duration.
     *
     * <p>
     * For example,
     * <pre>
     * "P1M" (1 month) * "12" = "P12M" (12 months)
     * "PT1M" (1 min) * "0.3" = "PT18S" (18 seconds)
     * "P1M" (1 month) * "1.5" = IllegalStateException
     * </pre>
     *
     * <p>
     * Since the {@code Duration} class is immutable, this method
     * doesn't change the value of this object. It simply computes
     * a new Duration object and returns it.
     *
     * <p>
     * The operation will be performed field by field with the precision
     * of {@link BigDecimal}. Since all the fields except seconds are
     * restricted to hold integers,
     * any fraction produced by the computation will be
     * carried down toward the next lower unit. For example,
     * if you multiply "P1D" (1 day) with "0.5", then it will be 0.5 day,
     * which will be carried down to "PT12H" (12 hours).
     * When fractions of month cannot be meaningfully carried down
     * to days, or year to months, this will cause an
     * {@link IllegalStateException} to be thrown.
     * For example if you multiple one month by 0.5.
     *
     * <p>
     * To avoid {@link IllegalStateException}, use
     * the {@link #normalizeWith(Calendar)} method to remove the years
     * and months fields.
     *
     * @param factor to multiply by
     *
     * @return
     *      returns a non-null valid {@code Duration} object
     *
     * @throws IllegalStateException if operation produces fraction in
     * the months field.
     *
     * @throws NullPointerException if the {@code factor} parameter is
     * {@code null}.
     *
     */
    public abstract Duration multiply(final BigDecimal factor);

    /**
     * Returns a new {@code Duration} object whose
     * value is {@code -this}.
     *
     * <p>
     * Since the {@code Duration} class is immutable, this method
     * doesn't change the value of this object. It simply computes
     * a new Duration object and returns it.
     *
     * @return
     *      always return a non-null valid {@code Duration} object.
     */
    public abstract Duration negate();

    /**
     * Converts the years and months fields into the days field
     * by using a specific time instant as the reference point.
     *
     * <p>For example, duration of one month normalizes to 31 days
     * given the start time instance "July 8th 2003, 17:40:32".
     *
     * <p>Formally, the computation is done as follows:
     * <ol>
     *  <li>the given Calendar object is cloned</li>
     *  <li>the years, months and days fields will be added to the {@link Calendar} object
     *      by using the {@link Calendar#add(int,int)} method</li>
     *  <li>the difference between the two Calendars in computed in milliseconds and converted to days,
     *     if a remainder occurs due to Daylight Savings Time, it is discarded</li>
     *  <li>the computed days, along with the hours, minutes and seconds
     *      fields of this duration object is used to construct a new
     *      Duration object.</li>
     * </ol>
     *
     * <p>Note that since the Calendar class uses {@code int} to
     * hold the value of year and month, this method may produce
     * an unexpected result if this duration object holds
     * a very large value in the years or months fields.
     *
     * @param startTimeInstant {@code Calendar} reference point.
     *
     * @return {@code Duration} of years and months of this {@code Duration} as days.
     *
     * @throws NullPointerException If the startTimeInstant parameter is null.
     */
    public abstract Duration normalizeWith(final Calendar startTimeInstant);

    /**
     * Partial order relation comparison with this {@code Duration} instance.
     *
     * <p>Comparison result must be in accordance with
     * <a href="http://www.w3.org/TR/xmlschema-2/#duration-order">W3C XML Schema 1.0 Part 2, Section 3.2.7.6.2,
     * <i>Order relation on duration</i></a>.
     *
     * <p>Return:
     * <ul>
     *   <li>{@link DatatypeConstants#LESSER} if this {@code Duration} is shorter than {@code duration} parameter</li>
     *   <li>{@link DatatypeConstants#EQUAL} if this {@code Duration} is equal to {@code duration} parameter</li>
     *   <li>{@link DatatypeConstants#GREATER} if this {@code Duration} is longer than {@code duration} parameter</li>
     *   <li>{@link DatatypeConstants#INDETERMINATE} if a conclusive partial order relation cannot be determined</li>
     * </ul>
     *
     * @param duration to compare
     *
     * @return the relationship between {@code this Duration} and {@code duration} parameter as
     *   {@link DatatypeConstants#LESSER}, {@link DatatypeConstants#EQUAL}, {@link DatatypeConstants#GREATER}
     *   or {@link DatatypeConstants#INDETERMINATE}.
     *
     * @throws UnsupportedOperationException If the underlying implementation
     *   cannot reasonably process the request, e.g. W3C XML Schema allows for
     *   arbitrarily large/small/precise values, the request may be beyond the
     *   implementations capability.
     * @throws NullPointerException if {@code duration} is {@code null}.
     *
     * @see #isShorterThan(Duration)
     * @see #isLongerThan(Duration)
     */
    public abstract int compare(final Duration duration);

    /**
     * Checks if this duration object is strictly longer than
     * another {@code Duration} object.
     *
     * <p>Duration X is "longer" than Y if and only if X {@literal >} Y
     * as defined in the section 3.2.6.2 of the XML Schema 1.0
     * specification.
     *
     * <p>For example, "P1D" (one day) {@literal >} "PT12H" (12 hours) and
     * "P2Y" (two years) {@literal >} "P23M" (23 months).
     *
     * @param duration {@code Duration} to test this {@code Duration} against.
     *
     * @throws UnsupportedOperationException If the underlying implementation
     *   cannot reasonably process the request, e.g. W3C XML Schema allows for
     *   arbitrarily large/small/precise values, the request may be beyond the
     *   implementations capability.
     * @throws NullPointerException If {@code duration} is null.
     *
     * @return
     *      true if the duration represented by this object
     *      is longer than the given duration. false otherwise.
     *
     * @see #isShorterThan(Duration)
     * @see #compare(Duration duration)
     */
    public boolean isLongerThan(final Duration duration) {
        return compare(duration) == DatatypeConstants.GREATER;
    }

    /**
     * Checks if this duration object is strictly shorter than
     * another {@code Duration} object.
     *
     * @param duration {@code Duration} to test this {@code Duration} against.
     *
     * @return {@code true} if {@code duration} parameter is shorter than this {@code Duration},
     *   else {@code false}.
     *
     * @throws UnsupportedOperationException If the underlying implementation
     *   cannot reasonably process the request, e.g. W3C XML Schema allows for
     *   arbitrarily large/small/precise values, the request may be beyond the
     *   implementations capability.
     * @throws NullPointerException if {@code duration} is null.
     *
     * @see #isLongerThan(Duration duration)
     * @see #compare(Duration duration)
     */
    public boolean isShorterThan(final Duration duration) {
        return compare(duration) == DatatypeConstants.LESSER;
    }

    /**
     * Checks if this duration object has the same duration
     * as another {@code Duration} object.
     *
     * <p>For example, "P1D" (1 day) is equal to "PT24H" (24 hours).
     *
     * <p>Duration X is equal to Y if and only if time instant
     * t+X and t+Y are the same for all the test time instants
     * specified in the section 3.2.6.2 of the XML Schema 1.0
     * specification.
     *
     * <p>Note that there are cases where two {@code Duration}s are
     * "incomparable" to each other, like one month and 30 days.
     * For example,
     * <pre>
     * !new Duration("P1M").isShorterThan(new Duration("P30D"))
     * !new Duration("P1M").isLongerThan(new Duration("P30D"))
     * !new Duration("P1M").equals(new Duration("P30D"))
     * </pre>
     *
     * @param duration
     *      The object to compare this {@code Duration} against.
     *
     * @return
     *      {@code true} if this duration is the same length as
     *         {@code duration}.
     *      {@code false} if {@code duration} is {@code null},
     *         is not a
     *         {@code Duration} object,
     *         or its length is different from this duration.
     *
     * @throws UnsupportedOperationException If the underlying implementation
     *   cannot reasonably process the request, e.g. W3C XML Schema allows for
     *   arbitrarily large/small/precise values, the request may be beyond the
     *   implementations capability.
     *
     * @see #compare(Duration duration)
     */
    public boolean equals(final Object duration) {

        if (duration == null || !(duration instanceof Duration)) {
            return false;
        }

        return compare((Duration) duration) == DatatypeConstants.EQUAL;
    }

    /**
     * Returns a hash code consistent with the definition of the equals method.
     *
     * @see Object#hashCode()
     */
    public abstract int hashCode();

    /**
     * Returns a {@code String} representation of this {@code Duration Object}.
     *
     * <p>The result is formatted according to the XML Schema 1.0 spec
     * and can be always parsed back later into the
     * equivalent {@code Duration Object} by {@link DatatypeFactory#newDuration(String  lexicalRepresentation)}.
     *
     * <p>Formally, the following holds for any {@code Duration}
     * {@code Object} x:
     * <pre>
     * new Duration(x.toString()).equals(x)
     * </pre>
     *
     * @return A non-{@code null} valid {@code String} representation of this {@code Duration}.
     */
    public String toString() {

        StringBuffer buf = new StringBuffer();

        if (getSign() < 0) {
            buf.append('-');
        }
        buf.append('P');

        BigInteger years = (BigInteger) getField(DatatypeConstants.YEARS);
        if (years != null) {
            buf.append(years + "Y");
        }

        BigInteger months = (BigInteger) getField(DatatypeConstants.MONTHS);
        if (months != null) {
            buf.append(months + "M");
        }

        BigInteger days = (BigInteger) getField(DatatypeConstants.DAYS);
        if (days != null) {
            buf.append(days + "D");
        }

        BigInteger hours = (BigInteger) getField(DatatypeConstants.HOURS);
        BigInteger minutes = (BigInteger) getField(DatatypeConstants.MINUTES);
        BigDecimal seconds = (BigDecimal) getField(DatatypeConstants.SECONDS);
        if (hours != null || minutes != null || seconds != null) {
            buf.append('T');
            if (hours != null) {
                buf.append(hours + "H");
            }
            if (minutes != null) {
                buf.append(minutes + "M");
            }
            if (seconds != null) {
                buf.append(toString(seconds) + "S");
            }
        }

        return buf.toString();
    }

    /**
     * Turns {@link BigDecimal} to a string representation.
     *
     * <p>Due to a behavior change in the {@link BigDecimal#toString()}
     * method in JDK1.5, this had to be implemented here.
     *
     * @param bd {@code BigDecimal} to format as a {@code String}
     *
     * @return  {@code String} representation of {@code BigDecimal}
     */
    private String toString(BigDecimal bd) {
        String intString = bd.unscaledValue().toString();
        int scale = bd.scale();

        if (scale == 0) {
            return intString;
        }

        /* Insert decimal point */
        StringBuffer buf;
        int insertionPoint = intString.length() - scale;
        if (insertionPoint == 0) { /* Point goes right before intVal */
            return "0." + intString;
        } else if (insertionPoint > 0) { /* Point goes inside intVal */
            buf = new StringBuffer(intString);
            buf.insert(insertionPoint, '.');
        } else { /* We must insert zeros between point and intVal */
            buf = new StringBuffer(3 - insertionPoint + intString.length());
            buf.append("0.");
            for (int i = 0; i < -insertionPoint; i++) {
                buf.append('0');
            }
            buf.append(intString);
        }
        return buf.toString();
    }


    /**
     * Calls the {@link Calendar#getTimeInMillis} method.
     * Prior to JDK1.4, this method was protected and therefore
     * cannot be invoked directly.
     *
     * <p>TODO: In future, this should be replaced by {@code cal.getTimeInMillis()}.
     *
     * @param cal {@code Calendar} to get time in milliseconds.
     *
     * @return Milliseconds of {@code cal}.
     */
    private static long getCalendarTimeInMillis(final Calendar cal) {
        return cal.getTime().getTime();
    }
}
