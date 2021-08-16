/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.platform.Metrics;
import jdk.test.lib.Utils;
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test
 * @key cgroups
 * @summary Test JDK Metrics class when running inside docker container
 * @requires docker.support
 * @library /test/lib
 * @modules java.base/jdk.internal.platform
 * @build MetricsMemoryTester
 * @run main/timeout=360 TestDockerMemoryMetrics
 */

public class TestDockerMemoryMetrics {
    private static final String imageName = Common.imageName("metrics-memory");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        // These tests create a docker image and run this image with
        // varying docker memory options.  The arguments passed to the docker
        // container include the Java test class to be run along with the
        // resource to be examined and expected result.

        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");
        try {
            testMemoryLimit("200m");
            testMemoryLimit("1g");

            testMemoryAndSwapLimit("200m", "1g");
            testMemoryAndSwapLimit("100m", "200m");

            Metrics m = Metrics.systemMetrics();
            // kernel memory, '--kernel-memory' switch, and OOM killer,
            // '--oom-kill-disable' switch, tests not supported by cgroupv2
            // runtimes
            if (m != null) {
                if ("cgroupv1".equals(m.getProvider())) {
                    testKernelMemoryLimit("100m");
                    testKernelMemoryLimit("1g");

                    testOomKillFlag("100m", false);
                } else {
                    System.out.println("kernel memory tests and OOM Kill flag tests not " +
                                       "possible with cgroupv2.");
                }
            }
            testOomKillFlag("100m", true);

            testMemoryFailCount("64m");

            testMemorySoftLimit("500m","200m");

        } finally {
            if (!DockerTestUtils.RETAIN_IMAGE_AFTER_TEST) {
                DockerTestUtils.removeDockerImage(imageName);
            }
        }
    }

    private static void testMemoryLimit(String value) throws Exception {
        Common.logNewTestCase("testMemoryLimit, value = " + value);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsMemoryTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
                .addDockerOpts("--memory=" + value)
                .addJavaOpts("-cp", "/test-classes/")
                .addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED")
                .addClassOptions("memory", value);
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testMemoryFailCount(String value) throws Exception {
        Common.logNewTestCase("testMemoryFailCount" + value);

        // Check whether swapping really works for this test
        // On some systems there is no swap space enabled. And running
        // 'java -Xms{mem-limit} -Xmx{mem-limit} -version' would fail due to swap space size being 0.
        DockerRunOptions preOpts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "-version");
        preOpts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
                .addDockerOpts("--memory=" + value)
                .addJavaOpts("-Xms" + value)
                .addJavaOpts("-Xmx" + value);
        OutputAnalyzer oa = DockerTestUtils.dockerRunJava(preOpts);
        String output = oa.getOutput();
        if (!output.contains("version")) {
            System.out.println("Swapping doesn't work for this test.");
            return;
        }

        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsMemoryTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
                .addDockerOpts("--memory=" + value)
                .addJavaOpts("-Xmx" + value)
                .addJavaOpts("-cp", "/test-classes/")
                .addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED")
                .addClassOptions("failcount");
        oa = DockerTestUtils.dockerRunJava(opts);
        output = oa.getOutput();
        if (output.contains("Ignoring test")) {
            System.out.println("Ignored by the tester");
            return;
        }
        oa.shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testMemoryAndSwapLimit(String memory, String memandswap) throws Exception {
        Common.logNewTestCase("testMemoryAndSwapLimit, memory = " + memory + ", memory and swap = " + memandswap);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsMemoryTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
                .addDockerOpts("--memory=" + memory)
                .addDockerOpts("--memory-swap=" + memandswap)
                .addJavaOpts("-cp", "/test-classes/")
                .addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED")
                .addClassOptions("memoryswap", memory, memandswap);
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testKernelMemoryLimit(String value) throws Exception {
        Common.logNewTestCase("testKernelMemoryLimit, value = " + value);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsMemoryTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
                .addDockerOpts("--kernel-memory=" + value)
                .addJavaOpts("-cp", "/test-classes/")
                .addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED")
                .addClassOptions("kernelmem", value);
        OutputAnalyzer oa = DockerTestUtils.dockerRunJava(opts);

        // Some container runtimes (e.g. runc, docker 18.09)
        // have been built without kernel memory accounting. In
        // that case, the runtime issues a message on stderr saying
        // so. Skip the test in that case.
        if (oa.getStderr().contains("kernel memory accounting disabled")) {
            System.out.println("Kernel memory accounting disabled, " +
                                       "skipping the test case");
            return;
        }

        oa.shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testOomKillFlag(String value, boolean oomKillFlag) throws Exception {
        Common.logNewTestCase("testOomKillFlag, oomKillFlag = " + oomKillFlag);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsMemoryTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
                .addDockerOpts("--memory=" + value);
        if (!oomKillFlag) {
            opts.addDockerOpts("--oom-kill-disable");
        }
        opts.addJavaOpts("-cp", "/test-classes/")
                .addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED")
                .addClassOptions("memory", value, oomKillFlag + "");
        OutputAnalyzer oa = DockerTestUtils.dockerRunJava(opts);
        oa.shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }

    private static void testMemorySoftLimit(String mem, String softLimit) throws Exception {
        Common.logNewTestCase("testMemorySoftLimit, memory = " + mem + ", soft limit = " + softLimit);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "MetricsMemoryTester");
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
                .addDockerOpts("--memory=" + mem)
                .addDockerOpts("--memory-reservation=" + softLimit);
        opts.addJavaOpts("-cp", "/test-classes/")
                .addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED")
                .addClassOptions("softlimit", softLimit);
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }
}
