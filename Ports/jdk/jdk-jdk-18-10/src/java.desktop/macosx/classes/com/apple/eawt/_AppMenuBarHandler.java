/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Container;
import java.awt.Frame;


import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.plaf.MenuBarUI;

import com.apple.laf.ScreenMenuBar;
import sun.awt.AWTAccessor;
import sun.lwawt.macosx.CMenuBar;

import com.apple.laf.AquaMenuBarUI;

class _AppMenuBarHandler {
    private static final int MENU_ABOUT = 1;
    private static final int MENU_PREFS = 2;

    private static native void nativeSetMenuState(final int menu, final boolean visible, final boolean enabled);
    private static native void nativeSetDefaultMenuBar(final long menuBarPeer);
    private static native void nativeActivateDefaultMenuBar(final long menuBarPeer);

    static final _AppMenuBarHandler instance = new _AppMenuBarHandler();
    static _AppMenuBarHandler getInstance() {
        return instance;
    }

    private static ScreenMenuBar defaultMenuBar;

    // callback from the native delegate -init function
    private static void initMenuStates(final boolean aboutMenuItemVisible,
                                       final boolean aboutMenuItemEnabled,
                                       final boolean prefsMenuItemVisible,
                                       final boolean prefsMenuItemEnabled) {
        synchronized (instance) {
            instance.aboutMenuItemVisible = aboutMenuItemVisible;
            instance.aboutMenuItemEnabled = aboutMenuItemEnabled;
            instance.prefsMenuItemVisible = prefsMenuItemVisible;
            instance.prefsMenuItemEnabled = prefsMenuItemEnabled;
        }
    }

    _AppMenuBarHandler() { }

    boolean aboutMenuItemVisible;
    boolean aboutMenuItemEnabled;

    boolean prefsMenuItemVisible;
    boolean prefsMenuItemEnabled;
    boolean prefsMenuItemExplicitlySet;

    void setDefaultMenuBar(final JMenuBar menuBar) {
        installDefaultMenuBar(menuBar);
    }

    static boolean isMenuBarActivationNeeded() {
        // scan the current frames, and see if any are foreground
        final Frame[] frames = Frame.getFrames();
        for (final Frame frame : frames) {
            if (frame.isVisible() && !isFrameMinimized(frame)) {
                return false;
            }
        }

        return true;
    }

    static boolean isFrameMinimized(final Frame frame) {
        return (frame.getExtendedState() & Frame.ICONIFIED) != 0;
    }

    static void installDefaultMenuBar(final JMenuBar menuBar) {

        if (menuBar == null) {
            // intentionally clearing the default menu
            if (defaultMenuBar != null) {
                defaultMenuBar.removeNotify();
                defaultMenuBar = null;
            }
            nativeSetDefaultMenuBar(0);
            return;
        }

        Container parent = menuBar.getParent();
        if (parent instanceof JLayeredPane) {
            ((JLayeredPane) parent).remove(menuBar);
        }

        MenuBarUI ui = menuBar.getUI();
        if (!(ui instanceof AquaMenuBarUI)) {
            ui = new AquaMenuBarUI();
            menuBar.setUI(ui);
        }

        final AquaMenuBarUI aquaUI = (AquaMenuBarUI)ui;
        final ScreenMenuBar screenMenuBar = aquaUI.getScreenMenuBar();
        if (screenMenuBar == null) {
            // Aqua is installed, but we aren't using the screen menu bar
            throw new IllegalStateException("Application.setDefaultMenuBar() only works if apple.laf.useScreenMenuBar=true");
        }

        if (screenMenuBar != defaultMenuBar) {
            if (defaultMenuBar != null) {
                defaultMenuBar.removeNotify();
            }
            defaultMenuBar = screenMenuBar;
            screenMenuBar.addNotify();
        }

        final Object peer = AWTAccessor.getMenuComponentAccessor().getPeer(screenMenuBar);
        if (!(peer instanceof CMenuBar)) {
            // such a thing should not be possible
            throw new IllegalStateException("Unable to determine native menu bar from provided JMenuBar");
        }

        // grab the pointer to the CMenuBar, and retain it in native
        ((CMenuBar) peer).execute(_AppMenuBarHandler::nativeSetDefaultMenuBar);

        // if there is no currently active frame, install the default menu bar in the application main menu
        if (isMenuBarActivationNeeded()) {
            ((CMenuBar) peer).execute(_AppMenuBarHandler::nativeActivateDefaultMenuBar);
        }
    }

    void setAboutMenuItemVisible(final boolean present) {
        synchronized (this) {
            if (aboutMenuItemVisible == present) return;
            aboutMenuItemVisible = present;
        }

        nativeSetMenuState(MENU_ABOUT, aboutMenuItemVisible, aboutMenuItemEnabled);
    }

    void setPreferencesMenuItemVisible(final boolean present) {
        synchronized (this) {
            prefsMenuItemExplicitlySet = true;
            if (prefsMenuItemVisible == present) return;
            prefsMenuItemVisible = present;
        }
        nativeSetMenuState(MENU_PREFS, prefsMenuItemVisible, prefsMenuItemEnabled);
    }

    void setAboutMenuItemEnabled(final boolean enable) {
        synchronized (this) {
            if (aboutMenuItemEnabled == enable) return;
            aboutMenuItemEnabled = enable;
        }
        nativeSetMenuState(MENU_ABOUT, aboutMenuItemVisible, aboutMenuItemEnabled);
    }

    void setPreferencesMenuItemEnabled(final boolean enable) {
        synchronized (this) {
            prefsMenuItemExplicitlySet = true;
            if (prefsMenuItemEnabled == enable) return;
            prefsMenuItemEnabled = enable;
        }
        nativeSetMenuState(MENU_PREFS, prefsMenuItemVisible, prefsMenuItemEnabled);
    }

    boolean isAboutMenuItemVisible() {
        return aboutMenuItemVisible;
    }

    boolean isPreferencesMenuItemVisible() {
        return prefsMenuItemVisible;
    }

    boolean isAboutMenuItemEnabled() {
        return aboutMenuItemEnabled;
    }

    boolean isPreferencesMenuItemEnabled() {
        return prefsMenuItemEnabled;
    }
}
