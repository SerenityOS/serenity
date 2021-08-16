/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 7154177
  @summary An invisible owner frame should never become visible
  @author anthony.petrov@oracle.com: area=awt.toplevel
  @library ../../regtesthelpers
  @build Util
  @run main InvisibleOwner
*/

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import test.java.awt.regtesthelpers.Util;

public class InvisibleOwner {
    private static volatile boolean invisibleOwnerClicked = false;
    private static volatile boolean backgroundClicked = false;

    private static final int F_X = 40, F_Y = 40, F_W = 200, F_H = 200;

    public static void main(String[] args) throws AWTException {
        // A background frame to compare a pixel color against
        Frame helperFrame = new Frame("Background frame");
        helperFrame.setBackground(Color.BLUE);
        helperFrame.setBounds(F_X - 10, F_Y - 10, F_W + 20, F_H + 20);
        helperFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                backgroundClicked= true;
            }
        });
        helperFrame.setVisible(true);

        // An owner frame that should stay invisible
        Frame frame = new Frame("Invisible Frame");
        frame.setBackground(Color.GREEN);
        frame.setLocation(F_X, F_Y);
        frame.setSize(F_W, F_H);
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                invisibleOwnerClicked = true;
            }
        });

        // An owned window
        final Window window = new Window(frame);
        window.setBackground(Color.RED);
        window.setSize(200, 200);
        window.setLocation(300, 300);
        window.setVisible(true);
        try { Thread.sleep(1000); } catch (Exception ex) {}

        Robot robot = new Robot();

        // Clicking the owned window shouldn't make its owner visible
        Util.clickOnComp(window, robot);
        try { Thread.sleep(500); } catch (Exception ex) {}


        // Assume the location and size are applied to the frame as expected.
        // This should work fine on the Mac. We can't call getLocationOnScreen()
        // since from Java perspective the frame is invisible anyway.

        // 1. Check the color at the center of the owner frame
        Color c = robot.getPixelColor(F_X + F_W / 2, F_Y + F_H / 2);
        System.err.println("Pixel color: " + c);
        if (c == null) {
            throw new RuntimeException("Robot.getPixelColor() failed");
        }
        if (c.equals(frame.getBackground())) {
            throw new RuntimeException("The invisible frame has become visible");
        }
        if (!c.equals(helperFrame.getBackground())) {
            throw new RuntimeException("The background helper frame has been covered by something unexpected");
        }

        // 2. Try to click it
        robot.mouseMove(F_X + F_W / 2, F_Y + F_H / 2);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        try { Thread.sleep(500); } catch (Exception ex) {}

        // Cleanup
        window.dispose();
        frame.dispose();
        helperFrame.dispose();

        // Final checks
        if (invisibleOwnerClicked) {
            throw new RuntimeException("An invisible owner frame got clicked. Looks like it became visible.");
        }
        if (!backgroundClicked) {
            throw new RuntimeException("The background helper frame hasn't been clciked");
        }
    }
}
