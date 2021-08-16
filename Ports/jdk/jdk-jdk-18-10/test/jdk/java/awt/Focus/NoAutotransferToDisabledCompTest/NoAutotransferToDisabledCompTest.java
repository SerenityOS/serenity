/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug       4685768
  @summary   Tests that auto-transfering focus doesn't stuck on a disabled component.
  @library   ../../regtesthelpers
  @build     Util
  @run       main NoAutotransferToDisabledCompTest
*/

import java.awt.Robot;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class NoAutotransferToDisabledCompTest {
    Robot robot;
    JFrame frame = new JFrame("Frame");
    JButton b0 = new JButton("b0");
    JButton b1 = new JButton("b1");
    JButton b2 = new JButton("b2");

    public static void main(String[] args) {
        NoAutotransferToDisabledCompTest app = new NoAutotransferToDisabledCompTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
        frame.add(b0);
        frame.add(b1);
        frame.add(b2);
        frame.setLayout(new FlowLayout());
        frame.pack();

        b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                b1.setEnabled(false);
                b2.setEnabled(false);
            }
        });
    }

    public void start() {
        Util.showWindowWait(frame);

        // Request focus on b1.
        if (!Util.focusComponent(b1, 2000)) {
            throw new TestErrorException("couldn't focus " + b1);
        }

        // Activate b1.
        robot.keyPress(KeyEvent.VK_SPACE);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_SPACE);
        Util.waitForIdle(robot);
        robot.delay(2000);

        // Check that focus has been transfered to b0.
        if (!b0.hasFocus()) {
            throw new TestFailedException("focus wasn't auto-transfered properly!");
        }
        System.out.println("Test passed.");
    }
}

/**
 * Thrown when the behavior being verified is found wrong.
 */
class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}

/**
 * Thrown when an error not related to the behavior being verified is encountered.
 */
class TestErrorException extends RuntimeException {
    TestErrorException(String msg) {
        super("Unexpected error: " + msg);
    }
}

