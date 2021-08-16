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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Handler for jar-files containing classes to compile.
 */
public class ClassPathJarEntry extends PathHandler.PathEntry {
    private final JarFile jarFile;

    public ClassPathJarEntry(Path root) {
        super(root);
        if (!Files.exists(root)) {
            throw new Error(root + " file not found");
        }
        try {
            jarFile = new JarFile(root.toFile());
        } catch (IOException e) {
            throw new Error("can not read " + root + " : " + e.getMessage(), e);
        }
    }

    @Override
    protected Stream<String> classes() {
        return jarFile.stream()
                      .map(JarEntry::getName)
                      .filter(Utils::isClassFile)
                      .map(Utils::fileNameToClassName);
    }

    @Override
    protected String description() {
        return "# jar: " + root;
    }

    @Override
    protected byte[] findByteCode(String classname) {
        try {
            String filename = Utils.classNameToFileName(classname);
            JarEntry entry = jarFile.getJarEntry(filename);

            if (entry == null) {
                return null;
            }

            try (InputStream is = jarFile.getInputStream(entry)) {
                return is.readAllBytes();
            }

        } catch (IOException e) {
            e.printStackTrace(CompileTheWorld.ERR);
            return null;
        }
    }
}

