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
package jdk.jpackage.test;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jpackage.internal.AppImageFile;
import jdk.jpackage.internal.ApplicationLayout;
import jdk.jpackage.test.Functional.ThrowingBiConsumer;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.Functional.ThrowingRunnable;



/**
 * Instance of PackageTest is for configuring and running a single jpackage
 * command to produce platform specific package bundle.
 *
 * Provides methods to hook up custom configuration of jpackage command and
 * verification of the output bundle.
 */
public final class PackageTest extends RunnablePackageTest {

    public PackageTest() {
        excludeTypes = new HashSet<>();
        forTypes();
        setExpectedExitCode(0);
        namedInitializers = new HashSet<>();
        handlers = currentTypes.stream()
                .collect(Collectors.toMap(v -> v, v -> new Handler()));
        packageHandlers = createDefaultPackageHandlers();
    }

    public PackageTest excludeTypes(PackageType... types) {
        excludeTypes.addAll(Stream.of(types).collect(Collectors.toSet()));
        return forTypes(currentTypes);
    }

    public PackageTest excludeTypes(Collection<PackageType> types) {
        return excludeTypes(types.toArray(PackageType[]::new));
    }

    public PackageTest forTypes(PackageType... types) {
        Collection<PackageType> newTypes;
        if (types == null || types.length == 0) {
            newTypes = PackageType.NATIVE;
        } else {
            newTypes = Stream.of(types).collect(Collectors.toSet());
        }
        currentTypes = newTypes.stream()
                .filter(PackageType::isSupported)
                .filter(Predicate.not(excludeTypes::contains))
                .collect(Collectors.toUnmodifiableSet());
        return this;
    }

    public PackageTest forTypes(Collection<PackageType> types) {
        return forTypes(types.toArray(PackageType[]::new));
    }

    public PackageTest notForTypes(PackageType... types) {
        return notForTypes(List.of(types));
    }

    public PackageTest notForTypes(Collection<PackageType> types) {
        Set<PackageType> workset = new HashSet<>(currentTypes);
        workset.removeAll(types);
        return forTypes(workset);
    }

    public PackageTest setExpectedExitCode(int v) {
        expectedJPackageExitCode = v;
        return this;
    }

    private PackageTest addInitializer(ThrowingConsumer<JPackageCommand> v,
            String id) {
        if (id != null) {
            if (namedInitializers.contains(id)) {
                return this;
            }

            namedInitializers.add(id);
        }
        currentTypes.forEach(type -> handlers.get(type).addInitializer(
                ThrowingConsumer.toConsumer(v)));
        return this;
    }

    private PackageTest addRunOnceInitializer(ThrowingRunnable v, String id) {
        return addInitializer(new ThrowingConsumer<JPackageCommand>() {
            @Override
            public void accept(JPackageCommand unused) throws Throwable {
                if (!executed) {
                    executed = true;
                    v.run();
                }
            }

            private boolean executed;
        }, id);
    }

    public PackageTest addInitializer(ThrowingConsumer<JPackageCommand> v) {
        return addInitializer(v, null);
    }

    public PackageTest addRunOnceInitializer(ThrowingRunnable v) {
        return addRunOnceInitializer(v, null);
    }

    public PackageTest addBundleVerifier(
            ThrowingBiConsumer<JPackageCommand, Executor.Result> v) {
        currentTypes.forEach(type -> handlers.get(type).addBundleVerifier(
                ThrowingBiConsumer.toBiConsumer(v)));
        return this;
    }

    public PackageTest addBundleVerifier(ThrowingConsumer<JPackageCommand> v) {
        return addBundleVerifier(
                (cmd, unused) -> ThrowingConsumer.toConsumer(v).accept(cmd));
    }

    public PackageTest addBundlePropertyVerifier(String propertyName,
            Predicate<String> pred, String predLabel) {
        return addBundleVerifier(cmd -> {
            final String value;
            if (TKit.isLinux()) {
                value = LinuxHelper.getBundleProperty(cmd, propertyName);
            } else if (TKit.isWindows()) {
                value = WindowsHelper.getMsiProperty(cmd, propertyName);
            } else {
                throw new IllegalStateException();
            }
            TKit.assertTrue(pred.test(value), String.format(
                    "Check value of %s property %s [%s]", propertyName,
                    predLabel, value));
        });
    }

