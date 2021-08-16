/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Launched by SetDefaultProvider to test startup with the default file system
 * provider overridden.
 */

public class Main {
    public static void main(String[] args) throws Exception {
        FileSystem fs = FileSystems.getDefault();
        if (fs.getClass().getModule() == Object.class.getModule())
            throw new RuntimeException("FileSystemProvider not overridden");

        // exercise the file system
        Path dir = Files.createTempDirectory("tmp");
        if (dir.getFileSystem() != fs)
            throw new RuntimeException("'dir' not in default file system");
        System.out.println("created: " + dir);

        Path foo = Files.createFile(dir.resolve("foo"));
        if (foo.getFileSystem() != fs)
            throw new RuntimeException("'foo' not in default file system");
        System.out.println("created: " + foo);

        // exercise interop with java.io.File
        File file = foo.toFile();
        Path path = file.toPath();
        if (path.getFileSystem() != fs)
            throw new RuntimeException("'path' not in default file system");
        if (!path.equals(foo))
            throw new RuntimeException(path + " not equal to " + foo);
    }
}
