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
  @bug 4199506
  @summary  java.awt.print.PageFormat.setpaper(Paper paper)
                 assertion test fails by not throwing
                 NullPointerException when a null paper instance is
                 passed as argument and this is specified in the doc.
  @author rbi: area=PageFormat
  @run main NullPaper
*/


//*** global search and replace NullPaper with name of the test ***

/**
 * NullPaper.java
 *
 * summary: java.awt.print.PageFormat.setpaper(Paper paper)
                 assertion test fails by not throwing
                 NullPointerException when a null paper instance is
                 passed as argument and this is specified in the doc.

 */

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;

// This test is a "main" test as applets would need Runtime permission
// "queuePrintJob".

public class NullPaper {

   private static void init()
    {
    boolean settingNullWorked = false;

    try {
        /* Setting the paper to null should throw an exception.
         * The bug was the exception was not being thrown.
         */
        new PageFormat().setPaper(null);
        settingNullWorked = true;

    /* If the test succeeds we'll end up here, so write
     * to standard out.
     */
    } catch (NullPointerException e) {
        pass();

    /* The test failed if we end up here because an exception
     * other than the one we were expecting was thrown.
     */
    } catch (Exception e) {
        fail("Instead of the expected NullPointerException, '" + e + "' was thrown.");
    }

    if (settingNullWorked) {
        fail("The expected NullPointerException was not thrown");
    }

    }//End  init()


   /*****************************************************
     Standard Test Machinery Section
      DO NOT modify anything in this section -- it's a
      standard chunk of code which has all of the
      synchronisation necessary for the test harness.
      By keeping it the same in all tests, it is easier
      to read and understand someone else's test, as
      well as insuring that all tests behave correctly
      with the test harness.
     There is a section following this for test-defined
      classes
   ******************************************************/
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
      catch( TestPassedException e )
       {
         //The test passed, so just return from main and harness will
         // interepret this return as a pass
         return;
       }
      //At this point, neither test passed nor test failed has been
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
      //pass was called from a different thread, so set the flag and interrupt
      // the main thead.
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

 }// class NullPaper

//This exception is used to exit from any level of call nesting
// when it's determined that the test has passed, and immediately
// end the test.
class TestPassedException extends RuntimeException
 {
 }
