/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package process;

import nsk.share.jdi.ArgumentHandler;
import nsk.share.jpda.IOPipe;

/**
 * A simple process that connects to a pipe and waits for command "quit" to
 * be received.
 *
 * Usage: java TestJavaProcess -pipe.port <PIPE_PORT_NUMBER>
 */

public class TestJavaProcess {

    static final int PASSED = 0;
    static final int FAILED = 2;

    public static void main(String argv[]) {

        log("Test Java process started!");

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        pipe.println("ready");
        log("Waiting for the quit command from the test ...");
        String cmd = pipe.readln();
        int exitCode = PASSED;
        if ("quit".equals(cmd)) {
            log("'quit' received");
        } else {
            log("Invalid command received " + cmd);
            exitCode = FAILED;
        }
        System.exit(exitCode);
    }

    private static void log(String message) {
        System.out.println(message);
    }
}
