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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/locationsOfLine_ssi/locationsOfLine_ssi003.
 * VM Testbase keywords: [quick, jpda, jdi, feature_sde, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks up that method 'com.sun.jdi.ReferenceType.locationsOfLine(String stratum, String sourceName, int lineNumber)' returns
 *     correct values defined for non-java stratum.
 *     Debugger creates copy of class file for class 'nsk.share.jdi.TestClass1' with SourceDebugExtension attribute
 *     which contains informations for stratum 'TestStratum' and for this stratums following line mapping
 *     is defined:
 *         "Java"          "TestStratum"
 *         <init>
 *         32      -->     1001
 *         32      -->     1002
 *         32      -->     1003
 *         32      -->     1004
 *     For this case locationsOfLine('TestStratum', null, 1001) should return single location for java line 32,
 *     and for lines 1002, 1003, 1004 should return empty list.
 *         33      -->     1005
 *         36      -->     1005
 *         37      -->     1005
 *         38      -->     1005
 *     For this case locationsOfLine('TestStratum', null, 1005) should return 4 locations for java lines 33 - 36,
 *         39      -->     1006
 *         40      -->     1007
 *         41      -->     1008
 *     For this case locationsOfLine for lines 1006-1007 should return single corresponding java line.
 *     Debugger forces debuggee to load 'TestClass1' from updated class file, obtains ReferenceType for this class
 *     and checks up that for obtained ReferenceType method 'com.sun.jdi.Method.locationsOfLine(String stratum, String sourceName, int lineNumber)'
 *     returns correct values for 3 cases described above.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.locationsOfLine_ssi.locationsOfLine_ssi003.locationsOfLine_ssi003
 * @run main/othervm
 *      nsk.jdi.ReferenceType.locationsOfLine_ssi.locationsOfLine_ssi003.locationsOfLine_ssi003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 *      -testClassPath ${test.class.path}
 *      -testWorkDir .
 */

package nsk.jdi.ReferenceType.locationsOfLine_ssi.locationsOfLine_ssi003;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.sde.*;

public class locationsOfLine_ssi003 extends SDEDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new locationsOfLine_ssi003().runIt(argv, out);
    }

    protected void preparePatchedClassFile(String className) {
        String smapFileName = "TestSMAP.smap";
        SmapGenerator smapGenerator = new SmapGenerator();

        SmapStratum smapStratum = new SmapStratum(testStratumName);
        smapStratum.addFile(testStratumSourceName, testStratumSourcePath);

        // single output line is mapped to the multiple input lines
        // 1001 -> 32
        // 1002 -> 32
        // 1003 -> 32
        // 1004 -> 32
        smapStratum.addLineData(1001, testStratumSourceName, 1, INIT_LINE, 1);
        smapStratum.addLineData(1002, testStratumSourceName, 1, INIT_LINE, 1);
        smapStratum.addLineData(1003, testStratumSourceName, 1, INIT_LINE, 1);
        smapStratum.addLineData(1004, testStratumSourceName, 1, INIT_LINE, 1);

        // multiple output lines are mapped to the single input line
        // 1005 -> 10
        // 1005 -> 11
        // 1005 -> 12
        // 1005 -> 13
        smapStratum.addLineData(1005, testStratumSourceName, 1, INIT_LINE + 1, 1);
        smapStratum.addLineData(1005, testStratumSourceName, 1, INIT_LINE + 2, 1);
        smapStratum.addLineData(1005, testStratumSourceName, 1, INIT_LINE + 3, 1);
        smapStratum.addLineData(1005, testStratumSourceName, 1, INIT_LINE + 4, 1);

        // single output line is mapped to the single input line
        // 1006 -> 14
        // 1007 -> 15
        // 1008 -> 16
        smapStratum.addLineData(1006, testStratumSourceName, 1, INIT_LINE + 5, 1);
        smapStratum.addLineData(1007, testStratumSourceName, 1, INIT_LINE + 6, 1);
        smapStratum.addLineData(1008, testStratumSourceName, 1, INIT_LINE + 7, 1);

        smapGenerator.addStratum(smapStratum, false);

        savePathcedClassFile(className, smapGenerator, smapFileName);
    }

    public void doTest() {
        String className = TestClass1.class.getName();

        preparePatchedClassFile(className);

        // debuggee loads TestClass1 from patched class file
        pipe.println(SDEDebuggee.COMMAND_LOAD_CLASS + ":" + className);

        if (!isDebuggeeReady())
            return;

        // obtain ReferenceType for loaded class
        ReferenceType referenceType = debuggee.classByName(className);

        List<DebugLocation> expectedLocations = new ArrayList<DebugLocation>();

        expectedLocations.add(new DebugLocation(testStratumSourceName, testStratumSourcePath,
                "<init>", 1001, INIT_LINE));
        log.display("Check case when single output line is mapped to the multiple input lines");
        // single output line is mapped to the multiple input lines
        // 1001 -> 32
        // 1002 -> 32
        // 1003 -> 32
        // 1004 -> 32
        try {
            // locationsOfLine.(testStratum, testStratumSource, 1001) should
            // return single java location
            compareLocations(
                    referenceType.locationsOfLine(testStratumName, testStratumSourceName, 1001),
                    expectedLocations,
                    testStratumName);

            // locationsOfLine.(testStratum, testStratumSource, [1002, 1003,
            // 1004]) should return empty list
            expectedLocations.clear();
            for (int i = 1002; i <= 1004; i++)
                compareLocations(
                        referenceType.locationsOfLine(testStratumName, testStratumSourceName, i),
                        expectedLocations,
                        testStratumName);
        } catch (AbsentInformationException e) {
            setSuccess(false);
            e.printStackTrace(log.getOutStream());
            log.complain("Unexpected exception: " + e);
        }

        expectedLocations.add(new DebugLocation(testStratumSourceName, testStratumSourcePath,
                "<init>", 1005, INIT_LINE + 1));
        expectedLocations.add(new DebugLocation(testStratumSourceName, testStratumSourcePath,
                "<init>", 1005, INIT_LINE + 2));
        expectedLocations.add(new DebugLocation(testStratumSourceName, testStratumSourcePath,
                "<init>", 1005, INIT_LINE + 3));
        expectedLocations.add(new DebugLocation(testStratumSourceName, testStratumSourcePath,
                "<init>", 1005, INIT_LINE + 4));

        log.display("Check case when multiple output lines are mapped to the single input line");
        // multiple output lines are mapped to the single input line
        // 1005 -> 33
        // 1005 -> 34
        // 1005 -> 35
        // 1005 -> 36
        try {
            // locationsOfLine.(testStratum, testStratumSource, 1005) should
            // return 4 java locations
            compareLocations(
                    referenceType.locationsOfLine(testStratumName, testStratumSourceName, 1005),
                    expectedLocations,
                    testStratumName);
        } catch (AbsentInformationException e) {
            setSuccess(false);
            e.printStackTrace(log.getOutStream());
            log.complain("Unexpected exception: " + e);
        }

        log.display("Check case when single output line is mapped to the single input line");
        // single output line is mapped to the single input line
        // 1006 -> 37
        // 1007 -> 38
        // 1008 -> 39
        try {
            for (int i = 0; i < 3; i++) {
                // locationsOfLine.(testStratum, testStratumSource, line) should
                // return 1 java locations
                expectedLocations.clear();
                expectedLocations.add(new DebugLocation(testStratumSourceName, testStratumSourcePath,
                        "<init>", 1006 + i, INIT_LINE + 5 + i));
                compareLocations(
                        referenceType.locationsOfLine(testStratumName, testStratumSourceName, 1006 + i),
                        expectedLocations,
                        testStratumName);
            }
        } catch (AbsentInformationException e) {
            setSuccess(false);
            e.printStackTrace(log.getOutStream());
            log.complain("Unexpected exception: " + e);
        }
    }
}
