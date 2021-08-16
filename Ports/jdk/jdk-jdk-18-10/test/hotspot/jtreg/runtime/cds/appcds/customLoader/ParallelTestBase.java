/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import jdk.test.lib.process.OutputAnalyzer;

/*
 * This is a base class for the following test cases:
 *   ParallelTestMultiFP.java
 *   ParallelTestSingleFP.java
 */
public class ParallelTestBase {
    public static final int MAX_CLASSES = 40; // must match ../test-classes/ParallelLoad.java
    public static int NUM_THREADS = 4;        // must match ../test-classes/ParallelLoad.java

    public static final int SINGLE_CUSTOM_LOADER = 1;
    public static final int MULTI_CUSTOM_LOADER  = 2;

    public static final int FINGERPRINT_MODE = 1;

    public static void run(String[] args, int loaderType, int mode) throws Exception {
        String[] cust_classes = new String[MAX_CLASSES];
        String[] cust_list;

        if (mode == FINGERPRINT_MODE) {
            cust_list = new String[MAX_CLASSES];
        } else {
            cust_list = new String[MAX_CLASSES * NUM_THREADS];
        }

        for (int i = 0; i<MAX_CLASSES; i++) {
            cust_classes[i] = "ParallelClass" + i;
        }
        String customJarPath = JarBuilder.build("ParallelTestBase", cust_classes);

        for (int i = 0, n=0; i<MAX_CLASSES; i++) {
            int super_id = 1;
            if (mode == FINGERPRINT_MODE) {
                // fingerprint mode -- no need to use the "loader:" option.
                int id = i + 2;
                cust_list[i] = cust_classes[i] + " id: " + id + " super: " + super_id + " source: " + customJarPath;
            } else {
                throw new RuntimeException("Only FINGERPRINT_MODE is supported");
            }
        }

        String app_list[];
        String mainClass;
        String appJar;

        if (mode == FINGERPRINT_MODE) {
            appJar = JarBuilder.build("parallel_fp",
                                      "ParallelLoad",
                                      "ParallelLoadThread",
                                      "ParallelLoadWatchdog");
            app_list = new String[] {
                "java/lang/Object id: 1",
                "ParallelLoad",
                "ParallelLoadThread",
                "ParallelLoadWatchdog"
            };
            mainClass = "ParallelLoad";
        } else {
            throw new RuntimeException("Currently only FINGERPRINT_MODE is supported");
        }

        OutputAnalyzer output;
        TestCommon.testDump(appJar, TestCommon.concat(app_list, cust_list));

        String loaderTypeArg = (loaderType == SINGLE_CUSTOM_LOADER) ? "SINGLE_CUSTOM_LOADER" : "MULTI_CUSTOM_LOADER";
        String modeArg = "FINGERPRINT_MODE";

        output = TestCommon.exec(appJar,
                                 // command-line arguments ...
                                 "--add-opens=java.base/java.security=ALL-UNNAMED",
                                 mainClass, loaderTypeArg, modeArg, customJarPath);
        TestCommon.checkExec(output);
    }
}
