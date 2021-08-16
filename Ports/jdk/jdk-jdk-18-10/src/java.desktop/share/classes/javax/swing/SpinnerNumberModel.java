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
 * A <code>SpinnerModel</code> for sequences of numbers.
 * The upper and lower bounds of the sequence are defined
 * by properties called <code>minimum</code> and
 * <code>maximum</code>. The size of the increase or decrease
 * computed by the <code>nextValue</code> and
 * <code>previousValue</code> methods is defined by a property called
 * <code>stepSize</code>.  The <code>minimum</code> and
 * <code>maximum</code> properties can be <code>null</code>
 * to indicate that the sequence has no lower or upper limit.
 * All of the properties in this class are defined in terms of two
 * generic types: <code>Number</code> and
 * <code>Comparable</code>, so that all Java numeric types
 * may be accommodated.  Internally, there's only support for
 * values whose type is one of the primitive <code>Number</code> types:
 * <code>Double</code>, <code>Float</code>, <code>Long</code>,
 * <code>Integer</code>, <code>Short</code>, or <code>Byte</code>.
 * <p>
 * To create a <code>SpinnerNumberModel</code> for the integer
 * range zero to one hundred, with
 * fifty as the initial value, one could write:
 * <pre>
 * Integer value = Integer.valueOf(50);
 * Integer min = Integer.valueOf(0);
 * Integer max = Integer.valueOf(100);
 * Integer step = Integer.valueOf(1);
 * SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, step);
 * int fifty = model.getNumber().intValue();
 * </pre>
 * <p>
 * Spinners for integers and doubles are common, so special constructors
 * for these cases are provided.  For example to create the model in
 * the previous example, one could also write:
 * <pre>
 * SpinnerNumberModel model = new SpinnerNumberModel(50, 0, 100, 1);
 * </pre>
 * <p>
 * This model inherits a <code>ChangeListener</code>.
 * The <code>ChangeListeners</code> are notified
 * whenever the model's <code>value</code>, <code>stepSize</code>,
 * <code>minimum</code>, or <code>maximum</code> properties changes.
 *
 * @see JSpinner
 * @see SpinnerModel
 * @see AbstractSpinnerModel
 * @see SpinnerListModel
 * @see SpinnerDateModel
 *
 * @author Hans Muller
 * @since 1.4
*/
@SuppressWarnings("serial") // Superclass is not serializable across versions
public class SpinnerNumberModel extends AbstractSpinnerModel implements Serializable
{
    private Number stepSize, value;
    // Both minimum and maximum are logically Comparable<? extends
    // Number>, but that type is awkward to use since different
    // instances of Number are not naturally Comparable. For example,
    // a Double implements Comparable<Double> and an Integer
    // implements Comparable<Integer>. Neither Integer nor Double will
    // have a bridge method for Comparable<Number>. However, it safe
    // to cast Comparable<?> to Comparable<Object> since all
    // Comparables will have a compare(Object> method, possibly as a
    // bridge.
    private Comparable<?> minimum, maximum;


