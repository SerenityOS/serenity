/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.hotspot.tools.ctw;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handler for dirs containing jar-files with classes to compile.
 */
public class ClassPathJarInDirEntry {
    public static List<PathHandler> create(Path path) {
        Objects.requireNonNull(path);
        if (!Files.exists(path)) {
            throw new Error(path + " directory not found");
        }
        try {
            return Stream.concat(
                    Stream.of(new PathHandler(new JarInDirEntry(path))),
                    Files.list(path)
                         .filter(p -> p.getFileName().toString().endsWith(".jar"))
                         .map(ClassPathJarEntry::new)
                         .map(PathHandler::new))
                         .collect(Collectors.toList());
        } catch (IOException e) {
            throw new Error("can not read " + path + " directory : " + e.getMessage(), e);
        }
    }

    // dummy path handler, used just to print description before real handlers.
    private static class JarInDirEntry extends PathHandler.PathEntry {
        private JarInDirEntry(Path root) {
            super(root);
        }

        @Override
        protected byte[] findByteCode(String name) {
            return null;
        }

        @Override
        protected Stream<String> classes() {
            return Stream.empty();
        }

        @Override
        protected String description() {
            return "# jar_in_dir: " + root;
        }
    }
}

