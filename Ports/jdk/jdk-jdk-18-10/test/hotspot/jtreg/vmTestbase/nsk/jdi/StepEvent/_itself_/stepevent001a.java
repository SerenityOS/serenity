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

package nsk.jdi.StepEvent._itself_;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

// This class is the debugged application in the test

public class stepevent001a {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    public static final int stepLineBegin = 90;
    public static final int stepLineEnd = 100;

    static private int counter;
    static private final int LIMIT = 10;

    static private ArgumentHandler argHandler;
    static private IOPipe pipe;

    public static void main(String args[]) {
        stepevent001a _stepevent001a = new stepevent001a();
        System.exit(JCK_STATUS_BASE + _stepevent001a.run(args));
    }

    int run( String args[]) {
        argHandler = new ArgumentHandler(args);
        pipe = argHandler.createDebugeeIOPipe();

        // notify debugger about ready to execute
        pipe.println(COMMAND_READY);

        // wait for command <GO> from debugger
        String command = pipe.readln();
        if (!command.equals(COMMAND_GO)) {
            System.err.println("TEST BUG: Debugee: unknown command: " + command);
            return FAILED;
        }

        // invoke checked method to generate StepEvents
        foo();

        // notify debugger that checked method invoked
        pipe.println(COMMAND_DONE);

        // wait for command <QUIT> from debugger and exit
        command = pipe.readln();
        if (!command.equals(COMMAND_QUIT)) {
            System.err.println("TEST BUG: Debugee: unknown command: " + command);
            return FAILED;
        }

        return PASSED;
    }

    void foo() {
        // Increment 10 times;
        counter = 0; // stepLineBegin
        counter = 1;
        counter = 2;
        counter = 3;
        counter = 4;
        counter = 5;
        counter = 6;
        counter = 7;
        counter = 8;
        counter = 9;
        counter = 10; // stepLineEnd
    }
}
