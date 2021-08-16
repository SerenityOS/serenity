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

/**
 * @test
 * @key headful
 * @summary setAlwaysOnTop doesn't behave correctly in Linux/Solaris under
 *          certain scenarios
 * @bug 8021961
 * @author Semyon Sadetsky
 * @run main ChildAlwaysOnTopTest
 */

import javax.swing.*;
import java.awt.*;

public class ChildAlwaysOnTopTest {

    private static Window win1;
    private static Window win2;
    private static Point point;

    public static void main(String[] args) throws Exception {
        if( Toolkit.getDefaultToolkit().isAlwaysOnTopSupported() ) {


            test(null);

            Window f = new Frame();
            f.setBackground(Color.darkGray);
            f.setSize(500, 500);
            try {
                test(f);
            } finally {
                f.dispose();
            }

            f = new Frame();
            f.setBackground(Color.darkGray);
            f.setSize(500, 500);
            f.setVisible(true);
            f = new Dialog((Frame)f);
            try {
                test(f);
            } finally {
                ((Frame)f.getParent()).dispose();
            }
        }
        System.out.println("ok");
    }

    public static void test(Window parent) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                win1 = parent == null ? new JDialog() : new JDialog(parent);
                win1.setName("top");
                win2 = parent == null ? new JDialog() : new JDialog(parent);
                win2.setName("behind");
                win1.setSize(200, 200);
                Panel panel = new Panel();
                panel.setBackground(Color.GREEN);
                win1.add(panel);
                panel = new Panel();
                panel.setBackground(Color.RED);
                win2.add(panel);
                win1.setAlwaysOnTop(true);
                win2.setAlwaysOnTop(false);
                win1.setVisible(true);
            }
        });

        Robot robot = new Robot();
        robot.delay(500);
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                point = win1.getLocationOnScreen();
                win2.setBounds(win1.getBounds());
                win2.setVisible(true);
            }
        });

        robot.delay(500);
        robot.waitForIdle();

        Color color = robot.getPixelColor(point.x + 100, point.y + 100);
        if(!color.equals(Color.GREEN)) {
            win1.dispose();
            win2.dispose();
            throw new RuntimeException("alawaysOnTop window is sent back by " +
                    "another child window setVisible(). " + color);
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                win2.toFront();
                if (parent != null) {
                    parent.setLocation(win1.getLocation());
                    parent.toFront();
                }
            }
        });

        robot.delay(500);
        robot.waitForIdle();

        color = robot.getPixelColor(point.x + 100, point.y + 100);
        if(!color.equals(Color.GREEN)) {
            win1.dispose();
            win2.dispose();
            throw new RuntimeException("alawaysOnTop window is sent back by " +
                    "another child window toFront(). " + color);
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                win1.setAlwaysOnTop(false);
                if (parent != null) {
                    parent.setVisible(false);
                    parent.setVisible(true);
                }
                win2.toFront();
            }
        });

        robot.delay(500);
        robot.waitForIdle();

        color = robot.getPixelColor(point.x + 100, point.y + 100);
        if(!color.equals(Color.RED)) {
            throw new RuntimeException("Failed to unset alawaysOnTop " + color);
        }

        win1.dispose();
        win2.dispose();
    }
}
