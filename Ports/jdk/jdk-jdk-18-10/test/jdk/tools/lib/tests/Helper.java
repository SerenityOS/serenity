/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package tests;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import tests.JImageGenerator.JLinkTask;
import tests.JImageGenerator.JModTask;

/**
 * JLink tests helper.
 */
public class Helper {

    private final Path explodedmodssrc;
    private final Path jmodssrc;
    private final Path jarssrc;
    private final Path explodedmodsclasses;
    private final Path jmodsclasses;
    private final Path jarsclasses;
    private final Path jmods;
    private final Path jars;
    private final Path images;
    private final Path explodedmods;
    private final Path stdjmods;
    private final Path extracted;
    private final Path recreated;

    private final Map<String, List<String>> moduleClassDependencies = new HashMap<>();
    private final Map<String, List<String>> moduleDependencies = new HashMap<>();
    private final List<String> bootClasses;
    private final FileSystem fs;

    public static Helper newHelper() throws IOException {
        Path jdkHome = Paths.get(System.getProperty("test.jdk"));
        if (!Files.exists(jdkHome.resolve("jmods"))) {
            // Skip test if the jmods directory is missing (e.g. exploded image)
            System.err.println("Test not run, NO jmods directory");
            return null;
        }
        return new Helper(jdkHome);
    }

    private Helper(Path jdkHome) throws IOException {
        this.stdjmods = jdkHome.resolve("jmods").normalize();
        if (!Files.exists(stdjmods)) {
            throw new IOException("Standard jMods do not exist.");
        }
        this.fs = FileSystems.getFileSystem(URI.create("jrt:/"));

        Path javabase = fs.getPath("/modules/java.base");
        this.bootClasses = Files.find(javabase, Integer.MAX_VALUE,
                (file, attrs) -> file.toString().endsWith(".class"))
                .map(Object::toString)
                .map(s -> s.substring("/modules".length()))
                .collect(Collectors.toList());

        if (bootClasses.isEmpty()) {
            throw new AssertionError("No boot class to check against");
        }

        this.jmods = Paths.get("jmods").toAbsolutePath();
        Files.createDirectories(jmods);
        this.jars = Paths.get("jars").toAbsolutePath();
        Files.createDirectories(jars);
        this.explodedmods = Paths.get("explodedmods").toAbsolutePath();
        Files.createDirectories(explodedmods);
        this.explodedmodssrc = explodedmods.resolve("src");
        Files.createDirectories(explodedmodssrc);
        this.jarssrc = jars.resolve("src");
        Files.createDirectories(jarssrc);
        this.jmodssrc = jmods.resolve("src");
        Files.createDirectories(jmodssrc);
        this.explodedmodsclasses = explodedmods.resolve("classes");
        Files.createDirectories(explodedmodsclasses);
        this.jmodsclasses = jmods.resolve("classes");
        Files.createDirectories(jmodsclasses);
        this.jarsclasses = jars.resolve("classes");
        Files.createDirectories(jarsclasses);
        this.images = Paths.get("images").toAbsolutePath();
        Files.createDirectories(images);
        this.extracted = Paths.get("extracted").toAbsolutePath();
        Files.createDirectories(extracted);
        this.recreated = Paths.get("recreated").toAbsolutePath();
        Files.createDirectories(recreated);
    }

    public void generateDefaultModules() throws IOException {
        generateDefaultJModule("leaf1");
        generateDefaultJModule("leaf2");
        generateDefaultJModule("leaf3");

        generateDefaultJarModule("leaf4");
        generateDefaultJarModule("leaf5");

        generateDefaultExplodedModule("leaf6");
        generateDefaultExplodedModule("leaf7");

        generateDefaultJarModule("composite1", "leaf1", "leaf2", "leaf4", "leaf6");
        generateDefaultJModule("composite2", "composite1", "leaf3", "leaf5", "leaf7",
                "java.management");
    }

    public String defaultModulePath() {
        return defaultModulePath(true);
    }

    public String defaultModulePath(boolean includeStdMods) {
        return (includeStdMods? stdjmods.toAbsolutePath().toString() : "") + File.pathSeparator
                + jmods.toAbsolutePath().toString() + File.pathSeparator
                + jars.toAbsolutePath().toString() + File.pathSeparator
                + explodedmodsclasses.toAbsolutePath().toString();
    }

