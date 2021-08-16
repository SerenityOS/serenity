/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EventListener;
import java.awt.AWTEvent;

/**
 * The listener interface for receiving notification of events
 * dispatched to objects that are instances of Component or
 * MenuComponent or their subclasses.  Unlike the other EventListeners
 * in this package, AWTEventListeners passively observe events
 * being dispatched in the AWT, system-wide.  Most applications
 * should never use this class; applications which might use
 * AWTEventListeners include event recorders for automated testing,
 * and facilities such as the Java Accessibility package.
 * <p>
 * The class that is interested in monitoring AWT events
 * implements this interface, and the object created with that
 * class is registered with the Toolkit, using the Toolkit's
 * {@code addAWTEventListener} method.  When an event is
 * dispatched anywhere in the AWT, that object's
 * {@code eventDispatched} method is invoked.
 *
 * @see java.awt.AWTEvent
 * @see java.awt.Toolkit#addAWTEventListener
 * @see java.awt.Toolkit#removeAWTEventListener
 *
 * @author Fred Ecks
 * @since 1.2
 */
public interface AWTEventListener extends EventListener {

    /**
     * Invoked when an event is dispatched in the AWT.
     * @param event the event to be processed
     */
    public void eventDispatched(AWTEvent event);

}
