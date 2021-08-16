/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug       6496958
  @summary   Tests that breaking the proccess of clearing LW requests doesn't break focus.
  @library    ../../regtesthelpers
  @build      Util
  @run       main ClearLwQueueBreakTest
*/

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClearLwQueueBreakTest {
    JFrame f1 = new JFrame("frame");
    JFrame f2 = new JFrame("frame");
    JButton b = new JButton("button");
    JTextField tf1 = new JTextField("     ");
    JTextField tf2 = new JTextField("     ");
    JTextField tf3 = new JTextField("     ");
    AtomicBoolean typed = new AtomicBoolean(false);
    FocusListener listener1;
    FocusListener listener2;

    Robot robot;

    public static void main(String[] args) {
        ClearLwQueueBreakTest app = new ClearLwQueueBreakTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
    }

    public void start() {
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    f2.setVisible(true);
                }
            });
        tf2.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == '9') {
                        synchronized (typed) {
                            typed.set(true);
                            typed.notifyAll();
                        }
                    }
                }
            });
        tf3.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == '8') {
                        synchronized (typed) {
                            typed.set(true);
                            typed.notifyAll();
                        }
                    }
                }
            });

        listener1 = new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    b.requestFocus();
                    tf1.requestFocus();
                    tf1.setFocusable(false);
                    tf2.requestFocus();
                }
            };

        listener2 = new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    b.requestFocus();
                    tf1.requestFocus();
                    tf2.requestFocus();
                    tf2.setFocusable(false);
                }
            };

        f1.add(b);
        f1.add(tf1);
        f1.add(tf2);
        f1.add(tf3);
        f1.setLayout(new FlowLayout());
        f1.pack();
        f1.setLocationRelativeTo(null);
        f1.setVisible(true);
        Util.waitForIdle(robot);

        /*
         * Break the sequence of LW requests in the middle.
         * Test that the last request succeeds
         */
        f2.addFocusListener(listener1);
        System.out.println("Stage 1.");
        test1();


        /*
         * Break the last LW request.
         * Test that focus is restored correctly.
         */
        f2.removeFocusListener(listener1);
        f2.addFocusListener(listener2);
        System.out.println("Stage 2.");
        test2();

        System.out.println("Test passed.");
    }

    void test1() {
        Util.clickOnComp(b, robot);
        Util.waitForIdle(robot);

        if (!tf2.hasFocus()) {
            throw new TestFailedException("target component didn't get focus!");
        }

        robot.keyPress(KeyEvent.VK_9);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_9);

        synchronized (typed) {
            if (!Util.waitForCondition(typed, 2000)) {
                throw new TestFailedException("key char couldn't be typed!");
            }
        }

        Util.clickOnComp(tf3, robot);
        Util.waitForIdle(robot);

        if (!tf3.hasFocus()) {
            throw new Error("a text field couldn't be focused.");
        }

        typed.set(false);
        robot.keyPress(KeyEvent.VK_8);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_8);

        synchronized (typed) {
            if (!Util.waitForCondition(typed, 2000)) {
                throw new TestFailedException("key char couldn't be typed!");
            }
        }
    }

    void test2() {
        Util.clickOnComp(b, robot);
        Util.waitForIdle(robot);

        if (!b.hasFocus()) {
            throw new TestFailedException("focus wasn't restored correctly!");
        }
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}
