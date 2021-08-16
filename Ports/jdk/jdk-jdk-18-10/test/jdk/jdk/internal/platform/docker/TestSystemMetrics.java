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

/*
 * @test
 * @key cgroups
 * @summary Test JDK Metrics class when running inside docker container
 * @requires docker.support
 * @library /test/lib
 * @modules java.base/jdk.internal.platform
 * @run main TestSystemMetrics
 */

import jdk.test.lib.Utils;
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.containers.cgroup.MetricsTester;

public class TestSystemMetrics {
    private static final String imageName = Common.imageName("metrics");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");

        try {
            Common.logNewTestCase("Test SystemMetrics");
            DockerRunOptions opts =
                    new DockerRunOptions(imageName, "/jdk/bin/java", "jdk.test.lib.containers.cgroup.MetricsTester");
            opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
            opts.addDockerOpts("--memory=256m");
            opts.addJavaOpts("-cp", "/test-classes/");
            opts.addJavaOpts("--add-exports", "java.base/jdk.internal.platform=ALL-UNNAMED");
            DockerTestUtils.dockerRunJava(opts).shouldHaveExitValue(0).shouldContain("TEST PASSED!!!");
        } finally {
            DockerTestUtils.removeDockerImage(imageName);
        }
    }
}