    public Path generateModuleCompiledClasses(
            Path src, Path classes, String moduleName, String... dependencies) throws IOException {
        return generateModuleCompiledClasses(src, classes, moduleName, getDefaultClasses(moduleName), dependencies);
    }

    public Path generateModuleCompiledClasses(
            Path src, Path classes, String moduleName,
            List<String> classNames, String... dependencies) throws IOException {
        if (classNames == null) {
            classNames = getDefaultClasses(moduleName);
        }
        putAppClasses(moduleName, classNames);
        moduleDependencies.put(moduleName, Arrays.asList(dependencies));
        String modulePath = defaultModulePath();
        JImageGenerator.generateSourcesFromTemplate(src, moduleName, classNames.toArray(new String[classNames.size()]));
        List<String> packages = classNames.stream()
                .map(JImageGenerator::getPackageName)
                .distinct()
                .collect(Collectors.toList());
        Path srcMod = src.resolve(moduleName);
        JImageGenerator.generateModuleInfo(srcMod, packages, dependencies);
        Path destination = classes.resolve(moduleName);
        if (!JImageGenerator.compile(srcMod, destination, "--module-path", modulePath, "-g")) {
            throw new AssertionError("Compilation failure");
        }
        return destination;
    }

    public Result generateDefaultJModule(String moduleName, String... dependencies) throws IOException {
        return generateDefaultJModule(moduleName, getDefaultClasses(moduleName), dependencies);
    }

    public Result generateDefaultJModule(String moduleName, List<String> classNames,
                                             String... dependencies) throws IOException {
        generateModuleCompiledClasses(jmodssrc, jmodsclasses, moduleName, classNames, dependencies);
        generateGarbage(jmodsclasses.resolve(moduleName));

        Path jmodFile = jmods.resolve(moduleName + ".jmod");
        JModTask task = JImageGenerator.getJModTask()
                .jmod(jmodFile)
                .addJmods(stdjmods)
                .addJmods(jmods.toAbsolutePath())
                .addJars(jars.toAbsolutePath())
                .addClassPath(jmodsclasses.resolve(moduleName));
        if (!classNames.isEmpty()) {
            task.mainClass(classNames.get(0));
        }
        return task.create();
    }

    public Result generateDefaultJarModule(String moduleName, String... dependencies) throws IOException {
        return generateDefaultJarModule(moduleName, getDefaultClasses(moduleName), dependencies);
    }

    public Result generateDefaultJarModule(String moduleName, List<String> classNames,
                                         String... dependencies) throws IOException {
        generateModuleCompiledClasses(jarssrc, jarsclasses, moduleName, classNames, dependencies);
        generateGarbage(jarsclasses.resolve(moduleName));

        Path jarFile = jars.resolve(moduleName + ".jar");
        JImageGenerator.createJarFile(jarFile, jarsclasses.resolve(moduleName));
        return new Result(0, "", jarFile);
    }

    public Result generateDefaultExplodedModule(String moduleName, String... dependencies) throws IOException {
        return generateDefaultExplodedModule(moduleName, getDefaultClasses(moduleName), dependencies);
    }

    public Result generateDefaultExplodedModule(String moduleName, List<String> classNames,
            String... dependencies) throws IOException {
        generateModuleCompiledClasses(explodedmodssrc, explodedmodsclasses,
                moduleName, classNames, dependencies);

        Path dir = explodedmods.resolve("classes").resolve(moduleName);
        return new Result(0, "", dir);
    }

    private void generateGarbage(Path compiled) throws IOException {
        Path metaInf = compiled.resolve("META-INF").resolve("services");
        Files.createDirectories(metaInf);
        Path provider = metaInf.resolve("MyProvider");
        Files.createFile(provider);
        Files.createFile(compiled.resolve("toto.jcov"));
    }

    public static Path createNewFile(Path root, String pathName, String extension) {
        Path out = root.resolve(pathName + extension);
        int i = 1;
        while (Files.exists(out)) {
            out = root.resolve(pathName + "-" + (++i) + extension);
        }
        return out;
    }

    public Result generateDefaultImage(String module) {
        return generateDefaultImage(new String[0], module);
    }

    public Result generateDefaultImage(String[] options, String module) {
        Path output = createNewFile(images, module, ".image");
        JLinkTask jLinkTask = JImageGenerator.getJLinkTask()
                .modulePath(defaultModulePath())
                .output(output)
                .addMods(module)
                .limitMods(module);
        for (String option : options) {
            jLinkTask.option(option);
        }
        return jLinkTask.call();
    }

