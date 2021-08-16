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

public class DialogToFrontModelessTest {

    private volatile TestDialog dialog, leftDialog;
    private volatile TestFrame  rightFrame;
    private volatile Frame parent;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private boolean isModeless;

    public DialogToFrontModelessTest(boolean modeless) throws Exception {
        isModeless = modeless;
        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    public DialogToFrontModelessTest() throws Exception { this(true); }

    private void createGUI() {

        parent = new Frame();

        leftDialog = new TestDialog(parent);
        leftDialog.setSize(200, 100);
        leftDialog.setLocation(50, 50);
        leftDialog.setVisible(true);

        dialog = new TestDialog(leftDialog);

        if (isModeless) { dialog.setModalityType(Dialog.ModalityType.MODELESS); }

        dialog.setSize(200, 100);
        dialog.setLocation(150, 50);

        rightFrame = new TestFrame();
        rightFrame.setSize(200, 100);
        rightFrame.setLocation(250, 50);

        dialog.setVisible(true);
        rightFrame.setVisible(true);
    }

    public void doTest() throws Exception {

        try {
            robot.waitForIdle(delay);

            rightFrame.clickCloseButton(robot);
            robot.waitForIdle(delay);

            EventQueue.invokeAndWait(() -> { leftDialog.toFront(); });
            robot.waitForIdle(delay);

            leftDialog.clickDummyButton(robot, 7, false,
                "Calling toFront method for the parent left dialog " +
                "brought it to the top of the child dialog.");
            robot.waitForIdle(delay);

            rightFrame.clickDummyButton(robot);
            robot.waitForIdle(delay);

            String msg = "The " + (isModeless ? "modeless" : "non-modal") +
            " dialog still on top of the right frame" +
            " even after a button on the frame is clicked.";
            dialog.clickDummyButton(robot, 7, false, msg);
            robot.waitForIdle(delay);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if (parent != null) { parent.dispose(); }
        if (leftDialog != null) { leftDialog.dispose(); }
        if (rightFrame != null) { rightFrame.dispose(); }
    }
}
