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


public class FrameToFrontModalBlockedTest {

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

    private FrameToFrontModalBlockedTest(Dialog.ModalityType modType,
                                         boolean             modal,
                                         DialogOwner         o) throws Exception {
        modalityType = modType;
        setModal = modal;
        owner = o;

        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    public FrameToFrontModalBlockedTest(Dialog.ModalityType modalityType,
                                        DialogOwner         o) throws Exception {
        this(modalityType, false, o);
    }

    public FrameToFrontModalBlockedTest(DialogOwner o) throws Exception {
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
        dialog.setLocation(230, 50);

        rightFrame = new TestFrame();
        rightFrame.setSize(200, 100);
        rightFrame.setLocation(280, 50);

        if  (setModal ||
            (modalityType == Dialog.ModalityType.APPLICATION_MODAL)) {
            rightFrame.setModalExclusionType(
                    Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        }

        if (modalityType == Dialog.ModalityType.TOOLKIT_MODAL) {
            rightFrame.setModalExclusionType(
                    Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        }

        dialog.setVisible(true);
    }

    private void blockingTest() throws Exception {

        dialog.clickOpenButton(robot);
        robot.waitForIdle(delay);

        rightFrame.clickCloseButton(robot);
        robot.waitForIdle(delay);

        EventQueue.invokeAndWait(() -> { leftFrame.toFront(); });
        robot.waitForIdle(delay);

        rightFrame.clickDummyButton(robot);
        robot.waitForIdle(delay);

        EventQueue.invokeAndWait(() -> { leftFrame.toFront(); });
        robot.waitForIdle(delay);

        leftFrame.clickDummyButton(robot, 7, false,
            "Calling toFront for Frame blocked by " + dialog.getModalityType() +
            " dialog brought it to the top of the modal dialog.");
        robot.waitForIdle(delay);
    }

    private void docModalTest() throws Exception {

        if (owner == DialogOwner.FRAME) { blockingTest(); }
        else {
            EventQueue.invokeAndWait(() -> { leftFrame.toFront(); });
            robot.waitForIdle(delay);

            leftFrame.clickDummyButton(robot);
            robot.waitForIdle(delay);

            EventQueue.invokeAndWait(() -> { dialog.toFront(); });
            robot.waitForIdle(delay);

            dialog.clickDummyButton(robot);
            robot.waitForIdle(delay);
        }

    }


    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            switch (modalityType) {
                case APPLICATION_MODAL:
                case TOOLKIT_MODAL:
                    blockingTest();
                    break;
                case DOCUMENT_MODAL:
                    docModalTest();
                    break;
                default:
                    throw new RuntimeException(
                        "improper modality type, please do not use it for this test");
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
