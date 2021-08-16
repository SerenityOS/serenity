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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.WindowsHelper;
import jdk.jpackage.test.TKit;

/**
 * Test both --win-upgrade-uuid and --app-version parameters. Output of the test
 * should be WinUpgradeUUIDTest-1.0.exe and WinUpgradeUUIDTest-2.0.exe
 * installers. Both output installers should provide the same functionality as
 * the default installer (see description of the default installer in
 * SimplePackageTest.java) but have the same product code and different
 * versions. Running WinUpgradeUUIDTest-2.0.exe installer should automatically
 * uninstall older version of the test application previously installed with
 * WinUpgradeUUIDTest-1.0.exe installer.
 */

/*
 * @test
 * @summary jpackage with --win-upgrade-uuid and --app-version
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @requires (jpackage.test.SQETest != null)
 * @build jdk.jpackage.test.*
 * @requires (os.family == "windows")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile WinUpgradeUUIDTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=WinUpgradeUUIDTest.test
 */

/*
 * @test
 * @summary jpackage with --win-upgrade-uuid and --app-version
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @requires (jpackage.test.SQETest == null)
 * @build jdk.jpackage.test.*
 * @requires (os.family == "windows")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile WinUpgradeUUIDTest.java
 * @run main/othervm/timeout=540 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=WinUpgradeUUIDTest
 */

public class WinUpgradeUUIDTest {

    @Test
    public static void test() {
        Supplier<PackageTest> init = () -> {
            final UUID upgradeCode = UUID.fromString(
                    "F0B18E75-52AD-41A2-BC86-6BE4FCD50BEB");
            return new PackageTest()
                .forTypes(PackageType.WIN_MSI)
                .addBundlePropertyVerifier("UpgradeCode", value -> {
                    if (value.startsWith("{")) {
                        value = value.substring(1);
                    }
                    if (value.endsWith("}")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    return UUID.fromString(value).equals(upgradeCode);
                }, "is a match with")
                .forTypes(PackageType.WINDOWS)
                .configureHelloApp()
                .addInitializer(cmd -> cmd.addArguments("--win-upgrade-uuid",
                        upgradeCode.toString())) ;

        };

        // Replace real uninstall command for the first package with nop action.
        // It will be uninstalled automatically when the second
        // package will be installed.
        // However uninstall verification for the first package will be executed.
        PackageTest test1 = init.get().setPackageUninstaller(cmd -> {});

        PackageTest test2 = init.get().addInitializer(cmd -> {
            cmd.setArgumentValue("--app-version", "2.0");
            cmd.setArgumentValue("--arguments", "bar");
        });

        new PackageTest.Group(test1, test2).run();
    }

    /**
     * Running jpackage multiple times with the same parameters should produce
     * MSI packages with the same UpgradeCode and ProductCode values.
     */
    @Test
    public static void testUUIDs() {
        Supplier<PackageTest> init = () -> {
            return new PackageTest()
            .forTypes(PackageType.WIN_MSI)
            .configureHelloApp()
            .addInitializer(cmd -> {
                cmd.setFakeRuntime();
                cmd.setArgumentValue("--dest", TKit.createTempDirectory("output"));
            });
        };

        PackageTest test1 = init.get();
        PackageTest test2 = init.get();
        PackageTest test3 = init.get().addInitializer(cmd -> {
            cmd.addArguments("--app-version", "2.0");
        });
        PackageTest test4 = init.get().addInitializer(cmd -> {
            cmd.addArguments("--app-version", "2.0");
            cmd.addArguments("--vendor", "Foo Inc.");
        });

        PackageTest[] tests = new PackageTest[] { test1, test2, test3, test4 };

        var productCodeVerifier = createPropertyVerifier("ProductCode", tests);
        var upgradeCodeVerifier = createPropertyVerifier("UpgradeCode", tests);

        List.of(tests).forEach(test -> {
            test.run(PackageTest.Action.CREATE);
        });

        productCodeVerifier.assertEquals(test1, test2);
        productCodeVerifier.assertNotEquals(test1, test3);
        productCodeVerifier.assertNotEquals(test1, test4);
        productCodeVerifier.assertNotEquals(test3, test4);

        upgradeCodeVerifier.assertEquals(test1, test2);
        upgradeCodeVerifier.assertEquals(test1, test3);
        upgradeCodeVerifier.assertNotEquals(test1, test4);
    }

    private static PropertyVerifier createPropertyVerifier(String propertyName,
            PackageTest... tests) {
        Map<PackageTest, Map.Entry<String, String>> properties = new HashMap<>();
        List.of(tests).forEach(test -> {
            test.addBundleVerifier(cmd -> {
                properties.put(test, Map.entry(cmd.getPrintableCommandLine(),
                        WindowsHelper.getMsiProperty(cmd, propertyName)));
            });
        });

        return new PropertyVerifier() {
            @Override
            protected String propertyName() {
                return propertyName;
            }

            @Override
            protected Map<PackageTest, Map.Entry<String, String>> propertyValues() {
                return properties;
            }
        };
    }

    static abstract class PropertyVerifier {
        void assertEquals(PackageTest x, PackageTest y) {
            var entryX = propertyValues().get(x);
            var entryY = propertyValues().get(y);
            // if MsiBundler is not supported, these will be null
            if (entryX != null && entryY != null) {
                TKit.assertEquals(entryX.getValue(), entryY.getValue(),
                    String.format(
                            "Check %s is the same for %s and %s command lines",
                            propertyName(), entryX.getKey(), entryY.getKey()));
            }
        }

        void assertNotEquals(PackageTest x, PackageTest y) {
            var entryX = propertyValues().get(x);
            var entryY = propertyValues().get(y);
            // if MsiBundler is not supported, these will be null
            if (entryX != null && entryY != null) {
                TKit.assertNotEquals(entryX.getValue(), entryY.getValue(),
                    String.format(
                            "Check %s is different for %s and %s command lines",
                            propertyName(), entryX.getKey(), entryY.getKey()));
            }
        }

        protected abstract String propertyName();
        protected abstract Map<PackageTest, Map.Entry<String, String>> propertyValues();
    }
}
