/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.KeyEvent;

import static jdk.test.lib.Asserts.*;


/*
 * @test
 * @key headful
 * @bug 8047367
 * @summary Check whether a Dialog set with null modality type
 *          behaves like a modeless dialog
 *
 * @library ../helpers /lib/client/
 * @library /test/lib
 * @build ExtendedRobot
 * @build Flag
 * @build TestDialog
 * @build TestFrame
 * @build TestWindow
 * @run main NullModalityDialogTest
 */


public class NullModalityDialogTest {

    class CustomDialog extends TestDialog {
        public CustomDialog(Frame f) {
            super(f);
        }
        @Override
        public void doOpenAction() {
            if (frame != null) {
                frame.setVisible(true);
            }
            if (window != null) {
                window.setVisible(true);
            }
        }
    }

    class CustomFrame extends TestFrame {
        @Override
        public void doOpenAction() {
            if (dialog != null) {
                dialog.setVisible(true);
            }
        }
    }

    private TestFrame  parent;
    private TestDialog dialog;
    private TestFrame  frame;
    private TestWindow window;

    private static final int delay = 1000;

    private final ExtendedRobot robot;

    NullModalityDialogTest() throws Exception {

        robot = new ExtendedRobot();
        robot.setAutoDelay(100);
        EventQueue.invokeAndWait(this::createGUI);
    }

    private void createGUI() {

        parent = new CustomFrame();
        parent.setTitle("Parent");
        parent.setLocation(50, 50);

        dialog = new CustomDialog(parent);
        dialog.setTitle("Dialog");
        dialog.setModalityType((Dialog.ModalityType) null);
        dialog.setLocation(250, 50);

        frame = new TestFrame();
        frame.setTitle("Frame");
        frame.setLocation(50, 250);

        window = new TestWindow(frame);
        window.setLocation(250, 250);

        parent.setVisible(true);
    }

    private void closeAll() {
        if (parent != null) { parent.dispose(); }
        if (dialog != null) { dialog.dispose(); }
        if (frame  != null) {  frame.dispose(); }
        if (window != null) { window.dispose(); }
    }

    public void doTest() throws Exception {

        robot.waitForIdle(delay);

        parent.clickOpenButton(robot);
        robot.waitForIdle(delay);

        dialog.activated.waitForFlagTriggered();
        assertTrue(dialog.activated.flag(), "Dialog did not trigger " +
                "Window Activated event when it became visible");

        dialog.closeGained.waitForFlagTriggered();
        assertTrue(dialog.closeGained.flag(), "the 1st button did not gain focus " +
            "when the Dialog became visible");

        assertTrue(dialog.closeButton.hasFocus(), "the 1st button in the Dialog " +
            "gained focus but lost it afterwards");

        dialog.openGained.reset();

        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.waitForIdle();

        dialog.openGained.waitForFlagTriggered();
        assertTrue(dialog.openGained.flag(),
            "Tab navigation did not happen properly on Dialog. Open button " +
            "did not gain focus on tab press when parent frame is visible");

        dialog.clickOpenButton(robot);
        robot.waitForIdle(delay);

        frame.activated.waitForFlagTriggered();
        assertTrue(frame.activated.flag(), "Frame did not trigger activated when " +
            "made visible. Dialog and its parent frame are visible");

        frame.checkUnblockedFrame(robot, "Frame is the parent of a visible Dialog.");
        window.checkUnblockedWindow(robot, "Frame and its child Dialog are visible.");

        robot.waitForIdle(delay);

        EventQueue.invokeAndWait(this::closeAll);
    }

    public static void main(String[] args) throws Exception {
        NullModalityDialogTest test = new NullModalityDialogTest();
        test.doTest();
    }
}
