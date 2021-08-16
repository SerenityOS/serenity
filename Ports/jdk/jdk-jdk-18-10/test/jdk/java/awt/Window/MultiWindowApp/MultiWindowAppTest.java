/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary After calling frame.toBack() dialog goes to the back on Ubuntu 12.04
 * @key headful
 * @bug 8022334
 * @author Semyon Sadetsky
 * @run main MultiWindowAppTest
 */

import java.awt.*;

public class MultiWindowAppTest {

    public static void main(String[] args) throws Exception {
        Window win1 = new Frame();
        Window win2 = new Dialog((Frame) null);

        int delay = 300;

        win1.setBounds(100, 100, 200, 200);
        win1.setBackground(Color.RED);
        win1.setVisible(true);

        Robot robot = new Robot();
        robot.delay(delay);
        robot.waitForIdle();

        win2.setBounds(win1.getBounds());
        win2.setVisible(true);

        robot.delay(delay);
        robot.waitForIdle();

        win1.toFront();
        robot.delay(delay);
        robot.waitForIdle();

        Point point = win1.getLocationOnScreen();
        Color color = robot.getPixelColor(point.x + 100, point.y + 100);

        if(!color.equals(Color.RED)) {
            win1.dispose();
            win2.dispose();
            throw new RuntimeException("Window was not sent to front.");
        }

        win1.toBack();
        robot.delay(delay);
        robot.waitForIdle();

        color = robot.getPixelColor(point.x + 100, point.y + 100);

        win1.dispose();
        win2.dispose();

        if(color.equals(Color.RED)) {
            throw new RuntimeException("Window was not sent to back.");
        }

        System.out.println("ok");
    }
}
