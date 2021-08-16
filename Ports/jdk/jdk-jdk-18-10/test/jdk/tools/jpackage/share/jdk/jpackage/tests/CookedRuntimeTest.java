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


/*
 * @test
 * @summary test '--runtime-image' option of jpackage
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile CookedRuntimeTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.CookedRuntimeTest
 */

public final class CookedRuntimeTest {

    public CookedRuntimeTest(String javaAppDesc, String jlinkOutputSubdir,
            String runtimeSubdir) {
        this.javaAppDesc = javaAppDesc;
        this.jlinkOutputSubdir = Path.of(jlinkOutputSubdir);
        this.runtimeSubdir = Path.of(runtimeSubdir);
    }

    @Test
    public void test() throws IOException {
        JavaAppDesc appDesc = JavaAppDesc.parse(javaAppDesc);

        JPackageCommand cmd = JPackageCommand.helloAppImage(appDesc);

        final String moduleName = appDesc.moduleName();

        if (moduleName != null) {
            // Build module jar.
            cmd.executePrerequisiteActions();
        }

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

        if (moduleName != null) {
            jlink.addArguments("--add-modules", moduleName, "--module-path",
                    Path.of(cmd.getArgumentValue("--module-path")).resolve(
                            "hello.jar").toString());
        }

        jlink.execute();

        TKit.trace("jlink output BEGIN");
        try (Stream<Path> paths = Files.walk(jlinkOutputDir)) {
            paths.filter(Files::isRegularFile)
                    .map(jlinkOutputDir::relativize)
                    .map(Path::toString)
                    .forEach(TKit::trace);
        }
        TKit.trace("jlink output END");

        cmd.setArgumentValue("--runtime-image", workDir.resolve(runtimeSubdir));
        cmd.executeAndAssertHelloAppImageCreated();
    }

    @Parameters
    public static Collection data() {
        final List<String> javaAppDescs = List.of("Hello",
                "com.foo/com.foo.main.Aloha");

        final List<String[]> paths = new ArrayList<>();
        paths.add(new String[] { "", "" });
        if (TKit.isOSX()) {
            // On OSX jpackage should accept both runtime root and runtime home
            // directories.
            paths.add(new String[] { "Contents/Home", "" });
        }

        List<Object[]> data = new ArrayList<>();
        for (var javaAppDesc : javaAppDescs) {
            for (var pathCfg : paths) {
                data.add(new Object[] { javaAppDesc, pathCfg[0], pathCfg[1] });
            }
        }

        return data;
    }

    private final String javaAppDesc;
    private final Path jlinkOutputSubdir;
    private final Path runtimeSubdir;
}
