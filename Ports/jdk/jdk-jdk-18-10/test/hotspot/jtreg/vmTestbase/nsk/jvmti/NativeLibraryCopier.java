/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti;

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NativeLibraryCopier {
    public static void main(String[] args) {
        Path src = Paths.get(Utils.TEST_NATIVE_PATH)
                        .resolve(libname(args[0]))
                        .toAbsolutePath();

        Path dstDir = Paths.get(".");
        for (int i = 1; i < args.length; ++i) {
            Path dst = dstDir.resolve(libname(args[i])).toAbsolutePath();
            System.out.println("copying " + src + " to " + dst);
            try {
                Files.copy(src, dst);
            } catch (IOException e) {
                throw new Error("can't copy " + src + " to " + dst, e);
            }
        }
    }

    private static String libname(String name) {
        return String.format("%s%s.%s",
                Platform.isWindows() ? "" : "lib",
                name,
                Platform.sharedLibraryExt());
    }
}
