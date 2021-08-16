/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6200670
  @summary MouseMoved events are triggered by Choice when mouse is moved outside the component, XToolkit
  @library ../../regtesthelpers/
  @build Util
  @run main PopdownGeneratesMouseEvents
*/

import test.java.awt.regtesthelpers.Util;

import java.awt.*;
import java.awt.event.*;

public class PopdownGeneratesMouseEvents extends Frame {
    private volatile Robot robot;
    private final Choice choice1 = new Choice();

    private volatile MouseMotionHandler mmh;

    public static void main(final String[] args) {
        PopdownGeneratesMouseEvents app = new PopdownGeneratesMouseEvents();
        app.init();
        app.start();
    }

    public void init() {
        for (int i = 1; i < 10; i++) {
            choice1.add("item-0" + i);
        }
        choice1.setForeground(Color.RED);
        choice1.setBackground(Color.RED);
        mmh = new MouseMotionHandler();
        choice1.addMouseMotionListener(mmh);
        Button b1 = new Button("FirstButton");
        Button b2 = new Button("SecondButton");
        add(b1);
        add(choice1);
        add(b2);
        setLayout (new FlowLayout());
    }

    public void start() {
        setSize(300, 200);
        setLocationRelativeTo(null);
        setVisible(true);
        validate();
        String toolkit = Toolkit.getDefaultToolkit().getClass().getName();

        /*
         * Choice should not generate MouseEvents outside of Choice
         * Test for XAWT only.
         */
        try{
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
            robot.setAutoDelay(50);

            if (toolkit.equals("sun.awt.X11.XToolkit")) {
                testMouseMoveOutside();
            } else {
                System.out.println("This test is for XToolkit only. Now using "
                                        + toolkit + ". Automatically passed.");
                return;
            }
        } catch (Throwable e) {
            throw new RuntimeException("Test failed. Exception thrown: " + e);
        }
        System.out.println("Passed : Choice should not generate MouseEvents outside of Choice.");
    }

    private void testMouseMoveOutside() {
        waitForIdle();
        Point pt = choice1.getLocationOnScreen();
        robot.mouseMove(pt.x + choice1.getWidth() / 2, pt.y + choice1.getHeight() / 2);
        waitForIdle();
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        waitForIdle();

        Color color = robot.getPixelColor(pt.x + choice1.getWidth() / 2,
                                          pt.y + 3 * choice1.getHeight());
        if (!color.equals(Color.RED)) {
            throw new RuntimeException("Choice wasn't opened with LEFTMOUSE button");
        }

        pt = getLocationOnScreen();
        robot.mouseMove(pt.x + getWidth() * 2, pt.y + getHeight() * 2);
        mmh.testStarted = true;

        int x0 = pt.x + getWidth() * 3 / 2;
        int y0 = pt.y + getHeight() * 3 / 2;
        int x1 = pt.x + getWidth() * 2;
        int y1 = pt.y + getHeight() * 2;

        Util.mouseMove(robot, new Point(x0, y0), new Point(x1, y0));
        Util.mouseMove(robot, new Point(x1, y0), new Point(x1, y1));

        waitForIdle();
        //close opened choice
        robot.keyPress(KeyEvent.VK_ESCAPE);
        robot.keyRelease(KeyEvent.VK_ESCAPE);
    }

    private void waitForIdle() {
        Util.waitForIdle(robot);
        robot.delay(500);
    }
}

class MouseMotionHandler extends MouseMotionAdapter {
    public volatile boolean testStarted;
    public void mouseMoved(MouseEvent ke) {
        if (testStarted) {
            throw new RuntimeException("Test failed: Choice generated MouseMove events while moving mouse outside of Choice");
        }
    }
    public void mouseDragged(MouseEvent ke) {
    }
}
