/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;

/**
 * @test
 * @key headful
 * @bug 8065739 8131339 8231564
 * @requires (os.family == "windows" | os.family == "mac")
 * @summary When Frame.setExtendedState(Frame.MAXIMIZED_BOTH)
 *          is called for a Frame after been called setMaximizedBounds() with
 *          certain value, Frame bounds must equal to this value.
 */
public class SetMaximizedBounds {

    public static void main(String[] args) throws Exception {
        //Supported platforms are Windows and OS X.
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("windows") && !os.contains("os x")) {
            return;
        }

        if (!Toolkit.getDefaultToolkit().
                isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
            return;
        }

        GraphicsEnvironment ge = GraphicsEnvironment.
                getLocalGraphicsEnvironment();

        for (GraphicsDevice gd : ge.getScreenDevices()) {
            for (GraphicsConfiguration gc : gd.getConfigurations()) {
                testMaximizedBounds(gc, false);
                testMaximizedBounds(gc, true);
            }
        }
    }

    static void testMaximizedBounds(GraphicsConfiguration gc, boolean undecorated)
            throws Exception {

        Frame frame = null;
        try {

            Rectangle maxArea = getMaximizedScreenArea(gc);

            Robot robot = new Robot();
            robot.setAutoDelay(50);

            frame = new Frame();
            frame.setUndecorated(undecorated);
            Rectangle maximizedBounds = new Rectangle(
                    maxArea.x + maxArea.width / 5,
                    maxArea.y + maxArea.height / 5,
                    maxArea.width / 2,
                    maxArea.height / 2);
            frame.setMaximizedBounds(maximizedBounds);
            frame.setSize(maxArea.width / 8, maxArea.height / 8);
            frame.setVisible(true);
            robot.waitForIdle();

            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            robot.waitForIdle();
            robot.delay(1000);

            Rectangle bounds = frame.getBounds();
            if (!bounds.equals(maximizedBounds)) {
                System.err.println("Expected: " + maximizedBounds);
                System.err.println("Actual: " + bounds);
                throw new RuntimeException("The bounds of the Frame do not equal to what"
                        + " is specified when the frame is in Frame.MAXIMIZED_BOTH state");
            }

            frame.setExtendedState(Frame.NORMAL);
            robot.waitForIdle();
            robot.delay(1000);

            maximizedBounds = new Rectangle(
                    maxArea.x + maxArea.width / 6,
                    maxArea.y + maxArea.height / 6,
                    maxArea.width / 3,
                    maxArea.height / 3);
            frame.setMaximizedBounds(maximizedBounds);
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            robot.waitForIdle();
            robot.delay(1000);

            bounds = frame.getBounds();
            if (!bounds.equals(maximizedBounds)) {
                System.err.println("Expected: " + maximizedBounds);
                System.err.println("Actual: " + bounds);
                throw new RuntimeException("The bounds of the Frame do not equal to what"
                        + " is specified when the frame is in Frame.MAXIMIZED_BOTH state");
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }
    }

    static Rectangle getMaximizedScreenArea(GraphicsConfiguration gc) {
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom);
    }
}
