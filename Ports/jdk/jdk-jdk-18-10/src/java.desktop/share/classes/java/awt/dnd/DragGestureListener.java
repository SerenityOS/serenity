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



package java.awt.dnd;

import java.util.EventListener;

/**
 * The listener interface for receiving drag gesture events.
 * This interface is intended for a drag gesture recognition
 * implementation. See a specification for {@code DragGestureRecognizer}
 * for details on how to register the listener interface.
 * Upon recognition of a drag gesture the {@code
 * DragGestureRecognizer} calls this interface's
 * {@link #dragGestureRecognized dragGestureRecognized()}
 * method and passes a {@code DragGestureEvent}.

 *
 * @see java.awt.dnd.DragGestureRecognizer
 * @see java.awt.dnd.DragGestureEvent
 * @see java.awt.dnd.DragSource
 */

 public interface DragGestureListener extends EventListener {

    /**
     * This method is invoked by the {@code DragGestureRecognizer}
     * when the {@code DragGestureRecognizer} detects a platform-dependent
     * drag initiating gesture. To initiate the drag and drop operation,
     * if appropriate, {@link DragGestureEvent#startDrag startDrag()} method on
     * the {@code DragGestureEvent} has to be invoked.
     *
     * @see java.awt.dnd.DragGestureRecognizer
     * @see java.awt.dnd.DragGestureEvent
     * @param dge the {@code DragGestureEvent} describing
     * the gesture that has just occurred
     */

     void dragGestureRecognized(DragGestureEvent dge);
}
