/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.colorchooser;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.Color;
import java.io.Serializable;

/**
 * A generic implementation of <code>ColorSelectionModel</code>.
 *
 * @author Steve Wilson
 *
 * @see java.awt.Color
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultColorSelectionModel implements ColorSelectionModel, Serializable {

    /**
     * Only one <code>ChangeEvent</code> is needed per model instance
     * since the event's only (read-only) state is the source property.
     * The source of events generated here is always "this".
     */
    protected transient ChangeEvent changeEvent = null;

    /**
     * The listener list.
     */
    protected EventListenerList listenerList = new EventListenerList();

    private Color selectedColor;

    /**
     * Creates a <code>DefaultColorSelectionModel</code> with the
     * current color set to <code>Color.white</code>.  This is
     * the default constructor.
     */
    public DefaultColorSelectionModel() {
        selectedColor = Color.white;
    }

    /**
     * Creates a <code>DefaultColorSelectionModel</code> with the
     * current color set to <code>color</code>, which should be
     * non-<code>null</code>.  Note that setting the color to
     * <code>null</code> is undefined and may have unpredictable
     * results.
     *
     * @param color the new <code>Color</code>
     */
    public DefaultColorSelectionModel(Color color) {
        selectedColor = color;
    }

    /**
     * Returns the selected <code>Color</code> which should be
     * non-<code>null</code>.
     *
     * @return the selected <code>Color</code>
     */
    public Color getSelectedColor() {
        return selectedColor;
    }

    /**
     * Sets the selected color to <code>color</code>.
     * Note that setting the color to <code>null</code>
     * is undefined and may have unpredictable results.
     * This method fires a state changed event if it sets the
     * current color to a new non-<code>null</code> color;
     * if the new color is the same as the current color,
     * no event is fired.
     *
     * @param color the new <code>Color</code>
     */
    public void setSelectedColor(Color color) {
        if (color != null && !selectedColor.equals(color)) {
            selectedColor = color;
            fireStateChanged();
        }
    }


    /**
     * Adds a <code>ChangeListener</code> to the model.
     *
     * @param l the <code>ChangeListener</code> to be added
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Removes a <code>ChangeListener</code> from the model.
     * @param l the <code>ChangeListener</code> to be removed
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this <code>DefaultColorSelectionModel</code> with
     * <code>addChangeListener</code>.
     *
     * @return all of the <code>ChangeListener</code>s added, or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Runs each <code>ChangeListener</code>'s
     * <code>stateChanged</code> method.
     *
     * <!-- @see #setRangeProperties    //bad link-->
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

}
