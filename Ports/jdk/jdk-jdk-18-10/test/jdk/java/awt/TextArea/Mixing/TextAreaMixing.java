/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8013189
 * @run main TextAreaMixing
 * @summary TextArea should support HW/LW mixing
 * @author anthony.petrov@oracle.com
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class TextAreaMixing {

    private static volatile boolean menuClicked = false;
    private static JMenuItem menuItem;

    public static void main(String[] args) throws Exception {
        // The bug is only reproducible on X11, but there's no reason
        // for this test to not pass on any platofrm

        final JFrame frame = new JFrame("JFrame");
        frame.setLayout(new GridLayout(0, 1));
        frame.setSize(200, 200);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Test Menu");

        for (int i = 0; i < 6; i++) {
            JMenuItem mi = new JMenuItem(Integer.toString(i));
            mi.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    menuClicked = true;
                }
            });
            menu.add(mi);

            // Choose a random (OK, the fourth) menu item to click on
            if (i == 3) {
                menuItem = mi;
            }
        }
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        frame.getContentPane().add(new TextArea());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        Thread.sleep(2000);

        Robot robot = new Robot();

        // Open the menu
        Point loc = menu.getLocationOnScreen();
        robot.mouseMove(loc.x + menu.getWidth() / 2, loc.y + menu.getHeight() / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        Thread.sleep(500);

        // Click an item
        loc = menuItem.getLocationOnScreen();
        robot.mouseMove(loc.x + menuItem.getWidth() / 2, loc.y + menuItem.getHeight() / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        Thread.sleep(500);

        frame.dispose();

        if (!menuClicked) {
            throw new RuntimeException("A menu item has never been clicked.");
        }
    }
}
