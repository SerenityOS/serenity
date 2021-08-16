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

import java.awt.*;
import java.awt.event.InputEvent;

/**
 * Abstract event all gestures inherit from.
 *
 * Note: GestureEvent is not subclass of {@link AWTEvent} and is not dispatched
 * directly from the {@link EventQueue}. This is an intentional design decision
 * to prevent collision with an official java.awt.* gesture event types subclassing
 * {@link InputEvent}.
 *
 * {@link GestureListener}s are only notified from the AWT Event Dispatch thread.
 *
 * @see GestureUtilities
 *
 * @since Java for Mac OS X 10.5 Update 7, Java for Mac OS X 10.6 Update 2
 */
public abstract class GestureEvent {
    boolean consumed;

    GestureEvent() {
        // package private
    }

    /**
     * Consuming an event prevents listeners later in the chain or higher in the
     * component hierarchy from receiving the event.
     */
    public void consume() {
        consumed = true;
    }

    /**
     * @return if the event has been consumed
     */
    protected boolean isConsumed() {
        return consumed;
    }
}
