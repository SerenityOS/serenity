/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8073453
 * @summary Focus doesn't move when pressing Shift + Tab keys
 * @compile AWTFocusTransitionTest.java
 * @run main/othervm AWTFocusTransitionTest
 */

import java.awt.Button;
import java.awt.Component;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.event.KeyEvent;

public class AWTFocusTransitionTest {
    private static Robot robot;

    private static Frame frame;
    private static TextField textField;
    private static Button button;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);

        try {
            createAndShowGUI();

            robot.waitForIdle();

            checkFocusOwner(textField);

            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.waitForIdle();

            checkFocusOwner(button);

            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_SHIFT);
            robot.waitForIdle();

            checkFocusOwner(textField);

            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_SHIFT);
            robot.waitForIdle();

            checkFocusOwner(button);
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }
        System.out.println("Test Passed!");
    }

    private static void createAndShowGUI() {
        frame = new Frame("AWTFocusTransitionTest");
        frame.setSize(300, 300);
        frame.setFocusTraversalPolicyProvider(true);
        frame.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());

        textField = new TextField();
        button = new Button();

        Panel panel = new Panel();
        panel.setFocusTraversalPolicyProvider(true);
        panel.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());

        Panel p = new Panel();
        p.setLayout(new GridLayout(3, 1));
        p.add(textField);
        p.add(button);
        p.add(panel);

        frame.add(p);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void checkFocusOwner(Component component) {
        if (component != frame.getFocusOwner()) {
            throw new RuntimeException("Test Failed! Incorrect focus " +
                    "owner: " + frame.getFocusOwner() +
                    ", but expected: " + component);
        }
    }
}
