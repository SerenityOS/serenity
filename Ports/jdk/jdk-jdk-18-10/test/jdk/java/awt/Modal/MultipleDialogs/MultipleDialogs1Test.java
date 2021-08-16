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
 * @summary Check whether a set of dialogs created with a toolkit excluded Frame
 *          parent has a proper modal blocking behavior. Also show a document modal
 *          dialog and check if it blocks the document properly.
 *
 * @library ../helpers /lib/client/
 * @library /test/lib
 * @build ExtendedRobot
 * @build Flag
 * @build TestDialog
 * @build TestFrame
 * @run main/timeout=500 MultipleDialogs1Test
 */


import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

public class MultipleDialogs1Test {

    private volatile CustomFrame frame;
    private List<CustomDialog> dialogList;

    private static int delay = 500;
    private int dialogCount = -1;

    public void createGUI() {

        final int n = 8;
        dialogList = new ArrayList<>();

        frame = new CustomFrame();
        frame.setLocation(50, 50);
        frame.setModalExclusionType(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        frame.setVisible(true);

        int x = 250, y = 50;

        CustomDialog dlg;

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

                    frame.checkUnblockedFrame(robot, i + ": Other dialogs are visible.");

                    if (i > 0) {
                        for (int j = 0; j < i; j++) {
                            dialogs.get(j).checkUnblockedDialog(robot, j + ": A toolkit modality " +
                                "excluded frame is a parent of this dialog.");
                        }
                    }
                } // i

                frame.clickOpenButton(robot);
                robot.waitForIdle(delay);

                dialogs.get(n - 1).checkUnblockedDialog(
                    robot, (n - 1) + ": This is " + getType(n - 1) + ".");

                frame.checkBlockedFrame(robot,
                    "A document modal dialog with Frame parent is visible.");

                for (int i = 0; i < n - 1; ++i) {
                    dialogs.get(i).checkUnblockedDialog(robot,
                        i + ": A document modal dialog should not block " +
                        "this dialog. The parent is modality excluded.");
                }

                dialogs.get(n - 1).clickCloseButton(robot);
                robot.waitForIdle(delay);

                for (int i = 0; i < n - 1; ++i) {
                    dialogs.get(i).checkUnblockedDialog(robot, i + ": A document modal " +
                        "dialog which blocked this dialog was closed.");
                }
                frame.checkUnblockedFrame(robot,
                    "A blocking document modal dialog was closed.");
                robot.waitForIdle(delay);
            } // synchronized

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

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

    private void closeAll() {

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

        public CustomDialog(Frame  f) { super(f); }
        public CustomDialog(Dialog d) { super(d); }

        @Override
        public void doCloseAction() { this.dispose(); }
    }


    public static void main(String[] args) throws Exception {
        (new MultipleDialogs1Test()).doTest();
    }
}
