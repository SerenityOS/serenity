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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.Functional.ThrowingFunction;
import jdk.jpackage.test.Functional.ThrowingSupplier;

public final class HelloApp {

    HelloApp(JavaAppDesc appDesc) {
        if (appDesc == null) {
            this.appDesc = createDefaltAppDesc();
        } else {
            this.appDesc = appDesc;
        }
    }

    private JarBuilder prepareSources(Path srcDir) throws IOException {
        final String qualifiedClassName = appDesc.className();

        final String className = qualifiedClassName.substring(
                qualifiedClassName.lastIndexOf('.') + 1);
        final String packageName = appDesc.packageName();

        final Path srcFile = srcDir.resolve(Path.of(String.join(
                File.separator, qualifiedClassName.split("\\.")) + ".java"));
        Files.createDirectories(srcFile.getParent());

        JarBuilder jarBuilder = createJarBuilder().addSourceFile(srcFile);
        final String moduleName = appDesc.moduleName();
        if (moduleName != null) {
            Path moduleInfoFile = srcDir.resolve("module-info.java");
            TKit.createTextFile(moduleInfoFile, List.of(
                    String.format("module %s {", moduleName),
                    String.format("    exports %s;", packageName),
                    "    requires java.desktop;",
                    "}"
            ));
            jarBuilder.addSourceFile(moduleInfoFile);
            jarBuilder.setModuleVersion(appDesc.moduleVersion());
        }

        // Add package directive and replace class name in java source file.
        // Works with simple test Hello.java.
        // Don't expect too much from these regexps!
        Pattern classNameRegex = Pattern.compile("\\bHello\\b");
        Pattern classDeclaration = Pattern.compile(
                "(^.*\\bclass\\s+)\\bHello\\b(.*$)");
        Pattern importDirective = Pattern.compile(
                "(?<=import (?:static )?+)[^;]+");
        AtomicBoolean classDeclared = new AtomicBoolean();
        AtomicBoolean packageInserted = new AtomicBoolean(packageName == null);

        var packageInserter = Functional.identityFunction((line) -> {
            packageInserted.setPlain(true);
            return String.format("package %s;%s%s", packageName,
                    System.lineSeparator(), line);
        });

        Files.write(srcFile, Files.readAllLines(HELLO_JAVA).stream().map(line -> {
            Matcher m;
            if (classDeclared.getPlain()) {
                if ((m = classNameRegex.matcher(line)).find()) {
                    line = m.replaceAll(className);
                }
                return line;
            }

            if (!packageInserted.getPlain() && importDirective.matcher(line).find()) {
                line = packageInserter.apply(line);
            } else if ((m = classDeclaration.matcher(line)).find()) {
                classDeclared.setPlain(true);
                line = m.group(1) + className + m.group(2);
                if (!packageInserted.getPlain()) {
                    line = packageInserter.apply(line);
                }
            }
            return line;
        }).collect(Collectors.toList()));

        return jarBuilder;
    }

    private JarBuilder createJarBuilder() {
        JarBuilder builder = new JarBuilder();
        if (appDesc.isWithMainClass()) {
            builder.setMainClass(appDesc.className());
        }
        return builder;
    }

    void addTo(JPackageCommand cmd) {
        final String moduleName = appDesc.moduleName();
        final String qualifiedClassName = appDesc.className();

        if (moduleName != null && appDesc.packageName() == null) {
            throw new IllegalArgumentException(String.format(
                    "Module [%s] with default package", moduleName));
        }

        Supplier<Path> getModulePath = () -> {
            // `--module-path` option should be set by the moment
            // when this action is being executed.
            return cmd.getArgumentValue("--module-path", cmd::inputDir, Path::of);
        };

        if (moduleName == null && CLASS_NAME.equals(qualifiedClassName)) {
            // Use Hello.java as is.
            cmd.addPrerequisiteAction((self) -> {
                if (self.inputDir() != null) {
                    Path jarFile = self.inputDir().resolve(appDesc.jarFileName());
                    createJarBuilder().setOutputJar(jarFile).addSourceFile(
                            HELLO_JAVA).create();
                }
            });
        } else if (appDesc.jmodFileName() != null) {
            // Modular app in .jmod file
            cmd.addPrerequisiteAction(unused -> {
                createBundle(appDesc, getModulePath.get());
            });
        } else {
            // Modular app in .jar file
            cmd.addPrerequisiteAction(unused -> {
                final Path jarFile;
                if (moduleName == null) {
                    jarFile = cmd.inputDir().resolve(appDesc.jarFileName());
                } else if (getModulePath.get() != null) {
                    jarFile = getModulePath.get().resolve(appDesc.jarFileName());
                } else {
                    jarFile = null;
                }
                if (jarFile != null) {
                    TKit.withTempDirectory("src",
                            workDir -> prepareSources(workDir).setOutputJar(jarFile).create());
                }
            });
        }

        if (moduleName == null) {
            cmd.addArguments("--main-jar", appDesc.jarFileName());
            cmd.addArguments("--main-class", qualifiedClassName);
        } else {
            cmd.addArguments("--module-path", TKit.workDir().resolve(
                    "input-modules"));
            cmd.addArguments("--module", String.join("/", moduleName,
                    qualifiedClassName));
            // For modular app assume nothing will go in input directory and thus
            // nobody will create input directory, so remove corresponding option
            // from jpackage command line.
            cmd.removeArgumentWithValue("--input");
        }
        if (TKit.isWindows()) {
            cmd.addArguments("--win-console");
        }
    }

