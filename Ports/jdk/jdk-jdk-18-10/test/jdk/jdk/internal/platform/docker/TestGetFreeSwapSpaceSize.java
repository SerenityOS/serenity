/*
 * Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
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
 * @bug 8242480
 * @requires docker.support
 * @library /test/lib
 * @build GetFreeSwapSpaceSize
 * @run driver TestGetFreeSwapSpaceSize
 */
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class TestGetFreeSwapSpaceSize {
    private static final String imageName = Common.imageName("memory");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");

        try {
            testGetFreeSwapSpaceSize(
                "150M", Integer.toString(((int) Math.pow(2, 20)) * 150),
                "150M", Integer.toString(0)
            );
        } finally {
            if (!DockerTestUtils.RETAIN_IMAGE_AFTER_TEST) {
                DockerTestUtils.removeDockerImage(imageName);
            }
        }
    }

    private static void testGetFreeSwapSpaceSize(String memoryAllocation, String expectedMemory,
            String swapAllocation, String expectedSwap) throws Exception {
        Common.logNewTestCase("TestGetFreeSwapSpaceSize");

        DockerRunOptions opts = Common.newOpts(imageName, "GetFreeSwapSpaceSize")
            .addDockerOpts(
                "--memory", memoryAllocation,
                "--memory-swap", swapAllocation
            );

        OutputAnalyzer out = DockerTestUtils.dockerRunJava(opts);
        out.shouldHaveExitValue(0)
           .shouldContain("TestGetFreeSwapSpaceSize");
    }
}
