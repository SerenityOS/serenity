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

import java.util.ArrayList;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameters;
import java.util.List;
import jdk.jpackage.test.PackageType;

/**
 * Test all possible combinations of --win-shortcut-prompt, --win-menu and
 * --win-shortcut parameters.
 */

/*
 * @test
 * @summary jpackage with --win-shortcut-prompt, --win-menu and --win-shortcut parameters
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build WinShortcutPromptTest
 * @requires (os.family == "windows")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m  jdk.jpackage.test.Main
 *  --jpt-run=WinShortcutPromptTest
 */
public class WinShortcutPromptTest {

    public WinShortcutPromptTest(Boolean withStartMenuShortcut,
            Boolean withDesktopShortcut, Boolean withShortcutPrompt) {
        this.withStartMenuShortcut = withStartMenuShortcut;
        this.withDesktopShortcut = withDesktopShortcut;
        this.withShortcutPrompt = withShortcutPrompt;
    }

    @Parameters
    public static List<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        for (var withStartMenuShortcut : List.of(Boolean.TRUE, Boolean.FALSE)) {
            for (var withDesktopShortcut : List.of(Boolean.TRUE, Boolean.FALSE)) {
                for (var withShortcutPrompt : List.of(Boolean.TRUE, Boolean.FALSE)) {
                    if (withShortcutPrompt && withStartMenuShortcut
                            && withDesktopShortcut) {
                        // Duplicates WinInstallerUiTestWithShortcutPromptTest (WinInstallerUiTest(withShortcutPrompt=true))
                        continue;
                    }

                    if (!withShortcutPrompt && !withStartMenuShortcut
                            && !withDesktopShortcut) {
                        // Duplicates SimplePackageTest
                        continue;
                    }

                    if (!withShortcutPrompt && !withStartMenuShortcut
                            && withDesktopShortcut) {
                        // Duplicates WinShortcutTest
                        continue;
                    }

                    if (!withShortcutPrompt && withStartMenuShortcut
                            && !withDesktopShortcut) {
                        // Duplicates WinMenuTest
                        continue;
                    }

                    data.add(new Object[]{withStartMenuShortcut,
                        withDesktopShortcut, withShortcutPrompt});
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

        if (withShortcutPrompt) {
            test.addInitializer(cmd -> cmd.addArgument("--win-shortcut-prompt"));
        }

        if (withStartMenuShortcut) {
            test.addInitializer(cmd -> cmd.addArgument("--win-menu"));
        }

        if (withDesktopShortcut) {
            test.addInitializer(cmd -> cmd.addArgument("--win-shortcut"));
        }

        test.run();
    }

    private void setPackageName(JPackageCommand cmd) {
        StringBuilder sb = new StringBuilder(cmd.name());
        sb.append("With");
        if (withShortcutPrompt) {
            sb.append("ShortcutPrompt");
        }
        if (withStartMenuShortcut) {
            sb.append("StartMenu");
        }
        if (withDesktopShortcut) {
            sb.append("Desktop");
        }
        cmd.setArgumentValue("--name", sb.toString());
    }

    private final boolean withStartMenuShortcut;
    private final boolean withDesktopShortcut;
    private final boolean withShortcutPrompt;
}
