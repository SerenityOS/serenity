/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.nio.file.Path;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Executor;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.JavaAppDesc;
import jdk.jpackage.test.JavaTool;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.HelloApp;


/*
 * @test
 * @summary test '--runtime-image' option of jpackage
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile NoMPathRuntimeTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.NoMPathRuntimeTest
 */

public final class NoMPathRuntimeTest {

    public NoMPathRuntimeTest(String jlinkOutputSubdir, String runtimeSubdir) {
        this.jlinkOutputSubdir = Path.of(jlinkOutputSubdir);
        this.runtimeSubdir = Path.of(runtimeSubdir);
    }

    @Test
    public void test() throws IOException {
        JavaAppDesc appDesc = JavaAppDesc.parse("com.foo/com.foo.main.Aloha");

        JPackageCommand cmd = JPackageCommand.helloAppImage(appDesc);

        // Build module jar.
        cmd.executePrerequisiteActions();

        final Path workDir = TKit.createTempDirectory("runtime").resolve("data");
        final Path jlinkOutputDir = workDir.resolve(jlinkOutputSubdir);
        Files.createDirectories(jlinkOutputDir.getParent());

        // List of modules required for test app.
        final var modules = new String[] {
            "java.base",
            "java.desktop"
        };

        Executor jlink = new Executor()
        .setToolProvider(JavaTool.JLINK)
        .dumpOutput()
        .addArguments(
                "--add-modules", String.join(",", modules),
                "--output", jlinkOutputDir.toString(),
                "--strip-debug",
                "--no-header-files",
                "--no-man-pages");

        jlink.addArguments("--add-modules", appDesc.moduleName(),
               "--module-path", Path.of(cmd.getArgumentValue("--module-path"))
               .resolve("hello.jar").toString());

        jlink.execute();

        // non-modular jar in current dir caused error whe no module-path given
        cmd.removeArgumentWithValue("--module-path");

        cmd.setArgumentValue("--runtime-image", workDir.resolve(runtimeSubdir));
        Path junkJar = null;
        try {
            // create a non-modular jar in the current directory
            junkJar = HelloApp.createBundle(
                    JavaAppDesc.parse("junk.jar:Hello"), Path.of("."));

            cmd.executeAndAssertHelloAppImageCreated();
        } finally {
            if (junkJar != null) {
                TKit.deleteIfExists(junkJar);
            }
        }

    }

    @Parameters
    public static Collection data() {

        final List<String[]> paths = new ArrayList<>();
        paths.add(new String[] { "", "" });
        if (TKit.isOSX()) {
            // On OSX jpackage should accept both runtime root and runtime home
            // directories.
            paths.add(new String[] { "Contents/Home", "" });
        }

        List<Object[]> data = new ArrayList<>();
        for (var pathCfg : paths) {
            data.add(new Object[] { pathCfg[0], pathCfg[1] });
        }

        return data;
    }

    private final Path jlinkOutputSubdir;
    private final Path runtimeSubdir;
}
