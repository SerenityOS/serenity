/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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
  @bug        6480534
  @summary    A Frame changing its state from ICONIFIED to NORMAL should regain focus.
  @library    ../../regtesthelpers
  @build      Util
  @run        main DeiconifiedFrameLoosesFocus
*/

import java.awt.Frame;
import java.awt.Robot;
import java.awt.Toolkit;
import test.java.awt.regtesthelpers.Util;

public class DeiconifiedFrameLoosesFocus {
    Robot robot;
    static final Frame frame = new Frame("Frame");

    public static void main(String[] args) {
        DeiconifiedFrameLoosesFocus app = new DeiconifiedFrameLoosesFocus();
        app.init();
        try {
            app.start();
        } finally {
            frame.dispose();
        }
    }

    public void init() {
        robot = Util.createRobot();
    }

    public void start() {
        if (!Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.ICONIFIED) ||
            !Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.NORMAL))
        {
            System.out.println("Frame.ICONIFIED or Frame.NORMAL state is unsupported.");
            return;
        }

        frame.setSize(100, 100);

        frame.setVisible(true);

        Util.waitForIdle(robot);
        robot.delay(1000);

        if (!frame.isFocused()) {
            Util.clickOnTitle(frame, robot);
            Util.waitForIdle(robot);
        }

        if (!frame.isFocused()) {
            throw new Error("Test error: couldn't focus the Frame.");
        }

        test();
        System.out.println("Test passed.");
    }

    void test() {
        frame.setExtendedState(Frame.ICONIFIED);

        Util.waitForIdle(robot);
        robot.delay(500);

        frame.setExtendedState(Frame.NORMAL);

        Util.waitForIdle(robot);
        robot.delay(500);

        if (!frame.isFocused()) {
            throw new TestFailedException("the Frame didn't regain focus after restoring!");
        }
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}
