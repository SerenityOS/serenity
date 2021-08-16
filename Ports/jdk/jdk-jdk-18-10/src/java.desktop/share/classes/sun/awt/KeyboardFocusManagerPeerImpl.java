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
package sun.awt;

import java.awt.Component;
import java.awt.Window;
import java.awt.Canvas;
import java.awt.Scrollbar;
import java.awt.Panel;

import java.awt.event.FocusEvent;

import java.awt.peer.KeyboardFocusManagerPeer;
import java.awt.peer.ComponentPeer;

import sun.awt.AWTAccessor.ComponentAccessor;
import sun.util.logging.PlatformLogger;

public abstract class KeyboardFocusManagerPeerImpl implements KeyboardFocusManagerPeer {

    private static final PlatformLogger focusLog = PlatformLogger.getLogger("sun.awt.focus.KeyboardFocusManagerPeerImpl");

    private static class KfmAccessor {
        private static AWTAccessor.KeyboardFocusManagerAccessor instance =
                AWTAccessor.getKeyboardFocusManagerAccessor();
    }

    // The constants are copied from java.awt.KeyboardFocusManager
    public static final int SNFH_FAILURE         = 0;
    public static final int SNFH_SUCCESS_HANDLED = 1;
    public static final int SNFH_SUCCESS_PROCEED = 2;

    @Override
    public void clearGlobalFocusOwner(Window activeWindow) {
        if (activeWindow != null) {
            Component focusOwner = activeWindow.getFocusOwner();
            if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                focusLog.fine("Clearing global focus owner " + focusOwner);
            }
            if (focusOwner != null) {
                FocusEvent fl = new FocusEvent(focusOwner, FocusEvent.FOCUS_LOST, false, null,
                                                     FocusEvent.Cause.CLEAR_GLOBAL_FOCUS_OWNER);
                SunToolkit.postPriorityEvent(fl);
            }
        }
    }

    /*
     * WARNING: Don't call it on the Toolkit thread.
     *
     * Checks if the component:
     * 1) accepts focus on click (in general)
     * 2) may be a focus owner (in particular)
     */
    public static boolean shouldFocusOnClick(Component component) {
        boolean acceptFocusOnClick = false;

        // A component is generally allowed to accept focus on click
        // if its peer is focusable. There're some exceptions though.


        // CANVAS & SCROLLBAR accept focus on click
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        if (component instanceof Canvas ||
            component instanceof Scrollbar)
        {
            acceptFocusOnClick = true;

        // PANEL, empty only, accepts focus on click
        } else if (component instanceof Panel) {
            acceptFocusOnClick = (((Panel)component).getComponentCount() == 0);


        // Other components
        } else {
            ComponentPeer peer = (component != null ? acc.getPeer(component) : null);
            acceptFocusOnClick = (peer != null ? peer.isFocusable() : false);
        }
        return acceptFocusOnClick && acc.canBeFocusOwner(component);
    }

    /*
     * Posts proper lost/gain focus events to the event queue.
     */
    @SuppressWarnings("deprecation")
    public static boolean deliverFocus(Component lightweightChild,
                                       Component target,
                                       boolean temporary,
                                       boolean focusedWindowChangeAllowed,
                                       long time,
                                       FocusEvent.Cause cause,
                                       Component currentFocusOwner) // provided by the descendant peers
    {
        if (lightweightChild == null) {
            lightweightChild = target;
        }

        Component currentOwner = currentFocusOwner;
        if (currentOwner != null && !currentOwner.isDisplayable()) {
            currentOwner = null;
        }
        if (currentOwner != null) {
            FocusEvent fl = new FocusEvent(currentOwner, FocusEvent.FOCUS_LOST,
                                                 false, lightweightChild, cause);

            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("Posting focus event: " + fl);
            }
            SunToolkit.postEvent(SunToolkit.targetToAppContext(currentOwner), fl);
        }

        FocusEvent fg = new FocusEvent(lightweightChild, FocusEvent.FOCUS_GAINED,
                                             false, currentOwner, cause);

        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("Posting focus event: " + fg);
        }
        SunToolkit.postEvent(SunToolkit.targetToAppContext(lightweightChild), fg);
        return true;
    }

    // WARNING: Don't call it on the Toolkit thread.
    public static void requestFocusFor(Component target, FocusEvent.Cause cause) {
        AWTAccessor.getComponentAccessor().requestFocus(target, cause);
    }

    // WARNING: Don't call it on the Toolkit thread.
    public static int shouldNativelyFocusHeavyweight(Component heavyweight,
                                                     Component descendant,
                                                     boolean temporary,
                                                     boolean focusedWindowChangeAllowed,
                                                     long time,
                                                     FocusEvent.Cause cause)
    {
        return KfmAccessor.instance.shouldNativelyFocusHeavyweight(
            heavyweight, descendant, temporary, focusedWindowChangeAllowed,
                time, cause);
    }

    public static void removeLastFocusRequest(Component heavyweight) {
        KfmAccessor.instance.removeLastFocusRequest(heavyweight);
    }

    // WARNING: Don't call it on the Toolkit thread.
    public static boolean processSynchronousLightweightTransfer(Component heavyweight,
                                                                Component descendant,
                                                                boolean temporary,
                                                                boolean focusedWindowChangeAllowed,
                                                                long time)
    {
        return KfmAccessor.instance.processSynchronousLightweightTransfer(
            heavyweight, descendant, temporary, focusedWindowChangeAllowed,
                time);
    }
}
