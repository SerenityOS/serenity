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

package nsk.jdwp.ArrayReference.Length;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

public class length001a {

    public static final String ARRAY_FIELD_NAME = "array";
    public static final int ARRAY_LENGTH = 16;

    public static void main(String args[]) {
        length001a _length001a = new length001a();
        System.exit(length001.JCK_STATUS_BASE + _length001a.runIt(args, System.err));
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
        log.display("Sending signal to debugger: " + length001.READY);
        pipe.println(length001.READY);

        // wait for signal QUIT from debugeer
        log.display("Waiting for signal from debugger: " + length001.QUIT);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (! signal.equals(length001.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + length001.QUIT + ")");
            log.display("Debugee FAILED");
            return length001.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return length001.PASSED;
    }

    // tested class with own static fields values
    public static class TestedClass {

        // static field with tested array
        public static int array[] = null;

        public static void setArrayValues() {
            array = new int[ARRAY_LENGTH];
            for (int i = 0; i < ARRAY_LENGTH; i++) {
                array[i] = i * 10;
            }
        }
    }
}
