/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;

/**
 * @test
 * @key headful
 * @bug 8065739 8129569
 * @requires (os.family == "mac")
 * @summary [macosx] Frame warps to lower left of screen when displayed
 * @author Alexandr Scherbatiy
 * @run main MaximizedToUnmaximized
 */
public class MaximizedToUnmaximized {

    public static void main(String[] args) throws Exception {
        testFrame(false);
        testFrame(true);
    }

    static void testFrame(boolean isUndecorated) throws Exception {
        Frame frame = new Frame();
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(100);

            frame.setUndecorated(isUndecorated);
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            int x = bounds.x + insets.left;
            int y = bounds.y + insets.top;
            int width = bounds.width - insets.left - insets.right;
            int height = bounds.height - insets.top - insets.bottom;
            Rectangle rect = new Rectangle(x, y, width, height);
            frame.pack();
            frame.setBounds(rect);
            frame.setVisible(true);
            robot.waitForIdle();
            robot.delay(500);

            if (frame.getWidth() <= width / 2
                    || frame.getHeight() <= height / 2) {
                throw new RuntimeException("Frame size is small!");
            }

            if (!isUndecorated && frame.getExtendedState() != Frame.MAXIMIZED_BOTH) {
                throw new RuntimeException("Frame state does not equal"
                        + " MAXIMIZED_BOTH!");
            }
        } finally {
            frame.dispose();
        }
    }
}
