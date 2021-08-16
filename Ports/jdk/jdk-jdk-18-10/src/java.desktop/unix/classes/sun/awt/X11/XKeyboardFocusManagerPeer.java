/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Window;

import sun.awt.AWTAccessor;
import java.awt.event.FocusEvent;
import sun.awt.KeyboardFocusManagerPeerImpl;
import sun.util.logging.PlatformLogger;

public class XKeyboardFocusManagerPeer extends KeyboardFocusManagerPeerImpl {
    private static final PlatformLogger focusLog = PlatformLogger.getLogger("sun.awt.X11.focus.XKeyboardFocusManagerPeer");
    private static final XKeyboardFocusManagerPeer inst = new XKeyboardFocusManagerPeer();

    private Component currentFocusOwner;
    private Window currentFocusedWindow;

    public static XKeyboardFocusManagerPeer getInstance() {
        return inst;
    }

    private XKeyboardFocusManagerPeer() {
    }

    @Override
    public void setCurrentFocusOwner(Component comp) {
        synchronized (this) {
            currentFocusOwner = comp;
        }
    }

    @Override
    public Component getCurrentFocusOwner() {
        synchronized(this) {
            return currentFocusOwner;
        }
    }

    @Override
    public void setCurrentFocusedWindow(Window win) {
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("Setting current focused window " + win);
        }

        XWindowPeer from = null, to = null;

        synchronized(this) {
            if (currentFocusedWindow != null) {
                from = AWTAccessor.getComponentAccessor().getPeer(currentFocusedWindow);
            }

            currentFocusedWindow = win;

            if (currentFocusedWindow != null) {
                to = AWTAccessor.getComponentAccessor().getPeer(currentFocusedWindow);
            }
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
        synchronized(this) {
            return currentFocusedWindow;
        }
    }

    // TODO: do something to eliminate this forwarding
    public static boolean deliverFocus(Component lightweightChild,
                                       Component target,
                                       boolean temporary,
                                       boolean focusedWindowChangeAllowed,
                                       long time,
                                       FocusEvent.Cause cause)
    {
        return KeyboardFocusManagerPeerImpl.deliverFocus(lightweightChild,
                                                         target,
                                                         temporary,
                                                         focusedWindowChangeAllowed,
                                                         time,
                                                         cause,
                                                         getInstance().getCurrentFocusOwner());
    }
}
