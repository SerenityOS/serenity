/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6827800
 * @summary Test to check hidden default button does not respond to 'Enter' key
 * @run main HiddenDefaultButtonTest
 */

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class HiddenDefaultButtonTest {

    private static int ButtonClickCount = 0;
    private static JFrame frame;

    private static void createGUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton button = new JButton("Default button");
        button.setDefaultCapable(true);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ButtonClickCount++;
            }
        });

        frame.add(button);
        button.setVisible(false);

        frame.getRootPane().setDefaultButton(button);

        frame.setSize(200, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void disposeTestUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.dispose();
        });
    }

    private static void test() throws Exception {
        // Create Robot
        Robot testRobot = new Robot();

        testRobot.waitForIdle();

        testRobot.keyPress(KeyEvent.VK_ENTER);
        testRobot.delay(20);
        testRobot.keyRelease(KeyEvent.VK_ENTER);
        testRobot.delay(200);
        testRobot.keyPress(KeyEvent.VK_ENTER);
        testRobot.delay(20);
        testRobot.keyRelease(KeyEvent.VK_ENTER);

        testRobot.waitForIdle();

        if (ButtonClickCount != 0) {
            disposeTestUI();
            throw new RuntimeException("DefaultButton is pressed even if it is invisible");
        }

    }

    public static void main(String[] args) throws Exception {
        // create UI
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                HiddenDefaultButtonTest.createGUI();
            }
        });

        // Test default button press by pressing EnterKey using Robot
        test();

        // dispose UI
        HiddenDefaultButtonTest.disposeTestUI();
    }
}

