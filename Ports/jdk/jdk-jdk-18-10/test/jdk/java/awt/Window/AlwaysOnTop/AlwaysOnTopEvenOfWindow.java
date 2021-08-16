/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 5028924
  @summary Always-on-top frame should be on top of Window.
  @author Yuri.Nesterenko: area=awt.toplevel
  @library ../../regtesthelpers
  @build Util
  @run main AlwaysOnTopEvenOfWindow
*/


import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

/**
 * AlwaysOnTopEvenOfWindow.java
 * Summary: tests that a Frame marked always-on-top actually is on top of
 * a Window.
 * Test fails in case of override-redirect Window (e.g. with JDK6.0);
 */
public class AlwaysOnTopEvenOfWindow {
    static boolean clicked = false;
    public static void main(String args[]) {

        Window win = new Window(null);
        win.setBounds( 50,50, 300,50);
        win.addMouseListener( new MouseAdapter() {
            public void mouseClicked( MouseEvent me ) {
                clicked = true;
            }
        });
        Frame frame = new Frame("top");
        frame.setBounds(100, 20, 50, 300);
        frame.setAlwaysOnTop( true );

        // position robot before show(): there may be point-to-focus;
        Robot robot = Util.createRobot();
        robot.mouseMove(125, 75);

        frame.setVisible(true);
        win.setVisible(true);
        Util.waitForIdle(robot);
        if(!frame.isAlwaysOnTopSupported())  {
            // pass
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Util.waitForIdle(robot);
        if( clicked ) {
            throw new RuntimeException("This part of Window should be invisible");
        }

    }
}
