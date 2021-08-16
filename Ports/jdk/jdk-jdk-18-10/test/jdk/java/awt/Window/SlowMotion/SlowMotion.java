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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * @test
 * @key headful
 * @bug 8211999
 * @run main/timeout=300 SlowMotion
 * @summary places the window on the screen outside of any insets, and waits to
 *          catch any strange window moving
 */
public final class SlowMotion {

    // some additional space, if getScreenInsets() does not work, say on Linux
    private static final int SAFE = 100;
    private static final int HEIGHT = 350;
    private static final int WIDTH = 279;
    private static Robot robot;

    public static void main(final String[] args) throws Exception {
        robot = new Robot();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] sds = ge.getScreenDevices();

        for (GraphicsDevice sd : sds) {
            GraphicsConfiguration gc = sd.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            bounds.translate(SAFE, SAFE);
            Point point = new Point(bounds.x, bounds.y);
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            while (point.y < bounds.y + bounds.height - insets.bottom - HEIGHT - SAFE * 2) {
                while (point.x < bounds.x + bounds.width - insets.right - WIDTH - SAFE * 2) {
                    test(point, new Frame());
                    test(point, new Window(null));
                    test(point, new Dialog((Dialog) null));
                    point.translate(bounds.width / 6, 0);
                }
                point.setLocation(bounds.x, point.y + bounds.height / 5);
            }
        }
    }

    private static void test(final Point loc, Window window) {
        try {
            window.setBounds(loc.x, loc.y, WIDTH, HEIGHT);
            window.setVisible(true);
            robot.delay(1000); // intentionally very slow, we try to catch
                               // very very last suspicion event
            Rectangle bounds = window.getBounds();
            if (loc.x != bounds.x || loc.y != bounds.y
                    || bounds.width != WIDTH || bounds.height != HEIGHT) {
                System.err.println("Component = " + window);
                System.err.println("Actual bounds = " + bounds);
                System.err.println("Expected location = " + loc);
                System.err.println("Expected width = " + WIDTH);
                System.err.println("Expected height = " + HEIGHT);
                throw new RuntimeException();
            }
        } finally {
            window.dispose();
        }
    }
}
