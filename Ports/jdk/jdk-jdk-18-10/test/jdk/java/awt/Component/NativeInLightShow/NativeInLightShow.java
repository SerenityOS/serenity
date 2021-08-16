/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test 1.0 04/05/20
  @key headful
  @bug 4140484
  @summary Heavyweight components inside invisible lightweight containers still show
  @author Your Name: art@sparc.spb.su
  @run main NativeInLightShow
*/

import java.awt.*;
import java.awt.event.*;


// The test verifies that the mixing code correctly handles COMPONENT_SHOWN events
// while the top-level container is invisible.

public class NativeInLightShow
{
    //Declare things used in the test, like buttons and labels here
    static boolean buttonPressed = false;
    public static void main(String args[]) throws Exception {
        Frame f = new Frame("Test");

        Robot robot = null;
        robot = new Robot();
        robot.setAutoDelay(50);

        Container c = new Container();
        c.setLayout(new BorderLayout());
        Button b = new Button("I'm should be visible!");
        b.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Test PASSED");
                buttonPressed = true;
            }
        });
        c.add(b);

        f.add(c);

        f.pack();

        c.setVisible(false);
        c.setVisible(true);

        // Wait for a while for COMPONENT_SHOW event to be dispatched
        robot.waitForIdle();

        f.setVisible(true);

        robot.waitForIdle();

        Point buttonLocation = b.getLocationOnScreen();

        robot.mouseMove(buttonLocation.x + 5, buttonLocation.y + 5);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        // Wait for a while for ACTION event to be dispatched
        robot.waitForIdle();
        robot.delay(100);

        if (!buttonPressed) {
            System.out.println("Test FAILED");
            throw new RuntimeException("Button was not pressed");
        }
    }

}
