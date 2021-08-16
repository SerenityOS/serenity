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
 * @bug 8054358
 * @summary Check correctness of modal blocking behavior for a chain of Dialogs
 *          having different modality types with a Frame as a document root.
 *
 * @library ../helpers /lib/client/
 * @library /test/lib
 * @build ExtendedRobot
 * @build Flag
 * @build TestDialog
 * @build TestFrame
 * @run main/timeout=500 MultipleDialogs3Test
 */


import java.awt.*;
import static jdk.test.lib.Asserts.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;


public class MultipleDialogs3Test {

    private volatile CustomFrame  frame;
    private List<CustomDialog> dialogList;
    private static int delay = 500;

    private int dialogCount = -1;

    public void createGUI() {

        final int n = 8;
        dialogList = new ArrayList<>();

        frame = new CustomFrame();
        frame.setLocation(50, 50);
        frame.setVisible(true);

        int x = 250;
        int y = 50;
        for (int i = 0; i < n; ++i) {

            CustomDialog dlg;
            if (i == 0) {
                dlg = new CustomDialog(frame);
            } else {
                dlg = new CustomDialog(dialogList.get(i - 1));
            }
            dlg.setLocation(x, y);
            x += 200;
            if (x > 600) {
                x = 50;
                y += 200;
            }

            Dialog.ModalityType type;

            if (i % 4 == 0) {
                type = Dialog.ModalityType.MODELESS;
            } else if (i % 4 == 1) {
                type = Dialog.ModalityType.DOCUMENT_MODAL;
            } else if (i % 4 == 2) {
                type = Dialog.ModalityType.APPLICATION_MODAL;
            } else {
                type = Dialog.ModalityType.TOOLKIT_MODAL;
            }

            dlg.setModalityType(type);
            dialogList.add(dlg);
        }
    }

