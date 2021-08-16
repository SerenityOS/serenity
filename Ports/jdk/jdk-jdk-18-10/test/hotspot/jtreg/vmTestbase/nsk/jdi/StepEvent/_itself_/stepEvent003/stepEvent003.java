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
 * @summary converted from VM Testbase nsk/jdi/StepEvent/_itself_/stepEvent003.
 * VM Testbase keywords: [jpda, jdi, feature_sde, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks up that StepEvents are generated correctly if SourceDebugExtension defines following line mapping:
 *         "Java"          "Test stratum"
 *         line 1  -->     line 1, source 1
 *         line 2  -->     line 1, source 2
 *         line 3  -->     line 1, source 3
 *     (lines in "Test stratum" has same numbers but different sources)
 *     Debugger creates copy of class file for class 'nsk.share.jdi.TestClass1' with SourceDebugExtension attribute
 *     which contains informations for 1 non-Java stratum and for this stratum following line mapping is defined:
 *         "Java"          "TestStratum"
 *         <init>
 *         32      -->     1000, source1
 *         33      -->     1000, source2
 *         ...             ...
 *         sde_testMethod1
 *         43      -->     1100, source1
 *         44      -->     1100, source2
 *         ...             ...
 *         sde_testMethod1
 *         54      -->     1200, source1
 *         55      -->     1200, source2
 *         ...             ...
 *     Then debugger forces debuggee to load 'TestClass1' from updated class file, starts event listener thread which saves all received StepEvents,
 *     enables StepEvent request(class filter is used to receive events only for 'TestClass1') and forces debuggee to execute all methods defined in 'TestClass1'.
 *     When all methods was executed debugger checks up that StepEvents was generated for each location specified for 'TestStratum'.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StepEvent._itself_.stepEvent003.stepEvent003
 * @run main/othervm
 *      nsk.jdi.StepEvent._itself_.stepEvent003.stepEvent003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 *      -testClassPath ${test.class.path}
 *      -testWorkDir .
 */

package nsk.jdi.StepEvent._itself_.stepEvent003;

import java.io.*;
import java.util.*;
import com.sun.jdi.request.StepRequest;
import nsk.share.Consts;
import nsk.share.jdi.EventHandler;
import nsk.share.jdi.sde.*;

public class stepEvent003 extends SDEDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new stepEvent003().runIt(argv, out);
    }

    private EventHandler eventHandler;

    protected List<DebugLocation> preparePatchedClassFile(String className) {
        /*
         * Create file with following line mapping:
         *
         * "Java" "TestStratum"
         *
         * <init>
         * 32 --> 1000, source1
         * ...
         * 39 --> 1000, source8
         *
         * sde_testMethod1
         * 43 --> 1100, source1
         * ...
         * 50 --> 1100, source8
         *
         * sde_testMethod2
         * 54 --> 1200, source1
         * ...
         * 61 --> 1200, source8
         */

        String sourceName = testStratumSourceName;
        String sourcePath = testStratumSourcePath;
        String stratumName = testStratumName;

        String smapFileName = "TestSMAP.smap";
        SmapGenerator smapGenerator = new SmapGenerator();

        SmapStratum smapStratum = new SmapStratum(stratumName);

        List<DebugLocation> testStratumData = new ArrayList<DebugLocation>();

        for (int i = 0; i < 8; i++) {
            String source = sourceName + (i + 1);
            String path = sourcePath + (i + 1);
            testStratumData.add(new DebugLocation(source, path,
                    "<init>", 1000 + i, INIT_LINE + i));
            smapStratum.addFile(source, path);

            testStratumData.add(new DebugLocation(source, path,
                    "sde_testMethod1", 1100 + i, METHOD1_LINE + i));
            smapStratum.addFile(source, path);

            testStratumData.add(new DebugLocation(source, path,
                    "sde_testMethod2", 1200 + i, METHOD2_LINE + i));
            smapStratum.addFile(source, path);
        }

        for (DebugLocation debugLocation : testStratumData) {
            smapStratum.addLineData(debugLocation.inputLine, debugLocation.sourceName, 1, debugLocation.outputLine, 1);
        }

        smapGenerator.addStratum(smapStratum, false);

        savePathcedClassFile(className, smapGenerator, smapFileName);

        return testStratumData;
    }

    public void doTest() {
        String className = TestClass1.class.getName();

        List<DebugLocation> locations = preparePatchedClassFile(className);

        initDefaultBreakpoint();

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

        vm.setDefaultStratum(testStratumName);

        pipe.println(SDEDebuggee.COMMAND_EXECUTE_TEST_METHODS + ":" + className);

        if (!isDebuggeeReady())
            return;

        stepEventListener.waitBreakpointEvent();

        compareLocations(stepEventListener.stepLocations(), locations, testStratumName);
    }
}
