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


public class FocusTransferDialogsTest {

    class CustomDialog1 extends TestDialog {

        public CustomDialog1(Frame f) {
            super(f);
        }

        @Override
        public void doOpenAction() {
            if (dialog2 != null) {
                dialog2.setVisible(true);
            }
        }

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }

    class CustomDialog2 extends TestDialog {

        public CustomDialog2(Dialog d) {
            super(d);
        }

        @Override
        public void doOpenAction() {
            if (dialog3 != null) {
                dialog3.setVisible(true);
            }
        }

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }

    class CustomDialog3 extends TestDialog {

        public CustomDialog3(Frame f) {
            super(f);
        }

        public CustomDialog3(Dialog d) {
            super(d);
        }

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }


    private TestDialog dialog1, dialog2, dialog3;
    private Frame parentFrame;

    private static final int delay = 1000;
    private final ExtendedRobot robot;
    private Dialog.ModalityType modalityType;

    FocusTransferDialogsTest(Dialog.ModalityType modType) throws Exception {

        modalityType = modType;
        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    private void createGUI() {

        dialog1 = new CustomDialog1((Frame) null);
        dialog1.setTitle("Dialog1");
        dialog1.setLocation(50, 50);

        if (modalityType != null) {
            dialog1.setModalityType(modalityType);
        } else {
            modalityType = Dialog.ModalityType.MODELESS;
        }

        dialog2 = new CustomDialog2(dialog1);
        dialog2.setTitle("Dialog2");
        dialog2.setLocation(250, 50);

        parentFrame = new Frame();
        dialog3 = new CustomDialog3(parentFrame);
        dialog3.setTitle("Dialog3");
        dialog3.setLocation(450, 50);

        dialog1.setVisible(true);
    }

    private void closeAll() {
        if (dialog1 != null) { dialog1.dispose(); }
        if (dialog2 != null) { dialog2.dispose(); }
        if (dialog3 != null) { dialog3.dispose(); }
        if (parentFrame != null) { parentFrame.dispose(); }
    }

    public void doTest() throws Exception {

        robot.waitForIdle(delay);

        try {

            dialog1.checkCloseButtonFocusGained(true);

            dialog1.clickOpenButton(robot);
            robot.waitForIdle(delay);

            dialog2.checkCloseButtonFocusGained(true);
            dialog1.checkOpenButtonFocusLost(true);

            dialog1.openGained.reset();
            dialog2.clickOpenButton(robot);
            robot.waitForIdle(delay);

            switch (modalityType) {
                case APPLICATION_MODAL:

                    dialog3.checkCloseButtonFocusGained(false, 10);
                    dialog2.checkOpenButtonFocusLost(true);

                    dialog1.checkCloseButtonFocusGained(true);
                    dialog3.closeGained.reset();

                    dialog1.clickCloseButton(robot);
                    robot.waitForIdle(delay);

                    dialog3.checkCloseButtonFocusGained(true);

                    break;

                case DOCUMENT_MODAL:
                case MODELESS:

                    dialog3.checkCloseButtonFocusGained(true);
                    dialog2.checkOpenButtonFocusLost(true);

                    dialog1.openGained.reset();

                    dialog2.clickCloseButton(robot);
                    robot.waitForIdle(delay);

                    dialog1.checkOpenButtonFocusGained(true);

                    break;
            }

        } catch (Exception e) {

            // make screenshot before exit
            Rectangle rect = new Rectangle(0, 0, 650, 250);
            java.awt.image.BufferedImage img = robot.createScreenCapture(rect);
            javax.imageio.ImageIO.write(img, "jpg", new java.io.File("NOK.jpg"));

            throw e;
        }

        robot.waitForIdle(delay);
        EventQueue.invokeAndWait(this::closeAll);
    }
}
