/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;

/**
 * @test
 * @key headful
 * @bug 8196030
 * @summary checks that Robot and MouseInfo use the same coordinates
 */
public final class MouseLocationOnScreen {

    public static void main(final String[] args) throws AWTException {
        GraphicsEnvironment lge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (final GraphicsDevice device : lge.getScreenDevices()) {
            GraphicsConfiguration dc = device.getDefaultConfiguration();
            Robot robot = new Robot(device);

            Rectangle bounds = dc.getBounds();
            int x1 = bounds.x;
            int x2 = x1 + bounds.width - 1;
            int y1 = bounds.y;
            int y2 = y1 + bounds.height - 1;

            // We'll check all edge (two pixels in a width) of the each screen
            edge(robot, device, x1, y1, x2, y1);         // top
            edge(robot, device, x1, y1 + 1, x2, y1 + 1); // top

            edge(robot, device, x2, y1, x2, y2);         // right
            edge(robot, device, x2 - 1, y1, x2 - 1, y2); // right

            edge(robot, device, x1, y1, x1, y2);         // left
            edge(robot, device, x1 + 1, y1, x1 + 1, y2); // left

            edge(robot, device, x1, y2, x2, y2);         // bottom
            edge(robot, device, x1, y2 - 1, x2, y2 - 1); // bottom

            // We'll check crossing of diagonals of each screen
            cross(robot, device, x1, y1, x2, y2); // cross left-bottom
            cross(robot, device, x1, y2, x2, y1); // cross left-top
        }
    }

    /**
     * This method checks the coordinates which were passed to robot and
     * returned by MouseInfo. Note that this method will be called for each
     * pixel and for performance reasons we try will try to skip waitForIdle()
     * a few times.
     */
    static void validate(Robot robot, GraphicsDevice device, int x, int y) {
        int attempt = 0;
        while (true) {
            attempt++;
            Point actLoc = MouseInfo.getPointerInfo().getLocation();
            GraphicsDevice actDevice = MouseInfo.getPointerInfo().getDevice();

            if (actLoc.x != x || actLoc.y != y || actDevice != device) {
                if (attempt <= 10) {
                    if (attempt >= 8) {
                        robot.waitForIdle();
                    }
                    continue;
                }
                System.err.println("Expected device: " + device);
                System.err.println("Actual device: " + actDevice);
                System.err.println("Expected X: " + x);
                System.err.println("Actual X: " + actLoc.x);
                System.err.println("Expected Y: " + y);
                System.err.println("Actual Y: " + actLoc.y);
                throw new RuntimeException();
            }
            return;
        }
    }

    private static void edge(Robot robot, GraphicsDevice device,
                             int x1, int y1, int x2, int y2) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                robot.mouseMove(x, y);
                validate(robot, device, x, y);
            }
        }
    }

    private static void cross(Robot robot, GraphicsDevice device,
                              int x0, int y0, int x1, int y1) {
        float dmax = (float) Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        float dx = (x1 - x0) / dmax;
        float dy = (y1 - y0) / dmax;

        robot.mouseMove(x0, y0);
        validate(robot, device, x0, y0);
        for (int i = 1; i <= dmax; i++) {
            int x = (int) (x0 + dx * i);
            int y = (int) (y0 + dy * i);
            robot.mouseMove(x, y);
            validate(robot, device, x, y);
        }
    }
}
