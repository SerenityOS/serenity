/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 *
 * A generator for jmods, jars and images.
 */
public class JImageGenerator {

    public static final String LOAD_ALL_CLASSES_TEMPLATE = "package PACKAGE;\n"
            + "\n"
            + "import java.net.URI;\n"
            + "import java.nio.file.FileSystems;\n"
            + "import java.nio.file.Files;\n"
            + "import java.nio.file.Path;\n"
            + "import java.util.function.Function;\n"
            + "\n"
            + "public class CLASS {\n"
            + "    private static long total_time;\n"
            + "    private static long num_classes;\n"
            + "    public static void main(String[] args) throws Exception {\n"
            + "        Function<Path, String> formatter = (path) -> {\n"
            + "            String clazz = path.toString().substring(\"modules/\".length()+1, path.toString().lastIndexOf(\".\"));\n"
            + "            clazz = clazz.substring(clazz.indexOf(\"/\") + 1);\n"
            + "            return clazz.replaceAll(\"/\", \"\\\\.\");\n"
            + "        };\n"
            + "        Files.walk(FileSystems.getFileSystem(URI.create(\"jrt:/\")).getPath(\"/modules/\")).\n"
            + "                filter((p) -> {\n"
            + "                    return Files.isRegularFile(p) && p.toString().endsWith(\".class\")\n"
            + "                    && !p.toString().endsWith(\"module-info.class\");\n"
            + "                }).\n"
            + "                map(formatter).forEach((clazz) -> {\n"
            + "                    try {\n"
            + "                        long t = System.currentTimeMillis();\n"
            + "                        Class.forName(clazz, false, Thread.currentThread().getContextClassLoader());\n"
            + "                        total_time+= System.currentTimeMillis()-t;\n"
            + "                        num_classes+=1;\n"
            + "                    } catch (IllegalAccessError ex) {\n"
            + "                        // Security exceptions can occur, this is not what we are testing\n"
            + "                        System.err.println(\"Access error, OK \" + clazz);\n"
            + "                    } catch (Exception ex) {\n"
            + "                        System.err.println(\"ERROR \" + clazz);\n"
            + "                        throw new RuntimeException(ex);\n"
            + "                    }\n"
            + "                });\n"
            + "    double res = (double) total_time / num_classes;\n"
            + "    // System.out.println(\"Total time \" + total_time + \" num classes \" + num_classes + \" average \" + res);\n"
            + "    }\n"
            + "}\n";

    private static final String OUTPUT_OPTION = "--output";
    private static final String POST_PROCESS_OPTION = "--post-process-path";
    private static final String MAIN_CLASS_OPTION = "--main-class";
    private static final String CLASS_PATH_OPTION = "--class-path";
    private static final String MODULE_PATH_OPTION = "--module-path";
    private static final String ADD_MODULES_OPTION = "--add-modules";
    private static final String LIMIT_MODULES_OPTION = "--limit-modules";
    private static final String PLUGIN_MODULE_PATH = "--plugin-module-path";
    private static final String LAUNCHER = "--launcher";

    private static final String CMDS_OPTION = "--cmds";
    private static final String CONFIG_OPTION = "--config";
    private static final String HASH_MODULES_OPTION = "--hash-modules";
    private static final String LIBS_OPTION = "--libs";
    private static final String MODULE_VERSION_OPTION = "--module-version";

    private JImageGenerator() {}

    private static String optionsPrettyPrint(String... args) {
        return Stream.of(args).collect(Collectors.joining(" "));
    }

    public static File getJModsDir(File jdkHome) {
        File jdkjmods = new File(jdkHome, "jmods");
        if (!jdkjmods.exists()) {
            return null;
        }
        return jdkjmods;
    }

    public static Path addFiles(Path module, InMemoryFile... resources) throws IOException {
        Path tempFile = Files.createTempFile("jlink-test", "");
        try (JarInputStream in = new JarInputStream(Files.newInputStream(module));
             JarOutputStream out = new JarOutputStream(new FileOutputStream(tempFile.toFile()))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                out.putNextEntry(new ZipEntry(name));
                copy(in, out);
                out.closeEntry();
            }
            for (InMemoryFile r : resources) {
                addFile(r, out);
            }
        }
        Files.move(tempFile, module, StandardCopyOption.REPLACE_EXISTING);
        return module;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        int len;
        byte[] buf = new byte[4096];
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    public static JModTask getJModTask() {
        return new JModTask();
    }

