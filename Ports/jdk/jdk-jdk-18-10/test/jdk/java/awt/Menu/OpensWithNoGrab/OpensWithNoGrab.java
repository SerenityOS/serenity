/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6354721
  @summary REG: Menu does not disappear when clicked, keeping Choice's drop-down open, XToolkit
  @author andrei.dmitriev: area=awt.menu
  @library ../../regtesthelpers
  @library /test/lib
  @modules java.desktop/sun.awt
  @build jdk.test.lib.Platform
  @build Util
  @run main OpensWithNoGrab
*/

import java.awt.*;
import java.awt.event.*;

import jdk.test.lib.Platform;
import test.java.awt.regtesthelpers.Util;

public class OpensWithNoGrab
{
    final static int delay = 50;
    private static void init()
    {
        if (!Platform.isLinux()) {
            System.out.println("This test is for XAWT/Motif only");
            OpensWithNoGrab.pass();
        }

        Choice ch = new Choice ();
        Frame f = new Frame ("OpensWithNoGrab");
        Robot robot;
        Point framePt, choicePt;

        ch.add("line 1");
        ch.add("line 2");
        ch.add("line 3");
        ch.add("line 4");
        ch.setBackground(Color.red);
        f.add(ch);

        Menu file = new Menu ("file");
        MenuBar mb = new MenuBar();
        mb.add(file);

        file.add(new MenuItem ("            "));
        file.add(new MenuItem ("            "));
        file.add(new MenuItem ("            "));
        file.add(new MenuItem ("            "));
        file.add(new MenuItem ("            "));
        file.add(new MenuItem ("            "));
        file.add(new MenuItem ("            "));

        f.setMenuBar(mb);

        f.setBackground(Color.green);
        f.setForeground(Color.green);
        f.setSize(300, 200);
        f.setVisible(true);
        try {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
            robot.setAutoDelay(50);

            Util.waitForIdle(robot);
            // press on Choice
            choicePt = ch.getLocationOnScreen();
            robot.mouseMove(choicePt.x + ch.getWidth()/2, choicePt.y + ch.getHeight()/2);
            robot.delay(delay);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(delay);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.delay(delay);

            // press on Menu
            framePt = f.getLocationOnScreen();
            robot.mouseMove(choicePt.x + 10, choicePt.y - 15);
            robot.delay(10*delay);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(delay);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.delay(delay);

            robot.mouseMove(choicePt.x + 15, choicePt.y + 15);
            Util.waitForIdle(robot);

            Color c = robot.getPixelColor(choicePt.x + 15, choicePt.y + 15);
            System.out.println("Color obtained under opened menu is: "+c );
            if (!c.equals(Color.red)){
                OpensWithNoGrab.fail("Failed: menu was opened by first click after opened Choice.");
            }
        }catch(Exception e){
            e.printStackTrace();
            OpensWithNoGrab.fail("Failed: exception occur "+e);
        }
        OpensWithNoGrab.pass();
    }//End  init()



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

    }//main

    public static synchronized void setTimeoutTo( int seconds )
    {
        sleepTime = seconds * 1000;
    }

    public static synchronized void pass()
    {
        System.out.println( "The test passed." );
        System.out.println( "The test is over, hit  Ctl-C to stop Java VM" );
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
    }//pass()

    public static synchronized void fail()
    {
        //test writer didn't specify why test failed, so give generic
        fail( "it just plain failed! :-)" );
    }

    public static synchronized void fail( String whyFailed )
    {
        System.out.println( "The test failed: " + whyFailed );
        System.out.println( "The test is over, hit  Ctl-C to stop Java VM" );
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
    }//fail()

}// class OpensWithNoGrab

//This exception is used to exit from any level of call nesting
// when it's determined that the test has passed, and immediately
// end the test.
class TestPassedException extends RuntimeException
{
}
