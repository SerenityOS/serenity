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

package com.apple.eawt;

import java.awt.Component;
import java.awt.Window;

import javax.swing.RootPaneContainer;

import com.apple.eawt.event.GestureUtilities;
import sun.lwawt.macosx.CPlatformWindow;

/**
 * Utility class perform animated full screen actions to top-level {@link Window}s.
 *
 * This class manages the relationship between {@link Window}s and the {@link FullScreenListener}s
 * attached to them. It adds additional functionality to AWT Windows, without adding new API to the
 * {@link java.awt.Window} class.
 *
 * Full screen operations can only be performed on top-level {@link Window}s that are also {@link RootPaneContainer}s.
 *
 * @see FullScreenAdapter
 * @see GestureUtilities
 *
 * @since Java for Mac OS X 10.7 Update 1
 */
public final class FullScreenUtilities {
    FullScreenUtilities() {
        // package private
    }

    /**
     * Marks a {@link Window} as able to animate into or out of full screen mode.
     *
     * Only top-level {@link Window}s which are {@link RootPaneContainer}s are able to be animated into and out of full screen mode.
     * The {@link Window} must be marked as full screen-able before the native peer is created with {@link Component#addNotify()}.
     *
     * @param window
     * @param canFullScreen
     * @throws IllegalArgumentException if window is not a {@link RootPaneContainer}
     */
    public static void setWindowCanFullScreen(final Window window, final boolean canFullScreen) {
        if (!(window instanceof RootPaneContainer)) throw new IllegalArgumentException("Can't mark a non-RootPaneContainer as full screen-able");
        final RootPaneContainer rpc = (RootPaneContainer)window;
        rpc.getRootPane().putClientProperty(CPlatformWindow.WINDOW_FULLSCREENABLE, Boolean.valueOf(canFullScreen));
    }

    /**
     * Attaches a {@link FullScreenListener} to the specified top-level {@link Window}.
     * @param window to attach the {@link FullScreenListener} to
     * @param listener to be notified when a full screen event occurs
     * @throws IllegalArgumentException if window is not a {@link RootPaneContainer}
     */
    public static void addFullScreenListenerTo(final Window window, final FullScreenListener listener) {
        if (!(window instanceof RootPaneContainer)) throw new IllegalArgumentException("Can't attach FullScreenListener to a non-RootPaneContainer");
        if (listener == null) throw new NullPointerException();
        FullScreenHandler.addFullScreenListenerTo((RootPaneContainer)window, listener);
    }

    /**
     * Removes a {@link FullScreenListener} from the specified top-level {@link Window}.
     * @param window to remove the {@link FullScreenListener} from
     * @param listener to be removed
     * @throws IllegalArgumentException if window is not a {@link RootPaneContainer}
     */
    public static void removeFullScreenListenerFrom(final Window window, final FullScreenListener listener) {
        if (!(window instanceof RootPaneContainer)) throw new IllegalArgumentException("Can't remove FullScreenListener from non-RootPaneContainer");
        if (listener == null) throw new NullPointerException();
        FullScreenHandler.removeFullScreenListenerFrom((RootPaneContainer)window, listener);
    }
}
