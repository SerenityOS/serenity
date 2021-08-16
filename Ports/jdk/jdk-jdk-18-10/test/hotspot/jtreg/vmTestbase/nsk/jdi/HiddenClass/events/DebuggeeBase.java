/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.HiddenClass.events;

import nsk.share.Log;
import nsk.jdi.HiddenClass.events.DebuggerBase;
import nsk.share.jdi.ArgumentHandler;
import nsk.share.jpda.IOPipe;

/* Debuggee base class. */
class DebuggeeBase {
    private final IOPipe pipe;
    private final Log log;

    protected void logMsg(String msg) { log.display(msg); }

    protected DebuggeeBase(ArgumentHandler argHandler) {
        pipe = argHandler.createDebugeeIOPipe();
        log = argHandler.createDebugeeLog();
    }

    protected void syncWithDebugger() {
        // Notify debugger that debuggee is ready.
        logMsg("Debuggee: Sending command: " + DebuggerBase.COMMAND_READY);
        pipe.println(DebuggerBase.COMMAND_READY);

        // Wait for COMMAND_RUN from debugger to continue execution.
        logMsg("Debuggee: Waiting for command: " + DebuggerBase.COMMAND_RUN);
        String command = pipe.readln();
        if (command == null || !command.equals(DebuggerBase.COMMAND_RUN)) {
            logMsg("FAIL: Debugee: unknown command: " + command);
            throw new RuntimeException("Failed in sync with debugger. ");
        }
    }

    protected void quitSyncWithDebugger() {
        // Notify debugger about debuggee completed status.
        logMsg("Debuggee: Sending command: " + DebuggerBase.COMMAND_DONE);
        pipe.println(DebuggerBase.COMMAND_DONE);

        // Wait for command QUIT from debugger to release started threads and exit.
        logMsg("Debuggee: Waiting for command: " + DebuggerBase.COMMAND_QUIT);
        String command = pipe.readln();
        if (command == null || !command.equals(DebuggerBase.COMMAND_QUIT)) {
            logMsg("FAIL: Debugee: unknown command: " + command);
            throw new RuntimeException("Failed in sync with debugger. ");
        }
    }
}
