/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.compiler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utility class for creating test modules.
 */
public class ModuleInfoMaker {
    private static final String MODULE_INFO_JAVA = "module-info.java";
    private static final Pattern MODULE_PATTERN =
        Pattern.compile("module\\s+((?:\\w+\\.)*)");
    private static final Pattern PACKAGE_PATTERN =
                       Pattern.compile("package\\s+(((?:\\w+\\.)*)(?:\\w+))");
    private static final Pattern CLASS_PATTERN =
          Pattern.compile("(?:public\\s+)?(?:class|enum|interface)\\s+(\\w+)");

    private final Path dir;

    public ModuleInfoMaker(Path dir) {
        this.dir = dir;
    }

    /**
     * Create java source files of the given module
     */
    public void writeJavaFiles(String module, String moduleInfoJava, String... contents)
        throws IOException
    {
        Path msrc = dir.resolve(module);
        new JavaSource(moduleInfoJava).write(msrc);
        for (String c : contents) {
            new JavaSource(c).write(msrc);
        }
    }

    /**
     * Compile the module to the given destination.
     */
    public void compile(String module, Path dest, String... options)
        throws IOException
    {
        Path msrc = dir.resolve(module);
        String[] args =
            Stream.concat(Arrays.stream(options),
                          Stream.of("--module-source-path",
                                    dir.toString())).toArray(String[]::new);
        if (!CompilerUtils.compile(msrc, dest, args)) {
            throw new Error("Fail to compile " + module);
        }
    }

    static class JavaSource {
        final String source;
        JavaSource(String source) {
            this.source = source;
        }

        /**
         * Writes the source code to a file in a specified directory.
         * @param dir the directory
         * @throws IOException if there is a problem writing the file
         */
        public void write(Path dir) throws IOException {
            Path file = dir.resolve(getJavaFileNameFromSource(source));
            Files.createDirectories(file.getParent());
            try (BufferedWriter out = Files.newBufferedWriter(file)) {
                out.write(source.replace("\n", System.lineSeparator()));
            }
        }

        /**
         * Extracts the Java file name from the class declaration.
         * This method is intended for simple files and uses regular expressions,
         * so comments matching the pattern can make the method fail.
         */
        static String getJavaFileNameFromSource(String source) {
            String packageName = null;

            Matcher matcher = MODULE_PATTERN.matcher(source);
            if (matcher.find())
                return MODULE_INFO_JAVA;

            matcher = PACKAGE_PATTERN.matcher(source);
            if (matcher.find())
                packageName = matcher.group(1).replace(".", "/");

            matcher = CLASS_PATTERN.matcher(source);
            if (matcher.find()) {
                String className = matcher.group(1) + ".java";
                return (packageName == null) ? className : packageName + "/" + className;
            } else if (packageName != null) {
                return packageName + "/package-info.java";
            } else {
                throw new Error("Could not extract the java class " +
                    "name from the provided source");
            }
        }
    }
}
