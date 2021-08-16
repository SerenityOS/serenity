/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6178755
  @summary The test checks that Container's method startLWModal
and stopLWModal work correctly. The test scenario is very close
to JOptionPane.showInternal*Dialog methods
  @modules java.desktop/java.awt:open
  @author artem.ananiev@...: area=awt.modal
  @library ../../regtesthelpers
  @build Util
  @run main LWModalTest
*/

import java.awt.*;
import java.awt.event.*;

import java.lang.reflect.*;

import javax.swing.*;

import test.java.awt.regtesthelpers.Util;

public class LWModalTest
{
    private static JFrame frame;
    private static volatile JInternalFrame internalFrame;

    private static volatile boolean passed = false;

    private static void init()
    {
        frame = new JFrame("JFrame");
        frame.setBounds(100, 100, 320, 240);
        frame.setVisible(true);
        Util.waitForIdle(null);

        new Thread(new Runnable()
        {
            public void run()
            {
                JOptionPane p = new JOptionPane("Message");
                internalFrame = p.createInternalFrame(frame.getContentPane(), "Title");
                internalFrame.setVisible(true);
                try
                {
                    Method m = Container.class.getDeclaredMethod("startLWModal", (Class[])null);
                    m.setAccessible(true);
                    m.invoke(internalFrame, (Object[])null);
                }
                catch (Exception z)
                {
                    z.printStackTrace(System.err);
                    LWModalTest.fail(z.getMessage());
                    return;
                }
                passed = true;
            }
        }).start();

        try
        {
            Thread.sleep(3000);
            Util.waitForIdle(null);

            internalFrame.dispose();

            Method m = Container.class.getDeclaredMethod("stopLWModal", (Class[])null);
            m.setAccessible(true);
            m.invoke(internalFrame, (Object[])null);

            Thread.sleep(3000);
            Util.waitForIdle(null);
        }
        catch (Exception z)
        {
            z.printStackTrace(System.err);
            LWModalTest.fail(z.getMessage());
            return;
        }

        if (passed)
        {
            LWModalTest.pass();
        }
        else
        {
            LWModalTest.fail("showInternalMessageDialog() has not returned");
        }
    }

    private static boolean theTestPassed = false;
    private static boolean testGeneratedInterrupt = false;
    private static String failureMessage = "";

    private static Thread mainThread = null;

    private static int sleepTime = 60000;

    public static void main(String args[])
        throws InterruptedException
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
            Thread.sleep( sleepTime );
            throw new RuntimeException("Timed out after " + sleepTime/1000 + " seconds");
        }
        catch (InterruptedException e)
        {
            if(!testGeneratedInterrupt) throw e;

            testGeneratedInterrupt = false;

            if (theTestPassed == false)
            {
                throw new RuntimeException(failureMessage);
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
