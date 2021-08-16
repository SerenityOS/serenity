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
  @bug        6396785
  @summary    MenuItem activated with space should swallow this space.
  @library    ../../../regtesthelpers
  @build      Util
  @run        main MenuItemActivatedTest
*/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
import test.java.awt.regtesthelpers.Util;

public class MenuItemActivatedTest {
    Robot robot;
    JFrame frame = new JFrame("Test Frame");
    JDialog dialog = new JDialog((Window)null, "Test Dialog", Dialog.ModalityType.DOCUMENT_MODAL);
    JTextField text = new JTextField();
    JMenuBar bar = new JMenuBar();
    JMenu menu = new JMenu("Menu");
    JMenuItem item = new JMenuItem("item");
    AtomicBoolean gotEvent = new AtomicBoolean(false);

    public static void main(String[] args) {
        MenuItemActivatedTest app = new MenuItemActivatedTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
    }

    public void start() {
        menu.setMnemonic('f');
        menu.add(item);
        bar.add(menu);
        frame.setJMenuBar(bar);
        frame.pack();
        frame.setLocationRelativeTo(null);

        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    dialog.add(text);
                    dialog.pack();
                dialog.setVisible(true);
                }
            });

        text.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == ' ') {
                        System.out.println(e.toString());
                        synchronized (gotEvent) {
                            gotEvent.set(true);
                            gotEvent.notifyAll();
                        }
                    }
                }
            });

        frame.setVisible(true);
        Util.waitForIdle(robot);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.delay(20);
        robot.keyPress(KeyEvent.VK_F);
        robot.delay(20);
        robot.keyRelease(KeyEvent.VK_F);
        robot.delay(20);
        robot.keyRelease(KeyEvent.VK_ALT);
        Util.waitForIdle(robot);

        item.setSelected(true);
        Util.waitForIdle(robot);

        robot.keyPress(KeyEvent.VK_SPACE);
        robot.delay(20);
        robot.keyRelease(KeyEvent.VK_SPACE);

        if (Util.waitForCondition(gotEvent, 2000)) {
            throw new TestFailedException("a space went into the dialog's text field!");
        }

        System.out.println("Test passed.");
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}
