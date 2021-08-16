/*
 * Copyright (c) 1997, 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;

/**
 * The {@code DropTargetListener} interface
 * is the callback interface used by the
 * {@code DropTarget} class to provide
 * notification of DnD operations that involve
 * the subject {@code DropTarget}. Methods of
 * this interface may be implemented to provide
 * "drag under" visual feedback to the user throughout
 * the Drag and Drop operation.
 * <p>
 * Create a listener object by implementing the interface and then register it
 * with a {@code DropTarget}. When the drag enters, moves over, or exits
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
 * @since 1.2
 */

public interface DropTargetListener extends EventListener {

    /**
     * Called while a drag operation is ongoing, when the mouse pointer enters
     * the operable part of the drop site for the {@code DropTarget}
     * registered with this listener.
     *
     * @param dtde the {@code DropTargetDragEvent}
     */

    void dragEnter(DropTargetDragEvent dtde);

    /**
     * Called when a drag operation is ongoing, while the mouse pointer is still
     * over the operable part of the drop site for the {@code DropTarget}
     * registered with this listener.
     *
     * @param dtde the {@code DropTargetDragEvent}
     */

    void dragOver(DropTargetDragEvent dtde);

    /**
     * Called if the user has modified
     * the current drop gesture.
     *
     * @param dtde the {@code DropTargetDragEvent}
     */

    void dropActionChanged(DropTargetDragEvent dtde);

    /**
     * Called while a drag operation is ongoing, when the mouse pointer has
     * exited the operable part of the drop site for the
     * {@code DropTarget} registered with this listener.
     *
     * @param dte the {@code DropTargetEvent}
     */

    void dragExit(DropTargetEvent dte);

    /**
     * Called when the drag operation has terminated with a drop on
     * the operable part of the drop site for the {@code DropTarget}
     * registered with this listener.
     * <p>
     * This method is responsible for undertaking
     * the transfer of the data associated with the
     * gesture. The {@code DropTargetDropEvent}
     * provides a means to obtain a {@code Transferable}
     * object that represents the data object(s) to
     * be transferred.<P>
     * From this method, the {@code DropTargetListener}
     * shall accept or reject the drop via the
     * acceptDrop(int dropAction) or rejectDrop() methods of the
     * {@code DropTargetDropEvent} parameter.
     * <P>
     * Subsequent to acceptDrop(), but not before,
     * {@code DropTargetDropEvent}'s getTransferable()
     * method may be invoked, and data transfer may be
     * performed via the returned {@code Transferable}'s
     * getTransferData() method.
     * <P>
     * At the completion of a drop, an implementation
     * of this method is required to signal the success/failure
     * of the drop by passing an appropriate
     * {@code boolean} to the {@code DropTargetDropEvent}'s
     * dropComplete(boolean success) method.
     * <P>
     * Note: The data transfer should be completed before the call  to the
     * {@code DropTargetDropEvent}'s dropComplete(boolean success) method.
     * After that, a call to the getTransferData() method of the
     * {@code Transferable} returned by
     * {@code DropTargetDropEvent.getTransferable()} is guaranteed to
     * succeed only if the data transfer is local; that is, only if
     * {@code DropTargetDropEvent.isLocalTransfer()} returns
     * {@code true}. Otherwise, the behavior of the call is
     * implementation-dependent.
     *
     * @param dtde the {@code DropTargetDropEvent}
     */

    void drop(DropTargetDropEvent dtde);
}
