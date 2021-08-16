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

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jpackage.internal.AppImageFile;
import jdk.jpackage.internal.ApplicationLayout;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.Functional.ThrowingFunction;
import jdk.jpackage.test.Functional.ThrowingSupplier;

/**
 * jpackage command line with prerequisite actions. Prerequisite actions can be
 * anything. The simplest is to compile test application and pack in a jar for
 * use on jpackage command line.
 */
public final class JPackageCommand extends CommandArguments<JPackageCommand> {

    public JPackageCommand() {
        prerequisiteActions = new Actions();
        verifyActions = new Actions();
    }

    public JPackageCommand(JPackageCommand cmd) {
        args.addAll(cmd.args);
        withToolProvider = cmd.withToolProvider;
        saveConsoleOutput = cmd.saveConsoleOutput;
        suppressOutput = cmd.suppressOutput;
        ignoreDefaultRuntime = cmd.ignoreDefaultRuntime;
        ignoreDefaultVerbose = cmd.ignoreDefaultVerbose;
        immutable = cmd.immutable;
        prerequisiteActions = new Actions(cmd.prerequisiteActions);
        verifyActions = new Actions(cmd.verifyActions);
    }

    JPackageCommand createImmutableCopy() {
        JPackageCommand reply = new JPackageCommand(this);
        reply.immutable = true;
        return reply;
    }

    public JPackageCommand setArgumentValue(String argName, String newValue) {
        verifyMutable();

        String prevArg = null;
        ListIterator<String> it = args.listIterator();
        while (it.hasNext()) {
            String value = it.next();
            if (prevArg != null && prevArg.equals(argName)) {
                if (newValue != null) {
                    it.set(newValue);
                } else {
                    it.remove();
                    it.previous();
                    it.remove();
                }
                return this;
            }
            prevArg = value;
        }

        if (newValue != null) {
            addArguments(argName, newValue);
        }

        return this;
    }

    public JPackageCommand setArgumentValue(String argName, Path newValue) {
        return setArgumentValue(argName, newValue.toString());
    }

    public JPackageCommand removeArgumentWithValue(String argName) {
        return setArgumentValue(argName, (String)null);
    }

    public JPackageCommand removeArgument(String argName) {
        args = args.stream().filter(arg -> !arg.equals(argName)).collect(
                Collectors.toList());
        return this;
    }

    public boolean hasArgument(String argName) {
        return args.contains(argName);
    }

    public <T> T getArgumentValue(String argName,
            Function<JPackageCommand, T> defaultValueSupplier,
            Function<String, T> stringConverter) {
        String prevArg = null;
        for (String arg : args) {
            if (prevArg != null && prevArg.equals(argName)) {
                return stringConverter.apply(arg);
            }
            prevArg = arg;
        }
        if (defaultValueSupplier != null) {
            return defaultValueSupplier.apply(this);
        }
        return null;
    }

    public String getArgumentValue(String argName,
            Function<JPackageCommand, String> defaultValueSupplier) {
        return getArgumentValue(argName, defaultValueSupplier, v -> v);
    }

    public <T> T getArgumentValue(String argName,
            Supplier<T> defaultValueSupplier,
            Function<String, T> stringConverter) {
        return getArgumentValue(argName, (unused) -> defaultValueSupplier.get(),
                stringConverter);
    }

    public String getArgumentValue(String argName,
            Supplier<String> defaultValueSupplier) {
        return getArgumentValue(argName, defaultValueSupplier, v -> v);
    }

    public String getArgumentValue(String argName) {
        return getArgumentValue(argName, (Supplier<String>)null);
    }

    public String[] getAllArgumentValues(String argName) {
        List<String> values = new ArrayList<>();
        String prevArg = null;
        for (String arg : args) {
            if (prevArg != null && prevArg.equals(argName)) {
                values.add(arg);
            }
            prevArg = arg;
        }
        return values.toArray(String[]::new);
    }

    public JPackageCommand addArguments(String name, Path value) {
        return addArguments(name, value.toString());
    }

