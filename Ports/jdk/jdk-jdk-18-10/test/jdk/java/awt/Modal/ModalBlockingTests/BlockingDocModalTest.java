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


/*
 * @test
 * @key headful
 * @bug 8049617
 * @summary Test if a document modality works as expected:
 *          whether all the windows lying down the document root
 *          (Frame) get blocked.
 *
 * @library ../helpers /lib/client/
 * @library /test/lib
 * @build ExtendedRobot
 * @build Flag
 * @build TestDialog
 * @build TestFrame
 * @build TestWindow
 * @run main BlockingDocModalTest
 */


public class BlockingDocModalTest {

    private static final int delay = 500;
    private final ExtendedRobot robot;

    private TestDialog dialog, childDialog;
    private TestFrame  frame;
    private TestWindow window;


    public BlockingDocModalTest() throws Exception {

        robot = new ExtendedRobot();
        EventQueue.invokeLater(this::createGUI);
    }

    private void createGUI() {

        frame = new CustomFrame();
        frame.setLocation(50, 50);
        frame.setVisible(true);

        dialog = new TestDialog(frame);
        dialog.setLocation(250, 250);
        dialog.setVisible(true);

        childDialog = new CustomDialog(dialog);
        childDialog.setLocation(250, 50);
        childDialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);

        window = new TestWindow(frame);
        window.setLocation(50, 250);
    }

    public void doTest() throws Exception {

        try {

            robot.waitForIdle(delay);

            frame.clickOpenButton(robot);
            robot.waitForIdle(delay);

            childDialog.activated.waitForFlagTriggered();
            assertTrue(childDialog.activated.flag(), "Dialog did not trigger " +
                "Window Activated event when it became visible");

            childDialog.closeGained.waitForFlagTriggered();
            assertTrue(childDialog.closeGained.flag(), "the 1st button did not " +
                "gain focus when the Dialog became visible");

            assertTrue(childDialog.closeButton.hasFocus(), "the 1st dialog button " +
                "gained focus but lost it afterwards");

            frame.checkBlockedFrame(robot, "A document modal Dialog from " +
                "this Frame's child hierarchy should block this frame");

            childDialog.checkUnblockedDialog(robot,
                "This is a document modal childDialog.");

            childDialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            window.checkBlockedWindow(robot,
                "A document modal dialog having a parent belonging " +
                "to this Window's document hierarchy is displayed.");

            dialog.checkBlockedDialog(robot,
                "A document modal child dialog should block this Dialog.");

            robot.waitForIdle(delay);

        } finally {
            EventQueue.invokeAndWait(this::closeAll);
        }
    }

    private void closeAll() {
        if (dialog != null) { dialog.dispose(); }
        if ( frame != null) {  frame.dispose(); }
        if (window != null) { window.dispose(); }
        if (childDialog != null) { childDialog.dispose(); }
    }


    class CustomFrame extends TestFrame {

        @Override
        public void doOpenAction() {
            if (childDialog != null) { childDialog.setVisible(true); }
        }
    }

    class CustomDialog extends TestDialog {

        public CustomDialog(Dialog d) { super(d); }

        @Override
        public void doOpenAction() {
            if (window != null) { window.setVisible(true); }
        }
    }


    public static void main(String[] args) throws Exception {
        (new BlockingDocModalTest()).doTest();
    }
}
