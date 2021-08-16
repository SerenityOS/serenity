/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.PackageType;
import static jdk.jpackage.test.RunnablePackageTest.Action.CREATE;
import jdk.jpackage.test.TKit;

/**
 * Test that --icon also changes icon of exe installer.
 */

/*
 * @test
 * @summary jpackage with --icon parameter for exe installer
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build WinInstallerIconTest
 * @requires (os.family == "windows")
 * @requires !vm.debug
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m  jdk.jpackage.test.Main
 *  --jpt-run=WinInstallerIconTest
 */

/*
 * note: AWT can throw assertion from GetDiBits() extracting icon
 * bits in fastdebug mode on windows headless systems. That is why
 * we have @requires !vm.debug" above.
 */
public class WinInstallerIconTest {

    @Test
    public void test() throws IOException {
        Path customIcon = iconPath("icon");

        BufferedImage[] defaultInstallerIconImg = new BufferedImage[1];

        // Create installer with the default icon
        long size1 = createInstaller(null, "WithDefaultIcon");

        // Create installer with custom icon.
        long size2 = createInstaller(customIcon, "WithCustomIcon");

        // Create another installer with custom icon.
        long size3 = createInstaller(customIcon, null);

        TKit.assertTrue(size2 < size1, "Installer 2 built with custom icon " +
                "should  be smaller than Installer 1 built with default icon");

        TKit.assertTrue(size3 < size1, "Installer 3 built with custom icon " +
                "should be smaller than Installer 1 built with default icon");

    }

    private long createInstaller(Path icon, String nameSuffix) throws IOException {

        PackageTest test = new PackageTest()
                .forTypes(PackageType.WIN_EXE)
                .addInitializer(JPackageCommand::setFakeRuntime)
                .configureHelloApp();
        if (icon != null) {
            test.addInitializer(cmd -> cmd.addArguments("--icon", icon));
        }

        if (nameSuffix != null) {
            test.addInitializer(cmd -> {
                String name = cmd.name() + nameSuffix;
                cmd.setArgumentValue("--name", name);
            });
        }

        Path installerExePath[] = new Path[1];

        test.addBundleVerifier(cmd -> {
            installerExePath[0] = cmd.outputBundle();
        });

        test.run(CREATE);

        long size = 0L;
        if (installerExePath[0] != null) {
            size = installerExePath[0].toFile().length();
            TKit.trace(" installer: " + installerExePath[0] + " - size: " + size);
            if (nameSuffix != null) {
                TKit.deleteIfExists(installerExePath[0]);
            }
        }
        return size;
    }

    private static Path iconPath(String name) {
        return TKit.TEST_SRC_ROOT.resolve(Path.of("resources", name
                + TKit.ICON_SUFFIX));
    }
}
