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

import java.nio.file.Path;
import java.io.IOException;
import jdk.jpackage.test.AdditionalLauncher;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.JPackageCommand;

/**
 * Test multiple launchers in two phases. First test creates app image and then
 * creates installer from this image. Output of the test should be
 * MultiLauncherTwoPhaseTest*.* installer. The output installer should be basic
 * installer with 3 launcher MultiLauncherTwoPhaseTest, bar and foo. On Windows
 * we should have start menu integration under MultiLauncherTwoPhaseTest and
 * desktop shortcuts for all 3 launchers. Linux should also create shortcuts for
 * all launchers.
 */

/*
 * @test
 * @summary Multiple launchers in two phases
 * @library ../helpers
 * @library /test/lib
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile MultiLauncherTwoPhaseTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=MultiLauncherTwoPhaseTest
 */

public class MultiLauncherTwoPhaseTest {

    @Test
    public static void test() throws IOException {
        Path appimageOutput = TKit.createTempDirectory("appimage");

        JPackageCommand appImageCmd = JPackageCommand.helloAppImage()
                .setArgumentValue("--dest", appimageOutput);

        AdditionalLauncher launcher1 = new AdditionalLauncher("bar");
        launcher1.setDefaultArguments().applyTo(appImageCmd);

        AdditionalLauncher launcher2 = new AdditionalLauncher("foo");
        launcher2.applyTo(appImageCmd);

        PackageTest packageTest = new PackageTest()
                .addLauncherName("bar") // Add launchers name for verification
                .addLauncherName("foo")
                .addRunOnceInitializer(() -> appImageCmd.execute())
                .addBundleDesktopIntegrationVerifier(true)
                .addInitializer(cmd -> {
                    cmd.addArguments("--app-image", appImageCmd.outputBundle());
                    cmd.removeArgumentWithValue("--input");
                })
                .forTypes(PackageType.WINDOWS)
                .addInitializer(cmd -> {
                    cmd.addArguments("--win-shortcut", "--win-menu",
                            "--win-menu-group", "MultiLauncherTwoPhaseTest");
                })
                .forTypes(PackageType.LINUX)
                .addInitializer(cmd -> {
                    cmd.addArguments("--linux-shortcut");
                });

        packageTest.run();
    }
}
