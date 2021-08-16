/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4350402
  @summary Tests that mouse behavior on LW component
  @compile ../../../regtesthelpers/Util.java
  @run main RobotLWTest
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class RobotLWTest {

    public static void main(String[] args) {
        RobotLWTest app = new RobotLWTest();
        app.start();
    }

    public void start ()
    {
        Frame frame = new Frame();
        MyLWContainer c = new MyLWContainer();
        MyLWComponent b = new MyLWComponent();
        c.add(b);
        frame.add(c);
        frame.setSize(400,400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        try {
            Robot robot = new Robot();
            robot.setAutoWaitForIdle(true);
            robot.setAutoDelay(100);
            robot.waitForIdle();

            Util.waitForIdle(robot);

            Point pt = frame.getLocationOnScreen();
            Point pt1 = b.getLocationOnScreen();

            //Testing capture with multiple buttons
            robot.mouseMove(pt1.x + b.getWidth()/2, pt1.y + b.getHeight()/2);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mousePress(InputEvent.BUTTON2_MASK);
            robot.mouseMove(pt.x + frame.getWidth()+10, pt.y + frame.getHeight()+10);
            robot.mouseRelease(InputEvent.BUTTON2_MASK);
            Util.waitForIdle(robot);

            b.last = null;
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Util.waitForIdle(robot);

            if (b.last == null) {
                throw new RuntimeException("RobotLWTest failed. Mouse Capture failed");
            }
            //Enter/Exit
            b.last = null;
            robot.mouseMove(pt1.x + b.getWidth()/2, pt1.y + b.getHeight()/2);
            Util.waitForIdle(robot);

            if (b.last == null || b.last.getID() != MouseEvent.MOUSE_ENTERED) {
                throw new RuntimeException("RobotLWTest failed. Enter/Exit failed");
            }
            b.last = b.prev = null;
            robot.mousePress(InputEvent.BUTTON1_MASK);
            Util.waitForIdle(robot);

            if (b.prev != null && b.prev.getID() == MouseEvent.MOUSE_ENTERED) {
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                throw new RuntimeException("RobotLWTest failed. Enter/Exit failed");
            }
            robot.mouseRelease(InputEvent.BUTTON1_MASK);

        } catch (Exception e) {
            throw new RuntimeException("The test was not completed.", e);
        }
    }// start()

}// class RobotLWTest

class MyLWContainer extends Container {
    public MouseEvent last = null;
    public MouseEvent prev = null;

    MyLWContainer() {
        enableEvents(MouseEvent.MOUSE_MOTION_EVENT_MASK);
    }

    public void processMouseEvent(MouseEvent e) {
        prev = last;
        last = e;
        System.out.println(e.toString());
        super.processMouseEvent(e);
    }
}

class MyLWComponent extends Component {
    public MouseEvent last = null;
    public MouseEvent prev = null;

    MyLWComponent() {
        setSize(50,30);
        enableEvents(MouseEvent.MOUSE_EVENT_MASK);
    }

    public void processMouseEvent(MouseEvent e) {
        prev = last;
        last = e;
        System.out.println(e.toString());
        super.processMouseEvent(e);
    }

    public void paint(Graphics g) {
        Dimension d = getSize();
        setBackground(isEnabled() ? Color.red : Color.gray);
        g.clearRect(0, 0, d.width - 1, d.height -1);
    }
}
