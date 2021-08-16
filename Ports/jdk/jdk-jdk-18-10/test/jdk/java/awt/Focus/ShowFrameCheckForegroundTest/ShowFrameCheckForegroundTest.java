/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug       6492970
  @summary   Tests that showing a toplvel in a not foreground Java process activates it.
  @library   ../../regtesthelpers
  @build     Util
  @run       main ShowFrameCheckForegroundTest
 */

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class ShowFrameCheckForegroundTest {
    Robot robot;
    Frame nofocusFrame = new Frame("Non-focusable");
    Frame frame = new Frame("Frame");
    Dialog dialog1 = new Dialog(nofocusFrame, "Owned Dialog", false);
    Dialog dialog2 = new Dialog((Frame)null, "Owned Dialog", false);
    Window testToplevel = null;
    Button testButton = new Button("button");
    Button showButton = new Button("show");
    Runnable action = new Runnable() {
        public void run() {
            robot.keyPress(KeyEvent.VK_SPACE);
            robot.delay(50);
            robot.keyRelease(KeyEvent.VK_SPACE);
        }
    };


    public static void main(String[] args) {
        ShowFrameCheckForegroundTest app = new ShowFrameCheckForegroundTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
    }

    public void start() {
        showButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Util.showWindowWait(testToplevel);
            }
        });
        nofocusFrame.add(showButton);
        nofocusFrame.pack();
        nofocusFrame.setFocusableWindowState(false);
        nofocusFrame.setLocation(200, 200);
        nofocusFrame.setVisible(true);
        Util.waitForIdle(robot);

        robot.delay(3000);

        // 1. Show the toplvel without clicking into the non-focusable frame.
        test(frame, 1);
        test(dialog1, 1);
        test(dialog2, 1);

        // 2. Showing the toplvel via clicking into the non-focusable frame.
        test(frame, 2);
        test(dialog1, 2);
        test(dialog2, 2);

        System.out.println("Test passed.");
    }

    private void test(Window toplevel, int stage) {
        toplevel.add(testButton);
        toplevel.pack();
        toplevel.setLocation(400, 200);

        switch (stage) {
            case 1:
                Util.showWindowWait(toplevel);
                break;
            case 2:
                testToplevel = toplevel;
                Util.showWindowWait(nofocusFrame);
                Util.waitForIdle(robot);
                Util.clickOnComp(showButton, robot);
                break;
        }
        Util.waitForIdle(robot);

        if (!Util.trackActionPerformed(testButton, action, 2000, false)) {
            throw new TestFailedException("Stage " + stage + ". The toplevel " + toplevel + " wasn't made foreground on showing");
        }
        System.out.println("Stage " + stage + ". Toplevel " + toplevel + " - passed");
        toplevel.dispose();
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}
