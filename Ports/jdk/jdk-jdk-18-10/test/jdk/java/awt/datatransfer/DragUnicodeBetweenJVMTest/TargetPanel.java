/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.dnd.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Arrays;

public class TargetPanel extends Panel implements DropTargetListener{

    private java.util.List <File> content = new ArrayList<File>();

    //private final CustomDropTargetListener dropTargetListener = new CustomDropTargetListener();

    private Frame frame;

    public TargetPanel (Frame frame)
    {
        this.frame = frame;
        setBackground(Color.DARK_GRAY);
        setPreferredSize(new Dimension(200, 200));
        setDropTarget(new DropTarget(this, this));
    }

    public void dragEnter(DropTargetDragEvent dtde) {
        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        }
    }

    public void dragOver(DropTargetDragEvent dtde) {
        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        }
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        }
    }

    public void dragExit(DropTargetEvent dte) {

    }

    public void drop(DropTargetDropEvent dtde) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            try {
                content = (java.util.List)dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                repaint();
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dtde.dropComplete(true);



            boolean listsAreEqual = true;

             for (int i = 0; i < content.size(); i++) {
                if(!FileListTransferable.files[i].getName().equals(content.get(i).getName())) {
                    listsAreEqual = false;
                }
            }

            if (listsAreEqual) {
                System.err.println(InterprocessMessages.EXECUTION_IS_SUCCESSFULL);
                System.exit(0);
            }
        }
        dtde.rejectDrop();
        System.err.println(InterprocessMessages.FILES_ON_TARGET_ARE_CORRUPTED);
        System.exit(1);
    }

    public void paint(Graphics g) {
        g.setColor(Color.YELLOW);
        int i = 0;
        for (Iterator <File> iterator = content.iterator(); iterator.hasNext();i++) {
            g.drawString(iterator.next().getName(), 5, g.getFontMetrics().getAscent()*i+20);
        }

    }

}
