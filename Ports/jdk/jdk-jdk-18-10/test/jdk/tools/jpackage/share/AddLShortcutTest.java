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
import java.io.File;
import java.util.Map;
import java.lang.invoke.MethodHandles;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.FileAssociations;
import jdk.jpackage.test.AdditionalLauncher;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Annotations.Test;

/**
 * Test --add-launcher parameter with shortcuts (platform permitting).
 * Output of the test should be AddLShortcutTest*.* installer.
 * The output installer should provide the same functionality as the
 * default installer (see description of the default installer in
 * SimplePackageTest.java) plus install extra application launchers with and
 * without various shortcuts to be tested manually.
 */

/*
 * @test
 * @summary jpackage with --add-launcher
 * @key jpackagePlatformPackage
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile AddLShortcutTest.java
 * @run main/othervm/timeout=540 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=AddLShortcutTest
 */

public class AddLShortcutTest {

    @Test
    public void test() {
        // Configure several additional launchers with each combination of
        // possible shortcut hints in add-launcher property file.
        // default is true so Foo (no property), and Bar (properties set to "true")
        // will have shortcuts while other launchers with some properties set
        // to "false" will have none.

        PackageTest packageTest = new PackageTest().configureHelloApp();
        packageTest.addInitializer(cmd -> {
            cmd.addArguments("--arguments", "Duke", "--arguments", "is",
                    "--arguments", "the", "--arguments", "King");
            if (TKit.isWindows()) {
                cmd.addArguments("--win-shortcut", "--win-menu");
            } else if (TKit.isLinux()) {
                cmd.addArguments("--linux-shortcut");
            }
        });

        new FileAssociations(
                MethodHandles.lookup().lookupClass().getSimpleName()).applyTo(
                packageTest);

        new AdditionalLauncher("Foo")
                .setDefaultArguments("yep!")
                .setIcon(GOLDEN_ICON)
                .applyTo(packageTest);

        new AdditionalLauncher("Bar")
                .setDefaultArguments("one", "two", "three")
                .setIcon(GOLDEN_ICON)
                .setShortcuts(true, true)
                .applyTo(packageTest);

        new AdditionalLauncher("Launcher3")
                .setDefaultArguments()
                .setIcon(GOLDEN_ICON)
                .setShortcuts(false, false)
                .applyTo(packageTest);

        new AdditionalLauncher("Launcher4")
                .setDefaultArguments()
                .setIcon(GOLDEN_ICON)
                .setShortcuts(true, false)
                .applyTo(packageTest);

        new AdditionalLauncher("Launcher5")
                .setDefaultArguments()
                .setIcon(GOLDEN_ICON)
                .setShortcuts(false, true)
                .applyTo(packageTest);

        packageTest.run();
    }

    private final static Path GOLDEN_ICON = TKit.TEST_SRC_ROOT.resolve(Path.of(
            "resources", "icon" + TKit.ICON_SUFFIX));
}
