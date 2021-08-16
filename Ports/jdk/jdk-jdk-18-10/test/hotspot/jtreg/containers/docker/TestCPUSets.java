/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key cgroups
 * @summary Test JVM's awareness of cpu sets (cpus and mems)
 * @requires docker.support
 * @requires (os.arch != "s390x")
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @build AttemptOOM sun.hotspot.WhiteBox PrintContainerInfo
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar whitebox.jar sun.hotspot.WhiteBox
 * @run driver TestCPUSets
 */
import java.util.List;
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.containers.cgroup.CPUSetsReader;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jtreg.SkippedException;

public class TestCPUSets {
    private static final String imageName = Common.imageName("cpusets");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }


        Common.prepareWhiteBox();
        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");

        try {
            // Sanity test the cpu sets reader and parser
            CPUSetsReader.test();
            testTheSet("Cpus_allowed_list");
            testTheSet("Mems_allowed_list");
        } finally {
            DockerTestUtils.removeDockerImage(imageName);
        }
    }


    private static void testTheSet(String setType) throws Exception {
        String cpuSetStr = CPUSetsReader.readFromProcStatus(setType);

        if (cpuSetStr == null) {
            String msg = String.format("The %s test is skipped: cpuSetStr is null %n", setType);
            throw new SkippedException(msg);
        }

        List<Integer> cpuSet = CPUSetsReader.parseCpuSet(cpuSetStr);
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        // print diagnostic info
        printSet(setType, cpuSet);
        log("getNumCpus(): " + CPUSetsReader.getNumCpus());
        log("Runtime.getRuntime().availableProcessors(): " + availableProcessors);

        int maxSetSize = Math.min(cpuSet.size(), availableProcessors);
        log("maxSetSize = " + maxSetSize);

        // Test subset of one, full set, and half of the set
        testCpuSet(CPUSetsReader.listToString(cpuSet, 1));
        if (maxSetSize >= 2) {
            String cpuSetParam = CPUSetsReader.listToString(cpuSet, maxSetSize);
            log("Testing with cpuSetParam = " + cpuSetParam);
            testCpuSet(cpuSetParam);
        }
        if (maxSetSize >= 4) {
            String cpuSetParam = CPUSetsReader.listToString(cpuSet, maxSetSize/2);
            log("Testing with cpuSetParam = " + cpuSetParam);
            testCpuSet(cpuSetParam);
        }
    }


    private static void printSet(String setType, List<Integer> set) {
        System.out.print("printSet(): " + setType + ": ");
        set.forEach( i -> System.out.print(i + ", "));
        System.out.println("");
    }


    private static void log(String msg) {
        System.out.println(msg);
    }


    private static DockerRunOptions commonOpts() {
        DockerRunOptions opts = new DockerRunOptions(imageName, "/jdk/bin/java",
                                                     "PrintContainerInfo");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
        opts.addJavaOpts("-Xlog:os+container=trace", "-cp", "/test-classes/");
        Common.addWhiteBoxOpts(opts);
        return opts;
    }


    private static void checkResult(List<String> lines, String lineMarker, String value) {
        boolean lineMarkerFound = false;

        for (String line : lines) {
            if (line.contains(lineMarker)) {
                lineMarkerFound = true;
                String[] parts = line.split(":");
                System.out.println("DEBUG: line = " + line);
                System.out.println("DEBUG: parts.length = " + parts.length);

                Asserts.assertEquals(parts.length, 2);
                String set = parts[1].replaceAll("\\s","");
                String actual = CPUSetsReader.listToString(CPUSetsReader.parseCpuSet(set));
                Asserts.assertEquals(actual, value);
                break;
            }
        }
        Asserts.assertTrue(lineMarkerFound);
    }


    private static void testCpuSet(String value) throws Exception {
        Common.logNewTestCase("cpusets.cpus, value = " + value);

        DockerRunOptions opts = commonOpts();
        opts.addDockerOpts("--cpuset-cpus=" + value);

        List<String> lines = Common.run(opts).asLines();
        checkResult(lines, "cpuset.cpus is:", value);
    }

    private static void testMemSet(String value) throws Exception {
        Common.logNewTestCase("cpusets.mems, value = " + value);

        DockerRunOptions opts = commonOpts();
        opts.addDockerOpts("--cpuset-mems=" + value);

        List<String> lines = Common.run(opts).asLines();
        checkResult(lines, "cpuset.mems is:", value);
    }

}
