/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.function.Consumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.LauncherIconVerifier;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.Executor;
import jdk.jpackage.test.LinuxHelper;
import jdk.jpackage.test.AdditionalLauncher;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.Functional.ThrowingBiConsumer;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;

/*
 * @test
 * @summary jpackage create image and package with custom icons for the main and additional launcher
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile IconTest.java
 * @run main/othervm/timeout=540 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=IconTest
 */

public class IconTest {

    enum IconType {
        /**
         * Icon not specified.
         */
        DefaultIcon,

        /**
         * Explicit no icon.
         */
        NoIcon,

        /**
         * Custom icon on command line.
         */
        CustomIcon,

        /**
         * Custom icon in resource dir.
         */
        ResourceDirIcon,

        /**
         * Custom icon on command line and in resource dir.
         */
        CustomWithResourceDirIcon
    }

    enum BundleType { AppImage, Package }

    public IconTest(BundleType bundleType, IconType mainLauncherIconType,
            IconType additionalLauncherIconType, String[] extraJPackageArgs) {
        this.appImage = (bundleType == BundleType.AppImage);
        this.extraJPackageArgs = extraJPackageArgs;
        config = Map.of(
                Launcher.Main, mainLauncherIconType,
                Launcher.Additional, additionalLauncherIconType);
    }

    public IconTest(BundleType bundleType, IconType mainLauncherIconType,
            IconType additionalLauncherIconType) {
        this.appImage = (bundleType == BundleType.AppImage);
        this.extraJPackageArgs = new String[0];
        config = Map.of(
                Launcher.Main, mainLauncherIconType,
                Launcher.Additional, additionalLauncherIconType);
    }

    public IconTest(BundleType bundleType, IconType mainLauncherIconType) {
        this.appImage = (bundleType == BundleType.AppImage);
        this.extraJPackageArgs = new String[0];
        config = Map.of(Launcher.Main, mainLauncherIconType);
    }

    @Parameters
    public static Collection data() {
        List<Object[]> data = new ArrayList<>();

        var withLinuxShortcut = Set.of(IconType.DefaultIcon, IconType.NoIcon);

        for (var bundleType : BundleType.values()) {
            if (TKit.isWindows() && bundleType == BundleType.Package) {
                // On Windows icons are embedded in launcher executables in
                // application image. Nothing is changed when app image is
                // packed in msi/exe package bundle, so skip testing of package
                // bundle.
                continue;
            }
            for (var mainLauncherIconType : IconType.values()) {
                if (mainLauncherIconType == IconType.NoIcon) {
                    // `No icon` setting is not applicable for the main launcher.
                    continue;
                }

                if (TKit.isOSX()) {
                    // Custom icons not supported for additional launchers on Mac.
                    data.add(new Object[]{bundleType, mainLauncherIconType});
                    continue;
                }

                for (var additionalLauncherIconType : IconType.values()) {
                    data.add(new Object[]{bundleType, mainLauncherIconType,
                        additionalLauncherIconType});

                    if (TKit.isLinux() && bundleType == BundleType.Package
                            && withLinuxShortcut.contains(mainLauncherIconType)
                            && withLinuxShortcut.contains(
                                    additionalLauncherIconType)) {
                        data.add(new Object[]{bundleType, mainLauncherIconType,
                            additionalLauncherIconType, new String[]{
                            "--linux-shortcut"}});
                    }
                }
            }
        }
        return data;
    }

    @Test
    public void test() throws IOException {
        if (appImage) {
            JPackageCommand cmd = initAppImageTest();
            var result = cmd.executeAndAssertImageCreated();
            ThrowingConsumer.toConsumer(createInstallVerifier()).accept(cmd);
            ThrowingBiConsumer.toBiConsumer(createBundleVerifier()).accept(cmd, result);
        } else {
            PackageTest test = initPackageTest();
            test.addInstallVerifier(createInstallVerifier());
            test.addBundleVerifier(createBundleVerifier());

            test.addBundleDesktopIntegrationVerifier(config.values().stream()
                    .anyMatch(this::isWithDesktopIntegration));

            test.run(PackageTest.Action.CREATE_AND_UNPACK);
        }
    }

