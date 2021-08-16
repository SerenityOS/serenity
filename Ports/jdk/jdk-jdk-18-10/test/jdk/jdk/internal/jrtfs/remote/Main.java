/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Basic jrt file system functionality testing
 *
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String javaHome = args[0];
        FileSystem fs = null;
        boolean isInstalled = false;
        if (args.length == 2) {
            fs = createFsByInstalledProvider();
            isInstalled = true;
        } else {
            fs = createFsWithURLClassloader(javaHome);
        }

        Path mods = fs.getPath("/modules");
        try (Stream<Path> stream = Files.walk(mods)) {
            stream.forEach(path -> {
                path.getFileName();
            });
        } finally {
            try {
                fs.close();
            } catch (UnsupportedOperationException e) {
                if (!isInstalled) {
                    throw new RuntimeException(
                        "UnsupportedOperationException is thrown unexpectedly");
                }
            }
        }
    }

    private static FileSystem createFsWithURLClassloader(String javaHome) throws IOException{
        URL url = Paths.get(javaHome, "lib", "jrt-fs.jar").toUri().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[] { url });
        return FileSystems.newFileSystem(URI.create("jrt:/"),
                                                    Collections.emptyMap(),
                                                    loader);
    }

    private static FileSystem createFsByInstalledProvider() throws IOException {
        return FileSystems.getFileSystem(URI.create("jrt:/"));
    }
}
