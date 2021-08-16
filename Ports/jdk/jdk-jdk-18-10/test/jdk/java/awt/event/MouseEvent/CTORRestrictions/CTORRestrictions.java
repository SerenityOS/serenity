/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
  test %I% %E%
  @bug 6315717
  @summary verifies that MouseEvent could be constructed correctly for mouse extra buttons in regard to sun.awt.enableExtraMouseButtons property
  @author Andrei Dmitriev : area=awt.event
  @run main CTORRestrictions
 */

/*
 * verify that user can create the MouseEvent? with button1|2|3|4|5|... when property "sun.awt.enableExtraMouseButtons" is true by default
 */
import java.awt.*;
import java.awt.event.*;

public class CTORRestrictions{
    static Frame frame = new Frame("MouseEvent Test Frame");
    static Point mousePosition;
    static Point mousePositionOnScreen;

    public static void main(String []s){
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException("Test Failed", ex);
        }
        frame.setSize (200,200);
        frame.setLocation (300, 400);
        frame.setVisible(true);
        robot.delay(1000);
        System.out.println("sun.awt.enableExtraMouseButtons = "+Toolkit.getDefaultToolkit().getDesktopProperty("sun.awt.enableExtraMouseButtons"));
        mousePosition = new Point(100, 100);
        mousePositionOnScreen = new  Point(frame.getLocationOnScreen().x + mousePosition.x,
                                                 frame.getLocationOnScreen().y + mousePosition.y);

        /*
         * On Linux the native system count a wheel (both directions) as two more buttons on a mouse.
         * So, MouseInfo.getNumberOfButtons() would report 5 buttons on a three-button mouse.
         * On Windows it would still report that MouseInfo.getNumberOfButtons() == 3.
         * We should handle XToolkit case and iterate through the buttons
         * up to (MouseInfo.getNumberOfButtons() - 2) value.
         */
        int numberOfButtons;
        if (Toolkit.getDefaultToolkit().getClass().getName().equals("sun.awt.windows.WToolkit")){
            numberOfButtons = MouseInfo.getNumberOfButtons();
        } else {
            numberOfButtons = MouseInfo.getNumberOfButtons() - 2;
        }
        System.out.println("Stage 1. Number of buttons = "+ numberOfButtons);

        for (int buttonId = 1; buttonId <= numberOfButtons; buttonId++){
            postMouseEventNewCtor(buttonId);
        }

        System.out.println("Stage 2. Number of buttons = "+ numberOfButtons);
        for (int buttonId = 1; buttonId <= numberOfButtons; buttonId++){
            postMouseEventOldCtor(buttonId);
        }
        System.out.println("Test passed.");
    }

    public static void postMouseEventNewCtor(int buttonId)    {
        MouseEvent me = new MouseEvent(frame,
                                       MouseEvent.MOUSE_PRESSED,
                                       System.currentTimeMillis(),
                                       MouseEvent.BUTTON1_DOWN_MASK,
                                       mousePosition.x, mousePosition.y,
                                       mousePositionOnScreen.x,
                                       mousePositionOnScreen.y,
                                       1,
                                       false,              //popupTrigger
                                       buttonId            //button
                                       );
        frame.dispatchEvent( ( AWTEvent )me );
    }

    public static void postMouseEventOldCtor(int buttonId)    {
        MouseEvent meOld = new MouseEvent(frame,
                                          MouseEvent.MOUSE_PRESSED,
                                          System.currentTimeMillis(),
                                          MouseEvent.BUTTON1_DOWN_MASK,
                                          mousePosition.x, mousePosition.y,
                                          1,
                                          false,              //popupTrigger
                                          buttonId //button
                                          );
        frame.dispatchEvent( ( AWTEvent )meOld );
    }
}
