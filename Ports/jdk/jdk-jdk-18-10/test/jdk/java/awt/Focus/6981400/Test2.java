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
 * @run     main Test2
 */

// A focus request made after a char is typed ahead shouldn't affect the char's target component.

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class Test2 {
    static Frame f = new Frame("frame");
    static TextArea t0 = new TextArea(1, 10) { public String toString() { return "[TA-0]";} };
    static TextArea t1 = new TextArea(1, 10) { public String toString() { return "[TA-1]";} };
    static TextArea t2 = new TextArea(1, 10) { public String toString() { return "[TA-2]";} };

    static volatile boolean passed = true;

    static Robot robot;

    public static void main(String[] args) {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                System.out.println(e);
                if (e.getID() == KeyEvent.KEY_TYPED) {
                    if (e.getSource() != t1) {
                        passed = false;
                        throw new RuntimeException("Test failed: the key event has wrong source: " + e);
                    }
                }
            }
        }, FocusEvent.FOCUS_EVENT_MASK | KeyEvent.KEY_EVENT_MASK);

        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException("Error: can't create Robot");
        }

        f.add(t0);
        f.add(t1);
        f.add(t2);

        f.setLayout(new FlowLayout());
        f.pack();

        t0.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                try {
                    Thread.sleep(3000);
                } catch (Exception ex) {}
            }
        });

        // The request shouldn't affect the key event delivery.
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (Exception ex) {}
                System.out.println("requesting focus to " + t2);
                t2.requestFocus();
            }
        }).start();


        f.setVisible(true);
        Util.waitForIdle(robot);

        test();

        if (passed) System.out.println("\nTest passed.");
    }

    static void test() {
        Util.clickOnComp(t1, robot);

        // The key event should be eventually delivered to t1.
        robot.delay(50);
        robot.keyPress(KeyEvent.VK_A);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_A);

        Util.waitForIdle(robot);
    }
}

