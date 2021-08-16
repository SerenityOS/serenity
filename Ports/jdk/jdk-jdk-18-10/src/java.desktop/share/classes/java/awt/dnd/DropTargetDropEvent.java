/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Serial;
import java.util.List;

/**
 * The {@code DropTargetDropEvent} is delivered
 * via the {@code DropTargetListener} drop() method.
 * <p>
 * The {@code DropTargetDropEvent} reports the <i>source drop actions</i>
 * and the <i>user drop action</i> that reflect the current state of the
 * drag-and-drop operation.
 * <p>
 * <i>Source drop actions</i> is a bitwise mask of {@code DnDConstants}
 * that represents the set of drop actions supported by the drag source for
 * this drag-and-drop operation.
 * <p>
 * <i>User drop action</i> depends on the drop actions supported by the drag
 * source and the drop action selected by the user. The user can select a drop
 * action by pressing modifier keys during the drag operation:
 * <pre>
 *   Ctrl + Shift -&gt; ACTION_LINK
 *   Ctrl         -&gt; ACTION_COPY
 *   Shift        -&gt; ACTION_MOVE
 * </pre>
 * If the user selects a drop action, the <i>user drop action</i> is one of
 * {@code DnDConstants} that represents the selected drop action if this
 * drop action is supported by the drag source or
 * {@code DnDConstants.ACTION_NONE} if this drop action is not supported
 * by the drag source.
 * <p>
 * If the user doesn't select a drop action, the set of
 * {@code DnDConstants} that represents the set of drop actions supported
 * by the drag source is searched for {@code DnDConstants.ACTION_MOVE},
 * then for {@code DnDConstants.ACTION_COPY}, then for
 * {@code DnDConstants.ACTION_LINK} and the <i>user drop action</i> is the
 * first constant found. If no constant is found the <i>user drop action</i>
 * is {@code DnDConstants.ACTION_NONE}.
 *
 * @since 1.2
 */

public class DropTargetDropEvent extends DropTargetEvent {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1721911170440459322L;

    /**
     * Construct a {@code DropTargetDropEvent} given
     * the {@code DropTargetContext} for this operation,
     * the location of the drag {@code Cursor}'s
     * hotspot in the {@code Component}'s coordinates,
     * the currently
     * selected user drop action, and the current set of
     * actions supported by the source.
     * By default, this constructor
     * assumes that the target is not in the same virtual machine as
     * the source; that is, {@link #isLocalTransfer()} will
     * return {@code false}.
     *
     * @param dtc        The {@code DropTargetContext} for this operation
     * @param cursorLocn The location of the "Drag" Cursor's
     * hotspot in {@code Component} coordinates
     * @param dropAction the user drop action.
     * @param srcActions the source drop actions.
     *
     * @throws NullPointerException
     * if cursorLocn is {@code null}
     * @throws IllegalArgumentException
     *         if dropAction is not one of  {@code DnDConstants}.
     * @throws IllegalArgumentException
     *         if srcActions is not a bitwise mask of {@code DnDConstants}.
     * @throws IllegalArgumentException if dtc is {@code null}.
     */

    public DropTargetDropEvent(DropTargetContext dtc, Point cursorLocn, int dropAction, int srcActions)  {
        super(dtc);

        if (cursorLocn == null) throw new NullPointerException("cursorLocn");

        if (dropAction != DnDConstants.ACTION_NONE &&
            dropAction != DnDConstants.ACTION_COPY &&
            dropAction != DnDConstants.ACTION_MOVE &&
            dropAction != DnDConstants.ACTION_LINK
        ) throw new IllegalArgumentException("dropAction = " + dropAction);

        if ((srcActions & ~(DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK)) != 0) throw new IllegalArgumentException("srcActions");

        location        = cursorLocn;
        actions         = srcActions;
        this.dropAction = dropAction;
    }

