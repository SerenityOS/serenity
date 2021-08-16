/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * The listener interface for receiving ancestor moved and resized events.
 * The class that is interested in processing these events either implements
 * this interface (and all the methods it contains) or extends the abstract
 * {@code HierarchyBoundsAdapter} class (overriding only the method of
 * interest).
 * The listener object created from that class is then registered with a
 * Component using the Component's {@code addHierarchyBoundsListener}
 * method. When the hierarchy to which the Component belongs changes by
 * the resizing or movement of an ancestor, the relevant method in the listener
 * object is invoked, and the {@code HierarchyEvent} is passed to it.
 * <p>
 * Hierarchy events are provided for notification purposes ONLY;
 * The AWT will automatically handle changes to the hierarchy internally so
 * that GUI layout works properly regardless of whether a
 * program registers an {@code HierarchyBoundsListener} or not.
 *
 * @author      David Mendenhall
 * @see         HierarchyBoundsAdapter
 * @see         HierarchyEvent
 * @since       1.3
 */
public interface HierarchyBoundsListener extends EventListener {
    /**
     * Called when an ancestor of the source is moved.
     * @param e the event to be processed
     */
    public void ancestorMoved(HierarchyEvent e);

    /**
     * Called when an ancestor of the source is resized.
     * @param e the event to be processed
     */
    public void ancestorResized(HierarchyEvent e);
}
