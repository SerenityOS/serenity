/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.*;
import javax.swing.event.*;


/**
 * A model for a potentially unbounded sequence of object values.  This model
 * is similar to <code>ListModel</code> however there are some important differences:
 * <ul>
 * <li> The number of sequence elements isn't necessarily bounded.
 * <li> The model doesn't support indexed random access to sequence elements.
 *      Only three sequence values are accessible at a time: current, next and
 *      previous.
 * <li> The current sequence element, can be set.
 * </ul>
 * <p>
 * A <code>SpinnerModel</code> has three properties, only the first is read/write.
 * <dl>
 *   <dt><code>value</code>
 *   <dd>The current element of the sequence.
 *
 *   <dt><code>nextValue</code>
 *   <dd>The following element or null if <code>value</code> is the
 *     last element of the sequence.
 *
 *   <dt><code>previousValue</code>
 *   <dd>The preceding element or null if <code>value</code> is the
 *     first element of the sequence.
 * </dl>
 * When the <code>value</code> property changes,
 * <code>ChangeListeners</code> are notified.  <code>SpinnerModel</code> may
 * choose to notify the <code>ChangeListeners</code> under other circumstances.
 *
 * @see JSpinner
 * @see AbstractSpinnerModel
 * @see SpinnerListModel
 * @see SpinnerNumberModel
 * @see SpinnerDateModel
 *
 * @author Hans Muller
 * @since 1.4
 */
public interface SpinnerModel
{
    /**
     * The <i>current element</i> of the sequence.  This element is usually
     * displayed by the <code>editor</code> part of a <code>JSpinner</code>.
     *
     * @return the current spinner value.
     * @see #setValue
     */
    Object getValue();


    /**
     * Changes current value of the model, typically this value is displayed
     * by the <code>editor</code> part of a  <code>JSpinner</code>.
     * If the <code>SpinnerModel</code> implementation doesn't support
     * the specified value then an <code>IllegalArgumentException</code>
     * is thrown.  For example a <code>SpinnerModel</code> for numbers might
     * only support values that are integer multiples of ten. In
     * that case, <code>model.setValue(new Number(11))</code>
     * would throw an exception.
     *
     * @param value  new value for the spinner
     * @throws IllegalArgumentException if <code>value</code> isn't allowed
     * @see #getValue
     */
    void setValue(Object value);


    /**
     * Return the object in the sequence that comes after the object returned
     * by <code>getValue()</code>. If the end of the sequence has been reached
     * then return null.  Calling this method does not effect <code>value</code>.
     *
     * @return the next legal value or null if one doesn't exist
     * @see #getValue
     * @see #getPreviousValue
     */
    Object getNextValue();


    /**
     * Return the object in the sequence that comes before the object returned
     * by <code>getValue()</code>.  If the end of the sequence has been reached then
     * return null. Calling this method does not effect <code>value</code>.
     *
     * @return the previous legal value or null if one doesn't exist
     * @see #getValue
     * @see #getNextValue
     */
    Object getPreviousValue();


    /**
     * Adds a <code>ChangeListener</code> to the model's listener list.  The
     * <code>ChangeListeners</code> must be notified when models <code>value</code>
     * changes.
     *
     * @param l the ChangeListener to add
     * @see #removeChangeListener
     */
    void addChangeListener(ChangeListener l);


    /**
     * Removes a <code>ChangeListener</code> from the model's listener list.
     *
     * @param l the ChangeListener to remove
     * @see #addChangeListener
     */
    void removeChangeListener(ChangeListener l);
}
