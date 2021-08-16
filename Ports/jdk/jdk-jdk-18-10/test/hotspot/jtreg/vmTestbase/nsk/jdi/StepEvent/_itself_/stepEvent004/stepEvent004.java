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
 * @summary converted from VM Testbase nsk/jdi/StepEvent/_itself_/stepEvent004.
 * VM Testbase keywords: [jpda, jdi, feature_sde, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test scenario:
 *     There is class 'TestClass1' defined in test package wich has 10 method with 100 locations in each method.
 *     Debugger creates copy of class file for this class with SourceDebugExtension attribute which contains informations for 3
 *     stratums('TestStratum1'-'TestStratum3') and for all this stratums following line mapping is defined:
 *         "Java"          "TestStratum"
 *         <init>
 *         31      -->     1001, source1
 *         sde_testMethod1
 *         35      -->     2000, source1
 *         ...             ...
 *         135     -->     2100, source1
 *         sde_testMethod2
 *         139     -->     3000, source1
 *         ...             ...
 *         239     -->     3100, source1
 *         ...             ...
 *         sde_testMethod10
 *         971     -->    11000, source1
 *         ...             ...
 *         1071    -->    11100, source1
 *     Then debugger forces debuggee to load 'TestClass1' from updated class file, starts event listener thread which saves all received StepEvents
 *     and enables StepEvent request(class filter is used to receive events only for 'TestClass1').
 *     for TestStratum in 'TestStratum1'-'TestStratum3'
 *     do
 *         - set TestStratum as VM default
 *         - force debuggee to execute all methods defined in 'TestClass1'
 *         - when all methods was executed check up that StepEvents was generated for each location specified for TestStratum
 *         Described event generation is performed 10 times.
 *     done
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StepEvent._itself_.stepEvent004.stepEvent004
 * @run main/othervm
 *      nsk.jdi.StepEvent._itself_.stepEvent004.stepEvent004
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

package nsk.jdi.StepEvent._itself_.stepEvent004;

import java.io.*;
import java.util.*;
import com.sun.jdi.request.StepRequest;
import nsk.share.Consts;
import nsk.share.jdi.EventHandler;
import nsk.share.jdi.sde.*;

public class stepEvent004 extends SDEDebugger {
    private static final int INIT_LINE = 31;
    private static final int METHOD1_LINE = 35;
    private static final int METHOD2_LINE = 139;
    private static final int METHOD3_LINE = 243;
    private static final int METHOD4_LINE = 347;
    private static final int METHOD5_LINE = 451;
    private static final int METHOD6_LINE = 555;
    private static final int METHOD7_LINE = 659;
    private static final int METHOD8_LINE = 763;
    private static final int METHOD9_LINE = 867;
    private static final int METHOD10_LINE = 971;

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new stepEvent004().runIt(argv, out);
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

    protected Map<String, LocationsData> preparePatchedClassFile(String className, int testStratumCount) {
        /*
         * Create file with following line mapping for each test stratum:
         *
         * "Java"  "TestStratum"
         * <init>
         * 31   --> 1001, source1
         * sde_testMethod1
         * 35   --> 2000, source1
         * ...             ...
         * 135  --> 2100, source1
         * sde_testMethod2
         * 139  --> 3000, source1
         * ...             ...
         * 239  --> 3100, source1
         * ...             ...
         * sde_testMethod10
         * 971  --> 11000, source1
         * ...             ...
         * 1071 --> 11100, source1
         */
        String smapFileName = "TestSMAP.smap";
        SmapGenerator smapGenerator = new SmapGenerator();

        Map<String, LocationsData> testStratumData = new TreeMap<String, LocationsData>();

        for (int i = 0; i < testStratumCount; i++) {
            String stratumName = testStratumName + (i + 1);

            LocationsData locationsData = new LocationsData(stratumName);

            String sourceName = testStratumSourceName + (i + 1);
            String sourcePath = testStratumSourcePath + (i + 1);

            locationsData.paths.add(sourcePath);

            SmapStratum smapStratum = new SmapStratum(stratumName);

            List<DebugLocation> sourceLocations = new ArrayList<DebugLocation>();

            sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                    "<init>", 1001, INIT_LINE));

            for (int j = 0; j < 100; j++) {
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod1",   2000 + j, METHOD1_LINE  + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod2",   3000 + j, METHOD2_LINE  + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod3",   4000 + j, METHOD3_LINE  + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod4",   5000 + j, METHOD4_LINE  + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod5",   6000 + j, METHOD5_LINE  + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod6",   7000 + j, METHOD6_LINE  + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod7",   8000 + j, METHOD7_LINE  + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod8",   9000 + j, METHOD8_LINE  + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod9",  10000 + j, METHOD9_LINE  + j));
                sourceLocations.add(new DebugLocation(sourceName, sourcePath,
                        "sde_testMethod10", 11000 + j, METHOD10_LINE + j));
            }

            locationsData.sourceLocations.put(sourceName, sourceLocations);
            locationsData.allLocations.addAll(sourceLocations);
            testStratumData.put(stratumName, locationsData);

            smapStratum.addFile(sourceName, sourcePath);

            for (DebugLocation debugLocation : sourceLocations) {
                smapStratum.addLineData(
                        debugLocation.inputLine,
                        debugLocation.sourceName,
                        1,
                        debugLocation.outputLine,
                        1);
            }

            smapGenerator.addStratum(smapStratum, false);
        }

        savePathcedClassFile(className, smapGenerator, smapFileName);

        return testStratumData;
    }

    public void doTest() {
        String className = TestClass1.class.getName();

        Map<String, LocationsData> testStratumData = preparePatchedClassFile(className, testStratumCount);

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

        // for each stratum defined for class
        for (String stratumName : testStratumData.keySet()) {
            log.display("Generate events for stratum: " + stratumName);

            vm.setDefaultStratum(stratumName);

            // perform event generation 10 times
            for (int i = 0; i < 10; i++) {
                stepEventListener.clearLocations();

                pipe.println(SDEDebuggee.COMMAND_EXECUTE_TEST_METHODS + ":" + className);

                if (!isDebuggeeReady())
                    return;

                stepEventListener.waitBreakpointEvent();

                compareLocations(
                        stepEventListener.stepLocations(),
                        testStratumData.get(stratumName).allLocations,
                        stratumName);
            }
        }

    }
}
