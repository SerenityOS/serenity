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

public class FileDialogModalityTest {

    private volatile TestDialog  dialog;
    private volatile ParentFrame parent;
    private volatile TestWindow  window;
    private volatile FileDialog  fileDialog;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private final Dialog.ModalityType modalityType;

    public static void main(String[] args) throws Exception {
        (new FileDialogModalityTest(Dialog.ModalityType.DOCUMENT_MODAL)).doTest();
        (new FileDialogModalityTest(Dialog.ModalityType.TOOLKIT_MODAL)).doTest();
        (new FileDialogModalityTest(Dialog.ModalityType.MODELESS)).doTest();
    }

    public FileDialogModalityTest(Dialog.ModalityType t) throws Exception {
        modalityType = t;
        robot = new ExtendedRobot();
    }

    private void createGUI() {

        parent = new ParentFrame();
        dialog = new CustomDialog((Frame) null);
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

    private void startTest() throws Exception {

        EventQueue.invokeLater(this::createGUI);

        robot.waitForIdle(delay);
        parent.clickOpenButton(robot);
        robot.waitForIdle(delay);
        window.clickOpenButton(robot);
        robot.waitForIdle(delay);
        dialog.clickOpenButton(robot);
        robot.waitForIdle(delay);
    }

    private void checkUnblockedWindows() throws Exception {

        String msg = " should not be blocked.";
        parent.checkUnblockedFrame (robot, "This frame" + msg);
        robot.waitForIdle(delay);
        window.checkUnblockedWindow(robot, "This window" + msg);
        robot.waitForIdle(delay);
        dialog.checkUnblockedDialog(robot, "This dialog" + msg);
        robot.waitForIdle(delay);
    }

    private void checkBlockedWindows() throws Exception {

        String msg = " should be blocked by the FileDialog.";
        parent.checkBlockedFrame (robot, "This Frame" + msg);
        robot.waitForIdle(delay);
        window.checkBlockedWindow(robot, "This Window" + msg);
        robot.waitForIdle(delay);
        dialog.checkBlockedDialog(robot, "This Dialog" + msg);
        robot.waitForIdle(delay);
    }

    private void docModalTest() throws Exception {

        String msg = "Document modal FileDialog should ";
        parent.checkUnblockedFrame (robot, msg + "not block this Frame.");
        robot.waitForIdle(delay);
        window.checkUnblockedWindow(robot, msg + "not block this Window.");
        robot.waitForIdle(delay);
        dialog.checkBlockedDialog(robot, msg + "block its parent Dialog.");
        robot.waitForIdle(delay);
    }

    public void doTest() throws Exception {

        try {
            startTest();

            switch (modalityType) {
                case APPLICATION_MODAL:
                case TOOLKIT_MODAL:
                    checkBlockedWindows();
                    break;
                case DOCUMENT_MODAL:
                    docModalTest();
                    break;
                case MODELESS:
                    checkUnblockedWindows();
                    break;
            }

            EventQueue.invokeAndWait(() -> { fileDialog.dispose(); });
            robot.waitForIdle(delay);

            if (modalityType != Dialog.ModalityType.MODELESS) {
                checkUnblockedWindows();
            }
        } finally {
            EventQueue.invokeLater(this::closeAll);
        }
    }

    private void closeAll() {
        if (parent != null) { parent.dispose(); }
        if (dialog != null) { dialog.dispose(); }
        if (window != null) { window.dispose(); }
        if (fileDialog != null) { fileDialog.dispose(); }
    }

    class ParentFrame extends TestFrame {

        @Override
        public void doOpenAction() {
            if (window != null) { window.setVisible(true); }
        }
    }

    class CustomDialog extends TestDialog {

        public CustomDialog(Frame f) { super(f); }

        @Override
        public void doOpenAction() {
            fileDialog = new FileDialog(this);
            fileDialog.setModalityType(modalityType);
            fileDialog.setLocation(50, 50);
            fileDialog.setVisible(true);
        }
    }

    class CustomWindow extends TestWindow {

        public CustomWindow(TestFrame f) { super(f); }

        @Override
        public void doOpenAction() {
            if (dialog != null) { dialog.setVisible(true); }
        }
    }
}
