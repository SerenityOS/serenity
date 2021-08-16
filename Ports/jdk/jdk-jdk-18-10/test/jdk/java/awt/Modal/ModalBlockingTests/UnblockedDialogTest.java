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


public class UnblockedDialogTest {

    private TestDialog dialog;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private Dialog parentDialog;
    private Frame  parentFrame;

    private enum DialogOwner {HIDDEN_DIALOG, HIDDEN_FRAME, NULL_DIALOG, NULL_FRAME};

    Dialog.ModalityType modalityType;
    boolean setModal;

    private UnblockedDialogTest(Dialog.ModalityType modType,
                                boolean             set) throws Exception {

        robot = new ExtendedRobot();
        modalityType = modType;
        setModal = set;
    }

    public UnblockedDialogTest(Dialog.ModalityType modType) throws Exception {
        this(modType, false);
    }

    public UnblockedDialogTest() throws Exception { this(null, true); }


    private void createGUI(DialogOwner owner) {

        switch (owner) {
            case HIDDEN_DIALOG:
                parentDialog = new Dialog((Frame) null);
                dialog = new TestDialog(parentDialog);
                break;
            case NULL_DIALOG:
                dialog = new TestDialog((Dialog) null);
                break;
            case HIDDEN_FRAME:
                parentFrame = new Frame();
                dialog = new TestDialog(parentFrame);
                break;
            case NULL_FRAME:
                dialog = new TestDialog((Frame) null);
                break;
        }

        assertFalse(dialog == null, "error: null dialog");

        dialog.setLocation(50, 50);
        if (setModal) {
            dialog.setModal(true);
        } else if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }

        dialog.setVisible(true);
    }

    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            for (DialogOwner owner: DialogOwner.values()) {

                EventQueue.invokeLater(() -> { createGUI(owner); });

                robot.waitForIdle(delay);

                dialog.activated.waitForFlagTriggered();
                assertTrue(dialog.activated.flag(), "Dialog did not trigger " +
                    "Window Activated event when it became visible");

                dialog.closeGained.waitForFlagTriggered();
                assertTrue(dialog.closeGained.flag(), "The 1st button did not " +
                    "gain focus when the dialog became visible");

                dialog.checkUnblockedDialog(robot, "");
                robot.waitForIdle(delay);
            }

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if (parentDialog != null) { parentDialog.dispose(); }
        if (parentFrame  != null) {  parentFrame.dispose(); }
    }
}
