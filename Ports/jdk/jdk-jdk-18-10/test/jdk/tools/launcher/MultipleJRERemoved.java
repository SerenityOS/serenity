/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8067437
 * @summary Verify Multiple JRE version support has been removed.
 * @modules jdk.compiler
 *          jdk.zipfs
 * @build TestHelper
 * @run main MultipleJRERemoved
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class MultipleJRERemoved extends TestHelper {

    public static final String VERSION_JAR = "version.jar";
    public static final String PRINT_VERSION_CLASS = "PrintVersion";
    private final File javaFile = new File(PRINT_VERSION_CLASS + ".java");
    private final File clsFile = new File(PRINT_VERSION_CLASS + ".class");

    private MultipleJRERemoved() {
    }

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws Exception {
        MultipleJRERemoved a = new MultipleJRERemoved();
        a.run(args);
    }

    /**
     * Check all combinations of flags: "-version:", "-jre-restrict-search", "-jre-no-restrict-search". Test expects to see errors.
     */
    @Test
    public void allFlagCombinations() throws IOException {
        final Pattern newLine = Pattern.compile("\n");
        createJar(Collections.emptyMap());

        for (Flag flag1 : Flag.values()) {
            for (Flag flag2 : Flag.values()) {
                for (Flag flag3 : Flag.values()) {
                    List<Flag> flags = Stream.of(flag1, flag2, flag3)
                            .filter(f -> !Flag.EMPTY.equals(f))
                            .collect(Collectors.toList());

                    if (flags.size() == 0) continue;

                    List<String> flagValues = flags.stream()
                            .map(Flag::value)
                            .collect(Collectors.toList());

                    List<String> errorMessages = flags.stream()
                            .map(Flag::errorMessage)
                            .flatMap(newLine::splitAsStream)
                            .collect(Collectors.toList());

                    List<String> jarCmd = new ArrayList<>();
                    jarCmd.add(javaCmd);
                    jarCmd.addAll(flagValues);
                    jarCmd.add("-jar");
                    jarCmd.add("version.jar");

                    check(jarCmd, errorMessages);

                    List<String> cmd = new ArrayList<>();
                    cmd.add(javaCmd);
                    cmd.addAll(flagValues);
                    cmd.add(PRINT_VERSION_CLASS);

                    check(cmd, errorMessages);
                }
            }
        }
    }

    private void check(List<String> cmd, List<String> errorMessages) {
        TestResult tr = doExec(cmd.toArray(new String[cmd.size()]));
        tr.checkNegative();
        tr.isNotZeroOutput();
        errorMessages.forEach(tr::contains);

        if (!tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test case: failed\n" + cmd);
        }
    }

    /**
     * Verifies that java -help output doesn't contain information about "mJRE" flags.
     */
    @Test
    public void javaHelp() {
        TestResult tr = doExec(javaCmd, "-help");
        tr.checkPositive();
        tr.isNotZeroOutput();
        tr.notContains("-version:<value>");
        tr.notContains("-jre-restrict-search");
        tr.notContains("-jre-no-restrict-search");
        tr.notContains("-no-jre-restrict-search");  //it's not a typo in flag name.
        if (!tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("Failed. java -help output contains obsolete flags.\n");
        }
    }

    /**
     * Verifies that java -jar version.jar output ignores "mJRE" manifest directives.
     */
    @Test
    public void manifestDirectives() throws IOException {
        Map<String, String> manifest = new TreeMap<>();
        manifest.put("JRE-Version", "1.8");
        manifest.put("JRE-Restrict-Search", "1.8");
        createJar(manifest);

        TestResult tr = doExec(javaCmd, "-jar", VERSION_JAR);
        tr.checkPositive();
        tr.contains(System.getProperty("java.version"));
        if (!tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("Failed.\n");
        }
    }

    private void emitFile() throws IOException {
        List<String> scr = new ArrayList<>();
        scr.add("public class PrintVersion {");
        scr.add("    public static void main(String... args) {");
        scr.add("       System.out.println(System.getProperty(\"java.version\"));");
        scr.add("    }");
        scr.add("}");
        createFile(javaFile, scr);
        compile(javaFile.getName());
    }

    private void createJar(Map<String, String> manifestAttributes) throws IOException {
        emitFile();

        Manifest manifest = new Manifest();
        final Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.putValue("Manifest-Version", "1.0");
        mainAttributes.putValue("Main-Class", PRINT_VERSION_CLASS);
        manifestAttributes.forEach(mainAttributes::putValue);

        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(VERSION_JAR), manifest)) {
            jar.putNextEntry(new ZipEntry(PRINT_VERSION_CLASS + ".class"));
            jar.write(Files.readAllBytes(clsFile.toPath()));
            jar.closeEntry();
        } finally {
            javaFile.delete();
        }
    }

    private enum Flag {
        EMPTY("", ""),
        VERSION("-version:1.9", "Error: Specifying an alternate JDK/JRE version is no longer supported.\n" +
                "The use of the flag '-version:' is no longer valid.\n" +
                "Please download and execute the appropriate version."),
        JRE_RESTRICT_SEARCH("-jre-restrict-search", "Error: Specifying an alternate JDK/JRE is no longer supported.\n" +
                "The related flags -jre-restrict-search | -jre-no-restrict-search are also no longer valid."),
        JRE_NO_RESTRICT_SEARCH("-jre-no-restrict-search", "Error: Specifying an alternate JDK/JRE is no longer supported.\n" +
                "The related flags -jre-restrict-search | -jre-no-restrict-search are also no longer valid.");
        private final String flag;
        private final String errorMessage;

        Flag(String flag, String errorMessage) {
            this.flag = flag;
            this.errorMessage = errorMessage;
        }

        String value() {
            return flag;
        }

        String errorMessage() {
            return errorMessage;
        }
    }
}
