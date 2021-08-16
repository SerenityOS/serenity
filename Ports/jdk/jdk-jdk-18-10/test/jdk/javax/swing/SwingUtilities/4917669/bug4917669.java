
/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4917669
 * @summary 1.4 REGRESSION: MenuItem accelerator doesn't work if parent menu is in JDialog
 * @author Alexander Zuev
 * @library ../../regtesthelpers
 * @run main bug4917669
 */

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class bug4917669 {

    private static volatile boolean passed = false;
    private static JFrame mainFrame;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(500);

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                createAndShowGUI();
            }
        });

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                createAndShowDialog();
            }
        });

        robot.waitForIdle();

        Util.hitKeys(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_O);
        robot.waitForIdle();

        if (!passed) {
            throw new RuntimeException("Action did not received by menu item.");
        }
    }

    private static void createAndShowDialog() {
        JDialog dialog = new JDialog(mainFrame, "Test Dialog", false);
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem menuItem = new JMenuItem("Open");
        menuItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                passed = true;
            }
        });
        file.add(menuItem);
        mb.add(file);
        dialog.setJMenuBar(mb);

        dialog.setSize(100, 100);
        dialog.setLocation(200, 200);
        dialog.setVisible(true);
    }

    private static void createAndShowGUI() {
        mainFrame = new JFrame("Bug4917669");
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(50, 50);
        mainFrame.setVisible(true);
    }

}