    static JavaAppDesc createDefaltAppDesc() {
        return new JavaAppDesc().setClassName(CLASS_NAME).setBundleFileName("hello.jar");
    }

    static void verifyOutputFile(Path outputFile, List<String> args,
            Map<String, String> params) {
        if (!outputFile.isAbsolute()) {
            verifyOutputFile(outputFile.toAbsolutePath().normalize(), args,
                    params);
            return;
        }

        TKit.assertFileExists(outputFile);

        List<String> contents = ThrowingSupplier.toSupplier(
                () -> Files.readAllLines(outputFile)).get();

        List<String> expected = new ArrayList<>(List.of(
                "jpackage test application",
                String.format("args.length: %d", args.size())
        ));
        expected.addAll(args);
        expected.addAll(params.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> String.format("-D%s=%s", entry.getKey(),
                        entry.getValue()))
                .collect(Collectors.toList()));

        TKit.assertStringListEquals(expected, contents, String.format(
                "Check contents of [%s] file", outputFile));
    }

    public static Path createBundle(JavaAppDesc appDesc, Path outputDir) {
        String jmodFileName = appDesc.jmodFileName();
        if (jmodFileName != null) {
            final Path jmodPath = outputDir.resolve(jmodFileName);
            TKit.withTempDirectory("jmod-workdir", jmodWorkDir -> {
                var jarAppDesc = JavaAppDesc.parse(appDesc.toString())
                        .setBundleFileName("tmp.jar");
                Path jarPath = createBundle(jarAppDesc, jmodWorkDir);
                Executor exec = new Executor()
                        .setToolProvider(JavaTool.JMOD)
                        .addArguments("create", "--class-path")
                        .addArgument(jarPath);

                if (appDesc.isWithMainClass()) {
                    exec.addArguments("--main-class", appDesc.className());
                }

                if (appDesc.moduleVersion() != null) {
                    exec.addArguments("--module-version", appDesc.moduleVersion());
                }

                final Path jmodFilePath;
                if (appDesc.isExplodedModule()) {
                    jmodFilePath = jmodWorkDir.resolve("tmp.jmod");
                    exec.addArgument(jmodFilePath);
                    TKit.deleteDirectoryRecursive(jmodPath);
                } else {
                    jmodFilePath = jmodPath;
                    exec.addArgument(jmodFilePath);
                    TKit.deleteIfExists(jmodPath);
                }

                Files.createDirectories(jmodPath.getParent());
                exec.execute();

                if (appDesc.isExplodedModule()) {
                    TKit.trace(String.format("Explode [%s] module file...",
                            jmodFilePath.toAbsolutePath().normalize()));
                    // Explode contents of the root `classes` directory of
                    // temporary .jmod file
                    final Path jmodRootDir = Path.of("classes");
                    try (var archive = new ZipFile(jmodFilePath.toFile())) {
                        archive.stream()
                        .filter(Predicate.not(ZipEntry::isDirectory))
                        .sequential().forEachOrdered(ThrowingConsumer.toConsumer(
                            entry -> {
                                try (var in = archive.getInputStream(entry)) {
                                    Path entryName = Path.of(entry.getName());
                                    if (entryName.startsWith(jmodRootDir)) {
                                        entryName = jmodRootDir.relativize(entryName);
                                    }
                                    final Path fileName = jmodPath.resolve(entryName);
                                    TKit.trace(String.format(
                                            "Save [%s] zip entry in [%s] file...",
                                            entry.getName(),
                                            fileName.toAbsolutePath().normalize()));
                                    Files.createDirectories(fileName.getParent());
                                    Files.copy(in, fileName);
                                }
                            }));
                    }
                }
            });

            return jmodPath;
        }

        final JavaAppDesc jarAppDesc;
        if (appDesc.isWithBundleFileName()) {
            jarAppDesc = appDesc;
        } else {
            // Create copy of original JavaAppDesc instance.
            jarAppDesc = JavaAppDesc.parse(appDesc.toString())
                        .setBundleFileName(createDefaltAppDesc().jarFileName());
        }

        JPackageCommand
                .helloAppImage(jarAppDesc)
                .setArgumentValue("--input", outputDir)
                .setArgumentValue("--module-path", outputDir)
                .executePrerequisiteActions();

        return outputDir.resolve(jarAppDesc.jarFileName());
    }

    public static void executeLauncherAndVerifyOutput(JPackageCommand cmd,
            String... args) {
        AppOutputVerifier av = getVerifier(cmd, args);
        if (av != null) {
            // when running app launchers, clear users environment
            av.executeAndVerifyOutput(true, args);
        }
    }

    public static Executor.Result executeLauncher(JPackageCommand cmd,
            String... args) {
        AppOutputVerifier av = getVerifier(cmd, args);
        return av.executeOnly(true, args);
    }

    private static AppOutputVerifier getVerifier(JPackageCommand cmd,
            String... args) {
        final Path launcherPath = cmd.appLauncherPath();
        if (!cmd.canRunLauncher(String.format("Not running [%s] launcher",
                launcherPath))) {
            return null;
        }

        return assertApp(launcherPath)
        .addDefaultArguments(Optional
                .ofNullable(cmd.getAllArgumentValues("--arguments"))
                .orElseGet(() -> new String[0]))
        .addJavaOptions(Optional
                .ofNullable(cmd.getAllArgumentValues("--java-options"))
                .orElseGet(() -> new String[0]));
    }


    public final static class AppOutputVerifier {
        AppOutputVerifier(Path helloAppLauncher) {
            this.launcherPath = helloAppLauncher;
            this.params = new HashMap<>();
            this.defaultLauncherArgs = new ArrayList<>();
        }

        public AppOutputVerifier addDefaultArguments(String... v) {
            return addDefaultArguments(List.of(v));
        }

        public AppOutputVerifier addDefaultArguments(Collection<String> v) {
            defaultLauncherArgs.addAll(v);
            return this;
        }

        public AppOutputVerifier addParam(String name, String value) {
            if (name.startsWith("param")) {
                params.put(name, value);
            }
            return this;
        }

        public AppOutputVerifier addParams(Collection<Map.Entry<String, String>> v) {
            v.forEach(entry -> addParam(entry.getKey(), entry.getValue()));
            return this;
        }
        public AppOutputVerifier addParams(Map<String, String> v) {
            return addParams(v.entrySet());
        }

        public AppOutputVerifier addParams(Map.Entry<String, String>... v) {
            return addParams(List.of(v));
        }

        public AppOutputVerifier addJavaOptions(String... v) {
            return addJavaOptions(List.of(v));
        }

        public AppOutputVerifier addJavaOptions(Collection<String> v) {
            return addParams(v.stream()
            .filter(javaOpt -> javaOpt.startsWith("-D"))
            .map(javaOpt -> {
                var components = javaOpt.split("=", 2);
                return Map.entry(components[0].substring(2), components[1]);
            })
            .collect(Collectors.toList()));
        }

        public void executeAndVerifyOutput(String... args) {
            executeAndVerifyOutput(false, args);
        }

        public void executeAndVerifyOutput(boolean removePath,
                List<String> launcherArgs, List<String> appArgs) {
            final int attempts = 3;
            final int waitBetweenAttemptsSeconds = 5;
            getExecutor(launcherArgs.toArray(new String[0])).dumpOutput().setRemovePath(
                    removePath).executeAndRepeatUntilExitCode(0, attempts,
                            waitBetweenAttemptsSeconds);
            Path outputFile = TKit.workDir().resolve(OUTPUT_FILENAME);
            verifyOutputFile(outputFile, appArgs, params);
        }

        public void executeAndVerifyOutput(boolean removePath, String... args) {
            final List<String> launcherArgs = List.of(args);
            final List<String> appArgs;
            if (launcherArgs.isEmpty()) {
                appArgs = defaultLauncherArgs;
            } else {
                appArgs = launcherArgs;
            }

            executeAndVerifyOutput(removePath, launcherArgs, appArgs);
        }

        public Executor.Result executeOnly(boolean removePath, String...args) {
            return getExecutor(args)
                    .saveOutput()
                    .setRemovePath(removePath)
                    .executeWithoutExitCodeCheck();
        }

        private Executor getExecutor(String...args) {

            // Output file might be created in the current directory.
            Path outputFile = TKit.workDir().resolve(OUTPUT_FILENAME);
            ThrowingFunction.toFunction(Files::deleteIfExists).apply(outputFile);

            final Path executablePath;
            if (launcherPath.isAbsolute()) {
                executablePath = launcherPath;
            } else {
                // Make sure path to executable is relative to the current directory.
                executablePath = Path.of(".").resolve(launcherPath.normalize());
            }

            final List<String> launcherArgs = List.of(args);
            return new Executor()
                    .setDirectory(outputFile.getParent())
                    .setExecutable(executablePath)
                    .addArguments(launcherArgs);
        }

        private final Path launcherPath;
        private final List<String> defaultLauncherArgs;
        private final Map<String, String> params;
    }

    public static AppOutputVerifier assertApp(Path helloAppLauncher) {
        return new AppOutputVerifier(helloAppLauncher);
    }

    final static String OUTPUT_FILENAME = "appOutput.txt";

    private final JavaAppDesc appDesc;

    private static final Path HELLO_JAVA = TKit.TEST_SRC_ROOT.resolve(
            "apps/image/Hello.java");

    private final static String CLASS_NAME = HELLO_JAVA.getFileName().toString().split(
            "\\.", 2)[0];
}
