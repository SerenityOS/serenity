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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;

import jdk.test.lib.util.JarUtils;

public class SetupJar {

    private static final String PROVIDER
            = "META-INF/services/java.nio.charset.spi.CharsetProvider";
    private static final String TEST_DIR = System.getProperty("test.dir", ".");

    public static void main(String args[]) throws Exception {
        Path xdir = Files.createDirectories(Paths.get(TEST_DIR, "xdir"));
        Path provider = xdir.resolve(PROVIDER);
        Files.createDirectories(provider.getParent());
        Files.write(provider, "FooProvider".getBytes(), CREATE);

        String[] files = {"FooCharset.class", "FooProvider.class"};
        for (String f : files) {
            Path source = Paths.get(System.getProperty("test.classes")).resolve(f);
            Path target = xdir.resolve(source.getFileName());
            Files.copy(source, target, REPLACE_EXISTING);
        }

        JarUtils.createJarFile(Paths.get("test.jar"), xdir);
    }
}
