/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @key headful
 * @bug 8011616 8145795
 * @summary JWindow.getLocation and JWindow.getLocationOnScreen return different
 *          values on Unity
 * @author Semyon Sadetsky
 */

import java.awt.*;

public class ScreenLocationTest {


    public static void main(String[] args) throws Exception {
        testLocation();
        testSize();
        System.out.println("ok");
    }

    public static void testLocation() throws Exception {
        Window window = new Window((Frame) null);
        window.setSize(100, 100);
        window.setLocation(0, 0);
        window.setVisible(true);

        Robot robot = new Robot();
        robot.delay(200);
        robot.waitForIdle();

        Point location1 = window.getLocation();
        Point location2 = window.getLocationOnScreen();
        window.setLocation(10000, 10000);

        if (!location1.equals(location2)) {
            window.dispose();
            throw new RuntimeException("getLocation is different");
        }

        robot.delay(200);
        robot.waitForIdle();
        location1 = window.getLocation();
        location2 = window.getLocationOnScreen();

        if (!location1.equals(location2)) {
            window.dispose();
            throw new RuntimeException("getLocation is different");
        }

        window.dispose();
    }

    public static void testSize() throws Exception {
        Window window = new Window((Frame) null);
        window.setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
        window.setVisible(true);

        Robot robot = new Robot();
        robot.delay(200);
        robot.waitForIdle();

        Dimension size = window.getSize();
        if (size.width == Integer.MAX_VALUE ||
                size.height == Integer.MAX_VALUE) {
            window.dispose();
            throw new RuntimeException("size is wrong");
        }

        window.dispose();
    }
}