    public boolean isImagePackageType() {
        return PackageType.IMAGE == getArgumentValue("--type",
                () -> null, PACKAGE_TYPES::get);
    }

    public PackageType packageType() {
        // Don't try to be in sync with jpackage defaults. Keep it simple:
        // if no `--type` explicitely set on the command line, consider
        // this is operator's fault.
        return getArgumentValue("--type",
                () -> {
                    throw new IllegalStateException("Package type not set");
                }, PACKAGE_TYPES::get);
    }

    public Path outputDir() {
        return getArgumentValue("--dest", () -> Path.of("."), Path::of);
    }

    public Path inputDir() {
        return getArgumentValue("--input", () -> null, Path::of);
    }

    public String version() {
        return getArgumentValue("--app-version", () -> "1.0");
    }

    public String name() {
        String appImage = getArgumentValue("--app-image", () -> null);
        if (appImage != null) {
            String name =  AppImageFile.extractAppName(Path.of(appImage));
            // can be null if using foreign app-image
            return ((name != null) ? name : getArgumentValue("--name"));
        }
        return getArgumentValue("--name", () -> getArgumentValue("--main-class"));
    }

    public String installerName() {
        verifyIsOfType(PackageType.NATIVE);
        String installerName = getArgumentValue("--name",
                () -> getArgumentValue("--main-class", () -> null));
        if (installerName == null) {
            String appImage = getArgumentValue("--app-image");
            if (appImage != null) {
                installerName = AppImageFile.extractAppName(Path.of(appImage));
            }
        }
        return installerName;
    }

    public boolean isRuntime() {
        return  hasArgument("--runtime-image")
                && !hasArgument("--main-jar")
                && !hasArgument("--module")
                && !hasArgument("--app-image");
    }

    public JPackageCommand setDefaultInputOutput() {
        setArgumentValue("--input", TKit.workDir().resolve("input"));
        setArgumentValue("--dest", TKit.workDir().resolve("output"));
        return this;
    }

    public JPackageCommand setFakeRuntime() {
        verifyMutable();

        ThrowingConsumer<Path> createBulkFile = path -> {
            Files.createDirectories(path.getParent());
            try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                byte[] bytes = new byte[4 * 1024];
                new SecureRandom().nextBytes(bytes);
                out.write(bytes);
            }
        };

        addPrerequisiteAction(cmd -> {
            Path fakeRuntimeDir = TKit.workDir().resolve("fake_runtime");

            TKit.trace(String.format("Init fake runtime in [%s] directory",
                    fakeRuntimeDir));

            Files.createDirectories(fakeRuntimeDir);

            if (TKit.isLinux()) {
                // Need to make the code in rpm spec happy as it assumes there is
                // always something in application image.
                fakeRuntimeDir.resolve("bin").toFile().mkdir();
            }

            if (TKit.isOSX()) {
                // Make MacAppImageBuilder happy
                createBulkFile.accept(fakeRuntimeDir.resolve(Path.of(
                        "lib/jli/libjli.dylib")));
            }

            // Mak sure fake runtime takes some disk space.
            // Package bundles with 0KB size are unexpected and considered
            // an error by PackageTest.
            createBulkFile.accept(fakeRuntimeDir.resolve(Path.of("bin", "bulk")));

            cmd.addArguments("--runtime-image", fakeRuntimeDir);
        });

