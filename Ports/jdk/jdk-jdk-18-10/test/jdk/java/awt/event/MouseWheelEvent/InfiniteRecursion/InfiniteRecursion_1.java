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
  @summary stack overflow on mouse wheel rotation
  @author Andrei Dmitriev: area=awt.event
  @run main InfiniteRecursion_1
*/

/**
 * InfiniteRecursion_1.java
 *
 * summary: put a JButton into JPanel and then put JPanel into JFrame.
 * Add MouseWheelListener to JFrame.
 * Add MouseListener to JPanel.
 * Rotating a wheel over the JButton would result in stack overflow.
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import test.java.awt.regtesthelpers.Util;
import test.java.awt.regtesthelpers.AbstractTest;
import test.java.awt.regtesthelpers.Sysout;

public class InfiniteRecursion_1 {
    final static Robot robot = Util.createRobot();
    final static int MOVE_COUNT = 5;
    //*2 for both rotation directions,
    //*2 as Java sends the wheel event to every for nested component in hierarchy under cursor
    final static int EXPECTED_COUNT = MOVE_COUNT * 2 * 2;
    static int actualEvents = 0;

    public static void main(String []s)
    {
        JFrame frame = new JFrame("A test frame");
        JPanel outputBox = new JPanel();
        JButton jButton = new JButton();

        frame.setSize(200, 200);
        frame.addMouseWheelListener(new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e)
                {
                    System.out.println("Wheel moved on FRAME : "+e);
                    actualEvents++;
                }
            });

        outputBox.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e)
                {
                    System.out.println("MousePressed on OUTBOX : "+e);
                }

            });
        frame.add(outputBox);
        outputBox.add(jButton);

        frame.setVisible(true);

        Util.waitForIdle(robot);

        Util.pointOnComp(jButton, robot);
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
