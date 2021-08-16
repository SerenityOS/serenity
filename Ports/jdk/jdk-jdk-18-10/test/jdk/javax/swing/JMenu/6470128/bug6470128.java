/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6470128
 * @summary Escape Key causes JMenu Selection to Disappear
 * @author Alexander Potochkin
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main bug6470128
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import jdk.test.lib.Platform;

public class bug6470128 {
    static JFrame frame;
    static JMenu subMenu;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame();
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    JMenuBar bar = new JMenuBar();
                    JMenu menu = new JMenu("Menu");
                    menu.setMnemonic('m');
                    subMenu = new JMenu("SubMenu");
                    JMenuItem item = new JMenuItem("Item");

                    frame.setJMenuBar(bar);
                    bar.add(menu);
                    menu.add(subMenu);
                    subMenu.add(item);

                    frame.setSize(200, 200);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                }
            });
            Robot robot = new Robot();
            robot.setAutoDelay(10);
            robot.waitForIdle();
            if (Platform.isOSX()) {
                robot.keyPress(KeyEvent.VK_CONTROL);
            }
            robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(KeyEvent.VK_M);
            robot.keyRelease(KeyEvent.VK_M);
            robot.keyRelease(KeyEvent.VK_ALT);
            if (Platform.isOSX()) {
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);
            robot.waitForIdle();
            if (!subMenu.isSelected()) {
                throw new RuntimeException("Submenu is unexpectedly unselected");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
