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

package nsk.jdi.EventRequestManager.breakpointRequests;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is a debuggee class containing different types of methods.
 */
public class breakpreq001t {
    public static void main(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args); // breakpreq001.DEBUGGEE_LNS[0]
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        pipe.println(breakpreq001.COMMAND_READY);
        String cmd = pipe.readln();
        if (!cmd.equals(breakpreq001.COMMAND_QUIT)) {
            System.err.println("TEST BUG: unknown debugger command: "
                + cmd);
            System.exit(breakpreq001.JCK_STATUS_BASE +
                breakpreq001.FAILED);
        }
        System.exit(breakpreq001.JCK_STATUS_BASE +
            breakpreq001.PASSED);
    }

// dummy methods are below
    byte byteMeth() {
        return -128; // breakpreq001.DEBUGGEE_LNS[1]
    }
    short shortMeth() {
        return 0; // breakpreq001.DEBUGGEE_LNS[2]
    }
    private int prMeth() {
        return 1; // breakpreq001.DEBUGGEE_LNS[3]
    }
    protected long protMeth() {
        return 9223372036854775807L; // breakpreq001.DEBUGGEE_LNS[4]
    }
    float floatMeth() {
        return 0.03456f; // breakpreq001.DEBUGGEE_LNS[5]
    }
    double doubleMeth() {
        return -56.789D; // breakpreq001.DEBUGGEE_LNS[6]
    }
    char charMeth() {
        return '_'; // breakpreq001.DEBUGGEE_LNS[7]
    }
    private boolean boolMeth() {
        return true; // breakpreq001.DEBUGGEE_LNS[8]
    }
    public String pubMeth() {
        return "returning string"; // breakpreq001.DEBUGGEE_LNS[9]
    }
}
