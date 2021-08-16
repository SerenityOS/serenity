/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4800638
  @summary Tests that Choice does not lock the Desktop
  @run main GrabLockTest
*/
import java.awt.*;
import java.awt.event.*;

public class GrabLockTest
{
    public static void main (String args[])
    {
        Frame frame = new TestFrame();
    }
}

class TestFrame extends Frame implements MouseListener {
    public TestFrame() {
        Choice choice = new Choice();
        choice.addItem("Fist Item");
        choice.addItem("Second Item");
        add(choice,BorderLayout.NORTH);
        Panel panel = new Panel();
        panel.addMouseListener(this);
        panel.setBackground(Color.RED);
        add(panel);
        setSize(200, 200);
        setVisible(true);
        toFront();

        try {
            Robot robot = new Robot();
            robot.setAutoWaitForIdle(true);
            robot.setAutoDelay(50);

            robot.waitForIdle();

            Point pt = choice.getLocationOnScreen();
            robot.mouseMove(pt.x + choice.getWidth() - choice.getHeight()/2,
                pt.y + choice.getHeight()/2);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();
            robot.mouseMove(pt.x + choice.getWidth()/2,
                pt.y + choice.getHeight()*2);
            robot.waitForIdle();
            robot.mousePress(InputEvent.BUTTON2_MASK);
            robot.waitForIdle();
            Point pt1 = panel.getLocationOnScreen();
            robot.mouseMove(pt1.x + panel.getWidth()/2,
                pt1.y + panel.getHeight()/2);
            robot.waitForIdle();
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON2_MASK);

            robot.waitForIdle();

            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(30);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();
            if (nPressed == 0) {
                robot.keyPress(KeyEvent.VK_ESCAPE);
                robot.keyRelease(KeyEvent.VK_ESCAPE);
                throw new RuntimeException("GrabLockTest failed." + nPressed);
            }
        } catch (Exception e) {
            throw new RuntimeException("The test was not completed.\n\n" + e);
        }

    }

    public int nPressed = 0;

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        nPressed++;
        System.out.println("Pressed!");
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
}// class TestFrame
