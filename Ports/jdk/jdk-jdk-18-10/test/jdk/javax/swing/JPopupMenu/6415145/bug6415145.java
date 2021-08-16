/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6415145
 * @summary REGRESSION: Selected item is not being updated while dragging above popup menu
 * @library /lib/client
 * @build ExtendedRobot
 * @author Mikhail Lapshin
 * @run main bug6415145
 */

import javax.swing.*;
import java.awt.event.*;
import java.awt.AWTException;
import java.awt.Component;

public class bug6415145 {
    private JFrame frame;
    private JButton button;
    private JPopupMenu popupMenu;
    private JMenuItem item1;
    private JMenuItem item2;
    private static ExtendedRobot robot;

    public static void main(String[] args) throws Exception {
        robot = new ExtendedRobot();
        final bug6415145 bugTest = new bug6415145();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    bugTest.init();
                }
            });

            robot.waitForIdle();
            bugTest.test();
        } finally {
            bugTest.stopEDT();
        }
    }

    private void stopEDT() {
        if (frame != null) {
            frame.dispose();
        }
    }

    private void init() {
        popupMenu = new JPopupMenu("test menu");
        item1 = new JMenuItem("item 1");
        item2 = new JMenuItem("item 2");
        popupMenu.add(item1);
        popupMenu.add(item2);

        button = new JButton("test button");
        button.addMouseListener(new MouseListener());

        frame = new JFrame("test frame");
        frame.add(popupMenu);
        frame.add(button);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private class MouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            popupMenu.show(button, e.getX(), e.getY());
        }
    }

    private void test() throws AWTException {
        try {
            moveMouseTo(robot, button);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();

            moveMouseTo(robot, item1);
            robot.waitForIdle();

            moveMouseTo(robot, item2);
            robot.waitForIdle();
            if ( (item1.isArmed()) || (!item2.isArmed()) ) {
                throw new RuntimeException("Selected item is not being updated" +
                        " while dragging above popup menu.");
            }
        } finally {
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        }
    }
    private void moveMouseTo(ExtendedRobot robot, Component c) {
        java.awt.Point p = c.getLocationOnScreen();
        java.awt.Dimension size = c.getSize();
        p.x += size.width / 2;
        p.y += size.height / 2;
        robot.mouseMove(p.x, p.y);
        robot.delay(100);
    }
}
