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

// FDF: Frame - Dialog - Frame

public class OnTopFDFTest {

    private volatile CustomDialog dialog;
    private volatile TestFrame leftFrame, rightFrame;
    private volatile Dialog hiddenDialog;
    private volatile Frame  hiddenFrame;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    public enum DialogOwner {HIDDEN_DIALOG, NULL_DIALOG, HIDDEN_FRAME, NULL_FRAME, FRAME};

    private DialogOwner owner;
    boolean setModal;

    Dialog.ModalityType modalityType;

    private OnTopFDFTest(Dialog.ModalityType modType,
                         boolean             modal,
                         DialogOwner         o) throws Exception {

        modalityType = modType;
        setModal = modal;
        owner = o;
        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    public OnTopFDFTest(Dialog.ModalityType modalityType,
                        DialogOwner         o) throws Exception {
        this(modalityType, false, o);
    }

    public OnTopFDFTest(DialogOwner o) throws Exception {
        this(null, true, o);
    }

    private void createGUI() {

        leftFrame = new TestFrame();
        leftFrame.setSize(200, 100);
        leftFrame.setLocation(50, 50);
        leftFrame.setVisible(true);

        switch (owner) {
            case HIDDEN_DIALOG:
                hiddenDialog = new Dialog((Frame) null);
                dialog = new CustomDialog(hiddenDialog);
                break;
            case NULL_DIALOG:
                dialog = new CustomDialog((Dialog) null);
                break;
            case HIDDEN_FRAME:
                hiddenFrame = new Frame();
                dialog = new CustomDialog(hiddenFrame);
                break;
            case NULL_FRAME:
                dialog = new CustomDialog((Frame) null);
                break;
            case FRAME:
                dialog = new CustomDialog(leftFrame);
                break;
        }

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

    private void BlockingTest() throws Exception {

        dialog.checkUnblockedDialog(robot, "Checking if " + modalityType +
            " dialog appears on top of blocked Frame.");
        robot.waitForIdle(delay);

        rightFrame.closeClicked.waitForFlagTriggered(5);
        assertFalse(rightFrame.closeClicked.flag(),
            "Frame is on top of " + modalityType + " Dialog.");
        robot.waitForIdle(delay);

        leftFrame.transferFocusToBlockedFrame(robot,
            modalityType + " dialog blocks the Frame.", leftFrame.closeButton);
        robot.waitForIdle(delay);

        dialog.clickCloseButton(robot);
        robot.waitForIdle(delay);
    }

    private void Test() throws Exception {

        rightFrame.clickCloseButton(robot);
        robot.waitForIdle(delay);

        rightFrame.closeClicked.reset();
        dialog.transferFocusToDialog(
            robot, "Frame partially hides the dialog.", dialog.openButton);
        robot.waitForIdle(delay);

        dialog.checkUnblockedDialog(
            robot, "This is " + modalityType + " dialog.");
        robot.waitForIdle(delay);

        rightFrame.closeClicked.waitForFlagTriggered(5);
        assertFalse(rightFrame.closeClicked.flag(), "Clicking on a " +
            modalityType + " dialog did not bring it to the top. " +
            "A frame is on top of the dialog.");
        robot.waitForIdle(delay);

        dialog.closeClicked.reset();

        if (owner == DialogOwner.FRAME) {

            if (modalityType == Dialog.ModalityType.MODELESS) {
                leftFrame.transferFocusToFrame(robot, "modeless dialog " +
                    "partially hides the Frame.", leftFrame.closeButton);
            } else {
                leftFrame.transferFocusToBlockedFrame(robot, "a document modal " +
                    "dialog partially hides the Frame.", leftFrame.closeButton);
            }

        } else {

            leftFrame.transferFocusToFrame(robot,
                "A dialog partially hides the Frame.", leftFrame.closeButton);
            robot.waitForIdle(delay);

            leftFrame.checkUnblockedFrame(robot,
                modalityType + " dialog present should not block this Frame.");
            robot.waitForIdle(delay);

            dialog.closeClicked.waitForFlagTriggered(5);
            assertFalse(dialog.closeClicked.flag(), "Clicking on a frame did not " +
                "bring it to the top. The document modal dialog is staying on top.");
        }

        robot.waitForIdle(delay);
    }


    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            dialog.activated.waitForFlagTriggered();
            assertTrue(dialog.activated.flag(), "Dialog still not visible.");

            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            switch (modalityType) {
                case DOCUMENT_MODAL:
                case MODELESS:
                    Test();
                    break;
                case APPLICATION_MODAL:
                case TOOLKIT_MODAL:
                    BlockingTest();
                    break;
            }

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if (leftFrame  != null) {  leftFrame.dispose(); }
        if (rightFrame != null) { rightFrame.dispose(); }
        if (hiddenDialog != null) { hiddenDialog.dispose(); }
        if (hiddenFrame  != null) {  hiddenFrame.dispose(); }
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
