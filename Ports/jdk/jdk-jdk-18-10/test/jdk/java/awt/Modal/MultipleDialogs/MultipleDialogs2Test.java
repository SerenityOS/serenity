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
 * @summary Check whether a set of dialogs created with an application excluded Frame
 *          parent has a proper modal blocking behavior. Also show a document modal
 *          dialog and check if it blocks the document properly.
 *
 * @library ../helpers /lib/client/
 * @library /test/lib
 * @build ExtendedRobot
 * @build Flag
 * @build TestDialog
 * @build TestFrame
 * @run main/timeout=500 MultipleDialogs2Test
 */


import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

public class MultipleDialogs2Test {

    private volatile CustomFrame frame;
    private List<CustomDialog> dialogList;
    private static final int delay = 500;

    private int dialogCount = -1;

    private void createGUI() {

        final int n = 8;
        dialogList = new ArrayList<>();

        frame = new CustomFrame();
        frame.setLocation(50, 50);
        frame.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        frame.setVisible(true);

        CustomDialog dlg;

        int x = 250, y = 50;
        for (int i = 0; i < n - 1; ++i) {

            dlg = new CustomDialog(frame);
            dlg.setLocation(x, y);
            x += 200;
            if (x > 600) {
                x = 50;
                y += 200;
            }

            Dialog.ModalityType type;
            if (i % 3 == 0) {
                type = Dialog.ModalityType.MODELESS;
            } else if (i % 3 == 1) {
                type = Dialog.ModalityType.APPLICATION_MODAL;
            } else {
                type = Dialog.ModalityType.TOOLKIT_MODAL;
            }
            dlg.setModalityType(type);
            dialogList.add(dlg);
        }

        dlg = new CustomDialog(frame);
        dlg.setLocation(x, y);
        dlg.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialogList.add(dlg);
    }

