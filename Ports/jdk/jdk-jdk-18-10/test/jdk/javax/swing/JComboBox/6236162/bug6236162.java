/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6236162
 * @summary Checks that there is no an inconsistence in combo box
 *          behavior when user points an item in combo popup
 *          by mouse and then uses UP/DOWN keys.
 * @run main bug6236162
 */

import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.*;
import java.awt.event.KeyEvent;

public class bug6236162 {
    private static JFrame frame;
    private static JComboBox combo;
    private static MyComboUI comboUI;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });
            robot.waitForIdle();
            robot.delay(1000);
            test();
            System.out.println("Test passed");
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static void createAndShowGUI() {
        frame = new JFrame("bug6236162");

        combo = new JComboBox(new String[]{"one", "two", "three", "four", "five"});
        combo.setEditable(true);
        comboUI = new MyComboUI();
        combo.setUI(comboUI);
        combo.setSelectedIndex(3);
        frame.getContentPane().add(combo);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.toFront();
    }

    private static void test() throws AWTException {

        // Open popup menu
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);

        // Move mouse to the first popup menu item
        robot.waitForIdle();
        Point p = combo.getLocationOnScreen();
        Dimension size = combo.getSize();
        p.x += size.width / 2;
        p.y += size.height;
        float dy = 1;
        robot.mouseMove(p.x, p.y - 5);
        for (int i=1; i <= 10; i++) {
            robot.mouseMove((int)(p.x), (int)(p.y - 5 + dy*i));
        }

        // Select the second popup menu item
        robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);

        robot.waitForIdle();
        JList list = comboUI.getComboPopup().getList();
        if (list.getSelectedIndex() != 1) {
            throw new RuntimeException("There is an inconsistence in combo box " +
                    "behavior when user points an item in combo popup " +
                    "by mouse and then uses UP/DOWN keys.");
        }
    }


    // Gives access to BasicComboBoxUI.popup field
    private static class MyComboUI extends MetalComboBoxUI {
        public ComboPopup getComboPopup() {
            return popup;
        }
    }
}
