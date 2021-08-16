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
 * @summary converted from VM Testbase nsk/jdi/Method/allLineLocations_ss/allLineLocations_ss002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_sde, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks up that method 'com.sun.jdi.Method.allLineLocations(String stratum, String sourceName)' returns
 *     correct values for all stratums available for class and if sourceName == null locaitions for all sources are returned.
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
 *     Then debugger forces debuggee to load 'TestClass1' from updated class file, obtains ReferenceType for this class
 *     and checks up that for all methods defined in this class method 'com.sun.jdi.Method.allLineLocations(String stratum, String sourceName)'
 *     returns only expected locations for all stratums: for 'Java' stratum, and for stratums 'TestStratum1'-'TestStratum3'.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Method.allLineLocations_ss.allLineLocations_ss002.allLineLocations_ss002
 * @run main/othervm
 *      nsk.jdi.Method.allLineLocations_ss.allLineLocations_ss002.allLineLocations_ss002
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

package nsk.jdi.Method.allLineLocations_ss.allLineLocations_ss002;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.sde.*;

public class allLineLocations_ss002 extends SDEDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new allLineLocations_ss002().runIt(argv, out);
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

        Map<String, LocationsData> testStratumData;

        testStratumData = prepareDefaultPatchedClassFile_Type1(className, testStratumCount, true);

        /*
         * Method 'prepareDefaultPatchedClassFile_Type1' creates class file with
         * following line mapping for each TestStratum: "Java" "TestStratum"
         *
         * <init>
         * 9 --> 1000, source1
         * 10 --> 1001, source1
         * ...
         * ...
         * 16 --> 1007, source1
         *
         * sde_testMethod1
         * 20 --> 1100, source1
         * 21 --> 1101, source1
         * ...
         * ...
         * 27 --> 1107, source1
         *
         * sde_testMethod1
         * 31 --> 1200, source1
         * 32 --> 1201, source1
         * ...
         * ...
         * 38 --> 1207, source1
         */

        // debuggee loads TestClass1 from patched class file
        pipe.println(SDEDebuggee.COMMAND_LOAD_CLASS + ":" + className);

        if (!isDebuggeeReady())
            return;

        // obtain ReferenceType for loaded class
        ReferenceType referenceType = debuggee.classByName(className);

        for (String stratumName : testStratumData.keySet()) {
            for (Method method : referenceType.methods()) {
                List<DebugLocation> expectedLocations = testStratumData.get(stratumName).allLocations;

                // all locations has the same source
                String locationsSource = expectedLocations.get(0).sourceName;

                log.display("Check locations for method '" + method.name() + "': stratum: " + stratumName + ", source "
                        + locationsSource);

                try {
                    compareLocations(method.allLineLocations(stratumName, locationsSource), locationsOfMethod(
                            expectedLocations,
                            method.name()), stratumName);
                } catch (AbsentInformationException e) {
                    setSuccess(false);
                    e.printStackTrace(log.getOutStream());
                    log.complain("Unexpected exception: " + e);
                }

                log.display("Check locations for method '" + method.name() + "': stratum: " + stratumName + ", source "
                        + null);

                try {
                    compareLocations(method.allLineLocations(stratumName, null), locationsOfMethod(
                            expectedLocations,
                            method.name()), stratumName);
                } catch (AbsentInformationException e) {
                    setSuccess(false);
                    e.printStackTrace(log.getOutStream());
                    log.complain("Unexpected exception: " + e);
                }
            }
        }

    }
}
