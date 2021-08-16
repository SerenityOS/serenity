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

package nsk.jdwp.ArrayReference.GetValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

public class getvalues002a {

    public static final String ARRAY_FIELD_NAME = "array";
    public static final int FIELDS_COUNT = 10;
    public static final int ARRAY_FIRST_INDEX = 4;
    public static final int ARRAY_LENGTH = ARRAY_FIRST_INDEX + FIELDS_COUNT + 5;

    public static void main(String args[]) {
        getvalues002a _getvalues002a = new getvalues002a();
        System.exit(getvalues002.JCK_STATUS_BASE + _getvalues002a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        // meke communication pipe to debugger
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        // ensure tested class loaded
        log.display("Creating and fille tested array");
        TestedClass.setArrayValues();

        // send debugger signal READY
        log.display("Sending signal to debugger: " + getvalues002.READY);
        pipe.println(getvalues002.READY);

        // wait for signal QUIT from debugeer
        log.display("Waiting for signal from debugger: " + getvalues002.QUIT);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (! signal.equals(getvalues002.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + getvalues002.QUIT + ")");
            log.display("Debugee FAILED");
            return getvalues002.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return getvalues002.PASSED;
    }

    // tested class with own static fields values
    public static class TestedClass {

        // static field with tested array
        public static Object array[] = null;

        // static fields with object values
        public static Object      nullObject           = null;
        public static Object      baseObject           = new Object();
        public static TestedClass derivedObject        = new TestedClass();
        public static String      stringObject         = new String("string");
        public static int[]       primitiveArrayObject = new int[10];
        public static Object[]    objectArrayObject    = new Object[10];
        public static Thread      threadObject         = Thread.currentThread();
        public static ThreadGroup threadGroupObject    = threadObject.getThreadGroup();
        public static Class       classObject          = derivedObject.getClass();
        public static ClassLoader classLoaderObject    = classObject.getClassLoader();

        public static void setArrayValues() {
            array = new Object[ARRAY_LENGTH];
            for (int i = 0; i < ARRAY_LENGTH; i++) {
                array[i] = null;
            }

            int i = ARRAY_FIRST_INDEX;
            array[i + 0] = nullObject;
            array[i + 1] = baseObject;
            array[i + 2] = derivedObject;
            array[i + 3] = stringObject;
            array[i + 4] = primitiveArrayObject;
            array[i + 5] = objectArrayObject;
            array[i + 6] = threadObject;
            array[i + 7] = threadGroupObject;
            array[i + 8] = classObject;
            array[i + 9] = classLoaderObject;
        }
    }
}
