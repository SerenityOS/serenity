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

/*
 * @test
 * @key headful
 * @bug 8054358 8055003
 * @summary Check whether application and document modality levels for Dialog
 *          work properly. Also check whether the blocking dialogs are
 *          opening on top of the windows they block.
 *
 * @library ../helpers /lib/client/
 * @library /test/lib
 * @build ExtendedRobot
 * @build Flag
 * @build TestDialog
 * @build TestFrame
 * @run main MultipleDialogs4Test
 */


import java.awt.*;
import static jdk.test.lib.Asserts.*;


public class MultipleDialogs4Test {

    private volatile TopLevelFrame frame;
    private volatile CustomFrame   secondFrame;
    private volatile TestFrame     thirdFrame;
    private volatile TestDialog    dialog, secondDialog, docChildDialog, appChildDialog;

    private static final int delay = 500;


    private void createGUI() {

        frame = new TopLevelFrame();
        frame.setLocation(50, 50);
        frame.setVisible(true);

        dialog = new TestDialog((Dialog) null);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLocation(150, 50);

        appChildDialog = new TestDialog(dialog);
        appChildDialog.setLocation(150, 90);

        secondFrame = new CustomFrame();
        secondFrame.setLocation(50, 250);

        secondDialog = new TestDialog(secondFrame);
        secondDialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        secondDialog.setLocation(150, 250);

        docChildDialog = new TestDialog(secondFrame);
        docChildDialog.setBackground(Color.black);
        docChildDialog.setLocation(250, 250);

        thirdFrame = new TestFrame();
        thirdFrame.setLocation(250, 50);
    }

    public void doTest() throws Exception {

        try {
            EventQueue.invokeAndWait(this::createGUI);

            final int nAttempts = 3;
            ExtendedRobot robot = new ExtendedRobot();
            robot.waitForIdle(delay);

            frame.clickOpenButton(robot);
            robot.waitForIdle(delay);

            secondFrame.clickOpenButton(robot);
            robot.waitForIdle(delay);

            secondFrame.checkBlockedFrame(robot,
                "A document modal dialog should block this Frame.");

            secondDialog.checkUnblockedDialog(robot, "This is a document " +
                "modal dialog. No window blocks it.");

            frame.checkUnblockedFrame(robot,
                "There are no dialogs blocking this Frame.");

            frame.clickCloseButton(robot);
            robot.waitForIdle(delay);

            frame.clickDummyButton(robot, nAttempts, false,
                "The frame still on top even after showing a modal dialog.");
            robot.waitForIdle(delay);

            EventQueue.invokeAndWait(() -> { thirdFrame.setVisible(true); });
            robot.waitForIdle(delay);

            dialog.clickDummyButton(robot);
            robot.waitForIdle(delay);

            secondDialog.clickDummyButton(
                robot, nAttempts, false, "The document modal dialog " +
                "was not blocked by an application modal dialog.");
            robot.waitForIdle(delay);

            EventQueue.invokeLater(() -> { docChildDialog.setVisible(true); });
            robot.waitForIdle(delay);

            Color c = robot.getPixelColor(
                (int) secondDialog.getLocationOnScreen().x + secondDialog.getSize().width  - 25,
                (int) secondDialog.getLocationOnScreen().y + secondDialog.getSize().height - 25);
            assertFalse(c.equals(Color.black), "A dialog which should be blocked " +
                "by document modal dialog overlapping it.");

            EventQueue.invokeLater(() -> { appChildDialog.setVisible(true); });
            robot.waitForIdle(delay);

            appChildDialog.activated.waitForFlagTriggered();
            assertTrue(appChildDialog.activated.flag(), "The child dialog of the " +
                "application modal dialog still not visible.");
            robot.waitForIdle(delay);

            dialog.clickDummyButton(robot, nAttempts, false, "The child dialog of " +
                "the application modal dialog did not overlap it.");
            robot.waitForIdle(delay);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    public void closeAll() {

        if (frame != null) { frame.dispose(); }
        if (dialog != null) { dialog.dispose(); }
        if (appChildDialog != null) { appChildDialog.dispose(); }
        if (secondFrame != null) { secondFrame.dispose(); }
        if (secondDialog != null) { secondDialog.dispose(); }
        if (docChildDialog != null) { docChildDialog.dispose(); }
        if (thirdFrame != null) { thirdFrame.dispose(); }
    }

    class TopLevelFrame extends TestFrame {

        @Override
        public void doOpenAction() { secondFrame.setVisible(true); }
        @Override
        public void doCloseAction() { dialog.setVisible(true); }
    }

    class CustomFrame extends TestFrame {

        @Override
        public void doOpenAction() { secondDialog.setVisible(true); }
    }

    public static void main(String[] args) throws Exception {
        (new MultipleDialogs4Test()).doTest();
    }
}
