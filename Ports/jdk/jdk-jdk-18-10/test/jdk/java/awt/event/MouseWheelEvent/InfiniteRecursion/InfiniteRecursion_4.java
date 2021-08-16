/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6480024
  @library ../../../regtesthelpers
  @build Util Sysout AbstractTest
  @summary check that the wheel event is generated over the JFrame
  @author Andrei Dmitriev: area=awt.event
  @run main InfiniteRecursion_4
*/

/**
 * InfiniteRecursion_4.java
 *
 * summary: create simple JFrame and check that the WheelEvent is generated over it.
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import test.java.awt.regtesthelpers.Util;
import test.java.awt.regtesthelpers.AbstractTest;
import test.java.awt.regtesthelpers.Sysout;

public class InfiniteRecursion_4 {
    final static Robot robot = Util.createRobot();
    final static int MOVE_COUNT = 5;
    //*2 for both rotation directions over a single frame without any siblings
    final static int EXPECTED_COUNT = MOVE_COUNT * 2;
    static int actualEvents = 0;

    public static void main(String []s)
    {
        JFrame frame = new JFrame("A test frame");

        frame.setSize(200, 200);
        frame.addMouseWheelListener(new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e)
                {
                    System.out.println("Wheel moved on FRAME : "+e);
                    actualEvents++;
                }
            });

        frame.setVisible(true);

        Util.waitForIdle(robot);

        Util.pointOnComp(frame, robot);
        Util.waitForIdle(robot);

        for (int i = 0; i < MOVE_COUNT; i++){
            robot.mouseWheel(1);
            robot.delay(10);
        }

        for (int i = 0; i < MOVE_COUNT; i++){
            robot.mouseWheel(-1);
            robot.delay(10);
        }

        Util.waitForIdle(robot);
        //Not fair to check for multiplier 4 as it's not specified actual number of WheelEvents
        //result in a single wheel rotation.
        if (actualEvents != EXPECTED_COUNT) {
            AbstractTest.fail("Expected events count: "+ EXPECTED_COUNT+" Actual events count: "+ actualEvents);
        }
    }
}
