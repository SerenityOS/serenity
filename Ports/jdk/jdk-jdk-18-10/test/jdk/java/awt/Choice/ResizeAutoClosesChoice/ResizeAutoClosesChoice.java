/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6399679
  @summary Choice is not invalidated when the frame gets resized programmatically when the drop-down is visible
  @author andrei.dmitriev area=awt.choice
  @library /test/lib
  @build jdk.test.lib.Platform
  @run main ResizeAutoClosesChoice
*/

import java.awt.*;
import java.awt.event.*;

import jdk.test.lib.Platform;

public class ResizeAutoClosesChoice
{
    static Frame frame = new Frame("Test Frame");
    static Choice choice1 = new Choice();
    static Robot robot;
    static Point pt;
    static String passed = null;
    static Button button = new Button("This button causes Frame to be resized on pack()");
    public static void main(String args[]) throws Exception
    {
        if (Platform.isOSX()) {
            System.out.println("Not for OS OX");
            return;
        }

        choice1.setForeground(Color.red);
        choice1.setBackground(Color.red);

        frame.setLayout (new BorderLayout ());
        for (int i = 1; i<10;i++){
            choice1.add("item "+i);
        }
        frame.setSize(300, 300);
        choice1.setLocation(50, 50);
        choice1.setSize(70, 20);

        button.setLocation(150, 100);
        button.setSize(150, 20);
        frame.add(choice1, BorderLayout.SOUTH);
        frame.pack();

        frame.validate();
        frame.setVisible(true);

        robot = new Robot();
        robot.waitForIdle();
        pt = choice1.getLocationOnScreen();
        robot.mouseMove(pt.x + choice1.getWidth()/10*9, pt.y + choice1.getHeight()/2);
        robot.waitForIdle();
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(1000);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        Color color = robot.getPixelColor(pt.x + choice1.getWidth()/2,
                                          pt.y + 3 * choice1.getHeight());
        //should take a color on the point on the choice's menu
        System.out.println("Choice opened. Color got : "+color);
        if ( !color.equals(Color.red) ){
            passed = "Choice wasn't opened with the mouse";
        }

        Rectangle oldBounds = choice1.getBounds();
        System.out.println("Choice's old bounds : "+oldBounds);

        frame.add(button, BorderLayout.NORTH);
        //            frame.setSize(500, 500);
        frame.pack();
        robot.waitForIdle();
        System.out.println("Choice's new bounds : "+choice1.getBounds());

        if (!choice1.getBounds().equals(oldBounds)){
            pt = choice1.getLocationOnScreen();
            color = robot.getPixelColor(pt.x + choice1.getWidth()/2,
                                        pt.y + 3 * choice1.getHeight());
            System.out.println("Choice opened. Color got : "+color);
            if (color.equals(Color.red) ){
                passed = "Choice wasn't closed when toplevel repacked.";
            }
        } else {
            System.out.println("frame.pack didn't changed Choice's size - dropdown menu should remain the same. Test passed.");
        }
        if (passed != null){
            throw new RuntimeException(passed);
        }
    }
}
