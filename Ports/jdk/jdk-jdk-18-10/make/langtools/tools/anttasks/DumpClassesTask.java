/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package anttasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class DumpClassesTask extends Task {

    private String moduleName;
    private File dir;

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setDestDir(File dir) {
        this.dir = dir;
    }

    @Override
    public void execute() {
        try (FileSystem fs = FileSystems.newFileSystem(new URI("jrt:/"), Collections.emptyMap(), DumpClassesTask.class.getClassLoader())) {
            Path source = fs.getPath("modules", moduleName);
            Path target = dir.toPath();

            try (Stream<Path> content = Files.walk(source)) {
                content.filter(Files :: isRegularFile)
                       .forEach(p -> {
                    try {
                        Path targetFile = target.resolve(source.relativize(p).toString());
                        if (!Files.exists(targetFile) || Files.getLastModifiedTime(targetFile).compareTo(Files.getLastModifiedTime(source)) < 0) {
                            Files.createDirectories(targetFile.getParent());
                            Files.copy(p, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
            }
        } catch (URISyntaxException | IOException | UncheckedIOException ex) {
            throw new BuildException(ex);
        }
    }
}
