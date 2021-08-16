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

// FDW: Frame -> Dialog -> Window
public class FocusTransferFDWTest {

    class CustomFrame extends TestFrame {

        @Override
        public void doOpenAction() {
            if (dialog != null) {
                dialog.setVisible(true);
            }
        }
    }

    class CustomWindow extends TestWindow {

        public CustomWindow(Dialog d) {
            super(d);
        }

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }

    class CustomDialog extends TestDialog {

        public CustomDialog(Frame f) {
            super(f);
        }

        @Override
        public void doOpenAction() {
            if (window != null) {
                window.setVisible(true);
            }
        }

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }

    private TestDialog dialog;
    private TestFrame  frame;
    private TestWindow window;

    private static final int delay = 1000;

    private final ExtendedRobot robot;

    private final Dialog.ModalityType modalityType;

    FocusTransferFDWTest(Dialog.ModalityType modType) throws Exception {

        modalityType = modType;

        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    private void createGUI() {

        frame = new CustomFrame();
        frame.setLocation(50, 50);
        dialog = new CustomDialog((Frame) null);
        if (modalityType != null) {
            dialog.setModalityType(modalityType);
        }
        dialog.setLocation(250, 50);
        window = new CustomWindow(dialog);
        window.setLocation(450, 50);
        frame.setVisible(true);
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if ( frame != null) {  frame.dispose(); }
        if (window != null) { window.dispose(); }
    }

    public void doTest() throws Exception {

        robot.waitForIdle(delay);

        try {

            frame.checkCloseButtonFocusGained(true);

            frame.clickOpenButton(robot);
            robot.waitForIdle(delay);

            dialog.checkCloseButtonFocusGained(true);

            frame.checkOpenButtonFocusLost(true);

            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            window.checkCloseButtonFocusGained(true);
            dialog.checkOpenButtonFocusLost(true);

            dialog.openGained.reset();
            window.clickCloseButton(robot);

            dialog.checkOpenButtonFocusGained(true);

            frame.openGained.reset();
            dialog.clickCloseButton(robot);

            frame.checkOpenButtonFocusGained(true);

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
