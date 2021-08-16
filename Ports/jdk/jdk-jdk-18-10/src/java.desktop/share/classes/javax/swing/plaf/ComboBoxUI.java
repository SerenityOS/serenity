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

package javax.swing.plaf;

import javax.swing.JComboBox;

/**
 * Pluggable look and feel interface for JComboBox.
 *
 * @author Arnaud Weber
 * @author Tom Santos
 */
public abstract class ComboBoxUI extends ComponentUI {

    /**
     * Constructor for subclasses to call.
     */
    protected ComboBoxUI() {}

    /**
     * Set the visibility of the popup
     *
     * @param c a {@code JComboBox}
     * @param v a {@code boolean} determining the visibilty of the popup
     */
    public abstract void setPopupVisible( JComboBox<?> c, boolean v );

    /**
     * Determine the visibility of the popup
     *
     * @param c a {@code JComboBox}
     * @return true if popup of the {@code JComboBox} is visible
     */
    public abstract boolean isPopupVisible( JComboBox<?> c );

    /**
     * Determine whether or not the combo box itself is traversable
     *
     * @param c a {@code JComboBox}
     * @return true if the given {@code JComboBox} is traversable
     */
    public abstract boolean isFocusTraversable( JComboBox<?> c );
}
