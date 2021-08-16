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
 * @summary Test JVM's memory resource awareness when running inside docker container
 * @requires docker.support
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.platform
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @build AttemptOOM sun.hotspot.WhiteBox PrintContainerInfo CheckOperatingSystemMXBean
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar whitebox.jar sun.hotspot.WhiteBox
 * @run driver TestMemoryAwareness
 */
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class TestMemoryAwareness {
    private static final String imageName = Common.imageName("memory");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        Common.prepareWhiteBox();
        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");

        try {
            testMemoryLimit("100m", "104857600");
            testMemoryLimit("500m", "524288000");
            testMemoryLimit("1g", "1073741824");
            testMemoryLimit("4g", "4294967296");

            testMemorySoftLimit("500m", "524288000");
            testMemorySoftLimit("1g", "1073741824");

            // Add extra 10 Mb to allocator limit, to be sure to cause OOM
            testOOM("256m", 256 + 10);

            testOperatingSystemMXBeanAwareness(
                "100M", Integer.toString(((int) Math.pow(2, 20)) * 100),
                "150M", Integer.toString(((int) Math.pow(2, 20)) * (150 - 100))
            );
            testOperatingSystemMXBeanAwareness(
                "128M", Integer.toString(((int) Math.pow(2, 20)) * 128),
                "256M", Integer.toString(((int) Math.pow(2, 20)) * (256 - 128))
            );
            testOperatingSystemMXBeanAwareness(
                "1G", Integer.toString(((int) Math.pow(2, 20)) * 1024),
                "1500M", Integer.toString(((int) Math.pow(2, 20)) * (1500 - 1024))
            );
        } finally {
            if (!DockerTestUtils.RETAIN_IMAGE_AFTER_TEST) {
                DockerTestUtils.removeDockerImage(imageName);
            }
        }
    }


    private static void testMemoryLimit(String valueToSet, String expectedTraceValue)
            throws Exception {

        Common.logNewTestCase("memory limit: " + valueToSet);

        DockerRunOptions opts = Common.newOpts(imageName)
            .addDockerOpts("--memory", valueToSet);

        Common.run(opts)
            .shouldMatch("Memory Limit is:.*" + expectedTraceValue);
    }


    private static void testMemorySoftLimit(String valueToSet, String expectedTraceValue)
            throws Exception {
        Common.logNewTestCase("memory soft limit: " + valueToSet);

        DockerRunOptions opts = Common.newOpts(imageName, "PrintContainerInfo");
        Common.addWhiteBoxOpts(opts);
        opts.addDockerOpts("--memory-reservation=" + valueToSet);

        Common.run(opts)
            .shouldMatch("Memory Soft Limit.*" + expectedTraceValue);
    }


    // provoke OOM inside the container, see how VM reacts
    private static void testOOM(String dockerMemLimit, int sizeToAllocInMb) throws Exception {
        Common.logNewTestCase("OOM");

        // add "--memory-swappiness 0" so as to disable anonymous page swapping.
        DockerRunOptions opts = Common.newOpts(imageName, "AttemptOOM")
            .addDockerOpts("--memory", dockerMemLimit, "--memory-swappiness", "0", "--memory-swap", dockerMemLimit);
        opts.classParams.add("" + sizeToAllocInMb);

        // make sure we avoid inherited Xmx settings from the jtreg vmoptions
        // set Xmx ourselves instead
        System.out.println("sizeToAllocInMb is:" + sizeToAllocInMb + " sizeToAllocInMb/2 is:" + sizeToAllocInMb/2);
        String javaHeapSize = sizeToAllocInMb/2 + "m";
        opts.addJavaOptsAppended("-Xmx" + javaHeapSize);

        OutputAnalyzer out = DockerTestUtils.dockerRunJava(opts);

        if (out.getExitValue() == 0) {
            throw new RuntimeException("We exited successfully, but we wanted to provoke an OOM inside the container");
        }

        out.shouldContain("Entering AttemptOOM main")
           .shouldNotContain("AttemptOOM allocation successful")
           .shouldContain("java.lang.OutOfMemoryError");
    }

    private static void testOperatingSystemMXBeanAwareness(String memoryAllocation, String expectedMemory,
            String swapAllocation, String expectedSwap) throws Exception {
        Common.logNewTestCase("Check OperatingSystemMXBean");

        DockerRunOptions opts = Common.newOpts(imageName, "CheckOperatingSystemMXBean")
            .addDockerOpts(
                "--memory", memoryAllocation,
                "--memory-swap", swapAllocation
            )
            // CheckOperatingSystemMXBean uses Metrics (jdk.internal.platform) for
            // diagnostics
            .addJavaOpts("--add-exports")
            .addJavaOpts("java.base/jdk.internal.platform=ALL-UNNAMED");

        OutputAnalyzer out = DockerTestUtils.dockerRunJava(opts);
        out.shouldHaveExitValue(0)
           .shouldContain("Checking OperatingSystemMXBean")
           .shouldContain("OperatingSystemMXBean.getTotalPhysicalMemorySize: " + expectedMemory)
           .shouldContain("OperatingSystemMXBean.getTotalMemorySize: " + expectedMemory)
           .shouldMatch("OperatingSystemMXBean\\.getFreeMemorySize: [1-9][0-9]+")
           .shouldMatch("OperatingSystemMXBean\\.getFreePhysicalMemorySize: [1-9][0-9]+");

        // in case of warnings like : "Your kernel does not support swap limit capabilities
        // or the cgroup is not mounted. Memory limited without swap."
        // the getTotalSwapSpaceSize and getFreeSwapSpaceSize return the system
        // values as the container setup isn't supported in that case.
        try {
            out.shouldContain("OperatingSystemMXBean.getTotalSwapSpaceSize: " + expectedSwap);
        } catch(RuntimeException ex) {
            out.shouldMatch("OperatingSystemMXBean.getTotalSwapSpaceSize: [0-9]+");
        }

        try {
            out.shouldMatch("OperatingSystemMXBean\\.getFreeSwapSpaceSize: [1-9][0-9]+");
        } catch(RuntimeException ex) {
            out.shouldMatch("OperatingSystemMXBean\\.getFreeSwapSpaceSize: 0");
        }
    }

}
