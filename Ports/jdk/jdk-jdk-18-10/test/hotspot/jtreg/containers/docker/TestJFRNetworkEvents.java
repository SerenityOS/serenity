/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test JFR network related events inside a container; make sure
 *          the reported host ip and host name are correctly reported within
 *          the container.
 * @requires docker.support
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @build JfrNetwork
 * @run driver TestJFRNetworkEvents
 */
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.Utils;


public class TestJFRNetworkEvents {
    private static final String imageName = Common.imageName("jfr-network");
    private static final int availableCPUs = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) throws Exception {
        System.out.println("Test Environment: detected availableCPUs = " + availableCPUs);
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");

        try {
            runTest("jdk.SocketWrite");
        } finally {
            DockerTestUtils.removeDockerImage(imageName);
        }
    }

    private static void runTest(String event) throws Exception {
        DockerRunOptions opts = new DockerRunOptions(imageName, "/jdk/bin/java", "JfrNetwork")
        .addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
        .addJavaOpts("-cp", "/test-classes/")
        .addDockerOpts("--hostname", JfrNetwork.HOST_NAME)
        .addClassOptions(event);
    DockerTestUtils.dockerRunJava(opts)
        .shouldHaveExitValue(0)
        .shouldContain(JfrNetwork.JFR_REPORTED_CONTAINER_HOSTNAME_TAG + JfrNetwork.HOST_NAME);
    }
}