    public PackageTest addBundlePropertyVerifier(String propertyName,
            String expectedPropertyValue) {
        return addBundlePropertyVerifier(propertyName,
                expectedPropertyValue::equals, "is");
    }

    public PackageTest addBundleDesktopIntegrationVerifier(boolean integrated) {
        forTypes(PackageType.LINUX, () -> {
            LinuxHelper.addBundleDesktopIntegrationVerifier(this, integrated);
        });
        return this;
    }

    public PackageTest addInstallVerifier(ThrowingConsumer<JPackageCommand> v) {
        currentTypes.forEach(type -> handlers.get(type).addInstallVerifier(
                ThrowingConsumer.toConsumer(v)));
        return this;
    }

    public PackageTest addUninstallVerifier(ThrowingConsumer<JPackageCommand> v) {
        currentTypes.forEach(type -> handlers.get(type).addUninstallVerifier(
                ThrowingConsumer.toConsumer(v)));
        return this;
    }

    public PackageTest setPackageInstaller(Consumer<JPackageCommand> v) {
        currentTypes.forEach(
                type -> packageHandlers.get(type).installHandler = v);
        return this;
    }

    public PackageTest setPackageUnpacker(
            BiFunction<JPackageCommand, Path, Path> v) {
        currentTypes.forEach(type -> packageHandlers.get(type).unpackHandler = v);
        return this;
    }

    public PackageTest setPackageUninstaller(Consumer<JPackageCommand> v) {
        currentTypes.forEach(
                type -> packageHandlers.get(type).uninstallHandler = v);
        return this;
    }

    static void withTestFileAssociationsFile(FileAssociations fa,
            ThrowingConsumer<Path> consumer) {
        final Path testFileDefaultName = Path.of("test" + fa.getSuffix());
        TKit.withTempFile(testFileDefaultName, testFile -> {
            if (TKit.isLinux()) {
                LinuxHelper.initFileAssociationsTestFile(testFile);
            }
            consumer.accept(testFile);
        });
    }

    PackageTest addHelloAppFileAssociationsVerifier(FileAssociations fa,
            String... faLauncherDefaultArgs) {

        // Setup test app to have valid jpackage command line before
        // running check of type of environment.
        addHelloAppInitializer(null);

        forTypes(PackageType.LINUX, () -> {
            LinuxHelper.addFileAssociationsVerifier(this, fa);
        });

        String noActionMsg = "Not running file associations test";
        if (GraphicsEnvironment.isHeadless()) {
            TKit.trace(String.format(
                    "%s because running in headless environment", noActionMsg));
            return this;
        }

        addInstallVerifier(cmd -> {
            if (cmd.isFakeRuntime(noActionMsg) || cmd.isPackageUnpacked(noActionMsg)) {
                return;
            }

            withTestFileAssociationsFile(fa, testFile -> {
                testFile = testFile.toAbsolutePath().normalize();

                final Path appOutput = testFile.getParent()
                        .resolve(HelloApp.OUTPUT_FILENAME);
                Files.deleteIfExists(appOutput);

                TKit.trace(String.format("Use desktop to open [%s] file",
                        testFile));
                Desktop.getDesktop().open(testFile.toFile());
                TKit.waitForFileCreated(appOutput, 7);

                List<String> expectedArgs = new ArrayList<>(List.of(
                        faLauncherDefaultArgs));
                expectedArgs.add(testFile.toString());

                // Wait a little bit after file has been created to
                // make sure there are no pending writes into it.
                Thread.sleep(3000);
                HelloApp.verifyOutputFile(appOutput, expectedArgs,
                        Collections.emptyMap());
            });
        });

        return this;
    }

