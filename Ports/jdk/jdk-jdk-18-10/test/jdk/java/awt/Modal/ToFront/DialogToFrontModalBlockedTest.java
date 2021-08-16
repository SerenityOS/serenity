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

public class DialogToFrontModalBlockedTest {

    private volatile CustomDialog dialog;
    private volatile TestDialog leftDialog;
    private volatile TestFrame  rightFrame;
    private volatile Frame parent;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private DialogToFrontModalBlockedTest(Dialog.ModalityType modalityType,
                                          boolean             setModal) throws Exception {

        robot = new ExtendedRobot();
        EventQueue.invokeLater(() -> {
            createGUI(modalityType, setModal);
        });
    }

    public DialogToFrontModalBlockedTest(Dialog.ModalityType modalityType) throws Exception {
        this(modalityType, false);
    }

    public DialogToFrontModalBlockedTest() throws Exception {
        this(null, true);
    }

    private void createGUI(Dialog.ModalityType modalityType,
                           boolean             setModal) {

        parent = new Frame();
        leftDialog = new TestDialog(parent);
        leftDialog.setSize(200, 100);
        leftDialog.setLocation(50, 50);
        leftDialog.setVisible(true);

        dialog = new CustomDialog(leftDialog);

        if (setModal) { dialog.setModal(true); }
        else if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }

        dialog.setSize(200, 100);
        dialog.setLocation(150, 50);

        rightFrame = new TestFrame();
        rightFrame.setSize(200, 100);
        rightFrame.setLocation(250, 50);

        if (setModal || modalityType == Dialog.ModalityType.APPLICATION_MODAL) {
            rightFrame.setModalExclusionType(
                Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        } else if (modalityType == Dialog.ModalityType.TOOLKIT_MODAL) {
            rightFrame.setModalExclusionType(
                Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        }

        dialog.setVisible(true);
    }

    public void doTest() throws Exception {

        try {
            robot.waitForIdle(delay);

            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            rightFrame.clickCloseButton(robot);
            robot.waitForIdle(delay);

            EventQueue.invokeAndWait(() -> { leftDialog.toFront(); });
            robot.waitForIdle(delay);

            rightFrame.clickDummyButton(robot);

            EventQueue.invokeAndWait(() -> { leftDialog.toFront(); });
            robot.waitForIdle(delay);

            leftDialog.clickDummyButton(robot, 7, false, "Calling toFront " +
                    "for Dialog blocked by " + dialog.getModalityType() +
                    "dialog brought it to the top of the modal dialog");

            robot.waitForIdle(delay);
        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if (parent != null) { parent.dispose(); }
        if (leftDialog != null) { leftDialog.dispose(); }
        if (rightFrame != null) { rightFrame.dispose(); }
    }

    class CustomDialog extends TestDialog {

        public CustomDialog(Dialog d) { super(d); }
        public CustomDialog(Frame  f) { super(f); }

        @Override
        public void doOpenAction() {
            if (rightFrame != null) {
                rightFrame.setVisible(true);
            }
        }
    }
}
