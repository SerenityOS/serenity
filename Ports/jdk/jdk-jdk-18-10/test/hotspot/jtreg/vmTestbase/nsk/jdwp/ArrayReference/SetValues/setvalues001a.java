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

package nsk.jdwp.ArrayReference.SetValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

public class setvalues001a {

    // name of the static field with the tested array object
    public static final String ARRAY_FIELD_NAME = "array";

    // length, first index and number of array components to get
    public static final int ARRAY_LENGTH = 16;
    public static final int ARRAY_FIRST_INDEX = 4;
    public static final int ARRAY_ITEMS_COUNT = 10;

    private static ArgumentHandler argumentHandler = null;
    private static Log log = null;

    public static void main(String args[]) {
        setvalues001a _setvalues001a = new setvalues001a();
        System.exit(setvalues001.JCK_STATUS_BASE + _setvalues001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // meke communication pipe to debugger
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        // ensure tested class loaded
        log.display("Creating and initializing tested array");
        TestedClass.initArrayValues();

        // send debugger signal READY
        log.display("Sending signal to debugger: " + setvalues001.READY);
        pipe.println(setvalues001.READY);

        // wait for signal RUN from debugeer
        log.display("Waiting for signal from debugger: " + setvalues001.RUN);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);
        // check received signal
        if (signal == null || !signal.equals(setvalues001.RUN)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + setvalues001.RUN + ")");
            log.display("Debugee FAILED");
            return setvalues001.FAILED;
        }

        // check assigned values
        log.display("Checking new array values");
        if (TestedClass.checkArrayValues()) {
            log.display("Sending signal to debugger: " + setvalues001.DONE);
            pipe.println(setvalues001.DONE);
        } else {
            log.display("Sending signal to debugger: " + setvalues001.ERROR);
            pipe.println(setvalues001.ERROR);
        }

        // wait for signal QUIT from debugeer
        log.display("Waiting for signal from debugger: " + setvalues001.QUIT);
        signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (! signal.equals(setvalues001.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + setvalues001.QUIT + ")");
            log.display("Debugee FAILED");
            return setvalues001.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return setvalues001.PASSED;
    }

    // tested class with own static fields values
    public static class TestedClass {

        // static field with tested array
        public static int array[] = null;

        public static void initArrayValues() {
            array = new int[ARRAY_LENGTH];
            for (int i = 0; i < ARRAY_LENGTH; i++) {
                array[i] = i * 10;
            }
        }

        public static boolean checkArrayValues() {
            if (array == null) {
                log.complain("Checked array == null after setting values: " + array);
                return false;
            }

            boolean success = true;
            if (array.length != ARRAY_LENGTH) {
                log.complain("Unexpected array length after setting values: "
                            + array.length + " (expected: " + ARRAY_LENGTH + ")");
                success = false;
            }

            for (int i = 0; i < array.length; i++) {
                int initial = i * 10;
                int changed = i * 100 + 1;
                if (i < ARRAY_FIRST_INDEX || i >= ARRAY_FIRST_INDEX + ARRAY_ITEMS_COUNT) {
                    log.display("  " + i + " (not changed): " + initial + " -> " + array[i]);
                    if (array[i] != initial) {
                        log.complain("Changed value of " + i + " component which is out of changed region: "
                                    + array[i] + "(initial: " + initial + ")");
                        success = false;
                    }
                } else {
                    log.display("  " + i + " (changed): " + initial + " -> " + array[i]);
                    if (array[i] != changed) {
                        if (array[i] == initial) {
                            log.complain("Value of " + i + " component not changed: "
                                    + array[i] + "(expected: " + changed + ")");
                            success = false;
                        } else {
                            log.complain("Value of " + i + " component changed incorrectly: "
                                    + array[i] + "(expected: " + changed + ")");
                            success = false;
                        }
                    }
                }
            }

            return success;
        }

    }
}
