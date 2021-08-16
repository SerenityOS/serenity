/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for Files.walkFileTree to test SKIP_SIBLINGS return value
 * @library ../..
 * @compile SkipSiblings.java CreateFileTree.java
 * @run main SkipSiblings
 * @key randomness
 */

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.*;

public class SkipSiblings {

    static final Random rand = new Random();
    static final Set<Path> skipped = new HashSet<Path>();

    // check if this path's directory has been skipped
    static void check(Path path) {
        if (skipped.contains(path.getParent()))
            throw new RuntimeException(path + " should not have been visited");
    }

    // indicates if the siblings of this path should be skipped
    static boolean skip(Path path) {
        Path parent = path.getParent();
        if (parent != null && rand.nextBoolean()) {
            skipped.add(parent);
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Path top = CreateFileTree.create();
        try {
            test(top);
        } finally {
            TestUtil.removeAll(top);
        }
    }

    static void test(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                check(dir);
                if (skip(dir))
                    return FileVisitResult.SKIP_SIBLINGS;
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                check(file);
                if (skip(file))
                    return FileVisitResult.SKIP_SIBLINGS;
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException x) {
                if (x != null)
                    throw new RuntimeException(x);
                check(dir);
                if (rand.nextBoolean()) {
                    return FileVisitResult.CONTINUE;
                } else {
                    return FileVisitResult.SKIP_SIBLINGS;
                }
            }
        });
    }
}
