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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/defaultStratum/defaultStratum004.
 * VM Testbase keywords: [jpda, jdi, feature_sde, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test scenario:
 *     Debugger creates copies of 3 class files for classes defined in test's package: 'TestClass1'-'TestClass3'.
 *     SourceDebugExtension attribute is added for all this classes, for all classes different line mapping is defined
 *     add different default stratums are specified.
 *     Then debugger forces debuggee to load 'TestClass1'-'TestClass3' from updated class files and starts event listener thread which saves all received StepEvents.
 *     Then for TestedClass in 'TestClass1'-'TestClass3':
 *         - enables StepEvent request(class filter is used to receive events only for TestedClass)
 *         - forces debuggee to execute constructor of TestedClass
 *         - when constructor was executed debugger checks up that StepEvents was generated for all locations specified for stratum specified as default in TestedClass
 *     done
 *     Described event generation is performed 3 times.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.defaultStratum.defaultStratum004.defaultStratum004
 *        nsk.jdi.ReferenceType.defaultStratum.defaultStratum004.defaultStratum004a
 * @run main/othervm
 *      nsk.jdi.ReferenceType.defaultStratum.defaultStratum004.defaultStratum004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 *      -testClassPath ${test.class.path}
 *      -testWorkDir .
 */

package nsk.jdi.ReferenceType.defaultStratum.defaultStratum004;

import java.io.*;
import java.util.*;
import com.sun.jdi.request.StepRequest;
import nsk.share.Consts;
import nsk.share.TestBug;
import nsk.share.jdi.EventHandler;
import nsk.share.jdi.sde.*;

public class defaultStratum004 extends SDEDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new defaultStratum004().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        if (classpath == null) {
            throw new TestBug("Debugger requires 'testClassPath' parameter");
        }

        return defaultStratum004a.class.getName() + " -testClassPath " + testWorkDir;
    }

    private EventHandler eventHandler;

    protected List<DebugLocation> preparePatchedClassFile(String className, String stratumName, String sourceName,
            String sourcePath, List<DebugLocation> testStratumData) {
        String smapFileName = "TestSMAP.smap";
        SmapGenerator smapGenerator = new SmapGenerator();

        SmapStratum smapStratum = new SmapStratum(stratumName);

        smapStratum.addFile(sourceName, sourcePath);

        for (DebugLocation debugLocation : testStratumData) {
            smapStratum.addLineData(debugLocation.inputLine, sourceName, 1, debugLocation.outputLine, 1);
        }

        // set as default stratum
        smapGenerator.addStratum(smapStratum, true);

        savePathcedClassFile(className, smapGenerator, smapFileName);

        return testStratumData;
    }

    Map<String, List<DebugLocation>> classLocations = new TreeMap<String, List<DebugLocation>>();

    public void doTest() {
        String sourceName = testStratumSourceName;
        String sourcePath = testStratumSourcePath;
        String methodName = "<init>";

        String className = TestClass1.class.getName();
        String stratumName = className + "_Stratum";
        List<DebugLocation> testStratumData = new ArrayList<DebugLocation>();
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1001, 32));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1002, 34));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1003, 37));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1004, 39));
        preparePatchedClassFile(className, stratumName, sourceName, sourcePath, testStratumData);
        classLocations.put(className, testStratumData);

        className = TestClass2.class.getName();
        stratumName = className + "_Stratum";
        testStratumData = new ArrayList<DebugLocation>();
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1001, 32));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1002, 33));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1003, 34));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1004, 35));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1005, 36));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1006, 37));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1007, 38));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1008, 39));
        preparePatchedClassFile(className, stratumName, sourceName, sourcePath, testStratumData);
        classLocations.put(className, testStratumData);

        className = TestClass3.class.getName();
        stratumName = className + "_Stratum";
        testStratumData = new ArrayList<DebugLocation>();
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1001, 32));
        testStratumData.add(new DebugLocation(sourceName, sourcePath, methodName, 1008, 39));
        preparePatchedClassFile(className, stratumName, sourceName, sourcePath, testStratumData);
        classLocations.put(className, testStratumData);

        initDefaultBreakpoint();

        String command = defaultStratum004a.COMMAND_LOAD_TEST_CLASSES + ":" + TestClass1.class.getName() + " "
                + TestClass2.class.getName() + " " + TestClass3.class.getName();

        pipe.println(command);

        if (!isDebuggeeReady())
            return;

        eventHandler = new EventHandler(debuggee, log);
        eventHandler.startListening();

        StepEventListener stepEventListener = new StepEventListener();
        eventHandler.addListener(stepEventListener);

        // perform step events generation 3 times
        for (int i = 0; i < 3; i++) {
            for (String testedClassName : classLocations.keySet()) {
                StepRequest stepRequest = debuggee.getEventRequestManager().createStepRequest(
                        debuggee.threadByName(SDEDebuggee.mainThreadName),
                        StepRequest.STEP_LINE,
                        StepRequest.STEP_INTO);

                stepRequest.addClassFilter(testedClassName);
                stepRequest.setSuspendPolicy(StepRequest.SUSPEND_EVENT_THREAD);
                stepRequest.enable();

                stepEventListener.clearLocations();

                pipe.println(defaultStratum004a.COMMAND_INSTANTIATE_TEST_CLASS + ":" + testedClassName);

                if (!isDebuggeeReady())
                    return;

                stepEventListener.waitBreakpointEvent();

                compareLocations(
                        stepEventListener.stepLocations(),
                        classLocations.get(testedClassName),
                        testedClassName + "_Stratum");

                stepRequest.disable();
            }
        }
    }
}