    public PackageTest forTypes(Collection<PackageType> types, Runnable action) {
        Set<PackageType> oldTypes = Set.of(currentTypes.toArray(
                PackageType[]::new));
        try {
            forTypes(types);
            action.run();
        } finally {
            forTypes(oldTypes);
        }
        return this;
    }

    public PackageTest forTypes(PackageType type, Runnable action) {
        return forTypes(List.of(type), action);
    }

    public PackageTest notForTypes(Collection<PackageType> types, Runnable action) {
        Set<PackageType> workset = new HashSet<>(currentTypes);
        workset.removeAll(types);
        return forTypes(workset, action);
    }

    public PackageTest notForTypes(PackageType type, Runnable action) {
        return notForTypes(List.of(type), action);
    }

    public PackageTest configureHelloApp() {
        return configureHelloApp(null);
    }

    public PackageTest configureHelloApp(String javaAppDesc) {
        addHelloAppInitializer(javaAppDesc);
        addInstallVerifier(HelloApp::executeLauncherAndVerifyOutput);
        return this;
    }

    public PackageTest addLauncherName(String name) {
        launcherNames.add(name);
        return this;
    }

    public final static class Group extends RunnablePackageTest {
        public Group(PackageTest... tests) {
            handlers = Stream.of(tests)
                    .map(PackageTest::createPackageTypeHandlers)
                    .flatMap(List<Consumer<Action>>::stream)
                    .collect(Collectors.toUnmodifiableList());
        }

        @Override
        protected void runAction(Action... action) {
            if (Set.of(action).contains(Action.UNINSTALL)) {
                ListIterator<Consumer<Action>> listIterator = handlers.listIterator(
                        handlers.size());
                while (listIterator.hasPrevious()) {
                    var handler = listIterator.previous();
                    List.of(action).forEach(handler::accept);
                }
            } else {
                handlers.forEach(handler -> List.of(action).forEach(handler::accept));
            }
        }

        private final List<Consumer<Action>> handlers;
    }

    final static class PackageHandlers {
        Consumer<JPackageCommand> installHandler;
        Consumer<JPackageCommand> uninstallHandler;
        BiFunction<JPackageCommand, Path, Path> unpackHandler;
    }

    @Override
    protected void runActions(List<Action[]> actions) {
        createPackageTypeHandlers().forEach(
                handler -> actions.forEach(
                        action -> List.of(action).forEach(handler::accept)));
    }

    @Override
    protected void runAction(Action... action) {
        throw new UnsupportedOperationException();
    }

    private void addHelloAppInitializer(String javaAppDesc) {
        addInitializer(
                cmd -> new HelloApp(JavaAppDesc.parse(javaAppDesc)).addTo(cmd),
                "HelloApp");
    }