        return this;
    }

    JPackageCommand addPrerequisiteAction(ThrowingConsumer<JPackageCommand> action) {
        verifyMutable();
        prerequisiteActions.add(action);
        return this;
    }

    JPackageCommand addVerifyAction(ThrowingConsumer<JPackageCommand> action) {
        verifyMutable();
        verifyActions.add(action);
        return this;
    }

    /**
     * Shorthand for {@code helloAppImage(null)}.
     */
    public static JPackageCommand helloAppImage() {
        JavaAppDesc javaAppDesc = null;
        return helloAppImage(javaAppDesc);
    }

    /**
     * Creates new JPackageCommand instance configured with the test Java app.
     * For the explanation of `javaAppDesc` parameter, see documentation for
     * #JavaAppDesc.parse() method.
     *
     * @param javaAppDesc Java application description
     * @return this
     */
    public static JPackageCommand helloAppImage(String javaAppDesc) {
        final JavaAppDesc appDesc;
        if (javaAppDesc == null) {
            appDesc = null;
        } else {
            appDesc = JavaAppDesc.parse(javaAppDesc);
        }
        return helloAppImage(appDesc);
    }

    public static JPackageCommand helloAppImage(JavaAppDesc javaAppDesc) {
        JPackageCommand cmd = new JPackageCommand();
        cmd.setDefaultInputOutput().setDefaultAppName();
        cmd.setPackageType(PackageType.IMAGE);
        new HelloApp(javaAppDesc).addTo(cmd);
        return cmd;
    }

    public JPackageCommand setPackageType(PackageType type) {
        verifyMutable();
        type.applyTo(this);
        return this;
    }

    public JPackageCommand setDefaultAppName() {
        return addArguments("--name", TKit.getCurrentDefaultAppName());
    }

    /**
     * Returns path to output bundle of configured jpackage command.
     *
     * If this is build image command, returns path to application image directory.
     */
    public Path outputBundle() {
        final String bundleName;
        if (isImagePackageType()) {
            String dirName = name();
            if (TKit.isOSX()) {
                dirName = dirName + ".app";
            }
            bundleName = dirName;
        } else if (TKit.isLinux()) {
            bundleName = LinuxHelper.getBundleName(this);
        } else if (TKit.isWindows()) {
            bundleName = WindowsHelper.getBundleName(this);
        } else if (TKit.isOSX()) {
            bundleName = MacHelper.getBundleName(this);
        } else {
            throw TKit.throwUnknownPlatformError();
        }

        return outputDir().resolve(bundleName);
    }

    /**
     * Returns application layout.
     *
     * If this is build image command, returns application image layout of the
     * output bundle relative to output directory. Otherwise returns layout of
     * installed application relative to the root directory.
     *
     * If this command builds Java runtime, not an application, returns
     * corresponding layout.
     */
    public ApplicationLayout appLayout() {
        ApplicationLayout layout = onLinuxPackageInstallDir(null,
                installDir -> {
                    String packageName = LinuxHelper.getPackageName(this);
                    // Convert '/usr' to 'usr'. It will be set to proper root in
                    // subsequent ApplicationLayout.resolveAt() call.
                    return ApplicationLayout.linuxUsrTreePackageImage(Path.of(
                            "/").relativize(installDir), packageName);
                });

        if (layout != null) {
        } else if (isRuntime()) {
            layout = ApplicationLayout.javaRuntime();
        } else {
            layout = ApplicationLayout.platformAppImage();
        }

        if (isImagePackageType()) {
            return layout.resolveAt(outputBundle());
        }

        return layout.resolveAt(pathToUnpackedPackageFile(
                appInstallationDirectory()));
    }

    /**
     * Returns path to package file in unpacked package directory or the given
     * path if the package is not unpacked.
     */
    public Path pathToUnpackedPackageFile(Path path) {
        Path unpackDir = unpackedPackageDirectory();
        if (unpackDir == null) {
            return path;
        }
        return unpackDir.resolve(TKit.removeRootFromAbsolutePath(path));
    }

    Path unpackedPackageDirectory() {
        verifyIsOfType(PackageType.NATIVE);
        return getArgumentValue(UNPACKED_PATH_ARGNAME, () -> null, Path::of);
    }

    /**
     * Returns path to directory where application will be installed or null if
     * this is build image command.
     *
     * E.g. on Linux for app named Foo default the function will return
     * `/opt/foo`.
     * On Linux for install directory in `/usr` tree the function returns `/`.
     *
     */
    public Path appInstallationDirectory() {
        if (isImagePackageType()) {
            return null;
        }

        if (TKit.isLinux()) {
            return onLinuxPackageInstallDir(installDir -> installDir.resolve(
                    LinuxHelper.getPackageName(this)),
                    installDir -> Path.of("/"));
        }

        if (TKit.isWindows()) {
            return WindowsHelper.getInstallationDirectory(this);
        }

        if (TKit.isOSX()) {
            return MacHelper.getInstallationDirectory(this);
        }

        throw TKit.throwUnknownPlatformError();
    }

    /**
     * Returns path to application's Java runtime.
     * If the command will package Java runtime only, returns correct path to
     * runtime directory.
     *
     * E.g.:
     * [jpackage --name Foo --type rpm] -> `/opt/foo/lib/runtime`
     * [jpackage --name Foo --type app-image --dest bar] -> `bar/Foo/lib/runtime`
     * [jpackage --name Foo --type rpm --runtime-image java] -> `/opt/foo`
     */
    public Path appRuntimeDirectory() {
        return appLayout().runtimeDirectory();
    }

    /**
     * Returns path for application launcher with the given name.
     *
     * E.g.: [jpackage --name Foo --type rpm] -> `/opt/foo/bin/Foo`
     * [jpackage --name Foo --type app-image --dest bar] ->
     * `bar/Foo/bin/Foo`
     *
     * @param launcherName name of launcher or {@code null} for the main
     * launcher
     *
     * @throws IllegalArgumentException if the command is configured for
     * packaging Java runtime
     */
    public Path appLauncherPath(String launcherName) {
        verifyNotRuntime();
        if (launcherName == null) {
            launcherName = name();
        }

        if (TKit.isWindows()) {
            launcherName = launcherName + ".exe";
        }

        return appLayout().launchersDirectory().resolve(launcherName);
    }

    /**
     * Shorthand for {@code appLauncherPath(null)}.
     */
    public Path appLauncherPath() {
        return appLauncherPath(null);
    }

    private void verifyNotRuntime() {
        if (isRuntime()) {
            throw new IllegalArgumentException("Java runtime packaging");
        }
    }

    /**
     * Returns path to .cfg file of the given application launcher.
     *
     * E.g.:
     * [jpackage --name Foo --type rpm] -> `/opt/foo/lib/app/Foo.cfg`
     * [jpackage --name Foo --type app-image --dest bar] -> `bar/Foo/lib/app/Foo.cfg`
     *
     * @param launcher name of launcher or {@code null} for the main launcher
     *
     * @throws IllegalArgumentException if the command is configured for
     * packaging Java runtime
     */
    public Path appLauncherCfgPath(String launcherName) {
        verifyNotRuntime();
        if (launcherName == null) {
            launcherName = name();
        }
        return appLayout().appDirectory().resolve(launcherName + ".cfg");
    }

    public boolean isFakeRuntime(String msg) {
        Path runtimeDir = appRuntimeDirectory();

        final Collection<Path> criticalRuntimeFiles;
        if (TKit.isWindows()) {
            criticalRuntimeFiles = WindowsHelper.CRITICAL_RUNTIME_FILES;
        } else if (TKit.isLinux()) {
            criticalRuntimeFiles = LinuxHelper.CRITICAL_RUNTIME_FILES;
        } else if (TKit.isOSX()) {
            criticalRuntimeFiles = MacHelper.CRITICAL_RUNTIME_FILES;
        } else {
            throw TKit.throwUnknownPlatformError();
        }

        if (criticalRuntimeFiles.stream().filter(
                v -> runtimeDir.resolve(v).toFile().exists()).findFirst().orElse(
                        null) == null) {
            // Fake runtime
            TKit.trace(String.format(
                    "%s because application runtime directory [%s] is incomplete",
                    msg, runtimeDir));
            return true;
        }
        return false;
    }

    public boolean canRunLauncher(String msg) {
        if (isFakeRuntime(msg)) {
            return false;
        }

        if (isPackageUnpacked()) {
            return Boolean.FALSE != onLinuxPackageInstallDir(null, installDir -> {
                TKit.trace(String.format(
                    "%s because the package in [%s] directory is not installed ",
                    msg, installDir));
                return Boolean.FALSE;
            });
        }

        return true;
    }

    public boolean isPackageUnpacked(String msg) {
        if (isPackageUnpacked()) {
            TKit.trace(String.format(
                    "%s because package was unpacked, not installed", msg));
            return true;
        }
        return false;
    }

    public boolean isPackageUnpacked() {
        return hasArgument(UNPACKED_PATH_ARGNAME);
    }

    public static void useToolProviderByDefault() {
        defaultWithToolProvider = true;
    }

    public static void useExecutableByDefault() {
        defaultWithToolProvider = false;
    }

    public JPackageCommand useToolProvider(boolean v) {
        verifyMutable();
        withToolProvider = v;
        return this;
    }

    public JPackageCommand saveConsoleOutput(boolean v) {
        verifyMutable();
        saveConsoleOutput = v;
        return this;
    }

    public JPackageCommand dumpOutput(boolean v) {
        verifyMutable();
        suppressOutput = !v;
        return this;
    }

    public JPackageCommand ignoreDefaultRuntime(boolean v) {
        verifyMutable();
        ignoreDefaultRuntime = v;
        return this;
    }

    public JPackageCommand ignoreDefaultVerbose(boolean v) {
        verifyMutable();
        ignoreDefaultVerbose = v;
        return this;
    }

    public boolean isWithToolProvider() {
        return Optional.ofNullable(withToolProvider).orElse(
                defaultWithToolProvider);
    }

    public JPackageCommand executePrerequisiteActions() {
        prerequisiteActions.run();
        return this;
    }

    public JPackageCommand executeVerifyActions() {
        verifyActions.run();
        return this;
    }

    private Executor createExecutor() {
        Executor exec = new Executor()
                .saveOutput(saveConsoleOutput).dumpOutput(!suppressOutput)
                .addArguments(args);

        if (isWithToolProvider()) {
            exec.setToolProvider(JavaTool.JPACKAGE);
        } else {
            exec.setExecutable(JavaTool.JPACKAGE);
            if (TKit.isWindows()) {
                exec.setWindowsTmpDir(System.getProperty("java.io.tmpdir"));
            }
        }

        return exec;
    }

    public Executor.Result execute() {
        return execute(0);
    }

    public Executor.Result execute(int expectedExitCode) {
        executePrerequisiteActions();

        if (isImagePackageType()) {
            TKit.deleteDirectoryContentsRecursive(outputDir());
        } else if (ThrowingSupplier.toSupplier(() -> TKit.deleteIfExists(
                outputBundle())).get()) {
            TKit.trace(
                    String.format("Deleted [%s] file before running jpackage",
                            outputBundle()));
        }

        Path resourceDir = getArgumentValue("--resource-dir", () -> null, Path::of);
        if (resourceDir != null && Files.isDirectory(resourceDir)) {
            TKit.trace(String.format("Files in [%s] resource dir:",
                    resourceDir));
            try (var files = Files.walk(resourceDir, 1)) {
                files.sequential()
                        .filter(Predicate.not(resourceDir::equals))
                        .map(path -> String.format("[%s]", path.getFileName()))
                        .forEachOrdered(TKit::trace);
                TKit.trace("Done");
            } catch (IOException ex) {
                TKit.trace(String.format(
                        "Failed to list files in [%s] resource directory: %s",
                        resourceDir, ex));
            }
        }

        Executor.Result result = new JPackageCommand(this)
                .adjustArgumentsBeforeExecution()
                .createExecutor()
                .execute(expectedExitCode);

        if (result.exitCode == 0) {
            executeVerifyActions();
        }

        return result;
    }

    public Executor.Result executeAndAssertHelloAppImageCreated() {
        Executor.Result result = executeAndAssertImageCreated();
        HelloApp.executeLauncherAndVerifyOutput(this);
        return result;
    }

    public Executor.Result executeAndAssertImageCreated() {
        Executor.Result result = execute();
        assertImageCreated();
        return result;
    }

    public JPackageCommand assertImageCreated() {
        verifyIsOfType(PackageType.IMAGE);
        assertAppLayout();
        return this;
    }

    JPackageCommand assertAppLayout() {
        if (isPackageUnpacked() || isImagePackageType()) {
            final Path rootDir = isPackageUnpacked() ? pathToUnpackedPackageFile(
                    appInstallationDirectory()) : outputBundle();
            final Path appImageFileName = AppImageFile.getPathInAppImage(
                    Path.of("")).getFileName();
            try (Stream<Path> walk = ThrowingSupplier.toSupplier(
                    () -> Files.walk(rootDir)).get()) {
                List<String> appImageFiles = walk
                        .filter(path -> path.getFileName().equals(appImageFileName))
                        .map(Path::toString)
                        .collect(Collectors.toList());
                if (isImagePackageType() || (TKit.isOSX() && !isRuntime())) {
                    List<String> expected = List.of(
                            AppImageFile.getPathInAppImage(rootDir).toString());
                    TKit.assertStringListEquals(expected, appImageFiles,
                            String.format(
                                    "Check there is only one file with [%s] name in the package",
                                    appImageFileName));
                } else {
                    TKit.assertStringListEquals(List.of(), appImageFiles,
                            String.format(
                                    "Check there are no files with [%s] name in the package",
                                    appImageFileName));
                }
            }
        } else if (TKit.isOSX()) {
            TKit.assertFileExists(AppImageFile.getPathInAppImage(
                    appInstallationDirectory()));
        } else {
            TKit.assertPathExists(AppImageFile.getPathInAppImage(
                    appInstallationDirectory()), false);
        }

        TKit.assertDirectoryExists(appRuntimeDirectory());

        if (!isRuntime()) {
            TKit.assertExecutableFileExists(appLauncherPath());
            TKit.assertFileExists(appLauncherCfgPath(null));

            if (TKit.isOSX()) {
                TKit.assertFileExists(appRuntimeDirectory().resolve(
                        "Contents/MacOS/libjli.dylib"));
            }
        }

        return this;
    }

    JPackageCommand setUnpackedPackageLocation(Path path) {
        verifyIsOfType(PackageType.NATIVE);
        setArgumentValue(UNPACKED_PATH_ARGNAME, path);
        return this;
    }

    private JPackageCommand adjustArgumentsBeforeExecution() {
        if (!hasArgument("--runtime-image") && !hasArgument("--app-image") && DEFAULT_RUNTIME_IMAGE != null && !ignoreDefaultRuntime) {
            addArguments("--runtime-image", DEFAULT_RUNTIME_IMAGE);
        }

        if (!hasArgument("--verbose") && TKit.VERBOSE_JPACKAGE && !ignoreDefaultVerbose) {
            addArgument("--verbose");
        }

        return this;
    }

    public String getPrintableCommandLine() {
        return createExecutor().getPrintableCommandLine();
    }

    public void verifyIsOfType(Collection<PackageType> types) {
        verifyIsOfType(types.toArray(PackageType[]::new));
    }

    public void verifyIsOfType(PackageType ... types) {
        final var typesSet = Stream.of(types).collect(Collectors.toSet());
        if (!hasArgument("--type")) {
            if (!isImagePackageType()) {
                if (TKit.isLinux() && typesSet.equals(PackageType.LINUX)) {
                    return;
                }

                if (TKit.isWindows() && typesSet.equals(PackageType.WINDOWS)) {
                    return;
                }

                if (TKit.isOSX() && typesSet.equals(PackageType.MAC)) {
                    return;
                }
            } else if (typesSet.equals(Set.of(PackageType.IMAGE))) {
                return;
            }
        }

        if (!typesSet.contains(packageType())) {
            throw new IllegalArgumentException("Unexpected type");
        }
    }

    public CfgFile readLauncherCfgFile() {
        return readLauncherCfgFile(null);
    }

    public CfgFile readLauncherCfgFile(String launcherName) {
        verifyIsOfType(PackageType.IMAGE);
        if (isRuntime()) {
            return null;
        }
        return ThrowingFunction.toFunction(CfgFile::readFromFile).apply(
                appLauncherCfgPath(launcherName));
    }

    public List<String> readRuntimeReleaseFile() {
        verifyIsOfType(PackageType.IMAGE);
        Path release = appLayout().runtimeHomeDirectory().resolve("release");
        try {
            return Files.readAllLines(release);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static String escapeAndJoin(String... args) {
        return escapeAndJoin(List.of(args));
    }

    public static String escapeAndJoin(List<String> args) {
        Pattern whitespaceRegexp = Pattern.compile("\\s");

        return args.stream().map(v -> {
            String str = v;
            // Escape backslashes.
            str = str.replace("\\", "\\\\");
            // Escape quotes.
            str = str.replace("\"", "\\\"");
            // If value contains whitespace characters, put the value in quotes
            if (whitespaceRegexp.matcher(str).find()) {
                str = "\"" + str + "\"";
            }
            return str;
        }).collect(Collectors.joining(" "));
    }

    public static Stream<String> stripTimestamps(Stream<String> stream) {
        // [HH:mm:ss.SSS]
        final Pattern timestampRegexp = Pattern.compile(
                "^\\[\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d\\] ");
        return stream.map(str -> {
            Matcher m = timestampRegexp.matcher(str);
            if (m.find()) {
                str = str.substring(m.end());
            }
            return str;
        });
    }

    @Override
    protected boolean isMutable() {
        return !immutable;
    }

    private <T> T onLinuxPackageInstallDir(Function<Path, T> anyInstallDirConsumer,
            Function<Path, T> usrInstallDirConsumer) {
        if (TKit.isLinux()) {
            Path installDir = Path.of(getArgumentValue("--install-dir",
                    () -> "/opt"));
            if (Set.of("/usr", "/usr/local").contains(installDir.toString())) {
                if (usrInstallDirConsumer != null) {
                    return usrInstallDirConsumer.apply(installDir);
                }
            } else if (anyInstallDirConsumer != null) {
                return anyInstallDirConsumer.apply(installDir);
            }
        }
        return null;
    }

    private final class Actions implements Runnable {
        Actions() {
            actions = new ArrayList<>();
        }

        Actions(Actions other) {
            this();
            actions.addAll(other.actions);
        }

        void add(ThrowingConsumer<JPackageCommand> action) {
            Objects.requireNonNull(action);
            verifyMutable();
            actions.add(new Consumer<JPackageCommand>() {
                @Override
                public void accept(JPackageCommand t) {
                    if (!executed) {
                        executed = true;
                        ThrowingConsumer.toConsumer(action).accept(t);
                    }
                }
                private boolean executed;
            });
        }

        @Override
        public void run() {
            verifyMutable();
            actions.forEach(action -> action.accept(JPackageCommand.this));
        }

        private final List<Consumer<JPackageCommand>> actions;
    }

    private Boolean withToolProvider;
    private boolean saveConsoleOutput;
    private boolean suppressOutput;
    private boolean ignoreDefaultRuntime;
    private boolean ignoreDefaultVerbose;
    private boolean immutable;
    private final Actions prerequisiteActions;
    private final Actions verifyActions;
    private static boolean defaultWithToolProvider;

    private final static Map<String, PackageType> PACKAGE_TYPES = Functional.identity(
            () -> {
                Map<String, PackageType> reply = new HashMap<>();
                for (PackageType type : PackageType.values()) {
                    reply.put(type.getName(), type);
                }
                return reply;
            }).get();

    public final static Path DEFAULT_RUNTIME_IMAGE = Functional.identity(() -> {
        // Set the property to the path of run-time image to speed up
        // building app images and platform bundles by avoiding running jlink
        // The value of the property will be automativcally appended to
        // jpackage command line if the command line doesn't have
        // `--runtime-image` parameter set.
        String val = TKit.getConfigProperty("runtime-image");
        if (val != null) {
            return Path.of(val);
        }
        return null;
    }).get();

    private final static String UNPACKED_PATH_ARGNAME = "jpt-unpacked-folder";
}
