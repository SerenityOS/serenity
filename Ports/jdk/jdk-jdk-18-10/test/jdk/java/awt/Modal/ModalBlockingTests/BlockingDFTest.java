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


// DF: Dialog -> Frame

public class BlockingDFTest {

    private TestDialog dialog;
    private TestFrame  frame;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private BlockingDFTest(Dialog.ModalityType modalityType,
                           boolean             setModal) throws Exception {

        robot = new ExtendedRobot();
        EventQueue.invokeLater(() -> { createGUI(modalityType, setModal); });
    }

    public BlockingDFTest(Dialog.ModalityType modalityType) throws Exception {
        this(modalityType, false);
    }

    public BlockingDFTest() throws Exception { this(null, true); }


    private void createGUI(Dialog.ModalityType modalityType,
                           boolean             setModal) {

        frame = new TestFrame();
        frame.setLocation(50, 50);
        frame.setVisible(true);

        dialog = new TestDialog((Dialog) null);
        dialog.setLocation(250, 50);
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
            dialog.activated.waitForFlagTriggered();
            assertTrue(dialog.activated.flag(), "Dialog did not trigger " +
                "Window Activated event when it became visible");

            dialog.closeGained.waitForFlagTriggered();
            assertTrue(dialog.closeGained.flag(), "The 1st button did not " +
                "gain focus when the dialog became visible");

            dialog.checkUnblockedDialog(robot, "");
            frame.checkBlockedFrame(robot, "");
            robot.waitForIdle(delay);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (frame  != null) {  frame.dispose(); }
        if (dialog != null) { dialog.dispose(); }
    }
}
