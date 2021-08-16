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
 * @bug 4983388 8015600
 * @summary shortcuts on menus do not work on JDS
 * @author Oleg Mokhovikov
 * @library ../../../../regtesthelpers
 * @build Util
 * @run main bug4983388
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;
import java.awt.event.KeyEvent;

public class bug4983388 {
    static volatile boolean bMenuSelected = false;

    private static class TestMenuListener implements MenuListener {
        public void menuCanceled(MenuEvent e) {}
        public void menuDeselected(MenuEvent e) {}
        public void menuSelected(MenuEvent e) {
            System.out.println("menuSelected");
            bMenuSelected = true;
        }
    }

    private static void createAndShowGUI() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        menuBar.add(menu);
        JFrame frame = new JFrame();
        frame.setJMenuBar(menuBar);
        frame.pack();
        frame.setVisible(true);
        MenuListener listener = new TestMenuListener();
        menu.addMenuListener(listener);
    }

    public static void main(String[] args) throws Exception {

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException ex) {
            System.err.println("GTKLookAndFeel is not supported on this platform. Using defailt LaF for this platform.");
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

        Robot robot = new Robot();
        robot.setAutoDelay(50);
        robot.waitForIdle();
        Util.hitMnemonics(robot, KeyEvent.VK_F);
        robot.waitForIdle();
        robot.delay(200);

        if (!bMenuSelected) {
            throw new RuntimeException("shortcuts on menus do not work");
        }
    }
}
