/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.LinuxHelper;
import static jdk.jpackage.test.TKit.assertTrue;
import static jdk.jpackage.test.TKit.assertFalse;

/**
 * Test --runtime-image parameter.
 * Output of the test should be RuntimePackageTest*.* installer.
 * The installer should install Java Runtime without an application.
 * Installation directory should not have "app" subfolder and should not have
 * an application launcher.
 *
 *
 * Windows:
 *
 * Java runtime should be installed in %ProgramFiles%\RuntimePackageTest directory.
 */

/*
 * @test
 * @summary jpackage with --runtime-image
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @requires (jpackage.test.SQETest == null)
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile RuntimePackageTest.java
 * @run main/othervm/timeout=1400 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=RuntimePackageTest
 */

/*
 * @test
 * @summary jpackage with --runtime-image
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @requires (jpackage.test.SQETest != null)
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile RuntimePackageTest.java
 * @run main/othervm/timeout=720 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=RuntimePackageTest.test
 */
public class RuntimePackageTest {

    @Test
    public static void test() {
        init(PackageType.NATIVE).run();
    }

    @Test
    public static void testUsrInstallDir() {
        init(PackageType.LINUX)
        .addInitializer(cmd -> cmd.addArguments("--install-dir", "/usr"))
        .run();
    }

    @Test
    public static void testUsrInstallDir2() {
        init(PackageType.LINUX)
        .addInitializer(cmd -> cmd.addArguments("--install-dir", "/usr/lib/Java"))
        .run();
    }

    private static PackageTest init(Set<PackageType> types) {
        return new PackageTest()
        .forTypes(types)
        .addInitializer(cmd -> {
            cmd.addArguments("--runtime-image", Optional.ofNullable(
                    JPackageCommand.DEFAULT_RUNTIME_IMAGE).orElse(Path.of(
                            System.getProperty("java.home"))));
            // Remove --input parameter from jpackage command line as we don't
            // create input directory in the test and jpackage fails
            // if --input references non existant directory.
            cmd.removeArgumentWithValue("--input");
        })
        .addInstallVerifier(cmd -> {
            Set<Path> srcRuntime = listFiles(Path.of(cmd.getArgumentValue("--runtime-image")));
            Path dest = cmd.appRuntimeDirectory();
            if (TKit.isOSX()) {
                dest = dest.resolve("Contents/Home");
            }
            Set<Path> dstRuntime = listFiles(dest);

            Set<Path> intersection = new HashSet<>(srcRuntime);
            intersection.retainAll(dstRuntime);

            srcRuntime.removeAll(intersection);
            dstRuntime.removeAll(intersection);

            assertFileListEmpty(srcRuntime, "Missing");
            assertFileListEmpty(dstRuntime, "Unexpected");
        })
        .forTypes(PackageType.LINUX_DEB)
        .addInstallVerifier(cmd -> {
            String installDir = cmd.getArgumentValue("--install-dir", () -> "/opt");
            Path copyright = Path.of("/usr/share/doc",
                    LinuxHelper.getPackageName(cmd), "copyright");
            boolean withCopyright = LinuxHelper.getPackageFiles(cmd).anyMatch(
                    Predicate.isEqual(copyright));
            if (installDir.startsWith("/usr/") || installDir.equals("/usr")) {
                assertTrue(withCopyright, String.format(
                        "Check the package delivers [%s] copyright file",
                        copyright));
            } else {
                assertFalse(withCopyright, String.format(
                        "Check the package doesn't deliver [%s] copyright file",
                        copyright));
            }
        });
    }

    private static Set<Path> listFiles(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            // Ignore files created by system prefs if any.
            final Path prefsDir = Path.of(".systemPrefs");
            return files.map(root::relativize)
                    .filter(x -> !x.startsWith(prefsDir))
                    .filter(x -> !x.endsWith(".DS_Store"))
                    .collect(Collectors.toSet());
        }
    }

    private static void assertFileListEmpty(Set<Path> paths, String msg) {
        TKit.assertTrue(paths.isEmpty(), String.format(
                "Check there are no %s files in installed image",
                msg.toLowerCase()), () -> {
            String msg2 = String.format("%s %d files", msg, paths.size());
            TKit.trace(msg2 + ":");
            paths.stream().map(Path::toString).sorted().forEachOrdered(
                    TKit::trace);
            TKit.trace("Done");
        });
    }
}
