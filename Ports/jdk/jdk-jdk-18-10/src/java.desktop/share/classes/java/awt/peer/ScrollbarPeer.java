/*
 * Copyright (c) 1995, 1998, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.peer;

import java.awt.Scrollbar;

/**
 * The peer interface for {@link Scrollbar}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface ScrollbarPeer extends ComponentPeer {

    /**
     * Sets the parameters for the scrollbar.
     *
     * @param value the current value
     * @param visible how much of the whole scale is visible
     * @param minimum the minimum value
     * @param maximum the maximum value
     *
     * @see Scrollbar#setValues(int, int, int, int)
     */
    void setValues(int value, int visible, int minimum, int maximum);

    /**
     * Sets the line increment of the scrollbar.
     *
     * @param l the line increment
     *
     * @see Scrollbar#setLineIncrement(int)
     */
    void setLineIncrement(int l);

    /**
     * Sets the page increment of the scrollbar.
     *
     * @param l the page increment
     *
     * @see Scrollbar#setPageIncrement(int)
     */
    void setPageIncrement(int l);
}
