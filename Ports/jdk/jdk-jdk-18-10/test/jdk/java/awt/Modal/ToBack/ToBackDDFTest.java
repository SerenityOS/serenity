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


// DDF: Dialog->Dialog->Frame

public class ToBackDDFTest {

    private volatile TestDialog leftDialog;
    private volatile TestFrame  rightFrame;
    private volatile CustomDialog dialog;

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private Frame hiddenFrame;

    private volatile boolean setModal;

    private Dialog.ModalityType modalityType;

    private ToBackDDFTest(Dialog.ModalityType modType,
                          boolean             modal) throws Exception {
        modalityType = modType;
        setModal = modal;

        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    public ToBackDDFTest(Dialog.ModalityType modalityType) throws Exception {
        this(modalityType, false);
    }

    public ToBackDDFTest(boolean modal) throws Exception { this(null, modal); }

    private void createGUI() {

        hiddenFrame = new Frame();
        leftDialog = new TestDialog(hiddenFrame);
        leftDialog.setLocation(50, 50);
        leftDialog.setBackground(Color.BLUE);
        leftDialog.setVisible(true);

        dialog = new CustomDialog(leftDialog);

        if (modalityType == null) {
            dialog.setModal(setModal);
            modalityType = dialog.getModalityType();
        } else if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }

        dialog.setBackground(Color.WHITE);
        dialog.setLocation(150, 50);

        rightFrame = new TestFrame();
        rightFrame.setLocation(250, 50);
        rightFrame.setBackground(Color.RED);

        if (modalityType == Dialog.ModalityType.APPLICATION_MODAL) {
            rightFrame.setModalExclusionType(
                Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        } else if (modalityType == Dialog.ModalityType.TOOLKIT_MODAL) {
            rightFrame.setModalExclusionType(
                Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        }

        dialog.setVisible(true);
    }

    private void checkLeftDialogIsOverlapped(String msg) {

        Point p = leftDialog.getLocationOnScreen();
        int x = p.x + (int)(leftDialog.getWidth()  * 0.9);
        int y = p.y + (int)(leftDialog.getHeight() * 0.9);
        boolean f = robot.getPixelColor(x, y).equals(leftDialog.getBackground());
        assertFalse(f, msg);
    }

    private void checkRightFrameIsOverlaped(String msg) {

        Point p = rightFrame.getLocationOnScreen();
        int x = p.x + (int)(rightFrame.getWidth()  * 0.1);
        int y = p.y + (int)(rightFrame.getHeight() * 0.9);
        boolean f = robot.getPixelColor(x, y).equals(rightFrame.getBackground());
        assertFalse(f, msg);
    }

    public void doTest() throws Exception {

        try {
            robot.waitForIdle(delay);

            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            dialog.clickCloseButton(robot);
            robot.waitForIdle(delay);

            EventQueue.invokeAndWait(() -> { dialog.toBack(); });
            robot.waitForIdle(delay);

            String type = modalityType.toString().toLowerCase().replace('_', ' ');

            boolean isModeless = (modalityType == Dialog.ModalityType.MODELESS);

            final String msg1;
            if (isModeless) {
                msg1 = "The modeless dialog was overlapped by the " +
                    "parent dialog after calling toBack method.";
            } else {
                msg1 = "The " + type + " dialog was overlapped by the blocked dialog.";
            }
            EventQueue.invokeAndWait(() -> { checkLeftDialogIsOverlapped(msg1); });

            if (isModeless) {
                EventQueue.invokeAndWait(() -> { dialog.toFront(); });
            } else {
                EventQueue.invokeAndWait(() -> { leftDialog.toFront(); });
            }
            robot.waitForIdle(delay);

            final String msg2 = "The dialog is still behind the right frame after " +
                "calling toFront method for " + (isModeless ? "it." : "its parent.");

            EventQueue.invokeAndWait(() -> { checkRightFrameIsOverlaped(msg2); });

            final String msg3;
            if (isModeless) {
                msg3 = "The modeless dialog is still behind the parent dialog.";
            } else {
                msg3 = "The " + type + " dialog was overlapped by the blocked " +
                "dialog after calling toFront method for the blocked dialog.";
            }
            EventQueue.invokeAndWait(() -> { checkLeftDialogIsOverlapped(msg3); });

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog      != null) {      dialog.dispose(); }
        if (leftDialog  != null) {  leftDialog.dispose(); }
        if (rightFrame  != null) {  rightFrame.dispose(); }
        if (hiddenFrame != null) { hiddenFrame.dispose(); }
    }


    class CustomDialog extends TestDialog {

        public CustomDialog(Dialog d) { super(d); }

        @Override
        public void doOpenAction() {
            if (rightFrame != null) { rightFrame.setVisible(true); }
        }
    }
}
