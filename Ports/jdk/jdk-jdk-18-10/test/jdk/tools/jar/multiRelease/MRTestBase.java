/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.testng.Assert.assertEquals;

public class MRTestBase {

    public static final int SUCCESS = 0;

    protected final String src = System.getProperty("test.src", ".");
    protected final String usr = System.getProperty("user.dir", ".");

    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
            .orElseThrow(()
                    -> new RuntimeException("jar tool not found")
            );

    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(()
                    -> new RuntimeException("javac tool not found")
            );

    protected void compile(String test) throws Throwable {
        Path classes = Paths.get(usr, "classes", "base");
        Files.createDirectories(classes);
        Path source = Paths.get(src, "data", test, "base", "version");
        javac(classes, source.resolve("Main.java"), source.resolve("Version.java"));

        classes = Paths.get(usr, "classes", "v9");
        Files.createDirectories(classes);
        source = Paths.get(src, "data", test, "v9", "version");
        javac(classes, source.resolve("Version.java"));

        classes = Paths.get(usr, "classes", "v10");
        Files.createDirectories(classes);
        source = Paths.get(src, "data", test, "v10", "version");
        javac(classes, source.resolve("Version.java"));
    }

    protected void checkMultiRelease(String jarFile,
                                     boolean expected) throws IOException {
        try (JarFile jf = new JarFile(new File(jarFile), true,
                ZipFile.OPEN_READ, JarFile.runtimeVersion())) {
            assertEquals(jf.isMultiRelease(), expected);
        }
    }

    // compares the bytes found in the jar entries with the bytes found in the
    // corresponding data files used to create the entries
    protected void compare(String jarfile,
                           Map<String, String[]> names) throws IOException {
        try (JarFile jf = new JarFile(jarfile)) {
            for (String name : names.keySet()) {
                Path path = Paths.get("classes", names.get(name));
                byte[] b1 = Files.readAllBytes(path);
                byte[] b2;
                JarEntry je = jf.getJarEntry(name);
                try (InputStream is = jf.getInputStream(je)) {
                    b2 = is.readAllBytes();
                }
                assertEquals(b1, b2);
            }
        }
    }

    void javac(Path dest, Path... sourceFiles) throws Throwable {

        List<String> commands = new ArrayList<>();
        String opts = System.getProperty("test.compiler.opts");
        if (!opts.isEmpty()) {
            commands.addAll(Arrays.asList(opts.split(" +")));
        }
        commands.addAll(Utils.getForwardVmOptions());
        commands.add("-d");
        commands.add(dest.toString());
        Stream.of(sourceFiles)
                .map(Object::toString)
                .forEach(x -> commands.add(x));

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            int rc = JAVAC_TOOL.run(pw, pw, commands.toArray(new String[0]));
            if(rc != 0) {
                throw new RuntimeException(sw.toString());
            }
        }

    }

    OutputAnalyzer jarWithStdin(File stdinSource,
                                String... args) throws Throwable {

        String jar = JDKToolFinder.getJDKTool("jar");
        List<String> commands = new ArrayList<>();
        commands.add(jar);
        commands.addAll(Utils.getForwardVmOptions());
        Stream.of(args).forEach(x -> commands.add(x));
        ProcessBuilder p = new ProcessBuilder(commands);
        if (stdinSource != null)
            p.redirectInput(stdinSource);
        return ProcessTools.executeCommand(p);
    }

    OutputAnalyzer jar(String... args) throws Throwable {
        return jarWithStdin(null, args);
    }

    OutputAnalyzer jarTool(String... args) {
        List<String> commands = new ArrayList<>();
        commands.addAll(Utils.getForwardVmOptions());
        Stream.of(args).forEach(x -> commands.add(x));
        return run(JAR_TOOL, args);
    }

    OutputAnalyzer run(ToolProvider tp, String[] commands) {
        int rc = 0;
        StringWriter sw = new StringWriter();
        StringWriter esw = new StringWriter();

        try (PrintWriter pw = new PrintWriter(sw);
             PrintWriter epw = new PrintWriter(esw)) {
            rc = tp.run(pw, epw, commands);
        }
        return new OutputAnalyzer(sw.toString(), esw.toString(), rc);
    }
}