    /**
     * Constructs a <code>SpinnerModel</code> that represents
     * a closed sequence of
     * numbers from <code>minimum</code> to <code>maximum</code>.  The
     * <code>nextValue</code> and <code>previousValue</code> methods
     * compute elements of the sequence by adding or subtracting
     * <code>stepSize</code> respectively.  All of the parameters
     * must be mutually <code>Comparable</code>, <code>value</code>
     * and <code>stepSize</code> must be instances of <code>Integer</code>
     * <code>Long</code>, <code>Float</code>, or <code>Double</code>.
     * <p>
     * The <code>minimum</code> and <code>maximum</code> parameters
     * can be <code>null</code> to indicate that the range doesn't
     * have an upper or lower bound.
     * If <code>value</code> or <code>stepSize</code> is <code>null</code>,
     * or if both <code>minimum</code> and <code>maximum</code>
     * are specified and <code>minimum &gt; maximum</code> then an
     * <code>IllegalArgumentException</code> is thrown.
     * Similarly if <code>(minimum &lt;= value &lt;= maximum</code>) is false,
     * an <code>IllegalArgumentException</code> is thrown.
     *
     * @param value the current (non <code>null</code>) value of the model
     * @param minimum the first number in the sequence or <code>null</code>
     * @param maximum the last number in the sequence or <code>null</code>
     * @param stepSize the difference between elements of the sequence
     *
     * @throws IllegalArgumentException if stepSize or value is
     *     <code>null</code> or if the following expression is false:
     *     <code>minimum &lt;= value &lt;= maximum</code>
     */
    @SuppressWarnings("unchecked") // Casts to Comparable<Object>
    public SpinnerNumberModel(Number value,
                               Comparable<?> minimum,
                               Comparable<?> maximum,
                               Number stepSize) {
        if ((value == null) || (stepSize == null)) {
            throw new IllegalArgumentException("value and stepSize must be non-null");
        }
        if (!(((minimum == null) || (((Comparable<Object>)minimum).compareTo(value) <= 0)) &&
              ((maximum == null) || (((Comparable<Object>)maximum).compareTo(value) >= 0)))) {
            throw new IllegalArgumentException("(minimum <= value <= maximum) is false");
        }
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
        this.stepSize = stepSize;
    }


    /**
     * Constructs a <code>SpinnerNumberModel</code> with the specified
     * <code>value</code>, <code>minimum</code>/<code>maximum</code> bounds,
     * and <code>stepSize</code>.
     *
     * @param value the current value of the model
     * @param minimum the first number in the sequence
     * @param maximum the last number in the sequence
     * @param stepSize the difference between elements of the sequence
     * @throws IllegalArgumentException if the following expression is false:
     *     <code>minimum &lt;= value &lt;= maximum</code>
     */
    public SpinnerNumberModel(int value, int minimum, int maximum, int stepSize) {
        this(Integer.valueOf(value), Integer.valueOf(minimum), Integer.valueOf(maximum), Integer.valueOf(stepSize));
    }


    /**
     * Constructs a <code>SpinnerNumberModel</code> with the specified
     * <code>value</code>, <code>minimum</code>/<code>maximum</code> bounds,
     * and <code>stepSize</code>.
     *
     * @param value the current value of the model
     * @param minimum the first number in the sequence
     * @param maximum the last number in the sequence
     * @param stepSize the difference between elements of the sequence
     * @throws IllegalArgumentException   if the following expression is false:
     *     <code>minimum &lt;= value &lt;= maximum</code>
     */
    public SpinnerNumberModel(double value, double minimum, double maximum, double stepSize) {
        this(Double.valueOf(value), Double.valueOf(minimum),
             Double.valueOf(maximum), Double.valueOf(stepSize));
    }


    /**
     * Constructs a <code>SpinnerNumberModel</code> with no
     * <code>minimum</code> or <code>maximum</code> value,
     * <code>stepSize</code> equal to one, and an initial value of zero.
     */
    public SpinnerNumberModel() {
        this(Integer.valueOf(0), null, null, Integer.valueOf(1));
    }


