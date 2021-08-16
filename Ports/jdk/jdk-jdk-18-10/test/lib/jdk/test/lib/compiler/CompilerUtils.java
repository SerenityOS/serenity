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

package jdk.test.lib.compiler;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class consists exclusively of static utility methods for invoking the
 * java compiler.
 */
public final class CompilerUtils {
    private CompilerUtils() { }

    /**
     * Compile all the java sources in {@code <source>/**} to
     * {@code <destination>/**}. The destination directory will be created if
     * it doesn't exist.
     *
     * Equivalent to calling {@code compile(source, destination, true, options);}.
     *
     * All warnings/errors emitted by the compiler are output to System.out/err.
     *
     * @param source Path to the source directory
     * @param destination Path to the destination directory
     * @param options Any options to pass to the compiler
     *
     * @return true if the compilation is successful
     *
     * @throws IOException
     *         if there is an I/O error scanning the source tree or
     *         creating the destination directory
     * @throws UnsupportedOperationException
     *         if there is no system java compiler
     */
    public static boolean compile(Path source, Path destination, String... options)
        throws IOException
    {
        return compile(source, destination, true, options);
    }

    /**
     * Compile all the java sources in {@code <source>} and optionally its
     * subdirectories, to
     * {@code <destination>}. The destination directory will be created if
     * it doesn't exist.
     *
     * All warnings/errors emitted by the compiler are output to System.out/err.
     *
     * @param source Path to the source directory
     * @param destination Path to the destination directory
     * @param recurse If {@code true} recurse into any {@code source} subdirectories
     *        to compile all java source files; else only compile those directly in
     *        {@code source}.
     * @param options Any options to pass to the compiler
     *
     * @return true if the compilation is successful
     *
     * @throws IOException
     *         if there is an I/O error scanning the source tree or
     *         creating the destination directory
     * @throws UnsupportedOperationException
     *         if there is no system java compiler
     */

   public static boolean compile(Path source, Path destination, boolean recurse, String... options)
        throws IOException
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            // no compiler available
            throw new UnsupportedOperationException("Unable to get system java compiler. "
                    + "Perhaps, jdk.compiler module is not available.");
        }
        StandardJavaFileManager jfm = compiler.getStandardFileManager(null, null, null);

        List<Path> sources
            = Files.find(source, (recurse ? Integer.MAX_VALUE : 1),
                (file, attrs) -> (file.toString().endsWith(".java")))
                .collect(Collectors.toList());

        Files.createDirectories(destination);
        jfm.setLocation(StandardLocation.CLASS_PATH, Collections.emptyList());
        jfm.setLocationFromPaths(StandardLocation.CLASS_OUTPUT,
                Collections.singletonList(destination));

        List<String> opts = Arrays.asList(options);
        JavaCompiler.CompilationTask task
            = compiler.getTask(null, jfm, null, opts, null,
                jfm.getJavaFileObjectsFromPaths(sources));

        return task.call();
    }
}
