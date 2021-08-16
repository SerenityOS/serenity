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

import static jdk.test.lib.Asserts.*;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class BlockingWindowsTest {

    private TestDialog dialog, childDialog, secondDialog, dummyDialog, parentDialog;
    private TestFrame frame, secondFrame;
    private TestWindow window, childWindow;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private List<Window> allWindows;

    private Dialog hiddenDialog;
    private Frame  hiddenFrame;

    private Dialog.ModalityType modalityType;

    public enum DialogOwner {HIDDEN_DIALOG, NULL_DIALOG, HIDDEN_FRAME, NULL_FRAME, DIALOG, FRAME};

    private BlockingWindowsTest(Dialog.ModalityType modType,
                                boolean             setModal,
                                DialogOwner         owner) throws Exception {

        modalityType = modType;
        robot = new ExtendedRobot();
        EventQueue.invokeLater(() -> {
            createGUI(setModal, owner);
        });
    }

    public BlockingWindowsTest(
            Dialog.ModalityType modalityType, DialogOwner owner) throws Exception {
        this(modalityType, false, owner);
    }

    public BlockingWindowsTest(DialogOwner owner) throws Exception {
        this(null, true, owner);
    }

    private void createGUI(boolean     setModal,
                           DialogOwner owner) {

        allWindows = new ArrayList<>();

        if (owner != DialogOwner.DIALOG) {
            frame = new CustomFrame();
            frame.setLocation(50, 50);
            frame.setVisible(true);
            allWindows.add(frame);
        }

        switch (owner) {
            case DIALOG:
                parentDialog = new ParentDialog((Dialog) null);
                parentDialog.setLocation(50, 50);
                parentDialog.setVisible(true);
                allWindows.add(parentDialog);
                dialog = new CustomDialog(parentDialog);
                break;
            case FRAME:
                dialog = new CustomDialog(frame);
                break;
            case HIDDEN_DIALOG:
                hiddenDialog = new Dialog((Frame) null);
                dialog = new CustomDialog(hiddenDialog);
                allWindows.add(hiddenDialog);
                break;
            case NULL_DIALOG:
                dialog = new CustomDialog((Dialog) null);
                break;
            case HIDDEN_FRAME:
                hiddenFrame = new Frame();
                dialog = new CustomDialog(hiddenFrame);
                allWindows.add(hiddenFrame);
                break;
            case NULL_FRAME:
                dialog = new CustomDialog((Frame) null);
                break;
        }

        assertFalse(dialog == null, "error: null dialog");

        if (setModal) {
            dialog.setModal(true);
        } else if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }

        dialog.setLocation(250, 50);
        allWindows.add(dialog);

        if (owner == DialogOwner.DIALOG) {
            window = new TestWindow(parentDialog);
        } else {
            window = new TestWindow(frame);
        }

        window.setLocation(50, 250);
        allWindows.add(window);

        if (owner == DialogOwner.DIALOG) {
            dummyDialog = new TestDialog(parentDialog);
        } else {
            dummyDialog = new TestDialog(frame);
        }
        dummyDialog.setLocation(450, 450);
        allWindows.add(dummyDialog);

        childWindow = new CustomWindow(dialog);
        childWindow.setLocation(450, 50);
        allWindows.add(childWindow);

        childDialog = new TestDialog(dialog);
        childDialog.setLocation(450, 250);
        allWindows.add(childDialog);

        if (owner == DialogOwner.DIALOG) {
            secondDialog = new CustomDialog(parentDialog);
        } else {
            secondDialog = new CustomDialog(frame);
        }
        if (setModal) {
            secondDialog.setModal(true);
        } else if (modalityType != null) {
            secondDialog.setModalityType(modalityType);
        }

        secondDialog.setLocation(50, 450);
        allWindows.add(secondDialog);

        secondFrame = new TestFrame();
        secondFrame.setLocation(250, 450);
        allWindows.add(secondFrame);
    }

    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            if (parentDialog == null) { frame.clickOpenButton(robot); }
            else { parentDialog.clickOpenButton(robot); }
            robot.waitForIdle(delay);

            dialog.activated.waitForFlagTriggered();
            assertTrue(dialog.activated.flag(), "Dialog did not trigger " +
                "Window Activated event when it became visible");

            dialog.closeGained.waitForFlagTriggered();
            assertTrue(dialog.closeGained.flag(), "The first button did not gain focus " +
                "when the dialog became visible");

            assertTrue(dialog.closeButton.hasFocus(), "The first dialog button " +
                "gained focus, but lost it afterwards");

            if (parentDialog == null) {
                frame.checkBlockedFrame(robot, modalityType + " Dialog is visible.");
            } else {
                parentDialog.checkBlockedDialog(robot, modalityType + " Dialog is visible.");
            }

            dialog.checkUnblockedDialog(robot, "A Frame is visible.");

            dialog.openClicked.reset();
            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            assertFalse(window.focusGained.flag(), "Window gained focus on becoming " +
                "visible when Frame and Dialog are visible");

            window.checkBlockedWindow(robot,
                "The parent of the Window is blocked by " + modalityType + " Dialog.");

            dummyDialog.checkBlockedDialog(robot,
                "The parent of the Dialog is blocked by " + modalityType + " Dialog.");

            childDialog.checkUnblockedDialog(robot,
                "The parent of the Dialog is " + modalityType + " Dialog");

            childWindow.checkUnblockedWindow(robot,
                "The parent of the Window is " + modalityType + " Dialog");

            childWindow.openClicked.reset();
            childWindow.clickOpenButton(robot);
            robot.waitForIdle(delay);

            secondDialog.checkUnblockedDialog(robot,
                "The dialog is " + modalityType + ", the parent of the dialog " +
                "is blocked by another " + modalityType + " dialog.");

            secondFrame.checkBlockedFrame(robot,
                modalityType + " dialog is displayed immediately after showing " +
                "this frame. Another modal dialog is alreay visible");

            secondDialog.clickCloseButton(robot);
            robot.waitForIdle(delay);

            childWindow.checkUnblockedWindow(robot, "A blocking dialog was closed.");
            robot.waitForIdle(delay);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        for (Window w: allWindows) {
            if (w != null) { w.dispose(); }
        }
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
            if (dummyDialog != null) { dummyDialog.setVisible(true); }
            if (childWindow != null) { childWindow.setVisible(true); }
            if (childDialog != null) { childDialog.setVisible(true); }
        }

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }

    class CustomWindow extends TestWindow {

        public CustomWindow(Window parent) {
            super(parent);
        }

        @Override
        public void doOpenAction() {
            if (secondFrame  != null) {  secondFrame.setVisible(true); }
            if (secondDialog != null) { secondDialog.setVisible(true); }
        }
    }

    class ParentDialog extends TestDialog {

        public ParentDialog(Dialog d) { super(d); }

        @Override
        public void doOpenAction() {
            if (dialog != null) {
                dialog.setVisible(true);
            }
        }
    }
}

