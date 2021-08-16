/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6562853 7035459
  @summary Tests that focus transfered directy to window w/o transfering it to frame.
  @author Oleg Sukhodolsky: area=awt.focus
  @library ../../regtesthelpers
  @build Util
  @run main TranserFocusToWindow
*/

import java.awt.Button;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Robot;
import java.awt.Window;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import test.java.awt.regtesthelpers.Util;

public class TranserFocusToWindow
{
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;

    public static void main(String[] args) {
        Robot robot = Util.createRobot();
        Frame owner_frame = new Frame("Owner frame");
        owner_frame.setBounds(0, 0, WIDTH, HEIGHT);
        owner_frame.setVisible(true);
        Util.waitForIdle(robot);

        Window window = new Window(owner_frame);
        Button btn1 = new Button("button for focus");
        window.add(btn1);
        window.pack();
        window.setLocation(0, HEIGHT + 100);
        window.setVisible(true);
        Util.waitForIdle(robot);

        Frame another_frame = new Frame("Another frame");
        Button btn2 = new Button("button in a frame");
        another_frame.add(btn2);
        another_frame.pack();
        another_frame.setLocation(WIDTH + 100, 0);
        another_frame.setVisible(true);
        Util.waitForIdle(robot);

        owner_frame.addWindowFocusListener(new WindowFocusListener() {
                public void windowLostFocus(WindowEvent we) {
                    System.out.println(we);
                }
                public void windowGainedFocus(WindowEvent we) {
                    System.out.println(we);
                    throw new RuntimeException("owner frame must not receive WINDWO_GAINED_FOCUS");
                }
            });
        window.addWindowFocusListener(new WindowFocusListener() {
                public void windowLostFocus(WindowEvent we) {
                    System.out.println(we);
                }
                public void windowGainedFocus(WindowEvent we) {
                    System.out.println(we);
                }
            });
        another_frame.addWindowFocusListener(new WindowFocusListener() {
                public void windowLostFocus(WindowEvent we) {
                    System.out.println(we);
                }
                public void windowGainedFocus(WindowEvent we) {
                    System.out.println(we);
                }
            });

        Util.clickOnTitle(owner_frame, robot);
        Util.waitForIdle(robot);
        setFocus(btn1, robot);
        setFocus(btn2, robot);
        // we need this delay so WM can not treat two clicks on title as double click
        robot.delay(500);
        Util.clickOnTitle(owner_frame, robot);
        Util.waitForIdle(robot);

        System.out.println("test passed");
    }

    private static void setFocus(final Component comp, final Robot r) {
        if (comp.hasFocus()) {
            return;
        }

        Util.clickOnComp(comp, r);
        Util.waitForIdle(r);

        if (!comp.hasFocus()) {
            throw new RuntimeException("can not set focus on " + comp);
        }
    }
}
