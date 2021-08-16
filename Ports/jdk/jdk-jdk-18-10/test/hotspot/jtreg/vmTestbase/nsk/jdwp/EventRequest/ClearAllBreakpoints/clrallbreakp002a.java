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

package nsk.jdwp.EventRequest.ClearAllBreakpoints;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class clrallbreakp002a {

    public static void main(String args[]) {
        clrallbreakp002a _clrallbreakp002a = new clrallbreakp002a();
        System.exit(clrallbreakp002.JCK_STATUS_BASE + _clrallbreakp002a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        // ensure tested class is loaded
        log.display("Creating object of tested class");
        TestedClass foo = new TestedClass();
        log.display("  ... object created");

        // exit debugee
        log.display("Debugee PASSED");
        return clrallbreakp002.PASSED;
    }

    // tested class
    public static class TestedClass {
        int foo = 0;

        public TestedClass() {
            foo = 1000;
        }
    }
}
