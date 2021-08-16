/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

import test.java.awt.regtesthelpers.Util;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;

class TargetFileListFrame extends Frame implements DropTargetListener {

    private List list = new List(FileListBetweenJVMsTest.VISIBLE_RAWS_IN_LIST);
    private int expectationTransferredFilesNumber;

    TargetFileListFrame(Point location, int expectationTransferredFilesNumber) {
        this.expectationTransferredFilesNumber = expectationTransferredFilesNumber;
        initGUI(location);
        setDropTarget(new DropTarget(list, DnDConstants.ACTION_COPY,
                this));
    }

    private void initGUI(Point location) {
        this.setLocation(location);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                TargetFileListFrame.this.dispose();
            }
        });
        this.add(new Panel().add(list));
        this.pack();
        this.setVisible(true);
    }

    public void dragEnter(DropTargetDragEvent dtde) {
        if (dtde.getCurrentDataFlavorsAsList().contains(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        }
    }

    public void dragOver(DropTargetDragEvent dtde) {
        if (dtde.getCurrentDataFlavorsAsList().contains(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        }
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        if (dtde.getCurrentDataFlavorsAsList().contains(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        }
    }

    public void dragExit(DropTargetEvent dte) {}

    public void drop(DropTargetDropEvent dtde) {
        list.removeAll();
        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        java.util.List<File> fileList = extractListOfFiles(dtde);
        for (File file:fileList) {
            list.add(file.getName());
        }

        if (fileList.size() != expectationTransferredFilesNumber)
        {
            System.err.println("ERROR: Expected file number:"
                    + expectationTransferredFilesNumber
                    + "; Received file number: "
                    + fileList.size());
            TargetFileListFrame.this.dispose();
            System.exit(InterprocessMessages.WRONG_FILES_NUMBER_ON_TARGET);
        }

        TargetFileListFrame.this.dispose();

    }

    private java.util.List<File> extractListOfFiles(DropTargetDropEvent dtde) {
        java.util.List<File> fileList = null;
        try {
            fileList = (java.util.List<File>)dtde.getTransferable().
                    getTransferData(DataFlavor.javaFileListFlavor);
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileList;
    }

    Point getDropTargetPoint() {
       return new Point((int)list.getLocationOnScreen().getX()+(list.getWidth()/2),
                (int)list.getLocationOnScreen().getY()+(list.getHeight()/2));
    }
}
