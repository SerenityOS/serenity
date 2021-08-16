/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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


package sun.lwawt.macosx;

import java.awt.AWTKeyStroke;
import java.awt.Point;
import java.awt.Toolkit;

import sun.awt.AWTAccessor;
import sun.awt.EmbeddedFrame;
import sun.lwawt.LWWindowPeer;

@SuppressWarnings("serial") // JDK implementation class
public class CEmbeddedFrame extends EmbeddedFrame {

    private CPlatformResponder responder;
    private static final Object classLock = new Object();
    private static volatile CEmbeddedFrame globalFocusedWindow;
    private CEmbeddedFrame browserWindowFocusedApplet;
    private boolean parentWindowActive = true;

    public CEmbeddedFrame() {
        show();
    }

    public void addNotify() {
        if (!isDisplayable()) {
            LWCToolkit toolkit = (LWCToolkit)Toolkit.getDefaultToolkit();
            LWWindowPeer peer = toolkit.createEmbeddedFrame(this);
            setPeer(peer);
            responder = new CPlatformResponder(peer, true);
        }
        super.addNotify();
    }

    public void registerAccelerator(AWTKeyStroke stroke) {}

    public void unregisterAccelerator(AWTKeyStroke stroke) {}

    protected long getLayerPtr() {
        return AWTAccessor.getComponentAccessor().<LWWindowPeer>getPeer(this)
                          .getLayerPtr();
    }

    // -----------------------------------------------------------------------
    //                          SYNTHETIC EVENT DELIVERY
    // -----------------------------------------------------------------------

    public void handleMouseEvent(int eventType, int modifierFlags, double pluginX,
                                 double pluginY, int buttonNumber, int clickCount) {
        int x = (int)pluginX;
        int y = (int)pluginY;
        Point locationOnScreen = getLocationOnScreen();
        int absX = locationOnScreen.x + x;
        int absY = locationOnScreen.y + y;

        if (eventType == CocoaConstants.NPCocoaEventMouseEntered) {
            CCursorManager.nativeSetAllowsCursorSetInBackground(true);
        } else if (eventType == CocoaConstants.NPCocoaEventMouseExited) {
            CCursorManager.nativeSetAllowsCursorSetInBackground(false);
        }

        responder.handleMouseEvent(eventType, modifierFlags, buttonNumber,
                                   clickCount, x, y, absX, absY);
    }

    public void handleScrollEvent(double pluginX, double pluginY, int modifierFlags,
                                  double deltaX, double deltaY, double deltaZ) {
        int x = (int)pluginX;
        int y = (int)pluginY;
        Point locationOnScreen = getLocationOnScreen();
        int absX = locationOnScreen.x + x;
        int absY = locationOnScreen.y + y;

        responder.handleScrollEvent(x, y, absX, absY, modifierFlags, deltaX,
                                    deltaY, NSEvent.SCROLL_PHASE_UNSUPPORTED);
    }

    public void handleKeyEvent(int eventType, int modifierFlags, String characters,
                               String charsIgnoringMods, boolean isRepeat, short keyCode,
                               boolean needsKeyTyped) {
        responder.handleKeyEvent(eventType, modifierFlags, characters, charsIgnoringMods,
                keyCode, needsKeyTyped, isRepeat);
    }

    public void handleInputEvent(String text) {
        responder.handleInputEvent(text);
    }

    // handleFocusEvent is called when the applet becames focused/unfocused.
    // This method can be called from different threads.
    public void handleFocusEvent(boolean focused) {
        synchronized (classLock) {
            // In some cases an applet may not receive the focus lost event
            // from the parent window (see 8012330)
            globalFocusedWindow = (focused) ? this
                    : ((globalFocusedWindow == this) ? null : globalFocusedWindow);
        }
        if (globalFocusedWindow == this) {
            // see bug 8010925
            // we can't put this to handleWindowFocusEvent because
            // it won't be invoced if focuse is moved to a html element
            // on the same page.
            CClipboard clipboard = (CClipboard) Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.checkPasteboardAndNotify();
        }
        if (parentWindowActive) {
            responder.handleWindowFocusEvent(focused, null);
        }
    }

    /**
     * When the parent window is activated this method is called for all EmbeddedFrames in it.
     *
     * For the CEmbeddedFrame which had focus before the deactivation this method triggers
     * focus events in the following order:
     *  1. WINDOW_ACTIVATED for this EmbeddedFrame
     *  2. WINDOW_GAINED_FOCUS for this EmbeddedFrame
     *  3. FOCUS_GAINED for the most recent focus owner in this EmbeddedFrame
     *
     * The caller must not requestFocus on the EmbeddedFrame together with calling this method.
     *
     * @param parentWindowActive true if the window is activated, false otherwise
     */
    // handleWindowFocusEvent is called for all applets, when the browser
    // becomes active/inactive. This event should be filtered out for
    // non-focused applet. This method can be called from different threads.
    public void handleWindowFocusEvent(boolean parentWindowActive) {
        this.parentWindowActive = parentWindowActive;
        // If several applets are running in different browser's windows, it is necessary to
        // detect the switching between the parent windows and update globalFocusedWindow accordingly.
        synchronized (classLock) {
            if (!parentWindowActive) {
                this.browserWindowFocusedApplet = globalFocusedWindow;
            }
            if (parentWindowActive && globalFocusedWindow != this && isParentWindowChanged()) {
                // It looks like we have switched to another browser window, let's restore focus to
                // the previously focused applet in this window. If no applets were focused in the
                // window, we will set focus to the first applet in the window.
                globalFocusedWindow = (this.browserWindowFocusedApplet != null) ? this.browserWindowFocusedApplet
                        : this;
            }
        }
        if (globalFocusedWindow == this) {
            responder.handleWindowFocusEvent(parentWindowActive, null);
        }
    }

    public boolean isParentWindowActive() {
        return parentWindowActive;
    }

    private boolean isParentWindowChanged() {
        // If globalFocusedWindow is located at inactive parent window or null, we have swithed to
        // another window.
        return globalFocusedWindow != null ? !globalFocusedWindow.isParentWindowActive() : true;
    }

    @Override
    public void synthesizeWindowActivation(boolean doActivate) {
        if (isParentWindowActive() != doActivate) {
            handleWindowFocusEvent(doActivate);
        }
    }

    public static void updateGlobalFocusedWindow(CEmbeddedFrame newGlobalFocusedWindow) {
        synchronized (classLock) {
            if (newGlobalFocusedWindow.isParentWindowActive()) {
                globalFocusedWindow = newGlobalFocusedWindow;
            }
        }
    }
}
