/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
* Panel is a DropTarget
*
*/

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.*;


class DnDTarget extends Panel implements DropTargetListener {
    //private int dragOperation = DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE;
    Color bgColor;
    Color htColor;

    DnDTarget(Color bgColor, Color htColor) {
        super();
        setLayout(new FlowLayout());
        this.bgColor = bgColor;
        this.htColor = htColor;
        setBackground(bgColor);
        setDropTarget(new DropTarget(this, this));
        add(new Label("drop here"));
    }

    boolean check(DropTargetDragEvent dtde)
    {
        if (dtde.getCurrentDataFlavorsAsList().contains(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
            return true;
        }
        return false;
    }

    public void dragEnter(DropTargetDragEvent dtde) {
        if(check(dtde)){
            setBackground(htColor);
            repaint();
        }
    }

    public void dragOver(DropTargetDragEvent dtde) {
        check(dtde);
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        check(dtde);
    }

    public void dragExit(DropTargetEvent e) {
        setBackground(bgColor);
        repaint();
    }

    public void dragScroll(DropTargetDragEvent e) {
        System.out.println("[Target] dragScroll");
    }

    public void drop(DropTargetDropEvent dtde) {
        System.out.println("[Target] drop");
        boolean success = false;
        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        if( dtde.getCurrentDataFlavorsAsList().contains(DataFlavor.javaFileListFlavor) ){
            System.out.println("[Target] DROP OK!");
            try {
                Transferable transfer = dtde.getTransferable();
                java.util.List<File> fl = (java.util.List<File>)transfer.getTransferData(DataFlavor.javaFileListFlavor);
                for(File f : fl){
                    add(new Button(f.getCanonicalPath()));
                    System.out.println("[Target] drop file:" + f.getCanonicalPath());
                }
                validate();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            setBackground(bgColor);
            repaint();
            success = true;
        }
        dtde.dropComplete(success);
    }
}
