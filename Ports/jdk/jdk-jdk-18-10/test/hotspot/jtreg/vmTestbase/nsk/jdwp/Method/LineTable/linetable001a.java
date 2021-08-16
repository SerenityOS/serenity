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

//    THIS TEST IS LINE NUMBER SENSITIVE

package nsk.jdwp.Method.LineTable;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class linetable001a {

    public static final int FIRST_LINE_NUMBER = 86;
    public static final int LAST_LINE_NUMBER = 97;

    public static void main(String args[]) {
        linetable001a _linetable001a = new linetable001a();
        System.exit(linetable001.JCK_STATUS_BASE + _linetable001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        // make communication pipe to debugger
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        // ensure tested class loaded
        log.display("Creating object of tested class");
        TestedClass foo = new TestedClass();

        // send debugger signal READY
        log.display("Sending signal to debugger: " + linetable001.READY);
        pipe.println(linetable001.READY);

        // wait for signal QUIT from debugeer
        log.display("Waiting for signal from debugger: " + linetable001.QUIT);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (! signal.equals(linetable001.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + linetable001.QUIT + ")");
            log.display("Debugee FAILED");
            return linetable001.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return linetable001.PASSED;
    }

    // tested class
    public static class TestedClass {
        int foo = 0;

        public void testedMethod() { // FIRST_LINE_NUMBER
            foo = 1;        // foo == 1
            foo++;          // foo == 2
            foo++;          // foo == 3
            foo++;          // foo == 4
            foo++;          // foo == 5
            foo++;          // foo == 6
            foo++;          // foo == 7
            foo++;          // foo == 8
            foo++;          // foo == 9
            foo++;          // foo == 10
        } // LAST_LINE_NUMBER
    }

}
