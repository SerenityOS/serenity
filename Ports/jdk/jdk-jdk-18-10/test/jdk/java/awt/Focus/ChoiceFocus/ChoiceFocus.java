/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4053856
  @summary Choice components don't honour key focus
  @library ../../regtesthelpers
  @build Util
  @author Andrei Dmitriev : area=awt.choice
  @run main ChoiceFocus
*/

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

/*
Here is the old description for the test.

This bug occurs in jdk <= 1.1.6, 1.2b3. The problem is that key press
and release events will not be delivered to a choice component with
the focus if the mouse is over another component (of any type);

This bug occurs in Motif and was fixed in the native code.

1. Set the focus on choice component 1 by selecting an item.
2. Move the mouse over choice component 2, BUT do not set the focus on it.
3. Type some characters e.g. 'a', 'b' etc.
4. Verify by the console output that all the key events are delivered to
   choice component 1, not 2. If all the key events are delivered to
   choice 1, the test passes.
*/

public class ChoiceFocus {
    static Robot robot;
    volatile static boolean keyPressed = false;
    volatile static boolean keyReleased = false;
    volatile static boolean keyTyped = false;

    public static void main(String[] args) {
        Frame f = new Frame("Test Frame");
        f.setLayout(new GridLayout());

        Choice c1 = new Choice();
        c1.add("Choice 1, Item 1");
        c1.add("Choice 1, Item 2");

        Choice c2 = new Choice();
        c2.add("Choice 2, Item 1");
        c2.add("Choice 2, Item 2");
        c1.addKeyListener(new KeyListener(){
                public void keyPressed(KeyEvent e){
                    System.out.println("Key Pressed Event "+e);
                    keyPressed = true;
                }
                public void keyReleased(KeyEvent e){
                    System.out.println("Key Released Event "+e);
                    keyReleased = true;
                }
                public void keyTyped(KeyEvent e){
                    System.out.println("Key Typed Event "+e);
                    keyTyped = true;
                }
            });

        f.add(c1);
        f.add(c2);

        f.pack();
        f.setVisible(true);

        robot = Util.createRobot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(50);

        //transfer focus to Choice
        Util.waitForIdle(robot);
        Util.clickOnComp(c1, robot);
        Util.waitForIdle(robot);

        //close choice
        Util.clickOnComp(c1, robot);

        //position a mouse over a different component
        Point pt = c2.getLocationOnScreen();
        robot.mouseMove(pt.x + c2.getWidth()/2, pt.y + c2.getHeight()/2);
        Util.waitForIdle(robot);

        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        Util.waitForIdle(robot);

        if (!keyPressed || !keyReleased || !keyTyped){
            throw new RuntimeException("Failed. Some of event wasn't come "+keyPressed + " : "+keyReleased+" : "+keyTyped);
        } else {
            System.out.println("Test passed");
        }
    }
}////~
