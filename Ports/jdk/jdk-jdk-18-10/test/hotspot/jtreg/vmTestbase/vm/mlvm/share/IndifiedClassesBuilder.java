/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share;

import jdk.test.lib.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.stream.Stream;

public class IndifiedClassesBuilder {
    public static void main(String... args) {
        Path[] targets;
        if (args.length != 0) {
            targets = Arrays.stream(args)
                            .map(Paths::get)
                            .toArray(Path[]::new);
            for (Path path : targets) {
                if (Files.notExists(path)) {
                    throw new Error(path + " doesn't exist");
                }
                if (!Files.isDirectory(path)) {
                    throw new Error(path + " isn't a directory");
                }
            }
        } else {
            targets = Arrays.stream(Utils.TEST_CLASS_PATH.split(File.pathSeparator))
                            .map(Paths::get)
                            .toArray(Path[]::new);
        }
        for (Path path : targets) {
            if (!Files.isDirectory(path)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(path)) {
                files.filter(
                        p -> {
                            String s = p.getFileName().toString();
                            return s.startsWith("INDIFY_") && s.endsWith(".class");
                        })
                     .forEach( p -> IndifiedClassesBuilder.indify(p, path));
            } catch (IOException e) {
                throw new Error("can't traverse path " + path);
            }
        }
    }

    private static void indify(Path file, Path path) {
        Path tmp = Paths.get("indify.tmp");
        try {
            Files.createDirectories(tmp);
        } catch (IOException e) {
            throw new Error("can't create dir " + tmp, e);
        }
        try {
            vm.mlvm.tools.Indify.main(
                    "--all",
                    "--overwrite",
                    "--transitionalJSR292=no",
                    "--dest", tmp.toAbsolutePath().toString(),
                    file.toAbsolutePath().toString());

            // workaround for "nested" classpaths
            if (Files.exists(tmp.resolve(path.relativize(file)))) {
                Files.copy(tmp.resolve(path.relativize(file)), file, StandardCopyOption.REPLACE_EXISTING);
            }

            delete(tmp);
        } catch (IOException e) {
            throw new Error("can't indify " + file, e);
        }
    }

    private static void delete(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                FileVisitResult result = super.visitFile(file, attrs);
                Files.delete(file);
                return result;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                FileVisitResult result = super.postVisitDirectory(dir, exc);
                Files.delete(dir);
                return result;
            }
        });
    }
}
