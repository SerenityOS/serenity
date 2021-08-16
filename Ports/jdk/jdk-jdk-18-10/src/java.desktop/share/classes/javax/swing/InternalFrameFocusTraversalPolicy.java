/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.Component;
import java.awt.FocusTraversalPolicy;

/**
 * A FocusTraversalPolicy which can optionally provide an algorithm for
 * determining a JInternalFrame's initial Component. The initial Component is
 * the first to receive focus when the JInternalFrame is first selected. By
 * default, this is the same as the JInternalFrame's default Component to
 * focus.
 *
 * @author David Mendenhall
 *
 * @since 1.4
 */
public abstract class InternalFrameFocusTraversalPolicy
    extends FocusTraversalPolicy
{
    /**
     * Constructor for subclasses to call.
     */
    protected InternalFrameFocusTraversalPolicy() {}

    /**
     * Returns the Component that should receive the focus when a
     * JInternalFrame is selected for the first time. Once the JInternalFrame
     * has been selected by a call to <code>setSelected(true)</code>, the
     * initial Component will not be used again. Instead, if the JInternalFrame
     * loses and subsequently regains selection, or is made invisible or
     * undisplayable and subsequently made visible and displayable, the
     * JInternalFrame's most recently focused Component will become the focus
     * owner. The default implementation of this method returns the
     * JInternalFrame's default Component to focus.
     *
     * @param frame the JInternalFrame whose initial Component is to be
     *        returned
     * @return the Component that should receive the focus when frame is
     *         selected for the first time, or null if no suitable Component
     *         can be found
     * @see JInternalFrame#getMostRecentFocusOwner
     * @throws IllegalArgumentException if window is null
     */
    public Component getInitialComponent(JInternalFrame frame) {
        return getDefaultComponent(frame);
    }
}
