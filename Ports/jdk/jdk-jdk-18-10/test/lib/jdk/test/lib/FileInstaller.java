/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

// !!!
// NOTE: this class is widely used. DO NOT depend on any other classes in any test library, or else
// you may see intermittent ClassNotFoundException as in JDK-8188828
// !!!

/**
 * Copy a resource: file or directory recursively, using relative path(src and dst)
 * which are applied to test source directory(src) and current directory(dst)
 */
public class FileInstaller {
    public static final String TEST_SRC = System.getProperty("test.src", "").trim();

    /**
     * @param args source and destination
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Unexpected number of arguments for file copy");
        }
        Path src = Paths.get(TEST_SRC, args[0]).toAbsolutePath().normalize();
        Path dst = Paths.get(args[1]).toAbsolutePath().normalize();
        if (src.toFile().exists()) {
            System.out.printf("copying %s to %s%n", src, dst);
            if (src.toFile().isDirectory()) {
                // can't use Files::copy for dirs, as 'dst' might exist already
                Files.walkFileTree(src, new CopyFileVisitor(src, dst));
            } else {
                Path dstDir = dst.getParent();
                if (!dstDir.toFile().exists()) {
                    Files.createDirectories(dstDir);
                }
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            throw new IOException("Can't find source " + src);
        }
    }

    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {
        private final Path copyFrom;
        private final Path copyTo;

        public CopyFileVisitor(Path copyFrom, Path copyTo) {
            this.copyFrom = copyFrom;
            this.copyTo = copyTo;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path file,
                BasicFileAttributes attrs) throws IOException {
            Path relativePath = copyFrom.relativize(file);
            Path destination = copyTo.resolve(relativePath);
            if (!destination.toFile().exists()) {
                Files.createDirectories(destination);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attrs) throws IOException {
            if (!file.toFile().isFile()) {
                return FileVisitResult.CONTINUE;
            }
            Path relativePath = copyFrom.relativize(file);
            Path destination = copyTo.resolve(relativePath);
            Files.copy(file, destination, StandardCopyOption.COPY_ATTRIBUTES);
            return FileVisitResult.CONTINUE;
        }
    }
}