    public Result postProcessImage(Path root, String[] options) {
        JLinkTask jLinkTask = JImageGenerator.getJLinkTask()
                .existing(root);
        for (String option : options) {
            jLinkTask.option(option);
        }
        return jLinkTask.callPostProcess();
    }

    private List<String> getDefaultClasses(String module) {
        return Arrays.asList(module + ".Main", module + ".com.foo.bar.X");
    }

    private void putAppClasses(String module, List<String> classes) {
        List<String> appClasses = toLocation(module, classes).stream().collect(Collectors.toList());
        appClasses.add(toLocation(module, "module-info"));
        moduleClassDependencies.put(module, appClasses);
    }

    private static String toLocation(String module, String className) {
        return "/" + module + "/" + className.replaceAll("\\.", "/") + ".class";
    }

    public static List<String> toLocation(String module, List<String> classNames) {
        return classNames.stream()
                .map(clazz -> toLocation(module, clazz))
                .collect(Collectors.toList());
    }

    public void checkImage(Path imageDir, String module, String[] paths, String[] files) throws IOException {
        checkImage(imageDir, module, paths, files, null);
    }

    public void checkImage(Path imageDir, String module, String[] paths, String[] files, String[] expectedFiles) throws IOException {
        List<String> unexpectedPaths = new ArrayList<>();
        if (paths != null) {
            Collections.addAll(unexpectedPaths, paths);
        }
        List<String> unexpectedFiles = new ArrayList<>();
        if (files != null) {
            Collections.addAll(unexpectedFiles, files);
        }

        JImageValidator validator = new JImageValidator(module, gatherExpectedLocations(module),
                imageDir.toFile(),
                unexpectedPaths,
                unexpectedFiles,
                expectedFiles);
        System.out.println("*** Validate Image " + module);
        validator.validate();
        long moduleExecutionTime = validator.getModuleLauncherExecutionTime();
        if (moduleExecutionTime != 0) {
            System.out.println("Module launcher execution time " + moduleExecutionTime);
        }
        System.out.println("Java launcher execution time "
                + validator.getJavaLauncherExecutionTime());
        System.out.println("***");
    }

    private List<String> gatherExpectedLocations(String module) throws IOException {
        List<String> expectedLocations = new ArrayList<>();
        expectedLocations.addAll(bootClasses);
        List<String> modules = moduleDependencies.get(module);
        for (String dep : modules) {
            Path path = fs.getPath("/modules/" + dep);
            if (Files.exists(path)) {
                List<String> locations = Files.find(path, Integer.MAX_VALUE,
                        (p, attrs) -> Files.isRegularFile(p) && p.toString().endsWith(".class")
                                && !p.toString().endsWith("module-info.class"))
                        .map(p -> p.toString().substring("/modules".length()))
                        .collect(Collectors.toList());
                expectedLocations.addAll(locations);
            }
        }

        List<String> appClasses = moduleClassDependencies.get(module);
        if (appClasses != null) {
            expectedLocations.addAll(appClasses);
        }
        return expectedLocations;
    }

    public static String getDebugSymbolsExtension() {
        return ".diz";
    }

    public Path createNewImageDir(String moduleName) {
        return createNewFile(getImageDir(), moduleName, ".image");
    }

    public Path createNewExtractedDir(String name) {
        return createNewFile(getExtractedDir(), name, ".extracted");
    }

    public Path createNewRecreatedDir(String name) {
        return createNewFile(getRecreatedDir(), name, ".jimage");
    }

    public Path createNewJmodFile(String moduleName) {
        return createNewFile(getJmodDir(), moduleName, ".jmod");
    }

    public Path createNewJarFile(String moduleName) {
        return createNewFile(getJarDir(), moduleName, ".jar");
    }

    public Path getJmodSrcDir() {
        return jmodssrc;
    }

    public Path getJarSrcDir() {
        return jarssrc;
    }

    public Path getJmodClassesDir() {
        return jmodsclasses;
    }

    public Path getJarClassesDir() {
        return jarsclasses;
    }

    public Path getJmodDir() {
        return jmods;
    }

    public Path getExplodedModsDir() {
        return explodedmods;
    }

    public Path getJarDir() {
        return jars;
    }

    public Path getImageDir() {
        return images;
    }

    public Path getStdJmodsDir() {
        return stdjmods;
    }

    public Path getExtractedDir() {
        return extracted;
    }

    public Path getRecreatedDir() {
        return recreated;
    }
}
