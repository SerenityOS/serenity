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

/*
 * @test
 * @bug 8189193
 * @library /test/lib
 * @build jdk.test.lib.process.ProcessTools
 * @build java.base/java.lang.BootNativeLibrary BootLoaderTest FindClassFromBoot
 * @run main/othervm/native -Xcheck:jni FindClassFromBoot
 * @summary verify if the native library loaded by the boot loader
 *          can only find classes visible to the boot loader
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.process.ProcessTools;

public class FindClassFromBoot {
    public static void main(String... args) throws Exception {
        Path patches = Paths.get(System.getProperty("test.classes"), "patches", "java.base");
        String syspaths = System.getProperty("sun.boot.library.path") +
                              File.pathSeparator + System.getProperty("java.library.path");
        ProcessTools.executeTestJvm("-Dsun.boot.library.path=" + syspaths,
                                    "--patch-module", "java.base=" + patches.toString(),
                                    "BootLoaderTest")
                    .shouldHaveExitValue(0);
    }
}
