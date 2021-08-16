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
 * The {@code DropTargetDragEvent} is delivered to a
 * {@code DropTargetListener} via its
 * dragEnter() and dragOver() methods.
 * <p>
 * The {@code DropTargetDragEvent} reports the <i>source drop actions</i>
 * and the <i>user drop action</i> that reflect the current state of
 * the drag operation.
 * <p>
 * <i>Source drop actions</i> is a bitwise mask of {@code DnDConstants}
 * that represents the set of drop actions supported by the drag source for
 * this drag operation.
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

public class DropTargetDragEvent extends DropTargetEvent {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8422265619058953682L;

    /**
     * Construct a {@code DropTargetDragEvent} given the
     * {@code DropTargetContext} for this operation,
     * the location of the "Drag" {@code Cursor}'s hotspot
     * in the {@code Component}'s coordinates, the
     * user drop action, and the source drop actions.
     *
     * @param dtc        The DropTargetContext for this operation
     * @param cursorLocn The location of the "Drag" Cursor's
     * hotspot in Component coordinates
     * @param dropAction The user drop action
     * @param srcActions The source drop actions
     *
     * @throws NullPointerException if cursorLocn is null
     * @throws IllegalArgumentException if dropAction is not one of
     *         {@code DnDConstants}.
     * @throws IllegalArgumentException if srcActions is not
     *         a bitwise mask of {@code DnDConstants}.
     * @throws IllegalArgumentException if dtc is {@code null}.
     */

    public DropTargetDragEvent(DropTargetContext dtc, Point cursorLocn, int dropAction, int srcActions)  {
        super(dtc);

        if (cursorLocn == null) throw new NullPointerException("cursorLocn");

        if (dropAction != DnDConstants.ACTION_NONE &&
            dropAction != DnDConstants.ACTION_COPY &&
            dropAction != DnDConstants.ACTION_MOVE &&
            dropAction != DnDConstants.ACTION_LINK
        ) throw new IllegalArgumentException("dropAction" + dropAction);

        if ((srcActions & ~(DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK)) != 0) throw new IllegalArgumentException("srcActions");

        location        = cursorLocn;
        actions         = srcActions;
        this.dropAction = dropAction;
    }

    /**
     * This method returns a {@code Point}
     * indicating the {@code Cursor}'s current
     * location within the {@code Component'}s
     * coordinates.
     *
     * @return the current cursor location in
     * {@code Component}'s coords.
     */

    public Point getLocation() {
        return location;
    }


    /**
     * This method returns the current {@code DataFlavor}s from the
     * {@code DropTargetContext}.
     *
     * @return current DataFlavors from the DropTargetContext
     */

    public DataFlavor[] getCurrentDataFlavors() {
        return getDropTargetContext().getCurrentDataFlavors();
    }

    /**
     * This method returns the current {@code DataFlavor}s
     * as a {@code java.util.List}
     *
     * @return a {@code java.util.List} of the Current {@code DataFlavor}s
     */

    public List<DataFlavor> getCurrentDataFlavorsAsList() {
        return getDropTargetContext().getCurrentDataFlavorsAsList();
    }

    /**
     * This method returns a {@code boolean} indicating
     * if the specified {@code DataFlavor} is supported.
     *
     * @param df the {@code DataFlavor} to test
     *
     * @return if a particular DataFlavor is supported
     */

    public boolean isDataFlavorSupported(DataFlavor df) {
        return getDropTargetContext().isDataFlavorSupported(df);
    }

    /**
     * This method returns the source drop actions.
     *
     * @return the source drop actions
     */
    public int getSourceActions() { return actions; }

    /**
     * This method returns the user drop action.
     *
     * @return the user drop action
     */
    public int getDropAction() { return dropAction; }

    /**
     * This method returns the Transferable object that represents
     * the data associated with the current drag operation.
     *
     * @return the Transferable associated with the drag operation
     * @throws InvalidDnDOperationException if the data associated with the drag
     *         operation is not available
     *
     * @since 1.5
     */
    public Transferable getTransferable() {
        return getDropTargetContext().getTransferable();
    }

    /**
     * Accepts the drag.
     *
     * This method should be called from a
     * {@code DropTargetListeners dragEnter},
     * {@code dragOver}, and {@code dropActionChanged}
     * methods if the implementation wishes to accept an operation
     * from the srcActions other than the one selected by
     * the user as represented by the {@code dropAction}.
     *
     * @param dragOperation the operation accepted by the target
     */
    public void acceptDrag(int dragOperation) {
        getDropTargetContext().acceptDrag(dragOperation);
    }

    /**
     * Rejects the drag as a result of examining either the
     * {@code dropAction} or the available {@code DataFlavor}
     * types.
     */
    public void rejectDrag() {
        getDropTargetContext().rejectDrag();
    }

    /*
     * fields
     */

    /**
     * The location of the drag cursor's hotspot in Component coordinates.
     *
     * @serial
     */
    private Point               location;

    /**
     * The source drop actions.
     *
     * @serial
     */
    private int                 actions;

    /**
     * The user drop action.
     *
     * @serial
     */
    private int                 dropAction;
}