    /**
     * Changes the lower bound for numbers in this sequence.
     * If <code>minimum</code> is <code>null</code>,
     * then there is no lower bound.  No bounds checking is done here;
     * the new <code>minimum</code> value may invalidate the
     * <code>(minimum &lt;= value &lt;= maximum)</code>
     * invariant enforced by the constructors.  This is to simplify updating
     * the model, naturally one should ensure that the invariant is true
     * before calling the <code>getNextValue</code>,
     * <code>getPreviousValue</code>, or <code>setValue</code> methods.
     * <p>
     * Typically this property is a <code>Number</code> of the same type
     * as the <code>value</code> however it's possible to use any
     * <code>Comparable</code> with a <code>compareTo</code>
     * method for a <code>Number</code> with the same type as the value.
     * For example if value was a <code>Long</code>,
     * <code>minimum</code> might be a Date subclass defined like this:
     * <pre>
     * MyDate extends Date {  // Date already implements Comparable
     *     public int compareTo(Long o) {
     *         long t = getTime();
     *         return (t &lt; o.longValue() ? -1 : (t == o.longValue() ? 0 : 1));
     *     }
     * }
     * </pre>
     * <p>
     * This method fires a <code>ChangeEvent</code>
     * if the <code>minimum</code> has changed.
     *
     * @param minimum a <code>Comparable</code> that has a
     *     <code>compareTo</code> method for <code>Number</code>s with
     *     the same type as <code>value</code>
     * @see #getMinimum
     * @see #setMaximum
     * @see SpinnerModel#addChangeListener
     */
    public void setMinimum(Comparable<?> minimum) {
        if ((minimum == null) ? (this.minimum != null) : !minimum.equals(this.minimum)) {
            this.minimum = minimum;
            fireStateChanged();
        }
    }


    /**
     * Returns the first number in this sequence.
     *
     * @return the value of the <code>minimum</code> property
     * @see #setMinimum
     */
    public Comparable<?> getMinimum() {
        return minimum;
    }


    /**
     * Changes the upper bound for numbers in this sequence.
     * If <code>maximum</code> is <code>null</code>, then there
     * is no upper bound.  No bounds checking is done here; the new
     * <code>maximum</code> value may invalidate the
     * <code>(minimum &lt;= value &lt; maximum)</code>
     * invariant enforced by the constructors.  This is to simplify updating
     * the model, naturally one should ensure that the invariant is true
     * before calling the <code>next</code>, <code>previous</code>,
     * or <code>setValue</code> methods.
     * <p>
     * Typically this property is a <code>Number</code> of the same type
     * as the <code>value</code> however it's possible to use any
     * <code>Comparable</code> with a <code>compareTo</code>
     * method for a <code>Number</code> with the same type as the value.
     * See {@link #setMinimum(Comparable)} for an example.
     * <p>
     * This method fires a <code>ChangeEvent</code> if the
     * <code>maximum</code> has changed.
     *
     * @param maximum a <code>Comparable</code> that has a
     *     <code>compareTo</code> method for <code>Number</code>s with
     *     the same type as <code>value</code>
     * @see #getMaximum
     * @see #setMinimum
     * @see SpinnerModel#addChangeListener
     */
    public void setMaximum(Comparable<?> maximum) {
        if ((maximum == null) ? (this.maximum != null) : !maximum.equals(this.maximum)) {
            this.maximum = maximum;
            fireStateChanged();
        }
    }


    /**
     * Returns the last number in the sequence.
     *
     * @return the value of the <code>maximum</code> property
     * @see #setMaximum
     */
    public Comparable<?> getMaximum() {
        return maximum;
    }


    /**
     * Changes the size of the value change computed by the
     * <code>getNextValue</code> and <code>getPreviousValue</code>
     * methods.  An <code>IllegalArgumentException</code>
     * is thrown if <code>stepSize</code> is <code>null</code>.
     * <p>
     * This method fires a <code>ChangeEvent</code> if the
     * <code>stepSize</code> has changed.
     *
     * @param stepSize the size of the value change computed by the
     *     <code>getNextValue</code> and <code>getPreviousValue</code> methods
     * @see #getNextValue
     * @see #getPreviousValue
     * @see #getStepSize
     * @see SpinnerModel#addChangeListener
     */
    public void setStepSize(Number stepSize) {
        if (stepSize == null) {
            throw new IllegalArgumentException("null stepSize");
        }
        if (!stepSize.equals(this.stepSize)) {
            this.stepSize = stepSize;
            fireStateChanged();
        }
    }


