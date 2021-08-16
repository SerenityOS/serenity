/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6249972
 * @summary Tests that JMenuItem(String,int) handles lower-case mnemonics properly.
 * @author Mikhail Lapshin
 * @run main bug6249972
 */

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;

public class bug6249972 implements ActionListener {

    private static JFrame frame;
    private static Robot robot;
    private JMenu menu;
    private volatile boolean testPassed = false;
    private volatile Point p = null;
    private volatile Dimension size = null;

    public static void main(String[] args) throws Exception {
        try {
            robot = new Robot();
            robot.setAutoDelay(100);
            bug6249972 bugTest = new bug6249972();
            robot.waitForIdle();
            robot.delay(1000);
            bugTest.test();
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(() -> frame.dispose());
            }
        }
    }

    public bug6249972() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("bug6249972");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JMenuBar bar = new JMenuBar();
            frame.setJMenuBar(bar);

            menu = new JMenu("Problem");
            bar.add(menu);

            JMenuItem item = new JMenuItem("JMenuItem(String,'z')", 'z');
            item.addActionListener(bug6249972.this);
            menu.add(item);

            frame.setLocationRelativeTo(null);
            frame.pack();
            frame.setVisible(true);
        });
    }


    private void test() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            p = menu.getLocationOnScreen();
            size = menu.getSize();
        });
        p.x += size.width / 2;
        p.y += size.height / 2;
        robot.mouseMove(p.x, p.y);
        robot.waitForIdle();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        robot.waitForIdle();
        robot.delay(100);
        robot.keyPress(KeyEvent.VK_Z);
        robot.keyRelease(KeyEvent.VK_Z);

        robot.waitForIdle();
        robot.delay(1000);

        if (!testPassed) {
            throw new RuntimeException("JMenuItem(String,int) does not handle " +
                    "lower-case mnemonics properly.");
        }

        System.out.println("Test passed");
    }

    public void actionPerformed(ActionEvent e) {
        // We are in the actionPerformed() method -
        // JMenuItem(String,int) handles lower-case mnemonics properly
        testPassed = true;
    }
}
