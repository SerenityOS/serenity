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

package jdk.test.lib.containers.docker;

/*
 * Methods and definitions common to docker tests container in this directory
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;


public class Common {
    public static final String imageNameAndTag = "jdk-internal:test";

    public static String imageName(String suffix) {
        return imageNameAndTag + "-" + suffix;
    }


    public static void prepareWhiteBox() throws Exception {
        Files.copy(Paths.get(new File("whitebox.jar").getAbsolutePath()),
                   Paths.get(Utils.TEST_CLASSES, "whitebox.jar"), StandardCopyOption.REPLACE_EXISTING);
    }


    // create simple commonly used options
    public static DockerRunOptions newOpts(String imageNameAndTag) {
        return new DockerRunOptions(imageNameAndTag, "/jdk/bin/java", "-version")
            .addJavaOpts("-Xlog:os+container=trace");
    }

    public static DockerRunOptions newOptsShowSettings(String imageNameAndTag) {
        return new DockerRunOptions(imageNameAndTag, "/jdk/bin/java", "-version", "-XshowSettings:system");
    }


    // create commonly used options with class to be launched inside container
    public static DockerRunOptions newOpts(String imageNameAndTag, String testClass) {
        DockerRunOptions opts =
            new DockerRunOptions(imageNameAndTag, "/jdk/bin/java", testClass);
        opts.addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/");
        opts.addJavaOpts("-Xlog:os+container=trace", "-cp", "/test-classes/");
        return opts;
    }


    public static DockerRunOptions addWhiteBoxOpts(DockerRunOptions opts) {
        opts.addJavaOpts("-Xbootclasspath/a:/test-classes/whitebox.jar",
                         "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI");
        return opts;
    }


    // most common type of run and checks
    public static OutputAnalyzer run(DockerRunOptions opts) throws Exception {
        return DockerTestUtils.dockerRunJava(opts)
            .shouldHaveExitValue(0).shouldContain("Initializing Container Support");
    }


    // log beginning of a test case
    public static void logNewTestCase(String msg) {
        System.out.println("========== NEW TEST CASE:      " + msg);
    }

}
