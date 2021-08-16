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

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CompilerUtils {
    private CompilerUtils() { }

    /**
     * Compile all the java sources in {@code <source>/**} to
     * {@code <destination>/**}. The destination directory will be created if
     * it doesn't exist.
     *
     * All warnings/errors emitted by the compiler are output to System.out/err.
     *
     * @return true if the compilation is successful
     *
     * @throws IOException if there is an I/O error scanning the source tree or
     *                     creating the destination directory
     */
    public static boolean compile(Path source, Path destination, String... options)
        throws IOException
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager jfm = compiler.getStandardFileManager(null, null, null);

        List<Path> sources
            = Files.find(source, Integer.MAX_VALUE,
                         (file, attrs) -> (file.toString().endsWith(".java")))
                   .collect(Collectors.toList());

        Files.createDirectories(destination);
        jfm.setLocationFromPaths(StandardLocation.CLASS_OUTPUT,
                                 Arrays.asList(destination));

        List<String> opts = Arrays.asList(options);
        JavaCompiler.CompilationTask task
            = compiler.getTask(null, jfm, null, opts, null,
                               jfm.getJavaFileObjectsFromPaths(sources));

        return task.call();
    }

    /**
     * Compile the specified module from the given module sourcepath
     *
     * All warnings/errors emitted by the compiler are output to System.out/err.
     *
     * @return true if the compilation is successful
     *
     * @throws IOException if there is an I/O error scanning the source tree or
     *                     creating the destination directory
     */
    public static boolean compileModule(Path source, Path destination,
                                        String moduleName, String... options) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager jfm = compiler.getStandardFileManager(null, null, null);

        try {
            Files.createDirectories(destination);
            jfm.setLocationFromPaths(StandardLocation.CLASS_OUTPUT,
                                     Arrays.asList(destination));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Stream<String> opts = Arrays.stream(new String[] {
            "--module-source-path", source.toString(), "-m", moduleName
        });
        List<String> javacOpts = Stream.concat(opts, Arrays.stream(options))
                                        .collect(Collectors.toList());
        JavaCompiler.CompilationTask task
            = compiler.getTask(null, jfm, null, javacOpts, null, null);
        return task.call();
    }


    public static void cleanDir(Path dir) throws IOException {
        if (Files.notExists(dir)) return;

        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException
            {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw e;
                }
            }
        });
        Files.deleteIfExists(dir);
    }
}
