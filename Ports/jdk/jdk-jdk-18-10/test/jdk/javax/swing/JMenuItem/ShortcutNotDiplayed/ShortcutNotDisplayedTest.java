/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7186371
 * @summary [macosx] Main menu shortcuts not displayed
 * @author vera.akulova@oracle.com
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main/manual ShortcutNotDisplayedTest
 */

import jdk.test.lib.Platform;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ShortcutNotDisplayedTest {
    static volatile boolean done = false;
    static volatile boolean pass = false;
    static final String PASS_COMMAND = "pass";

    public static void main(String[] args) throws Exception {
        if (!Platform.isOSX()) {
            System.out.println("This test is for MacOS only. Automatically passed on other platforms.");
            return;
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

        do { try { Thread.sleep(300); } catch (Exception e) {} } while (!done) ;
        if (!pass) {
            throw new Exception("Shortcuts not displayed as expected in the screen menu bar.");
        }
    }

    private static void createAndShowGUI() {
        JMenuItem newItem = new JMenuItem("Exit");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, java.awt.event.InputEvent.META_MASK));

        JMenu menu = new JMenu("Test Frame Window Menu");
        menu.setMnemonic(KeyEvent.VK_M);
        menu.add(newItem);

        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        JTextArea text = new JTextArea(
            "  Please follow instructions:\n" +
            "  1. You should see \"Test Frame Window Menu\" menu on the screen menu bar.\n" +
            "  2. Open \"Test Frame Window Menu\" menu. \n" +
            "     Check that menu item \"Exit\" has a shortcut with image for Command Key and symbol \"E\". \n" +
            "     If you see the shortcut press \"Passed\". Otherwise press \"Failed\".\n"
        );
        text.setEditable(false);

        JScrollPane sp = new JScrollPane(text);
        sp.setSize(300,200);

        JButton passBtn = new JButton("Pass");
        passBtn.setActionCommand(PASS_COMMAND);
        JButton failBtn = new JButton("Fail");
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(PASS_COMMAND)) {
                    pass = true;
                }
                done = true;
            }
        };

        JFrame testFrame = new JFrame("Test Frame Window");
        testFrame.setLayout(new FlowLayout());
        testFrame.setBounds(100, 100, 600, 180);
        testFrame.setJMenuBar(bar);
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        passBtn.addActionListener(listener);
        failBtn.addActionListener(listener);
        testFrame.getContentPane().add(sp);
        testFrame.getContentPane().add(passBtn);
        testFrame.getContentPane().add(failBtn);
        testFrame.setVisible(true);
    }
}
