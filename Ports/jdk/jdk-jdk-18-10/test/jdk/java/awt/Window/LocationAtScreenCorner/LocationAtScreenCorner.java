/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;

/**
 * @test
 * @key headful
 * @bug 8201364 8232433 8211999
 * @summary Component.getLocation() should returns correct location if
 *          Component.setBounds() was ignored by the OS
 */
public final class LocationAtScreenCorner {

    public static void main(final String[] args) throws Exception {
        test(true);
        test(false);
    }

    private static void test(final boolean undecorated) throws AWTException {
        Robot robot = new Robot();
        Frame frame = new Frame();
        frame.setUndecorated(undecorated);
        frame.setSize(200, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        robot.waitForIdle();

        GraphicsEnvironment lge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = lge.getScreenDevices();

        // The Component.setBounds() for corners of the screen can be ignored by
        // OS because of menubar, taskbar, dock etc. But in this case
        // getLocation() and getLocationOnScreen() should always return the same
        // coordinates.
        for (GraphicsDevice device : devices) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            test(robot, frame, bounds.x, bounds.y);
            test(robot, frame, bounds.width, bounds.y);
            test(robot, frame, bounds.x + bounds.width, bounds.y);
            test(robot, frame, bounds.x, bounds.height);
            test(robot, frame, bounds.x, bounds.y + bounds.height);
            test(robot, frame, bounds.width, bounds.height);
            test(robot, frame, bounds.x + bounds.width, bounds.y + bounds.height);
        }
        frame.dispose();
    }

    private static void test(Robot robot, Frame frame, int x, int y) {
        for (int i = 0; i < 10; ++i) {
            // intentionally set the same coordinates a few times
            frame.setLocation(x, y); // x and y are cached in the frame
            int attempt = 0;
            while (true) {
                robot.waitForIdle();
                // location was cached in the frame and should be updated to the
                // real location by the native callback some time later.
                // this is why we make a few attempts
                Point location = frame.getLocation();
                // locationOnScreen is fetched from the peer
                Point locationOnScreen = frame.getLocationOnScreen();
                if (location.equals(locationOnScreen)) {
                    break;
                }
                if (attempt++ > 10) {
                    frame.dispose();
                    System.err.println("Location: " + location);
                    System.err.println("Location on screen: " + locationOnScreen);
                    throw new RuntimeException("Wrong location");
                }
            }
        }
    }
}
