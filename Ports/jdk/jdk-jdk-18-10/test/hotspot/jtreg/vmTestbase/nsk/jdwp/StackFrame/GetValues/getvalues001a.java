/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdwp.StackFrame.GetValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class getvalues001a {

    // name of the tested object and thread classes
    public static final String OBJECT_CLASS_NAME = "TestedObjectClass";
    public static final String THREAD_CLASS_NAME = "TestedThreadClass";
    public static final String THREAD_NAME = "TestedThreadName";

    // name of the static fields with the tested object values
    public static final String THREAD_FIELD_NAME = "thread";
    public static final String OBJECT_FIELD_NAME = "object";
    public static final String OBJECT_METHOD_NAME = "testedMethod";

    // notification object to notify debuggee that thread is ready
    private static Object threadReady = new Object();
    // lock object to prevent thread from exit
    private static Object threadLock = new Object();

    // scaffold objects
    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        getvalues001a _getvalues001a = new getvalues001a();
        System.exit(getvalues001.JCK_STATUS_BASE + _getvalues001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // make communication pipe to debugger
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        // lock the object to prevent thread from exit
        synchronized (threadLock) {

            // load tested class and create tested thread and object
            log.display("Creating object of tested class");
            TestedObjectClass.object = new TestedObjectClass();
            log.display("Creating tested thread");
            TestedObjectClass.thread = new TestedThreadClass(THREAD_NAME);

            // start the thread and wait for notification from it
            synchronized (threadReady) {
                TestedObjectClass.thread.start();
                try {
                    threadReady.wait();
                    // send debugger signal READY
                    log.display("Sending signal to debugger: " + getvalues001.READY);
                    pipe.println(getvalues001.READY);
                } catch (InterruptedException e) {
                    log.complain("Interruption while waiting for thread started: " + e);
                    pipe.println(getvalues001.ERROR);
                }
            }

            // wait for signal QUIT from debugeer
            log.display("Waiting for signal from debugger: " + getvalues001.QUIT);
            String signal = pipe.readln();
            log.display("Received signal from debugger: " + signal);

            // check received signal
            if (signal == null || !signal.equals(getvalues001.QUIT)) {
                log.complain("Unexpected communication signal from debugee: " + signal
                            + " (expected: " + getvalues001.QUIT + ")");
                log.complain("Debugee FAILED");
                return getvalues001.FAILED;
            }

            // allow started thread to finish
        }

        // wait for tested thread finished
        try {
            log.display("Waiting for tested thread finished");
            TestedObjectClass.thread.join();
        } catch (InterruptedException e) {
            log.complain("Interruption while waiting for tested thread finished:\n\t"
                        + e);
            log.complain("Debugee FAILED");
            return getvalues001.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return getvalues001.PASSED;
    }

    // tested thread class
    public static class TestedThreadClass extends Thread {

        TestedThreadClass(String name) {
            super(name);
        }

        public void run() {
            log.display("Tested thread started");

            // invoke tested method for the tested object from the tested thread
            TestedObjectClass.object.testedMethod();

            log.display("Tested thread finished");
        }

    }

    // tested object class
    public static class TestedObjectClass {

        // field with the tested thread and object values
        public static volatile TestedThreadClass thread = null;
        public static volatile TestedObjectClass object = null;

        public void testedMethod() {
            // local variables
            boolean booleanValue = true;
            byte    byteValue    = (byte)0x0F;
            char    charValue    = 'Z';
            int     intValue     = 100;
            short   shortValue   = (short)10;
            long    longValue    = (long)1000000;
            float   floatValue   = (float)3.14;
            double  doubleValue  = (double)2.8e-12;
            Object  objectValue  = null;

            log.display("Tested frame entered");

            // notify debuggee that tested thread ready for testing
            synchronized (threadReady) {
                threadReady.notifyAll();
            }

            // wait for lock object released
            synchronized (threadLock) {
                log.display("Tested frame dropped");
            }
        }

    }

}
