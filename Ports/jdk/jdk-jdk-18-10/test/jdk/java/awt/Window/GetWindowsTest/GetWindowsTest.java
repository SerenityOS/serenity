/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6322270
  @summary Test for new API introduced in the fix for 6322270: Window.getWindows(),
Window.getOwnerlessWindows() and Frame.getFrames()
  @author artem.ananiev: area=awt.toplevel
  @run main GetWindowsTest
*/

import java.awt.*;
import java.awt.event.*;

import java.util.*;

public class GetWindowsTest
{
    private static Vector<Window> frames = new Vector<Window>();
    private static Vector<Window> windows = new Vector<Window>();
    private static Vector<Window> ownerless = new Vector<Window>();

    private static void init()
    {
        Frame f1 = new Frame("F1");
        f1.setBounds(100, 100, 100, 100);
        f1.setVisible(true);
        addToWindowsList(f1);

        Dialog d1 = new Dialog(f1, "D1", Dialog.ModalityType.MODELESS);
        d1.setBounds(120, 120, 100, 100);
        d1.setVisible(true);
        addToWindowsList(d1);

        Window w1 = new Window(d1);
        w1.setBounds(140, 140, 100, 100);
        w1.setVisible(true);
        addToWindowsList(w1);

        Frame f2 = new Frame("F2");
        f2.setBounds(300, 100, 100, 100);
        f2.setVisible(true);
        addToWindowsList(f2);

        Window w2 = new Window(f2);
        w2.setBounds(320, 120, 100, 100);
        w2.setVisible(true);
        addToWindowsList(w2);

        Dialog d2 = new Dialog(f2, "D2", Dialog.ModalityType.MODELESS);
        d2.setBounds(340, 140, 100, 100);
        d2.setVisible(true);
        addToWindowsList(d2);

        Dialog d3 = new Dialog((Frame)null, "D3", Dialog.ModalityType.MODELESS);
        d3.setBounds(500, 100, 100, 100);
        d3.setVisible(true);
        addToWindowsList(d3);

        Dialog d4 = new Dialog(d3, "D4", Dialog.ModalityType.MODELESS);
        d4.setBounds(520, 120, 100, 100);
        d4.setVisible(true);
        addToWindowsList(d4);

        Window w3 = new Window((Frame)null);
        w3.setBounds(700, 100, 100, 100);
        w3.setVisible(true);
        addToWindowsList(w3);

        Window w4 = new Window(w3);
        w4.setBounds(720, 120, 100, 100);
        w4.setVisible(true);
        addToWindowsList(w4);

        try {
            Robot robot = new Robot();
            robot.waitForIdle();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new Error("Unexpected failure");
        }

        Frame[] fl = Frame.getFrames();
        Vector<Window> framesToCheck = new Vector<Window>();
        for (Frame f : fl)
        {
            framesToCheck.add(f);
        }
        checkWindowsList(frames, framesToCheck, "Frame.getFrames()");

        Window[] wl = Window.getWindows();
        Vector<Window> windowsToCheck = new Vector<Window>();
        for (Window w : wl)
        {
            windowsToCheck.add(w);
        }
        checkWindowsList(windows, windowsToCheck, "Window.getWindows()");

        Window[] ol = Window.getOwnerlessWindows();
        Vector<Window> ownerlessToCheck = new Vector<Window>();
        for (Window o : ol)
        {
            ownerlessToCheck.add(o);
        }
        checkWindowsList(ownerless, ownerlessToCheck, "Window.getOwnerlessWindows()");

        GetWindowsTest.pass();
    }

    private static void addToWindowsList(Window w)
    {
        if (w instanceof Frame)
        {
            frames.add(w);
        }
        windows.add(w);
        if (w.getOwner() == null)
        {
            ownerless.add(w);
        }
    }

    private static void checkWindowsList(Vector<Window> wl1, Vector<Window> wl2, String methodName)
    {
        if ((wl1.size() != wl2.size()) ||
            !wl1.containsAll(wl2) ||
            !wl2.containsAll(wl1))
        {
            fail("Test FAILED: method " + methodName + " returns incorrect list of windows");
        }
    }

/*****************************************************
 * Standard Test Machinery Section
 * DO NOT modify anything in this section -- it's a
 * standard chunk of code which has all of the
 * synchronisation necessary for the test harness.
 * By keeping it the same in all tests, it is easier
 * to read and understand someone else's test, as
 * well as insuring that all tests behave correctly
 * with the test harness.
 * There is a section following this for test-
 * classes
 ******************************************************/

    private static boolean theTestPassed = false;
    private static boolean testGeneratedInterrupt = false;
    private static String failureMessage = "";

    private static Thread mainThread = null;

    private static int sleepTime = 300000;

    // Not sure about what happens if multiple of this test are
    //  instantiated in the same VM.  Being static (and using
    //  static vars), it aint gonna work.  Not worrying about
    //  it for now.
    public static void main( String args[] ) throws InterruptedException
    {
        mainThread = Thread.currentThread();
        try
        {
            init();
        }
        catch( TestPassedException e )
        {
            //The test passed, so just return from main and harness will
            // interepret this return as a pass
            return;
        }
        //At this point, neither test pass nor test fail has been
        // called -- either would have thrown an exception and ended the
        // test, so we know we have multiple threads.

        //Test involves other threads, so sleep and wait for them to
        // called pass() or fail()
        try
        {
            Thread.sleep( sleepTime );
            //Timed out, so fail the test
            throw new RuntimeException( "Timed out after " + sleepTime/1000 + " seconds" );
        }
        catch (InterruptedException e)
        {
            //The test harness may have interrupted the test.  If so, rethrow the exception
            // so that the harness gets it and deals with it.
            if( ! testGeneratedInterrupt ) throw e;

            //reset flag in case hit this code more than once for some reason (just safety)
            testGeneratedInterrupt = false;

            if ( theTestPassed == false )
            {
                throw new RuntimeException( failureMessage );
            }
        }
    }

    public static synchronized void setTimeoutTo( int seconds )
    {
        sleepTime = seconds * 1000;
    }

    public static synchronized void pass()
    {
        //first check if this is executing in main thread
        if ( mainThread == Thread.currentThread() )
        {
            //Still in the main thread, so set the flag just for kicks,
            // and throw a test passed exception which will be caught
            // and end the test.
            theTestPassed = true;
            throw new TestPassedException();
        }
        theTestPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static synchronized void fail()
    {
        //test writer didn't specify why test failed, so give generic
        fail( "it just plain failed! :-)" );
    }

    public static synchronized void fail( String whyFailed )
    {
        //check if this called from main thread
        if ( mainThread == Thread.currentThread() )
        {
            //If main thread, fail now 'cause not sleeping
            throw new RuntimeException( whyFailed );
        }
        theTestPassed = false;
        testGeneratedInterrupt = true;
        failureMessage = whyFailed;
        mainThread.interrupt();
    }
}

//This exception is used to exit from any level of call nesting
// when it's determined that the test has passed, and immediately
// end the test.
class TestPassedException extends RuntimeException
{
}

//*********** End Standard Test Machinery Section **********
