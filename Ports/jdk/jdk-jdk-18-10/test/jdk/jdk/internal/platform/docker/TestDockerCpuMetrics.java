/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jdk.internal.platform.Metrics;
import jdk.test.lib.Utils;
import jdk.test.lib.containers.cgroup.CPUSetsReader;
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;

/*
 * @test
 * @key cgroups
 * @summary Test JDK Metrics class when running inside docker container
 * @requires docker.support
 * @library /test/lib
 * @modules java.base/jdk.internal.platform
 * @build MetricsCpuTester
 * @run main/timeout=360 TestDockerCpuMetrics
 */

public class TestDockerCpuMetrics {
    private static final String imageName = Common.imageName("metrics-cpu");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        // These tests create a docker image and run this image with
        // varying docker cpu options.  The arguments passed to the docker
        // container include the Java test class to be run along with the
        // resource to be examined and expected result.

        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");

        try {
            int numCpus = CPUSetsReader.getNumCpus();
            testCpuSet("0");
            testCpuSet("0-" + (numCpus - 1));
            if (numCpus > 2) {
                testCpuSet("0-" + ((numCpus - 1) / 2));
                testCpuSet((((numCpus - 1) / 2) + 1) + "-" + (numCpus - 1));
            }
            testCpuSet(IntStream.range(0, numCpus).mapToObj(a -> Integer.toString(a)).collect(Collectors.joining(",")));

            testCpuQuota(50 * 1000, 100 * 1000);
            testCpuQuota(100 * 1000, 100 * 1000);
            testCpuQuota(150 * 1000, 100 * 1000);
            testCpuQuota(400 * 1000, 100 * 1000);

            testCpuShares(256);
            testCpuShares(2048);
            testCpuShares(4096);

            testCpuThrottling(0.5);// --cpus=<value>

            int[] cpuSetMems = Metrics.systemMetrics().getCpuSetMems();
            String memNodes = null;
            if (cpuSetMems != null && cpuSetMems.length > 1) {
                int endNode = (cpuSetMems[cpuSetMems.length - 1] - cpuSetMems[0]) / 2 + cpuSetMems[0];
                memNodes = cpuSetMems[0] + "-" + endNode;
            } else if (cpuSetMems != null && cpuSetMems.length == 1) {
                memNodes = cpuSetMems[0] + "";
            }

            if(memNodes != null)
                testCpuSetMems(memNodes);

            testComboOptions("0-" + (numCpus - 1), 200 * 1000, 100 * 1000, 4 * 1024);
            testComboOptions("0", 200 * 1000, 100 * 1000, 1023);
        } finally {
            DockerTestUtils.removeDockerImage(imageName);
        }
    }

    private static void testCpuSetMems(String value) throws Exception {
        Common.logNewTestCase("testCpuSetMems, mem nodes = " + value);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsCpuTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
        opts.addDockerOpts("--cpuset-mems=" + value);
        opts.addJavaOpts("-cp", "/test-classes/").addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED");
        opts.addClassOptions("cpumems", value);
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testCpuSet(String value) throws Exception {
        Common.logNewTestCase("testCpuSet, value = " + value);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsCpuTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
        opts.addJavaOpts("-cp", "/test-classes/");
        opts.addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED");
        opts.addClassOptions("cpusets", value);
        opts.addDockerOpts("--cpuset-cpus=" + value);
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testCpuQuota(long quota, long period) throws Exception {
        Common.logNewTestCase("testCpuQuota, quota = " + quota + ", period = " + period);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsCpuTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
        opts.addDockerOpts("--cpu-period=" + period).addDockerOpts("--cpu-quota=" + quota);
        opts.addJavaOpts("-cp", "/test-classes/").addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED");
        opts.addClassOptions("cpuquota", quota + "", period + "");
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testCpuShares(int shares) throws Exception {
        Common.logNewTestCase("testCpuShares, shares = " + shares);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsCpuTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
        opts.addDockerOpts("--cpu-shares=" + shares);
        opts.addJavaOpts("-cp", "/test-classes/").addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED");
        opts.addClassOptions("cpushares", shares + "");
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testCpuThrottling(double cpus) throws Exception {
        Common.logNewTestCase("testCpuThrottling, cpus = " + cpus);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsCpuTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
        opts.addDockerOpts("--cpus=" + cpus);
        opts.addJavaOpts("-cp", "/test-classes/").addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED");
        opts.addClassOptions("cpus", cpus + "");
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testComboOptions(String cpuset, int quota, int period, int shares) throws Exception {
        Common.logNewTestCase("testComboOptions, shares = " + shares);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsCpuTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
        opts.addDockerOpts("--cpuset-cpus", "" + cpuset)
                .addDockerOpts("--cpu-period=" + period)
                .addDockerOpts("--cpu-quota=" + quota)
                .addDockerOpts("--cpu-shares=" + shares);
        opts.addJavaOpts("-cp", "/test-classes/").addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED");
        opts.addClassOptions("combo", cpuset, quota + "", period + "", shares + "");
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }
}
