/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6981400
 * @summary Tabbing between textfiled do not work properly when ALT+TAB
 * @author  anton.tarasov
 * @library ../../regtesthelpers
 * @build   Util
 * @run     main Test3
 */

// A menu item in a frame should not be auto-selected when switching by Alt+TAB back and forth.

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class Test3 {
    static JFrame f = new JFrame("Frame");
    static JMenuBar bar = new JMenuBar();
    static JMenu menu = new JMenu("File");
    static JMenuItem item = new JMenuItem("Save");

    static JButton b0 = new JButton("b0");
    static JButton b1 = new JButton("b1");

    static Robot robot;

    public static void main(String[] args) {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                System.err.println(e);
            }
        }, KeyEvent.KEY_EVENT_MASK);

        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException("Error: can't create Robot");
        }

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {}

        b0.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent f) {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {}
            }
        });

        menu.add(item);
        bar.add(menu);
        f.setJMenuBar(bar);

        f.add(b0);
        f.add(b1);

        f.setLayout(new FlowLayout());
        f.setSize(400, 100);
        f.setVisible(true);
        Util.waitForIdle(robot);

        if (!b0.hasFocus()) {
            Util.clickOnComp(b0, robot);
            Util.waitForIdle(robot);
            if (!b0.hasFocus()) {
                throw new RuntimeException("Error: can't focus " + b0);
            }
        }

        test();

        System.out.println("Test passed.");
    }

    public static void test() {
        robot.keyPress(KeyEvent.VK_TAB);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.delay(50);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.delay(50);
        robot.keyPress(KeyEvent.VK_TAB);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_ALT);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_TAB);

        robot.delay(500);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.delay(50);
        robot.keyPress(KeyEvent.VK_TAB);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_ALT);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_TAB);

        // Control shot.
        Util.clickOnTitle(f, robot);
        Util.waitForIdle(robot);

        if (menu.isSelected()) {
            throw new RuntimeException("Test failed: the menu gets selected");
        }
        if (!b1.hasFocus()) {
            throw new RuntimeException("Test failed: the button is not a focus owner " + b1);
        }
    }
}
