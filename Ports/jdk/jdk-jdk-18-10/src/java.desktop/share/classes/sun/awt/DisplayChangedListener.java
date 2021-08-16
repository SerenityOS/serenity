/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.util.EventListener;

/**
 * The listener interface for receiving display change events.
 * The class that is interested in processing a display change event
 * implements this interface (and all the methods it
 * contains).
 *
 * For Motif, this interface is only used for dragging windows between Xinerama
 * screens.
 *
 * For win32, the listener object created from that class is then registered
 * with the WToolkit object using its {@code addDisplayChangeListener}
 * method. When the display resolution is changed (which occurs,
 * in Windows, either by the user changing the properties of the
 * display through the control panel or other utility or by
 * some other application which has gotten fullscreen-exclusive
 * control of the display), the listener is notified through its
 * displayChanged() or paletteChanged() methods.
 *
 * @author Chet Haase
 * @author Brent Christian
 * @since 1.4
 */
public interface DisplayChangedListener extends EventListener {
    /**
     * Invoked when the display mode has changed.
     */
    public void displayChanged();

    /**
     * Invoked when the palette has changed.
     */
    public void paletteChanged();

}