    /**
     * Construct a {@code DropTargetEvent} given the
     * {@code DropTargetContext} for this operation,
     * the location of the drag {@code Cursor}'s hotspot
     * in the {@code Component}'s
     * coordinates, the currently selected user drop action,
     * the current set of actions supported by the source,
     * and a {@code boolean} indicating if the source is in the same JVM
     * as the target.
     *
     * @param dtc        The DropTargetContext for this operation
     * @param cursorLocn The location of the "Drag" Cursor's
     * hotspot in Component's coordinates
     * @param dropAction the user drop action.
     * @param srcActions the source drop actions.
     * @param isLocal  True if the source is in the same JVM as the target
     *
     * @throws NullPointerException
     *         if cursorLocn is  {@code null}
     * @throws IllegalArgumentException
     *         if dropAction is not one of {@code DnDConstants}.
     * @throws IllegalArgumentException if srcActions is not a bitwise mask of {@code DnDConstants}.
     * @throws IllegalArgumentException  if dtc is {@code null}.
     */

    public DropTargetDropEvent(DropTargetContext dtc, Point cursorLocn, int dropAction, int srcActions, boolean isLocal)  {
        this(dtc, cursorLocn, dropAction, srcActions);

        isLocalTx = isLocal;
    }

    /**
     * This method returns a {@code Point}
     * indicating the {@code Cursor}'s current
     * location in the {@code Component}'s coordinates.
     *
     * @return the current {@code Cursor} location in Component's coords.
     */

    public Point getLocation() {
        return location;
    }


    /**
     * This method returns the current DataFlavors.
     *
     * @return current DataFlavors
     */

    public DataFlavor[] getCurrentDataFlavors() {
        return getDropTargetContext().getCurrentDataFlavors();
    }

    /**
     * This method returns the currently available
     * {@code DataFlavor}s as a {@code java.util.List}.
     *
     * @return the currently available DataFlavors as a java.util.List
     */

    public List<DataFlavor> getCurrentDataFlavorsAsList() {
        return getDropTargetContext().getCurrentDataFlavorsAsList();
    }

    /**
     * This method returns a {@code boolean} indicating if the
     * specified {@code DataFlavor} is available
     * from the source.
     *
     * @param df the {@code DataFlavor} to test
     *
     * @return if the DataFlavor specified is available from the source
     */

    public boolean isDataFlavorSupported(DataFlavor df) {
        return getDropTargetContext().isDataFlavorSupported(df);
    }

    /**
     * This method returns the source drop actions.
     *
     * @return the source drop actions.
     */
    public int getSourceActions() { return actions; }

    /**
     * This method returns the user drop action.
     *
     * @return the user drop actions.
     */
    public int getDropAction() { return dropAction; }

    /**
     * This method returns the {@code Transferable} object
     * associated with the drop.
     *
     * @return the {@code Transferable} associated with the drop
     */

    public Transferable getTransferable() {
        return getDropTargetContext().getTransferable();
    }

    /**
     * accept the drop, using the specified action.
     *
     * @param dropAction the specified action
     */

    public void acceptDrop(int dropAction) {
        getDropTargetContext().acceptDrop(dropAction);
    }

    /**
     * reject the Drop.
     */

    public void rejectDrop() {
        getDropTargetContext().rejectDrop();
    }

    /**
     * This method notifies the {@code DragSource}
     * that the drop transfer(s) are completed.
     *
     * @param success a {@code boolean} indicating that the drop transfer(s) are completed.
     */

    public void dropComplete(boolean success) {
        getDropTargetContext().dropComplete(success);
    }

    /**
     * This method returns an {@code int} indicating if
     * the source is in the same JVM as the target.
     *
     * @return if the Source is in the same JVM
     */

    public boolean isLocalTransfer() {
        return isLocalTx;
    }

    /*
     * fields
     */

    private static final Point  zero     = new Point(0,0);

    /**
     * The location of the drag cursor's hotspot in Component coordinates.
     *
     * @serial
     */
    private Point               location   = zero;

    /**
     * The source drop actions.
     *
     * @serial
     */
    private int                 actions    = DnDConstants.ACTION_NONE;

    /**
     * The user drop action.
     *
     * @serial
     */
    private int                 dropAction = DnDConstants.ACTION_NONE;

    /**
     * {@code true} if the source is in the same JVM as the target.
     *
     * @serial
     */
    private boolean             isLocalTx = false;
}
