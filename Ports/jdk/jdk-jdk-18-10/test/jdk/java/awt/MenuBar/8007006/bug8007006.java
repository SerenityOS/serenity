/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8007006
 * @requires (os.family == "mac")
 * @summary [macosx] Closing subwindow loses main window menus.
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main bug8007006
 */

import java.awt.*;
import java.awt.event.*;

import jdk.test.lib.Platform;

public class bug8007006 {
    private static Frame frame1;
    private static Frame frame2;
    private static volatile boolean isActionPerformed;

    public static void main(String[] args) throws Exception {
        if (!Platform.isOSX()) {
            System.out.println("This test is for MacOS only. Automatically passed on other platforms.");
            return;
        }

        System.setProperty("apple.laf.useScreenMenuBar", "true");

        Robot robot = new Robot();
        robot.setAutoDelay(300);

        createAndShowGUI();
        robot.waitForIdle();
        frame2.dispose();
        robot.waitForIdle();

        performMenuItemTest(robot);

        frame1.dispose();
        if (!isActionPerformed) {
            throw new Exception("Test failed: menu item action was not performed");
        }
    }

    private static void createAndShowGUI() {
        frame1 = new Frame("Frame 1");
        frame1.setMenuBar(createMenuBar());
        frame1.setSize(200, 200);

        frame2 = new Frame("Frame 2");
        frame2.setMenuBar(createMenuBar());
        frame2.setSize(200, 200);

        frame1.setVisible(true);
        frame2.setVisible(true);
    }

    private static MenuBar createMenuBar() {
        // A very long name makes it more likely that the robot will hit the
        // menu
        Menu menu = new Menu("TestTestTestTestTestTestTestTestTestTest");
        MenuItem item = new MenuItem("TestTestTestTestTestTestTestTestTestTest");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                isActionPerformed = true;
            }
        });
        menu.add(item);
        MenuBar mb = new MenuBar();
        mb.add(menu);
        return mb;
    }

    private static void performMenuItemTest(Robot robot) {
        // Find the menu on the screen menu bar
        // The location depends upon the application name which is the name
        // of the first menu.
        // Unfortunately, the application name can vary based on how the
        // application is run.
        // The work around is to make the menu and the menu item names very
        // long.
        int menuBarX = 250;
        int menuBarY = 11;
        int menuItemX = menuBarX;
        int menuItemY = 34;
        robot.mouseMove(menuBarX, menuBarY);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseMove(menuItemX, menuItemY);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
        waitForAction();
    }

    private static void waitForAction() {
        try {
            for (int i = 0; i < 10; i++) {
                if (isActionPerformed) {
                    return;
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException ex) {
        }
    }
}
