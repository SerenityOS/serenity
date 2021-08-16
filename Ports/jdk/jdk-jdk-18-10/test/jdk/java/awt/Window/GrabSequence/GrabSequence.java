/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6273031
  @summary  PIT. Choice drop down does not close once it is right clicked to show a popup menu
  @author andrei.dmitriev: area=awt.window
  @library ../../regtesthelpers
  @build Util
  @run main GrabSequence
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class GrabSequence
{
    private static void init()
    {
        String toolkit = Toolkit.getDefaultToolkit().getClass().getName();
        if ( toolkit.equals("sun.awt.motif.MToolkit")){
            System.out.println("This test is for XToolkit and WToolkit only. Now using " + toolkit + ". Automatically passed.");
            return;
        }
        Frame frame = new Frame("Frame");
        frame.setBackground(Color.green);
        frame.setForeground(Color.green);
        frame.setLayout(new FlowLayout());
        Choice ch = new Choice();
        ch.setBackground(Color.red);
        ch.setForeground(Color.red);
        ch.addItem("One");
        ch.addItem("Two");
        ch.addItem("Three");
        ch.addItem("Four");
        ch.addItem("Five");
        final PopupMenu pm = new PopupMenu();

        MenuItem i1 = new MenuItem("Item1");
        MenuItem i2 = new MenuItem("Item2");
        MenuItem i3 = new MenuItem("Item3");
        pm.add(i1);
        pm.add(i2);
        pm.add(i3);
        ch.add(pm);
        ch.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    System.out.println("mousePressed"+event);
                    pm.show((Choice)event.getSource(), event.getX(), event.getY());
                }
            }

            public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    System.out.println("mouseReleased"+event);
                    pm.show((Choice)event.getSource(), event.getX(), event.getY());
                }
            }
        });
        frame.add(ch);
        frame.setSize(200, 200);


        frame.setVisible(true);

        try {
            Robot robot = new Robot();
            Util.waitForIdle(robot);
            robot.mouseMove(ch.getLocationOnScreen().x + ch.getWidth()/2,
                            ch.getLocationOnScreen().y + ch.getHeight()/2 );
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(100);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Util.waitForIdle(robot);

            Color color = robot.getPixelColor(ch.getLocationOnScreen().x + ch.getWidth()/2,
                                              ch.getLocationOnScreen().y + ch.getHeight()*4);
            System.out.println("1. Color is " + color);
            if (!color.equals(Color.red)){
                GrabSequence.fail("Test failed. Choice is still not opened. ");
            }

            robot.mousePress(InputEvent.BUTTON3_MASK);
            robot.delay(100);
            robot.mouseRelease(InputEvent.BUTTON3_MASK);
            Util.waitForIdle(robot);

            color = robot.getPixelColor(ch.getLocationOnScreen().x + ch.getWidth()/2,
                                              ch.getLocationOnScreen().y + ch.getHeight()*2);
            System.out.println("2. Color is " + color);
            if (color.equals(Color.green)){
                GrabSequence.fail("Test failed. popup menu is not opened. ");
            }

            color = robot.getPixelColor(ch.getLocationOnScreen().x + ch.getWidth()/2,
                                              ch.getLocationOnScreen().y + ch.getHeight()*4);
            System.out.println("3. Color is " + color);
            if (!color.equals(Color.green) && !Toolkit.getDefaultToolkit().getClass().getName().equals("sun.awt.windows.WToolkit")){
                GrabSequence.fail("Test failed. Choice is still opened. ");
            }
        } catch(AWTException e){
            GrabSequence.fail("Test interrupted."+e);
        }

        GrabSequence.pass();
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

}// class

//This exception is used to exit from any level of call nesting
// when it's determined that the test has passed, and immediately
// end the test.
class TestPassedException extends RuntimeException
{
}
