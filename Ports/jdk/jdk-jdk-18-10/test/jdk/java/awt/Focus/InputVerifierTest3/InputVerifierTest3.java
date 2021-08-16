/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6432665
  @summary Inputverifier is not executed when focus owner is removed
  @author oleg.sukhodolsky: area=awt.focus
  @library ../../regtesthelpers
  @build Util
  @run main InputVerifierTest3
*/

/**
 * InputVerifierTest3.java
 *
 * summary: Inputverifier is not executed when focus owner is removed
 */

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.Toolkit;

import java.awt.event.InputEvent;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextField;

import test.java.awt.regtesthelpers.Util;

public class InputVerifierTest3
{
    static volatile boolean verifier_called = false;

    private static void init()
    {
        //*** Create instructions for the user here ***

        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        JTextField tf1 = new JTextField(10);
        tf1.setInputVerifier(new InputVerifier() {
                public boolean verify(JComponent input) {
                    System.err.println("verify on " + input);
                    verifier_called = true;
                    return true;
                }
            });
        frame.getContentPane().add(tf1);
        JTextField tf2 = new JTextField(10);
        frame.getContentPane().add(tf2);

        frame.setSize(200, 200);
        frame.setVisible(true);

        Robot r = null;
        try {
            r = new Robot();
        } catch (AWTException e) {
            InputVerifierTest3.fail(e);
        }


        try {
            Util.waitForIdle(r);
            Util.clickOnComp(tf1, r);
            Util.waitForIdle(r);


            if (!tf1.isFocusOwner()) {
                System.out.println("focus owner = " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
                throw new RuntimeException("tf1 is not a focus owner");
            }

            frame.getContentPane().remove(tf1);
            Util.waitForIdle(r);

            if (!tf2.isFocusOwner()) {
                System.out.println("focus owner = " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
                throw new RuntimeException("tf2 is not a focus owner");
            }

            if (!verifier_called) {
                throw new RuntimeException("verifier was not called");
            }

        } catch (Exception e) {
            InputVerifierTest3.fail(e);
        }

        InputVerifierTest3.pass();

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

    public static synchronized void fail( Exception whyFailed )
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
        failureMessage = whyFailed.toString();
        mainThread.interrupt();
    }//fail()

}// class InputVerifierTest3

//This exception is used to exit from any level of call nesting
// when it's determined that the test has passed, and immediately
// end the test.
class TestPassedException extends RuntimeException
{
}
