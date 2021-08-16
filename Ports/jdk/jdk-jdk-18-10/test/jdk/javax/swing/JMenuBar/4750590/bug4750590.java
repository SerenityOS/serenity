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
 * @library ../../regtesthelpers
 * @build Util
 * @bug 4750590 8015597
 * @summary SwingSet: Cannot change Themes using menu accelerators
 * @author Alexander Zuev
 * @run main bug4750590
 */

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class bug4750590 {

    public static PassedListener pass = new PassedListener();
    public static volatile boolean passed = false;
    private static JFrame mainFrame;

    public static void main(String args[]) throws Throwable {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI();
                }
            });

            Robot robo = new Robot();
            robo.setAutoDelay(500);
            robo.waitForIdle();

            Util.hitMnemonics(robo, KeyEvent.VK_F);
            robo.keyPress(KeyEvent.VK_M);
            robo.keyRelease(KeyEvent.VK_M);

            robo.waitForIdle();

            if (passed) {
                System.out.println("Test passed!");
            } else {
                throw new RuntimeException("Test FAILED!");
            }
        } finally {
            if (mainFrame != null) SwingUtilities.invokeAndWait(() -> mainFrame.dispose());
        }
    }

    private static void createAndShowGUI() {
        mainFrame = new JFrame("Bug 4750590");
        JMenuBar mbar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        JMenu submenu = new JMenu("Submenu");
        submenu.add(new JMenuItem("SubMenu Item 1")).setMnemonic('S');
        submenu.add(new JMenuItem("SubMenu Item 2"));
        menu.add(submenu);

        menu.add(new JMenuItem("Menu Item 1"));
        JMenuItem menuItem = menu.add(new JMenuItem("Menu Item 2"));
        menuItem.setMnemonic('M');
        menuItem.addActionListener(pass);
        mbar.add(menu);
        mainFrame.setJMenuBar(mbar);

        mainFrame.setSize(200, 200);
        mainFrame.setLocation(200, 200);
        mainFrame.setVisible(true);
        mainFrame.toFront();
    }

    public static class PassedListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {
            passed = true;
        }
    }

}
