/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.eawt.event;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;

/**
 * Registration utility class to add {@link GestureListener}s to Swing components.
 *
 * This class manages the relationship between {@link JComponent}s and the {@link GestureListener}s
 * attached to them. It adds additional functionality to AWT Windows, without adding new API to the
 * {@link java.awt.Window} class.
 *
 * To add a {@link GestureListener} to a top-level Swing window, use the {@link JRootPane} of the
 * top-level window type.
 *
 * @see GestureAdapter
 * @see JFrame#getRootPane()
 *
 * @since Java for Mac OS X 10.5 Update 7, Java for Mac OS X 10.6 Update 2
 */
public final class GestureUtilities {
    GestureUtilities() {
        // package private
    }

    /**
     * Attaches a {@link GestureListener} to the specified {@link JComponent}.
     * @param component to attach the {@link GestureListener} to
     * @param listener to be notified when a gesture occurs
     */
    public static void addGestureListenerTo(final JComponent component, final GestureListener listener) {
        if (component == null || listener == null) throw new NullPointerException();
        GestureHandler.addGestureListenerTo(component, listener);
    }

    /**
     * Removes a {@link GestureListener} from the specified {@link JComponent}
     * @param component to remove the {@link GestureListener} from
     * @param listener to be removed
     */
    public static void removeGestureListenerFrom(final JComponent component, final GestureListener listener) {
        if (component == null || listener == null) throw new NullPointerException();
        GestureHandler.removeGestureListenerFrom(component, listener);
    }
}
