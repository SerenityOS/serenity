/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.windows;

import java.awt.Graphics;
import java.awt.Window;

import javax.swing.JWindow;

/**
 * A class which tags a window with a particular semantic usage,
 * either tooltip, menu, sub-menu, popup-menu, or comobobox-popup.
 * This is used as a temporary solution for getting native AWT support
 * for transition effects in Windows 98 and Windows 2000.  The native
 * code will interpret the windowType property and automatically
 * implement appropriate animation when the window is shown/hidden.
 * <p>
 * Note that support for transition effects may be supported with a
 * different mechanism in the future and so this class is
 * package-private and targeted for Swing implementation use only.
 *
 * @author Amy Fowler
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class WindowsPopupWindow extends JWindow {

    static final int UNDEFINED_WINDOW_TYPE      = 0;
    static final int TOOLTIP_WINDOW_TYPE        = 1;
    static final int MENU_WINDOW_TYPE           = 2;
    static final int SUBMENU_WINDOW_TYPE        = 3;
    static final int POPUPMENU_WINDOW_TYPE      = 4;
    static final int COMBOBOX_POPUP_WINDOW_TYPE = 5;

    private int windowType;

    WindowsPopupWindow(Window parent) {
        super(parent);
        setFocusableWindowState(false);
    }

    void setWindowType(int type) {
        windowType = type;
    }

    int getWindowType() {
        return windowType;
    }

    public void update(Graphics g) {
        paint(g);
    }

    @SuppressWarnings("deprecation")
    public void hide() {
        super.hide();
        /** We need to call removeNotify() here because hide() does
         * something only if Component.visible is true. When the app
         * frame is miniaturized, the parent frame of this frame is
         * invisible, causing AWT to believe that this frame
         *  is invisible and causing hide() to do nothing
         */
        removeNotify();
    }

    @SuppressWarnings("deprecation")
    public void show() {
        super.show();
        this.pack();
    }
}
