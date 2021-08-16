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
  @test
  @key headful
  @bug       6187066
  @summary   Tests the Window.autoRequestFocus property for the Window.toFront() method.
  @library /java/awt/patchlib     ../../regtesthelpers
  @build java.desktop/java.awt.Helper
  @build      Util
  @run       main AutoRequestFocusToFrontTest
*/

import java.awt.*;
import test.java.awt.regtesthelpers.Util;

public class AutoRequestFocusToFrontTest {
    static boolean haveDelays;

    static Frame auxFrame;
    static Frame frame;
    static Button frameButton;
    static Frame frame2;
    static Button frameButton2;
    static Frame frame3;
    static Button frameButton3;
    static Window window;
    static Button winButton;
    static Dialog dialog;
    static Button dlgButton;
    static Window ownedWindow;
    static Button ownWinButton;
    static Dialog ownedDialog;
    static Button ownDlgButton;
    static Dialog modalDialog;
    static Button modalDlgButton;

    static String toolkitClassName;
    static Robot robot = Util.createRobot();

    public static void main(String[] args) {

        if (args.length != 0) {
            haveDelays = "delay".equals(args[0]) ? true : false;
        }

        AutoRequestFocusToFrontTest app = new AutoRequestFocusToFrontTest();
        app.init();
        app.start();
    }

    public void init() {
        toolkitClassName = Toolkit.getDefaultToolkit().getClass().getName();
    }

    static void recreateGUI() {
        if (auxFrame != null) {
            auxFrame.dispose();
            frame.dispose();
            frame2.dispose();
            frame3.dispose();
            window.dispose();
            dialog.dispose();
            ownedWindow.dispose();
            ownedDialog.dispose();
            modalDialog.dispose();
        }

        auxFrame = new Frame("Auxiliary Frame");

        frame = new Frame("Test Frame");
        frameButton = new Button("button");

        frame2 = new Frame("Test Frame 2");
        frameButton2 = new Button("button");

        frame3 = new Frame("Test Frame 3");
        frameButton3 = new Button("button");

        window = new Window(null);
        winButton = new Button("button");
        dialog = new Dialog((Frame)null, "Test Dialog");
        dlgButton = new Button("button");

        ownedWindow = new Window(frame);
        ownWinButton = new Button("button");

        ownedDialog = new Dialog(frame2, "Test Owned Dialog");
        ownDlgButton = new Button("button");

        modalDialog = new Dialog(frame3, "Test Modal Dialog");
        modalDlgButton = new Button("button");

        auxFrame.setBounds(100, 100, 300, 300);

        frame.setBounds(120, 120, 260, 260);
        frame.add(frameButton);

        frame2.setBounds(120, 120, 260, 260);
        frame2.add(frameButton2);

        frame3.setBounds(120, 120, 260, 260);
        frame3.add(frameButton3);

        window.setBounds(120, 120, 260, 260);
        window.add(winButton);

        dialog.setBounds(120, 120, 260, 260);
        dialog.add(dlgButton);

        ownedWindow.setBounds(140, 140, 220, 220);
        ownedWindow.add(ownWinButton);

        ownedDialog.setBounds(140, 140, 220, 220);
        ownedDialog.add(ownDlgButton);

        modalDialog.setBounds(140, 140, 220, 220);
        modalDialog.add(modalDlgButton);
        modalDialog.setModal(true);
    }

    public void start() {
        // 1. Simple Frame.
        //////////////////

        recreateGUI();
        Test.setWindows(frame, null, null);
        Test.test("Test stage 1 in progress", frameButton);


        // 2. Ownerless Window.
        //////////////////////

        recreateGUI();
        Test.setWindows(window, null, null);
        Test.test("Test stage 2 in progress", winButton);


        // 3. Ownerless Dialog.
        //////////////////////

        recreateGUI();
        Test.setWindows(dialog, null, null);
        Test.test("Test stage 3 in progress", dlgButton);


        // 4.1. Owner Frame (with owned Window).
        ///////////////////////////////////////

        recreateGUI();
        Test.setWindows(frame, null, new Window[] {ownedWindow, frame});
        Test.test("Test stage 4.1 in progress", ownWinButton);


        // 4.2. Owned Window (with owner Frame).
        ///////////////////////////////////////

        recreateGUI();
        Test.setWindows(ownedWindow, null, new Window[] {ownedWindow, frame});
        Test.test("Test stage 4.2 in progress", ownWinButton);


        // 5.1. Owner Frame (with owned Dialog).
        ///////////////////////////////////////

        recreateGUI();
        Test.setWindows(frame2, null, new Window[] {ownedDialog, frame2});
        Test.test("Test stage 5.1 in progress", ownDlgButton);


        // 5.2. Owned Dialog (with owner Frame).
        ///////////////////////////////////////

        recreateGUI();
        Test.setWindows(ownedDialog, null, new Window[] {ownedDialog, frame2});
        Test.test("Test stage 5.2 in progress", ownDlgButton);


        ////////////////////////////////////////////////
        // 6.1. Owned modal Dialog (with owner Frame).
        //      Focused frame is excluded from modality.
        ////////////////////////////////////////////////

        if (!"sun.awt.motif.MToolkit".equals(toolkitClassName)) {
            recreateGUI();
            auxFrame.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

            Test.setWindows(modalDialog, modalDialog, new Window[] {modalDialog, frame3});
            Test.test("Test stage 6.1 in progress", modalDlgButton);
        }


        // 6.2. Owner Frame (with owned modal Dialog).
        //      Focused frame is excluded from modality.
        ////////////////////////////////////////////////

        if (!"sun.awt.motif.MToolkit".equals(toolkitClassName)) {
            recreateGUI();
            auxFrame.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

            Test.setWindows(frame3, modalDialog, new Window[] {modalDialog, frame3});
            Test.test("Test stage 6.2 in progress", modalDlgButton, true);
        }

        ///////////////////////////////////////////////////
        // 7. Calling setVisible(true) for the shown Frame.
        ///////////////////////////////////////////////////

        recreateGUI();
        Test.setWindows(frame, null, null);
        Test.setTestSetVisible();
        Test.test("Test stage 7 in progress", frameButton);


        System.out.println("Test passed.");
    }

