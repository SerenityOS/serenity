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
 * A simple implementation of <code>SpinnerModel</code> whose
 * values are defined by an array or a <code>List</code>.
 * For example to create a model defined by
 * an array of the names of the days of the week:
 * <pre>
 * String[] days = new DateFormatSymbols().getWeekdays();
 * SpinnerModel model = new SpinnerListModel(Arrays.asList(days).subList(1, 8));
 * </pre>
 * This class only stores a reference to the array or <code>List</code>
 * so if an element of the underlying sequence changes, it's up
 * to the application to notify the <code>ChangeListeners</code> by calling
 * <code>fireStateChanged</code>.
 * <p>
 * This model inherits a <code>ChangeListener</code>.
 * The <code>ChangeListener</code>s are notified whenever the
 * model's <code>value</code> or <code>list</code> properties changes.
 *
 * @see JSpinner
 * @see SpinnerModel
 * @see AbstractSpinnerModel
 * @see SpinnerNumberModel
 * @see SpinnerDateModel
 *
 * @author Hans Muller
 * @since 1.4
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
public class SpinnerListModel extends AbstractSpinnerModel implements Serializable
{
    private List<?> list;
    private int index;


    /**
     * Constructs a <code>SpinnerModel</code> whose sequence of
     * values is defined by the specified <code>List</code>.
     * The initial value (<i>current element</i>)
     * of the model will be <code>values.get(0)</code>.
     * If <code>values</code> is <code>null</code> or has zero
     * size, an <code>IllegalArugmentException</code> is thrown.
     *
     * @param values the sequence this model represents
     * @throws IllegalArgumentException if <code>values</code> is
     *    <code>null</code> or zero size
     */
    public SpinnerListModel(List<?> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("SpinnerListModel(List) expects non-null non-empty List");
        }
        this.list = values;
        this.index = 0;
    }


    /**
     * Constructs a <code>SpinnerModel</code> whose sequence of values
     * is defined by the specified array.  The initial value of the model
     * will be <code>values[0]</code>.  If <code>values</code> is
     * <code>null</code> or has zero length, an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param values the sequence this model represents
     * @throws IllegalArgumentException if <code>values</code> is
     *    <code>null</code> or zero length
     */
    public SpinnerListModel(Object[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("SpinnerListModel(Object[]) expects non-null non-empty Object[]");
        }
        this.list = Arrays.asList(values);
        this.index = 0;
    }


    /**
     * Constructs an effectively empty <code>SpinnerListModel</code>.
     * The model's list will contain a single
     * <code>"empty"</code> string element.
     */
    public SpinnerListModel() {
        this(new Object[]{"empty"});
    }


    /**
     * Returns the <code>List</code> that defines the sequence for this model.
     *
     * @return the value of the <code>list</code> property
     * @see #setList
     */
    public List<?> getList() {
        return list;
    }


    /**
     * Changes the list that defines this sequence and resets the index
     * of the models <code>value</code> to zero.  Note that <code>list</code>
     * is not copied, the model just stores a reference to it.
     * <p>
     * This method fires a <code>ChangeEvent</code> if <code>list</code> is
     * not equal to the current list.
     *
     * @param list the sequence that this model represents
     * @throws IllegalArgumentException if <code>list</code> is
     *    <code>null</code> or zero length
     * @see #getList
     */
    public void setList(List<?> list) {
        if ((list == null) || (list.size() == 0)) {
            throw new IllegalArgumentException("invalid list");
        }
        if (!list.equals(this.list)) {
            this.list = list;
            index = 0;
            fireStateChanged();
        }
    }


    /**
     * Returns the current element of the sequence.
     *
     * @return the <code>value</code> property
     * @see SpinnerModel#getValue
     * @see #setValue
     */
    public Object getValue() {
        return list.get(index);
    }


    /**
     * Changes the current element of the sequence and notifies
     * <code>ChangeListeners</code>.  If the specified
     * value is not equal to an element of the underlying sequence
     * then an <code>IllegalArgumentException</code> is thrown.
     * In the following example the <code>setValue</code> call
     * would cause an exception to be thrown:
     * <pre>
     * String[] values = {"one", "two", "free", "four"};
     * SpinnerModel model = new SpinnerListModel(values);
     * model.setValue("TWO");
     * </pre>
     *
     * @param elt the sequence element that will be model's current value
     * @throws IllegalArgumentException if the specified value isn't allowed
     * @see SpinnerModel#setValue
     * @see #getValue
     */
    public void setValue(Object elt) {
        int index = list.indexOf(elt);
        if (index == -1) {
            throw new IllegalArgumentException("invalid sequence element");
        }
        else if (index != this.index) {
            this.index = index;
            fireStateChanged();
        }
    }


    /**
     * Returns the next legal value of the underlying sequence or
     * <code>null</code> if value is already the last element.
     *
     * @return the next legal value of the underlying sequence or
     *     <code>null</code> if value is already the last element
     * @see SpinnerModel#getNextValue
     * @see #getPreviousValue
     */
    public Object getNextValue() {
        return (index >= (list.size() - 1)) ? null : list.get(index + 1);
    }


    /**
     * Returns the previous element of the underlying sequence or
     * <code>null</code> if value is already the first element.
     *
     * @return the previous element of the underlying sequence or
     *     <code>null</code> if value is already the first element
     * @see SpinnerModel#getPreviousValue
     * @see #getNextValue
     */
    public Object getPreviousValue() {
        return (index <= 0) ? null : list.get(index - 1);
    }


    /**
     * Returns the next object that starts with <code>substring</code>.
     *
     * @param substring the string to be matched
     * @return the match
     */
    Object findNextMatch(String substring) {
        int max = list.size();

        if (max == 0) {
            return null;
        }
        int counter = index;

        do {
            Object value = list.get(counter);
            if (value != null) {
                String string = value.toString();

                if (string != null && string.startsWith(substring)) {
                    return value;
                }
            }
            counter = (counter + 1) % max;
        } while (counter != index);
        return null;
    }
}
