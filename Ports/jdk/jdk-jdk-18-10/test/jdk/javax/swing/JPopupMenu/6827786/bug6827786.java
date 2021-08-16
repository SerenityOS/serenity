/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6827786
 * @summary Tests duplicate mnemonics
 * @author Peter Zhelezniakov
 * @library ../../regtesthelpers
 * @library /test/lib
 * @modules java.desktop/sun.awt
 * @build jdk.test.lib.Platform
 * @build Util
 * @run main bug6827786
 */

import jdk.test.lib.Platform;
import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;

public class bug6827786 {

    private static JMenu menu;
    private static Component focusable;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(100);
            // move mouse outside menu to prevent auto selection
            robot.mouseMove(100,100);
            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createAndShowGUI();
                }
            });

            robot.waitForIdle();
            robot.delay(1000);

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    focusable.requestFocus();
                }
            });

            robot.waitForIdle();
            checkfocus();

            // select menu
            if (Platform.isOSX()) {
                Util.hitKeys(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_F);
            } else {
                Util.hitKeys(robot, KeyEvent.VK_ALT, KeyEvent.VK_F);
            }
            // select submenu
            Util.hitKeys(robot, KeyEvent.VK_S);
            robot.waitForIdle();
            // verify submenu is selected
            verify(1);

            Util.hitKeys(robot, KeyEvent.VK_S);
            robot.waitForIdle();
            // verify last item is selected
            verify(2);

            Util.hitKeys(robot, KeyEvent.VK_S);
            robot.waitForIdle();
            // selection should wrap to first item
            verify(0);

            System.out.println("PASSED");
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static void checkfocus() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            public void run() {
                if (!focusable.isFocusOwner()) {
                    throw new RuntimeException("Button is not the focus owner.");
                }
            }
        });
    }

    private static void verify(final int index) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                MenuElement[] path =
                        MenuSelectionManager.defaultManager().getSelectedPath();
                MenuElement item = path[3];
                if (item != menu.getMenuComponent(index)) {
                    System.err.println("Selected: " + item);
                    System.err.println("Should be: "
                            + menu.getMenuComponent(index));
                    throw new RuntimeException("Test Failed");
                }
            }
        });
    }

    private static JMenuBar createMenuBar() {
        menu = new JMenu("File");
        menu.setMnemonic('F');

        menu.add(new JMenuItem("Save", 'S'));

        JMenu sub = new JMenu("Submenu");
        sub.setMnemonic('S');
        sub.add(new JMenuItem("Sub Item"));
        menu.add(sub);

        menu.add(new JMenuItem("Special", 'S'));

        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        return bar;
    }

    private static void createAndShowGUI() {
        frame = new JFrame("bug6827786");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setJMenuBar(createMenuBar());
        focusable = new JButton("Set Focus Here");
        frame.add(focusable);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
