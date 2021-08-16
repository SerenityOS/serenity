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
package nsk.jdi.VirtualMachineManager.connectedVirtualMachines;

import nsk.share.Log;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debuggee application for the convm002 JDI test.
 */

public class convm002a {

    static Log log;

    private static void log1(String message) {
            log.display("**> convm002a: " + message);
    }

    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);

        log = argHandler.createDebugeeLog();
        log1("debuggee started!");

        // informing a debugger of readiness
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        pipe.println("ready");

        /*
         * In this test debugger kills debuggee using VirtualMachine.exit, so
         * standard JDI tests communication protocol isn't used here
         */
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (Throwable t) {
            // ignore all exceptions
        }
    }
}
