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
  @bug        6426132
  @summary    A Window should be focused upon start (XAWT bug).
  @library    ../../regtesthelpers
  @build      Util
  @run        main WindowInitialFocusTest
*/

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;
import test.java.awt.regtesthelpers.Util;

public class WindowInitialFocusTest {
    Frame frame = new Frame("Test Frame");
    Window window = new Window(frame);
    Button button = new Button("button");
    AtomicBoolean focused = new AtomicBoolean(false);
    Robot robot;

    public static void main(String[] args) {
        WindowInitialFocusTest app = new WindowInitialFocusTest();
        app.start();
    }

    public void start() {
        frame.setBounds(800, 0, 200, 100);
        window.setBounds(800, 200, 200, 100);
        window.setLayout(new FlowLayout());
        window.add(button);

        window.addWindowFocusListener(new WindowAdapter() {
                public void windowGainedFocus(WindowEvent e) {
                    System.out.println(e.toString());
                    synchronized (focused) {
                        focused.set(true);
                        focused.notifyAll();
                    }
                }});

        frame.setVisible(true);
        try {
            robot = new Robot();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }
        robot.waitForIdle();

        // Test 1. Show the window, check that it become focused.

        window.setVisible(true);
        robot.waitForIdle();

        if (!Util.waitForCondition(focused, 2000L)) {
            throw new TestFailedException("the window didn't get focused on its showing!");
        }

        // Test 2. Show unfocusable window, check that it doesn't become focused.

        window.setVisible(false);
        robot.waitForIdle();

        window.setFocusableWindowState(false);
        focused.set(false);

        window.setVisible(true);
        robot.waitForIdle();

        if (Util.waitForCondition(focused, 2000L)) {
            throw new TestFailedException("the unfocusable window got focused on its showing!");
        } else {
            System.out.println("Test passed");
        }
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}
