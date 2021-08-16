/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6451578
  @library ../../../regtesthelpers
  @build Sysout AbstractTest Util
  @summary A mouse listener method happens to process mouse events whose time is in the future.
  @author andrei dmitriev : area=awt.event
  @run main EventTimeInFuture
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.AbstractTest;
import test.java.awt.regtesthelpers.Sysout;
import test.java.awt.regtesthelpers.Util;

public class EventTimeInFuture {

    public static void main(String []s) {
        Frame frame = new SensibleFrame();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Robot robot = Util.createRobot();
        Util.waitForIdle(robot);

        /* The defect may appear on every kind of mouse event: movement, press, etc.
         * so start mouse move from frame's outside. Use small threshhold depending on the
         * frame's size.
         */
        Point start = new Point(frame.getLocationOnScreen().x - frame.getWidth()/5,
                                frame.getLocationOnScreen().y - frame.getHeight()/5);
        Point end = new Point(frame.getLocationOnScreen().x + frame.getWidth() * 6 / 5,
                              frame.getLocationOnScreen().y + frame.getHeight() * 6 / 5);
        System.out.println("start = " + start);
        System.out.println("end = " + end);
        Util.mouseMove(robot, start, end);

        // Start drag inside toplevel.
        start = new Point(frame.getLocationOnScreen().x + frame.getWidth()/2,
                          frame.getLocationOnScreen().y + frame.getHeight()/2);
        end = new Point(frame.getLocationOnScreen().x + frame.getWidth() * 6 / 5,
                        frame.getLocationOnScreen().y + frame.getHeight() * 6 / 5);
        Util.drag(robot, start, end, MouseEvent.BUTTON1_MASK);
    }
}

class SensibleFrame extends Frame implements MouseListener,
    MouseMotionListener{

    public SensibleFrame(){
        super("Is event time in future");
        setPreferredSize(new Dimension(100,100));
        setBackground(Color.white);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    private void traceMouse(String k, MouseEvent e){
        long eventTime = e.getWhen();
        long currTime = System.currentTimeMillis();
        long diff = currTime - eventTime;

        System.out.println(k + " diff is " + diff + ", event is "+ e);

        if (diff < 0){
            AbstractTest.fail(k + " diff is " + diff + ", event = "+e);
        }
    }

    public void mouseMoved(MouseEvent e){
        traceMouse("moved",e);
    }

    public void mouseEntered(MouseEvent e){
        traceMouse("entered",e);
    }
    public void mouseExited(MouseEvent e){
        traceMouse("exited",e);
    }
    public void mouseClicked(MouseEvent e){
        traceMouse("clicked",e);
    }
    public void mousePressed(MouseEvent e){
        traceMouse("pressed",e);
    }
    public void mouseReleased(MouseEvent e){
        traceMouse("released",e);
    }
    public void mouseDragged(MouseEvent e){
        traceMouse("dragged",e);
    }
}
