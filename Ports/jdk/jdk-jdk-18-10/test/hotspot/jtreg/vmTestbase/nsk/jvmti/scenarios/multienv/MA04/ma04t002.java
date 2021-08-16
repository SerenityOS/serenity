/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.multienv.MA04;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ma04t002 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new ma04t002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // tested objects
    static Object testedObject1 = new Object();
    static Object testedObject2 = new Object();
    static ma04t002 testedInstance1 = new ma04t002();
    static ma04t002 testedInstance2 = new ma04t002();

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        log.display("Debugee started");

        // testcase 1 - check that there are no objects tagged - the test
        // calles both the IterateOverHeap and IterateOverInstancesOfClass,
        // and uses both the JVMTI_HEAP_OBJECT_EITHER and
        // JVMTI_HEAP_OBJECT_TAGGED filters in both agents
        status = checkStatus(status);

        // testcase 2 - test tags testedObject1 in the 1st agent and
        // testedObject2 in the 2nd agent, then calls iterateOverHeap
        // for each of the possible filters in both agents
        status = checkStatus(status);

        // testcase 3 - test tags testedInstance1 in the 1st agent and
        // testedInstance2 in the 2nd agent, then calls
        // IterateOverInstancesOfClass for each of the possible
        // filters in both agents
        status = checkStatus(status);

        log.display("Debugee finished");

        return status;
    }
}
