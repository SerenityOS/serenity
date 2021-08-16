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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/instanceCounts/instancecounts001.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 * This scenario in particular cover the situation described in CR 6376715.
 *      The test scenario is following:
 *      - the debugger gets the debuggee running on another JavaVM and
 *        establishes a pipe with the debuggee program
 *      - upon receiving corresponding command from the debugger process the debuggee
 *        loads set of classes and creates the number of class instances
 *      - the debugger process check that instanceCounts returns correct number
 *       - the debuggee process deletes previously created objects(make them unreachable) and
 *         enforces GC to clean up the heap.
 *      - the debugger process checks that instanceCounts returns 0 and no
 *        com.sun.jdi.ObjectCollectedException is thrown
 *
 * @requires !vm.graal.enabled
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.instanceCounts.instancecounts001.instancecounts001
 *        nsk.share.jdi.TestClass1
 *        nsk.share.jdi.TestClass2
 *        nsk.share.jdi.TestInterfaceImplementer1
 * @run main/othervm/native
 *      nsk.jdi.VirtualMachine.instanceCounts.instancecounts001.instancecounts001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.VirtualMachine.instanceCounts.instancecounts001;

import java.io.PrintStream;
import java.util.ArrayList;
import nsk.share.Consts;
import nsk.share.jdi.HeapwalkingDebuggee;
import nsk.share.jdi.HeapwalkingDebugger;
import nsk.share.jpda.AbstractDebuggeeTest;

public class instancecounts001 extends HeapwalkingDebugger {
    //force or not GC in debuggee vm (value of this field differs in instancecounts001 and instancecounts002)
    private boolean forceGC;

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new instancecounts001().runIt(argv, out);
    }

    //initialize test and remove arguments unsupported by nsk.share.jdi.ArgumentHandler
    protected String[] doInit(String args[], PrintStream out) {
        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-forceGC"))
                forceGC = true;
            else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    protected String debuggeeClassName() {
        return nsk.share.jdi.HeapwalkingDebuggee.class.getName();
    }

    protected void doTest() {
        String testClassNames[] = { "nsk.share.jdi.TestClass1", "nsk.share.jdi.TestClass2", "nsk.share.jdi.TestInterfaceImplementer1"};

        int testInstanceCount[] = { 2, 20, 1000, 500 };

        //create instances
        for (int i = 0; i < testClassNames.length; i++) {
            pipe.println(HeapwalkingDebuggee.COMMAND_CREATE_INSTANCES + ":" + testClassNames[i] + ":" + testInstanceCount[i]);
            checkDebugeeAnswer_instanceCounts(testClassNames[i], testInstanceCount[i]);
        }

        testInstanceCounts();

        //delete half of instances
        for (int i = 0; i < testClassNames.length; i++) {
            int deleteCount = (testInstanceCount[i] / 2);

            pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_INSTANCES + ":" + testClassNames[i] + ":" + deleteCount);

            testInstanceCount[i] -= deleteCount;
            checkDebugeeAnswer_instanceCounts(testClassNames[i], testInstanceCount[i]);
        }

        if (forceGC) {
            forceGC();
        }

        testInstanceCounts();

        //delete all instances
        for (int i = 0; i < testClassNames.length; i++) {
            int deleteCount = testInstanceCount[i];

            pipe.println(HeapwalkingDebuggee.COMMAND_DELETE_INSTANCES + ":" + testClassNames[i] + ":" + deleteCount);

            testInstanceCount[i] -= deleteCount;
            checkDebugeeAnswer_instanceCounts(testClassNames[i], testInstanceCount[i]);
        }

        testInstanceCounts();

    }


}
