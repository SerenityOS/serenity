/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Rectangle;
import javax.swing.JTabbedPane;

/**
 * Pluggable look and feel interface for JTabbedPane.
 *
 * @author Dave Moore
 * @author Amy Fowler
 */
public abstract class TabbedPaneUI extends ComponentUI {
    /**
     * Constructor for subclasses to call.
     */
    protected TabbedPaneUI() {}

    /**
     * Returns the tab for the coordinate.
     * @param pane the pane
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the tab for the coordinate
     */
    public abstract int tabForCoordinate(JTabbedPane pane, int x, int y);
    /**
     * Returns the rectangle for the tab bounds.
     * @param pane the pane
     * @param index the index
     * @return the rectangle for the tab bounds
     */
    public abstract Rectangle getTabBounds(JTabbedPane pane, int index);
    /**
     * Returns the tab run count.
     * @param pane the pane
     * @return the tab run count
     */
    public abstract int getTabRunCount(JTabbedPane pane);
}
