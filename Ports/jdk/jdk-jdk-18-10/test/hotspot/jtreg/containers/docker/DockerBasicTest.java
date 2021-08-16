/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic (sanity) test for JDK-under-test inside a docker image.
 * @requires docker.support
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @build HelloDocker
 * @run driver DockerBasicTest
 */
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;


public class DockerBasicTest {
    private static final String imageNameAndTag = Common.imageName("basic");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        DockerTestUtils.buildJdkDockerImage(imageNameAndTag, "Dockerfile-BasicTest", "jdk-docker");

        try {
            testJavaVersion();
            testHelloDocker();
        } finally {
            if (!DockerTestUtils.RETAIN_IMAGE_AFTER_TEST) {
                DockerTestUtils.removeDockerImage(imageNameAndTag);
            }
        }
    }


    private static void testJavaVersion() throws Exception {
        DockerRunOptions opts =
            new DockerRunOptions(imageNameAndTag, "/jdk/bin/java", "-version");

        DockerTestUtils.dockerRunJava(opts)
            .shouldHaveExitValue(0)
            .shouldContain(Platform.vmName);
    }


    private static void testHelloDocker() throws Exception {
        DockerRunOptions opts =
            new DockerRunOptions(imageNameAndTag, "/jdk/bin/java", "HelloDocker")
            .addJavaOpts("-cp", "/test-classes/")
            .addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");

        DockerTestUtils.dockerRunJava(opts)
            .shouldHaveExitValue(0)
            .shouldContain("Hello Docker");
    }
}
