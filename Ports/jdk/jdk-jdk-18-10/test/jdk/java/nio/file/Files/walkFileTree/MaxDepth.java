/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Unit test for Files.walkFileTree to test maxDepth parameter
 * @library ../..
 * @compile MaxDepth.java CreateFileTree.java
 * @run main MaxDepth
 */

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.*;

public class MaxDepth {
    public static void main(String[] args) throws IOException {
        Path top = CreateFileTree.create();
        try {
            test(top);
        } finally {
            TestUtil.removeAll(top);
        }
    }

    static void test(final Path top) throws IOException {
        for (int i=0; i<5; i++) {
            Set<FileVisitOption> opts = Collections.emptySet();
            final int maxDepth = i;
            Files.walkFileTree(top, opts, maxDepth, new SimpleFileVisitor<Path>() {
                // compute depth based on relative path to top directory
                private int depth(Path file) {
                    Path rp = file.relativize(top);
                    return (rp.getFileName().toString().equals("")) ? 0 : rp.getNameCount();
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    int d = depth(dir);
                    if (d == maxDepth)
                        throw new RuntimeException("Should not open directories at maxDepth");
                    if (d > maxDepth)
                        throw new RuntimeException("Too deep");
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    int d = depth(file);
                    if (d > maxDepth)
                        throw new RuntimeException("Too deep");
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
