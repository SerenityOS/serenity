/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4980161 7158623 8204860 8208125 8215280
  @summary Setting focusable window state to false makes the maximized frame resizable
  @compile UnfocusableMaximizedFrameResizablity.java
  @run main UnfocusableMaximizedFrameResizablity
*/

import java.awt.Toolkit;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.AWTException;
import java.awt.event.InputEvent;
import java.awt.Robot;

public class UnfocusableMaximizedFrameResizablity {

    private static Frame frame;
    private static Robot robot;
    private static boolean isProgInterruption = false;
    private static Thread mainThread = null;
    private static int sleepTime = 300000;

    private static void createAndShowFrame() throws Exception {

        //MAXIMIZED_BOTH frame is resizable on Mac OS by default. Nothing to test.
        if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
            cleanup();
            return;
        }

        //The MAXIMIZED_BOTH state is not supported by the toolkit. Nothing to test.
        if (!Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
            cleanup();
            return;
        }

        frame = new Frame("Unfocusable frame");
        frame.setMaximizedBounds(new Rectangle(0, 0, 300, 300));
        frame.setSize(200, 200);
        frame.setVisible(true);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setFocusableWindowState(false);

        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Robot creation failed");
        }
        robot.delay(2000);

        // The initial bounds of the frame
        final Rectangle bounds = frame.getBounds();

        // Let's move the mouse pointer to the bottom-right coner of the frame (the "size-grip")
        robot.mouseMove(bounds.x + bounds.width - 2, bounds.y + bounds.height - 2);
        robot.waitForIdle();

        // ... and start resizing
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
        robot.mouseMove(bounds.x + bounds.width + 20, bounds.y + bounds.height + 15);
        robot.waitForIdle();

        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();

        // The bounds of the frame after the attempt of resizing is made
        final Rectangle finalBounds = frame.getBounds();

        if (!finalBounds.equals(bounds)) {
            cleanup();
            throw new RuntimeException("The maximized unfocusable frame can be resized.");
        }
        cleanup();
    }

    private static void cleanup() {
        if (frame != null) {
            frame.dispose();
        }
        isProgInterruption = true;
        mainThread.interrupt();
    }

    public static void main(String args[]) throws Exception {

        mainThread = Thread.currentThread();

        try {
            createAndShowFrame();
            mainThread.sleep(sleepTime);
        } catch (InterruptedException e) {
            if (!isProgInterruption) {
                throw e;
            }
        }

        if (!isProgInterruption) {
            throw new RuntimeException("Timed out after " + sleepTime / 1000
                    + " seconds");
        }
    }
}

