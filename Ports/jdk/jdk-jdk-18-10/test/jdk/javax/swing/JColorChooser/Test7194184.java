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
 * Portions Copyright (c) 2012 IBM Corporation
 */

/* @test
   @key headful
 * @bug 7194184 8163274
 * @summary Tests JColorChooser Swatch keyboard accessibility.
 * @library ../regtesthelpers
 * @build Util
 * @run main/timeout=500 Test7194184
 */
import java.awt.Component;
import java.awt.Color;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Test7194184 {

    private static JFrame frame;
    private static JColorChooser colorChooser;
    private static Color testColor;
    private static Color newColor;

    private static Robot robot;

    public static void main(String[] args) throws Exception {
        try {
            robot = new Robot();
            robot.setAutoDelay(100);
            createUI();
            robot.waitForIdle();
            robot.delay(1000);
            accessRecentSwatch();
            robot.waitForIdle();
            runRobot();
            testColorChooser();
        } finally {
            cleanUpUI();
        }
    }

    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                String title = getClass().getName();
                frame = new JFrame(title);
                colorChooser = new JColorChooser();
                frame.add(colorChooser);
                frame.pack();
                frame.setVisible(true);
                frame.setLocationRelativeTo(null);
            }
        });
    }

    private static void accessRecentSwatch() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Component recentSwatchPanel = Util.findSubComponent(colorChooser, "RecentSwatchPanel");
                if (recentSwatchPanel == null) {
                    throw new RuntimeException("RecentSwatchPanel not found");
                }
                recentSwatchPanel.requestFocusInWindow();
                testColor = colorChooser.getColor();

            }
        });
    }

    private static void runRobot() {
        // Tab to move the focus to MainSwatch
        Util.hitKeys(robot, KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
        // Select the color on right
        Util.hitKeys(robot, KeyEvent.VK_RIGHT);
        Util.hitKeys(robot, KeyEvent.VK_RIGHT);
        Util.hitKeys(robot, KeyEvent.VK_SPACE);
    }

    private static void testColorChooser() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                newColor = colorChooser.getColor();
                if (newColor == testColor) {
                    throw new RuntimeException("JColorChooser misses keyboard accessibility");
                }
            }
        });
    }

    private static void cleanUpUI() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }
}
