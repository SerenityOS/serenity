/*
 * Copyright (c) 2001, 2007, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.event;

import java.util.EventListenerProxy;
import java.awt.AWTEvent;

/**
 * A class which extends the {@code EventListenerProxy}
 * specifically for adding an {@code AWTEventListener}
 * for a specific event mask.
 * Instances of this class can be added as {@code AWTEventListener}s
 * to a {@code Toolkit} object.
 * <p>
 * The {@code getAWTEventListeners} method of {@code Toolkit}
 * can return a mixture of {@code AWTEventListener}
 * and {@code AWTEventListenerProxy} objects.
 *
 * @see java.awt.Toolkit
 * @see java.util.EventListenerProxy
 * @since 1.4
 */
public class AWTEventListenerProxy
        extends EventListenerProxy<AWTEventListener>
        implements AWTEventListener {

    private final long eventMask;

    /**
     * Constructor which binds the {@code AWTEventListener}
     * to a specific event mask.
     *
     * @param eventMask  the bitmap of event types to receive
     * @param listener   the listener object
     */
    public AWTEventListenerProxy (long eventMask, AWTEventListener listener) {
        super(listener);
        this.eventMask = eventMask;
    }

    /**
     * Forwards the AWT event to the listener delegate.
     *
     * @param event  the AWT event
     */
    public void eventDispatched(AWTEvent event) {
        getListener().eventDispatched(event);
    }

    /**
     * Returns the event mask associated with the listener.
     *
     * @return the event mask associated with the listener
     */
    public long getEventMask() {
        return this.eventMask;
    }
}
