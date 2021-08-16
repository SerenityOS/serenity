/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * A listener interface for receiving mouse motion events during a drag
 * operation.
 * <p>
 * The class that is interested in processing mouse motion events during
 * a drag operation either implements this interface or extends the abstract
 * {@code DragSourceAdapter} class (overriding only the methods of
 * interest).
 * <p>
 * Create a listener object using that class and then register it with
 * a {@code DragSource}. Whenever the mouse moves during a drag
 * operation initiated with this {@code DragSource}, that object's
 * {@code dragMouseMoved} method is invoked, and the
 * {@code DragSourceDragEvent} is passed to it.
 *
 * @see DragSourceDragEvent
 * @see DragSource
 * @see DragSourceListener
 * @see DragSourceAdapter
 *
 * @since 1.4
 */

public interface DragSourceMotionListener extends EventListener {

    /**
     * Called whenever the mouse is moved during a drag operation.
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    void dragMouseMoved(DragSourceDragEvent dsde);
}
