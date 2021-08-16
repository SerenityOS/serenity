/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Path;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.LinuxHelper;
import jdk.jpackage.test.Annotations.Test;
import java.util.List;

/*
 * @test
 * @summary jpackage with --resource-dir
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @requires (os.family == "linux")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile LinuxResourceTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=LinuxResourceTest
 */

public class LinuxResourceTest {
    @Test
    public static void testHardcodedProperties() throws IOException {
        new PackageTest()
        .forTypes(PackageType.LINUX)
        .configureHelloApp()
        .addInitializer(cmd -> {
            cmd
            .setFakeRuntime()
            .saveConsoleOutput(true)
            .addArguments("--resource-dir", TKit.createTempDirectory("resources"));
        })
        .forTypes(PackageType.LINUX_DEB)
        .addInitializer(cmd -> {
            Path controlFile = Path.of(cmd.getArgumentValue("--resource-dir"),
                    "control");
            TKit.createTextFile(controlFile, List.of(
                "Package: dont-install-me",
                "Version: 1.2.3-R2",
                "Section: APPLICATION_SECTION",
                "Maintainer: APPLICATION_MAINTAINER",
                "Priority: optional",
                "Architecture: bar",
                "Provides: dont-install-me",
                "Description: APPLICATION_DESCRIPTION",
                "Installed-Size: APPLICATION_INSTALLED_SIZE",
                "Depends: PACKAGE_DEFAULT_DEPENDENCIES"
            ));
        })
        .addBundleVerifier((cmd, result) -> {
            TKit.assertTextStream("Using custom package resource [DEB control file]")
                    .predicate(String::contains)
                    .apply(result.getOutput().stream());
            TKit.assertTextStream(String.format(
                    "Expected value of \"Package\" property is [%s]. Actual value in output package is [dont-install-me]",
                    LinuxHelper.getPackageName(cmd)))
                    .predicate(String::contains)
                    .apply(result.getOutput().stream());
            TKit.assertTextStream(
                    "Expected value of \"Version\" property is [1.0-1]. Actual value in output package is [1.2.3-R2]")
                    .predicate(String::contains)
                    .apply(result.getOutput().stream());
            TKit.assertTextStream(String.format(
                    "Expected value of \"Architecture\" property is [%s]. Actual value in output package is [bar]",
                    LinuxHelper.getDefaultPackageArch(cmd.packageType())))
                    .predicate(String::contains)
                    .apply(result.getOutput().stream());
        })
        .forTypes(PackageType.LINUX_RPM)
        .addInitializer(cmd -> {
            Path specFile = Path.of(cmd.getArgumentValue("--resource-dir"),
                    LinuxHelper.getPackageName(cmd) + ".spec");
            TKit.createTextFile(specFile, List.of(
                "Name: dont-install-me",
                "Version: 1.2.3",
                "Release: R2",
                "Summary: APPLICATION_SUMMARY",
                "License: APPLICATION_LICENSE_TYPE",
                "Prefix: %{dirname:APPLICATION_DIRECTORY}",
                "Provides: dont-install-me",
                "%description",
                "APPLICATION_DESCRIPTION",
                "%prep",
                "%build",
                "%install",
                "rm -rf %{buildroot}",
                "install -d -m 755 %{buildroot}APPLICATION_DIRECTORY",
                "cp -r %{_sourcedir}APPLICATION_DIRECTORY/* %{buildroot}APPLICATION_DIRECTORY",
                "%files",
                "APPLICATION_DIRECTORY"
            ));
        })
        .addBundleVerifier((cmd, result) -> {
            TKit.assertTextStream("Using custom package resource [RPM spec file]")
                    .predicate(String::contains)
                    .apply(result.getOutput().stream());
            TKit.assertTextStream(String.format(
                    "Expected value of \"Name\" property is [%s]. Actual value in output package is [dont-install-me]",
                    LinuxHelper.getPackageName(cmd)))
                    .predicate(String::contains)
                    .apply(result.getOutput().stream());
            TKit.assertTextStream(
                    "Expected value of \"Version\" property is [1.0]. Actual value in output package is [1.2.3]")
                    .predicate(String::contains)
                    .apply(result.getOutput().stream());
            TKit.assertTextStream(
                    "Expected value of \"Release\" property is [1]. Actual value in output package is [R2]")
                    .predicate(String::contains)
                    .apply(result.getOutput().stream());
        })
        .run();
    }
}
