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
 * An abstract adapter class for receiving drop target events. The methods in
 * this class are empty. This class exists only as a convenience for creating
 * listener objects.
 * <p>
 * Extend this class to create a {@code DropTargetEvent} listener
 * and override the methods for the events of interest. (If you implement the
 * {@code DropTargetListener} interface, you have to define all of
 * the methods in it. This abstract class defines a null implementation for
 * every method except {@code drop(DropTargetDropEvent)}, so you only have
 * to define methods for events you care about.) You must provide an
 * implementation for at least {@code drop(DropTargetDropEvent)}. This
 * method cannot have a null implementation because its specification requires
 * that you either accept or reject the drop, and, if accepted, indicate
 * whether the drop was successful.
 * <p>
 * Create a listener object using the extended class and then register it with
 * a {@code DropTarget}. When the drag enters, moves over, or exits
 * the operable part of the drop site for that {@code DropTarget}, when
 * the drop action changes, and when the drop occurs, the relevant method in
 * the listener object is invoked, and the {@code DropTargetEvent} is
 * passed to it.
 * <p>
 * The operable part of the drop site for the {@code DropTarget} is
 * the part of the associated {@code Component}'s geometry that is not
 * obscured by an overlapping top-level window or by another
 * {@code Component} higher in the Z-order that has an associated active
 * {@code DropTarget}.
 * <p>
 * During the drag, the data associated with the current drag operation can be
 * retrieved by calling {@code getTransferable()} on
 * {@code DropTargetDragEvent} instances passed to the listener's
 * methods.
 * <p>
 * Note that {@code getTransferable()} on the
 * {@code DropTargetDragEvent} instance should only be called within the
 * respective listener's method and all the necessary data should be retrieved
 * from the returned {@code Transferable} before that method returns.
 *
 * @see DropTargetEvent
 * @see DropTargetListener
 *
 * @author David Mendenhall
 * @since 1.4
 */
public abstract class DropTargetAdapter implements DropTargetListener {

    /**
     * Constructs a {@code DropTargetAdapter}.
     */
    protected DropTargetAdapter() {}

    /**
     * Called while a drag operation is ongoing, when the mouse pointer enters
     * the operable part of the drop site for the {@code DropTarget}
     * registered with this listener.
     *
     * @param dtde the {@code DropTargetDragEvent}
     */
    public void dragEnter(DropTargetDragEvent dtde) {}

    /**
     * Called when a drag operation is ongoing, while the mouse pointer is still
     * over the operable part of the drop site for the {@code DropTarget}
     * registered with this listener.
     *
     * @param dtde the {@code DropTargetDragEvent}
     */
    public void dragOver(DropTargetDragEvent dtde) {}

    /**
     * Called if the user has modified
     * the current drop gesture.
     *
     * @param dtde the {@code DropTargetDragEvent}
     */
    public void dropActionChanged(DropTargetDragEvent dtde) {}

    /**
     * Called while a drag operation is ongoing, when the mouse pointer has
     * exited the operable part of the drop site for the
     * {@code DropTarget} registered with this listener.
     *
     * @param dte the {@code DropTargetEvent}
     */
    public void dragExit(DropTargetEvent dte) {}
}
