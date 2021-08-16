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


// FDW: Frame -> Dialog -> Window

public class BlockingFDWTest {

    private TestFrame  frame;
    private TestDialog dialog;
    private TestWindow window;

    private Dialog hiddenDialog;
    private Frame  hiddenFrame;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    public enum DialogOwner {HIDDEN_DIALOG, NULL_DIALOG, HIDDEN_FRAME, NULL_FRAME};

    public BlockingFDWTest(Dialog.ModalityType modalityType,
                           DialogOwner         owner) throws Exception {

        robot = new ExtendedRobot();
        EventQueue.invokeLater(() -> { createGUI(modalityType, owner); });
    }

    private void createGUI(Dialog.ModalityType modalityType,
                           DialogOwner         owner) {

        frame = new CustomFrame();
        frame.setLocation(50, 50);

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
        }

        assertFalse(dialog == null, "error: null dialog");

        dialog.setLocation(250, 50);
        if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }

        window = new TestWindow(frame);
        window.setLocation(450, 50);

        frame.setVisible(true);
    }

    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            frame.clickOpenButton(robot);
            robot.waitForIdle(delay);

            dialog.activated.waitForFlagTriggered();
            assertTrue(dialog.activated.flag(), "Dialog did not trigger " +
                "Window Activated event when it became visible");

            dialog.closeGained.waitForFlagTriggered();
            assertTrue(dialog.closeGained.flag(), "the 1st Dialog button " +
                "did not gain focus when it became visible");

            assertTrue(dialog.closeButton.hasFocus(), "the 1st Dialog button " +
                "gained the focus but lost it afterwards");

            frame.checkUnblockedFrame(robot, "A " + dialog.getModalityType() + " dialog is visible.");

            dialog.checkUnblockedDialog(robot, "A Frame is visible.");

            dialog.openClicked.reset();
            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            window.checkUnblockedWindow(robot,
                "A Frame and a " + dialog.getModalityType() + " Dialog are visible.");
            robot.waitForIdle(delay);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (frame  != null) {  frame.dispose(); }
        if (dialog != null) { dialog.dispose(); }
        if (window != null) { window.dispose(); }
        if (hiddenDialog != null) { hiddenDialog.dispose(); }
        if (hiddenFrame  != null) {  hiddenFrame.dispose(); }
    }


    class CustomFrame extends TestFrame {

        @Override
        public void doOpenAction() {
            if (dialog != null) { dialog.setVisible(true); }
        }
    }

    class CustomDialog extends TestDialog {

        public CustomDialog(Dialog dialog) {
            super(dialog);
        }

        public CustomDialog(Frame frame) {
            super(frame);
        }

        @Override
        public void doOpenAction() {
            if (window != null) { window.setVisible(true); }
        }

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }
}
