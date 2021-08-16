/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8074883
 * @summary Tab key should move to focused button in a button group
 * @run main ButtonGroupFocusTest
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class ButtonGroupFocusTest {

    private static JRadioButton button1;
    private static JRadioButton button2;
    private static JRadioButton button3;
    private static JRadioButton button4;
    private static JRadioButton button5;
    private static Robot robot;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);

        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame();
            Container contentPane = frame.getContentPane();
            contentPane.setLayout(new FlowLayout());
            button1 = new JRadioButton("Button 1");
            contentPane.add(button1);
            button2 = new JRadioButton("Button 2");
            contentPane.add(button2);
            button3 = new JRadioButton("Button 3");
            contentPane.add(button3);
            button4 = new JRadioButton("Button 4");
            contentPane.add(button4);
            button5 = new JRadioButton("Button 5");
            contentPane.add(button5);
            ButtonGroup group = new ButtonGroup();
            group.add(button1);
            group.add(button2);
            group.add(button3);

            group = new ButtonGroup();
            group.add(button4);
            group.add(button5);

            button2.setSelected(true);

            frame.pack();
            frame.setVisible(true);
        });

        robot.waitForIdle();
        robot.delay(200);

        SwingUtilities.invokeAndWait(() -> {
            if( !button2.hasFocus() ) {
                frame.dispose();
                throw new RuntimeException(
                        "Button 2 should get focus after activation");
            }
        });

        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);

        robot.waitForIdle();
        robot.delay(200);

        SwingUtilities.invokeAndWait(() -> {
            if( !button4.hasFocus() ) {
                frame.dispose();
                throw new RuntimeException(
                        "Button 4 should get focus");
            }
            button3.setSelected(true);
        });

        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);

        robot.waitForIdle();
        robot.delay(200);

        SwingUtilities.invokeAndWait(() -> {
            if( !button3.hasFocus() ) {
                frame.dispose();
                throw new RuntimeException(
                        "selected Button 3 should get focus");
            }
        });

        SwingUtilities.invokeLater(frame::dispose);
    }
}
