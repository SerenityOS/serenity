/*
 * Copyright (c) 1995, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug       6886678
  @summary   Tests that clicking an owner frame switches focus from its owned window.
  @library   ../../regtesthelpers
  @build     Util
  @run       main FocusOwnerFrameOnClick
*/

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;
import test.java.awt.regtesthelpers.Util;

public class FocusOwnerFrameOnClick {
    Robot robot;
    Frame frame = new Frame("Frame");
    Window window = new Window(frame);
    Button fButton = new Button("fButton");
    Button wButton = new Button("wButton");

    AtomicBoolean focused = new AtomicBoolean(false);

    public static void main(String[] args) {
        FocusOwnerFrameOnClick app = new FocusOwnerFrameOnClick();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();

        frame.setLayout(new FlowLayout());
        frame.setSize(200, 200);
        frame.add(fButton);

        window.setLocation(300, 0);
        window.add(wButton);
        window.pack();
    }

    public void start() {
        frame.setVisible(true);
        Util.waitForIdle(robot);

        window.setVisible(true);
        Util.waitForIdle(robot);

        if (!wButton.hasFocus()) {
            if (!Util.trackFocusGained(wButton, new Runnable() {
                    public void run() {
                        Util.clickOnComp(wButton, robot);
                    }
                }, 2000, false))
            {
                throw new TestErrorException("wButton didn't gain focus on showing");
            }
        }

        Runnable clickAction = new Runnable() {
                public void run() {
                    Point loc = fButton.getLocationOnScreen();
                    Dimension dim = fButton.getSize();

                    robot.mouseMove(loc.x, loc.y + dim.height + 20);
                    robot.delay(50);
                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    robot.delay(50);
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                }
            };

        if (!Util.trackWindowGainedFocus(frame, clickAction, 2000, true)) {
            throw new TestFailedException("The frame wasn't focused on click");
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
