/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4654927
 * @summary Clicking on Greyed Menuitems closes the Menubar Dropdown
 * @author Alexander Potochkin
 * @library ../../regtesthelpers
 * @build Util
 * @run main bug4654927
 */

import javax.swing.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.Callable;

public class bug4654927 {

    private static volatile JMenu menu;
    private static volatile JMenuItem menuItem;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            String systemLAF = UIManager.getSystemLookAndFeelClassName();
            // the test is not applicable to Motif L&F
            if(systemLAF.endsWith("MotifLookAndFeel")){
                return;
            }

            UIManager.setLookAndFeel(systemLAF);
            Robot robot = new Robot();
            robot.setAutoDelay(10);

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createAndShowUI();
                }
            });
            robot.waitForIdle();

            // test mouse press
            Point point = Util.getCenterPoint(menu);
            robot.mouseMove(point.x, point.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();

            point = Util.getCenterPoint(menuItem);
            robot.mouseMove(point.x, point.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();

            if (!isMenuItemShowing()) {
                throw new RuntimeException("Popup is unexpectedly closed");
            }

            // test mouse drag
            point = Util.getCenterPoint(menu);
            robot.mouseMove(point.x, point.y);
            Point menuLocation = Util.invokeOnEDT(new Callable<Point>() {

                @Override
                public Point call() throws Exception {
                    return menu.getLocationOnScreen();
                }
            });

            Point itemLocation = Util.invokeOnEDT(new Callable<Point>() {

                @Override
                public Point call() throws Exception {
                    return menuItem.getLocationOnScreen();
                }
            });

            int x0 = menuLocation.x + 10;
            int y0 = menuLocation.y + 10;
            int x1 = itemLocation.x + 10;
            int y1 = itemLocation.y + 10;

            // close menu
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();

            robot.mousePress(InputEvent.BUTTON1_MASK);
            Util.glide(robot, x0, y0, x1, y1);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();

            if (!isMenuItemShowing()) {
                throw new RuntimeException("Popup is unexpectedly closed");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static boolean isMenuItemShowing() throws Exception {
        return Util.invokeOnEDT(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                return menuItem.isShowing();
            }
        });
    }

    private static void createAndShowUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        menu = new JMenu("Menu");
        menu.add(new JMenuItem("menuItem"));
        menuItem = new JMenuItem("menuItem");
        menuItem.setEnabled(false);
        menu.add(menuItem);
        menu.add(new JMenuItem("menuItem"));

        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        frame.setJMenuBar(bar);

        frame.setSize(200, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }
}
