/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8176490
 * @summary Tests that there is no hang or deadlock when the visibility
 *      of parent and child windows is changed.
 * @library ../../regtesthelpers
 * @build Util
 * @run main/timeout=20 WindowDeadlockTest
 */

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Robot;

import test.java.awt.regtesthelpers.Util;

public class WindowDeadlockTest {
    public static void main(String[] args) throws Exception {
        Robot robot = Util.createRobot();

        Frame main = new Frame("Main");
        main.setBounds(0, 0, 200, 100);
        main.setVisible(true);

        Dialog first = new Dialog(main, "First");
        first.setBounds(250, 0, 200, 100);
        first.setVisible(true);

        Dialog second = new Dialog(first, "Second");
        second.setBounds(0, 150, 200, 100);
        second.setVisible(true);

        Util.waitForIdle(robot);
        robot.delay(2000);

        Dialog third = new Dialog(first, "Third", false);
        third.setBounds(250, 150, 200, 100);
        third.setVisible(true);
        first.setVisible(false); // the hang takes place here

        Util.waitForIdle(robot);
        robot.delay(2000);

        third.dispose();
        second.dispose();
        first.dispose();
        main.dispose();
    }
}
