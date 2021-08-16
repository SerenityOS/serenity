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
 * @test
 * @key headful
 * @bug        4752312
 * @summary    Tests that after moving non-focusable window it ungrabs mouse pointer
 * @library    ../../regtesthelpers
 * @build      Util
 * @run        main FrameJumpingToMouse
 */

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.JFrame;
import test.java.awt.regtesthelpers.Util;

public class FrameJumpingToMouse {

    JFrame frame = new JFrame("Test jumping frame");
    Robot robot = Util.createRobot();

    public static void main(String[] args) {
        FrameJumpingToMouse test = new FrameJumpingToMouse();
        test.init();
        test.start();
    }

    public void init() {
        frame.setFocusableWindowState(false);
        frame.setBounds(100, 100, 100, 100);
    }

    public void start() {
        frame.setVisible(true);
        Util.waitTillShown(frame);

        Point loc = frame.getLocationOnScreen();
        robot.mouseMove(loc.x + frame.getWidth() / 4, loc.y + frame.getInsets().top / 2);
        robot.delay(50);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
        robot.mouseMove(loc.x + 100, loc.y + 50);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        Util.waitForIdle(robot);

        loc = frame.getLocation();
        robot.mouseMove(loc.x + frame.getWidth() / 2, loc.y + frame.getHeight() / 2);
        Util.waitForIdle(robot);

        if (!(frame.getLocation().equals(loc))) {
            throw new RuntimeException("Test failed: frame is moving to mouse with grab!");
        }
        System.out.println("Test passed.");
    }
}
