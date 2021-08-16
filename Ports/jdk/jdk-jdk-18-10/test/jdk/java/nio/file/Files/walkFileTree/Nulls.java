/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4313887 6865748
 * @summary Unit test for java.nio.file.Files.walkFileTree
 */

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import java.util.*;

public class Nulls {

    static void npeExpected() {
        throw new RuntimeException("NullPointerException expected");
    }

    public static void main(String[] args) throws IOException {
        try {
            Files.walkFileTree(null, EnumSet.noneOf(FileVisitOption.class),
                Integer.MAX_VALUE, new SimpleFileVisitor<Path>(){});
            npeExpected();
        } catch (NullPointerException e) {
        }
        try {
            Files.walkFileTree(Paths.get("."), null, Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>(){});
            npeExpected();
        } catch (NullPointerException e) {
        }
        try {
            Files.walkFileTree(Paths.get("."), EnumSet.noneOf(FileVisitOption.class),
                -1, new SimpleFileVisitor<Path>(){});
            throw new RuntimeException("IllegalArgumentExpected expected");
        } catch (IllegalArgumentException e) {
        }
        try {
            Set<FileVisitOption> opts = new HashSet<>(1);
            opts.add(null);
            Files.walkFileTree(Paths.get("."), opts, Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>(){});
            npeExpected();
        } catch (NullPointerException e) {
        }
        try {
            Files.walkFileTree(Paths.get("."), EnumSet.noneOf(FileVisitOption.class),
                Integer.MAX_VALUE, null);
            npeExpected();
        } catch (NullPointerException e) {
        }

        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() { };
        boolean ranTheGauntlet = false;
        Path dir = Paths.get(".");
        BasicFileAttributes attrs = Files.readAttributes(dir, BasicFileAttributes.class);

        try { visitor.preVisitDirectory(null, attrs);
        } catch (NullPointerException x0) {
        try { visitor.preVisitDirectory(dir, null);
        } catch (NullPointerException x1) {
        try { visitor.visitFile(null, attrs);
        } catch (NullPointerException x2) {
        try {  visitor.visitFile(dir, null);
        } catch (NullPointerException x3) {
        try { visitor.visitFileFailed(null, new IOException());
        } catch (NullPointerException x4) {
        try { visitor.visitFileFailed(dir, null);
        } catch (NullPointerException x5) {
        try { visitor.postVisitDirectory(null, new IOException());
        } catch (NullPointerException x6) {
            // if we get here then all visit* methods threw NPE as expected
            ranTheGauntlet = true;
        }}}}}}}
        if (!ranTheGauntlet)
            throw new RuntimeException("A visit method did not throw NPE");
    }
}
