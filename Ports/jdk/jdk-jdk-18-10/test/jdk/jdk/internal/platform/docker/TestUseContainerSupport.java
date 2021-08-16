/*
 * Copyright (c) 2020, Red Hat, Inc.
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
 * @summary UseContainerSupport flag should reflect Metrics being available
 * @requires docker.support
 * @library /test/lib
 * @modules java.base/jdk.internal.platform
 * @build CheckUseContainerSupport
 * @run main/timeout=360 TestUseContainerSupport
 */

import jdk.test.lib.Utils;
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;

public class TestUseContainerSupport {
    private static final String imageName = Common.imageName("useContainerSupport");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");

        try {
            testUseContainerSupport(true);
            testUseContainerSupport(false);
        } finally {
            DockerTestUtils.removeDockerImage(imageName);
        }
    }

    private static void testUseContainerSupport(boolean useContainerSupport) throws Exception {
        String testMsg = " with -XX:" + (useContainerSupport ? "+" : "-") + "UseContainerSupport";
        Common.logNewTestCase("Test TestUseContainerSupport" + testMsg);
        DockerRunOptions opts =
                new DockerRunOptions(imageName, "/jdk/bin/java", "CheckUseContainerSupport");
        opts.addClassOptions(Boolean.valueOf(useContainerSupport).toString());
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
        if (useContainerSupport) {
            opts.addJavaOpts("-XX:+UseContainerSupport");
        } else {
            opts.addJavaOpts("-XX:-UseContainerSupport");
        }
        opts.addJavaOpts("-cp", "/test-classes/");
        opts.addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED");
        DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
    }
}
