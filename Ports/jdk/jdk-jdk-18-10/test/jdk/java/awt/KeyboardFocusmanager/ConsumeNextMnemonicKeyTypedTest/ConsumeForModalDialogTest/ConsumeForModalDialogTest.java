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

/*
  @test
  @key headful
  @bug        6391688
  @summary    Tests that next mnemonic KeyTyped is consumed for a modal dialog.
  @run        main ConsumeForModalDialogTest
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ConsumeForModalDialogTest {
    Robot robot;
    JFrame frame = new JFrame("Test Frame");
    JDialog dialog = new JDialog((Window)null, "Test Dialog", Dialog.ModalityType.DOCUMENT_MODAL);
    JTextField text = new JTextField();
    static boolean passed = true;

    public static void main(String[] args) {
        ConsumeForModalDialogTest app = new ConsumeForModalDialogTest();
        app.init();
        app.start();
    }

    public void init() {
        try {
            robot = new Robot();
            robot.setAutoDelay(50);
        } catch (AWTException e) {
            throw new RuntimeException("Error: unable to create robot", e);
        }
    }

    public void start() {

        text.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    System.out.println(e.toString());
                    passed = false;
                }
            });

        JMenuItem testItem = new JMenuItem();
        testItem.setMnemonic('s');
        testItem.setText("Test");

        testItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    dialog.setVisible(true);
            }
        });

        JMenu menu = new JMenu();
        menu.setMnemonic('f');
        menu.setText("File");
        menu.add(testItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menu);

        dialog.setSize(100, 100);
        dialog.add(text);

        frame.setJMenuBar(menuBar);
        frame.setSize(100, 100);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        robot.waitForIdle();

        if (!frame.isFocusOwner()) {
            Point loc = frame.getLocationOnScreen();
            Dimension size = frame.getSize();
            robot.mouseMove(loc.x + size.width/2, loc.y + size.height/2);
            robot.delay(10);
            robot.mousePress(MouseEvent.BUTTON1_MASK);
            robot.delay(10);
            robot.mouseRelease(MouseEvent.BUTTON1_MASK);

            robot.waitForIdle();

            int iter = 10;
            while (!frame.isFocusOwner() && iter-- > 0) {
                robot.delay(200);
            }
            if (iter <= 0) {
                System.out.println("Test: the frame couldn't be focused!");
                return;
            }
        }

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_F);
        robot.delay(10);
        robot.keyRelease(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.waitForIdle();

        robot.keyPress(KeyEvent.VK_S);
        robot.delay(10);
        robot.keyRelease(KeyEvent.VK_S);

        robot.delay(1000);

        if (passed) {
            System.out.println("Test passed.");
        } else {
            throw new RuntimeException("Test failed! Enexpected KeyTyped came into the JTextField.");
        }
    }
}