    /**
     * Returns the size of the value change computed by the
     * <code>getNextValue</code>
     * and <code>getPreviousValue</code> methods.
     *
     * @return the value of the <code>stepSize</code> property
     * @see #setStepSize
     */
    public Number getStepSize() {
        return stepSize;
    }

    @SuppressWarnings("unchecked") // Casts to Comparable<Object>
    private Number incrValue(int dir)
    {
        Number newValue;
        if ((value instanceof Float) || (value instanceof Double)) {
            double v = value.doubleValue() + (stepSize.doubleValue() * (double)dir);
            if (value instanceof Double) {
                newValue = Double.valueOf(v);
            }
            else {
                newValue = Float.valueOf((float)v);
            }
        } else {
            long v = value.longValue() + (stepSize.longValue() * (long)dir);

            if (value instanceof Long) {
                newValue = Long.valueOf(v);
            }
            else if (value instanceof Integer) {
                newValue = Integer.valueOf((int)v);
            }
            else if (value instanceof Short) {
                newValue = Short.valueOf((short)v);
            }
            else {
                newValue = Byte.valueOf((byte)v);
            }
        }

        if ((maximum != null) && (((Comparable<Object>)maximum).compareTo(newValue) < 0)) {
            return null;
        }
        if ((minimum != null) && (((Comparable<Object>)minimum).compareTo(newValue) > 0)) {
            return null;
        }
        else {
            return newValue;
        }
    }


    /**
     * Returns the next number in the sequence.
     *
     * @return <code>value + stepSize</code> or <code>null</code> if the sum
     *     exceeds <code>maximum</code>.
     *
     * @see SpinnerModel#getNextValue
     * @see #getPreviousValue
     * @see #setStepSize
     */
    public Object getNextValue() {
        return incrValue(+1);
    }


    /**
     * Returns the previous number in the sequence.
     *
     * @return <code>value - stepSize</code>, or
     *     <code>null</code> if the sum is less
     *     than <code>minimum</code>.
     *
     * @see SpinnerModel#getPreviousValue
     * @see #getNextValue
     * @see #setStepSize
     */
    public Object getPreviousValue() {
        return incrValue(-1);
    }


    /**
     * Returns the value of the current element of the sequence.
     *
     * @return the value property
     * @see #setValue
     */
    public Number getNumber() {
        return value;
    }


    /**
     * Returns the value of the current element of the sequence.
     *
     * @return the value property
     * @see #setValue
     * @see #getNumber
     */
    public Object getValue() {
        return value;
    }


    /**
     * Sets the current value for this sequence.  If <code>value</code> is
     * <code>null</code>, or not a <code>Number</code>, an
     * <code>IllegalArgumentException</code> is thrown.  No
     * bounds checking is done here; the new value may invalidate the
     * <code>(minimum &lt;= value &lt;= maximum)</code>
     * invariant enforced by the constructors.   It's also possible to set
     * the value to be something that wouldn't naturally occur in the sequence,
     * i.e. a value that's not modulo the <code>stepSize</code>.
     * This is to simplify updating the model, and to accommodate
     * spinners that don't want to restrict values that have been
     * directly entered by the user. Naturally, one should ensure that the
     * <code>(minimum &lt;= value &lt;= maximum)</code> invariant is true
     * before calling the <code>next</code>, <code>previous</code>, or
     * <code>setValue</code> methods.
     * <p>
     * This method fires a <code>ChangeEvent</code> if the value has changed.
     *
     * @param value the current (non <code>null</code>) <code>Number</code>
     *         for this sequence
     * @throws IllegalArgumentException if <code>value</code> is
     *         <code>null</code> or not a <code>Number</code>
     * @see #getNumber
     * @see #getValue
     * @see SpinnerModel#addChangeListener
     */
    public void setValue(Object value) {
        if ((value == null) || !(value instanceof Number)) {
            throw new IllegalArgumentException("illegal value");
        }
        if (!value.equals(this.value)) {
            this.value = (Number)value;
            fireStateChanged();
        }
    }
}
