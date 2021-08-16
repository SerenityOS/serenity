/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 7036733
  @summary Regression : NullPointerException when scrolling horizontally on AWT List
  @author Andrei Dmitriev area=awt-list
  @library ../../regtesthelpers
  @build Util
  @run main ScrollOut
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class ScrollOut
{
    public static final void main(String args[])
    {
        final Frame frame = new Frame();
        final List list = new List();
        Robot robot = null;

        for (int i = 0; i < 5; i++){
            list.add("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz");
        }

        frame.add(list);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);


        try{
            robot = new Robot();
        }catch(AWTException e){
            throw new RuntimeException(e);
        }
        robot.waitForIdle();

        //Drag from center to the outside on left
        Point from = new Point(list.getLocationOnScreen().x + list.getWidth()/2,
                               list.getLocationOnScreen().y + list.getHeight()/2);
        Point to = new Point(list.getLocationOnScreen().x - 30,
                             from.y);

        robot.waitForIdle();
        Util.drag(robot, from, to, InputEvent.BUTTON1_MASK);

        robot.waitForIdle();

        //Drag from center to the outside on up
        to = new Point(from.x,
                       list.getLocationOnScreen().y - 50);

        robot.waitForIdle();
        Util.drag(robot, from, to, InputEvent.BUTTON1_MASK);

    }//End  init()
}
