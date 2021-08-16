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

package nsk.jdi.Event._itself_;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This is a debuggee class containing dummy fields.
 */
public class event001t {
// dummy fields below
    static byte  byteFld  = 127;
    static short shortFld = -32768;
    static int   intFld   = 2147483647;

    public static void main(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        Log log = argHandler.createDebugeeLog();

        do {
            log.display("Debuggee: sending the command: "
                + event001.COMMAND_READY);
            pipe.println(event001.COMMAND_READY);
            String cmd = pipe.readln();
            log.display("Debuggee: received the command: "
                + cmd);
            if (cmd.equals(event001.COMMAND_RUN[0])) {
                if (byteFld == 127)
                    log.display("Debuggee: access to the field \"byteFld\""
                        + " is done");
            } else if (cmd.equals(event001.COMMAND_RUN[1])) {
                if (shortFld == -32768)
                    log.display("Debuggee: access to the field \"shortFld\""
                        + " is done");
            } else if (cmd.equals(event001.COMMAND_RUN[2])) {
                if (intFld == 2147483647)
                    log.display("Debuggee: access to the field \"intFld\""
                        + " is done");
            } else if (cmd.equals(event001.COMMAND_QUIT)) {
                break;
            } else {
                System.err.println("TEST BUG: unknown debugger command: "
                    + cmd);
                System.exit(event001.JCK_STATUS_BASE +
                    event001.FAILED);
            }
        } while(true);
        log.display("Debuggee: exiting");
        System.exit(event001.JCK_STATUS_BASE +
            event001.PASSED);
    }
}
