/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Abstract adapter class for receiving gesture events. This class is provided
 * as a convenience for creating listeners.
 *
 * Subclasses registered with {@link GestureUtilities#addGestureListenerTo}
 * will receive all phase, magnification, rotation, and swipe events.
 *
 * @see GestureUtilities
 *
 * @since Java for Mac OS X 10.5 Update 7, Java for Mac OS X 10.6 Update 2
 */
public abstract class GestureAdapter implements GesturePhaseListener, MagnificationListener, RotationListener, SwipeListener {
    public void gestureBegan(final GesturePhaseEvent e) { }
    public void gestureEnded(final GesturePhaseEvent e) { }
    public void magnify(final MagnificationEvent e) { }
    public void rotate(final RotationEvent e) { }
    public void swipedDown(final SwipeEvent e) { }
    public void swipedLeft(final SwipeEvent e) { }
    public void swipedRight(final SwipeEvent e) { }
    public void swipedUp(final SwipeEvent e) { }
}
