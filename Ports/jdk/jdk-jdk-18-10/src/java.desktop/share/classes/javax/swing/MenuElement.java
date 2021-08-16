/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;

/**
 * Any component that can be placed into a menu should implement this interface.
 * This interface is used by {@code MenuSelectionManager}
 * to handle selection and navigation in menu hierarchies.
 *
 * @author Arnaud Weber
 * @since 1.2
 */

public interface MenuElement {

    /**
     * Processes a mouse event. {@code event} is a {@code MouseEvent} with
     * source being the receiving element's component. {@code path} is the
     * path of the receiving element in the menu hierarchy including the
     * receiving element itself. {@code manager} is the
     * {@code MenuSelectionManager} for the menu hierarchy. This method should
     * process the {@code MouseEvent} and change the menu selection if necessary
     * by using {@code MenuSelectionManager}'s API Note: you do not have to
     * forward the event to sub-components. This is done automatically by the
     * {@code MenuSelectionManager}.
     *
     * @param event a {@code MouseEvent} to be processed
     * @param path the path of the receiving element in the menu hierarchy
     * @param manager the {@code MenuSelectionManager} for the menu hierarchy
     */
    public void processMouseEvent(MouseEvent event, MenuElement[] path, MenuSelectionManager manager);


    /**
     *  Process a key event.
     *
     * @param event a {@code KeyEvent} to be processed
     * @param path the path of the receiving element in the menu hierarchy
     * @param manager the {@code MenuSelectionManager} for the menu hierarchy
     */
    public void processKeyEvent(KeyEvent event, MenuElement[] path, MenuSelectionManager manager);

    /**
     * Call by the {@code MenuSelectionManager} when the {@code MenuElement} is
     * added or removed from the menu selection.
     *
     * @param isIncluded can be used to indicate if this {@code MenuElement} is
     *        active (if it is a menu) or is on the part of the menu path that
     *        changed (if it is a menu item).
     */
    public void menuSelectionChanged(boolean isIncluded);

    /**
     * This method should return an array containing the sub-elements for the
     * receiving menu element.
     *
     * @return an array of {@code MenuElement}s
     */
    public MenuElement[] getSubElements();

    /**
     * This method should return the {@code java.awt.Component} used to paint the
     * receiving element. The returned component will be used to convert events
     * and detect if an event is inside a {@code MenuElement}'s component.
     *
     * @return the {@code Component} value
     */
    public Component getComponent();
}
