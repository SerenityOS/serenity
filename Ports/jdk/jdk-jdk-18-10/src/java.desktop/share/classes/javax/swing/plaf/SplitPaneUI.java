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

import javax.swing.JSplitPane;
import java.awt.Graphics;

/**
 * Pluggable look and feel interface for JSplitPane.
 *
 * @author Scott Violet
 */
public abstract class SplitPaneUI extends ComponentUI
{
    /**
     * Constructor for subclasses to call.
     */
    protected SplitPaneUI() {}

    /**
     * Messaged to relayout the JSplitPane based on the preferred size
     * of the children components.
     *
     * @param jc a {@code JSplitPane}
     */
    public abstract void resetToPreferredSizes(JSplitPane jc);

    /**
     * Sets the location of the divider to location.
     *
     * @param jc a {@code JSplitPane}
     * @param location an integer specifying the location of the divider
     */
    public abstract void setDividerLocation(JSplitPane jc, int location);

    /**
     * Returns the location of the divider.
     *
     * @param jc a {@code JSplitPane}
     * @return an integer specifying the location of the divider
     */
    public abstract int getDividerLocation(JSplitPane jc);

    /**
     * Returns the minimum possible location of the divider.
     *
     * @param jc a {@code JSplitPane}
     * @return and integer specifying the minimum location of the divider
     */
    public abstract int getMinimumDividerLocation(JSplitPane jc);

    /**
     * Returns the maximum possible location of the divider.
     *
     * @param jc a {@code JSplitPane}
     * @return an integer specifying the maximum location of the divider
     */
    public abstract int getMaximumDividerLocation(JSplitPane jc);

    /**
     * Messaged after the JSplitPane the receiver is providing the look
     * and feel for paints its children.
     *
     * @param jc a {@code JSplitPane}
     * @param g the {@code Graphics} context
     */
    public abstract void finishedPaintingChildren(JSplitPane jc, Graphics g);
}
