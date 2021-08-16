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
  @bug       6522725
  @summary   Tests for proper request-focus-back on FOCUS_LOST.
  @library   ../../regtesthelpers
  @build     Util
  @run       main IconifiedFrameFocusChangeTest
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class IconifiedFrameFocusChangeTest {
    Frame testFrame = new Frame("Test Frame");
    Frame otherFrame = new Frame("Other Frame");
    Button testButton = new Button("test button");
    Button otherButton = new Button("other button");
    Robot robot;

    public static void main(String[] args) {
        IconifiedFrameFocusChangeTest app = new IconifiedFrameFocusChangeTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();

        testFrame.add(testButton);
        testFrame.pack();
        otherFrame.add(otherButton);
        otherFrame.pack();
        otherFrame.setLocation(200, 0);

        testButton.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                testButton.requestFocus();
            }
        });
    }

    public void start() {
        otherFrame.setVisible(true);
        Util.waitForIdle(robot);
        testFrame.setVisible(true);
        Util.waitForIdle(robot);

        robot.delay(1000); // additional delay is required

        if (!testButton.hasFocus()) {
            testButton.requestFocus();
            Util.waitForIdle(robot);
            if (!testButton.hasFocus()) {
                throw new TestErrorException("couldn't focus " + testButton);
            }
        }

        /*
         * Iconify the Frame. Test that focus switches properly.
         */
        Runnable action = new Runnable() {
            public void run() {
                testFrame.setExtendedState(Frame.ICONIFIED);
            }
        };
        if (!Util.trackFocusGained(otherButton, action, 2000, true)) {
            throw new TestFailedException("iconifying focused window didn't trigger focus change");
        }

        /*
         * Test that key events go into the focus owner.
         */
        action = new Runnable() {
            public void run() {
                robot.keyPress(KeyEvent.VK_SPACE);
                robot.delay(50);
                robot.keyRelease(KeyEvent.VK_SPACE);
            }
        };
        if (!Util.trackActionPerformed(otherButton, action, 2000, true)) {
            throw new TestFailedException("Java focus owner doesn't match to the native one");
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