    public void doTest() throws Exception {

        try {
            EventQueue.invokeAndWait(this::createGUI);

            ExtendedRobot robot = new ExtendedRobot();
            robot.waitForIdle(delay);

            frame.clickOpenButton(robot);
            robot.waitForIdle(delay);

            List<CustomDialog> dialogs = Collections.synchronizedList(dialogList);
            final int n = dialogs.size();

            synchronized(dialogs) {
                for (int i = 0; i < n; ++i) {
                    dialogs.get(i).activated.waitForFlagTriggered();
                    assertTrue(dialogs.get(i).activated.flag(), i + ": Dialog did not " +
                        "trigger windowActivated event when it became visible.");

                    dialogs.get(i).closeGained.waitForFlagTriggered();
                    assertTrue(dialogs.get(i).closeGained.flag(), i + ": Close button " +
                        "did not gain focus when Dialog became visible.");

                    assertTrue(dialogs.get(i).closeButton.hasFocus(), i +
                        ": Close button gained focus but then lost it.");

                    dialogs.get(i).checkUnblockedDialog(robot,
                        i + ": The dialog shouldn't be blocked.");

                    if (i == 0) {
                        assertTrue(dialogs.get(0).getModalityType() ==
                            Dialog.ModalityType.MODELESS, "0: invalid modality type.");

                        frame.checkUnblockedFrame(robot, i + ": A modeless dialog was " +
                            "shown, but the parent frame became blocked.");

                    } else {
                        if (i % 4 == 0) { // modeless dialog
                            assertTrue(dialogs.get(i).getModalityType() ==
                                Dialog.ModalityType.MODELESS, i +
                                ": incorrect dialog modality type.");

                            dialogs.get(i - 1).checkUnblockedDialog(robot, i + ": A modeless " +
                                "dialog was shown, but the parent dialog became blocked.");

                            if (i > 0) {
                                for (int j = 0; j < i - 1; ++j) {

                                    dialogs.get(j).checkBlockedDialog(robot, i + ", " + j +
                                        ": Showing a modeless dialog as a child of a " +
                                        "modal dialog unblocked some dialog in its hierarchy.");
                                }
                            }
                        } else {

                            for (int j = 0; j < i; ++j) {

                                dialogs.get(j).checkBlockedDialog(robot, i + ", " + j +
                                    ": A modal dialog was shown, but some dialog " +
                                    "in its hierarchy became unblocked.");
                            }
                        }

                        frame.checkBlockedFrame(robot, i + ": A modal was dialog shown, " +
                            "but the document root frame became unblocked.");
                    }

                    if (i != n - 1) {
                        dialogs.get(i).clickOpenButton(robot);
                        robot.waitForIdle(delay);
                    }
                }

                for (int i = n - 1; i >= 0; --i) {

                    resetAll();
                    dialogs.get(i).clickCloseButton(robot);
                    robot.waitForIdle(delay);

                    if (i > 0) {

                        dialogs.get(i - 1).activated.waitForFlagTriggered();
                        assertTrue(dialogs.get(i - 1).activated.flag(), i + ": Dialog " +
                            "was not activated when a child dialog was closed.");

                        if (i == 1) {

                            frame.checkUnblockedFrame(robot, "1: Frame having " +
                                "a child modeless dialog was blocked.");

                            dialogs.get(0).checkUnblockedDialog(robot,
                                "0: A modeless dialog at the bottom " +
                                "of the hierarchy was blocked.");

                        } else if ((i - 1) % 4 == 0) { // dialog[i - 1] is modeless

                            for (int j = 0; j < i - 2; ++j) {

                                dialogs.get(j).checkBlockedDialog(robot, i + ", " + j +
                                    ": A dialog in the hierarchy was not blocked. " +
                                    "A dialog blocking a modeless dialog was closed.");
                            }

                            dialogs.get(i - 2).checkUnblockedDialog(robot, i + ": A modal " +
                                "dialog having a child modeless dialog was blocked.");

                            dialogs.get(i - 1).checkUnblockedDialog(robot, i + ": A modeless " +
                                "dialog at the bottom of the hierarchy was blocked.");

                            frame.checkBlockedFrame(robot, i +
                                ": Frame having a child modal dialog was not blocked.");

                        } else {
                            for (int j = 0; j <= i - 2; ++j) {
                                dialogs.get(j).checkBlockedDialog(robot, i + ", " + j +
                                    ": A dialog in the hierarchy was not blocked. " +
                                    "A child dialog was closed.");
                            }

                            dialogs.get(i - 1).checkUnblockedDialog(robot, (i - 1) +
                                ": A dialog was not unblocked when the modal dialog was closed.");

                            frame.checkBlockedFrame(robot, i + ": Frame having " +
                                "a child modal dialog was not blocked. " +
                                "Another child dialog was closed.");
                        }
                    } else {
                        frame.activated.waitForFlagTriggered();
                        assertTrue(frame.activated.flag(), i + ": Frame was not " +
                            "activated when a child dialog was closed.");
                    }
                }
            } // synchronized

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void resetAll() {
        frame.resetStatus();
        Iterator<CustomDialog> it = dialogList.iterator();
        while (it.hasNext()) { it.next().resetStatus(); }
    }

    public void closeAll() {
        if (frame != null) { frame.dispose(); }
        if (dialogList != null) {
            Iterator<CustomDialog> it = dialogList.iterator();
            while (it.hasNext()) { it.next().dispose(); }
        }
    }

    class CustomFrame extends TestFrame {

        @Override
        public void doOpenAction() {
            if ((dialogList != null) && (dialogList.size() > dialogCount)) {
                dialogCount++;
                CustomDialog d = dialogList.get(dialogCount);
                if (d != null) { d.setVisible(true); }
            }
        }
    }

    class CustomDialog extends TestDialog {

        public CustomDialog(Frame frame)   { super(frame); }
        public CustomDialog(Dialog dialog) { super(dialog); }

        @Override
        public void doCloseAction() { this.dispose(); }

        @Override
        public void doOpenAction() {
            if ((dialogList != null) && (dialogList.size() > dialogCount)) {
                dialogCount++;
                CustomDialog d = dialogList.get(dialogCount);
                if (d != null) { d.setVisible(true); }
            }
        }
    }


    public static void main(String[] args) throws Exception {
        (new MultipleDialogs3Test()).doTest();
    }
}
