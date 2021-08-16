/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.event.*;
import java.io.Serializable;
import java.util.EventListener;

/**
 * A generic implementation of BoundedRangeModel.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author David Kloba
 * @author Hans Muller
 * @see BoundedRangeModel
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultBoundedRangeModel implements BoundedRangeModel, Serializable
{
    /**
     * Only one <code>ChangeEvent</code> is needed per model instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this".
     */
    protected transient ChangeEvent changeEvent = null;

    /** The listeners waiting for model changes. */
    protected EventListenerList listenerList = new EventListenerList();

    private int value = 0;
    private int extent = 0;
    private int min = 0;
    private int max = 100;
    private boolean isAdjusting = false;


    /**
     * Initializes all of the properties with default values.
     * Those values are:
     * <ul>
     * <li><code>value</code> = 0
     * <li><code>extent</code> = 0
     * <li><code>minimum</code> = 0
     * <li><code>maximum</code> = 100
     * <li><code>adjusting</code> = false
     * </ul>
     */
    public DefaultBoundedRangeModel() {
    }


    /**
     * Initializes value, extent, minimum and maximum. Adjusting is false.
     * Throws an <code>IllegalArgumentException</code> if the following
     * constraints aren't satisfied:
     * <pre>
     * min &lt;= value &lt;= value+extent &lt;= max
     * </pre>
     *
     * @param value  an int giving the current value
     * @param extent the length of the inner range that begins at the model's value
     * @param min    an int giving the minimum value
     * @param max    an int giving the maximum value
     */
    public DefaultBoundedRangeModel(int value, int extent, int min, int max)
    {
        if ((max >= min) &&
            (value >= min) &&
            ((value + extent) >= value) &&
            ((value + extent) <= max)) {
            this.value = value;
            this.extent = extent;
            this.min = min;
            this.max = max;
        }
        else {
            throw new IllegalArgumentException("invalid range properties");
        }
    }


    /**
     * Returns the model's current value.
     * @return the model's current value
     * @see #setValue
     * @see BoundedRangeModel#getValue
     */
    public int getValue() {
      return value;
    }


    /**
     * Returns the model's extent.
     * @return the model's extent
     * @see #setExtent
     * @see BoundedRangeModel#getExtent
     */
    public int getExtent() {
      return extent;
    }


    /**
     * Returns the model's minimum.
     * @return the model's minimum
     * @see #setMinimum
     * @see BoundedRangeModel#getMinimum
     */
    public int getMinimum() {
      return min;
    }


    /**
     * Returns the model's maximum.
     * @return  the model's maximum
     * @see #setMaximum
     * @see BoundedRangeModel#getMaximum
     */
    public int getMaximum() {
        return max;
    }


    /**
     * Sets the current value of the model. For a slider, that
     * determines where the knob appears. Ensures that the new
     * value, <I>n</I> falls within the model's constraints:
     * <pre>
     *     minimum &lt;= value &lt;= value+extent &lt;= maximum
     * </pre>
     *
     * @see BoundedRangeModel#setValue
     */
    public void setValue(int n) {
        n = Math.min(n, Integer.MAX_VALUE - extent);

        int newValue = Math.max(n, min);
        if (newValue + extent > max) {
            newValue = max - extent;
        }
        setRangeProperties(newValue, extent, min, max, isAdjusting);
    }


    /**
     * Sets the extent to <I>n</I> after ensuring that <I>n</I>
     * is greater than or equal to zero and falls within the model's
     * constraints:
     * <pre>
     *     minimum &lt;= value &lt;= value+extent &lt;= maximum
     * </pre>
     * @see BoundedRangeModel#setExtent
     */
    public void setExtent(int n) {
        int newExtent = Math.max(0, n);
        if(value + newExtent > max) {
            newExtent = max - value;
        }
        setRangeProperties(value, newExtent, min, max, isAdjusting);
    }


    /**
     * Sets the minimum to <I>n</I> after ensuring that <I>n</I>
     * that the other three properties obey the model's constraints:
     * <pre>
     *     minimum &lt;= value &lt;= value+extent &lt;= maximum
     * </pre>
     * @see #getMinimum
     * @see BoundedRangeModel#setMinimum
     */
    public void setMinimum(int n) {
        int newMax = Math.max(n, max);
        int newValue = Math.max(n, value);
        int newExtent = Math.min(newMax - newValue, extent);
        setRangeProperties(newValue, newExtent, n, newMax, isAdjusting);
    }


    /**
     * Sets the maximum to <I>n</I> after ensuring that <I>n</I>
     * that the other three properties obey the model's constraints:
     * <pre>
     *     minimum &lt;= value &lt;= value+extent &lt;= maximum
     * </pre>
     * @see BoundedRangeModel#setMaximum
     */
    public void setMaximum(int n) {
        int newMin = Math.min(n, min);
        int newExtent = Math.min(n - newMin, extent);
        int newValue = Math.min(n - newExtent, value);
        setRangeProperties(newValue, newExtent, newMin, n, isAdjusting);
    }


    /**
     * Sets the <code>valueIsAdjusting</code> property.
     *
     * @see #getValueIsAdjusting
     * @see #setValue
     * @see BoundedRangeModel#setValueIsAdjusting
     */
    public void setValueIsAdjusting(boolean b) {
        setRangeProperties(value, extent, min, max, b);
    }


    /**
     * Returns true if the value is in the process of changing
     * as a result of actions being taken by the user.
     *
     * @return the value of the <code>valueIsAdjusting</code> property
     * @see #setValue
     * @see BoundedRangeModel#getValueIsAdjusting
     */
    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }


    /**
     * Sets all of the <code>BoundedRangeModel</code> properties after forcing
     * the arguments to obey the usual constraints:
     * <pre>
     *     minimum &lt;= value &lt;= value+extent &lt;= maximum
     * </pre>
     * <p>
     * At most, one <code>ChangeEvent</code> is generated.
     *
     * @see BoundedRangeModel#setRangeProperties
     * @see #setValue
     * @see #setExtent
     * @see #setMinimum
     * @see #setMaximum
     * @see #setValueIsAdjusting
     */
    public void setRangeProperties(int newValue, int newExtent, int newMin, int newMax, boolean adjusting)
    {
        if (newMin > newMax) {
            newMin = newMax;
        }
        if (newValue > newMax) {
            newMax = newValue;
        }
        if (newValue < newMin) {
            newMin = newValue;
        }

        /* Convert the addends to long so that extent can be
         * Integer.MAX_VALUE without rolling over the sum.
         * A JCK test covers this, see bug 4097718.
         */
        if (((long)newExtent + (long)newValue) > newMax) {
            newExtent = newMax - newValue;
        }

        if (newExtent < 0) {
            newExtent = 0;
        }

        boolean isChange =
            (newValue != value) ||
            (newExtent != extent) ||
            (newMin != min) ||
            (newMax != max) ||
            (adjusting != isAdjusting);

        if (isChange) {
            value = newValue;
            extent = newExtent;
            min = newMin;
            max = newMax;
            isAdjusting = adjusting;

            fireStateChanged();
        }
    }


    /**
     * Adds a <code>ChangeListener</code>.  The change listeners are run each
     * time any one of the Bounded Range model properties changes.
     *
     * @param l the ChangeListener to add
     * @see #removeChangeListener
     * @see BoundedRangeModel#addChangeListener
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }


    /**
     * Removes a <code>ChangeListener</code>.
     *
     * @param l the <code>ChangeListener</code> to remove
     * @see #addChangeListener
     * @see BoundedRangeModel#removeChangeListener
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }


    /**
     * Returns an array of all the change listeners
     * registered on this <code>DefaultBoundedRangeModel</code>.
     *
     * @return all of this model's <code>ChangeListener</code>s
     *         or an empty
     *         array if no change listeners are currently registered
     *
     * @see #addChangeListener
     * @see #removeChangeListener
     *
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }


    /**
     * Runs each <code>ChangeListener</code>'s <code>stateChanged</code> method.
     *
     * @see #setRangeProperties
     * @see EventListenerList
     */
    protected void fireStateChanged()
    {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -=2 ) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }


    /**
     * Returns a string that displays all of the
     * <code>BoundedRangeModel</code> properties.
     */
    public String toString()  {
        String modelString =
            "value=" + getValue() + ", " +
            "extent=" + getExtent() + ", " +
            "min=" + getMinimum() + ", " +
            "max=" + getMaximum() + ", " +
            "adj=" + getValueIsAdjusting();

        return getClass().getName() + "[" + modelString + "]";
    }

    /**
     * Returns an array of all the objects currently registered as
     * <code><em>Foo</em>Listener</code>s
     * upon this model.
     * <code><em>Foo</em>Listener</code>s
     * are registered using the <code>add<em>Foo</em>Listener</code> method.
     * <p>
     * You can specify the <code>listenerType</code> argument
     * with a class literal, such as <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a <code>DefaultBoundedRangeModel</code>
     * instance <code>m</code>
     * for its change listeners
     * with the following code:
     *
     * <pre>ChangeListener[] cls = (ChangeListener[])(m.getListeners(ChangeListener.class));</pre>
     *
     * If no such listeners exist,
     * this method returns an empty array.
     *
     * @param <T> the type of {@code EventListener} class being requested
     * @param listenerType  the type of listeners requested;
     *          this parameter should specify an interface
     *          that descends from <code>java.util.EventListener</code>
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s
     *          on this model,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if <code>listenerType</code> doesn't
     *          specify a class or interface that implements
     *          <code>java.util.EventListener</code>
     *
     * @see #getChangeListeners
     *
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
