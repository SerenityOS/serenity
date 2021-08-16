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

// DD: Dialog -> Dialog

public class BlockingDDTest {

    private TestDialog parent, dialog;

    private static final int delay = 1000;
    private final ExtendedRobot robot;

    private final Dialog.ModalityType modalityType;
    private final boolean setModal;

    private BlockingDDTest(Dialog.ModalityType modType, boolean modal) throws Exception {

        modalityType = modType;
        setModal = modal;
        robot = new ExtendedRobot();
        createGUI();
    }

    public BlockingDDTest(Dialog.ModalityType modType) throws Exception {
        this(modType, false);
    }

    public BlockingDDTest() throws Exception {
        this(null, true);
    }


    private void showParent() {

        parent = new TestDialog((Frame) null);
        parent.setTitle("Parent");
        parent.setLocation(50, 50);
        parent.setVisible(true);
    }

    private void showChild() {

        dialog = new TestDialog(parent);
        if (setModal) {
            dialog.setModal(true);
        } else if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }

        dialog.setLocation(250, 50);
        dialog.setVisible(true);
    }


    private void createGUI() throws Exception {

        EventQueue.invokeAndWait(this::showParent);
        robot.waitForIdle(delay);
        EventQueue.invokeLater(this::showChild);
        robot.waitForIdle(delay);
    }

    public void doTest() throws Exception {

        try {
            dialog.activated.waitForFlagTriggered();
            assertTrue(dialog.activated.flag(), "Dialog did not trigger " +
                "Window Activated event when it became visible");

            dialog.closeGained.waitForFlagTriggered();
            assertTrue(dialog.closeGained.flag(), "the 1st Dialog button " +
                "did not gain focus when it became visible");

            assertTrue(dialog.closeButton.hasFocus(), "the 1st Dialog button " +
                "gained the focus but lost it afterwards");

            dialog.checkUnblockedDialog(robot, "Modal Dialog shouldn't be blocked.");

            if ((modalityType == Dialog.ModalityType.APPLICATION_MODAL) ||
                (modalityType == Dialog.ModalityType.DOCUMENT_MODAL) ||
                (modalityType == Dialog.ModalityType.TOOLKIT_MODAL) ||
                dialog.isModal())
            {
                parent.checkBlockedDialog(robot,
                    "Dialog is the parent of a visible " + modalityType + " Dialog.");
            } else {
                parent.checkUnblockedDialog(robot,
                    "Dialog is the parent of a visible " + modalityType + " Dialog.");
            }

            robot.waitForIdle(delay);
        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (parent != null) { parent.dispose(); }
        if (dialog != null) { dialog.dispose(); }
    }
}
