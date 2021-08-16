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

// WDF: Window -> Dialog -> Frame
public class FocusTransferWDFTest {

    class CustomDialog extends TestDialog {

        public CustomDialog(Frame f) {
            super(f);
        }

        public CustomDialog(Dialog d) {
            super(d);
        }

        @Override
        public void doOpenAction() {
            if (frame != null) {
                frame.setVisible(true);
            }
        }

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }

    class CustomFrame extends TestFrame {

        @Override
        public void doCloseAction() {
            this.dispose();
        }
    }

    class CustomWindow extends TestWindow {

        public CustomWindow(Frame f) {
            super(f);
        }

        @Override
        public void doOpenAction() {
            if (dialog != null) {
                dialog.setVisible(true);
            }
        }
    }


    private TestDialog dialog;
    private TestFrame  frame;
    private TestWindow window;

    private Frame  parentFrame;

    private static final int delay = 1000;

    private final ExtendedRobot robot;

    private Dialog.ModalityType modalityType;

    public enum DialogParent {FRAME, NULL_DIALOG};
    private DialogParent dialogParent;

    public enum WindowParent {FRAME, NEW_FRAME};
    private WindowParent windowParent;


    FocusTransferWDFTest(Dialog.ModalityType modType,
                         DialogParent        dlgParent,
                         WindowParent        winParent) throws Exception {

        modalityType = modType;
        dialogParent = dlgParent;
        windowParent = winParent;

        robot = new ExtendedRobot();
        EventQueue.invokeLater( this::createGUI );
    }

    private void createGUI() {

        frame = new CustomFrame();
        frame.setLocation(50, 50);

        switch (dialogParent) {
            case FRAME:
                dialog = new CustomDialog(frame);
                break;
            case NULL_DIALOG:
                dialog = new CustomDialog((Dialog) null);
                break;
        }
        assertTrue(dialog != null, "error: null dialog");

        if (modalityType == null) {
            modalityType = Dialog.ModalityType.MODELESS;
        } else {
            dialog.setModalityType(modalityType);
        }

        dialog.setLocation(250, 50);

        switch (windowParent) {
            case FRAME:
                window = new CustomWindow(frame);
                break;
            case NEW_FRAME:
                parentFrame = new Frame();
                window = new CustomWindow(parentFrame);
                break;
        }
        assertTrue(window != null, "error: null window");

        window.setLocation(450, 50);
        window.setVisible(true);
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if ( frame != null) {  frame.dispose(); }
        if (window != null) { window.dispose(); }

        if (parentFrame  != null) {  parentFrame.dispose(); }
    }

    private void ModalTest() throws Exception {
        frame.checkCloseButtonFocusGained(false, 10);
        dialog.checkOpenButtonFocusLost(false, 10);

        dialog.clickCloseButton(robot);
        robot.waitForIdle(delay);

        frame.checkCloseButtonFocusGained(true);

        window.openGained.reset();

        frame.clickCloseButton(robot);
        robot.waitForIdle(delay);
    }

    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            window.checkCloseButtonFocusGained(false, 10);

            window.clickOpenButton(robot);
            robot.waitForIdle(delay);

            dialog.checkCloseButtonFocusGained(true);
            window.checkOpenButtonFocusLost(false, 10);

            dialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            switch (modalityType) {
                case APPLICATION_MODAL:
                    ModalTest();
                    if (windowParent == WindowParent.FRAME) {
                        assertFalse(window.isVisible(),
                            "window shouldn't be visible");
                    } else { // WindowParent.NEW_FRAME
                        window.checkOpenButtonFocusGained(false, 10);
                    }

                    break;

                case DOCUMENT_MODAL:
                    if (dialogParent == DialogParent.FRAME) {
                        ModalTest();
                        if (windowParent == WindowParent.FRAME) { // 10
                            assertFalse(window.isVisible(),
                                "window shouldn't be visible");
                        } else { // WindowParent.NEW_FRAME
                            window.checkOpenButtonFocusGained(false, 10);
                        }
                    } else { // DialogParent.NULL_DIALOG
                        frame.checkCloseButtonFocusGained(true);
                        dialog.checkOpenButtonFocusLost(true);

                        dialog.openGained.reset();

                        frame.clickCloseButton(robot);
                        robot.waitForIdle(delay);

                        dialog.checkOpenButtonFocusGained(true);

                        window.openGained.reset();

                        dialog.clickCloseButton(robot);
                        robot.waitForIdle(delay);

                        window.checkOpenButtonFocusGained(false, 10);
                    }
                    break;

                case MODELESS:

                    frame.checkCloseButtonFocusGained(true);
                    dialog.checkOpenButtonFocusLost(true);

                    dialog.openGained.reset();

                    frame.clickCloseButton(robot);
                    robot.waitForIdle(delay);

                    if (dialogParent == DialogParent.NULL_DIALOG) {
                        dialog.checkOpenButtonFocusGained(true);

                        window.openGained.reset();

                        dialog.clickCloseButton(robot);
                        robot.waitForIdle(delay);

                        window.checkOpenButtonFocusGained(false, 10);
                    } else {
                        assertFalse(dialog.isVisible(),
                            "dialog shouldn't be visible");

                        if (windowParent == WindowParent.FRAME) {
                            assertFalse(window.isVisible(),
                                "window shouldn't be visible");
                        }
                    }

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
