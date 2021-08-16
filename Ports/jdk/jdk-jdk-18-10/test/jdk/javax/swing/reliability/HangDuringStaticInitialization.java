/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @test
 * @bug 8189604 8208702
 * @requires !vm.debug | os.family != "windows"
 * @run main/othervm -Djava.awt.headless=false HangDuringStaticInitialization
 * @run main/othervm -Djava.awt.headless=true HangDuringStaticInitialization
 */
public final class HangDuringStaticInitialization {

    public static void main(final String[] args) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        test(fs, "/modules/java.desktop");
        test(fs, "/modules/java.datatransfer");
    }

    private static void test(FileSystem fs, String s) throws Exception {
        Files.walkFileTree(fs.getPath(s), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) {
                file = file.subpath(2, file.getNameCount());
                String name = file.toString();
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.indexOf(".")).replace('/', '.');
                    try {
                        Class.forName(name, true, null);
                    } catch (Throwable e) {
                        // only the crash / hang will be considered as failure
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
