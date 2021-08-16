/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import jdk.jpackage.internal.ApplicationLayout;
import jdk.jpackage.test.Functional.ThrowingBiConsumer;

public final class AdditionalLauncher {

    public AdditionalLauncher(String name) {
        this.name = name;
        this.rawProperties = new ArrayList<>();
        setPersistenceHandler(null);
    }

    public AdditionalLauncher setDefaultArguments(String... v) {
        defaultArguments = new ArrayList<>(List.of(v));
        return this;
    }

    public AdditionalLauncher addDefaultArguments(String... v) {
        if (defaultArguments == null) {
            return setDefaultArguments(v);
        }

        defaultArguments.addAll(List.of(v));
        return this;
    }

    public AdditionalLauncher setJavaOptions(String... v) {
        javaOptions = new ArrayList<>(List.of(v));
        return this;
    }

    public AdditionalLauncher addJavaOptions(String... v) {
        if (javaOptions == null) {
            return setJavaOptions(v);
        }

        javaOptions.addAll(List.of(v));
        return this;
    }

    public AdditionalLauncher addRawProperties(Map.Entry<String, String>... v) {
        return addRawProperties(List.of(v));
    }

    public AdditionalLauncher addRawProperties(
            Collection<Map.Entry<String, String>> v) {
        rawProperties.addAll(v);
        return this;
    }

    public AdditionalLauncher setShortcuts(boolean menu, boolean shortcut) {
        withMenuShortcut = menu;
        withShortcut = shortcut;
        return this;
    }

    public AdditionalLauncher setIcon(Path iconPath) {
        if (iconPath == NO_ICON) {
            throw new IllegalArgumentException();
        }

        icon = iconPath;
        return this;
    }

    public AdditionalLauncher setNoIcon() {
        icon = NO_ICON;
        return this;
    }

    public AdditionalLauncher setPersistenceHandler(
            ThrowingBiConsumer<Path, List<Map.Entry<String, String>>> handler) {
        if (handler != null) {
            createFileHandler = ThrowingBiConsumer.toBiConsumer(handler);
        } else {
            createFileHandler = TKit::createPropertiesFile;
        }
        return this;
    }

    public void applyTo(JPackageCommand cmd) {
        cmd.addPrerequisiteAction(this::initialize);
        cmd.addVerifyAction(this::verify);
    }

    public void applyTo(PackageTest test) {
        test.addLauncherName(name);
        test.addInitializer(this::initialize);
        test.addInstallVerifier(this::verify);
    }

    private void initialize(JPackageCommand cmd) {
        final Path propsFile = TKit.workDir().resolve(name + ".properties");

        cmd.addArguments("--add-launcher", String.format("%s=%s", name,
                    propsFile));

        List<Map.Entry<String, String>> properties = new ArrayList<>();
        if (defaultArguments != null) {
            properties.add(Map.entry("arguments",
                    JPackageCommand.escapeAndJoin(defaultArguments)));
        }

        if (javaOptions != null) {
            properties.add(Map.entry("java-options",
                    JPackageCommand.escapeAndJoin(javaOptions)));
        }

        if (icon != null) {
            final String iconPath;
            if (icon == NO_ICON) {
                iconPath = "";
            } else {
                iconPath = icon.toAbsolutePath().toString().replace('\\', '/');
            }
            properties.add(Map.entry("icon", iconPath));
        }

        if (withShortcut != null) {
            if (TKit.isLinux()) {
                properties.add(Map.entry("linux-shortcut", withShortcut.toString()));
            } else if (TKit.isWindows()) {
                properties.add(Map.entry("win-shortcut", withShortcut.toString()));
            }
        }

        if (TKit.isWindows() && withMenuShortcut != null)  {
            properties.add(Map.entry("win-menu", withMenuShortcut.toString()));
        }

        properties.addAll(rawProperties);

        createFileHandler.accept(propsFile, properties);
    }

