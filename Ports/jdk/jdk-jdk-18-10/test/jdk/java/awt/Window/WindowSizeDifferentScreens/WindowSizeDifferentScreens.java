/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;

/**
 * @test
 * @key headful
 * @bug 8211999
 * @summary The test creates the packed/unpacked top level components on the
 *          different screens and compares their bounds
 * @run main/othervm WindowSizeDifferentScreens
 * @run main/othervm -Dsun.java2d.uiScale=1 WindowSizeDifferentScreens
 * @run main/othervm -Dsun.java2d.uiScale=1.25 WindowSizeDifferentScreens
 */
public final class WindowSizeDifferentScreens {

    public static void main(String[] args) throws Exception {
        test("window");
        test("dialog");
        test("frame");
    }

    private static void test(String top) throws Exception {
        var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Robot robot = new Robot();
        Window main = getTopLevel(top);
        try {
            main.setVisible(true);
            robot.waitForIdle();
            main.setSize(500, 500);
            robot.waitForIdle();
            for (GraphicsDevice gd : ge.getScreenDevices()) {
                Rectangle bounds = gd.getDefaultConfiguration().getBounds();
                Point point = bounds.getLocation();
                point.translate(100, 100);
                main.setLocation(point);
                main.setBackground(Color.RED);
                robot.waitForIdle();

                Window packed = getTopLevel(top);
                Window unpacked = getTopLevel(top);
                try {
                    packed.pack();
                    robot.waitForIdle();
                    packed.setBackground(Color.GREEN);
                    unpacked.setBackground(Color.BLUE);
                    packed.setSize(500, 500);
                    unpacked.setSize(500, 500);
                    packed.setLocation(point);
                    unpacked.setLocation(point);
                    robot.waitForIdle();
                    packed.setVisible(true);
                    unpacked.setVisible(true);
                    robot.waitForIdle();
                    Rectangle mBounds = main.getBounds();
                    Rectangle pBounds = packed.getBounds();
                    Rectangle uBounds = unpacked.getBounds();

                    if (!mBounds.equals(uBounds) ||
                            !mBounds.equals(pBounds)) {
                        System.err.println("Expected bounds: " + mBounds);
                        System.err.println("Actual unpacked: " + uBounds);
                        System.err.println("Actual packed: " + pBounds);
                        throw new RuntimeException();
                    }
                } finally {
                    packed.dispose();
                    unpacked.dispose();
                }
            }
        } finally {
            main.dispose();
        }
    }

    private static Window getTopLevel(String top) {
        return switch (top) {
            case "window" -> new Window(null);
            case "dialog" -> new Dialog((Dialog) null);
            case "frame" -> new Frame();
            default -> throw new IllegalArgumentException("Unexpected: " + top);
        };
    }
}
