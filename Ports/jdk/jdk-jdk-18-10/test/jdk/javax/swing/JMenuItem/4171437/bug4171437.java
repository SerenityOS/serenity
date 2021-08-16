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
 * @bug 4171437
 * @library ../../regtesthelpers
 * @build Util
 * @author Georges Saab
 * @run main bug4171437
 */

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.*;

public class bug4171437 {
    static volatile boolean closeActivated = false;
    static volatile boolean customActivated = false;
    static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(100);
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });

            robot.waitForIdle();
            robot.delay(1000);

            Util.hitMnemonics(robot, KeyEvent.VK_F);
            Util.hitKeys(robot, KeyEvent.VK_C);

            robot.waitForIdle();

            if (!closeActivated || customActivated) {
                throw new RuntimeException("Didn't pass the muster");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
    public static void createAndShowGUI() {
        JMenuBar menubar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');

        JMenuItem fmi1 = new JMenuItem();
        fmi1 = new JMenuItem("Open");
        JMenuItem fmi2 = new JMenuItem();
        fmi2 = new JMenuItem("Close");
        fmi2.setMnemonic('c');
        fmi2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeActivated = true;
            }
        });

        fileMenu.add( fmi1);
        fileMenu.add( fmi2);

        menubar.add( fileMenu);

        JMenu custom = new JMenu("Custom");
        custom.setMnemonic('c');
        JMenuItem cmi = new JMenuItem();
        cmi = new JMenuItem("Properties");
        cmi.setMnemonic('p');
        custom.add( cmi);
        custom.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                customActivated = true;
            }
            public void menuDeselected(MenuEvent e) {}
            public void menuCanceled(MenuEvent e) {}
        });
        menubar.add( custom);

        frame = new JFrame();
        frame.setJMenuBar( menubar);
        frame.setSize(300, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
