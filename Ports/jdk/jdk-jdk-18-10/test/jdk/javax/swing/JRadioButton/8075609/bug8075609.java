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
 * @library ../../regtesthelpers
 * @build Util
 * @bug 8075609
 * @summary  IllegalArgumentException when transferring focus from JRadioButton using tab
 * @author Vivi An
 * @run main bug8075609
 */

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;

public class bug8075609 {
    private static Robot robot;
    private static JTextField textField;
    private static JFrame mainFrame;

    public static void main(String args[]) throws Throwable {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });

            robot = new Robot();
            Thread.sleep(100);

            robot.setAutoDelay(100);

            // Radio button group tab key test
            runTest1();
        } finally {
            if (mainFrame != null) SwingUtilities.invokeAndWait(() -> mainFrame.dispose());
        }
    }

    private static void createAndShowGUI() {
        mainFrame = new JFrame("Bug 8075609 - 1 test");

        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
        formPanel.setFocusCycleRoot(true);

        JRadioButton option1 = new JRadioButton("Option 1", true);
        JRadioButton option2 = new JRadioButton("Option 2");

        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(option1);
        radioButtonGroup.add(option2);

        formPanel.add(option1);
        formPanel.add(option2);
        textField = new JTextField("Another focusable component");
        formPanel.add(textField);

        rootPanel.add(formPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        rootPanel.add(okButton, BorderLayout.SOUTH);

        mainFrame.add(rootPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);
        mainFrame.toFront();
    }

    // Radio button Group as a single component when traversing through tab key
    private static void runTest1() throws Exception{
        hitKey(robot, KeyEvent.VK_TAB);

        robot.delay(1000 );
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                if (!textField.hasFocus()) {
                    System.out.println("Radio Button Group Go To Next Component through Tab Key failed");
                    throw new RuntimeException("Focus is not on textField as Expected");
                }
            }
        });
    }

    private static void hitKey(Robot robot, int keycode) {
        robot.keyPress(keycode);
        robot.keyRelease(keycode);
        robot.waitForIdle();
    }
}
