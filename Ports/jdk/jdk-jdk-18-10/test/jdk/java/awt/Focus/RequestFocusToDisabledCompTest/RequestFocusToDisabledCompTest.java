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
  @summary   Tests that it's possible to manually request focus on a disabled component.
  @library   ../../regtesthelpers
  @build     Util
  @run       main RequestFocusToDisabledCompTest
*/

import java.awt.Robot;
import javax.swing.*;
import java.awt.*;
import test.java.awt.regtesthelpers.Util;

public class RequestFocusToDisabledCompTest {
    Robot robot;
    JFrame frame = new JFrame("Frame");
    JButton b0 = new JButton("b0");
    JButton b1 = new JButton("b1");

    public static void main(String[] args) {
        RequestFocusToDisabledCompTest app = new RequestFocusToDisabledCompTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
        frame.add(b0);
        frame.add(b1);
        frame.setLayout(new FlowLayout());
        frame.pack();

        b1.setEnabled(false);
    }

    public void start() {
        Util.showWindowWait(frame);

        if (!b0.hasFocus()) {
            // Request focus on b0.
            if (!Util.focusComponent(b0, 2000)) {
                throw new TestErrorException("couldn't focus " + b0);
            }
        }

        // Try to request focus on b1.
        if (!Util.focusComponent(b1, 2000)) {
            throw new TestFailedException("focus wasn't requested on disabled " + b1);
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
