/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Ensure that certain JFR events return correct results for resource values
 *          when run inside Docker container, such as available CPU and memory.
 *          Also make sure that PIDs are based on value provided by container,
 *          not by the host system.
 * @requires (docker.support & os.maxMemory >= 2g)
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @build JfrReporter
 * @run driver TestJFREvents
 */
import java.util.List;
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;


public class TestJFREvents {
    private static final String imageName = Common.imageName("jfr-events");
    private static final String TEST_ENV_VARIABLE = "UNIQUE_VARIABLE_ABC592903XYZ";
    private static final String TEST_ENV_VALUE = "unique_value_abc592903xyz";
    private static final int availableCPUs = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) throws Exception {
        System.out.println("Test Environment: detected availableCPUs = " + availableCPUs);
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");

        try {

            long MB = 1024*1024;
            testMemory("200m", "" + 200*MB);
            testMemory("500m", "" + 500*MB);
            testMemory("1g", "" + 1024*MB);

            testProcessInfo();

            testEnvironmentVariables();

            containerInfoTestCase();
            testCpuUsage();
            testCpuThrottling();
            testMemoryUsage();
            testIOUsage();
        } finally {
            DockerTestUtils.removeDockerImage(imageName);
        }
    }

    private static void containerInfoTestCase() throws Exception {
            // leave one CPU for system and tools, otherwise this test may be unstable
            int maxNrOfAvailableCpus =  availableCPUs - 1;
            for (int i=1; i < maxNrOfAvailableCpus; i = i * 2) {
                for (int j=64; j <= 256; j *= 2) {
                    testContainerInfo(i, j);
                }
            }
    }

    private static void testContainerInfo(int expectedCPUs, int expectedMemoryMB) throws Exception {
        Common.logNewTestCase("ContainerInfo: --cpus = " + expectedCPUs + " --memory=" + expectedMemoryMB + "m");
        String eventName = "jdk.ContainerConfiguration";
        long expectedSlicePeriod = 100000; // default slice period
        long expectedMemoryLimit = expectedMemoryMB * 1024 * 1024;

        String cpuCountFld = "effectiveCpuCount";
        String cpuQuotaFld = "cpuQuota";
        String cpuSlicePeriodFld = "cpuSlicePeriod";
        String memoryLimitFld = "memoryLimit";

        DockerTestUtils.dockerRunJava(
                                      commonDockerOpts()
                                      .addDockerOpts("--cpus=" + expectedCPUs)
                                      .addDockerOpts("--memory=" + expectedMemoryMB + "m")
                                      .addClassOptions(eventName))
            .shouldHaveExitValue(0)
            .shouldContain(cpuCountFld + " = " + expectedCPUs)
            .shouldContain(cpuSlicePeriodFld + " = " + expectedSlicePeriod)
            .shouldContain(cpuQuotaFld + " = " + expectedCPUs * expectedSlicePeriod)
            .shouldContain(memoryLimitFld + " = " + expectedMemoryLimit);
    }

    private static void testCpuUsage() throws Exception {
        Common.logNewTestCase("CPU Usage");
        String eventName = "jdk.ContainerCPUUsage";

        String cpuTimeFld = "cpuTime";
        String cpuUserTimeFld = "cpuUserTime";
        String cpuSystemTimeFld = "cpuSystemTime";

        DockerTestUtils.dockerRunJava(
                                      commonDockerOpts()
                                      .addClassOptions(eventName, "period=endChunk"))
            .shouldHaveExitValue(0)
            .shouldNotContain(cpuTimeFld + " = " + 0)
            .shouldNotContain(cpuUserTimeFld + " = " + 0)
            .shouldNotContain(cpuSystemTimeFld + " = " + 0);
    }

    private static void testMemoryUsage() throws Exception {
        Common.logNewTestCase("Memory Usage");
        String eventName = "jdk.ContainerMemoryUsage";

        String memoryFailCountFld = "memoryFailCount";
        String memoryUsageFld = "memoryUsage";
        String swapMemoryUsageFld = "swapMemoryUsage";

        DockerTestUtils.dockerRunJava(
                                      commonDockerOpts()
                                      .addClassOptions(eventName, "period=endChunk"))
            .shouldHaveExitValue(0)
            .shouldContain(memoryFailCountFld)
            .shouldContain(memoryUsageFld)
            .shouldContain(swapMemoryUsageFld);
    }

    private static void testIOUsage() throws Exception {
        Common.logNewTestCase("I/O Usage");
        String eventName = "jdk.ContainerIOUsage";

        String serviceRequestsFld = "serviceRequests";
        String dataTransferredFld = "dataTransferred";

        DockerTestUtils.dockerRunJava(
                                      commonDockerOpts()
                                      .addClassOptions(eventName, "period=endChunk"))
            .shouldHaveExitValue(0)
            .shouldContain(serviceRequestsFld)
            .shouldContain(dataTransferredFld);
    }

    private static void testCpuThrottling() throws Exception {
        Common.logNewTestCase("CPU Throttling");
        String eventName = "jdk.ContainerCPUThrottling";

        String cpuElapsedSlicesFld = "cpuElapsedSlices";
        String cpuThrottledSlicesFld = "cpuThrottledSlices";
        String cpuThrottledTimeFld = "cpuThrottledTime";

        DockerTestUtils.dockerRunJava(
                                      commonDockerOpts()
                                      .addClassOptions(eventName, "period=endChunk"))
            .shouldHaveExitValue(0)
            .shouldContain(cpuElapsedSlicesFld)
            .shouldContain(cpuThrottledSlicesFld)
            .shouldContain(cpuThrottledTimeFld);
    }


    private static void testMemory(String valueToSet, String expectedValue) throws Exception {
        Common.logNewTestCase("Memory: --memory = " + valueToSet);
        DockerTestUtils.dockerRunJava(
                                      commonDockerOpts()
                                      .addDockerOpts("--memory=" + valueToSet)
                                      .addClassOptions("jdk.PhysicalMemory"))
            .shouldHaveExitValue(0)
            .shouldContain("totalSize = " + expectedValue);
    }


    private static void testProcessInfo() throws Exception {
        Common.logNewTestCase("ProcessInfo");
        DockerTestUtils.dockerRunJava(
                                      commonDockerOpts()
                                      .addClassOptions("jdk.SystemProcess"))
            .shouldHaveExitValue(0)
            .shouldContain("pid = 1");
    }

    private static DockerRunOptions commonDockerOpts() {
        return new DockerRunOptions(imageName, "/jdk/bin/java", "JfrReporter")
            .addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
            .addJavaOpts("-cp", "/test-classes/");
    }


    private static void testEnvironmentVariables() throws Exception {
        Common.logNewTestCase("EnvironmentVariables");

        List<String> cmd = DockerTestUtils.buildJavaCommand(
                                      commonDockerOpts()
                                      .addClassOptions("jdk.InitialEnvironmentVariable"));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // Container has JAVA_HOME defined via the Dockerfile; make sure
        // it is reported by JFR event.
        // Environment variable set in host system should not be visible inside a container,
        // and should not be reported by JFR.
        pb.environment().put(TEST_ENV_VARIABLE, TEST_ENV_VALUE);

        System.out.println("[COMMAND]\n" + Utils.getCommandLine(pb));
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        System.out.println("[STDERR]\n" + out.getStderr());
        System.out.println("[STDOUT]\n" + out.getStdout());

        out.shouldHaveExitValue(0)
            .shouldContain("key = JAVA_HOME")
            .shouldContain("value = /jdk")
            .shouldNotContain(TEST_ENV_VARIABLE)
            .shouldNotContain(TEST_ENV_VALUE);
    }
}
