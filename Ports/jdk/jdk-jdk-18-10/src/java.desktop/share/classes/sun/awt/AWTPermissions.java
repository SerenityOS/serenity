/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTPermission;

/**
 * Defines the {@code AWTPermission} objects used for permission checks.
 */

public final class AWTPermissions {
    private AWTPermissions() { }

    public static final AWTPermission TOPLEVEL_WINDOW_PERMISSION =
        new AWTPermission("showWindowWithoutWarningBanner");

    public static final AWTPermission ACCESS_CLIPBOARD_PERMISSION =
        new AWTPermission("accessClipboard");

    public static final AWTPermission CHECK_AWT_EVENTQUEUE_PERMISSION =
        new AWTPermission("accessEventQueue");

    public static final AWTPermission TOOLKIT_MODALITY_PERMISSION =
        new AWTPermission("toolkitModality");

    public static final AWTPermission READ_DISPLAY_PIXELS_PERMISSION =
        new AWTPermission("readDisplayPixels");

    public static final AWTPermission CREATE_ROBOT_PERMISSION =
        new AWTPermission("createRobot");

    public static final AWTPermission WATCH_MOUSE_PERMISSION =
        new AWTPermission("watchMousePointer");

    public static final AWTPermission SET_WINDOW_ALWAYS_ON_TOP_PERMISSION =
        new AWTPermission("setWindowAlwaysOnTop");

    public static final AWTPermission ALL_AWT_EVENTS_PERMISSION =
        new AWTPermission("listenToAllAWTEvents");

    public static final AWTPermission ACCESS_SYSTEM_TRAY_PERMISSION =
        new AWTPermission("accessSystemTray");
}
