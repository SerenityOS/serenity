/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;
import javax.swing.*;

import sun.lwawt.macosx.CPlatformWindow;
import sun.swing.SwingAccessor;

class ScreenPopupFactory extends PopupFactory {
    static final Float TRANSLUCENT = 248f/255f;
    static final Float OPAQUE = 1.0f;

    boolean fIsActive = true;

    // Only popups generated with the Aqua LaF turned on will be translucent with shadows
    void setActive(final boolean b) {
        fIsActive = b;
    }

    private static Window getWindow(final Component c) {
        Component w = c;
        while(!(w instanceof Window) && (w!=null)) {
            w = w.getParent();
        }
        return (Window)w;
    }

    public Popup getPopup(final Component comp, final Component invoker, final int x, final int y) {
        if (invoker == null) throw new IllegalArgumentException("Popup.getPopup must be passed non-null contents");

        final Popup popup;
        if (fIsActive) {
            popup = SwingAccessor.getPopupFactoryAccessor()
                    .getHeavyWeightPopup(this, comp, invoker, x, y);
        } else {
            popup = super.getPopup(comp, invoker, x, y);
        }

        // Make the popup semi-translucent if it is a heavy weight
        // see <rdar://problem/3547670> JPopupMenus have incorrect background
        final Window w = getWindow(invoker);
        if (w == null) return popup;

        if (!(w instanceof RootPaneContainer)) return popup;
        final JRootPane popupRootPane = ((RootPaneContainer)w).getRootPane();

        // we need to set every time, because PopupFactory caches the heavy weight
        // TODO: CPlatformWindow constants?
        if (fIsActive) {
            popupRootPane.putClientProperty(CPlatformWindow.WINDOW_ALPHA, TRANSLUCENT);
            popupRootPane.putClientProperty(CPlatformWindow.WINDOW_SHADOW, Boolean.TRUE);
            popupRootPane.putClientProperty(CPlatformWindow.WINDOW_FADE_DELEGATE, invoker);

            w.setBackground(UIManager.getColor("PopupMenu.translucentBackground"));
            popupRootPane.putClientProperty(CPlatformWindow.WINDOW_DRAGGABLE_BACKGROUND, Boolean.FALSE);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    popupRootPane.putClientProperty(CPlatformWindow.WINDOW_SHADOW_REVALIDATE_NOW, Double.valueOf(Math.random()));
                }
            });
        } else {
            popupRootPane.putClientProperty(CPlatformWindow.WINDOW_ALPHA, OPAQUE);
            popupRootPane.putClientProperty(CPlatformWindow.WINDOW_SHADOW, Boolean.FALSE);
        }

        return popup;
    }
}
