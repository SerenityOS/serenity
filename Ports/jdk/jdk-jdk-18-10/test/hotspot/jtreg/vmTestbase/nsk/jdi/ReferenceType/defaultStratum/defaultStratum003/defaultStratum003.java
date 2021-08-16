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
 * @modules java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/defaultStratum/defaultStratum003.
 * VM Testbase keywords: [jpda, jdi, feature_sde, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks up that default stratum specified in class file affects StepEvents generation.
 *     Debugger creates copy of class file for class 'nsk.share.jdi.TestClass1' with SourceDebugExtension attribute
 *     which contains informations for 3 stratums('TestStratum1'-'TestStratum3') and for each of this stratums following line mapping
 *     is defined:
 *         "Java"          "TestStratum"
 *         <init>
 *         9       -->     1000, source1
 *         11      -->     1002, source1
 *         ...             ...
 *         sde_testMethod1
 *         20      -->     1100, source1
 *         22      -->     1101, source1
 *         ...             ...
 *         sde_testMethod1
 *         31      -->     1200, source1
 *         33      -->     1201, source1
 *         ...             ...
 *     Stratum 'TestStratum1' is specified as default for 'TestClass1'.
 *     Then debugger forces debuggee to load 'TestClass1' from updated class file, starts event listener thread which saves all received StepEvents,
 *     enables StepEvent request(class filter is used to receive events only for 'TestClass1') and forces debuggee to execute all methods defined in 'TestClass1'.
 *     When all methods was executed debugger checks up that StepEvents was generated for all locations specified for 'TestStratum1'.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.defaultStratum.defaultStratum003.defaultStratum003
 * @run main/othervm
 *      nsk.jdi.ReferenceType.defaultStratum.defaultStratum003.defaultStratum003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 *      -testClassPath ${test.class.path}
 *      -testWorkDir .
 *      -testStratumCount 3
 */

package nsk.jdi.ReferenceType.defaultStratum.defaultStratum003;

import java.io.*;
import java.util.*;
import com.sun.jdi.request.StepRequest;
import nsk.share.Consts;
import nsk.share.TestBug;
import nsk.share.jdi.EventHandler;
import nsk.share.jdi.sde.*;

public class defaultStratum003 extends SDEDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new defaultStratum003().runIt(argv, out);
    }

    protected String[] doInit(String args[], PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testStratumCount") && (i < args.length - 1)) {
                testStratumCount = Integer.parseInt(args[i + 1]);
                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    private int testStratumCount = 1;

    private EventHandler eventHandler;

    public void doTest() {
        String className = TestClass1.class.getName();

        Map<String, LocationsData> testStratumData = prepareDefaultPatchedClassFile_Type3(
                className,
                testStratumCount,
                false);
        /*
         * Method 'prepareDefaultPatchedClassFile_Type3' creates class file with
         * following line mapping for each test stratum:
         *
         * "Java" "TestStratum"
         *
         * <init>
         * 9 --> 1001, source1
         * 11 --> 1002, source1
         * 14 --> 1003, source1
         * 16 --> 1004, source1
         *
         * sde_testMethod1
         * 20 --> 1101, source1
         * 22 --> 1102, source1
         * 24 --> 1103, source1
         * 26 --> 1104, source1
         *
         * sde_testMethod2
         * 31 --> 1201, source1
         * 33 --> 1202, source1
         * 35 --> 1203, source1
         * 37 --> 1204, source1
         */

        String defaultStratum = null;

        // find wich stratum was set default
        // (prepareDefaultPatchedClassFile_Type3 should set default first
        // not-java stratum)
        for (LocationsData locationsData : testStratumData.values()) {
            if (locationsData.isDefault) {
                defaultStratum = locationsData.stratumName;
                break;
            }
        }

        if (defaultStratum == null || defaultStratum.equals(javaStratumName)) {
            throw new TestBug("Class file with default not-java stratum was not generated");
        }

        initDefaultBreakpoint();

        // check that events are generated for default stratum
        eventHandler = new EventHandler(debuggee, log);
        eventHandler.startListening();

        StepEventListener stepEventListener = new StepEventListener();
        eventHandler.addListener(stepEventListener);

        StepRequest stepRequest = debuggee.getEventRequestManager().createStepRequest(
                debuggee.threadByName(SDEDebuggee.mainThreadName),
                StepRequest.STEP_LINE,
                StepRequest.STEP_INTO);

        stepRequest.setSuspendPolicy(StepRequest.SUSPEND_EVENT_THREAD);
        stepRequest.addClassFilter(TestClass1.class.getName());
        stepRequest.enable();

        pipe.println(SDEDebuggee.COMMAND_EXECUTE_TEST_METHODS + ":" + className);

        if (!isDebuggeeReady())
            return;

        stepEventListener.waitBreakpointEvent();

        compareLocations(
                stepEventListener.stepLocations(),
                testStratumData.get(defaultStratum).allLocations,
                defaultStratum);
    }
}
