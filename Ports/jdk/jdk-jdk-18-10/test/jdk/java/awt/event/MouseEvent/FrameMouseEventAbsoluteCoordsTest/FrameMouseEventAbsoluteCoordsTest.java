/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4992908
  @summary Need way to get location of MouseEvent in screen  coordinates (Unit-test)
  @library ../../../regtesthelpers
  @build Util
  @run main FrameMouseEventAbsoluteCoordsTest
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

// Part II
// Create Frame.
// Click on this frame.
// verify that our MouseEvent contain correct xAbs, yAbs values

public class FrameMouseEventAbsoluteCoordsTest implements MouseListener
{
    Robot robot;
    Frame frame = new Frame("MouseEvent Test Frame II");
    Button button = new Button("Just Button");
    Point mousePositionAbsolute;
    Point mousePosition;

    public static void main(final String[] args) {
        FrameMouseEventAbsoluteCoordsTest app = new FrameMouseEventAbsoluteCoordsTest();
        app.init();
        app.start();
    }

    public void init()
    {
        button.addMouseListener(this);
        frame.add(button);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }//End  init()

    public void start ()
    {
        frame.setVisible(true);
        Util.waitForIdle(robot);

        try {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
            mousePositionAbsolute = new Point(button.getLocationOnScreen().x + button.getWidth()/2,
                                              button.getLocationOnScreen().y + button.getHeight()/2);
            mousePosition = new Point(button.getWidth()/2,
                                      button.getHeight()/2);
            robot.mouseMove(mousePositionAbsolute.x,
                            mousePositionAbsolute.y );
            //            robot.delay(1000);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        }catch(AWTException e) {
            throw new RuntimeException("Test Failed. AWTException thrown.");
        }
    }// start()

    public void mousePressed(MouseEvent e){
        checkEventAbsolutePosition(e, "MousePressed OK");
    };
    public void mouseReleased(MouseEvent e){
        checkEventAbsolutePosition(e, "MouseReleased OK");
    };
    public void mouseClicked(MouseEvent e){
        checkEventAbsolutePosition(e, "MouseClicked OK");
    };
    public void mouseEntered(MouseEvent e){
        System.out.println("mouse entered");
    };
    public void mouseExited(MouseEvent e){
        System.out.println("mouse exited");
    };

    public void checkEventAbsolutePosition(MouseEvent evt, String message){
        if (evt.getXOnScreen() != mousePositionAbsolute.x ||
            evt.getYOnScreen() != mousePositionAbsolute.y ||
            !evt.getLocationOnScreen().equals( mousePositionAbsolute )  ){
            throw new RuntimeException("get(X|Y)OnScreen() or getLocationOnScreen() works incorrectly: expected"+
                                       mousePositionAbsolute.x+":"+mousePositionAbsolute.y+
                                       "\n Got:"+ evt.getXOnScreen()+":"+evt.getYOnScreen());
        }
        if (evt.getX() != mousePosition.x ||
            evt.getY() != mousePosition.y ||
            !evt.getPoint().equals( mousePosition )  ){
            throw new RuntimeException("get(X|Y)() or getLocationOnScreen() works incorrectly: expected"+
                                       mousePositionAbsolute.x+":"+mousePositionAbsolute.y+"\n Got:"
                                       +evt.getX()+":"+evt.getY());
        }
        System.out.println(message);
    }

}// class
