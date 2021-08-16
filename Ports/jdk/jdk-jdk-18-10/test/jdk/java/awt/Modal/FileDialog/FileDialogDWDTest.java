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


// DWD: Dialog, Window, Dialog

public class FileDialogDWDTest {

    private volatile FileDialog fileDialog;
    private volatile ParentDialog parent;
    private volatile CustomDialog dialog;
    private volatile TestWindow window;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    boolean setModal;

    Dialog.ModalityType modalityType;

    private FileDialogDWDTest(Dialog.ModalityType modType,
                              boolean             modal) throws Exception {
        modalityType = modType;
        setModal = modal;

        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    public FileDialogDWDTest(Dialog.ModalityType modalityType) throws Exception {
        this(modalityType, false);
    }

    public FileDialogDWDTest() throws Exception {
        this(null, true);
    }

    private void createGUI() {

        parent = new ParentDialog();
        dialog = new CustomDialog(parent);

        if (setModal) {
            dialog.setModal(true);
            modalityType = dialog.getModalityType();
        } else if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }

        window = new CustomWindow(parent);

        int x = Toolkit.getDefaultToolkit().getScreenSize().width -
            parent.getWidth() - 50;
        int y = 50;
        parent.setLocation(x, y);
        y += (parent.getHeight() + 50);
        window.setLocation(x, y);
        y += (window.getHeight() + 50);
        dialog.setLocation(x, y);

        parent.setVisible(true);
    }

    private void openAll() throws Exception {
        robot.waitForIdle(delay);
        parent.clickOpenButton(robot);
        robot.waitForIdle(delay);
        window.clickOpenButton(robot);
        robot.waitForIdle(delay);
        dialog.clickOpenButton(robot);
        robot.waitForIdle(delay);
    }

    private void checkBlockedWindows() throws Exception {

        String msg = "FileDialog should block this ";
        parent.checkBlockedDialog(robot, msg + "Dialog.");
        robot.waitForIdle(delay);
        window.checkBlockedWindow(robot, msg + "Window.");
        robot.waitForIdle(delay);
        dialog.checkBlockedDialog(robot, msg + "Dialog.");
        robot.waitForIdle(delay);
    }

    private void checkUnblockedWindows() throws Exception {

        String msg = "Blocking dialogs were closed.";
        parent.checkUnblockedDialog(robot, msg + "Frame.");
        robot.waitForIdle(delay);
        window.checkUnblockedWindow(robot, msg + "Window.");
        robot.waitForIdle(delay);
    }


    private void modalTest(String type) throws Exception {

        checkBlockedWindows();

        EventQueue.invokeAndWait(() -> { fileDialog.dispose(); });
        robot.waitForIdle(delay);

        String msg = "FileDialog was closed, " +
            "but the " + type + " modal dialog should block this ";

        parent.checkBlockedDialog(robot, msg + "Dialog.");
        robot.waitForIdle(delay);

        window.checkBlockedWindow(robot, msg + "Window.");
        robot.waitForIdle(delay);

        dialog.checkUnblockedDialog(robot, "FileDialog was closed.");
        robot.waitForIdle(delay);

        dialog.clickCloseButton(robot);
        robot.waitForIdle(delay);

        checkUnblockedWindows();
    }

    private void nonModalTest() throws Exception {

        checkBlockedWindows();

        EventQueue.invokeAndWait(() -> { fileDialog.dispose(); });
        robot.waitForIdle(delay);

        dialog.checkUnblockedDialog(robot, "FileDialog was closed.");
        robot.waitForIdle(delay);

        checkUnblockedWindows();
    }

    public void doTest() throws Exception {

        try {

            openAll();

            if (modalityType == null) {
                nonModalTest();
                return;
            }

            switch (modalityType) {
                case APPLICATION_MODAL:
                    modalTest("application");
                    break;
                case DOCUMENT_MODAL:
                    modalTest("document");
                    break;
                case TOOLKIT_MODAL:
                    modalTest("toolkit");
                    break;
                case MODELESS:
                    nonModalTest();
                    break;
            }

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (parent != null) { parent.dispose(); }
        if (dialog != null) { dialog.dispose(); }
        if (window != null) { window.dispose(); }
        if (fileDialog != null) { fileDialog.dispose(); }

    }

    class ParentDialog extends TestDialog {

        public ParentDialog() { super((Frame) null); }

        @Override
        public void doOpenAction() {
            if (window != null) { window.setVisible(true); }
        }

    }


    class CustomDialog extends TestDialog {

        public CustomDialog(Dialog d) { super(d); }

        @Override
        public void doOpenAction() {
            fileDialog = new FileDialog((Frame) null);
            fileDialog.setLocation(50, 50);
            fileDialog.setVisible(true);
        }

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }

    class CustomWindow extends TestWindow {

        public CustomWindow(Dialog d) { super(d); }

        @Override
        public void doOpenAction() {
            if (dialog != null) { dialog.setVisible(true); }
        }
    }
}
