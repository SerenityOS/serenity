/*
 * Copyright (c) 2000, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.util.*;
import java.io.Serializable;


/**
 * A <code>SpinnerModel</code> for sequences of <code>Date</code>s.
 * The upper and lower bounds of the sequence are defined by properties called
 * <code>start</code> and <code>end</code> and the size
 * of the increase or decrease computed by the <code>nextValue</code>
 * and <code>previousValue</code> methods is defined by a property
 * called <code>calendarField</code>.  The <code>start</code>
 * and <code>end</code> properties can be <code>null</code> to
 * indicate that the sequence has no lower or upper limit.
 * <p>
 * The value of the <code>calendarField</code> property must be one of the
 * <code>java.util.Calendar</code> constants that specify a field
 * within a <code>Calendar</code>.  The <code>getNextValue</code>
 * and <code>getPreviousValue</code>
 * methods change the date forward or backwards by this amount.
 * For example, if <code>calendarField</code> is <code>Calendar.DAY_OF_WEEK</code>,
 * then <code>nextValue</code> produces a <code>Date</code> that's 24
 * hours after the current <code>value</code>, and <code>previousValue</code>
 * produces a <code>Date</code> that's 24 hours earlier.
 * <p>
 * The legal values for <code>calendarField</code> are:
 * <ul>
 *   <li><code>Calendar.ERA</code>
 *   <li><code>Calendar.YEAR</code>
 *   <li><code>Calendar.MONTH</code>
 *   <li><code>Calendar.WEEK_OF_YEAR</code>
 *   <li><code>Calendar.WEEK_OF_MONTH</code>
 *   <li><code>Calendar.DAY_OF_MONTH</code>
 *   <li><code>Calendar.DAY_OF_YEAR</code>
 *   <li><code>Calendar.DAY_OF_WEEK</code>
 *   <li><code>Calendar.DAY_OF_WEEK_IN_MONTH</code>
 *   <li><code>Calendar.AM_PM</code>
 *   <li><code>Calendar.HOUR</code>
 *   <li><code>Calendar.HOUR_OF_DAY</code>
 *   <li><code>Calendar.MINUTE</code>
 *   <li><code>Calendar.SECOND</code>
 *   <li><code>Calendar.MILLISECOND</code>
 * </ul>
 * However some UIs may set the calendarField before committing the edit
 * to spin the field under the cursor. If you only want one field to
 * spin you can subclass and ignore the setCalendarField calls.
 * <p>
 * This model inherits a <code>ChangeListener</code>.  The
 * <code>ChangeListeners</code> are notified whenever the models
 * <code>value</code>, <code>calendarField</code>,
 * <code>start</code>, or <code>end</code> properties changes.
 *
 * @see JSpinner
 * @see SpinnerModel
 * @see AbstractSpinnerModel
 * @see SpinnerListModel
 * @see SpinnerNumberModel
 * @see Calendar#add
 *
 * @author Hans Muller
 * @since 1.4
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
public class SpinnerDateModel extends AbstractSpinnerModel implements Serializable
{
    private Comparable<Date> start, end;
    private Calendar value;
    private int calendarField;


    private boolean calendarFieldOK(int calendarField) {
        switch(calendarField) {
        case Calendar.ERA:
        case Calendar.YEAR:
        case Calendar.MONTH:
        case Calendar.WEEK_OF_YEAR:
        case Calendar.WEEK_OF_MONTH:
        case Calendar.DAY_OF_MONTH:
        case Calendar.DAY_OF_YEAR:
        case Calendar.DAY_OF_WEEK:
        case Calendar.DAY_OF_WEEK_IN_MONTH:
        case Calendar.AM_PM:
        case Calendar.HOUR:
        case Calendar.HOUR_OF_DAY:
        case Calendar.MINUTE:
        case Calendar.SECOND:
        case Calendar.MILLISECOND:
            return true;
        default:
            return false;
        }
    }


    /**
     * Creates a <code>SpinnerDateModel</code> that represents a sequence of dates
     * between <code>start</code> and <code>end</code>.  The
     * <code>nextValue</code> and <code>previousValue</code> methods
     * compute elements of the sequence by advancing or reversing
     * the current date <code>value</code> by the
     * <code>calendarField</code> time unit.  For a precise description
     * of what it means to increment or decrement a <code>Calendar</code>
     * <code>field</code>, see the <code>add</code> method in
     * <code>java.util.Calendar</code>.
     * <p>
     * The <code>start</code> and <code>end</code> parameters can be
     * <code>null</code> to indicate that the range doesn't have an
     * upper or lower bound.  If <code>value</code> or
     * <code>calendarField</code> is <code>null</code>, or if both
     * <code>start</code> and <code>end</code> are specified and
     * <code>minimum &gt; maximum</code> then an
     * <code>IllegalArgumentException</code> is thrown.
     * Similarly if <code>(minimum &lt;= value &lt;= maximum)</code> is false,
     * an IllegalArgumentException is thrown.
     *
     * @param value the current (non <code>null</code>) value of the model
     * @param start the first date in the sequence or <code>null</code>
     * @param end the last date in the sequence or <code>null</code>
     * @param calendarField one of
     *   <ul>
     *    <li><code>Calendar.ERA</code>
     *    <li><code>Calendar.YEAR</code>
     *    <li><code>Calendar.MONTH</code>
     *    <li><code>Calendar.WEEK_OF_YEAR</code>
     *    <li><code>Calendar.WEEK_OF_MONTH</code>
     *    <li><code>Calendar.DAY_OF_MONTH</code>
     *    <li><code>Calendar.DAY_OF_YEAR</code>
     *    <li><code>Calendar.DAY_OF_WEEK</code>
     *    <li><code>Calendar.DAY_OF_WEEK_IN_MONTH</code>
     *    <li><code>Calendar.AM_PM</code>
     *    <li><code>Calendar.HOUR</code>
     *    <li><code>Calendar.HOUR_OF_DAY</code>
     *    <li><code>Calendar.MINUTE</code>
     *    <li><code>Calendar.SECOND</code>
     *    <li><code>Calendar.MILLISECOND</code>
     *   </ul>
     *
     * @throws IllegalArgumentException if <code>value</code> or
     *    <code>calendarField</code> are <code>null</code>,
     *    if <code>calendarField</code> isn't valid,
     *    or if the following expression is
     *    false: <code>(start &lt;= value &lt;= end)</code>.
     *
     * @see Calendar#add
     * @see #setValue
     * @see #setStart
     * @see #setEnd
     * @see #setCalendarField
     */
    public SpinnerDateModel(Date value, Comparable<Date> start, Comparable<Date> end, int calendarField) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (!calendarFieldOK(calendarField)) {
            throw new IllegalArgumentException("invalid calendarField");
        }
        if (!(((start == null) || (start.compareTo(value) <= 0)) &&
              ((end == null) || (end.compareTo(value) >= 0)))) {
            throw new IllegalArgumentException("(start <= value <= end) is false");
        }
        this.value = Calendar.getInstance();
        this.start = start;
        this.end = end;
        this.calendarField = calendarField;

        this.value.setTime(value);
    }


    /**
     * Constructs a <code>SpinnerDateModel</code> whose initial
     * <code>value</code> is the current date, <code>calendarField</code>
     * is equal to <code>Calendar.DAY_OF_MONTH</code>, and for which
     * there are no <code>start</code>/<code>end</code> limits.
     */
    public SpinnerDateModel() {
        this(new Date(), null, null, Calendar.DAY_OF_MONTH);
    }


    /**
     * Changes the lower limit for Dates in this sequence.
     * If <code>start</code> is <code>null</code>,
     * then there is no lower limit.  No bounds checking is done here:
     * the new start value may invalidate the
     * <code>(start &lt;= value &lt;= end)</code>
     * invariant enforced by the constructors.  This is to simplify updating
     * the model.  Naturally one should ensure that the invariant is true
     * before calling the <code>nextValue</code>, <code>previousValue</code>,
     * or <code>setValue</code> methods.
     * <p>
     * Typically this property is a <code>Date</code> however it's possible to use
     * a <code>Comparable</code> with a <code>compareTo</code> method for Dates.
     * For example <code>start</code> might be an instance of a class like this:
     * <pre>
     * MyStartDate implements Comparable {
     *     long t = 12345;
     *     public int compareTo(Date d) {
     *            return (t &lt; d.getTime() ? -1 : (t == d.getTime() ? 0 : 1));
     *     }
     *     public int compareTo(Object o) {
     *            return compareTo((Date)o);
     *     }
     * }
     * </pre>
     * Note that the above example will throw a <code>ClassCastException</code>
     * if the <code>Object</code> passed to <code>compareTo(Object)</code>
     * is not a <code>Date</code>.
     * <p>
     * This method fires a <code>ChangeEvent</code> if the
     * <code>start</code> has changed.
     *
     * @param start defines the first date in the sequence
     * @see #getStart
     * @see #setEnd
     * @see #addChangeListener
     */
    public void setStart(Comparable<Date> start) {
        if ((start == null) ? (this.start != null) : !start.equals(this.start)) {
            this.start = start;
            fireStateChanged();
        }
    }


    /**
     * Returns the first <code>Date</code> in the sequence.
     *
     * @return the value of the <code>start</code> property
     * @see #setStart
     */
    public Comparable<Date> getStart() {
        return start;
    }


    /**
     * Changes the upper limit for <code>Date</code>s in this sequence.
     * If <code>start</code> is <code>null</code>, then there is no upper
     * limit.  No bounds checking is done here: the new
     * start value may invalidate the <code>(start &lt;= value &lt;= end)</code>
     * invariant enforced by the constructors.  This is to simplify updating
     * the model.  Naturally, one should ensure that the invariant is true
     * before calling the <code>nextValue</code>, <code>previousValue</code>,
     * or <code>setValue</code> methods.
     * <p>
     * Typically this property is a <code>Date</code> however it's possible to use
     * <code>Comparable</code> with a <code>compareTo</code> method for
     * <code>Date</code>s.  See <code>setStart</code> for an example.
     * <p>
     * This method fires a <code>ChangeEvent</code> if the <code>end</code>
     * has changed.
     *
     * @param end defines the last date in the sequence
     * @see #getEnd
     * @see #setStart
     * @see #addChangeListener
     */
    public void setEnd(Comparable<Date> end) {
        if ((end == null) ? (this.end != null) : !end.equals(this.end)) {
            this.end = end;
            fireStateChanged();
        }
    }


    /**
     * Returns the last <code>Date</code> in the sequence.
     *
     * @return the value of the <code>end</code> property
     * @see #setEnd
     */
    public Comparable<Date> getEnd() {
        return end;
    }


    /**
     * Changes the size of the date value change computed
     * by the <code>nextValue</code> and <code>previousValue</code> methods.
     * The <code>calendarField</code> parameter must be one of the
     * <code>Calendar</code> field constants like <code>Calendar.MONTH</code>
     * or <code>Calendar.MINUTE</code>.
     * The <code>nextValue</code> and <code>previousValue</code> methods
     * simply move the specified <code>Calendar</code> field forward or backward
     * by one unit with the <code>Calendar.add</code> method.
     * You should use this method with care as some UIs may set the
     * calendarField before committing the edit to spin the field under
     * the cursor. If you only want one field to spin you can subclass
     * and ignore the setCalendarField calls.
     *
     * @param calendarField one of
     *  <ul>
     *    <li><code>Calendar.ERA</code>
     *    <li><code>Calendar.YEAR</code>
     *    <li><code>Calendar.MONTH</code>
     *    <li><code>Calendar.WEEK_OF_YEAR</code>
     *    <li><code>Calendar.WEEK_OF_MONTH</code>
     *    <li><code>Calendar.DAY_OF_MONTH</code>
     *    <li><code>Calendar.DAY_OF_YEAR</code>
     *    <li><code>Calendar.DAY_OF_WEEK</code>
     *    <li><code>Calendar.DAY_OF_WEEK_IN_MONTH</code>
     *    <li><code>Calendar.AM_PM</code>
     *    <li><code>Calendar.HOUR</code>
     *    <li><code>Calendar.HOUR_OF_DAY</code>
     *    <li><code>Calendar.MINUTE</code>
     *    <li><code>Calendar.SECOND</code>
     *    <li><code>Calendar.MILLISECOND</code>
     *  </ul>
     * <p>
     * This method fires a <code>ChangeEvent</code> if the
     * <code>calendarField</code> has changed.
     *
     * @see #getCalendarField
     * @see #getNextValue
     * @see #getPreviousValue
     * @see Calendar#add
     * @see #addChangeListener
     */
    public void setCalendarField(int calendarField) {
        if (!calendarFieldOK(calendarField)) {
            throw new IllegalArgumentException("invalid calendarField");
        }
        if (calendarField != this.calendarField) {
            this.calendarField = calendarField;
            fireStateChanged();
        }
    }


    /**
     * Returns the <code>Calendar</code> field that is added to or subtracted from
     * by the <code>nextValue</code> and <code>previousValue</code> methods.
     *
     * @return the value of the <code>calendarField</code> property
     * @see #setCalendarField
     */
    public int getCalendarField() {
        return calendarField;
    }


    /**
     * Returns the next <code>Date</code> in the sequence, or <code>null</code> if
     * the next date is after <code>end</code>.
     *
     * @return the next <code>Date</code> in the sequence, or <code>null</code> if
     *     the next date is after <code>end</code>.
     *
     * @see SpinnerModel#getNextValue
     * @see #getPreviousValue
     * @see #setCalendarField
     */
    public Object getNextValue() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(value.getTime());
        cal.add(calendarField, 1);
        Date next = cal.getTime();
        return ((end == null) || (end.compareTo(next) >= 0)) ? next : null;
    }


    /**
     * Returns the previous <code>Date</code> in the sequence, or <code>null</code>
     * if the previous date is before <code>start</code>.
     *
     * @return the previous <code>Date</code> in the sequence, or
     *     <code>null</code> if the previous date
     *     is before <code>start</code>
     *
     * @see SpinnerModel#getPreviousValue
     * @see #getNextValue
     * @see #setCalendarField
     */
    public Object getPreviousValue() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(value.getTime());
        cal.add(calendarField, -1);
        Date prev = cal.getTime();
        return ((start == null) || (start.compareTo(prev) <= 0)) ? prev : null;
    }


    /**
     * Returns the current element in this sequence of <code>Date</code>s.
     * This method is equivalent to <code>(Date)getValue</code>.
     *
     * @return the <code>value</code> property
     * @see #setValue
     */
    public Date getDate() {
        return value.getTime();
    }


    /**
     * Returns the current element in this sequence of <code>Date</code>s.
     *
     * @return the <code>value</code> property
     * @see #setValue
     * @see #getDate
     */
    public Object getValue() {
        return value.getTime();
    }


    /**
     * Sets the current <code>Date</code> for this sequence.
     * If <code>value</code> is <code>null</code>,
     * an <code>IllegalArgumentException</code> is thrown.  No bounds
     * checking is done here:
     * the new value may invalidate the <code>(start &lt;= value &lt; end)</code>
     * invariant enforced by the constructors.  Naturally, one should ensure
     * that the <code>(start &lt;= value &lt;= maximum)</code> invariant is true
     * before calling the <code>nextValue</code>, <code>previousValue</code>,
     * or <code>setValue</code> methods.
     * <p>
     * This method fires a <code>ChangeEvent</code> if the
     * <code>value</code> has changed.
     *
     * @param value the current (non <code>null</code>)
     *    <code>Date</code> for this sequence
     * @throws IllegalArgumentException if value is <code>null</code>
     *    or not a <code>Date</code>
     * @see #getDate
     * @see #getValue
     * @see #addChangeListener
     */
    public void setValue(Object value) {
        if ((value == null) || !(value instanceof Date)) {
            throw new IllegalArgumentException("illegal value");
        }
        if (!value.equals(this.value.getTime())) {
            this.value.setTime((Date)value);
            fireStateChanged();
        }
    }
}
