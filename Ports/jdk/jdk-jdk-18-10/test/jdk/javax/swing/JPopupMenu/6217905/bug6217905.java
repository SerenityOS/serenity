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
 * @bug 6217905
 * @summary JPopupMenu keyboard navigation stops working
 * @author Alexander Potochkin
 * @requires (os.family == "windows")
 * @library /lib/client
 * @build ExtendedRobot
 * @run main bug6217905
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class bug6217905 {
    private static JPanel popupPanel;
    private static JMenuItem firstItem;
    private static JMenuItem lastItem;
    private static JFrame frame;

    private static void createGui() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPopupMenu popup = new JPopupMenu("Menu");
        firstItem = new JMenuItem("MenuItem");
        popup.add(firstItem);
        popup.add(new JMenuItem("MenuItem"));
        lastItem = new JMenuItem("MenuItem");
        popup.add(lastItem);

        popupPanel = new JPanel();
        popupPanel.setComponentPopupMenu(popup);
        frame.add(popupPanel);
        frame.setSize(100, 100);
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            // This test is for WinLaf only
            System.out.println("This test is for Windows LaF only.");
            return;
        }

        try {
            ExtendedRobot robot = new ExtendedRobot();
            robot.setAutoDelay(10);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    bug6217905.createGui();
                }
            });
            robot.waitForIdle();
            Point loc = popupPanel.getLocationOnScreen();
            int x = loc.x + popupPanel.getWidth()/2;
            int y = loc.y + popupPanel.getHeight()/2;
            robot.glide(0, 0, x, y);
            robot.mousePress(InputEvent.BUTTON3_MASK);
            robot.mouseRelease(InputEvent.BUTTON3_MASK);
            robot.waitForIdle();
            if (getSelectedPathLength() != 1) {
                throw new RuntimeException("Only popup must be selected");
            }
            robot.glide(x, y, 0, 0);
            robot.type(KeyEvent.VK_DOWN);
            robot.waitForIdle();
            if (getSelectedPathLength() != 2 || !firstItem.isArmed()) {
                throw new RuntimeException("First item must be selected");
            }
            robot.type(KeyEvent.VK_ESCAPE);
            robot.waitForIdle();
            if (getSelectedPathLength() != 0) {
                throw new RuntimeException("There must be no selected items");
            }
            robot.glide(0, 0, x, y);
            robot.mousePress(InputEvent.BUTTON3_MASK);
            robot.mouseRelease(InputEvent.BUTTON3_MASK);
            robot.waitForIdle();
            robot.glide(x, y, 0, 0);
            robot.type(KeyEvent.VK_UP);
            robot.waitForIdle();
            if (getSelectedPathLength() != 2 || !lastItem.isArmed()) {
                throw new RuntimeException("Last item must be selected");
            }
        } finally {
             if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static int getSelectedPathLength() {
        return MenuSelectionManager.defaultManager().getSelectedPath().length;
    }
}
