/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4664415
  @summary Test that double clicking the titlebar does not send RELEASE/CLICKED
  @library    ../../regtesthelpers
  @build      Util
  @run main TitleBarDoubleClick
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class TitleBarDoubleClick implements MouseListener,
 WindowListener
{
    //Declare things used in the test, like buttons and labels here
    private final static Rectangle BOUNDS = new Rectangle(300, 300, 300, 300);
    private final static int TITLE_BAR_OFFSET = 10;

    Frame frame;
    Robot robot;

    public static void main(final String[] args) {
        TitleBarDoubleClick app = new TitleBarDoubleClick();
        app.start();
    }

    public void start ()
    {
            robot = Util.createRobot();
            robot.setAutoDelay(100);
            robot.mouseMove(BOUNDS.x + (BOUNDS.width / 2),
                            BOUNDS.y + (BOUNDS.height/ 2));

            frame = new Frame("TitleBarDoubleClick");
            frame.setBounds(BOUNDS);
            frame.addMouseListener(this);
            frame.addWindowListener(this);
            frame.setVisible(true);
    }// start()

    // Move the mouse into the title bar and double click to maximize the
    // Frame
    static boolean hasRun = false;

    private void doTest() {
        if (hasRun) return;
        hasRun = true;

        System.out.println("doing test");
            robot.mouseMove(BOUNDS.x + (BOUNDS.width / 2),
                            BOUNDS.y + TITLE_BAR_OFFSET);
            robot.delay(50);
            // Util.waitForIdle(robot) seem always hangs here.
            // Need to use it instead robot.delay() when the bug become fixed.
            System.out.println("1st press:   currentTimeMillis: " + System.currentTimeMillis());
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            System.out.println("1st release: currentTimeMillis: " + System.currentTimeMillis());
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            System.out.println("2nd press:   currentTimeMillis: " + System.currentTimeMillis());
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            System.out.println("2nd release: currentTimeMillis: " + System.currentTimeMillis());
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            System.out.println("done:        currentTimeMillis: " + System.currentTimeMillis());
    }

    private void fail() {
        throw new AWTError("Test failed");
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {fail();}
    public void mouseReleased(MouseEvent e) {fail();}
    public void mouseClicked(MouseEvent e) {fail();}

    public void windowActivated(WindowEvent  e) {doTest();}
    public void windowClosed(WindowEvent  e) {}
    public void windowClosing(WindowEvent  e) {}
    public void windowDeactivated(WindowEvent  e) {}
    public void windowDeiconified(WindowEvent  e) {}
    public void windowIconified(WindowEvent  e) {}
    public void windowOpened(WindowEvent  e) {}

}// class TitleBarDoubleClick
