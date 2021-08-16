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
import java.awt.event.KeyEvent;
import static jdk.test.lib.Asserts.*;

// DFW: Dialog -> Frame -> Window

public class BlockingDFWTest {

    private static final int delay = 500;
    private final ExtendedRobot robot;

    public enum Parent {DIALOG, FRAME};

    private ParentDialog parentDialog;
    private ParentFrame  parentFrame;
    private TestDialog dialog;
    private TestFrame frame;
    private TestWindow window;


    public BlockingDFWTest(Parent parentWin, Dialog.ModalityType modalityType) throws Exception {

        robot = new ExtendedRobot();
        EventQueue.invokeLater(() -> { createGUI(parentWin, modalityType); });
    }

    private void createGUI(Parent parentWin, Dialog.ModalityType modalityType) {

        Window p = null;
        switch (parentWin) {
            case DIALOG:
                parentDialog = new ParentDialog((Dialog) null);
                dialog = new CustomDialog(parentDialog);
                p = parentDialog;
                break;
            case FRAME:
                parentFrame = new ParentFrame();
                dialog = new CustomDialog(parentFrame);
                p = parentFrame;
                break;
        }

        assertFalse(p == null, "invalid parent");
        p.setLocation(50, 50);
        dialog.setLocation(250, 50);
        if (modalityType != null) { dialog.setModalityType(modalityType); }

        frame = new TestFrame();
        frame.setLocation(50, 250);
        window = new TestWindow(frame);
        window.setLocation(250, 250);

        p.setVisible(true);
    }

    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            if (parentDialog != null) { parentDialog.clickOpenButton(robot); }
            else if (parentFrame != null) { parentFrame.clickOpenButton(robot); }
            robot.waitForIdle(delay);

            dialog.activated.waitForFlagTriggered();
            assertTrue(dialog.activated.flag(), "Dialog did not trigger " +
                "Window Activated event when it became visible");

            dialog.closeGained.waitForFlagTriggered();
            assertTrue(dialog.closeGained.flag(), "the 1st button did not gain " +
                "focus when the dialog became visible");

            assertTrue(dialog.closeButton.hasFocus(), "the 1st dialog button " +
                "gained focus, but lost it afterwards");

            dialog.openGained.reset();
            robot.type(KeyEvent.VK_TAB);

            dialog.openGained.waitForFlagTriggered();
            assertTrue(dialog.openGained.flag(), "Tab navigation did not happen " +
                "properly; open button did not gain focus on tab press " +
                "when parent frame is visible");

            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            frame.activated.waitForFlagTriggered();
            assertTrue(frame.activated.flag(), "Frame did not trigger " +
                "Window Activated event when made visible.");

            frame.checkUnblockedFrame(robot, "Frame should not be blocked.");
            window.checkUnblockedWindow(robot, "Window should not be blocked.");
            robot.waitForIdle(delay);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if ( frame != null) {  frame.dispose(); }
        if (window != null) { window.dispose(); }
        if (parentDialog != null) { parentDialog.dispose(); }
        if (parentFrame  != null) {  parentFrame.dispose(); }
    }


    class ParentDialog extends TestDialog {

        public ParentDialog(Dialog d) { super(d); }

        @Override
        public void doOpenAction() {
            if (dialog != null) { dialog.setVisible(true); }
        }
    }

    class ParentFrame extends TestFrame {

        @Override
        public void doOpenAction() {
            if (dialog != null) { dialog.setVisible(true); }
        }
    }

    class CustomDialog extends TestDialog {

        public CustomDialog(Dialog d) { super(d); }
        public CustomDialog(Frame  f) { super(f); }

        @Override
        public void doOpenAction() {
            if (frame  != null) {  frame.setVisible(true); }
            if (window != null) { window.setVisible(true); }
        }
    }
}
