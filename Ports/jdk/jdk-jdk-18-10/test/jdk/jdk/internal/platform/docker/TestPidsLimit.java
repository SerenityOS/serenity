/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 SAP SE. All rights reserved.
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
 * @summary Test JDK Metrics class when running inside a docker container with limited pids
 * @bug 8266490
 * @requires docker.support
 * @library /test/lib
 * @build TestPidsLimit
 * @run driver TestPidsLimit
 */
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Asserts;

public class TestPidsLimit {
    private static final String imageName = Common.imageName("pids");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        DockerTestUtils.buildJdkDockerImage(imageName, "Dockerfile-BasicTest", "jdk-docker");

        try {
            testPidsLimit("1000");
            testPidsLimit("2000");
            testPidsLimit("Unlimited");
        } finally {
            if (!DockerTestUtils.RETAIN_IMAGE_AFTER_TEST) {
                DockerTestUtils.removeDockerImage(imageName);
            }
        }
    }

    private static void checkResult(List<String> lines, String lineMarker, String expectedValue) {
        boolean lineMarkerFound = false;

        for (String line : lines) {
            if (line.contains("WARNING: Your kernel does not support pids limit capabilities")) {
                System.out.println("Docker pids limitation seems not to work, avoiding check");
                return;
            }

            if (line.contains(lineMarker)) {
                lineMarkerFound = true;
                String[] parts = line.split(":");
                System.out.println("DEBUG: line = " + line);
                System.out.println("DEBUG: parts.length = " + parts.length);

                Asserts.assertEquals(parts.length, 2);
                String actual = parts[1].replaceAll("\\s","");
                // Unlimited pids leads on some setups not to "max" in the output, but to a high number
                if (expectedValue.equals("Unlimited")) {
                    if (actual.equals("Unlimited")) {
                        System.out.println("Found expected value for unlimited pids.");
                    } else {
                        try {
                            int ai = Integer.parseInt(actual);
                            if (ai > 20000) {
                                System.out.println("Limit value " + ai + " got accepted as unlimited, log line was " + line);
                            } else {
                                throw new RuntimeException("Limit value " + ai + " is not accepted as unlimited, log line was " + line);
                            }
                        } catch (NumberFormatException ex) {
                            throw new RuntimeException("Could not convert " + actual + " to an integer, log line was " + line);
                        }
                    }
                } else {
                    Asserts.assertEquals(actual, expectedValue);
                }
                break;
            }
        }
        Asserts.assertTrue(lineMarkerFound);
    }

    private static void testPidsLimit(String pidsLimit) throws Exception {
        Common.logNewTestCase("testPidsLimit (limit: " + pidsLimit + ")");
        DockerRunOptions opts = Common.newOptsShowSettings(imageName);
        if (pidsLimit.equals("Unlimited")) {
            opts.addDockerOpts("--pids-limit=-1");
        } else {
            opts.addDockerOpts("--pids-limit="+pidsLimit);
        }

        OutputAnalyzer out = DockerTestUtils.dockerRunJava(opts);
        out.shouldHaveExitValue(0);
        // some docker enviroments do not have the pids limit capabilities
        String sdr = out.getOutput();
        if (sdr.contains("WARNING: Your kernel does not support pids limit capabilities")) {
            System.out.println("Docker pids limitation seems not to work, avoiding check");
        } else {
            List<String> lines = new ArrayList<>();
            sdr.lines().forEach(s -> lines.add(s));
            checkResult(lines, "Maximum Processes Limit: ", pidsLimit);
        }
    }
}
