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
  @bug 6533330
  @summary ANCESTOR_RESIZED is not sent while resizing a frame. Regression caused by 6500477.
  @author anthony.petrov: area=awt.toplevel
  @library ../../../regtesthelpers
  @build Util
  @run main AncestorResized
*/


/**
 * AncestorResized.java
 *
 * summary:  After fixing the 6500477, the ANCESTOR_RESIZED event stoped
 *           firing while resizing a frame. This was a regression.
 *           The test checks whether the event starts dispatching as it
 *           was before fixing the 6500477.
 */

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;


public class AncestorResized
{
    public static volatile int ancestorResizedCounter = 0;


    static class HierarchyBoundsListenerImpl implements HierarchyBoundsListener {
        public void ancestorMoved(HierarchyEvent ce) {
            // ANCESTOR_MOVED seems to work OK.
        }
        public void ancestorResized(HierarchyEvent ce) {
            ancestorResizedCounter++;
        }
    }

    private static void init()
    {
        Frame frame;
        Panel panel;
        Button button;
        Label label;
        Component[] components;

        frame = new Frame("Test Frame");
        frame.setLayout(new FlowLayout());

        panel = new Panel();
        button = new Button("Button");
        label = new Label("Label");

        components = new Component[] {
            panel, button, label
        };

        frame.setSize(300, 300);
        frame.setVisible(true);

        Robot robot = Util.createRobot();
        robot.setAutoDelay(20);

        // To ensure the window is shown and packed
        Util.waitForIdle(robot);

        Insets insets = frame.getInsets();
        if (insets.right == 0 || insets.bottom == 0) {
            // Because we want to catch the "size-grip" of the frame.
            System.out.println("The test environment must have non-zero right & bottom insets! The current insets are: " + insets);
            pass();
            return;
        }

        // Let's move the mouse pointer to the bottom-right coner of the frame (the "size-grip")
        Rectangle bounds = frame.getBounds();

        robot.mouseMove(bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);

        // From now on the ANCESTOR_RESIZED events get counted.
        HierarchyBoundsListener listener = new HierarchyBoundsListenerImpl();
        for (int i = 0; i < components.length; i++) {
            components[i].addHierarchyBoundsListener(listener);
            frame.add(components[i]);
        }

        // ... and start resizing
        robot.mousePress( InputEvent.BUTTON1_MASK );
        robot.mouseMove(bounds.x + bounds.width + 20, bounds.y + bounds.height + 15);
        Util.waitForIdle(robot);

        if (ancestorResizedCounter == 0) {
            robot.mouseRelease( InputEvent.BUTTON1_MASK );
            AncestorResized.fail("No ANCESTOR_RESIZED events received.");
            return;
        }

        robot.mouseRelease( InputEvent.BUTTON1_MASK );

        AncestorResized.pass();
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

}// class AncestorResized

//This exception is used to exit from any level of call nesting
// when it's determined that the test has passed, and immediately
// end the test.
class TestPassedException extends RuntimeException
{
}

//*********** End Standard Test Machinery Section **********


//************ Begin classes defined for the test ****************

// if want to make listeners, here is the recommended place for them, then instantiate
//  them in init()

/* Example of a class which may be written as part of a test
class NewClass implements anInterface
 {
   static int newVar = 0;

   public void eventDispatched(AWTEvent e)
    {
      //Counting events to see if we get enough
      eventCount++;

      if( eventCount == 20 )
       {
         //got enough events, so pass

         AncestorResized.pass();
       }
      else if( tries == 20 )
       {
         //tried too many times without getting enough events so fail

         AncestorResized.fail();
       }

    }// eventDispatched()

 }// NewClass class

*/


//************** End classes defined for the test *******************
