/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.Path;

import tests.Helper;
import tests.JImageGenerator;

/*
 * @test
 * @summary Tests whether a custom Hijrah configuration properties file works correctly
 * @bug 8187987
 * @requires (vm.compMode != "Xcomp" & os.maxMemory >= 2g)
 * @library /tools/lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build HijrahConfigCheck tests.*
 * @run main/othervm -Xmx1g HijrahConfigTest
 */
public class HijrahConfigTest {

    private static final String TEST_CONFIG = "hijrah-config-Hijrah-test_islamic-test.properties";

    public static void main(String[] args) throws Exception {
        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }

        // Create the test JDK image
        Path outputPath = helper.createNewImageDir("HijrahConfigTest");
        JImageGenerator.getJLinkTask()
                .output(outputPath)
                .addMods("java.base")
                .call().assertSuccess();

        // Install the test hijrah configuration properties
        Path confPath = outputPath.resolve("conf").resolve("chronology");
        Files.createDirectory(confPath);
        Files.copy(Path.of(System.getProperty("test.src"), TEST_CONFIG),
                confPath.resolve(TEST_CONFIG));

        // Run tests
        Path launcher = outputPath.resolve("bin").resolve("java");
        ProcessBuilder builder = new ProcessBuilder(
                launcher.toAbsolutePath().toString(), "-ea", "-esa", "HijrahConfigCheck");
        Process p = builder.inheritIO().start();
        p.waitFor();
        int exitValue = p.exitValue();
        if (exitValue != 0) {
            throw new RuntimeException("HijrahConfigTest failed. Exit value: " + exitValue);
        }
    }
}
