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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.Functional;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.JavaTool;
import jdk.jpackage.test.Executor;

/*
 * @test
 * @summary jpackage with --runtime-image
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @modules jdk.incubator.jpackage/jdk.incubator.jpackage.internal
 * @compile RuntimeImageTest.java
 * @run main/othervm/timeout=1400 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=RuntimeImageTest
 */

public class RuntimeImageTest {

    @Test
    @Parameter("0")
    @Parameter("1")
    @Parameter("2")
    public static void test(String compression) throws Exception {
        final Path workDir = TKit.createTempDirectory("runtime").resolve("data");
        final Path jlinkOutputDir = workDir.resolve("temp.runtime");
        Files.createDirectories(jlinkOutputDir.getParent());

        new Executor()
        .setToolProvider(JavaTool.JLINK)
        .dumpOutput()
        .addArguments(
                "--output", jlinkOutputDir.toString(),
                "--compress=" + compression,
                "--add-modules", "ALL-MODULE-PATH",
                "--strip-debug",
                "--no-header-files",
                "--no-man-pages",
                "--strip-native-commands")
        .execute();

        JPackageCommand cmd = JPackageCommand.helloAppImage()
            .setArgumentValue("--runtime-image", jlinkOutputDir.toString());

        cmd.executeAndAssertHelloAppImageCreated();
    }

}
