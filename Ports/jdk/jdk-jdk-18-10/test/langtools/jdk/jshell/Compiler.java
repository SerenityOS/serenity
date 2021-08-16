/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.ToolBox;

public class Compiler {

    private final ToolBox tb = new ToolBox();

    public Path getClassDir() {
        String classes = ToolBox.testClasses;
        if (classes == null) {
            return Paths.get("build");
        } else {
            return Paths.get(classes);
        }
    }

    public Path getPath(String path) {
        return getPath(Paths.get(path));
    }

    public Path getPath(Path path) {
        return getClassDir().resolve(path);
    }

    public void compile(String...sources) {
        compile(Paths.get("."), sources);
    }

    public void compile(Path directory, String...sources) {
        Path classDir = getClassDir();
        new JavacTask(tb)
                .options("-d", classDir.resolve(directory).toString())
                .sources(sources)
                .run();
    }

    public void jar(String jarName, String...files) {
        jar(Paths.get("."), jarName, files);
    }

    public void jar(Path directory, String jarName, String...files) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        Path classDirPath = getClassDir();
        Path baseDir = classDirPath.resolve(directory);
        Path jarPath = baseDir.resolve(jarName);
        new JarTask(tb, jarPath.toString())
                .manifest(manifest)
                .baseDir(baseDir.toString())
                .files(files).run();
    }

    public void writeToFile(Path path, String...sources) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String source : sources) {
                writer.append(source);
                writer.append('\n');
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
