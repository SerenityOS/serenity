/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javadoc.tester;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public abstract class HtmlChecker extends HtmlParser {
    static final Path currDir = Paths.get(".").toAbsolutePath().normalize();

    protected Path currFile;
    protected int files;
    protected int errors;

    protected HtmlChecker(PrintStream out, Function<Path,String> fileReader) {
        super(out, fileReader);
    }

    public void checkDirectory(Path dir) throws IOException {
        checkFiles(List.of(dir), false, Collections.emptySet());
    }

    public void checkFiles(List<Path> files, boolean skipSubdirs, Set<Path> excludeFiles) throws IOException {
        for (Path file : files) {
            Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
                int depth = 0;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if ((skipSubdirs && depth > 0) || excludeFiles.contains(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    depth++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                    if (excludeFiles.contains(p)) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (Files.isRegularFile(p) && p.getFileName().toString().endsWith(".html")) {
                        checkFile(p);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    depth--;
                    return super.postVisitDirectory(dir, e);
                }
            });
        }
    }

    protected void checkFile(Path file) {
        try {
            currFile = file.toAbsolutePath().normalize();
            read(file);
            files++;
        } catch (IOException e) {
            error(file, 0, e);
        }
    }

    protected abstract void report();

    protected int getErrorCount() {
        return errors;
    }

    @Override
    protected void error(Path file, int lineNumber, String message) {
        super.error(relativePath(file), lineNumber, message);
        errors++;
    }

    @Override
    protected void error(Path file, int lineNumber, Throwable t) {
        super.error(relativePath(file), lineNumber, t);
        errors++;
    }

    protected Path relativePath(Path path) {
        return path.startsWith(currDir) ? currDir.relativize(path) : path;
    }
}
