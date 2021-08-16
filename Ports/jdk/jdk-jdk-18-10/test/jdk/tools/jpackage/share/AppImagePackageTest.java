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

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.RunnablePackageTest.Action;
import jdk.jpackage.test.Annotations.Test;

/**
 * Test --app-image parameter. The output installer should provide the same
 * functionality as the default installer (see description of the default
 * installer in SimplePackageTest.java)
 */

/*
 * @test
 * @summary jpackage with --app-image
 * @key jpackagePlatformPackage
 * @library ../helpers
 * @requires (jpackage.test.SQETest == null)
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile AppImagePackageTest.java
 * @run main/othervm/timeout=540 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=AppImagePackageTest
 */
public class AppImagePackageTest {

    @Test
    public static void test() {
        Path appimageOutput = TKit.workDir().resolve("appimage");

        JPackageCommand appImageCmd = JPackageCommand.helloAppImage()
                .setArgumentValue("--dest", appimageOutput);

        new PackageTest()
        .addRunOnceInitializer(() -> appImageCmd.execute())
        .addInitializer(cmd -> {
            cmd.addArguments("--app-image", appImageCmd.outputBundle());
            cmd.removeArgumentWithValue("--input");
        }).addBundleDesktopIntegrationVerifier(false).run();
    }

    @Test
    @Parameter("true")
    @Parameter("false")
    public static void testEmpty(boolean withIcon) throws IOException {
        final String name = "EmptyAppImagePackageTest";
        final String imageName = name + (TKit.isOSX() ? ".app" : "");
        Path appImageDir = TKit.createTempDirectory(null).resolve(imageName);

        Files.createDirectories(appImageDir.resolve("bin"));
        Path libDir = Files.createDirectories(appImageDir.resolve("lib"));
        TKit.createTextFile(libDir.resolve("README"),
                List.of("This is some arbitrary text for the README file\n"));

        new PackageTest()
        .addInitializer(cmd -> {
            cmd.addArguments("--app-image", appImageDir);
            if (withIcon) {
                cmd.addArguments("--icon", iconPath("icon"));
            }
            cmd.removeArgumentWithValue("--input");

            // on mac, with --app-image and without --mac-package-identifier,
            // will try to infer it from the image, so foreign image needs it.
            if (TKit.isOSX()) {
                cmd.addArguments("--mac-package-identifier", name);
            }
        }).run(Action.CREATE, Action.UNPACK);
        // default: {CREATE, UNPACK, VERIFY}, but we can't verify foreign image
    }

    private static Path iconPath(String name) {
        return TKit.TEST_SRC_ROOT.resolve(Path.of("resources", name
                + TKit.ICON_SUFFIX));
    }
}
