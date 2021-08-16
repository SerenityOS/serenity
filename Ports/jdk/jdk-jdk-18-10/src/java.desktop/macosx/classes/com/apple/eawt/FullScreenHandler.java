/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.apple.eawt.event.FullScreenEvent;
import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.RootPaneContainer;

import sun.awt.SunToolkit;

import java.lang.annotation.Native;

final class FullScreenHandler {
    private static final String CLIENT_PROPERTY = "com.apple.eawt.event.internalFullScreenHandler";

    @Native static final int FULLSCREEN_WILL_ENTER = 1;
    @Native static final int FULLSCREEN_DID_ENTER = 2;
    @Native static final int FULLSCREEN_WILL_EXIT = 3;
    @Native static final int FULLSCREEN_DID_EXIT = 4;

    // installs a private instance of the handler, if necessary
    static void addFullScreenListenerTo(final RootPaneContainer window, final FullScreenListener listener) {
        final Object value = window.getRootPane().getClientProperty(CLIENT_PROPERTY);
        if (value instanceof FullScreenHandler) {
            ((FullScreenHandler)value).addListener(listener);
            return;
        }

        if (value != null) return; // some other garbage is in our client property

        final FullScreenHandler newHandler = new FullScreenHandler();
        newHandler.addListener(listener);
        window.getRootPane().putClientProperty(CLIENT_PROPERTY, newHandler);
    }

    // asks the installed FullScreenHandler to remove it's listener (does not uninstall the FullScreenHandler)
    static void removeFullScreenListenerFrom(final RootPaneContainer window, final FullScreenListener listener) {
        final Object value = window.getRootPane().getClientProperty(CLIENT_PROPERTY);
        if (!(value instanceof FullScreenHandler)) return;
        ((FullScreenHandler)value).removeListener(listener);
    }

    static FullScreenHandler getHandlerFor(final RootPaneContainer window) {
        final Object value = window.getRootPane().getClientProperty(CLIENT_PROPERTY);
        if (value instanceof FullScreenHandler) return (FullScreenHandler)value;
        return null;
    }

    // called from native
    static void handleFullScreenEventFromNative(final Window window, final int type) {
        if (!(window instanceof RootPaneContainer)) return; // handles null

        SunToolkit.executeOnEventHandlerThread(window, new Runnable() {
            public void run() {
                final FullScreenHandler handler = getHandlerFor((RootPaneContainer)window);
                if (handler != null) handler.notifyListener(new FullScreenEvent(window), type);
            }
        });
    }


    final List<FullScreenListener> listeners = new LinkedList<FullScreenListener>();

    FullScreenHandler() { }

    void addListener(final FullScreenListener listener) {
        listeners.add(listener);
    }

    void removeListener(final FullScreenListener listener) {
        listeners.remove(listener);
    }

    void notifyListener(final FullScreenEvent e, final int op) {
        for (final FullScreenListener listener : listeners) {
                switch (op) {
                case FULLSCREEN_WILL_ENTER:
                        listener.windowEnteringFullScreen(e);
                    return;
                case FULLSCREEN_DID_ENTER:
                        listener.windowEnteredFullScreen(e);
                    return;
                case FULLSCREEN_WILL_EXIT:
                        listener.windowExitingFullScreen(e);
                    return;
                case FULLSCREEN_DID_EXIT:
                        listener.windowExitedFullScreen(e);
                    return;
            }
        }
    }
}
