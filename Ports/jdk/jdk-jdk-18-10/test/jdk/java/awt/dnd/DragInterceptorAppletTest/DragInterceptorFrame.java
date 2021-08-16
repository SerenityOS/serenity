/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.dnd.*;
import java.io.IOException;
import java.security.Permission;
import java.security.AccessControlException;

class DragInterceptorFrame extends Frame implements DropTargetListener {

    private static int exitMessage = InterprocessMessages.TEST_PASSED;
    private static boolean dataIsAccessible = false;
    private static boolean exceptionHasBeenThrown = false;

    DragInterceptorFrame(Point location) {
        System.setSecurityManager(new ClipboardDefender());
        initGUI(location);
        setDropTarget(new DropTarget(this, DnDConstants.ACTION_COPY,
                this));
    }

    private void initGUI(Point location) {
        this.setLocation(location);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                DragInterceptorFrame.this.dispose();
            }
        });
        setSize (200, 200);
        this.setVisible(true);
    }

    public void dragEnter(DropTargetDragEvent dtde) {
        // We want to set the exception handler on EDT
        Thread.currentThread().setUncaughtExceptionHandler (
            new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    exceptionHasBeenThrown = true;
                }
            }
        );
        examineTransferable(dtde);
    }

    public void dragOver(DropTargetDragEvent dtde) {
        examineTransferable(dtde);
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        examineTransferable(dtde);
    }

    public void dragExit(DropTargetEvent dte) {}

    public void drop(DropTargetDropEvent dtde) {

        if (dataIsAccessible && !exceptionHasBeenThrown) {
            exitMessage = InterprocessMessages.DATA_WAS_INTERCEPTED_AND_EXCEPTION_HANDLER_WAS_NOT_TRIGGERED;
        } else if (dataIsAccessible) {
            exitMessage = InterprocessMessages.DATA_WAS_INTERCEPTED;
        } else if (!exceptionHasBeenThrown) {
            exitMessage = InterprocessMessages.EXCEPTION_HANDLER_WAS_NOT_TRIGGERED;
        }

        // This returns the diagnostic code from the child VM
        System.exit(exitMessage);
    }

    Point getDropTargetPoint() {
        return new Point((int)getLocationOnScreen().getX()+(getWidth()/2),
                (int)getLocationOnScreen().getY()+(getHeight()/2));
    }

    private void examineTransferable(DropTargetDragEvent dtde) {
        if (dtde.getCurrentDataFlavorsAsList().contains(DataFlavor.stringFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
            try{
                if (null != dtde.getTransferable().getTransferData(DataFlavor.stringFlavor)) {
                    dataIsAccessible = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                exitMessage = InterprocessMessages.UNEXPECTED_IO_EXCEPTION;
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
                exitMessage = InterprocessMessages.UNEXPECTED_UNSUPPORTED_FLAVOR_EXCEPTION;
            }
        }
    }

    static class ClipboardDefender extends SecurityManager {
        public void checkPermission(Permission p) {
           if (p instanceof java.awt.AWTPermission &&
                   p.getName().equals("accessClipboard")) {
               throw new AccessControlException("access denied ");
           }
        }
    }

    public static void main(String[] args) {
        new DragInterceptorFrame(new Point(200,200));
    }
}
