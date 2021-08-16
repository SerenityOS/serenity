/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * @test
 * @key headful
 * @bug 8175301 8198613
 * @summary Java GUI hangs on Windows when Display set to 125%
 * @run main/othervm -Dsun.java2d.uiScale=2 ScaledFrameBackgroundTest
 * @run main/othervm -Dsun.java2d.uiScale=2 -Dsun.java2d.d3d=true ScaledFrameBackgroundTest
 */
public class ScaledFrameBackgroundTest {

    private static final Color BACKGROUND = Color.RED;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(100);

            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(400, 300);
                JPanel panel = new JPanel();
                panel.setBackground(BACKGROUND);
                frame.getContentPane().add(panel);
                frame.setVisible(true);
                frame.setLocationRelativeTo(null);
            });

            robot.waitForIdle();
            robot.delay(1000);

            Rectangle[] rects = new Rectangle[1];
            SwingUtilities.invokeAndWait(() -> {
                rects[0] = frame.getBounds();
            });

            Rectangle bounds = rects[0];

            int x = bounds.x + bounds.width / 4;
            int y = bounds.y + bounds.height / 4;

            Color color = robot.getPixelColor(x, y);

            if (!BACKGROUND.equals(color)) {
                throw new RuntimeException("Wrong backgound color!");
            }

            x = bounds.x + 3 * bounds.width / 4;
            y = bounds.y + 3 * bounds.height / 4;

            color = robot.getPixelColor(x, y);

            if (!BACKGROUND.equals(color)) {
                throw new RuntimeException("Wrong backgound color!!");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