    boolean isWithDesktopIntegration(IconType iconType) {
        if (appImage) {
            return false;
        }
        boolean withDesktopFile = !Set.of(
                IconType.NoIcon,
                IconType.DefaultIcon).contains(iconType);
        withDesktopFile |= List.of(extraJPackageArgs).contains("--linux-shortcut");
        return withDesktopFile;
    }

    private ThrowingBiConsumer<JPackageCommand, Executor.Result> createBundleVerifier() {
        return (cmd, result) -> {
            var verifier = createConsoleOutputVerifier(cmd.name(), config.get(
                    Launcher.Main), null);
            if (verifier != null) {
                verifier.apply(result.getOutput().stream());
            }

            if (config.containsKey(Launcher.Additional)) {
                verifier = createConsoleOutputVerifier(
                        Launcher.Additional.launcherName, config.get(
                                Launcher.Additional), config.get(Launcher.Main));
                if (verifier != null) {
                    verifier.apply(result.getOutput().stream());
                }
            }
        };
    }

    private TKit.TextStreamVerifier createConsoleOutputVerifier(
            String launcherName, IconType iconType, IconType mainIconType) {
        if (iconType == IconType.DefaultIcon && mainIconType != null) {
            iconType = mainIconType;
        }
        return createConsoleOutputVerifier(launcherName, iconType);
    }

    private static TKit.TextStreamVerifier createConsoleOutputVerifier(
            String launcherName, IconType iconType) {
        String lookupString = null;
        switch (iconType) {
            case DefaultIcon:
                lookupString = String.format(
                        "Using default package resource %s [icon] (add %s%s to the resource-dir to customize)",
                        "JavaApp" + TKit.ICON_SUFFIX,
                        launcherName, TKit.ICON_SUFFIX);
                break;

            case ResourceDirIcon:
                lookupString = String.format(
                        "Using custom package resource [icon] (loaded from %s%s)",
                        launcherName, TKit.ICON_SUFFIX);
                break;

            case CustomIcon:
            case CustomWithResourceDirIcon:
                lookupString = "Using custom package resource [icon] (loaded from file";
                break;

            default:
                return null;
        }

        return TKit.assertTextStream(lookupString);
    }

    private ThrowingConsumer<JPackageCommand> createInstallVerifier() {
        LauncherIconVerifier verifier = new LauncherIconVerifier();
        switch (config.get(Launcher.Main)) {
            case NoIcon:
                verifier.setExpectedIcon(null);
                break;

            case DefaultIcon:
                verifier.setExpectedDefaultIcon();
                break;

            case CustomIcon:
                verifier.setExpectedIcon(Launcher.Main.cmdlineIcon);
                break;

            case ResourceDirIcon:
                verifier.setExpectedIcon(Launcher.Main.resourceDirIcon);
                break;

            case CustomWithResourceDirIcon:
                verifier.setExpectedIcon(Launcher.Main2.cmdlineIcon);
                break;
        }

        return cmd -> {
            verifier.applyTo(cmd);
            if (TKit.isLinux() && !cmd.isImagePackageType()) {
                Path desktopFile = LinuxHelper.getDesktopFile(cmd);
                if (isWithDesktopIntegration(config.get(Launcher.Main))) {
                    TKit.assertFileExists(desktopFile);
                } else {
                    TKit.assertPathExists(desktopFile, false);
                }
            }
        };
    }

    private void initTest(JPackageCommand cmd, PackageTest test) {
        config.entrySet().forEach(ThrowingConsumer.toConsumer(entry -> {
            initTest(entry.getKey(), entry.getValue(), cmd, test);
        }));

        ThrowingConsumer<JPackageCommand> initializer = testCmd -> {
            testCmd.saveConsoleOutput(true);
            testCmd.setFakeRuntime();
            testCmd.addArguments(extraJPackageArgs);
        };

        if (test != null) {
            test.addInitializer(initializer);
        } else {
            ThrowingConsumer.toConsumer(initializer).accept(cmd);
        }
    }

