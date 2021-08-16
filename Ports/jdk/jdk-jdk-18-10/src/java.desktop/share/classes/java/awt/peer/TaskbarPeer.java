/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.Taskbar;
import java.awt.Taskbar.Feature;
import java.awt.Taskbar.State;
import java.awt.Window;


/**
 * The {@code TaskbarPeer} interface provides methods for interacting with
 * system task area.
 */
public interface TaskbarPeer {

    /**
     * Requests user attention to this application.
     *
     * @param enabled disables this request if false
     * @param critical if this is an important request
     * @see Taskbar#requestUserAttention
     */
    default void requestUserAttention(boolean enabled, final boolean critical) {}

    /**
     * Requests user attention to the specified window.
     *
     * @param w window
     */
    default void requestWindowUserAttention(Window w) {}

    /**
     * Attaches the contents of the provided PopupMenu to the application icon
     * in system task area.
     *
     * @param menu the PopupMenu to attach to this application
     */
    default void setMenu(final PopupMenu menu) {}

    /**
     * Gets PopupMenu used to add items to this application's icon in system task area.
     *
     * @return the PopupMenu
     */
    default PopupMenu getMenu() { return null; }

    /**
     * Changes this application's icon to the provided image.
     *
     * @param image to change
     */
    default void setIconImage(final Image image) {}

    /**
     * Obtains an image of this application's icon.
     *
     * @return an image of this application's icon
     */
    default Image getIconImage() { return null; }

    /**
     * Affixes a small system-provided badge to this application's icon.
     * Usually a number.
     *
     * @param badge label to affix to the icon
     */
    default void setIconBadge(final String badge) {}

    /**
     * Affixes a small badge to this application's icon in task area
     * for the specified window.
     *
     * @param w window to update
     * @param badge image to affix to the icon
     */
    default void setWindowIconBadge(Window w, final Image badge) {}

    /**
     * Displays progress for specified window.
     *
     * @param w window to update
     * @param value from 0 to 100, other to disable progress indication
     */
    default void setWindowProgressValue(Window w, int value) {}

    /**
     * Sets a progress state for a specified window.
     *
     * @param w window
     * @param state to change to
     * @see Taskbar#setWindowProgressState
     */
    default void setWindowProgressState(Window w, State state) {}

    /**
     * Affixes a small system-provided progress bar to this application's icon.
     *
     * @param value from 0 to 100, other to disable progress indication
     */
    default void setProgressValue(int value) {}

    /**
     * Tests support of {@code Feature} on current platform.
     * @param f feature to test
     * @return true if feature supported supported
     */
    default public boolean isSupported(Feature f) { return false; }
}
