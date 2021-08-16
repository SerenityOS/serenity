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

import java.nio.file.Path;
import java.util.ArrayList;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameters;
import java.util.List;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.TKit;

/**
 * Test all possible combinations of --win-dir-chooser, --win-shortcut-prompt
 * and --license parameters.
 */

/*
 * @test
 * @summary jpackage with --win-dir-chooser, --win-shortcut-prompt and --license parameters
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build WinInstallerUiTest
 * @requires (os.family == "windows")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m  jdk.jpackage.test.Main
 *  --jpt-run=WinInstallerUiTest
 */
public class WinInstallerUiTest {

    public WinInstallerUiTest(Boolean withDirChooser, Boolean withLicense,
            Boolean withShortcutPrompt) {
        this.withShortcutPrompt = withShortcutPrompt;
        this.withDirChooser = withDirChooser;
        this.withLicense = withLicense;
    }

    @Parameters
    public static List<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        for (var withDirChooser : List.of(Boolean.TRUE, Boolean.FALSE)) {
            for (var withLicense : List.of(Boolean.TRUE, Boolean.FALSE)) {
                for (var withShortcutPrompt : List.of(Boolean.TRUE, Boolean.FALSE)) {
                    if (!withDirChooser && !withLicense && !withShortcutPrompt) {
                        // Duplicates SimplePackageTest
                        continue;
                    }

                    if (withDirChooser && !withLicense && !withShortcutPrompt) {
                        // Duplicates WinDirChooserTest
                        continue;
                    }

                    if (!withDirChooser && withLicense && !withShortcutPrompt) {
                        // Duplicates LicenseTest
                        continue;
                    }

                    data.add(new Object[]{withDirChooser, withLicense,
                        withShortcutPrompt});
                }
            }
        }

        return data;
    }

    @Test
    public void test() {
        PackageTest test = new PackageTest()
                .forTypes(PackageType.WINDOWS)
                .configureHelloApp();

        test.addInitializer(JPackageCommand::setFakeRuntime);
        test.addInitializer(this::setPackageName);

        if (withDirChooser) {
            test.addInitializer(cmd -> cmd.addArgument("--win-dir-chooser"));
        }

        if (withShortcutPrompt) {
            test.addInitializer(cmd -> {
                cmd.addArgument("--win-shortcut-prompt");
                cmd.addArgument("--win-menu");
                cmd.addArgument("--win-shortcut");
            });
        }

        if (withLicense) {
            test.addInitializer(cmd -> {
                cmd.addArguments("--license-file", TKit.createRelativePathCopy(
                        TKit.TEST_SRC_ROOT.resolve(Path.of("resources",
                                "license.txt"))));
            });
        }

        test.run();
    }

    private void setPackageName(JPackageCommand cmd) {
        StringBuilder sb = new StringBuilder(cmd.name());
        sb.append("With");
        if (withDirChooser) {
            sb.append("DirChooser");
        }
        if (withShortcutPrompt) {
            sb.append("ShortcutPrompt");
        }
        if (withLicense) {
            sb.append("License");
        }
        cmd.setArgumentValue("--name", sb.toString());
    }

    private final boolean withDirChooser;
    private final boolean withLicense;
    private final boolean withShortcutPrompt;
}
