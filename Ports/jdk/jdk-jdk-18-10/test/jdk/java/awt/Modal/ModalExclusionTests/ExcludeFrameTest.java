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


public class ExcludeFrameTest implements AWTEventListener {
    class CustomFrame extends TestFrame {

        @Override
        public void doOpenAction() {
            if (window != null) {
                window.setVisible(true);
            }
        }
    }

    class CustomDialog extends TestDialog {

        public CustomDialog(Frame frame) {
            super(frame);
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
                    fileDialog = new FileDialog((Frame) null);
                    fileDialog.setLocation(20, 200);
                    fileDialog.setVisible(true);
                    break;
            }
        }
    }

    class CustomWindow extends TestWindow {

        public CustomWindow(Frame frame) {
            super(frame);
        }

        @Override
        public void doOpenAction() {
            if (dialog != null) {
                dialog.setVisible(true);
            }
        }
    }

    private TestDialog dialog;
    private TestFrame  frame;
    private TestWindow window;
    private FileDialog fileDialog;

    private boolean windowAppeared = false;
    private final Object windowLock;

    private static final int delay = 1000;
    private final ExtendedRobot robot;

    private final Dialog.ModalExclusionType exclusionType;

    public enum DialogToShow {PAGE_SETUP, PRINT_SETUP, FILE_DIALOG};
    private final DialogToShow childDialogType;
    private String type;

    public ExcludeFrameTest(Dialog.ModalExclusionType exclType,
                            DialogToShow dialogType) throws Exception {
        exclusionType = exclType;
        childDialogType = dialogType;

        type = "File";
        if (dialogType == DialogToShow.PAGE_SETUP) {
            type = "Page setup";
        } else if (dialogType == DialogToShow.PRINT_SETUP) {
            type = "Print setup";
        }

        windowLock = new Object();
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait( this::createGUI );
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (event.getID() == WindowEvent.WINDOW_OPENED) {
            windowAppeared = true;
            synchronized (windowLock) {
                windowLock.notifyAll();
            }
        }
    }

    private void createGUI() {
        frame = new CustomFrame();
        frame.setLocation(20, 20);
        frame.setModalExclusionType(exclusionType);
        dialog = new CustomDialog(frame);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLocation(220, 20);
        window = new CustomWindow(frame);
        window.setLocation(420, 20);
        Toolkit.getDefaultToolkit().addAWTEventListener(
                ExcludeFrameTest.this, AWTEvent.WINDOW_EVENT_MASK);
        frame.setVisible(true);
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if (frame  != null) { frame.dispose();  }
        if (window != null) { window.dispose(); }
        if (fileDialog != null) {
            fileDialog.dispose();
        } else {
            robot.type(KeyEvent.VK_ESCAPE);
        }
    }

    public void doTest() throws Exception {

        robot.waitForIdle(delay);

        frame.clickOpenButton(robot);
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

        frame.toFront();
        robot.waitForIdle(delay);

        String excl = "";
        if (exclusionType == Dialog.ModalExclusionType.APPLICATION_EXCLUDE) {
            excl = "Application";
        } else if (exclusionType == Dialog.ModalExclusionType.TOOLKIT_EXCLUDE) {
            excl = "Toolkit";
        }

        frame.checkUnblockedFrame(robot, excl + " modal " + type +
                " dialog should not block this modal excluded Frame");
        dialog.checkUnblockedDialog(robot, excl + " modal " + type +
                " dialog should not block this modal excluded app. modal Dialog");
        window.checkUnblockedWindow(robot, excl + " modal " + type +
                " dialog should not block this modal excluded Window");

        robot.waitForIdle();
        closeAll();
    }
}