    public void doTest() throws Exception {

        try {
            EventQueue.invokeAndWait(this::createGUI);

            ExtendedRobot robot = new ExtendedRobot();
            robot.waitForIdle(delay);

            List<CustomDialog> dialogs = Collections.synchronizedList(dialogList);
            final int n = dialogs.size();

            synchronized(dialogs) {
                for (int i = 0; i < n - 1; ++i) {

                    frame.clickOpenButton(robot);
                    robot.waitForIdle(delay);

                    dialogs.get(i).checkUnblockedDialog(
                        robot, i + ": This is " + getType(i) + ".");

                    if (isToolkitModal(i)) {

                        for (int j = 0; j < i; ++j) {
                            dialogs.get(j).checkBlockedDialog(robot, j + ": This dialog " +
                                "should be blocked by a toolkit modal dialog.");
                        }

                        frame.checkBlockedFrame(robot, i + ": A toolkit modal dialog " +
                            "should block this application modality excluded frame.");

                        break;

                    } else {
                        for (int j = 0; j < i; ++j) {
                            dialogs.get(j).checkUnblockedDialog(robot,
                                j + ": An application modality excluded frame " +
                                "is the parent of this dialog.");
                        }

                        frame.checkUnblockedFrame(robot, i + ": The frame is " +
                            "application modality excluded, but it is blocked.");
                    }
                }

                int tkIndex = dialogCount; // continue testing
                final int tk0 = tkIndex;

                for (int i = tk0 + 1; i < n - 1; ++i) {

                    dialogs.get(tkIndex).clickOpenButton(robot);
                    robot.waitForIdle(delay);

                    frame.checkBlockedFrame(robot, i + ": A toolkit modal dialog " +
                        "should block this application modality excluded frame.");

                    if (isToolkitModal(i)) {
                        dialogs.get(i).checkUnblockedDialog(robot, i + ": This is " +
                            "a toolkit modal dialog with blocked frame parent. " +
                            "Another toolkit modal dialog is visible.");

                        for (int j = 0; j < i; ++j) {
                            dialogs.get(j).checkBlockedDialog(robot, j + ": " +
                                "A toolkit modal dialog should block this child " +
                                "dialog of application modality excluded frame.");
                        }
                        tkIndex = i;
                    } else {
                        dialogs.get(i).checkBlockedDialog(
                            robot, i + ": This is " + getType(i) + " with blocked " +
                            "Frame parent. Also, a toolkit modal dialog is visible.");

                        for (int j = 0; j < i; ++j) {
                            if (j != tkIndex) {
                                dialogs.get(j).checkBlockedDialog(robot, j +
                                    ": A toolkit modal dialog should block this " +
                                    "child dialog of an application modality excluded frame.");
                            }
                        }
                    }
                }

                // show a document modal dialog; the toolkit modal dialog should block it
                dialogs.get(tkIndex).clickOpenButton(robot);
                robot.waitForIdle(delay);

                frame.checkBlockedFrame(robot, "A toolkit modal dialog is visible.");

                for (int i = 0; i < n; ++i) {
                    if (i == tkIndex) {
                        dialogs.get(tkIndex).checkUnblockedDialog(robot,
                            tkIndex + ": This is a toolkit modal dialog. " +
                            "A document modal dialog is visible.");
                    } else {
                        dialogs.get(i).checkBlockedDialog(robot,
                            i + ": A toolkit modal dialog should block this dialog.");
                    }
                }

                dialogs.get(tk0 + 3).clickCloseButton(robot); // close 2nd toolkit dialog
                robot.waitForIdle(delay);

                for (int i = 0; i < n; ++i) {

                    if (i == tk0 + 3) { continue; }
                    if (i == tk0) {
                        dialogs.get(i).checkUnblockedDialog(robot,
                            i + ": This is a toolkit modal dialog. A blocking " +
                            "toolkit modal dialog was opened and then closed.");
                    } else {
                        dialogs.get(i).checkBlockedDialog(robot,
                            i + ": This dialog should be blocked by a toolkit modal dialog. " +
                            "Another blocking toolkit modal dialog was closed.");
                    }
                }

                dialogs.get(tk0).clickCloseButton(robot);
                robot.waitForIdle(delay);

                frame.checkBlockedFrame(
                        robot, "A document modal dialog should block this Frame.");

                for (int i = 0; i < n - 1; ++i) {
                    if (!isToolkitModal(i)) {
                        dialogs.get(i).checkUnblockedDialog(robot, i + ": The parent " +
                            "of the dialog is an app modality excluded Frame.");
                    }
                }

                dialogs.get(n - 1).clickCloseButton(robot);
                robot.waitForIdle(delay);

                frame.checkUnblockedFrame(robot, "A document modal dialog " +
                    "blocking this Frame was closed.");

                for (int i = 0; i < n - 1; ++i) {
                    if (!isToolkitModal(i)) {
                        dialogs.get(i).checkUnblockedDialog(robot, i + ": A document modal " +
                            "dialog blocking the parent frame was closed.");
                    }
                }
            } // synchronized

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private boolean isToolkitModal(int i) { return (i % 3 == 2); }

    private String getType(int i) {

        switch (dialogList.get(i).getModalityType()) {
            case APPLICATION_MODAL:
                return "an application modal dialog";
            case DOCUMENT_MODAL:
                return "a document modal dialog";
            case TOOLKIT_MODAL:
                return "a toolkit modal dialog";
        }
        return "a modeless dialog";
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

        public CustomDialog(Frame frame) { super(frame); }

        @Override
        public void doCloseAction() { this.dispose(); }

        @Override
        public void doOpenAction() {
            if ((dialogList != null) && (dialogList.size() > dialogCount)) {
                dialogCount++;
                if (dialogList.get(dialogCount) != null) {
                    dialogList.get(dialogCount).setVisible(true);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        (new MultipleDialogs2Test()).doTest();
    }
}
