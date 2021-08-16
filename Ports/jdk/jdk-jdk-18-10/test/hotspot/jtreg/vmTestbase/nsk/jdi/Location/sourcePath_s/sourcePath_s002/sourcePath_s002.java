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
 * @summary converted from VM Testbase nsk/jdi/Location/sourcePath_s/sourcePath_s002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_sde, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks up that method 'com.sun.jdi.Location.sourcePath(String stratum)' throws AbsentInformationException
 *     if source path is unavailable for given stratum.
 *     Debugger creates copy of class file for class 'nsk.share.jdi.TestClass1' with SourceDebugExtension attribute
 *     which defines following line mapping:
 *     "Java"          "TestStratum1"              "TestStratum2"          "TestStratum3"
 *     <init>
 *     9       -->         1000, source1, path1
 *     ...                 ...
 *     16      -->         1007, source1, path1
 *     sde_testMethod1
 *     20      -->                                 1100, source1, path1
 *     ...                                         ...
 *     27      -->                                 1107, source1, path1
 *     sde_testMethod2
 *     31      -->                                                           1200, source1, path1
 *     ...                                                                   ...
 *     38      -->                                                           1207, source1, path1
 *     Then debugger forces debuggee to load 'TestClass1' from updated class file, obtains ReferenceType for this class
 *     and checks up that for all locations obtained for one the 'TestStratumXXX' method 'sourcePath(String stratum)' throws AbsentInformationException
 *     when try to get source path for all others 'TestStratumXXX'.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Location.sourcePath_s.sourcePath_s002.sourcePath_s002
 * @run main/othervm
 *      nsk.jdi.Location.sourcePath_s.sourcePath_s002.sourcePath_s002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 *      -testClassPath ${test.class.path}
 *      -testWorkDir .
 */

package nsk.jdi.Location.sourcePath_s.sourcePath_s002;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.sde.*;

public class sourcePath_s002 extends SDEDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new sourcePath_s002().runIt(argv, out);
    }

    public void doTest() {
        String className = TestClass1.class.getName();

        Map<String, LocationsData> testStratumData = prepareDefaultPatchedClassFile_Type4(className);
        /*
         * Method 'prepareDefaultPatchedClassFile_Type4' creates class file with
         * following line mapping: "Java" "TestStratum1" "TestStratum2"
         * "TestStratum3"
         *
         * <init>
         * 9 --> 1000
         * ...
         * ...
         * 16 --> 1007
         *
         * sde_testMethod1
         * 20 --> 1100
         * ...
         * ...
         * 27 --> 1107
         *
         * sde_testMethod2
         * 31 --> 1200
         * ...
         * ...
         * 38 --> 1207
         */

        // debuggee loads TestClass1 from patched class file
        pipe.println(SDEDebuggee.COMMAND_LOAD_CLASS + ":" + className);

        if (!isDebuggeeReady())
            return;

        // obtain ReferenceType for loaded class
        ReferenceType referenceType = debuggee.classByName(className);

        for (String stratumName1 : testStratumData.keySet()) {
            log.display("Check locations for stratum: " + stratumName1);

            List<Location> allLocations;

            try {
                allLocations = referenceType.allLineLocations(stratumName1, null);
            } catch (AbsentInformationException e) {
                setSuccess(false);
                e.printStackTrace(log.getOutStream());
                log.complain("Unexpected exception: " + e);

                continue;
            }

            for (Location location : allLocations) {
                // check that for all stratums other than stratumName1
                // Location.sourcePath() throws AbsentInformationException
                for (String stratumName2 : testStratumData.keySet()) {
                    if (!stratumName2.equals(stratumName1)) {
                        log.display("Try get source path for stratum: " + stratumName2);

                        try {
                            location.sourcePath(stratumName2);
                            setSuccess(false);
                            log.complain("Expected AbsentInformationException was not thrown");
                        } catch (AbsentInformationException e) {
                            // expected exception
                        }
                    }
                }
            }
        }
    }
}
