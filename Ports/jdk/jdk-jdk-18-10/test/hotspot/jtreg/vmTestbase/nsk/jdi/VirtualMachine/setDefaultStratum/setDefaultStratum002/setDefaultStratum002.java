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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/setDefaultStratum/setDefaultStratum002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_sde, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks up that method 'com.sun.jdi.VirtualMachine.setDefaultStratum(String stratum)' affects result of following methods:
 *         - ReferenceType.defaultStratum()
 *         - ReferenceType.sourceName()
 *         - ReferenceType.allLineLocations()
 *         - ReferenceType.locationsOfLine(int lineNumber)
 *         - Method.allLineLocations()
 *         - Method.locationsOfLine(int lineNumber)
 *     Debugger creates copy of class file for class 'nsk.share.jdi.TestClass1' with SourceDebugExtension attribute
 *     which contains informations for 3 stratums('TestStratum1'-'TestStratum3') and for each of this stratums following line mapping
 *     is defined:
 *         "Java"          "TestStratum"
 *         <init>
 *         9       -->     1000, source1
 *         10      -->     1001, source1
 *         ...             ...
 *         16      -->     1007, source1
 *         sde_testMethod1
 *         20      -->     1100, source1
 *         21      -->     1101, source1
 *         ...             ...
 *         27      -->     1107, source1
 *         sde_testMethod1
 *         31      -->     1200, source1
 *         32      -->     1201, source1
 *         ...             ...
 *         38      -->     1207, source1
 *     Then debugger forces debuggee to load 'TestClass1' from updated class file and obtains ReferenceType for this class
 *     for TestStratum in 'TestStratum1'-'TestStratum3'
 *     do
 *         - set TestStratum as VM default using 'VirtualMachine.setDefaultStratum(String stratum)'
 *         - checks up that following methods:
 *                 - ReferenceType.defaultStratum()
 *                 - ReferenceType.sourceName()
 *                 - ReferenceType.allLineLocations()
 *                 - ReferenceType.locationsOfLine(int lineNumber)
 *                 - Method.allLineLocations()
 *                 - Method.locationsOfLine(int lineNumber)
 *         returns information for TestStratum.
 *     done
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.setDefaultStratum.setDefaultStratum002.setDefaultStratum002
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.setDefaultStratum.setDefaultStratum002.setDefaultStratum002
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

package nsk.jdi.VirtualMachine.setDefaultStratum.setDefaultStratum002;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.sde.*;

public class setDefaultStratum002 extends SDEDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new setDefaultStratum002().runIt(argv, out);
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

    public void doTest() {
        String className = TestClass1.class.getName();

        Map<String, LocationsData> testStratumData = prepareDefaultPatchedClassFile_Type1(
                className,
                testStratumCount,
                true);

        // debuggee loads TestClass1 from patched class file
        pipe.println(SDEDebuggee.COMMAND_LOAD_CLASS + ":" + className);

        if (!isDebuggeeReady())
            return;

        // obtain ReferenceType for loaded class
        ReferenceType referenceType = debuggee.classByName(className);

        List<String> sourceNames = new ArrayList<String>();
        List<String> sourcePaths = new ArrayList<String>();
        // if ReferenceType.sourcePaths(String stratum) is called with 'null'
        // argument it returns source path for
        // declaring type's default stratum, in this test default stratum for
        // TestClass1 is "Java"
        sourcePaths.add(javaSourcePath_TestClass1);

        // for each stratum available for class
        for (String stratumName : testStratumData.keySet()) {
            // for each stratum all locations have similar sourceName
            String sourceName = testStratumData.get(stratumName).allLocations.get(0).sourceName;

            log.display("Check default stratum '" + stratumName + "' (source name '" + sourceName + "')");

            sourceNames.clear();
            sourceNames.add(sourceName);

            vm.setDefaultStratum(stratumName);
            checkReferenceType(
                    null,
                    referenceType,
                    sourceNames,
                    sourcePaths,
                    testStratumData.get(stratumName).allLocations);
        }
    }
}
