/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4874070
  @summary Tests basic DnD functionality
  @run main ImageDecoratedDnDNegative
*/

import java.awt.*;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;


import java.awt.dnd.DragSource;

/*
    "Automatic test.",
    "A Frame, which contains a yellow button labeled \"Drag ME!\" and ",
    "a red panel, will appear below. ",
    "1. The button would be clicked and dragged to the red panel. ",
    "2. When the mouse enters the red panel during the drag, the panel ",
    "should turn yellow. On the systems that supports pictured drag, ",
    "the image under the drag-cursor should appear (ancor is shifted ",
    "from top-left corner of the picture inside the picture to 10pt in both dimensions ). ",
    "In WIN32 systems the image under cursor would be visible ONLY over ",
    "the drop targets with activated extended OLE D\'n\'D support (that are ",
    "the desktop and IE ).",
    "3. The mouse would be released.",
    "The panel should turn red again and a yellow button labeled ",
    "\"Drag ME!\" should appear inside the panel. You should be able ",
    "to repeat this operation multiple times."
 */
public class ImageDecoratedDnDNegative {

    public static void moveTo(
        Robot r,
        Point b,
        Point e)
    {
        Point2D.Double ee = new Point2D.Double(e.getX(), e.getY());
        Point2D.Double bb = new Point2D.Double(b.getX(), b.getY());
        final int count = (int)(ee.distance(bb));
        Point2D.Double c = new Point2D.Double(bb.getX(), bb.getY());
        for(int i=0; i<count; ++i){
            c.setLocation(
                    bb.getX() + (ee.getX()-bb.getX())*i/count,
                    bb.getY() + (ee.getY()-bb.getY())*i/count);
            r.mouseMove(
                    (int)c.getX(),
                    (int)c.getY());
            r.delay(5);
        }
        r.mouseMove(
                (int)ee.getX(),
                (int)ee.getY());
        r.delay(5);
    }

    public static void main(final String[] args) {
        Frame f = new Frame("Use keyboard for DnD change");
        Panel mainPanel;
        Component dragSource, dropTarget;

        f.setBounds(0, 400, 200, 200);
        f.setLayout(new BorderLayout());

        mainPanel = new Panel();
        mainPanel.setLayout(new BorderLayout());

        mainPanel.setBackground(Color.blue);

        dropTarget = new DnDTarget(Color.red, Color.yellow);
        dragSource = new DnDSource("Drag ME! (" + (DragSource.isDragImageSupported()?"with ":"without") + " image)" );

        mainPanel.add(dragSource, "North");
        mainPanel.add(dropTarget, "Center");
        f.add(mainPanel, BorderLayout.CENTER);
        f.setUndecorated(true);
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        Point sourcePoint = dragSource.getLocationOnScreen();
        Dimension d = dragSource.getSize();
        sourcePoint.translate(d.width / 2, d.height / 2);

        try {
            Robot robot = new Robot();
            robot.waitForIdle();
            robot.mouseMove(sourcePoint.x, sourcePoint.y);
            Point start = new Point(
                    sourcePoint.x,
                    sourcePoint.y);
            Point out = new Point(
                    sourcePoint.x + d.width / 2 + 10,
                    sourcePoint.y + d.height);

            Point cur = start;
            for(int i = 2; i < 5; ++i){
                moveTo(robot, cur, start);
                robot.delay(500);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.delay(500);
                moveTo(robot, start, out);
                robot.keyPress(KeyEvent.VK_CONTROL);
                Point drop = new Point(
                        (int)start.getX(),
                        (int)start.getY() + (d.height + 5) * i );
                moveTo(robot, out, drop);

                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                robot.delay(10);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                robot.delay(1000);

                cur = drop;
            }
        } catch( Exception e){
            e.printStackTrace();
            throw new RuntimeException("test failed: drop was not successful with exception " + e);
        }
    }
}// class DnDAcceptanceTest
