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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Handler for dirs containing classes to compile.
 */
public class ClassPathDirEntry extends PathHandler.PathEntry {
    private final int rootLength;

    public ClassPathDirEntry(Path root) {
        super(root);
        if (!Files.exists(root)) {
            throw new Error(root + " dir does not exist");
        }
        rootLength = root.toString()
                         .length();
    }

    @Override
    protected Stream<String> classes() {
        try {
            return Files.walk(root, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)
                        .filter(p -> Utils.isClassFile(p.toString()))
                        .map(this::pathToClassName);
        } catch (IOException e) {
            throw new Error("can not traverse " + root + " : " + e.getMessage(), e);
        }
    }

    @Override
    protected String description() {
        return "# dir: " + root;
    }

    @Override
    protected byte[] findByteCode(String classname) {
        Path path = root;
        for (String c : Utils.classNameToFileName(classname).split("/")) {
            path = path.resolve(c);
        }
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace(CompileTheWorld.ERR);
            return null;
        }
    }

    private String pathToClassName(Path file) {
        String fileString;
        if (root == file) {
            fileString = file.normalize()
                             .toString();
        } else {
            fileString = file.normalize()
                             .toString()
                             .substring(rootLength + 1);
        }
        return Utils.fileNameToClassName(fileString);
    }
}

