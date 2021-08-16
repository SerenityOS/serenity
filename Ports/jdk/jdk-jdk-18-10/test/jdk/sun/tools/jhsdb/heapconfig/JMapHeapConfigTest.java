/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.SA.SATestUtils;
import jdk.test.lib.Utils;

/**
 * @test
 * @bug 8042397
 * @summary Unit test for jmap utility test heap configuration reader
 *
 * @requires vm.hasSA
 * @library /test/lib
 * @modules java.management
 *          jdk.hotspot.agent/sun.jvm.hotspot
 *
 * @build JMapHeapConfigTest TmtoolTestScenario
 * @run main JMapHeapConfigTest
 */
public class JMapHeapConfigTest {

    static final String expectedJMapValues[] = {
        "MinHeapFreeRatio",
        "MaxHeapFreeRatio",
        "MaxHeapSize",
        "NewSize",
        "OldSize",
        "NewRatio",
        "SurvivorRatio",
        "MetaspaceSize",
        "CompressedClassSpaceSize",
        "G1HeapRegionSize"};

    // Test can't deal with negative jlongs:
    //  ignoring MaxMetaspaceSize
    //  ignoring MaxNewSize

    static final String desiredMaxHeapSize = "-Xmx128m";

    private static Map<String, String> parseJMapOutput(List<String> jmapOutput) {
        Map<String, String> heapConfigMap = new HashMap<String, String>();
        boolean shouldParse = false;

        for (String line : jmapOutput) {
            line = line.trim();

            if (line.startsWith("Heap Configuration:")) {
                shouldParse = true;
                continue;
            }

            if (line.startsWith("Heap Usage:")) {
                shouldParse = false;
                continue;
            }

            if (shouldParse && !line.equals("")) {
                String[] lv = line.split("\\s+");
                try {
                    heapConfigMap.put(lv[0], lv[2]);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    // Ignore mailformed lines
                }
            }
        }
        return heapConfigMap;
    }

    // Compare stored values
    private static void compareValues(Map<String, String> parsedJMapOutput, Map<String, String> parsedVmOutput) {
        for (String key : expectedJMapValues) {
            try {
                String jmapVal = parsedJMapOutput.get(key);
                if (jmapVal == null) {
                    throw new RuntimeException("Key '" + key + "' doesn't exists in jmap output");
                }

                String vmVal = parsedVmOutput.get(key);
                if (vmVal == null) {
                    throw new RuntimeException("Key '" + key + "' doesn't exists in vm output");
                }

                if (new BigDecimal(jmapVal).compareTo(new BigDecimal(vmVal)) != 0) {
                    throw new RuntimeException(String.format("Key %s doesn't match %s vs %s", key, vmVal, jmapVal));
                }
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Unexpected key '" + key + "' value", ex);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting JMapHeapConfigTest");
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.

        if (!LingeredApp.isLastModifiedWorking()) {
            // Exact behaviour of the test depends to operating system and the test nature,
            // so just print the warning and continue
            System.err.println("Warning! Last modified time doesn't work.");
        }

        boolean mx_found = false;
        String[] jvmOptions = Utils.getTestJavaOpts();
        for (String option : jvmOptions) {
            if (option.startsWith("-Xmx")) {
               System.out.println("INFO: maximum heap size set by JTREG as " + option);
               mx_found = true;
               break;
           }
        }

        // Forward vm options to LingeredApp
        ArrayList<String> cmd = new ArrayList();
        Collections.addAll(cmd, Utils.getTestJavaOpts());
        if (!mx_found) {
            cmd.add(desiredMaxHeapSize);
            System.out.println("INFO: maximum heap size set explicitly as " + desiredMaxHeapSize);
        }
        cmd.add("-XX:+PrintFlagsFinal");

        TmtoolTestScenario tmt = TmtoolTestScenario.create("jmap", "--heap");
        int exitcode = tmt.launch(cmd.toArray(new String[0]));
        if (exitcode != 0) {
            throw new RuntimeException("Test FAILED jmap exits with non zero exit code " + exitcode);
        }

        Map<String,String> parsedJmapOutput = parseJMapOutput(tmt.getToolOutput());
        Map<String,String> parsedVMOutput = tmt.parseFlagsFinal();

        compareValues(parsedJmapOutput, parsedVMOutput);

        // If test fails it throws RuntimeException
        System.out.println("Test PASSED");
    }
}
