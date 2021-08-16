/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;
import java.awt.print.*;

import static jdk.test.lib.Asserts.*;


public class ExcludeDialogTest implements AWTEventListener {

    class ParentDialog extends TestDialog {

        public ParentDialog() {
            super((Frame) null);
        }

        @Override
        public void doOpenAction() {
            if (window != null) {
                window.setVisible(true);
            }
        }
    }

    class CustomDialog extends TestDialog {

        public CustomDialog(Dialog dialog) {
            super(dialog);
        }

        @Override
        public void doOpenAction() {

            switch (childDialogType) {
                case PRINT_SETUP:
                    PrinterJob.getPrinterJob().printDialog();
                    break;
                case PAGE_SETUP:
                    PrinterJob.getPrinterJob().pageDialog(new PageFormat());
                    break;
                case FILE_DIALOG:
                    fileDialog = new FileDialog((Dialog) null);
                    fileDialog.setLocation(20, 200);
                    fileDialog.setVisible(true);
                    break;
            }
        }
    }

    class CustomWindow extends TestWindow {

        public CustomWindow(Dialog parent) {
            super(parent);
        }

        @Override
        public void doOpenAction() {
            if (dialog != null) {
                dialog.setVisible(true);
            }
        }
    }

    private TestWindow window;
    private TestDialog dialog;
    private ParentDialog parent;
    private FileDialog fileDialog;

    private static final int delay = 1000;

    private final ExtendedRobot robot;

    private boolean windowAppeared = false;
    private final Object windowLock;

    private final Dialog.ModalExclusionType exclusionType;

    public enum DialogToShow {PAGE_SETUP, PRINT_SETUP, FILE_DIALOG};
    private DialogToShow childDialogType;
    private String type;

    @Override
    public void eventDispatched(AWTEvent event) {
        if (event.getID() == WindowEvent.WINDOW_OPENED) {
            windowAppeared = true;
            synchronized (windowLock) {
                windowLock.notifyAll();
            }
        }
    }

    ExcludeDialogTest(Dialog.ModalExclusionType exclType,
                      DialogToShow dialogType) throws Exception {
        exclusionType = exclType;
        childDialogType = dialogType;

        type = "File";
        if (dialogType == DialogToShow.PAGE_SETUP) {
            type = "Page setup";
        } else if (dialogType == DialogToShow.PRINT_SETUP) {
            type = "Print setup";
        }

        robot = new ExtendedRobot();
        windowLock = new Object();
        EventQueue.invokeAndWait( this::createGUI );
    }

    private void createGUI() {
        parent = new ParentDialog();
        parent.setLocation(20, 20);
        parent.setModalExclusionType(exclusionType);
        dialog = new CustomDialog(parent);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLocation(220, 20);
        window = new CustomWindow(parent);
        window.setLocation(420, 20);
        Toolkit.getDefaultToolkit().addAWTEventListener(
            ExcludeDialogTest.this, AWTEvent.WINDOW_EVENT_MASK);
        parent.setVisible(true);
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if (parent != null) { parent.dispose(); }
        if (window != null) { window.dispose(); }
        if (fileDialog != null) {
            fileDialog.dispose();
        } else {
            robot.type(KeyEvent.VK_ESCAPE);
        }
    }

    public void doTest() throws Exception {

        robot.waitForIdle(delay);

        parent.clickOpenButton(robot);
        robot.waitForIdle(delay);

        window.clickOpenButton(robot);
        robot.waitForIdle(delay);

        dialog.clickOpenButton(robot);
        robot.waitForIdle(delay);

        if (! windowAppeared) {
            synchronized (windowLock) {
                try {
                    windowLock.wait(10 * delay);
                } catch (InterruptedException e) {}
            }
        }

        assertTrue(windowAppeared, type + " dialog didn't appear");

        parent.toFront();
        robot.waitForIdle(delay);

        String excl = "";
        if (exclusionType == Dialog.ModalExclusionType.APPLICATION_EXCLUDE) {
            excl = "Application";
        } else if (exclusionType == Dialog.ModalExclusionType.TOOLKIT_EXCLUDE) {
            excl = "Toolkit";
        }

        parent.checkUnblockedDialog(robot, excl + " modal " + type +
                " dialog should not block this modal excluded Dialog");
        dialog.checkUnblockedDialog(robot, excl + " modal " + type +
                " dialog should not block this modal excluded app. modal Dialog");
        window.checkUnblockedWindow(robot, excl + " modal " + type +
                " dialog should not block this modal excluded Window");

        robot.waitForIdle();
        closeAll();
    }
}
