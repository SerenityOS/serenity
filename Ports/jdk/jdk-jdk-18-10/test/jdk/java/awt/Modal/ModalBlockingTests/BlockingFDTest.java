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


// FD: Frame -> Dialog

public class BlockingFDTest {

    private TestFrame  frame;
    private TestDialog dialog;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private final Dialog.ModalityType modalityType;
    private final boolean setModal;

    private BlockingFDTest(Dialog.ModalityType modType, boolean modal) throws Exception {

        modalityType = modType;
        setModal = modal;

        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    public BlockingFDTest(Dialog.ModalityType modType) throws Exception {
        this(modType, false);
    }

    public BlockingFDTest() throws Exception {
        this(null, true);
    }

    private void createGUI() {

        frame = new CustomFrame();
        frame.setLocation(50, 50);
        dialog = new TestDialog(frame);
        if (setModal) {
            dialog.setModal(true);
        } else if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }
        dialog.setLocation(250, 50);

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

            if ((modalityType == Dialog.ModalityType.APPLICATION_MODAL) ||
                (modalityType == Dialog.ModalityType.DOCUMENT_MODAL) ||
                (modalityType == Dialog.ModalityType.TOOLKIT_MODAL) ||
                setModal)
            {
                frame.checkBlockedFrame(robot,
                    "Frame is the parent of a visible " + modalityType + " Dialog.");
            } else {
                frame.checkUnblockedFrame(robot,
                    "Frame is the parent of a visible " + modalityType + " Dialog.");
            }
            robot.waitForIdle(delay);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (frame  != null) {  frame.dispose(); }
        if (dialog != null) { dialog.dispose(); }
    }


    class CustomFrame extends TestFrame {

        @Override
        public void doOpenAction() {
            if (dialog != null) { dialog.setVisible(true); }
        }
    }
}
