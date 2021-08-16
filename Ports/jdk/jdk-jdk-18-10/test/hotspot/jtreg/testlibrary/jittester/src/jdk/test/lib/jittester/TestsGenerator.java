/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

public abstract class TestsGenerator implements BiConsumer<IRNode, IRNode> {
    private static final int DEFAULT_JTREG_TIMEOUT = 120;
    protected static final String JAVA_BIN = getJavaPath();
    protected static final String JAVAC = Paths.get(JAVA_BIN, "javac").toString();
    protected static final String JAVA = Paths.get(JAVA_BIN, "java").toString();
    protected final Path generatorDir;
    protected final Path tmpDir;
    protected final Function<String, String[]> preRunActions;
    protected final String jtDriverOptions;
    private static final String DISABLE_WARNINGS = "-XX:-PrintWarnings";

    protected TestsGenerator(String suffix) {
        this(suffix, s -> new String[0], "");
    }

    protected TestsGenerator(String suffix, Function<String, String[]> preRunActions,
            String jtDriverOptions) {
        generatorDir = getRoot().resolve(suffix).toAbsolutePath();
        try {
            tmpDir = Files.createTempDirectory(suffix).toAbsolutePath();
        } catch (IOException e) {
            throw new Error("Can't get a tmp dir for " + suffix, e);
        }
        this.preRunActions = preRunActions;
        this.jtDriverOptions = jtDriverOptions;
    }

    protected void generateGoldenOut(String mainClassName) {
        String classPath = tmpDir.toString() + File.pathSeparator
                + generatorDir.toString();
        ProcessBuilder pb = new ProcessBuilder(JAVA, "-Xint", DISABLE_WARNINGS, "-Xverify",
                "-cp", classPath, mainClassName);
        String goldFile = mainClassName + ".gold";
        try {
            runProcess(pb, generatorDir.resolve(goldFile).toString());
        } catch (IOException | InterruptedException e)  {
            throw new Error("Can't run generated test ", e);
        }
    }

    protected static int runProcess(ProcessBuilder pb, String name)
            throws IOException, InterruptedException {
        pb.redirectError(new File(name + ".err"));
        pb.redirectOutput(new File(name + ".out"));
        Process process = pb.start();
        try {
            if (process.waitFor(DEFAULT_JTREG_TIMEOUT, TimeUnit.SECONDS)) {
                try (FileWriter file = new FileWriter(name + ".exit")) {
                    file.write(Integer.toString(process.exitValue()));
                }
                return process.exitValue();
            }
        } finally {
            process.destroyForcibly();
        }
        return -1;
    }

    protected void compilePrinter() {
        Path root = getRoot();
        ProcessBuilder pbPrinter = new ProcessBuilder(JAVAC,
                "-d", tmpDir.toString(),
                root.resolve("jdk")
                    .resolve("test")
                    .resolve("lib")
                    .resolve("jittester")
                    .resolve("jtreg")
                    .resolve("Printer.java")
                    .toString());
        try {
            int exitCode = runProcess(pbPrinter, root.resolve("Printer").toString());
            if (exitCode != 0) {
                throw new Error("Printer compilation returned exit code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new Error("Can't compile printer", e);
        }
    }

    protected static void ensureExisting(Path path) {
        if (Files.notExists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    protected String getJtregHeader(String mainClassName) {
        String synopsis = "seed = '" + ProductionParams.seed.value() + "'"
                + ", specificSeed = '" + PseudoRandom.getCurrentSeed() + "'";
        StringBuilder header = new StringBuilder();
        header.append("/*\n * @test\n * @summary ")
              .append(synopsis)
              .append(" \n * @library / ../\n");
        header.append(" * @run build jdk.test.lib.jittester.jtreg.JitTesterDriver "
                        + "jdk.test.lib.jittester.jtreg.Printer\n");
        for (String action : preRunActions.apply(mainClassName)) {
            header.append(" * ")
                  .append(action)
                  .append("\n");
        }
        header.append(" * @run driver jdk.test.lib.jittester.jtreg.JitTesterDriver ")
              .append(DISABLE_WARNINGS)
              .append(" ")
              .append(jtDriverOptions)
              .append(" ")
              .append(mainClassName)
              .append("\n */\n\n");
        if (ProductionParams.printHierarchy.value()) {
            header.append("/*\n")
                  .append(printHierarchy())
                  .append("*/\n");
        }
        return header.toString();
    }

    protected static Path getRoot() {
        return Paths.get(ProductionParams.testbaseDir.value());
    }

    protected static void writeFile(Path targetDir, String fileName, String content) {
        try (FileWriter file = new FileWriter(targetDir.resolve(fileName).toFile())) {
            file.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String printHierarchy() {
        return TypeList.getAll()
                .stream()
                .filter(t -> t instanceof TypeKlass)
                .map(t -> typeDescription((TypeKlass) t))
                .collect(Collectors.joining("\n","CLASS HIERARCHY:\n", "\n"));
    }

    private static String typeDescription(TypeKlass type) {
        StringBuilder result = new StringBuilder();
        String parents = type.getParentsNames().stream().collect(Collectors.joining(","));
        result.append(type.isAbstract() ? "abstract " : "")
              .append(type.isFinal() ? "final " : "")
              .append(type.isInterface() ? "interface " : "class ")
              .append(type.getName())
              .append(parents.isEmpty() ? "" : ": " + parents);
        return result.toString();
    }

    private static String getJavaPath() {
        String[] env = { "JDK_HOME", "JAVA_HOME", "BOOTDIR" };
        for (String name : env) {
            String path = System.getenv(name);
            if (path != null) {
                return Paths.get(path)
                            .resolve("bin")
                            .toAbsolutePath()
                            .toString();
            }
        }
        return "";
    }
}
