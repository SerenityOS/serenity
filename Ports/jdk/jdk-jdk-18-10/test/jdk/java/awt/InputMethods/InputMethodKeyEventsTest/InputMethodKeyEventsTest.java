/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8177414
 * @summary KEY_PRESSED and KEY_TYPED events are not generated if a key's held
 * @library ../../regtesthelpers
 * @build Util
 * @run main/manual InputMethodKeyEventsTest
 */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.atomic.AtomicBoolean;

import test.java.awt.regtesthelpers.Util;

public class InputMethodKeyEventsTest {
    private static final AtomicBoolean testCompleted = new AtomicBoolean(false);
    private static volatile boolean testResult = false;

    private static Dialog controlDialog;
    private static Frame testFrame;

    private static final String instructions =
            "Verify that KEY_PRESSED and KEY_TYPED events are generated after a key's " +
            "\npressed and held.\n" +
            "\nThis test is for OS X only. For other platforms please simply press \"Pass\".\n" +
            "\n1. Go to \"System Preferences -> Keyboard -> Input Sources\" and add \"British\" IM." +
            "\n2. Set current IM to \"British\"." +
            "\n3. Set focus to the frame located at north-west corner." +
            "\n4. Pres and hold the \"i\" key for 3 seconds." +
            "\n5. Press and release the \"i\" key again. Use log area to ensure that " +
            "\n   KEY_TYPED and KEY_PRESSED events are still generated." +
            "\nIf KEY_PRESSED, KEY_TYPED and KEY_RELEASED are generated for every key press then " +
            "\npress \"Pass\", otherwise press \"Fail\".";

    public static void main(String[] args) {
        try {
            Robot robot = Util.createRobot();

            createAndShowGUI();
            Util.waitForIdle(robot);

            Util.waitForCondition(testCompleted);
            if (!testResult) {
                throw new RuntimeException("Test FAILED!");
            }
        } finally {
            if (controlDialog != null) {
                controlDialog.dispose();
            }
            if (testFrame != null) {
                testFrame.dispose();
            }
        }
    }

    private static void createAndShowGUI() {
        controlDialog = new Dialog((Frame)null, "InputMethodKeyEventsTest");

        TextArea messageArea = new TextArea(instructions, 15, 80, TextArea.SCROLLBARS_BOTH);
        controlDialog.add("North", messageArea);

        TextArea logArea = new TextArea("Test's logs are displayed here\n", 15, 80, TextArea.SCROLLBARS_BOTH);
        controlDialog.add("Center", logArea);

        Button passedButton = new Button("Pass");
        passedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testResult = true;
                completeTest();
            }
        });

        Button failedButton = new Button("Fail");
        failedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testResult = false;
                completeTest();
            }
        });

        Panel buttonPanel = new Panel();
        buttonPanel.add("West",passedButton);
        buttonPanel.add("East", failedButton);
        controlDialog.add("South", buttonPanel);

        controlDialog.setBounds(250, 0, 500, 500);
        controlDialog.setVisible(true);

        testFrame = new Frame("InputMethodKeyEventsTest");
        testFrame.setSize(200, 200);
        testFrame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                logArea.append("KEY_TYPED keyCode = " + e.getKeyCode() + "\n");
            }

            @Override
            public void keyPressed(KeyEvent e) {
                logArea.append("KEY_PRESSED keyCode = " + e.getKeyCode() + "\n");
            }

            @Override
            public void keyReleased(KeyEvent e) {
                logArea.append("KEY_RELEASED keyCode = " + e.getKeyCode() + "\n");
            }
        });
        testFrame.setVisible(true);
    }

    private static void completeTest() {
        testCompleted.set(true);
        synchronized (testCompleted) {
            testCompleted.notifyAll();
        }
    }
}
