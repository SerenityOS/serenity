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

package nsk.jdi.BreakpointEvent._itself_;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is a debuggee class containing several dummy methods.
 */
public class breakpoint002t {
    public static void main(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        Log log = argHandler.createDebugeeLog();

        do {
            log.display("Debuggee: sending the command: "
                + breakpoint002.COMMAND_READY);
            pipe.println(breakpoint002.COMMAND_READY);
            String cmd = pipe.readln();
            log.display("Debuggee: received the command: "
                + cmd);
            if (cmd.equals(breakpoint002.COMMAND_RUN[0])) {
                if (byteMeth() == -128)
                    log.display("Debuggee: returned from the method \"byteMeth\"");
            } else if (cmd.equals(breakpoint002.COMMAND_RUN[1])) {
                if (shortMeth() == 32767)
                    log.display("Debuggee: returned from the method \"shortMeth\"");
            } else if (cmd.equals(breakpoint002.COMMAND_RUN[2])) {
                if (intMeth() == -2147483648)
                    log.display("Debuggee: returned from the method \"intMeth\"");
            } else if (cmd.equals(breakpoint002.COMMAND_RUN[3])) {
                if (longMeth() == -9223372036854775808L)
                    log.display("Debuggee: returned from the method \"longMeth\"");
            } else if (cmd.equals(breakpoint002.COMMAND_QUIT)) {
                break;
            } else {
                System.err.println("TEST BUG: unknown debugger command: "
                    + cmd);
                System.exit(breakpoint002.JCK_STATUS_BASE +
                    breakpoint002.FAILED);
            }
        } while(true);
        log.display("Debuggee: exiting");
        System.exit(breakpoint002.JCK_STATUS_BASE +
            breakpoint002.PASSED);
    }

// dummy methods are below
    static byte byteMeth() {
        return -128;                  // breakpoint002.DEBUGGEE_LNS[0]
    }
    static short shortMeth() {
        return 32767;                 // breakpoint002.DEBUGGEE_LNS[1]
    }
    static int intMeth() {
        return -2147483648;           // breakpoint002.DEBUGGEE_LNS[2]
    }
    static long longMeth() {
        return -9223372036854775808L; // breakpoint002.DEBUGGEE_LNS[3]
    }
}
