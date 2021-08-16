/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt;

import java.awt.Component;
import java.awt.Window;
import sun.awt.KeyboardFocusManagerPeerImpl;

public class LWKeyboardFocusManagerPeer extends KeyboardFocusManagerPeerImpl {
    private static final LWKeyboardFocusManagerPeer inst = new LWKeyboardFocusManagerPeer();

    private Window focusedWindow;
    private Component focusOwner;

    public static LWKeyboardFocusManagerPeer getInstance() {
        return inst;
    }

    private LWKeyboardFocusManagerPeer() {
    }

    @Override
    public void setCurrentFocusedWindow(Window win) {
        LWWindowPeer from, to;

        synchronized (this) {
            if (focusedWindow == win) {
                return;
            }

            from = (LWWindowPeer)LWToolkit.targetToPeer(focusedWindow);
            to = (LWWindowPeer)LWToolkit.targetToPeer(win);

            focusedWindow = win;
        }

        if (from != null) {
            from.updateSecurityWarningVisibility();
        }

        if (to != null) {
            to.updateSecurityWarningVisibility();
        }
    }

    @Override
    public Window getCurrentFocusedWindow() {
        synchronized (this) {
            return focusedWindow;
        }
    }

    @Override
    public Component getCurrentFocusOwner() {
        synchronized (this) {
            return focusOwner;
        }
    }

    @Override
    public void setCurrentFocusOwner(Component comp) {
        synchronized (this) {
            focusOwner = comp;
        }
    }
}
