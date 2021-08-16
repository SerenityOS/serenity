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

package nsk.jdi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExtraClassesInstaller {
    public static void main(String[] args) {
        Path target = Paths.get("bin")
                           .resolve(args[0]);
        ClassLoader cl = ExtraClassesInstaller.class.getClassLoader();
        for (int i = 1; i < args.length; ++i) {

            String pathName = args[i];
            try {
                try (InputStream is = cl.getResourceAsStream(pathName)) {
                    if (is == null) {
                        throw new Error("can't find " + pathName);
                    }
                    Path file = target.resolve(pathName);
                    Path dir = file.getParent();
                    try {
                        Files.createDirectories(dir);
                    } catch (IOException e) {
                        throw new Error("can't create dir " + dir, e);
                    }
                    try {
                        Files.copy(is, file);
                    } catch (IOException e) {
                        throw new Error("can't write to " + file, e);
                    }
                }
            } catch (IOException e) {
                throw new Error("IOE on closing " + pathName + " resource stream", e);
            }
        }
    }
}
