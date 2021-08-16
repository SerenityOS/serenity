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


public class FrameToFrontModelessTest {

    private volatile TestDialog dialog;
    private volatile TestFrame  leftFrame, rightFrame;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private boolean isModeless;

    public FrameToFrontModelessTest(boolean modeless) throws Exception {
        isModeless = modeless;
        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    private void createGUI() {

        leftFrame = new TestFrame();
        leftFrame.setSize(200, 100);
        leftFrame.setLocation(50, 50);
        leftFrame.setVisible(true);

        dialog = new TestDialog(leftFrame);
        if (isModeless) { dialog.setModalityType(Dialog.ModalityType.MODELESS); }
        dialog.setSize(200, 100);
        dialog.setLocation(150, 50);
        dialog.setVisible(true);

        rightFrame = new TestFrame();
        rightFrame.setSize(200, 100);
        rightFrame.setLocation(250, 50);
        rightFrame.setVisible(true);
    }


    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            EventQueue.invokeAndWait(() -> { leftFrame.toFront(); });
            robot.waitForIdle(delay);

            leftFrame.clickDummyButton(
                robot, 7, false, "Calling toFront method on the parent " +
                "left frame brought it to the top of the child dialog");
            robot.waitForIdle(delay);

            // show the right frame appear on top of the dialog
            rightFrame.clickDummyButton(robot);
            robot.waitForIdle(delay);

            String msg = "The " + (isModeless ? "modeless" : "non-modal") +
                " dialog still on top of the right frame" +
                " even after a button on the frame is clicked";
            dialog.clickDummyButton(robot, 7, false, msg);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if (leftFrame  != null) {  leftFrame.dispose(); }
        if (rightFrame != null) { rightFrame.dispose(); }
    }
}

