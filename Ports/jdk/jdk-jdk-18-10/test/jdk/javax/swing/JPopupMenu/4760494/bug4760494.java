/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @key headful
   @bug 4760494 8159597
   @summary JPopupMenu doessn't accept keyboard input (4212563 not fixed)
*/

import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class bug4760494 {

    static JFrame frame = null;

    public static PassedListener pass;
    public static TestStateListener tester;
    public static Robot robot;
    private static volatile boolean pressed = false;
    private static volatile boolean passed = false;
    private static volatile JPopupMenu popup = null;

    public static void main(String args[]) throws Throwable {
        pass = new PassedListener();
        tester = new TestStateListener();
        robot = new Robot();
        SwingUtilities.invokeAndWait(() -> createUI());
        while (!pressed) {
           try {
                Thread.sleep(1000);
           } catch (InterruptedException e) {
           }
        }
        int count = 0;
        while (!passed && count < 10) {
           try {
                count++;
                Thread.sleep(1000);
           } catch (InterruptedException e) {
           }
       }
       SwingUtilities.invokeAndWait(() -> frame.dispose());
       if (!passed) {
           throw new RuntimeException("Menu item not selected");
       }
    }

    static void createUI() {
        frame = new JFrame("Bug 4760494");
        frame.addWindowListener(tester);

        popup = new JPopupMenu();
        JMenuItem popupItem = popup.add(new JMenuItem("Test item"));
        popupItem.setMnemonic('T');
        popupItem.addActionListener(new PassedListener());

        frame.addMouseListener(new MouseAdapter() {
            public void mouseReleased( MouseEvent e ){
                popup.show(frame,e.getX(),e.getY());
            }
        });

        frame.setSize(200, 200);
        frame.setLocation(200, 200);
        frame.setVisible(true);
        frame.toFront();
    }

    public static class PassedListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {
              passed = true;
              System.out.println("passed!");
        }
    }

    public static class TestStateListener extends WindowAdapter {
        public void windowOpened(WindowEvent ev) {
            try {
                new Thread(new RobotThread()).start();
            } catch (Exception ex) {
                throw new RuntimeException("Thread Exception");
            }
        }
    }

    public static class RobotThread implements Runnable {
        public void run() {
            robot.setAutoDelay(500);
            robot.waitForIdle();
            // Move over the window
            robot.mouseMove(250, 250);
            // display the popup
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle();
            // These delays are so a human has a chance to see it
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            while (!popup.isVisible()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
            // select the item using the keyboard mnemonic
            robot.keyPress(KeyEvent.VK_T);
            robot.keyRelease(KeyEvent.VK_T);
            robot.waitForIdle();
            pressed = true;
        }
     }
}
