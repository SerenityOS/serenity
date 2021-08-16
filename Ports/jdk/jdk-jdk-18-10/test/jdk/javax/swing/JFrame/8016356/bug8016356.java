/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8016356
 * @summary Any swing frame resizes ugly.
 * @author Oleg Pekhovskiy
 * @requires (os.family == "windows")
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main bug8016356
 */

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import jdk.test.lib.Platform;

public class bug8016356 {
    private static JFrame frame;
    private static Color color;
    private static int scrTop;

    private static Point frLoc;
    private static Dimension frSize;

    public static void main(String[] args) throws Exception {

        // Windows only test
        if (Platform.isWindows()) {

            // Retrieving top edge of Desktop
            GraphicsConfiguration grConf = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            Rectangle scrRect = grConf.getBounds();
            Insets scrInsets = Toolkit.getDefaultToolkit().getScreenInsets(grConf);
            scrTop = scrRect.y + scrInsets.top;

            color = new Color(0, 255, 0);

            SwingUtilities.invokeAndWait(() -> {
                createAndShowUI();
            });

            try {
                Robot robot = new Robot();
                robot.setAutoDelay(500);
                robot.setAutoWaitForIdle(true);
                robot.delay(1000);

                // Resizing a window to invoke Windows Snap feature
                readFrameInfo();
                robot.mouseMove(frLoc.x + frSize.width / 2, frLoc.y);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseMove(frLoc.x + frSize.width / 2, scrTop);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);

                // Retrieving the color of window expanded area
                readFrameInfo();
                Insets insets = frame.getInsets();
                Color bgColor = robot.getPixelColor(frLoc.x + frSize.width / 2,
                        frLoc.y + frSize.height - insets.bottom - 1);

                frame.dispose();

                if (!bgColor.equals(color)) {
                    throw new RuntimeException("TEST FAILED: got "
                            + bgColor + " instead of " + color);
                }
                System.out.println("TEST PASSED!");
            } catch (AWTException ex) {
                throw new RuntimeException("TEST FAILED!");
            }
        }
    }

    private static void createAndShowUI() {
        frame = new JFrame();
        frame.setBounds(10, scrTop + 10, 300, 100);
        JPanel panel = new JPanel();
        panel.setBackground(color);
        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void readFrameInfo() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frLoc = frame.getLocationOnScreen();
            frSize = frame.getSize();
        });
    }
}
