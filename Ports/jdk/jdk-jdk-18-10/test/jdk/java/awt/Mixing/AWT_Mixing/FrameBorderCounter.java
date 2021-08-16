/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FrameBorderCounter {

    private static Frame frame;
    private static Frame background;
    private static Dimension size;
    private static Point location;
    private static Point entered;

    public static void main(String[] args) throws Exception {
        final Robot robot = new Robot();
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                robot.mouseMove(0, 0);
            }
        });
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                background = new Frame();
                background.setBounds(100, 100, 300, 300);
                background.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        entered = e.getLocationOnScreen();
                        System.err.println("[ENTERED] : " + entered);
                    }
                });
                background.setVisible(true);
            }
        });
        robot.waitForIdle();
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                frame = new Frame("Frame");
                frame.setBounds(200, 200, 100, 100);
                frame.setVisible(true);
            }
        });
        Thread.sleep(1000);
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                location = frame.getLocationOnScreen();
                size = frame.getSize();
            }
        });
        int out = 20;
        for (int x = location.x + size.width - out; x <= location.x + size.width + out; ++x) {
            robot.mouseMove(x, location.y + size.height / 2);
            Thread.sleep(50);
        }
        System.err.println("[LOCATION] : " + location);
        System.err.println("[SIZE] : " + size);
        Thread.sleep(250);
        int shift = entered.x - location.x - size.width - 1;
        System.err.println("Done");
        System.out.println(shift);
        frame.dispose();
        background.dispose();
    }
}