    static class Test {
        static Window testWindow; // a window to move to front with autoRequestFocus set
        static Window focusWindow; // a window to gain focus
        static Window[] showWindows; // windows to show, or null if only testWindow should be shown

        static boolean testSetVisible;

        static void setWindows(Window _testWindow, Window _focusWindow, Window[] _showWindows) {
            testWindow = _testWindow;
            focusWindow = _focusWindow;
            showWindows = _showWindows;
        }
        static void setTestSetVisible() {
            testSetVisible = true;
        }

        /*
         * @param msg notifies test stage number
         * @param testButton a button of the window (owner or owned) that is to be on the top of stack order
         * @param shouldFocusChange true for modal dialogs
         */
        static void test(String msg, final Button testButton, boolean shouldFocusChange) {
            System.out.println(msg);

            showWindows(testWindow, showWindows, true);

            pause(100);

            /////////////////////////////////////////////////////////
            // Test that calling toFront() doesn't cause focus change
            // when 'autoRequestFocus' is false.
            /////////////////////////////////////////////////////////

            Runnable action = new Runnable() {
                    public void run() {
                        testWindow.setAutoRequestFocus(false);
                        if (testSetVisible) {
                            setVisible(testWindow, true);
                        } else {
                            toFront(testWindow);
                        }
                    }
                };

            if (shouldFocusChange) {
                action.run();
                Util.waitForIdle(robot);
                if (!focusWindow.isFocused()) {
                    throw new TestFailedException("the window must gain focus on moving to front but it didn't!");
                }
            } else if (TestHelper.trackFocusChangeFor(action, robot)) {
                throw new TestFailedException("the window shouldn't gain focus on moving to front but it did!");
            }

            pause(100);

            ///////////////////////////////////////////////////////
            // Test that the window (or its owned window) is on top.
            ///////////////////////////////////////////////////////

            // The latest versions of Metacity (e.g. 2.16) have problems with moving a window to the front.
            if (Util.getWMID() != Util.METACITY_WM) {

                boolean performed = Util.trackActionPerformed(testButton, new Runnable() {
                        public void run() {
                            Util.clickOnComp(testButton, robot);
                        }
                    }, 1000, false);

                if (!performed) {
                    // For the case when the robot failed to trigger ACTION_EVENT.
                    System.out.println("(ACTION_EVENT was not generated. One more attemp.)");
                    performed = Util.trackActionPerformed(testButton, new Runnable() {
                            public void run() {
                                Util.clickOnComp(testButton, robot);
                            }
                        }, 1000, false);
                    if (!performed) {
                        throw new TestFailedException("the window moved to front is not on the top!");
                    }
                }
            }

            showWindows(testWindow, showWindows, false);


            /////////////////////////////////////////////////
            // Test that calling toFront() focuses the window
            // when 'autoRequestFocus' is true.
            /////////////////////////////////////////////////

            // Skip this stage for unfocusable window
            if (!testWindow.isFocusableWindow()) {
                return;
            }

            showWindows(testWindow, showWindows, true);

            pause(100);

            boolean gained = Util.trackWindowGainedFocus(testWindow, new Runnable() {
                    public void run() {
                        testWindow.setAutoRequestFocus(true);
                        if (testSetVisible) {
                            setVisible(testWindow, true);
                        } else {
                            toFront(testWindow);
                        }
                    }
                }, 1000, false);

            // Either the window or its owned window must be focused
            if (!gained && !testButton.hasFocus()) {
                throw new TestFailedException("the window should gain focus automatically but it didn't!");
            }

            showWindows(testWindow, showWindows, false);
        }

        static void test(String msg, Button testButton) {
            test(msg, testButton, false);
        }

        private static void showWindows(Window win, Window[] wins, final boolean visible) {
            pause(100);

            if (wins == null) {
                wins = new Window[] {win}; // operate with 'win'
            }
            for (final Window w: wins) {
                if (visible) {
                    if ((w instanceof Dialog) && ((Dialog)w).isModal()) {
                        TestHelper.invokeLaterAndWait(new Runnable() {
                                public void run() {
                                    w.setVisible(true);
                                }
                            }, robot);
                    } else {
                        setVisible(w, true);
                    }
                } else {
                    w.dispose();
                }
            }
            setVisible(auxFrame, visible);

            if (visible) {
                if (!auxFrame.isFocused()) {
                    Util.clickOnTitle(auxFrame, robot);
                    Util.waitForIdle(robot);
                    if (!auxFrame.isFocused()) {
                        throw new Error("Test error: the frame couldn't be focused.");
                    }
                }
            }
        }
    }

    private static void setVisible(Window w, boolean b) {
        w.setVisible(b);
        try {
            Util.waitForIdle(robot);
        } catch (RuntimeException rte) { // InfiniteLoop
            rte.printStackTrace();
        }
        robot.delay(200);
    }

    private static void toFront(Window w) {
        w.toFront();
        Util.waitForIdle(robot);
        robot.delay(200);
    }

    private static void pause(int msec) {
        if (haveDelays) {
            robot.delay(msec);
        }
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}

