/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug       6542975
  @summary   Tests that switching focus from an owned window doesn't crash.
  @author    anton.tarasov@sun.com: area=awt-focus
  @library    ../../regtesthelpers
  @build     Util
  @run       main OwnedWindowFocusIMECrashTest
*/

import java.awt.*;
import javax.swing.*;
import test.java.awt.regtesthelpers.Util;

public class OwnedWindowFocusIMECrashTest {
    Robot robot;
    JFrame owner = new JFrame("Owner Frame");
    JFrame frame = new JFrame("Other Frame");
    JWindow window = new JWindow(owner);
    JButton button = new JButton("Button");

    public static void main(String[] args) {
        OwnedWindowFocusIMECrashTest app = new OwnedWindowFocusIMECrashTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
    }

    public void start() {
        owner.setBounds(100, 100, 200, 100);
        window.setBounds(100, 250, 200, 100);
        frame.setBounds(350, 100, 200, 100);
        window.add(button);

        owner.setVisible(true);
        frame.setVisible(true);
        window.setVisible(true);

        Util.waitForIdle(robot);

        test();

        System.out.println("Test passed");
    }

    void test() {
        Util.clickOnComp(button, robot);
        if (!button.hasFocus()) {
            throw new TestErrorException("the button couldn't be focused by click");
        }
        Util.clickOnTitle(frame, robot); // here there was a crash
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
