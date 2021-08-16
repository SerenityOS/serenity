/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.WindowConstants;

import jdk.test.lib.Platform;

/**
 * @test
 * @key headful
 * @bug 5028014
 * @summary Focus request & mouse click being performed nearly synchronously
 *          shouldn't break the focus subsystem
 * @author  anton.tarasov@sun.com: area=awt-focus
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main MouseClickRequestFocusRaceTest
 */
public class MouseClickRequestFocusRaceTest {
    static Robot robot;
    static JFrame frame1 = new JFrame("Frame-1") {
            public String toString() { return "Frame-1";}
        };
    static JFrame frame2 = new JFrame("Frame-2") {
            public String toString() { return "Frame-2";}
        };
    static JButton button1 = new JButton("button-1") {
            public String toString() { return "button-1";}
        };
    static JButton button2 = new JButton("button-2") {
            public String toString() { return "button-2";}
        };
    static JPopupMenu popup = new JPopupMenu();

    public static void main(String[] args) {
        try {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
            robot.setAutoDelay(100);
        } catch (AWTException e) {
            throw new RuntimeException("Error: unable to create robot", e);
        }
        frame1.add(button1);
        frame2.add(button2);
        frame1.setBounds(0, 0, 200, 300);
        frame2.setBounds(300, 0, 200, 300);
        frame1.setLayout(new FlowLayout());
        frame2.setLayout(new FlowLayout());

        popup.add(new JMenuItem("black"));
        popup.add(new JMenuItem("yellow"));
        popup.add(new JMenuItem("white"));

        frame1.add(popup);

        frame1.addMouseListener(new MouseAdapter() {
                void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        Point loc = button1.getLocation();
                        popup.show(button1, e.getX() - loc.x, e.getY() - loc.y);
                    }
                }
                public void mousePressed(MouseEvent e) {
                    popup(e);
                }
                public void mouseReleased(MouseEvent e) {
                    popup(e);
                }
            });

        frame2.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    button1.requestFocusInWindow();
                }
            });

        frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        frame1.setVisible(true);
        frame2.setVisible(true);

        robot.delay(1000);
        try {
            test();
        } finally {
            frame1.dispose();
            frame2.dispose();
        }
    }

    public static void test() {
        // Right click Frame-1
        robot.mouseMove(frame1.getLocation().x + 100, frame1.getLocation().y + 200);
        robot.mousePress(InputEvent.BUTTON3_MASK);
        robot.mouseRelease(InputEvent.BUTTON3_MASK);

        robot.delay(1000);

        // Left click Frame-2
        robot.mouseMove(frame2.getLocation().x + 100, frame1.getLocation().y + 200);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.delay(1000);

        JComponent focusOwner = (JComponent)KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JFrame focusedWindow = (JFrame)KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();

        System.out.println("focus owner: " + focusOwner);
        System.out.println("focused window: " + focusedWindow);

        // Verify that the focused window is the ancestor of the focus owner
        if (!focusedWindow.isAncestorOf(focusOwner)) {
            throw new RuntimeException("The focus owner is not in the focused window!");
        }

        if (!Platform.isOSX()) {
            // Try to close native focused window
            robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(KeyEvent.VK_F4);
            robot.keyRelease(KeyEvent.VK_F4);
            robot.keyRelease(KeyEvent.VK_ALT);
            robot.delay(1000);
            // Verify that the Java focused window really mapped the native focused window.
            if (focusedWindow.isVisible()) {
                throw new RuntimeException("The focused window is different on Java and on the native level.");
            }
        } else {
            // Try to move native focus to previous window
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_F4);
            robot.keyRelease(KeyEvent.VK_F4);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.delay(1000);
            // Verify that the Java focused window really mapped the native focused window.
            if (focusedWindow.isFocused()) {
                throw new RuntimeException("The focused window is different on Java and on the native level.");
            }
        }
    }
}
