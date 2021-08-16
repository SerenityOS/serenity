/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.aod;

import nsk.share.*;
import nsk.share.jpda.SocketIOPipe;

/*
Class TargetApplication is part of the framework used in the AttachOnDemand tests
(tests against Attach API, API from package com.sun.tools.attach).

This class is used in tests where main test application uses Attach API, but doesn't load agents to the another VM.
In these test there are 2 java applications: main application using Attach API and another
'dummy' application which should be alive while main application is working.

To synchronize main and dummy application SocketIOPipe is used: when DummyTargetApplication starts
it sends signal that it is ready for test and waits for signal permitting finish execution
(socket number used for connection establishing should be passed via command line).
 */
public class DummyTargetApplication {

    protected Log log = new Log(System.out, true);

    protected AODTargetArgParser argParser;

    protected SocketIOPipe pipe;

    public DummyTargetApplication(String[] args) {
        argParser = new AODTargetArgParser(args);
    }

    protected void targetApplicationActions() {
        // do nothing by default
    }

    public void runTargetApplication() {
        pipe = SocketIOPipe.createClientIOPipe(log, "localhost", argParser.getPort(), 0);
        log.display("Sending signal '" + AODTestRunner.SIGNAL_READY_FOR_ATTACH + "'");
        pipe.println(AODTestRunner.SIGNAL_READY_FOR_ATTACH);

        targetApplicationActions();

        String signal = pipe.readln();
        log.display("Signal received: '" + signal + "'");

        if ((signal == null) || !signal.equals(AODTestRunner.SIGNAL_FINISH))
            throw new TestBug("Unexpected signal: '" + signal + "'");
    }

    public static void main(String[] args) {
        new DummyTargetApplication(args).runTargetApplication();
    }
}
