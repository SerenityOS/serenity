/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/*
 * @test
 * @key headful
 * @bug  8049069
 * @summary Tests whether right mouse click releases a pressed JButton
 */

public class PressedButtonRightClickTest {

    private static Robot testRobot;
    private static JFrame myFrame;
    private static JButton myButton;

    public static void main(String[] args) throws Throwable {

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                constructTestUI();
            }
        });

        try {
            testRobot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException("Exception in Robot creation");
        }

        testRobot.waitForIdle();

        // Method performing auto test operation
        test();

        disposeTestUI();
    }

    private static void test() {
        Point loc = myFrame.getLocationOnScreen();

        testRobot.mouseMove((loc.x + 100), (loc.y + 100));

        // Press the left mouse button
        testRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        myButton.setText("Left button pressed");
        testRobot.delay(1000);

        // Press the right mouse button
        testRobot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        myButton.setText("Left button pressed + Right button pressed");
        testRobot.delay(1000);

        // Release the right mouse button
        testRobot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        myButton.setText("Right button released");
        testRobot.delay(1000);

        // Test whether the button is still pressed
        boolean pressed = myButton.getModel().isPressed();
        testRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        if (!pressed) {
            disposeTestUI();
            throw new RuntimeException("Test Failed!");
        }
    }

    private static void disposeTestUI() {
        myFrame.setVisible(false);
        myFrame.dispose();
    }

    public static void constructTestUI() {
        myFrame = new JFrame();
        myFrame.setLayout(new BorderLayout());
        myButton = new JButton("Whatever");
        myFrame.add(myButton, BorderLayout.CENTER);
        myFrame.setSize(400, 300);
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myFrame.setLocationRelativeTo(null);
        myFrame.setVisible(true);
    }
}