    private static Path iconInResourceDir(JPackageCommand cmd,
            String launcherName) {
        Path resourceDir = cmd.getArgumentValue("--resource-dir", () -> null,
                Path::of);
        if (resourceDir != null) {
            Path icon = resourceDir.resolve(
                    Optional.ofNullable(launcherName).orElseGet(() -> cmd.name())
                    + TKit.ICON_SUFFIX);
            if (Files.exists(icon)) {
                return icon;
            }
        }
        return null;
    }

    private void verifyIcon(JPackageCommand cmd) throws IOException {
        var verifier = new LauncherIconVerifier().setLauncherName(name);

        if (TKit.isOSX()) {
            // On Mac should be no icon files for additional launchers.
            verifier.applyTo(cmd);
            return;
        }

        boolean withLinuxDesktopFile = false;

        final Path effectiveIcon = Optional.ofNullable(icon).orElseGet(
                () -> iconInResourceDir(cmd, name));
        while (effectiveIcon != NO_ICON) {
            if (effectiveIcon != null) {
                withLinuxDesktopFile = Boolean.FALSE != withShortcut;
                verifier.setExpectedIcon(effectiveIcon);
                break;
            }

            Path customMainLauncherIcon = cmd.getArgumentValue("--icon",
                    () -> iconInResourceDir(cmd, null), Path::of);
            if (customMainLauncherIcon != null) {
                withLinuxDesktopFile = Boolean.FALSE != withShortcut;
                verifier.setExpectedIcon(customMainLauncherIcon);
                break;
            }

            verifier.setExpectedDefaultIcon();
            break;
        }

        if (TKit.isLinux() && !cmd.isImagePackageType()) {
            if (effectiveIcon != NO_ICON && !withLinuxDesktopFile) {
                withLinuxDesktopFile = (Boolean.FALSE != withShortcut) &&
                        Stream.of("--linux-shortcut").anyMatch(cmd::hasArgument);
                verifier.setExpectedDefaultIcon();
            }
            Path desktopFile = LinuxHelper.getDesktopFile(cmd, name);
            if (withLinuxDesktopFile) {
                TKit.assertFileExists(desktopFile);
            } else {
                TKit.assertPathExists(desktopFile, false);
            }
        }

        verifier.applyTo(cmd);
    }

    private void verifyShortcuts(JPackageCommand cmd) throws IOException {
        if (TKit.isLinux() && !cmd.isImagePackageType()
                && withShortcut != null) {
            Path desktopFile = LinuxHelper.getDesktopFile(cmd, name);
            if (withShortcut) {
                TKit.assertFileExists(desktopFile);
            } else {
                TKit.assertPathExists(desktopFile, false);
            }
        }
    }

    private void verify(JPackageCommand cmd) throws IOException {
        verifyIcon(cmd);
        verifyShortcuts(cmd);

        Path launcherPath = cmd.appLauncherPath(name);

        TKit.assertExecutableFileExists(launcherPath);

        if (!cmd.canRunLauncher(String.format(
                "Not running %s launcher", launcherPath))) {
            return;
        }

        HelloApp.assertApp(launcherPath)
        .addDefaultArguments(Optional
                .ofNullable(defaultArguments)
                .orElseGet(() -> List.of(cmd.getAllArgumentValues("--arguments"))))
        .addJavaOptions(Optional
                .ofNullable(javaOptions)
                .orElseGet(() -> List.of(cmd.getAllArgumentValues("--java-options"))))
        .executeAndVerifyOutput();
    }

    private List<String> javaOptions;
    private List<String> defaultArguments;
    private Path icon;
    private final String name;
    private final List<Map.Entry<String, String>> rawProperties;
    private BiConsumer<Path, List<Map.Entry<String, String>>> createFileHandler;
    private Boolean withMenuShortcut;
    private Boolean withShortcut;

    private final static Path NO_ICON = Path.of("");
}
