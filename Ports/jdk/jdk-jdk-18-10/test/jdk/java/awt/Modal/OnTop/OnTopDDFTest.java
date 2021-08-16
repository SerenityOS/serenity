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
import static jdk.test.lib.Asserts.*;

// DDF: Dialog - Dialog - Frame

public class OnTopDDFTest {

    private volatile TestDialog dialog, leftDialog;
    private volatile TestFrame rightFrame;
    private volatile Frame hiddenFrame;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    boolean setModal;

    Dialog.ModalityType modalityType;

    private OnTopDDFTest(Dialog.ModalityType modType,
                         boolean             modal) throws Exception {
        modalityType = modType;
        setModal = modal;

        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    public OnTopDDFTest(Dialog.ModalityType modalityType) throws Exception {
        this(modalityType, false);
    }

    public OnTopDDFTest() throws Exception {
        this(null, true);
    }

    private void createGUI() {

        hiddenFrame = new Frame();
        leftDialog = new TestDialog(hiddenFrame);
        leftDialog.setSize(200, 100);
        leftDialog.setLocation(50, 50);
        leftDialog.setVisible(true);

        dialog = new CustomDialog(leftDialog);
        if (setModal) {
            dialog.setModal(true);
            modalityType = dialog.getModalityType();
        } else if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }

        dialog.setSize(200, 100);
        dialog.setLocation(200, 50);

        rightFrame = new TestFrame();
        rightFrame.setSize(200, 100);
        rightFrame.setLocation(350, 50);

        dialog.setVisible(true);
    }

    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            dialog.activated.waitForFlagTriggered();
            assertTrue(dialog.activated.flag(), "Dialog still not visible.");

            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            if ((modalityType == Dialog.ModalityType.MODELESS) ||
                (modalityType == Dialog.ModalityType.DOCUMENT_MODAL)) {

                rightFrame.clickCloseButton(robot);
                robot.waitForIdle(delay);

                rightFrame.closeClicked.reset();
                dialog.transferFocusToDialog(robot, "A Frame partially hides the " +
                    modalityType + " Dialog.", dialog.openButton);
                robot.waitForIdle(delay);

                dialog.checkUnblockedDialog(robot,
                    "This is " + modalityType + " dialog and no other Dialogs blocks it.");
                robot.waitForIdle(delay);

                rightFrame.closeClicked.waitForFlagTriggered(5);
                assertFalse(rightFrame.closeClicked.flag(), "Clicking on " + modalityType +
                    "dialog did not bring it to the top. A frame on top of Dialog.");
                robot.waitForIdle(delay);

                dialog.closeClicked.reset();
                if (modalityType == Dialog.ModalityType.MODELESS) {
                    leftDialog.transferFocusToDialog(robot, "This dialog is not " +
                        "blocked by any other dialogs.", leftDialog.closeButton);
                } else {
                    leftDialog.transferFocusToBlockedDialog(robot, "This dialog is not " +
                        "blocked by any other dialogs.", leftDialog.closeButton);
                }
            } else {
                dialog.checkUnblockedDialog(robot, "Checking if modal dialog " +
                    "appears on top of blocked Frame.");
                robot.waitForIdle(delay);

                rightFrame.closeClicked.waitForFlagTriggered(5);
                assertFalse(rightFrame.closeClicked.flag(),
                    "Frame on top of an application modal Dialog.");
                robot.waitForIdle(delay);

                leftDialog.transferFocusToBlockedDialog(robot,
                    "An application modal dialog blocks the Dialog.", leftDialog.closeButton);
            }

            robot.waitForIdle(delay);

            dialog.clickCloseButton(robot);
            robot.waitForIdle(delay);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if (leftDialog != null) { leftDialog.dispose(); }
        if (rightFrame != null) { rightFrame.dispose(); }
        if (hiddenFrame != null) { hiddenFrame.dispose(); }
    }


    class CustomDialog extends TestDialog {

        public CustomDialog(Dialog d) { super(d); }

        @Override
        public void doOpenAction() {
            if (rightFrame != null) {
                rightFrame.setVisible(true);
            }
        }
    }
}
