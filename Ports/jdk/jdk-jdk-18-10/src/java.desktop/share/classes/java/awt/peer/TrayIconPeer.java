/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.SystemTray;
import java.awt.TrayIcon;

/**
 * The peer interface for the {@link TrayIcon}. This doesn't need to be
 * implemented if {@link SystemTray#isSupported()} returns false.
 */
public interface TrayIconPeer {

    /**
     * Disposes the tray icon and releases and resources held by it.
     *
     * @see TrayIcon#removeNotify()
     */
    void dispose();

    /**
     * Sets the tool tip for the tray icon.
     *
     * @param tooltip the tooltip to set
     *
     * @see TrayIcon#setToolTip(String)
     */
    void setToolTip(String tooltip);

    /**
     * Updates the icon image. This is supposed to display the current icon
     * from the TrayIcon component in the actual tray icon.
     *
     * @see TrayIcon#setImage(java.awt.Image)
     * @see TrayIcon#setImageAutoSize(boolean)
     */
    void updateImage();

    /**
     * Displays a message at the tray icon.
     *
     * @param caption the message caption
     * @param text the actual message text
     * @param messageType the message type
     *
     * @see TrayIcon#displayMessage(String, String, java.awt.TrayIcon.MessageType)
     */
    void displayMessage(String caption, String text, String messageType);

    /**
     * Shows the popup menu of this tray icon at the specified position.
     *
     * @param x the X location for the popup menu
     * @param y the Y location for the popup menu
     */
    void showPopupMenu(int x, int y);
}
