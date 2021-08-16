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
  @bug       6785058
  @summary   Tests that an owner is activated on closing its owned dialog with the warning icon.
  @library   ../../regtesthelpers
  @build     Util
  @run       main/othervm/policy=java.policy -Djava.security.manager CloseDialogActivateOwnerTest
*/

import java.awt.*;
import test.java.awt.regtesthelpers.Util;

public class CloseDialogActivateOwnerTest {
    Robot robot;

    public static void main(String[] args) {
        CloseDialogActivateOwnerTest app = new CloseDialogActivateOwnerTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
    }

    public void start() {
        final Frame frame = new Frame("Owner Frame");
        final Dialog dialog = new Dialog(frame, "Owned Dialog");

        frame.setSize(100, 100);
        dialog.setSize(100, 100);

        // Show the owner. Check that it's focused.
        if (!Util.trackWindowGainedFocus(frame, new Runnable() {
                public void run() {
                    frame.setVisible(true);
                }
            }, 2000, false))
        {
            throw new TestErrorException("the owner frame hasn't been activated on show");
        }

        // Show the owned dialog. Check that it's focused.
        if (!Util.trackWindowGainedFocus(dialog, new Runnable() {
                public void run() {
                    dialog.setVisible(true);
                }
            }, 2000, true))
        {
            throw new TestErrorException("the owned dialog hasn't been activated on show");
        }

        robot.delay(2000); // wait for the warning icon is shown

        // Close the dialog. Check that the owner is activated.
        if (!Util.trackWindowGainedFocus(frame, new Runnable() {
                public void run() {
                    dialog.dispose();
                }
            }, 2000, false))
        {
            throw new TestFailedException("the owner hasn't been activated on closing the owned dialog");
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
class TestErrorException extends Error {
    TestErrorException(String msg) {
        super("Unexpected error: " + msg);
    }
}

