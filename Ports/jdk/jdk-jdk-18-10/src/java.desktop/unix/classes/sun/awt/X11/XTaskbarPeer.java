/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.X11;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Taskbar.Feature;
import java.awt.peer.TaskbarPeer;
import java.awt.event.ActionEvent;

import sun.awt.UNIXToolkit;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.security.action.GetPropertyAction;

final class XTaskbarPeer implements TaskbarPeer {

    private static boolean nativeLibraryLoaded = false;
    private static boolean initExecuted = false;

    private PopupMenu menu = null;
    private static boolean isUnity;

    static {
        @SuppressWarnings("removal")
        String de = AccessController.doPrivileged(
                        (PrivilegedAction<String>) ()
                                -> System.getenv("XDG_CURRENT_DESKTOP"));
        isUnity = de != null && de.equals("Unity");
    }

    private static void initWithLock() {
        XToolkit.awtLock();
        try {
            if (!initExecuted) {
                @SuppressWarnings("removal")
                String dname = AccessController.doPrivileged(
                                new GetPropertyAction("java.desktop.appName", ""));
                nativeLibraryLoaded = init(dname,
                        UNIXToolkit.getEnabledGtkVersion().ordinal(),
                        UNIXToolkit.isGtkVerbose());
                if (nativeLibraryLoaded) {
                    Thread t = new Thread(null, () -> { runloop(); },
                                          "TaskBar", 0, false);
                    t.setDaemon(true);
                    t.start();
                }
            }
        } finally {
            initExecuted = true;
            XToolkit.awtUnlock();
        }
    }

    XTaskbarPeer() {
        initWithLock();
    }

    static boolean isTaskbarSupported() {
        if (!isUnity) {
            return false;
        }
        initWithLock();
        return nativeLibraryLoaded;
    }

    @Override
    public boolean isSupported(Feature feature) {
        switch (feature) {
            case ICON_BADGE_NUMBER:
            case MENU:
            case PROGRESS_VALUE:
            case USER_ATTENTION:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setProgressValue(int value) {
        boolean visible
                = value >= 0
                && value <= 100;

        double v = visible
                ? (double) value / 100
                : 0d;

        updateProgress(v, visible);
    }

    @Override
    public void setIconBadge(String badge) {
        boolean visible = false;
        long val = 0;
        if (badge != null) {
            try {
                val = Long.parseLong(badge);
                visible = true;
            } catch (NumberFormatException e) {
                throw new UnsupportedOperationException("The " + Feature.ICON_BADGE_TEXT
                    + " feature is not supported on the current platform!");
            }
        }
        setBadge(val, visible);
    }

    @Override
    public PopupMenu getMenu() {
        return menu;
    }

    @Override
    public synchronized void setMenu(PopupMenu m) {
        this.menu = m;

        if (menu != null && menu.getItemCount() > 0) {
            int msize = menu.getItemCount();
            MenuItem[] items = new MenuItem[msize];
            for (int i = 0; i < msize; i++) {
                items[i] = menu.getItem(i);
            }
            setNativeMenu(items);
        } else {
            setNativeMenu(null);
        }
    }

    @Override
    public void requestUserAttention(boolean enabled, boolean critical) {
        setUrgent(enabled);
    }

    private static void menuItemCallback(MenuItem mi) {
        if (mi != null) {
            ActionEvent ae = new ActionEvent(mi, ActionEvent.ACTION_PERFORMED,
                    mi.getActionCommand());
            try {
                XToolkit.awtLock();
                XToolkit.postEvent(XToolkit.targetToAppContext(ae.getSource()), ae);
            } finally {
                XToolkit.awtUnlock();
            }
        }
    }

    private static native boolean init(String name, int version,
                                                               boolean verbose);

    private static native void runloop();

    private native void setBadge(long value, boolean visible);

    private native void updateProgress(double value, boolean visible);

    private native void setUrgent(boolean urgent);

    private native void setNativeMenu(MenuItem[] items);
}
