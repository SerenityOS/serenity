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

package com.apple.eawt.event;

import sun.awt.SunToolkit;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import java.lang.annotation.Native;

final class GestureHandler {
    private static final String CLIENT_PROPERTY = "com.apple.eawt.event.internalGestureHandler";

    // native constants for the supported types of high-level gestures
    @Native static final int PHASE = 1;
    @Native static final int ROTATE = 2;
    @Native static final int MAGNIFY = 3;
    @Native static final int SWIPE = 4;

    // installs a private instance of GestureHandler, if necessary
    static void addGestureListenerTo(final JComponent component, final GestureListener listener) {
        final Object value = component.getClientProperty(CLIENT_PROPERTY);
        if (value instanceof GestureHandler) {
            ((GestureHandler)value).addListener(listener);
            return;
        }

        if (value != null) return; // some other garbage is in our client property

        final GestureHandler newHandler = new GestureHandler();
        newHandler.addListener(listener);
        component.putClientProperty(CLIENT_PROPERTY, newHandler);
    }

    // asks the installed GestureHandler to remove it's listener (does not uninstall the GestureHandler)
    static void removeGestureListenerFrom(final JComponent component, final GestureListener listener) {
        final Object value = component.getClientProperty(CLIENT_PROPERTY);
        if (!(value instanceof GestureHandler)) return;
        ((GestureHandler)value).removeListener(listener);
    }


    // called from native - finds the deepest component with an installed GestureHandler,
    // creates a single event, and dispatches it to a recursive notifier
    static void handleGestureFromNative(final Window window, final int type, final double x, final double y, final double a, final double b) {
        if (window == null) return; // should never happen...

        SunToolkit.executeOnEventHandlerThread(window, new Runnable() {
            public void run() {
                final Component component = SwingUtilities.getDeepestComponentAt(window, (int)x, (int)y);

                final PerComponentNotifier firstNotifier;
                if (component instanceof RootPaneContainer) {
                    firstNotifier = getNextNotifierForComponent(((RootPaneContainer)component).getRootPane());
                } else {
                    firstNotifier = getNextNotifierForComponent(component);
                }
                if (firstNotifier == null) return;

                switch (type) {
                    case PHASE:
                        firstNotifier.recursivelyHandlePhaseChange(a, new GesturePhaseEvent());
                        return;
                    case ROTATE:
                        firstNotifier.recursivelyHandleRotate(new RotationEvent(a));
                        return;
                    case MAGNIFY:
                        firstNotifier.recursivelyHandleMagnify(new MagnificationEvent(a));
                        return;
                    case SWIPE:
                        firstNotifier.recursivelyHandleSwipe(a, b, new SwipeEvent());
                        return;
                }
            }
        });
    }


    final List<GesturePhaseListener> phasers = new LinkedList<GesturePhaseListener>();
    final List<RotationListener> rotaters = new LinkedList<RotationListener>();
    final List<MagnificationListener> magnifiers = new LinkedList<MagnificationListener>();
    final List<SwipeListener> swipers = new LinkedList<SwipeListener>();

    GestureHandler() { }

    void addListener(final GestureListener listener) {
        if (listener instanceof GesturePhaseListener) phasers.add((GesturePhaseListener)listener);
        if (listener instanceof RotationListener) rotaters.add((RotationListener)listener);
        if (listener instanceof MagnificationListener) magnifiers.add((MagnificationListener)listener);
        if (listener instanceof SwipeListener) swipers.add((SwipeListener)listener);
    }

    void removeListener(final GestureListener listener) {
        phasers.remove(listener);
        rotaters.remove(listener);
        magnifiers.remove(listener);
        swipers.remove(listener);
    }

    // notifies all listeners in a particular component/handler pair
    // and recursively notifies up the component hierarchy
    static class PerComponentNotifier {
        final Component component;
        final GestureHandler handler;

        public PerComponentNotifier(final Component component, final GestureHandler handler) {
            this.component = component;
            this.handler = handler;
        }

        void recursivelyHandlePhaseChange(final double phase, final GesturePhaseEvent e) {
            for (final GesturePhaseListener listener : handler.phasers) {
                if (phase < 0) {
                    listener.gestureBegan(e);
                } else {
                    listener.gestureEnded(e);
                }
                if (e.isConsumed()) return;
            }

            final PerComponentNotifier next = getNextNotifierForComponent(component.getParent());
            if (next != null) next.recursivelyHandlePhaseChange(phase, e);
        }

        void recursivelyHandleRotate(final RotationEvent e) {
            for (final RotationListener listener : handler.rotaters) {
                listener.rotate(e);
                if (e.isConsumed()) return;
            }

            final PerComponentNotifier next = getNextNotifierForComponent(component.getParent());
            if (next != null) next.recursivelyHandleRotate(e);
        }

        void recursivelyHandleMagnify(final MagnificationEvent e) {
            for (final MagnificationListener listener : handler.magnifiers) {
                listener.magnify(e);
                if (e.isConsumed()) return;
            }

            final PerComponentNotifier next = getNextNotifierForComponent(component.getParent());
            if (next != null) next.recursivelyHandleMagnify(e);
        }

        void recursivelyHandleSwipe(final double x, final double y, final SwipeEvent e) {
            for (final SwipeListener listener : handler.swipers) {
                if (x < 0) listener.swipedLeft(e);
                if (x > 0) listener.swipedRight(e);
                if (y < 0) listener.swipedDown(e);
                if (y > 0) listener.swipedUp(e);
                if (e.isConsumed()) return;
            }

            final PerComponentNotifier next = getNextNotifierForComponent(component.getParent());
            if (next != null) next.recursivelyHandleSwipe(x, y, e);
        }
    }

    // helper function to get a handler from a Component
    static GestureHandler getHandlerForComponent(final Component c) {
        if (!(c instanceof JComponent)) return null;
        final Object value = ((JComponent)c).getClientProperty(CLIENT_PROPERTY);
        if (!(value instanceof GestureHandler)) return null;
        return (GestureHandler)value;
    }

    // recursive helper to find the next component/handler pair
    static PerComponentNotifier getNextNotifierForComponent(final Component c) {
        if (c == null) return null;

        final GestureHandler handler = getHandlerForComponent(c);
        if (handler != null) {
            return new PerComponentNotifier(c, handler);
        }

        return getNextNotifierForComponent(c.getParent());
    }
}
