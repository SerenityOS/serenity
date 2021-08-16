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
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.JPackageCommand;

/**
 * Test creation of packages in tho phases with different names.
 * The first phase creates and app image, and the second phase uses that image.
 * If the first phase has no --name, it will derive name from main-class.
 * If the second phase has no --name, will derive it from the app-image content.
 * The resulting name may differ, and all should still work
 */

/*
 * @test
 * @summary Multiple names in two phases
 * @library ../helpers
 * @library /test/lib
 * @key jpackagePlatformPackage
 * @requires (jpackage.test.SQETest == null)
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile MultiNameTwoPhaseTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=MultiNameTwoPhaseTest
 */

public class MultiNameTwoPhaseTest {

    @Test
    @Parameter({"MultiNameTest", "MultiNameTest"})
    @Parameter({"MultiNameTest", "MultiNameTestInstaller"})
    @Parameter({"MultiNameTest", ""})
    @Parameter({"", "MultiNameTestInstaller"})
    @Parameter({"", ""})
    public static void test(String... testArgs) throws IOException {
        String appName = testArgs[0];
        String installName = testArgs[1];

        Path appimageOutput = TKit.createTempDirectory("appimage");

        JPackageCommand appImageCmd = JPackageCommand.helloAppImage()
                .setArgumentValue("--dest", appimageOutput)
                .removeArgumentWithValue("--name");
        if (!appName.isEmpty()) {
            appImageCmd.addArguments("--name", appName);
        }

        PackageTest packageTest = new PackageTest()
                .addRunOnceInitializer(() -> appImageCmd.execute())
                .addBundleDesktopIntegrationVerifier(true)
                .addInitializer(cmd -> {
                    cmd.addArguments("--app-image", appImageCmd.outputBundle());
                    cmd.removeArgumentWithValue("--input");
                    cmd.removeArgumentWithValue("--name");
                    if (!installName.isEmpty()) {
                        cmd.addArguments("--name", installName);
                    }
                })
                .forTypes(PackageType.WINDOWS)
                .addInitializer(cmd -> {
                    cmd.addArguments("--win-shortcut", "--win-menu",
                            "--win-menu-group", "MultiNameTwoPhaseTest");
                })
                .forTypes(PackageType.LINUX)
                .addInitializer(cmd -> {
                    cmd.addArguments("--linux-shortcut");
                });

        packageTest.run();
    }
}
