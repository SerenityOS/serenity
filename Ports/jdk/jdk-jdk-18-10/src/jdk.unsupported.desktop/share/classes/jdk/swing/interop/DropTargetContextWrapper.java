/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.swing.interop;

import java.awt.dnd.peer.DropTargetContextPeer;
import java.awt.dnd.DropTarget;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.DropTargetContext;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import sun.awt.AWTAccessor;

/**
 * This class provides a wrapper over inner class DropTargetContextPeerProxy
 * which implements jdk internal java.awt.dnd.peer.DropTargetContextPeer interface
 * and provides APIs to be used by FX swing interop to access and use
 * DropTargetContextPeer APIs.
 *
 * @since 11
 */
public abstract class DropTargetContextWrapper {

    private DropTargetContextPeerProxy dcp;
    public DropTargetContextWrapper() {
        dcp = new DropTargetContextPeerProxy();
    }

    public void setDropTargetContext(DropTargetContext dtc,
                                         DropTargetContextWrapper dtcpw) {
        AWTAccessor.getDropTargetContextAccessor().
                    setDropTargetContextPeer(dtc, dtcpw.dcp);
    }

    public void reset(DropTargetContext dtc) {
        AWTAccessor.getDropTargetContextAccessor().reset(dtc);
    }

    public abstract void setTargetActions(int actions);

    public abstract int getTargetActions();

    public abstract DropTarget getDropTarget();

    public abstract DataFlavor[] getTransferDataFlavors();

    public abstract Transferable getTransferable() throws InvalidDnDOperationException;

    public abstract boolean isTransferableJVMLocal();

    public abstract void acceptDrag(int dragAction);

    public abstract void rejectDrag();

    public abstract void acceptDrop(int dropAction);

    public abstract void rejectDrop();

    public abstract void dropComplete(boolean success);

    private class DropTargetContextPeerProxy implements DropTargetContextPeer {

        public void setTargetActions(int actions) {
            DropTargetContextWrapper.this.setTargetActions(actions);
        }

        public int getTargetActions() {
            return DropTargetContextWrapper.this.getTargetActions();
        }

        public DropTarget getDropTarget() {
            return DropTargetContextWrapper.this.getDropTarget();
        }

        public DataFlavor[] getTransferDataFlavors() {
            return DropTargetContextWrapper.this.getTransferDataFlavors();
        }

        public Transferable getTransferable()
                throws InvalidDnDOperationException {
            return DropTargetContextWrapper.this.getTransferable();
        }

        public boolean isTransferableJVMLocal() {
            return DropTargetContextWrapper.this.isTransferableJVMLocal();
        }

        public void acceptDrag(int dragAction) {
            DropTargetContextWrapper.this.acceptDrag(dragAction);
        }

        public void rejectDrag() {
            DropTargetContextWrapper.this.rejectDrag();
        }

        public void acceptDrop(int dropAction) {
            DropTargetContextWrapper.this.acceptDrop(dropAction);
        }

        public void rejectDrop() {
            DropTargetContextWrapper.this.rejectDrop();
        }

        public void dropComplete(boolean success) {
            DropTargetContextWrapper.this.dropComplete(success);
        }
    }
}
