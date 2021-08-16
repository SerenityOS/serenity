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

package nsk.jdwp.StackFrame.SetValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class setvalues001a {

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
        setvalues001a _setvalues001a = new setvalues001a();
        System.exit(setvalues001.JCK_STATUS_BASE + _setvalues001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // make communication pipe to debugger
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        // lock the object to prevent thread from running further
        synchronized (threadLock) {

            // load tested class and create tested thread and object
            log.display("Creating object of tested class");
            TestedObjectClass.object = new TestedObjectClass();
            log.display("Creating tested thread");
            TestedObjectClass.thread = new TestedThreadClass(THREAD_NAME);

            OriginalValuesClass original = new OriginalValuesClass();
            TargetValuesClass target = new TargetValuesClass();

            // start the thread and wait for notification from it
            synchronized (threadReady) {
                TestedObjectClass.thread.start();
                try {
                    threadReady.wait();
                    // send debugger signal READY
                    log.display("Sending signal to debugger: " + setvalues001.READY);
                    pipe.println(setvalues001.READY);
                } catch (InterruptedException e) {
                    log.complain("Interruption while waiting for thread started: " + e);
                    pipe.println(setvalues001.ERROR);
                    // exit debuggee
                    log.complain("Debugee FAILED");
                    return setvalues001.FAILED;
                }
            }

            // wait for signal RUN from debugeer
            log.display("Waiting for signal from debugger: " + setvalues001.RUN);
            String signal = pipe.readln();
            log.display("Received signal from debugger: " + signal);

            // check received signal
            if (signal == null || !signal.equals(setvalues001.RUN)) {
                log.complain("Unexpected communication signal from debugee: " + signal
                            + " (expected: " + setvalues001.RUN + ")");
                // skip checking for new values
                TestedObjectClass.object.checking = false;
                // exit debuggee
                log.complain("Debugee FAILED");
                return setvalues001.FAILED;
            }

            // allow started thread to run and finish
        }

        // wait for tested thread finished
        try {
            log.display("Waiting for tested thread finished");
            TestedObjectClass.thread.join();
        } catch (InterruptedException e) {
            log.complain("Interruption while waiting for tested thread finished:\n\t"
                        + e);
            // exit debuggee
            log.complain("Debugee FAILED");
            return setvalues001.FAILED;
        }

        // confirm that new values of local variables are correct
        if (TestedObjectClass.object.different == 0) {
            log.display("Sending signal to debugger: " + setvalues001.DONE);
            pipe.println(setvalues001.DONE);
        } else {
            log.complain("Values of " + TestedObjectClass.object.different +
                        " local variables have not been set correctly");
            log.display("Sending signal to debugger: " + setvalues001.ERROR);
            pipe.println(setvalues001.ERROR);
        }

        // wait for signal QUIT from debugeer
        log.display("Waiting for signal from debugger: " + setvalues001.QUIT);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (signal == null || !signal.equals(setvalues001.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + setvalues001.QUIT + ")");
            // exit debuggee
            log.complain("Debugee FAILED");
            return setvalues001.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return setvalues001.PASSED;
    }

    // tested thread class
    public static class TestedThreadClass extends Thread {

        public TestedThreadClass(String name) {
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

        // allow to check new values of local variables
        public volatile boolean checking = true;
        // number of variables with unexpected new values
        public volatile int different = 0;

        // tested method with local variables
        public void testedMethod() {

            // local variables
            boolean  booleanValue = OriginalValuesClass.booleanValue;
            byte     byteValue    = OriginalValuesClass.byteValue;
            char     charValue    = OriginalValuesClass.charValue;
            int      intValue     = OriginalValuesClass.intValue;
            short    shortValue   = OriginalValuesClass.shortValue;
            long     longValue    = OriginalValuesClass.longValue;
            float    floatValue   = OriginalValuesClass.floatValue;
            double   doubleValue  = OriginalValuesClass.doubleValue;
            String   stringValue  = OriginalValuesClass.stringValue;
            Object   objectValue  = OriginalValuesClass.objectValue;

            log.display("Tested frame entered");

            // notify debuggee that tested thread ready for testing
            synchronized (threadReady) {
                threadReady.notifyAll();
            }

            // wait for lock object released
            synchronized (threadLock) {
                log.display("Checking that values have been set correctly:");
            }

            // check new values of local variables
            if (checking) {

                // check value of the variable
                if (booleanValue != TargetValuesClass.booleanValue) {
                    different++;
                    log.complain("  booleanValue = " + booleanValue + "\n"
                               + "    setting: " + OriginalValuesClass.booleanValue
                                        + " -> " + TargetValuesClass.booleanValue);
                    if (booleanValue == OriginalValuesClass.booleanValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  booleanValue: " + OriginalValuesClass.booleanValue
                                            + " -> " + TargetValuesClass.booleanValue);
                }

                // check value of the variable
                if (byteValue != TargetValuesClass.byteValue) {
                    different++;
                    log.complain("  byteValue = " + byteValue + "\n"
                               + "    setting: " + OriginalValuesClass.byteValue
                                        + " -> " + TargetValuesClass.byteValue);
                    if (byteValue == OriginalValuesClass.byteValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  byteValue: " + OriginalValuesClass.byteValue
                                            + " -> " + TargetValuesClass.byteValue);
                }

                // check value of the variable
                if (charValue != TargetValuesClass.charValue) {
                    different++;
                    log.complain("  charValue = " + charValue + "\n"
                               + "    setting: " + OriginalValuesClass.charValue
                                        + " -> " + TargetValuesClass.charValue);
                    if (charValue == OriginalValuesClass.charValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  charValue: " + OriginalValuesClass.charValue
                                            + " -> " + TargetValuesClass.charValue);
                }

                // check value of the variable
                if (intValue != TargetValuesClass.intValue) {
                    different++;
                    log.complain("  intValue = " + intValue + "\n"
                               + "    setting: " + OriginalValuesClass.intValue
                                        + " -> " + TargetValuesClass.intValue);
                    if (intValue == OriginalValuesClass.intValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  intValue: " + OriginalValuesClass.intValue
                                            + " -> " + TargetValuesClass.intValue);
                }

                // check value of the variable
                if (shortValue != TargetValuesClass.shortValue) {
                    different++;
                    log.complain("  shortValue = " + shortValue + "\n"
                               + "    setting: " + OriginalValuesClass.shortValue
                                        + " -> " + TargetValuesClass.shortValue);
                    if (shortValue == OriginalValuesClass.shortValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  shortValue: " + OriginalValuesClass.shortValue
                                            + " -> " + TargetValuesClass.shortValue);
                }

                // check value of the variable
                if (longValue != TargetValuesClass.longValue) {
                    different++;
                    log.complain("  longValue = " + longValue + "\n"
                               + "    setting: " + OriginalValuesClass.longValue
                                        + " -> " + TargetValuesClass.longValue);
                    if (longValue == OriginalValuesClass.longValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  longValue: " + OriginalValuesClass.longValue
                                            + " -> " + TargetValuesClass.longValue);
                }

                // check value of the variable
                if (floatValue != TargetValuesClass.floatValue) {
                    different++;
                    log.complain("  floatValue = " + floatValue + "\n"
                               + "    setting: " + OriginalValuesClass.floatValue
                                        + " -> " + TargetValuesClass.floatValue);
                    if (floatValue == OriginalValuesClass.floatValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  floatValue: " + OriginalValuesClass.floatValue
                                            + " -> " + TargetValuesClass.floatValue);
                }

                // check value of the variable
                if (doubleValue != TargetValuesClass.doubleValue) {
                    different++;
                    log.complain("  doubleValue = " + doubleValue + "\n"
                               + "    setting: " + OriginalValuesClass.doubleValue
                                        + " -> " + TargetValuesClass.doubleValue);
                    if (doubleValue == OriginalValuesClass.doubleValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  doubleValue: " + OriginalValuesClass.doubleValue
                                            + " -> " + TargetValuesClass.doubleValue);
                }

                // check value of the variable
                if (stringValue != TargetValuesClass.stringValue) {
                    different++;
                    log.complain("  stringValue = " + stringValue + "\n"
                               + "    setting: " + OriginalValuesClass.stringValue
                                        + " -> " + TargetValuesClass.stringValue);
                    if (stringValue == OriginalValuesClass.stringValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  stringValue: " + OriginalValuesClass.stringValue
                                            + " -> " + TargetValuesClass.stringValue);
                }

                // check value of the variable
                if (objectValue != TargetValuesClass.objectValue) {
                    different++;
                    log.complain("  objectValue = " + objectValue + "\n"
                               + "    setting: " + OriginalValuesClass.objectValue
                                        + " -> " + TargetValuesClass.objectValue);
                    if (objectValue == OriginalValuesClass.objectValue) {
                        log.complain("      not changed!");
                    } else {
                        log.complain("      changed incorrectly!");
                    }
                } else {
                    log.display("  objectValue: " + OriginalValuesClass.objectValue
                                            + " -> " + TargetValuesClass.objectValue);
                }
            }

            log.display("Tested frame dropped");
        }

    }

    // class with the original values of static fields
    public static class OriginalValuesClass {
        static final boolean booleanValue = true;
        static final byte    byteValue    = (byte)0x01;
        static final char    charValue    = 'Z';
        static final int     intValue     = 100;
        static final short   shortValue   = (short)10;
        static final long    longValue    = (long)1000000;
        static final float   floatValue   = (float)3.14;
        static final double  doubleValue  = (double)2.8e-12;
        static final String  stringValue  = "text";
        static final Object  objectValue  = new OriginalValuesClass();
    }

    // class with the original values of static fields
    public static class TargetValuesClass {
        static final boolean booleanValue = false;
        static final byte    byteValue    = (byte)0x0F;
        static final char    charValue    = 'A';
        static final int     intValue     = 999;
        static final short   shortValue   = (short)88;
        static final long    longValue    = (long)11111111;
        static final float   floatValue   = (float)7.19;
        static final double  doubleValue  = (double)4.6e24;
        static final String  stringValue  = "new text";
        static final Object  objectValue  = new TargetValuesClass();
    }

}
