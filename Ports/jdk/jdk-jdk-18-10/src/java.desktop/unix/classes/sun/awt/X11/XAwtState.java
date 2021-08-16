/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This class is a placeholder for all internal static objects that represent
 * system state. We keep our representation up-to-date with actual system
 * state by tracking events, such as X Focus, Component under cursor etc.
 * All attributes should be private static with accessors to simpify change
 * tracking.
 */
package sun.awt.X11;

import java.awt.Component;
import java.lang.ref.WeakReference;

class XAwtState {
    /**
     * The mouse is over this component.
     * If the component is not disabled, it received MOUSE_ENTERED but no MOUSE_EXITED.
     */
    private static WeakReference<Component> componentMouseEnteredRef = null;

    static void setComponentMouseEntered(Component component) {
        XToolkit.awtLock();
        try {
            if (component == null) {
                componentMouseEnteredRef = null;
                return;
            }
            if (component != getComponentMouseEntered()) {
                componentMouseEnteredRef = new WeakReference<>(component);
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    static Component getComponentMouseEntered() {
        XToolkit.awtLock();
        try {
            if (componentMouseEnteredRef == null) {
                return null;
            }
            return componentMouseEnteredRef.get();
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /**
     * The XBaseWindow is created with OwnerGrabButtonMask
     * (see X vol. 1, 8.3.3.2) so inside the app Key, Motion, and Button events
     * are received by the window they actualy happened on, not the grabber.
     * Then XBaseWindow dispatches them to the grabber. As a result
     * XAnyEvent.get_window() returns actual window the event is originated,
     * though the event is dispatched by  the grabber.
     */
    private static boolean inManualGrab = false;

    static boolean isManualGrab() {
        return inManualGrab;
    }

    private static WeakReference<XBaseWindow> grabWindowRef = null;

    /**
     * The X Active Grab overrides any other active grab by the same
     * client see XGrabPointer, XGrabKeyboard
     */
    static void setGrabWindow(XBaseWindow grabWindow) {
        setGrabWindow(grabWindow, false);
    }

    /**
     * Automatic passive grab doesn't override active grab see XGrabButton
     */
    static void setAutoGrabWindow(XBaseWindow grabWindow) {
        setGrabWindow(grabWindow, true);
    }

    private static void setGrabWindow(XBaseWindow grabWindow, boolean isAutoGrab) {
        XToolkit.awtLock();
        try {
            if (inManualGrab && isAutoGrab) {
                return;
            }
            inManualGrab = grabWindow != null && !isAutoGrab;
            if (grabWindow == null) {
                grabWindowRef = null;
                return;
            }
            if (grabWindow != getGrabWindow()) {
                grabWindowRef = new WeakReference<>(grabWindow);
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    static XBaseWindow getGrabWindow() {
        XToolkit.awtLock();
        try {
            if (grabWindowRef == null) {
                return null;
            }
            XBaseWindow xbw = grabWindowRef.get();
            if( xbw != null && xbw.isDisposed() ) {
                xbw = null;
                grabWindowRef = null;
            }else if( xbw == null ) {
                grabWindowRef = null;
            }
            return xbw;
        } finally {
            XToolkit.awtUnlock();
        }
    }
}
