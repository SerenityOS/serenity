/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
  @bug 5025858 8144125
  @summary Tests that after programmatically moving or resizing a component,
           corresponding ComponentEvents are generated only once
*/

import java.awt.Button;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/*
  IMPORTANT: this test can fail because some window managers may generate
  strange events and that would lead to the test to fail. However, on most
  popular platforms (windows, linux w/ KDE or GNOME, solaris w/ CDE or
  GNOME) it usually passes successfully.
*/

public class MovedResizedTwiceTest
{
    private static volatile int componentMovedCount;
    private static volatile int componentResizedCount;

    private static volatile int rightX, rightY;

    private static volatile boolean failed = false;

    private static void init()
    {
        componentMovedCount = componentResizedCount = 0;

        Robot robot;
        try {
            robot = new Robot();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Cannot create Robot: failure");
        }

        Frame f = new Frame("Frame F");
        f.setLayout(null);
        f.setBounds(100, 100, 100, 100);
        f.add(new Button("Button"));

        f.setVisible(true);
        robot.waitForIdle();

        ComponentListener cl = new ComponentAdapter()
        {
            public void componentMoved(ComponentEvent e)
            {
                componentMovedCount++;
                Component c = (Component)e.getSource();
                if (!(c instanceof Window))
                {
                    return;
                }
                Point p = c.getLocationOnScreen();
                if ((p.x != rightX) || (p.y != rightY))
                {
                    System.err.println("Error: wrong location on screen after COMPONENT_MOVED");
                    System.err.println("Location on screen is (" + p.x + ", " + p.y + ") against right location (" + rightX + ", " + rightY + ")");
                    failed = true;
                }
            }
            public void componentResized(ComponentEvent e)
            {
                componentResizedCount++;
            }
        };

        f.addComponentListener(cl);

        componentResizedCount = componentMovedCount = 0;
        rightX = 100;
        rightY = 100;
        f.setSize(200, 200);
        robot.waitForIdle();
        checkResized("setSize", f);

        componentResizedCount = componentMovedCount = 0;
        rightX = 200;
        rightY = 200;
        f.setLocation(200, 200);
        robot.waitForIdle();
        checkMoved("setLocation", f);

        componentResizedCount = componentMovedCount = 0;
        rightX = 150;
        rightY = 150;
        f.setBounds(150, 150, 100, 100);
        robot.waitForIdle();
        checkResized("setBounds", f);
        checkMoved("setBounds", f);

        Button b = new Button("B");
        b.setBounds(10, 10, 40, 40);
        f.add(b);
        robot.waitForIdle();

        b.addComponentListener(cl);

        componentResizedCount = componentMovedCount = 0;
        b.setBounds(20, 20, 50, 50);
        robot.waitForIdle();
        checkMoved("setBounds", b);
        checkResized("setBounds", b);
        f.remove(b);

        Component c = new Component() {};
        c.setBounds(10, 10, 40, 40);
        f.add(c);
        robot.waitForIdle();

        c.addComponentListener(cl);

        componentResizedCount = componentMovedCount = 0;
        c.setBounds(20, 20, 50, 50);
        robot.waitForIdle();
        checkMoved("setBounds", c);
        checkResized("setBounds", c);
        f.remove(c);

        if (failed)
        {
            MovedResizedTwiceTest.fail("Test FAILED");
        }
        else
        {
            MovedResizedTwiceTest.pass();
        }
    }

    private static void checkResized(String methodName, Component c)
    {
        String failMessage = null;
        if (componentResizedCount == 1)
        {
            return;
        }
        else if (componentResizedCount == 0)
        {
            failMessage = "Test FAILED: COMPONENT_RESIZED is not sent after call to " + methodName + "()";
        }
        else
        {
            failMessage = "Test FAILED: COMPONENT_RESIZED is sent " + componentResizedCount + " + times after call to " + methodName + "()";
        }
        System.err.println("Failed component: " + c);
        MovedResizedTwiceTest.fail(failMessage);
    }

    private static void checkMoved(String methodName, Component c)
    {
        String failMessage = null;
        if (componentMovedCount == 1)
        {
            return;
        }
        if (componentMovedCount == 0)
        {
            failMessage = "Test FAILED: COMPONENT_MOVED is not sent after call to " + methodName + "()";
        }
        else
        {
            failMessage = "Test FAILED: COMPONENT_MOVED is sent " + componentMovedCount + " times after call to " + methodName + "()";
        }
        System.err.println("Failed component: " + c);
        MovedResizedTwiceTest.fail(failMessage);
    }

    private static boolean theTestPassed = false;
    private static boolean testGeneratedInterrupt = false;
    private static String failureMessage = "";

    private static Thread mainThread = null;

    private static int sleepTime = 300000;

    public static void main( String args[] ) throws InterruptedException
    {
        mainThread = Thread.currentThread();
        try
        {
            init();
        }
        catch (TestPassedException e)
        {
            return;
        }

        try
        {
            Thread.sleep(sleepTime);
            throw new RuntimeException( "Timed out after " + sleepTime/1000 + " seconds" );
        }
        catch (InterruptedException e)
        {
            if (!testGeneratedInterrupt)
            {
                throw e;
            }

            testGeneratedInterrupt = false;

            if (!theTestPassed)
            {
                throw new RuntimeException( failureMessage );
            }
        }
    }

    public static synchronized void setTimeoutTo(int seconds)
    {
        sleepTime = seconds * 1000;
    }

    public static synchronized void pass()
    {
        if (mainThread == Thread.currentThread())
        {
            theTestPassed = true;
            throw new TestPassedException();
        }
        theTestPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static synchronized void fail()
    {
        fail("it just plain failed! :-)");
    }

    public static synchronized void fail(String whyFailed)
    {
        if (mainThread == Thread.currentThread())
        {
            throw new RuntimeException(whyFailed);
        }
        theTestPassed = false;
        testGeneratedInterrupt = true;
        failureMessage = whyFailed;
        mainThread.interrupt();
    }
}

class TestPassedException extends RuntimeException
{
}
