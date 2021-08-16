/*
 * Copyright 2012 Red Hat, Inc.  All Rights Reserved.
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug      7043963
  @summary  Tests  that the screen location of windows is
            updated properly after a maximize.
  @requires os.family == "linux"
  @modules java.desktop/sun.awt.X11
  @author   Denis Lila
  @library  ../../regtesthelpers
  @build    Util
  @run      main MutterMaximizeTest
*/

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import test.java.awt.regtesthelpers.Util;

@SuppressWarnings("serial")
public class MutterMaximizeTest extends Frame {

    public static void main(String[] args) throws InterruptedException {
        if (Util.getWMID() != Util.MUTTER_WM) {
            System.out.println("This test is only useful on Mutter");
            return;
        }
        MutterMaximizeTest frame = new MutterMaximizeTest();
        frame.addWindowListener(Util.getClosingWindowAdapter());

        //Display the window.
        frame.setSize(500, 500);
        Util.showWindowWait(frame);
        runRobotTest(frame);
    }

    private static void runRobotTest(Frame frame) {
        try {
            Thread robotThread = startRegTest(frame);
            robotThread.start();
            waitForThread(robotThread);
        } finally {
            frame.dispose();
        }
    }

    private static void waitForThread(Thread t) {
        while (t.isAlive()) {
            try {
                t.join();
            } catch (InterruptedException e) {
            }
        }
    }

    private static void sleepFor(long millis) {
        long dT = 0;
        long start = System.nanoTime();
        while (dT < millis) {
            try {
                long toSleep = millis - dT/1000000;
                if (toSleep > 0) {
                    Thread.sleep(toSleep);
                }
                // if this ends without an interrupted exception,
                // that's good enough.
                break;
            } catch (InterruptedException e) {
                long now = System.nanoTime();
                dT = now - start;
            }
        }
    }

    private static void rmove(Robot robot, Point p) {
        robot.mouseMove(p.x, p.y);
    }
    private static void rdown(Robot robot) {
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
    }
    private static void rup(Robot robot) {
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(50);
    }

    public static void click(Robot robot) {
        rdown(robot);
        rup(robot);
    }

    public static void doubleClick(Robot robot) {
        click(robot);
        click(robot);
    }

    private static void dragWindow(Window w, int dx, int dy, Robot robot) {
        Point p = Util.getTitlePoint(w);
        rmove(robot, p);
        rdown(robot);
        p.translate(dx, dy);
        rmove(robot, p);
        rup(robot);
    }

    // f must be visible
    private static Thread startRegTest(final Frame f) {
        Thread robot = new Thread(new Runnable() {
            public void run() {
                Robot r = Util.createRobot();
                dragWindow(f, 100, 100, r);
                // wait for the location to be set.
                sleepFor(2000);

                final Point l2 = f.getLocationOnScreen();

                // double click should maximize the frame
                doubleClick(r);

                // wait for location again.
                sleepFor(2000);
                final Point l3 = f.getLocationOnScreen();
                if (l3.equals(l2)) {
                    throw new RuntimeException("Bad location after maximize. Window location has not moved");
                }
            }
        });
        return robot;
    }
}

