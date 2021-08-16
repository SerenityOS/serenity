/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6538132
 * @summary Regression: Pressing Escape key don't close the menu items from jdk7.0 b07 onwards
 * @author Alexander Potochkin
 * @requires (os.family == "windows")
 * @library /lib/client
 * @build ExtendedRobot
 * @run main bug6538132
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class bug6538132 {
    private static JMenu menu1;
    private static JMenu menu2;
    private static volatile boolean isWinLaf;
    private static JFrame frame;

    private static void createGui() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            isWinLaf = true;
        } catch (Exception e) {
            // If we can't set WinLaf it means we are not under Windows
            // make the test pass
            isWinLaf = false;
            return;
        }
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        menu1 = createMenu();
        menuBar.add(menu1);
        menu2 = createMenu();
        menuBar.add(menu2);
        frame.setJMenuBar(menuBar);

        frame.setSize(200, 200);
        frame.setVisible(true);
    }

    static JMenu createMenu() {
        JMenu menu = new JMenu("Menu");
        menu.add(new JMenuItem("MenuItem"));
        menu.add(new JMenuItem("MenuItem"));
        menu.add(new JMenuItem("MenuItem"));
        return menu;
    }

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    bug6538132.createGui();
                }
            });
            if(isWinLaf) {
                ExtendedRobot robot = new ExtendedRobot();
                robot.setAutoDelay(10);
                robot.waitForIdle();
                Point p1 = menu1.getLocationOnScreen();
                final int x1 = p1.x + menu1.getWidth() / 2;
                final int y1 = p1.y + menu1.getHeight() / 2;
                robot.glide(0, 0, x1, y1);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                assertPopupOpen();
                Point p2 = menu2.getLocationOnScreen();
                final int x2 = p2.x + menu2.getWidth() / 2;
                final int y2 = p2.y + menu2.getHeight() / 2;
                robot.glide(x1, y1, x2, y2);
                assertPopupOpen();
                robot.keyPress(KeyEvent.VK_ESCAPE);
                robot.keyRelease(KeyEvent.VK_ESCAPE);
                assertPopupNotOpen();
                robot.glide(x2, y2, x1, y1);
                assertPopupNotOpen();
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                assertPopupOpen();
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    static void assertPopupOpen() {
        if (getLastPopup() == null) {
            throw new RuntimeException("PopupMenu is not open");
        }
    }

    static void assertPopupNotOpen() {
        if (getLastPopup() != null) {
            throw new RuntimeException("PopupMenu is unexpectedly open");
        }
    }

    // copied from BasicPopupMenuUI
    static JPopupMenu getLastPopup() {
        MenuSelectionManager msm = MenuSelectionManager.defaultManager();
        MenuElement[] p = msm.getSelectedPath();
        JPopupMenu popup = null;

        for (int i = p.length - 1; popup == null && i >= 0; i--) {
            if (p[i] instanceof JPopupMenu)
                popup = (JPopupMenu) p[i];
        }
        return popup;
    }
}
