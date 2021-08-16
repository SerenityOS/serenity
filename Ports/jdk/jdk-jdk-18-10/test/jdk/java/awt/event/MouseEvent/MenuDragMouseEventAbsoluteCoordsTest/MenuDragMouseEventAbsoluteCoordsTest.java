/*
 * Copyright (c) 2004, 2006, Oracle and/or its affiliates. All rights reserved.
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
  @summary Need way to get location of MouseEvent in screen coordinates
  @library ../../../regtesthelpers
  @build Util
  @run main MenuDragMouseEventAbsoluteCoordsTest
*/

import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

// The test consits of several parts:
// 1. create MenuDragMouseEvent with new Ctor and checking get(X|Y)OnScreen(),
// getLocationOnScreen(), get(X|Y), getPoint().
// 2. create MenuDragMouseEvent with old Ctor and checking get(X|Y)OnScreen(),
// getLocationOnScreen(),  get(X|Y), getPoint() .


public class MenuDragMouseEventAbsoluteCoordsTest implements MouseListener
{
    Frame frame = new Frame("MenuDragMouseEvent Test Frame");

    Point mousePositionOnScreen = new Point(200, 200);
    Point mousePosition = new Point(100, 100);

    public static void main(String[] args) {
        MenuDragMouseEventAbsoluteCoordsTest app = new MenuDragMouseEventAbsoluteCoordsTest();
        app.init();
        app.start();
    }

    public void init()
    {
        frame.addMouseListener(this);
    }//End  init()

    public void start ()
    {
        //Get things going.  Request focus, set size, et cetera
        frame.setSize (200,200);
        frame.setVisible(true);

        try {
            Util.waitForIdle(new Robot());
        }catch (Exception e){
            throw new RuntimeException("Test failed.", e);
        }

        // use new MenuDragMouseEvent's Ctor with user-defined absolute
        // coordinates

        System.out.println("New Ctor Stage MOUSE_PRESSED");
        postMenuDragMouseEventNewCtor(MouseEvent.MOUSE_PRESSED);
        // now we are going to use old MenuDragMouseEvent's Ctor thus absolute
        // position calculates as frame's location + relative coords
        // of the event.
        mousePositionOnScreen = new Point(frame.getLocationOnScreen().x + mousePosition.x,
                                          frame.getLocationOnScreen().y + mousePosition.y);

        System.out.println("Old Ctor Stage MOUSE_PRESSED");
        postMenuDragMouseEventOldCtor(MouseEvent.MOUSE_PRESSED);
    }// start()

    public void mousePressed(MouseEvent e){
        checkEventAbsolutePosition(e, "MousePressed OK");
    };

    public void mouseExited(MouseEvent e){
        System.out.println("mouse exited");
    };
    public void mouseReleased(MouseEvent e){
        checkEventAbsolutePosition(e, "MousePressed OK");
    };
    public void mouseEntered(MouseEvent e){
        System.out.println("mouse entered");
    };
    public void mouseClicked(MouseEvent e){
        checkEventAbsolutePosition(e, "MousePressed OK");
    };

    public void postMenuDragMouseEventNewCtor(int MouseEventType)    {
        MouseEvent me = new MenuDragMouseEvent(frame,
                                               MouseEventType,
                                               System.currentTimeMillis(),
                                               MouseEvent.BUTTON1_DOWN_MASK,
                                               mousePosition.x, mousePosition.y,
                                               mousePositionOnScreen.x,
                                               mousePositionOnScreen.y,
                                               1,                   //clickCount
                                               false,              //popupTrigger
                                               null,                //MenuElement
                                               null                 //MenuSelectionManager
                                               );
        frame.dispatchEvent( ( AWTEvent )me );
    }

    public void postMenuDragMouseEventOldCtor(int MouseEventType)    {
        MouseEvent meOld = new MenuDragMouseEvent(frame,
                                          MouseEventType,
                                          System.currentTimeMillis(),
                                          MouseEvent.BUTTON1_DOWN_MASK,
                                          mousePosition.x, mousePosition.y,
                                          1,
                                          false,              //popupTrigger
                                          null, null
                                          );
        frame.dispatchEvent( ( AWTEvent )meOld );
    }

    public void checkEventAbsolutePosition(MouseEvent evt, String message){
            if (evt.getXOnScreen() != mousePositionOnScreen.x ||
                evt.getYOnScreen() != mousePositionOnScreen.y ||
                !evt.getLocationOnScreen().equals( mousePositionOnScreen )  ){
                System.out.println("evt.location = "+evt.getLocationOnScreen());
                System.out.println("mouse.location = "+mousePositionOnScreen);
                throw new RuntimeException("get(X|Y)OnScreen() or getPointOnScreen() work incorrectly");
            }

            if (evt.getX() != mousePosition.x ||
                evt.getY() != mousePosition.y ||
                !evt.getPoint().equals( mousePosition )  ){
                throw new RuntimeException("get(X|Y)() or getPoint() work incorrectly");
            }
        System.out.println(message);
    }
}// class AutomaticAppletTest
