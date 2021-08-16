/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.*;

/**
 * @test
 * @key headful
 * @bug 8065739
 * @summary Moved window is maximazed to new screen
 * @author Alexandr Scherbatiy
 *
 * @run main MaximizedMovedWindow
 */

public class MaximizedMovedWindow {

    public static void main(String[] args) throws Exception {

        //Supported platforms are Windows and OS X.
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("os x")) {
            return;
        }

        if (!Toolkit.getDefaultToolkit().
                isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
            return;
        }

        GraphicsEnvironment ge = GraphicsEnvironment.
                getLocalGraphicsEnvironment();

        if (ge.isHeadlessInstance()) {
            return;
        }

        GraphicsDevice[] devices = ge.getScreenDevices();

        if (devices.length < 2) {
            return;
        }

        Frame frame = null;
        try {

            GraphicsConfiguration gc1 = devices[0].getDefaultConfiguration();
            GraphicsConfiguration gc2 = devices[1].getDefaultConfiguration();

            Robot robot = new Robot();
            robot.setAutoDelay(50);

            frame = new Frame();
            Rectangle maxArea1 = getMaximizedScreenArea(gc1);
            frame.setBounds(getSmallerRectangle(maxArea1));
            frame.setVisible(true);
            robot.waitForIdle();

            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            robot.waitForIdle();
            robot.delay(1000);

            Rectangle bounds = frame.getBounds();
            if (!bounds.equals(maxArea1)) {
                throw new RuntimeException("The bounds of the Frame do not equal"
                        + " to screen 1 size");
            }

            frame.setExtendedState(Frame.NORMAL);
            robot.waitForIdle();
            robot.delay(1000);

            Rectangle maxArea2 = getMaximizedScreenArea(gc2);
            frame.setBounds(getSmallerRectangle(maxArea2));
            robot.waitForIdle();
            robot.delay(1000);

            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            robot.waitForIdle();
            robot.delay(1000);

            bounds = frame.getBounds();
            if (!bounds.equals(maxArea2)) {
                throw new RuntimeException("The bounds of the Frame do not equal"
                        + " to screen 2 size");
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }
    }

    static Rectangle getSmallerRectangle(Rectangle rect) {
        return new Rectangle(
                rect.x + rect.width / 6,
                rect.y + rect.height / 6,
                rect.width / 3,
                rect.height / 3);
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