    private List<Consumer<Action>> createPackageTypeHandlers() {
        return PackageType.NATIVE.stream()
                .map(type -> {
                    Handler handler = handlers.entrySet().stream()
                        .filter(entry -> !entry.getValue().isVoid())
                        .filter(entry -> entry.getKey() == type)
                        .map(entry -> entry.getValue())
                        .findAny().orElse(null);
                    Map.Entry<PackageType, Handler> result = null;
                    if (handler != null) {
                        result = Map.entry(type, handler);
                    }
                    return result;
                })
                .filter(Objects::nonNull)
                .map(entry -> createPackageTypeHandler(
                        entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Consumer<Action> createPackageTypeHandler(
            PackageType type, Handler handler) {
        return ThrowingConsumer.toConsumer(new ThrowingConsumer<Action>() {
            @Override
            public void accept(Action action) throws Throwable {
                if (action == Action.FINALIZE) {
                    if (unpackDir != null && Files.isDirectory(unpackDir)
                            && !unpackDir.startsWith(TKit.workDir())) {
                        TKit.deleteDirectoryRecursive(unpackDir);
                    }
                }

                if (aborted) {
                    return;
                }

                final JPackageCommand curCmd;
                if (Set.of(Action.INITIALIZE, Action.CREATE).contains(action)) {
                    curCmd = cmd;
                } else {
                    curCmd = cmd.createImmutableCopy();
                }

                switch (action) {
                    case UNPACK: {
                        var handler = packageHandlers.get(type).unpackHandler;
                        if (!(aborted = (handler == null))) {
                            unpackDir = TKit.createTempDirectory(
                                            String.format("unpacked-%s",
                                                    type.getName()));
                            unpackDir = handler.apply(cmd, unpackDir);
                            cmd.setUnpackedPackageLocation(unpackDir);
                        }
                        break;
                    }

                    case INSTALL: {
                        var handler = packageHandlers.get(type).installHandler;
                        if (!(aborted = (handler == null))) {
                            handler.accept(curCmd);
                        }
                        break;
                    }

                    case UNINSTALL: {
                        var handler = packageHandlers.get(type).uninstallHandler;
                        if (!(aborted = (handler == null))) {
                            handler.accept(curCmd);
                        }
                        break;
                    }

                    case CREATE:
                        handler.accept(action, curCmd);
                        aborted = (expectedJPackageExitCode != 0);
                        return;

                    default:
                        handler.accept(action, curCmd);
                        break;
                }

                if (aborted) {
                    TKit.trace(
                            String.format("Aborted [%s] action of %s command",
                                    action, cmd.getPrintableCommandLine()));
                }
            }

            private Path unpackDir;
            private boolean aborted;
            private final JPackageCommand cmd = Functional.identity(() -> {
                JPackageCommand result = new JPackageCommand();
                result.setDefaultInputOutput().setDefaultAppName();
                if (BUNDLE_OUTPUT_DIR != null) {
                    result.setArgumentValue("--dest", BUNDLE_OUTPUT_DIR.toString());
                }
                type.applyTo(result);
                return result;
            }).get();
        });
    }

    private class Handler implements BiConsumer<Action, JPackageCommand> {

        Handler() {
            initializers = new ArrayList<>();
            bundleVerifiers = new ArrayList<>();
            installVerifiers = new ArrayList<>();
            uninstallVerifiers = new ArrayList<>();
        }

        boolean isVoid() {
            return initializers.isEmpty();
        }

        void addInitializer(Consumer<JPackageCommand> v) {
            initializers.add(v);
        }

        void addBundleVerifier(BiConsumer<JPackageCommand, Executor.Result> v) {
            bundleVerifiers.add(v);
        }

        void addInstallVerifier(Consumer<JPackageCommand> v) {
            installVerifiers.add(v);
        }

        void addUninstallVerifier(Consumer<JPackageCommand> v) {
            uninstallVerifiers.add(v);
        }

        @Override
        public void accept(Action action, JPackageCommand cmd) {
            switch (action) {
                case INITIALIZE:
                    initializers.forEach(v -> v.accept(cmd));
                    if (cmd.isImagePackageType()) {
                        throw new UnsupportedOperationException();
                    }
                    cmd.executePrerequisiteActions();
                    break;

                case CREATE:
                    Executor.Result result = cmd.execute(expectedJPackageExitCode);
                    if (expectedJPackageExitCode == 0) {
                        TKit.assertFileExists(cmd.outputBundle());
                    } else {
                        TKit.assertPathExists(cmd.outputBundle(), false);
                    }
                    verifyPackageBundle(cmd, result);
                    break;

                case VERIFY_INSTALL:
                    if (expectedJPackageExitCode == 0) {
                        verifyPackageInstalled(cmd);
                    }
                    break;

                case VERIFY_UNINSTALL:
                    if (expectedJPackageExitCode == 0) {
                        verifyPackageUninstalled(cmd);
                    }
                    break;
            }
        }

        private void verifyPackageBundle(JPackageCommand cmd,
                Executor.Result result) {
            if (expectedJPackageExitCode == 0) {
                if (PackageType.LINUX.contains(cmd.packageType())) {
                    LinuxHelper.verifyPackageBundleEssential(cmd);
                }
            }
            bundleVerifiers.forEach(v -> v.accept(cmd, result));
        }

        private void verifyPackageInstalled(JPackageCommand cmd) {
            final String formatString;
            if (cmd.isPackageUnpacked()) {
                formatString = "Verify unpacked: %s";
            } else {
                formatString = "Verify installed: %s";
            }
            TKit.trace(String.format(formatString, cmd.getPrintableCommandLine()));

            if (!cmd.isRuntime()) {
                if (PackageType.WINDOWS.contains(cmd.packageType())
                        && !cmd.isPackageUnpacked(
                                "Not verifying desktop integration")) {
                    // Check main launcher
                    new WindowsHelper.DesktopIntegrationVerifier(cmd, null);
                    // Check additional launchers
                    launcherNames.forEach(name -> {
                        new WindowsHelper.DesktopIntegrationVerifier(cmd, name);
                    });
                }
            }
            cmd.assertAppLayout();

            installVerifiers.forEach(v -> v.accept(cmd));
        }

        private void verifyPackageUninstalled(JPackageCommand cmd) {
            TKit.trace(String.format("Verify uninstalled: %s",
                    cmd.getPrintableCommandLine()));
            if (!cmd.isRuntime()) {
                TKit.assertPathExists(cmd.appLauncherPath(), false);

                if (PackageType.WINDOWS.contains(cmd.packageType())) {
                    // Check main launcher
                    new WindowsHelper.DesktopIntegrationVerifier(cmd, null);
                    // Check additional launchers
                    launcherNames.forEach(name -> {
                        new WindowsHelper.DesktopIntegrationVerifier(cmd, name);
                    });
                }
            }

            Path appInstallDir = cmd.appInstallationDirectory();
            if (TKit.isLinux() && Path.of("/").equals(appInstallDir)) {
                ApplicationLayout appLayout = cmd.appLayout();
                TKit.assertPathExists(appLayout.runtimeDirectory(), false);
            } else {
                TKit.assertPathExists(appInstallDir, false);
            }

            uninstallVerifiers.forEach(v -> v.accept(cmd));
        }

        private final List<Consumer<JPackageCommand>> initializers;
        private final List<BiConsumer<JPackageCommand, Executor.Result>> bundleVerifiers;
        private final List<Consumer<JPackageCommand>> installVerifiers;
        private final List<Consumer<JPackageCommand>> uninstallVerifiers;
    }

    private static Map<PackageType, PackageHandlers> createDefaultPackageHandlers() {
        HashMap<PackageType, PackageHandlers> handlers = new HashMap<>();
        if (TKit.isLinux()) {
            handlers.put(PackageType.LINUX_DEB, LinuxHelper.createDebPackageHandlers());
            handlers.put(PackageType.LINUX_RPM, LinuxHelper.createRpmPackageHandlers());
        }

        if (TKit.isWindows()) {
            handlers.put(PackageType.WIN_MSI, WindowsHelper.createMsiPackageHandlers());
            handlers.put(PackageType.WIN_EXE, WindowsHelper.createExePackageHandlers());
        }

        if (TKit.isOSX()) {
            handlers.put(PackageType.MAC_DMG,  MacHelper.createDmgPackageHandlers());
            handlers.put(PackageType.MAC_PKG,  MacHelper.createPkgPackageHandlers());
        }

        return handlers;
    }

    private Collection<PackageType> currentTypes;
    private Set<PackageType> excludeTypes;
    private int expectedJPackageExitCode;
    private Map<PackageType, Handler> handlers;
    private Set<String> namedInitializers;
    private Map<PackageType, PackageHandlers> packageHandlers;
    private final List<String> launcherNames = new ArrayList();

    private final static File BUNDLE_OUTPUT_DIR;

    static {
        final String propertyName = "output";
        String val = TKit.getConfigProperty(propertyName);
        if (val == null) {
            BUNDLE_OUTPUT_DIR = null;
        } else {
            BUNDLE_OUTPUT_DIR = new File(val).getAbsoluteFile();

            if (!BUNDLE_OUTPUT_DIR.isDirectory()) {
                throw new IllegalArgumentException(String.format("Invalid value of %s sytem property: [%s]. Should be existing directory",
                        TKit.getConfigPropertyName(propertyName),
                        BUNDLE_OUTPUT_DIR));
            }
        }
    }
}
