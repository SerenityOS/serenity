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

package nsk.jdb.set.set002;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class set002a {

    private static final String DEBUGGEE_PASSED = "Debuggee PASSED";
    private static final String DEBUGGEE_FAILED = "Debuggee FAILED";

    static set002a _set002a = new set002a();

    public static void main(String args[]) {
       System.exit(set002.JCK_STATUS_BASE + _set002a.runIt(args, System.out));
    }

    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);
        String debuggeeResult = DEBUGGEE_PASSED;

        int localInt = 0;
        lastBreak();
        /* jdb should change values of fileds and variables */

        if (!_set002a.myArrayField[0][0].toString().equals("ABCDE")) {
            errorMessage += "\nWrong value of _set002a.myArrayField[0][0]: " + _set002a.myArrayField[0][0] + ", expected: \"ABCDE\"";
        }
        if (localInt != Integer.MIN_VALUE) {
            errorMessage += "\nWrong value of localInt: " + localInt + ", expected: " + Integer.MIN_VALUE;
        }
        if (errorMessage.length() > 0) {
            debuggeeResult = DEBUGGEE_FAILED;
        }

        lastBreak(); // a breakpoint to check value of debuggeeResult

        log.display(debuggeeResult);
        if (debuggeeResult.equals(DEBUGGEE_PASSED)) {
            return set002.PASSED;
        } else {
            return set002.FAILED;
        }
    }

    static String errorMessage = "";
    public MyClass[][] myArrayField;

    private set002a () {
         myArrayField = new MyClass[][] {new MyClass[] {new MyClass("")}};
    }

    static class MyClass {
        private String line;

        public MyClass (String s) {
            line = s;
        }

        public String toString() {
             return line;
        }
    }
}
