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
 * @bug 4670486
 * @author Mark Davidson
 * @summary Regression: Popup menu bindings doesn't work when a default button has been defined.
 * @library ../../regtesthelpers
 * @build Util
 * @run main bug4670486
 */
public class bug4670486 {

    public static volatile boolean actionExpected = false;
    public static volatile boolean actionRecieved = false;
    public static JFrame frame;

    private static JMenuBar createMenuBar() {
        JMenuBar menubar = new JMenuBar();

        // Control with unique menu
        JMenu menu = new JMenu("Unique Menu");
        menu.setMnemonic('U');
        menu.add(createMenuItem("Sunday", 'S'));
        menu.add(createMenuItem("Monday", 'M'));

        menu.add(createMenuItem("Tuesday", 'T'));
        menu.add(createMenuItem("Wednesday", 'W'));
        menu.add(createMenuItem("Thursday", 'U'));
        menu.add(createMenuItem("Friday", 'F'));
        menu.add(createMenuItem("Saturday", 'A'));

        menubar.add(menu);

        return menubar;
    }

    private static JPanel createPanel(JFrame frame) {
        JPanel panel = new JPanel();
        JButton button = new JButton("Button");
        JButton button2 = new JButton("Button 2");
        JButton button3 = new JButton("Button 3");

        JRootPane root = frame.getRootPane();
        root.setDefaultButton(button);

        panel.add(button);
        panel.add(button2);
        panel.add(button3);

        return panel;
    }

    /**
     * Creates and returns the menu item.
     */
    private static JMenuItem createMenuItem(String name, char mnemonic) {
        JMenuItem menuItem = new JMenuItem(name, mnemonic);
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                actionRecieved = true;
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

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    frame = new JFrame("Test");
                    frame.setContentPane(createPanel(frame));
                    frame.setJMenuBar(createMenuBar());
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.pack();
                    frame.setVisible(true);
                }
            });

            robot.waitForIdle();

            // Change the default button to
            // force a call to BasicRootPaneUI.updateDefaultButtonBindings()
            Util.hitKeys(robot, KeyEvent.VK_TAB);

            // If the bug exists, then as soon as the menu appears,
            // the VK_ENTER, VK_DOWN, VK_UP and VK_ESC will have no
            // effect.
            Util.hitMnemonics(robot, KeyEvent.VK_U);
            Util.hitKeys(robot, KeyEvent.VK_ENTER);
            robot.waitForIdle();

            checkAction();

            Util.hitMnemonics(robot, KeyEvent.VK_U);
            Util.hitKeys(robot, KeyEvent.VK_DOWN);
            Util.hitKeys(robot, KeyEvent.VK_ENTER);
            robot.waitForIdle();

            checkAction();
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
