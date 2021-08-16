/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/instanceCounts/instancecounts004.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *       This is stress test against method "VirtualMachine.instanceCounts()".
 *       Test scenario:
 *       - Repeat the following steps 3 times:
 *          - Debuggee VM: create the number of different objects to consume 30% of available memory
 *          - Debuggee VM: create new thread 'GCProvocateur' which will try to consume the rest of memory to provoke GC
 *          - Debugger VM: at the same time call VirtualMachine.instanceCounts(VirtualMachine.allClasses()) 10 times
 *          - Debuggee VM: stop 'GCProvocateur' thread
 *       Test is treated as passed if no any unexpected events was caught during test execution and debuggee VM doesn't crash.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.instanceCounts.instancecounts004.instancecounts004
 *        nsk.jdi.VirtualMachine.instanceCounts.instancecounts004.instancecounts004a
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.instanceCounts.instancecounts004.instancecounts004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx256M ${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.VirtualMachine.instanceCounts.instancecounts004;

import java.io.PrintStream;
import java.util.*;
import nsk.share.Consts;
import nsk.share.jdi.HeapwalkingDebugger;
import nsk.share.test.Stresser;

public class instancecounts004 extends HeapwalkingDebugger {

    // It is possible to specify 'testCount', 'methodCallCount' via command line (for example: -methodCallCount 10)

    // test iterations count
    private int testCount = 1;

    // how many times call vm.instanceCounts() during single iteration
    private int methodCallCount = 10;

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new instancecounts004().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return instancecounts004a.class.getName();
    }

    // initialize test and remove arguments unsupported by nsk.share.jdi.ArgumentHandler
    protected String[] doInit(String[] args, PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testCount") && (i < args.length - 1)) {
                testCount = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-methodCallCount") && (i < args.length - 1)) {
                methodCallCount = Integer.parseInt(args[i + 1]);
                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    protected void testInstanceCounts(Stresser stresser) {
        for (int i = 0; i < methodCallCount && stresser.continueExecution(); i++) {
            try {
                log.display("Doing vm.instanceCounts(vm.allClasses())");
                vm.instanceCounts(vm.allClasses());
            } catch (Throwable e) {
                setSuccess(false);
                log.complain("Unexpected exception: " + e);
                e.printStackTrace(log.getOutStream());
            }
        }
    }

    protected void doTest() {
        log.display("Use testCount = " + testCount);
        log.display("Use methodCallCount = " + methodCallCount);

        stresser.start(testCount);

        try {
            while (stresser.iteration()) {
                pipe.println(instancecounts004a.COMMAND_CONSUME_MEMORY + ":" + 0.3);

                if (!isDebuggeeReady())
                    break;

                pipe.println(instancecounts004a.COMMAND_START_GC_PROVOCATEUR);

                if (!isDebuggeeReady())
                    break;

                testInstanceCounts(stresser);

                pipe.println(instancecounts004a.COMMAND_STOP_GC_PROVOCATEUR);

                if (!isDebuggeeReady())
                    break;
            }

            if (stresser.getIterationsLeft() > 0) {
                log.display("Test execution stopped because of test time exceeded");
            }
        } finally {
            stresser.finish();
        }
    }

}
