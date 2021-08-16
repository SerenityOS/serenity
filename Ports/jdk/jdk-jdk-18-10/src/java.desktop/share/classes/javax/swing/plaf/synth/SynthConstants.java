/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.synth;

import javax.swing.*;

/**
 * Constants used by Synth. Not all Components support all states. A
 * Component will at least be in one of the primary states. That is, the
 * return value from <code>SynthContext.getComponentState()</code> will at
 * least be one of <code>ENABLED</code>, <code>MOUSE_OVER</code>,
 * <code>PRESSED</code> or <code>DISABLED</code>, and may also contain
 * <code>FOCUSED</code>, <code>SELECTED</code> or <code>DEFAULT</code>.
 *
 * @since 1.5
 */
public interface SynthConstants {
    /**
     * Primary state indicating the component is enabled.
     */
    public static final int ENABLED = 1 << 0;
    /**
     * Primary state indicating the mouse is over the region.
     */
    public static final int MOUSE_OVER = 1 << 1;
    /**
     * Primary state indicating the region is in a pressed state. Pressed
     * does not necessarily mean the user has pressed the mouse button.
     */
    public static final int PRESSED = 1 << 2;
    /**
     * Primary state indicating the region is not enabled.
     */
    public static final int DISABLED = 1 << 3;

    /**
     * Indicates the region has focus.
     */
    public static final int FOCUSED = 1 << 8;
    /**
     * Indicates the region is selected.
     */
    public static final int SELECTED = 1 << 9;
    /**
     * Indicates the region is the default. This is typically used for buttons
     * to indicate this button is somehow special.
     */
    public static final int DEFAULT = 1 << 10;
}
