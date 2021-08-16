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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * @test
 * @key headful
 * @bug 4515762
 * @author Mark Davidson
 * @summary Tests the ability to support duplicate mnemonics
 * @library ../../regtesthelpers
 * @build Util
 * @run main bug4515762
 */
public class bug4515762 {

    private static volatile boolean actionExpected = false;
    private static volatile boolean actionRecieved = false;
    private static JFrame frame;

    /**
     * @param str name of Menu
     */
    private static JMenuBar createMenuBar() {
        JMenuBar menubar = new JMenuBar();

        // Duplicate menu item test for 4515762
        JMenu menu = new JMenu("Duplicate Menu");
        menu.setMnemonic('D');
        menu.add(createMenuItem("Sunday", 'S'));
        menu.add(createMenuItem("Monday", 'M'));

        menu.add(createMenuItem("Tuesday", 'S'));
        menu.add(createMenuItem("Wednesday", 'S'));
        menu.add(createMenuItem("Thursday", 'S'));
        menu.add(createMenuItem("Friday", 'F'));
        menu.add(createMenuItem("Saturday", 'S'));

        // Control with unique menu
        JMenu menu2 = new JMenu("Unique Menu");
        menu2.setMnemonic('U');
        menu2.add(createMenuItem("Sunday", 'S'));
        menu2.add(createMenuItem("Monday", 'M'));

        menu2.add(createMenuItem("Tuesday", 'T'));
        menu2.add(createMenuItem("Wednesday", 'W'));
        menu2.add(createMenuItem("Thursday", 'U'));
        menu2.add(createMenuItem("Friday", 'F'));
        menu2.add(createMenuItem("Saturday", 'A'));

        menubar.add(menu);
        menubar.add(menu2);

        return menubar;
    }

    /**
     * Creates and returns the menu item.
     */
    private static JMenuItem createMenuItem(String name, char mnemonic) {
        JMenuItem menuItem = new JMenuItem(name, mnemonic);
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                JMenuItem item = (JMenuItem) evt.getSource();
                if (actionExpected == false) {
                    throw new RuntimeException("Menu Action: "
                            + item.getText() + " should not be called");
                } else {
                    actionRecieved = true;
                }
            }
        });

        return menuItem;
    }

    public static void checkAction() {
        if (actionRecieved == true) {
            actionRecieved = false;
        } else {
            throw new RuntimeException("Action has not been received");
        }
    }

    public static void main(String[] args) throws Throwable {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(250);

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    frame = new JFrame("Test");
                    frame.setJMenuBar(createMenuBar());
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.pack();
                    frame.setVisible(true);
                    frame.toFront();
                }
            });

            robot.waitForIdle();

            Util.hitMnemonics(robot, KeyEvent.VK_D);
            robot.waitForIdle();

            // Press the S key many times (should not cause an action peformed)
            int TIMES = 5;
            for (int i = 0; i < TIMES; i++) {
                Util.hitKeys(robot, KeyEvent.VK_S);
            }
            robot.waitForIdle();

            // Unique menu items.
            actionExpected = true;
            Util.hitMnemonics(robot, KeyEvent.VK_U);

            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_S);
            robot.keyRelease(KeyEvent.VK_S);
            robot.waitForIdle();

            checkAction();

            Util.hitMnemonics(robot, KeyEvent.VK_U);
            robot.waitForIdle();

            robot.keyPress(KeyEvent.VK_M);
            robot.keyRelease(KeyEvent.VK_M);
            robot.waitForIdle();

            checkAction();

            Util.hitMnemonics(robot, KeyEvent.VK_U);
            robot.waitForIdle();
            Util.hitKeys(robot, KeyEvent.VK_T);
            robot.waitForIdle();

            checkAction();

            Util.hitMnemonics(robot, KeyEvent.VK_U);
            robot.waitForIdle();
            Util.hitKeys(robot, KeyEvent.VK_W);
            robot.waitForIdle();

            checkAction();

            Util.hitMnemonics(robot, KeyEvent.VK_U);
            robot.waitForIdle();
            Util.hitKeys(robot, KeyEvent.VK_U);
            robot.waitForIdle();

            checkAction();
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