    private static void initTest(Launcher cfg, IconType iconType,
            JPackageCommand cmd, PackageTest test) throws IOException {
        Consumer<AdditionalLauncher> addLauncher = v -> {
            if (test != null) {
                v.applyTo(test);
            } else {
                v.applyTo(cmd);
            }
        };

        switch (iconType) {
            case DefaultIcon:
                if (cfg.launcherName != null) {
                    addLauncher.accept(new AdditionalLauncher(cfg.launcherName));
                }
                break;

            case NoIcon:
                if (cfg.launcherName != null) {
                    addLauncher.accept(
                            new AdditionalLauncher(cfg.launcherName).setNoIcon());
                }
                break;

            case CustomIcon:
                if (test != null) {
                    addCustomIcon(null, test, cfg.launcherName, cfg.cmdlineIcon);
                } else {
                    addCustomIcon(cmd, null, cfg.launcherName, cfg.cmdlineIcon);
                }
                break;

            case ResourceDirIcon:
                if (Launcher.PRIMARY.contains(cfg) && cfg.launcherName != null) {
                    addLauncher.accept(new AdditionalLauncher(cfg.launcherName));
                }
                if (test != null) {
                    test.addInitializer(testCmd -> {
                        addResourceDirIcon(testCmd, cfg.launcherName,
                                cfg.resourceDirIcon);
                    });
                } else {
                    addResourceDirIcon(cmd, cfg.launcherName, cfg.resourceDirIcon);
                }
                break;

            case CustomWithResourceDirIcon:
                switch (cfg) {
                    case Main:
                        initTest(Launcher.Main2, IconType.CustomIcon, cmd, test);
                        initTest(Launcher.Main2, IconType.ResourceDirIcon, cmd, test);
                        break;

                    case Additional:
                        initTest(Launcher.Additional2, IconType.CustomIcon, cmd, test);
                        initTest(Launcher.Additional2, IconType.ResourceDirIcon, cmd, test);
                        break;

                    default:
                        throw new IllegalArgumentException();
                }
                break;
        }
    }

    private JPackageCommand initAppImageTest() {
        JPackageCommand cmd = JPackageCommand.helloAppImage();
        initTest(cmd, null);
        return cmd;
    }

    private PackageTest initPackageTest() {
        PackageTest test = new PackageTest().configureHelloApp();
        initTest(null, test);
        return test;
    }

    private static void addResourceDirIcon(JPackageCommand cmd,
            String launcherName, Path iconPath) throws IOException {
        Path resourceDir = cmd.getArgumentValue("--resource-dir", () -> null,
                Path::of);
        if (resourceDir == null) {
            resourceDir = TKit.createTempDirectory("resources");
            cmd.addArguments("--resource-dir", resourceDir);
        }

        String dstIconFileName = Optional.ofNullable(launcherName).orElseGet(
                () -> cmd.name()) + TKit.ICON_SUFFIX;

        TKit.trace(String.format("Resource file: [%s] <- [%s]",
                resourceDir.resolve(dstIconFileName), iconPath));
        Files.copy(iconPath, resourceDir.resolve(dstIconFileName),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private static void addCustomIcon(JPackageCommand cmd, PackageTest test,
            String launcherName, Path iconPath) throws IOException {

        if (launcherName != null) {
            AdditionalLauncher al = new AdditionalLauncher(launcherName).setIcon(
                    iconPath);
            if (test != null) {
                al.applyTo(test);
            } else {
                al.applyTo(cmd);
            }
        } else if (test != null) {
            test.addInitializer(testCmd -> {
                testCmd.addArguments("--icon", iconPath);
            });
        } else {
            cmd.addArguments("--icon", iconPath);
        }
    }

    private enum Launcher {
        Main(null, ICONS[0], ICONS[1]),
        Main2(null, ICONS[1], ICONS[0]),
        Additional("x", ICONS[2], ICONS[3]),
        Additional2("x", ICONS[3], ICONS[2]);

        Launcher(String name, Path cmdlineIcon, Path resourceDirIcon) {
            this.launcherName = name;
            this.cmdlineIcon = cmdlineIcon;
            this.resourceDirIcon = resourceDirIcon;
        }

        private final String launcherName;
        private final Path cmdlineIcon;
        private final Path resourceDirIcon;

        private final static Set<Launcher> PRIMARY = Set.of(Main, Additional);
    }

    private final boolean appImage;
    private final Map<Launcher, IconType> config;
    private final String[] extraJPackageArgs;

    private static Path iconPath(String name) {
        return TKit.TEST_SRC_ROOT.resolve(Path.of("resources", name
                + TKit.ICON_SUFFIX));
    }

    private final static Path[] ICONS = Stream.of("icon", "icon2", "icon3",
            "icon4")
            .map(IconTest::iconPath)
            .collect(Collectors.toList()).toArray(Path[]::new);
}
