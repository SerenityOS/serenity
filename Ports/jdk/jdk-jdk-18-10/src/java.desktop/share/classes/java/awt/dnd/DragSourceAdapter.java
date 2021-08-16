/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * An abstract adapter class for receiving drag source events. The methods in
 * this class are empty. This class exists only as a convenience for creating
 * listener objects.
 * <p>
 * Extend this class to create a {@code DragSourceEvent} listener
 * and override the methods for the events of interest. (If you implement the
 * {@code DragSourceListener} interface, you have to define all of
 * the methods in it. This abstract class defines null methods for them
 * all, so you only have to define methods for events you care about.)
 * <p>
 * Create a listener object using the extended class and then register it with
 * a {@code DragSource}. When the drag enters, moves over, or exits
 * a drop site, when the drop action changes, and when the drag ends, the
 * relevant method in the listener object is invoked, and the
 * {@code DragSourceEvent} is passed to it.
 * <p>
 * The drop site is <i>associated with the previous {@code dragEnter()}
 * invocation</i> if the latest invocation of {@code dragEnter()} on this
 * adapter corresponds to that drop site and is not followed by a
 * {@code dragExit()} invocation on this adapter.
 *
 * @see DragSourceEvent
 * @see DragSourceListener
 * @see DragSourceMotionListener
 *
 * @author David Mendenhall
 * @since 1.4
 */
public abstract class DragSourceAdapter
    implements DragSourceListener, DragSourceMotionListener {

    /**
     * Constructs a {@code DragSourceAdapter}.
     */
    protected DragSourceAdapter() {}

    /**
     * Called as the cursor's hotspot enters a platform-dependent drop site.
     * This method is invoked when all the following conditions are true:
     * <UL>
     * <LI>The cursor's hotspot enters the operable part of
     * a platform-dependent drop site.
     * <LI>The drop site is active.
     * <LI>The drop site accepts the drag.
     * </UL>
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    public void dragEnter(DragSourceDragEvent dsde) {}

    /**
     * Called as the cursor's hotspot moves over a platform-dependent drop site.
     * This method is invoked when all the following conditions are true:
     * <UL>
     * <LI>The cursor's hotspot has moved, but still intersects the
     * operable part of the drop site associated with the previous
     * dragEnter() invocation.
     * <LI>The drop site is still active.
     * <LI>The drop site accepts the drag.
     * </UL>
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    public void dragOver(DragSourceDragEvent dsde) {}

    /**
     * Called whenever the mouse is moved during a drag operation.
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    public void dragMouseMoved(DragSourceDragEvent dsde) {}

    /**
     * Called when the user has modified the drop gesture.
     * This method is invoked when the state of the input
     * device(s) that the user is interacting with changes.
     * Such devices are typically the mouse buttons or keyboard
     * modifiers that the user is interacting with.
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    public void dropActionChanged(DragSourceDragEvent dsde) {}

    /**
     * Called as the cursor's hotspot exits a platform-dependent drop site.
     * This method is invoked when any of the following conditions are true:
     * <UL>
     * <LI>The cursor's hotspot no longer intersects the operable part
     * of the drop site associated with the previous dragEnter() invocation.
     * </UL>
     * OR
     * <UL>
     * <LI>The drop site associated with the previous dragEnter() invocation
     * is no longer active.
     * </UL>
     * OR
     * <UL>
     * <LI> The drop site associated with the previous dragEnter() invocation
     * has rejected the drag.
     * </UL>
     *
     * @param dse the {@code DragSourceEvent}
     */
    public void dragExit(DragSourceEvent dse) {}

    /**
     * This method is invoked to signify that the Drag and Drop
     * operation is complete. The getDropSuccess() method of
     * the {@code DragSourceDropEvent} can be used to
     * determine the termination state. The getDropAction() method
     * returns the operation that the drop site selected
     * to apply to the Drop operation. Once this method is complete, the
     * current {@code DragSourceContext} and
     * associated resources become invalid.
     *
     * @param dsde the {@code DragSourceDropEvent}
     */
    public void dragDropEnd(DragSourceDropEvent dsde) {}
}
