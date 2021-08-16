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


// FDF: Frame->Dialog->Frame

public class ToBackFDFTest {

    private volatile CustomDialog dialog;
    private volatile TestFrame leftFrame, rightFrame;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private Dialog hiddenDialog;
    private Frame  hiddenFrame;

    public enum DialogOwner {HIDDEN_DIALOG, NULL_DIALOG, HIDDEN_FRAME, NULL_FRAME, FRAME};

    private DialogOwner owner;
    private volatile boolean setModal;

    private Dialog.ModalityType modalityType;

    private ToBackFDFTest(Dialog.ModalityType modType,
                          boolean             modal,
                          DialogOwner         o) throws Exception {
        modalityType = modType;
        setModal = modal;
        owner = o;

        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    public ToBackFDFTest(Dialog.ModalityType modalityType,
                         DialogOwner         o) throws Exception {
        this(modalityType, false, o);
    }

    public ToBackFDFTest(boolean modal, DialogOwner o) throws Exception {
        this(null, modal, o);
    }

    private void createGUI() {

        leftFrame = new TestFrame();
        leftFrame.setLocation(50, 50);
        leftFrame.setBackground(Color.BLUE);
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

        if (modalityType == null) {
            dialog.setModal(setModal);
            modalityType = dialog.getModalityType();
        } else if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }

        dialog.setBackground(Color.WHITE);
        dialog.setLocation(150, 50);

        rightFrame = new TestFrame();
        rightFrame.setLocation(250, 50);
        rightFrame.setBackground(Color.RED);

        if (modalityType == Dialog.ModalityType.APPLICATION_MODAL) {
            rightFrame.setModalExclusionType(
                Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        } else if (modalityType == Dialog.ModalityType.TOOLKIT_MODAL) {
            rightFrame.setModalExclusionType(
                Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        }

        dialog.setVisible(true);
    }

    private void checkIfLeftOnTop(boolean refState, String msg) {

        Point p = leftFrame.getLocationOnScreen();
        int x = p.x + (int)(leftFrame.getWidth()  * 0.9);
        int y = p.y + (int)(leftFrame.getHeight() * 0.9);
        boolean f = robot.getPixelColor(x, y).equals(leftFrame.getBackground());
        assertEQ(refState, f, msg);
    }

    private void checkIfRightOnTop(boolean refState, String msg) {

        Point p = rightFrame.getLocationOnScreen();
        int x = p.x + (int)(rightFrame.getWidth()  * 0.1);
        int y = p.y + (int)(rightFrame.getHeight() * 0.9);
        boolean f = robot.getPixelColor(x, y).equals(rightFrame.getBackground());
        assertEQ(refState, f, msg);
    }

    private void Test() throws Exception {

        String type =
            dialog.getModalityType().toString().toLowerCase().replace('_', ' ');

        final String msg1 = "The " + type + "dialog was " +
            "overlapped by the blocked frame.";
        EventQueue.invokeAndWait(() -> { checkIfLeftOnTop(false, msg1); });

        EventQueue.invokeAndWait(() -> { leftFrame.toFront(); });
        robot.waitForIdle(delay);

        final String msg2 = "The dialog is still overlapped by the right frame" +
            " after calling toFront method for the blocked (left) frame.";
        EventQueue.invokeAndWait(() -> { checkIfRightOnTop(false, msg2); });

        final String msg3 = "The " + type + " dialog was overlapped by the " +
            "blocked frame after calling toFront method for the blocked frame.";
        EventQueue.invokeAndWait(() -> { checkIfLeftOnTop(false, msg3); });


        if (owner == DialogOwner.FRAME) { return; }

        EventQueue.invokeAndWait(() -> { leftFrame.toBack(); });
        robot.waitForIdle(delay);

        final String msg4 = "Calling toBack " +
            "for the blocked frame pushed the blocking dialog to back.";
        EventQueue.invokeAndWait(() -> { checkIfRightOnTop(false, msg4); });

        final String msg5 = "The " + type + " dialog was overlapped " +
            "by the blocked frame after toBack was called for the left frame.";
        EventQueue.invokeAndWait(() -> { checkIfLeftOnTop(false, msg5); });
    }

    private void docTest() throws Exception {

        if (owner == DialogOwner.FRAME) { Test(); }
        else {

            final String msg1 = "toBack was called for the dialog.";
            EventQueue.invokeAndWait(() -> {  checkIfLeftOnTop(true, msg1); });
            EventQueue.invokeAndWait(() -> { checkIfRightOnTop(true, msg1); });

            EventQueue.invokeAndWait(() -> { dialog.toFront(); });
            robot.waitForIdle(delay);

            final String msg2 = "Dialog still behind " +
                "the right frame even after calling toFront method.";
            EventQueue.invokeAndWait(() -> { checkIfRightOnTop(false, msg2); });
            final String msg3 = "The document modal dialog " +
                "gone behind the blocked left frame.";
            EventQueue.invokeAndWait(() -> { checkIfLeftOnTop(false, msg3); });

            EventQueue.invokeAndWait(() -> { leftFrame.toBack(); });
            robot.waitForIdle(delay);

            final String msg4 = "Calling toBack for the left " +
                "frame pushed the document modal dialog to back.";
            EventQueue.invokeAndWait(() -> { checkIfRightOnTop(false, msg4); });
            final String msg5 = "The document modal dialog " +
                "was pushed behind the left frame when toBack called for the frame.";
            EventQueue.invokeAndWait(() -> { checkIfRightOnTop(false, msg5); });
        }
    }

    private void modelessTest() throws Exception {

        if (owner == DialogOwner.FRAME) {
            final String msg = "The modeless dialog was " +
                "pushed behind the parent left frame after toBack call.";
            EventQueue.invokeAndWait(() -> { checkIfLeftOnTop(false, msg); });
        } else {
            final String msg =
                "Dialog should not overlap the frame after calling toBack.";
            EventQueue.invokeAndWait(() -> {  checkIfLeftOnTop(true, msg); });
            EventQueue.invokeAndWait(() -> { checkIfRightOnTop(true, msg); });
        }

        EventQueue.invokeAndWait(() -> { dialog.toFront(); });
        robot.waitForIdle(delay);

        final String msg1 = "The frames should not overlap the dialog " +
            "after calling toFront for it.";
        EventQueue.invokeAndWait(() -> {  checkIfLeftOnTop(false, msg1); });
        EventQueue.invokeAndWait(() -> { checkIfRightOnTop(false, msg1); });

        if (owner == DialogOwner.FRAME) { return; }

        EventQueue.invokeAndWait(() -> { leftFrame.toBack(); });
        robot.waitForIdle(delay);

        final String msg2 = "Calling toBack method for the " +
            "left frame pushed the modeless dialog to back.";
        EventQueue.invokeAndWait(() -> { checkIfRightOnTop(false, msg2); });
        final String msg3 = "The modeless dialog was pushed " +
            "behind the left frame after toBack was called for the frame.";
        EventQueue.invokeAndWait(() -> { checkIfLeftOnTop(false, msg3); });
    }

    public void doTest() throws Exception {

        try {
            robot.waitForIdle(delay);

            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            dialog.clickCloseButton(robot);
            robot.waitForIdle(delay);

            EventQueue.invokeAndWait(() -> { dialog.toBack(); });
            robot.waitForIdle(delay);

            switch (modalityType) {
                case APPLICATION_MODAL:
                case TOOLKIT_MODAL:
                    Test();
                    break;
                case DOCUMENT_MODAL:
                    docTest();
                    break;
                case MODELESS:
                    modelessTest();
                    break;
            }
        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog       != null) {       dialog.dispose(); }
        if (leftFrame    != null) {    leftFrame.dispose(); }
        if (rightFrame   != null) {   rightFrame.dispose(); }
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
