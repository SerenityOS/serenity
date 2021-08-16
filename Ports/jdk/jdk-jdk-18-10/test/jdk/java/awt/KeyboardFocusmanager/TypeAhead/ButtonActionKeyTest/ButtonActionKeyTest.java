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
  @summary    Action key pressed on a button should be swallowed.
  @library    ../../../regtesthelpers
  @build      Util
  @run        main ButtonActionKeyTest
*/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
import test.java.awt.regtesthelpers.Util;

public class ButtonActionKeyTest {
    Robot robot;
    JFrame frame = new JFrame("Frame");
    JButton button = new JButton("button");
    JTextField text = new JTextField("text");
    AtomicBoolean gotEvent = new AtomicBoolean(false);

    public static void main(String[] args) {
        ButtonActionKeyTest app = new ButtonActionKeyTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
    }

    public void start() {
        frame.setLayout(new FlowLayout());
        frame.add(button);
        frame.add(text);
        frame.pack();

        button.getInputMap().put(KeyStroke.getKeyStroke("A"), "GO!");
        button.getActionMap().put("GO!", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Action performed!");
                text.requestFocusInWindow();
            }
        });

        text.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == 'a') {
                        System.out.println(e.toString());
                        synchronized (gotEvent) {
                            gotEvent.set(true);
                            gotEvent.notifyAll();
                        }
                    }
                }
            });
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        Util.waitForIdle(robot);

        Util.clickOnComp(button, robot);
        Util.waitForIdle(robot);

        if (!button.isFocusOwner()) {
            throw new Error("Test error: a button didn't gain focus.");
        }

        robot.keyPress(KeyEvent.VK_A);
        robot.delay(20);
        robot.keyRelease(KeyEvent.VK_A);

        if (Util.waitForCondition(gotEvent, 2000)) {
            throw new TestFailedException("an action key went into the text field!");
        }

        System.out.println("Test passed.");
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}