    public static JLinkTask getJLinkTask() {
        return new JLinkTask();
    }

    public static JImageTask getJImageTask() {
        return new JImageTask();
    }

    private static void addFile(InMemoryFile resource, JarOutputStream target) throws IOException {
        String fileName = resource.getPath();
        fileName = fileName.replace("\\", "/");
        String[] ss = fileName.split("/");
        Path p = Paths.get("");
        for (int i = 0; i < ss.length; ++i) {
            if (i < ss.length - 1) {
                if (!ss[i].isEmpty()) {
                    p = p.resolve(ss[i]);
                    JarEntry entry = new JarEntry(p.toString() + "/");
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
            } else {
                p = p.resolve(ss[i]);
                JarEntry entry = new JarEntry(p.toString());
                target.putNextEntry(entry);
                copy(resource.getBytes(), target);
                target.closeEntry();
            }
        }
    }

    public static Path createNewFile(Path root, String pathName, String extension) {
        Path out = root.resolve(pathName + extension);
        int i = 1;
        while (Files.exists(out)) {
            out = root.resolve(pathName + "-" + (++i) + extension);
        }
        return out;
    }

    public static Path generateSources(Path output, String moduleName, List<InMemorySourceFile> sources) throws IOException {
        Path moduleDir = output.resolve(moduleName);
        Files.createDirectory(moduleDir);
        for (InMemorySourceFile source : sources) {
            Path fileDir = moduleDir;
            if (!source.packageName.isEmpty()) {
                String dir = source.packageName.replace('.', File.separatorChar);
                fileDir = moduleDir.resolve(dir);
                Files.createDirectories(fileDir);
            }
            Files.write(fileDir.resolve(source.className + ".java"), source.source.getBytes());
        }
        return moduleDir;
    }

    public static Path generateSourcesFromTemplate(Path output, String moduleName, String... classNames) throws IOException {
        List<InMemorySourceFile> sources = new ArrayList<>();
        for (String className : classNames) {
            String packageName = getPackageName(className);
            String simpleName = getSimpleName(className);
            String content = LOAD_ALL_CLASSES_TEMPLATE
                    .replace("CLASS", simpleName);
            if (packageName.isEmpty()) {
                content = content.replace("package PACKAGE;", packageName);
            } else {
                content = content.replace("PACKAGE", packageName);
            }
            sources.add(new InMemorySourceFile(packageName, simpleName, content));
        }
        return generateSources(output, moduleName, sources);
    }

    public static void generateModuleInfo(Path moduleDir, List<String> packages, String... dependencies) throws IOException {
        StringBuilder moduleInfoBuilder = new StringBuilder();
        Path file = moduleDir.resolve("module-info.java");
        String moduleName = moduleDir.getFileName().toString();
        moduleInfoBuilder.append("module ").append(moduleName).append("{\n");
        for (String dep : dependencies) {
            moduleInfoBuilder.append("requires ").append(dep).append(";\n");
        }
        for (String pkg : packages) {
            if (!pkg.trim().isEmpty()) {
                moduleInfoBuilder.append("exports ").append(pkg).append(";\n");
            }
        }
        moduleInfoBuilder.append("}");
        Files.write(file, moduleInfoBuilder.toString().getBytes());
    }

    public static void compileSuccess(Path source, Path destination, String... options) throws IOException {
        if (!compile(source, destination, options)) {
            throw new AssertionError("Compilation failed.");
        }
    }

    public static boolean compile(Path source, Path destination, String... options) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager jfm = compiler.getStandardFileManager(null, null, null)) {
            List<Path> sources
                    = Files.find(source, Integer.MAX_VALUE,
                    (file, attrs) -> file.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            Files.createDirectories(destination);
            jfm.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, Collections.singleton(destination));

            List<String> opts = Arrays.asList(options);
            JavaCompiler.CompilationTask task
                    = compiler.getTask(null, jfm, null, opts, null,
                    jfm.getJavaFileObjectsFromPaths(sources));
            List<String> list = new ArrayList<>(opts);
            list.addAll(sources.stream()
                    .map(Path::toString)
                    .collect(Collectors.toList()));
            System.err.println("javac options: " + optionsPrettyPrint(list.toArray(new String[list.size()])));
            return task.call();
        }
    }

    public static Path createJarFile(Path jarfile, Path dir) throws IOException {
        return createJarFile(jarfile, dir, Paths.get("."));
    }

    public static Path createJarFile(Path jarfile, Path dir, Path file) throws IOException {
        // create the target directory
        Path parent = jarfile.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        List<Path> entries = Files.find(dir.resolve(file), Integer.MAX_VALUE,
                (p, attrs) -> attrs.isRegularFile())
                .map(dir::relativize)
                .collect(Collectors.toList());

        try (OutputStream out = Files.newOutputStream(jarfile);
             JarOutputStream jos = new JarOutputStream(out)) {
            for (Path entry : entries) {
                // map the file path to a name in the JAR file
                Path normalized = entry.normalize();
                String name = normalized
                        .subpath(0, normalized.getNameCount())  // drop root
                        .toString()
                        .replace(File.separatorChar, '/');

                jos.putNextEntry(new JarEntry(name));
                Files.copy(dir.resolve(entry), jos);
            }
        }
        return jarfile;
    }

    public static Set<String> getModuleContent(Path module) {
        Result result = JImageGenerator.getJModTask()
                .jmod(module)
                .list();
        result.assertSuccess();
        return Stream.of(result.getMessage().split("\r?\n"))
                .collect(Collectors.toSet());
    }

    public static void checkModule(Path module, Set<String> expected) throws IOException {
        Set<String> actual = getModuleContent(module);
        if (!Objects.equals(actual, expected)) {
            Set<String> unexpected = new HashSet<>(actual);
            unexpected.removeAll(expected);
            Set<String> notFound = new HashSet<>(expected);
            notFound.removeAll(actual);
            System.err.println("Unexpected files:");
            unexpected.forEach(s -> System.err.println("\t" + s));
            System.err.println("Not found files:");
            notFound.forEach(s -> System.err.println("\t" + s));
            throw new AssertionError("Module check failed.");
        }
    }

    public static class JModTask {
        static final java.util.spi.ToolProvider JMOD_TOOL =
            java.util.spi.ToolProvider.findFirst("jmod").orElseThrow(() ->
                new RuntimeException("jmod tool not found")
            );

        private final List<Path> classpath = new ArrayList<>();
        private final List<Path> libs = new ArrayList<>();
        private final List<Path> cmds = new ArrayList<>();
        private final List<Path> config = new ArrayList<>();
        private final List<Path> jars = new ArrayList<>();
        private final List<Path> jmods = new ArrayList<>();
        private final List<String> options = new ArrayList<>();
        private Path output;
        private String hashModules;
        private String mainClass;
        private String moduleVersion;

        public JModTask addNativeLibraries(Path cp) {
            this.libs.add(cp);
            return this;
        }

        public JModTask hashModules(String hash) {
            this.hashModules = hash;
            return this;
        }

        public JModTask addCmds(Path cp) {
            this.cmds.add(cp);
            return this;
        }

        public JModTask addClassPath(Path cp) {
            this.classpath.add(cp);
            return this;
        }

        public JModTask addConfig(Path cp) {
            this.config.add(cp);
            return this;
        }

        public JModTask addJars(Path jars) {
            this.jars.add(jars);
            return this;
        }

        public JModTask addJmods(Path jmods) {
            this.jmods.add(jmods);
            return this;
        }

        public JModTask jmod(Path output) {
            this.output = output;
            return this;
        }

        public JModTask moduleVersion(String moduleVersion) {
            this.moduleVersion = moduleVersion;
            return this;
        }

        public JModTask mainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        public JModTask option(String o) {
            this.options.add(o);
            return this;
        }

        private String modulePath() {
            // This is expect FIRST jmods THEN jars, if you change this, some tests could fail
            String jmods = toPath(this.jmods);
            String jars = toPath(this.jars);
            return jmods + File.pathSeparator + jars;
        }

        private String toPath(List<Path> paths) {
            return paths.stream()
                    .map(Path::toString)
                    .collect(Collectors.joining(File.pathSeparator));
        }

        private String[] optionsJMod(String cmd) {
            List<String> options = new ArrayList<>();
            options.add(cmd);
            if (!cmds.isEmpty()) {
                options.add(CMDS_OPTION);
                options.add(toPath(cmds));
            }
            if (!config.isEmpty()) {
                options.add(CONFIG_OPTION);
                options.add(toPath(config));
            }
            if (hashModules != null) {
                options.add(HASH_MODULES_OPTION);
                options.add(hashModules);
            }
            if (mainClass != null) {
                options.add(MAIN_CLASS_OPTION);
                options.add(mainClass);
            }
            if (!libs.isEmpty()) {
                options.add(LIBS_OPTION);
                options.add(toPath(libs));
            }
            if (!classpath.isEmpty()) {
                options.add(CLASS_PATH_OPTION);
                options.add(toPath(classpath));
            }
            if (!jars.isEmpty() || !jmods.isEmpty()) {
                options.add(MODULE_PATH_OPTION);
                options.add(modulePath());
            }
            if (moduleVersion != null) {
                options.add(MODULE_VERSION_OPTION);
                options.add(moduleVersion);
            }
            options.addAll(this.options);
            if (output != null) {
                options.add(output.toString());
            }
            return options.toArray(new String[options.size()]);
        }

        public Result create() {
            return cmd("create");
        }

        public Result list() {
            return cmd("list");
        }

        public Result call() {
            return cmd("");
        }

        private Result cmd(String cmd) {
            String[] args = optionsJMod(cmd);
            System.err.println("jmod options: " + optionsPrettyPrint(args));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            int exitCode = JMOD_TOOL.run(ps, ps, args);
            String msg = new String(baos.toByteArray());
            return new Result(exitCode, msg, output);
        }
    }

    public static String getPackageName(String canonicalName) {
        int index = canonicalName.lastIndexOf('.');
        return index > 0 ? canonicalName.substring(0, index) : "";
    }

    public static String getSimpleName(String canonicalName) {
        int index = canonicalName.lastIndexOf('.');
        return canonicalName.substring(index + 1);
    }

    public static class JImageTask {

        private final List<Path> pluginModulePath = new ArrayList<>();
        private final List<String> options = new ArrayList<>();
        private Path dir;
        private Path image;

        public JImageTask pluginModulePath(Path p) {
            this.pluginModulePath.add(p);
            return this;
        }

        public JImageTask image(Path image) {
            this.image = image;
            return this;
        }

        public JImageTask dir(Path dir) {
            this.dir = dir;
            return this;
        }

        public JImageTask option(String o) {
            this.options.add(o);
            return this;
        }

        private String toPath(List<Path> paths) {
            return paths.stream()
                    .map(Path::toString)
                    .collect(Collectors.joining(File.pathSeparator));
        }

        private String[] optionsJImage(String cmd) {
            List<String> options = new ArrayList<>();
            options.add(cmd);
            if (dir != null) {
                options.add("--dir");
                options.add(dir.toString());
            }
            if (!pluginModulePath.isEmpty()) {
                options.add(PLUGIN_MODULE_PATH);
                options.add(toPath(pluginModulePath));
            }
            options.addAll(this.options);
            options.add(image.toString());
            return options.toArray(new String[options.size()]);
        }

        private Result cmd(String cmd, Path returnPath) {
            String[] args = optionsJImage(cmd);
            System.err.println("jimage options: " + optionsPrettyPrint(args));
            StringWriter writer = new StringWriter();
            int exitCode = jdk.tools.jimage.Main.run(args, new PrintWriter(writer));
            return new Result(exitCode, writer.toString(), returnPath);
        }

        public Result extract() {
            return cmd("extract", dir);
        }
    }

    public static class JLinkTask {
        static final java.util.spi.ToolProvider JLINK_TOOL =
            java.util.spi.ToolProvider.findFirst("jlink").orElseThrow(() ->
                new RuntimeException("jlink tool not found")
            );

        private final List<Path> jars = new ArrayList<>();
        private final List<Path> jmods = new ArrayList<>();
        private final List<Path> pluginModulePath = new ArrayList<>();
        private final List<String> addMods = new ArrayList<>();
        private final List<String> limitMods = new ArrayList<>();
        private final List<String> options = new ArrayList<>();
        private String modulePath;
        // if you want to specifiy repeated --module-path option
        private String repeatedModulePath;
        // if you want to specifiy repeated --limit-modules option
        private String repeatedLimitMods;
        private Path output;
        private Path existing;
        private String launcher; // optional

        public JLinkTask modulePath(String modulePath) {
            this.modulePath = modulePath;
            return this;
        }

        public JLinkTask launcher(String cmd) {
            launcher = Objects.requireNonNull(cmd);
            return this;
        }

        public JLinkTask repeatedModulePath(String modulePath) {
            this.repeatedModulePath = modulePath;
            return this;
        }

        public JLinkTask addJars(Path jars) {
            this.jars.add(jars);
            return this;
        }

        public JLinkTask addJmods(Path jmods) {
            this.jmods.add(jmods);
            return this;
        }

        public JLinkTask pluginModulePath(Path p) {
            this.pluginModulePath.add(p);
            return this;
        }

        public JLinkTask addMods(String moduleName) {
            this.addMods.add(moduleName);
            return this;
        }

        public JLinkTask limitMods(String moduleName) {
            this.limitMods.add(moduleName);
            return this;
        }

        public JLinkTask repeatedLimitMods(String modules) {
            this.repeatedLimitMods = modules;
            return this;
        }

        public JLinkTask output(Path output) {
            this.output = output;
            return this;
        }

        public JLinkTask existing(Path existing) {
            this.existing = existing;
            return this;
        }

        public JLinkTask option(String o) {
            this.options.add(o);
            return this;
        }

        private String modulePath() {
            // This is expect FIRST jmods THEN jars, if you change this, some tests could fail
            String jmods = toPath(this.jmods);
            String jars = toPath(this.jars);
            return jmods + File.pathSeparator + jars;
        }

        private String toPath(List<Path> paths) {
            return paths.stream()
                    .map(Path::toString)
                    .collect(Collectors.joining(File.pathSeparator));
        }

        private String[] optionsJLink() {
            List<String> options = new ArrayList<>();
            if (output != null) {
                options.add(OUTPUT_OPTION);
                options.add(output.toString());
            }
            if (!addMods.isEmpty()) {
                options.add(ADD_MODULES_OPTION);
                options.add(addMods.stream().collect(Collectors.joining(",")));
            }
            if (!limitMods.isEmpty()) {
                options.add(LIMIT_MODULES_OPTION);
                options.add(limitMods.stream().collect(Collectors.joining(",")));
            }
            if (repeatedLimitMods != null) {
                options.add(LIMIT_MODULES_OPTION);
                options.add(repeatedLimitMods);
            }
            if (!jars.isEmpty() || !jmods.isEmpty()) {
                options.add(MODULE_PATH_OPTION);
                options.add(modulePath());
            }
            if (modulePath != null) {
                options.add(MODULE_PATH_OPTION);
                options.add(modulePath);
            }
            if (repeatedModulePath != null) {
                options.add(MODULE_PATH_OPTION);
                options.add(repeatedModulePath);
            }
            if (!pluginModulePath.isEmpty()) {
                options.add(PLUGIN_MODULE_PATH);
                options.add(toPath(pluginModulePath));
            }
            if (launcher != null && !launcher.isEmpty()) {
                options.add(LAUNCHER);
                options.add(launcher);
            }
            options.addAll(this.options);
            return options.toArray(new String[options.size()]);
        }

        private String[] optionsPostProcessJLink() {
            List<String> options = new ArrayList<>();
            if (existing != null) {
                options.add(POST_PROCESS_OPTION);
                options.add(existing.toString());
            }
            options.addAll(this.options);
            return options.toArray(new String[options.size()]);
        }

        public Result call() {
            String[] args = optionsJLink();
            System.err.println("jlink options: " + optionsPrettyPrint(args));
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            int exitCode = JLINK_TOOL.run(pw, pw, args);
            return new Result(exitCode, writer.toString(), output);
        }

        public Result callPostProcess() {
            String[] args = optionsPostProcessJLink();
            System.err.println("jlink options: " + optionsPrettyPrint(args));
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            int exitCode = JLINK_TOOL.run(pw, pw, args);
            return new Result(exitCode, writer.toString(), output);
        }
    }

    public static class InMemorySourceFile {
        public final String packageName;
        public final String className;
        public final String source;

        public InMemorySourceFile(String packageName, String simpleName, String source) {
            this.packageName = packageName;
            this.className = simpleName;
            this.source = source;
        }
    }

    public static class InMemoryFile {
        private final String path;
        private final byte[] bytes;

        public String getPath() {
            return path;
        }

        public InputStream getBytes() {
            return new ByteArrayInputStream(bytes);
        }

        public InMemoryFile(String path, byte[] bytes) {
            this.path = path;
            this.bytes = bytes;
        }

        public InMemoryFile(String path, InputStream is) throws IOException {
            this(path, readAllBytes(is));
        }
    }

    public static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (true) {
            int n = is.read(buf);
            if (n < 0) {
                break;
            }
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }
}
